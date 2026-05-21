package blue.strategic.parquet;

/**
 * Creates and hydrates a rich domain object from a Parquet row.
 */
public interface Hydrator<T, R, F> {

    /**
     * Creates a new mutable instance to be hydrated.
     * @return new instance to be hydrated
     */
    T start();

    /**
     * Hydrates the target instance by applying the specified value from the Parquet row.
     * @param target object being hydrated
     * @param userFieldContext the user object matching the field whose value is being supplied
     * @param value the value to apply
     * @return the new target
     */
    T add(T target, F userFieldContext, Object value);

    /**
     * Seals the mutable hydration target and returns a finished record.
     * @param target object being hydrated
     * @return the finished record object
     */
    R finish(T target);
}
