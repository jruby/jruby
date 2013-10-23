
package org.jruby.ext.cffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

/**
 *
 */
@JRubyClass(name = "JRuby::CFFI::Type", parent = "Object")
public abstract class Type extends RubyObject {
    private static final java.util.Locale LOCALE = java.util.Locale.ENGLISH;
    private final NativeType nativeType;

    /** Size of this type in bytes */
    private final int size;

    /** Minimum alignment of this type in bytes */
    private final int alignment;

    public static RubyClass createTypeClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass typeClass = ffiModule.defineClassUnder("Type", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        typeClass.defineAnnotatedMethods(Type.class);
        typeClass.defineAnnotatedConstants(Type.class);

        RubyClass builtinClass = typeClass.defineClassUnder("Builtin", typeClass,
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        builtinClass.defineAnnotatedMethods(Builtin.class);
        
        RubyModule nativeType = ffiModule.defineModuleUnder("NativeType");

        
        defineBuiltinType(runtime, builtinClass, NativeType.SCHAR, "char", "schar", "int8", "sint8");
        defineBuiltinType(runtime, builtinClass, NativeType.UCHAR, "uchar", "uint8");
        defineBuiltinType(runtime, builtinClass, NativeType.SSHORT, "short", "sshort", "int16", "sint16");
        defineBuiltinType(runtime, builtinClass, NativeType.USHORT, "ushort", "uint16");
        defineBuiltinType(runtime, builtinClass, NativeType.SINT, "int", "sint", "int32", "sint32");
        defineBuiltinType(runtime, builtinClass, NativeType.UINT, "uint", "uint32");
        defineBuiltinType(runtime, builtinClass, NativeType.SLONG_LONG, "long_long", "slong_long", "int64", "sint64");
        defineBuiltinType(runtime, builtinClass, NativeType.ULONG_LONG, "ulong_long", "uint64");
        defineBuiltinType(runtime, builtinClass, NativeType.SLONG, "long", "slong");
        defineBuiltinType(runtime, builtinClass, NativeType.ULONG, "ulong");
        defineBuiltinType(runtime, builtinClass, NativeType.FLOAT, "float", "float32");
        defineBuiltinType(runtime, builtinClass, NativeType.DOUBLE, "double", "float64");
        defineBuiltinType(runtime, builtinClass, NativeType.POINTER, "address", "pointer");
        
        for (NativeType t : NativeType.values()) {
            if (!builtinClass.hasConstant(t.name())) {
                try {
                    Type b = new Builtin(runtime, builtinClass, t, t.name().toLowerCase(LOCALE));
                    builtinClass.defineConstant(t.name().toUpperCase(LOCALE), b);
                } catch (UnsupportedOperationException ex) {
                }

            }
        }

        //
        // Add aliases in Type::*, NativeType::* and FFI::TYPE_*
        //
        for (Map.Entry<String, RubyModule.ConstantEntry> c : builtinClass.getConstantMap().entrySet()) {
            if (c.getValue().value instanceof Type.Builtin) {
                typeClass.defineConstant(c.getKey(), c.getValue().value);
                nativeType.defineConstant(c.getKey(), c.getValue().value);
                ffiModule.defineConstant("TYPE_" + c.getKey(), c.getValue().value);
            }
        }

        return typeClass;
    }

    private static final void defineBuiltinType(Ruby runtime, RubyClass builtinClass, NativeType nativeType, String... names) {
        try {
            if (names.length > 0) {
                for (String n : names) {
                    builtinClass.setConstant(n.toUpperCase(LOCALE),
                            new Builtin(runtime, builtinClass, nativeType, n.toLowerCase(LOCALE)));
                }
            } else {
                builtinClass.setConstant(nativeType.name(),
                        new Builtin(runtime, builtinClass, nativeType, nativeType.name().toLowerCase(LOCALE)));
            }
        } catch (UnsupportedOperationException ex) {
        }
    }

    public static RubyClass getTypeClass(Ruby runtime) {
        return JRubyCFFILibrary.getModule(runtime).getClass("Type");
    }

    /**
     * Initializes a new <tt>Type</tt> instance.
     */
    protected Type(Ruby runtime, RubyClass klass, NativeType type, int size, int alignment) {
        super(runtime, klass);
        this.nativeType = type;
        this.size = size;
        this.alignment = alignment;
    }

    /**
     * Initializes a new <tt>Type</tt> instance.
     */
    protected Type(Ruby runtime, RubyClass klass, NativeType type) {
        super(runtime, klass);
        this.nativeType = type;
        this.size = getNativeSize(type);
        this.alignment = getNativeAlignment(type);
    }

    /**
     * Gets the native type of this <tt>Type</tt> when passed as a parameter
     *
     * @return The native type of this Type.
     */
    final NativeType nativeType() {
        return nativeType;
    }

    /**
     * Gets the native size of this <tt>Type</tt> in bytes
     *
     * @return The native size of this Type.
     */
    public final int size() {
        return size;
    }

    /**
     * Gets the native alignment of this <tt>Type</tt> in bytes
     *
     * @return The native alignment of this Type.
     */
    public final int alignment() {
        return alignment;
    }

    /**
     * Gets the native size of this <tt>Type</tt> in bytes
     *
     * @param context The Ruby thread context.
     * @return The native size of this Type.
     */
    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        return context.runtime.newFixnum(size());
    }

