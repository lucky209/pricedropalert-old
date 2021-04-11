package com.offer.compass.pricedropalert.helper;

import com.offer.compass.pricedropalert.constant.CanvaConstant;
import com.offer.compass.pricedropalert.constant.Constant;
import com.offer.compass.pricedropalert.entity.SiteDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CanvaHelper {

    @Autowired
    private BrowserHelper browserHelper;

    public void makeCanvaDesign(List<SiteDetail> canvaList) throws Exception {
        WebDriver browser = browserHelper.openBrowser(true, CanvaConstant.CANVA_WEB_URL);
        //login
        List<WebElement> loginElements = browser.findElements(By.className(CanvaConstant.LOGIN_ELEMENT_CLASS_NAME));
        loginElements.forEach(element -> {
            if (element.getText().contains(CanvaConstant.LOGIN_KEY)) {
                element.click();
            }
        });
        browser.findElement(By.tagName(Constant.TAG_INPUT)).sendKeys(CanvaConstant.LOGIN_EMAIL, Keys.TAB, CanvaConstant.LOGIN_PASSWORD, Keys.ENTER);
        Thread.sleep(3000);
        //click all your designs
        List<WebElement> allYourDesignElements = browser.findElements(By.className(CanvaConstant.ALL_DESIGN_ELEMENT_CLASS_NAME));
        allYourDesignElements.forEach(element -> {
            if (element.getText().contains(CanvaConstant.ALL_YOUR_DESIGN_KEY)) {
                element.click();
            }
        });
        //click the design
        List<WebElement> designs = browser.findElements(By.cssSelector(CanvaConstant.DESIGNS_ELEMENT_CSS_CLASS_NAME));
        if (!designs.isEmpty()) {
            designs.get(0).click();
            Thread.sleep(3000);
        } else {
            throw new Exception("Cannot find the design");
        }
        //switch to next tab
        List<String> tabs = new ArrayList<>(browser.getWindowHandles());
        browser.switchTo().window(tabs.get(1));
        //get all 25 templates
        List<WebElement> templateMainDivs = browser.findElements(By.className(CanvaConstant.TEMPLATE_MAIN_DIV_CLASS_NAME));
        if (!templateMainDivs.isEmpty()) {
            List<WebElement> templateDivs = browser.findElements(By.className(CanvaConstant.TEMPLATE_SUB_DIV_CLASS_NAME));
            for (int i = 0; i < canvaList.size(); i++) {
                try {
                    if (canvaList.get(i).getProductName() != null)
                        this.updateTemplate(browser, templateDivs.get(i), canvaList.get(i));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            log.info("Template is updated successfully...");
        } else {
            log.info("templateMainDiv not found....");
        }
    }

    private void updateTemplate(WebDriver browser, WebElement templateDivElement, SiteDetail siteDetail) throws InterruptedException {
        ((JavascriptExecutor) browser).executeScript("arguments[0].scrollIntoView(true);", templateDivElement);
        Thread.sleep(2000);
        List<WebElement> components = templateDivElement.findElements(By.className(CanvaConstant.SINGLE_TEMPLATE_CLASS_NAME));
        String productName = components.get(0).getText();
        String amazonPrice = components.get(1).getText();
        String flipkartPrice = components.get(2).getText();
        Actions actions = new Actions(browser);
        //update title
        this.updateText(components.get(0), actions, StringUtils.capitalize(siteDetail.getProductName()), productName);
        //update amazon price
        if (siteDetail.getSiteName().equalsIgnoreCase("amazon")) {
            this.updateText(components.get(1), actions, "₹" + this.convertToIndianNumberFormat(
                    siteDetail.getPrice()), amazonPrice);
        } else {
            if (siteDetail.getCrossSitePrice() != null)
                this.updateText(components.get(1), actions, "₹" + this.convertToIndianNumberFormat(
                        siteDetail.getCrossSitePrice()), amazonPrice);
            else
                this.updateText(components.get(1), actions, "₹N/A", amazonPrice);
        }
        //update flipkart price
        if (siteDetail.getSiteName().equalsIgnoreCase("flipkart")) {
            this.updateText(components.get(2), actions, "₹" + this.convertToIndianNumberFormat(
                    siteDetail.getPrice()), flipkartPrice);
        } else {
            if (siteDetail.getCrossSitePrice() != null)
                this.updateText(components.get(2), actions, "₹" + this.convertToIndianNumberFormat(
                        siteDetail.getCrossSitePrice()), flipkartPrice);
            else
                this.updateText(components.get(2), actions, "₹N/A", flipkartPrice);
        }
        Thread.sleep(2000);
    }

    private String convertToIndianNumberFormat(Integer currentPrice) {
        String strPrice = currentPrice.toString();
        if (strPrice.length() == 4) {
            strPrice = strPrice.substring(0,1) + "," + strPrice.substring(1);
        } else if (strPrice.length() == 5) {
            strPrice = strPrice.substring(0,2) + "," + strPrice.substring(2);
        }
        return strPrice;
    }

    private void updateText(WebElement element, Actions actions, String updateText, String currentText) {
        actions.moveToElement(element).build().perform();
        actions.doubleClick().build().perform();
        actions.sendKeys(Keys.END).build().perform();
        for (int i=0;i<currentText.length();i++) {
            actions.sendKeys(Keys.BACK_SPACE).build().perform();
        }
        actions.sendKeys(updateText).build().perform();
    }
}
