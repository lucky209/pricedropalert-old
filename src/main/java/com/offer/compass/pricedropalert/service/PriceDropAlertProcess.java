package com.offer.compass.pricedropalert.service;

import com.offer.compass.pricedropalert.entity.PriceHistory;
import com.offer.compass.pricedropalert.entity.PriceHistoryGraph;
import com.offer.compass.pricedropalert.helper.PriceDropAlertProcessHelper;
import com.offer.compass.pricedropalert.helper.SiteDetailHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PriceDropAlertProcess extends Thread {

    private List<PriceHistoryGraph> batchEntities;
    private PriceDropAlertProcessHelper helper;

    PriceDropAlertProcess(List<PriceHistoryGraph> batchEntities, PriceDropAlertProcessHelper helper) {
        this.helper = helper;
        this.batchEntities = batchEntities;
    }


    @SneakyThrows
    @Override
    public void run() {
        log.info("::: " + Thread.currentThread().getName() + " is started...");
        try {
            helper.process(batchEntities);
        } catch (Exception ex) {
            log.info("Exception occurred. Exception is " + ex.getMessage());
        }
    }
}
