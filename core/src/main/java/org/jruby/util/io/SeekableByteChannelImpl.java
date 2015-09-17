/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    JRuby - initial API and implementation and/or initial documentation
 */
package org.jruby.util.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

/**
 *
 * @author kares
 */
final class SeekableByteChannelImpl extends AbstractInterruptibleChannel  // Not really interruptible
    implements ReadableByteChannel, SeekableByteChannel {

    private static final byte[] EMPTY_BUF = new byte[0];

    private final ByteArrayInputStream in;
    private byte buf[] = EMPTY_BUF;
    private final Object readLock = new Object();

    private final int mark;
    private final int count;

    private int truncatedBy = 0;

    SeekableByteChannelImpl(ByteArrayInputStream in) {
        this.in = in;
        this.mark = ByteArrayInputStreamHelper.mark(in);
        this.count = ByteArrayInputStreamHelper.count(in);
    }

    @Override // based on java.nio.channels.Channels.ReadableByteChannelImpl
    public int read(ByteBuffer dst) throws IOException {
        final int len = dst.remaining();
        int totalRead = 0; int bytesRead = 0;
        synchronized (readLock) {
            while (totalRead < len) {
                int bytesToRead = Math.min((len - totalRead), 4096);
                if (buf.length < bytesToRead) buf = new byte[bytesToRead];
                final int avail = in.available() - truncatedBy;
                if ( totalRead > 0 && avail <= 0 ) break; // block at most once
                try {
                    begin();
                    // make sure we account for truncated bytes :
                    if (bytesToRead > avail) bytesToRead = avail;

                    bytesRead = in.read(buf, 0, bytesToRead);
                } finally {
                    end(bytesRead > 0);
                }
                if (bytesRead < 0) break;
                else totalRead += bytesRead;
                dst.put(buf, 0, bytesRead);
            }
            if ((bytesRead < 0) && (totalRead == 0)) return -1;

            return totalRead;
        }
    }

    @Override
    protected void implCloseChannel() throws IOException { in.close(); }

    // SeekableByteChannel interface :

    public long position() {
        return ByteArrayInputStreamHelper.pos(in) - mark;
    }

    public SeekableByteChannel position(long newPosition) throws IOException {
        if ( newPosition < 0 ) {
            throw new IllegalArgumentException("negative new position: " + newPosition);
        }
        if ( newPosition > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException("can not set new position: " + newPosition + " too big!");
        }
        synchronized (readLock) {
            this.in.reset(); // to initial mark (0 or offset)
            if ( newPosition > 0 ) this.in.skip(newPosition);
            //this.position = (int) newPosition;
        }
        return this;
    }

    public long size() {
        return Math.max(count - truncatedBy, 0);
    }

    public SeekableByteChannel truncate(long size) throws IOException {
        if ( size < 0 ) {
            throw new IllegalArgumentException("negative truncate size given: " + size);
        }
        final int s = Math.min((int) size(), size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size);
        this.truncatedBy += s;
        return this;
    }

    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("write not supported");
    }

    private static class ByteArrayInputStreamHelper {

        static int pos(ByteArrayInputStream in) {
            return readField(in, "pos"); // current pos
        }

        static int count(ByteArrayInputStream in) {
            return readField(in, "count"); // buf.length or offset + length
        }

        static int mark(ByteArrayInputStream in) {
            return readField(in, "mark"); // 0 oir offset or if marked previously
        }

        private static int readField(ByteArrayInputStream self, String name) {
            try {
                Field field = ByteArrayInputStream.class.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(self);
            }
            catch (NoSuchFieldException ex) { throw new RuntimeException(ex); } // should never happen
            catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex); // TODO
            }
        }

    }

}
