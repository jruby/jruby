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
import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import java.nio.ByteBuffer;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * A FFIProvider that uses JNA to load and execute native functions.
 */
public final class JNAProvider extends FFIProvider {

    JNAProvider(Ruby runtime) {
        super(runtime);
    }
    
    @Override
    public final AbstractInvoker createInvoker(Ruby runtime, String libraryName, String functionName,
            NativeType returnType, NativeParam[] parameterTypes, String convention) {
        int conv = "stdcall".equals(convention) ? Function.ALT_CONVENTION : Function.C_CONVENTION;
        if (libraryName == null) {
            libraryName = Platform.LIBC;
        }
        Function function = NativeLibrary.getInstance(libraryName).getFunction(functionName, conv);
        FunctionInvoker functionInvoker = getFunctionInvoker(returnType);
        Marshaller[] marshallers = new Marshaller[parameterTypes.length];
        for (int i = 0; i < marshallers.length; ++i) {
            marshallers[i] = getMarshaller(parameterTypes[i], conv);
        }

        return new JNAInvoker(runtime, FFIProvider.getModule(runtime).fastGetClass("Invoker"), function, functionInvoker, marshallers);
    }
    
    public int getLastError() {
        return Native.getLastError();
    }
    public void setLastError(int error) {
        Native.setLastError(error);
    }

    
    /**
     * Gets a {@link FunctionInvoker} for a native return type.
     * 
     * @param returnType The return type of the native function.
     * @return A new <tt>FunctionInvoker</tt> to invoke the native function.
     */
    static FunctionInvoker getFunctionInvoker(NativeType returnType) {
        switch (returnType) {
            case VOID:
                return VoidInvoker.INSTANCE;
            case POINTER:
                return PointerInvoker.INSTANCE;
            case INT8:
                return Signed8Invoker.INSTANCE;
            case INT16:
                return Signed16Invoker.INSTANCE;
            case INT32:
                return Signed32Invoker.INSTANCE;
            case UINT8:
                return Unsigned8Invoker.INSTANCE;
            case UINT16:
                return Unsigned16Invoker.INSTANCE;
            case UINT32:
                return Unsigned32Invoker.INSTANCE;
            case INT64:
                return Signed64Invoker.INSTANCE;
            case UINT64:
                return Unsigned64Invoker.INSTANCE;
            case LONG:
                return Platform.getPlatform().addressSize() == 32
                        ? Signed32Invoker.INSTANCE
                        : Signed64Invoker.INSTANCE;
            case ULONG:
                return Platform.getPlatform().addressSize() == 32
                        ? Unsigned32Invoker.INSTANCE
                        : Unsigned64Invoker.INSTANCE;
            case FLOAT32:
                return Float32Invoker.INSTANCE;
            case FLOAT64:
                return Float64Invoker.INSTANCE;
            case STRING:
            case RBXSTRING:
                return StringInvoker.INSTANCE;
            default:
                throw new IllegalArgumentException("Invalid return type: " + returnType);
        }
    }
    /**
     * Gets a marshaller to convert from a ruby type to a native type.
     * 
     * @param type The native type to convert to.
     * @return A new <tt>Marshaller</tt>
     */
    static final Marshaller getMarshaller(NativeParam type, int convention) {
        if (type instanceof NativeType) {
            return getMarshaller((NativeType) type);
        } else if (type instanceof org.jruby.ext.ffi.CallbackInfo) {
            return new CallbackMarshaller((org.jruby.ext.ffi.CallbackInfo) type, convention);
        } else {
            return null;
        }        
    }
    /**
     * Gets a marshaller to convert from a ruby type to a native type.
     * 
     * @param type The native type to convert to.
     * @return A new <tt>Marshaller</tt>
     */
    static final Marshaller getMarshaller(NativeType type) {
        switch (type) {
            case INT8:
                return Signed8Marshaller.INSTANCE;
            case UINT8:
                return Unsigned8Marshaller.INSTANCE;
            case INT16:
                return Signed16Marshaller.INSTANCE;
            case UINT16:
                return Unsigned16Marshaller.INSTANCE;
            case INT32:
                return Signed32Marshaller.INSTANCE;
            case UINT32:
                return Unsigned32Marshaller.INSTANCE;
            case INT64:
                return Signed64Marshaller.INSTANCE;
            case UINT64:
                return Unsigned64Marshaller.INSTANCE;
            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Marshaller.INSTANCE
                        : Signed64Marshaller.INSTANCE;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Marshaller.INSTANCE
                        : Signed64Marshaller.INSTANCE;
            case FLOAT32:
                return Float32Marshaller.INSTANCE;
            case FLOAT64:
                return Float64Marshaller.INSTANCE;
            case STRING:
                return StringMarshaller.INSTANCE;
            case RBXSTRING:
                return RbxStringMarshaller.INSTANCE;
            case POINTER:
                return PointerMarshaller.INSTANCE;
            case BUFFER_IN:
            case BUFFER_OUT:
            case BUFFER_INOUT:
                return BufferMarshaller.INSTANCE;
            default:
                throw new IllegalArgumentException("Invalid parameter type: " + type);
        }
    }
    
