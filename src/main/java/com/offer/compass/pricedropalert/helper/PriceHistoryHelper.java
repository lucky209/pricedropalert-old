package com.offer.compass.pricedropalert.helper;

import com.offer.compass.pricedropalert.constant.Constant;
import com.offer.compass.pricedropalert.entity.*;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class PriceHistoryHelper {

    @Autowired
    private BrowserHelper browserHelper;
    @Autowired
    private CommonHelper commonHelper;
    @Autowired
    private PriceHistoryRepo priceHistoryRepo;
    @Autowired
    private PriceHistoryGraphRepo priceHistoryGraphRepo;

    public void process(List<CurrentDeal> batchEntities) {
        WebDriver browser = browserHelper.openBrowser(true);
        List<String> tabs = browserHelper.openNTabs(browser, batchEntities.size());
        try {
            //load price history link in all tabs
            for (int i = 0; i < tabs.size(); i++) {
                browser.switchTo().window(tabs.get(i));
                browser.get(batchEntities.get(i).getPriceHistoryLink());
                if (batchEntities.size() < 10) {
                    Thread.sleep(1000);
                }
            }
            //fetch price history details
            for (int i = 0; i < tabs.size(); i++) {
                try {
                    browser.switchTo().window(tabs.get(i));
                    this.getAllPricesAndSaveInTable(browser, batchEntities.get(i));
                } catch (Exception ex) {
                    log.info("Error occurred. Retrying...");
                    try {
                        browser.navigate().refresh();
                        Thread.sleep(5000);
                        this.getAllPricesAndSaveInTable(browser, batchEntities.get(i));
                    } catch (Exception e) {
                        log.info("Exception occurred again. Exception is {} . So continuing with next tab", ex.getMessage());
                    }
                }
            }
            log.info("Current deal products price history details saved successfully...");
        } catch (Exception ex) {
            log.info("Error occurred for the current url {} .Exception is {}", browser.getCurrentUrl(), ex.getMessage());
        } finally {
            browser.quit();
        }
    }

    private void getAllPricesAndSaveInTable(WebDriver browser, CurrentDeal currentDeal) {
        Integer currentPrice = this.getCurrentPrice(browser, currentDeal.getPrice());
        Integer lowestPrice = this.getLowestPrice(browser);
        Integer highestPrice = this.getHighestPrice(browser);
        if (lowestPrice != null && highestPrice != null && currentPrice != null) {
            boolean isGoodProduct = this.isGoodProduct(currentPrice, lowestPrice, highestPrice);
            if (isGoodProduct) {
                String dropChances = this.getDropChances(browser);
                String productName = this.getProductName(browser);
                String ratingStar = this.getRatingStar(browser);
                this.saveInPriceHistoryTable(productName, ratingStar, currentPrice, lowestPrice, highestPrice,
                        dropChances, currentDeal.getUrl(), browser.getCurrentUrl());
            }
        }
    }

    private void saveInPriceHistoryTable(String productName, String ratingStar, int currentPrice, int lowestPrice,
                                         int highestPrice, String dropChances, String siteUrl, String currentUrl) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setProductName(productName);
        priceHistory.setRatingStar(ratingStar);
        priceHistory.setCurrentPrice(currentPrice);
        priceHistory.setLowestPrice(lowestPrice);
        priceHistory.setHighestPrice(highestPrice);
        priceHistory.setPhSiteUrl(siteUrl);
        priceHistory.setDropChances(dropChances);
        priceHistory.setPhUrl(currentUrl);
        priceHistory.setCreatedDate(LocalDateTime.now());
        priceHistory.setIsPicked(false);
        priceHistoryRepo.save(priceHistory);
    }

    private String getDropChances(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id("dropChances"));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        return null;
    }

    private boolean isGoodProduct(int currentPrice, int lowestPrice, int highestPrice) {
        int lowestDel = currentPrice - lowestPrice;
        int highestDel = highestPrice - currentPrice;
        if (lowestDel < highestDel) {
            int midVal = (highestPrice - lowestPrice)/4;
            return currentPrice <= (lowestPrice + midVal);
        }
        return false;
    }

    private Integer getHighestPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id("highestPrice"));
        if (!elements.isEmpty()) {
            return commonHelper.convertStringRupeeToInteger(elements.get(0).getText().trim());
        }
        log.info("Cannot fetch highest price for the url {}", browser.getCurrentUrl());
        return null;
    }

    private Integer getLowestPrice(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id("lowestPrice"));
        if (!elements.isEmpty()) {
            return commonHelper.convertStringRupeeToInteger(elements.get(0).getText().trim());
        }
        log.info("Cannot fetch lowest price for the url {}", browser.getCurrentUrl());
        return null;
    }

    private Integer getCurrentPrice(WebDriver browser, Integer price) {
        List<WebElement> elements = browser.findElements(By.id("currentPrice"));
        if (!elements.isEmpty()) {
            String currentPriceText = elements.get(0).getText().trim();
            if (!currentPriceText.contains(Constant.UTIL_RUPEE)) {
                log.info("Current Price is loading still. So passing the current deal table price.");
                return price;
            }
            return commonHelper.convertStringRupeeToInteger(currentPriceText);
        }
        return price;
    }

    private String getRatingStar(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.cssSelector(".text-gray-800.ml-2"));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch rating star for the url {}", browser.getCurrentUrl());
        return null;
    }

    private String getProductName(WebDriver browser) {
        List<WebElement> elements = browser.findElements(By.id("name"));
        if (!elements.isEmpty()) {
            return elements.get(0).getText().trim();
        }
        log.info("Cannot fetch product name for the url {}", browser.getCurrentUrl());
        return null;
    }

    public void graphProcess(List<CurrentDeal> batchEntities) {
        WebDriver browser = browserHelper.openBrowser(true);
        List<String> tabs = browserHelper.openNTabs(browser, batchEntities.size());
        try {
            //load price history link in all tabs
            for (int i = 0; i < tabs.size(); i++) {
                browser.switchTo().window(tabs.get(i));
                browser.get(batchEntities.get(i).getPriceHistoryLink());
                if (batchEntities.size() < 10) {
                    Thread.sleep(1000);
                }
            }
            //wait for current price and dotted element and get width of the element
            Actions actions = new Actions(browser);
            for (int i = 0; i < tabs.size(); i++) {
                browser.switchTo().window(tabs.get(i));
                try {
                    this.fetchDropDetails(browser, actions, batchEntities.get(i));
                } catch (Exception ex) {
                    log.info("Exception occurred. Exception is {} . So Retrying...", ex.getMessage());
                    try {
                        browser.get(batchEntities.get(i).getPriceHistoryLink());
                        Thread.sleep(3000);
                        this.fetchDropDetails(browser, actions, batchEntities.get(i));
                    } catch (Exception e) {
                        log.info("Exception occurred again for the url {} . Moving to next tab.", browser.getCurrentUrl());
                    }
                }
            }
        } catch (Exception ex) {
            log.info("Error occurred for the current url {} .Exception is {}", browser.getCurrentUrl(), ex.getMessage());
        } finally {
            browser.quit();
        }
    }

    private LocalDate convertPhDateToLocalDate(String phDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return LocalDate.parse(phDate, formatter);
    }

    private synchronized void moveOverElementByOffset(WebElement element, int width, Actions actions) {
        actions.moveToElement(element, width, 0);
        actions.moveToElement(element, width, 0);
        actions.build().perform();
    }

    private WebElement getCurrentPriceDottedElement(WebDriver browser) {
        List<WebElement> highCharts = browser.findElements(By.className("highcharts-plot-line"));
        if (!highCharts.isEmpty()) {
            for (WebElement element : highCharts) {
                if (element.getAttribute("stroke") != null) {
                    if (element.getAttribute("stroke").equalsIgnoreCase("purple")) {
                        return element;
                    }
                }
            }
        }
        return null;
    }

    private void loadCurrentPriceElement(WebDriver browser) {
        WebDriverWait wait = new WebDriverWait(browser, 15);
        WebElement cpElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("currentPrice")));
        while (StringUtils.isBlank(cpElement.getText()) ||
                cpElement.getText().trim().equalsIgnoreCase("Checking...")) {
            cpElement = browser.findElement(By.id("currentPrice"));
        }
    }

    private void fetchDropDetails(WebDriver browser, Actions actions, CurrentDeal currentDeal) {
        String currentPrice = null; String currentDate = null;String priceDropDate = null; String priceDropPrice = null;
        this.loadCurrentPriceElement(browser);
        WebElement cpDotElement = this.getCurrentPriceDottedElement(browser);
        if (cpDotElement != null) {
            Dimension dimension = cpDotElement.getSize();
            int width = dimension.getWidth() / 2;
            this.moveOverElementByOffset(cpDotElement, width, actions);
            //get current price
            List<WebElement> textElements = browser.findElements(By.tagName("text"));
            for (WebElement textElement : textElements) {
                if (textElement.getAttribute("x").equals("8")) {
                    List<WebElement> childElements = textElement.findElements(By.xpath("./*"));
                    if (childElements.size() == 4) {
                        currentDate = childElements.get(0).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                        currentPrice = childElements.get(3).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                        break;
                    }
                }
            }
            //move inside and find last price changed node
            for (int j=1;j<=5;j++) {
                this.moveOverElementByOffset(cpDotElement, width -(j*12), actions);
                textElements = browser.findElements(By.tagName("text"));
                for (WebElement textElement : textElements) {
                    if (textElement.getAttribute("x").equals("8")) {
                        List<WebElement> childElements = textElement.findElements(By.xpath("./*"));
                        if (childElements.size() == 4) {
                            priceDropDate = childElements.get(0).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                            priceDropPrice = childElements.get(3).getAttribute(Constant.ATTRIBUTE_INNER_HTML).trim();
                            if (currentPrice != null && !currentPrice.equals(priceDropPrice)) {
                                if (!currentDate.equals(priceDropDate)) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (priceDropPrice != null && !priceDropPrice.equals(currentPrice))
                    if (!priceDropDate.equals(currentDate))
                        break;
            }
        }
        //save in price history graph table
        if (priceDropPrice != null && !priceDropPrice.equals(currentPrice)) {
            if (!priceDropDate.equals(currentDate)) {
                this.savePriceHistoryGraphDetails(browser, currentDeal, priceDropDate, priceDropPrice,
                        currentDate, currentPrice);
            }
        }
    }

    private void savePriceHistoryGraphDetails(WebDriver browser, CurrentDeal currentDeal, String priceDropDate,
                                              String priceDropPrice, String currentDate, String currentPrice) {
        int priceDropFromPrice = commonHelper.convertStringRupeeToInteger(priceDropPrice);
        int priceDropToPrice = commonHelper.convertStringRupeeToInteger(currentPrice);
        if (priceDropFromPrice > priceDropToPrice) {
            PriceHistoryGraph priceHistory = new PriceHistoryGraph();
            priceHistory.setCreatedDate(LocalDateTime.now());
            priceHistory.setDropChances(this.getDropChances(browser));
            priceHistory.setHighestPrice(this.getHighestPrice(browser));
            priceHistory.setIsPicked(false);
            priceHistory.setLowestPrice(this.getLowestPrice(browser));
            priceHistory.setPhSiteUrl(currentDeal.getUrl());
            priceHistory.setPhUrl(currentDeal.getPriceHistoryLink());
            priceHistory.setPricedropFromDate(this.convertPhDateToLocalDate(priceDropDate));
            priceHistory.setPricedropFromPrice(priceDropFromPrice);
            priceHistory.setPricedropToDate(this.convertPhDateToLocalDate(currentDate));
            priceHistory.setPricedropToPrice(priceDropToPrice);
            priceHistory.setProductName(this.getProductName(browser));
            priceHistory.setRatingStar(this.getRatingStar(browser));
            priceHistory.setFilterFactor(this.getFilterFactor(priceDropFromPrice, priceDropToPrice));
            priceHistoryGraphRepo.save(priceHistory);
        }
    }

    private Integer getFilterFactor(int priceDropFromPrice, int priceDropToPrice) {
        double doubleDiff = ((double) priceDropFromPrice - (double) priceDropToPrice)/ (double) priceDropFromPrice;
        doubleDiff = doubleDiff * 100;
        BigDecimal bd = new BigDecimal(doubleDiff).setScale(2, RoundingMode.HALF_EVEN);
        return (int) bd.doubleValue();
    }
}
