package org.jruby.ext.ffi.jffi;

import java.math.BigInteger;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.ext.ffi.AbstractMemory;
import org.jruby.ext.ffi.Buffer;
import org.jruby.ext.ffi.Pointer;
import org.jruby.ext.ffi.Struct;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public final class JITRuntime {
    private JITRuntime() {}
    
    public static RuntimeException newArityError(ThreadContext context, int got, int expected) {
        return context.getRuntime().newArgumentError(got, expected);
    }
    
    public static long other2long(IRubyObject parameter) {
        if (parameter instanceof RubyNumeric) {
            return RubyNumeric.num2long(parameter);

        } else if (parameter.isNil()) {
            return 0L;

        } else if (parameter instanceof RubyString) {
            return chr2long(parameter);
        }

        throw parameter.getRuntime().newTypeError("value " 
                + parameter.getMetaClass().getRealClass()
                + " cannot be converted to integer");
    }
    
    public static int s8Value(IRubyObject parameter) {
        return (byte) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }
    
    public static int u8Value(IRubyObject parameter) {
        return (int) ((parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xff);
    }
    
    public static int s16Value(IRubyObject parameter) {
        return (short) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }
    
    public static int u16Value(IRubyObject parameter) {
        return (int) (((parameter instanceof RubyFixnum)
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffffL);
    }
    
    public static int s32Value(IRubyObject parameter) {
        return (int) (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter));
    }
    
    public static long u32Value(IRubyObject parameter) {
        return (parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffffffffL;
    }
    
    public static long s64Value(IRubyObject parameter) {
        return parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter);
    }
    
    public static long other2u64(IRubyObject parameter) {
        if (parameter instanceof RubyBignum) {
            return RubyBignum.big2ulong((RubyBignum) parameter);

        } else {
            return other2long(parameter);
        }
    }

    public static long u64Value(IRubyObject parameter) {
        return ((parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2u64(parameter)) & 0xffffffffffffffffL);
    }
    
    public static long chr2long(IRubyObject parameter) {
        CharSequence cs = parameter.asJavaString();
        if (cs.length() == 1) {
            return cs.charAt(0);
        }
        
        throw parameter.getRuntime().newRangeError("value "
                    + parameter + " is not an integer");
    }
    
    public static int float2int(IRubyObject parameter) {
        if (parameter instanceof RubyFloat) {
            return Float.floatToRawIntBits((float) ((RubyFloat) parameter).getDoubleValue());
        
        } else {
            return s32Value(parameter);
        }
    }
    
    public static long float2long(IRubyObject parameter) {
        if (parameter instanceof RubyFloat) {
            return Float.floatToRawIntBits((float) ((RubyFloat) parameter).getDoubleValue());
        
        } else {
            return other2long(parameter);
        }
    }
    
    public static long double2long(IRubyObject parameter) {
        if (parameter instanceof RubyFloat) {
            return Double.doubleToRawLongBits(((RubyFloat) parameter).getDoubleValue());
        
        } else {
            return other2long(parameter);
        }
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
    
    public static IRubyObject newSigned8(ThreadContext context, byte value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value);
    }

    public static IRubyObject newUnsigned8(ThreadContext context, byte value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value < 0 ? (long)((value & 0x7FL) + 0x80L) : value);
    }

    public static IRubyObject newSigned16(ThreadContext context, short value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value);
    }

    public static IRubyObject newUnsigned16(ThreadContext context, short value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value < 0 ? (long)((value & 0x7FFFL) + 0x8000L) : value);
    }

    public static IRubyObject newSigned32(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value);
    }

    public static IRubyObject newUnsigned32(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.getRuntime(), value < 0 ? (long)((value & 0x7FFFFFFFL) + 0x80000000L) : value);
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
}
