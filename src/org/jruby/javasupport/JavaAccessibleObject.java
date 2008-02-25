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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class JavaAccessibleObject extends RubyObject {

	protected JavaAccessibleObject(Ruby runtime, RubyClass rubyClass) {
		super(runtime, rubyClass);
	}

	public static void registerRubyMethods(Ruby runtime, RubyClass result) {
        CallbackFactory callbackFactory = runtime.callbackFactory(JavaAccessibleObject.class);

        result.defineFastMethod("==", callbackFactory.getFastMethod("op_equal", IRubyObject.class));
        result.defineFastMethod("eql?", callbackFactory.getFastMethod("op_equal", IRubyObject.class));
        result.defineFastMethod("equal?", callbackFactory.getFastMethod("same", IRubyObject.class));
        result.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));

        result.defineFastMethod("accessible?", callbackFactory.getFastMethod("isAccessible"));
        result.defineFastMethod("accessible=", callbackFactory.getFastMethod("setAccessible", IRubyObject.class));
        result.defineFastMethod("annotations", callbackFactory.getFastMethod("annotations"));
        result.defineFastMethod("annotations?", callbackFactory.getFastMethod("annotations_p"));
        result.defineFastMethod("declared_annotations?", callbackFactory.getFastMethod("declared_annotations_p"));
        result.defineFastMethod("annotation", callbackFactory.getFastMethod("annotation", IRubyObject.class));
        result.defineFastMethod("annotation_present?", callbackFactory.getFastMethod("annotation_present_p", IRubyObject.class));
        result.defineFastMethod("declaring_class", callbackFactory.getFastMethod("declaring_class"));
        result.defineFastMethod("modifiers", callbackFactory.getFastMethod("modifiers"));
        result.defineFastMethod("name", callbackFactory.getFastMethod("name"));
        result.defineFastMethod("synthetic?", callbackFactory.getFastMethod("synthetic_p"));
        result.defineFastMethod("to_string", callbackFactory.getFastMethod("to_string"));
        result.defineFastMethod("to_s", callbackFactory.getFastMethod("to_string"));
	}
	protected abstract AccessibleObject accessibleObject();

    public boolean equals(Object other) {
        return other instanceof JavaAccessibleObject &&
            this.accessibleObject() == ((JavaAccessibleObject)other).accessibleObject();
    }
    
    public int hashCode() {
        return this.accessibleObject().hashCode();
    }

	public RubyFixnum hash() {
		return getRuntime().newFixnum(hashCode());
    }

    public IRubyObject op_equal(IRubyObject other) {
		return other instanceof JavaAccessibleObject && accessibleObject().equals(((JavaAccessibleObject)other).accessibleObject()) ? getRuntime().getTrue() : getRuntime().getFalse();
    }
   
	public IRubyObject same(IRubyObject other) {
        return getRuntime().newBoolean(equals(other));
	}
       
	public RubyBoolean isAccessible() {
		return new RubyBoolean(getRuntime(),accessibleObject().isAccessible());
	}

	public IRubyObject setAccessible(IRubyObject object) {
	    accessibleObject().setAccessible(object.isTrue());
		return object;
	}
	
    @SuppressWarnings("unchecked")
    public IRubyObject annotation(IRubyObject annoClass) {
        if (!(annoClass instanceof JavaClass)) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        return Java.getInstance(getRuntime(), accessibleObject().getAnnotation(((JavaClass)annoClass).javaClass()));
    }

    public IRubyObject annotations() {
        return Java.getInstance(getRuntime(), accessibleObject().getAnnotations());
    }
    
    public RubyBoolean annotations_p() {
        return getRuntime().newBoolean(accessibleObject().getAnnotations().length > 0);
    }
    
    public IRubyObject declared_annotations() {
        return Java.getInstance(getRuntime(), accessibleObject().getDeclaredAnnotations());
    }
    
    public RubyBoolean declared_annotations_p() {
        return getRuntime().newBoolean(accessibleObject().getDeclaredAnnotations().length > 0);
    }
    
    public IRubyObject annotation_present_p(IRubyObject annoClass) {
        if (!(annoClass instanceof JavaClass)) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        return getRuntime().newBoolean(this.accessibleObject().isAnnotationPresent(((JavaClass)annoClass).javaClass()));
    }

    // for our purposes, Accessibles are also Members, and vice-versa,
    // so we'll include Member methods here.

    public IRubyObject declaring_class() {
        Class<?> clazz = ((Member)accessibleObject()).getDeclaringClass();
        if (clazz != null) {
            return JavaClass.get(getRuntime(), clazz);
        }
        return getRuntime().getNil();
    }

    public IRubyObject modifiers() {
        return getRuntime().newFixnum(((Member)accessibleObject()).getModifiers());
    }

    public IRubyObject name() {
        return getRuntime().newString(((Member)accessibleObject()).getName());
    }

    public IRubyObject synthetic_p() {
        return getRuntime().newBoolean(((Member)accessibleObject()).isSynthetic());
    }
    
    public RubyString to_string() {
        return getRuntime().newString(accessibleObject().toString());
    }

}
