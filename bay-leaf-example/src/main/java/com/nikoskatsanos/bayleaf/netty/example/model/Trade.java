package com.nikoskatsanos.bayleaf.netty.example.model;

import com.nikoskatsanos.bayleaf.netty.example.model.TradeRequest.Side;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class Trade {

    private final long timestamp;
    private final String id;
    private final String symbol;
    private final double quantity;
    private final double price;
    private final Side side;
}
