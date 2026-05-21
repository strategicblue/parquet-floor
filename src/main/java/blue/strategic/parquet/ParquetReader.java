package blue.strategic.parquet;

import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.ColumnReadStore;
import org.apache.parquet.column.ColumnReader;
import org.apache.parquet.column.impl.ColumnReadStoreImpl;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.DummyRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.DelegatingSeekableInputStream;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reads Parquet files column-by-column, hydrating domain objects via a {@link Hydrator}.
 * <p>
 * Supported column types:
 * <ul>
 *   <li>Flat (non-repeated) primitive columns</li>
 *   <li>MAP columns with primitive key and primitive value (exactly 2 leaf columns in the
 *       repeated group). Maps with struct/group values are not supported and will be skipped.</li>
 * </ul>
 * <p>
 * A {@code Function<String[], F>} fieldMapper controls which fields are read and maps them to
 * opaque user context objects passed to the hydrator. The fieldMapper is called once per unique
 * field path and the result is cached. The {@code String[]} passed to the fieldMapper is reused
 * internally for map key resolution — callers must not retain a reference to it.
 */
public final class ParquetReader<U, S, F> implements Spliterator<S>, Closeable {

    private static final Object SKIP = new Object();
    private static final Function<String[], String[]> DEFAULT_FIELD_MAPPER = Function.identity();

    private final ParquetFileReader reader;
    private final Hydrator<U, S, F> hydrator;
    private final Function<String[], F> fieldMapper;
    private final MessageType schema;
    private final GroupConverter recordConverter;
    private final String createdBy;

    private final ColumnDescriptor[] flatColumns;
    private final F[] flatHeadings;

    private final ColumnDescriptor[] mapKeyColumns;
    private final ColumnDescriptor[] mapValueColumns;
    private final Map<String, F>[] mapKeyHeadingCaches;
    private final String[][] mapPathBuffers;

    private boolean finished;
    private long currentRowGroupSize = -1L;
    private ColumnReader[] flatReaders;
    private ColumnReader[] mapKeyReaders;
    private ColumnReader[] mapValueReaders;
    private long currentRowIndex = -1L;

    public static <U, S> Stream<S> streamContent(File file,
                                                 Hydrator<U, S, String[]> hydrator) throws IOException {
        return streamContent(makeInputFile(file), hydrator);
    }

    public static <U, S> Stream<S> streamContent(InputFile file,
                                                 Hydrator<U, S, String[]> hydrator) throws IOException {
        return stream(new ParquetReader<>(file, hydrator, DEFAULT_FIELD_MAPPER));
    }

    public static <U, S, F> Stream<S> streamContent(File file,
                                                    Hydrator<U, S, F> hydrator,
                                                    Function<String[], F> fieldMapper) throws IOException {
        return streamContent(makeInputFile(file), hydrator, fieldMapper);
    }

    /**
     * Streams content using a fieldMapper function to resolve headings.
     * <p>
     * The fieldMapper is called once per unique field path encountered and the result is cached.
     * It receives a {@code String[]} path (e.g. {@code ["colName"]} for flat columns,
     * {@code ["mapName", "key"]} for map entries) and returns the opaque heading object
     * to pass to the hydrator, or {@code null} to skip the field.
     */
    public static <U, S, F> Stream<S> streamContent(InputFile file,
                                                    Hydrator<U, S, F> hydrator,
                                                    Function<String[], F> fieldMapper) throws IOException {
        return stream(new ParquetReader<>(file, hydrator, fieldMapper));
    }

    public static <U, S> ParquetReader<U, S, String[]> spliterator(File file,
                                                                   Hydrator<U, S, String[]> hydrator) throws IOException {
        return spliterator(makeInputFile(file), hydrator);
    }

    public static <U, S> ParquetReader<U, S, String[]> spliterator(InputFile file,
                                                                   Hydrator<U, S, String[]> hydrator) throws IOException {
        return new ParquetReader<>(file, hydrator, DEFAULT_FIELD_MAPPER);
    }

    public static <U, S, F> ParquetReader<U, S, F> spliterator(File file,
                                                               Hydrator<U, S, F> hydrator,
                                                               Function<String[], F> fieldMapper) throws IOException {
        return spliterator(makeInputFile(file), hydrator, fieldMapper);
    }

