package blue.strategic.parquet;

import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.ColumnReadStore;
import org.apache.parquet.column.ColumnReader;
import org.apache.parquet.column.impl.ColumnReadStoreImpl;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.DummyRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.io.DelegatingSeekableInputStream;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveStringifier;
import org.apache.parquet.schema.PrimitiveType;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ParquetReader {

    public static Stream<Object[]> readFile(File file) throws IOException {
        InputFile f = new InputFile() {
            @Override
            public long getLength() {
                return file.length();
            }

            @Override
            public SeekableInputStream newStream() throws IOException {
                FileInputStream fis = new FileInputStream(file);
                return new DelegatingSeekableInputStream(fis) {
                    long position = 0L;

                    @Override
                    public long getPos() {
                        return position;
                    }

                    @Override
                    public void seek(long newPos) throws IOException {
                        fis.getChannel().position(newPos);
                        position = newPos;
                    }
                };
            }
        };
        return readInputFile(f);
    }

    private static Stream<Object[]> readInputFile(InputFile file) throws IOException {
        ParquetFileReader reader = ParquetFileReader.open(file);
        return StreamSupport
                .stream(new PqSpliterator(reader, Collections.emptySet()), false)
                .onClose(() -> closeSilently(reader));
    }

    private static void closeSilently (Closeable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private static class PqSpliterator implements Spliterator<Object[]> {
        private final ParquetFileReader reader;
        private final List<ColumnDescriptor> columns;
        private final MessageType schema;
        private final GroupConverter recordConverter;
        private final String createdBy;

        private boolean finished = false;
        private PageReadStore currentRowGroup;
        private List<ColumnReader> currentRowGroupColumnReaders;
        private long currentRowGroupSize = -1L;
        private long currentRowIndex = -1L;

        public PqSpliterator(ParquetFileReader reader, Set<String> columnNames) {
            this.reader = reader;

            FileMetaData meta = reader.getFooter().getFileMetaData();
            this.schema = meta.getSchema();
            this.recordConverter = new DummyRecordConverter(this.schema).getRootConverter();
            this.createdBy = meta.getCreatedBy();

            this.columns = schema.getColumns().stream()
                    .filter(c -> columnNames.isEmpty() || columnNames.contains(Arrays.deepToString(c.getPath())))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean tryAdvance(Consumer<? super Object[]> action) {
            try {
                if (this.finished) {
                    return false;
                }

                if (currentRowIndex == currentRowGroupSize) {
                    PageReadStore rowGroup = reader.readNextRowGroup();
                    if (rowGroup == null) {
                        this.finished = true;
                        return false;
                    }

                    ColumnReadStore columnReadStore = new ColumnReadStoreImpl(rowGroup, this.recordConverter, this.schema, this.createdBy);

                    this.currentRowGroup = rowGroup;
                    this.currentRowGroupSize = this.currentRowGroup.getRowCount();
                    this.currentRowGroupColumnReaders = columns.stream().map(columnReadStore::getColumnReader).collect(Collectors.toList());
                    this.currentRowIndex = 0L;
                }

                action.accept(
                        this.currentRowGroupColumnReaders.stream()
                        .map(columnReader -> {
                            String value = readValue(columnReader);
//                            String[] columnName = columnReader.getDescriptor().getPath();
                            columnReader.consume();
                            if (columnReader.getCurrentRepetitionLevel() != 0) {
                                throw new IllegalStateException("Unexpected repetition");
                            }
                            return value;
                        }).toArray());

                this.currentRowIndex ++;

                return true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read parquet", e);
            }
        }

        @Override
        public Spliterator<Object[]> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return reader.getRecordCount();
        }

        @Override
        public int characteristics() {
            return ORDERED | NONNULL | DISTINCT;
        }
    }

    private static String readValue(ColumnReader columnReader) {
        ColumnDescriptor column = columnReader.getDescriptor();
        PrimitiveType primitiveType = column.getPrimitiveType();
        PrimitiveStringifier stringifier = primitiveType.stringifier();
        int maxDefinitionLevel = column.getMaxDefinitionLevel();

        if (columnReader.getCurrentDefinitionLevel() == maxDefinitionLevel) {
            switch (primitiveType.getPrimitiveTypeName()) {
                case BINARY:
                case FIXED_LEN_BYTE_ARRAY:
                case INT96:
                    return stringifier.stringify(columnReader.getBinary());
                case BOOLEAN:
                    return stringifier.stringify(columnReader.getBoolean());
                case DOUBLE:
                    return stringifier.stringify(columnReader.getDouble());
                case FLOAT:
                    return stringifier.stringify(columnReader.getFloat());
                case INT32:
                    return stringifier.stringify(columnReader.getInteger());
                case INT64:
                    return stringifier.stringify(columnReader.getLong());
                default:
                    throw new IllegalArgumentException("Unsupported type: " + primitiveType);
            }
        } else {
            return null;
        }
    }
}
