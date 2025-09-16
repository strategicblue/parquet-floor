package blue.strategic.parquet.io;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.parquet.io.DelegatingPositionOutputStream;


/**
 * Default implementation of a <code>PositionOutputStream</code>
 * 
 * @author mrmx
 */
public class PositionAwareOutputStream extends DelegatingPositionOutputStream {
    private final OutputStream outputStream;
    private long position;

    public PositionAwareOutputStream(OutputStream outputStream) {
        super(outputStream);
        this.outputStream = outputStream;
        this.position = 0;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
        position++;
    }

    @Override
    public void write(byte[] b) throws IOException {
        outputStream.write(b);
        position += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
        position += len;
    }

    @Override
    public long getPos() throws IOException {
        return position;
    }

}
