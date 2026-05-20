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

        Hydrator<Map<String, Object>, Map<String, Object>, String[]> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() {
                return new HashMap<>();
            }

            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String[] heading, Object value) {
                final HashMap<String, Object> r = new HashMap<>(target);
                r.put(String.join(".", heading), value);
                return r;
            }

            @Override
            public Map<String, Object> finish(Map<String, Object> target) {
                return target;
            }
        };

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, hydrator)) {
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

        Hydrator<Map<String, Object>, Map<String, Object>, String[]> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() {
                return new HashMap<>();
            }

            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String[] heading, Object value) {
                final HashMap<String, Object> r = new HashMap<>(target);
                r.put(String.join(".", heading), value);
                return r;
            }

            @Override
            public Map<String, Object> finish(Map<String, Object> target) {
                return target;
            }
        };

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, hydrator)) {
            final List<Map<String, Object>> result = s.collect(Collectors.toList());

            assertEquals(1036, result.size());
            assertEquals(91, result.get(0).size());

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

    @Test
    public void readMetadata_cur2() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/cur2.parquet")).getFile());

        ParquetMetadata metadata = ParquetReader.readMetadata(parquet);
        MessageType schema = metadata.getFileMetaData().getSchema();

        assertEquals("parquet-mr version 1.12.3 (build a182e7fc0669229eec61273c6c42f359031f239b)",
                metadata.getFileMetaData().getCreatedBy());
        assertNotNull(schema);
        assertEquals(118, schema.getColumns().size());
    }

    /**
     * cur2.parquet contains MAP columns (cost_category, discount, product, resource_tags)
     * which use repeated fields.
     */
    @Test
    public void streamContent_cur2() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/cur2.parquet")).getFile());

        Hydrator<Map<String, Object>, Map<String, Object>, String[]> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() { return new HashMap<>(); }
            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String[] heading, Object value) {
                final HashMap<String, Object> r = new HashMap<>(target);
                r.put(String.join(".", heading), value);
                return r;
            }
            @Override
            public Map<String, Object> finish(Map<String, Object> target) { return target; }
        };

        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, hydrator)) {
            List<Map<String, Object>> rows = s.collect(Collectors.toList());
            assertEquals(13524, rows.size());

            // First row has a product map entry
            Map<String, Object> first = rows.get(0);
            assertEquals("Anniversary", first.get("bill_bill_type"));
            assertEquals("AWS CloudFormation", first.get("product.product_name"));
        }
    }

    @Test
    public void streamContent_cur2_withFieldMapper_selectiveColumns() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/cur2.parquet")).getFile());

        Hydrator<Map<String, Object>, Map<String, Object>, String> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() { return new HashMap<>(); }
            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String heading, Object value) {
                final HashMap<String, Object> r = new HashMap<>(target);
                r.put(heading, value);
                return r;
            }
            @Override
            public Map<String, Object> finish(Map<String, Object> target) { return target; }
        };

        // Only select bill_bill_type and all product map keys
        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, hydrator, path -> {
            if (path.length == 1) {
                if ("bill_bill_type".equals(path[0]) || "product".equals(path[0])) {
                    return path[0];
                }
                return null; // skip other columns
            }
            // Map key: accept all product keys
            return path[0] + "." + path[1];
        })) {
            List<Map<String, Object>> rows = s.collect(Collectors.toList());
            assertEquals(13524, rows.size());

            Map<String, Object> first = rows.get(0);
            assertEquals("Anniversary", first.get("bill_bill_type"));
            assertEquals("AWS CloudFormation", first.get("product.product_name"));
            // Should NOT have columns we didn't ask for
            assertNull(first.get("line_item_currency_code"));
        }
    }

    @Test
    public void streamContent_cur2_withFieldMapper_customHeadings() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/cur2.parquet")).getFile());

        Hydrator<Map<String, Object>, Map<String, Object>, String> hydrator = new Hydrator<>() {
            @Override
            public Map<String, Object> start() { return new HashMap<>(); }
            @Override
            public HashMap<String, Object> add(Map<String, Object> target, String heading, Object value) {
                final HashMap<String, Object> r = new HashMap<>(target);
                r.put(heading, value);
                return r;
            }
            @Override
            public Map<String, Object> finish(Map<String, Object> target) { return target; }
        };

        // Custom headings and selective map keys
        try (Stream<Map<String, Object>> s = ParquetReader.streamContent(parquet, hydrator, path -> {
            if (path.length == 1) {
                if ("bill_bill_type".equals(path[0])) return "bill_type";
                if ("product".equals(path[0])) return "product";
                return null;
            }
            // Only accept product_name from the product map
            if ("product".equals(path[0]) && "product_name".equals(path[1])) {
                return "the_product_name";
            }
            return null; // skip other map keys
        })) {
            List<Map<String, Object>> rows = s.collect(Collectors.toList());
            assertEquals(13524, rows.size());

            Map<String, Object> first = rows.get(0);
            assertEquals("Anniversary", first.get("bill_type"));
            assertEquals("AWS CloudFormation", first.get("the_product_name"));
            assertNull(first.get("product.region"));
            assertNull(first.get("line_item_currency_code"));
            assertNull(first.get("bill_bill_type"));
        }
    }

    /**
     * Demonstrates that fieldMapper headings can be arbitrary Objects (e.g. enums),
     * not just Strings — enabling Clojure keywords or other opaque identifiers.
     */
    @Test
    public void streamContent_cur2_withOpaqueKeys() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/cur2.parquet")).getFile());

        Hydrator<Map<Object, Object>, Map<Object, Object>, Object> hydrator = new Hydrator<>() {
            @Override
            public Map<Object, Object> start() { return new HashMap<>(); }
            @Override
            public Map<Object, Object> add(Map<Object, Object> target, Object heading, Object value) {
                Map<Object, Object> r = new HashMap<>(target);
                r.put(heading, value);
                return r;
            }
            @Override
            public Map<Object, Object> finish(Map<Object, Object> target) { return target; }
        };

        Object BILL_TYPE_KEY = 42;
        Object PRODUCT_NAME_KEY = 99;

        try (Stream<Map<Object, Object>> s = ParquetReader.streamContent(parquet, hydrator, path -> {
            if (path.length == 1) {
                if ("bill_bill_type".equals(path[0])) return BILL_TYPE_KEY;
                if ("product".equals(path[0])) return "product";
                return null;
            }
            if ("product".equals(path[0]) && "product_name".equals(path[1])) return PRODUCT_NAME_KEY;
            return null;
        })) {
            List<Map<Object, Object>> rows = s.collect(Collectors.toList());
            assertEquals(13524, rows.size());

            Map<Object, Object> first = rows.get(0);
            assertEquals("Anniversary", first.get(BILL_TYPE_KEY));
            assertEquals("AWS CloudFormation", first.get(PRODUCT_NAME_KEY));
            assertNull(first.get("bill_bill_type"));
        }
    }

    @Test
    public void streamContentToStrings_mtcars() throws IOException {
        final File parquet = new File(Objects.requireNonNull(
                getClass().getResource("/mtcars.parquet")).getFile());

        try (Stream<String[]> s = ParquetReader.streamContentToStrings(parquet)) {
            List<String[]> result = s.collect(Collectors.toList());
            assertEquals(32, result.size());

            String[] first = result.get(0);
            assertEquals("model=Mazda RX4", first[0]);
            assertEquals("mpg=21.0", first[1]);
        }
    }

}
