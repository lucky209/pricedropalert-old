package com.offer.compass.pricedropalert.service;

import com.offer.compass.pricedropalert.entity.CurrentDeal;
import com.offer.compass.pricedropalert.helper.PriceHistoryHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PriceHistoryGraphProcess extends Thread {

    private List<CurrentDeal> batchEntities;
    private PriceHistoryHelper priceHistoryHelper;

    PriceHistoryGraphProcess(List<CurrentDeal> batchEntities, PriceHistoryHelper priceHistoryHelper) {
        this.batchEntities = batchEntities;
        this.priceHistoryHelper = priceHistoryHelper;
    }

    @Override
    public void run() {
        log.info("::: " + Thread.currentThread().getName() + " is started...");
        priceHistoryHelper.graphProcess(batchEntities);
    }
}
