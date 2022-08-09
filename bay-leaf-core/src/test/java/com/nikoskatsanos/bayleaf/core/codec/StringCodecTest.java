package com.nikoskatsanos.bayleaf.core.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringCodecTest {

    private final StringCodec codec = new StringCodec();

    @Test
    public void testEncodingDecoding() {
        // given a string
        final String out = "This is a message";
        // when serializing
        final byte[] outBytes = codec.serializer().serialize(out, String.class);
        // and deserializing
        final String deserialized = codec.deserializer().deserialize(outBytes, String.class);
        // then assert
        assertEquals(out, deserialized);
        assertArrayEquals(out.getBytes(), outBytes);
    }
}
