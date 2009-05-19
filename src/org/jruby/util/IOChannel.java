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
 * Copyright (C) 2009 Sun Microsystems, Inc
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.jruby.RubyString;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.MethodIndex;

/**
 * This class wraps a IRubyObject in an OutputStream. Depending on which messages
 * the IRubyObject answers to, it will have different functionality.
 * 
 * The point is that the IRubyObject could exhibit duck typing, in the style of IO versus StringIO, for example.
 *
 * At the moment, the only functionality supported is writing, and the only requirement on the io-object is
 * that it responds to write() and close() like IO.
 * 
 * @author <a href="mailto:Ola.Bini@ki.se">Ola Bini</a>
 */
public abstract class IOChannel implements Channel {
    private final IRubyObject io;
    private final CallSite closeAdapter = MethodIndex.getFunctionalCallSite("close");

    /**
     * Creates a new OutputStream with the object provided.
     *
     * @param io the ruby object
     */
    protected IOChannel(final IRubyObject io) {
        this.io = io;
    }
    
    public void close() throws IOException {
        if (io.respondsTo("close")) closeAdapter.call(io.getRuntime().getCurrentContext(), io, io);
    }

    public boolean isOpen() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected int read(CallSite read, ByteBuffer dst) throws IOException {
        IRubyObject readValue = read.call(io.getRuntime().getCurrentContext(), io, io, io.getRuntime().newFixnum(dst.remaining()));
        int returnValue = -1;
        if (!readValue.isNil()) {
            ByteList str = ((RubyString)readValue).getByteList();
            dst.put(str.bytes, str.begin, str.realSize);
            returnValue = str.realSize;
        }
        return returnValue;
    }

    protected int write(CallSite write, ByteBuffer src) throws IOException {
        ByteList buffer = new ByteList(src.array(), src.position(), src.remaining(), false);
        IRubyObject written = write.call(io.getRuntime().getCurrentContext(), io, io, RubyString.newStringLight(io.getRuntime(), buffer));
        return (int)written.convertToInteger().getLongValue();
    }

    protected CallSite initReadSite() {
        if(io.respondsTo("read")) {
            return MethodIndex.getFunctionalCallSite("read");
        } else {
            throw new IllegalArgumentException(io.getMetaClass() + "not coercible to " + getClass().getSimpleName() + ": no `read' method");
        }
    }

    protected CallSite initWriteSite() {
        if(io.respondsTo("write")) {
            return MethodIndex.getFunctionalCallSite("write");
        } else if (io.respondsTo("<<")) {
            return MethodIndex.getFunctionalCallSite("<<");
        } else {
            throw new IllegalArgumentException(io.getMetaClass() + "not coercible to " + getClass().getSimpleName() + ": no `write' method");
        }
    }
    
    public static class IOReadableByteChannel extends IOChannel implements ReadableByteChannel {
        private final CallSite read;

        public IOReadableByteChannel(final IRubyObject io) {
            super(io);
            read = initReadSite();
        }
        
        public int read(ByteBuffer dst) throws IOException {
            return read(read, dst);
        }
    }

    public static class IOWritableByteChannel extends IOChannel implements WritableByteChannel {
        private final CallSite write;
        public IOWritableByteChannel(final IRubyObject io) {
            super(io);
            write = initWriteSite();
        }

        public int write(ByteBuffer src) throws IOException {
            return write(write, src);
        }
    }

    public static class IOReadableWritableByteChannel extends IOChannel implements ReadableByteChannel, WritableByteChannel {
        private final CallSite write;
        private final CallSite read;
        public IOReadableWritableByteChannel(final IRubyObject io) {
            super(io);
            read = initReadSite();
            write = initWriteSite();
        }

        public int read(ByteBuffer dst) throws IOException {
            return read(read, dst);
        }

        public int write(ByteBuffer src) throws IOException {
            return write(write, src);
        }
    }
}
