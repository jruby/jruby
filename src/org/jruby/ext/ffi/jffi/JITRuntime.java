package org.jruby.ext.ffi.jffi;

import org.jruby.*;
import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.Buffer;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Struct;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.StringSupport;

import java.math.BigInteger;

/**
 *
 */
public final class JITRuntime {
    private JITRuntime() {}
    
    public static RuntimeException newArityError(ThreadContext context, int got, int expected) {
        return context.getRuntime().newArgumentError(got, expected);
    }
    
    public static long other2long(IRubyObject parameter) {
        return RubyNumeric.num2long(parameter);
    }
    
    public static int s8Value32(IRubyObject parameter) {
        return (byte) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }

    public static long s8Value64(IRubyObject parameter) {
        return (byte) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }

    public static int u8Value32(IRubyObject parameter) {
        return (int) ((parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffL);
    }

    public static long u8Value64(IRubyObject parameter) {
        return (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffL;
    }

    public static int s16Value32(IRubyObject parameter) {
        return (short) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }

    public static long s16Value64(IRubyObject parameter) {
        return (short) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }

    public static int u16Value32(IRubyObject parameter) {
        return (int) (((parameter instanceof RubyFixnum)
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter))  & 0xffffL);
    }

    public static long u16Value64(IRubyObject parameter) {
        return ((parameter instanceof RubyFixnum)
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffffL;
    }

    public static int s32Value32(IRubyObject parameter) {
        return (int) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }

    public static long s32Value64(IRubyObject parameter) {
        return (int) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }

    public static int u32Value32(IRubyObject parameter) {
        return (int) ((parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffffffffL);
    }

    public static long u32Value64(IRubyObject parameter) {
        return (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffffffffL;
    }

    public static long s64Value64(IRubyObject parameter) {
        return parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter);
    }

    public static long other2u64(IRubyObject parameter) {
        if (parameter instanceof RubyBignum) {
            return ((RubyBignum) parameter).getValue().longValue();

        } else {
            return other2long(parameter);
        }
    }

    public static long u64Value64(IRubyObject parameter) {
        return parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2u64(parameter);
    }

    public static int f32Value32(IRubyObject parameter) {
        if (parameter instanceof RubyFloat) {
            return Float.floatToRawIntBits((float) ((RubyFloat) parameter).getDoubleValue());

        } else {
            return s32Value32(parameter);
        }
    }

    public static long f32Value64(IRubyObject parameter) {
        if (parameter instanceof RubyFloat) {
            return Float.floatToRawIntBits((float) ((RubyFloat) parameter).getDoubleValue());

        } else {
            return s32Value32(parameter);
        }
    }
    
    public static long f64Value64(IRubyObject parameter) {
        if (parameter instanceof RubyFloat) {
            return Double.doubleToRawLongBits(((RubyFloat) parameter).getDoubleValue());
        
        } else {
            return other2long(parameter);
        }
    }

    public static int boolValue32(IRubyObject parameter) {
        return boolValue(parameter) ? 1 : 0;
    }

    public static long boolValue64(IRubyObject parameter) {
        return boolValue(parameter) ? 1L : 0L;
    }

    private static boolean other2bool(IRubyObject parameter) {
        if (parameter instanceof RubyNumeric) {
            return ((RubyNumeric) parameter).getLongValue() != 0;
        }
        
        throw parameter.getRuntime().newTypeError("cannot convert " 
                + parameter.getMetaClass().getRealClass() + " to bool");
    }

    public static boolean boolValue(IRubyObject parameter) {
        if (parameter instanceof RubyBoolean) {
            return parameter.isTrue();
        }
        
        return other2bool(parameter);
    }
    
    public static IRubyObject other2ptr(ThreadContext context, IRubyObject parameter) {
        if (parameter instanceof Struct) {
            return ((Struct) parameter).getMemory();
        
        } else if (parameter instanceof RubyString || parameter instanceof Buffer || parameter.isNil()) {
            // Cannot be converted to a Pointer instance, let the asm code check it
            return parameter;
        }
        
        return convert2ptr(context, parameter);
    }
    
    public static IRubyObject convert2ptr(ThreadContext context, IRubyObject parameter) {
        final int MAXRECURSE = 4;
        IRubyObject ptr = parameter;
        for (int i = 0; i < MAXRECURSE && !(ptr instanceof AbstractMemory) && ptr.respondsTo("to_ptr"); i++) {
            ptr = ptr.callMethod(context, "to_ptr");
        }

        return ptr;
    }
    
    public static IRubyObject newSigned8(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.getRuntime(), (byte) value);
    }

    public static IRubyObject newSigned8(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.getRuntime(), (byte) value);
    }

    public static IRubyObject newUnsigned8(ThreadContext context, int value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return RubyFixnum.newFixnum(context.getRuntime(), n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static IRubyObject newUnsigned8(ThreadContext context, long value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return RubyFixnum.newFixnum(context.getRuntime(), n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static IRubyObject newSigned16(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.getRuntime(), (short) value);
    }

    public static IRubyObject newSigned16(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.getRuntime(), (short) value);
    }

    public static IRubyObject newUnsigned16(ThreadContext context, int value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return RubyFixnum.newFixnum(context.getRuntime(), n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static IRubyObject newUnsigned16(ThreadContext context, long value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return RubyFixnum.newFixnum(context.getRuntime(), n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static IRubyObject newSigned32(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value);
    }

    public static IRubyObject newSigned32(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.getRuntime(), (int) value);
    }

    public static IRubyObject newUnsigned32(ThreadContext context, int value) {
        long n = value;
        return RubyFixnum.newFixnum(context.getRuntime(), n < 0 ? ((n & 0x7FFFFFFFL) + 0x80000000L) : n);
    }

    public static IRubyObject newUnsigned32(ThreadContext context, long value) {
        long n = (int) value; // only keep the low 32 bits
        return RubyFixnum.newFixnum(context.getRuntime(), n < 0 ? ((n & 0x7FFFFFFFL) + 0x80000000L) : n);
    }

    public static IRubyObject newSigned64(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value);
    }

    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    public static IRubyObject newUnsigned64(ThreadContext context, long value) {
        return value < 0
                ? RubyBignum.newBignum(context.getRuntime(), BigInteger.valueOf(value & 0x7fffffffffffffffL).add(UINT64_BASE))
                : RubyFixnum.newFixnum(context.getRuntime(), value);
    }
    
    public static IRubyObject newNil(ThreadContext context, int ignored) {
        return context.getRuntime().getNil();
    }
    
    public static IRubyObject newNil(ThreadContext context, long ignored) {
        return context.getRuntime().getNil();
    }
    
    public static IRubyObject newPointer32(ThreadContext context, int address) {
        Ruby runtime = context.getRuntime();
        return new Pointer(runtime, 
                NativeMemoryIO.wrap(runtime, ((long) address) & 0xffffffffL));
    }
    
    public static IRubyObject newPointer32(ThreadContext context, long address) {
        Ruby runtime = context.getRuntime();
        return new Pointer(runtime, 
                NativeMemoryIO.wrap(runtime, address & 0xffffffffL));
    }
    
    public static IRubyObject newPointer64(ThreadContext context, long address) {
        Ruby runtime = context.getRuntime();
        return new Pointer(runtime, NativeMemoryIO.wrap(runtime, address));
    }
    
    public static IRubyObject newString(ThreadContext context, int address) {
        return FFIUtil.getString(context.getRuntime(), address);
    }
    
    public static IRubyObject newString(ThreadContext context, long address) {
        return FFIUtil.getString(context.getRuntime(), address);
    }
    
    public static IRubyObject newBoolean(ThreadContext context, int value) {
        return context.getRuntime().newBoolean((value & 0x1) != 0);
    }
    
    public static IRubyObject newBoolean(ThreadContext context, long value) {
        return context.getRuntime().newBoolean((value & 0x1L) != 0);
    }
    
    public static IRubyObject newFloat32(ThreadContext context, int value) {
        return RubyFloat.newFloat(context.getRuntime(), Float.intBitsToFloat(value));
    }
    
    public static IRubyObject newFloat32(ThreadContext context, long value) {
        return RubyFloat.newFloat(context.getRuntime(), Float.intBitsToFloat((int) value));
    }
    
    public static IRubyObject newFloat64(ThreadContext context, long value) {
        return RubyFloat.newFloat(context.getRuntime(), Double.longBitsToDouble(value));
    }

    private static final PointerParameterStrategy DIRECT_POINTER = new DirectPointerParameterStrategy();
    private static final PointerParameterStrategy DIRECT_STRUCT = new DirectStructParameterStrategy();
    private static final PointerParameterStrategy HEAP_STRUCT = new HeapStructParameterStrategy();
    private static final PointerParameterStrategy NIL_POINTER_STRATEGY = new NilPointerParameterStrategy();
    private static final PointerParameterStrategy HEAP_POINTER_STRATEGY = new HeapPointerParameterStrategy();
    private static final PointerParameterStrategy TRANSIENT_STRING_PARAMETER_STRATEGY = new TransientStringParameterStrategy();
    private static final PointerParameterStrategy DIRECT_STRING_POINTER_STRATEGY = new ConstStringPointerParameterStrategy();

    public static PointerParameterStrategy pointerParameterStrategy(IRubyObject parameter) {
        if (parameter instanceof Pointer) {
            return DIRECT_POINTER;

        } else if (parameter instanceof Buffer) {
            return ((AbstractMemory) parameter).getMemoryIO().isDirect() ? DIRECT_POINTER : HEAP_POINTER_STRATEGY;

        } else if (parameter instanceof Struct) {
            return ((Struct) parameter).getMemory().getMemoryIO().isDirect() ? DIRECT_STRUCT : HEAP_STRUCT;

        } else if (parameter.isNil()) {
            return NIL_POINTER_STRATEGY;

        } else if (parameter instanceof RubyString) {
            return TRANSIENT_STRING_PARAMETER_STRATEGY;

        } else if (parameter.respondsTo("to_ptr")) {
            IRubyObject ptr = parameter.callMethod(parameter.getRuntime().getCurrentContext(), "to_ptr");

            return new DelegatingPointerParameterStrategy(ptr, pointerParameterStrategy(ptr));

        } else {
            throw parameter.getRuntime().newTypeError("cannot convert parameter to native pointer");
        }
    }

    public static PointerParameterStrategy stringParameterStrategy(IRubyObject parameter) {
        if (parameter instanceof RubyString) {
            StringSupport.checkStringSafety(parameter.getRuntime(), parameter);
            return DIRECT_STRING_POINTER_STRATEGY;

        } else if (parameter.isNil()) {
            return NIL_POINTER_STRATEGY;

        } else {
            throw parameter.getRuntime().newTypeError("cannot convert parameter to native pointer");
        }
    }

    public static PointerParameterStrategy transientStringParameterStrategy(IRubyObject parameter) {
        if (parameter instanceof RubyString) {
            StringSupport.checkStringSafety(parameter.getRuntime(), parameter);
            return TRANSIENT_STRING_PARAMETER_STRATEGY;

        } else if (parameter.isNil()) {
            return NIL_POINTER_STRATEGY;

        } else {
            throw parameter.getRuntime().newTypeError("cannot convert parameter to native pointer");
        }
    }
}
