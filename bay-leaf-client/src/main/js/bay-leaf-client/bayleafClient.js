import {v4 as uuidv4} from 'uuid';
import MessageType from './domain/MessageType'
import Heartbeat from './domain/Heartbeat'
import { Message, ApplicationMessage, ErrorMessage, MESSAGING_PATTERN, RRPromise, RRAPromise } from './domain/Messages';
import {JsonCodec} from './codec/codecs';

const jsonCodec = new JsonCodec();

const wsClientsMap = new Map();

const awaitingCallbacks = new Map();

const psStreams = new Map();

const ssStreams = new Map();

const broadcasts = new Map();

class BayLeafClient {

  sessionId;

  sessionCallbackListeners = [];

  isUserInitiatedClose = false;
  isReconnecting = null;
  isSessionInitialized = false;

  services = new Map();

  constructor(url, sessionCallbackListener) {
      this.addSessionCallbackListener(sessionCallbackListener);
      this.url = url;
      this.ws = new WebSocket(this.url);
      this.ws.onopen = this.onOpen;
      this.ws.onclose = this.onClose;
      this.ws.onmessage = this.onMessage;
  }

  onOpen = (event) => {
      console.info(`Connection opened to URL=${this.url}`);
  }

  onClose = (event) => {
      console.info(`Connection and session closed to URL=${this.url}, SessionId=${this.sessionId}, IsUserInitiatedClose=${this.isUserInitiatedClose}`);
      this.isSessionInitialized = false;
      for(let sessionCallbackListener of this.sessionCallbackListeners) {
          const onReconnectLoop = !this.isUserInitiatedClose;
          sessionCallbackListener.onSessionDestroyed(onReconnectLoop);
      }
      this.services.clear();
      if(this.isUserInitiatedClose) {
        wsClientsMap.delete(this.url);
      } else {
        console.warn(`Reconnecting to URL=${this.url}`);
        this.isReconnecting = setTimeout(() => {
          this.ws = new WebSocket(this.url);
          this.ws.onopen = this.onOpen;
          this.ws.onclose = this.onClose;
          this.ws.onmessage = this.onMessage;
        }, 3000);
      }
  }

  addSessionCallbackListener(sessionCallbackListener) {
      if(sessionCallbackListener) {
        this.sessionCallbackListeners.push(sessionCallbackListener);
      }
      if(this.isSessionInitialized) {
        setTimeout(() => sessionCallbackListener.onSessionInitialized())
      }
  }

  onMessage = (event) => {
      const msg = JSON.parse(event.data);
      console.debug(msg);
      switch(msg.messageType) {
          case MessageType.SESSION_INITIALIZING:
              console.info(`OnSessionInitializing SessionId=${msg.correlationId}`);
              this.sessionId  = msg.correlationId;
              const authMessage = new Message(this.sessionId, MessageType.AUTH, jsonCodec.encode({username: 'user', password: 'pass'}));
              this.send(JSON.stringify(authMessage));
              break;
          case MessageType.SESSION_INITIALIZED:
              console.info(`OnSessionInitialized SessionId=${this.sessionId}`);
              this.isSessionInitialized = true;
              for(let sessionCallbackListener of this.sessionCallbackListeners) {
                  sessionCallbackListener.onSessionInitialized();
              }
              break;
          case MessageType.HEARTBEAT:
            const heartbeatPayload = JSON.parse(atob(msg.data))
            if(this.services.has(heartbeatPayload.serviceName)) {
              this.services.get(heartbeatPayload.serviceName).onHeartbeat(heartbeatPayload);
            } else {
              console.warn(`Heartbeat received for unknown service ${JSON.stringify(heartbeatPayload)}`);
            }
            break;
          case MessageType.DATA:
          case MessageType.INITIAL_DATA:
          case MessageType.ERROR:
            if(awaitingCallbacks.has(msg.correlationId)) {
              awaitingCallbacks.get(msg.correlationId).onMessage(msg);
            }
            awaitingCallbacks.delete(msg.correlationId);

            if(msg.messagingPattern === MESSAGING_PATTERN.BC) {
                if(broadcasts.has(msg.route)) {
                    for(let serviceCallback of broadcasts.get(msg.route)) {
                      serviceCallback.onMessage(msg);
                    }
                }
            } else if(msg.messagingPattern === MESSAGING_PATTERN.PS) {
                if(psStreams.has(msg.correlationId)) {
                  psStreams.get(msg.correlationId).onMessage(msg);
                }
            } else if(msg.messagingPattern === MESSAGING_PATTERN.SS) {
              if(ssStreams.has(msg.correlationId)) {
                ssStreams.get(msg.correlationId).onMessage(msg);
              }
            }
            break;
          default:
              console.warn(`Unhandled MsgType=${msg.messageType}, Msg=${JSON.parse(msg)}`);
              break;
      }
  }

