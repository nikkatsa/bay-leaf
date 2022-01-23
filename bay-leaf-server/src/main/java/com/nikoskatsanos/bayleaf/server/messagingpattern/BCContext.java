package com.nikoskatsanos.bayleaf.server.messagingpattern;

public interface BCContext<BROADCAST> {

    void broadcast(final Broadcast<BROADCAST> broadcast);
}
