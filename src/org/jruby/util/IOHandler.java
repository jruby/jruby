/*
 * IOHandler.java
 *
 * Copyright (C) 2004 Thomas E Enebo, Charles O Nutter
 * Thomas E Enebo <enebo@acm.org>
 * Charles O Nutter <headuis@headius.com>
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

import org.jruby.Ruby;
import org.jruby.exceptions.EOFError;
import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.SystemCallError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 */
public abstract class IOHandler {
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    
    // We use a highly uncommon string to represent the paragraph delimeter. 
    // The 100% solution is not really worth the extra code.
    public static final String PARAGRAPH_DELIMETER = "PARAGRPH_DELIM_MRK_ER";
    
    private Ruby runtime;
    protected IOModes modes;
    protected int fileno;
    protected boolean isOpen = false;
    protected boolean isSync = false;
    
    // Last char to be 'ungot'.  <0 indicates nothing waiting to be re-got  
    private int ungotc = -1;
    
    protected IOHandler(Ruby runtime) {
        this.runtime = runtime;
    }

    public int getFileno() {
        return fileno;
    }
    
    public void setFileno(int fileno) {
        this.fileno = fileno;
    }

    protected Ruby getRuntime() {
        return runtime;
    }
    
    public boolean isReadable() {
        return modes.isReadable();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isWriteable() {
        return modes.isWriteable();
    }

    protected void checkOpen() {
        if (!isOpen) {
            throw new IOError(getRuntime(), "not opened");
        }
    }
    
    protected void checkReadable() {
        if (!isOpen) {
            throw ErrnoError.getErrnoError(getRuntime(), "EBADF",
                    "Bad file descriptor");
        }
        
        if (!modes.isReadable()) {
            throw new IOError(getRuntime(), "not opened for reading");
        }
    }

    protected void checkWriteable() {
        if (!isOpen) {
            throw ErrnoError.getErrnoError(getRuntime(), "EBADF",
            "Bad file descriptor");
        }
        
        if (!modes.isWriteable()) {
            throw new IOError(getRuntime(), "not opened for writing");
        }
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
    
    public String gets(String separatorString) {
        checkReadable();
        
        if (separatorString == null) {
            return getsEntireStream();
        }
        
        final char[] separator = separatorString.equals(PARAGRAPH_DELIMETER) ?
        		"\n\n".toCharArray() : separatorString.toCharArray();

        int c = read();
        if (c == -1) {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        LineLoop : while (true) {
            while (c != separator[0] && c != -1) {
                buffer.append((char) c);
                c = read();
            }
            for (int i = 0; i < separator.length; i++) {
                if (c == -1) {
                    break LineLoop;
                } else if (c != separator[i]) {
                    continue LineLoop;
                }
                buffer.append((char) c);
                if (i < separator.length - 1) {
                    c = read();
                }
            }
            break;
        }
        
        if (separatorString.equals(PARAGRAPH_DELIMETER)) {
            while (c == separator[0]) {
                c = read();
            }
            ungetc(c);
        }
        
        return buffer.toString();
    }
    
    public String getsEntireStream() {
        StringBuffer result = new StringBuffer();
        int c;
        while ((c = read()) != -1) {
            result.append((char) c);
        }
        
        // We are already at EOF
        if (result.length() == 0) {
            return null;
        }
        
        return result.toString();
    }
    
    public int read() {
        try {
            if (ungotc >= 0) {
                int c = ungotc;
                ungotc = -1;
                return c;
            }
            
            return sysread();
        } catch (SystemCallError e) {
            throw new IOError(getRuntime(), e.getMessage());
        } catch (EOFError e) {
            return -1;
        } catch (IOException e) {
            throw new IOError(getRuntime(), e.getMessage());
        }
    }
    
    public int getc() {
        checkReadable();
        
        int c = read();
        
        if (c == -1) {
            return c;
        }
        return c & 0xff;
    }

    // TODO: We overflow on large files...We could increase to long to limit
    // this, but then the impl gets more involved since java io APIs based on
    // int (means we have to chunk up a long into a series of int ops).
    
    public String read(int number) {
        try {
            
            if (ungotc >= 0) {
                String buf2 = sysread(number - 1);
                int c = ungotc;
                ungotc = -1;
                return c + buf2;
            } 

            return sysread(number);
        } catch (SystemCallError e) {
            throw new IOError(getRuntime(), e.getMessage());
        } catch (EOFError e) {
            return null;
        }
    }
    
    public void ungetc(int c) {
        // Ruby silently ignores negative ints for some reason?
        if (c >= 0) {
            ungotc = c;
        }
    }
    
    public void putc(int c) {
        try {
            syswrite("" + (char) c);         // LAME
        } catch (IOError e) {
        }
    }
    
    public void reset(IOModes subsetModes) {
        checkPermissionsSubsetOf(subsetModes);
        
        resetByModes(subsetModes);
    }
    
    public int write(String string) {
        try {
            return syswrite(string);
        } catch (SystemCallError e) {
            throw new IOError(getRuntime(), e.getMessage());
        }
    }
    
    private int sysread(StringBuffer buf, int length) throws IOException {
        if (buf == null) {
            throw new IOException("sysread2: Buf is null");
        }
        
        int i = 0;
        for (;i < length; i++) {
            int c = sysread();
            
            if (c == -1) {
                if (i <= 0) {
                    return -1;
                }
                break;
            }
            
            buf.append((char) c);
        }
        
        return i;
    }

    // Question: We should read bytes or chars?
    public String sysread(int number) {
        if (!isOpen()) {
            throw new IOError(getRuntime(), "File not open");
        }
        checkReadable();
        
        StringBuffer buf = new StringBuffer();
        int position = 0;
        
        try {
            while (position < number) {
                int s = sysread(buf, number - position);
                
                if (s == -1) {
                    if (position <= 0) {
                        throw new EOFError(getRuntime());
                    }
                    break;
                }
                
                position += s;
            }
            
            return buf.toString();
        } catch (IOException e) {
            throw new SystemCallError(getRuntime(), e.toString());
        }
    }
    
    
    public abstract IOHandler cloneIOHandler();
    public abstract void close();
    public abstract void flush();
    public abstract InputStream getInputStream();
    public abstract OutputStream getOutputStream();
    
    /**
     * <p>Return true when at end of file (EOF).</p>
     * 
     * @return true if at EOF; false otherwise
     */
    public abstract boolean isEOF();
    
    /**
     * <p>Get the process ID associated with this handler.</p>
     * 
     * @return the pid if the IOHandler represents a process; otherwise -1
     */
    public abstract int pid();
    
    /**
     * <p>Get the current position within the file associated with this
     * handler.</p>  
     * 
     * @return the current position in the file.
     * @throws ErrnoError ESPIPE (illegal seek) when not a file
     * 
     */
    public abstract long pos();
    
    protected abstract void resetByModes(IOModes modes);
    public abstract void rewind();
    
    /**
     * <p>Perform a seek based on pos().  </p> 
     */
    public abstract void seek(long offset, int type);
    /**
     * <p>Flush and sync all writes to the filesystem.</p>
     * 
     * @throws IOException if the sync does not work
     */
    public abstract void sync() throws IOException; 
    public abstract int sysread() throws IOException;
    public abstract int syswrite(String buf);
    public abstract void truncate(long newLength) throws IOException;
}
