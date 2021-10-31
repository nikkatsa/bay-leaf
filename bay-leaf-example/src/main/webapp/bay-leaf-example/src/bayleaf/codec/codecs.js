
const utfEncoder = new TextEncoder();
const utfDecoder = new TextDecoder();

class JsonCodec {

  /**
  * msg -> byte[]
  **/
  encode(msg) {
    return Array.from(utfEncoder.encode(JSON.stringify(msg)));
  }

  /**
  * byte[] -> msg
  **/
  decode(bytes) {
    return JSON.parse(atob(bytes))
  }
}

class StringCodec {

  encode(msg) {
    return Array.from(utfEncoder.encode(msg));
  }

  decode(bytes) {
    return atob(bytes);
  }
}

export {JsonCodec, StringCodec};