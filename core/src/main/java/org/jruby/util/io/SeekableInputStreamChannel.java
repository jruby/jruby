/*
 * Copyright (c) 2026 JRuby.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *    JRuby - initial API and implementation and/or initial documentation
 */
package org.jruby.util.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.AccessDeniedException;

/**
 * Read-only seekable channel over a (re-openable) input stream source e.g. a (compressed) jar entry.
 *
 * <p>
 * Streams have no random access, thus seeking forward skips and seeking backward re-opens the source.
 * Sequential reads (the common case) cost the same as reading the stream directly.
 * </p>
 *
 * @author kares
 */
public final class SeekableInputStreamChannel extends AbstractInterruptibleChannel implements SeekableByteChannel {

    @FunctionalInterface
    public interface Source {
        InputStream open() throws IOException;
    }

    private final Source source;
    private final String path; // for error reporting

    private final Cursor cursor = new Cursor(); // backs read(dst) and position
    private Cursor positionalCursor; // pread-s must not disturb position

    private long position;
    private long size;

    private byte[] buf;

    /**
     * @param source to (re-)open the stream from
     * @param size the stream's length or -1 if unknown (computed when needed)
     * @param path used for error messages
     */
    public SeekableInputStreamChannel(Source source, long size, String path) throws IOException {
        this.source = source;
        this.size = size;
        this.path = path;
        cursor.in = openSource(); // fail early (e.g. missing resource)
    }

    @Override
    public synchronized int read(ByteBuffer dst) throws IOException {
        ensureOpen();
        int read = read(cursor, dst, position);
        if (read > 0) position += read;
        return read;
    }

    /**
     * Read at the given position, without changing the channel's position (like pread(2)).
     * @return bytes read or -1 when position is at (or past) EOF
     */
    public synchronized int read(ByteBuffer dst, long position) throws IOException {
        if (position < 0) throw new IllegalArgumentException("negative position: " + position);
        ensureOpen();
        if (positionalCursor == null) positionalCursor = new Cursor();
        return read(positionalCursor, dst, position);
    }

    @Override
    public synchronized long position() throws IOException {
        ensureOpen();
        return position;
    }

    @Override
    public synchronized SeekableInputStreamChannel position(long newPosition) throws IOException {
        if (newPosition < 0) throw new IllegalArgumentException("negative position: " + newPosition);
        ensureOpen();
        this.position = newPosition; // stream gets synced (lazily) on next read
        return this;
    }

    @Override
    public synchronized long size() throws IOException {
        ensureOpen();
        if (size < 0) size = computeSize();
        return size;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new AccessDeniedException(path); // EACCES as with a non-writable channel
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        throw new NonWritableChannelException();
    }

    @Override
    protected synchronized void implCloseChannel() throws IOException {
        try {
            cursor.close();
        } finally {
            if (positionalCursor != null) positionalCursor.close();
        }
    }

    private int read(final Cursor cursor, final ByteBuffer dst, final long position) throws IOException {
        if (!dst.hasRemaining()) return 0;

        // fill dst buffer as a regular file read would
        int total = 0;
        boolean completed = false;
        try {
            begin();
            if (!cursor.seek(position)) {
                completed = true;
                return -1;
            }

            byte[] buf = this.buf;
            if (buf == null) this.buf = buf = new byte[8192];
            while (dst.hasRemaining()) {
                int read = cursor.in.read(buf, 0, Math.min(dst.remaining(), buf.length));
                if (read == -1) break;
                dst.put(buf, 0, read);
                cursor.pos += read;
                total += read;
            }
            completed = true;
            return total == 0 ? -1 : total;
        } finally {
            end(completed);
        }
    }

    private long computeSize() throws IOException {
        // NOTE: a separate stream, not to disturb the cursors
        try (InputStream in = openSource()) {
            long size = 0;
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) != -1; ) size += n;
            return size;
        }
    }

    private InputStream openSource() throws IOException {
        InputStream in = source.open();
        if (in == null) throw new FileNotFoundException(path); // e.g. ClassLoader#getResourceAsStream
        return in;
    }

    private void ensureOpen() throws ClosedChannelException {
        if (!isOpen()) throw new ClosedChannelException();
    }

    private final class Cursor {

        InputStream in;
        long pos; // bytes consumed from in

        /**
         * @return false if target position is known to be at or past EOF
         */
        boolean seek(final long target) throws IOException {
            if (size >= 0 && target >= size) return false;

            if (in == null || target < pos) {
                close();
                in = openSource();
                pos = 0;
            }

            while (pos < target) {
                long skipped = in.skip(target - pos);
                if (skipped <= 0) { // skip might return 0 before EOF, a read tells for sure
                    if (in.read() == -1) return false;
                    skipped = 1;
                }
                pos += skipped;
            }
            return true;
        }

        void close() throws IOException {
            final InputStream in = this.in;
            this.in = null;
            if (in != null) in.close();
        }

    }
}
