package com.nikoskatsanos.bayleaf.core.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nikoskatsanos.bayleaf.core.test.Test.TestMsg;
import org.junit.jupiter.api.Test;

public class ProtobufCodecTest {

    private final ProtobufCodec codec = new ProtobufCodec();

    @Test
    public void testEncodingDecoding() {
        // given a .proto
        final TestMsg out = TestMsg.newBuilder().setTxt("This is a message").setId(1).build();
        // when serializing
        final byte[] outBytes = codec.serializer().serialize(out, TestMsg.class);
        // and deserializing
        final TestMsg deserialized = this.codec.deserializer().deserialize(outBytes, TestMsg.class);
        // then assert same
        assertEquals(out, deserialized);
    }
}
