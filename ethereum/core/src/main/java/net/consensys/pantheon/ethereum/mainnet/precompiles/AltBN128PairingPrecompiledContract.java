package net.consensys.pantheon.ethereum.mainnet.precompiles;

import net.consensys.pantheon.crypto.altbn128.AltBn128Fq12Pairer;
import net.consensys.pantheon.crypto.altbn128.AltBn128Fq2Point;
import net.consensys.pantheon.crypto.altbn128.AltBn128Point;
import net.consensys.pantheon.crypto.altbn128.Fq;
import net.consensys.pantheon.crypto.altbn128.Fq12;
import net.consensys.pantheon.crypto.altbn128.Fq2;
import net.consensys.pantheon.ethereum.core.Gas;
import net.consensys.pantheon.ethereum.mainnet.AbstractPrecompiledContract;
import net.consensys.pantheon.ethereum.vm.GasCalculator;
import net.consensys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AltBN128PairingPrecompiledContract extends AbstractPrecompiledContract {

  private static final int FIELD_LENGTH = 32;
  private static final int PARAMETER_LENGTH = 192;

  private static final BytesValue FALSE =
      BytesValue.fromHexString(
          "0x0000000000000000000000000000000000000000000000000000000000000000");
  private static final BytesValue TRUE =
      BytesValue.fromHexString(
          "0x0000000000000000000000000000000000000000000000000000000000000001");

  public AltBN128PairingPrecompiledContract(final GasCalculator gasCalculator) {
    super("AltBN128Pairing", gasCalculator);
  }

  @Override
  public Gas gasRequirement(final BytesValue input) {
    final int parameters = input.size() / PARAMETER_LENGTH;
    return Gas.of(80_000L).times(Gas.of(parameters)).plus(Gas.of(100_000L));
  }

  @Override
  public BytesValue compute(final BytesValue input) {
    if (input.isEmpty()) {
      return TRUE;
    }
    if (input.size() % PARAMETER_LENGTH != 0) {
      return null;
    }

    final int parameters = input.size() / PARAMETER_LENGTH;
    final List<AltBn128Point> a = new ArrayList<>();
    final List<AltBn128Fq2Point> b = new ArrayList<>();
    for (int i = 0; i < parameters; ++i) {
      final BigInteger p1_x = extractParameter(input, i * PARAMETER_LENGTH, FIELD_LENGTH);
      final BigInteger p1_y = extractParameter(input, i * PARAMETER_LENGTH + 32, FIELD_LENGTH);
      final AltBn128Point p1 = new AltBn128Point(Fq.create(p1_x), Fq.create(p1_y));
      if (!p1.isOnCurve()) {
        return null;
      }
      a.add(p1);

      final BigInteger p2_xImag = extractParameter(input, i * PARAMETER_LENGTH + 64, FIELD_LENGTH);
      final BigInteger p2_xReal = extractParameter(input, i * PARAMETER_LENGTH + 96, FIELD_LENGTH);
      final BigInteger p2_yImag = extractParameter(input, i * PARAMETER_LENGTH + 128, FIELD_LENGTH);
      final BigInteger p2_yReal = extractParameter(input, i * PARAMETER_LENGTH + 160, FIELD_LENGTH);
      final Fq2 p2_x = Fq2.create(p2_xReal, p2_xImag);
      final Fq2 p2_y = Fq2.create(p2_yReal, p2_yImag);
      final AltBn128Fq2Point p2 = new AltBn128Fq2Point(p2_x, p2_y);
      if (!p2.isOnCurve()) {
        return null;
      }
      b.add(p2);
    }

    Fq12 exponent = Fq12.one();
    for (int i = 0; i < parameters; ++i) {
      exponent = exponent.multiply(AltBn128Fq12Pairer.pair(a.get(i), b.get(i)));
    }

    if (AltBn128Fq12Pairer.finalize(exponent).equals(Fq12.one())) {
      return TRUE;
    } else {
      return FALSE;
    }
  }

  private static BigInteger extractParameter(
      final BytesValue input, final int offset, final int length) {
    if (offset > input.size() || length == 0) {
      return BigInteger.ZERO;
    }
    final byte[] raw = Arrays.copyOfRange(input.extractArray(), offset, offset + length);
    return new BigInteger(1, raw);
  }
}
