
package org.jruby.ext.ffi.jffi;

import java.util.Arrays;
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

import static org.jruby.api.Create.newEmptyString;
import static org.jruby.api.Error.typeError;

/**
 * Some utility functions for FFI &lt;=&gt; jffi conversions
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
        m.put(NativeType.LONGDOUBLE, com.kenai.jffi.Type.LONGDOUBLE);
        m.put(NativeType.POINTER, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_IN, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_OUT, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.BUFFER_INOUT, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.STRING, com.kenai.jffi.Type.POINTER);
        m.put(NativeType.TRANSIENT_STRING, com.kenai.jffi.Type.POINTER);

        return m;
    }

    static final com.kenai.jffi.Type getFFIType(Type type) {
        Object jffiType;

        if ((jffiType = type.getFFIHandle()) instanceof com.kenai.jffi.Type) {
            return (com.kenai.jffi.Type) jffiType;
        }

        return cacheFFIType(type);
    }

    private static com.kenai.jffi.Type cacheFFIType(Type type) {
        Object ffiType;
        synchronized (type) {

            if ((ffiType = type.getFFIHandle()) instanceof com.kenai.jffi.Type) {
                return (com.kenai.jffi.Type) ffiType;
            }

            if (type instanceof Type.Builtin || type instanceof CallbackInfo) {

                ffiType = FFIUtil.getFFIType(type.getNativeType());

            } else if (type instanceof org.jruby.ext.ffi.StructLayout) {

                ffiType = FFIUtil.newStruct((org.jruby.ext.ffi.StructLayout) type);

            } else if (type instanceof org.jruby.ext.ffi.StructByValue) {

                ffiType = FFIUtil.newStruct(((org.jruby.ext.ffi.StructByValue) type).getStructLayout());

            } else if (type instanceof org.jruby.ext.ffi.Type.Array) {

                ffiType = FFIUtil.newArray((org.jruby.ext.ffi.Type.Array) type);

            } else if (type instanceof org.jruby.ext.ffi.MappedType) {

                ffiType = FFIUtil.getFFIType(((org.jruby.ext.ffi.MappedType) type).getRealType());

            } else {
                return null;
            }

            type.setFFIHandle(ffiType);
        }

        return (com.kenai.jffi.Type) ffiType;
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
    static final com.kenai.jffi.Aggregate newStruct(org.jruby.ext.ffi.StructLayout layout) {

        if (layout.isUnion()) {

            //
            // The jffi union type is broken, so emulate a union with a Struct type, containing
            // an array of elements of the correct alignment.
            //
            com.kenai.jffi.Type[] alignmentTypes = {
                    com.kenai.jffi.Type.SINT8,
                    com.kenai.jffi.Type.SINT16,
                    com.kenai.jffi.Type.SINT32,
                    com.kenai.jffi.Type.SINT64,
                    com.kenai.jffi.Type.FLOAT,
                    com.kenai.jffi.Type.DOUBLE,
                    com.kenai.jffi.Type.LONGDOUBLE,
            };

            com.kenai.jffi.Type alignmentType = null;
            for (com.kenai.jffi.Type t : alignmentTypes) {
                if (t.alignment() == layout.getNativeAlignment()) {
                    alignmentType = t;
                    break;
                }
            }
            if (alignmentType == null) {
                throw layout.getRuntime().newRuntimeError("cannot discern base alignment type for union of alignment "
                        + layout.getNativeAlignment());
            }

            com.kenai.jffi.Type[] fields = new com.kenai.jffi.Type[layout.getNativeSize() / alignmentType.size()];
            Arrays.fill(fields, alignmentType);

            return com.kenai.jffi.Struct.newStruct(fields);

        } else {

            Collection<StructLayout.Member> structMembers = layout.getMembers();
            java.util.List<com.kenai.jffi.Type> fields = new java.util.ArrayList<com.kenai.jffi.Type>();

            for (StructLayout.Member m : structMembers) {
                com.kenai.jffi.Type fieldType;
                fieldType = FFIUtil.getFFIType(m.type());
                if (fieldType == null) throw typeError(layout.getRuntime().getCurrentContext(), "unsupported Struct field type " + m);
                if (fieldType.size() > 0) fields.add(fieldType);
            }

            return com.kenai.jffi.Struct.newStruct(fields.toArray(new com.kenai.jffi.Type[fields.size()]));
        }
    }

    /**
     * Creates a new JFFI type descriptor for an array
     *
     * @param arrayType The structure layout
     * @return A new Struct descriptor.
     */
    static com.kenai.jffi.Array newArray(org.jruby.ext.ffi.Type.Array arrayType) {
        com.kenai.jffi.Type componentType = FFIUtil.getFFIType(arrayType.getComponentType());

        if (componentType == null) {
            throw typeError(arrayType.getRuntime().getCurrentContext(), "unsupported array element type " + arrayType.getComponentType());
        }

        return com.kenai.jffi.Array.newArray(componentType, arrayType.length());
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
        var context = runtime.getCurrentContext();
        if (address == 0) return context.nil;

        byte[] bytes = IO.getZeroTerminatedByteArray(address);
        return bytes.length == 0 ? newEmptyString(context) : RubyString.newStringNoCopy(runtime, bytes);
    }
}
