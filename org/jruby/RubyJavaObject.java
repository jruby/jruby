/*
 * RubyJavaObject.java - No description
 * Created on 21. September 2001, 14:43
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyJavaObject extends RubyObject {
    private Object value;

    public RubyJavaObject(Ruby ruby, RubyClass rubyClass) {
        this(ruby, rubyClass, null);
    }

    public RubyJavaObject(Ruby ruby, RubyClass rubyClass, Object value) {
        super(ruby, rubyClass);
        this.value = value;
    }

    public Class getJavaClass() {
        return value.getClass();
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public Object getValue() {
        return value;
    }

    /** Setter for property value.
     * @param value New value of property value.
     */
    public void setValue(Object value) {
        this.value = value;
    }
    
    public static RubyClass createJavaObjectClass(Ruby ruby) {
        RubyClass javaObjectClass = ruby.defineClass("JavaObject", ruby.getClasses().getObjectClass());

        javaObjectClass.defineMethod("to_s", CallbackFactory.getMethod(RubyJavaObject.class, "to_s"));
        javaObjectClass.defineMethod("eql?", CallbackFactory.getMethod(RubyJavaObject.class, "equal"));
        javaObjectClass.defineMethod("==", CallbackFactory.getMethod(RubyJavaObject.class, "equal"));
		javaObjectClass.defineMethod("hash", CallbackFactory.getMethod(RubyJavaObject.class, "hash"));

        javaObjectClass.getInternalClass().undefMethod("new");

        return javaObjectClass;
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(ruby, value.hashCode());
    }

    // JavaObject methods
    public RubyString to_s() {
        return RubyString.newString(getRuntime(), getValue() != null ? getValue().toString() : "null");
    }

    public RubyBoolean equal(IRubyObject other) {
        if (other instanceof RubyJavaObject) {
            return (getValue() != null && getValue().equals(((RubyJavaObject) other).getValue()))
                ? getRuntime().getTrue()
                : getRuntime().getFalse();
        }
        return getRuntime().getFalse();
    }
}
