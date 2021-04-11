package com.offer.compass.pricedropalert.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class PriceHistoryGraph {

    @Id
    private String phUrl;
    private String phSiteUrl;
    private Integer pricedropFromPrice;
    private LocalDate pricedropFromDate;
    private Integer pricedropToPrice;
    private LocalDate pricedropToDate;
    private Integer lowestPrice;
    private Integer highestPrice;
    private String productName;
    private String ratingStar;
    private String dropChances;
    private Boolean isPicked;
    private LocalDateTime createdDate;
    private Integer filterFactor;
}
