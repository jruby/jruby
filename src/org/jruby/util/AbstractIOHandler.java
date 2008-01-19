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
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
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
package org.jruby.util;

import java.io.FileDescriptor;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.jruby.Ruby;

/**
 */
public abstract class AbstractIOHandler implements IOHandler {
    private Ruby runtime;
    protected IOModes modes;
    protected int fileno;
    protected FileDescriptor fileDescriptor = null;
    protected boolean isOpen = false;
    protected boolean isSync = false;
    
    protected AbstractIOHandler(Ruby runtime) {
        this.runtime = runtime;
    }

    public int getFileno() {
        return fileno;
    }
    
    public void setFileno(int fileno) {
        this.fileno = fileno;
    }
    
    public FileDescriptor getFD() {
        return fileDescriptor;
    }

    public Ruby getRuntime() {
        return runtime;
    }
    
    public abstract FileChannel getFileChannel();
    
    public boolean isOpen() {
        return isOpen;
    }

    public boolean isReadable() {
        return modes.isReadable();
    }

    public boolean isWritable() {
        return modes.isWritable();
    }

    public void checkOpen(String message) throws IOException {
        if (!isOpen) throw new IOException(message);
    }
    
    public void checkReadable() throws IOException, BadDescriptorException {
        if (!isOpen) throw new BadDescriptorException();
        if (!modes.isReadable()) throw new IOException("not opened for reading");
    }

    public void checkWritable() throws IOException, BadDescriptorException {
        if (!isOpen) throw new BadDescriptorException();
        if (!modes.isWritable()) throw new IOException("not opened for writing");
    }

    public void checkPermissionsSubsetOf(IOModes subsetModes) {
        subsetModes.checkSubsetOf(modes);
    }
    
    public IOModes getModes() {
    	return modes;
    }
    
    public boolean isSync() {
        return isSync;
    }

    public void setIsSync(boolean isSync) {
        this.isSync = isSync;
    }

    public void reset(IOModes subsetModes) throws IOException, InvalidValueException {
        checkPermissionsSubsetOf(subsetModes);
        
        resetByModes(subsetModes);
    }

    // TODO: We overflow on large files...We could increase to long to limit
    // this, but then the impl gets more involved since java io APIs based on
    // int (means we have to chunk up a long into a series of int ops).

    public void closeWrite() throws IOException {
    }

    /**
     * Implement IO#wait as per io/wait in MRI.
     * waits until input available or timed out and returns self, or nil when EOF reached.
     *
     * The default implementation loops while ready returns 0.
     */
    public void waitUntilReady() throws IOException, InterruptedException {
        while (ready() == 0) {
            Thread.sleep(10);
        }
    }

    public boolean hasPendingBuffered() {
        return false;
    }
    
    public static ByteList sysread(int number, ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(number);
        int bytes_read = 0;
        bytes_read = channel.read(buffer);
        if (bytes_read < 0) {
            throw new EOFException();
        }

        byte[] ret;
        if (buffer.hasRemaining()) {
            buffer.flip();
            ret = new byte[buffer.remaining()];
            buffer.get(ret);
        } else {
            ret = buffer.array();
        }
        return new ByteList(ret,false);
    }
    
    public static int syswrite(ByteList buf, WritableByteChannel channel) throws IOException {
        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) return 0;
        
        return channel.write(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
    }
    
    public static int syswrite(int c, WritableByteChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put((byte)c);
        buf.flip();
        
        return channel.write(buf);
    }
}
