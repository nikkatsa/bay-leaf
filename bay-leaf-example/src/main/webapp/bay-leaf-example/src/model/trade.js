
class TradeRequest {

    constructor(id, symbol, quantity, price, side) {
        this.id=  id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
    }
}

class TradeResponse {
    constructor(id, isDone, rejectionMsg) {
        this.id = id;
        this.isDone = isDone;
        this.rejectionMsg = rejectionMsg;
    }
}

class TradeBlotterRequest {
    constructor() {

    }
}
export {TradeRequest, TradeResponse, TradeBlotterRequest };