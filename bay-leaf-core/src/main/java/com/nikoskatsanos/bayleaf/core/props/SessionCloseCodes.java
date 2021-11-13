package com.nikoskatsanos.bayleaf.core.props;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionCloseCodes {

    public static final byte CONNECTOR_CLOSE = 0x0000;
    public static final byte INVALID_HEARTBEAT = 0x0001;
    public static final byte SERVICE_UNAVAILABLE = 0x0002;
}
