# parquet-floor
A lightweight Java library that facilitates reading and writing Apache Parquet files without Hadoop dependencies

## Distribution

This library is distributed via Maven Central.

```xml
<dependency>
    <groupId>blue.strategic.parquet</groupId>
    <artifactId>parquet-floor</artifactId>
    <version>1.51</version>
</dependency>
```

## Mission
Reading Parquet files in Java ought to be easy, but you can't seem to avoid pulling in most of Hadoop as dependencies. There are quite a few people complaining about this (e.g. https://stackoverflow.com/questions/59939309/read-local-parquet-file-without-hadoop-path-api, https://stackoverflow.com/questions/29279865/parquet-without-hadoop and https://issues.apache.org/jira/browse/PARQUET-1126), but there are no simple solutions out there.

This library is put together using the fewest possible dependencies.  In order to avoid pulling in the Hadoop dependency tree, it deliberately re-implements certain classes in the `org.apache.hadoop` package.  Code has been lifted from the Apache Hadoop project (particularly https://github.com/apache/hadoop/tree/trunk/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs).

