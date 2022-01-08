package com.nikoskatsanos.bayleaf.client.netty.messagingpatterns;

import com.nikoskatsanos.bayleaf.core.message.ErrorMessage;

public interface RRAPromise<REQUEST, RESPONSE> {

    void onResponse(final REQUEST request, final RESPONSE response, final Ack ack);

    void onError(final ErrorMessage errorMessage);

    @FunctionalInterface
    interface Ack {

        void ack();
    }
}
