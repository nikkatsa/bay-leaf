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

    <v-row>
        <v-card elevation="3" outlined class="ma-2" :disabled="isTradeInProgress" :loading="isTradeInProgress">
            <v-card-title class="pa-1">
                <v-select hide-details="auto" class="ma-0 pa-0" v-model="ccy" :items="ccyList">
                </v-select>
            </v-card-title>

            <v-card-text class="pa-1" style="min-height: 80px">
              <v-container class="pa-0">
                <v-row no-gutters style="height: 30px" justify="space-around">
                  <span>
                    <v-text-field class="pa-0" style="width: 100px; text-align: centre" v-model="quantity" dense hide-details="auto" type="number" />
                  </span>
                </v-row>
                <v-row no-gutters>
                  <v-col>
                    <v-btn class="pa-0 ma-0" style="height: 80px" elevation="3" block text :color="bidPriceColor" @click="trade">
                      {{ lastBidPrice.toFixed(4) }} 
                      <v-icon :color="bidPriceColor">
                        {{ bidMarketTrendMdiIcon }}
                      </v-icon>
                    </v-btn>
                  </v-col>

                  <v-col>
                    <v-btn class="pa-0 ma-0" style="height: 80px" elevation="3" block text :color="askPriceColor">
                      {{ lastAskPrice.toFixed(4) }} 
                      <v-icon :color="askPriceColor">
                        {{ askMarketTrendMdiIcon }}
                      </v-icon>
                    </v-btn>
                  </v-col>
                </v-row>
              </v-container>
            </v-card-text>
        </v-card>
    </v-row>
  </v-container>
</template>

<script>
import { createClient } from "./../bayleaf/bayleafClient";
import { JsonCodec } from "../bayleaf/codec/codecs";
import { TradeRequest, TradeResponse } from "../model/trade";

export default {
    name: 'MultiServiceExample',

    data: () => {
        return {
            url: "wss://localhost:9999",
            isConnected: false,
            connectionInProgress: false,

            // MarketDataService
            ccyList: [],
            marketDataService: null,
            isMarketServiceDataActive: false,
            ccy: null,
            activeSubscriptions: new Map(),

            lastBidPrice: 0.0,
            bidPriceColor: 'grey',
            bidMarketTrendMdiIcon: 'mid-minus',
            lastAskPrice: 0.0,
            askPriceColor: 'grey',
            askMarketTrendMdiIcon: 'mid-minus',

            marketTrendMdiIconUp: 'mdi-arrow-top-right-thick',
            marketTrendMdiIconDown: 'mdi-arrow-bottom-right-thick',
            marketTrendMdiIconNeutral: 'mdi-minus',

            // TradeService
            tradeService: null,
            isTradeServiceActive: false,
            tradeId: 0,
            quantity: 0,
            isTradeInProgress: false,
        }
    },

    watch: {
      ccy: function (newVal, oldVal) {
        if(oldVal) {
          this.unsubscrive(oldVal);
        }
        this.subscribe(newVal);
      }      
    },
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

      this.marketDataService = this.client.createService( "marketData", new JsonCodec(), { onHeartbeatReceived: this.onMarketDataServiceHeartbeat });

      this.tradeService = this.client.createService("tradeService", new JsonCodec(), { onHeartbeatReceived: this.onTradeServiceHeartbeat });
    },
    onSessionDestroyed(isReconnecting) {
      this.isConnected = false;
      this.connectionInProgress = isReconnecting;
      this.isMarketServiceDataActive = false;
      this.isTradeServiceActive = true;
    },
    onMarketDataServiceHeartbeat() {
      this.isMarketServiceDataActive = true;
      
      if(this.ccyList.length === 0) {
        this.marketDataService.requestResponse("securityList", {})
          .then( securityListResponse => {
            this.ccyList = [].concat(securityListResponse.ccyList);
          })
          .catch( error => console.error(`Failed to retrieve security list`));
      }
    },
    onTradeServiceHeartbeat() {
        this.isTradeServiceActive = true;
    },

    subscribe(symbol) {
      const subscriptionId = this.marketDataService.sharedStream("stream", { symbol: symbol }, {initialData: this.onMarketData, data: this.onMarketData});
      this.activeSubscriptions.set(symbol, subscriptionId);
    },
    unsubscrive(symbol) {
      const subscriptionId = this.activeSubscriptions.get(symbol);
      if(!subscriptionId) {
        return;
      }
      this.marketDataService.sharedStreamClose("stream", subscriptionId, {symbol: symbol});
      this.activeSubscriptions.delete(symbol);
    },
    onMarketData(marketData) {
        if(this.lastBidPrice && this.lastBidPrice < marketData.bidPrice) {
          this.bidPriceColor = 'green';
          this.bidMarketTrendMdiIcon  =  this.marketTrendMdiIconUp;
        } else if(this.lastBidPrice && this.lastBidPrice > marketData.bidPrice) {
          this.bidPriceColor = 'red';
          this.bidMarketTrendMdiIcon  =  this.marketTrendMdiIconDown;
        } else {
          this.bidPriceColor = 'grey';
          this.bidMarketTrendMdiIcon  =  this.marketTrendMdiIconNeutral;
        }

        if(this.lastAskPrice && this.lastAskPrice < marketData.askPrice) {
          this.askPriceColor = 'green';
          this.askMarketTrendMdiIcon  =  this.marketTrendMdiIconUp;
        } else if(this.lastAskPrice && this.lastAskPrice > marketData.askPrice) {
          this.askPriceColor = 'red';
          this.askMarketTrendMdiIcon  =  this.marketTrendMdiIconDown;
        } else {
          this.askPriceColor = 'grey';
          this.askMarketTrendMdiIcon  =  this.marketTrendMdiIconNeutral;
        }

        this.lastBidPrice = marketData.bidPrice;
        this.lastAskPrice = marketData.askPrice;
    },

    trade() {
        this.isTradeInProgress = true;
        this.tradeService.requestResponseAck('trade', new TradeRequest(++this.tradeId, this.ccy, this.quantity, this.price))
            .then( responseWithAck => {
                this.isTradeInProgress = false;
                responseWithAck.ack();
            })
            .catch( error => { 
              console.error(error);
              this.isTradeInProgress = false;
            })
    },
    }
}
</script>
