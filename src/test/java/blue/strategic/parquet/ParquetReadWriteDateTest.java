package blue.strategic.parquet;

import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParquetReadWriteDateTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    Hydrator<Map<String, Object>, Map<String, Object>> HYDRATOR = new Hydrator<>() {
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

    @Test
    public void date_roundtrip() throws IOException {
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.of(2026, Month.MAY, 10);

        MessageType schema = new MessageType("test",
                Types.required(INT32)
                        .as(LogicalTypeAnnotation.dateType()).named("date"),
                Types.required(INT64).named("ts"));

        Dehydrator<Object[]> dehydrator = (record, writer) -> {
            writer.write("date", record[0]);
            writer.write("ts", record[1]);
        };

        File file = new File(folder.getRoot(), "date.parquet");
        try (ParquetWriter<Object[]> writer = ParquetWriter.writeFile(schema, file, dehydrator)) {
            writer.write(new Object[]{date1, 1000L});
            writer.write(new Object[]{date2, 2000L});
        }


        try (Stream<Map<String, Object>> s =
                     ParquetReader.streamContent(file, HydratorSupplier.constantly(HYDRATOR))) {
            List<Map<String, Object>> rows = s.collect(Collectors.toList());

            assertThat(rows.size(), is(2));
            assertThat(rows.get(0).get("date"), is(date1));
            assertThat(rows.get(1).get("date"), is(date2));
            assertThat(rows.get(0).get("ts"), is(1000L));
        }
    }

    /**
     * You can use a java.util.Date as input. However, the output will always be a LocalDate.
     */
    @Test
    public void date2localDate() throws IOException {
        Date date1 = new Date(1778371200000L);
        Date date2 = new Date(1054166400000L);

        LocalDate expectedDate1 = LocalDate.of(2026, Month.MAY, 10);
        LocalDate expectedDate2 = LocalDate.of(2003, Month.MAY, 29);

        MessageType schema = new MessageType("test",
                Types.required(INT32)
                        .as(LogicalTypeAnnotation.dateType()).named("date"),
                Types.required(INT64).named("ts"));

        Dehydrator<Object[]> dehydrator = (record, writer) -> {
            writer.write("date", record[0]);
            writer.write("ts", record[1]);
        };

        File file = new File(folder.getRoot(), "date.parquet");
        try (ParquetWriter<Object[]> writer = ParquetWriter.writeFile(schema, file, dehydrator)) {
            writer.write(new Object[]{date1, 1000L});
            writer.write(new Object[]{date2, 2000L});
        }

        try (Stream<Map<String, Object>> s =
                     ParquetReader.streamContent(file, HydratorSupplier.constantly(HYDRATOR))) {
            List<Map<String, Object>> rows = s.collect(Collectors.toList());
            assertThat(rows.size(), is(2));
            assertThat(rows.get(0).get("date"), is(expectedDate1));
            assertThat(rows.get(1).get("date"), is(expectedDate2));
            assertThat(rows.get(0).get("ts"), is(1000L));
        }
    }
}

