package com.nikoskatsanos.bayleaf.netty.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TradeRequest {

    private String id;
    private String symbol;
    private double quantity;
    private double price;
    private Side side;

    public static enum Side {
        BID, ASK
    }
}
