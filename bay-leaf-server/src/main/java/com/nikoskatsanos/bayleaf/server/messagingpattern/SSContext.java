package com.nikoskatsanos.bayleaf.server.messagingpattern;

import com.nikoskatsanos.bayleaf.core.Session;
import java.util.function.Consumer;

/**
 * Represents a shared stream context, for a particular {@link Session} on a particular endpoint
 *
 * @param <SUBSCRIPTION> Shared subscription type
 * @param <INITIAL_DATA> Published initial(snapshot) data type
 * @param <DATA> Published data type
 */
public interface SSContext<SUBSCRIPTION, INITIAL_DATA, DATA> {

    /**
     * @return The session the context refers to
     */
    Session session();

    /**
     * @param subscriptionConsumer The callback to be called for all received {@link SharedSubscription}s, from this {@link Session}
     */
    void onSubscription(final Consumer<SharedSubscription<SUBSCRIPTION>> subscriptionConsumer);

    /**
     * @param closeConsumer The callback to be called when a {@link SharedSubscription} is closed from the user side. The close action represents a single {@link
     * SharedSubscription} and does not refer to the {@link SSContext} as a whole
     */
    void onSubscriptionClose(final Consumer<SharedSubscription<SUBSCRIPTION>> closeConsumer);

    /**
     * @param snapshot data to be send to the remote end. This {@link SharedSubscriptionData} will only be send to the corresponding {@link Session} and not to all subscribers
     */
    void snapshot(final SharedSubscriptionData<SUBSCRIPTION, INITIAL_DATA> snapshot);

    /**
     * @param subscription A subscription to a shared stream
     * @return The shared stream for this {@code subscription}. The shared stream can be utilized to send messages to all subscribers
     */
    StreamContext<SUBSCRIPTION, DATA> streamContext(SUBSCRIPTION subscription);

    interface StreamContext<SUBSCRIPTION, DATA> {

        /**
         * @return The subscription this {@link StreamContext} corresponds to
         */
        SUBSCRIPTION subscription();

        /**
         * @param closeConsumer to be called when this {@link StreamContext} is closed/destroyed.
         */
        void onClose(final Consumer<Void> closeConsumer);

        /**
         * @param stream data to be send to all subscribers on a particular {@code SUBSCRIPTION}
         */
        void stream(final SharedSubscriptionData<SUBSCRIPTION, DATA> stream);
    }
}
