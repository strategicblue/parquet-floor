package blue.strategic.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;

import java.util.Collections;

public final class Writificator extends WriteSupport<Object> {

    private final MessageType schema;
    private final Dehydrator dehydrator;
    private final ValueWriter valueWriter = new ValueWriter();

    private RecordConsumer recordConsumer;

    public Writificator(MessageType schema, Dehydrator dehydrator) {
        this.schema = schema;
        this.dehydrator = dehydrator;
    }

    @Override
    public WriteContext init(Configuration configuration) {
        return new WriteContext(schema, Collections.emptyMap());
    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        this.recordConsumer = recordConsumer;
    }

    @Override
    public void write(Object record) {
        recordConsumer.startMessage();
        dehydrator.dehydrate(record, valueWriter);
        recordConsumer.endMessage();
    }

    public final class ValueWriter {
        public void write(String name, Object value) {
            int fieldIndex = schema.getFieldIndex(name);
            PrimitiveType type = schema.getType(fieldIndex).asPrimitiveType();
            recordConsumer.startField(name, fieldIndex);

            switch (type.getPrimitiveTypeName()) {
                case INT32: recordConsumer.addInteger((int)value); break;
                case INT64: recordConsumer.addLong((long)value); break;
                case DOUBLE: recordConsumer.addDouble((double)value); break;
                case BOOLEAN: recordConsumer.addBoolean((boolean)value); break;
                case FLOAT: recordConsumer.addFloat((float)value); break;
                case BINARY: if (type.getLogicalTypeAnnotation() == LogicalTypeAnnotation.stringType()) {
                    recordConsumer.addBinary(Binary.fromString((String)value));
                } else {
                    throw new UnsupportedOperationException("We don't support writing " + type.getLogicalTypeAnnotation());
                } break;
                case INT96:
                case FIXED_LEN_BYTE_ARRAY:
                    throw new UnsupportedOperationException("We don't support writing " + type.getPrimitiveTypeName());
            }
            recordConsumer.endField(name, fieldIndex);
        }
    }
}
