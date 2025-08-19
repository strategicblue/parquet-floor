package blue.strategic.parquet.io;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;

/**
 * Base class for parquet output
 * 
 * @author mrmx
 */
public abstract class AbstractParquetOutput implements OutputFile{
    

    @Override
    public PositionOutputStream create(long blockSizeHint) throws IOException {
        return createOrOverwrite(blockSizeHint);
    }

    @Override
    public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
        return createPositionOutputStream();
    }

    @Override
    public boolean supportsBlockSize() {
        return false;
    }

    @Override
    public long defaultBlockSize() {
        return 1024L;
    }
    
    protected abstract PositionOutputStream createPositionOutputStream() throws IOException;
    
}
