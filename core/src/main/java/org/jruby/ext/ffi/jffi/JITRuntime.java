package org.jruby.ext.ffi.jffi;

import org.jruby.*;
import org.jruby.ext.ffi.*;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;

import java.math.BigInteger;

/**
 *
 */
public final class JITRuntime {
    private JITRuntime() {}
    
    public static RuntimeException newArityError(ThreadContext context, int got, int expected) {
        return context.runtime.newArgumentError(got, expected);
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
            return (int) other2long(parameter);
        }
    }

    public static long f32Value64(IRubyObject parameter) {
        if (parameter instanceof RubyFloat) {
            return Float.floatToRawIntBits((float) ((RubyFloat) parameter).getDoubleValue());

        } else {
            return other2long(parameter);
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
        return RubyFixnum.newFixnum(context.runtime, (byte) value);
    }

    public static IRubyObject newSigned8(Ruby runtime, int value) {
        return RubyFixnum.newFixnum(runtime, (byte) value);
    }

    public static IRubyObject newSigned8(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.runtime, (byte) value);
    }

    public static IRubyObject newSigned8(Ruby runtime, long value) {
        return RubyFixnum.newFixnum(runtime, (byte) value);
    }

    public static IRubyObject newUnsigned8(ThreadContext context, int value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return RubyFixnum.newFixnum(context.runtime, n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static IRubyObject newUnsigned8(Ruby runtime, int value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return RubyFixnum.newFixnum(runtime, n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static IRubyObject newUnsigned8(ThreadContext context, long value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return RubyFixnum.newFixnum(context.runtime, n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static IRubyObject newUnsigned8(Ruby runtime, long value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return RubyFixnum.newFixnum(runtime, n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static IRubyObject newSigned16(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.runtime, (short) value);
    }

    public static IRubyObject newSigned16(Ruby runtime, int value) {
        return RubyFixnum.newFixnum(runtime, (short) value);
    }

    public static IRubyObject newSigned16(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.runtime, (short) value);
    }

    public static IRubyObject newSigned16(Ruby runtime, long value) {
        return RubyFixnum.newFixnum(runtime, (short) value);
    }

    public static IRubyObject newUnsigned16(ThreadContext context, int value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return RubyFixnum.newFixnum(context.runtime, n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static IRubyObject newUnsigned16(Ruby runtime, int value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return RubyFixnum.newFixnum(runtime, n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static IRubyObject newUnsigned16(ThreadContext context, long value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return RubyFixnum.newFixnum(context.runtime, n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static IRubyObject newUnsigned16(Ruby runtime, long value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return RubyFixnum.newFixnum(runtime, n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static IRubyObject newSigned32(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    public static IRubyObject newSigned32(Ruby runtime, int value) {
        return RubyFixnum.newFixnum(runtime, value);
    }

    public static IRubyObject newSigned32(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.runtime, (int) value);
    }

    public static IRubyObject newSigned32(Ruby runtime, long value) {
        return RubyFixnum.newFixnum(runtime, (int) value);
    }

    public static IRubyObject newUnsigned32(ThreadContext context, int value) {
        long n = value;
        return RubyFixnum.newFixnum(context.runtime, n < 0 ? ((n & 0x7FFFFFFFL) + 0x80000000L) : n);
    }

    public static IRubyObject newUnsigned32(Ruby runtime, int value) {
        long n = value;
        return RubyFixnum.newFixnum(runtime, n < 0 ? ((n & 0x7FFFFFFFL) + 0x80000000L) : n);
    }

    public static IRubyObject newUnsigned32(ThreadContext context, long value) {
        long n = (int) value; // only keep the low 32 bits
        return RubyFixnum.newFixnum(context.runtime, n < 0 ? ((n & 0x7FFFFFFFL) + 0x80000000L) : n);
    }

    public static IRubyObject newUnsigned32(Ruby runtime, long value) {
        long n = (int) value; // only keep the low 32 bits
        return RubyFixnum.newFixnum(runtime, n < 0 ? ((n & 0x7FFFFFFFL) + 0x80000000L) : n);
    }

    public static IRubyObject newSigned64(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    public static IRubyObject newSigned64(Ruby runtime, long value) {
        return RubyFixnum.newFixnum(runtime, value);
    }

    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    public static IRubyObject newUnsigned64(ThreadContext context, long value) {
        return value < 0
                ? RubyBignum.newBignum(context.runtime, BigInteger.valueOf(value & 0x7fffffffffffffffL).add(UINT64_BASE))
                : RubyFixnum.newFixnum(context.runtime, value);
    }

    public static IRubyObject newUnsigned64(Ruby runtime, long value) {
        return value < 0
                ? RubyBignum.newBignum(runtime, BigInteger.valueOf(value & 0x7fffffffffffffffL).add(UINT64_BASE))
                : RubyFixnum.newFixnum(runtime, value);
    }

    public static IRubyObject newNil(ThreadContext context, int ignored) {
        return context.nil;
    }

    public static IRubyObject newNil(Ruby runtime, int ignored) {
        return runtime.getNil();
    }
    
    public static IRubyObject newNil(ThreadContext context, long ignored) {
        return context.nil;
    }

    public static IRubyObject newNil(Ruby runtime, long ignored) {
        return runtime.getNil();
    }
    
    public static IRubyObject newPointer32(ThreadContext context, int address) {
        Ruby runtime = context.runtime;
        return new Pointer(runtime, 
                NativeMemoryIO.wrap(runtime, ((long) address) & 0xffffffffL));
    }

    public static IRubyObject newPointer32(Ruby runtime, int address) {
        return new Pointer(runtime,
                NativeMemoryIO.wrap(runtime, ((long) address) & 0xffffffffL));
    }
    
    public static IRubyObject newPointer32(ThreadContext context, long address) {
        Ruby runtime = context.runtime;
        return new Pointer(runtime, 
                NativeMemoryIO.wrap(runtime, address & 0xffffffffL));
    }

    public static IRubyObject newPointer32(Ruby runtime, long address) {
        return new Pointer(runtime,
                NativeMemoryIO.wrap(runtime, address & 0xffffffffL));
    }
    
    public static IRubyObject newPointer64(ThreadContext context, long address) {
        Ruby runtime = context.runtime;
        return new Pointer(runtime, NativeMemoryIO.wrap(runtime, address));
    }

    public static IRubyObject newPointer64(Ruby runtime, long address) {
        return new Pointer(runtime, NativeMemoryIO.wrap(runtime, address));
    }
    
    public static IRubyObject newString(ThreadContext context, int address) {
        return FFIUtil.getString(context.runtime, address);
    }

    public static IRubyObject newString(Ruby runtime, int address) {
        return FFIUtil.getString(runtime, address);
    }
    
    public static IRubyObject newString(ThreadContext context, long address) {
        return FFIUtil.getString(context.runtime, address);
    }

    public static IRubyObject newString(Ruby runtime, long address) {
        return FFIUtil.getString(runtime, address);
    }
    
    public static IRubyObject newBoolean(ThreadContext context, int value) {
        return RubyBoolean.newBoolean(context, (value & 0x1) != 0);
    }

    public static IRubyObject newBoolean(Ruby runtime, int value) {
        return runtime.newBoolean((value & 0x1) != 0);
    }
    
    public static IRubyObject newBoolean(ThreadContext context, long value) {
        return RubyBoolean.newBoolean(context, (value & 0x1L) != 0);
    }

    public static IRubyObject newBoolean(Ruby runtime, long value) {
        return runtime.newBoolean((value & 0x1L) != 0);
    }
    
    public static IRubyObject newFloat32(ThreadContext context, int value) {
        return RubyFloat.newFloat(context.runtime, Float.intBitsToFloat(value));
    }

    public static IRubyObject newFloat32(Ruby runtime, int value) {
        return RubyFloat.newFloat(runtime, Float.intBitsToFloat(value));
    }
    
    public static IRubyObject newFloat32(ThreadContext context, long value) {
        return RubyFloat.newFloat(context.runtime, Float.intBitsToFloat((int) value));
    }

    public static IRubyObject newFloat32(Ruby runtime, long value) {
        return RubyFloat.newFloat(runtime, Float.intBitsToFloat((int) value));
    }
    
    public static IRubyObject newFloat64(ThreadContext context, long value) {
        return RubyFloat.newFloat(context.runtime, Double.longBitsToDouble(value));
    }

    public static IRubyObject newFloat64(Ruby runtime, long value) {
        return RubyFloat.newFloat(runtime, Double.longBitsToDouble(value));
    }

    private static final PointerParameterStrategy DIRECT_MEMORY_OBJECT = new MemoryObjectParameterStrategy(true);
    private static final PointerParameterStrategy HEAP_MEMORY_OBJECT = new MemoryObjectParameterStrategy(false);
    private static final PointerParameterStrategy DIRECT_MEMORY_IO = new MemoryIOParameterStrategy(true);
    private static final PointerParameterStrategy HEAP_MEMORY_IO = new MemoryIOParameterStrategy(false);
    private static final PointerParameterStrategy NIL_POINTER_STRATEGY = new NilPointerParameterStrategy();
    private static final PointerParameterStrategy INTEGER_POINTER_STRATEGY = new IntegerPointerParameterStrategy();
    private static final PointerParameterStrategy HEAP_STRING_POINTER_STRATEGY = new StringParameterStrategy(false, false);
    private static final PointerParameterStrategy TRANSIENT_STRING_PARAMETER_STRATEGY = new StringParameterStrategy(false, true);
    private static final PointerParameterStrategy DIRECT_STRING_PARAMETER_STRATEGY = new StringParameterStrategy(true, true);

    public static PointerParameterStrategy lookupPointerParameterStrategy(IRubyObject parameter) {
        if (parameter instanceof MemoryObject) {
            return ((MemoryObject) parameter).getMemoryIO().isDirect() ? DIRECT_MEMORY_OBJECT : HEAP_MEMORY_OBJECT;

        } else if (parameter instanceof RubyNil) {
            return NIL_POINTER_STRATEGY;

        } else if (parameter instanceof RubyString) {
            return HEAP_STRING_POINTER_STRATEGY;

        } else if (parameter instanceof RubyInteger) {
            return INTEGER_POINTER_STRATEGY;

        }
        
        return null;
    }

    public static MemoryIO lookupPointerMemoryIO(IRubyObject parameter) {
        if (parameter instanceof MemoryObject) {
            return ((MemoryObject) parameter).getMemoryIO();

        } else if (parameter instanceof RubyNil) {
            return NilPointerParameterStrategy.NullMemoryIO.INSTANCE;

        } else if (parameter instanceof RubyString) {
            return StringParameterStrategy.getMemoryIO((RubyString) parameter, false, false);

        }

        return null;
    }

    public static MemoryIO getPointerMemoryIO(IRubyObject parameter) {
        MemoryIO memory = lookupPointerMemoryIO(parameter);
        if (memory != null) {
            return memory;
        } else {
            return convertToPointerMemoryIO(parameter);
        }
    }

    private static MemoryIO convertToPointerMemoryIO(IRubyObject parameter) {
        IRubyObject obj = parameter;
        ThreadContext context = parameter.getRuntime().getCurrentContext();
        for (int depth = 0; depth < 4; depth++) {
            if (obj.respondsTo("to_ptr")) {
                obj = obj.callMethod(context, "to_ptr");
                MemoryIO memory = lookupPointerMemoryIO(obj);
                if (memory != null) {
                    return memory;
                }
            
            } else {
                throw parameter.getRuntime().newArgumentError("cannot convert parameter to native pointer");
            }
        }

        throw parameter.getRuntime().newRuntimeError("to_ptr recursion limit reached for " + parameter.getMetaClass());
    }
    
    private static MemoryIO convertToStringMemoryIO(IRubyObject parameter, ThreadContext context, CachingCallSite callSite,
                                                    boolean isDirect, boolean checkStringSafety) {
        DynamicMethod conversionMethod;
        if (parameter instanceof RubyString) {
            return StringParameterStrategy.getMemoryIO((RubyString) parameter, isDirect, checkStringSafety);

        } else if (parameter instanceof RubyNil) {
            return NilPointerParameterStrategy.NullMemoryIO.INSTANCE;

        } else if (!(conversionMethod = callSite.retrieveCache(parameter).method).isUndefined()) {
            IRubyObject convertedParameter = conversionMethod.call(context, parameter, parameter.getMetaClass(), callSite.getMethodName(), Block.NULL_BLOCK);
            if (convertedParameter instanceof RubyString) {
                return StringParameterStrategy.getMemoryIO((RubyString) convertedParameter, isDirect, checkStringSafety);
            }
            // Fall through to the default conversion which will raise an error if the converted value is not of the correct type.
        }
        
        return StringParameterStrategy.getMemoryIO(parameter.convertToString(), isDirect, checkStringSafety);
    } 

    public static MemoryIO convertToStringMemoryIO(IRubyObject parameter, ThreadContext context, CachingCallSite callSite) {
        return convertToStringMemoryIO(parameter, context, callSite, true, true); 
    }

    public static MemoryIO convertToTransientStringMemoryIO(IRubyObject parameter, ThreadContext context, CachingCallSite callSite) {
        return convertToStringMemoryIO(parameter, context, callSite, false, true);
    }

    public static PointerParameterStrategy getMemoryIOStrategy(MemoryIO memory) {
        return memory.isDirect() ? DIRECT_MEMORY_IO : HEAP_MEMORY_IO;
    }

    public static PointerParameterStrategy pointerParameterStrategy(IRubyObject parameter) {
        PointerParameterStrategy strategy = lookupPointerParameterStrategy(parameter);
        if (strategy != null) {
            return strategy;
        
        } else if (parameter.respondsTo("to_ptr")) {
            IRubyObject ptr = parameter.callMethod(parameter.getRuntime().getCurrentContext(), "to_ptr");

            return new DelegatingPointerParameterStrategy(ptr, pointerParameterStrategy(ptr));

        } else {
            throw parameter.getRuntime().newArgumentError("cannot convert parameter to native pointer");
        }
    }

    public static PointerParameterStrategy stringParameterStrategy(IRubyObject parameter) {
        if (parameter instanceof RubyString) {
            return DIRECT_STRING_PARAMETER_STRATEGY;

        } else if (parameter.isNil()) {
            return NIL_POINTER_STRATEGY;

        } else {
            return stringParameterStrategy(parameter.convertToString());
        }
    }

    public static PointerParameterStrategy transientStringParameterStrategy(IRubyObject parameter) {
        if (parameter instanceof RubyString) {
            return TRANSIENT_STRING_PARAMETER_STRATEGY;

        } else if (parameter.isNil()) {
            return NIL_POINTER_STRATEGY;

        } else {
            return transientStringParameterStrategy(parameter.convertToString());
        }
    }

    public static MemoryIO convertToPointerMemoryIO(ThreadContext context, IRubyObject parameter, CachingCallSite callSite) {
        DynamicMethod method = getConversionMethod(parameter, callSite);
        IRubyObject ptr = method.call(context, parameter, parameter.getMetaClass(), callSite.getMethodName(), Block.NULL_BLOCK);
        if (ptr instanceof AbstractMemory) {
            return ((AbstractMemory) ptr).getMemoryIO();
        }

        throw parameter.getRuntime().newTypeError(parameter.getMetaClass() + "#" + callSite.getMethodName() 
                + " should return " + context.runtime.getFFI().pointerClass);
    }

    public static DynamicMethod getConversionMethod(IRubyObject parameter, CachingCallSite callSite) {
        DynamicMethod method = callSite.retrieveCache(parameter).method;
        if (method.isUndefined()) {
            throw parameter.getRuntime().newTypeError("cannot convert parameter of type " + parameter.getMetaClass()
                    + " to native pointer; does not respond to :" + callSite.getMethodName());
        }
        
        return method;
    }

    public static boolean isDirectPointer(IRubyObject parameter) {
        return parameter instanceof MemoryObject && ((MemoryObject) parameter).getMemoryIO().isDirect();
    }

    public static int pointerValue32(IRubyObject parameter) {
        return (int) ((MemoryObject) parameter).getMemoryIO().address();
    }

    public static long pointerValue64(IRubyObject parameter) {
        return ((MemoryObject) parameter).getMemoryIO().address();
    }

    public static boolean isTrue(boolean p1) {
        return p1;
    }

    public static boolean isTrue(boolean p1, boolean p2) {
        return p1 & p2;
    }

    public static boolean isTrue(boolean p1, boolean p2, boolean p3) {
        return p1 & p2 & p3;
    }

    public static boolean isTrue(boolean p1, boolean p2, boolean p3, boolean p4) {
        return p1 & p2 & p3 & p4;
    }

    public static boolean isTrue(boolean p1, boolean p2, boolean p3, boolean p4, boolean p5) {
        return p1 & p2 & p3 & p4 & p5;
    }

    public static boolean isTrue(boolean p1, boolean p2, boolean p3, boolean p4, boolean p5, boolean p6) {
        return p1 & p2 & p3 & p4 & p5 & p5 & p6;
    }
}
