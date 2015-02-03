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
import java.io.ObjectInputStream;
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
import org.jruby.anno.JRubyClass;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyObjectInputStream;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Java::JavaObject")
public class JavaObject extends RubyObject {

    private static final Object NULL_LOCK = new Object();
    
    private final VariableAccessor objectAccessor;

    protected JavaObject(Ruby runtime, RubyClass rubyClass, Object value) {
        super(runtime, rubyClass);
        objectAccessor = rubyClass.getVariableAccessorForWrite("__wrap_struct__");
        dataWrapStruct(value);
    }

    @Override
    public Object dataGetStruct() {
        return objectAccessor.get(this);
    }

    @Override
    public void dataWrapStruct(Object object) {
        objectAccessor.set(this, object);
    }

    protected JavaObject(Ruby runtime, Object value) {
        this(runtime, runtime.getJavaSupport().getJavaObjectClass(), value);
    }

    public static JavaObject wrap(final Ruby runtime, final Object value) {
        if ( value != null ) {
            if ( value instanceof Class ) {
                return JavaClass.get(runtime, (Class<?>) value);
            }
            if ( value.getClass().isArray() ) {
                return new JavaArray(runtime, value);
            }
        }
        return new JavaObject(runtime, value);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject wrap(final ThreadContext context,
        final IRubyObject self, final IRubyObject object) {
        final Object objectValue = unwrapObject(object, NEVER);

        if ( objectValue == NEVER ) return context.nil;

        return wrap(context.runtime, objectValue);
    }

    @Override
    public Class<?> getJavaClass() {
        Object dataStruct = dataGetStruct();
        return dataStruct != null ? dataStruct.getClass() : Void.TYPE;
    }

    public Object getValue() {
        return dataGetStruct();
    }

    public static RubyClass createJavaObjectClass(Ruby runtime, RubyModule javaModule) {
        // FIXME: Ideally JavaObject instances should be marshallable, which means that
        // the JavaObject metaclass should have an appropriate allocator. JRUBY-414
        RubyClass result = javaModule.defineClassUnder("JavaObject", runtime.getObject(), JAVA_OBJECT_ALLOCATOR);

        registerRubyMethods(runtime, result);

        result.getMetaClass().undefineMethod("new");
        result.getMetaClass().undefineMethod("allocate");

        return result;
    }

    protected static void registerRubyMethods(Ruby runtime, RubyClass result) {
        result.defineAnnotatedMethods(JavaObject.class);
    }

    @Override
    public boolean equals(final Object other) {
        final Object otherValue;
        if ( other instanceof IRubyObject ) {
            otherValue = unwrapObject((IRubyObject) other, NEVER);
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
        if ( value == null ) return 0;
        return value.hashCode();
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        return to_s(getRuntime(), dataGetStruct());
    }

    public static IRubyObject to_s(Ruby runtime, Object dataStruct) {
        if (dataStruct != null) {
            String stringValue = dataStruct.toString();
            if (stringValue != null) {
                return RubyString.newUnicodeString(runtime, dataStruct.toString());
            }

            return runtime.getNil();
        }
        return RubyString.newEmptyString(runtime);
    }

    @JRubyMethod(name = {"==", "eql?"}, required = 1)
    public IRubyObject op_equal(final IRubyObject other) {
        return equals(getRuntime(), getValue(), other);
    }

    public static RubyBoolean op_equal(JavaProxy self, IRubyObject other) {
        return equals(self.getRuntime(), self.getObject(), other);
    }

    private static RubyBoolean equals(final Ruby runtime,
        final Object thisValue, final IRubyObject other) {

        final Object otherValue = unwrapObject(other, NEVER);

        if ( otherValue == NEVER ) { // not a wrapped object
            return runtime.getFalse();
        }

        if ( thisValue == null ) {
            return runtime.newBoolean(otherValue == null);
        }

        return runtime.newBoolean(thisValue.equals(otherValue));
    }

    @JRubyMethod(name = "equal?", required = 1)
    public IRubyObject same(final IRubyObject other) {
        final Ruby runtime = getRuntime();
        final Object thisValue = getValue();
        final Object otherValue = unwrapObject(other, NEVER);

        if ( otherValue == NEVER ) { // not a wrapped object
            return runtime.getFalse();
        }

        if ( ! (other instanceof JavaObject) ) return runtime.getFalse();

        return runtime.newBoolean(thisValue == otherValue);
    }

    private static Object unwrapObject(
        final IRubyObject wrapped, final Object defaultValue) {
        if ( wrapped instanceof JavaObject ) {
            return ((JavaObject) wrapped).getValue();
        }
        if ( wrapped instanceof JavaProxy ) {
            return ((JavaProxy) wrapped).getObject();
        }
        return defaultValue;
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

    @JRubyMethod(name = "java_proxy?")
    public IRubyObject is_java_proxy() {
        return getRuntime().getTrue();
    }

    @JRubyMethod(name = "synchronized")
    public IRubyObject ruby_synchronized(ThreadContext context, Block block) {
        Object lock = getValue();
        synchronized (lock != null ? lock : NULL_LOCK) {
            return block.yield(context, null);
        }
    }

    public static IRubyObject ruby_synchronized(ThreadContext context, Object lock, Block block) {
        synchronized (lock != null ? lock : NULL_LOCK) {
            return block.yield(context, null);
        }
    }

    @JRubyMethod
    public IRubyObject marshal_dump() {
        if (Serializable.class.isAssignableFrom(getJavaClass())) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(getValue());

                return getRuntime().newString(new ByteList(baos.toByteArray()));
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        } else {
            throw getRuntime().newTypeError("no marshal_dump is defined for class " + getJavaClass());
        }
    }

    @JRubyMethod
    public IRubyObject marshal_load(ThreadContext context, IRubyObject str) {
        try {
            ByteList byteList = str.convertToString().getByteList();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());
            ObjectInputStream ois = new JRubyObjectInputStream(context.runtime, bais);

            dataWrapStruct(ois.readObject());

            return this;
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw context.runtime.newTypeError("Class not found unmarshaling Java type: " + cnfe.getLocalizedMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object toJava(final Class target) {
        final Object value = getValue();
        if ( value == null ) return null;

        if ( target.isAssignableFrom( value.getClass() ) ) {
            return value;
        }
        return super.toJava(target);
    }

    private static final ObjectAllocator JAVA_OBJECT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new JavaObject(runtime, klazz, null);
        }
    };

}
