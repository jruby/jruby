package org.jruby.util.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class SplitChannel implements ReadableByteChannel, WritableByteChannel {

    private ReadableByteChannel inChannel;
    private WritableByteChannel outChannel;
    private boolean isOpen;
    private boolean writable;
    private boolean readable;

    public SplitChannel(ReadableByteChannel inChannel, WritableByteChannel outChannel) {
        super();
        this.inChannel = inChannel;
        this.outChannel = outChannel;
        this.isOpen = true;
        
        if (inChannel != null) {
            readable = true;
        }
        if (outChannel != null) {
            writable = true;
        }
    }

    public int read(ByteBuffer buffer) throws IOException {
        if (isOpen && readable) {
            return inChannel.read(buffer);
        } else {
            throw new EOFException();
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void close() throws IOException {
        inChannel.close();
        outChannel.close();
        isOpen = false;
    }
    
    public void closeRead() throws IOException {
        inChannel.close();
        readable = false;
    }
    
    public void closeWrite() throws IOException {
        outChannel.close();
        writable = false;
    }

    public int write(ByteBuffer buffer) throws IOException {
        if (isOpen && writable) {
            return outChannel.write(buffer);
        } else {
            throw new EOFException();
        }
    }
}