  createService(serviceName, codec, serviceCallbackListener) {
    if(this.services.has(serviceName)) {
      const service = this.services.get(serviceName);
      service.addServiceCallbackListener(serviceCallbackListener);
      return service;
    } else {
      const data = { serviceName: serviceName }
      const service = new Service(serviceName, codec, serviceCallbackListener, this);
      this.services.set(serviceName, service);
      const msg = new Message(uuidv4(), MessageType.SERVICE_CREATE, jsonCodec.encode(data));
      this.send(JSON.stringify(msg));
      return service;
    }
  }

  send(data) {
    this.ws.send(data);
  }

  close() {
    console.log(`Closing connection to URL=${this.url}`);
    this.isUserInitiatedClose = true;
    this.ws.close();
  }
}

class Service {

  rrPromises  = new Map();
  rraPromises = new Map();
  psCallbacks = new Map();
  ssCallbacks = new Map();
  broadcastCallbacks = new Map();

  constructor(serviceName, codec, serviceCallbackListener, bayLeafClient) {
    this.serviceName = serviceName;
    this.codec = codec;
    this.serviceCallbackListeners = [];
    if(serviceCallbackListener) {
      this.serviceCallbackListeners.push(serviceCallbackListener);
    }
    this.bayLeafClient = bayLeafClient;
  }

  addServiceCallbackListener(serviceCallbackListener) {
    if(serviceCallbackListener) {
      this.serviceCallbackListeners.push(serviceCallbackListener);
    }
  }

  requestResponse(route, request) {
    const correlationId = uuidv4();
    const requestMsg = new ApplicationMessage(correlationId, MessageType.DATA, this.serviceName, route, MESSAGING_PATTERN.RR, this.codec.encode(request));

    console.info(`RR ServiceName=${this.serviceName} Route=${route} Request=${JSON.stringify(request)} ApplicationMessage=${JSON.stringify(requestMsg)}`);
    const rrPromise = new RRPromise();
    this.rrPromises.set(correlationId, rrPromise);
    awaitingCallbacks.set(correlationId, this);
    this.bayLeafClient.send(JSON.stringify(requestMsg));
    return rrPromise.getPromise();
  }

  requestResponseAck(route, request) {
    const correlationId = uuidv4();
    const requestMsg = new ApplicationMessage(correlationId, MessageType.DATA, this.serviceName, route, MESSAGING_PATTERN.RRA, this.codec.encode(request));

    console.info(`RRA ServiceName=${this.serviceName} Route=${route} Request=${JSON.stringify(request)} ApplicationMessage=${JSON.stringify(requestMsg)}`);
    const ackMsg = new ApplicationMessage(correlationId, MessageType.DATA_ACK, this.serviceName, route, MESSAGING_PATTERN.RRA, this.codec.encode(request));
    const ackMsgConsumer = ackMsg => { this.bayLeafClient.send(JSON.stringify(ackMsg)) };
    const rraPromise = new RRAPromise(ackMsg, ackMsgConsumer);
    this.rraPromises.set(correlationId, rraPromise);
    awaitingCallbacks.set(correlationId, this);
    this.bayLeafClient.send(JSON.stringify(requestMsg));
    return rraPromise.getPromise();
  }

  privateStream(route, subscription, callback) {
    const correlationId = uuidv4();
    const subscriptionMsg = new ApplicationMessage(correlationId, MessageType.DATA, this.serviceName, route, MESSAGING_PATTERN.PS, this.codec.encode(subscription));

    console.info(`PS ServiceName=${this.serviceName} Route=${route} Subscription=${JSON.stringify(subscriptionMsg)}`);
    this.psCallbacks.set(correlationId, callback);
    psStreams.set(correlationId, this);
    this.bayLeafClient.send(JSON.stringify(subscriptionMsg));
    return correlationId;
  }

  privateStreamClose(route, subscriptionId, subscription) {
    const closePS = new ApplicationMessage(subscriptionId, MessageType.DATA_CLOSE, this.serviceName, route, MESSAGING_PATTERN.PS, this.codec.encode(subscription));
    console.info(`Close PS ServiceName=${this.serviceName}, Route=${route}, SubscriptionId=${subscriptionId}, Subscription=${subscription}`);
    this.bayLeafClient.send(JSON.stringify(closePS));
    psStreams.delete(subscriptionId);
    this.psCallbacks.delete(subscriptionId);
  }

