package com.offer.compass.pricedropalert.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SiteDetailRepo extends JpaRepository<SiteDetail, String> {

    List<SiteDetail> findByIsPicked(boolean isPicked);

    @Query(value = "select * from pricedropservice.site_detail" +
            "where filter_factor > ?1", nativeQuery = true)
    List<SiteDetail> findByFilterFactor(int filterFactor);
}
