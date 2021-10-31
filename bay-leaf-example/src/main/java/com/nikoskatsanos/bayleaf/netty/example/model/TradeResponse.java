package com.nikoskatsanos.bayleaf.netty.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    private String id;
    private boolean isDone;
    private String rejectionMsg;
}
