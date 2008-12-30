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

package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.CallingConvention;
import com.kenai.jffi.Closure;
import com.kenai.jffi.ClosureManager;
import com.kenai.jffi.InvocationBuffer;
import com.kenai.jffi.Type;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.jruby.Ruby;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Converts a ruby string or java <tt>ByteBuffer</tt> into a native pointer.
 */
final class CallbackMarshaller implements ParameterMarshaller {
    private static final Map<Object, Closure.Handle> callbackMap =
            Collections.synchronizedMap(new WeakHashMap<Object, Closure.Handle>());
    private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    private final CallbackInfo cbInfo;
    private final CallingConvention convention;
    private final NativeType[] parameterTypes;
    private final Type[] ffiParameterTypes;
    private final Type ffiReturnType;
    public CallbackMarshaller(CallbackInfo cbInfo, CallingConvention convention) {
        this.cbInfo = cbInfo;
        this.convention = convention;
        NativeParam[] nativeParams = cbInfo.getParameterTypes();
        ffiParameterTypes = new Type[nativeParams.length];
        parameterTypes = new NativeType[nativeParams.length];
        for (int i = 0; i < nativeParams.length; ++i) {
            if (!(nativeParams[i] instanceof NativeType)) {
                throw new RuntimeException("Invalid callback parameter type: " + nativeParams[i]);
            }
            switch ((NativeType) nativeParams[i]) {
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
                case STRING:
                    ffiParameterTypes[i] = getFFIType((NativeType) nativeParams[i]);
                    parameterTypes[i] = (NativeType) nativeParams[i];
                    break;
                default:
                    throw new RuntimeException("Invalid callback parameter type: " + nativeParams[i]);
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
                this.ffiReturnType = getFFIType(cbInfo.getReturnType());
                break;
            default:
               throw cbInfo.getRuntime().newArgumentError("Invalid callback return type: " + cbInfo.getReturnType());
                
        }        
    }
    private static final Type getFFIType(NativeType type) {
        switch (type) {
            case VOID: return com.kenai.jffi.Type.VOID;
            case INT8: return com.kenai.jffi.Type.SINT8;
            case UINT8: return com.kenai.jffi.Type.UINT8;
            case INT16: return com.kenai.jffi.Type.SINT16;
            case UINT16: return com.kenai.jffi.Type.UINT16;
            case INT32: return com.kenai.jffi.Type.SINT32;
            case UINT32: return com.kenai.jffi.Type.UINT32;
            case INT64: return com.kenai.jffi.Type.SINT64;
            case UINT64: return com.kenai.jffi.Type.UINT64;
            case LONG:
                return com.kenai.jffi.Platform.getPlatform().addressSize() == 32
                        ? com.kenai.jffi.Type.SINT32
                        : com.kenai.jffi.Type.SINT64;
            case ULONG:
                return com.kenai.jffi.Platform.getPlatform().addressSize() == 32
                        ? com.kenai.jffi.Type.UINT32
                        : com.kenai.jffi.Type.UINT64;
            case FLOAT32: return com.kenai.jffi.Type.FLOAT;
            case FLOAT64: return com.kenai.jffi.Type.DOUBLE;
            case POINTER: return com.kenai.jffi.Type.POINTER;
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
                return com.kenai.jffi.Type.POINTER;
            case STRING: return com.kenai.jffi.Type.POINTER;
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }
    public void marshal(Invocation invocation, InvocationBuffer buffer, IRubyObject value) {
        marshal(invocation.getThreadContext(), buffer, value);
    }

    public void marshal(ThreadContext context, InvocationBuffer buffer, IRubyObject value) {
        marshalParam(context, buffer, value);
    }
    void marshal(ThreadContext context, InvocationBuffer buffer, Block value) {
        marshalParam(context, buffer, value);
    }
    void marshalParam(ThreadContext context, InvocationBuffer buffer, Object value) {
        Closure.Handle handle = callbackMap.get(value);
        if (handle == null) {
            Closure closure = new RubyCallback(context.getRuntime(), value);
            handle = ClosureManager.getInstance().newClosure(closure,
                    ffiReturnType, ffiParameterTypes, convention);
        }
        buffer.putAddress(handle.getAddress());
    }
    public boolean needsInvocationSession() {
        return false;
    }
    private class RubyCallback implements Closure {
        private final WeakReference<Object> proc;
        private final Ruby runtime;

        RubyCallback(Ruby runtime, Object proc) {
            this.proc = new WeakReference<Object>(proc);
            this.runtime = runtime;
        }

        public void invoke(Buffer buffer) {
            Object recv = proc.get();
            if (recv == null) {
                buffer.setInt32Return(0);
                return;
            }
            IRubyObject[] params = new IRubyObject[ffiParameterTypes.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = fromNative(runtime, (NativeType) cbInfo.getParameterTypes()[i], buffer, i);
            }
            IRubyObject retVal;
            if (recv instanceof RubyProc) {
                retVal = ((RubyProc) recv).call(runtime.getCurrentContext(), params);
            } else {
                retVal = ((Block) recv).call(runtime.getCurrentContext(), params);
            }
            setReturnValue(runtime, cbInfo.getReturnType(), buffer, retVal);
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
    private static final void setReturnValue(Ruby runtime, NativeType type,
            Closure.Buffer buffer, IRubyObject value) {
        switch ((NativeType) type) {
            case VOID:
                break;
            case INT8:
                buffer.setInt8Return((byte) longValue(value)); break;
            case UINT8:
                buffer.setInt8Return((byte) longValue(value)); break;
            case INT16:
                buffer.setInt16Return((short) longValue(value)); break;
            case UINT16:
                buffer.setInt16Return((short) longValue(value)); break;
            case INT32:
                buffer.setInt32Return((int) longValue(value)); break;
            case UINT32:
                buffer.setInt32Return((int) longValue(value)); break;
            case INT64:
            case UINT64:
                buffer.setInt64Return(longValue(value)); break;
            case FLOAT32:
                buffer.setFloatReturn((float) RubyNumeric.num2dbl(value)); break;
            case FLOAT64:
                buffer.setDoubleReturn(RubyNumeric.num2dbl(value)); break;
            case POINTER:
                buffer.setAddressReturn(longValue(value)); break;
            default:
        }
    }
    private static final IRubyObject fromNative(Ruby runtime, NativeType type,
            Closure.Buffer buffer, int index) {
        switch (type) {
            case VOID:
                return runtime.getNil();
            case INT8:
                return Util.newSigned8(runtime, buffer.getInt8(index));
            case UINT8:
                return Util.newUnsigned8(runtime, buffer.getInt8(index));
            case INT16:
                return Util.newSigned16(runtime, buffer.getInt16(index));
            case UINT16:
                return Util.newUnsigned16(runtime, buffer.getInt16(index));
            case INT32:
                return Util.newSigned32(runtime, buffer.getInt32(index));
            case UINT32:
                return Util.newUnsigned32(runtime, buffer.getInt32(index));
            case INT64:
                return Util.newSigned64(runtime, buffer.getInt64(index));
            case UINT64:
                return Util.newUnsigned64(runtime, buffer.getInt64(index));
            case FLOAT32:
                return runtime.newFloat(buffer.getFloat(index));
            case FLOAT64:
                return runtime.newFloat(buffer.getDouble(index));
            case POINTER:
                return new BasePointer(runtime, buffer.getAddress(index));
            case STRING:
                return getStringParameter(runtime, buffer, index);
            default:
                throw new IllegalArgumentException("Invalid type " + type);
        }
    }
    private static final IRubyObject getStringParameter(Ruby runtime, Closure.Buffer buffer, int index) {
        long address = buffer.getAddress(index);
        if (address == 0) {
            return runtime.getNil();
        }
        int len = (int) IO.getStringLength(address);
        if (len == 0) {
            return RubyString.newEmptyString(runtime);
        }
        byte[] bytes = new byte[len];
        IO.getByteArray(address, bytes, 0, len);

        RubyString s = RubyString.newStringShared(runtime, bytes);
        s.setTaint(true);
        return s;
    }
}
