/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.api.Check;
import org.jruby.api.Convert;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.JavaSites.TypeConverterSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

public class TypeConverter {
    private static final String[] IMPLICIT_CONVERTED_METHODS = new String[] {
            "to_int", "to_ary", "to_str", "to_sym", "to_hash", "to_proc", "to_io", "to_a", "to_s", "to_i", "to_r"
    };

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to target type
     * @param raise will throw an Error if conversion does not work
     * @return the converted value
     */
    public static IRubyObject convertToType(IRubyObject obj, RubyClass target, String convertMethod, boolean raise) {
        return convertToType(target.getClassRuntime().getCurrentContext(), obj, target, convertMethod, raise);
    }

    public static IRubyObject convertToType(ThreadContext context, IRubyObject obj, RubyClass target, String convertMethod, boolean raise) {
        IRubyObject r = obj.checkCallMethod(context, convertMethod);
        return r == null ? handleUncoercibleObject(context.runtime, obj, target, raise) : r;
    }

    // This is essentially convertToType but it is used when we already know the method we are going to call will exist
    // (as much as we can due to possible race conditions all convert methods have).  This is used in places where we
    // need to explicitly check for the method (e.g. to_ary) before we actually try and convert the value.
    public static IRubyObject convertToTypeUnchecked(ThreadContext context, IRubyObject obj, RubyClass target, String convertMethod, boolean raise) {
        IRubyObject r = obj.callMethod(context, convertMethod);
        return r == null ? handleUncoercibleObject(context.runtime, obj, target, raise) : r;
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type 1.9).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param sites is the CheckedSites call sites to be called to try and convert to targetType
     * @param raise will throw an Error if conversion does not work
     * @return the converted value
     */
    public static IRubyObject convertToType(ThreadContext context, IRubyObject obj, RubyClass target, JavaSites.CheckedSites sites, boolean raise) {
        IRubyObject r = obj.checkCallMethod(context, sites);

        return r == null ? handleUncoercibleObject(context.runtime, obj, target, raise) : r;
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method and raises TypeError exception on failure (MRI: rb_convert_type).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static IRubyObject convertToType(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType(obj, target, convertMethod, true);
        if (!target.isInstance(val)) throw newTypeErrorMismatch(obj.getRuntime(), obj, target, convertMethod, val);
        return val;

    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method and raises TypeError exception on failure (MRI: rb_convert_type).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param sites is the CheckedSites call sites to use to dispatch the convert method
     * @return the converted value
     */
    public static IRubyObject convertToType(ThreadContext context, IRubyObject obj, RubyClass target, JavaSites.CheckedSites sites) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType(context, obj, target, sites, true);
        if (!target.isInstance(val)) throw newTypeErrorMismatch(context.runtime, obj, target, sites.methodName, val);
        return val;
    }

    // MRI: rb_to_float - adjusted to handle also Java numbers (non RubyNumeric types)
    public static RubyFloat toFloat(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyNumeric) return obj.convertToFloat();
        if (obj instanceof RubyString || obj.isNil()) throw typeError(runtime.getCurrentContext(), obj, "Float");

        // Java types which can become floats pass to to_f logic and we don't try and match this error string.
        if (!(obj instanceof JavaProxy) && !obj.getMetaClass().isKindOfModule(runtime.getNumeric())) {
            throw typeError(runtime.getCurrentContext(), str(runtime, "can't convert ", types(runtime, obj.getMetaClass()), " into Float"));
        }
        return (RubyFloat) TypeConverter.convertToType(obj, runtime.getFloat(), "to_f", true);
    }

    /**
     * Checks that this object is of type DATA and then returns it, otherwise raises failure (MRI: Check_Type(obj, T_DATA))
     *
     * @param obj the object to check
     * @return the converted value
     */
    @Deprecated
    public static IRubyObject checkData(IRubyObject obj) {
        if (obj instanceof org.jruby.runtime.marshal.DataType) return obj;

        Ruby runtime = obj.getRuntime();
        throw typeError(runtime.getCurrentContext(), str(runtime, "wrong argument type ", typeAsString(obj), " (expected Data)"));
    }

