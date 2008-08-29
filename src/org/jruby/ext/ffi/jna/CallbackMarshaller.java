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

import com.sun.jna.CallbackProxy;
import com.sun.jna.Pointer;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.ext.ffi.Callback;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Converts a ruby string or java <tt>ByteBuffer</tt> into a native pointer.
 */
final class CallbackMarshaller implements Marshaller {
    private static final Map<Object, com.sun.jna.Callback> callbackMap = 
            Collections.synchronizedMap(new WeakHashMap<Object, com.sun.jna.Callback>());
    private final Callback callback;
    private final Class[] paramTypes;
    private final Class returnType;

    public CallbackMarshaller(Callback cb) {
        this.callback = cb;
        NativeType[] nativeParams = cb.getParameterTypes();
        paramTypes = new Class[nativeParams.length];
        for (int i = 0; i < nativeParams.length; ++i) {
            paramTypes[i] = classFor(nativeParams[i]);
            if (paramTypes[i] == null) {
                throw cb.getRuntime().newArgumentError("Invalid callback parameter type: " + nativeParams[i]);
            }
        }
        switch (cb.getReturnType()) {
            case INT8:
            case UINT8:
            case INT16:
            case UINT16:
            case INT32:
            case UINT32:
            case LONG:
            case ULONG:
            case INT64:
            case UINT64:
            case FLOAT32:
            case FLOAT64:
            case POINTER:
                this.returnType = classFor(cb.getReturnType());
                break;
            default:
               throw cb.getRuntime().newArgumentError("Invalid callback return type: " + cb.getReturnType()); 
                
        }        
    }

    private static final Class classFor(NativeType type) {
        switch (type) {
            case INT8:
            case UINT8:
                return byte.class;
            case INT16:
            case UINT16:
                return short.class;
            case INT32:
            case UINT32:
                return int.class;
            case INT64:
            case UINT64:
                return long.class;
            case FLOAT32:
                return float.class;
            case FLOAT64:
                return double.class;
            case POINTER:
                return Pointer.class;
            default:
                return null;
        }
    }

    public final Object marshal(Invocation invocation, final IRubyObject parameter) {
        com.sun.jna.Callback cb = callbackMap.get(parameter);
        if (cb != null) {
            return cb;
        }
        cb = new ProcCallback(parameter.getRuntime(), parameter);
        callbackMap.put(parameter, cb);
        return cb;
    }
    public final Object marshal(Ruby runtime, final Block block) {
        com.sun.jna.Callback cb = callbackMap.get(block);
        if (cb != null) {
            return cb;
        }
        cb = new ProcCallback(runtime, block);
        callbackMap.put(block, cb);
        return cb;
    }
    private class ProcCallback implements CallbackProxy {
        final WeakReference<Object> proc;
        final Ruby runtime;

        ProcCallback(Ruby runtime, Object proc) {
            this.proc = new WeakReference<Object>(proc);
            this.runtime = runtime;
        }

        public Object callback(Object[] args) {
            Object recv = proc.get();
            if (recv == null) {
                return 0L;
            }
            NativeType[] nativeParams = callback.getParameterTypes();
            IRubyObject[] params = new IRubyObject[nativeParams.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = fromNative(runtime, nativeParams[i], args[i]);
            }
            IRubyObject retVal;
            if (recv instanceof RubyProc) {
                retVal = ((RubyProc) recv).call(runtime.getCurrentContext(), params);
            } else {
                retVal = ((Block) recv).call(runtime.getCurrentContext(), params);
            }
            return toNative(runtime, callback.getReturnType(), retVal);
        }

        public Class[] getParameterTypes() {
            return paramTypes;
        }

        public Class getReturnType() {
            return returnType;
        }
    }
    private static final Object toNative(Ruby runtime, NativeType type, IRubyObject value) {
        switch (type) {
            case INT8:
                return (byte) Util.int8Value(value);
            case UINT8:
                return (byte) Util.uint8Value(value);
            case INT16:
                return (short) Util.int16Value(value);
            case UINT16:
                return (short) Util.uint16Value(value);
            case INT32:
                return (int) Util.int32Value(value);
            case UINT32:
                return (int) Util.uint32Value(value);
            case INT64:
            case UINT64:
                return (long) Util.int64Value(value);
            case FLOAT32:
                return (float) Util.floatValue(value);
            case FLOAT64:
                return (double) Util.doubleValue(value);
            case POINTER:
                return ((JNAMemoryPointer) value).getNativeMemory();
            default:
                return Long.valueOf(0);
        }
    }
    private static final IRubyObject fromNative(Ruby runtime, NativeType type,
            Object value) {
        switch (type) {
            case INT8:
                return runtime.newFixnum((Byte) value);
            case UINT8:
                return Util.newUnsigned8(runtime, (Byte) value);
            case INT16:
                return runtime.newFixnum((Short) value);
            case UINT16:
                return Util.newUnsigned16(runtime, (Short) value);
            case INT32:
                return runtime.newFixnum((Integer) value);
            case UINT32:
                return Util.newUnsigned32(runtime, (Integer) value);
            case INT64:
                return runtime.newFixnum((Long) value);
            case UINT64:
                return Util.newUnsigned64(runtime, (Long) value);
            case FLOAT32:
                return runtime.newFloat((Float) value);
            case FLOAT64:
                return runtime.newFloat((Double) value);
            case POINTER:
                return new JNAMemoryPointer(runtime, (Pointer) value);
            default:
                throw new IllegalArgumentException("Invalid type " + type);
        }
    }
}
