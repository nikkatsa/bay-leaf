package com.nikoskatsanos.bayleaf.client;

/**
 * BayLeaf client session callbacks
 */
public interface SessionCallback {

    /**
     * Called when a session is established to the remote BayLeaf server. A session is established after the client successfully authenticates with the remote server
     */
    void onSessionInitialized();

    /**
     * Called when a session is closed
     */
    void onSessionDestroyed();

    class NullSessionCallback implements SessionCallback {

        @Override
        public void onSessionInitialized() {
        }

        @Override
        public void onSessionDestroyed() {
        }
    }

}
