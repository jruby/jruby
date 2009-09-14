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
 * Copyright (C) 2008 The JRuby Community <www.jruby.org>
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
package org.jruby.util.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.jruby.util.ByteList;
import org.jruby.Ruby;

/**
 */
public interface Stream {
    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    
    // We use a highly uncommon string to represent the paragraph delimiter (100% soln not worth it) 
    public static final ByteList PARAGRAPH_DELIMETER = ByteList.create("PARAGRPH_DELIM_MRK_ER");
    
    public static final ByteList PARAGRAPH_SEPARATOR = ByteList.create("\n\n");
    
    public ChannelDescriptor getDescriptor();
    
    public void clearerr();
    
    public ModeFlags getModes();
    
    public boolean isSync();

    public void setSync(boolean sync);

    public abstract ByteList fgets(ByteList separatorString) throws IOException, BadDescriptorException, EOFException;
    public abstract ByteList readall() throws IOException, BadDescriptorException, EOFException;
    

    /**
     * Read all bytes up to and including a terminator byte.
     *
     * <p>If the terminator byte is found, it will be the last byte in the output buffer.</p>
     *
     * @param dst The output buffer.
     * @param terminator The byte to terminate reading.
     * @return The number of bytes read, or -1 if EOF is reached.
     *
     * @throws java.io.IOException
     * @throws org.jruby.util.io.BadDescriptorException
     */
    public abstract int getline(ByteList dst, byte terminator) throws IOException, BadDescriptorException;

    /**
     * Reads all bytes up to and including a terminator byte or until limit is reached.
     *
     * <p>If the terminator byte is found, it will be the last byte in the output buffer.</p>
     *
     * @param dst The output buffer.
     * @param terminator The byte to terminate reading.
     * @param limit the number of bytes to read unless EOF or terminator is found
     * @return The number of bytes read, or -1 if EOF is reached.
     *
     * @throws java.io.IOException
     * @throws org.jruby.util.io.BadDescriptorException
     */
    public abstract int getline(ByteList dst, byte terminator, long limit) throws IOException, BadDescriptorException;
    
    // TODO: We overflow on large files...We could increase to long to limit
    // this, but then the impl gets more involved since java io APIs based on
    // int (means we have to chunk up a long into a series of int ops).

    public abstract ByteList fread(int number) throws IOException, BadDescriptorException, EOFException;
    public abstract int fwrite(ByteList string) throws IOException, BadDescriptorException;

    public abstract int fgetc() throws IOException, BadDescriptorException, EOFException;
    public abstract int ungetc(int c);
    public abstract void fputc(int c) throws IOException, BadDescriptorException;
    
    public abstract ByteList read(int number) throws IOException, BadDescriptorException, EOFException;
    
    public abstract void fclose() throws IOException, BadDescriptorException;
    public abstract int fflush() throws IOException, BadDescriptorException;
    
    /**
     * <p>Flush and sync all writes to the filesystem.</p>
     * 
     * @throws IOException if the sync does not work
     */
    public void sync() throws IOException, BadDescriptorException;
    
    /**
     * <p>Return true when at end of file (EOF).</p>
     * 
     * @return true if at EOF; false otherwise
     * @throws IOException 
     * @throws BadDescriptorException 
     */
    public boolean feof() throws IOException, BadDescriptorException;
    
    /**
     * <p>Get the current position within the file associated with this
     * handler.</p>  
     * 
     * @return the current position in the file.
     * @throws IOException 
     * @throws PipeException ESPIPE (illegal seek) when not a file 
     * 
     */
    public long fgetpos() throws IOException, PipeException, BadDescriptorException, InvalidValueException;
    
    /**
     * <p>Perform a seek based on pos().  </p> 
     * @throws IOException 
     * @throws PipeException 
     * @throws InvalidValueException 
     */
    public void lseek(long offset, int type) throws IOException, InvalidValueException, PipeException, BadDescriptorException;
    public void ftruncate(long newLength) throws IOException, PipeException,
            InvalidValueException, BadDescriptorException;
    
    /**
     * Implement IO#ready? as per io/wait in MRI.
     * returns non-nil if input available without blocking, or nil.
     */
    public int ready() throws IOException;

    /**
     * Implement IO#wait as per io/wait in MRI.
     * waits until input available or timed out and returns self, or nil when EOF reached.
     *
     * The default implementation loops while ready returns 0.
     */
    public void waitUntilReady() throws IOException, InterruptedException;

    public boolean readDataBuffered();
    public boolean writeDataBuffered();
    
    public InputStream newInputStream();
    
    public OutputStream newOutputStream();
    
    public boolean isBlocking();
    
    public void setBlocking(boolean blocking) throws IOException;
    
    public void freopen(Ruby runtime, String path, ModeFlags modes) throws DirectoryAsFileException, IOException, InvalidValueException, PipeException, BadDescriptorException;
}
