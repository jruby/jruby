/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="Java::JavaProxyClass")
public class JavaProxyReflectionObject extends RubyObject {

    public JavaProxyReflectionObject(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass, false);
    }

    protected static void registerRubyMethods(Ruby runtime, RubyClass klass) {
        klass.defineAnnotatedMethods(JavaProxyReflectionObject.class);
        klass.getMetaClass().defineAlias("__j_allocate", "allocate");
    }

    @Deprecated
    public IRubyObject op_equal(IRubyObject other) {
        return op_eqq(getRuntime().getCurrentContext(), other);
    }

    @Override
    @JRubyMethod(name = {"==", "eql?"})
    public RubyBoolean op_eqq(final ThreadContext context, IRubyObject obj) {
        if ( ! ( obj instanceof JavaProxyReflectionObject ) ) {
            final Object wrappedObj = obj.dataGetStruct();
            if ( ! ( wrappedObj instanceof JavaObject ) ) {
                return context.runtime.getFalse();
            }
            obj = (IRubyObject) wrappedObj;
        }
        return context.runtime.newBoolean( this.equals(obj) );
    }

    @Deprecated
    public IRubyObject same(IRubyObject other) {
        return op_equal(getRuntime().getCurrentContext(), other);
    }

    @Override
    @JRubyMethod(name = "equal?")
    public RubyBoolean op_equal(final ThreadContext context, IRubyObject obj) {
        if ( this == obj ) return context.runtime.getTrue();

        if ( ! ( obj instanceof JavaProxyReflectionObject ) ) {
            final Object wrappedObj = obj.dataGetStruct();
            if ( ! ( wrappedObj instanceof JavaObject ) ) {
                return context.runtime.getFalse();
            }
            obj = (IRubyObject) wrappedObj;
        }
        return context.runtime.newBoolean(this == obj);
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    @JRubyMethod
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @Override
    public int hashCode() {
        return 11 * getJavaClass().hashCode();
    }

    @Override
    @JRubyMethod
    public IRubyObject to_s() {
        return getRuntime().newString(toString());
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    @JRubyMethod
    public RubyString java_type() {
        return getRuntime().newString(getJavaClass().getName());
    }

    @JRubyMethod
    public JavaClass java_class() {
        return JavaClass.get(getRuntime(), getJavaClass());
    }

    @JRubyMethod
    public RubyFixnum length() {
        throw getRuntime().newTypeError("not a java array");
    }

    @JRubyMethod(name = "[]")
    public IRubyObject aref(IRubyObject index) {
        throw getRuntime().newTypeError("not a java array");
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject aset(IRubyObject index, IRubyObject someValue) {
        throw getRuntime().newTypeError("not a java array");
    }

    @JRubyMethod(name = "java_proxy?")
    public IRubyObject is_java_proxy() {
        return getRuntime().getFalse();
    }

    //
    // utility methods
    //

    @Deprecated
    protected RubyArray buildRubyArray(IRubyObject[] elements) {
        RubyArray result = getRuntime().newArray(elements.length);
        for (int i = 0; i < elements.length; i++) {
            result.append(elements[i]);
        }
        return result;
    }

    @Deprecated
    protected RubyArray buildRubyArray(final Class<?>[] classes) {
        return JavaClass.toRubyArray(getRuntime(), classes);
    }

    final RubyArray toRubyArray(final IRubyObject[] elements) {
        return RubyArray.newArrayNoCopy(getRuntime(), elements);
    }

    final RubyArray toRubyArray(final Class<?>[] classes) {
        return JavaClass.toRubyArray(getRuntime(), classes);
    }

}
