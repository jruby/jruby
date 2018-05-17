/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.callsite.RespondToCallSite;

/**
 * Wrap an IO object in a Channel.
 *
 * @see IOReadableByteChannel
 * @see IOWritableByteChannel
 * @see IOReadableWritableByteChannel
 */
public abstract class IOChannel implements Channel {
    private final IRubyObject io;
    private final CallSite closeAdapter = MethodIndex.getFunctionalCallSite("close");
    private final RespondToCallSite respondToClosed = new RespondToCallSite("closed?");
    private final CallSite isClosedAdapter = MethodIndex.getFunctionalCallSite("closed?");
    private final Ruby runtime;

    protected IOChannel(final IRubyObject io) {
        this.io = io;
        this.runtime = io.getRuntime();
    }
    
    public void close() throws IOException {
        ThreadContext context = runtime.getCurrentContext();

        // no call site use here since this will likely only be called once
        if (io.respondsTo("close")) {
            closeAdapter.call(context, io, io);
        }

        // can't close, assume it doesn't need to be
    }

    public boolean isOpen() {
        ThreadContext context = runtime.getCurrentContext();

        if (respondToClosed.respondsTo(context, io, io)) {
            return !isClosedAdapter.call(context, io, io).isTrue();
        }

        // can't determine, assume it's open
        return true;
    }

    protected int read(CallSite read, ByteBuffer dst) throws IOException {
        IRubyObject readValue = read.call(runtime.getCurrentContext(), io, io, runtime.newFixnum(dst.remaining()));
        int returnValue = -1;
        if (!readValue.isNil()) {
            ByteList str = ((RubyString)readValue).getByteList();
            dst.put(str.getUnsafeBytes(), str.getBegin(), str.getRealSize());
            returnValue = str.getRealSize();
        }
        return returnValue;
    }

    protected int write(CallSite write, ByteBuffer src) throws IOException {
        ByteList buffer;

        if (src.hasArray()) {
            buffer = new ByteList(src.array(), src.position(), src.remaining(), true);
        } else {
            buffer = new ByteList(src.remaining());
            buffer.append(src, src.remaining());
        }
        IRubyObject written = write.call(runtime.getCurrentContext(), io, io, RubyString.newStringLight(runtime, buffer));
        return (int)written.convertToInteger().getLongValue();
    }

    protected CallSite initReadSite(String readMethod) {
        // no call site use here since this will only be called once
        if(io.respondsTo(readMethod)) {
            return MethodIndex.getFunctionalCallSite(readMethod);
        } else {
            throw new IllegalArgumentException(io.getMetaClass() + "not coercible to " + getClass().getSimpleName() + ": no `" + readMethod + "' method");
        }
    }

    protected CallSite initWriteSite() {
        // no call site use here since this will only be called once
        if(io.respondsTo("write")) {
            return MethodIndex.getFunctionalCallSite("write");
        } else if (io.respondsTo("<<")) {
            return MethodIndex.getFunctionalCallSite("<<");
        } else {
            throw new IllegalArgumentException(io.getMetaClass() + "not coercible to " + getClass().getSimpleName() + ": no `write' method");
        }
    }

    /**
     * A {@link ReadableByteChannel} wrapper around an IO-like Ruby object.
     */
    public static class IOReadableByteChannel extends IOChannel implements ReadableByteChannel {
        private final CallSite read;

        public IOReadableByteChannel(final IRubyObject io) {
            this(io, "read");
        }

        public IOReadableByteChannel(final IRubyObject io, final String readMethod) {
            super(io);
            read = initReadSite(readMethod);
        }
        
        public int read(ByteBuffer dst) throws IOException {
            return read(read, dst);
        }
    }


    /**
     * A {@link WritableByteChannel} wrapper around an IO-like Ruby object.
     */
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


    /**
     * A {@link ReadableByteChannel} and {@link WritableByteChannel} wrapper around an IO-like Ruby object.
     */
    public static class IOReadableWritableByteChannel extends IOChannel implements ReadableByteChannel, WritableByteChannel {
        private final CallSite write;
        private final CallSite read;
        public IOReadableWritableByteChannel(final IRubyObject io) {
            super(io);
            read = initReadSite("read");
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
