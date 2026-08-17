package org.apache.hadoop.io.compress;

/**
 * Stub interface for DirectDecompressionCodec to satisfy parquet-hadoop without a real Hadoop dependency.
 */
public interface DirectDecompressionCodec extends CompressionCodec {
    DirectDecompressor createDirectDecompressor();
}
