
package org.jruby.ext.ffi;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
@JRubyClass(name = "FFI::Type", parent = "Object")
public abstract class Type extends RubyObject {
    private static final java.util.Locale LOCALE = java.util.Locale.ENGLISH;
    protected final NativeType nativeType;

    /** Size of this type in bytes */
    protected final int size;

    /** Minimum alignment of this type in bytes */
    protected final int alignment;

    public static RubyClass createTypeClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass typeClass = ffiModule.defineClassUnder("Type", runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        typeClass.defineAnnotatedMethods(Type.class);
        typeClass.defineAnnotatedConstants(Type.class);

        RubyClass builtinClass = typeClass.defineClassUnder("Builtin", typeClass,
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        builtinClass.defineAnnotatedMethods(Builtin.class);
        
        RubyModule nativeType = ffiModule.defineModuleUnder("NativeType");

        
        defineBuiltinType(runtime, builtinClass, NativeType.CHAR, "char", "schar", "int8", "sint8");
        defineBuiltinType(runtime, builtinClass, NativeType.UCHAR, "uchar", "uint8");
        defineBuiltinType(runtime, builtinClass, NativeType.SHORT, "short", "sshort", "int16", "sint16");
        defineBuiltinType(runtime, builtinClass, NativeType.USHORT, "ushort", "uint16");
        defineBuiltinType(runtime, builtinClass, NativeType.INT, "int", "sint", "int32", "sint32");
        defineBuiltinType(runtime, builtinClass, NativeType.UINT, "uint", "uint32");
        defineBuiltinType(runtime, builtinClass, NativeType.LONG_LONG, "long_long", "slong_long", "int64", "sint64");
        defineBuiltinType(runtime, builtinClass, NativeType.ULONG_LONG, "ulong_long", "uint64");
        defineBuiltinType(runtime, builtinClass, NativeType.LONG, "long", "slong");
        defineBuiltinType(runtime, builtinClass, NativeType.ULONG, "ulong");
        defineBuiltinType(runtime, builtinClass, NativeType.FLOAT, "float", "float32");
        defineBuiltinType(runtime, builtinClass, NativeType.DOUBLE, "double", "float64");
        
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

        RubyClass arrayTypeClass = typeClass.defineClassUnder("Array", typeClass,
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        arrayTypeClass.defineAnnotatedMethods(Type.Array.class);

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

    public static final RubyClass getTypeClass(Ruby runtime) {
        return runtime.getFFI().typeClass;
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
    public final NativeType getNativeType() {
        return nativeType;
    }

    /**
     * Gets the native size of this <tt>Type</tt> in bytes
     *
     * @return The native size of this Type.
     */
    public final int getNativeSize() {
        return size;
    }

    /**
     * Gets the native alignment of this <tt>Type</tt> in bytes
     *
     * @return The native alignment of this Type.
     */
    public final int getNativeAlignment() {
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
        return context.runtime.newFixnum(getNativeSize());
    }

    /**
     * Gets the native alignment of this <tt>Type</tt> in bytes
     *
     * @param context The Ruby thread context.
     * @return The native alignment of this Type.
     */
    @JRubyMethod(name = "alignment")
    public IRubyObject alignment(ThreadContext context) {
        return context.runtime.newFixnum(getNativeAlignment());
    }

    @JRubyClass(name = "FFI::Type::Builtin", parent = "FFI::Type")
    public final static class Builtin extends Type {
        private final RubySymbol sym;

        /**
         * Initializes a new <tt>BuiltinType</tt> instance.
         */
        private Builtin(Ruby runtime, RubyClass klass, NativeType nativeType, String symName) {
            super(runtime, klass, nativeType, Type.getNativeSize(nativeType), Type.getNativeAlignment(nativeType));
            this.sym = runtime.newSymbol(symName);
        }

        @JRubyMethod(name = "inspect")
        public final IRubyObject inspect(ThreadContext context) {
            return RubyString.newString(context.runtime,
                    String.format("#<FFI::Type::Builtin:%s size=%d alignment=%d>",
                    nativeType.name(), size, alignment));
        }
        
        @Override
        public final String toString() {
            return nativeType.name();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Builtin) && ((Builtin) obj).nativeType.equals(nativeType);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + nativeType.hashCode();
            return hash;
        }

        @JRubyMethod
        public final IRubyObject to_sym(ThreadContext context) {
            return sym;
        }

        @Override
        @JRubyMethod(name = "==", required = 1)
        public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
            return RubyBoolean.newBoolean(context, this.equals(obj));
        }

        @Override
        @JRubyMethod(name = "equal?", required = 1)
        public IRubyObject equal_p(ThreadContext context, IRubyObject obj) {
            return RubyBoolean.newBoolean(context, this.equals(obj));
        }
        
        @JRubyMethod(name = "eql?", required = 1)
        public IRubyObject eql_p(ThreadContext context, IRubyObject obj) {
            return RubyBoolean.newBoolean(context, this.equals(obj));
        }

    }

    @JRubyClass(name = "FFI::Type::Array", parent = "FFI::Type")
    public final static class Array extends Type {
        private final Type componentType;
        private final int length;

        /**
         * Initializes a new <tt>Type.Array</tt> instance.
         */
        public Array(Ruby runtime, RubyClass klass, Type componentType, int length) {
            super(runtime, klass, NativeType.ARRAY, componentType.getNativeSize() * length, componentType.getNativeAlignment());
            this.componentType = componentType;
            this.length = length;
        }

        /**
         * Initializes a new <tt>Type.Array</tt> instance.
         */
        public Array(Ruby runtime, Type componentType, int length) {
            this(runtime, getTypeClass(runtime).getClass("Array"), componentType, length);
        }


        public final Type getComponentType() {
            return componentType;
        }

        public final int length() {
            return length;
        }

        @JRubyMethod(name = "new", required = 2, meta = true)
        public static final IRubyObject newInstance(ThreadContext context, IRubyObject klass, IRubyObject componentType, IRubyObject length) {
            if (!(componentType instanceof Type)) {
                throw context.runtime.newTypeError(componentType, getTypeClass(context.runtime));
            }

            return new Array(context.runtime, (RubyClass) klass, (Type) componentType, RubyNumeric.fix2int(length));
        }

        @JRubyMethod
        public final IRubyObject length(ThreadContext context) {
            return context.runtime.newFixnum(length);
        }

        @JRubyMethod
        public final IRubyObject elem_type(ThreadContext context) {
            return componentType;
        }

    }

    private static final boolean isPrimitive(NativeType type) {
        switch (type) {
            case VOID:
            case BOOL:
            case CHAR:
            case UCHAR:
            case SHORT:
            case USHORT:
            case INT:
            case UINT:
            case LONG_LONG:
            case ULONG_LONG:
            case LONG:
            case ULONG:
            case FLOAT:
            case DOUBLE:
            case BUFFER_IN:
            case BUFFER_INOUT:
            case BUFFER_OUT:
            case POINTER:
            case STRING:
            case TRANSIENT_STRING:
                return true;
            default:
                return false;
        }

    }
    static final int getNativeAlignment(NativeType type) {
        return isPrimitive(type) ? Factory.getInstance().alignmentOf(type) : 1;
    }
    static final int getNativeSize(NativeType type) {
        return isPrimitive(type) ? Factory.getInstance().sizeOf(type) : 0;
    }

}
