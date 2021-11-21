package com.nikoskatsanos.bayleaf.core.codec;

import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec.Deserializer;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec.Serializer;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodecDetails {

    private final BayLeafCodec.Serializer serializer;
    private final BayLeafCodec.Deserializer deserializer;
    private final Class<?> inType;
    private final Class<?> outType;
    private final Class<?> snapshotType;

    public CodecDetails(Serializer serializer, Deserializer deserializer, Class<?> inType, Class<?> outType) {
        this(serializer, deserializer, inType, outType, null);
    }
}
