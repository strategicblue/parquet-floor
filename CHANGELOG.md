# Changelog

## 2.0

### Breaking Changes

- **`Hydrator` interface now has 3 type parameters**: `Hydrator<T, R, F>` (was `Hydrator<U, S>`).
  The new `F` parameter is the type of the field context object passed to `add()`. For simple
  use cases, use `String[]` as `F` to receive column path arrays (equivalent to the old `String heading`).

- **`Hydrator.add()` signature changed**: now receives a user-defined field context object (`F`)
  instead of a plain `String heading`.

- **`HydratorSupplier` removed**: `ParquetReader` now takes a `Hydrator` directly instead of a
  `HydratorSupplier`. The column-list-aware factory pattern is no longer needed.

- **`ParquetReader` has 3 type parameters**: `ParquetReader<U, S, F>` (was `ParquetReader<U, S>`).

- **Column filtering replaced by `fieldMapper`**: the old `Collection<String> columns` parameter
  is gone. Instead, pass a `Function<String[], F>` fieldMapper that receives field paths and
  returns `null` to skip a field, or a user context object to include it.

### New Features

- **MAP column support**: `ParquetReader` now reads Parquet MAP columns (repeated key-value groups
  with primitive keys and values). Map entries are delivered to the hydrator with a 2-element path
  `["mapName", "key"]`. Note: the fieldMapper is called with the top-level map column name
  (e.g. `["tags"]`) to decide whether to read the map at all — return `null` to skip the entire
  map. However, there is no corresponding `hydrator.add()` call for the map itself; only individual
  key-value entries produce `add()` calls (with paths like `["tags", "someKey"]`).

- **Field path arrays**: flat columns are delivered as single-element `String[]` paths, map entries
  as 2-element paths. This gives hydrators full context about nested structure.

### Migration Guide

**Before (1.x):**
```java
Hydrator<MyObj, MyObj> hydrator = new Hydrator<>() {
    public MyObj start() { return new MyObj(); }
    public MyObj add(MyObj target, String heading, Object value) {
        // heading was a plain column name string
    }
    public MyObj finish(MyObj target) { return target; }
};
ParquetReader.streamContent(file, HydratorSupplier.constantly(hydrator));
```

**After (2.0):**
```java
Hydrator<MyObj, MyObj, String[]> hydrator = new Hydrator<>() {
    public MyObj start() { return new MyObj(); }
    public MyObj add(MyObj target, String[] heading, Object value) {
        // heading[0] is the column name; heading[1] is the map key (for MAP columns)
    }
    public MyObj finish(MyObj target) { return target; }
};
ParquetReader.streamContent(file, hydrator);
```

Or with a custom fieldMapper for selective reading and typed field context:
```java
ParquetReader.streamContent(file, hydrator, path -> {
    // return null to skip, or a user object to include
    return MyEnum.fromPath(path);
});
```
