package com.nikoskatsanos.bayleaf.client.netty.messagingpatterns;

import com.nikoskatsanos.bayleaf.core.message.ErrorMessage;

public interface RRPromise<REQUEST, RESPONSE> {

    void onResponse(final REQUEST request, final RESPONSE response);

    void onError(final ErrorMessage errorMsg);
}
