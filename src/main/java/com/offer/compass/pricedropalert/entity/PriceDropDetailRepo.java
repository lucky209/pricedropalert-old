package com.offer.compass.pricedropalert.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceDropDetailRepo extends JpaRepository<PriceDropDetail, String> {

    List<PriceDropDetail> findByIsPicked(boolean isPicked);
}
