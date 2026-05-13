package blue.strategic.parquet;

import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Types;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.*;
import static org.junit.Assert.*;

/**
 * These tests use real world Parquet files from
 * <a href="https://www.agentsfordata.com/parquet/sample">https://www.agentsfordata.com/parquet/sample</a>
 * <p>
 * "These sample files are perfect for learning data analysis, testing your scripts, or practicing with new tools.
 * They're free to download and use."
 */
public class RealWorldTests {

    @Test
    public void streamContent_mtcars() throws IOException {

        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/mtcars.parquet")).getFile());

        Hydrator<Map<String, Object>, Map<String, Object>> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() {
                return new HashMap<>();
            }

            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String heading, Object value) {
                final HashMap<String, Object> r = new HashMap<>(target);
                r.put(heading, value);
                return r;
            }

            @Override
            public Map<String, Object> finish(Map<String, Object> target) {
                return target;
            }
        };

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, HydratorSupplier.constantly(hydrator))) {
            final List<Map<String, Object>> result = s.collect(Collectors.toList());

            // the dataset contains be 32 rows
            assertEquals(32, result.size());

            // check some types
            for (Map<String, Object> it : result) {
                assertEquals(String.class, it.get("model").getClass());
                assertEquals(Double.class, it.get("mpg").getClass());
                assertEquals(Integer.class, it.get("cyl").getClass());
            }

            // make sure that all columns are present
            assertEquals(Set.of("model", "mpg", "cyl", "disp", "hp", "drat", "wt", "qsec", "vs", "am", "gear", "carb"),
                    result.get(0).keySet());

            // check one entry completely
            final Map<String, Object> entry = result.get(0);
            assertEquals("Mazda RX4", entry.get("model"));
            assertEquals(21., entry.get("mpg"));
            assertEquals(6, entry.get("cyl"));
            assertEquals(160., entry.get("disp"));
            assertEquals(110, entry.get("hp"));
            assertEquals(3.9, entry.get("drat"));
            assertEquals(2.62, entry.get("wt"));
            assertEquals(16.46, entry.get("qsec"));
            assertEquals(0, entry.get("vs"));
            assertEquals(1, entry.get("am"));
            assertEquals(4, entry.get("gear"));
            assertEquals(4, entry.get("carb"));
        }
    }

    @Test
    public void readMetadata_mtcars() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/mtcars.parquet")).getFile());

        ParquetMetadata metadata = ParquetReader.readMetadata(parquet);
        MessageType schema = metadata.getFileMetaData().getSchema();

        assertEquals("DuckDB", metadata.getFileMetaData().getCreatedBy());
        assertEquals(FileMetaData.EncryptionType.UNENCRYPTED, metadata.getFileMetaData().getEncryptionType());
        assertTrue(metadata.getFileMetaData().getKeyValueMetaData().isEmpty());
        assertNotNull(schema);

        List<ColumnDescriptor> expectedColumns = List.of(
                new ColumnDescriptor(
                        new String[]{"model"},
                        Types.required(BINARY).as(LogicalTypeAnnotation.stringType()).named("model"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"mpg"},
                        Types.optional(DOUBLE).named("mpg"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"cyl"},
                        Types.optional(INT32).named("cyl"),
                        0, 1), new ColumnDescriptor(
                        new String[]{"disp"},
                        Types.optional(DOUBLE).named("disp"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"hp"},
                        Types.optional(INT32).named("hp"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"drat"},
                        Types.optional(DOUBLE).named("drat"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"wt"},
                        Types.optional(DOUBLE).named("wt"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"qsec"},
                        Types.optional(DOUBLE).named("qsec"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"vs"},
                        Types.optional(INT32).named("vs"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"am"},
                        Types.optional(INT32).named("am"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"gear"},
                        Types.optional(INT32).named("gear"),
                        0, 1),
                new ColumnDescriptor(
                        new String[]{"carb"},
                        Types.optional(INT32).named("carb"),
                        0, 1)
        );

        assertEquals(expectedColumns, schema.getColumns());
    }

    @Test
    public void streamContent_cur1() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/cur1.parquet")).getFile());

        Hydrator<Map<String, Object>, Map<String, Object>> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() {
                return new HashMap<>();
            }

            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String heading, Object value) {
                final HashMap<String, Object> r = new HashMap<>(target);
                r.put(heading, value);
                return r;
            }

            @Override
            public Map<String, Object> finish(Map<String, Object> target) {
                return target;
            }
        };

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, HydratorSupplier.constantly(hydrator))) {
            final List<Map<String, Object>> result = s.collect(Collectors.toList());

            assertEquals(1036, result.size());
            assertEquals(91, result.get(0).keySet().size());

            // check types: String, Double, and Long columns
            Map<String, Object> first = result.get(0);
            assertEquals(String.class, first.get("bill_bill_type").getClass());
            assertEquals(Double.class, first.get("line_item_usage_amount").getClass());
            assertEquals(Long.class, first.get("bill_billing_period_start_date").getClass());

            // check first row values
            assertEquals("Anniversary", first.get("bill_bill_type"));
            assertEquals("AWS", first.get("bill_billing_entity"));
            assertEquals("Tax", first.get("line_item_line_item_type"));
            assertEquals("AWSDataTransfer", first.get("line_item_product_code"));
            assertEquals("USD", first.get("line_item_currency_code"));
            assertEquals("AWS EMEA SARL", first.get("line_item_legal_entity"));
            assertEquals(1.0, first.get("line_item_usage_amount"));
            assertEquals(0.0, first.get("line_item_unblended_cost"));
            assertEquals(1622505600000L, first.get("bill_billing_period_start_date"));
            assertEquals(1625097600000L, first.get("bill_billing_period_end_date"));
        }
    }

    @Test
    public void readMetadata_cur1() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/cur1.parquet")).getFile());

        ParquetMetadata metadata = ParquetReader.readMetadata(parquet);
        MessageType schema = metadata.getFileMetaData().getSchema();

        assertEquals("parquet-mr version 1.10.1 (build 815bcfa4a4aacf66d207b3dc692150d16b5740b9)",
                metadata.getFileMetaData().getCreatedBy());
        assertNotNull(schema);
        assertEquals(91, schema.getColumns().size());
        assertTrue(metadata.getFileMetaData().getKeyValueMetaData()
                .containsKey("org.apache.spark.sql.parquet.row.metadata"));
    }

}
