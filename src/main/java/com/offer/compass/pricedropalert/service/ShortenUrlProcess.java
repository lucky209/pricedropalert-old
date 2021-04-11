package com.offer.compass.pricedropalert.service;

import com.offer.compass.pricedropalert.entity.PriceDropDetail;
import com.offer.compass.pricedropalert.entity.SiteDetail;
import com.offer.compass.pricedropalert.helper.SiteDetailHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ShortenUrlProcess extends Thread {

    private List<PriceDropDetail> batchEntities;
    private SiteDetailHelper siteDetailHelper;

    ShortenUrlProcess(List<PriceDropDetail> batchEntities, SiteDetailHelper siteDetailHelper) {
        this.batchEntities = batchEntities;
        this.siteDetailHelper = siteDetailHelper;
    }

    @Override
    public void run() {
        log.info("::: " + Thread.currentThread().getName() + " is started...");
        try {
            //main url first
            siteDetailHelper.shortenUrlProcess(batchEntities, false);
            //cross site
            siteDetailHelper.shortenUrlProcess(batchEntities, true);
        } catch (Exception ex) {
            log.info("Exception occurred. Exception is " + ex.getMessage());
        }
    }
}
