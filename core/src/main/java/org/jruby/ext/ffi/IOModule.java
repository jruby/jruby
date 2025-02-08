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
 * Copyright (C) 2009 JRuby project
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

package org.jruby.ext.ffi;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.OpenFile;

import static com.headius.backport9.buffer.Buffers.limitBuffer;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Error.indexError;
import static org.jruby.api.Error.typeError;

/**
* FFI specific I/O routines
 */
public class IOModule {

    public static void createIOModule(ThreadContext context, RubyModule FFI) {
        FFI.defineModuleUnder(context, "IO").defineMethods(context, IOModule.class);
    }

    @JRubyMethod(name = "native_read", module = true)
    public static final IRubyObject native_read(ThreadContext context, IRubyObject self,
            IRubyObject src, IRubyObject dst, IRubyObject rbLength) {
        if (!(src instanceof RubyIO)) throw typeError(context, "wrong argument (expected IO)");
        if (!(dst instanceof AbstractMemory)) throw typeError(context, "wrong argument (expected FFI memory)");

        try {
            OpenFile openFile = ((RubyIO) src).getOpenFile();
            openFile.checkClosed();
            openFile.checkReadable(context);


            ByteBuffer buffer = ((AbstractMemory) dst).getMemoryIO().asByteBuffer();
            int count = RubyNumeric.num2int(rbLength);

            if (count > buffer.remaining()) throw indexError(context, "read count too big for output buffer");

            if (count < buffer.remaining()) {
                buffer = buffer.duplicate();
                limitBuffer(buffer, count);
            }

            // TODO: This used to use ChannelStream and honor its buffers; it does not honor OpenFile buffers now
            return asFixnum(context, openFile.readChannel().read(buffer));
        } catch (EOFException e) {
            return asFixnum(context, -1);
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        }
    }
}
