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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.jruby.IRuby;
import org.jruby.RubyIO;

public class IOHandlerProcess extends IOHandlerJavaIO {
    protected InputStream input = null;
    protected OutputStream output = null;
    protected Process process = null;

    public IOHandlerProcess(IRuby runtime, Process process, IOModes modes) throws IOException {
        super(runtime);
        
        if (process == null) {
        	throw new IOException("Null process");
        }
        
        this.process = process;
        this.input = new BufferedInputStream(process.getInputStream());
        this.output = process.getOutputStream();
        
        isOpen = true;

        this.modes = modes;
        fileno = RubyIO.getNewFileno();
    }
    
    public IOHandler cloneIOHandler() throws IOException {
    	// may need to pass streams instead?
        return new IOHandlerProcess(getRuntime(), process, modes); 
    }

    /**
     * <p>Close IO handler resources.</p>
     * @throws IOException 
     * @throws BadDescriptorException 
     * 
     * @see org.jruby.util.IOHandler#close()
     */
    public void close() throws IOException, BadDescriptorException {
        if (!isOpen()) {
        	throw new BadDescriptorException();
        }
        
        isOpen = false;

        input.close();
        output.close();
        
        // TODO: to destroy or not destroy the process?
        process = null;
    }

    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#flush()
     */
    public void flush() throws IOException, BadDescriptorException {
        checkWriteable();

        output.flush();
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
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#isEOF()
     */
    public boolean isEOF() throws IOException, BadDescriptorException {
        checkReadable();

        int c = input.read();
        if (c == -1) {
            return true;
        }
        ungetc(c);
        return false;
    }
    
    /**
     * @see org.jruby.util.IOHandler#pid()
     */
    public int pid() {
    	// no way to get pid, so faking it
        return process.hashCode();
    }
    
    /**
     * @throws PipeException 
     * @see org.jruby.util.IOHandler#pos()
     */
    public long pos() throws PipeException {
        throw new IOHandler.PipeException();
    }
    
    public void resetByModes(IOModes newModes) {
    }
    
    /**
     * @throws PipeException 
     * @see org.jruby.util.IOHandler#rewind()
     */
    public void rewind() throws PipeException {
        throw new IOHandler.PipeException();
    }
    
    /**
     * @throws PipeException 
     * @see org.jruby.util.IOHandler#seek(long, int)
     */
    public void seek(long offset, int type) throws PipeException {
        throw new IOHandler.PipeException();
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
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    public int syswrite(String buf) throws IOException, BadDescriptorException {
        getRuntime().secure(4);
        checkWriteable();
        
        if (buf == null || buf.length() == 0) {
            return 0;
        }
        
        output.write(buf.getBytes());

        // Should syswrite sync?
        if (isSync) {
            sync();
        }
            
        return buf.length();
    }
    
    public void truncate(long newLength) throws IOException, PipeException {
        throw new IOHandler.PipeException();
    }

	public FileChannel getFileChannel() {
		assert false : "No file channel for process streams";
		return null;
	}
}
