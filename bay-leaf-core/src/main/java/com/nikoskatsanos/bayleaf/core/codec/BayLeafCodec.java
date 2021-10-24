package com.nikoskatsanos.bayleaf.core.codec;

public interface BayLeafCodec {

    Serializer serializer();

    Deserializer deserializer();

    interface Serializer {

        <OUT> byte[] serialize(final OUT msg);
    }

    interface Deserializer {

        <IN> IN deserialize(final byte[] bytes);
    }
}
