
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
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

        for (NativeType t : NativeType.values()) {
            try {
                Type b = new Builtin(runtime, builtinClass, t);
                typeClass.fastSetConstant(t.name(), b);
                nativeType.fastSetConstant(t.name(), b);
                ffiModule.fastSetConstant("TYPE_" + t.name(), b);
            } catch (UnsupportedOperationException ex) { }
        }

        return typeClass;
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
     * Gets the native alignment this <tt>Type</tt> in bytes
     *
     * @return The native alignment of this Type.
     */
    public final int getNativeAlignment() {
        return alignment;
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

    @JRubyClass(name = "FFI::Type::Builtin", parent = "FFI::Type")
    public final static class Builtin extends Type {
        
        /**
         * Initializes a new <tt>BuiltinType</tt> instance.
         */
        private Builtin(Ruby runtime, RubyClass klass, NativeType nativeType) {
            super(runtime, klass, nativeType, Type.getNativeSize(nativeType), Type.getNativeAlignment(nativeType));
        }

        @JRubyMethod(name = "to_s")
        public final IRubyObject to_s(ThreadContext context) {
            return RubyString.newString(context.getRuntime(),
                    String.format("#<FFI::Type::Builtin:%s>", nativeType.name()));
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
    }
    private static final boolean isSparc() {
        final Platform.CPU cpu = Platform.getPlatform().getCPU();
        return cpu == Platform.CPU.SPARC || cpu == Platform.CPU.SPARCV9;
    }

    static final int LONG_SIZE = Platform.getPlatform().longSize();
    static final int ADDRESS_SIZE = Platform.getPlatform().addressSize();
    static final int REGISTER_SIZE = Platform.getPlatform().addressSize();
    static final long LONG_MASK = LONG_SIZE == 32 ? 0x7FFFFFFFL : 0x7FFFFFFFFFFFFFFFL;
    static final int LONG_ALIGN = isSparc() ? 64 : LONG_SIZE;
    static final int ADDRESS_ALIGN = isSparc() ? 64 : REGISTER_SIZE;
    static final int DOUBLE_ALIGN = isSparc() ? 64 : REGISTER_SIZE;
    static final int FLOAT_ALIGN = isSparc() ? 64 : Float.SIZE;

    private static final int getNativeAlignment(NativeType type) {
        switch (type) {
            case VOID: return 0;
            case INT8:
            case UINT8:
                return 1;
            case INT16:
            case UINT16:
                return 2;
            case INT32:
            case UINT32:
                return 4;
            case INT64:
            case UINT64:
                return LONG_ALIGN / 8;
            case LONG:
            case ULONG:
                return LONG_ALIGN / 8;
            case FLOAT32:
                return FLOAT_ALIGN / 8;
            case FLOAT64:
                return DOUBLE_ALIGN / 8;
            case BUFFER_IN:
            case BUFFER_INOUT:
            case BUFFER_OUT:
            case POINTER:
            case STRING:
            case RBXSTRING:
                return ADDRESS_ALIGN / 8;
            default:
                return 0;
        }
    }
    private static final int getNativeSize(NativeType type) {
        switch (type) {
            case VOID: return 0;
            case INT8:
            case UINT8:
                return 1;
            case INT16:
            case UINT16:
                return 2;
            case INT32:
            case UINT32:
                return 4;
            case INT64:
            case UINT64:
                return 8;
            case LONG:
            case ULONG:
                return Platform.getPlatform().longSize() >> 3;
            case FLOAT32:
                return Float.SIZE >> 3;
            case FLOAT64:
                return Double.SIZE >> 3;
            case BUFFER_IN:
            case BUFFER_INOUT:
            case BUFFER_OUT:
            case POINTER:
            case STRING:
            case RBXSTRING:
                return Platform.getPlatform().addressSize() >> 3;
            default:
                return 0;
        }
    }

}
