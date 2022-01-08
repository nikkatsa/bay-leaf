package com.nikoskatsanos.bayleaf.client.netty.messagingpatterns;

import lombok.Data;

@Data
public class RequestAndPromiseRecord<REQUEST, RESPONSE> {

    private final REQUEST request;
    private final RRPromise<REQUEST, RESPONSE> promise;
}
