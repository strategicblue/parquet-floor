package org.apache.hadoop.io.compress.zlib;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.Compressor;

import java.io.IOException;

/**
 * Stub implementation of ZlibCompressor to satisfy parquet-hadoop without a real Hadoop dependency.
 */
public class ZlibCompressor implements Compressor {

    public enum CompressionLevel {
        NO_COMPRESSION,
        BEST_SPEED,
        BEST_COMPRESSION,
        DEFAULT_COMPRESSION
    }

    public enum CompressionStrategy {
        DEFAULT_STRATEGY,
        FILTERED,
        HUFFMAN_ONLY
    }

    public enum CompressionHeader {
        NO_HEADER,
        DEFAULT_HEADER,
        GZIP_FORMAT,
        AUTODETECT_GZIP_ZLIB
    }

    public ZlibCompressor() {
    }

    public ZlibCompressor(CompressionLevel level, CompressionStrategy strategy, CompressionHeader header, int bufferSize) {
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public boolean needsInput() {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public void setDictionary(byte[] b, int off, int len) {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public long getBytesRead() {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public long getBytesWritten() {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public void finish() {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public boolean finished() {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public int compress(byte[] b, int off, int len) throws IOException {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public void end() {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }

    @Override
    public void reinit(Configuration conf) {
        throw new UnsupportedOperationException("ZlibCompressor is not supported in parquet-floor");
    }
}
