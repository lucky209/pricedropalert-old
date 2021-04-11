package com.offer.compass.pricedropalert.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VoiceTextDetails {
    private Integer productNo;
    private String productName;
    private String siteName;
    private String url;
    private String phUrl;
    private Integer lowestPrice;
    private Integer highestPrice;
    private Integer pricedropToPrice;
    private Integer pricedropFromPrice;
    private LocalDate pricedropFromDate;
    private String dropChances;
    private String ratingStar;
}
