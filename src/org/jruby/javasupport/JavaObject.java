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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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
package org.jruby.javasupport;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
public class JavaObject extends RubyObject {
    private static Object NULL_LOCK = new Object();
    private final Object value;

    protected JavaObject(IRuby runtime, RubyClass rubyClass, Object value) {
        super(runtime, rubyClass);
        this.value = value;
    }

    protected JavaObject(IRuby runtime, Object value) {
        this(runtime, runtime.getModule("Java").getClass("JavaObject"), value);
    }

    public static JavaObject wrap(IRuby runtime, Object value) {
        Object lock = value == null ? NULL_LOCK : value;
        
        synchronized (lock) {
            JavaObject wrapper = runtime.getJavaSupport().getJavaObjectFromCache(value);
            if (wrapper == null) {
            	if (value == null) {
            		wrapper = new JavaObject(runtime, value);
            	} else if (value.getClass().isArray()) {
                	wrapper = new JavaArray(runtime, value);
                } else if (value.getClass().equals(Class.class)) {
                	wrapper = JavaClass.get(runtime, (Class)value);
                } else {
                	wrapper = new JavaObject(runtime, value);
                }
                runtime.getJavaSupport().putJavaObjectIntoCache(wrapper);
            }
            return wrapper;
        }
    }

    public Class getJavaClass() {
        return value != null ? value.getClass() : Void.TYPE;
    }

    public Object getValue() {
        return value;
    }

    public static RubyClass createJavaObjectClass(IRuby runtime, RubyModule javaModule) {
    	RubyClass result = javaModule.defineClassUnder("JavaObject", runtime.getObject());

    	registerRubyMethods(runtime, result);

        result.getMetaClass().undefineMethod("new");

        return result;
    }

	protected static void registerRubyMethods(IRuby runtime, RubyClass result) {
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

    public IRubyObject to_s() {
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

    public IRubyObject aset(IRubyObject index, IRubyObject someValue) {
        throw getRuntime().newTypeError("not a java array");
    }
    
    public IRubyObject is_java_proxy() {
        return getRuntime().getTrue();
    }
}
