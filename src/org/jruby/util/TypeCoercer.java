/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public interface TypeCoercer {
    /**
     * Methods which perform to_xxx if the object has such a method
     * @return
     */
    RubyArray convertToArray(IRubyObject src);
    /**
     *
     * @return
     */
    RubyHash convertToHash(IRubyObject src);    
    /**
    *
    * @return
    */    
    RubyFloat convertToFloat(IRubyObject src);
    /**
     *
     * @return
     */
    RubyInteger convertToInteger(IRubyObject src);
    /**
     *
     * @return
     */
    RubyInteger convertToInteger(IRubyObject src, int convertMethodIndex, String convertMethod);
    /**
     *
     * @return
     */
    RubyString convertToString(IRubyObject src);
    
    /**
     * Converts this object to type 'targetType' using 'convertMethod' method (MRI: convert_type).
     *
     * @param targetType is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @param raiseOnError will throw an Error if conversion does not work
     * @return the converted value
     */
    IRubyObject convertToType(IRubyObject src, RubyClass targetType, int convertMethodIndex, String convertMethod, boolean raiseOnError);

    /**
     * Converts this object to type 'targetType' using 'convertMethod' method and raises TypeError exception on failure (MRI: rb_convert_type).
     *
     * @param targetType is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */    
    IRubyObject convertToType(IRubyObject src, RubyClass targetType, int convertMethodIndex, String convertMethod);    

    /**
     * Higher level conversion utility similar to convertToType but it can throw an
     * additional TypeError during conversion (MRI: rb_check_convert_type).
     *
     * @param targetType is the type we are trying to convert to
     * @param convertMethod is the method to be called to try and convert to targeType
     * @return the converted value
     */
    IRubyObject convertToTypeWithCheck(IRubyObject src, RubyClass targetType, int convertMethodIndex, String convertMethod);
    
    /**
     *
     * @return
     */
    IRubyObject anyToString(IRubyObject src);
    
    /** rb_obj_as_string
     * @return
     */
    RubyString asString(IRubyObject src);
    
    /**
     *
     * @return
     */
    IRubyObject checkStringType(IRubyObject src);
    
    /**
     *
     * @return
     */
    IRubyObject checkArrayType(IRubyObject src);
}
