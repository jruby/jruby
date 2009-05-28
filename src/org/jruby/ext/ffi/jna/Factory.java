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

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.AllocatedDirectMemoryIO;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.CallbackManager;
import org.jruby.ext.ffi.DirectMemoryIO;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * An implementation of FFI for JNA
 */
public class Factory extends org.jruby.ext.ffi.Factory {
    
    @Override
    public void init(Ruby runtime, RubyModule ffi) {
        super.init(runtime, ffi);
        //
        // Hook up the MemoryPointer class if its not already there
        //
        synchronized (ffi) {
            if (ffi.fastGetClass("DynamicLibrary") == null) {
                DynamicLibrary.createDynamicLibraryClass(runtime, ffi);
            }
            if (ffi.fastGetClass("Invoker") == null) {
                JNAInvoker.createInvokerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("VariadicInvoker") == null) {
                JNAVariadicInvoker.createVariadicInvokerClass(runtime, ffi);
            }
            ffi.defineModuleUnder("LastError").defineAnnotatedMethods(LastError.class);
        }
    }
    
    @Override
    public <T> T loadLibrary(String libraryName, Class<T> libraryClass) {
        try {
            return libraryClass.cast(Native.loadLibrary(libraryName, libraryClass));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param clear If the memory should be cleared.
     * @return A new <tt>MemoryIO</tt>.
     */
    public AllocatedDirectMemoryIO allocateDirectMemory(Ruby runtime, int size, boolean clear) {
        return AllocatedNativeMemoryIO.allocate(runtime, size, clear);
    }

    public DirectMemoryIO wrapDirectMemory(Ruby runtime, long address) {
        com.sun.jna.Pointer ptr;
        if (Platform.getPlatform().addressSize() == 32) {
            IntByReference ref = new IntByReference((int) address);
            ptr = ref.getPointer().getPointer(0);
        } else {
            LongByReference ref = new LongByReference(address);
            ptr = ref.getPointer().getPointer(0);
        }
        return NativeMemoryIO.wrap(runtime, ptr);
    }

    public CallbackManager getCallbackManager() {
        return new CallbackManager() {
            @Override
            public Pointer getCallback(Ruby runtime, CallbackInfo cbInfo, Object proc) {
                throw runtime.newNotImplementedError("Not implemented");
            }
        };
    }

    private static final boolean isSparc() {
        final Platform.CPU cpu = Platform.getPlatform().getCPU();
        return cpu == Platform.CPU.SPARC || cpu == Platform.CPU.SPARCV9;
    }

    static final int LONG_SIZE = Platform.getPlatform().longSize();
    static final int ADDRESS_SIZE = Platform.getPlatform().addressSize();
    static final int REGISTER_SIZE = Platform.getPlatform().addressSize();
    static final long LONG_MASK = LONG_SIZE == 32 ? 0x7FFFFFFFL : 0x7FFFFFFFFFFFFFFFL;
    static final int LONG_ALIGN = isSparc() ? 64 : LONG_SIZE;
    static final int ADDRESS_ALIGN = isSparc() ? 64 : REGISTER_SIZE;
    static final int DOUBLE_ALIGN = isSparc() ? 64 : REGISTER_SIZE;
    static final int FLOAT_ALIGN = isSparc() ? 64 : Float.SIZE;
    
    public int sizeOf(NativeType type) {
        switch (type) {
            case VOID: return 0;
            case INT8:
            case UINT8:
                return 1;
            case INT16:
            case UINT16:
                return 2;
            case INT32:
            case UINT32:
                return 4;
            case INT64:
            case UINT64:
                return 8;
            case LONG:
            case ULONG:
                return Platform.getPlatform().longSize() >> 3;
            case FLOAT32:
                return Float.SIZE >> 3;
            case FLOAT64:
                return Double.SIZE >> 3;
            case BUFFER_IN:
            case BUFFER_INOUT:
            case BUFFER_OUT:
            case POINTER:
            case STRING:
            case RBXSTRING:
                return Platform.getPlatform().addressSize() >> 3;
            default:
                throw new UnsupportedOperationException("Cannot determine native type for " + type);
        }
    }

    public int alignmentOf(NativeType type) {
        switch (type) {
            case VOID: return 0;
            case INT8:
            case UINT8:
                return 1;
            case INT16:
            case UINT16:
                return 2;
            case INT32:
            case UINT32:
                return 4;
            case INT64:
            case UINT64:
                return LONG_ALIGN / 8;
            case LONG:
            case ULONG:
                return LONG_ALIGN / 8;
            case FLOAT32:
                return FLOAT_ALIGN / 8;
            case FLOAT64:
                return DOUBLE_ALIGN / 8;
            case BUFFER_IN:
            case BUFFER_INOUT:
            case BUFFER_OUT:
            case POINTER:
            case STRING:
            case RBXSTRING:
                return ADDRESS_ALIGN / 8;
            default:
                throw new UnsupportedOperationException("Cannot determine native type for " + type);
        }
    }

    public static final class LastError {
        @JRubyMethod(name = {  "error" }, meta = true)
        public static final  IRubyObject error(ThreadContext context, IRubyObject recv) {
            return context.getRuntime().newFixnum(Native.getLastError());
        }
    }
}
