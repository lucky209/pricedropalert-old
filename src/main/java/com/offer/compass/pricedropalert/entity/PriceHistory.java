package com.offer.compass.pricedropalert.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
public class PriceHistory {

    @Id
    private String phUrl;
    private String phSiteUrl;
    private Integer currentPrice;
    private Integer lowestPrice;
    private Integer highestPrice;
    private String productName;
    private String ratingStar;
    private String dropChances;
    private Boolean isPicked;
    private LocalDateTime createdDate;
}
