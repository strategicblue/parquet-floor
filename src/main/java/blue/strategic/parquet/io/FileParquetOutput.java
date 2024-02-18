package blue.strategic.parquet.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.parquet.io.DelegatingPositionOutputStream;
import org.apache.parquet.io.PositionOutputStream;

/**
 * File based parquet output
 * 
 * @author mrmx
 */
public class FileParquetOutput extends AbstractParquetOutput {
    private String path;
    private FileOutputStream stream;
    
    public FileParquetOutput(File file) throws FileNotFoundException {
        this(file.getPath(),new FileOutputStream(file));
    }
    
    public FileParquetOutput(FileOutputStream stream) {
        this(null,stream);
    }

    public FileParquetOutput(String path, FileOutputStream stream) {
        this.path = path;
        this.stream = stream;
    }
    
    @Override
    protected PositionOutputStream createPositionOutputStream() throws IOException {
        return new DelegatingPositionOutputStream(stream) {
                    @Override
                    public long getPos() throws IOException {
                        return stream.getChannel().position();
                    }
                };
    }

    @Override
    public String getPath() {
        return path;
    }
    
}
