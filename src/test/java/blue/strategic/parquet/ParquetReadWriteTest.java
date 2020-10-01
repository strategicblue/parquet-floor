package blue.strategic.parquet;

import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

        ParquetRecord.Builder b = ParquetRecord.builder(schema);

        try(ParquetWriter writer = ParquetWriter.writeFile(schema, parquet)) {
            writer.write(b.build(new Object[]{1L, "hello1"}));
            writer.write(b.build(new Object[]{2L, "hello2"}));
        }

        try (Stream<Object[]> s = ParquetReader.readFile(parquet)) {
            List<Object[]> result = s.collect(Collectors.toList());

            assertThat(result, hasItems(
                    new Object[] {"1", "hello1"},
                    new Object[] {"2", "hello2"}));
        }
    }
}