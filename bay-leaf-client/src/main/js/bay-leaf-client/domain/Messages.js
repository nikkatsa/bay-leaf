import MessageType from './MessageType';

class Message {

    constructor(correlationId, messageType, data) {
        this.correlationId = correlationId;
        this.messageType = messageType;
        this.data = data;
    }
}

class ApplicationMessage extends Message {

    constructor(correlationId, messageType, serviceName, route, messagingPattern, data) {
        super(correlationId, messageType, data);
        this.serviceName = serviceName;
        this.route = route;
        this.messagingPattern = messagingPattern;
    }
}

class ErrorMessage extends ApplicationMessage {

    constructor(errorCode, errorMsg, correlationId, serviceName, route, messagingPattern) {
        super(correlationId, MessageType.ERROR, serviceName, route, messagingPattern, null);
        this.errorCode = 0;
        this.errorMsg = undefined;
    }
}

class RRPromise {

    constructor() {
        let onSuccess, onError;
        this.promise = new Promise((success, error) => {
            onSuccess = success;
            onError = error;
        });
        this.onSuccessCallback = onSuccess;
        this.onErrorCallback = onError;
    }

    success(response) {
        this.onSuccessCallback(response);
    }

    error(error) {
        this.onErrorCallback(error)
    }

    getPromise() {
        return this.promise;
    }
}

class RRAPromise extends RRPromise {

    /**
     * A RRAPromise extending RRPromise and adding functionality for sending back an ack
     * 
     * @param {ApplicationMessage} ackMessage 
     * @param {function} ackMessageConsumer The function to be called with the ackMessage as the parameter. This function is the action when client calls the ack() method upon a success
     */
    constructor(ackMessage, ackMessageConsumer) {
        super();
        this.ackMessage = ackMessage;
        this.ackMessageConsumer = ackMessageConsumer
    }

    success(response) {
        super.success({ response: response, ack: () => { this.onAck(); } });
    }

    onAck() {
        this.ackMessageConsumer(this.ackMessage);
    }
}

const MESSAGING_PATTERN = Object.freeze({
    RR: 'RR',
    RRA: 'RRA',
    PS: 'PS',
    SS: 'SS',
    BC: 'BC',
});

export { Message, ErrorMessage, ApplicationMessage, MESSAGING_PATTERN, RRPromise, RRAPromise };