  sharedStream(route, subscription, callback) {
    const correlationId = uuidv4();
    const subscriptionMsg = new ApplicationMessage(correlationId, MessageType.DATA, this.serviceName, route, MESSAGING_PATTERN.SS, this.codec.encode(subscription));

    console.info(`SS ServiceName=${this.serviceName}, Route=${route}, Subscription=${JSON.stringify(subscriptionMsg)}`);
    this.ssCallbacks.set(correlationId, callback);
    ssStreams.set(correlationId, this);
    this.bayLeafClient.send(JSON.stringify(subscriptionMsg));
    return correlationId;
  }

  sharedStreamClose(route, subscriptionId, subscription) {
    const closeSS = new ApplicationMessage(subscriptionId, MessageType.DATA_CLOSE, this.serviceName, route, MESSAGING_PATTERN.SS, this.codec.encode(subscription));
    console.info(`Close SS ServiceName=${this.serviceName}, Route=${route}, SubscriptionId=${subscriptionId}, Subscription=${subscription}`);
    this.bayLeafClient.send(JSON.stringify(closeSS));
    ssStreams.delete(subscriptionId);
    this.ssCallbacks.delete(subscriptionId);
  }

  broadcast(route, callback) {
    this.broadcastCallbacks.set(route, callback);
    if(broadcasts.has(route)) {
      broadcasts.get(route).add(this);
    } else {
      const broadcastListeners = new Set();
      broadcastListeners.add(this);
      broadcasts.set(route, broadcastListeners);
    }
  }

  onMessage(msg) {
    const messagingPattern = msg.messagingPattern;
    switch(messagingPattern) {
      case MESSAGING_PATTERN.RR:
        if(this.rrPromises.has(msg.correlationId)) {
          if(msg.messageType === MessageType.DATA) {
            const response = this.codec.decode(msg.data);
            this.rrPromises.get(msg.correlationId).success(response);
          } else {
            this.rrPromises.get(msg.correlationId).error(msg);
          }
          this.rrPromises.delete(msg.correlationId);
        } else {
          console.warn(`Unknown RR Response=${msg}`);
        }
      break;
      case MESSAGING_PATTERN.RRA:
        if(this.rraPromises.has(msg.correlationId)) {
          if(msg.messageType === MessageType.DATA) {
            const response = this.codec.decode(msg.data);
            this.rraPromises.get(msg.correlationId).success(response);
          } else {
            this.rraPromises.get(msg.correlationId).error(msg);
          }
          this.rraPromises.delete(msg.correlationId);
        } else {
          console.warn(`Unknown RRA Response=${JSON.stringify(msg)}`);
        }
        break;
      case MESSAGING_PATTERN.PS:
        if(this.psCallbacks.has(msg.correlationId)) {
          if(msg.messageType === MessageType.DATA || msg.messageType === MessageType.INITIAL_DATA) {
            const psStreamData = this.codec.decode(msg.data);
            const callback = msg.messageType === MessageType.DATA ? this.psCallbacks.get(msg.correlationId).data : this.psCallbacks.get(msg.correlationId).initialData;
            callback(psStreamData);
          }
        }
      break;
      case MESSAGING_PATTERN.SS:
        if(this.ssCallbacks.has(msg.correlationId)) {
          if(msg.messageType === MessageType.INITIAL_DATA || msg.messageType === MessageType.DATA) {
            const ssStreamData = this.codec.decode(msg.data);
            const callback = msg.messageType === MessageType.DATA ? this.ssCallbacks.get(msg.correlationId).data : this.ssCallbacks.get(msg.correlationId).initialData;
            callback(ssStreamData);
          }
        }
        break;
      case MESSAGING_PATTERN.BC:
        if(msg.messageType === MessageType.DATA) {
          const broadcast = this.codec.decode(msg.data);
          if(this.broadcastCallbacks.has(msg.route)) {
              const broadcastMsg = this.codec.decode(msg.data);
              const callback = this.broadcastCallbacks.get(msg.route)
              callback(broadcastMsg);
          }
        }
      break;
    }
  }

  onHeartbeat(heartbeat) {
    for(const serviceCallbackListener of this.serviceCallbackListeners) {
        serviceCallbackListener.onHeartbeatReceived();
    }

    const heartbeatOut = new Message(heartbeat.id, MessageType.HEARTBEAT, jsonCodec.encode(new Heartbeat(this.serviceName, heartbeat.id, Date.now())));
    this.bayLeafClient.send(JSON.stringify(heartbeatOut));
  }
}

const createClient = (url, sessionCallbackListener) => {
    url = 'wss://' + url;
    if(wsClientsMap.has(url)) {
        const client = wsClientsMap.get(url);
        client.addSessionCallbackListener(sessionCallbackListener);
        return wsClientsMap.get(url);
    }
    console.info(`Creating client URL=${url}`);
    const client = new BayLeafClient(url, sessionCallbackListener)
    wsClientsMap.set(url, client);
    return client;
}

export { createClient };