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
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class JavaAccessibleObject extends RubyObject {

    protected JavaAccessibleObject(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
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
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @JRubyMethod(name = {"==", "eql?"})
    public RubyBoolean op_equal(final IRubyObject other) {
        return RubyBoolean.newBoolean(getRuntime(), equals(other));
    }

    @JRubyMethod(name = "equal?")
    public RubyBoolean same(final IRubyObject other) {
        final boolean same = other instanceof JavaAccessibleObject && same((JavaAccessibleObject) other);
        return same ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "accessible?")
    public RubyBoolean isAccessible() {
        return RubyBoolean.newBoolean(getRuntime(), accessibleObject().isAccessible());
    }

    @JRubyMethod(name = "accessible=")
    public IRubyObject setAccessible(IRubyObject object) {
        accessibleObject().setAccessible(object.isTrue());
        return object;
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod
    public IRubyObject annotation(final IRubyObject annoClass) {
        if ( ! ( annoClass instanceof JavaClass ) ) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        final Class annotation = ((JavaClass) annoClass).javaClass();
        return Java.getInstance(getRuntime(), accessibleObject().getAnnotation(annotation));
    }

    @JRubyMethod
    public IRubyObject annotations() {
        return Java.getInstance(getRuntime(), accessibleObject().getAnnotations());
    }

    @JRubyMethod(name = "annotations?")
    public RubyBoolean annotations_p() {
        return getRuntime().newBoolean(accessibleObject().getAnnotations().length > 0);
    }

    @JRubyMethod
    public IRubyObject declared_annotations() {
        return Java.getInstance(getRuntime(), accessibleObject().getDeclaredAnnotations());
    }

    @JRubyMethod(name = "declared_annotations?")
    public RubyBoolean declared_annotations_p() {
        return getRuntime().newBoolean(accessibleObject().getDeclaredAnnotations().length > 0);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "annotation_present?")
    public IRubyObject annotation_present_p(final IRubyObject annoClass) {
        if ( ! ( annoClass instanceof JavaClass ) ) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        final Class annotation = ((JavaClass) annoClass).javaClass();
        return getRuntime().newBoolean( accessibleObject().isAnnotationPresent(annotation) );
    }

    // for our purposes, Accessibles are also Members, and vice-versa,
    // so we'll include Member methods here.
    @JRubyMethod
    public IRubyObject declaring_class() {
        Class<?> clazz = ((Member) accessibleObject()).getDeclaringClass();
        if ( clazz != null ) return JavaClass.get(getRuntime(), clazz);
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject modifiers() {
        return getRuntime().newFixnum(((Member) accessibleObject()).getModifiers());
    }

    @JRubyMethod
    public IRubyObject name() {
        return getRuntime().newString(((Member) accessibleObject()).getName());
    }

    @JRubyMethod(name = "synthetic?")
    public IRubyObject synthetic_p() {
        return getRuntime().newBoolean(((Member) accessibleObject()).isSynthetic());
    }

    @JRubyMethod(name = {"to_s", "to_string"})
    public RubyString to_string() {
        return getRuntime().newString( toString() );
    }

    @Override
    public String toString() {
        return accessibleObject().toString();
    }

    @Override
    public Object toJava(Class target) {
        if ( AccessibleObject.class.isAssignableFrom(target) ) {
            return accessibleObject();
        }
        return super.toJava(target);
    }

}
