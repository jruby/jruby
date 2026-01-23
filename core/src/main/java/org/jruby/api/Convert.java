package org.jruby.api;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.Builtins;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.jruby.RubyBignum.LONG_MAX;
import static org.jruby.RubyBignum.LONG_MIN;
import static org.jruby.RubyNumeric.negFixable;
import static org.jruby.RubyNumeric.posFixable;
import static org.jruby.api.Access.floatClass;
import static org.jruby.api.Access.integerClass;
import static org.jruby.api.Error.floatDomainError;
import static org.jruby.api.Error.rangeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.TypeConverter.convertToTypeWithCheck;
import static org.jruby.util.TypeConverter.sites;

/**
 * <p>Conversion utilities.
 *
 * <p>There are 3 mechanisms for converting between types:
 *  * <ul>
 *     <li>type#getValue() where we know exactly what it is and we will be responsible for how it is used</li>
 *     <li>`as` where we know it is the thing but we will not care about truncation or range issues</li>
 *     <li>`to` where we do not know but hope it is the thing OR we know but need to check range</li>
 * </ul>
 *
 * <p>For example, I have a RubyFixnum and I know I need a long I can just call `getValue()`.  If I need an
 * int and I know enough about it to not care about range I can call `asInt`.  If I do need check range I can
 * call `toInt(context, (RubyFixnum) value)` and it will make sure it is a valid int value.   If I don't know
 * for sure if it is even capable of being an int I will call `toInt(context, (IRubyObject) value)`.
 *
 * <p>The naming conventions will tend to be {resolvedType}?[As|To}{returnedType} where {resolvedType} is omitted
 * when the convention is obvious (`long toLong(ThreadContext context, IRubyObject)`).  Right now we have
 * `RubyInteger toInteger(ThreadContext, IRubyObject)` which means making a Ruby Integer vs a Java boxed Integer.
 * If we ever needed a Java Integer (narrator: we won't) we would have to break this convention and make
 * `Integer toJavaInteger(ThreadContext, IRubyObject)`.  There are no example of {resolvedType} in any
 * conversion methods but this is reserved in case we have more naming conflicts due to overlapping names.
 */
