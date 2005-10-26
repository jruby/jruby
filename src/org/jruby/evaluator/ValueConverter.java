/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.evaluator;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public final class ValueConverter {
    private IRuby runtime;

    public ValueConverter(IRuby runtime) {
        this.runtime = runtime;
    }

    public RubyArray singleToArray(IRubyObject value) {
        if (value == null || value.isNil()) {
            return runtime.newArray(0);
        } else if (value instanceof RubyArray) {
            if (((RubyArray)value).getLength() == 1) {
                return (RubyArray) value;
            }
			return runtime.newArray(value);
        } else {
            return toArray(value);
        }
    }
    
    public IRubyObject arrayToSingle(IRubyObject value, boolean useUndefined) {
        if (!(value instanceof RubyArray)) {
            value = toArray(value);
        }
        switch (((RubyArray)value).getLength()) {
            case 0:
                return useUndefined ? null : runtime.getNil();
            case 1:
                return ((RubyArray)value).entry(0);
            default:
                return value;
        }
    }

    public RubyArray singleToMultiple(IRubyObject value) {
        if (value == null || value.isNil()) {
            return runtime.newArray(0);
        } else if (value instanceof RubyArray) {
            return (RubyArray) value;
        } else {
            return toArray(value);
        }
    }

    public IRubyObject multipleToSingle(IRubyObject value) {
        if (!(value instanceof RubyArray)) {
            value = toArray(value);
        }
        switch (((RubyArray)value).getLength()) {
            case 0:
                return runtime.getNil();
            case 1:
                if (!(((RubyArray)value).entry(0) instanceof RubyArray)) {
                    return ((RubyArray)value).entry(0);
                }
            default:
                return value;
        }
    }

    private RubyArray toArray(IRubyObject value) {
        if (value.isNil()) {
            return runtime.newArray(0);
        } else if (value instanceof RubyArray) {
            return (RubyArray)value;
        }
        if (value.respondsTo("to_ary")) {
            return (RubyArray)value.convertType(RubyArray.class, "Array", "to_ary");
        }
        return runtime.newArray(value);
    }
}
