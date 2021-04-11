package com.offer.compass.pricedropalert.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepo extends JpaRepository<Property,Integer> {
    Property findByPropName(String name);
}
