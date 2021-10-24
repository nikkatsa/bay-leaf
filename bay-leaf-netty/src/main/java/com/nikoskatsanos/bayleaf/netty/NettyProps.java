package com.nikoskatsanos.bayleaf.netty;

import io.netty.handler.logging.LogLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NettyProps {

    public static final String NETTY_SESSION_ID = "BAY_LEAF_SESSION";

    private static final String SESSION_INITIALIZED_TIMEOUT_MS_PROP_KEY = "bay.leaf.session.initialized.timeout";
    public static final long SESSION_INITIALIZED_TIMEOUT_MS = Long.parseLong(System.getProperty(SESSION_INITIALIZED_TIMEOUT_MS_PROP_KEY, "5000"));

    private static final String BAY_LEAF_NETTY_LOG_LEVEL_PROP_KEY = "bay.leaf.netty.log.level";
    public static final LogLevel BAY_LEAF_NETTY_LOG_LEVEL = LogLevel.valueOf(System.getProperty(BAY_LEAF_NETTY_LOG_LEVEL_PROP_KEY, "INFO"));
}
