/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.ast.util;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

/**
 *
 * @author  jpetersen
 */
public final class ArgsUtil {
    public static IRubyObject[] convertToJavaArray(IRubyObject value) {
        if (value == null) {
        	return IRubyObject.NULL_ARRAY;
        }
        
        if (value instanceof RubyArray) {
            return ((RubyArray)value).toJavaArrayMaybeUnsafe();
        }
        
        return new IRubyObject[] { value };
    }

    /**
     * This name may be a bit misleading, since this also attempts to coerce
     * array behavior using to_ary.
     * 
     * @param runtime The JRuby runtime
     * @param value The value to convert
     * @param coerce Whether to coerce using to_ary or just wrap with an array
     */
    public static RubyArray convertToRubyArray(Ruby runtime, IRubyObject value, boolean coerce) {
        if (value == null) {
            return RubyArray.newEmptyArray(runtime);
        }
        
        if (coerce) return convertToRubyArrayWithCoerce(runtime, value);

        // don't attempt to coerce to array, just wrap and return
        return RubyArray.newArrayLight(runtime, value);
    }
    
    public static RubyArray convertToRubyArrayWithCoerce(Ruby runtime, IRubyObject value) {
        if (value instanceof RubyArray) return ((RubyArray)value);
        
        IRubyObject newValue = TypeConverter.convertToType(value, runtime.getArray(), "to_ary", false);

        if (newValue.isNil()) {
            return RubyArray.newArrayLight(runtime, value);
        }
        
        // must be array by now, or error
        if (!(newValue instanceof RubyArray)) {
            throw runtime.newTypeError(newValue.getMetaClass() + "#" + "to_ary" + " should return Array");
        }
        
        return (RubyArray)newValue;
    }
    
    public static RubyArray convertToRubyArray19(Ruby runtime, IRubyObject value, boolean coerce) {
        if (value == null) {
            return RubyArray.newEmptyArray(runtime);
        }
        
        if (coerce) return convertToRubyArrayWithCoerce19(runtime, value);

        // don't attempt to coerce to array, just wrap and return
        return RubyArray.newArrayLight(runtime, value);
    }
    
    public static RubyArray convertToRubyArrayWithCoerce19(Ruby runtime, IRubyObject value) {
        if (value instanceof RubyArray) return ((RubyArray)value);
        
        IRubyObject newValue = TypeConverter.convertToType19(value, runtime.getArray(), "to_ary", false);

        if (newValue.isNil()) {
            return RubyArray.newArrayLight(runtime, value);
        }
        
        // must be array by now, or error
        if (!(newValue instanceof RubyArray)) {
            throw runtime.newTypeError(newValue.getMetaClass() + "#" + "to_ary" + " should return Array");
        }
        
        return (RubyArray)newValue;
    }    
    
    /**
     * Remove first element from array
     * 
     * @param array to have first element "popped" off
     * @return all but first element of the supplied array
     */
    public static IRubyObject[] popArray(IRubyObject[] array) {
    	if (array == null || array.length == 0) {
    		return IRubyObject.NULL_ARRAY;
    	}
    	
    	IRubyObject[] newArray = new IRubyObject[array.length - 1];
    	System.arraycopy(array, 1, newArray, 0, array.length - 1);
    	
    	return newArray;
    }
    
    public static int arrayLength(IRubyObject node) {
        return node instanceof RubyArray ? ((RubyArray)node).getLength() : 0;
    }
    
    public static IRubyObject getOptionsArg(Ruby runtime, IRubyObject... args) {
        if (args.length >= 1) {
            return TypeConverter.checkHashType(runtime, args[args.length - 1]);
            
        }
        return runtime.getNil();
    }
}
