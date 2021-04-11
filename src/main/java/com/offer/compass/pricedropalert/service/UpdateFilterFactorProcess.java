package com.offer.compass.pricedropalert.service;

import com.offer.compass.pricedropalert.entity.SiteDetail;
import com.offer.compass.pricedropalert.helper.SiteDetailHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class UpdateFilterFactorProcess extends Thread {

    private List<SiteDetail> batchEntities;
    private SiteDetailHelper siteDetailHelper;

    UpdateFilterFactorProcess(List<SiteDetail> batchEntities, SiteDetailHelper siteDetailHelper) {
        this.siteDetailHelper = siteDetailHelper;
        this.batchEntities = batchEntities;
    }

    @Override
    public void run() {
        log.info("::: " + Thread.currentThread().getName() + " is started...");
        try {
            siteDetailHelper.updateFilterFactorProcess(batchEntities);
        } catch (Exception ex) {
            log.info("Exception occurred. Exception is " + ex.getMessage());
        }
    }
}
