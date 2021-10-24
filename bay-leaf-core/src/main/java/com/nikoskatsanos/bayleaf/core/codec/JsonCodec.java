package com.nikoskatsanos.bayleaf.core.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonCodec implements BayLeafCodec{

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final Serializer SERIALIZER = new Serializer();
    private static final Deserializer DESERIALIZER = new Deserializer();

    @Override
    public BayLeafCodec.Serializer serializer() {
        return SERIALIZER;
    }

    @Override
    public BayLeafCodec.Deserializer deserializer() {
        return DESERIALIZER;
    }

    public static class Serializer implements BayLeafCodec.Serializer{

        @SneakyThrows
        public <OUT> byte[] serialize(final OUT msg) {
            return JSON_MAPPER.writeValueAsBytes(msg);
        }
    }

    @RequiredArgsConstructor
    public static class Deserializer implements BayLeafCodec.Deserializer{

        @SneakyThrows
        public <IN> IN deserialize(final byte[] msg) {
            return (IN) JSON_MAPPER.readValue(msg, msg.getClass());
        }
    }
}
