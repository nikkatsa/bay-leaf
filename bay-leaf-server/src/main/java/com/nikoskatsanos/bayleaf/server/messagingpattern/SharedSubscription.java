package com.nikoskatsanos.bayleaf.server.messagingpattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedSubscription<SUBSCRIPTION> {

    private String subscriptionId;

    private SUBSCRIPTION subscription;
}
