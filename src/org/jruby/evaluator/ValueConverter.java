/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.evaluator;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class ValueConverter {
    private Ruby runtime;

    public ValueConverter(Ruby runtime) {
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