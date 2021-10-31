package com.nikoskatsanos.bayleaf.core.codec;

/**
 * A codec is the notion of a {@link Serializer} and a {@link Deserializer} to be used for the transport of in/out messages from the BayLeaf framework
 *
 * <p>
 * A {@link com.nikoskatsanos.bayleaf.core.Connector} accepts a {@link Serializer} and a {@link Deserializer}, which are used for the marshalling/unmarshalling of all messages
 * to/from the various messaging patterns that the {@link com.nikoskatsanos.bayleaf.core.Connector} has
 * </p>
 */
public interface BayLeafCodec {

    /**
     * @return the serializer to be used for marshalling messages published by BayLeaf framework
     */
    Serializer serializer();

    /**
     * @return the deserializer to be used for unmarshalling messages received by BayLeaf framework
     */
    Deserializer deserializer();

    interface Serializer {

        /**
         * Serialize {@code msg} into a {@link byte[]}
         *
         * @param msg The message to serialize
         * @param outType The type of the message. This parameter may, or may not be used by the implementations. It is mainly there to keep the method signature symmetric with the
         * deserializer's one
         * @param <OUT> The type of the message that is been published
         * @return a {@link byte[]} representing the serialized {@code msg}
         */
        <OUT> byte[] serialize(final OUT msg, final Class<OUT> outType);
    }

    interface Deserializer {

        /**
         * Deserialize {@code bytes} into a POJO
         *
         * @param bytes that represent the marshalled message
         * @param inType type of message represented by the {@code bytes}. May, or may not, be used by the implementations
         * @param <IN> The type of the message that is been received
         * @return a POJO after unmarshalling the {@code bytes}
         */
        <IN> IN deserialize(final byte[] bytes, final Class<IN> inType);
    }
}
