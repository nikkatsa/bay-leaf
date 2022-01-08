package com.nikoskatsanos.bayleaf.client.netty.messagingpatterns;

import lombok.Data;

@Data
public class SubscriptionAndStreamCallbackRecord<SUBSCRIPTION, INITIAL_DATA, DATA> {

    private final SUBSCRIPTION subscription;
    private final StreamCallback<SUBSCRIPTION, INITIAL_DATA, DATA> streamCallback;
}
