/*
 * JavaObject.java - No description
 * Created on 21. September 2001, 14:43
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
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

package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JavaObject extends RubyObject {
    private final Object value;

    protected JavaObject(Ruby ruby, RubyClass rubyClass, Object value) {
        super(ruby, rubyClass);
        this.value = value;
    }

    protected JavaObject(Ruby ruby, Object value) {
        this(ruby, ruby.getClasses().getJavaObjectClass(), value);
    }

    public static synchronized JavaObject wrap(Ruby runtime, Object value) {
        JavaObject wrapper = runtime.getJavaSupport().getJavaObjectFromCache(value);
        if (wrapper == null) {
            if (value != null && value.getClass().isArray()) {
                wrapper = new JavaArray(runtime, value);
            } else {
                wrapper = new JavaObject(runtime, value);
            }
            runtime.getJavaSupport().putJavaObjectIntoCache(wrapper);
        }
        return wrapper;
    }

    public Class getJavaClass() {
        return value != null ? value.getClass() : Void.TYPE;
    }

    public Object getValue() {
        return value;
    }

    public static RubyClass createJavaObjectClass(Ruby ruby) {
        RubyClass result = 
            ruby.defineClass("JavaObject", ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();

        result.defineMethod("to_s", 
            callbackFactory.getMethod(JavaObject.class, "to_s"));
        result.defineMethod("==", 
            callbackFactory.getMethod(JavaObject.class, "equal", IRubyObject.class));
        result.defineMethod("eql?", 
            callbackFactory.getMethod(JavaObject.class, "equal", IRubyObject.class));
        result.defineMethod("hash", 
            callbackFactory.getMethod(JavaObject.class, "hash"));
        result.defineMethod("java_type", 
            callbackFactory.getMethod(JavaObject.class, "java_type"));
        result.defineMethod("java_class", 
            callbackFactory.getMethod(JavaObject.class, "java_class"));
        result.defineMethod("length", 
            callbackFactory.getMethod(JavaObject.class, "length"));
        result.defineMethod("[]", 
            callbackFactory.getMethod(JavaObject.class, "aref", IRubyObject.class));
        result.defineMethod("[]=", 
            callbackFactory.getMethod(JavaObject.class, "aset", IRubyObject.class, IRubyObject.class));

        result.getMetaClass().undefineMethod("new");

        return result;
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(runtime, 
           value == null ? 0 : value.hashCode());
    }

    public RubyString to_s() {
        return RubyString.newString(runtime, 
           value == null ? "null" : value.toString());
    }

    public IRubyObject equal(IRubyObject other) {
        if (other instanceof JavaObject == false) {
	    return getRuntime().getFalse();
	}

	if (getValue() == null && ((JavaObject) other).getValue() == null) {
	    return getRuntime().getTrue();
	}

	return (getValue().equals(((JavaObject) other).getValue()))
	    ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyString java_type() {
        return RubyString.newString(getRuntime(), getJavaClass().getName());
    }

    public IRubyObject java_class() {
        return new JavaClass(getRuntime(), getJavaClass());
    }

    public RubyFixnum length() {
        throw new TypeError(getRuntime(), "not a java array");
    }

    public IRubyObject aref(IRubyObject index) {
        throw new TypeError(getRuntime(), "not a java array");
    }

    public IRubyObject aset(IRubyObject index, IRubyObject value) {
        throw new TypeError(getRuntime(), "not a java array");
    }
}