public class Convert {
    /**
     * Cast the given value to a RubyArray with most basic typeError thrown
     * if the value is not a RubyArray.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyArray
     */
    public static RubyArray castAsArray(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyArray)) throw typeError(context, newValue, "Array");
        return (RubyArray) newValue;
    }

    /**
     * Cast the given value to a RubyArray with most basic typeError thrown
     * if the value is not a RubyArray. Note: if message is constructed you will pay
     * that contruction cost.  Manually cast to avoid that overhead in that case.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @param message the message to include in the type error
     * @return the value as a RubyArray
     */
    public static RubyArray castAsArray(ThreadContext context, IRubyObject newValue, String message) {
        if (!(newValue instanceof RubyArray)) throw typeError(context, newValue, message);
        return (RubyArray) newValue;
    }

    /**
     * Cast the given value to a RubyBignum with most basic typeError thrown
     * if the value is not a RubyBignum.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyBignum
     */
    public static RubyBignum castAsBignum(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyBignum)) throw typeError(context, newValue, "Bignum");
        return (RubyBignum) newValue;
    }

    /**
     * Cast the given value to a RubyClass with most basic typeError thrown
     * if the value is not a RubyClass.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyClass
     */
    public static RubyClass castAsClass(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyClass)) throw typeError(context, newValue, "Class");
        return (RubyClass) newValue;
    }

    /**
     * Cast the given value to a RubyFile with most basic typeError thrown
     * if the value is not a RubyFile.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyFile
     */
    public static RubyFile castAsFile(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyFile)) throw typeError(context, newValue, "File");
        return (RubyFile) newValue;
    }

    /**
     * Cast the given value to a RubyFixnum with most basic typeError thrown
     * if the value is not a RubyFixnum.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyFixnum
     */
    public static RubyFixnum castAsFixnum(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyFixnum)) throw typeError(context, newValue, "Fixnum");
        return (RubyFixnum) newValue;
    }

    /**
     * Cast the given value to a RubyFixnum with most basic typeError thrown
     * if the value is not a RubyFixnum. Note: if message is constructed you will pay
     * that contruction cost.  Manually cast to avoid that overhead in that case.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @param message the message to include in the type error
     * @return the value as a RubyFixnum
     */
    public static RubyFixnum castAsFixnum(ThreadContext context, IRubyObject newValue, String message) {
        if (!(newValue instanceof RubyFixnum)) throw typeError(context, message);
        return (RubyFixnum) newValue;
    }

    /**
     * Cast the given value to a RubyHash with most basic typeError thrown
     * if the value is not a RubyHash.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyHash
     */
    public static RubyHash castAsHash(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyHash)) throw typeError(context, newValue, "Hash");
        return (RubyHash) newValue;
    }

    /**
     * Cast the given value to a RubyHash with most basic typeError thrown
     * if the value is not a RubyHash. Note: if message is constructed you will pay
     * that contruction cost.  Manually cast to avoid that overhead in that case.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @param message the message to include in the type error
     * @return the value as a RubyHash
     */
    public static RubyHash castAsHash(ThreadContext context, IRubyObject newValue, String message) {
        if (!(newValue instanceof RubyHash)) throw typeError(context, message);
        return (RubyHash) newValue;
    }

    /**
     * Cast the given value to a RubyInteger with most basic typeError thrown
     * if the value is not a RubyInteger.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyInteger
     */
    public static RubyInteger castAsInteger(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyInteger)) throw typeError(context, newValue, "Integer");
        return (RubyInteger) newValue;
    }

    /**
     * Cast the given value to a RubyInteger with most basic typeError thrown
     * if the value is not a RubyInteger. Note: if message is constructed you will pay
     * that contruction cost.  Manually cast to avoid that overhead in that case.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @param message the message to include in the type error
     * @return the value as a RubyInteger
     */
    public static RubyInteger castAsInteger(ThreadContext context, IRubyObject newValue, String message) {
        if (!(newValue instanceof RubyInteger)) throw typeError(context, message);
        return (RubyInteger) newValue;
    }


    /**
     * Cast the given value to a RubyModule with most basic typeError thrown
     * if the value is not a RubyModule.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyModule
     */
    public static RubyModule castAsModule(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyModule)) throw typeError(context, newValue, "Module");
        return (RubyModule) newValue;
    }

    /**
     * Cast the given value to a RubyModule with most basic typeError thrown
     * if the value is not a RubyModule.  Note: if message is constructed you will pay
     * that contruction cost.  Manually cast to avoid that overhead in that case.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @param message the message to include in the type error
     * @return the value as a RubyModule
     */
    public static RubyModule castAsModule(ThreadContext context, IRubyObject newValue, String message) {
        if (!(newValue instanceof RubyModule)) throw typeError(context, message);
        return (RubyModule) newValue;
    }

    /**
     * Cast the given value to a RubyNumeric with most basic typeError thrown
     * if the value is not a RubyNumeric. Note: if message is constructed you will pay
     * that contruction cost.  Manually cast to avoid that overhead in that case.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @param message the message to include in the type error
     * @return the value as a RubyNumeric
     */
    public static RubyNumeric castAsNumeric(ThreadContext context, IRubyObject newValue, String message) {
        if (!(newValue instanceof RubyNumeric)) throw typeError(context, message);
        return (RubyNumeric) newValue;
    }

    /**
     * Cast the given value to a RubyProc with most basic typeError thrown
     * if the value is not a RubyProc.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyProc
     */
    public static RubyProc castAsProc(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyProc)) throw typeError(context, newValue, "Proc");
        return (RubyProc) newValue;
    }

    /**
     * Cast the given value to a RubyProc with most basic typeError thrown
     * if the value is not a RubyProc. Note: if message is constructed you will pay
     * that contruction cost.  Manually cast to avoid that overhead in that case.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @param message the message to include in the type error
     * @return the value as a RubyProc
     */
    public static RubyProc castAsProc(ThreadContext context, IRubyObject newValue, String message) {
        if (!(newValue instanceof RubyProc)) throw typeError(context, message);
        return (RubyProc) newValue;
    }

    /**
     * Cast the given value to a RubyRange with most basic typeError thrown
     * if the value is not a RubyRange.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyRange
     */
    public static RubyRange castAsRange(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyRange)) throw typeError(context, newValue, "Range");
        return (RubyRange) newValue;
    }

    /**
     * Cast the given value to a RubyString with most basic typeError thrown
     * if the value is not a RubyString.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubyString
     */
    public static RubyString castAsString(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubyString)) throw typeError(context, newValue, "String");
        return (RubyString) newValue;
    }


    /**
     * Cast the given value to a RubySymbol with most basic typeError thrown
     * if the value is not a RubySymbol.
     *
     * @param context the current thread context
     * @param newValue the value to cast
     * @return the value as a RubySymbol
     */
    public static RubySymbol castAsSymbol(ThreadContext context, IRubyObject newValue) {
        if (!(newValue instanceof RubySymbol)) throw typeError(context, newValue, "Symbol");
        return (RubySymbol) newValue;
    }

    // FIXME: Create annotation @MRI so we can formalize these comments and provide a dictionary for embedders.
    // MRI: rb_check_to_integer
    /**
     * Check whether the given object is an Integer or can be converted to an Integer using #to_int.
     * @param context the current thread context
     * @param obj the object to be converted
     * @return the integer value or nil if the object or conversion is not an Integer.
     */
    public static IRubyObject checkToInteger(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyFixnum) return obj;

        JavaSites.TypeConverterSites sites = sites(context);

        IRubyObject conv = convertToTypeWithCheck(context, obj, context.runtime.getInteger(), sites.to_int_checked);

        return conv instanceof RubyInteger ? conv : context.nil;
    }

    // MRI: rb_check_convert_type with Rational and to_r
    /**
     * Convert the given argument to a Rational, or return nil if it cannot be converted.
     *
     * @param context the current thread context
     * @param obj the object to convert
     * @return a Rational based on the object, or nil if it could not be converted
     */
    public static IRubyObject checkToRational(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyRational) return obj;

        JavaSites.TypeConverterSites sites = sites(context);

        IRubyObject conv = convertToTypeWithCheck(context, obj, context.runtime.getRational(), sites.to_r_checked);

        return conv instanceof RubyRational ? conv : context.nil;
    }

    // MRI: rb_check_to_string
    /**
     * Check whether the given object is a String or can be converted to a String using #to_str.
     * @param context the current thread context
     * @param obj the object to be converted
     * @return the String value or nil if the object or conversion is not a String.
     */
    public static IRubyObject checkToString(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyString) return obj;

        JavaSites.TypeConverterSites sites = sites(context);

        IRubyObject conv = convertToTypeWithCheck(context, obj, context.runtime.getString(), sites.to_str_checked);

        return conv instanceof RubyInteger ? conv : context.nil;
    }

    /**
     * Check to make sure the long num given will fit into an int.
     *
     * @param context the current thread context
     * @param num the long to check
     * @return the int value
     */
    public static int checkInt(ThreadContext context, long num) {
        if (((int) num) != num) {
            throw rangeError(context, "integer " + num +
                    (num < Integer.MIN_VALUE ? " too small to convert to 'int'" : " too big to convert to 'int'"));
        }

        return (int) num;
    }

    /**
     * Create a Ruby Boolean from a java boolean.
     * @param context the current thread context
     * @param value the boolean value
     * @return the Ruby Boolean
     */
    public static RubyBoolean asBoolean(ThreadContext context, boolean value) {
        return value ? context.tru : context.fals;
    }

    /**
     * Create a Ruby Fixnum from a java long.
     *
     * MRI: RB_INT2FIX
     * @param context the current thread context
     * @param value the long value
     * @return the Ruby Fixnum
     */
    public static RubyFixnum asFixnum(ThreadContext context, long value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    /**
     * Create a Ruby Fixnum from a java int.
     * @param context the current thread context
     * @param value the int value
     * @return the Ruby Fixnum
     */
    public static RubyFixnum asFixnum(ThreadContext context, int value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    /**
     * Create a Ruby Fixnum from a java short.
     * @param context the current thread context
     * @param value the short value
     * @return the Ruby Fixnum
     */
    public static RubyFixnum asFixnum(ThreadContext context, short value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    /**
     * Create a Ruby Fixnum from a java byte.
     * @param context the current thread context
     * @param value the byte value
     * @return the Ruby Fixnum
     */
    public static RubyFixnum asFixnum(ThreadContext context, byte value) {
        return RubyFixnum.newFixnum(context.runtime, value);
    }

    /**
     * Create a Ruby Float from a java double.
     * @param context the current thread context
     * @param value the double value
     * @return the Ruby Float
     */
    public static RubyFloat asFloat(ThreadContext context, double value) {
        return RubyFloat.newFloat(context.runtime, value);
    }

    // MRI: macro DBL2NUM
    /**
     * Create a Ruby Float from a java long.
     * @param context the current thread context
     * @param value the long value
     * @return the Ruby Float
     */
    public static RubyFloat asFloat(ThreadContext context, long value) {
        return RubyFloat.newFloat(context.runtime, value);
    }

    // MRI: macro DBL2IVAL
    /**
     * Create some type of Ruby Integer from a java double
     * @param context the current thread context
     * @param value the double value
     * @return the result
     */
    public static RubyInteger asInteger(ThreadContext context, double value) {
        // MRI: macro FIXABLE, RB_FIXABLE (inlined + adjusted) :
        if (Double.isNaN(value) || Double.isInfinite(value))  {
            throw floatDomainError(context, Double.toString(value));
        }

        final long fix = (long) value;
        if (fix == RubyFixnum.MIN || fix == RubyFixnum.MAX) {
            BigInteger big = BigDecimal.valueOf(value).toBigInteger();
            if (posFixable(big) && negFixable(big)) return asFixnum(context, fix);
        } else if (posFixable(value) && negFixable(value)) {
            return asFixnum(context, fix);
        }
        return RubyBignum.newBignorm(context.runtime, value);
    }

    public static byte toByte(ThreadContext context, IRubyObject arg) {
        // weird wrinkle in old and this impl...empty strings error in toInt
        if (arg instanceof RubyString str && !str.isEmpty()) {
            return (byte) str.getByteList().get(0);
        }

        return (byte) toInt(context, arg);
    }

    // MRI: rb_num2dbl and NUM2DBL
    /**
     * Safely convert a Ruby Numeric into a java double value.  Raising if the value will not fit.
     * @param context the current thread context
     * @param arg the Object to convert
     * @return the value
     */
    public static double toDouble(ThreadContext context, IRubyObject arg) {
        return switch (arg) {
            case RubyFloat flote -> flote.getValue();
            case RubyInteger integer when Builtins.checkIntegerToF(context) -> integer.asDouble(context);
            case RubyRational rational when Builtins.checkRationalToF(context) -> rational.asDouble(context);
            case RubyString a -> throw typeError(context, "can't convert String to Float");
            case RubyNil a -> throw typeError(context, "can't convert nil to Float");
            case RubyBoolean a -> throw typeError(context, "can't convert " + (arg.isTrue() ? "true" : "false" + " to Float"));
            default -> ((RubyFloat) TypeConverter.convertToType(arg, floatClass(context), "to_f")).getValue();
        };
    }

    // MRI: rb_num2long and FIX2LONG (numeric.c)
    /**
     * Safely convert a Ruby Numeric into a java long value.  Raising if the value will not fit.
     * @param context the current thread context
     * @param arg the RubyNumeric to convert
     * @return the value
     */
    public static long toLong(ThreadContext context, IRubyObject arg) {
        return switch (arg) {
            case RubyFixnum fixnum -> fixnum.getValue();
            case RubyFloat flote -> toLong(context, flote);
            case RubyBignum bignum -> toLong(context, bignum);
            default -> toLongOther(context, arg);
        };
    }

    public static BigInteger toLongLong(ThreadContext context, IRubyObject arg) {
        return switch (arg) {
            case RubyFixnum fixnum -> BigInteger.valueOf(fixnum.getValue());
            case RubyFloat flote -> BigInteger.valueOf(toLong(context, flote));
            case RubyBignum bignum -> bignum.getValue();
            default -> toLongLongOther(context, arg);
        };
    }

    public static long toLong(ThreadContext context, RubyBignum value) {
        BigInteger big = value.getValue();

        if (big.compareTo(LONG_MIN) < 0 || big.compareTo(LONG_MAX) > 0) {
            throw rangeError(context, "bignum too big to convert into 'long'");
        }

        return big.longValue();
    }

    public static long toLong(ThreadContext context, RubyFloat value) {
        final double aFloat = value.getValue();

        if (aFloat <= (double) Long.MAX_VALUE && aFloat >= (double) Long.MIN_VALUE) {
            return (long) aFloat;
        }

        throw rangeError(context, "float " + aFloat + " out of range of integer");
    }

    // toLong handles all known types and this is only called when we need to try to_int.
    private static long toLongOther(ThreadContext context, IRubyObject arg) {
        if (arg.isNil()) throw typeError(context, "no implicit conversion from nil to integer");

        return toLong(context, TypeConverter.convertToType(arg, integerClass(context), "to_int"));
    }

    private static BigInteger toLongLongOther(ThreadContext context, IRubyObject arg) {
        if (arg.isNil()) throw typeError(context, "no implicit conversion from nil to integer");

        return toLongLong(context, TypeConverter.convertToType(arg, integerClass(context), "to_int"));
    }

    /**
     * Safely convert a Ruby Numeric into a java long value.  Raising if the value will not fit.
     * @param context the current thread context
     * @param arg the RubyNumeric to convert
     * @return the int value
     */
    public static int toInt(ThreadContext context, IRubyObject arg) {
        long value = switch (arg) {
            case RubyFixnum fixnum -> fixnum.getValue();
            case RubyFloat flote -> toInt(context, flote);
            case RubyBignum bignum -> toLong(context, bignum);
            default -> toIntOther(context, arg);
        };
        checkInt(context, value);
        return (int) value;
    }

    public static long toInt(ThreadContext context, RubyFloat value) {
        final double aFloat = value.getValue();

        if (aFloat <= (double) Long.MAX_VALUE && aFloat >= (double) Long.MIN_VALUE) {
            return (long) aFloat;
        }

        throw rangeError(context, "float " + aFloat + " out of range of integer");
    }

    public static int toInt(ThreadContext context, RubyFixnum arg) {
        long value = arg.getValue();
        checkInt(context, value);
        return (int) value;
    }

    private static long toIntOther(ThreadContext context, IRubyObject arg) {
        if (arg.isNil()) throw typeError(context, "no implicit conversion from nil to integer");

        return ((RubyInteger) TypeConverter.convertToType(arg, integerClass(context), "to_int")).asLong(context);
    }


    /**
     * Safely convert a Ruby Numeric into a java long value.  Raising if the value will not fit.
     * @param context the current thread context
     * @param arg the RubyNumeric to convert
     * @return the int value
     */
    public static RubyInteger toInteger(ThreadContext context, IRubyObject arg) {
        // FIXME: Make proper impl which is amalgam of RubyNumeric num2int and convertToInteger and hen have numTo{Long,Int} use this
        return arg.convertToInteger();
    }

    /**
     * Create a Java String from a ByteList with the specified encoding.
     * @param bytes to be made into a string
     * @return a new Java String
     */
    public static String asJavaString(ByteList bytes) {
        var encoding = bytes.getEncoding();
        return encoding == UTF8Encoding.INSTANCE ?
                RubyEncoding.decodeUTF8(bytes.unsafeBytes(), bytes.begin(), bytes.length()) :
                RubyEncoding.decode(bytes.getUnsafeBytes(), bytes.begin(), bytes.length(), encoding.getCharset());
    }

    /**
     * Creates a new RubySymbol from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a symbol
     * @return the new RubyString
     */
    public static RubySymbol asSymbol(ThreadContext context, String string) {
        return context.runtime.newSymbol(string);
    }

    /**
     * Creates a new RubySymbol from the provided java String.
     *
     * @param context the current thread context
     * @param bytelist the contents to become a symbol
     * @return the new RubyString
     */
    public static RubySymbol asSymbol(ThreadContext context, ByteList bytelist) {
        return context.runtime.newSymbol(bytelist);
    }

    /**
     * Creates a new RubySymbol from the provided java String.
     *
     * @param context the current thread context
     * @param string the contents to become a symbol
     * @return the new RubySymbol
     */
    public static RubySymbol asSymbol(ThreadContext context, RubyString string) {
        return context.runtime.newSymbol(string.getByteList());
    }

    /**
     * Convert the given object to a Symbol, possibly calling to_str in the process.
     *
     * MRI: rb_to_symbol
     * @param context the current thread context
     * @param arg the object to convert to a symbol
     * @return the new RubySymbol
     */
    public static RubySymbol toSymbol(ThreadContext context, IRubyObject arg) {
        return RubySymbol.toSymbol(context, arg);
    }

    /**
     * Produce a string from a given object using its type identity.
     *
     * Equivalent to RubyBasicObject#anyToString but without re-acquiring context.
     *
     * @return The object represented as a hashy type string.
     */
    public static RubyString anyToString(ThreadContext context, IRubyObject obj) {
        /* 6:tags 16:addr 1:eos */
        String hex = Integer.toHexString(System.identityHashCode(obj));
        ByteList className = obj.getType().toRubyString(context).getByteList();
        ByteList bytes = new ByteList(2 + className.realSize() + 3 + hex.length() + 1);
        bytes.setEncoding(className.getEncoding());
        bytes.append('#').append('<');
        bytes.append(className);
        bytes.append(':').append('0').append('x');
        bytes.append(hex.getBytes());
        bytes.append('>');

        return Create.newString(context, bytes);
    }
}
