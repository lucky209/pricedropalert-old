package com.offer.compass.pricedropalert.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CurrentDealRepo extends JpaRepository<CurrentDeal, String> {

    CurrentDeal findByProductName(String productName);

    @Query(value = "delete from pricedropservice.current_deal where created_date <= ?1", nativeQuery = true)
    void deleteRecordsByCreatedDate(LocalDate date);

    @Query(value = "select count(url) from pricedropservice.current_deal where created_date <= ?1", nativeQuery = true)
    int getRecordsCountByCreatedDate(LocalDate date);

    @Query(value = "SELECT * FROM pricedropservice.current_deal cd ORDER BY cd.created_date desc LIMIT ?1", nativeQuery = true)
    List<CurrentDeal> fetchLastAttemptCurrentDeals(int lastAttemptDealsCount);
}
