package com.nikoskatsanos.bayleaf.netty.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketDataResponse {

    private String symbol;
    private double bidPrice;
    private double askPrice;
}
