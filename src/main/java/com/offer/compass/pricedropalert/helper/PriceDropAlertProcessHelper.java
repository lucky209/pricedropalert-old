package com.offer.compass.pricedropalert.helper;

import com.offer.compass.pricedropalert.entity.PriceDropDetail;
import com.offer.compass.pricedropalert.entity.PriceDropDetailRepo;
import com.offer.compass.pricedropalert.entity.PriceHistoryGraph;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class PriceDropAlertProcessHelper {

    @Autowired
    private BrowserHelper browserHelper;
    @Autowired
    private SiteDetailHelper siteDetailHelper;
    @Autowired
    private PriceDropDetailRepo priceDropDetailRepo;

    public void process(List<PriceHistoryGraph> batchEntities) {
        //open browser tabs
        WebDriver browser = browserHelper.openBrowser(true);
        List<String> tabs = browserHelper.openNTabs(browser, batchEntities.size());
        try {
            //open all urls in all tabs
            for (int i = 0; i < tabs.size(); i++) {
                browser.switchTo().window(tabs.get(i));
                browser.get(batchEntities.get(i).getPhSiteUrl());
                if (batchEntities.size() < 10) {
                    Thread.sleep(1000);
                }
            }
            //save in table and download images
            List<String> dept; String productName;int count = 1;
            for (int i = 0; i < tabs.size(); i++) {
                try {
                    browser.switchTo().window(tabs.get(i));
                    if (browser.getCurrentUrl().contains("www.flipkart.com")) { //flipkart
                        //get dept and product name and save them in table
                        dept = siteDetailHelper.getFlipkartDepts(browser);
                        productName = siteDetailHelper.getFlipkartProductName(browser);
                        boolean isAvailableToBuy = siteDetailHelper.isAvailableToBuy(browser);
                        if (isAvailableToBuy) {
                            this.saveInPriceDropDetailTable(browser, dept, productName, batchEntities.get(i), "Flipkart");
                            //now download images
                            siteDetailHelper.downloadFlipkartImages(browser,count, dept.get(0));
                        }
                    } else { //amazon
                        //get dept and product name and save them in table
                        dept = siteDetailHelper.getAmazonDepts(browser);
                        productName = siteDetailHelper.getAmazonProductName(browser);
                        boolean isAvailableToBuy = siteDetailHelper.isAvailableToBuy(browser);
                        if (isAvailableToBuy) {
                            this.saveInPriceDropDetailTable(browser, dept, productName, batchEntities.get(i), "Amazon");
                            //now download images
                            siteDetailHelper.downloadAmazonImages(browser,count, dept.get(0));
                        }
                    }
                } catch (Exception e) {
                    log.info("Exception occurred. Exception is {} . So continuing with next tab", e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.info("Error occurred for the current url {} .Exception is {}", browser.getCurrentUrl(), ex.getMessage());
        } finally {
            browser.quit();
        }
    }

    private void saveInPriceDropDetailTable(WebDriver browser, List<String> depts, String productName,
                                            PriceHistoryGraph priceHistoryGraph, String site) {
        int deptCount = Math.min(depts.size(), 6);
        PriceDropDetail priceDropDetail = new PriceDropDetail();
        priceDropDetail.setPhUrl(priceHistoryGraph.getPhUrl());
        priceDropDetail.setSiteUrl(browser.getCurrentUrl());
        priceDropDetail.setProductName(productName);
        priceDropDetail.setSiteName(site);
        priceDropDetail.setCreatedDate(LocalDateTime.now());
        priceDropDetail.setIsPicked(true);
        if (deptCount == 6) {
            priceDropDetail.setMainDept(depts.get(deptCount-6));
            priceDropDetail.setSubDept1(depts.get(deptCount-5));
            priceDropDetail.setSubDept2(depts.get(deptCount-4));
            priceDropDetail.setSubDept3(depts.get(deptCount-3));
            priceDropDetail.setSubDept4(depts.get(deptCount-2));
            priceDropDetail.setSubDept5(depts.get(deptCount-1));
        } else if (deptCount == 5) {
            priceDropDetail.setMainDept(depts.get(deptCount-5));
            priceDropDetail.setSubDept1(depts.get(deptCount-4));
            priceDropDetail.setSubDept2(depts.get(deptCount-3));
            priceDropDetail.setSubDept3(depts.get(deptCount-2));
            priceDropDetail.setSubDept4(depts.get(deptCount-1));
        } else if (deptCount == 4) {
            priceDropDetail.setMainDept(depts.get(deptCount-4));
            priceDropDetail.setSubDept1(depts.get(deptCount-3));
            priceDropDetail.setSubDept2(depts.get(deptCount-2));
            priceDropDetail.setSubDept3(depts.get(deptCount-1));
        } else if (deptCount == 3) {
            priceDropDetail.setMainDept(depts.get(deptCount-3));
            priceDropDetail.setSubDept1(depts.get(deptCount-2));
            priceDropDetail.setSubDept2(depts.get(deptCount-1));
        } else if (deptCount == 2) {
            priceDropDetail.setMainDept(depts.get(deptCount-2));
            priceDropDetail.setSubDept1(depts.get(deptCount-1));
        } else if (deptCount == 1) {
            priceDropDetail.setMainDept(depts.get(deptCount-1));
        } else {
            log.info("No departments found for the url {}", browser.getCurrentUrl());
        }
        if (priceDropDetail.getMainDept() != null)
            priceDropDetailRepo.save(priceDropDetail);
    }
}
