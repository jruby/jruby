
package org.jruby.ext.ffi.jffi;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.ext.ffi.CallbackInfo;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.StructLayout;
import org.jruby.ext.ffi.Type;

/**
 * Some utility functions for FFI <=> jffi conversions
 */
public final class FFIUtil {
    private FFIUtil() {}
    private static final Map<NativeType, com.kenai.jffi.Type> typeMap = buildTypeMap();

    private static final Map<NativeType, com.kenai.jffi.Type> buildTypeMap() {
        Map<NativeType, com.kenai.jffi.Type> m = new EnumMap<NativeType, com.kenai.jffi.Type>(NativeType.class);
        m.put(NativeType.VOID, com.kenai.jffi.Type.VOID);
        m.put(NativeType.INT8, com.kenai.jffi.Type.SINT8);
        m.put(NativeType.INT16, com.kenai.jffi.Type.SINT16);
        m.put(NativeType.INT32, com.kenai.jffi.Type.SINT32);
        m.put(NativeType.INT64, com.kenai.jffi.Type.SINT64);

        m.put(NativeType.UINT8, com.kenai.jffi.Type.UINT8);
        m.put(NativeType.UINT16, com.kenai.jffi.Type.UINT16);
        m.put(NativeType.UINT32, com.kenai.jffi.Type.UINT32);
        m.put(NativeType.UINT64, com.kenai.jffi.Type.UINT64);

        if (com.kenai.jffi.Platform.getPlatform().longSize() == 32) {
            m.put(NativeType.LONG, com.kenai.jffi.Type.SINT32);
            m.put(NativeType.ULONG, com.kenai.jffi.Type.UINT32);
        } else {
            m.put(NativeType.LONG, com.kenai.jffi.Type.SINT64);
            m.put(NativeType.ULONG, com.kenai.jffi.Type.UINT64);
        }
        m.put(NativeType.FLOAT32, com.kenai.jffi.Type.FLOAT);
        m.put(NativeType.FLOAT64, com.kenai.jffi.Type.DOUBLE);
        m.put(NativeType.POINTER, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_IN, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_OUT, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_INOUT, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.STRING, com.kenai.jffi.Type.POINTER);

        return m;
    }

    static final com.kenai.jffi.Type getFFIType(Type type) {

        if (type instanceof Type.Builtin || type instanceof CallbackInfo || type instanceof org.jruby.ext.ffi.Enum) {

            return FFIUtil.getFFIType(type.getNativeType());

        } else if (type instanceof org.jruby.ext.ffi.StructByValue) {

            return FFIUtil.newStruct(((org.jruby.ext.ffi.StructByValue) type).getStructLayout());

        } else {
            return null;
        }
    }

    static final com.kenai.jffi.Type getFFIType(NativeType type) {
        return typeMap.get(type);
    }

    /**
     * Creates a new JFFI Struct descriptor from a list of struct members
     *
     * @param the runtime
     * @param structMembers the members of the struct
     * @return A new Struct descriptor.
     */
    static final com.kenai.jffi.Struct newStruct(Ruby runtime, Collection<StructLayout.Member> structMembers) {
        com.kenai.jffi.Type[] fields = new com.kenai.jffi.Type[structMembers.size()];

        int i = 0;
        for (StructLayout.Member m : structMembers) {
            com.kenai.jffi.Type fieldType;
            if (m instanceof StructLayout.Aggregate) {
                fieldType = newStruct(runtime, ((StructLayout.Aggregate) m).getFields());
            } else {
                fieldType = FFIUtil.getFFIType(m.getNativeType());
            }
            if (fieldType == null) {
                throw runtime.newTypeError("Unsupported Struct field type " + m);
            }
            fields[i++] = fieldType;
        }
        return new com.kenai.jffi.Struct(fields);
    }

    /**
     * Creates a new JFFI Struct descriptor for a StructLayout
     *
     * @param layout The structure layout
     * @return A new Struct descriptor.
     */
    static final com.kenai.jffi.Struct newStruct(StructLayout layout) {
        return newStruct(layout.getRuntime(), layout.getFields());
    }
}
