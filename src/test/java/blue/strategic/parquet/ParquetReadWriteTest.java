package blue.strategic.parquet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class ParquetReadWriteTest {
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void testUsingTempFolder() throws IOException {
        File parquet = new File(folder.getRoot(), "foo.parquet");
        ParquetWriter.writeFile(parquet);
        ParquetReader.readFile(parquet);
    }
}