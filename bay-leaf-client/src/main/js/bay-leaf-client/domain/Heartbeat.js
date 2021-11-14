class Heartbeat {

    constructor(serviceName, heartbeatId, timestamp) {
        this.serviceName = serviceName;
        this.id = heartbeatId;
        this.timestamp = timestamp;
    }
}

export default Heartbeat;