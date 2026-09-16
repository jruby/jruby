
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
import org.jruby.ext.ffi.Platform;
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

            return newUnion(layout);

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

    private static final boolean SYSV_X86_64 = Platform.getPlatform().getCPU() == Platform.CPU_TYPE.X86_64
            && Platform.getPlatform().getOS() != Platform.OS_TYPE.WINDOWS;

    private static final com.kenai.jffi.Type[] INTEGER_FILLERS = {
            com.kenai.jffi.Type.SINT8, com.kenai.jffi.Type.SINT16, com.kenai.jffi.Type.SINT32,
            com.kenai.jffi.Type.SINT64, com.kenai.jffi.Type.LONGDOUBLE,
    };

    private record Leaf(NativeType type, int offset, int size) {}

    /**
     * Creates a new JFFI Struct descriptor for a union. libffi has no union type, so the union is
     * described as a struct of filler cells that libffi classifies the way the C ABI classifies the union.
     *
     * @param layout The union layout
     * @return A new Struct descriptor.
     */
    static final com.kenai.jffi.Aggregate newUnion(org.jruby.ext.ffi.StructLayout layout) {
        final int size = layout.getNativeSize(), alignment = layout.getNativeAlignment();

        java.util.List<Leaf> leaves = new java.util.ArrayList<>();
        for (StructLayout.Member m : layout.getMembers()) {
            collectLeaves(m.type(), m.offset(), leaves);
        }

        NativeType homogeneous = leaves.isEmpty() ? null : leaves.get(0).type();
        for (Leaf leaf : leaves) {
            if (leaf.type() != homogeneous) homogeneous = null;
        }

        com.kenai.jffi.Type filler = null;
        if (homogeneous != null && isFloatingPoint(homogeneous)) {
            // Every member is made of one floating type: a homogeneous floating-point aggregate
            // on AArch64 (and PPC64 ELFv2), SSE class on SysV x86_64. Keep the real type.
            filler = getFFIType(homogeneous);

        } else if (SYSV_X86_64 && alignment >= 4 && alignment <= 8 && size <= 16) {
            // SysV x86_64 classifies each eightbyte separately: SSE only if every field overlapping it
            // is float or double, INTEGER otherwise. Decide per cell: libffi merges the cells into
            // eightbytes at the union's offset inside an enclosing struct, so the result holds there too.
            com.kenai.jffi.Type[] cells = new com.kenai.jffi.Type[size / alignment];
            for (int i = 0; i < cells.length; i++) {
                int cell = i * alignment;
                boolean sse = true;
                for (Leaf leaf : leaves) {
                    if (leaf.offset() < cell + alignment && leaf.offset() + leaf.size() > cell
                            && leaf.type() != NativeType.FLOAT && leaf.type() != NativeType.DOUBLE) {
                        sse = false;
                        break;
                    }
                }
                cells[i] = sse
                        ? (alignment == 8 ? com.kenai.jffi.Type.DOUBLE : com.kenai.jffi.Type.FLOAT)
                        : (alignment == 8 ? com.kenai.jffi.Type.SINT64 : com.kenai.jffi.Type.SINT32);
            }
            return com.kenai.jffi.Struct.newStruct(cells);
        }

        if (filler == null) {
            // Anything else travels in integer registers or memory: an integer of the union's alignment.
            for (com.kenai.jffi.Type t : INTEGER_FILLERS) {
                if (t.alignment() == alignment) {
                    filler = t;
                    break;
                }
            }
        }
        if (filler == null) {
            throw layout.getRuntime().newRuntimeError("cannot discern base alignment type for union of alignment "
                    + alignment);
        }

        com.kenai.jffi.Type[] fields = new com.kenai.jffi.Type[size / filler.size()];
        Arrays.fill(fields, filler);

        return com.kenai.jffi.Struct.newStruct(fields);
    }

    private static boolean isFloatingPoint(NativeType type) {
        return type == NativeType.FLOAT || type == NativeType.DOUBLE || type == NativeType.LONGDOUBLE;
    }

    /** Flattens arrays, nested structs/unions and mapped types into scalar leaves with their offsets. */
    private static void collectLeaves(Type type, int offset, java.util.List<Leaf> leaves) {
        if (type instanceof Type.Array array) {
            Type component = array.getComponentType();
            for (int i = 0; i < array.length(); i++) {
                collectLeaves(component, offset + i * component.getNativeSize(), leaves);
            }

        } else if (type instanceof org.jruby.ext.ffi.StructByValue sbv) {
            collectLeaves(sbv.getStructLayout(), offset, leaves);

        } else if (type instanceof StructLayout struct) {
            for (StructLayout.Member m : struct.getMembers()) {
                collectLeaves(m.type(), offset + m.offset(), leaves);
            }

        } else if (type instanceof org.jruby.ext.ffi.MappedType mapped) {
            collectLeaves(mapped.getRealType(), offset, leaves);

        } else {
            leaves.add(new Leaf(type.getNativeType(), offset, type.getNativeSize()));
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
