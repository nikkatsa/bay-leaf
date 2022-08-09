package com.nikoskatsanos.bayleaf.core.codec;

import static org.junit.jupiter.api.Assertions.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

public class JsonCodecTest {

    private final JsonCodec codec = new JsonCodec();

    @Test
    public void testEncodingDecoding() {
        // given an object
        final Demo out = new Demo("This is a message", 1);
        // when serializing it
        final byte[] outBytes = codec.serializer().serialize(out, Demo.class);
        // and deserializing the serialized bytes
        final Demo deserialized = this.codec.deserializer().deserialize(outBytes, Demo.class);
        // then assert objects same
        assertEquals(out, deserialized);
    }

    @Test
    public void testDecoding_unknownField() {
        // given a JSON message, with an extra unknown field
        final byte[] outBytes = """
            { "fieldI": "This is a message", "fieldII": 1, "fieldIII": "UNKNOWN" }
            """.getBytes();
        // when deserializing
        final Demo deserialized = this.codec.deserializer().deserialize(outBytes, Demo.class);
        // then expect correct message
        assertEquals(new Demo("This is a message", 1), deserialized);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Demo {
        private String fieldI;
        private int fieldII;
    }
}