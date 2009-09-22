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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
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
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Java::JavaObject")
public class JavaObject extends RubyObject {

    private static Object NULL_LOCK = new Object();
    private final RubyClass.VariableAccessor objectAccessor;

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
        return other instanceof JavaObject &&
                this.dataGetStruct() == ((JavaObject) other).dataGetStruct();
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
        Object dataStruct = dataGetStruct();
        if (dataStruct != null) {
            String stringValue = dataStruct.toString();
            if (stringValue != null) {
                return RubyString.newUnicodeString(getRuntime(), dataStruct.toString());
            }

            return getRuntime().getNil();
        }
        return RubyString.newEmptyString(getRuntime());
    }

    @JRubyMethod(name = {"==", "eql?"}, required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        if (!(other instanceof JavaObject)) {
            other = (JavaObject)other.dataGetStruct();
            if (!(other instanceof JavaObject)) {
                return getRuntime().getFalse();
            }
        }

        if (getValue() == null && ((JavaObject) other).getValue() == null) {
            return getRuntime().getTrue();
        }

        boolean isEqual = getValue().equals(((JavaObject) other).getValue());
        return isEqual ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "equal?", required = 1)
    public IRubyObject same(IRubyObject other) {
        if (!(other instanceof JavaObject)) {
            other = (JavaObject)other.dataGetStruct();
            if (!(other instanceof JavaObject)) {
                return getRuntime().getFalse();
            }
        }

        if (getValue() == null && ((JavaObject) other).getValue() == null) {
            return getRuntime().getTrue();
        }

        boolean isSame = getValue() == ((JavaObject) other).getValue();
        return isSame ? getRuntime().getTrue() : getRuntime().getFalse();
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

    @JRubyMethod(name = "[]", required = 1)
    public IRubyObject aref(IRubyObject index) {
        throw getRuntime().newTypeError("not a java array");
    }

    @JRubyMethod(name = "[]=", required = 2)
    public IRubyObject aset(IRubyObject index, IRubyObject someValue) {
        throw getRuntime().newTypeError("not a java array");
    }

    @JRubyMethod(name = "fill", required = 3)
    public IRubyObject afill(IRubyObject beginIndex, IRubyObject endIndex, IRubyObject someValue) {
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
    
    @JRubyMethod(frame = true)
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

    @JRubyMethod(frame = true)
    public IRubyObject marshal_load(ThreadContext context, IRubyObject str) {
        try {
            ByteList byteList = str.convertToString().getByteList();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteList.bytes, byteList.begin, byteList.realSize);
            ObjectInputStream ois = new ObjectInputStream(bais);

            dataWrapStruct(ois.readObject());

            return this;
        } catch (IOException ioe) {
            throw context.getRuntime().newIOErrorFromException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw context.getRuntime().newTypeError("Class not found unmarshaling Java type: " + cnfe.getLocalizedMessage());
        }
    }

    public Object toJava(Class cls) {
        return JavaUtil.coerceJavaObjectToType(getRuntime().getCurrentContext(), getValue(), cls);
    }

    private static final ObjectAllocator JAVA_OBJECT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new JavaObject(runtime, klazz, null);
        }
    };

}
