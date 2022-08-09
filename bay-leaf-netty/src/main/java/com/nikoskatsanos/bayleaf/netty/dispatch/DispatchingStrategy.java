package com.nikoskatsanos.bayleaf.netty.dispatch;

import com.nikoskatsanos.bayleaf.domain.Session;
import com.nikoskatsanos.bayleaf.domain.User;
import com.nikoskatsanos.bayleaf.core.util.BayLeafThreadFactory;
import com.nikoskatsanos.bayleaf.core.util.OrderedExecutor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;

/**
 * The strategy on how to execute events that come from the network. Those events will be using Netty's epoll thread group and depending on the application's
 * semantics a different thread for dispatching those events to the application layer(business logic layer) might be needed.
 */
public interface DispatchingStrategy {

    @FunctionalInterface
    interface Action {

        void dispatch();
    }

    @FunctionalInterface
    interface Dispatcher {

        void dispatch(final Action action);
    }

    /**
     * @param session User's session can be used to make a {@link Dispatcher} decision
     * @return a dispatcher that is going to hand messages from the network layer (netty) to the application layer (business logic)
     */
    Dispatcher dispatcher(final Session session);

    static DispatchingStrategy defaultStrategy() {
        return new DirectDispatcher();
    }

    class DirectDispatcher implements DispatchingStrategy, Dispatcher {

        @Override
        public void dispatch(final Action action) {
            action.dispatch();
        }

        @Override
        public Dispatcher dispatcher(final Session session) {
            return this;
        }
    }

    @RequiredArgsConstructor
    class ThreadPoolDispatcher implements DispatchingStrategy, Dispatcher {

        private final ExecutorService dispatchers;

        public ThreadPoolDispatcher() {
            this(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        }

        public ThreadPoolDispatcher(int threads) {
            this(Executors.newFixedThreadPool(threads, new BayLeafThreadFactory("BayLeafNettyDispatcher")));
        }

        @Override
        public void dispatch(final Action action) {
            this.dispatchers.execute(action::dispatch);
        }

        @Override
        public Dispatcher dispatcher(final Session session) {
            return this;
        }
    }

    @RequiredArgsConstructor
    class OrderedDispatcher implements DispatchingStrategy {

        private final Map<User, OrderedExecutor> userExecutors = new ConcurrentHashMap<>();

        private final ExecutorService dispatchers;

        @Override
        public Dispatcher dispatcher(final Session session) {
            final OrderedExecutor orderedExecutor = this.userExecutors.computeIfAbsent(session.getUser(), k -> new OrderedExecutor(this.dispatchers));
            return a -> orderedExecutor.execute(()-> a.dispatch());
        }
    }
}
