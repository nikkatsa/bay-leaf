package com.nikoskatsanos.bayleaf.core.messagingpattern;

public interface BCContext<BROADCAST> {

    void broadcast(final Broadcast<BROADCAST> broadcast);
}
