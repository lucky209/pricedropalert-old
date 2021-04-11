package com.offer.compass.pricedropalert.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
public class SiteDetail {
    @Id
    private String phUrl;
    private String siteUrl;
    private String siteName;
    private String productName;
    private int price;
    private String ratingStar;
    private String mainDept;
    private String subDept1;
    private String subDept2;
    private String subDept3;
    private String subDept4;
    private String subDept5;
    private Boolean isPicked;
    private LocalDateTime createdDate;
    private Integer filterFactor;
    private Integer crossSitePrice;
    private String crossSiteUrl;
    private String amazonShortUrl;
    private String flipkartShortUrl;
    private String designName;
    private Integer productNo;
}
