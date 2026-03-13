package blue.strategic.parquet;

import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParquetReadWriteTest {
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void writes_and_reads_parquet() throws IOException {
        File parquet = new File(folder.getRoot(), "foo.parquet");

        MessageType schema = new MessageType("foo",
                Types.required(INT64).named("id"),
                Types.required(BINARY).as(LogicalTypeAnnotation.stringType()).named("email")
        );

        Dehydrator<Object[]> dehydrator = (record, valueWriter) -> {
            valueWriter.write("id", record[0]);
            valueWriter.write("email", record[1]);
        };

        Hydrator<Map<String, Object>, Map<String, Object>> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() {
                return new HashMap<>();
            }

            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String heading, Object value) {
                HashMap<String, Object> r = new HashMap<>(target);
                r.put(heading, value);
                return r;
            }

            @Override
            public Map<String, Object> finish(Map<String, Object> target) {
                return target;
            }
        };

        try(ParquetWriter<Object[]> writer = ParquetWriter.writeFile(schema, parquet, dehydrator)) {
            writer.write(new Object[]{1L, "hello1"});
            writer.write(new Object[]{2L, "hello2"});
        }

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, HydratorSupplier.constantly(hydrator))) {
            List<Map<String, Object>> result = s.collect(Collectors.toList());

            //noinspection unchecked
            assertThat(result, hasItems(
                    Map.of("id", 1L, "email", "hello1"),
                    Map.of("id", 2L, "email", "hello2")));
        }

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, HydratorSupplier.constantly(hydrator), Collections.singleton("id"))) {
            List<Map<String, Object>> result = s.collect(Collectors.toList());

            //noinspection unchecked
            assertThat(result, hasItems(
                    Map.of("id", 1L),
                    Map.of("id", 2L)));
        }
    }

    @Test
    public void writes_and_reads_uuid() throws IOException {
        File parquet = new File(folder.getRoot(), "foo.parquet");

        MessageType schema = new MessageType("foo",
                Types.required(FIXED_LEN_BYTE_ARRAY).length(16).as(LogicalTypeAnnotation.uuidType()).named("id"),
                Types.required(BINARY).as(LogicalTypeAnnotation.stringType()).named("name")
        );

        Dehydrator<MyEntry> dehydrator = (record, valueWriter) -> {
            byte[] uuidBytes = ByteBuffer.allocate(16)
                    .putLong(record.id.getMostSignificantBits())
                    .putLong(record.id.getLeastSignificantBits())
                    .array();

            valueWriter.write("id", uuidBytes);
            valueWriter.write("name", record.name);
        };


        try (ParquetWriter<MyEntry> writer = ParquetWriter.writeFile(schema, parquet, dehydrator)) {
            writer.write(new MyEntry(UUID.fromString("92d600d9-8581-42c4-92e6-ac41c85801a8"), "hello1"));
            writer.write(new MyEntry(UUID.fromString("177e54e9-1a40-4646-ac7c-998efd47b810"), "hello2"));
        }

        Hydrator<Map<String, Object>, Map<String, Object>> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() {
                return new HashMap<>();
            }

            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String heading, Object value) {
                HashMap<String, Object> r = new HashMap<>(target);
                r.put(heading, value);
                return r;
            }

            @Override
            public Map<String, Object> finish(Map<String, Object> target) {
                return target;
            }
        };

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, HydratorSupplier.constantly(hydrator))) {
            List<Map<String, Object>> result = s.collect(Collectors.toList());

            //noinspection unchecked
            assertThat(result, hasItems(
                    Map.of("id", UUID.fromString("92d600d9-8581-42c4-92e6-ac41c85801a8"), "name", "hello1"),
                    Map.of("id", UUID.fromString("177e54e9-1a40-4646-ac7c-998efd47b810"), "name", "hello2")));
        }
    }

    static class MyEntry {
        UUID id;
        String name;

        MyEntry( UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}