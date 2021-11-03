package com.nikoskatsanos.bayleaf.netty;

import com.nikoskatsanos.bayleaf.core.Connector;
import com.nikoskatsanos.bayleaf.core.ConnectorRegistry;
import com.nikoskatsanos.bayleaf.core.Heartbeat;
import com.nikoskatsanos.bayleaf.core.codec.CodecDetails;
import com.nikoskatsanos.bayleaf.core.message.ServiceMessage;
import com.nikoskatsanos.bayleaf.core.Session;
import com.nikoskatsanos.bayleaf.core.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.core.message.Message;
import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPattern;
import com.nikoskatsanos.bayleaf.core.messagingpattern.BCContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.MessagingPatternContextFactory;
import com.nikoskatsanos.bayleaf.core.messagingpattern.PSContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.RRAContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.RRContext;
import com.nikoskatsanos.bayleaf.core.messagingpattern.SSContext;
import com.nikoskatsanos.bayleaf.netty.dispatch.DispatchingStrategy;
import com.nikoskatsanos.bayleaf.netty.messagingpattern.NettyBCContext;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import com.nikoskatsanos.bayleaf.netty.messagingpattern.NettyPSContext;
import com.nikoskatsanos.bayleaf.netty.messagingpattern.NettyRRAContext;
import com.nikoskatsanos.bayleaf.netty.messagingpattern.NettyRRContext;
import com.nikoskatsanos.bayleaf.netty.messagingpattern.NettySSContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Sharable
public class BayLeafServerServiceMessageHandler extends SimpleChannelInboundHandler<Message> {

    private static final NettyJsonCodec NETTY_JSON_CODEC = NettyJsonCodec.instance();

    private final ConnectorRegistry connectorRegistry;
    private final DispatchingStrategy dispatchingStrategy;

