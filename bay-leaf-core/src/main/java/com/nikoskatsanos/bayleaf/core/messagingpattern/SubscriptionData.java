package com.nikoskatsanos.bayleaf.core.messagingpattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionData<SUBSCRIPTION, DATA> {

    private Subscription<SUBSCRIPTION> subscription;

    private DATA data;
}
