package com.nikoskatsanos.bayleaf.server;

import com.nikoskatsanos.bayleaf.domain.message.Heartbeat;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec.Deserializer;
import com.nikoskatsanos.bayleaf.core.codec.BayLeafCodec.Serializer;
import com.nikoskatsanos.bayleaf.core.codec.CodecDetails;
import com.nikoskatsanos.bayleaf.server.messagingpattern.BC;
import com.nikoskatsanos.bayleaf.server.messagingpattern.BCContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.MessagingPatternDetails;
import com.nikoskatsanos.bayleaf.server.messagingpattern.PS;
import com.nikoskatsanos.bayleaf.server.messagingpattern.PSContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RR;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRA;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRAContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.RRContext;
import com.nikoskatsanos.bayleaf.server.messagingpattern.MessagingPatternContextFactory;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SS;
import com.nikoskatsanos.bayleaf.server.messagingpattern.SSContext;
import com.nikoskatsanos.bayleaf.core.props.BayLeafProps;
import com.nikoskatsanos.bayleaf.core.props.SessionCloseCodes;
import com.nikoskatsanos.bayleaf.core.util.BayLeafThreadFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Connector {

    @Getter
    private final String name;
    private final BayLeafCodec.Deserializer deserializer;
    private final BayLeafCodec.Serializer serializer;

    private final Map<String, MessagingPatternDetails> rrMessagingPatterns = new HashMap<>();
    private final Map<String, MessagingPatternDetails> rraMessagingPatterns = new HashMap<>();
    private final Map<String, MessagingPatternDetails> psMessagingPatterns = new HashMap<>();
    private final Map<String, MessagingPatternDetails> ssMessagingPatterns = new HashMap<>();
    private final Map<String, MessagingPatternDetails> bcMessagingPatterns = new HashMap<>();

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final Map<String, Heartbeater> heartbeaters = new ConcurrentHashMap<>();

    private final ScheduledExecutorService heartbeatThreads;

    public Connector(String name, Deserializer deserializer, Serializer serializer) {
        this.name = name;
        this.deserializer = deserializer;
        this.serializer = serializer;
        this.heartbeatThreads = Executors.newScheduledThreadPool(4, new BayLeafThreadFactory(this.name + "-heartbeater"));
    }

    public void start() {
        logger.info("Starting Connector={}", getClass().getSimpleName());
        logger.info("Identifying messaging patterns for Connector={}", this.name);
        this.analyze();

        if (this.rrMessagingPatterns.size() > 0) {
            logger.info("Connector={}, RRMessagingPatterns=[{}]", this.name, this.rrMessagingPatterns.keySet().stream().collect(Collectors.joining(",")));
        }
        if (this.rraMessagingPatterns.size() > 0) {
            logger.info("Connector={}, RRAMessagingPatterns=[{}]", this.name, this.rraMessagingPatterns.keySet().stream().collect(Collectors.joining(",")));
        }
        if (this.psMessagingPatterns.size() > 0) {
            logger.info("Connector={}, PSMessagingPatterns=[{}]", this.name, this.psMessagingPatterns.keySet().stream().collect(Collectors.joining(",")));
        }
        if(this.ssMessagingPatterns.size() > 0){
            logger.info("Connector={}, SSMessagingPatterns=[{}]", this.name, this.ssMessagingPatterns.keySet().stream().collect(Collectors.joining(",")));
        }
        if (this.bcMessagingPatterns.size() > 0) {
            logger.info("Connector={}, BCMessagingPatterns=[{}]", this.name, this.bcMessagingPatterns.keySet().stream().collect(Collectors.joining(",")));
        }
    }

    public void stop() {
        logger.info("Stopping Connector={}", getClass().getSimpleName());
        for (final String sessionId : this.sessions.keySet()) {
            this.sessions.remove(sessionId).closeSession(SessionCloseCodes.CONNECTOR_CLOSE);
            this.heartbeaters.remove(sessionId).stop();
        }
    }

    public void onSessionOpened(final SessionContext sessionContext, final MessagingPatternContextFactory messagingPatternContextFactory) {
        logger.info("OnServiceSessionOpened Service={}, Session={}", this.name, sessionContext.getSession());
        this.sessions.put(sessionContext.getSession().getSessionId(), sessionContext);

        this.rrMessagingPatterns.entrySet().forEach(rr -> {
            RRContext rrContext = messagingPatternContextFactory.createRR(rr.getKey());
            try {
                rr.getValue().getMethod().invoke(this, rrContext);
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        this.rraMessagingPatterns.entrySet().forEach(rra -> {
            RRAContext rraContext = messagingPatternContextFactory.createRRA(rra.getKey());
            try {
                rra.getValue().getMethod().invoke(this, rraContext);
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        this.bcMessagingPatterns.entrySet().forEach(bc -> {
            final BCContext bcContext = messagingPatternContextFactory.createBroadcast(bc.getKey());
            try {
                bc.getValue().getMethod().invoke(this, bcContext);
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        this.psMessagingPatterns.entrySet().forEach(ps -> {
            final PSContext psContext = messagingPatternContextFactory.createPS(ps.getKey());
            try {
                ps.getValue().getMethod().invoke(this, psContext);
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        this.ssMessagingPatterns.entrySet().forEach(ss -> {
            final SSContext ssContext = messagingPatternContextFactory.createSS(ss.getKey());
            try {
                ss.getValue().getMethod().invoke(this, ssContext);
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        final Heartbeater heartbeater = new Heartbeater(sessionContext);
        this.heartbeaters.put(sessionContext.getSession().getSessionId(), heartbeater);
        sessionContext.onHeartbeat(this.name, heartbeater::incomingHeartbeat);
        heartbeater.schedule();
    }

    public void onSessionClosed(final SessionContext sessionContext) {
        logger.info("Closed Service={}, Session={}", this.name, sessionContext.getSession());
        final SessionContext removed = this.sessions.remove(sessionContext.getSession().getSessionId());
        final Heartbeater heartbeater = this.heartbeaters.remove(sessionContext.getSession().getSessionId());
        if (Objects.nonNull(heartbeater)) {
            heartbeater.stop();
        }
    }

    private void analyze() {
        final Method[] methods = this.getClass().getMethods();

        for (final Method method : methods) {
            if (method.isAnnotationPresent(RR.class)) {
                final RR rrAnnotation = method.getAnnotation(RR.class);
                final String name = rrAnnotation.name();
                final Class<?> inType = rrAnnotation.requestType();
                final Class<?> outType = rrAnnotation.responseType();
                final Parameter[] parameters = method.getParameters();
                if (parameters.length != 1 || !RRContext.class.equals(parameters[0].getType())) {
                    final String errorMsg = String.format("%s annotated method must have one parameter of type %s. Method=%s with %s#name=%s has Parameters=[%s]",
                        RR.class.getName(), RRContext.class.getName(), method.getName(), RR.class.getName(), name,
                        Stream.of(parameters).map(p -> String.format("%s %s", p.getType(), p.getName())).collect(Collectors.joining(",")));
                    throw new IllegalArgumentException(errorMsg);
                }

                this.rrMessagingPatterns.put(name, new MessagingPatternDetails(name, method, inType, outType));
            } else if (method.isAnnotationPresent(RRA.class)) {
                final RRA rraAnnotation = method.getAnnotation(RRA.class);
                final String name = rraAnnotation.name();
                final Class<?> inType = rraAnnotation.requestType();
                final Class<?> outType = rraAnnotation.responseType();
                final Parameter[] parameters = method.getParameters();
                if (parameters.length != 1 || !RRAContext.class.equals(parameters[0].getType())) {
                    final String errorMsg = String.format("%s annotated method must have one parameters of type %s. Method=%s with %s#name=%s has Parameters=[%s]",
                        RRA.class.getName(), RRAContext.class.getName(), method.getName(), RRA.class.getName(), name,
                        Stream.of(parameters).map(p -> String.format("%s %s", p.getType(), p.getName())).collect(Collectors.joining(",")));
                    throw new IllegalArgumentException(errorMsg);
                }

                this.rraMessagingPatterns.put(name, new MessagingPatternDetails(name, method, inType, outType));
            } else if (method.isAnnotationPresent(BC.class)) {
                final BC bcAnnotation = method.getAnnotation(BC.class);
                final String name = bcAnnotation.name();
                final Class<?> inType = Void.class;
                final Class<?> outType = bcAnnotation.broadcastType();
                final Parameter[] parameters = method.getParameters();
                if (parameters.length != 1 || !BCContext.class.equals(parameters[0].getType())) {
                    final String errorMsg = String.format("%s annotated method must have one parameter of type %s. Method=%s with %s#name=%s has Parameters=[%s]",
                        BC.class.getName(), BCContext.class.getName(), method.getName(), BC.class, name,
                        Stream.of(parameters).map(p -> String.format("%s %s", p.getType(), p.getName())).collect(Collectors.joining(",")));
                    throw new IllegalArgumentException(errorMsg);
                }

                this.bcMessagingPatterns.put(name, new MessagingPatternDetails(name, method, inType, outType));
            } else if (method.isAnnotationPresent(PS.class)) {
                final PS psAnnotation = method.getAnnotation(PS.class);
                final String name = psAnnotation.name();
                final Class<?> inType = psAnnotation.subscriptionType();
                final Class<?> outType = psAnnotation.dataType();
                final Class<?> snapshotType = psAnnotation.initialDataType();
                final Parameter[] parameters = method.getParameters();
                if (parameters.length != 1 || !PSContext.class.equals(parameters[0].getType())) {
                    final String errorMsg = String.format("%s annotated method must have one parameter of type %s. Method=%s with %s#name=%s has Parametrs=[%s]",
                        PS.class.getName(), PSContext.class.getName(), method.getName(), PS.class, name,
                        Stream.of(parameters).map(p -> String.format("%s %s", p.getType(), p.getName())).collect(Collectors.joining(",")));
                    throw new IllegalArgumentException(errorMsg);
                }

                this.psMessagingPatterns.put(name, new MessagingPatternDetails(name, method, inType, outType, snapshotType));
            } else if (method.isAnnotationPresent(SS.class)) {
                final SS ssAnnotation = method.getAnnotation(SS.class);
                final String name = ssAnnotation.name();
                final Class<?> inType = ssAnnotation.subscriptionType();
                final Class<?> snapshotType = ssAnnotation.initialDataType();
                final Class<?> outType = ssAnnotation.dataType();
                final Parameter[] parameters = method.getParameters();
                if (parameters.length != 1 || !SSContext.class.equals(parameters[0].getType())) {
                    final String errorMsg = String.format("%s annotated method must have one parameter of type %s. Method=%s with %s#name=%s has Parameters=[%s]",
                        SS.class.getName(), SSContext.class.getName(), method.getName(), SS.class, name,
                        Stream.of(parameters).map(p -> String.format("%s %s", p.getType(), p.getName())).collect(Collectors.joining(",")));
                    throw new IllegalArgumentException(errorMsg);
                }

                this.ssMessagingPatterns.put(name, new MessagingPatternDetails(name, method, inType, outType, snapshotType));
            }
        }
    }

    public CodecDetails getCodecDetails(final String endpointName) {
        MessagingPatternDetails messagingPatternDetails = this.rrMessagingPatterns.get(endpointName);
        if(Objects.nonNull(messagingPatternDetails)) {
            return new CodecDetails(this.serializer, this.deserializer, messagingPatternDetails.getInType(), messagingPatternDetails.getOutType());
        }
        messagingPatternDetails = this.rraMessagingPatterns.get(endpointName);
        if(Objects.nonNull(messagingPatternDetails)) {
            return new CodecDetails(this.serializer, this.deserializer, messagingPatternDetails.getInType(), messagingPatternDetails.getOutType());
        }
        messagingPatternDetails = this.psMessagingPatterns.get(endpointName);
        if(Objects.nonNull(messagingPatternDetails)) {
            return new CodecDetails(this.serializer, this.deserializer, messagingPatternDetails.getInType(), messagingPatternDetails.getOutType(), messagingPatternDetails.getSnapshotType());
        }
        messagingPatternDetails = this.ssMessagingPatterns.get(endpointName);
        if (Objects.nonNull(messagingPatternDetails)) {
            return new CodecDetails(this.serializer, this.deserializer, messagingPatternDetails.getInType(), messagingPatternDetails.getOutType(), messagingPatternDetails.getSnapshotType());
        }
        messagingPatternDetails = this.bcMessagingPatterns.get(endpointName);
        if (Objects.nonNull(messagingPatternDetails)) {
            return new CodecDetails(this.serializer, this.deserializer, messagingPatternDetails.getInType(), messagingPatternDetails.getOutType());
        }
        throw new IllegalArgumentException(String.format("EndpointName=%s was not found in Connector=%s messaging patterns", endpointName, this.name));
    }

    @RequiredArgsConstructor
    class Heartbeater {

        private final AtomicInteger heartbeatId = new AtomicInteger(-1);
        private final SessionContext sessionContext;
        private volatile ScheduledFuture<?> heartbeatSchedule;
        private volatile Heartbeat incomingHeartbeat;

        void schedule() {
            this.heartbeatSchedule = heartbeatThreads.scheduleAtFixedRate(() -> {
                try {
                    if (!this.validateIncomingHeartbeat()) {
                        logger.warn("Invalid heartbeat received for Service={}, Session={}, Heartbeat={}, LastServerHeartbeatId={}",
                            Connector.this.name, this.sessionContext.getSession(), this.incomingHeartbeat, this.heartbeatId.get());
                        this.sessionContext.closeSession(SessionCloseCodes.INVALID_HEARTBEAT);
                        return;
                    }
                    final Heartbeat heartbeat = new Heartbeat(Connector.this.name, heartbeatId.incrementAndGet(), System.currentTimeMillis());
                    this.sessionContext.heartbeat(heartbeat);
                } catch (final Exception e) {
                    logger.error("Exception while sending heartbeat to Session={}", this.sessionContext.getSession(), e);
                }
            }, 0, BayLeafProps.BAY_LEAF_HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS);
        }

        void stop() {
            if (Objects.nonNull(this.heartbeatSchedule)) {
                this.heartbeatSchedule.cancel(true);
            }
        }

        void incomingHeartbeat(final Heartbeat incomingHeartbeat) {
            this.incomingHeartbeat = incomingHeartbeat;
        }

        boolean validateIncomingHeartbeat() {
            if (this.heartbeatId.get() == -1) {
                return true;
            } else if (this.heartbeatId.get() >= 0 && Objects.isNull(this.incomingHeartbeat)) {
                return false;
            }
            return this.heartbeatId.get() == this.incomingHeartbeat.getId();
        }
    }
}
