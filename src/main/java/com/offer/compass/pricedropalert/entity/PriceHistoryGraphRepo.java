package com.offer.compass.pricedropalert.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PriceHistoryGraphRepo extends JpaRepository<PriceHistoryGraph, String> {

    List<PriceHistory> findByIsPicked(boolean isPicked);

    @Query(value = "select * from pricedropservice.price_history_graph" +
            "where filter_factor > ?1", nativeQuery = true)
    List<SiteDetail> findByFilterFactor(int filterFactor);
}
