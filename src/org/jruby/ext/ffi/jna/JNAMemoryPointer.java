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
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi.jna;

import org.jruby.ext.ffi.*;
import com.sun.jna.Pointer;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
@JRubyClass(name = "FFI::MemoryPointer", parent = "FFI::BasePointer")
public class JNAMemoryPointer extends JNABasePointer {
    public static final String MEMORY_POINTER_NAME = "MemoryPointer";

    public static RubyClass createMemoryPointerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(MEMORY_POINTER_NAME, 
                module.getClass(JNABasePointer.JNA_POINTER_NAME),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(JNAMemoryPointer.class);
        result.defineAnnotatedConstants(JNAMemoryPointer.class);

        return result;
    }
    
    private JNAMemoryPointer(Ruby runtime, IRubyObject klass, MemoryIO io, long size) {
        super(runtime, (RubyClass) klass, io, size);
    }
    private static final IRubyObject allocate(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, int count, boolean clear, Block block) {
        int size = calculateSize(context, sizeArg);
        int total = size * count;
        if (total < 0) {
            throw context.getRuntime().newArgumentError(String.format("Negative size (%d objects of %d size)", count, size));
        }
        NativeMemoryIO io = AllocatedNativeMemoryIO.allocate(total > 0 ? total : 1, clear);
        if (io == null) {
            Ruby runtime = context.getRuntime();
            throw new RaiseException(runtime, runtime.getNoMemoryError(),
                    String.format("Failed to allocate %d objects of %d bytes", count, size), true);
        }
        JNAMemoryPointer ptr = new JNAMemoryPointer(context.getRuntime(), recv, io, total);
        ptr.fastSetInstanceVariable("@type_size", context.getRuntime().newFixnum(size));
        if (block.isGiven()) {
            return block.yield(context, ptr);
        } else {
            return ptr;
        }
    }
    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, Block block) {
        return allocate(context, recv, sizeArg, 1, true, block);
    }
    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject count, Block block) {
        return allocate(context, recv, sizeArg, RubyNumeric.fix2int(count), true, block);
    }
    @JRubyMethod(name = { "new" }, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject count, IRubyObject clear, Block block) {
        return allocate(context, recv, sizeArg, RubyNumeric.fix2int(count), clear.isTrue(), block);
    }

    @Override
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(ThreadContext context, IRubyObject[] args) {
        Pointer address = getAddress();
        String hex = address != null ? address.toString() : "native@0x0";
        return RubyString.newString(context.getRuntime(), MEMORY_POINTER_NAME + "[address=" + hex + "]");
    }

    @Override
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        String hex = Long.toHexString(ptr2long(getAddress()));
        return RubyString.newString(context.getRuntime(), 
                String.format("#<MemoryPointer address=0x%s size=%d>", hex, size));
    }

    @JRubyMethod(name = "free")
    public IRubyObject free(ThreadContext context) {
        ((AllocatedNativeMemoryIO) getMemoryIO()).free();
        return this;
    }

    @JRubyMethod(name = "autorelease=", required = 1)
    public IRubyObject autorelease(ThreadContext context, IRubyObject release) {
        ((AllocatedNativeMemoryIO) getMemoryIO()).setAutoRelease(release.isTrue());
        return this;
    }
}
