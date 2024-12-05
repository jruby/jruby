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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.javasupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyObjectInputStream;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Error.typeError;
import static org.jruby.javasupport.JavaUtil.unwrapJava;

/**
 * Java::JavaObject wrapping is no longer used with JRuby.
 * The (automatic) Java proxy wrapping has been the preferred method for a while.
 * Just keep using <code>java.lang.Object.new</code> as usual, without the manual
 * <code>JavaObject.wrap java_object</code>.
 *
 * @deprecated since 9.4
 * @author  jpetersen
 */
@Deprecated // @JRubyClass(name="Java::JavaObject")
public class JavaObject extends RubyObject {

    private static final Object NULL_LOCK = new Object();

    private final VariableAccessor objectAccessor;

    protected JavaObject(Ruby runtime, RubyClass rubyClass, Object value) {
        super(runtime, rubyClass);
        objectAccessor = rubyClass.getVariableAccessorForWrite("__wrap_struct__");
        dataWrapStruct(value);
    }

    private JavaObject(Ruby runtime, RubyClass klazz) {
        this(runtime, klazz, null);
    }

    @Override
    public final Object dataGetStruct() {
        return objectAccessor.get(this);
    }

    @Override
    public final void dataWrapStruct(Object object) {
        objectAccessor.set(this, object);
    }

    @Deprecated
    public static JavaObject wrap(final Ruby runtime, final Object value) {
        if ( value != null ) {
            if ( value instanceof Class ) {
                return JavaClass.get(runtime, (Class<?>) value);
            }
            if ( value.getClass().isArray() ) {
                return new JavaArray(runtime, value);
            }
        }
        return new JavaObject(runtime, runtime.getJavaSupport().getJavaModule().getClass("JavaObject"), value);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject wrap(final ThreadContext context,
        final IRubyObject self, final IRubyObject object) {
        final Object objectValue = unwrapJava(object, NEVER);

        if ( objectValue == NEVER ) return context.nil;

        return wrap(context.runtime, objectValue);
    }

    @Override
    public final Class<?> getJavaClass() {
        Object dataStruct = dataGetStruct();
        return dataStruct != null ? dataStruct.getClass() : Void.TYPE;
    }

    public final Object getValue() {
        return dataGetStruct();
    }

    public static RubyClass createJavaObjectClass(Ruby runtime, RubyClass Object, RubyModule javaModule) {
        var context = runtime.getCurrentContext();
        RubyClass JavaObject = javaModule.defineClassUnder(context, "JavaObject", Object, JAVA_OBJECT_ALLOCATOR).
                tap(c -> c.getMetaClass().undefMethods("new", "allocate"));

        JavaObject.defineMethods(context, JavaObject.class);

        return JavaObject;
    }

    @Override
    public boolean equals(final Object other) {
        final Object otherValue;
        if ( other instanceof IRubyObject ) {
            otherValue = unwrapJava((IRubyObject) other, NEVER);
        }
        else {
            otherValue = other;
        }

        if ( otherValue == NEVER ) return false;

        return getValue() == otherValue; // TODO seems weird why not equals ?!
    }

    @Override
    public int hashCode() {
        final Object value = dataGetStruct();
        return value == null ? 0 : value.hashCode();
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(getRuntime(), hashCode());
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s(ThreadContext context) {
        return JavaProxyMethods.to_s(context, dataGetStruct());
    }

    /**
     * @param runtime
     * @param dataStruct
     * @return ""
     * @deprecated Use {@link
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject to_s(Ruby runtime, Object dataStruct) {
        return JavaProxyMethods.to_s(runtime.getCurrentContext(), dataStruct);
    }

    @JRubyMethod(name = {"==", "eql?"})
    public IRubyObject op_equal(final IRubyObject other) {
        return JavaProxyMethods.equals(getRuntime(), getValue(), other);
    }

    public static RubyBoolean op_equal(JavaProxy self, IRubyObject other) {
        return JavaProxyMethods.equals(self.getRuntime(), self.getObject(), other);
    }

    @JRubyMethod(name = "equal?")
    public IRubyObject same(ThreadContext context, final IRubyObject other) {
        final Object thisValue = getValue();
        final Object otherValue = unwrapJava(other, NEVER);

        if (otherValue == NEVER) return context.fals; // not a wrapped object
        if (!(other instanceof JavaObject)) return context.fals;

        return asBoolean(context, thisValue == otherValue);
    }

    @Deprecated
    public IRubyObject same(final IRubyObject other) {
        return same(getCurrentContext(), other);
    }

    @JRubyMethod
    public RubyString java_type() {
        return getRuntime().newString(getJavaClass().getName());
    }

    @Deprecated
    public JavaClass java_class() {
        return JavaClass.get(getRuntime(), getJavaClass());
    }

    @JRubyMethod
    public IRubyObject get_java_class() {
        return Java.getInstance(getRuntime(), getJavaClass());
    }

    @JRubyMethod
    public RubyFixnum length() {
        throw typeError(getRuntime().getCurrentContext(), "not a java array");
    }

    @JRubyMethod(name = "java_proxy?")
    public IRubyObject is_java_proxy() {
        return getRuntime().getTrue();
    }

    @JRubyMethod(name = "synchronized")
    public final IRubyObject ruby_synchronized(ThreadContext context, Block block) {
        final Object lock = getValue();
        synchronized (lock != null ? lock : NULL_LOCK) {
            return block.yield(context, null);
        }
    }

    public static IRubyObject ruby_synchronized(ThreadContext context, final Object lock, Block block) {
        synchronized (lock != null ? lock : NULL_LOCK) {
            return block.yield(context, null);
        }
    }

    @JRubyMethod
    public IRubyObject marshal_dump(ThreadContext context) {
        if (Serializable.class.isAssignableFrom(getJavaClass())) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                new ObjectOutputStream(baos).writeObject(getValue());

                return context.runtime.newString(new ByteList(baos.toByteArray(), false));
            } catch (IOException ex) {
                throw context.runtime.newIOErrorFromException(ex);
            }
        }
        throw typeError(context, "no marshal_dump is defined for class " + getJavaClass());
    }

    @JRubyMethod
    public IRubyObject marshal_load(ThreadContext context, IRubyObject str) {
        try {
            ByteList byteList = str.convertToString().getByteList();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());

            dataWrapStruct(new JRubyObjectInputStream(context.runtime, bais).readObject());

            return this;
        } catch (IOException ex) {
            throw context.runtime.newIOErrorFromException(ex);
        } catch (ClassNotFoundException ex) {
            throw typeError(context, "Class not found unmarshaling Java type: " + ex.getLocalizedMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T toJava(Class<T> target) {
        final Object value = getValue();
        if ( value == null ) return null;

        if ( target.isAssignableFrom( value.getClass() ) ) {
            return target.cast(value);
        }
        return super.toJava(target);
    }

    private static final ObjectAllocator JAVA_OBJECT_ALLOCATOR = JavaObject::new;

}
