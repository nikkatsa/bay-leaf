package com.nikoskatsanos.bayleaf.server.messagingpattern;

import com.nikoskatsanos.bayleaf.domain.Session;
import java.util.function.Consumer;

/**
 * Represents a private stream context, for a particular {@link Session} on a particular endpoint
 *
 * @param <SUBSCRIPTION> subscription type
 * @param <INITIAL_DATA> Published initial(snapshot) data type
 * @param <DATA> Published data type
 */
public interface PSContext<SUBSCRIPTION, INITIAL_DATA, DATA> {

    /**
     * @return The session the context refers to
     */
    Session session();

    /**
     * @param subscriptionConsumer to be notified when a {@link Subscription} arrives
     */
    void onSubscription(final Consumer<Subscription<SUBSCRIPTION>> subscriptionConsumer);

    /**
     * @param closeConsumer to be notified when the {@link Subscription} is closed
     */
    void onClose(final Consumer<Subscription<SUBSCRIPTION>> closeConsumer);

    /**
     * @param snapshot of data to be send to the remote end
     */
    void initialData(final SubscriptionData<SUBSCRIPTION, INITIAL_DATA> snapshot);

    /**
     * @param subscriptionData to be send to the remote end
     */
    void data(final SubscriptionData<SUBSCRIPTION, DATA> subscriptionData);
}
