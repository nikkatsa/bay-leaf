package com.nikoskatsanos.bayleaf.client.netty.props;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BayLeafClientProps {

    private static final String BAY_LEAF_CLIENT_RECONNECT_TIMEOUT_PROP_NAME = "bayleaf.client.recconnect.ms";
    public static final Long BAY_LEAF_CLIENT_RECONNECT_TIMEOUT_MS = Long.getLong(BAY_LEAF_CLIENT_RECONNECT_TIMEOUT_PROP_NAME, 5_000);
}
