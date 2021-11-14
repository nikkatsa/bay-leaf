const client = require("./bayleafClient");
const codecs = require("./codec/codecs");

const createClient = client.createClient;
const StringCodec = codecs.StringCodec;
const JsonCodec = codecs.JsonCodec;

module.exports = { createClient, StringCodec, JsonCodec };