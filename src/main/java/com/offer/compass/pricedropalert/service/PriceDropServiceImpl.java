package com.offer.compass.pricedropalert.service;

import com.google.common.collect.Lists;
import com.offer.compass.pricedropalert.constant.Constant;
import com.offer.compass.pricedropalert.constant.PriceHistoryConstants;
import com.offer.compass.pricedropalert.constant.PropertyConstants;
import com.offer.compass.pricedropalert.entity.*;
import com.offer.compass.pricedropalert.helper.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PriceDropServiceImpl implements PriceDropService {

    @Autowired
    private BrowserHelper browserHelper;
    @Autowired
    private PropertyRepo propertyRepo;
    @Autowired
    private CurrentDealRepo currentDealRepo;
    @Autowired
    private PriceHistoryHelper priceHistoryHelper;
    @Autowired
    private PriceHistoryRepo priceHistoryRepo;
    @Autowired
    private PriceHistoryGraphRepo priceHistoryGraphRepo;
    @Autowired
    private SiteDetailRepo siteDetailRepo;
    @Autowired
    private SiteDetailHelper siteDetailHelper;
    @Autowired
    private CommonHelper commonHelper;
    @Autowired
    private CurrentDealHelper currentDealHelper;
    @Autowired
    private CanvaHelper canvaHelper;

    @Value("${product.needed.count.default.value}")
    private int productNeededCount;
    @Value("${search.per.page}")
    private int searchPerPage;
    @Value("${filter.factor.value}")
    private int filterFactor;
    @Value("#{'${filter.factor.neglected.departments}'.split(',')}")
    private List<String> neglectedDepartments;

    @Override
    @Async
    public void getDeals() throws Exception {
        currentDealHelper.cleanupCurrentDealTable();
        WebDriver browser = browserHelper.openBrowser(true, PriceHistoryConstants.DEALS_URL);
        try {
            WebElement mainDiv = browser.findElement(By.id(PriceHistoryConstants.MAIN_PRODUCT_DIV_ID));
            if (propertyRepo.findByPropName(
                    PropertyConstants.PRICE_HISTORY_PRODUCT_NEEDED_COUNT).isEnabled()) {
                productNeededCount = Integer.parseInt(propertyRepo.findByPropName(
                        PropertyConstants.PRICE_HISTORY_PRODUCT_NEEDED_COUNT).getPropValue());
            }
            //fetching product elements
            List<WebElement> productElements = mainDiv.findElements(By.cssSelector(
                    PriceHistoryConstants.SINGLE_PRODUCT_CSS_SELECTOR));
            if (productElements.size() > 0) {
                int productsCount = productElements.size();
                while (productsCount < productNeededCount) {
                    browserHelper.executeScrollDownScript(browser, browser.findElement(By.tagName(PriceHistoryConstants
                            .SCROLL_DOWN_ELEMENT_TAG)));
                    String loadingTextClass =  browser.findElement(By.id(PriceHistoryConstants.LOADING_ELEMENT_ID))
                            .getAttribute(Constant.ATTRIBUTE_CLASS);
                    while (loadingTextClass.equals(PriceHistoryConstants.VISIBLE_LOADING_ELEMENT_CLASS_NAME)) {
                        loadingTextClass =  browser.findElement(By.id(PriceHistoryConstants.LOADING_ELEMENT_ID))
                                .getAttribute(Constant.ATTRIBUTE_CLASS);
                    }
                    productElements = mainDiv.findElements(By.cssSelector(PriceHistoryConstants
                            .SINGLE_PRODUCT_CSS_SELECTOR));
                    productsCount = productElements.size();
                    log.info("Fetched products count so far is {}", productsCount);
                }
                log.info("Fetched needed products...");
            } else
                log.info("No product elements found");
            //saving in current deal table
            log.info("Saving the products in the table...");
            String productName; String url; int price; String priceHistoryUrl;int saveCount=0;
            for (int i = 0; i < productNeededCount; i++) {
                WebElement elementProductName = productElements.get(i).findElement(By.cssSelector(
                        PriceHistoryConstants.PRODUCT_NAME_CSS_SELECTOR));
                if (elementProductName != null) {
                    productName = elementProductName.getText().trim();
                    if (!productName.toLowerCase().contains("amazon")) {
                        url = elementProductName.findElement(By.tagName(Constant.TAG_ANCHOR))
                                .getAttribute(Constant.ATTRIBUTE_HREF);
                        if (url.contains(PriceHistoryConstants.AMAZON_URL) ||
                                url.contains(PriceHistoryConstants.FLIPKART_URL)) {
                            price = commonHelper.convertStringRupeeToInteger(productElements.get(i).findElement(
                                    By.className(PriceHistoryConstants.PRICE_CLASS)).getText().trim());
                            priceHistoryUrl = productElements.get(i).findElement(By.className(
                                    PriceHistoryConstants.PRICE_HISTORY_URL_CLASS))
                                    .getAttribute(Constant.ATTRIBUTE_HREF);
                            CurrentDeal currentDeal = currentDealRepo.findByProductName(productName);
                            if (currentDeal == null) {
                                currentDealHelper.saveInCurrentDealTable(productName, url, price, priceHistoryUrl);
                                saveCount++;
                            }
                        }
                    }
                }
            }
            //save in property table
            Property property = propertyRepo.findByPropName(PropertyConstants.PRODUCTS_SAVED_IN_LAST_ATTEMPT_COUNT);
            property.setPropValue(String.valueOf(saveCount));
            property.setCreatedDate(LocalDateTime.now());
            propertyRepo.save(property);
            log.info("************Summary************");
            log.info("Number of products needed is {}", productNeededCount);
            log.info("Number of products saved successfully in current deal table is {}", saveCount);
            log.info("******************************");
        } catch (Exception ex) {
            log.info("Exception occurred. Quitting the browser...");
            browser.quit();
            throw new Exception("Exception occurred. Exception is " + ex.getMessage());
        }
        log.info("Quitting the browser...");
        browser.quit();
    }

    @Override
    public void getPriceHistoryGraphDetails() throws InterruptedException {
        priceHistoryGraphRepo.deleteAll();
        int lastAttemptFetchedCount = Integer.parseInt(propertyRepo.findByPropName(
                PropertyConstants.PRODUCTS_SAVED_IN_LAST_ATTEMPT_COUNT).getPropValue());
        List<CurrentDeal> currentDealList = currentDealRepo.fetchLastAttemptCurrentDeals(lastAttemptFetchedCount);
        int maxThreads = commonHelper.maxThreads(currentDealList.size());
        if (currentDealList.size() > 0) {
            log.info("Number of deals found from current_deal table is " + currentDealList.size());
            ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
            for (List<CurrentDeal> batchEntities : Lists.partition(currentDealList,
                    Math.min(currentDealList.size(), searchPerPage))) {
                Thread thread = new PriceHistoryGraphProcess(batchEntities, priceHistoryHelper);
                pool.execute(thread);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
            log.info("Completed the Price history process...");
        }
    }

    @Override
    public void getSiteDetails() throws Exception {
        siteDetailRepo.deleteAll();
        List<PriceHistory> priceHistoryList = priceHistoryRepo.findAll();
        int maxThreads = commonHelper.maxThreads(priceHistoryList.size());
        if (priceHistoryList.size() > 0) {
            log.info("Deals found from price_history table is " + priceHistoryList.size());
            ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
            for (List<PriceHistory> batchEntities : Lists.partition(priceHistoryList,
                    Math.min(priceHistoryList.size(), searchPerPage))) {
                Thread thread = new SiteDetailProcess(batchEntities, siteDetailHelper);
                pool.execute(thread);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
            log.info("Completed the get Site Details process...");
        }
    }

    @Override
    public void getPriceHistoryDetails() throws InterruptedException {
        priceHistoryRepo.deleteAll();
        int lastAttemptFetchedCount = Integer.parseInt(propertyRepo.findByPropName(
                PropertyConstants.PRODUCTS_SAVED_IN_LAST_ATTEMPT_COUNT).getPropValue());
        List<CurrentDeal> currentDealList = currentDealRepo.fetchLastAttemptCurrentDeals(lastAttemptFetchedCount);
        int maxThreads = commonHelper.maxThreads(currentDealList.size());
        if (currentDealList.size() > 0) {
            log.info("Number of deals found from current_deal table is " + currentDealList.size());
            ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
            for (List<CurrentDeal> batchEntities : Lists.partition(currentDealList,
                    Math.min(currentDealList.size(), searchPerPage))) {
                Thread thread = new PriceHistoryProcess(batchEntities, priceHistoryHelper);
                pool.execute(thread);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
            log.info("Completed the Price history process...");
        }
    }

    @Override
    public void updateFilterFactor() throws InterruptedException {
        List<SiteDetail> updateList = siteDetailRepo.findByIsPicked(true);
        updateList = updateList
                .stream()
                .filter(entity -> !neglectedDepartments.contains(entity.getMainDept())).collect(Collectors.toList());
        if (updateList.size() > 0) {
            log.info("Number of deals found from price history table is " + updateList.size());
            ExecutorService pool = Executors.newFixedThreadPool(1);
            for (List<SiteDetail> batchEntities : Lists.partition(updateList,
                    Math.min(updateList.size(), searchPerPage))) {
                Thread thread = new UpdateFilterFactorProcess(batchEntities, siteDetailHelper);
                pool.execute(thread);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
            log.info("Completed update filter factor process...");
        }
    }

    @Override
    public void shortenUrl() throws InterruptedException {
        List<SiteDetail> shortenUrlList = siteDetailRepo.findByIsPicked(true)
                .stream().sorted(Comparator.comparing(SiteDetail::getProductName)).collect(Collectors.toList());
        if (!shortenUrlList.isEmpty()) {
            log.info("Number of deals found from site_details table is " + shortenUrlList.size());
            ExecutorService pool = Executors.newFixedThreadPool(1);
            for (List<SiteDetail> batchEntities : Lists.partition(shortenUrlList,
                    Math.min(shortenUrlList.size(), searchPerPage))) {
                Thread thread = new ShortenUrlProcess(batchEntities, siteDetailHelper);
                pool.execute(thread);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
            log.info("Completed the shorten url process...");
        }
    }

    @Override
    public void downloadImages(String dept) throws InterruptedException {
        List<SiteDetail> shortenUrlList = siteDetailRepo.findByIsPicked(true)
                .stream().sorted(Comparator.comparing(SiteDetail::getProductName)).collect(Collectors.toList());
        if (!shortenUrlList.isEmpty()) {
            log.info("Number of deals found from site_details table is " + shortenUrlList.size());
            ExecutorService pool = Executors.newFixedThreadPool(1);
            int imgCount = 0;
            for (List<SiteDetail> batchEntities : Lists.partition(shortenUrlList,
                    Math.min(shortenUrlList.size(), searchPerPage))) {
                Thread thread = new downloadImagesProcess(batchEntities, siteDetailHelper, dept, imgCount);
                pool.execute(thread);
                imgCount = imgCount + searchPerPage;
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
        }
        log.info("Completed download images process...");
        //update product number
        log.info("Setting up the product numbers...");
        for (int i = 0; i < shortenUrlList.size(); i++) {
            shortenUrlList.get(i).setProductNo(i+1);
            siteDetailRepo.save(shortenUrlList.get(i));
        }
        log.info("Product number set up successfully...");
    }

    @Override
    public void makeCanvaDesign() throws Exception {
        List<SiteDetail> canvaList = siteDetailRepo.findByIsPicked(true)
                .stream().sorted(Comparator.comparing(SiteDetail::getProductName)).collect(Collectors.toList());
        log.info("Number of deals found from site_details table is " + canvaList.size());
        Property property = propertyRepo.findByPropName(PropertyConstants.HEADLESS_MODE);
        if (!canvaList.isEmpty()) {
            boolean isEnabled = property.isEnabled();
            property.setEnabled(false);
            propertyRepo.save(property);
            canvaHelper.makeCanvaDesign(canvaList);
            property.setEnabled(isEnabled);
            propertyRepo.save(property);
        }
    }

    @Override
    public void getYoutubeDescription(String dept) throws FileNotFoundException, UnsupportedEncodingException {
        String mainPath = Constant.PATH_TO_SAVE_YOUTUBE_DESC + dept + "-" + LocalDate.now() + ".txt";
        List<SiteDetail> youtubeDescList = siteDetailRepo.findByIsPicked(true)
                .stream().sorted(Comparator.comparing(SiteDetail::getProductName)).collect(Collectors.toList());
        //write text file
        PrintWriter writerDesc = new PrintWriter(mainPath, "UTF-8");
        for (SiteDetail siteDetail : youtubeDescList) {
            writerDesc.println(siteDetail.getProductNo() + ". " + siteDetail.getProductName());
            if (siteDetail.getAmazonShortUrl() != null)
                writerDesc.println("Amazon url -- " + siteDetail.getAmazonShortUrl());
            if (siteDetail.getFlipkartShortUrl() != null) {
                writerDesc.println("Flipkart url -- " + siteDetail.getFlipkartShortUrl());
            }
            writerDesc.println();
        }
        writerDesc.close();
        log.info("Description is printed successfully...");
    }

    @Override
    public void test(String url) throws InterruptedException {
        WebDriver browser = browserHelper.openBrowser(true);
        browser.get(url);
        //check current price loaded
        WebDriverWait wait = new WebDriverWait(browser, 15);
        WebElement cpElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("currentPrice")));
        while (StringUtils.isBlank(cpElement.getText()) ||
                cpElement.getText().trim().equalsIgnoreCase("Checking...")) {
            cpElement = browser.findElement(By.id("currentPrice"));
        }
        //get CP dot element dot line
        WebElement cpDotElement = null;
        List<WebElement> highCharts = browser.findElements(By.className("highcharts-plot-line"));
        if (!highCharts.isEmpty()) {
            for (WebElement element : highCharts) {
                if (element.getAttribute("stroke") != null) {
                    if (element.getAttribute("stroke").equalsIgnoreCase("purple")) {
                        cpDotElement = element;
                    }
                }
            }
        }
        //fetch values
        String priceDropDate = ""; String dropFromPrice = "";
        if (cpDotElement != null) {
            Dimension dimension = cpDotElement.getSize();
            int width = dimension.getWidth()/2;
            Actions actions = new Actions(browser);
            //move to the ned of the element
            actions.moveToElement(cpDotElement, width, 0);
            actions.moveToElement(cpDotElement, width, 0);
            actions.build().perform();
            log.info("Moved to the end of the element");
            List<WebElement> textElements = browser.findElements(By.tagName("text"));
            for (WebElement textElement : textElements) {
                if (textElement.getAttribute("x").equals("8")) {
                    List<WebElement> childElements = textElement.findElements(By.xpath("./*"));
                    if (childElements.size() == 4) {
                        log.info("found desired element");
                        priceDropDate = childElements.get(0).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                        dropFromPrice = childElements.get(3).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                        break;
                    }
                }
            }
            Thread.sleep(5000);
            String lastDate; String lastPrice = null;
            //now hover very slow to left maybe width of -10?
            for (int i=1;i<=5;i++) {
                actions.moveToElement(cpDotElement, width-(i*12), 0);
                actions.moveToElement(cpDotElement, width-(i*12), 0);
                actions.build().perform();
                Thread.sleep(1000);
                textElements = browser.findElements(By.tagName("text"));
                for (WebElement textElement : textElements) {
                    if (textElement.getAttribute("x").equals("8")) {
                        List<WebElement> childElements = textElement.findElements(By.xpath("./*"));
                        if (childElements.size() == 4) {
                            lastDate = childElements.get(0).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                            lastPrice = childElements.get(3).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                            if (!dropFromPrice.equals(lastPrice)) {
                                if (!lastDate.equals(priceDropDate)) {
                                    log.info("diff node value found");
                                    log.info("lastDate {}", lastDate);
                                    log.info("lastPrice {}", lastPrice);
                                    log.info("priceDropDate {}", priceDropDate);
                                    log.info("dropFromPrice {}", dropFromPrice);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (lastPrice != null)
                    if (!lastPrice.equals(dropFromPrice))
                        break;
            }

            if (lastPrice != null) {
                if (lastPrice.equals(dropFromPrice))
                    log.info("Couldnt find diff node value for the url {}", browser.getCurrentUrl());
            } else {
                log.info("Couldnt find diff node value for the url {}", browser.getCurrentUrl());
            }
        }
        browser.quit();
    }
}
