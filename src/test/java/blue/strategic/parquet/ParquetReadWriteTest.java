package blue.strategic.parquet;

import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.BINARY;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParquetReadWriteTest {
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void testUsingTempFolder() throws IOException {
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

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, hydrator)) {
            List<Map<String, Object>> result = s.collect(Collectors.toList());

            //noinspection unchecked
            assertThat(result, hasItems(
                    Map.of("id", 1L, "email", "hello1"),
                    Map.of("id", 2L, "email", "hello2")));
        }

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, hydrator, Collections.singleton("id"))) {
            List<Map<String, Object>> result = s.collect(Collectors.toList());

            //noinspection unchecked
            assertThat(result, hasItems(
                    Map.of("id", 1L),
                    Map.of("id", 2L)));
        }
    }
}