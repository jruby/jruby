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
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.SystemCallError;

/**
 * @author enebo
 *
 */
public class IOHandlerUnseekable extends IOHandler {
    protected InputStream input = null;
    protected OutputStream output = null;

    /**
     * @param inStream
     * @param outStream
     */
    public IOHandlerUnseekable(Ruby runtime, InputStream inStream, 
                     OutputStream outStream) {
        super(runtime);
        String mode = "";
        
        if (inStream != null) {
            // No reason to buffer the buffer (cloning trips over this)
            if (inStream instanceof BufferedInputStream) {
                input = inStream;
            } else {
                input = new BufferedInputStream(inStream);
            }
            mode += "r";
            isOpen = true;
        }
        
        if (outStream != null) {
            output = outStream;
            mode += "w";
            isOpen = true;
        }
        if ("rw".equals(mode)) {
            modes = new IOModes(runtime, IOModes.RDWR);
            isOpen = true;
        } else {
        
	        // Neither stream exists?
	        if (!isOpen) {
	            throw new IOError(runtime, "Opening nothing?");
	        }
	        
	        modes = new IOModes(runtime, mode);
        }
        fileno = RubyIO.getNewFileno();
    }
    
    public IOHandlerUnseekable(Ruby runtime, int fileno) {
        super(runtime);
        
        switch (fileno) {
        case 0:
            input = new RubyInputStream(System.in);
            modes = new IOModes(runtime, "r");
            isOpen = true;
            break;
        case 1:
            output = System.out;
            modes = new IOModes(runtime, "w");
            isOpen = true;
            break;
        case 2:
            output = System.err;
            modes = new IOModes(runtime, "w");
            isOpen = true;
            break;
        default:
            throw new IOError(getRuntime(), "Bad file descriptor");
        }
        
        this.fileno = fileno;
    }
    
    public IOHandlerUnseekable(Ruby runtime, int fileno, String mode) {
        super(runtime);

        modes = new IOModes(runtime, mode);
        
        if (fileno < 0 || fileno > 2) {
            throw new IOError(getRuntime(), "Bad file descriptor");
        }
        
        if (modes.isReadable()) {
            input = new RubyInputStream(System.in);
            isOpen = true;
            if (modes.isWriteable()) {
                output = System.out;
            }
        } else if (isWriteable()) {
            output = System.out;
            isOpen = true;
        }
        
        this.fileno = fileno;
    }
    
    public IOHandler cloneIOHandler() {
        return new IOHandlerUnseekable(getRuntime(), input, output); 
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

        if (modes.isReadable()) {
            try {
                input.close();
            } catch (IOException e) {
                throw IOError.fromException(getRuntime(), e);
            }
        }
        
        if (modes.isWriteable()) {
            try {
                output.close();
            } catch (IOException e) {
                throw IOError.fromException(getRuntime(), e);
            }
        }
    }

    /**
     * @see org.jruby.util.IOHandler#flush()
     */
    public void flush() {
        checkWriteable();

        try {
            output.flush();
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
    }
    
    /**
     * @see org.jruby.util.IOHandler#getInputStream()
     */
    public InputStream getInputStream() {
        return input;
    }

    /**
     * @see org.jruby.util.IOHandler#getOutputStream()
     */
    public OutputStream getOutputStream() {
        return output;
    }

    /**
     * @see org.jruby.util.IOHandler#isEOF()
     */
    public boolean isEOF() {
        checkReadable();

        try {
            int c = input.read();
            if (c == -1) {
                return true;
            }
            ungetc(c);
            return false;
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
    }
    
    /**
     * @see org.jruby.util.IOHandler#pid()
     */
    public int pid() {
        // Not a process.
        return -1;
    }
    
    /**
     * @see org.jruby.util.IOHandler#pos()
     */
    public long pos() {
        throw ErrnoError.getErrnoError(getRuntime(), "ESPIPE", "Illegal seek");
    }
    
    public void resetByModes(IOModes modes) {
    }
    
    /**
     * @see org.jruby.util.IOHandler#rewind()
     */
    public void rewind() {
        throw ErrnoError.getErrnoError(getRuntime(), "ESPIPE", "Illegal seek");
    }
    
    /**
     * @see org.jruby.util.IOHandler#seek(long, int)
     */
    public void seek(long offset, int type) {
        throw ErrnoError.getErrnoError(getRuntime(), "ESPIPE", "Illegal seek");
    }
    
    /**
     * @see org.jruby.util.IOHandler#sync()
     */
    public void sync() throws IOException {
        output.flush();
        
        if (output instanceof FileOutputStream) {
            ((FileOutputStream) output).getFD().sync();
        }
    }
    
    /**
     * @see org.jruby.util.IOHandler#sysread()
     */
    public int sysread() throws IOException {
        return input.read();
    }

    /**
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    public int syswrite(String buf) {
        getRuntime().secure(4);
        checkWriteable();
        
        if (buf == null || buf.length() == 0) {
            return 0;
        }
        
        try {
            output.write(buf.getBytes());

            // Should syswrite sync?
            if (isSync) {
                sync();
            }
            
            return buf.length();
        } catch (IOException e) {
            throw new SystemCallError(getRuntime(), e.toString());
        }
    }
    
    public void truncate(long newLength) throws IOException {
        throw ErrnoError.getErrnoError(getRuntime(), "ESPIPE", "Illegal seek");
    }
}