    private final Map<String, ContextHolder> sessionContexts = new ConcurrentHashMap<>();
    private final Map<String, NettyBCContext> broadcastContexts = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Message message) throws Exception {
        final Session session = ctx.channel().attr(AttributeKey.<Session>valueOf(NettyProps.NETTY_SESSION_ID)).get();
        switch (message.getMessageType()) {
            case SERVICE_CREATE:
                final ServiceMessage serviceMessage = NETTY_JSON_CODEC.deserializeFromBytes(message.getData(), ServiceMessage.class);

                if (!this.connectorRegistry.hasConnector(serviceMessage.getServiceName())) {
                    logger.error("No connector found for Service={}", serviceMessage.getServiceName());
                    return;
                }
                final Connector connector = this.connectorRegistry.getConnector(serviceMessage.getServiceName());

                final ContextHolder contextHolder = this.sessionContexts.computeIfAbsent(session.getSessionId(), k -> new ContextHolder(new NettySessionContext(session, ctx)));
                connector.onSessionOpened(contextHolder.sessionContext, new ContextFactory(session, serviceMessage.getServiceName(), connector, ctx));
                ctx.channel().closeFuture().addListener(c -> {
                    connector.onSessionClosed(contextHolder.sessionContext);
                    ContextHolder remove = this.sessionContexts.remove(session.getSessionId());// will be called one time per service, but is fine

                });
                break;
            case HEARTBEAT:
                final Heartbeat heartbeatIn = NETTY_JSON_CODEC.deserializeFromBytes(message.getData(), Heartbeat.class);
                final NettySessionContext incomingHeartbeatSessionContext = this.sessionContexts.get(session.getSessionId()).sessionContext;
                incomingHeartbeatSessionContext.heartbeatIn(heartbeatIn);
                break;
            case DATA:
                ApplicationMessage appMsg = (ApplicationMessage) message;
                switch (appMsg.getMessagingPattern()) {
                    case RR:
                        NettyRRContext nettyRRContext = this.sessionContexts.get(session.getSessionId()).rrContextByName.get(appMsg.getRoute());
                        nettyRRContext.request(appMsg);
                        break;
                    case RRA:
                        NettyRRAContext rraContext = this.sessionContexts.get(session.getSessionId()).rraContextByName.get(appMsg.getRoute());
                        rraContext.request(appMsg);
                        break;
                    case PS:
                        final NettyPSContext nettyPSContext = this.sessionContexts.get(session.getSessionId()).psContextByName.get(appMsg.getRoute());
                        nettyPSContext.subscription(appMsg);
                        break;
                    case SS:
                        final NettySSContext nettySSContext = this.sessionContexts.get(session.getSessionId()).ssContextByName.get(appMsg.getRoute());
                        nettySSContext.subscription(appMsg);
                        break;
                    case BC:
                        logger.warn("Unexpected message for MessagingPattern={}", MessagingPattern.BC);
                        break;
                }
                break;
            case DATA_ACK:
                ApplicationMessage ackMessage = (ApplicationMessage) message;
                switch (ackMessage.getMessagingPattern()) {
                    case RRA:
                        NettyRRAContext rraContext = this.sessionContexts.get(session.getSessionId()).rraContextByName.get(ackMessage.getRoute());
                        rraContext.ack(ackMessage);
                        break;
                }
                break;
            case DATA_CLOSE:
                final ApplicationMessage dataCloseAppMsg = (ApplicationMessage) message;
                switch (dataCloseAppMsg.getMessagingPattern()) {
                    case PS:
                        final NettyPSContext nettyPSContext = this.sessionContexts.get(session.getSessionId()).psContextByName.get(dataCloseAppMsg.getRoute());
                        nettyPSContext.close(dataCloseAppMsg);
                        break;
                    case SS:
                        final NettySSContext nettySSContext = this.sessionContexts.get(session.getSessionId()).ssContextByName.get(dataCloseAppMsg.getRoute());
                        nettySSContext.close(dataCloseAppMsg);
                        break;
                }
                break;
        }
    }

    @RequiredArgsConstructor
    class ContextFactory implements MessagingPatternContextFactory {

        private final Session session;
        private final String serviceName;
        private final Connector serviceConnector;
        private final ChannelHandlerContext channelHandlerContext;

        @Override
        public RRContext createRR(final String rrEndpointName) {
            final NettyRRContext rrContext = new NettyRRContext<>(this.session, this.serviceName, rrEndpointName, this.channelHandlerContext, dispatchingStrategy.dispatcher(this.session));
            final CodecDetails codecDetails = this.serviceConnector.getCodecDetails(rrEndpointName);
            rrContext.setSerializer(codecDetails.getSerializer(), codecDetails.getOutType());
            rrContext.setDeserializer(codecDetails.getDeserializer(), codecDetails.getInType());

            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).rrContextByName.put(rrEndpointName, rrContext);
            return rrContext;
        }

        @Override
        public RRAContext createRRA(final String rraEndpointName) {
            final NettyRRAContext rraContext = new NettyRRAContext<>(this.session, this.serviceName, rraEndpointName, this.channelHandlerContext, dispatchingStrategy.dispatcher(this.session));
            final CodecDetails codecDetails = this.serviceConnector.getCodecDetails(rraEndpointName);
            rraContext.setSerializer(codecDetails.getSerializer(), codecDetails.getOutType());
            rraContext.setDeserializer(codecDetails.getDeserializer(), codecDetails.getInType());

            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).rraContextByName.put(rraEndpointName, rraContext);
            return rraContext;
        }

        @Override
        public BCContext createBroadcast(final String broadcastEndpointName) {
            final NettyBCContext bcContext = BayLeafServerServiceMessageHandler.this.broadcastContexts.computeIfAbsent(broadcastEndpointName, k -> {
                final NettyBCContext bcCtx = new NettyBCContext<>(this.serviceName, broadcastEndpointName);
                final CodecDetails codecDetails = this.serviceConnector.getCodecDetails(broadcastEndpointName);
                bcCtx.setSerializer(codecDetails.getSerializer(), codecDetails.getOutType());
                return bcCtx;
            });
            bcContext.addChannelHandlerContext(this.channelHandlerContext);
            return bcContext;
        }

        @Override
        public PSContext createPS(final String psEndpointName) {
            final NettyPSContext psContext = new NettyPSContext<>(this.session, this.serviceName, psEndpointName, this.channelHandlerContext, dispatchingStrategy.dispatcher(this.session));
            final CodecDetails codecDetails = this.serviceConnector.getCodecDetails(psEndpointName);
            psContext.setSerializer(codecDetails.getSerializer(), codecDetails.getOutType());
            psContext.setDeserializer(codecDetails.getDeserializer(), codecDetails.getInType());
            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).psContextByName.put(psEndpointName, psContext);
            return psContext;
        }

        @Override
        public SSContext createSS(final String ssEndpointName) {
            final NettySSContext ssContext = new NettySSContext<>(this.session, this.serviceName, ssEndpointName, this.channelHandlerContext, dispatchingStrategy.dispatcher(this.session));
            final CodecDetails codecDetails = this.serviceConnector.getCodecDetails(ssEndpointName);
            ssContext.setSerializer(codecDetails.getSerializer(), codecDetails.getInType());
            ssContext.setDeserializer(codecDetails.getDeserializer(), codecDetails.getOutType());
            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).ssContextByName.put(ssEndpointName, ssContext);
            return ssContext;
        }
    }

    @RequiredArgsConstructor
    class ContextHolder {
        private final NettySessionContext sessionContext;
        private final Map<String, NettyRRContext> rrContextByName = new HashMap<>();
        private final Map<String, NettyRRAContext> rraContextByName = new HashMap<>();
        private final Map<String, NettyPSContext> psContextByName = new HashMap<>();
        private final Map<String, NettySSContext> ssContextByName = new HashMap<>();
    }
}
