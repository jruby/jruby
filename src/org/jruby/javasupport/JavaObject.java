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

    private static Object NULL_LOCK = new Object();
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

    public static JavaObject wrap(Ruby runtime, Object value) {
        if (value != null) {
            if (value instanceof Class) {
                return JavaClass.get(runtime, (Class<?>) value);
            } else if (value.getClass().isArray()) {
                return new JavaArray(runtime, value);
            }
        }
        return new JavaObject(runtime, value);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject wrap(ThreadContext context, IRubyObject self, IRubyObject object) {
        Ruby runtime = context.runtime;
        Object obj = getWrappedObject(object, NEVER);

        if (obj == NEVER) return runtime.getNil();

        return wrap(runtime, obj);
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
    public boolean equals(Object other) {
        Object myValue = getValue();
        Object otherValue = other;
        if (other instanceof IRubyObject) {
            otherValue = getWrappedObject((IRubyObject)other, NEVER);
        }

        if (otherValue == NEVER) {
            // not a wrapped object
            return false;
        }
        return myValue == otherValue;
    }

    @Override
    public int hashCode() {
        Object dataStruct = dataGetStruct();
        if (dataStruct != null) {
            return dataStruct.hashCode();
        }
        return 0;
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
    public IRubyObject op_equal(IRubyObject other) {
        Object myValue = getValue();
        return opEqualShared(myValue, other);
    }

    public static IRubyObject op_equal(JavaProxy self, IRubyObject other) {
        Object myValue = self.getObject();
        return opEqualShared(myValue, other);
    }

    private static IRubyObject opEqualShared(Object myValue, IRubyObject other) {
        Ruby runtime = other.getRuntime();
        Object otherValue = getWrappedObject(other, NEVER);

        if (other == NEVER) {
            // not a wrapped object
            return runtime.getFalse();
        }

        if (myValue == null && otherValue == null) {
            return runtime.getTrue();
        }

        return runtime.newBoolean(myValue.equals(otherValue));
    }

    @JRubyMethod(name = "equal?", required = 1)
    public IRubyObject same(IRubyObject other) {
        Ruby runtime = getRuntime();
        Object myValue = getValue();
        Object otherValue = getWrappedObject(other, NEVER);

        if (other == NEVER) {
            // not a wrapped object
            return runtime.getFalse();
        }

        if (myValue == null && otherValue == null) {
            return getRuntime().getTrue();
        }

        if (!(other instanceof JavaObject)) return runtime.getFalse();

        boolean isSame = getValue() == ((JavaObject) other).getValue();
        return isSame ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    private static Object getWrappedObject(IRubyObject other, Object def) {
        if (other instanceof JavaObject) {
            return ((JavaObject)other).getValue();
        } else if (other instanceof JavaProxy) {
            return ((JavaProxy)other).getObject();
        } else {
            return def;
        }
    }

    @JRubyMethod
    public RubyString java_type() {
        return getRuntime().newString(getJavaClass().getName());
    }

    @JRubyMethod
    public IRubyObject java_class() {
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
    public Object toJava(Class cls) {
        if (getValue() == null) {
            // THIS SHOULD NEVER HAPPEN, but it DOES
            return getValue();
        }
        
        if (cls.isAssignableFrom(getValue().getClass())) {
            return getValue();
        }
        
        return super.toJava(cls);
    }

    private static final ObjectAllocator JAVA_OBJECT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new JavaObject(runtime, klazz, null);
        }
    };

}
