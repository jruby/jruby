/*
 * Copyright (c) 2015 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    JRuby - initial API and implementation and/or initial documentation
 */
package org.jruby.util.io;

import org.jruby.javasupport.Java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

/**
 * Seekable byte channel impl over a byte array stream.
 * @author kares
 */
final class SeekableByteChannelImpl extends AbstractInterruptibleChannel
    implements ReadableByteChannel, SeekableByteChannel {

    private final ByteArrayInputStream in;

    private final int mark;
    private final int count;

    private int truncatedBy = 0;

    SeekableByteChannelImpl(ByteArrayInputStream in) {
        this.in = in;
        this.mark = mark(in);
        this.count = count(in);
    }

    @Override
    public synchronized int read(ByteBuffer target) throws IOException {
        //if ( ! isOpen() ) throw new ClosedChannelException();

        final int available = in.available() - truncatedBy;
        if ( available <= 0 ) return 0;

        int maxToRead = target.remaining(); int readCount = 0;
        // make sure we account for truncated bytes :
        if (maxToRead > available) maxToRead = available;
        byte[] readBytes = new byte[maxToRead];
        try {
            begin();
            readCount = in.read(readBytes);
        }
        finally {
            end(readCount >= 0);
        }
        if (readCount > 0) {
            target.put(readBytes, 0, readCount);
        }
        return readCount;
    }

    @Override
    protected void implCloseChannel() throws IOException { in.close(); }

    // SeekableByteChannel interface :

    public long position() {
        return pos(in) - mark;
    }

    public synchronized SeekableByteChannel position(long newPosition) throws IOException {
        if ( newPosition < 0 ) {
            throw new IllegalArgumentException("negative new position: " + newPosition);
        }
        if ( newPosition > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException("can not set new position: " + newPosition + " too big!");
        }
        this.in.reset(); // to initial mark (0 or offset)
        if ( newPosition > 0 ) this.in.skip(newPosition);
        //this.position = (int) newPosition;
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

    // static helpers

    private static int pos(ByteArrayInputStream in) {
        return readIntField(in, posField); // current pos
    }

    private static int count(ByteArrayInputStream in) {
        return readIntField(in, countField); // buf.length or offset + length
    }

    private static int mark(ByteArrayInputStream in) {
        return readIntField(in, markField); // 0 or offset or if marked previously
    }

    private static int readIntField(ByteArrayInputStream self, Field field) {
        try {
            return field.getInt(self);
        }
        catch (IllegalAccessException ex) {
            // fields are set-accessible thus should not happen
            throw new IllegalStateException(ex);
        }
    }

    static final boolean USABLE;

    private static final Field posField;
    private static final Field countField;
    private static final Field markField;

    static {
        posField = accessibleField("pos");
        if (posField != null) {
            countField = accessibleField("count");
            markField = accessibleField("mark");
            USABLE = true;
        }
        else {
            countField = markField = null;
            USABLE = false;
        }
    }

    private static Field accessibleField(final String name) {
        try {
            Field field = ByteArrayInputStream.class.getDeclaredField(name);
            Java.trySetAccessible(field);
            return field;
        }
        catch (NoSuchFieldException ex) {
            return null; // should never happen
        }
        catch (SecurityException ex) {
            return null;
        }
    }

}
