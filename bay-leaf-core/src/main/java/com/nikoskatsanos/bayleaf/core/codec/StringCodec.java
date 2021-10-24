package com.nikoskatsanos.bayleaf.core.codec;

public class StringCodec implements BayLeafCodec {

    private static final StringCodec.Serializer SERIALIZER = new StringCodec.Serializer();
    private static final StringCodec.Deserializer DESERIALIZER = new StringCodec.Deserializer();

    @Override
    public Serializer serializer() {
        return SERIALIZER;
    }

    @Override
    public Deserializer deserializer() {
        return DESERIALIZER;
    }

    static class Serializer implements BayLeafCodec.Serializer {

        @Override
        public <OUT> byte[] serialize(final OUT msg) {
            return ((String)msg).getBytes();
        }
    }

    static class Deserializer implements BayLeafCodec.Deserializer {

        @Override
        public String deserialize(final byte[] bytes) {
            return new String(bytes);
        }
    }
}
