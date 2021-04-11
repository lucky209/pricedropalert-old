package com.offer.compass.pricedropalert.service;

import com.offer.compass.pricedropalert.entity.SiteDetail;
import com.offer.compass.pricedropalert.helper.SiteDetailHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class downloadImagesProcess extends Thread {

    private List<SiteDetail> batchEntities;
    private SiteDetailHelper siteDetailHelper;
    private String dept;
    private int imgCount;

    downloadImagesProcess(List<SiteDetail> batchEntities, SiteDetailHelper siteDetailHelper, String dept, int imgCount) {
        this.batchEntities = batchEntities;
        this.siteDetailHelper = siteDetailHelper;
        this.dept = dept;
        this.imgCount = imgCount;
    }

    @Override
    public void run() {
        log.info("::: " + Thread.currentThread().getName() + " is started...");
        try {
            //main url first
            siteDetailHelper.downloadImagesProcess(batchEntities, dept, imgCount);
        } catch (Exception ex) {
            log.info("Exception occurred. Exception is " + ex.getMessage());
        }
    }
}
