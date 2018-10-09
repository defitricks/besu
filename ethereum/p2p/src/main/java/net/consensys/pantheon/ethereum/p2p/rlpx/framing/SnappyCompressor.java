package net.consensys.pantheon.ethereum.p2p.rlpx.framing;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import org.xerial.snappy.Snappy;

/**
 * A strategy for compressing and decompressing data with the Snappy algorithm.
 *
 * @see <a href="https://google.github.io/snappy/">Snappy algorithm</a>
 */
public class SnappyCompressor implements Compressor {

  @Override
  public byte[] compress(final byte[] uncompressed) {
    checkArgument(uncompressed != null, "input data must not be null");
    try {
      return Snappy.compress(uncompressed);
    } catch (final IOException e) {
      throw new CompressionException("Snappy compression failed", e);
    }
  }

  @Override
  public byte[] decompress(final byte[] compressed) {
    checkArgument(compressed != null, "input data must not be null");
    try {
      return Snappy.uncompress(compressed);
    } catch (final IOException e) {
      throw new CompressionException("Snappy decompression failed", e);
    }
  }
}
