package com.offer.compass.pricedropalert.helper;

import com.offer.compass.pricedropalert.entity.CurrentDeal;
import com.offer.compass.pricedropalert.entity.CurrentDealRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Slf4j
public class CurrentDealHelper {

    @Autowired
    private CurrentDealRepo currentDealRepo;

    public void saveInCurrentDealTable(String productName, String url, int price, String priceHistoryUrl) {
        CurrentDeal currentDeal = new CurrentDeal();
        currentDeal.setUrl(url);
        currentDeal.setProductName(productName);
        currentDeal.setPrice(price);
        currentDeal.setPriceHistoryLink(priceHistoryUrl);
        currentDeal.setCreatedDate(LocalDateTime.now());
        currentDeal.setIsPicked(false);
        currentDealRepo.save(currentDeal);
    }

    public void cleanupCurrentDealTable() {
        LocalDate seventhDayFromToday = LocalDate.now().minusDays(7);
        int count = currentDealRepo.getRecordsCountByCreatedDate(seventhDayFromToday);
        if (count != 0) {
            currentDealRepo.deleteRecordsByCreatedDate(seventhDayFromToday);
            log.info("Deleted {} record(s)...", count);
        }
        else
            log.info("Deleted 0 records...");
    }
}
