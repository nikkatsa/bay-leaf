package com.nikoskatsanos.bayleaf.client.netty.handler;

import com.nikoskatsanos.bayleaf.client.netty.BayLeafServiceNettyImpl;
import com.nikoskatsanos.bayleaf.domain.message.ApplicationMessage;
import com.nikoskatsanos.bayleaf.domain.message.ErrorMessage;
import com.nikoskatsanos.bayleaf.netty.codec.NettyJsonCodec;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class BayLeafApplicationMessageHandler extends SimpleChannelInboundHandler<ApplicationMessage> {

    private static final NettyJsonCodec NETTY_JSON_CODEC = NettyJsonCodec.instance();

    private final Map<String, BayLeafServiceNettyImpl> services = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ApplicationMessage msg) throws Exception {
        final String serviceName = msg.getServiceName();
        final BayLeafServiceNettyImpl bayLeafService = services.get(serviceName);
        if (Objects.isNull(bayLeafService)) {
            logger.warn("Message received for unknown Service={}, Message={}", serviceName, msg);
            return;
        }

        switch (msg.getMessagingPattern()) {
            case RR:
                this.handleRRMessage(bayLeafService, msg);
                break;
            case RRA:
                this.handleRRAMessage(bayLeafService, msg);
                break;
            case PS:
                this.handlePSMessage(bayLeafService, msg);
                break;
            case SS:
                this.handleSSMessage(bayLeafService, msg);
                break;
            case BC:
                this.handleBCMessage(bayLeafService, msg);
                break;
        }
    }

    private void handleRRMessage(final BayLeafServiceNettyImpl bayLeafService, final ApplicationMessage msg) {
        switch (msg.getMessageType()) {
            case DATA:
                bayLeafService.onRRResponse(msg);
                break;
            case ERROR:
                final ErrorMessage errorMessage = NETTY_JSON_CODEC.deserializeFromBytes(msg.getData(), ErrorMessage.class);
                bayLeafService.onRRError(errorMessage);
                break;
            default:
                logger.warn("Unhandled RR message Service={}, Msg={}", bayLeafService.getServiceName(), msg);
        }
    }

    private void handleRRAMessage(final BayLeafServiceNettyImpl bayLeafService, final ApplicationMessage msg) {
        switch (msg.getMessageType()) {
            case DATA:
                bayLeafService.onRRAResponse(msg);
                break;
            case ERROR:
                final ErrorMessage errorMessage = NETTY_JSON_CODEC.deserializeFromBytes(msg.getData(), ErrorMessage.class);
                bayLeafService.onRRAError(errorMessage);
                break;
            default:
                logger.warn("Unhandled RRA message Service={}, Msg={}", bayLeafService.getServiceName(), msg);
        }
    }

    private void handlePSMessage(final BayLeafServiceNettyImpl bayLeafService, final ApplicationMessage msg) {
        switch (msg.getMessageType()) {
            case INITIAL_DATA:
                bayLeafService.onPSInitialData(msg);
                break;
            case DATA:
                bayLeafService.onPSData(msg);
                break;
            default:
                logger.warn("Unhandled PS message Service={}, Msg={}", bayLeafService.getServiceName(), msg);
        }
    }

    private void handleSSMessage(final BayLeafServiceNettyImpl bayLeafService, final ApplicationMessage msg) {
        switch (msg.getMessageType()) {
            case INITIAL_DATA:
                bayLeafService.onSSInitialData(msg);
                break;
            case DATA:
                bayLeafService.onSSData(msg);
                break;
            default:
                logger.warn("Unhandled SS message Service={}, Msg={}", bayLeafService.getServiceName(), msg);
        }
    }

    private void handleBCMessage(final BayLeafServiceNettyImpl bayLeafService, final ApplicationMessage msg) {
        switch (msg.getMessageType()) {
            case DATA:
                bayLeafService.onBroadcast(msg);
                break;
            default:
                logger.warn("Unhandled BC message Service={}, Msg={}", bayLeafService.getServiceName(), msg);
        }
    }

    public void registerService(final BayLeafServiceNettyImpl service) {
        this.services.putIfAbsent(service.getServiceName(), service);
    }
}
