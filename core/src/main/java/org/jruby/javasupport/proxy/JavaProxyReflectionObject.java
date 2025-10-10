/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.javasupport.proxy;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.typeError;

public class JavaProxyReflectionObject extends RubyObject {

    public JavaProxyReflectionObject(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass, false);
    }

    protected static void registerRubyMethods(ThreadContext context, RubyClass klass) {
        klass.defineMethods(context, JavaProxyReflectionObject.class);
        klass.getMetaClass().defineAlias(context, "__j_allocate", "allocate");
    }

    @Deprecated(since = "1.7.20")
    public IRubyObject op_equal(IRubyObject other) {
        return op_eqq(getCurrentContext(), other);
    }

    @Override
    @JRubyMethod(name = {"==", "eql?"})
    public RubyBoolean op_eqq(final ThreadContext context, IRubyObject obj) {
        if ( ! ( obj instanceof JavaProxyReflectionObject ) ) {
            final Object wrappedObj = obj.dataGetStruct();
            if ( ! ( wrappedObj instanceof RubyObject ) ) { // JavaObject || JavaProxy
                return context.fals;
            }
            obj = (IRubyObject) wrappedObj;
        }
        return asBoolean(context, this.equals(obj));
    }

    @Deprecated(since = "1.7.20")
    public IRubyObject same(IRubyObject other) {
        return op_equal(getCurrentContext(), other);
    }

    @Override
    @JRubyMethod(name = "equal?")
    public RubyBoolean op_equal(final ThreadContext context, IRubyObject obj) {
        if ( this == obj ) return context.tru;

        if ( ! ( obj instanceof JavaProxyReflectionObject ) ) {
            final Object wrappedObj = obj.dataGetStruct();
            if ( ! ( wrappedObj instanceof RubyObject ) ) { // JavaObject || JavaProxy
                return context.fals;
            }
            obj = (IRubyObject) wrappedObj;
        }
        return asBoolean(context, this == obj);
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, hashCode());
    }

    @Override
    public int hashCode() {
        return 11 * getJavaClass().hashCode();
    }

    @Override
    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        return newString(context, toString());
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @Deprecated(since = "10.0.0.0")
    public RubyString java_type() {
        return java_type(getCurrentContext());
    }

    @JRubyMethod
    public RubyString java_type(ThreadContext context) {
        return newString(context, getJavaClass().getName());
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject java_class() {
        return java_class(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject java_class(ThreadContext context) {
        return Java.getInstance(context.runtime, getJavaClass());
    }

    @Deprecated(since = "10.0.0.0")
    public RubyFixnum length() {
        return length(getCurrentContext());
    }

    @JRubyMethod
    public RubyFixnum length(ThreadContext context) {
        throw typeError(context, "not a java array");
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject aref(IRubyObject index) {
        return aref(getCurrentContext(), index);
    }

    @JRubyMethod(name = "[]")
    public IRubyObject aref(ThreadContext context, IRubyObject index) {
        throw typeError(context, "not a java array");
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject aset(IRubyObject index, IRubyObject someValue) {
        return aset(getCurrentContext(), index, someValue);
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject aset(ThreadContext context, IRubyObject index, IRubyObject someValue) {
        throw typeError(context, "not a java array");
    }

    @Deprecated(since = "10.0.0.0")
    public IRubyObject is_java_proxy() {
        return is_java_proxy(getCurrentContext());
    }

    @JRubyMethod(name = "java_proxy?")
    public IRubyObject is_java_proxy(ThreadContext context) {
        return context.fals;
    }

    //
    // utility methods
    //

    final RubyArray toRubyArray(ThreadContext context, final IRubyObject[] elements) {
        return RubyArray.newArrayMayCopy(context.runtime, elements);
    }

    static RubyArray toClassArray(ThreadContext context, final Class<?>[] classes) {
        IRubyObject[] javaClasses = new IRubyObject[classes.length];
        for ( int i = classes.length; --i >= 0; ) {
            javaClasses[i] = Java.getProxyClass(context, classes[i]);
        }
        return RubyArray.newArrayMayCopy(context.runtime, javaClasses);
    }

}
