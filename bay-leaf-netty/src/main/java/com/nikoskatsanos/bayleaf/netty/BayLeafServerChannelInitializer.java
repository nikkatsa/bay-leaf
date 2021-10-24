package com.nikoskatsanos.bayleaf.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
public class BayLeafServerChannelInitializer extends ChannelInitializer<Channel> {

    private final BayLeafServerSessionHandler sessionHandler;
    private final BayLeafServerServiceMessageHandler serviceMessageHandler;

    private SslContext sslContext = null;

    @Override
    protected void initChannel(final Channel channel) throws Exception {
        final ChannelPipeline pipeline = channel.pipeline();

        if (Objects.nonNull(this.sslContext)) {
            pipeline.addLast(this.sslContext.newHandler(channel.alloc()));
        }

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(64_000));
        pipeline.addLast(new WebSocketServerProtocolHandler("/"));
        pipeline.addLast(this.sessionHandler);
        pipeline.addLast(this.serviceMessageHandler);
    }
}
