package org.apache.hadoop.io.compress;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Stub interface for DirectDecompressor to satisfy parquet-hadoop without a real Hadoop dependency.
 */
public interface DirectDecompressor {
    void decompress(ByteBuffer src, ByteBuffer dst) throws IOException;
}
