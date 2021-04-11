package com.offer.compass.pricedropalert.controller;

import com.offer.compass.pricedropalert.service.PriceDropService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class PriceDropController {

    @Autowired
    private PriceDropService priceDropService;

    @GetMapping("/price-drop/get-deals")
    public boolean getDeals() throws Exception {
        log.info("Request received to fetch price drop deals");
        priceDropService.getDeals();
        return true;
    }

    @GetMapping("/price-drop/get-site-details")
    public boolean getSiteDetails() throws Exception {
        log.info("Request received to fetch site details");
        priceDropService.getSiteDetails();
        return true;
    }

    @GetMapping("/price-drop/get-pricedrop-details")
    public boolean getPriceHistoryDropDetails() throws Exception {
        log.info("Request received to fetch PriceDrop details");
        priceDropService.getPriceDropDetails();
        return true;
    }

    @GetMapping("/price-drop/get-price-history-details")
    public boolean getPriceHistoryDetails() throws Exception {
        log.info("Request received to fetch PriceHistory Details");
        priceDropService.getPriceHistoryDetails();
        return true;
    }

    @GetMapping("/price-drop/get-price-history-graph-details")
    public boolean getPriceHistoryGraphDetails() throws Exception {
        log.info("Request received to fetch PriceHistory Graph Details");
        priceDropService.getPriceHistoryGraphDetails();
        return true;
    }

    @GetMapping("/price-drop/update-filter-factor")
    public boolean updateFilterFactor() throws Exception {
        log.info("Request received to update filter factor");
        priceDropService.updateFilterFactor();
        return true;
    }

    @GetMapping("/price-drop/shorten-url")
    public boolean shortenUrl() throws Exception {
        log.info("Request received to shorten the url");
        priceDropService.shortenUrl();
        return true;
    }

    @PostMapping("/price-drop/download-images")
    public boolean downloadImages(@RequestBody String dept) throws Exception {
        log.info("Request received to shorten the url");
        priceDropService.downloadImages(dept);
        return true;
    }

    @GetMapping("/price-drop/canva-design")
    public boolean makeCanvaDesign() throws Exception {
        log.info("Request received to make canva design");
        priceDropService.makeCanvaDesign();
        return true;
    }

    @GetMapping("/price-drop/get-text-details")
    public boolean getTextDetails(@RequestBody String dept) throws Exception {
        log.info("Request received get Text Details");
        priceDropService.getTextDetails(dept);
        return true;
    }

    @GetMapping("/test")
    public boolean test(@RequestBody String url) throws InterruptedException {
        priceDropService.test(url);
        return true;
    }
}
