package com.nikoskatsanos.bayleaf.netty.example.model;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeBlotterResponse {

    private List<Trade> trades = Collections.emptyList();
}
