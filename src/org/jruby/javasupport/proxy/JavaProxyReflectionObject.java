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
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
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

package org.jruby.javasupport.proxy;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaProxyReflectionObject extends RubyObject {

    public JavaProxyReflectionObject(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass, false);
    }

    protected static void registerRubyMethods(Ruby runtime, RubyClass result) {
        CallbackFactory callbackFactory = runtime
                .callbackFactory(JavaProxyReflectionObject.class);

        result.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        result.defineFastMethod("==", callbackFactory.getFastMethod("equal",
                IRubyObject.class));
        result.defineFastMethod("eql?", callbackFactory.getFastMethod("equal",
                IRubyObject.class));
        result.defineFastMethod("equal?", callbackFactory.getFastMethod("same",
                IRubyObject.class));
        result.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));
        result
                .defineFastMethod("java_type", callbackFactory
                        .getFastMethod("java_type"));
        result.defineFastMethod("java_class", callbackFactory
                .getFastMethod("java_class"));
        result.defineFastMethod("java_proxy?", callbackFactory
                .getFastMethod("is_java_proxy"));
        result.defineFastMethod("length", callbackFactory.getFastMethod("length"));
        result.defineFastMethod("[]", callbackFactory.getFastMethod("aref",
                IRubyObject.class));
        result.defineFastMethod("[]=", callbackFactory.getFastMethod("aset",
                IRubyObject.class, IRubyObject.class));

        result.getMetaClass().defineAlias("__j_allocate","allocate");
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    public IRubyObject to_s() {
        return getRuntime().newString(toString());
    }

    public IRubyObject equal(IRubyObject other) {
        if (!(other instanceof JavaProxyReflectionObject)) {
            other = other.getInstanceVariable("@java_object");
            if (!(other instanceof JavaObject)) {
                return getRuntime().getFalse();
            }
        }

        boolean isEqual = equals(other);
        return isEqual ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    public int hashCode() {
        return getClass().hashCode();
    }

    public String toString() {
        return getClass().getName();
    }

    public boolean equals(Object other) {
        return this == other;
    }
    
    public IRubyObject same(IRubyObject other) {
        if (!(other instanceof JavaObject)) {
            other = other.getInstanceVariable("@java_object");
            if (!(other instanceof JavaObject)) {
                return getRuntime().getFalse();
            }
        }

        boolean isSame = this == other;
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
        return getRuntime().getFalse();
    }

    //
    // utility methods
    //

    protected RubyArray buildRubyArray(IRubyObject[] constructors) {
        RubyArray result = getRuntime().newArray(constructors.length);
        for (int i = 0; i < constructors.length; i++) {
            result.append(constructors[i]);
        }
        return result;
    }

    protected RubyArray buildRubyArray(Class[] classes) {
        RubyArray result = getRuntime().newArray(classes.length);
        for (int i = 0; i < classes.length; i++) {
            result.append(JavaClass.get(getRuntime(), classes[i]));
        }
        return result;
    }

}
