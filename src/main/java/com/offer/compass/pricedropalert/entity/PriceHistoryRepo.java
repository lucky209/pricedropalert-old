package com.offer.compass.pricedropalert.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepo extends JpaRepository<PriceHistory, String> {

    List<PriceHistory> findByIsPicked(boolean isPicked);
}
