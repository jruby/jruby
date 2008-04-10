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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="Java::JavaField")
public class JavaField extends JavaAccessibleObject {
    private Field field;

    public static RubyClass createJavaFieldClass(Ruby runtime, RubyModule javaModule) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result = javaModule.defineClassUnder("JavaField", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(JavaField.class);

        JavaAccessibleObject.registerRubyMethods(runtime, result);

        result.defineFastMethod("value_type", callbackFactory.getFastMethod("value_type"));
        result.defineFastMethod("public?", callbackFactory.getFastMethod("public_p"));
        result.defineFastMethod("static?", callbackFactory.getFastMethod("static_p"));
        result.defineFastMethod("value", callbackFactory.getFastMethod("value", IRubyObject.class));
        result.defineFastMethod("set_value", callbackFactory.getFastMethod("set_value", IRubyObject.class, IRubyObject.class));
        result.defineFastMethod("set_static_value", callbackFactory.getFastMethod("set_static_value", IRubyObject.class));
        result.defineFastMethod("final?", callbackFactory.getFastMethod("final_p"));
        result.defineFastMethod("static_value", callbackFactory.getFastMethod("static_value"));
        result.defineFastMethod("name", callbackFactory.getFastMethod("name"));
        result.defineFastMethod("==", callbackFactory.getFastMethod("op_equal", IRubyObject.class));
        result.defineAlias("===", "==");
        result.defineFastMethod("enum_constant?", callbackFactory.getFastMethod("enum_constant_p"));
        result.defineFastMethod("to_generic_string", callbackFactory.getFastMethod("to_generic_string"));
        result.defineFastMethod("type", callbackFactory.getFastMethod("field_type"));

        return result;
    }

    public JavaField(Ruby runtime, Field field) {
        super(runtime, runtime.getJavaSupport().getJavaFieldClass());
        this.field = field;
    }

    public boolean equals(Object other) {
        return other instanceof JavaField &&
            this.field == ((JavaField)other).field;
    }
    
    public int hashCode() {
        return field.hashCode();
    }

    @JRubyMethod
    public RubyString value_type() {
        return getRuntime().newString(field.getType().getName());
    }

    @JRubyMethod(name = {"==", "==="})
    public IRubyObject op_equal(IRubyObject other) {
    	if (!(other instanceof JavaField)) {
    		return getRuntime().getFalse();
    	}
    	
        return getRuntime().newBoolean(field.equals(((JavaField) other).field));
    }

    @JRubyMethod(name = "public?")
    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(field.getModifiers()));
    }

    @JRubyMethod(name = "static?")
    public RubyBoolean static_p() {
        return getRuntime().newBoolean(Modifier.isStatic(field.getModifiers()));
    }
    
    @JRubyMethod(name = "enum_constant?")
    public RubyBoolean enum_constant_p() {
        return getRuntime().newBoolean(field.isEnumConstant());
    }

    @JRubyMethod
    public RubyString to_generic_string() {
        return getRuntime().newString(field.toGenericString());
    }
    
    @JRubyMethod(name = "type")
    public IRubyObject field_type() {
        return JavaClass.get(getRuntime(), field.getType());
    }

    @JRubyMethod
    public JavaObject value(IRubyObject object) {
        if (! (object instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object");
        }
        Object javaObject = ((JavaObject) object).getValue();
        try {
            return JavaObject.wrap(getRuntime(), field.get(javaObject));
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError("illegal access");
        }
    }

    @JRubyMethod
    public JavaObject set_value(IRubyObject object, IRubyObject value) {
        if (! (object instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object: " + object);
        }
        if (! (value instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object:" + value);
        }
        Object javaObject = ((JavaObject) object).getValue();
        try {
            Object convertedValue = JavaUtil.convertArgument(value.getRuntime(), ((JavaObject) value).getValue(),
                                                             field.getType());

            field.set(javaObject, convertedValue);
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError(
                                "illegal access on setting variable: " + iae.getMessage());
        } catch (IllegalArgumentException iae) {
            throw getRuntime().newTypeError(
                                "wrong type for " + field.getType().getName() + ": " +
                                ((JavaObject) value).getValue().getClass().getName());
        }
        return (JavaObject) value;
    }

    @JRubyMethod(name = "final?")
    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(field.getModifiers()));
    }

    @JRubyMethod
    public JavaObject static_value() {
        try {
            // TODO: Only setAccessible to account for pattern found by
            // accessing constants included from a non-public interface.
            // (aka java.util.zip.ZipConstants being implemented by many
            // classes)
            if (!Ruby.isSecurityRestricted()) {
                field.setAccessible(true);
            }
            return JavaObject.wrap(getRuntime(), field.get(null));
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError("illegal static value access: " + iae.getMessage());
        }
    }

    @JRubyMethod
    public JavaObject set_static_value(IRubyObject value) {
        if (! (value instanceof JavaObject)) {
            throw getRuntime().newTypeError("not a java object:" + value);
        }
        try {
            Object convertedValue = JavaUtil.convertArgument(getRuntime(), ((JavaObject) value).getValue(),
                                                             field.getType());
            // TODO: Only setAccessible to account for pattern found by
            // accessing constants included from a non-public interface.
            // (aka java.util.zip.ZipConstants being implemented by many
            // classes)
            // TODO: not sure we need this at all, since we only expose
            // public fields.
            //field.setAccessible(true);
            field.set(null, convertedValue);
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError(
                                "illegal access on setting static variable: " + iae.getMessage());
        } catch (IllegalArgumentException iae) {
            throw getRuntime().newTypeError(
                                "wrong type for " + field.getType().getName() + ": " +
                                ((JavaObject) value).getValue().getClass().getName());
        }
        return (JavaObject) value;
    }
    
    @JRubyMethod
    public RubyString name() {
        return getRuntime().newString(field.getName());
    }
    
    protected AccessibleObject accessibleObject() {
        return field;
    }
}
