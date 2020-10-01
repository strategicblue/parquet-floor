package blue.strategic.parquet;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParquetReadWriteTest {
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void testUsingTempFolder() throws IOException {
        File parquet = new File(folder.getRoot(), "foo.parquet");
        ParquetWriter.writeFile(parquet);

        try (Stream<Object[]> s = ParquetReader.readFile(parquet)) {
            List<Object[]> result = s.collect(Collectors.toList());

            assertThat(result, hasItems(
                    new Object[] {"1", "hello1"},
                    new Object[] {"2", "hello2"}));
        }
    }
}