# parquet-floor

[![](https://jitpack.io/v/qtsurfer/parquet-floor.svg)](https://jitpack.io/#qtsurfer/parquet-floor)

A lightweight Java library that facilitates reading and writing Apache Parquet files without Hadoop dependencies.

This is a fork of [strategicblue/parquet-floor](https://github.com/strategicblue/parquet-floor) with added `OutputStream` support for writing Parquet data to arbitrary output streams (e.g. S3, MinIO, network sockets).

## Changes from upstream

- `ParquetWriter.writeOutputStream(schema, outputStream, dehydrator)` — write Parquet to any `OutputStream`
- `ParquetWriter.writeOutput(schema, outputFile, dehydrator)` — write Parquet to any `OutputFile` implementation
- `StreamParquetOutput` — `OutputFile` adapter for `OutputStream`
- `FileParquetOutput` — `OutputFile` adapter for `File` (extracted from inline anonymous class)
- Rebased on upstream master (parquet 1.17.0)

## Maven coordinates

Add the JitPack repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<dependency>
    <groupId>com.github.qtsurfer</groupId>
    <artifactId>parquet-floor</artifactId>
    <version>1.63</version>
</dependency>
```

## Mission

Reading Parquet files in Java ought to be easy, but you can't seem to avoid pulling in most of Hadoop as dependencies. There are quite a few people complaining about this (e.g. https://stackoverflow.com/questions/59939309/read-local-parquet-file-without-hadoop-path-api, https://stackoverflow.com/questions/29279865/parquet-without-hadoop and https://issues.apache.org/jira/browse/PARQUET-1126), but there are no simple solutions out there.

This library is put together using the fewest possible dependencies. In order to avoid pulling in the Hadoop dependency tree, it deliberately re-implements certain classes in the `org.apache.hadoop` package. Code has been lifted from the Apache Hadoop project (particularly https://github.com/apache/hadoop/tree/trunk/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

## Copyright

- Original work: Copyright Strategic Blue Ltd — [strategicblue/parquet-floor](https://github.com/strategicblue/parquet-floor)
- OutputStream support and fork maintenance: Copyright Wualabs — [QTSurfer/parquet-floor](https://github.com/QTSurfer/parquet-floor)
