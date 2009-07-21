/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

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
        if (!obj.respondsTo(convertMethod)) {
            return handleUncoercibleObject(raise, obj, target);
        }
        
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
        if (!obj.respondsTo(convertMethod)) {
            return handleUncoercibleObject(raise, obj, target);
        }
        
        return obj.callMethod(obj.getRuntime().getCurrentContext(), convertMethod);
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
     * Checks that this object is of type DATA and then returns it, otherwise raises failure (MRI: Check_Type(obj, T_DATA))
     *
     * @param obj the object to check
     * @return the converted value
     */
    public static final IRubyObject checkData(IRubyObject obj) {
        if(obj instanceof org.jruby.runtime.marshal.DataType) {
            return obj;
        }
        String type;
        if (obj.isNil()) {
            type = "nil";
        } else if (obj instanceof RubyBoolean) {
            type = obj.isTrue() ? "true" : "false";
        } else {
            type = obj.getMetaClass().getRealClass().getName();
        }
        throw obj.getRuntime().newTypeError("wrong argument type " + type + " (expected Data)");
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

    private static IRubyObject handleUncoercibleObject(boolean raise, IRubyObject obj, RubyClass target) throws RaiseException {
        if (raise) {
            String type;
            if (obj.isNil()) {
                type = "nil";
            } else if (obj instanceof RubyBoolean) {
                type = obj.isTrue() ? "true" : "false";
            } else {
                type = obj.getMetaClass().getRealClass().getName();
            }
            throw obj.getRuntime().newTypeError("can't convert " + type + " into " + target);
        } else {
            return obj.getRuntime().getNil();
        }
    }
}
