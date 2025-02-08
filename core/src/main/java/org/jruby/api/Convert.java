package org.jruby.api;

import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static org.jruby.RubyBignum.big2long;
import static org.jruby.RubyNumeric.num2int;
import static org.jruby.RubyNumeric.num2long;
import static org.jruby.api.Error.rangeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.TypeConverter.convertToTypeWithCheck;
import static org.jruby.util.TypeConverter.sites;

/**
 * Conversion utilities.
 * <p>
 * By convention if a method has `As` in it then it implies it is already the thing and it may error
 * if wrong.  If it has `To` in it then it implies it is converting to that thing and it might not
 * be that thing.  For example, `integerAsInt` implies the value is already an int and will error if
 * it is not.  `checkToInteger` implies the value might not be an integer and that it may try and convert
 * it to one.
 * <p>
 * Methods where the parameter to `As` methods will omit the type from in front of as.  For example,
 * `longAsInteger` will be `asInteger(context, long)`.  Additionally, naming is terse but in cases where
 * something is ambiguous (asFloat() return a Ruby float but if we need a Java equivalent it will take
 * the extra naming asJavaFloat()).  Luckily for Java primitives as Ruby types there are not too many
 * conflicts.
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
     * Create a Ruby Float from a java double.
     * @param context the current thread context
     * @param value the double value
     * @return the Ruby Float
     */
    public static RubyFloat asFloat(ThreadContext context, double value) {
        return RubyFloat.newFloat(context.runtime, value);
    }

    /**
     * Create a Ruby Float from a java long.
     * @param context the current thread context
     * @param value the long value
     * @return the Ruby Float
     */
    public static RubyFloat asFloat(ThreadContext context, long value) {
        return RubyFloat.newFloat(context.runtime, value);
    }

    // MRI: rb_num2long and FIX2LONG (numeric.c)
    /**
     * Safely convert a Ruby Numeric into a java long value.  Raising if the value will not fit.
     * @param context the current thread context
     * @param arg the RubyNumeric to convert
     * @return the long value
     */
    public static long toLong(ThreadContext context, IRubyObject arg) {
        return num2long(arg);
    }

    /**
     * Safely convert a Ruby Numeric into a java long value.  Raising if the value will not fit.
     * @param context the current thread context
     * @param arg the RubyNumeric to convert
     * @return the int value
     */
    public static int toInt(ThreadContext context, IRubyObject arg) {
        return num2int(arg);
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
     * @return the new RubyString
     */
    public static RubySymbol asSymbol(ThreadContext context, RubyString string) {
        return context.runtime.newSymbol(string.getByteList());
    }
}
