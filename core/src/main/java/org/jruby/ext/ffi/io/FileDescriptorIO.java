/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi.io;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.ModeFlags;

/**
 * An IO implementation that reads/writes to a native file descriptor.
 */
@JRubyClass(name="FFI::" + FileDescriptorIO.CLASS_NAME, parent="IO")
public class FileDescriptorIO extends RubyIO {
    public static final String CLASS_NAME = "FileDescriptorIO";
    private static final class Allocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new FileDescriptorIO(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new Allocator();
    }

    public FileDescriptorIO(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        MakeOpenFile();
    }

    public FileDescriptorIO(Ruby runtime, IRubyObject fd) {
        super(runtime, runtime.getModule("FFI").getClass(CLASS_NAME));
        MakeOpenFile();
        ModeFlags modes;
        try {
            modes = newModeFlags(runtime, ModeFlags.RDWR);
            int fileno = RubyNumeric.fix2int(fd);
            jnr.posix.FileStat stat = runtime.getPosix().fstat(fileno);
            java.nio.channels.ByteChannel channel;

            if (stat.isSocket()) {
                channel = new jnr.enxio.channels.NativeSocketChannel(fileno);
            } else if (stat.isBlockDev() || stat.isCharDev()) {
                channel = new jnr.enxio.channels.NativeDeviceChannel(fileno);
            } else {
                channel = new FileDescriptorByteChannel(runtime, fileno);
            }

            openFile.setMainStream(ChannelStream.open(getRuntime(), new ChannelDescriptor(channel, modes, FileDescriptorHelper.wrap(fileno))));
            openFile.setPipeStream(openFile.getMainStreamSafe());
            openFile.setMode(modes.getOpenFileFlags());
            openFile.getMainStreamSafe().setSync(true);
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
    }

    public static RubyClass createFileDescriptorIOClass(Ruby runtime, RubyModule module) {
        RubyClass result = runtime.defineClassUnder(CLASS_NAME, runtime.getClass("IO"),
                Allocator.INSTANCE, module);
        result.defineAnnotatedMethods(FileDescriptorIO.class);
        result.defineAnnotatedConstants(FileDescriptorIO.class);

        return result;
    }

    @JRubyMethod(name = "new", meta = true)
    public static FileDescriptorIO newInstance(ThreadContext context, IRubyObject recv, IRubyObject fd) {
        return new FileDescriptorIO(context.runtime, fd);
    }

    @JRubyMethod(name = "wrap", required = 1, meta = true)
    public static RubyIO wrap(ThreadContext context, IRubyObject recv, IRubyObject fd) {
        return new FileDescriptorIO(context.runtime, fd);
    }

    static class FileDescriptorHelper {
        static java.lang.reflect.Constructor<java.io.FileDescriptor> CONSTRUCTOR;
        static {
            java.lang.reflect.Constructor<java.io.FileDescriptor> constructor;
            try {
                constructor = java.io.FileDescriptor.class.getDeclaredConstructor(int.class);
                constructor.setAccessible(true);
            } catch (Throwable t) {
                constructor = null;
            }
            CONSTRUCTOR = constructor;
        }

        public static java.io.FileDescriptor wrap(int fileno) {
            try {
                return CONSTRUCTOR != null ? CONSTRUCTOR.newInstance(fileno) : new java.io.FileDescriptor();
            } catch (IllegalAccessException iae) {
                return new java.io.FileDescriptor();
            } catch (InstantiationException ie) {
                return new java.io.FileDescriptor();
            } catch (java.lang.reflect.InvocationTargetException ite) {
                return new java.io.FileDescriptor();
            }
        }
    }
}