    public static RubyString typeAsString(IRubyObject obj) {
        if (obj.isNil()) return obj.getRuntime().newString("nil");
        if (obj instanceof RubyBoolean) return obj.getRuntime().newString(obj.isTrue() ? "true" : "false");

        return obj.getMetaClass().getRealClass().rubyName();
    }

    @Deprecated(since = "10.0")
    public static RubySymbol checkID(IRubyObject obj) {
        return Check.checkID(obj.getRuntime().getCurrentContext(), obj);
    }

    /**
     * Convert the supplied object into an internal identifier String.  Basically, symbols
     * are stored internally as raw bytes from whatever encoding they were originally sourced from.
     * When methods are stored they must also get stored in this same raw fashion so that if we
     * use symbols to look up methods or make symbols from these method names they will match up.
     *
     * For 2.2 compatibility, we also force all incoming identifiers to get anchored as hard-referenced symbols.
     */
    @Deprecated(since = "10.0")
    public static RubySymbol checkID(Ruby runtime, String name) {
        return RubySymbol.newHardSymbol(runtime, name.intern());
    }

    /**
     * Higher level conversion utility similar to convertToType but it can throw an
     * additional TypeError during conversion (MRI: rb_check_convert_type).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static IRubyObject convertToTypeWithCheck(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType(obj, target, convertMethod, false);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) {
            throw newTypeErrorMismatch(obj.getRuntime(), obj, target, convertMethod, val);
        }
        return val;
    }

    /**
     * Higher level conversion utility similar to convertToType but it can throw an
     * additional TypeError during conversion (MRI: rb_check_convert_type).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param sites the CheckedSites call sites to use for coersion
     * @return the converted value
     */
    public static IRubyObject convertToTypeWithCheck(ThreadContext context, IRubyObject obj, RubyClass target, JavaSites.CheckedSites sites) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType(context, obj, target, sites, false);
        if (val == context.nil) return val;
        if (!target.isInstance(val)) {
            throw newTypeErrorMismatch(context.runtime, obj, target, sites.methodName, val);
        }
        return val;
    }

    public static RaiseException newTypeError(IRubyObject obj, RubyClass target, String convertMethod, IRubyObject val) {
        return newTypeError(obj.getRuntime(), obj, target, convertMethod, val);
    }

    public static RaiseException newTypeError(Ruby runtime, IRubyObject obj, RubyClass target, String methodName, IRubyObject val) {
        IRubyObject className =  types(runtime, obj.getType());
        String message = isImplicitTypeError(methodName) ? "no implicit conversion of" : "can't convert ";

        return typeError(runtime.getCurrentContext(), str(runtime, message, className, " to ", types(runtime, target)));
    }

    public static RaiseException newTypeErrorMismatch(Ruby runtime, IRubyObject obj, RubyClass target, String methodName, IRubyObject val) {
        IRubyObject className =  types(runtime, obj.getType());

        return typeError(runtime.getCurrentContext(), str(runtime, "can't convert ", className, " to ", types(runtime, target), " (",
                className, '#' + methodName + " gives ", types(runtime, val.getType()), ")"));
    }

    // rb_check_integer_type
    public static IRubyObject checkIntegerType(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyInteger) return obj;

        TypeConverterSites sites = sites(context);

        IRubyObject conv = convertToType(context, obj, context.runtime.getInteger(), sites.to_int_checked, false);
        if (conv.isNil()) {
            return context.nil;
        }
        if (!(conv instanceof RubyInteger)) {
            throw newTypeErrorMismatch(context.runtime, obj, context.runtime.getInteger(), "to_int", conv);
        }
        return conv;
    }

    // rb_check_to_integer
    @Deprecated // Use Convert.checkToInteger
    public static IRubyObject checkToInteger(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyFixnum) return obj;

        TypeConverterSites sites = sites(context);

        IRubyObject conv = convertToTypeWithCheck(context, obj, context.runtime.getInteger(), sites.to_int_checked);

        return conv instanceof RubyInteger ? conv : context.nil;
    }

    // rb_check_to_integer
    @Deprecated // Use Convert.checkToInteger (and MRI uses not to_i so I do not think this signature is needed).
    public static IRubyObject checkToInteger(Ruby runtime, IRubyObject obj, String method) {
        if (method.equals("to_int")) return Convert.checkToInteger(runtime.getCurrentContext(), obj);

        if (obj instanceof RubyFixnum) return obj;

        if (method.equals("to_i")) {
            ThreadContext context = runtime.getCurrentContext();
            TypeConverterSites sites = sites(context);

            IRubyObject conv = convertToTypeWithCheck(context, obj, runtime.getInteger(), sites.to_i_checked);
            return conv instanceof RubyInteger ? conv : runtime.getNil();
        }

        IRubyObject conv = TypeConverter.convertToType(obj, runtime.getInteger(), method, false);
        return conv instanceof RubyInteger ? conv : runtime.getNil();
    }

    // rb_check_to_float
    public static IRubyObject checkFloatType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyFloat) return obj;
        if (!(obj instanceof RubyNumeric)) return runtime.getNil();

        ThreadContext context = runtime.getCurrentContext();
        TypeConverterSites sites = sites(context);

        return TypeConverter.convertToTypeWithCheck(context, obj, runtime.getFloat(), sites.to_f_checked);
    }

    // rb_check_hash_type
    public static IRubyObject checkHashType(Ruby runtime, IRubyObject obj) {
        return checkHashType(runtime, obj, true);
    }

    public static IRubyObject checkHashType(Ruby runtime, IRubyObject obj, boolean raise) {
        if (obj instanceof RubyHash) return obj;
        if (raise) {
            return TypeConverter.convertToTypeWithCheck(obj, runtime.getHash(), "to_hash");
        } else {
            return TypeConverter.convertToType(obj, runtime.getHash(), "to_hash", false);
        }
    }

    // rb_check_hash_type
    public static IRubyObject checkHashType(ThreadContext context, JavaSites.CheckedSites sites, IRubyObject obj) {
        if (obj instanceof RubyHash) return obj;
        return TypeConverter.convertToTypeWithCheck(context, obj, hashClass(context), sites);
    }

    // rb_check_string_type
    public static IRubyObject checkStringType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyString) return obj;
        return TypeConverter.convertToTypeWithCheck(obj, runtime.getString(), "to_str");
    }

    // rb_check_string_type
    public static IRubyObject checkStringType(ThreadContext context, JavaSites.CheckedSites sites, IRubyObject obj) {
        if (obj instanceof RubyString) return obj;
        return TypeConverter.convertToTypeWithCheck(context, obj, stringClass(context), sites);
    }

    // rb_check_string_type
    public static IRubyObject checkStringType(ThreadContext context, JavaSites.CheckedSites sites, IRubyObject obj, RubyClass target) {
        if (obj instanceof RubyString) return obj;
        return TypeConverter.convertToTypeWithCheck(context, obj, target, sites);
    }

    // rb_check_array_type
    public static IRubyObject checkArrayType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyArray) return obj;
        return TypeConverter.convertToTypeWithCheck(obj, runtime.getArray(), "to_ary");
    }

    // rb_check_array_type
    public static IRubyObject checkArrayType(ThreadContext context, JavaSites.CheckedSites sites, IRubyObject obj) {
        if (obj instanceof RubyArray) return obj;
        return TypeConverter.convertToTypeWithCheck(context, obj, arrayClass(context), sites);
    }

    // rb_io_check_io
    public static IRubyObject ioCheckIO(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyIO) return obj;
        return TypeConverter.convertToTypeWithCheck(obj, runtime.getIO(), "to_io");
    }

    // rb_io_get_io
    public static RubyIO ioGetIO(Ruby runtime, IRubyObject obj) {
        return (RubyIO)convertToType(obj, runtime.getIO(), "to_io");
    }

    // MRI: rb_check_array_type
    public static IRubyObject checkArrayType(ThreadContext context, IRubyObject obj) {
        return TypeConverter.convertToTypeWithCheck(context, obj, arrayClass(context), sites(context).to_ary_checked);
    }

    @Deprecated // no longer used
    public static IRubyObject checkArrayType(IRubyObject obj) {
        return checkArrayType(obj.getRuntime().getCurrentContext(), obj);
    }

    public static IRubyObject handleUncoercibleObject(boolean raise, IRubyObject obj, RubyClass target) {
        return handleUncoercibleObject(obj.getRuntime(), obj, target, raise);
    }

    public static IRubyObject handleUncoercibleObject(Ruby runtime, IRubyObject obj, RubyClass target, boolean raise) {
        if (raise) throw typeError(runtime.getCurrentContext(), str(runtime, "no implicit conversion of ", typeAsString(obj), " into " , target));
        return runtime.getNil();
    }

    @Deprecated // not-used
    public static IRubyObject handleImplicitlyUncoercibleObject(boolean raise, IRubyObject obj, RubyClass target) {
        return handleUncoercibleObject(obj.getRuntime(), obj, target, raise);
    }

    // rb_check_type and Check_Type
    public static void checkType(ThreadContext context, IRubyObject x, final RubyModule type) {
        assert x != RubyBasicObject.UNDEF;

        ClassIndex xt = x.getMetaClass().getClassIndex();

        // MISSING: special error for T_DATA of a certain type (isInstance is attempt at similar behavior)
        if (xt != type.getClassIndex() && !type.isInstance(x)) {
            Ruby runtime = context.runtime;
            throw typeError(context, str(runtime, "wrong argument type ", types(runtime, x.getMetaClass()), " (expected ", types(runtime, type), ")"));
        }
    }

    // rb_convert_to_integer
    public static IRubyObject convertToInteger(ThreadContext context, IRubyObject val, int base, boolean exception) {
        final Ruby runtime = context.runtime;
        IRubyObject tmp;

        if (base != 0) {
            tmp = TypeConverter.checkStringType(context.runtime, val);

            if (!tmp.isNil()) {
                val = tmp;
            } else if (!exception) {
                return context.nil;
            } else {
                //raise
                return raiseIntegerBaseError(context, true);
            }
        }

        if (val instanceof RubyFloat) {
            RubyFloat f = (RubyFloat) val;
            if (!exception && !Double.isFinite(f.getDoubleValue())) return context.nil;
            return RubyNumeric.dbl2ival(context.runtime, f.getValue());
        } else if (val instanceof RubyInteger) {
            return val;
        } else if (val instanceof RubyString) {
            return RubyNumeric.str2inum(context.runtime, (RubyString) val, base, true, exception);
        } else if (val.isNil()) {
            if (!exception) return context.nil;
            throw typeError(context, "can't convert nil into Integer");
        }

        try {
            tmp = TypeConverter.convertToType(context, val, runtime.getInteger(), sites(context).to_int_checked, false);
            if (tmp instanceof RubyInteger) {
                return tmp;
            }
        } catch (RaiseException re) {
            if (!exception) return context.nil;
            throw re;
        }

        if (!exception) {
            try {
                IRubyObject ret = TypeConverter.convertToType(context, val, runtime.getInteger(), sites(context).to_i_checked, false);
                if (ret instanceof RubyInteger) return ret;
            } catch (RaiseException re) {
                if (exception) throw re;
            }

            return context.nil;
        }

        return val.convertToInteger("to_i");
    }

    public static IRubyObject convertToInteger(ThreadContext context, IRubyObject val, int base) {
        return convertToInteger(context, val, base, true);
    }

    // MRI: rb_Array
    public static RubyArray rb_Array(ThreadContext context, IRubyObject val) {
        IRubyObject tmp = checkArrayType(context, val); // to_ary

        if (tmp == context.nil) {
            TypeConverterSites sites = sites(context);
            tmp = convertToTypeWithCheck(context, val, arrayClass(context), sites.to_a_checked);
            if (tmp == context.nil) return newArray(context, val);
        }
        return (RubyArray) tmp;
    }

    // MRI: to_ary
    public static RubyArray to_ary(ThreadContext context, IRubyObject ary) {
        return (RubyArray) convertToType(context, ary, arrayClass(context), sites(context).to_ary_checked);
    }

    private static IRubyObject raiseIntegerBaseError(ThreadContext context, boolean exception) {
        if (!exception) return context.nil;

        throw argumentError(context, "base specified for non string value");
    }

    public static TypeConverterSites sites(ThreadContext context) {
        return context.sites.TypeConverter;
    }

    @Deprecated
    public static IRubyObject convertToType(IRubyObject obj, RubyClass target, int convertMethodIndex, String convertMethod, boolean raise) {
        if (!obj.respondsTo(convertMethod)) return handleUncoercibleObject(raise, obj, target);

        return obj.callMethod(obj.getRuntime().getCurrentContext(), convertMethod);
    }

    @Deprecated
    public static IRubyObject convertToType(IRubyObject obj, RubyClass target, int convertMethodIndex, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType(obj, target, convertMethod, true);
        if (!target.isInstance(val)) {
            Ruby runtime = obj.getRuntime();
            throw typeError(runtime.getCurrentContext(), str(runtime, types(runtime, obj.getMetaClass()),
                    "#" + convertMethod + " should return ", types(runtime, target)));
        }
        return val;
    }

    @Deprecated
    public static IRubyObject convertToTypeWithCheck(IRubyObject obj, RubyClass target, int convertMethodIndex, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = TypeConverter.convertToType(obj, target, convertMethod, false);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) {
            Ruby runtime = obj.getRuntime();
            throw typeError(runtime.getCurrentContext(), str(runtime, types(runtime, obj.getMetaClass()),
                    "#" + convertMethod + " should return ", types(runtime, target)));
        }
        return val;
    }

    @Deprecated
    public static String convertToIdentifier(IRubyObject obj) {
        // Assume Symbol already returns ISO8859-1/raw bytes from asJavaString()
        // Assume all other objects cannot participate in providing raw bytes since we cannot
        // grab it's string representation without calling a method which properly encodes
        // the string.
        if (obj instanceof RubyString) {
            return RubyEncoding.decodeRaw(((RubyString) obj).getByteList()).intern();
        }
        return obj.asJavaString().intern();
    }
    /**
     * Higher level conversion utility similar to convertToType but it can throw an
     * additional TypeError during conversion (MRI: rb_check_convert_type).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    @Deprecated // no longer used
    public static IRubyObject convertToTypeOrRaise(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = TypeConverter.convertToType(obj, target, convertMethod, true);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) {
            Ruby runtime = obj.getRuntime();
            throw typeError(runtime.getCurrentContext(), str(runtime, types(runtime, obj.getMetaClass()),
                    "#" + convertMethod + " should return ", types(runtime, target)));
        }
        return val;
    }

    private static boolean isImplicitTypeError(String methodID) {
        for (int i = 0; i < IMPLICIT_CONVERTED_METHODS.length; i++) {
            if (IMPLICIT_CONVERTED_METHODS[i].equals(methodID)) return true;
        }

        return false;
    }

    // MRI: rb_bool_expected
    public static boolean booleanExpected(ThreadContext context, IRubyObject object, String id) {
        if (object == context.tru) return true;
        if (object == context.fals) return false;

        throw argumentError(context,
                str(context.runtime, "true or false is expected as ", Convert.asSymbol(context, id), ": ", object));
    }
}
