package blue.strategic.parquet;

import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.DelegatingPositionOutputStream;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ParquetWriter implements Closeable {

    private final org.apache.parquet.hadoop.ParquetWriter<Group> writer;
    private final MessageType schema;

    public static ParquetWriter writeFile(MessageType schema, File out) throws IOException {
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
        return writeOutputFile(schema, f);
    }

    private static ParquetWriter writeOutputFile(MessageType schema, OutputFile file) throws IOException {
        return new ParquetWriter(file, schema);
    }

    private static void writeValue(Group group, String heading, PrimitiveType.PrimitiveTypeName type, Object value) {
        switch (type) {
        case INT64:
            group.add(heading, (long) value);
            break;
        case INT32:
            group.add(heading, (int) value);
            break;
        case BOOLEAN:
            group.add(heading, (boolean) value);
            break;
        case FLOAT:
            group.add(heading, (float) value);
            break;
        case DOUBLE:
            group.add(heading, (double) value);
            break;
        case BINARY:
        case FIXED_LEN_BYTE_ARRAY:
        case INT96:
            group.add(heading, Binary.fromString((String) value));
            break;
        default:
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private ParquetWriter(OutputFile outputFile, MessageType schema) throws IOException {
        this.schema = schema;
        this.writer = ExampleParquetWriter.builder(outputFile)
                .withType(schema)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build();
    }

    public void write(ParquetRecord record) throws IOException {
        SimpleGroup a = new SimpleGroup(this.schema);

        record.forEach((heading, type, value) -> {
            writeValue(a, heading, type, value);
        });

        writer.write(a);
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

}
