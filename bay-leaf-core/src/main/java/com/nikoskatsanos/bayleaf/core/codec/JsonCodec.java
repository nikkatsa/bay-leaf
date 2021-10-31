package com.nikoskatsanos.bayleaf.core.codec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

public class JsonCodec implements BayLeafCodec {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.INDENT_OUTPUT, false);

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

    public static class Serializer implements BayLeafCodec.Serializer {

        @SneakyThrows
        @Override
        public <OUT> byte[] serialize(OUT msg, Class<OUT> outType) {
            return JSON_MAPPER.writeValueAsBytes(msg);
        }
    }

    @RequiredArgsConstructor
    public static class Deserializer implements BayLeafCodec.Deserializer {

        @SneakyThrows
        @Override
        public <IN> IN deserialize(final byte[] bytes, final Class<IN> inType){
            return JSON_MAPPER.readValue(bytes, inType);
        }
    }
}
