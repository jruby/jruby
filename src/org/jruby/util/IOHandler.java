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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.jruby.IRuby;

/**
 */
public abstract class IOHandler {
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    
    // We use a highly uncommon string to represent the paragraph delimeter. 
    // The 100% solution is not really worth the extra code.
    public static final String PARAGRAPH_DELIMETER = "PARAGRPH_DELIM_MRK_ER";
    
    private IRuby runtime;
    protected IOModes modes;
    protected int fileno;
    protected boolean isOpen = false;
    protected boolean isSync = false;
    
    // Last char to be 'ungot'.  <0 indicates nothing waiting to be re-got  
    private int ungotc = -1;
    
    protected IOHandler(IRuby runtime) {
        this.runtime = runtime;
    }

    public int getFileno() {
        return fileno;
    }
    
    public void setFileno(int fileno) {
        this.fileno = fileno;
    }

    protected IRuby getRuntime() {
        return runtime;
    }
    
    public abstract FileChannel getFileChannel();
    
    public boolean isReadable() {
        return modes.isReadable();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isWriteable() {
        return modes.isWriteable();
    }

    protected void checkOpen() throws IOException {
        if (!isOpen) {
            throw new IOException("not opened");
        }
    }
    
    protected void checkReadable() throws IOException, BadDescriptorException {
        if (!isOpen) {
        	throw new BadDescriptorException();
        }
        
        if (!modes.isReadable()) {
            throw new IOException("not opened for reading");
        }
    }

    protected void checkWriteable() throws IOException, BadDescriptorException {
        if (!isOpen) {
            throw new BadDescriptorException();
        }
        
        if (!modes.isWriteable()) {
            throw new IOException("not opened for writing");
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
    
    public String gets(String separatorString) throws IOException, BadDescriptorException {
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
    
    public String getsEntireStream() throws IOException {
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
    
    public int read() throws IOException {
        try {
            if (ungotc >= 0) {
                int c = ungotc;
                ungotc = -1;
                return c;
            }
            
            return sysread();
        } catch (EOFException e) {
            return -1;
        }
    }
    
    public int getc() throws IOException, BadDescriptorException {
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
    
    public String read(int number) throws IOException, BadDescriptorException {
        try {
            
            if (ungotc >= 0) {
                String buf2 = sysread(number - 1);
                int c = ungotc;
                ungotc = -1;
                return c + buf2;
            } 

            return sysread(number);
        } catch (EOFException e) {
            return null;
        }
    }
    
    public void ungetc(int c) {
        // Ruby silently ignores negative ints for some reason?
        if (c >= 0) {
            ungotc = c;
        }
    }
    
    public void putc(int c) throws IOException, BadDescriptorException {
        try {
            syswrite("" + (char) c);         // LAME
        } catch (IOException e) {
        }
    }
    
    public void reset(IOModes subsetModes) throws IOException, InvalidValueException {
        checkPermissionsSubsetOf(subsetModes);
        
        resetByModes(subsetModes);
    }
    
    public int write(String string) throws IOException, BadDescriptorException {
        return syswrite(string);
    }
    
    protected int sysread(StringBuffer buf, int length) throws IOException {
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
    public String sysread(int number) throws IOException, BadDescriptorException {
        if (!isOpen()) {
            throw new IOException("File not open");
        }
        checkReadable();
        
        StringBuffer buf = new StringBuffer();
        int position = 0;
        
        while (position < number) {
            int s = sysread(buf, number - position);
                
            if (s == -1) {
                if (position <= 0) {
                    throw new EOFException();
                }
                break;
            }
                
            position += s;
        }
            
        return buf.toString();
    }
    
    
    public abstract IOHandler cloneIOHandler() throws IOException, PipeException, InvalidValueException;
    public abstract void close() throws IOException, BadDescriptorException;
    public abstract void flush() throws IOException, BadDescriptorException;
    public abstract InputStream getInputStream();
    public abstract OutputStream getOutputStream();
    
    /**
     * <p>Return true when at end of file (EOF).</p>
     * 
     * @return true if at EOF; false otherwise
     * @throws IOException 
     * @throws BadDescriptorException 
     */
    public abstract boolean isEOF() throws IOException, BadDescriptorException;
    
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
     * @throws IOException 
     * @throws PipeException ESPIPE (illegal seek) when not a file 
     * 
     */
    public abstract long pos() throws IOException, PipeException;
    
    protected abstract void resetByModes(IOModes newModes) throws IOException, InvalidValueException;
    public abstract void rewind() throws IOException, PipeException, InvalidValueException;
    
    /**
     * <p>Perform a seek based on pos().  </p> 
     * @throws IOException 
     * @throws PipeException 
     * @throws InvalidValueException 
     */
    public abstract void seek(long offset, int type) throws IOException, PipeException, InvalidValueException;
    /**
     * <p>Flush and sync all writes to the filesystem.</p>
     * 
     * @throws IOException if the sync does not work
     */
    public abstract void sync() throws IOException; 
    public abstract int sysread() throws IOException;
    public abstract int syswrite(String buf) throws IOException, BadDescriptorException;
    public abstract void truncate(long newLength) throws IOException, PipeException;
    
    public class PipeException extends Exception {
		private static final long serialVersionUID = 1L;
    }
    public class BadDescriptorException extends Exception {
		private static final long serialVersionUID = 1L;
    }
    public class InvalidValueException extends Exception {
		private static final long serialVersionUID = 1L;
    }
}
