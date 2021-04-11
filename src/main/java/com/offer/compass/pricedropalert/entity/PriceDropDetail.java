package com.offer.compass.pricedropalert.entity;

import com.offer.compass.pricedropalert.model.VoiceTextDetails;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
public class PriceDropDetail {
    @Id
    private String phUrl;
    private String siteUrl;
    private String siteName;
    private String productName;
    private String mainDept;
    private String subDept1;
    private String subDept2;
    private String subDept3;
    private String subDept4;
    private String subDept5;
    private Boolean isPicked;
    private LocalDateTime createdDate;
    private Integer crossSitePrice;
    private String crossSiteUrl;
    private String amazonShortUrl;
    private String flipkartShortUrl;
    private String designName;
    private Integer productNo;
}
