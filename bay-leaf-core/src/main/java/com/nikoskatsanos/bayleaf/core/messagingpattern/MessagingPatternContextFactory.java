package com.nikoskatsanos.bayleaf.core.messagingpattern;

public interface MessagingPatternContextFactory {

    RRContext createRR(final String rrEndpointName);

    RRAContext createRRA(final String rraEndpointName);

    PSContext createPS(final String psEndpointName);

    SSContext createSS(final String ssEndpointName);

    BCContext createBroadcast(final String broadcastEndpointName);
}
