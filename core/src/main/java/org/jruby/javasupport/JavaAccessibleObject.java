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
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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

package org.jruby.javasupport;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class JavaAccessibleObject {

    protected JavaAccessibleObject(Ruby runtime, RubyClass rubyClass) {
        this(runtime, rubyClass, false);
    }

    JavaAccessibleObject(Ruby runtime, RubyClass rubyClass, boolean objectSpace) {
        // super(runtime, rubyClass, objectSpace);
    }

    public static void registerRubyMethods(Ruby runtime, RubyClass result) {
        result.defineAnnotatedMethods(JavaAccessibleObject.class);
    }

    public abstract AccessibleObject accessibleObject();

    public boolean equals(final Object other) {
        if ( this == other ) return true;
        return other instanceof JavaAccessibleObject &&
            this.accessibleObject().equals( ((JavaAccessibleObject) other).accessibleObject() );
    }

    boolean same(final JavaAccessibleObject that) {
        if ( this == that ) return true;
        return this.accessibleObject() == that.accessibleObject();
    }

    public int hashCode() {
        return accessibleObject().hashCode();
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return context.runtime.newFixnum(hashCode());
    }

    @JRubyMethod(name = {"==", "eql?"})
    public RubyBoolean op_equal(ThreadContext context, final IRubyObject other) {
        return RubyBoolean.newBoolean(context.runtime, equals(other));
    }

    @JRubyMethod(name = "equal?")
    public RubyBoolean same(ThreadContext context, final IRubyObject other) {
        final boolean same = other instanceof JavaAccessibleObject && same((JavaAccessibleObject) other);
        return same ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    @JRubyMethod(name = "accessible?")
    @Deprecated
    public RubyBoolean isAccessible(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, accessibleObject().isAccessible());
    }

    @JRubyMethod(name = "accessible=")
    public IRubyObject setAccessible(IRubyObject object) {
        accessibleObject().setAccessible(object.isTrue());
        return object;
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod
    public IRubyObject annotation(ThreadContext context, final IRubyObject annoClass) {
        final Class annotation;
        if (annoClass instanceof RubyClass && ((RubyClass) annoClass).getJavaProxy()) {
            annotation = ((RubyClass) annoClass).getJavaClass();
        } else if (annoClass instanceof JavaClass) {
            annotation = ((JavaClass) annoClass).javaClass();
        } else {
            throw context.runtime.newTypeError("expected a Java (proxy) class, got: " + annoClass);
        }
        return Java.getInstance(context.runtime, accessibleObject().getAnnotation(annotation));
    }

    @JRubyMethod
    public IRubyObject annotations(ThreadContext context) {
        return Java.getInstance(context.runtime, accessibleObject().getAnnotations());
    }

    @JRubyMethod(name = "annotations?")
    public RubyBoolean annotations_p(ThreadContext context) {
        return context.runtime.newBoolean(accessibleObject().getAnnotations().length > 0);
    }

    @JRubyMethod
    public IRubyObject declared_annotations(ThreadContext context) {
        return Java.getInstance(context.runtime, accessibleObject().getDeclaredAnnotations());
    }

    @JRubyMethod(name = "declared_annotations?")
    public RubyBoolean declared_annotations_p(ThreadContext context) {
        return context.runtime.newBoolean(accessibleObject().getDeclaredAnnotations().length > 0);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "annotation_present?")
    public IRubyObject annotation_present_p(ThreadContext context, final IRubyObject annoClass) {
        final Class annotation;
        if (annoClass instanceof RubyClass && ((RubyClass) annoClass).getJavaProxy()) {
            annotation = ((RubyClass) annoClass).getJavaClass();
        } else if (annoClass instanceof JavaClass) {
            annotation = ((JavaClass) annoClass).javaClass();
        } else {
            throw context.runtime.newTypeError("expected a Java (proxy) class, got: " + annoClass);
        }
        return context.runtime.newBoolean( accessibleObject().isAnnotationPresent(annotation) );
    }

    // for our purposes, Accessibles are also Members, and vice-versa,
    // so we'll include Member methods here.
    @JRubyMethod
    @SuppressWarnings("deprecation")
    public IRubyObject declaring_class(ThreadContext context) {
        Class<?> clazz = ((Member) accessibleObject()).getDeclaringClass();
        if ( clazz != null ) return Java.getProxyClass(context.runtime, clazz);
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject modifiers(ThreadContext context) {
        return context.runtime.newFixnum(((Member) accessibleObject()).getModifiers());
    }

    @JRubyMethod
    public IRubyObject name(ThreadContext context) {
        return context.runtime.newString(((Member) accessibleObject()).getName());
    }

    @JRubyMethod(name = "synthetic?")
    public IRubyObject synthetic_p(ThreadContext context) {
        return context.runtime.newBoolean(((Member) accessibleObject()).isSynthetic());
    }

    @JRubyMethod(name = {"to_s", "to_string"})
    public RubyString to_string(ThreadContext context) {
        return context.runtime.newString( toString() );
    }

    @Override
    public String toString() {
        return accessibleObject().toString();
    }

}
