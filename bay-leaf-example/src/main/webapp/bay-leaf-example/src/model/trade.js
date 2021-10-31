
class TradeRequest {

    constructor(id, symbol, quantity, price) {
        this.id=  id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
    }
}

class TradeResponse {
    constructor(id, isDone, rejectionMsg) {
        this.id = id;
        this.isDone = isDone;
        this.rejectionMsg = rejectionMsg;
    }
}

export {TradeRequest, TradeResponse};