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
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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

public class IOHandlerNull extends IOHandler {
    
    public IOHandlerNull(IRuby runtime, IOModes modes) {
        super(runtime);
        
        this.modes = modes;
    }

    public FileChannel getFileChannel() {
        // TODO Auto-generated method stub
        return null;
    }

    public String gets(String separatorString) throws IOException, BadDescriptorException, 
        EOFException {
        throw new EOFException();
    }

    public String getsEntireStream() throws IOException,
            BadDescriptorException, EOFException {
        throw new EOFException();
    }

    public String read(int number) throws IOException, BadDescriptorException,
            EOFException {
        throw new EOFException();
    }

    public int write(String string) throws IOException, BadDescriptorException {
        return string.length();
    }

    public int getc() throws IOException, BadDescriptorException, EOFException {
        throw new EOFException();
    }

    public void ungetc(int c) {
    }

    public void putc(int c) throws IOException, BadDescriptorException {
    }

    public String sysread(int number) throws IOException, BadDescriptorException, EOFException {
        throw new EOFException();
    }

    public int syswrite(String buf) throws IOException, BadDescriptorException {
        return buf.length();
    }

    public int syswrite(int c) throws IOException, BadDescriptorException {
        return 1;
    }

    public IOHandler cloneIOHandler() throws IOException, PipeException, InvalidValueException {
        return new IOHandlerNull(getRuntime(), modes);
    }

    public void close() throws IOException, BadDescriptorException {
    }

    public void flush() throws IOException, BadDescriptorException {
    }

    public void sync() throws IOException, BadDescriptorException {
    }

    public boolean isEOF() throws IOException, BadDescriptorException {
        return true;
    }

    public int pid() {
        return 0;
    }

    public long pos() throws IOException, PipeException {
        return 0;
    }

    protected void resetByModes(IOModes newModes) throws IOException, InvalidValueException {
    }

    public void rewind() throws IOException, PipeException, InvalidValueException {
    }

    public void seek(long offset, int type) throws IOException, PipeException, 
        InvalidValueException {
    }

    public void truncate(long newLength) throws IOException, PipeException {
    }
}
