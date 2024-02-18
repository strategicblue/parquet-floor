package blue.strategic.parquet.io;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.parquet.io.PositionOutputStream;

/**
 * <code>OutputStream</code> based parquet output
 * 
 * @author mrmx
 */
public class StreamParquetOutput extends AbstractParquetOutput {
    private final OutputStream outputStream;

    public StreamParquetOutput(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
        
    @Override
    protected PositionOutputStream createPositionOutputStream() throws IOException {
        return new PositionAwareOutputStream(outputStream);
    }
    
}
