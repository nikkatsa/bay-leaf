package com.nikoskatsanos.bayleaf.core.messagingpattern;

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
}
