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
import com.sun.jna.Function;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.Ruby;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.ext.ffi.BasePointer;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.NullMemoryIO;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Converts a ruby string or java <tt>ByteBuffer</tt> into a native pointer.
 */
final class CallbackMarshaller implements Marshaller {
    private static final Map<Object, com.sun.jna.Callback> callbackMap = 
            Collections.synchronizedMap(new WeakHashMap<Object, com.sun.jna.Callback>());
    private final CallbackInfo cbInfo;
    private final Class[] paramTypes;
    private final Class returnType;
    private final int convention;
    
    public CallbackMarshaller(CallbackInfo cbInfo, int convention) {
        this.cbInfo = cbInfo;
        this.convention = convention;
        NativeParam[] nativeParams = cbInfo.getParameterTypes();
        paramTypes = new Class[nativeParams.length];
        for (int i = 0; i < nativeParams.length; ++i) {
            paramTypes[i] = classFor(nativeParams[i]);
            if (paramTypes[i] == null) {
                throw cbInfo.getRuntime().newArgumentError("Invalid callback parameter type: " + nativeParams[i]);
            }
        }
        switch (cbInfo.getReturnType()) {
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
            case VOID:
                this.returnType = classFor(cbInfo.getReturnType());
                break;
            default:
               throw cbInfo.getRuntime().newArgumentError("Invalid callback return type: " + cbInfo.getReturnType());
                
        }        
    }

    private static final Class classFor(NativeParam type) {
        switch ((NativeType) type) {
            case VOID:
                return void.class;
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
                return com.sun.jna.Pointer.class;
            default:
                return null;
        }
    }
    private final Object getCallback(Ruby runtime, Object obj) {
        com.sun.jna.Callback cb = callbackMap.get(obj);
        if (cb != null) {
            return cb;
        }
        cb = (convention == Function.ALT_CONVENTION)
                ? new StdcallCallback(runtime, obj)
                : new DefaultCallback(runtime, obj);
        callbackMap.put(obj, cb);
        return cb;
    }
    public final Object marshal(Invocation invocation, final IRubyObject parameter) {
        return parameter.isNil() ? null : getCallback(parameter.getRuntime(), parameter);
    }
    public final Object marshal(Ruby runtime, final Block block) {
        return getCallback(runtime, block);
    }
    private class RubyCallback implements CallbackProxy {
        private final WeakReference<Object> proc;
        private final Ruby runtime;

        RubyCallback(Ruby runtime, Object proc) {
            this.proc = new WeakReference<Object>(proc);
            this.runtime = runtime;
        }

        public Object callback(Object[] args) {
            Object recv = proc.get();
            if (recv == null) {
                return 0L;
            }
            NativeParam[] nativeParams = cbInfo.getParameterTypes();
            IRubyObject[] params = new IRubyObject[nativeParams.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = fromNative(runtime, nativeParams[i], args[i]);
            }
            IRubyObject retVal;
            try {
                if (recv instanceof RubyProc) {
                    retVal = ((RubyProc) recv).call(runtime.getCurrentContext(), params);
                } else {
                    retVal = ((Block) recv).call(runtime.getCurrentContext(), params);
                }
            } catch (Throwable t) {
                return Long.valueOf(0);
            }
            return toNative(runtime, cbInfo.getReturnType(), retVal);
        }

        public Class[] getParameterTypes() {
            return paramTypes;
        }

        public Class getReturnType() {
            return returnType;
        }
    }
    
    /**
     * The default callback type
     */
    private final class DefaultCallback extends RubyCallback {
        DefaultCallback(Ruby runtime, Object recv) {
            super(runtime, recv);
        }
    }

    /**
     * A callback for Win32 stdcall libraries
     */
    private final class StdcallCallback extends RubyCallback implements com.sun.jna.AltCallingConvention {
        StdcallCallback(Ruby runtime, Object recv) {
            super(runtime, recv);
        }
    }
    /**
     * Extracts the primitive value from a Ruby object.
     * This is similar to Util.longValue(), except it won't throw exceptions for
     * invalid values.
     *
     * @param value The Ruby object to convert
     * @return a java long value.
     */
    private static final long longValue(IRubyObject value) {
        if (value instanceof RubyNumeric) {
            return ((RubyNumeric) value).getLongValue();
        } else if (value.isNil()) {
            return 0L;
        }
        return 0;
    }
    private static final Object toNative(Ruby runtime, NativeParam type, IRubyObject value) {
        switch ((NativeType) type) {
            case VOID:
                return Long.valueOf(0);
            case INT8:
                return (byte) longValue(value);
            case UINT8:
                return (byte) longValue(value);
            case INT16:
                return (short) longValue(value);
            case UINT16:
                return (short) longValue(value);
            case INT32:
                return (int) longValue(value);
            case UINT32:
                return (int) longValue(value);
            case INT64:
            case UINT64:
                return (long) longValue(value);
            case FLOAT32:
                return (float) RubyNumeric.num2dbl(value);
            case FLOAT64:
                return (double) RubyNumeric.num2dbl(value);
            case POINTER:
                if (value instanceof Pointer) {
                    MemoryIO io = ((Pointer) value).getMemoryIO();
                    if (io instanceof NativeMemoryIO) {
                        return ((NativeMemoryIO) io).getPointer();
                    } else {
                        return null;
                    }
                } else if (value.isNil()) {
                    return null;
                } else {
                    throw runtime.newArgumentError("Invalid pointer value");
                }
            default:
                return Long.valueOf(0);
        }
    }
    private static final IRubyObject fromNative(Ruby runtime, NativeParam type,
            Object value) {
        switch ((NativeType) type) {
            case VOID:
                return runtime.getNil();
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
                return new BasePointer(runtime, 
                        value != null ? new NativeMemoryIO((com.sun.jna.Pointer) value) : new NullMemoryIO(runtime)) ;
            default:
                throw new IllegalArgumentException("Invalid type " + type);
        }
    }
}
