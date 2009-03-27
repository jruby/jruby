
package org.jruby.ext.ffi.jffi;

import java.util.EnumMap;
import java.util.Map;
import org.jruby.ext.ffi.NativeType;

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
    static final com.kenai.jffi.Type getFFIType(NativeType type) {
        return typeMap.get(type);
    }
}
