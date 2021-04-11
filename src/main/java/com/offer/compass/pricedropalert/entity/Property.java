package com.offer.compass.pricedropalert.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
public class Property {
    @Id
    private int id;
    private String propName;
    private String propValue;
    private boolean enabled;
    private LocalDateTime createdDate;
}
