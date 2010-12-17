
package org.jruby.ext.ffi.jffi;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.StructLayout;
import org.jruby.ext.ffi.Type;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Some utility functions for FFI <=> jffi conversions
 */
public final class FFIUtil {
    private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();
    
    private FFIUtil() {}
    private static final Map<NativeType, com.kenai.jffi.Type> typeMap = buildTypeMap();

    private static final Map<NativeType, com.kenai.jffi.Type> buildTypeMap() {
        Map<NativeType, com.kenai.jffi.Type> m = new EnumMap<NativeType, com.kenai.jffi.Type>(NativeType.class);
        m.put(NativeType.VOID, com.kenai.jffi.Type.VOID);
        m.put(NativeType.BOOL, com.kenai.jffi.Type.UINT8);

        m.put(NativeType.CHAR, com.kenai.jffi.Type.SCHAR);
        m.put(NativeType.SHORT, com.kenai.jffi.Type.SSHORT);
        m.put(NativeType.INT, com.kenai.jffi.Type.SINT);
        m.put(NativeType.LONG, com.kenai.jffi.Type.SLONG);
        m.put(NativeType.LONG_LONG, com.kenai.jffi.Type.SLONG_LONG);

        m.put(NativeType.UCHAR, com.kenai.jffi.Type.UCHAR);
        m.put(NativeType.USHORT, com.kenai.jffi.Type.USHORT);
        m.put(NativeType.UINT, com.kenai.jffi.Type.UINT);
        m.put(NativeType.ULONG, com.kenai.jffi.Type.ULONG);
        m.put(NativeType.ULONG_LONG, com.kenai.jffi.Type.ULONG_LONG);

        m.put(NativeType.FLOAT, com.kenai.jffi.Type.FLOAT);
        m.put(NativeType.DOUBLE, com.kenai.jffi.Type.DOUBLE);
        m.put(NativeType.POINTER, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_IN, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_OUT, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_INOUT, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.STRING, com.kenai.jffi.Type.POINTER);

        return m;
    }

    static final com.kenai.jffi.Type getFFIType(Type type) {

        if (type instanceof Type.Builtin || type instanceof CallbackInfo) {

            return FFIUtil.getFFIType(type.getNativeType());
        
        } else if (type instanceof org.jruby.ext.ffi.StructLayout) {

            return FFIUtil.newStruct((org.jruby.ext.ffi.StructLayout) type);

        } else if (type instanceof org.jruby.ext.ffi.StructByValue) {

            return FFIUtil.newStruct(((org.jruby.ext.ffi.StructByValue) type).getStructLayout());

        } else if (type instanceof org.jruby.ext.ffi.Type.Array) {

            return FFIUtil.newArray((org.jruby.ext.ffi.Type.Array) type);

        } else if (type instanceof org.jruby.ext.ffi.MappedType) {

            return FFIUtil.getFFIType(((org.jruby.ext.ffi.MappedType) type).getRealType());

        } else {
            return null;
        }
    }

    static final com.kenai.jffi.Type getFFIType(NativeType type) {
        return typeMap.get(type);
    }
   
    /**
     * Creates a new JFFI Struct descriptor for a StructLayout
     *
     * @param layout The structure layout
     * @return A new Struct descriptor.
     */
    static final com.kenai.jffi.Struct newStruct(org.jruby.ext.ffi.StructLayout layout) {
        Collection<StructLayout.Member> structMembers = layout.getMembers();
        com.kenai.jffi.Type[] fields = new com.kenai.jffi.Type[structMembers.size()];

        int i = 0;
        for (StructLayout.Member m : structMembers) {
            com.kenai.jffi.Type fieldType;
            fieldType = FFIUtil.getFFIType(m.type());
            if (fieldType == null) {
                throw layout.getRuntime().newTypeError("unsupported Struct field type " + m);
            }
            fields[i++] = fieldType;
        }

        return new com.kenai.jffi.Struct(fields);
    }

    /**
     * Creates a new JFFI type descriptor for an array
     *
     * @param layout The structure layout
     * @return A new Struct descriptor.
     */
    static final com.kenai.jffi.Array newArray(org.jruby.ext.ffi.Type.Array arrayType) {
        com.kenai.jffi.Type componentType = FFIUtil.getFFIType(arrayType.getComponentType());

        if (componentType == null) {
            throw arrayType.getRuntime().newTypeError("unsupported array element type " + arrayType.getComponentType());
        }

        return new com.kenai.jffi.Array(componentType, arrayType.length());
    }

    /**
     * Reads a nul-terminated string from native memory and boxes it up in a ruby
     * string.
     *
     * @param runtime The ruby runtime for the resulting string.
     * @param address The memory address to read the string from.
     * @return A ruby string.
     */
    static final IRubyObject getString(Ruby runtime, long address) {
        if (address == 0) {
            return runtime.getNil();
        }
        byte[] bytes = getZeroTerminatedByteArray(address);
        if (bytes.length == 0) {
            return RubyString.newEmptyString(runtime);
        }

        RubyString s = RubyString.newStringNoCopy(runtime, bytes);
        s.setTaint(true);
        return s;
    }
    
    static final byte[] getZeroTerminatedByteArray(long address) {
        return IO.getZeroTerminatedByteArray(address);
    }

    static final byte[] getZeroTerminatedByteArray(long address, int maxlen) {
        return IO.getZeroTerminatedByteArray(address, maxlen);
    }
    static final void putZeroTerminatedByteArray(long address, byte[] bytes, int off, int len) {
        IO.putByteArray(address, bytes, off, len);
        IO.putByte(address + len, (byte) 0);
    }

    static final Type resolveType(ThreadContext context, IRubyObject obj) {
        if (obj instanceof Type) {
            return (Type) obj;
        }

        final RubyModule ffi = context.getRuntime().fastGetModule("FFI");
        final IRubyObject typeDefs = ffi.fastFetchConstant("TypeDefs");

        if (!(typeDefs instanceof RubyHash)) {
            throw context.getRuntime().newRuntimeError("invalid or corrupted FFI::TypeDefs");
        }

        IRubyObject type = ((RubyHash) typeDefs).fastARef(obj);
        if (type == null || type.isNil()) {
            type = ffi.callMethod(context, "find_type", obj);
        }

        if (!(type instanceof Type)) {
            throw context.getRuntime().newTypeError("Could not resolve type: " + obj);
        }

        return (Type) type;
    }
}