    /**
     * Gets the native alignment of this <tt>Type</tt> in bytes
     *
     * @param context The Ruby thread context.
     * @return The native alignment of this Type.
     */
    @JRubyMethod(name = "alignment")
    public IRubyObject alignment(ThreadContext context) {
        return context.runtime.newFixnum(alignment());
    }

    @JRubyClass(name = "CFFI::Type::Builtin", parent = "CFFI::Type")
    public final static class Builtin extends Type {
        private final RubySymbol sym;

        /**
         * Initializes a new <tt>BuiltinType</tt> instance.
         */
        private Builtin(Ruby runtime, RubyClass klass, NativeType nativeType, String symName) {
            super(runtime, klass, nativeType, Type.getNativeSize(nativeType), Type.getNativeAlignment(nativeType));
            this.sym = runtime.newSymbol(symName);
        }

        @JRubyMethod(name = "to_s")
        public final IRubyObject to_s(ThreadContext context) {
            return RubyString.newString(context.runtime,
                    String.format("#<CFFI::Type::Builtin:%s size=%d alignment=%d>",
                            nativeType().name(), size(), alignment()));
        }
        
        @Override
        public final String toString() {
            return nativeType().name();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Builtin) && ((Builtin) obj).nativeType().equals(nativeType());
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + nativeType().hashCode();
            return hash;
        }

        @JRubyMethod
        public final IRubyObject to_sym(ThreadContext context) {
            return sym;
        }

        @Override
        @JRubyMethod(name = "==", required = 1)
        public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
            return context.runtime.newBoolean(this.equals(obj));
        }

        @Override
        @JRubyMethod(name = "equal?", required = 1)
        public IRubyObject equal_p(ThreadContext context, IRubyObject obj) {
            return context.runtime.newBoolean(this.equals(obj));
        }
        
        @JRubyMethod(name = "eql?", required = 1)
        public IRubyObject eql_p(ThreadContext context, IRubyObject obj) {
            return context.runtime.newBoolean(this.equals(obj));
        }

    }

    @Override
    public Object toJava(Class target) {
        return this;
    }

    private static boolean isPrimitive(NativeType type) {
        switch (type) {
            case VOID:
            case SCHAR:
            case UCHAR:
            case SSHORT:
            case USHORT:
            case SINT:
            case UINT:
            case SLONG_LONG:
            case ULONG_LONG:
            case SLONG:
            case ULONG:
            case FLOAT:
            case DOUBLE:
            case POINTER:
                return true;
            default:
                return false;
        }

    }
    private static int getNativeAlignment(NativeType type) {
        return isPrimitive(type) ? jffiType(type).alignment() : 1;
    }
    private static int getNativeSize(NativeType type) {
        return isPrimitive(type) ? jffiType(type).size() : 0;
    }
    
    static com.kenai.jffi.Type jffiType(NativeType nativeType) {
        switch (nativeType) {
            case SCHAR:
                return com.kenai.jffi.Type.SCHAR;

            case UCHAR:
                return com.kenai.jffi.Type.UCHAR;

            case SSHORT:
                return com.kenai.jffi.Type.SSHORT;

            case USHORT:
                return com.kenai.jffi.Type.USHORT;

            case SINT:
                return com.kenai.jffi.Type.SINT;

            case UINT:
                return com.kenai.jffi.Type.UINT;

            case SLONG:
                return com.kenai.jffi.Type.SLONG;

            case ULONG:
                return com.kenai.jffi.Type.ULONG;

            case SLONG_LONG:
                return com.kenai.jffi.Type.SLONG_LONG;

            case ULONG_LONG:
                return com.kenai.jffi.Type.ULONG_LONG;

            case FLOAT:
                return com.kenai.jffi.Type.FLOAT;
            
            case DOUBLE:
                return com.kenai.jffi.Type.DOUBLE;

            case POINTER:
                return com.kenai.jffi.Type.POINTER;

            case VOID:
                return com.kenai.jffi.Type.VOID;

            default:
                throw new UnsupportedOperationException("Cannot resolve type " + nativeType);
        }
    }

}
