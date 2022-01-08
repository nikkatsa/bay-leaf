package com.nikoskatsanos.bayleaf.client.netty;

import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.StreamCallback;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRAPromise;
import com.nikoskatsanos.bayleaf.client.netty.messagingpatterns.RRPromise;
import com.nikoskatsanos.bayleaf.core.Heartbeat;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.core.message.ErrorMessage;
import java.util.function.Consumer;

public interface BayLeafService extends AutoCloseable {

    /**
     * Start the service by connecting to the remote server's endpoint
     */
    void start();

    /**
     * Handle the incoming heartbeat. It is the implementation's responsibility to respond to the heartbeat
     *
     * @param heartbeatIn incoming heartbeat along with its info that can be used for the outgoing matching heartbeat
     */
    void onHeartbeat(final Heartbeat heartbeatIn);

    /**
     * @return serializer corresponding to this service/endpoint
     */
    BayLeafCodec.Serializer serializer();

    /**
     * @return deserializer corresponding to this service/endpoint
     */
    BayLeafCodec.Deserializer deserializer();

    /**
     * @param route the remote route this service's RR endpoint is advertised as
     * @param requestType the endpoint accepts
     * @param responseType the endpoint responds
     * @param <REQUEST> type of the request
     * @param <RESPONSE> type of the response
     * @return a RequestResponse endpoint
     */
    <REQUEST, RESPONSE> RR<REQUEST, RESPONSE> createRR(final String route, final Class<REQUEST> requestType, final Class<RESPONSE> responseType);

    /**
     * @param route the remote route this service's RRA endpoint is advertised as
     * @param requestType the endpoint accepts
     * @param responseType the endpoint responds
     * @param <REQUEST> type of the request
     * @param <RESPONSE> type of the response
     * @return a RequestResponseAck endpoint
     */
    <REQUEST, RESPONSE> RRA<REQUEST, RESPONSE> createRRA(final String route, final Class<REQUEST> requestType, final Class<RESPONSE> responseType);

    /**
     * @param route the remote route this service's PS endpoint is advertised as
     * @param subscriptionType the endpoint accepts
     * @param initialDataType the endpoint might respond once (depending on the endpoint's implementation)
     * @param dataType the endpoint streams
     * @param <SUBSCRIPTION> type of the subscription
     * @param <INITIAL_DATA> type of the initial data
     * @param <DATA> type of the data
     * @return a PrivateStream endpoint
     */
    <SUBSCRIPTION, INITIAL_DATA, DATA> PS<SUBSCRIPTION, INITIAL_DATA, DATA> createPS(final String route, final Class<SUBSCRIPTION> subscriptionType,
        final Class<INITIAL_DATA> initialDataType, final Class<DATA> dataType);

    /**
     * @param route the remote route this service's SS endpoint is advertised as
     * @param subscriptionType the endpoint accepts
     * @param initialDataType the endpoint might respond once (depending on the endpoint's implementation)
     * @param dataType the endpoint streams
     * @param <SUBSCRIPTION> type of the subscription
     * @param <INITIAL_DATA> type of the initial data
     * @param <DATA> type of the data
     * @return a SharedStream endpoint
     */
    <SUBSCRIPTION, INITIAL_DATA, DATA> SS<SUBSCRIPTION, INITIAL_DATA, DATA> createSS(final String route, final Class<SUBSCRIPTION> subscriptionType,
        final Class<INITIAL_DATA> initialDataType, final Class<DATA> dataType);

    /**
     * @param route the remote route this service's BC endpoint is advertised as
     * @param broadcastType the endpoint broadcasts
     * @param <BROADCAST> type of the broadcast data
     * @return a Broadcast endpoint
     */
    <BROADCAST> BC<BROADCAST> createBC(final String route, final Class<BROADCAST> broadcastType);

    interface RR<REQUEST, RESPONSE> {

        void request(final REQUEST request, final RRPromise<REQUEST, RESPONSE> promise);

        void response(final String correlationId, final byte[] responseBytes);

        void error(final ErrorMessage errorMessage);
    }

    interface RRA<REQUEST, RESPONSE> {

        void request(final REQUEST request, final RRAPromise<REQUEST, RESPONSE> promise);

        void response(final String correlationId, final byte[] responseBytes);

        void error(final ErrorMessage errorMessage);
    }

    interface PS<SUBSCRIPTION, INITIAL_DATA, DATA> {

        void subscribe(final SUBSCRIPTION subscription, final StreamCallback<SUBSCRIPTION, INITIAL_DATA, DATA> psCallback);

        void initialData(final String correlationId, final byte[] initialData);

        void data(final String correlationId, final byte[] data);

        void close(final SUBSCRIPTION subscription);
    }

    interface SS<SUBSCRIPTION, INITIAL_DATA, DATA> {

        void subscribe(final SUBSCRIPTION subscription, final StreamCallback<SUBSCRIPTION, INITIAL_DATA, DATA> ssCallback);

        void initialData(final String correlationId, final byte[] initialData);

        void data(final String correlationId, final byte[] data);

        void close(final SUBSCRIPTION subscription);
    }

    interface BC<BROADCAST> {

        void joinBroadcast(final Consumer<BROADCAST> onBroadcastMessage);

        void onBroadcast(final String correlationId, final byte[] broadcastBytes);
    }
}
