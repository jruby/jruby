/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 *
 * @author headius
 */
public class NullWritableChannel implements WritableByteChannel {
    private boolean isOpen = true;

    public int write(ByteBuffer buffer) throws IOException {
        if (!isOpen) {
            throw new EOFException();
        }
        return buffer.remaining();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void close() throws IOException {
        isOpen = false;
    }

}
