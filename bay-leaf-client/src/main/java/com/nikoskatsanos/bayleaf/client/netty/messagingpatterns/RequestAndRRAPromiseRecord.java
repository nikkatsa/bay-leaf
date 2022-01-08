package com.nikoskatsanos.bayleaf.client.netty.messagingpatterns;

import lombok.Data;

@Data
public class RequestAndRRAPromiseRecord<REQUEST, RESPONSE> {

    private final REQUEST request;
    private final RRAPromise<REQUEST, RESPONSE> promise;
}
