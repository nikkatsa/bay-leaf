package com.nikoskatsanos.bayleaf.core.codec;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CodecDetails {

    private final BayLeafCodec.Serializer serializer;
    private final BayLeafCodec.Deserializer deserializer;
    private final Class<?> inType;
    private final Class<?> outType;
}
