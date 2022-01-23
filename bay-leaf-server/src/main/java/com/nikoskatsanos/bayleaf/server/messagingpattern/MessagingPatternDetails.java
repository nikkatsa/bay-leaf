package com.nikoskatsanos.bayleaf.server.messagingpattern;

import java.lang.reflect.Method;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class MessagingPatternDetails {

    private final String endpointName;
    private final Method method;
    private final Class<?> inType;
    private final Class<?> outType;
    private final Class<?> snapshotType;

    public MessagingPatternDetails(String endpointName, Method method, Class<?> inType, Class<?> outType) {
        this(endpointName, method, inType, outType, null);
    }
}
