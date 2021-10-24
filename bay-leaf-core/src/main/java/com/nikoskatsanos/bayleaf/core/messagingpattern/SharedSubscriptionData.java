package com.nikoskatsanos.bayleaf.core.messagingpattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedSubscriptionData<SUBSCRIPTION, DATA> {

    private SharedSubscription<SUBSCRIPTION> subscription;
    private DATA data;
}
