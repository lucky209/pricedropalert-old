package com.offer.compass.pricedropalert.service;

import com.offer.compass.pricedropalert.entity.CurrentDeal;
import com.offer.compass.pricedropalert.helper.PriceHistoryHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PriceHistoryProcess extends Thread {

    private List<CurrentDeal> batchEntities;
    private PriceHistoryHelper priceHistoryHelper;

    PriceHistoryProcess(List<CurrentDeal> batchEntities, PriceHistoryHelper priceHistoryHelper) {
        this.batchEntities = batchEntities;
        this.priceHistoryHelper = priceHistoryHelper;
    }

    @SneakyThrows
    @Override
    public void run() {
        log.info("::: " + Thread.currentThread().getName() + " is started...");
        priceHistoryHelper.process(batchEntities);
    }
}
