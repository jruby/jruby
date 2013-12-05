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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;

public class TypeConverter {
    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type).
     *
     * @param obj the object to convert
     * @param targetType is the type we are trying to convert to
     * @param convertMethodIndex the fast index to use for calling the method
     * @param convertMethod is the method to be called to try and convert to targeType
     * @param raiseOnError will throw an Error if conversion does not work
     * @return the converted value
     */
    @Deprecated
    public static final IRubyObject convertToType(IRubyObject obj, RubyClass target, int convertMethodIndex, String convertMethod, boolean raise) {
        if (!obj.respondsTo(convertMethod)) return handleUncoercibleObject(raise, obj, target);
        
        return obj.callMethod(obj.getRuntime().getCurrentContext(), convertMethod);
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type).
     *
     * @param obj the object to convert
     * @param targetType is the type we are trying to convert to
     * @param convertMethodIndex the fast index to use for calling the method
     * @param convertMethod is the method to be called to try and convert to targeType
     * @param raiseOnError will throw an Error if conversion does not work
     * @return the converted value
     */
    public static final IRubyObject convertToType(IRubyObject obj, RubyClass target, String convertMethod, boolean raise) {
        if (!obj.respondsTo(convertMethod)) return handleUncoercibleObject(raise, obj, target);
        
        return obj.callMethod(obj.getRuntime().getCurrentContext(), convertMethod);
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type 1.9).
     *
     * @param obj the object to convert
     * @param targetType is the type we are trying to convert to
     * @param convertMethodIndex the fast index to use for calling the method
     * @param convertMethod is the method to be called to try and convert to targeType
     * @param raiseOnError will throw an Error if conversion does not work
     * @return the converted value
     */
    public static final IRubyObject convertToType19(IRubyObject obj, RubyClass target, String convertMethod, boolean raise) {
        IRubyObject r = obj.checkCallMethod(obj.getRuntime().getCurrentContext(), convertMethod);
        
        return r == null ? handleUncoercibleObject(raise, obj, target) : r;
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method and raises TypeError exception on failure (MRI: rb_convert_type).
     *
     * @param obj the object to convert
     * @param targetType is the type we are trying to convert to
     * @param convertMethodIndex the fast index to use for calling the method
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    @Deprecated
    public static final IRubyObject convertToType(IRubyObject obj, RubyClass target, int convertMethodIndex, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType(obj, target, convertMethod, true);
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method and raises TypeError exception on failure (MRI: rb_convert_type).
     *
     * @param obj the object to convert
     * @param targetType is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static final IRubyObject convertToType(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = convertToType(obj, target, convertMethod, true);
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method and raises TypeError exception on failure (MRI: rb_convert_type in 1.9).
     *
     * @param obj the object to convert
     * @param targetType is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static final IRubyObject convertToType19(IRubyObject obj, RubyClass target, String convertMethod) {
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
    public static final IRubyObject checkData(IRubyObject obj) {
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
     * @param targetType is the type we are trying to convert to
     * @param convertMethodIndex the fast index to use for calling the method
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    @Deprecated
    public static final IRubyObject convertToTypeWithCheck(IRubyObject obj, RubyClass target, int convertMethodIndex, String convertMethod) {  
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
     * @param targetType is the type we are trying to convert to
     * @param convertMethodIndex the fast index to use for calling the method
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static final IRubyObject convertToTypeWithCheck(IRubyObject obj, RubyClass target, String convertMethod) {
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
     * @param targetType is the type we are trying to convert to
     * @param convertMethodIndex the fast index to use for calling the method
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static final IRubyObject convertToTypeWithCheck19(IRubyObject obj, RubyClass target, String convertMethod) {
        if (target.isInstance(obj)) return obj;
        IRubyObject val = TypeConverter.convertToType19(obj, target, convertMethod, false);
        if (val.isNil()) return val;
        if (!target.isInstance(val)) throw obj.getRuntime().newTypeError(obj.getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    /**
     * Higher level conversion utility similar to convertToType but it can throw an
     * additional TypeError during conversion (MRI: rb_check_convert_type).
     *
     * @param obj the object to convert
     * @param targetType is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    public static final IRubyObject convertToTypeOrRaise(IRubyObject obj, RubyClass target, String convertMethod) {  
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

    // 1.9 rb_check_to_float
    public static IRubyObject checkFloatType(Ruby runtime, IRubyObject obj) {
        if (obj instanceof RubyFloat) return obj;
        if (!(obj instanceof RubyNumeric)) return runtime.getNil();

        return TypeConverter.convertToTypeWithCheck(obj, runtime.getFloat(), "to_f");
    }

    // 1.9 rb_check_hash_type
    public static IRubyObject checkHashType(Ruby runtime, IRubyObject obj) {
        return TypeConverter.convertToTypeWithCheck(obj, runtime.getHash(), "to_hash");
    }

    public static IRubyObject handleUncoercibleObject(boolean raise, IRubyObject obj, RubyClass target) throws RaiseException {
        if (raise) throw obj.getRuntime().newTypeError("can't convert " + typeAsString(obj) + " into " + target);

        return obj.getRuntime().getNil();
    }
}
