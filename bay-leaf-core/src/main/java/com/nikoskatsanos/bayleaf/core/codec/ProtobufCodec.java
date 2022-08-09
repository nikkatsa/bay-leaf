package com.nikoskatsanos.bayleaf.core.codec;

import com.google.protobuf.MessageLite;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Proto3 {@link BayLeafCodec}
 */
public class ProtobufCodec implements BayLeafCodec {

    private static final ProtobufSerializer SERIALIZER = new ProtobufSerializer();
    private static final ProtobufDeserializer DESERIALIZER = new ProtobufDeserializer();

    @Override
    public Serializer serializer() {
        return SERIALIZER;
    }

    @Override
    public Deserializer deserializer() {
        return DESERIALIZER;
    }

    public static final class ProtobufSerializer implements Serializer {

        @Override
        public <OUT> byte[] serialize(final OUT msg, final Class<OUT> outType) {
            if (!MessageLite.class.isAssignableFrom(outType)) {
                throw new IllegalArgumentException(outType + " is not a .proto type");
            }
            return ((MessageLite) msg).toByteArray();
        }
    }

    public static final class ProtobufDeserializer implements Deserializer {

        private static final Map<Class<? extends MessageLite>, Function<byte[], ?>> MEMOIZED_PROTO_TYPES = new ConcurrentHashMap<>();

        @Override
        public <IN> IN deserialize(final byte[] bytes, final Class<IN> inType) {
            return (IN) memoize(inType).apply(bytes);
        }

        private static Function<byte[], ?> memoize(final Class<?> protoType) {
            if (!MessageLite.class.isAssignableFrom(protoType)) {
                throw new IllegalArgumentException(protoType + " is not a .proto type");
            }
            return MEMOIZED_PROTO_TYPES.computeIfAbsent((Class<? extends MessageLite>) protoType, clazz -> {
                try {
                    final Method parseFrom = Class.forName(protoType.getName()).getDeclaredMethod("parseFrom", byte[].class);
                    final Function<byte[], ?> func = bytes -> {
                        try {
                            return parseFrom.invoke(null, bytes);
                        } catch (final IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalArgumentException("Could not call " + clazz + "#parseFrom to translate bytes into a .proto POJO", e);
                        }
                    };
                    return func;
                } catch (NoSuchMethodException | ClassNotFoundException e) {
                    throw new IllegalArgumentException("Cannot decode", e);
                }
            });
        }
    }
}
