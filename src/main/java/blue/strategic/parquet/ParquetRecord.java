package blue.strategic.parquet;

import org.apache.parquet.Preconditions;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ParquetRecord {
    private final String[] header;
    private final PrimitiveType.PrimitiveTypeName[] types;
    private final Object[] data;

    private final Map<String, Integer> colIndexByName;

    private ParquetRecord(String[] header, Map<String, Integer> colIndexByName, PrimitiveType.PrimitiveTypeName[] types, Object[] data) {
        Preconditions.checkState(types.length == header.length, "Header and types have differing lengths");
        Preconditions.checkState(data.length == header.length, "Header and data have differing lengths");
        this.data = data;
        this.types = types;
        this.header = header;
        this.colIndexByName = colIndexByName;
    }

    public Object getValue(String columnName) {
        Integer index = colIndexByName.get(columnName);
        return index == null ? null : data[index];
    }

    public Short getShort(String columnName) {
        return (Short) getValue(columnName);
    }

    public Double getDouble(String columnName) {
        return (Double) getValue(columnName);
    }

    public Integer getInteger(String columnName) {
        return (Integer) getValue(columnName);
    }

    public Long getLong(String columnName) {
        return (Long) getValue(columnName);
    }

    public String getString(String columnName) {
        return (String) getValue(columnName);
    }

    public Instant getInstant(String columnName) {
        return Instant.ofEpochMilli(getLong(columnName));
    }

    public Map<String, Object> getRowMap() {
        Map<String, Object> rowMap = new HashMap<>();
        for (String colName: colIndexByName.keySet()) {
            rowMap.put(colName, this.getValue(colName));
        }
        return rowMap;
    }

    public static Map<String, Integer> indexByNameMapFor(String[] header) {
        return IntStream.range(0, header.length)
                .boxed()
                .collect(Collectors.toMap(i -> header[i], Function.identity()));
    }

    public Object[] getData() {
        return data;
    }

    public interface ValueVisitor {
        void visit(String name, PrimitiveType.PrimitiveTypeName type, Object value);
    }

    public void forEach(ValueVisitor visitor) {
        for (int i = 0; i < header.length; i++) {
            visitor.visit(header[i], types[i], data[i]);
        }
    }

    @Override
    public String toString() {
        return IntStream.range(0, data.length)
                .filter(i -> !"".equals(data[i]))
                .mapToObj(i -> header[i] + ":" + data[i])
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    public static Builder builder(MessageType schema) {
        List<ColumnDescriptor> columns = schema.getColumns();

        String[] header = new String[columns.size()];
        PrimitiveType.PrimitiveTypeName[] types = new PrimitiveType.PrimitiveTypeName[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            ColumnDescriptor column = columns.get(i);
            String[] path = column.getPath();
            Preconditions.checkState(path.length == 1, "Deep path! " + Arrays.deepToString(path));
            header[i] = path[0];
            types[i] = column.getPrimitiveType().getPrimitiveTypeName();
        }

        return new Builder(header, types);
    }

    public static final class Builder {
        private final String[] header;
        private final PrimitiveType.PrimitiveTypeName[] types;
        private final Map<String, Integer> index;

        private Builder(String[] header, PrimitiveType.PrimitiveTypeName[] types) {
            this.header = header.clone();
            this.types = types.clone();
            this.index = ParquetRecord.indexByNameMapFor(header);
        }

        public ParquetRecord build(Object[] data) {
            return new ParquetRecord(header, index, types, data);
        }
    }
}
