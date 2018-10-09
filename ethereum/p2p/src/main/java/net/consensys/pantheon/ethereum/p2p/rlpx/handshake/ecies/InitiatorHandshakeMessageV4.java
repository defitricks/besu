package net.consensys.pantheon.ethereum.p2p.rlpx.handshake.ecies;

import net.consensys.pantheon.crypto.Hash;
import net.consensys.pantheon.crypto.SECP256K1;
import net.consensys.pantheon.ethereum.rlp.BytesValueRLPInput;
import net.consensys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import net.consensys.pantheon.ethereum.rlp.RLPInput;
import net.consensys.pantheon.util.bytes.Bytes32;
import net.consensys.pantheon.util.bytes.Bytes32s;
import net.consensys.pantheon.util.bytes.BytesValue;
import net.consensys.pantheon.util.bytes.MutableBytes32;

public final class InitiatorHandshakeMessageV4 implements InitiatorHandshakeMessage {

  public static final int VERSION = 4;

  private final SECP256K1.PublicKey pubKey;
  private final SECP256K1.Signature signature;
  private final SECP256K1.PublicKey ephPubKey;
  private final Bytes32 ephPubKeyHash;
  private final Bytes32 nonce;

  public static InitiatorHandshakeMessageV4 create(
      final SECP256K1.PublicKey ourPubKey,
      final SECP256K1.KeyPair ephKeyPair,
      final Bytes32 staticSharedSecret,
      final Bytes32 nonce) {
    final MutableBytes32 toSign = MutableBytes32.create();
    Bytes32s.xor(staticSharedSecret, nonce, toSign);
    return new InitiatorHandshakeMessageV4(
        ourPubKey, SECP256K1.sign(toSign, ephKeyPair), ephKeyPair.getPublicKey(), nonce);
  }

  /**
   * Decodes this message.
   *
   * @param bytes The raw bytes.
   * @param keyPair Our keypair to calculat ECDH key agreements.
   * @return The decoded message.
   */
  public static InitiatorHandshakeMessageV4 decode(
      final BytesValue bytes, final SECP256K1.KeyPair keyPair) {
    final RLPInput input = new BytesValueRLPInput(bytes, true);
    input.enterList();
    final SECP256K1.Signature signature = SECP256K1.Signature.decode(input.readBytesValue());
    final SECP256K1.PublicKey pubKey = SECP256K1.PublicKey.create(input.readBytesValue());
    final Bytes32 nonce = input.readBytes32();
    final Bytes32 staticSharedSecret =
        SECP256K1.calculateKeyAgreement(keyPair.getPrivateKey(), pubKey);
    final Bytes32 toSign = Bytes32s.xor(staticSharedSecret, nonce);
    final SECP256K1.PublicKey ephPubKey =
        SECP256K1.PublicKey.recoverFromSignature(toSign, signature)
            .orElseThrow(() -> new RuntimeException("Could not recover public key from signature"));

    return new InitiatorHandshakeMessageV4(pubKey, signature, ephPubKey, nonce);
  }

  private InitiatorHandshakeMessageV4(
      final SECP256K1.PublicKey pubKey,
      final SECP256K1.Signature signature,
      final SECP256K1.PublicKey ephPubKey,
      final Bytes32 nonce) {
    this.pubKey = pubKey;
    this.signature = signature;
    this.ephPubKey = ephPubKey;
    this.ephPubKeyHash = Hash.keccak256(ephPubKey.getEncodedBytes());
    this.nonce = nonce;
  }

  @Override
  public BytesValue encode() {
    final BytesValueRLPOutput temp = new BytesValueRLPOutput();
    temp.startList();
    temp.writeBytesValue(signature.encodedBytes());
    temp.writeBytesValue(pubKey.getEncodedBytes());
    temp.writeBytesValue(nonce);
    temp.writeIntScalar(VERSION);
    temp.endList();
    return temp.encoded();
  }

  @Override
  public Bytes32 getNonce() {
    return nonce;
  }

  @Override
  public SECP256K1.PublicKey getPubKey() {
    return pubKey;
  }

  @Override
  public SECP256K1.PublicKey getEphPubKey() {
    return ephPubKey;
  }

  @Override
  public Bytes32 getEphPubKeyHash() {
    return ephPubKeyHash;
  }
}
