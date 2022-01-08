package com.nikoskatsanos.bayleaf.client.netty.messagingpatterns;

public interface StreamCallback<SUBSCRIPTION, INITIAL_DATA, DATA> {

    void onInitialData(final INITIAL_DATA initial_data);

    void onData(final DATA data);
}