    public static <U, S, F> ParquetReader<U, S, F> spliterator(InputFile file,
                                                               Hydrator<U, S, F> hydrator,
                                                               Function<String[], F> fieldMapper) throws IOException {
        return new ParquetReader<>(file, hydrator, fieldMapper);
    }

    public static <U, S, F> Stream<S> stream(ParquetReader<U, S, F> reader) {
        return StreamSupport
                .stream(reader, false)
                .onClose(() -> closeSilently(reader));
    }

    public static Stream<String[]> streamContentToStrings(File file) throws IOException {
        return stream(spliterator(makeInputFile(file),
                new Hydrator<List<String>, String[], String[]>() {
                    @Override
                    public List<String> start() {
                        return new ArrayList<>();
                    }

                    @Override
                    public List<String> add(List<String> target, String[] heading, Object value) {
                        target.add(String.join(".", heading) + "=" + value.toString());
                        return target;
                    }

                    @Override
                    public String[] finish(List<String> target) {
                        return target.toArray(new String[0]);
                    }
                }));
    }

    public static ParquetMetadata readMetadata(File file) throws IOException {
        return readMetadata(makeInputFile(file));
    }

    public static ParquetMetadata readMetadata(InputFile file) throws IOException {
        try (ParquetFileReader reader = ParquetFileReader.open(file)) {
            return reader.getFooter();
        }
    }

    @SuppressWarnings("unchecked")
    private ParquetReader(InputFile file,
                          Hydrator<U, S, F> hydrator,
                          Function<String[], F> fieldMapper) throws IOException {
        this.reader = ParquetFileReader.open(file);
        FileMetaData meta = reader.getFooter().getFileMetaData();
        this.schema = meta.getSchema();
        this.recordConverter = new DummyRecordConverter(this.schema).getRootConverter();
        this.createdBy = meta.getCreatedBy();
        this.hydrator = hydrator;
        this.fieldMapper = fieldMapper;

        List<ColumnDescriptor> flatColList = new ArrayList<>();
        List<F> flatHeadingList = new ArrayList<>();
        Map<String, ColumnDescriptor[]> repeatedGroups = new LinkedHashMap<>();

        for (ColumnDescriptor col : schema.getColumns()) {
            if (col.getMaxRepetitionLevel() == 0) {
                F heading = this.fieldMapper.apply(col.getPath());
                if (heading != null) {
                    flatColList.add(col);
                    flatHeadingList.add(heading);
                }
            } else {
                String mapName = col.getPath()[0];
                if (!repeatedGroups.containsKey(mapName)) {
                    if (this.fieldMapper.apply(new String[]{mapName}) == null) {
                        continue;
                    }
                    repeatedGroups.put(mapName, new ColumnDescriptor[2]);
                }
                ColumnDescriptor[] pair = repeatedGroups.get(mapName);
                if (pair[0] == null) {
                    pair[0] = col;
                } else {
                    pair[1] = col;
                }
            }
        }

        this.flatColumns = flatColList.toArray(new ColumnDescriptor[0]);
        this.flatHeadings = (F[]) flatHeadingList.toArray();

        List<ColumnDescriptor> keyColList = new ArrayList<>();
        List<ColumnDescriptor> valueColList = new ArrayList<>();
        List<Map<String, F>> caches = new ArrayList<>();
        List<String[]> pathBuffers = new ArrayList<>();

        for (Map.Entry<String, ColumnDescriptor[]> entry : repeatedGroups.entrySet()) {
            ColumnDescriptor[] pair = entry.getValue();
            if (pair[0] != null && pair[1] != null) {
                keyColList.add(pair[0]);
                valueColList.add(pair[1]);
                caches.add(new HashMap<>());
                pathBuffers.add(new String[]{entry.getKey(), null});
            }
        }

        this.mapKeyColumns = keyColList.toArray(new ColumnDescriptor[0]);
        this.mapValueColumns = valueColList.toArray(new ColumnDescriptor[0]);
        this.mapKeyHeadingCaches = caches.toArray(new Map[0]);
        this.mapPathBuffers = pathBuffers.toArray(new String[0][]);
    }

