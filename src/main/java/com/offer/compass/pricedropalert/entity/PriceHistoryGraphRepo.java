package com.offer.compass.pricedropalert.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryGraphRepo extends JpaRepository<PriceHistoryGraph, String> {

    List<PriceHistoryGraph> findByIsPicked(boolean isPicked);

    PriceHistoryGraph findByPhUrl(String url);
}
