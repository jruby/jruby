/*
 * IOHandlerProcess.java
 *
 * Copyright (C) 2004 Thomas E Enebo, Charles O Nutter
 * Thomas E Enebo <enebo@acm.org>
 * Charles O Nutter <headius@headius.com>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.util;

import java.io.BufferedInputStream;
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
public class IOHandlerProcess extends IOHandler {
    protected InputStream input = null;
    protected OutputStream output = null;
    protected Process process = null;

    public IOHandlerProcess(Ruby runtime, Process process, IOModes modes) {
        super(runtime);
        
        if (process == null) {
        	throw new IOError(runtime, "Null process");
        }
        
        this.process = process;
        this.input = new BufferedInputStream(process.getInputStream());
        this.output = process.getOutputStream();
        
        isOpen = true;

        this.modes = modes;
        fileno = RubyIO.getNewFileno();
    }
    
    public IOHandler cloneIOHandler() {
    	// may need to pass streams instead?
        return new IOHandlerProcess(getRuntime(), process, modes); 
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
            input.close();
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
        
        try {
            output.close();
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
        
        // TODO: to destroy or not destroy the process?
        process = null;
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
    	// no way to get pid, so faking it
        return process.hashCode();
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
