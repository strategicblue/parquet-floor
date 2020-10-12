package blue.strategic.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.DelegatingPositionOutputStream;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.schema.MessageType;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ParquetWriter implements Closeable {

    private final org.apache.parquet.hadoop.ParquetWriter<Object> writer;

    public static ParquetWriter writeFile(MessageType schema, File out, Dehydrator dehydrator) throws IOException {
        OutputFile f = new OutputFile() {
            @Override
            public PositionOutputStream create(long blockSizeHint) throws IOException {
                return createOrOverwrite(blockSizeHint);
            }

            @Override
            public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
                FileOutputStream fos = new FileOutputStream(out);
                return new DelegatingPositionOutputStream(fos) {
                    @Override
                    public long getPos() throws IOException {
                        return fos.getChannel().position();
                    }
                };
            }

            @Override
            public boolean supportsBlockSize() {
                return false;
            }

            @Override
            public long defaultBlockSize() {
                return 1024L;
            }
        };
        return writeOutputFile(schema, f, dehydrator);
    }

    private static ParquetWriter writeOutputFile(MessageType schema, OutputFile file, Dehydrator dehydrator) throws IOException {
        return new ParquetWriter(file, schema, dehydrator);
    }

    private ParquetWriter(OutputFile outputFile, MessageType schema, Dehydrator dehydrator) throws IOException {
        this.writer = new Builder(outputFile)
                .withType(schema)
                .withDehydrator(dehydrator)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build();
    }

    public void write(Object record) throws IOException {
        writer.write(record);
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

    private static class Builder extends org.apache.parquet.hadoop.ParquetWriter.Builder<Object, ParquetWriter.Builder> {
        private MessageType schema;
        private Dehydrator dehydrator;

        private Builder(OutputFile file) {
            super(file);
        }

        public ParquetWriter.Builder withType(MessageType schema) {
            this.schema = schema;
            return this;
        }

        public ParquetWriter.Builder withDehydrator(Dehydrator dehydrator) {
            this.dehydrator = dehydrator;
            return this;
        }

        @Override
        protected ParquetWriter.Builder self() {
            return this;
        }

        @Override
        protected WriteSupport<Object> getWriteSupport(Configuration conf) {
            return new Writificator(schema, dehydrator);
        }
    }
}
