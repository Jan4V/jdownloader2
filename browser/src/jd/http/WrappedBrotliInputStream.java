package jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.brotli.dec.BrotliInputStream;

public class WrappedBrotliInputStream extends InputStream {
    protected volatile BrotliInputStream bis    = null;
    protected final PushbackInputStream  is;
    protected boolean                    brotli = false;

    public WrappedBrotliInputStream(InputStream is) {
        this.is = new PushbackInputStream(is, 32);
    }

    private synchronized void initializeBrotliInputStream() throws IOException {
        if (this.bis == null) {
            this.bis = new BrotliInputStream(this.is);
        }
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (this.bis == null) {
            final int read = this.is.read();
            if (read == -1) {
                return -1;
            }
            this.is.unread(read);
            this.initializeBrotliInputStream();
        }
        return this.bis.read(b, off, len);
    }

    @Override
    public synchronized int read() throws IOException {
        if (this.bis == null) {
            final int read = this.is.read();
            if (read == -1) {
                return -1;
            }
            this.is.unread(read);
            this.initializeBrotliInputStream();
        }
        return this.bis.read();
    }

    @Override
    public void close() throws IOException {
        this.bis.close();
    }

    @Override
    public int available() throws IOException {
        if (this.bis != null) {
            return this.bis.available();
        } else {
            return this.is.available();
        }
    }
}
