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
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.jruby.IRuby;
import org.jruby.RubyIO;

/**
 * <p>This file implements a seekable IO file.</p>
 * 
 * @author Thomas E Enebo (enebo@acm.org)
 */
public class IOHandlerSeekable extends IOHandlerJavaIO {
    protected RandomAccessFile file;
    protected String path;
    
    public IOHandlerSeekable(IRuby runtime, String path, IOModes modes) 
    	throws IOException, InvalidValueException {
        super(runtime);
        
        this.path = path;
        this.modes = modes;
        JRubyFile theFile = JRubyFile.create(runtime.getCurrentDirectory(),path);
        
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
                throw new FileNotFoundException();
            }
        }

		// Do not open as 'rw' if we don't need to since a file with permissions for read-only
		// will barf if opened 'rw'.
		String javaMode = "r";
		if (modes.isWriteable()) {
			javaMode += "w";
		}
		
        // We always open this rw since we can only open it r or rw.
        file = new RandomAccessFile(theFile, javaMode);
        isOpen = true;
        
        if (modes.isAppendable()) {
            seek(0, SEEK_END);
        }

        // We give a fileno last so that we do not consume these when
        // we have a problem opening a file.
        fileno = RubyIO.getNewFileno();
    }
    
    public IOHandler cloneIOHandler() throws IOException, PipeException, InvalidValueException {
        IOHandler newHandler = new IOHandlerSeekable(getRuntime(), path, modes); 
            
        newHandler.seek(pos(), SEEK_CUR);
            
        return newHandler;
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

        file.close();
    }

    /**
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#flush()
     */
    public void flush() throws IOException, BadDescriptorException {
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
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#isEOF()
     */
    public boolean isEOF() throws IOException, BadDescriptorException {
        checkReadable();

        int c = file.read();
        if (c == -1) {
            return true;
        }
        file.seek(file.getFilePointer() - 1);
        return false;
    }
    
    /**
     * @see org.jruby.util.IOHandler#pid()
     */
    public int pid() {
        // A file is not a process.
        return -1;
    }
    
    /**
     * @throws IOException 
     * @see org.jruby.util.IOHandler#pos()
     */
    public long pos() throws IOException {
        checkOpen();
        
        return file.getFilePointer();
    }
    
    public void resetByModes(IOModes newModes) throws IOException, InvalidValueException {
        if (newModes.isAppendable()) {
            seek(0L, SEEK_END);
        } else if (newModes.isWriteable()) {
            rewind();
        }
    }

    /**
     * @throws IOException 
     * @throws InvalidValueException 
     * @see org.jruby.util.IOHandler#rewind()
     */
    public void rewind() throws IOException, InvalidValueException {
        seek(0, SEEK_SET);
    }
    
    /**
     * @throws IOException 
     * @throws InvalidValueException 
     * @see org.jruby.util.IOHandler#seek(long, int)
     */
    public void seek(long offset, int type) throws IOException, InvalidValueException {
        checkOpen();

        // TODO:  This seems wrong...Invalid value should be for switch..not any IOError?
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
        	throw new InvalidValueException();
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
     * @throws IOException 
     * @throws BadDescriptorException 
     * @see org.jruby.util.IOHandler#syswrite(String buf)
     */
    public int syswrite(String buf) throws IOException, BadDescriptorException {
        getRuntime().secure(4);
        checkWriteable();
        
        // Ruby ignores empty syswrites
        if (buf == null || buf.length() == 0) {
            return 0;
        }
        
        file.writeBytes(buf);
            
        if (isSync()) {
            sync();
        }
            
        return buf.length();
    }
    
    public void truncate(long newLength) throws IOException {
        file.setLength(newLength);
    }

	public FileChannel getFileChannel() {
		return file.getChannel();
	}
}
