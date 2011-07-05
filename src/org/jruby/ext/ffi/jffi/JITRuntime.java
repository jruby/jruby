package org.jruby.ext.ffi.jffi;

import java.math.BigInteger;

import com.kenai.jffi.ObjectParameterInfo;
import com.kenai.jffi.ObjectParameterInvoker;
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
import org.jruby.util.StringSupport;

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
                : other2long(parameter)) & 0xff);
    }

    public static long u8Value64(IRubyObject parameter) {
        return (int) ((parameter instanceof RubyFixnum
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xff);
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
                : other2long(parameter)) & 0xffffL);
    }

    public static long u16Value64(IRubyObject parameter) {
        return (short) (((parameter instanceof RubyFixnum)
                ? ((RubyFixnum) parameter).getLongValue()
                : other2long(parameter)) & 0xffffL);
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

    public static long chr2long(IRubyObject parameter) {
        CharSequence cs = parameter.asJavaString();
        if (cs.length() == 1) {
            return cs.charAt(0);
        }

        throw parameter.getRuntime().newRangeError("value "
                    + parameter + " is not an integer");
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
    private static final PointerParameterStrategy STRING_POINTER_STRATEGY = new StringPointerParameterStrategy();

    public static PointerParameterStrategy pointerParameterStrategy(IRubyObject parameter) {
        if (parameter instanceof Pointer) {
            return DIRECT_POINTER;

        } else if (parameter instanceof Buffer) {
            return HEAP_POINTER_STRATEGY;

        } else if (parameter instanceof Struct) {
            return ((Struct) parameter).getMemory() instanceof Pointer ? DIRECT_STRUCT : HEAP_STRUCT;

        } else if (parameter.isNil()) {
            return NIL_POINTER_STRATEGY;

        } else if (parameter instanceof RubyString) {
            StringSupport.checkStringSafety(parameter.getRuntime(), parameter);
            return STRING_POINTER_STRATEGY;

        } else if (parameter.respondsTo("to_ptr")) {
            IRubyObject ptr = parameter.callMethod(parameter.getRuntime().getCurrentContext(), "to_ptr");

            return new DelegatingPointerParameterStrategy(ptr, pointerParameterStrategy(ptr));

        } else {
            throw parameter.getRuntime().newTypeError("cannot convert parameter to native pointer");
        }
    }

    private static RuntimeException newObjectCountError(int objCount) {
        return new RuntimeException("invalid object count: " + objCount);
    }

    private static RuntimeException newHeapObjectCountError(int objCount) {
        return new RuntimeException("insufficient number of heap objects supplied (" + objCount + " required)");
    }

    public static long invokeN1OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, int objCount, IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info) {

        if (objCount == 1) {
            return ObjectParameterInvoker.getInstance().invokeN1O1rN(function, n1,
                    s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

        } else {
            throw newObjectCountError(objCount);
        }
    }

    public static long invokeN2OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, long n2, int objCount,
                                  IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info) {
        if (objCount == 1) {
            return ObjectParameterInvoker.getInstance().invokeN2O1rN(function, n1, n2,
                    s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

        } else {
            throw newObjectCountError(objCount);
        }
    }

    public static long invokeN2OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, long n2, int objCount,
                                  IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info,
                                  IRubyObject o2, PointerParameterStrategy s2, ObjectParameterInfo o2info) {
        if (objCount == 1) {
            // only one object is to be passed down as a a heap object - figure out which one
            if (!s1.isDirect()) {
                // do nothing, use the first param as-is

            } else {
                // move second into first place
                o1 = o2; s1 = s2; o1info = o2info;
            }

            return ObjectParameterInvoker.getInstance().invokeN2O1rN(function, n1, n2,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

        } else if (objCount == 2) {
            // Two objects to be passed as heap objects, just use both arguments as-is
            return ObjectParameterInvoker.getInstance().invokeN2O2rN(function, n1, n2,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info);

        } else {
            throw newObjectCountError(objCount);
        }
    }

    public static long invokeN3OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                      long n1, long n2, long n3, int objCount,
                                      IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info) {
        if (objCount == 1) {

            return ObjectParameterInvoker.getInstance().invokeN3O1rN(function, n1, n2, n3,
                    s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

        } else {
            throw newObjectCountError(objCount);
        }
    }


    public static long invokeN3OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                      long n1, long n2, long n3, int objCount,
                                      IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info,
                                      IRubyObject o2, PointerParameterStrategy s2, ObjectParameterInfo o2info) {
        if (objCount == 1) {
            // only one object is to be passed down as a a heap object - figure out which one
            if (!s1.isDirect()) {
                // do nothing, use the first param as-is

            } else {
                // move second into first place
                o1 = o2; s1 = s2; o1info = o2info;
            }

            return ObjectParameterInvoker.getInstance().invokeN3O1rN(function, n1, n2, n3,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

        } else if (objCount == 2) {
            return ObjectParameterInvoker.getInstance().invokeN3O2rN(function, n1, n2, n3,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info);

        } else {
            throw newObjectCountError(objCount);
        }
    }

    public static long invokeN3OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, long n2, long n3, int objCount,
                                  IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info,
                                  IRubyObject o2, PointerParameterStrategy s2, ObjectParameterInfo o2info,
                                  IRubyObject o3, PointerParameterStrategy s3, ObjectParameterInfo o3info) {

        if (objCount < 3) {
            int next;
            // Sort out which is the first non-direct object
            if (!s1.isDirect()) {
                // do nothing, use the first param as-is
                next = 2;

            } else if (!s2.isDirect()) {
                // move second into first place
                o1 = o2; s1 = s2; o1info = o2info;
                next = 3;

            } else {
                // move third into first place
                o1 = o3; s1 = s3; o1info = o3info;
                next = 4;
            }

            if (objCount == 1) {

                return ObjectParameterInvoker.getInstance().invokeN3O1rN(function, n1, n2, n3,
                        s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

            } else if (objCount == 2) {
                // Sort out which is the second non-direct object
                if (next <= 2 && !s2.isDirect()) {
                    // do nothing, use the second param as-is

                } else if (next <= 3) {
                    // move third param into second  place
                    o2 = o3; s2 = s3; o2info = o3info;

                } else {
                    throw newHeapObjectCountError(objCount);
                }

                return ObjectParameterInvoker.getInstance().invokeN3O2rN(function, n1, n2, n3,
                        s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                        s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info);
            } else {
                throw newObjectCountError(objCount);
            }
        }

        // Three objects to be passed as heap objects, just use all arguments as-is
        return ObjectParameterInvoker.getInstance().invokeN3O3rN(function, n1, n2, n3,
            s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
            s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info,
            s3.array(o3), s3.arrayOffset(o3), s3.arrayLength(o3), o3info);
    }

    public static long invokeN4OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, long n2, long n3, long n4, int objCount,
                                  IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info) {

        if (objCount == 1) {
            return ObjectParameterInvoker.getInstance().invokeN4O1rN(function, n1, n2, n3, n4,
                    s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);
        } else {
            throw newObjectCountError(objCount);
        }
    }

    public static long invokeN4OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, long n2, long n3, long n4, int objCount,
                                  IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info,
                                  IRubyObject o2, PointerParameterStrategy s2, ObjectParameterInfo o2info) {
        if (objCount == 1) {
            // only one object is to be passed down as a a heap object - figure out which one
            if (!s1.isDirect()) {
                // do nothing, use the first param as-is

            } else {
                // move second into first place
                o1 = o2; s1 = s2; o1info = o2info;
            }

            return ObjectParameterInvoker.getInstance().invokeN4O1rN(function, n1, n2, n3, n4,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

        } else if (objCount == 2) {
            // Two objects to be passed as heap objects, just use both arguments as-is
            return ObjectParameterInvoker.getInstance().invokeN4O2rN(function, n1, n2, n3, n4,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info);

        } else {
            throw newObjectCountError(objCount);
        }
    }
    
    public static long invokeN4OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, long n2, long n3, long n4, int objCount,
                                  IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info,
                                  IRubyObject o2, PointerParameterStrategy s2, ObjectParameterInfo o2info,
                                  IRubyObject o3, PointerParameterStrategy s3, ObjectParameterInfo o3info) {
        if (objCount < 3) {
            int next;
            // Sort out which is the first non-direct object
            if (!s1.isDirect()) {
                // do nothing, use the first param as-is
                next = 2;

            } else if (!s2.isDirect()) {
                // move second into first place
                o1 = o2; s1 = s2; o1info = o2info;
                next = 3;

            } else {
                // move third into first place
                o1 = o3; s1 = s3; o1info = o3info;
                next = 4;
            }


            if (objCount == 1) {

                return ObjectParameterInvoker.getInstance().invokeN4O1rN(function, n1, n2, n3, n4,
                        s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);

            } else if (objCount == 2) {
                // Sort out which is the second non-direct object

                if (next <= 2 && !s2.isDirect()) {
                    // do nothing, use the second param as-is

                } else if (next <= 3 && !s3.isDirect()) {
                    // move third param into second  place
                    o2 = o3; s2 = s3; o2info = o3info;

                } else {
                    throw newHeapObjectCountError(objCount);
                }

                return ObjectParameterInvoker.getInstance().invokeN4O2rN(function, n1, n2, n3, n4,
                        s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                        s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info);
            } else {
                throw newObjectCountError(objCount);
            }
        }

        // Three objects to be passed as heap objects, just use all arguments as-is
        return ObjectParameterInvoker.getInstance().invokeN4O3rN(function, n1, n2, n3, n4,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info,
                s3.array(o3), s3.arrayOffset(o3), s3.arrayLength(o3), o3info);
    }

    public static long invokeN4OrN(com.kenai.jffi.Invoker invoker, com.kenai.jffi.Function function,
                                  long n1, long n2, long n3, long n4, int objCount,
                                  IRubyObject o1, PointerParameterStrategy s1, ObjectParameterInfo o1info,
                                  IRubyObject o2, PointerParameterStrategy s2, ObjectParameterInfo o2info,
                                  IRubyObject o3, PointerParameterStrategy s3, ObjectParameterInfo o3info,
                                  IRubyObject o4, PointerParameterStrategy s4, ObjectParameterInfo o4info) {
        int next;
        // Sort out which is the first non-direct object
        if (!s1.isDirect()) {
            // do nothing, use the first param as-is
            next = 2;

        } else if (!s2.isDirect()) {
            // move second into first place
            o1 = o2; s1 = s2; o1info = o2info;
            next = 3;

        } else if (!s3.isDirect()) {
            // move third into first place
            o1 = o3; s1 = s3; o1info = o3info;
            next = 4;

        } else {
            // move fourth into first place
            o1 = o4; s1 = s4; o1info = o4info;
            next = 5;
        }

        if (objCount == 1) {
            return ObjectParameterInvoker.getInstance().invokeN4O1rN(function, n1, n2, n3, n4,
                    s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info);
        }

        // Sort out which is the second non-direct object
        if (next <= 2 && !s2.isDirect()) {
            // do nothing, use the second param as-is
            next = 3;

        } else if (next <= 3 && !s3.isDirect()) {
            // move third param into second  place
            o2 = o3; s2 = s3; o2info = o3info;
            next = 4;

        } else if (next <= 4) {
            // move fourth param into second  place
            o2 = o4; s2 = s4; o2info = o4info;
            next = 5;
        }
        
        if (objCount == 2) {
            return ObjectParameterInvoker.getInstance().invokeN4O2rN(function, n1, n2, n3, n4,
                    s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                    s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info);
        }
        
        // Sort out third parameter
        if (next <= 3 && !s3.isDirect()) {
            // do nothing, use the third param as-is

        } else if (next <= 4) {
            // move fourth param into third place
            o3 = o4; s3 = s4; o3info = o4info;

        } else {
            throw newHeapObjectCountError(objCount);
        }

        if (objCount == 3) {
            return ObjectParameterInvoker.getInstance().invokeN4O3rN(function, n1, n2, n3, n4,
                s1.array(o1), s1.arrayOffset(o1), s1.arrayLength(o1), o1info,
                s2.array(o2), s2.arrayOffset(o2), s2.arrayLength(o2), o2info,
                s3.array(o3), s3.arrayOffset(o3), s3.arrayLength(o3), o3info);

        } else {
            throw newObjectCountError(objCount);
        }
    }
}
