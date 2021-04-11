package com.offer.compass.pricedropalert.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Slf4j
@Entity
@Data
public class CurrentDeal {
    @Id
    private String url;
    private String productName;
    private Integer price;
    private String priceHistoryLink;
    private LocalDateTime createdDate;
    private Boolean isPicked;
}
