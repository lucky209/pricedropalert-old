package com.offer.compass.pricedropalert.helper;

import com.offer.compass.pricedropalert.constant.Constant;
import com.offer.compass.pricedropalert.constant.GoogleConstants;
import com.offer.compass.pricedropalert.constant.PriceHistoryConstants;
import com.offer.compass.pricedropalert.constant.PropertyConstants;
import com.offer.compass.pricedropalert.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class SiteDetailHelper {

    @Autowired
    private BrowserHelper browserHelper;
    @Autowired
    private SiteDetailRepo siteDetailRepo;
    @Autowired
    private PriceDropDetailRepo priceDropDetailRepo;
    @Autowired
    private CommonHelper commonHelper;
    @Autowired
    private PropertyRepo propertyRepo;

    public void process(List<PriceHistory> batchEntities) {
        WebDriver browser = browserHelper.openBrowser(true);
        List<String> tabs = browserHelper.openNTabs(browser, batchEntities.size());
        //load product site urls in all tabs
        try {
            for (int i = 0; i < tabs.size(); i++) {
                browser.switchTo().window(tabs.get(i));
                browser.get(batchEntities.get(i).getPhSiteUrl());
                if (batchEntities.size() < 10) {
                    Thread.sleep(1000);
                }
            }
            for (int i = 0; i < tabs.size(); i++) {
                try {
                    browser.switchTo().window(tabs.get(i));
                    //get dept
                    List<String> dept; String productName; Integer price;String ratingStar;
                    if (batchEntities.get(i).getPhSiteUrl().contains("www.flipkart.com")) {
                        browser.get(batchEntities.get(i).getPhSiteUrl());
                        Thread.sleep(2000);
                        dept = this.getFlipkartDepts(browser);
                        productName = this.getFlipkartProductName(browser);
                        price = this.getFlipkartPrice(browser);
                        ratingStar = this.getFlipkartRatingStar(browser);
                        boolean isAvailableToBuy = this.isAvailableToBuy(browser);
                        if (price != null && isAvailableToBuy)
                            this.saveInSiteDetailTable(dept, productName, price, ratingStar, "Flipkart",
                                    batchEntities.get(i).getPhSiteUrl(), browser.getCurrentUrl());
                    } else if (batchEntities.get(i).getPhSiteUrl().contains("www.amazon.in")) {
                        dept = this.getAmazonDepts(browser);
                        productName = this.getAmazonProductName(browser);
                        price = this.getAmazonPrice(browser);
                        ratingStar = this.getAmazonRatingStar(browser);
                        if (price != null)
                            this.saveInSiteDetailTable(dept, productName, price, ratingStar, "Amazon",
                                    batchEntities.get(i).getPhSiteUrl(), browser.getCurrentUrl());
                    } else {
                        log.info("Different site found for the url {}", batchEntities.get(i).getPhSiteUrl());
                    }
                } catch (Exception ex) {
                    log.info("Exception occurred. Exception is {} . So continuing with next tab", ex.getMessage());
                }
            }
            log.info("Price history products site details saved successfully...");
        } catch (Exception ex) {
            log.info("Error occurred for the current url {} .Exception is {}", browser.getCurrentUrl(), ex.getMessage());
        } finally {
            browser.quit();
        }
    }

    boolean isAvailableToBuy(WebDriver browser) {
        return !(browser.getPageSource().toLowerCase().contains("sold out") ||
                browser.getPageSource().toLowerCase().contains("currently unavailable"));
    }

    private void saveInSiteDetailTable(List<String> depts, String productName, int price,
                                       String ratingStar, String site, String phUrl, String siteUrl) {
        int deptCount = Math.min(depts.size(), 6);
        SiteDetail siteDetail = new SiteDetail();
        siteDetail.setPhUrl(phUrl);
        siteDetail.setSiteUrl(siteUrl);
        siteDetail.setProductName(productName);
        siteDetail.setPrice(price);
        siteDetail.setRatingStar(ratingStar);
        siteDetail.setSiteName(site);
        siteDetail.setCreatedDate(LocalDateTime.now());
        siteDetail.setIsPicked(false);
        if (deptCount == 6) {
            siteDetail.setMainDept(depts.get(deptCount-6));
            siteDetail.setSubDept1(depts.get(deptCount-5));
            siteDetail.setSubDept2(depts.get(deptCount-4));
            siteDetail.setSubDept3(depts.get(deptCount-3));
            siteDetail.setSubDept4(depts.get(deptCount-2));
            siteDetail.setSubDept5(depts.get(deptCount-1));
        } else if (deptCount == 5) {
            siteDetail.setMainDept(depts.get(deptCount-5));
            siteDetail.setSubDept1(depts.get(deptCount-4));
            siteDetail.setSubDept2(depts.get(deptCount-3));
            siteDetail.setSubDept3(depts.get(deptCount-2));
            siteDetail.setSubDept4(depts.get(deptCount-1));
        } else if (deptCount == 4) {
            siteDetail.setMainDept(depts.get(deptCount-4));
            siteDetail.setSubDept1(depts.get(deptCount-3));
            siteDetail.setSubDept2(depts.get(deptCount-2));
            siteDetail.setSubDept3(depts.get(deptCount-1));
        } else if (deptCount == 3) {
            siteDetail.setMainDept(depts.get(deptCount-3));
            siteDetail.setSubDept1(depts.get(deptCount-2));
            siteDetail.setSubDept2(depts.get(deptCount-1));
        } else if (deptCount == 2) {
            siteDetail.setMainDept(depts.get(deptCount-2));
            siteDetail.setSubDept1(depts.get(deptCount-1));
        } else if (deptCount == 1) {
            siteDetail.setMainDept(depts.get(deptCount-1));
        } else {
            log.info("No departments found for the url {}", siteUrl);
        }
        siteDetailRepo.save(siteDetail);
    }

    private synchronized String getAmazonRatingStar(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id("acrPopover"));
        if (!elements.isEmpty()) {
            String ratingStar = elements.get(0).getAttribute("title").trim();
            return ratingStar.substring(0, Math.min(ratingStar.length(), 3));
        }
        return null;
    }

    private synchronized Integer getAmazonPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id("priceblock_dealprice"));
        if (!elements.isEmpty()) {
            return commonHelper.convertStringRupeeToInteger(elements.get(0).getText().trim());
        } else {
            elements = browser.findElements(By.id("priceblock_ourprice"));
            if (!elements.isEmpty()) {
                return commonHelper.convertStringRupeeToInteger(elements.get(0).getText().trim());
            } else {
                elements = browser.findElements(By.id("priceblock_saleprice"));
                if (!elements.isEmpty()) {
                    return commonHelper.convertStringRupeeToInteger(elements.get(0).getText().trim());
                }
            }
        }
        return null;
    }

    synchronized String getAmazonProductName(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id("productTitle"));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch product name of the amazon element for the url {}", browser.getCurrentUrl());
        return null;
    }

    private synchronized String getFlipkartRatingStar(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.xpath("//*[starts-with(@id,'productRating_')]"));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        return null;
    }

    private synchronized Integer getFlipkartPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.cssSelector("._30jeq3._16Jk6d"));
        if (!elements.isEmpty()) {
            return commonHelper.convertStringRupeeToInteger(elements.get(0).getText().trim());
        }
        return null;
    }

    synchronized String getFlipkartProductName(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.className("B_NuCI"));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch product name of the flipkart element for the url " + browser.getCurrentUrl());
        return null;
    }

    synchronized List<String> getAmazonDepts(WebDriver browser) {
        List<String> dept = new ArrayList<>();
        List<WebElement> deptElements = browser.findElements(By.id("wayfinding-breadcrumbs_feature_div"));
        if (!deptElements.isEmpty()) {
            List<WebElement> liElementList = deptElements.get(0).findElements(By.tagName(Constant.TAG_LI));
            for (WebElement element : liElementList) {
                if (!element.getText().trim().contains("›")) {
                    dept.add(element.getText().trim());
                }
            }
        } else {
            deptElements = browser.findElements(By.id("nav-subnav"));
            if (!deptElements.isEmpty()) {
                List<WebElement> anchorElementList = deptElements.get(0).findElements(By.tagName(Constant.TAG_ANCHOR));
                for (WebElement element : anchorElementList) {
                    if (!element.getText().trim().contains("›")) {
                        dept.add(element.getText().trim());
                    }
                }
            }
        }
        if (dept.size() == 0)
            log.info("Cannot fetch departments of the amazon element for the url {}", browser.getCurrentUrl());
        return dept;
    }

    synchronized List<String> getFlipkartDepts(WebDriver browser) throws Exception {
        List<String> dept = new ArrayList<>();
        List<WebElement> deptElements = browser.findElements(By.className("_1MR4o5"));
        if (!deptElements.isEmpty()) {
            List<WebElement> anchorElementList = deptElements.get(0).findElements(By.tagName(Constant.TAG_ANCHOR));
            for (WebElement element : anchorElementList) {
                if (!element.getText().trim().toLowerCase().equals("home")) {
                    dept.add(element.getText().trim());
                }
            }
            if (dept.size() > 0) {
                return dept;
            }
        }
        throw new Exception("Cannot fetch departments of the flipkart element for the url " + browser.getCurrentUrl());
    }

    public void updateFilterFactorProcess(List<SiteDetail> batchEntities) throws InterruptedException {
        Property property = propertyRepo.findByPropName(PropertyConstants.CROSS_SITE_PROCESS_ENABLED);
        property.setEnabled(false);
        propertyRepo.save(property);
        WebDriver browser = browserHelper.openBrowser(true);
        List<String> tabs = browserHelper.openNTabs(browser, batchEntities.size());
        //load bing web in all tabs
        for (int i = 0; i < tabs.size(); i++) {
            browser.switchTo().window(tabs.get(i));
            browser.get(GoogleConstants.URL);
            if (batchEntities.get(i).getSiteName().equalsIgnoreCase("amazon")) {
                browser.findElement(By.cssSelector(GoogleConstants.SEARCH_INPUT_CSS_CLASS))
                        .sendKeys(batchEntities.get(i).getProductName() +
                                GoogleConstants.FLIPKART_SEARCH + Keys.ENTER);
            } else {
                browser.findElement(By.cssSelector(GoogleConstants.SEARCH_INPUT_CSS_CLASS))
                        .sendKeys(batchEntities.get(i).getProductName() +
                                GoogleConstants.AMAZON_SEARCH + Keys.ENTER);
            }
            Thread.sleep(2000);
        }
        //wait for selecting the link manually
        while(!propertyRepo.findByPropName(PropertyConstants.CROSS_SITE_PROCESS_ENABLED).isEnabled()) {
            log.info("Waiting for cross site process to be enable");
            Thread.sleep(30000);
        }
        Integer crossSitePrice;
        for (int i = 0; i < tabs.size(); i++) {
            browser.switchTo().window(tabs.get(i));
            browser.navigate().refresh();
            Thread.sleep(1000);
            if (batchEntities.get(i).getSiteName().equalsIgnoreCase("amazon")) {
                if (browser.getCurrentUrl().contains(GoogleConstants.FLIPKART_URL)) {
                    crossSitePrice = this.getFlipkartPrice(browser);
                    if (crossSitePrice != null) {
                        this.saveFilterFactor(crossSitePrice, batchEntities.get(i), browser.getCurrentUrl());
                    }
                } else {
                    log.info("Cannot fetch cross site price form flipkart");
                }
            } else {
                if (browser.getCurrentUrl().contains(GoogleConstants.AMAZON_URL)) {
                    crossSitePrice = this.getAmazonPrice(browser);
                    if (crossSitePrice != null) {
                        this.saveFilterFactor(crossSitePrice, batchEntities.get(i), browser.getCurrentUrl());
                    }
                } else {
                    log.info("Cannot fetch cross site price form amazon");
                }
            }
        }
        //revert the cross site process
        property.setEnabled(false);
        propertyRepo.save(property);
        browser.quit();
    }

    private void saveFilterFactor(int crossSitePrice, SiteDetail siteDetail, String crossSiteUrl) {
        int diffFactor = this.getDifferenceFactor(siteDetail.getPrice(), crossSitePrice);
        siteDetail.setFilterFactor(diffFactor);
        siteDetail.setCrossSitePrice(crossSitePrice);
        siteDetail.setCrossSiteUrl(crossSiteUrl);
        siteDetailRepo.save(siteDetail);
    }

    private int getDifferenceFactor(int currentPrice, int crossSitePrice) {
        if ((double) currentPrice < (double) crossSitePrice) {
            double doubleDiff = ((double) crossSitePrice - (double) currentPrice)/ (double) crossSitePrice;
            doubleDiff = doubleDiff * 100;
            BigDecimal bd = new BigDecimal(doubleDiff).setScale(2, RoundingMode.HALF_EVEN);
            return (int) bd.doubleValue();
        } else {
            double doubleDiff = ((double) currentPrice - (double) crossSitePrice)/ (double) currentPrice;
            doubleDiff = doubleDiff * 100;
            BigDecimal bd = new BigDecimal(doubleDiff).setScale(2, RoundingMode.HALF_EVEN);
            return (int) bd.doubleValue();
        }
    }

    public void shortenUrlProcess(List<PriceDropDetail> batchEntities, boolean isCrossSiteUrl) throws Exception {
        Property property = propertyRepo.findByPropName(PropertyConstants.HEADLESS_MODE);
        boolean isEnabled = property.isEnabled();
        property.setEnabled(false);
        propertyRepo.save(property);

        //open tabs
        WebDriver browser = browserHelper.openBrowser(true);
        System.setProperty("java.awt.headless", "false");
        List<String> tabs = browserHelper.openNTabs(browser, batchEntities.size());
        for (String tab : tabs) {
            browser.switchTo().window(tab);
            browser.get(Constant.SHORTEN_WEB_PAGE);
        }
        //close popup
        for (String tab : tabs) {
            browser.switchTo().window(tab);
            this.closePopup(browser);
        }
        //click short url button and copy & save
        for (int i=0;i<tabs.size();i++) {
            boolean isSendKeys = false;
            browser.switchTo().window(tabs.get(i));
            try {
                if (!isCrossSiteUrl) {
                    browser.findElement(By.id(Constant.LONG_URL_TEXT_BOX_ID)).sendKeys(batchEntities.get(i).getSiteUrl());
                    isSendKeys = true;
                } else {
                    if (batchEntities.get(i).getCrossSiteUrl() != null) {
                        browser.findElement(By.id(Constant.LONG_URL_TEXT_BOX_ID)).sendKeys(batchEntities.get(i).getCrossSiteUrl());
                        isSendKeys = true;
                    }
                }
                if (isSendKeys) {
                    browser.findElement(By.id(Constant.SHORTEN_BUTTON_ID)).click();
                    Thread.sleep(600);
                    String copiedUrl = this.clickCopyButtonAndGetUrl(browser);
                    if (!isCrossSiteUrl) {
                        if (batchEntities.get(i).getSiteUrl().contains(PriceHistoryConstants.AMAZON_URL)) {
                            batchEntities.get(i).setAmazonShortUrl(copiedUrl);
                        } else {
                            batchEntities.get(i).setFlipkartShortUrl(copiedUrl);
                        }
                    } else {
                        if (batchEntities.get(i).getCrossSiteUrl().contains(PriceHistoryConstants.AMAZON_URL)) {
                            batchEntities.get(i).setAmazonShortUrl(copiedUrl);
                        } else {
                            batchEntities.get(i).setFlipkartShortUrl(copiedUrl);
                        }
                    }
                    priceDropDetailRepo.save(batchEntities.get(i));
                }
            } catch (Exception ex) {
                log.info("Exception occurred for the url {}. Exception is {}", browser.getCurrentUrl(), ex.getMessage());
                log.info("Continuing with next tab...");
            }
        }
        property.setEnabled(isEnabled);
        propertyRepo.save(property);
        browser.quit();
    }

    private synchronized void closePopup(WebDriver browser) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(browser, 10);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(
                By.id(Constant.SHORTEN_WEB_POPUP_CLOSE_BUTTON_ID)));
        Actions actions = new Actions(browser);
        actions.moveToElement(element);
        actions.click().build().perform();
        Thread.sleep(1000);
    }

    private synchronized String clickCopyButtonAndGetUrl(WebDriver browser) throws Exception {
        browser.findElement(By.id("shortened_btn")).click();
        Thread.sleep(500);
        return  (String) Toolkit.getDefaultToolkit().
                getSystemClipboard().getData(DataFlavor.stringFlavor);
    }

    public void downloadImagesProcess(List<SiteDetail> batchEntities, String dept, int imgCount) {
        //open tabs
        WebDriver browser = browserHelper.openBrowser(true);
        List<String> tabs = browserHelper.openNTabs(browser, batchEntities.size());
        //open amazon urls in all tabs
        for (int i=0;i<tabs.size();i++) {
            browser.switchTo().window(tabs.get(i));
            browser.get(batchEntities.get(i).getSiteUrl());
        }
        //download images
        for (int i=0;i<tabs.size();i++) {
            try {
                browser.switchTo().window(tabs.get(i));
                if (browser.getCurrentUrl().contains(PriceHistoryConstants.AMAZON_URL)) {
                    this.downloadAmazonImages(browser, imgCount, dept);
                    browser.navigate().refresh();
                    this.takeScreenshot(browser, dept, imgCount, true);
                    if (batchEntities.get(i).getCrossSiteUrl() != null) {
                        browser.get(batchEntities.get(i).getCrossSiteUrl());
                        this.takeScreenshot(browser, dept, imgCount, false);
                    }
                } else {
                    this.downloadFlipkartImages(browser, imgCount, dept);
                    this.takeScreenshot(browser, dept, imgCount, false);
                    if (batchEntities.get(i).getCrossSiteUrl() != null) {
                        browser.get(batchEntities.get(i).getCrossSiteUrl());
                        this.takeScreenshot(browser, dept, imgCount, true);
                    }
                }
            } catch (Exception ex) {
               log.info("Exception occurred for the url {}. Exception is {}", browser.getCurrentUrl(), ex.getMessage());
                log.info("Continuing with next tab...");
            }
            imgCount++;
        }
        browser.quit();
    }

    void downloadFlipkartImages(WebDriver browser, int count, String dept) throws Exception {
        List<WebElement> liElements = browser.findElement(By.className("_2mLllQ")).findElements(
                By.tagName(Constant.TAG_LI));
        Actions actions = new Actions(browser);
        for (int i = 0; i < liElements.size(); i++) {
            actions.moveToElement(liElements.get(i)).build().perform();
            List<WebElement> imgElements = browser.findElement(By.className("_1BweB8")).findElements(By.tagName(Constant.TAG_IMAGE));
            if (!imgElements.isEmpty()) {
                String imgSrc = imgElements.get(0).getAttribute(Constant.TAG_SRC);
                URL url = new URL(imgSrc);
                BufferedImage saveImage = ImageIO.read(url);
                String folderPath = Constant.PATH_TO_SAVE_THUMBNAIL + dept + "-" + LocalDate.now() + Constant.UTIL_PATH_SLASH;
                String pathToSave = folderPath + (count + 1) + "-" + (i + 1) + Constant.IMAGE_FORMAT;
                this.createImageFromBufferedImage(saveImage, pathToSave, folderPath);
                Thread.sleep(1500);
            }
        }
    }

    void downloadAmazonImages(WebDriver browser, int count, String dept) throws Exception {
        browser.findElement(By.id(Constant.IMAGE_ID)).click();
        Thread.sleep(1000);
        List<WebElement> thumbnailElements = browser.findElements(By.cssSelector(Constant.THUMBNAILS_CSS_SELECTOR));
        for (int j = 0; j < thumbnailElements.size(); j++) {
            browser.findElement(By.id("ivImage_" + j)).click();
            Thread.sleep(1500);
            this.downloadAndSaveAmazonProductImage(browser, (count+1) + "-" + (j+1) + Constant.IMAGE_FORMAT, dept);
        }
    }

    private void takeScreenshot(WebDriver browser, String dept, int count, boolean isAmazon) throws IOException {
        File srcFile = ((TakesScreenshot) browser).getScreenshotAs(OutputType.FILE);
        BufferedImage image = ImageIO.read(srcFile);
        String folderPath = Constant.PATH_TO_SAVE_THUMBNAIL + dept + "-" + LocalDate.now() + Constant.UTIL_PATH_SLASH;
        String pathToSave;
        if (isAmazon)
            pathToSave = folderPath + (count+1)+ "-SS-Amazon" + Constant.IMAGE_FORMAT;
        else
            pathToSave = folderPath + (count+1)+ "-SS-Flipkart" + Constant.IMAGE_FORMAT;
        this.createImageFromBufferedImage(image, pathToSave, folderPath);
    }

    private void downloadAndSaveAmazonProductImage(WebDriver browser, String imgName, String dept) throws Exception {
        String folderPath = Constant.PATH_TO_SAVE_THUMBNAIL + dept + "-" + LocalDate.now() + Constant.UTIL_PATH_SLASH;
        String pathToSave = folderPath + imgName;
        WebElement imgElement = browser.findElement(By.id(Constant.THUMBNAIL_ID)).findElement(By.tagName(Constant.TAG_IMAGE));
        String imgSrc = imgElement.getAttribute(Constant.TAG_SRC);
        URL url = new URL(imgSrc);
        BufferedImage saveImage = ImageIO.read(url);
        this.createImageFromBufferedImage(saveImage, pathToSave, folderPath);
        Thread.sleep(1500);
    }

    private void createImageFromBufferedImage(BufferedImage image, String pathToSave, String folderPath) throws IOException {
        this.createNewDirectory(folderPath);
        pathToSave = this.renameFile(pathToSave);
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        ImageIO.write(copy, Constant.IMAGE_FORMAT_V2, new File(pathToSave));
    }

    private void createNewDirectory(String folderPath) {
        File file = new File(folderPath);
        if (!file.exists()) {
            boolean isCreated = file.mkdirs();
            if (isCreated)
                log.info("New folder created, path is " + folderPath);
        }
    }

    private String renameFile(String fileName) {
        if (new File(fileName).isFile()) {
            log.info("File with same name found. Renaming it...");
            fileName = fileName.replace(Constant.IMAGE_FORMAT, "") + "-copy" + Constant.IMAGE_FORMAT;
            return renameFile(fileName);
        }
        return fileName;
    }
}
