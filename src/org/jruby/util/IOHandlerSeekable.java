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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.SystemCallError;

/**
 * <p>This file implements a seekable IO file.</p>
 * 
 * @author Thomas E Enebo (enebo@acm.org)
 */
public class IOHandlerSeekable extends IOHandler {
    protected RandomAccessFile file;
    protected String path;
    
    public IOHandlerSeekable(Ruby runtime, String path, IOModes modes) 
    	throws IOException {
        super(runtime);
        
        this.path = path;
        this.modes = modes;
        File theFile = new File(path);

        if (theFile.exists()) {
            if (modes.shouldTruncate()) {
                // If we only want to open for writing we should remove
                // the old file before opening the fresh one.  If it fails
                // to remove it we should do something?
                if (!theFile.delete()) {
                }
            }
        } else {
            if (modes.isReadable() && !modes.isWriteable()) {
                throw ErrnoError.getErrnoError(runtime, "ENOENT", "No such file");
            }
        }

        // We always open this rw since we can only open it r or rw.
        file = new RandomAccessFile(theFile, "rw");
        isOpen = true;
        
        if (modes.isAppendable()) {
            seek(0, SEEK_END);
        }

        // We give a fileno last so that we do not consume these when
        // we have a problem opening a file.
        fileno = RubyIO.getNewFileno();
    }
    
    public IOHandler cloneIOHandler() {
        try {
            IOHandler newHandler =
                new IOHandlerSeekable(getRuntime(), path, modes); 
            
            newHandler.seek(pos(), SEEK_CUR);
            
            return newHandler;
        } catch (IOException e) {
            throw new IOError(getRuntime(), e.toString());
        }
    }
    
    /**
     * <p>Close IO handler resources.</p>
     * 
     * @see org.jruby.util.IOHandler#close()
     */
    public void close() {
        if (!isOpen()) {
            throw ErrnoError.getErrnoError(getRuntime(), "EBADF", "Bad File Descriptor");
        }
        
        isOpen = false;

        try {
            file.close();
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
    }

    /**
     * @see org.jruby.util.IOHandler#flush()
     */
    public void flush() {
        checkWriteable();

        // No flushing a random access file.
    }

    /**
     * @see org.jruby.util.IOHandler#getInputStream()
     */
    public InputStream getInputStream() {
        return new BufferedInputStream(new DataInputBridgeStream(file));
    }

    /**
     * @see org.jruby.util.IOHandler#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return new BufferedOutputStream(new DataOutputBridgeStream(file));
    }
    
    /**
     * @see org.jruby.util.IOHandler#isEOF()
     */
    public boolean isEOF() {
        checkReadable();

        try {
            int c = file.read();
            if (c == -1) {
                return true;
            }
            file.seek(file.getFilePointer() - 1);
            return false;
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
    }
    
    /**
     * @see org.jruby.util.IOHandler#pid()
     */
    public int pid() {
        // A file is not a process.
        return -1;
    }
    
    /**
     * @see org.jruby.util.IOHandler#pos()
     */
    public long pos() {
        checkOpen();
        
        try {
            return file.getFilePointer();
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
    }
    
    public void resetByModes(IOModes modes) {
        if (modes.isAppendable()) {
            seek(0L, SEEK_END);
        } else if (modes.isWriteable()) {
            rewind();
        }
    }

    /**
     * @see org.jruby.util.IOHandler#rewind()
     */
    public void rewind() {
        seek(0, SEEK_SET);
    }
    
    /**
     * @see org.jruby.util.IOHandler#seek(long, int)
     */
    public void seek(long offset, int type) {
        checkOpen();
        
        try {
            switch (type) {
            case SEEK_SET:
                file.seek(offset);
                break;
            case SEEK_CUR:
                file.seek(file.getFilePointer() + offset);
                break;
            case SEEK_END:
                file.seek(file.length() + offset);
                break;
            }
        } catch (IOException e) {
            throw ErrnoError.getErrnoError(getRuntime(), "EINVAL", e.toString());
        }
    }

    /**
     * @see org.jruby.util.IOHandler#sync()
     */
    public void sync() throws IOException {
        file.getFD().sync();
        // RandomAccessFile is always synced?
    }
    
    /**
     * @see org.jruby.util.IOHandler#sysread()
     */
    public int sysread() throws IOException {
        return file.read();
    }
    
    /**
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    public int syswrite(String buf) {
        getRuntime().secure(4);
        checkWriteable();
        
        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) {
            return 0;
        }
        
        try {
            file.writeBytes(buf);
            
            if (isSync()) {
                sync();
            }
            
            return buf.length();
        } catch (IOException e) {
            throw new SystemCallError(getRuntime(), e.toString());
        }
    }
    
    public void truncate(long newLength) throws IOException {
        file.setLength(newLength);
    }
}