    /**
     * Invokes the native function with no return type, and returns nil to ruby.
     */
    private static final class VoidInvoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            function.invoke(args);
            return runtime.getNil();
        }
        public static final FunctionInvoker INSTANCE = new VoidInvoker();
    }
    /**
     * Invokes the native function with n signed 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed8Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return Util.newSigned8(runtime, function.invokeInt(args));
        }
        public static final FunctionInvoker INSTANCE = new Signed8Invoker();
    }

    /**
     * Invokes the native function with an unsigned 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned8Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return Util.newUnsigned8(runtime, function.invokeInt(args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned8Invoker();
    }

    /**
     * Invokes the native function with n signed 8 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed16Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return Util.newSigned16(runtime, function.invokeInt(args));
        }
        public static final FunctionInvoker INSTANCE = new Signed16Invoker();
    }

    /**
     * Invokes the native function with an unsigned 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned16Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return Util.newUnsigned16(runtime, function.invokeInt(args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned16Invoker();
    }
    /**
     * Invokes the native function with a 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed32Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {   
            return Util.newSigned32(runtime, function.invokeInt(args));
        }
        public static final FunctionInvoker INSTANCE = new Signed32Invoker();
    }
    
    /**
     * Invokes the native function with an unsigned 32 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Unsigned32Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return Util.newUnsigned32(runtime, function.invokeInt(args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned32Invoker();
    }
    
    /**
     * Invokes the native function with a 64 bit integer return value.
     * Returns a Fixnum to ruby.
     */
    private static final class Signed64Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return Util.newSigned64(runtime, function.invokeLong(args));
        }
        public static final FunctionInvoker INSTANCE = new Signed64Invoker();
    }

    /**
     * Invokes the native function with a 64 bit unsigned integer return value.
     * Returns a ruby Fixnum or Bignum.
     */
    private static final class Unsigned64Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return Util.newUnsigned64(runtime, function.invokeLong(args));
        }
        public static final FunctionInvoker INSTANCE = new Unsigned64Invoker();
    }
    
    /**
     * Invokes the native function with a 32 bit float return value.
     * Returns a Float to ruby.
     */
    private static final class Float32Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return runtime.newFloat(function.invokeFloat(args));
        }
        public static final FunctionInvoker INSTANCE = new Float32Invoker();
    }
    
    /**
     * Invokes the native function with a 64 bit float return value.
     * Returns a Float to ruby.
     */
    private static final class Float64Invoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            return runtime.newFloat(function.invokeDouble(args));
        }
        public static final FunctionInvoker INSTANCE = new Float64Invoker();
    }
    
    /**
     * Invokes the native function with a native pointer return value.
     * Returns a {@link MemoryPointer} to ruby.
     */
    private static final class PointerInvoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            com.sun.jna.Pointer ptr = function.invokePointer(args);
            return new BasePointer(runtime, ptr != null ? new NativeMemoryIO(ptr) : new NullMemoryIO(runtime));
        }
        public static final FunctionInvoker INSTANCE = new PointerInvoker();
    }
    
    /**
     * Invokes the native function with a native string return value.
     * Returns a {@link RubyString} to ruby.
     */
    private static final class StringInvoker implements FunctionInvoker {
        public final IRubyObject invoke(Ruby runtime, Function function, Object[] args) {
            com.sun.jna.Pointer address = function.invokePointer(args);
            if (address == null) {
                return runtime.getNil();
            }
            int len = (int) address.indexOf(0, (byte) 0);
            if (len == 0) {
                return RubyString.newEmptyString(runtime);
            }
            ByteList bl = new ByteList(len);
            bl.length(len);
            address.read(0, bl.unsafeBytes(), bl.begin(), len);
            
            RubyString s =  RubyString.newString(runtime, bl);
            s.setTaint(true);
            return s;
        }
        public static final FunctionInvoker INSTANCE = new StringInvoker();
    }
    
    /**
     * Converts a ruby Fixnum into an 8 bit native integer.
     */
    static final class Signed8Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Byte.valueOf(Util.int8Value(parameter));
        }
        public static final Marshaller INSTANCE = new Signed8Marshaller();
    }
    
    /**
     * Converts a ruby Fixnum into an 8 bit native unsigned integer.
     */
    static final class Unsigned8Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Byte.valueOf((byte) Util.uint8Value(parameter));
        }
        public static final Marshaller INSTANCE = new Unsigned8Marshaller();
    }
    
    /**
     * Converts a ruby Fixnum into a 16 bit native signed integer.
     */
    static final class Signed16Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Short.valueOf(Util.int16Value(parameter));
        }
        public static final Marshaller INSTANCE = new Signed16Marshaller();
    }
    
    /**
     * Converts a ruby Fixnum into a 16 bit native unsigned integer.
     */
    static final class Unsigned16Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Short.valueOf((short) Util.uint16Value(parameter));
        }
        public static final Marshaller INSTANCE = new Unsigned16Marshaller();
    }
    
    /**
     * Converts a ruby Fixnum into a 32 bit native signed integer.
     */
    static final class Signed32Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Integer.valueOf(Util.int32Value(parameter));
        }
        public static final Marshaller INSTANCE = new Signed32Marshaller();
    }
    
    /**
     * Converts a ruby Fixnum into a 32 bit native unsigned integer.
     */
    static final class Unsigned32Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Integer.valueOf((int) Util.uint32Value(parameter));
        }
        public static final Marshaller INSTANCE = new Unsigned32Marshaller();
    }
    
    /**
     * Converts a ruby Fixnum into a 64 bit native signed integer.
     */
    static final class Signed64Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Long.valueOf(Util.int64Value(parameter));
        }
        public static final Marshaller INSTANCE = new Signed64Marshaller();
    }
    
    /**
     * Converts a ruby Fixnum into a 64 bit native unsigned integer.
     */
    static final class Unsigned64Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Long.valueOf(Util.uint64Value(parameter));
        }
        public static final Marshaller INSTANCE = new Unsigned64Marshaller();
    }
    
    /**
     * Converts a ruby Float into a 32 bit native float.
     */
    static final class Float32Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Float.valueOf(Util.floatValue(parameter));
        }
        public static final Marshaller INSTANCE = new Float32Marshaller();
    }
    
    /**
     * Converts a ruby Float into a 64 bit native float.
     */
    static final class Float64Marshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            return Double.valueOf(Util.doubleValue(parameter));
        }
        public static final Marshaller INSTANCE = new Float64Marshaller();
    }
    private static final Object getNativeMemory(Pointer memory) {
        MemoryIO io = memory.getMemoryIO();
        return io instanceof NativeMemoryIO ? ((NativeMemoryIO) io).getPointer() : null;
    }
    private static final Object getNativeMemory(Ruby runtime, IRubyObject memory) {
        if (memory instanceof Pointer) {
            return getNativeMemory((Pointer) memory);
        } else if (memory instanceof Buffer) {
            ArrayMemoryIO io = (ArrayMemoryIO) ((Buffer) memory).getMemoryIO();
            return ByteBuffer.wrap(io.array(), io.arrayOffset(), io.arrayLength());
        } else if (memory == null || memory.isNil()) {
            return com.sun.jna.Pointer.NULL;
        } else {
            throw runtime.newArgumentError("Invalid memory object");
        }
    }
    /**
     * Converts a ruby MemoryPointer into a native pointer.
     */
    static final class PointerMarshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            if (parameter instanceof Pointer) {
                return getNativeMemory((Pointer) parameter);
            } else if (parameter instanceof Buffer) {
                ArrayMemoryIO io = (ArrayMemoryIO) ((Buffer) parameter).getMemoryIO();
                return ByteBuffer.wrap(io.array(), io.arrayOffset(), io.arrayLength());
            } else if (parameter instanceof Struct) {
                return getNativeMemory(invocation.getThreadContext().getRuntime(), ((Struct) parameter).getMemory());
            } else if (parameter.isNil()) {
                return com.sun.jna.Pointer.NULL;
            } else if (parameter instanceof RubyString) {
                // Handle a string being used as a inout buffer
                final ByteList bl = ((RubyString) parameter).getByteList();
                final int len = bl.length();
                final Memory memory = new Memory(len);
                memory.write(0, bl.unsafeBytes(), bl.begin(), len);

                //
                // Arrange for the bytes to be copied back after the function is called
                //
                invocation.addPostInvoke(new Runnable() {
                    public void run() {
                        memory.read(0, bl.unsafeBytes(), bl.begin(), len);
                    }
                });
                return memory;
            } else if (parameter.respondsTo("to_ptr")) {
                return getNativeMemory(invocation.getThreadContext().getRuntime(), 
                        parameter.callMethod(invocation.getThreadContext(), "to_ptr"));
            }
            throw invocation.getThreadContext().getRuntime().newArgumentError("Invalid Pointer argument");
        }
        public static final Marshaller INSTANCE = new PointerMarshaller();
    }
    
    /**
     * Converts a ruby string into a native string.
     * <p>
     * <b>Note:</b> This treats the string as immutable.  i.e. Native data is
     * <b>not</b> copied back to the ruby string after the call.
     * </p>
     */
    private static final class StringMarshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            if (parameter instanceof RubyString) {
                Util.checkStringSafety(invocation.getThreadContext().getRuntime(), parameter);
                // Ruby strings are UTF-8, so should be able to just copy directly
                RubyString s = parameter.asString();
                ByteList bl = s.getByteList();
                final Memory memory = new Memory(bl.length() + 1);
                memory.write(0, bl.unsafeBytes(), bl.begin(), bl.length());
                memory.setByte(bl.length(), (byte) 0);
                return memory;
            } else if (parameter.isNil()) {
                return null;
            } else {
                throw invocation.getThreadContext().getRuntime().newArgumentError("Invalid string parameter");
            }
        }
        public static final Marshaller INSTANCE = new StringMarshaller();
    }
    
    /**
     * Converts a ruby string into a native string.
     * <p>
     * <b>Note:</b> The string is mutable.  i.e. Changes to the native string
     * are copied back to the ruby string after the call.
     * </p>
     */
    private static final class RbxStringMarshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            // Ruby strings are UTF-8, so should be able to just copy directly
            final ByteList bl = parameter.asString().getByteList();
            final int strlen = bl.length();
            final Memory memory = new Memory(strlen + 1);
            memory.write(0, bl.unsafeBytes(), bl.begin(), strlen);
            memory.setByte(bl.length(), (byte) 0);
            
            //
            // Arrange for the bytes to be copied back after the function is called
            //
            invocation.addPostInvoke(new Runnable() {
                public void run() {
                    memory.read(0, bl.unsafeBytes(), bl.begin(), strlen);
                }
            });
            return memory;
        }
        public static final Marshaller INSTANCE = new RbxStringMarshaller();
    }
    
    /**
     * Converts a ruby string or java <tt>ByteBuffer</tt> into a native pointer.
     */
    private static final class BufferMarshaller implements Marshaller {
        public final Object marshal(Invocation invocation, IRubyObject parameter) {
            if (parameter instanceof Pointer) {
                return getNativeMemory((Pointer) parameter);
            } else if (parameter instanceof Buffer) {
                ArrayMemoryIO io = (ArrayMemoryIO) ((Buffer) parameter).getMemoryIO();
                return ByteBuffer.wrap(io.array(), io.arrayOffset(), io.arrayLength());
            } else if (parameter instanceof Struct) {
                return getNativeMemory(invocation.getThreadContext().getRuntime(), ((Struct) parameter).getMemory());
            } else if (parameter.isNil()) {
                return com.sun.jna.Pointer.NULL;
            } else if (parameter instanceof RubyString) {
                ByteList bl = ((RubyString) parameter).getByteList();
                return ByteBuffer.wrap(bl.unsafeBytes(), bl.begin(), bl.realSize);
            } else if (parameter.respondsTo("to_ptr")) {
                return getNativeMemory(invocation.getThreadContext().getRuntime(),
                        parameter.callMethod(invocation.getThreadContext(), "to_ptr"));
            }
            throw invocation.getThreadContext().getRuntime().newArgumentError("Invalid Buffer argument");
        }
        public static final Marshaller INSTANCE = new BufferMarshaller();
    }
}