    private static void closeSilently(Closeable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private static Object readValue(ColumnReader columnReader) {
        ColumnDescriptor column = columnReader.getDescriptor();
        PrimitiveType primitiveType = column.getPrimitiveType();
        int maxDefinitionLevel = column.getMaxDefinitionLevel();

        if (columnReader.getCurrentDefinitionLevel() == maxDefinitionLevel) {
            switch (primitiveType.getPrimitiveTypeName()) {
            case BINARY:
            case FIXED_LEN_BYTE_ARRAY:
            case INT96:
                return primitiveType.stringifier().stringify(columnReader.getBinary());
            case BOOLEAN:
                return columnReader.getBoolean();
            case DOUBLE:
                return columnReader.getDouble();
            case FLOAT:
                return columnReader.getFloat();
            case INT32:
                return columnReader.getInteger();
            case INT64:
                return columnReader.getLong();
            default:
                throw new IllegalArgumentException("Unsupported type: " + primitiveType);
            }
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private F resolveMapKeyHeading(int mapIndex, String key) {
        Map<String, F> cache = mapKeyHeadingCaches[mapIndex];
        F cached = cache.get(key);
        if (cached != null) {
            return cached == SKIP ? null : cached;
        }
        String[] pathBuffer = mapPathBuffers[mapIndex];
        pathBuffer[1] = key;
        F heading = fieldMapper.apply(pathBuffer);
        // SKIP is a private singleton so it can never collide with a real F value from the fieldMapper
        cache.put(key, heading == null ? (F) SKIP : heading);
        return heading;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public boolean tryAdvance(Consumer<? super S> action) {
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

                this.currentRowGroupSize = rowGroup.getRowCount();
                this.flatReaders = new ColumnReader[flatColumns.length];
                for (int i = 0; i < flatColumns.length; i++) {
                    this.flatReaders[i] = columnReadStore.getColumnReader(flatColumns[i]);
                }
                this.mapKeyReaders = new ColumnReader[mapKeyColumns.length];
                this.mapValueReaders = new ColumnReader[mapValueColumns.length];
                for (int i = 0; i < mapKeyColumns.length; i++) {
                    this.mapKeyReaders[i] = columnReadStore.getColumnReader(mapKeyColumns[i]);
                    this.mapValueReaders[i] = columnReadStore.getColumnReader(mapValueColumns[i]);
                }
                this.currentRowIndex = 0L;
            }

            U record = hydrator.start();

            for (int i = 0; i < flatReaders.length; i++) {
                ColumnReader columnReader = flatReaders[i];
                record = hydrator.add(record, flatHeadings[i], readValue(columnReader));
                columnReader.consume();
            }

            for (int m = 0; m < mapKeyReaders.length; m++) {
                ColumnReader keyReader = mapKeyReaders[m];
                ColumnReader valueReader = mapValueReaders[m];

                int keyDefLevel = keyReader.getCurrentDefinitionLevel();
                if (keyDefLevel == 0) {
                    keyReader.consume();
                    valueReader.consume();
                } else {
                    do {
                        String key = (String) readValue(keyReader);
                        F heading = (key != null) ? resolveMapKeyHeading(m, key) : null;
                        if (heading != null) {
                            record = hydrator.add(record, heading, readValue(valueReader));
                        }
                        keyReader.consume();
                        valueReader.consume();
                    } while (keyReader.getCurrentRepetitionLevel() != 0);
                }
            }

            action.accept(hydrator.finish(record));
            this.currentRowIndex++;

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read parquet", e);
        }
    }

    @Override
    public Spliterator<S> trySplit() {
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

    public ParquetMetadata metaData() {
        return this.reader.getFooter();
    }

    public static InputFile makeInputFile(File file) {
        return new InputFile() {
            @Override
            public long getLength() {
                return file.length();
            }

            @Override
            public SeekableInputStream newStream() throws IOException {
                FileInputStream fis = new FileInputStream(file);
                return new DelegatingSeekableInputStream(fis) {
                    private long position;

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
    }
}
