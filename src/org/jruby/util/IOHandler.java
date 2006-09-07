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

import java.io.EOFException;
import java.io.IOException;
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
	checkWritable();
    }

    protected void checkWritable() throws IOException, BadDescriptorException {
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

    public void reset(IOModes subsetModes) throws IOException, InvalidValueException {
        checkPermissionsSubsetOf(subsetModes);
        
        resetByModes(subsetModes);
    }

    public abstract String gets(String separatorString) throws IOException, BadDescriptorException, EOFException;
    public abstract String getsEntireStream() throws IOException, BadDescriptorException, EOFException;

    // TODO: We overflow on large files...We could increase to long to limit
    // this, but then the impl gets more involved since java io APIs based on
    // int (means we have to chunk up a long into a series of int ops).

    public abstract String read(int number) throws IOException, BadDescriptorException, EOFException;
    public abstract int write(String string) throws IOException, BadDescriptorException;

    public abstract int getc() throws IOException, BadDescriptorException, EOFException;
    public abstract void ungetc(int c);
    public abstract void putc(int c) throws IOException, BadDescriptorException;
    
    public abstract String sysread(int number) throws IOException, BadDescriptorException, EOFException;
    public abstract int syswrite(String buf) throws IOException, BadDescriptorException;
    public abstract int syswrite(int ch) throws IOException, BadDescriptorException;
    
    public abstract IOHandler cloneIOHandler() throws IOException, PipeException, InvalidValueException;
    public abstract void close() throws IOException, BadDescriptorException;
    public abstract void flush() throws IOException, BadDescriptorException;
    /**
     * <p>Flush and sync all writes to the filesystem.</p>
     * 
     * @throws IOException if the sync does not work
     */
    public abstract void sync() throws IOException, BadDescriptorException; 
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
