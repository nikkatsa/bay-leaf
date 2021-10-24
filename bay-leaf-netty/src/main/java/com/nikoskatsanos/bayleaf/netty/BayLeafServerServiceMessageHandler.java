package com.nikoskatsanos.bayleaf.netty;

import com.nikoskatsanos.bayleaf.core.Connector;
import com.nikoskatsanos.bayleaf.core.ConnectorRegistry;
import com.nikoskatsanos.bayleaf.core.Heartbeat;
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

                final NettySessionContext nettySessionContext = new NettySessionContext(session, ctx);
                final ContextHolder contextHolder = new ContextHolder();
                contextHolder.sessionContext = nettySessionContext;
                this.sessionContexts.put(session.getSessionId(), contextHolder);

                connector.onSessionOpened(nettySessionContext, new ContextFactory(session, serviceMessage.getServiceName(), connector, ctx));
                ctx.channel().closeFuture().addListener(c -> connector.onSessionClosed(nettySessionContext));
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
                ApplicationMessage applicationMessage = (ApplicationMessage) message;
                switch (applicationMessage.getMessagingPattern()) {
                    case RRA:
                        NettyRRAContext rraContext = this.sessionContexts.get(session.getSessionId()).rraContextByName.get(applicationMessage.getRoute());
                        rraContext.ack();
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
            final NettyRRContext rrContext = new NettyRRContext<>();
            rrContext.setSession(this.session);
            rrContext.setServiceName(this.serviceName);
            rrContext.setRoute(rrEndpointName);
            rrContext.setChannelContext(this.channelHandlerContext);
            rrContext.setSerializer(this.serviceConnector.getSerializer());
            rrContext.setDeserializer(this.serviceConnector.getDeserializer());

            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).rrContextByName.put(rrEndpointName, rrContext);
            return rrContext;
        }

        @Override
        public RRAContext createRRA(final String rraEndpointName) {
            final NettyRRAContext rraContext = new NettyRRAContext();
            rraContext.setSession(this.session);
            rraContext.setServiceName(this.serviceName);
            rraContext.setRoute(rraEndpointName);
            rraContext.setChannelContext(this.channelHandlerContext);
            rraContext.setSerializer(this.serviceConnector.getSerializer());
            rraContext.setDeserializer(this.serviceConnector.getDeserializer());

            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).rraContextByName.put(rraEndpointName, rraContext);
            return rraContext;
        }

        @Override
        public BCContext createBroadcast(final String broadcastEndpointName) {
            final NettyBCContext bcContext = BayLeafServerServiceMessageHandler.this.broadcastContexts.computeIfAbsent(broadcastEndpointName, k -> {
                final NettyBCContext bcCtx = new NettyBCContext<>();
                bcCtx.setServiceName(this.serviceName);
                bcCtx.setRoute(broadcastEndpointName);
                bcCtx.setSerializer(this.serviceConnector.getSerializer());
                return bcCtx;
            });
            bcContext.addChannelHandlerContext(this.channelHandlerContext);
            return bcContext;
        }

        @Override
        public PSContext createPS(final String psEndpointName) {
            final NettyPSContext psContext = new NettyPSContext<>();
            psContext.setSession(session);
            psContext.setServiceName(this.serviceName);
            psContext.setRoute(psEndpointName);
            psContext.setSerializer(this.serviceConnector.getSerializer());
            psContext.setDeserializer(this.serviceConnector.getDeserializer());
            psContext.setChannelContext(this.channelHandlerContext);

            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).psContextByName.put(psEndpointName, psContext);
            return psContext;
        }

        @Override
        public SSContext createSS(final String ssEndpointName) {
            final NettySSContext ssContext = new NettySSContext();
            ssContext.setSession(this.session);
            ssContext.setServiceName(this.serviceName);
            ssContext.setRoute(ssEndpointName);
            ssContext.setSerializer(this.serviceConnector.getSerializer());
            ssContext.setDeserializer(this.serviceConnector.getDeserializer());
            ssContext.setChannelCtx(this.channelHandlerContext);
            BayLeafServerServiceMessageHandler.this.sessionContexts.get(this.session.getSessionId()).ssContextByName.put(ssEndpointName, ssContext);
            return ssContext;
        }
    }

    class ContextHolder {
        private NettySessionContext sessionContext;
        private final Map<String, NettyRRContext> rrContextByName = new HashMap<>();
        private final Map<String, NettyRRAContext> rraContextByName = new HashMap<>();
        private final Map<String, NettyPSContext> psContextByName = new HashMap<>();
        private final Map<String, NettySSContext> ssContextByName = new HashMap<>();
    }
}
