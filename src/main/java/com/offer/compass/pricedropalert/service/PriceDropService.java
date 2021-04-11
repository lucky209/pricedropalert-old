package com.offer.compass.pricedropalert.service;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public interface PriceDropService {
    void getDeals() throws Exception;

    void getSiteDetails() throws Exception;

    void getPriceHistoryDetails() throws InterruptedException;

    void updateFilterFactor() throws InterruptedException;

    void shortenUrl() throws InterruptedException;

    void downloadImages(String dept) throws InterruptedException;

    void makeCanvaDesign() throws Exception;

    void getTextDetails(String dept) throws FileNotFoundException, UnsupportedEncodingException;

    void test(String url) throws InterruptedException;

    void getPriceHistoryGraphDetails() throws InterruptedException;

    void getPriceDropDetails() throws InterruptedException;
}
