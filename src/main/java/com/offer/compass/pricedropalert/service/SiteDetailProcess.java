package com.offer.compass.pricedropalert.service;

import com.offer.compass.pricedropalert.entity.CurrentDeal;
import com.offer.compass.pricedropalert.entity.PriceHistory;
import com.offer.compass.pricedropalert.helper.PriceHistoryHelper;
import com.offer.compass.pricedropalert.helper.SiteDetailHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SiteDetailProcess extends Thread {

    private List<PriceHistory> batchEntities;
    private SiteDetailHelper siteDetailHelper;

    SiteDetailProcess(List<PriceHistory> batchEntities, SiteDetailHelper siteDetailHelper) {
        this.siteDetailHelper = siteDetailHelper;
        this.batchEntities = batchEntities;
    }


    @SneakyThrows
    @Override
    public void run() {
        log.info("::: " + Thread.currentThread().getName() + " is started...");
        try {
            siteDetailHelper.process(batchEntities);
        } catch (Exception ex) {
            log.info("Exception occurred. Exception is " + ex.getMessage());
        }
    }
}
