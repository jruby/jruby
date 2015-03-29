/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import org.jruby.RubyBignum;
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
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ClassIndex;
import org.jruby.RubyNil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;
import java.util.List;

public class TypeConverter {
    private static final List<String> IMPLICIT_CONVERSIONS =
        Arrays.asList("to_int", "to_ary", "to_str", "to_sym", "to_hash", "to_proc", "to_io");

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @param raise will throw an Error if conversion does not work
     * @return the converted value
     */
    public static IRubyObject convertToType(IRubyObject obj, RubyClass target, String convertMethod, boolean raise) {
        if (!obj.respondsTo(convertMethod)) {
            if (IMPLICIT_CONVERSIONS.contains(convertMethod)) {
                return handleImplicitlyUncoercibleObject(raise, obj, target);
            } else {
                return handleUncoercibleObject(raise, obj, target);
            }
        }

        return obj.callMethod(obj.getRuntime().getCurrentContext(), convertMethod);
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type 1.9).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @param raise will throw an Error if conversion does not work
     * @return the converted value
     */
    public static IRubyObject convertToType19(IRubyObject obj, RubyClass target, String convertMethod, boolean raise) {
        IRubyObject r = obj.checkCallMethod(obj.getRuntime().getCurrentContext(), convertMethod);
        
        return r == null ? handleUncoercibleObject(raise, obj, target) : r;
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
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method and raises TypeError exception on failure (MRI: rb_convert_type in 1.9).
     *
     * @param obj the object to convert
     * @param target is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static IRubyObject convertToType19(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType19(obj, target, convertMethod, true);
        if (!target.isInstance(val)) {
            String cname = obj.getMetaClass().toString();
            throw obj.getRuntime().newTypeError("can't convert " + cname + " to " + target.getName() + " (" + cname + "#" + convertMethod + " gives " + val.getMetaClass() + ")");
        }
        return val;
    }

    // MRI: rb_to_float 1.9
    public static RubyNumeric toFloat(Ruby runtime, IRubyObject obj) {
        RubyClass floatClass = runtime.getFloat();
        
        if (floatClass.isInstance(obj)) return (RubyNumeric) obj;
        if (!runtime.getNumeric().isInstance(obj)) throw runtime.newTypeError(obj, "Float");

        return (RubyNumeric) convertToType19(obj, floatClass, "to_f", true);
    }
    /**
     * Checks that this object is of type DATA and then returns it, otherwise raises failure (MRI: Check_Type(obj, T_DATA))
     *
     * @param obj the object to check
     * @return the converted value
     */
    public static IRubyObject checkData(IRubyObject obj) {
        if(obj instanceof org.jruby.runtime.marshal.DataType) return obj;

        throw obj.getRuntime().newTypeError("wrong argument type " + typeAsString(obj) + " (expected Data)");
    }
    
    private static String typeAsString(IRubyObject obj) {
        if (obj.isNil()) return "nil";
        if (obj instanceof RubyBoolean) return obj.isTrue() ? "true" : "false";

        return obj.getMetaClass().getRealClass().getName();
    }

    /**
     * Convert the supplied object into an internal identifier String.  Basically, symbols
     * are stored internally as raw bytes from whatever encoding they were originally sourced from.
     * When methods are stored they must also get stored in this same raw fashion so that if we
     * use symbols to look up methods or make symbols from these method names they will match up.
     */
    public static String convertToIdentifier(IRubyObject obj) {
        // Assume Symbol already returns ISO8859-1/raw bytes from asJavaString()
        // Assume all other objects cannot participate in providing raw bytes since we cannot
        // grab it's string representation without calling a method which properly encodes
        // the string.
        if (obj instanceof RubyString) {
            return new String(ByteList.plain(((RubyString) obj).getByteList()), RubyEncoding.ISO).intern();
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
    public static IRubyObject convertToTypeWithCheck(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = TypeConverter.convertToType(obj, target, convertMethod, false);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
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
    public static IRubyObject convertToTypeWithCheck19(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = TypeConverter.convertToType19(obj, target, convertMethod, false);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) {
            String cname = obj.getMetaClass().getName();
            throw obj.getRuntime().newTypeError("can't convert " + cname + " to " + target.getName() + " (" + cname + "#" + convertMethod + " gives " + val.getMetaClass().getName() + ")");
        }
        return val;
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
    public static IRubyObject convertToTypeOrRaise(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = TypeConverter.convertToType(obj, target, convertMethod, true);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    // rb_check_to_integer
    public static IRubyObject checkIntegerType(Ruby runtime, IRubyObject obj, String method) {
        if (obj instanceof RubyFixnum) return obj;

        IRubyObject conv = TypeConverter.convertToType(obj, runtime.getInteger(), method, false);
        return conv instanceof RubyInteger ? conv : runtime.getNil();
    }

    // rb_check_to_float
    public static IRubyObject checkFloatType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyFloat) return obj;
        if (!(obj instanceof RubyNumeric)) return runtime.getNil();

        return TypeConverter.convertToTypeWithCheck(obj, runtime.getFloat(), "to_f");
    }

    // rb_check_hash_type
    public static IRubyObject checkHashType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyHash) return obj;
        return TypeConverter.convertToTypeWithCheck(obj, runtime.getHash(), "to_hash");
    }

    // rb_check_string_type
    public static IRubyObject checkStringType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyString) return obj;
        return TypeConverter.convertToTypeWithCheck(obj, runtime.getString(), "to_str");
    }

    // rb_check_array_type
    public static IRubyObject checkArrayType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyArray) return obj;
        return TypeConverter.convertToTypeWithCheck(obj, runtime.getArray(), "to_ary");
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
    public static IRubyObject checkArrayType(IRubyObject self) {
        return TypeConverter.convertToTypeWithCheck19(self, self.getRuntime().getArray(), "to_ary");
    }

    public static IRubyObject handleUncoercibleObject(boolean raise, IRubyObject obj, RubyClass target) throws RaiseException {
        if (raise) throw obj.getRuntime().newTypeError("can't convert " + typeAsString(obj) + " into " + target);

        return obj.getRuntime().getNil();
    }

    public static IRubyObject handleImplicitlyUncoercibleObject(boolean raise, IRubyObject obj, RubyClass target) throws RaiseException {
        if (raise) throw obj.getRuntime().newTypeError("no implicit conversion of " + typeAsString(obj) + " into " + target);

        return obj.getRuntime().getNil();
    }

    // rb_check_type and Check_Type
    public static void checkType(ThreadContext context, IRubyObject x, RubyModule t) {
        ClassIndex xt;

        if (x == RubyBasicObject.UNDEF) {
            throw context.runtime.newRuntimeError("bug: undef leaked to the Ruby space");
        }

        xt = x.getMetaClass().getNativeClassIndex();

        // MISSING: special error for T_DATA of a certain type
        if (xt != t.getClassIndex()) {
            String tname = t.getBaseName();
            if (tname != null) {
                throw context.runtime.newTypeError("wrong argument type " + tname + " (expected " + t.getName() + ")");
            }
            throw context.runtime.newRuntimeError("bug: unknown type " + t.getClassIndex() + " (" + xt + " given)");
        }
    }

    // rb_convert_to_integer
    public static IRubyObject convertToInteger(ThreadContext context, IRubyObject val, int base) {
        Ruby runtime = context.runtime;
        IRubyObject tmp;

        for (;;) {
            if (val instanceof RubyFloat) {
                if (base != 0) raiseIntegerBaseError(context);
                double value = ((RubyFloat)val).getValue();
                if (value <= RubyFixnum.MAX ||
                        value >= RubyFixnum.MIN) {
                    return RubyNumeric.dbl2num(context.runtime, value);
                }
            } else if (val instanceof RubyFixnum || val instanceof RubyBignum) {
                if (base != 0) raiseIntegerBaseError(context);
                return val;
            } else if (val instanceof RubyString) {
                return RubyNumeric.str2inum(context.runtime, (RubyString)val, base, true);
            } else if (val instanceof RubyNil) {
                if (base != 0) raiseIntegerBaseError(context);
                throw context.runtime.newTypeError("can't convert nil into Integer");
            }

            if (base != 0) {
                tmp = TypeConverter.checkStringType(context.runtime, val);
                if (!tmp.isNil()) {
                    continue;
                }
                raiseIntegerBaseError(context);
            }

            break;
        }

        tmp = TypeConverter.convertToType19(val, runtime.getString(), "to_int", false);
        if (tmp.isNil()) {
            return val.convertToInteger("to_i");
        }
        return tmp;
    }

    // MRI: rb_Array
    public static RubyArray rb_Array(ThreadContext context, IRubyObject val) {
        IRubyObject tmp = checkArrayType(val);

        if (tmp.isNil()) {
            tmp = convertToTypeWithCheck19(val, context.runtime.getArray(), "to_a");
            if (tmp.isNil()) {
                return context.runtime.newArray(val);
            }
        }
        return (RubyArray)tmp;
    }

    // MRI: to_ary
    public static RubyArray to_ary(ThreadContext context, IRubyObject ary) {
        return (RubyArray)convertToType19(ary, context.runtime.getArray(), "to_ary");
    }

    private static void raiseIntegerBaseError(ThreadContext context) {
        throw context.runtime.newArgumentError("base specified for non string value");
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
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    @Deprecated
    public static IRubyObject convertToTypeWithCheck(IRubyObject obj, RubyClass target, int convertMethodIndex, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = TypeConverter.convertToType(obj, target, convertMethod, false);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }
}
