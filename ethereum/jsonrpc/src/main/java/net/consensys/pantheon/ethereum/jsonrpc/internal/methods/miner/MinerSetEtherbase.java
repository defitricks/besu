package net.consensys.pantheon.ethereum.jsonrpc.internal.methods.miner;

import net.consensys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import net.consensys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;

public class MinerSetEtherbase implements JsonRpcMethod {

  private final MinerSetCoinbase minerSetCoinbaseMethod;

  public MinerSetEtherbase(final MinerSetCoinbase minerSetCoinbaseMethod) {

    this.minerSetCoinbaseMethod = minerSetCoinbaseMethod;
  }

  @Override
  public String getName() {
    return "miner_setEtherbase";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest req) {
    return minerSetCoinbaseMethod.response(req);
  }
}
