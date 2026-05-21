# parquet-floor
A lightweight Java library that facilitates reading and writing Apache Parquet files without Hadoop dependencies

## Distribution

This library is distributed via Maven Central.

```xml
<dependency>
    <groupId>blue.strategic.parquet</groupId>
    <artifactId>parquet-floor</artifactId>
    <version>2.0</version>
</dependency>
```

## Mission
Reading Parquet files in Java ought to be easy, but you can't seem to avoid pulling in most of Hadoop as dependencies. There are quite a few people complaining about this (e.g. https://stackoverflow.com/questions/59939309/read-local-parquet-file-without-hadoop-path-api, https://stackoverflow.com/questions/29279865/parquet-without-hadoop and https://issues.apache.org/jira/browse/PARQUET-1126), but there are no simple solutions out there.

This library is put together using the fewest possible dependencies.  In order to avoid pulling in the Hadoop dependency tree, it deliberately re-implements certain classes in the `org.apache.hadoop` package.  Code has been lifted from the Apache Hadoop project (particularly https://github.com/apache/hadoop/tree/trunk/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs).

## API

### Reading

The primary entry point is `ParquetReader`, which streams Parquet rows as domain objects via a `Hydrator`.

```java
// Simple: read all columns, receive field paths as String[]
Stream<MyRecord> records = ParquetReader.streamContent(file, new Hydrator<MyRecord, MyRecord, String[]>() {
    public MyRecord start() { return new MyRecord(); }
    public MyRecord add(MyRecord target, String[] field, Object value) {
        switch (field[0]) {
            case "name": target.name = (String) value; break;
            case "age": target.age = (int) value; break;
        }
        return target;
    }
    public MyRecord finish(MyRecord target) { return target; }
});
```

#### Field Mapper

A `Function<String[], F>` fieldMapper gives you control over which fields are read and how they're identified in the hydrator. Return `null` to skip a field.

```java
Stream<MyRecord> records = ParquetReader.streamContent(file, hydrator, path -> {
    if (path[0].equals("unwanted_column")) return null; // skip
    return MyFieldEnum.fromPath(path); // custom field context
});
```

#### MAP columns

Parquet MAP columns (repeated key-value groups) are supported. The fieldMapper receives 2-element paths like `["tags", "environment"]` where the first element is the map column name and the second is the key.

Note: the fieldMapper is also called once with just the top-level map name (e.g. `["tags"]`) to decide whether to read the map at all — return `null` to skip the entire map. However, there is no corresponding `hydrator.add()` call for the map column itself; only individual key-value entries produce `add()` calls.

#### Other entry points

- `ParquetReader.streamContent(InputFile, Hydrator)` — for custom `InputFile` implementations
- `ParquetReader.spliterator(...)` — returns the `Spliterator` directly for manual iteration
- `ParquetReader.streamContentToStrings(File)` — quick debugging helper
- `ParquetReader.readMetadata(File)` — read only the Parquet footer metadata

### Writing

`ParquetWriter` writes domain objects to Parquet via a `Dehydrator`.

```java
MessageType schema = new MessageType("record",
    new PrimitiveType(REQUIRED, BINARY, "name", stringType()),
    new PrimitiveType(REQUIRED, INT32, "age"));

try (ParquetWriter<MyRecord> writer = ParquetWriter.writeFile(schema, outputFile, (record, valueWriter) -> {
    valueWriter.write("name", record.name);
    valueWriter.write("age", record.age);
})) {
    writer.write(myRecord);
}
```

Supported write types: INT32, INT64, DOUBLE, FLOAT, BOOLEAN, BINARY (string and JSON logical types).

### Interfaces

#### `Hydrator<T, R, F>`

Assembles a domain object from Parquet column values.

| Method | Description |
|--------|-------------|
| `T start()` | Create a new mutable instance to hydrate |
| `T add(T target, F field, Object value)` | Apply a field value; return the (possibly new) target |
| `R finish(T target)` | Seal and return the finished record |

#### `Dehydrator<T>`

Writes a domain object's fields into a Parquet row.

| Method | Description |
|--------|-------------|
| `void dehydrate(T record, ValueWriter writer)` | Write all fields of the record |

#### `ValueWriter`

Passed to the dehydrator to write individual field values.

| Method | Description |
|--------|-------------|
| `void write(String name, Object value)` | Write a named field value |
