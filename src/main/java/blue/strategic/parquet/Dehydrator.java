package blue.strategic.parquet;


/**
 * Dehydrates a rich java object into a Parquet row.
 */
public interface Dehydrator {
    /**
     * Write the specified record into the Parquet row using the supplied writer.
     * @param record the rich java object
     * @param valueWriter facilitates writing to the Parquet row
     */
    void dehydrate(Object record, Writificator.ValueWriter valueWriter);
}
