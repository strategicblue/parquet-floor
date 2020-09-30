package blue.strategic.parquet;

import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.DelegatingPositionOutputStream;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Types;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64;

public class ParquetWriter {

    public static void writeFile(File out) throws IOException {
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
        writeOutputFile(f);
    }

    private static void writeOutputFile(OutputFile file) throws IOException {
        MessageType schema = new MessageType("foo",
                Types.required(INT64).named("id"),
                Types.required(BINARY).as(LogicalTypeAnnotation.stringType()).named("email")
        );

        ExampleParquetWriter.Builder writerBuilder = ExampleParquetWriter.builder(file)
                .withType(schema)
                .withCompressionCodec(CompressionCodecName.SNAPPY);

        try (org.apache.parquet.hadoop.ParquetWriter<Group> writer = writerBuilder.build()) {
            SimpleGroup g = new SimpleGroup(schema);
            g.add("id", 1L);
            g.add("email", Binary.fromString("hello"));
            writer.write(g);
        }
    }
}
