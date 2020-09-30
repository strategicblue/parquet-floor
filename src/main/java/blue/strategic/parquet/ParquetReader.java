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
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveStringifier;
import org.apache.parquet.schema.PrimitiveType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParquetReader {

    public static Stream<Object> readFile(File file) throws IOException {
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

    private static Stream<Object> readInputFile(InputFile file) throws IOException {
        try (ParquetFileReader reader = ParquetFileReader.open(file)) {
            FileMetaData meta = reader.getFooter().getFileMetaData();

            MessageType schema = meta.getSchema();
            List<ColumnDescriptor> columns = schema.getColumns();
            int rowCount = 0;
            while (true) {
                PageReadStore rowGroup = reader.readNextRowGroup();
                if (rowGroup == null) {
                    break;
                }

                ColumnReadStore columnReadStore = new ColumnReadStoreImpl(rowGroup, new DummyRecordConverter(schema).getRootConverter(), schema, meta.getCreatedBy());
                List<ColumnReader> columnReaders = columns.stream().map(columnReadStore::getColumnReader).collect(Collectors.toList());

                for (int rowIndex = 0; rowIndex < rowGroup.getRowCount(); rowIndex++) {
                    rowCount++;
                    for (ColumnReader columnReader : columnReaders) {
                        String value = readValue(columnReader);
                        System.out.println(Arrays.deepToString(columnReader.getDescriptor().getPath()) + "=" + value);
                        columnReader.consume();
                        if (columnReader.getCurrentRepetitionLevel() != 0) {
                            throw new IllegalStateException("Unexpected repetition");
                        }
                    }
                }
            }
            System.out.println(rowCount);
        }

        return null;
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
