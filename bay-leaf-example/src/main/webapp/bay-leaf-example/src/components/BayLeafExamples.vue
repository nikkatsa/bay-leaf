<template>
  <v-container fluid>
    <v-row class="text-center">
      <v-card
        elevation="3"
        class="ma-0"
        style="min-width: 100%"
        :loading="true"
      >
        <v-card-title>Connection</v-card-title>

        <template slot="progress">
          <v-progress-linear
            :value="connectionInProgress ? 0 : 100"
            :buffer-value="0"
            stream
            :color="isConnected ? 'success' : 'error'"
            height="10"
          ></v-progress-linear>
        </template>
        <v-container>
          <v-row>
            <v-col style="max-width: 200px">
              <v-btn
                class="ma-4"
                @click="isConnected ? disconnect() : connect()">{{ isConnected ? "Disconnect" : "Connect" }}</v-btn
              >
            </v-col>
            <v-col style="min-width: 300px">
              <v-text-field v-model="url" />
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-row>

    <v-divider><v-spacer /></v-divider>
    <!-- RR -->
    <v-row class="mt-8">
      <v-card style="min-width: 100%">
        <v-card-title>
          RR Pattern (EchoService#echo)
          <v-icon :color="isEchoServiceHeartbeating ? 'red' : 'grey'">{{isEchoServiceHeartbeating ? "mdi-heart-pulse" : "mdi-heart-off"}}</v-icon>
        </v-card-title>
        <v-container fluid>
          <v-row>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Request:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="rrRequest" />
            </v-col>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Response:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="rrResponse" />
            </v-col>
            <v-col>
              <v-btn @click="rrSend">Send</v-btn>
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-row>

    <!-- RR Error -->
    <v-row class="mt-8">
      <v-card style="min-width: 100%">
        <v-card-title>
          RR Error Pattern (EchoService#echoError)
          <v-icon :color="isEchoServiceHeartbeating ? 'red' : 'grey'">{{isEchoServiceHeartbeating ? "mdi-heart-pulse" : "mdi-heart-off"}}</v-icon>
        </v-card-title>
        <v-container fluid>
          <v-row>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Request:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="rrErrorRequest" />
            </v-col>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Response:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="rrErrorResponse" />
            </v-col>
            <v-col>
              <v-btn @click="rrErrorSend">Send</v-btn>
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-row>

    <!-- RRA -->
    <v-row class="mt-8">
      <v-card style="min-width: 100%">
        <v-card-title>
          RRA Pattern (EchoService#echoAck)
          <v-icon :color="isEchoServiceHeartbeating ? 'red' : 'grey'">{{isEchoServiceHeartbeating ? "mdi-heart-pulse" : "mdi-heart-off"}}</v-icon>
        </v-card-title>
        <v-container fluid>
          <v-row>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Request:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="rraRequest" />
            </v-col>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Response:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="rraResponse" />
            </v-col>
            <v-col>
              <v-btn @click="rraSend">Send</v-btn>
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-row>

    <!-- PS -->
    <v-row class="mt-8">
      <v-card style="min-width: 100%">
        <v-card-title>
          PS Pattern (EchoService#echoStream)
          <v-icon :color="isEchoServiceHeartbeating ? 'red' : 'grey'">{{isEchoServiceHeartbeating ? "mdi-heart-pulse" : "mdi-heart-off"}}</v-icon>
        </v-card-title>
        <v-container fluid>
          <v-row>
            <v-col class="mt-4" style="max-width: 120px">
              <v-label style="max-width: 100%">Subscription:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="psSubscription" />
            </v-col>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Data:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="psData" />
            </v-col>
            <v-col>
              <v-btn class="mr-2" @click="psSubscribe">Sub</v-btn>
              <v-btn @click="psClose">Close</v-btn>
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-row>

    <!-- SS -->
    <v-row class="mt-8">
      <v-card style="min-width: 100%">
        <v-card-title>
          SS Pattern (EchoService#echoStream#echoShared)
          <v-icon :color="isEchoServiceHeartbeating ? 'red' : 'grey'">{{isEchoServiceHeartbeating ? "mdi-heart-pulse" : "mdi-heart-off"}}</v-icon>
        </v-card-title>
        <v-container fluid>
          <v-row>
            <v-col class="mt-4" style="max-width: 120px">
              <v-label style="max-width: 100%">Subscription:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="ssSubscription" />
            </v-col>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Data:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="ssData" />
            </v-col>
            <v-col>
              <v-btn class="mr-2" @click="ssSubscribe">Sub</v-btn>
              <v-btn @click="ssClose">Close</v-btn>
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-row>

    <!-- BS -->
    <v-row class="mt-8">
      <v-card style="min-width: 100%">
        <v-card-title>
          BS Pattern (EchoService#echoBroadcast)
          <v-icon :color="isEchoServiceHeartbeating ? 'red' : 'grey'">{{isEchoServiceHeartbeating ? "mdi-heart-pulse" : "mdi-heart-off"}}</v-icon>
        </v-card-title>
        <v-container fluid>
          <v-row>
            <v-col class="mt-4" style="max-width: 100px">
              <v-label>Broadcast Message:</v-label>
            </v-col>
            <v-col>
              <v-text-field v-model="bsResponse" />
            </v-col>
          </v-row>
        </v-container>
      </v-card>
    </v-row>
  </v-container>
</template>

<script>
import { createClient } from "./../bayleaf/bayleafClient";
import { StringCodec } from "../bayleaf/codec/codecs";

export default {
  name: "BayLeafExamples",

  destroyed: function() {
    if(this.echoService) {
      console.info(`Closing echoService`);
      this.disconnect();
    }
  },
  data: () => ({
    url: "wss://localhost:9999",
    isConnected: false,
    connectionInProgress: false,
    client: undefined,
    echoService: undefined,

    isEchoServiceHeartbeating: false,
    rrRequest: "",
    rrResponse: "",

    rrErrorRequest: "",
    rrErrorResponse: "",

    rraRequest: "",
    rraResponse: "",

    psSubscription: "Sub",
    psSubscriptionIds : new Map(),
    psData: "",

    ssSubscription: "A",
    ssSubscriptionIds: new Map(),
    ssData: "",

    bsResponse: "",
  }),

  methods: {
    connect() {
      this.connectionInProgress = true;
      let connUrl = "";
      if (this.url.startsWith("ws://")) {
        connUrl = this.url.substring(5);
      } else if (this.url.startsWith("wss://")) {
        connUrl = this.url.substring(6);
      }
      this.client = createClient(connUrl, {
        onSessionInitialized: this.onSessionInitialized,
        onSessionDestroyed: this.onSessionDestroyed,
      });
    },
    disconnect() {
      this.client.close();
    },
    onSessionInitialized() {
      this.isConnected = true;
      this.connectionInProgress = false;

      this.echoService = this.client.createService(
        "echoService",
        new StringCodec(),
        { onHeartbeatReceived: this.onHeartbeatReceived }
      );
      this.echoService.broadcast(
        "echoBroadcast",
        this.onBroadcastMessageReceived
      );
    },
    onSessionDestroyed(isReconnecting) {
      this.isConnected = false;
      this.connectionInProgress = isReconnecting;
      this.isEchoServiceHeartbeating = false;
    },
    onHeartbeatReceived() {
      this.isEchoServiceHeartbeating = true;
    },
    rrSend() {
      console.log(`Sending RR Request=${this.rrRequest}`);
      if (this.echoService) {
        this.echoService
          .requestResponse("echo", this.rrRequest)
          .then((response) => {
            console.log(response);
            this.rrResponse = response;
          });
      } else {
        console.warn(`EchoService is not initialized`);
      }
    },
    rrErrorSend() {
      if (this.echoService) {
        this.echoService
          .requestResponse("echoError", this.rrErrorRequest)
          .then((response) => {})
          .catch(
            (error) =>
              (this.rrErrorResponse = error.errorCode + ":" + error.errorMsg)
          );
      }
    },
    rraSend() {
      if (this.echoService) {
        this.echoService
          .requestResponseAck("echoAck", this.rraRequest)
          .then((responseWithAck) => {
            this.rraResponse = responseWithAck.response;
            console.log(`Acking RRA ${this.rraResponse}`);
            responseWithAck.ack();
          })
          .catch((error) => console.error(error));
      }
    },
    psSubscribe() {
      if (this.echoService) {
        const subscriptionId = this.echoService.privateStream("echoStream", this.psSubscription, { initialData: this.psInitialData, data: this.psDataCallback });
        this.psSubscriptionIds.set(this.psSubscription,subscriptionId);
      }
    },
    psClose() {
      if (this.echoService) {
        const subscriptionId = this.psSubscriptionIds.get(this.psSubscription);
        this.psSubscriptionIds.delete(this.psSubscription);
        this.echoService.privateStreamClose("echoStream", subscriptionId, this.psSubscription);
      }
    },
    psInitialData(psData) {
      console.info(`InitialData=${psData}`);
    },
    psDataCallback(psData) {
      this.psData = psData;
    },
    ssSubscribe() {
      if(this.echoService) {
        const ssSubscriptionId = this.echoService.sharedStream("echoShared", this.ssSubscription, {initialData: this.ssInitialData, data: this.ssDataCallback});
        this.ssSubscriptionIds.set(this.ssSubscription, ssSubscriptionId)
      }
    },
    ssClose() {
      if(this.echoService) {
        const ssSubscriptionId = this.ssSubscriptionIds.get(this.ssSubscription);
        this.ssSubscriptionIds.delete(this.ssSubscription);
        this.echoService.sharedStreamClose("echoShared", ssSubscriptionId, this.ssSubscription);
      }
    },
    ssInitialData(ssData) {
      this.ssData = ssData;
    },
    ssDataCallback(ssData) {
      this.ssData = ssData;
    },
    onBroadcastMessageReceived(broadcastMsg) {
      this.bsResponse = broadcastMsg;
    },
  },
};
</script>
