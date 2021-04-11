package com.offer.compass.pricedropalert.service;

import com.google.common.collect.Lists;
import com.offer.compass.pricedropalert.constant.Constant;
import com.offer.compass.pricedropalert.constant.PriceHistoryConstants;
import com.offer.compass.pricedropalert.constant.PropertyConstants;
import com.offer.compass.pricedropalert.entity.*;
import com.offer.compass.pricedropalert.helper.*;
import com.offer.compass.pricedropalert.model.VoiceTextDetails;
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
import java.util.ArrayList;
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
    @Autowired
    private PriceDropDetailRepo priceDropDetailRepo;
    @Autowired
    private PriceDropAlertProcessHelper priceDropAlertProcessHelper;

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
    public void getPriceDropDetails() throws InterruptedException {
        priceDropDetailRepo.deleteAll();
        List<PriceHistoryGraph> priceHistoryGraphList = priceHistoryGraphRepo.findByIsPicked(true);
        int maxThreads = commonHelper.maxThreads(priceHistoryGraphList.size());
        if (priceHistoryGraphList.size() > 0) {
            log.info("Deals found from price_history table is " + priceHistoryGraphList.size());
            ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
            for (List<PriceHistoryGraph> batchEntities : Lists.partition(priceHistoryGraphList,
                    Math.min(priceHistoryGraphList.size(), searchPerPage))) {
                Thread thread = new PriceDropAlertProcess(batchEntities, priceDropAlertProcessHelper);
                pool.execute(thread);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
            log.info("Completed the get Site Details process...");
        }
    }

    @Override
    public void shortenUrl() throws InterruptedException {
        List<PriceDropDetail> shortenUrlList = priceDropDetailRepo.findByIsPicked(true)
                .stream().sorted(Comparator.comparing(PriceDropDetail::getProductName)).collect(Collectors.toList());
        if (!shortenUrlList.isEmpty()) {
            log.info("Number of deals found from site_details table is " + shortenUrlList.size());
            ExecutorService pool = Executors.newFixedThreadPool(1);
            for (List<PriceDropDetail> batchEntities : Lists.partition(shortenUrlList,
                    Math.min(shortenUrlList.size(), searchPerPage))) {
                Thread thread = new ShortenUrlProcess(batchEntities, siteDetailHelper);
                pool.execute(thread);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.HOURS);
            log.info("Completed the shorten url process...");
        }

        //update product number
        log.info("Setting up the product numbers...");
        for (int i = 0; i < shortenUrlList.size(); i++) {
            shortenUrlList.get(i).setProductNo(i+1);
            priceDropDetailRepo.save(shortenUrlList.get(i));
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
    public void getTextDetails(String dept) throws FileNotFoundException, UnsupportedEncodingException {
        String mainPath = Constant.PATH_TO_SAVE_YOUTUBE_DESC + dept + "-" + LocalDate.now() + ".txt";
        List<PriceDropDetail> youtubeDescList = priceDropDetailRepo.findByIsPicked(true)
                .stream().sorted(Comparator.comparing(PriceDropDetail::getProductName)).collect(Collectors.toList());
        //write youtube desc text file
        PrintWriter writerDesc = new PrintWriter(mainPath, "UTF-8");
        for (PriceDropDetail priceDropDetail : youtubeDescList) {
            writerDesc.println(priceDropDetail.getProductNo() + ". " + priceDropDetail.getProductName());
            if (priceDropDetail.getAmazonShortUrl() != null)
                writerDesc.println("Amazon url -- " + priceDropDetail.getAmazonShortUrl());
            if (priceDropDetail.getFlipkartShortUrl() != null) {
                writerDesc.println("Flipkart url -- " + priceDropDetail.getFlipkartShortUrl());
            }
            writerDesc.println();
        }
        writerDesc.close();
        log.info("Description is printed successfully...");
        List<VoiceTextDetails> voiceDetailsTextList = new ArrayList<>();
        for (PriceDropDetail priceDropDetail : youtubeDescList) {
            PriceHistoryGraph priceHistoryGraph = priceHistoryGraphRepo.findByPhUrl(priceDropDetail.getPhUrl());
            VoiceTextDetails voiceTextDetails = new VoiceTextDetails();
            voiceTextDetails.setDropChances(priceHistoryGraph.getDropChances());
            voiceTextDetails.setHighestPrice(priceHistoryGraph.getHighestPrice());
            voiceTextDetails.setLowestPrice(priceHistoryGraph.getLowestPrice());
            voiceTextDetails.setPhUrl(priceHistoryGraph.getPhUrl());
            voiceTextDetails.setPricedropFromDate(priceHistoryGraph.getPricedropFromDate());
            voiceTextDetails.setPricedropFromPrice(priceHistoryGraph.getPricedropFromPrice());
            voiceTextDetails.setPricedropToPrice(priceHistoryGraph.getPricedropToPrice());
            voiceTextDetails.setProductName(priceHistoryGraph.getProductName());
            voiceTextDetails.setProductNo(priceDropDetail.getProductNo());
            voiceTextDetails.setRatingStar(priceHistoryGraph.getRatingStar());
            voiceTextDetails.setSiteName(priceDropDetail.getSiteName());
            voiceTextDetails.setUrl(priceDropDetail.getSiteUrl());
            voiceDetailsTextList.add(voiceTextDetails);
        }
        mainPath = Constant.PATH_TO_SAVE_YOUTUBE_DESC + dept + "-VoiceText-" + LocalDate.now() + ".txt";
        PrintWriter writerVoiceDesc = new PrintWriter(mainPath, "UTF-8");
        for (VoiceTextDetails voiceTextDetail : voiceDetailsTextList) {
            writerVoiceDesc.println(voiceTextDetail.getProductNo() + "."
                    + voiceTextDetail.getProductName() + "--" + voiceTextDetail.getSiteName());
            writerVoiceDesc.println(voiceTextDetail.getUrl());
            writerVoiceDesc.println(voiceTextDetail.getPhUrl());
            writerVoiceDesc.println("Lowest price -- " + voiceTextDetail.getLowestPrice());
            writerVoiceDesc.println("Highest price -- " + voiceTextDetail.getHighestPrice());
            writerVoiceDesc.println("Todays price -- " + voiceTextDetail.getPricedropToPrice());
            writerVoiceDesc.println("From Date -- " + voiceTextDetail.getPricedropFromDate() +
                    "  From Price -- " + voiceTextDetail.getPricedropFromPrice());
            writerVoiceDesc.println("Drop chances -- " + voiceTextDetail.getDropChances());
            writerVoiceDesc.println("Rating star -- " + voiceTextDetail.getRatingStar());
            writerVoiceDesc.println();
        }
        writerVoiceDesc.close();
        log.info("Voice details is printed successfully...");
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
    public void test(String url) throws InterruptedException {

    }
}
