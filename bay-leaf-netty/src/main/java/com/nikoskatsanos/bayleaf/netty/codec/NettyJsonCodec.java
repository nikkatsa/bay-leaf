package com.nikoskatsanos.bayleaf.netty.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NettyJsonCodec {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    static {
        JSON_MAPPER.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        JSON_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static NettyJsonCodec instance() {
        return NettyJsonCodecHolder.CODEC;
    }

    public <T> String serializeToString(final T data) {
        try {
            return JSON_MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Failed to serialize Object=%s into JSON", data));
        }
    }

    public <T> T deserializeFromString(final String data, final Class<T> type) {
        try {
            return JSON_MAPPER.readValue(data, type);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Failed to deserialize Payload=%s, Type=%s", data, type.getName()), e);
        }
    }

    public <T> byte[] serializeToBytes(final T data) {
        try {
            return JSON_MAPPER.writeValueAsBytes(data);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Fail to serialize Object=%s into JSON", data));
        }
    }

    public <T> T deserializeFromBytes(final byte[] data, final Class<T> type) {
        try {
            return JSON_MAPPER.readValue(data, type);
        } catch (final IOException e) {
            throw new IllegalArgumentException(String.format("Failed to deserialize Payload=%s, Type=%s", Arrays.toString(data), type.getName()));
        }
    }

    private static final class NettyJsonCodecHolder {

        private final static NettyJsonCodec CODEC = new NettyJsonCodec();
    }
}
