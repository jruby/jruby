/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 JRuby project
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi.io;

import com.kenai.jaffl.annotations.In;
import com.kenai.jaffl.annotations.Out;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * An implementation of ByteChannel that reads from and writes to a native unix
 * file descriptor.
 */
public class FileDescriptorByteChannel implements ByteChannel {
    private final static LibC libc = com.kenai.jaffl.Library.loadLibrary("c", LibC.class);
    private final int fd;
    private volatile boolean isOpen = true;

    /**
     * Creates a new <tt>FileDescriptorByteChannel</tt>.
     *
     * @param fd The native unix fd to read/write.
     */
    public FileDescriptorByteChannel(int fd) {
        this.fd = fd;
    }

    /**
     * Reads data from the native unix file descriptor.
     * @param dst The destination <tt>ByteBuffer</tt> to place read bytes in.
     * @return The number of bytes read.
     *
     * @throws java.io.IOException If an error occurred during reading.
     */
    public int read(ByteBuffer dst) throws IOException {
        if (!isOpen) {
            throw new IOException("Not open");
        }
        int n = libc.read(fd, dst, dst.remaining());
        if (n > 0) {
            dst.position(dst.position() + n);
        }
        return n;
    }

    /**
     * Writes data to the native unix file descriptor.
     *
     * @param src The source <tt>ByteBuffer</tt> to write to the file descriptor.
     * @return The number of bytes written.
     *
     * @throws java.io.IOException If an error occurred during writing.
     */
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen) {
            throw new IOException("Not open");
        }
        int n = libc.write(fd, src, src.remaining());
        if (n > 0) {
            src.position(src.position() + n);
        }
        return n;
    }

    /**
     * Tests if the ByteChannel is open.
     *
     * @return <tt>true</tt> if the Channel is still open
     */
    public boolean isOpen() {
        return isOpen;
    }
    /**
     * Closes the <tt>Channel</tt>.
     * <p>
     * This closes the underlying native file descriptor.
     * </p>
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        if (!isOpen) {
            throw new IllegalStateException("file already closed");
        }
        isOpen = false;
        libc.close(fd);
    }

    /**
     * The native library functions used to access the file descriptor.
     */
    private static interface LibC {
        int read(int fd, @Out ByteBuffer dst, int len);
        int write(int fd, @In ByteBuffer src, int len);
        int close(int fd);
    }
}
