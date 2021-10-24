package com.nikoskatsanos.bayleaf.core.props;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BayLeafProps {

    private static final String BAY_LEAF_HEARTBEAT_PERIOD_MS_PROPERTY_NAME = "bay.leaf.heartbeat.period.ms";
    public static final int BAY_LEAF_HEARTBEAT_PERIOD_MS = Integer.parseInt(System.getProperty(BAY_LEAF_HEARTBEAT_PERIOD_MS_PROPERTY_NAME, "10000"));
}
