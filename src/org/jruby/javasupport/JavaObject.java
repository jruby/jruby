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
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JavaObject extends RubyObject {
    private final Object value;

    protected JavaObject(Ruby runtime, RubyClass rubyClass, Object value) {
        super(runtime, rubyClass);
        this.value = value;
    }

    protected JavaObject(Ruby runtime, Object value) {
        this(runtime, runtime.getClasses().getJavaObjectClass(), value);
    }

    public static synchronized JavaObject wrap(Ruby runtime, Object value) {
        JavaObject wrapper = runtime.getJavaSupport().getJavaObjectFromCache(value);
        if (wrapper == null) {
            if (value != null && value.getClass().isArray()) {
                wrapper = new JavaArray(runtime, value);
            } else if (value != null && value.getClass().equals(Class.class)) {
                wrapper = JavaClass.get(runtime, (Class)value);
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

    public static RubyClass createJavaObjectClass(Ruby runtime) {
        RubyClass result = 
            runtime.defineClass("JavaObject", runtime.getClasses().getObjectClass());
        registerRubyMethods(runtime, result);

        result.getMetaClass().undefineMethod("new");

        return result;
    }

	protected static void registerRubyMethods(Ruby runtime, RubyClass result) {
		CallbackFactory callbackFactory = runtime.callbackFactory(JavaObject.class);

        result.defineMethod("to_s", 
            callbackFactory.getMethod("to_s"));
        result.defineMethod("==", 
            callbackFactory.getMethod("equal", IRubyObject.class));
        result.defineMethod("eql?", 
            callbackFactory.getMethod("equal", IRubyObject.class));
        result.defineMethod("equal?", 
            callbackFactory.getMethod("same", IRubyObject.class));
        result.defineMethod("hash", 
            callbackFactory.getMethod("hash"));
        result.defineMethod("java_type", 
            callbackFactory.getMethod("java_type"));
        result.defineMethod("java_class", 
            callbackFactory.getMethod("java_class"));
        result.defineMethod("java_proxy?", 
            callbackFactory.getMethod("is_java_proxy"));
        result.defineMethod("length", 
            callbackFactory.getMethod("length"));
        result.defineMethod("[]", 
            callbackFactory.getMethod("aref", IRubyObject.class));
        result.defineMethod("[]=", 
            callbackFactory.getMethod("aset", IRubyObject.class, IRubyObject.class));
	}

	public RubyFixnum hash() {
        return getRuntime().newFixnum(value == null ? 0 : value.hashCode());
    }

    public RubyString to_s() {
        return getRuntime().newString(
           value == null ? "null" : value.toString());
    }

    public IRubyObject equal(IRubyObject other) {
        if (!(other instanceof JavaObject)) {
            other = other.getInstanceVariable("@java_object");
            if (!(other instanceof JavaObject)) {
                return getRuntime().getFalse();
            }
        }
    	
        if (getValue() == null && ((JavaObject) other).getValue() == null) {
            return getRuntime().getTrue();
        }
    	
        boolean isEqual = getValue().equals(((JavaObject) other).getValue());
        return isEqual ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    public IRubyObject same(IRubyObject other) {
        if (!(other instanceof JavaObject)) {
            other = other.getInstanceVariable("@java_object");
            if (!(other instanceof JavaObject)) {
              return getRuntime().getFalse();
            }
        }
      
        if (getValue() == null && ((JavaObject) other).getValue() == null) {
            return getRuntime().getTrue();
        }
      
        boolean isSame = getValue() == ((JavaObject) other).getValue();
        return isSame ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyString java_type() {
        return getRuntime().newString(getJavaClass().getName());
    }

    public IRubyObject java_class() {
        return JavaClass.get(getRuntime(), getJavaClass());
    }

    public RubyFixnum length() {
        throw getRuntime().newTypeError("not a java array");
    }

    public IRubyObject aref(IRubyObject index) {
        throw getRuntime().newTypeError("not a java array");
    }

    public IRubyObject aset(IRubyObject index, IRubyObject value) {
        throw getRuntime().newTypeError("not a java array");
    }
    
    public IRubyObject is_java_proxy() {
        return RubyBoolean.createTrueClass(getRuntime());
    }
}
