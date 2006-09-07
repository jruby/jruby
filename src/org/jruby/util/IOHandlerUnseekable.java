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
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.jruby.IRuby;
import org.jruby.RubyIO;

/**
 * @author enebo
 *
 */
public class IOHandlerUnseekable extends IOHandlerJavaIO {
    protected InputStream input = null;
    protected OutputStream output = null;

    /**
     * @param inStream
     * @param outStream
     * @throws IOException 
     */
    public IOHandlerUnseekable(IRuby runtime, InputStream inStream, 
                     OutputStream outStream) throws IOException {
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
	            throw new IOException("Opening nothing?");
	        }
	        
	        modes = new IOModes(runtime, mode);
        }
        fileno = RubyIO.getNewFileno();
    }
    
    public IOHandlerUnseekable(IRuby runtime, int fileno) throws IOException {
        super(runtime);
        
        switch (fileno) {
        case 0:
            input = new RubyInputStream(runtime.getIn());
            modes = new IOModes(runtime, "r");
            isOpen = true;
            break;
        case 1:
            output = runtime.getOut();
            modes = new IOModes(runtime, "w");
            isOpen = true;
            break;
        case 2:
            output = runtime.getErr();
            modes = new IOModes(runtime, "w");
            isOpen = true;
            break;
        default:
            throw new IOException("Bad file descriptor");
        }
        
        this.fileno = fileno;
    }
    
    public IOHandlerUnseekable(IRuby runtime, int fileno, String mode) throws IOException {
        super(runtime);

        modes = new IOModes(runtime, mode);
        
        if (fileno < 0 || fileno > 2) {
            throw new IOException("Bad file descriptor");
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
    
    public IOHandler cloneIOHandler() throws IOException {
        return new IOHandlerUnseekable(getRuntime(), input, output); 
    }

    // FIXME: Only close underlying stream if it is not shared by another IO.
    //  The real problem is that when an IO is dup'd, we do not actually
    //  clone the handler AND get a new fileno.  The importance of this is 
    //  that we need to make sure we only close underlying streams that are 
    //  not shared by multiple IOHandlers.

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

        // We are not allowing the underlying System stream to be closed 
        if (modes.isReadable() && input != System.in) {
            input.close();
        }
        
        // We are not allowing the underlying System stream to be closed 
        if (modes.isWriteable() && output != System.out && output != System.err) {
            output.close();
        }
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
        // Not a process.
        return -1;
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
        
        output.write(buf.getBytes("ISO8859_1"));

        // Should syswrite sync?
        if (isSync) {
            sync();
        }
            
        return buf.length();
    }

    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    public int syswrite(int c) throws IOException, BadDescriptorException {
        getRuntime().secure(4);
        checkWriteable();
        
        output.write(c);

        // Should syswrite sync?
        if (isSync) {
            sync();
        }
            
        return c;
    }
    
    public void truncate(long newLength) throws IOException, PipeException {
        throw new IOHandler.PipeException();
    }

	public FileChannel getFileChannel() {
		assert false : "No file channel for unseekable IO";
		return null;
	}
}
