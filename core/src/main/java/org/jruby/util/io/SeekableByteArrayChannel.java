/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

/**
 * Seekable byte channel impl over a byte array stream.
 * @author kares
 */
public final class SeekableByteArrayChannel extends AbstractInterruptibleChannel
    implements ReadableByteChannel, SeekableByteChannel {

    private final byte[] bytes;
    private final int base;
    private final int size;
    private volatile int pos = 0;

    public SeekableByteArrayChannel(byte[] bytes) {
        this.bytes = bytes;
        this.base = 0;
        this.size = bytes.length;
    }

    public SeekableByteArrayChannel(byte[] bytes, int base) {
        assert base >= 0 && base <= bytes.length;

        this.bytes = bytes;
        this.base = base;
        this.size = bytes.length;
    }

    public SeekableByteArrayChannel(byte[] bytes, int base, int size) {
        assert base >= 0 && base <= bytes.length;
        assert size >= 0 && base + size <= bytes.length;

        this.bytes = bytes;
        this.base = base;
        this.size = size;
    }

    @Override
    public synchronized int read(ByteBuffer target) throws IOException {
        final int available = size - pos;
        if ( available <= 0 ) return -1;

        int maxToRead = target.remaining();
        int readCount = 0;

        if (maxToRead > available) maxToRead = available;
        byte[] readBytes = new byte[maxToRead];
        try {
            begin();
            System.arraycopy(bytes, base + pos, readBytes, 0, maxToRead);
            readCount = maxToRead;
            pos += readCount;
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
    protected void implCloseChannel() {
        // meaningless
    }

    // SeekableByteChannel interface :

    public long position() {
        return pos;
    }

    public synchronized SeekableByteChannel position(long newPosition) throws IOException {
        if ( newPosition < 0 ) {
            throw new IllegalArgumentException("negative new position: " + newPosition);
        }
        if ( newPosition > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException("can not set new position: " + newPosition + " too big!");
        }
        this.pos = (int) newPosition;
        return this;
    }

    public long size() {
        return size;
    }

    public SeekableByteChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException("write not supported");
    }

    public int write(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException("write not supported");
    }

}
