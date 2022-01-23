package com.nikoskatsanos.bayleaf.server.messagingpattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Subscription<SUBSCRIPTION> {

    private String id;

    private SUBSCRIPTION subscription;
}
