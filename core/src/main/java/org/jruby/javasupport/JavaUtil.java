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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.javasupport;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import static java.lang.Character.isLetter;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isDigit;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.headius.backport9.modules.Modules;
import org.jcodings.Encoding;
import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyInstanceConfig;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.proxies.RubyObjectHolderProxy;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;

public class JavaUtil {
    public static IRubyObject[] convertJavaArrayToRuby(final Ruby runtime, final Object[] objects) {
        if ( objects == null || objects.length == 0 ) return IRubyObject.NULL_ARRAY;

        if (objects instanceof String[]) return convertStringArrayToRuby(runtime, (String[]) objects, JAVA_STRING_CONVERTER);

        IRubyObject[] rubyObjects = new IRubyObject[objects.length];

        for (int i = 0; i < objects.length; i++) {
            rubyObjects[i] = convertJavaToUsableRubyObject(runtime, objects[i]);
        }
        return rubyObjects;
    }

    public static IRubyObject[] convertStringArrayToRuby(final Ruby runtime, final String[] strings, StringConverter converter) {
        if ( strings == null || strings.length == 0 ) return IRubyObject.NULL_ARRAY;

        IRubyObject[] rubyObjects = new IRubyObject[strings.length];

        for (int i = 0; i < strings.length; i++) {
            rubyObjects[i] = convertJavaToUsableRubyObjectWithConverter(runtime, strings[i], converter);
        }
        return rubyObjects;
    }

    public static RubyArray convertJavaArrayToRubyWithNesting(final ThreadContext context, final Object array) {
        final int length = Array.getLength(array);
        final IRubyObject[] rubyElements = new IRubyObject[length];
        for ( int i = 0; i < length; i++ ) {
            final Object element = Array.get(array, i);
            if ( element instanceof ArrayJavaProxy ) {
                rubyElements[i] = convertJavaArrayToRubyWithNesting(context, ((ArrayJavaProxy) element).getObject());
            }
            else if ( element != null && element.getClass().isArray() ) {
                rubyElements[i] = convertJavaArrayToRubyWithNesting(context, element);
            }
            else {
                rubyElements[i] = convertJavaToUsableRubyObject(context.runtime, element);
            }
        }
        return context.runtime.newArrayNoCopy(rubyElements);
    }

    public static JavaConverter getJavaConverter(Class clazz) {
        final JavaConverter converter = JAVA_CONVERTERS.get(clazz);
        return converter == null ? JAVA_DEFAULT_CONVERTER : converter;
    }

    public static IRubyObject convertJavaToRuby(Ruby runtime, Object object) {
        return convertJavaToUsableRubyObject(runtime, object);
    }

    public static IRubyObject convertJavaToRuby(Ruby runtime, Object object, Class javaClass) {
        return convertJavaToUsableRubyObjectWithConverter(runtime, object, getJavaConverter(javaClass));
    }

    public static IRubyObject convertJavaToRuby(Ruby runtime, int i) {
        return runtime.newFixnum(i);
    }

    public static IRubyObject convertJavaToRuby(Ruby runtime, long l) {
        return runtime.newFixnum(l);
    }

    public static IRubyObject convertJavaToRuby(Ruby runtime, float f) {
        return runtime.newFloat(f);
    }

    public static IRubyObject convertJavaToRuby(Ruby runtime, double d) {
        return runtime.newFloat(d);
    }

    public static IRubyObject convertJavaToRuby(Ruby runtime, boolean b) {
        return runtime.newBoolean(b);
    }

    /**
     * Returns a usable RubyObject; for types that are not converted to Ruby native
     * types, a Java proxy will be returned.
     *
     * @param runtime
     * @param object
     * @return corresponding Ruby type, or a functional Java proxy
     */
    public static IRubyObject convertJavaToUsableRubyObject(Ruby runtime, Object object) {
        IRubyObject result = trySimpleConversions(runtime, object);

        if (result != null) return result;

        JavaConverter converter = getJavaConverter(object.getClass());
        if (converter == null || converter == JAVA_DEFAULT_CONVERTER) {
            return Java.getInstance(runtime, object);
        }
        return converter.convert(runtime, object);
    }

    public static IRubyObject convertJavaToUsableRubyObjectWithConverter(Ruby runtime, Object object, JavaConverter converter) {
        IRubyObject result = trySimpleConversions(runtime, object);

        if (result != null) return result;

        if (converter == null || converter == JAVA_DEFAULT_CONVERTER) {
            return Java.getInstance(runtime, object);
        }
        return converter.convert(runtime, object);
    }

    public static IRubyObject convertJavaArrayElementToRuby(Ruby runtime, JavaConverter converter, Object array, int i) {
        if (converter == null || converter == JAVA_DEFAULT_CONVERTER) {
            return convertJavaToUsableRubyObject(runtime, ((Object[])array)[i]);
        }
        return converter.get(runtime, array, i);
    }

    public static Class<?> primitiveToWrapper(final Class<?> type) {
        return type.isPrimitive() ? CodegenUtils.getBoxType(type) : type;
    }

    public static boolean isDuckTypeConvertable(final Class<?> argumentType, final Class<?> targetType) {
        return targetType.isInterface() &&
                ! targetType.isAssignableFrom(argumentType) &&
                    RubyObject.class.isAssignableFrom(argumentType);
    }

    public static <T> T convertProcToInterface(ThreadContext context, RubyObject rubyObject, Class<T> targetType) {
        return convertProcToInterface(context, (RubyBasicObject) rubyObject, targetType);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertProcToInterface(ThreadContext context, RubyBasicObject rubyObject, Class<T> targetType) {
        final Ruby runtime = context.runtime;

        // Capture original class; we only detach the singleton for natural Proc instances
        RubyClass procClass = rubyObject.getMetaClass();

        // Extend the interfaces into the proc's class. This creates a singleton class to connect up the Java proxy.
        final RubyModule ifaceModule = Java.getInterfaceModule(runtime, targetType);
        if ( ! ifaceModule.isInstance(rubyObject) ) {
            ifaceModule.callMethod(context, "extend_object", rubyObject);
            ifaceModule.callMethod(context, "extended", rubyObject);
        }

        if ( rubyObject instanceof RubyProc ) {
            // Proc implementing an interface, pull in the catch-all code that lets the proc get invoked
            // no matter what method is called on the interface
            final RubyClass singletonClass = rubyObject.getSingletonClass();

            if (procClass == runtime.getProc()) {
                // We reattach the singleton class to the Proc class object to prevent the method cache in the interface
                // impl from rooting the proc and its binding in the host classloader. See GH-4968.
                ((MetaClass) singletonClass).setAttached(runtime.getProc());
            }

            final Java.ProcToInterface procToIface = new Java.ProcToInterface(singletonClass);
            singletonClass.addMethod("method_missing", procToIface);
            // similar to Iface.impl { ... } - bind interface method(s) to avoid Java-Ruby conflicts
            // ... e.g. calling a Ruby implemented Predicate#test should not dispatch to Kernel#test
            // getMethods for interface returns all methods (including ones from super-interfaces)
            for ( Method method : targetType.getMethods() ) {
                Java.ProcToInterface.ConcreteMethod implMethod = procToIface.getConcreteMethod(method.getName());
                if ( Modifier.isAbstract(method.getModifiers()) ) {
                    singletonClass.addMethodInternal(method.getName(), implMethod);
                }
            }

        }
        JavaObject javaObject = (JavaObject) Helpers.invoke(context, rubyObject, "__jcreate_meta!");
        return (T) javaObject.getValue();
    }

    public static <T> NumericConverter<T> getNumericConverter(Class<T> target) {
        final NumericConverter converter = NUMERIC_CONVERTERS.get(target);
        return converter == null ? NUMERIC_TO_OTHER : converter;
    }

    /**
     * Test if a passed instance is a wrapper Java object.
     * @param object
     * @return true if the object is wrapping a Java object
     */
    public static boolean isJavaObject(final IRubyObject object) {
        return object instanceof JavaProxy || object.dataGetStruct() instanceof JavaObject;
    }

    /**
     * Unwrap a wrapped Java object.
     * @param object
     * @return Java object
     * @see JavaUtil#isJavaObject(IRubyObject)
     */
    public static <T> T unwrapJavaObject(final IRubyObject object) {
        if ( object instanceof JavaProxy ) {
            return (T) ((JavaProxy) object).getObject();
        }
        return (T) ((JavaObject) object.dataGetStruct()).getValue();
    }

    /**
     * Unwrap if the passed object is a Java object, otherwise return object.
     * @param object
     * @return java object or passed object
     * @see JavaUtil#isJavaObject(IRubyObject)
     */
    public static <T> T unwrapIfJavaObject(final IRubyObject object) {
        if ( object instanceof JavaProxy ) {
            return (T) ((JavaProxy) object).getObject();
        }
        final Object unwrap = object.dataGetStruct();
        if ( unwrap instanceof JavaObject ) {
            return (T) ((JavaObject) unwrap).getValue();
        }
        return (T) object; // assume correct instance
    }

    @Deprecated // no longer used
    public static Object unwrapJavaValue(final Ruby runtime, final IRubyObject object, final String errorMessage) {
        if ( object instanceof JavaProxy ) {
            return ((JavaProxy) object).getObject();
        }
        if ( object instanceof JavaObject ) {
            return ((JavaObject) object).getValue();
        }
        final Object unwrap = object.dataGetStruct();
        if ( unwrap instanceof IRubyObject ) {
            return unwrapJavaValue(runtime, (IRubyObject) unwrap, errorMessage);
        }
        throw runtime.newTypeError(errorMessage);
    }

    public static RubyString inspectObject(ThreadContext context, Object obj) {
        if (!(obj instanceof IRubyObject)) {
            obj = Java.getInstance(context.runtime, obj);
        }
        return RubyObject.inspect(context, (IRubyObject) obj);
    }

    /**
     * @param object
     * @note Returns null if not a wrapped Java value.
     * @return unwrapped Java (object's) value
     */
    public static Object unwrapJavaValue(final IRubyObject object) {
        if ( object instanceof JavaProxy ) {
            return ((JavaProxy) object).getObject();
        }
        if ( object instanceof JavaObject ) {
            return ((JavaObject) object).getValue();
        }
        final Object unwrap = object.dataGetStruct();
        if ( unwrap instanceof IRubyObject ) {
            return unwrapJavaValue((IRubyObject) unwrap);
        }
        return null;
    }

    /**
     * For methods that match /(get|set|is)([A-Z0-9])(.*)/, return the "name"
     * part of the property with leading lower-case.
     *
     * @note Does not use regular expression for performance reasons.
     *
     * @param beanMethodName the bean method from which to extract a name
     * @return the bean property name (or null)
     */
    public static String getJavaPropertyName(final String beanMethodName) {
        final int length = beanMethodName.length(); char ch;
        final boolean maybeGetOrSet = length > 3 && beanMethodName.charAt(2) == 't';
        if ( maybeGetOrSet && ( beanMethodName.startsWith("get") || beanMethodName.startsWith("set") ) ) {
            if (isUpperDigit(ch = beanMethodName.charAt(3))) {
                if ( length == 4 ) return Character.toString(toLowerCase(ch));
                return toLowerCase(ch) + beanMethodName.substring(4);
            }
        }
        else if ( beanMethodName.startsWith("is") && length > 2 ) {
            if (isUpperDigit(ch = beanMethodName.charAt(2))) {
                if ( length == 3 ) return Character.toString( toLowerCase(ch) );
                return toLowerCase(ch) + beanMethodName.substring(3);
            }
        }
        return null;
    }

    // property -> getProperty
    public static String toJavaGetName(final String propertyName) {
        if ( propertyName == null ) return null;
        final int len = propertyName.length();
        if ( len == 0 ) return null;
        final char first = toUpperCase(propertyName.charAt(0));
        if ( len == 1 ) return "get" + first;
        return "get" + first + propertyName.substring(1);
    }

    // property -> isProperty
    public static String toJavaIsName(final String propertyName) {
        if ( propertyName == null ) return null;
        final int len = propertyName.length();
        if ( len == 0 ) return null;
        final char first = toUpperCase(propertyName.charAt(0));
        if ( len == 1 ) return "is" + first;
        return "is" + first + propertyName.substring(1);
    }

    /**
     * Build a Ruby name from a Java name by treating '_' as divider and successive
     * caps as all the same word.
     * @param javaCasedName
     * @return Ruby (under-score) cased named e.g. "get_foo_bar"
     */
    public static String getRubyCasedName(final String javaCasedName) {
        final char[] javaName = javaCasedName.toCharArray();
        final int len = javaName.length;
        final StringBuilder rubyName = new StringBuilder(len + 8);

        int behind = 0;
        for (int i = 0; i < len; i++) {
            if ( behind < 2 ) behind++;
            else behind = consume(rubyName, javaName, i);
        }

        if (behind == 2) {
            final char c1 = javaName[len - 1], c2 = javaName[len - 2];
            rubyName.append( toLowerCase( c2 ) );
            if ( isUpperCase( c1 ) && ! isUpperCase( c2 ) ) rubyName.append('_');
            rubyName.append( toLowerCase( c1 ) );
        }
        else if (behind > 0) {
            if ( behind > 1 ) {
                rubyName.append( toLowerCase( javaName[len - 2] ) );
            }
            rubyName.append( toLowerCase( javaName[len - 1] ) );
        }
        return rubyName.toString();
    }

    private static int consume(final StringBuilder rubyName, final char[] javaName, int i) {
        final char prev1 = javaName[i - 1], prev2 = javaName[i - 2];
        if ( isLowerDigit( prev2 ) && isUpperCase( prev1 ) ) {
            rubyName.append( prev2 ).append('_').append( toLowerCase(prev1) );
            return 1;
        }
        char cur;
        if ( isLetterDigit( prev2 ) && isUpperCase( prev1 ) && isLowerCase( cur = javaName[i] )) {
            rubyName.append( toLowerCase(prev2) ).append('_').append( toLowerCase(prev1) ).append(cur);
            return 0;
        }
        rubyName.append( toLowerCase(prev2) );
        return 2;
    }

    private static boolean isUpperDigit(char c) {
        return isUpperCase(c) || isDigit(c);
    }
    private static boolean isLowerDigit(char c) {
        return isLowerCase(c) || isDigit(c);
    }
    private static boolean isLetterDigit(char c) {
        return isLetter(c) || isDigit(c);
    }

    private static final Pattern RUBY_CASE_SPLITTER = Pattern.compile("([a-z][0-9]*)_([a-z])");
    public static String getJavaCasedName(String javaCasedName) {
        Matcher m = RUBY_CASE_SPLITTER.matcher(javaCasedName);
        StringBuffer newName = new StringBuffer();
        if (!m.find()) {
            return null;
        }
        m.reset();

        while (m.find()) {
            m.appendReplacement(newName, m.group(1) + Character.toUpperCase(m.group(2).charAt(0)));
        }

        m.appendTail(newName);

        return newName.toString();
    }

    /**
     * Given a simple Java method name and the Java Method objects that represent
     * all its overloads, add to the given nameSet all possible Ruby names that would
     * be valid.
     *
     * @param javaName
     * @param methods
     * @return method names
     */
    public static Set<String> getRubyNamesForJavaName(final String javaName, final List<Method> methods) {
        final String javaPropertyName = getJavaPropertyName(javaName);
        final String rubyName = getRubyCasedName(javaName);

        final int len = methods.size();

        final LinkedHashSet<String> nameSet = new LinkedHashSet<String>(6 * len + 2, 1f); // worse-case 6
        nameSet.add(javaName);
        nameSet.add(rubyName);

        if ( len == 1 ) { // hot-path - most of the time no-overloads for a given method name
            addRubyNamesForJavaName(javaName, methods.get(0), javaPropertyName, rubyName, nameSet);
        }
        else {
            for ( int i = 0; i < len; i++ ) { // passed list is ArrayList
                addRubyNamesForJavaName(javaName, methods.get(i), javaPropertyName, rubyName, nameSet);
            }
        }
        return nameSet;
    }

    private static void addRubyNamesForJavaName(final String javaName, final Method method,
        final String javaPropertyName, final String rubyName, final LinkedHashSet<String> nameSet) {
        final Class<?> resultType = method.getReturnType();

        // Add property name aliases
        if (javaPropertyName != null) {
            final Class<?>[] argTypes = method.getParameterTypes();
            final int argCount = argTypes.length;
            // string starts-with "get_" or "set_" micro-optimization :
            final boolean maybeGetOrSet_ = rubyName.length() > 3 && rubyName.charAt(3) == '_';

            if (maybeGetOrSet_ && rubyName.startsWith("get")) { // rubyName.startsWith("get_")
                if (argCount == 0 ||                                // getFoo      => foo
                    argCount == 1 && argTypes[0] == int.class) {    // getFoo(int) => foo(int)
                    final String rubyPropertyName = rubyName.substring(4);
                    nameSet.add(javaPropertyName);
                    nameSet.add(rubyPropertyName);
                    if (resultType == boolean.class) {              // getFooBar() => fooBar?, foo_bar?(*)
                        nameSet.add(javaPropertyName + '?');
                        nameSet.add(rubyPropertyName + '?');
                    }
                }
            }
            else if (maybeGetOrSet_ && rubyName.startsWith("set")) { // rubyName.startsWith("set_")
                if (argCount == 1 && resultType == void.class) {    // setFoo(Foo) => foo=(Foo)
                    final String rubyPropertyName = rubyName.substring(4);
                    nameSet.add(javaPropertyName + '=');
                    nameSet.add(rubyPropertyName + '=');
                }
            }
            else if (rubyName.startsWith("is_")) {
                if (resultType == boolean.class) {                  // isFoo() => foo?, isFoo(*) => foo(*)
                    final String rubyPropertyName = rubyName.substring(3);
                    nameSet.add(javaPropertyName); // NOTE: these are really a bad idea - and can cause issues
                    nameSet.add(rubyPropertyName); // GH-3470 unfortunately due backwards-compat they stay ;(
                    nameSet.add(javaPropertyName + '?');
                    nameSet.add(rubyPropertyName + '?');
                }
            }
        } else {
            // If not a property, but is boolean add ?-postfixed aliases.
            if (resultType == boolean.class) {
                // is_something?, contains_thing?
                nameSet.add(javaName + '?');
                nameSet.add(rubyName + '?');
            }
        }
    }

    public static Object[] convertArguments(final IRubyObject[] args, final Class<?>[] types) {
        return convertArguments(args, types, 0);
    }

    public static Object[] convertArguments(final IRubyObject[] args, final Class<?>[] types, int offset) {
        final Object[] arguments = new Object[ args.length - offset ];
        for ( int i = arguments.length; --i >= 0; ) {
            arguments[i] = args[ i + offset ].toJava( types[i] );
        }
        return arguments;
    }

    /**
     * Clone a Java object, assuming its class has an accessible <code>clone</code> method.
     * @param object
     * @return cloned object or null (if method is not found or inaccessible)
     */
    public static <T> T clone(final Object object) {
        return (T) clone(object, false);
    }

    static Object clone(final Object object, final boolean silent) {
        try {
            final Method clone = object.getClass().getMethod("clone");
            return clone.invoke(object);
        }
        catch (NoSuchMethodException|IllegalAccessException e) {
            return null;
        }
        catch (InvocationTargetException e) {
            if ( ! silent ) Helpers.throwException(e.getTargetException());
            return null;
        }
    }

    public static MethodHandle getHandleSafe(Method method, Class caller, MethodHandles.Lookup lookup) {
        try {
            if (Modules.trySetAccessible(method, caller)) {
                return lookup.unreflect(method);
            }
        } catch (Exception iae2) {
            // ignore, return null below
        }

        return null;
    }

    public static MethodHandle getGetterSafe(Field field, Class caller, MethodHandles.Lookup lookup) {
        try {
            if (Modules.trySetAccessible(field, caller)) {
                return lookup.unreflectGetter(field);
            }
        } catch (Exception iae2) {
            // ignore, return null below
        }

        return null;
    }

    public static MethodHandle getSetterSafe(Field field, Class caller, MethodHandles.Lookup lookup) {
        try {
            if (Modules.trySetAccessible(field, caller)) {
                return lookup.unreflectSetter(field);
            }
        } catch (Exception iae2) {
            // ignore, return null below
        }

        return null;
    }

    public static abstract class JavaConverter {
        private final Class type;
        public JavaConverter(Class type) {this.type = type;}
        public abstract IRubyObject convert(Ruby runtime, Object object);
        public abstract IRubyObject get(Ruby runtime, Object array, int i);
        public abstract void set(Ruby runtime, Object array, int i, IRubyObject value);
        public String toString() {return type.getName() + " converter";}
    }

    public interface NumericConverter<T> {
        T coerce(RubyNumeric numeric, Class<T> target);
    }

    public static IRubyObject trySimpleConversions(Ruby runtime, Object object) {
        if ( object == null ) return runtime.getNil();

        if ( object instanceof IRubyObject ) return (IRubyObject) object;

        if ( object instanceof RubyObjectHolderProxy ) {
            return ((RubyObjectHolderProxy) object).__ruby_object();
        }

        if ( object instanceof ReifiedJavaProxy ) {
            final ReifiedJavaProxy internalJavaProxy = (ReifiedJavaProxy) object;
            IRubyObject orig = internalJavaProxy.___jruby$rubyObject();
            if (orig != null) return orig;
        }

        return null;
    }

    public static final JavaConverter JAVA_DEFAULT_CONVERTER = new JavaConverter(Object.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            IRubyObject result = trySimpleConversions(runtime, object);
            return result == null ? JavaObject.wrap(runtime, object) : result;
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Object[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Object[])array)[i] = value.toJava(Object.class);
        }
    };

    public static final JavaConverter JAVA_BOOLEAN_CONVERTER = new JavaConverter(Boolean.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyBoolean.newBoolean(runtime, ((Boolean)object).booleanValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Boolean[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Boolean[])array)[i] = (Boolean)value.toJava(Boolean.class);
        }
    };

    public static final JavaConverter JAVA_FLOAT_CONVERTER = new JavaConverter(Float.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFloat.newFloat(runtime, ((Float)object).doubleValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Float[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Float[])array)[i] = (Float)value.toJava(Float.class);
        }
    };

    public static final JavaConverter JAVA_DOUBLE_CONVERTER = new JavaConverter(Double.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFloat.newFloat(runtime, ((Double)object).doubleValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Double[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Double[])array)[i] = (Double)value.toJava(Double.class);
        }
    };

    public static final JavaConverter JAVA_CHAR_CONVERTER = new JavaConverter(Character.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Character)object).charValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Character[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Character[])array)[i] = (Character)value.toJava(Character.class);
        }
    };

    public static final JavaConverter JAVA_BYTE_CONVERTER = new JavaConverter(Byte.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Byte)object).byteValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Byte[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Byte[])array)[i] = (Byte)value.toJava(Byte.class);
        }
    };

    public static final JavaConverter JAVA_SHORT_CONVERTER = new JavaConverter(Short.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Short)object).shortValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Short[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Short[])array)[i] = (Short)value.toJava(Short.class);
        }
    };

    public static final JavaConverter JAVA_INT_CONVERTER = new JavaConverter(Integer.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Integer)object).intValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Integer[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Integer[])array)[i] = (Integer)value.toJava(Integer.class);
        }
    };

    public static final JavaConverter JAVA_LONG_CONVERTER = new JavaConverter(Long.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Long)object).longValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Long[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Long[])array)[i] = (Long)value.toJava(Long.class);
        }
    };

    public static final JavaConverter JAVA_BOOLEANPRIM_CONVERTER = new JavaConverter(boolean.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyBoolean.newBoolean(runtime, ((Boolean)object).booleanValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyBoolean.newBoolean(runtime, ((boolean[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((boolean[])array)[i] = (Boolean)value.toJava(boolean.class);
        }
    };

    public static final JavaConverter JAVA_FLOATPRIM_CONVERTER = new JavaConverter(float.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFloat.newFloat(runtime, ((Float)object).doubleValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyFloat.newFloat(runtime, ((float[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((float[])array)[i] = (Float)value.toJava(float.class);
        }
    };

    public static final JavaConverter JAVA_DOUBLEPRIM_CONVERTER = new JavaConverter(double.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFloat.newFloat(runtime, ((Double)object).doubleValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyFloat.newFloat(runtime, ((double[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((double[])array)[i] = (Double)value.toJava(double.class);
        }
    };

    public static final JavaConverter JAVA_CHARPRIM_CONVERTER = new JavaConverter(char.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Character)object).charValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyFixnum.newFixnum(runtime, ((char[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((char[])array)[i] = (Character)value.toJava(char.class);
        }
    };

    public static final JavaConverter JAVA_BYTEPRIM_CONVERTER = new JavaConverter(byte.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Byte)object).byteValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyFixnum.newFixnum(runtime, ((byte[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((byte[])array)[i] = (Byte)value.toJava(byte.class);
        }
    };

    public static final JavaConverter JAVA_SHORTPRIM_CONVERTER = new JavaConverter(short.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Short)object).shortValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyFixnum.newFixnum(runtime, ((short[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((short[])array)[i] = (Short)value.toJava(short.class);
        }
    };

    public static final JavaConverter JAVA_INTPRIM_CONVERTER = new JavaConverter(int.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Integer)object).intValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyFixnum.newFixnum(runtime, ((int[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((int[])array)[i] = (Integer)value.toJava(int.class);
        }
    };

    public static final JavaConverter JAVA_LONGPRIM_CONVERTER = new JavaConverter(long.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Long)object).longValue());
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return RubyFixnum.newFixnum(runtime, ((long[])array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((long[])array)[i] = (Long)value.toJava(long.class);
        }
    };

    public static class StringConverter extends JavaConverter {
        private final Encoding encoding;

        public StringConverter() {
            this(null);
        }
        
        public StringConverter(Encoding encoding) {
            super(String.class);
            this.encoding = encoding;
        }

        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            if (encoding == null) {
            	return RubyString.newUnicodeString(runtime, (String)object);
            }
            else {
            	return RubyString.newString(runtime, (String)object, encoding);
            }
        }

        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((String[]) array)[i]);
        }

        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((String[])array)[i] = value.toJava(String.class);
        }
    }

    public static final StringConverter JAVA_STRING_CONVERTER = new StringConverter();

    public static final JavaConverter JAVA_CHARSEQUENCE_CONVERTER = new JavaConverter(CharSequence.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyString.newUnicodeString(runtime, (CharSequence)object);
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((CharSequence[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((CharSequence[])array)[i] = (CharSequence)value.toJava(CharSequence.class);
        }
    };

    public static final JavaConverter BYTELIST_CONVERTER = new JavaConverter(ByteList.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyString.newString(runtime, (ByteList)object);
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((ByteList[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((ByteList[])array)[i] = (ByteList)value.toJava(ByteList.class);
        }
    };

    public static final JavaConverter JAVA_BIGINTEGER_CONVERTER = new JavaConverter(BigInteger.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyBignum.newBignum(runtime, (BigInteger)object);
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((BigInteger[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((BigInteger[])array)[i] = (BigInteger)value.toJava(BigInteger.class);
        }
    };

    private static final Map<Class, JavaConverter> JAVA_CONVERTERS = new IdentityHashMap<>(24);

    static {
        JAVA_CONVERTERS.put(Byte.class, JAVA_BYTE_CONVERTER);
        JAVA_CONVERTERS.put(Byte.TYPE, JAVA_BYTEPRIM_CONVERTER);
        JAVA_CONVERTERS.put(Short.class, JAVA_SHORT_CONVERTER);
        JAVA_CONVERTERS.put(Short.TYPE, JAVA_SHORTPRIM_CONVERTER);
        JAVA_CONVERTERS.put(Character.class, JAVA_CHAR_CONVERTER);
        JAVA_CONVERTERS.put(Character.TYPE, JAVA_CHARPRIM_CONVERTER);
        JAVA_CONVERTERS.put(Integer.class, JAVA_INT_CONVERTER);
        JAVA_CONVERTERS.put(Integer.TYPE, JAVA_INTPRIM_CONVERTER);
        JAVA_CONVERTERS.put(Long.class, JAVA_LONG_CONVERTER);
        JAVA_CONVERTERS.put(Long.TYPE, JAVA_LONGPRIM_CONVERTER);
        JAVA_CONVERTERS.put(Float.class, JAVA_FLOAT_CONVERTER);
        JAVA_CONVERTERS.put(Float.TYPE, JAVA_FLOATPRIM_CONVERTER);
        JAVA_CONVERTERS.put(Double.class, JAVA_DOUBLE_CONVERTER);
        JAVA_CONVERTERS.put(Double.TYPE, JAVA_DOUBLEPRIM_CONVERTER);
        JAVA_CONVERTERS.put(Boolean.class, JAVA_BOOLEAN_CONVERTER);
        JAVA_CONVERTERS.put(Boolean.TYPE, JAVA_BOOLEANPRIM_CONVERTER);

        JAVA_CONVERTERS.put(String.class, JAVA_STRING_CONVERTER);
        JAVA_CONVERTERS.put(CharSequence.class, JAVA_CHARSEQUENCE_CONVERTER);

        JAVA_CONVERTERS.put(ByteList.class, BYTELIST_CONVERTER);

        JAVA_CONVERTERS.put(BigInteger.class, JAVA_BIGINTEGER_CONVERTER);
    }

    private static final NumericConverter<Byte> NUMERIC_TO_BYTE = (numeric, target) -> {
        final long value = numeric.getLongValue();
        if ( isLongByteable(value) ) return (byte) value;
        throw numeric.getRuntime().newRangeError("too big for byte: " + numeric);
    };
    private static final NumericConverter<Short> NUMERIC_TO_SHORT = (numeric, target) -> {
        final long value = numeric.getLongValue();
        if ( isLongShortable(value) ) return (short) value;
        throw numeric.getRuntime().newRangeError("too big for short: " + numeric);
    };
    private static final NumericConverter<Character> NUMERIC_TO_CHARACTER = (numeric, target) -> {
        final long value = numeric.getLongValue();
        if ( isLongCharable(value) ) return (char) value;
        throw numeric.getRuntime().newRangeError("too big for char: " + numeric);
    };
    private static final NumericConverter<Integer> NUMERIC_TO_INTEGER = (numeric, target) -> {
        final long value = numeric.getLongValue();
        if ( isLongIntable(value) ) return (int) value;
        throw numeric.getRuntime().newRangeError("too big for int: " + numeric);
    };
    private static final NumericConverter<Long> NUMERIC_TO_LONG = (numeric, target) -> numeric.getLongValue();
    private static final NumericConverter<Float> NUMERIC_TO_FLOAT = (numeric, target) -> {
        final double value = numeric.getDoubleValue();
        // many cases are ok to convert to float; if not one of these, error
        if ( isDoubleFloatable(value) ) return (float) value;
        throw numeric.getRuntime().newTypeError("too big for float: " + numeric);
    };
    private static final NumericConverter<Double> NUMERIC_TO_DOUBLE = (numeric, target) -> numeric.getDoubleValue();
    private static final NumericConverter<BigInteger> NUMERIC_TO_BIGINTEGER = (numeric, target) -> numeric.getBigIntegerValue();

    private static final NumericConverter NUMERIC_TO_OTHER = (numeric, target) -> {
        if (target.isAssignableFrom(numeric.getClass())) {
            // just return as-is, since we can't do any coercion
            return numeric;
        }
        // otherwise, error; no conversion available
        throw numeric.getRuntime().newTypeError("could not coerce " + numeric.getMetaClass() + " to " + target);
    };
    private static final NumericConverter<Object> NUMERIC_TO_OBJECT = (numeric, target) -> {
        // for Object, default to natural wrapper type
        if (numeric instanceof RubyFixnum) {
            long value = numeric.getLongValue();
            return Long.valueOf(value);
        } else if (numeric instanceof RubyFloat) {
            double value = numeric.getDoubleValue();
            return Double.valueOf(value);
        } else if (numeric instanceof RubyBignum) {
            return ((RubyBignum)numeric).getValue();
        } else if (numeric instanceof RubyBigDecimal) {
            return ((RubyBigDecimal)numeric).getValue();
        } else {
            return NUMERIC_TO_OTHER.coerce(numeric, target);
        }
    };
    private static final NumericConverter NUMERIC_TO_VOID = (numeric, target) -> null;
    private static boolean isDoubleFloatable(double value) {
        return true;
    }
    private static boolean isLongByteable(long value) {
        return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
    }
    private static boolean isLongShortable(long value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }
    private static boolean isLongCharable(long value) {
        return value >= Character.MIN_VALUE && value <= Character.MAX_VALUE;
    }
    private static boolean isLongIntable(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    private static final Map<Class, NumericConverter> NUMERIC_CONVERTERS = new IdentityHashMap<>(24);

    static {
        NUMERIC_CONVERTERS.put(Byte.TYPE, NUMERIC_TO_BYTE);
        NUMERIC_CONVERTERS.put(Byte.class, NUMERIC_TO_BYTE);
        NUMERIC_CONVERTERS.put(Short.TYPE, NUMERIC_TO_SHORT);
        NUMERIC_CONVERTERS.put(Short.class, NUMERIC_TO_SHORT);
        NUMERIC_CONVERTERS.put(Character.TYPE, NUMERIC_TO_CHARACTER);
        NUMERIC_CONVERTERS.put(Character.class, NUMERIC_TO_CHARACTER);
        NUMERIC_CONVERTERS.put(Integer.TYPE, NUMERIC_TO_INTEGER);
        NUMERIC_CONVERTERS.put(Integer.class, NUMERIC_TO_INTEGER);
        NUMERIC_CONVERTERS.put(Long.TYPE, NUMERIC_TO_LONG);
        NUMERIC_CONVERTERS.put(Long.class, NUMERIC_TO_LONG);
        NUMERIC_CONVERTERS.put(Float.TYPE, NUMERIC_TO_FLOAT);
        NUMERIC_CONVERTERS.put(Float.class, NUMERIC_TO_FLOAT);
        NUMERIC_CONVERTERS.put(Double.TYPE, NUMERIC_TO_DOUBLE);
        NUMERIC_CONVERTERS.put(Double.class, NUMERIC_TO_DOUBLE);
        NUMERIC_CONVERTERS.put(BigInteger.class, NUMERIC_TO_BIGINTEGER);
        NUMERIC_CONVERTERS.put(Object.class, NUMERIC_TO_OBJECT);
        NUMERIC_CONVERTERS.put(Number.class, NUMERIC_TO_OBJECT);
        NUMERIC_CONVERTERS.put(Serializable.class, NUMERIC_TO_OBJECT);
        NUMERIC_CONVERTERS.put(void.class, NUMERIC_TO_VOID);
    }

    public static Object objectFromJavaProxy(IRubyObject self) {
        return ((JavaProxy)self).getObject();
    }

    public static final Map<String, Class> PRIMITIVE_CLASSES;
    static {
        Map<String, Class> primitiveClasses = new HashMap<>(10, 1);
        primitiveClasses.put("boolean", Boolean.TYPE);
        primitiveClasses.put("byte", Byte.TYPE);
        primitiveClasses.put("char", Character.TYPE);
        primitiveClasses.put("short", Short.TYPE);
        primitiveClasses.put("int", Integer.TYPE);
        primitiveClasses.put("long", Long.TYPE);
        primitiveClasses.put("float", Float.TYPE);
        primitiveClasses.put("double", Double.TYPE);
        PRIMITIVE_CLASSES = Collections.unmodifiableMap(primitiveClasses);
    }

    public static Class<?> getPrimitiveClass(final String name) {
        switch (name) {
            case "boolean": return Boolean.TYPE;
            case "byte": return Byte.TYPE;
            case "char": return Character.TYPE;
            case "short": return Short.TYPE;
            case "int": return Integer.TYPE;
            case "long": return Long.TYPE;
            case "float": return Float.TYPE;
            case "double": return Double.TYPE;

            case "void": return Void.TYPE;
        }
        return null;
    }

    @Deprecated
    public static Object convertRubyToJava(IRubyObject rubyObject) {
        return convertRubyToJava(rubyObject, Object.class);
    }

    @Deprecated
    public static Object convertRubyToJava(IRubyObject rubyObject, Class javaClass) {
        if ( javaClass == void.class || rubyObject == null || rubyObject.isNil() ) {
            return null;
        }

        final Ruby runtime = rubyObject.getRuntime();
        final ThreadContext context = runtime.getCurrentContext();

        IRubyObject origObject = rubyObject;
        if (rubyObject.dataGetStruct() instanceof JavaObject) {
            rubyObject = (IRubyObject) rubyObject.dataGetStruct();
            if ( rubyObject == null ) {
                throw new RuntimeException("dataGetStruct returned null for " + origObject.getType().getName());
            }
        } else if (rubyObject.respondsTo("java_object")) {
            rubyObject = rubyObject.callMethod(context, "java_object");
            if( rubyObject == null ) {
                throw new RuntimeException("java_object returned null for " + origObject.getType().getName());
            }
        }

        if (rubyObject instanceof JavaObject) {
            Object value =  ((JavaObject) rubyObject).getValue();
            return convertArgument(runtime, value, value.getClass());
        }

        if (javaClass == Object.class || javaClass == null) {
            /* The Java method doesn't care what class it is, but we need to
               know what to convert it to, so we use the object's own class.
               If that doesn't help, we use String to force a call to the
               object's "to_s" method. */
            javaClass = rubyObject.getJavaClass();
        }

        if (javaClass.isInstance(rubyObject)) {
            // rubyObject is already of the required jruby class (or subclass)
            return rubyObject;
        }

        // the converters handle not only primitive types but also their boxed versions, so we should check
        // if we have a converter before checking for isPrimitive()
        RubyConverter converter = RUBY_CONVERTERS.get(javaClass);
        if (converter != null) {
            return converter.convert(context, rubyObject);
        }

        if (javaClass.isPrimitive()) {
            String s = ((RubyString) TypeConverter.convertToType(rubyObject, runtime.getString(), "to_s", true)).getUnicodeValue();
            if ( s.length() > 0 ) return s.charAt(0);
            return '\0';
        }
        if (javaClass == String.class) {
            RubyString rubyString = (RubyString) rubyObject.callMethod(context, "to_s");
            ByteList bytes = rubyString.getByteList();
            return RubyEncoding.decodeUTF8(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        }
        if (javaClass == ByteList.class) {
            return rubyObject.convertToString().getByteList();
        }
        if (javaClass == BigInteger.class) {
         	if ( rubyObject instanceof RubyBignum ) {
         		return ((RubyBignum) rubyObject).getValue();
         	}
            if ( rubyObject instanceof RubyNumeric ) {
 				return BigInteger.valueOf( ((RubyNumeric) rubyObject).getLongValue() );
         	}
            if ( rubyObject.respondsTo("to_i") ) {
         		RubyNumeric rubyNumeric = ((RubyNumeric) rubyObject.callMethod(context, "to_f"));
 				return  BigInteger.valueOf( rubyNumeric.getLongValue() );
         	}
        }
        if (javaClass == BigDecimal.class && !(rubyObject instanceof JavaObject)) {
         	if (rubyObject.respondsTo("to_f")) {
             	double double_value = ((RubyNumeric)rubyObject.callMethod(context, "to_f")).getDoubleValue();
             	return new BigDecimal(double_value);
         	}
        }

        try {
            if ( isDuckTypeConvertable(rubyObject.getClass(), javaClass) ) {
                return convertProcToInterface(context, (RubyObject) rubyObject, javaClass);
            }
            return ((JavaObject) rubyObject).getValue();
        }
        catch (ClassCastException ex) {
            if (runtime.getDebug().isTrue()) ex.printStackTrace();
            return null;
        }
    }

    @Deprecated
    public static byte convertRubyToJavaByte(IRubyObject rubyObject) {
        return (Byte) convertRubyToJava(rubyObject, byte.class);
    }

    @Deprecated
    public static short convertRubyToJavaShort(IRubyObject rubyObject) {
        return (Short) convertRubyToJava(rubyObject, short.class);
    }

    @Deprecated
    public static char convertRubyToJavaChar(IRubyObject rubyObject) {
        return (Character) convertRubyToJava(rubyObject, char.class);
    }

    @Deprecated
    public static int convertRubyToJavaInt(IRubyObject rubyObject) {
        return (Integer) convertRubyToJava(rubyObject, int.class);
    }

    @Deprecated
    public static long convertRubyToJavaLong(IRubyObject rubyObject) {
        return (Long) convertRubyToJava(rubyObject, long.class);
    }

    @Deprecated
    public static float convertRubyToJavaFloat(IRubyObject rubyObject) {
        return (Float) convertRubyToJava(rubyObject, float.class);
    }

    @Deprecated
    public static double convertRubyToJavaDouble(IRubyObject rubyObject) {
        return (Double) convertRubyToJava(rubyObject, double.class);
    }

    @Deprecated
    public static boolean convertRubyToJavaBoolean(IRubyObject rubyObject) {
        return (Boolean) convertRubyToJava(rubyObject, boolean.class);
    }

    @Deprecated
    public static Object convertArgumentToType(ThreadContext context, IRubyObject arg, Class target) {
        return arg.toJava(target);
    }

    @Deprecated
    public static Object coerceNilToType(RubyNil nil, Class target) {
        return nil.toJava(target);
    }

    @Deprecated
    public static final RubyConverter RUBY_BOOLEAN_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.isTrue();
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_BYTE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return (byte) ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue();
            }
            return (byte) 0;
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_SHORT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return (short) ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue();
            }
            return (short) 0;
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_CHAR_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return (char) ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue();
            }
            return (char) 0;
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_INTEGER_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return (int) ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue();
            }
            return (int) 0;
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_LONG_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue();
            }
            return 0L;
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_FLOAT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return (float) ((RubyNumeric) rubyObject.callMethod(context, "to_f")).getDoubleValue();
            }
            return 0.0f;
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_DOUBLE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return ((RubyNumeric) rubyObject.callMethod(context, "to_f")).getDoubleValue();
            }
            return 0.0d;
        }
    };

    @Deprecated
    public static final Map<Class, RubyConverter> RUBY_CONVERTERS = new HashMap<>(16, 1);
    static {
        RUBY_CONVERTERS.put(Boolean.class, RUBY_BOOLEAN_CONVERTER);
        RUBY_CONVERTERS.put(Boolean.TYPE, RUBY_BOOLEAN_CONVERTER);
        RUBY_CONVERTERS.put(Byte.class, RUBY_BYTE_CONVERTER);
        RUBY_CONVERTERS.put(Byte.TYPE, RUBY_BYTE_CONVERTER);
        RUBY_CONVERTERS.put(Short.class, RUBY_SHORT_CONVERTER);
        RUBY_CONVERTERS.put(Short.TYPE, RUBY_SHORT_CONVERTER);
        RUBY_CONVERTERS.put(Integer.class, RUBY_INTEGER_CONVERTER);
        RUBY_CONVERTERS.put(Integer.TYPE, RUBY_INTEGER_CONVERTER);
        RUBY_CONVERTERS.put(Long.class, RUBY_LONG_CONVERTER);
        RUBY_CONVERTERS.put(Long.TYPE, RUBY_LONG_CONVERTER);
        RUBY_CONVERTERS.put(Float.class, RUBY_FLOAT_CONVERTER);
        RUBY_CONVERTERS.put(Float.TYPE, RUBY_FLOAT_CONVERTER);
        RUBY_CONVERTERS.put(Double.class, RUBY_DOUBLE_CONVERTER);
        RUBY_CONVERTERS.put(Double.TYPE, RUBY_DOUBLE_CONVERTER);
    }

    @Deprecated
    public static IRubyObject convertJavaToRuby(Ruby runtime, JavaConverter converter, Object object) {
        if (converter == null || converter == JAVA_DEFAULT_CONVERTER) {
            return Java.getInstance(runtime, object);
        }
        return converter.convert(runtime, object);
    }

    @Deprecated
    public interface RubyConverter {
        public Object convert(ThreadContext context, IRubyObject rubyObject);
    }

    @Deprecated
    public static final RubyConverter ARRAY_BOOLEAN_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Boolean.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_BYTE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Byte.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_SHORT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Short.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_CHAR_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Character.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_INT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Integer.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_LONG_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Long.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_FLOAT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Float.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_DOUBLE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Double.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_OBJECT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Object.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_CLASS_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(Class.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_STRING_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(String.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_BIGINTEGER_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(BigInteger.class);
        }
    };

    @Deprecated
    public static final RubyConverter ARRAY_BIGDECIMAL_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return rubyObject.toJava(BigDecimal.class);
        }
    };

    @Deprecated
    public static final Map<Class, RubyConverter> ARRAY_CONVERTERS = new HashMap<>(24, 1);
    static {
        ARRAY_CONVERTERS.put(Boolean.class, ARRAY_BOOLEAN_CONVERTER);
        ARRAY_CONVERTERS.put(Boolean.TYPE, ARRAY_BOOLEAN_CONVERTER);
        ARRAY_CONVERTERS.put(Byte.class, ARRAY_BYTE_CONVERTER);
        ARRAY_CONVERTERS.put(Byte.TYPE, ARRAY_BYTE_CONVERTER);
        ARRAY_CONVERTERS.put(Short.class, ARRAY_SHORT_CONVERTER);
        ARRAY_CONVERTERS.put(Short.TYPE, ARRAY_SHORT_CONVERTER);
        ARRAY_CONVERTERS.put(Character.class, ARRAY_CHAR_CONVERTER);
        ARRAY_CONVERTERS.put(Character.TYPE, ARRAY_CHAR_CONVERTER);
        ARRAY_CONVERTERS.put(Integer.class, ARRAY_INT_CONVERTER);
        ARRAY_CONVERTERS.put(Integer.TYPE, ARRAY_INT_CONVERTER);
        ARRAY_CONVERTERS.put(Long.class, ARRAY_LONG_CONVERTER);
        ARRAY_CONVERTERS.put(Long.TYPE, ARRAY_LONG_CONVERTER);
        ARRAY_CONVERTERS.put(Float.class, ARRAY_FLOAT_CONVERTER);
        ARRAY_CONVERTERS.put(Float.TYPE, ARRAY_FLOAT_CONVERTER);
        ARRAY_CONVERTERS.put(Double.class, ARRAY_DOUBLE_CONVERTER);
        ARRAY_CONVERTERS.put(Double.TYPE, ARRAY_DOUBLE_CONVERTER);
        ARRAY_CONVERTERS.put(String.class, ARRAY_STRING_CONVERTER);
        ARRAY_CONVERTERS.put(Class.class, ARRAY_CLASS_CONVERTER);
        ARRAY_CONVERTERS.put(BigInteger.class, ARRAY_BIGINTEGER_CONVERTER);
        ARRAY_CONVERTERS.put(BigDecimal.class, ARRAY_BIGDECIMAL_CONVERTER);
    }

    @Deprecated
    public static RubyConverter getArrayConverter(Class type) {
        RubyConverter converter = ARRAY_CONVERTERS.get(type);
        if (converter == null) {
            return ARRAY_OBJECT_CONVERTER;
        }
        return converter;
    }

    /**
     * High-level object conversion utility.
     */
    @Deprecated
    public static IRubyObject ruby_to_java(final IRubyObject recv, IRubyObject object, Block unusedBlock) {
        if (object.respondsTo("to_java_object")) {
            IRubyObject result = (IRubyObject)object.dataGetStruct();
            if (result == null) {
                result = object.callMethod(recv.getRuntime().getCurrentContext(), "to_java_object");
            }
            if (result instanceof JavaObject) {
                recv.getRuntime().getJavaSupport().getObjectProxyCache().put(((JavaObject) result).getValue(), object);
            }
            return result;
        }

        return primitive_to_java(recv, object, unusedBlock);
    }

    @Deprecated
    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        if (object instanceof JavaObject) {
            return JavaUtil.convertJavaToRuby(recv.getRuntime(), ((JavaObject) object).getValue());
        }

        return object;
    }

    @Deprecated
    public static IRubyObject primitive_to_java(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        if (object instanceof JavaObject) {
            return object;
        }
        Ruby runtime = recv.getRuntime();
        Object javaObject;
        switch (object.getMetaClass().getClassIndex()) {
        case NIL:
            javaObject = null;
            break;
        case INTEGER:
            if (object instanceof RubyFixnum) {
                javaObject = Long.valueOf(((RubyFixnum) object).getLongValue());
            } else {
                javaObject = ((RubyBignum) object).getValue();
            }
            break;
        case FLOAT:
            javaObject = new Double(((RubyFloat) object).getValue());
            break;
        case STRING:
            ByteList bytes = ((RubyString) object).getByteList();
            javaObject = RubyEncoding.decodeUTF8(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
            break;
        case TRUE:
            javaObject = Boolean.TRUE;
            break;
        case FALSE:
            javaObject = Boolean.FALSE;
            break;
        case TIME:
            javaObject = ((RubyTime) object).getJavaDate();
            break;
        default:
            // it's not one of the types we convert, so just pass it out as-is without wrapping
            return object;
        }

        // we've found a Java type to which we've coerced the Ruby value, wrap it
        return JavaObject.wrap(runtime, javaObject);
    }

    @Deprecated
    public static Object convertArgument(Ruby runtime, Object argument, Class<?> parameterType) {
        if (argument == null) {
          if(parameterType.isPrimitive()) {
            throw runtime.newTypeError("primitives do not accept null");
          } else {
            return null;
          }
        }

        if (argument instanceof JavaObject) {
            argument = ((JavaObject) argument).getValue();
            if (argument == null) {
                return null;
            }
        }
        Class<?> type = primitiveToWrapper(parameterType);

        if (argument.getClass() == type) return argument;

        if (type == Void.class) {
            return null;
        }

        if (argument instanceof Number) {
            final Number number = (Number) argument;
            if (type == Long.class) {
                return number.longValue();
            } else if (type == Integer.class) {
                return number.intValue();
            } else if (type == Byte.class) {
                return number.byteValue();
            } else if (type == Character.class) {
                return (char)number.intValue();
            } else if (type == Double.class) {
                return number.doubleValue();
            } else if (type == Float.class) {
                return number.floatValue();
            } else if (type == Short.class) {
                return number.shortValue();
            }
        }
        if (isDuckTypeConvertable(argument.getClass(), parameterType)) {
            RubyObject rubyObject = (RubyObject) argument;
            if (!rubyObject.respondsTo("java_object")) {
                return convertProcToInterface(runtime.getCurrentContext(), rubyObject, parameterType);
            }
        }
        return argument;
    }

    /**
     * High-level object conversion utility function 'java_to_primitive' is the low-level version
     */
    @Deprecated
    public static IRubyObject java_to_ruby(Ruby runtime, IRubyObject object) {
        if (object instanceof JavaObject) {
            return JavaUtil.convertJavaToUsableRubyObject(runtime, ((JavaObject) object).getValue());
        }
        return object;
    }

    // FIXME: This doesn't actually support anything but String
    @Deprecated
    public static Object coerceStringToType(RubyString string, Class target) {
        try {
            ByteList bytes = string.getByteList();

            // 1.9 support for encodings
            // TODO: Fix charset use for JRUBY-4553
            return new String(bytes.getUnsafeBytes(), bytes.begin(), bytes.length(), string.getEncoding().toString());
        } catch (UnsupportedEncodingException uee) {
            return string.toString();
        }
    }

    @Deprecated
    public static Object coerceOtherToType(ThreadContext context, IRubyObject arg, Class target) {
        if (isDuckTypeConvertable(arg.getClass(), target)) {
            RubyObject rubyObject = (RubyObject) arg;
            if (!rubyObject.respondsTo("java_object")) {
                return convertProcToInterface(context, rubyObject, target);
            }
        }

        // it's either as converted as we can make it via above logic or it's
        // not one of the types we convert, so just pass it out as-is without wrapping
        return arg;
    }

    @Deprecated
    public static Object coerceJavaObjectToType(ThreadContext context, Object javaObject, Class target) {
        if (javaObject != null && isDuckTypeConvertable(javaObject.getClass(), target)) {
            RubyObject rubyObject = (RubyObject) javaObject;
            if (!rubyObject.respondsTo("java_object")) {
                return convertProcToInterface(context, rubyObject, target);
            }

            // can't be converted any more, return it
            return javaObject;
        } else {
            return javaObject;
        }
    }

    @Deprecated
    public static JavaObject unwrapJavaObject(Ruby runtime, IRubyObject convertee, String errorMessage) {
        IRubyObject obj = convertee;
        if(!(obj instanceof JavaObject)) {
            if (obj.dataGetStruct() != null && (obj.dataGetStruct() instanceof JavaObject)) {
                obj = (JavaObject)obj.dataGetStruct();
            } else {
                throw runtime.newTypeError(errorMessage);
            }
        }
        return (JavaObject)obj;
    }

    @Deprecated
    public static final boolean CAN_SET_ACCESSIBLE = true;
}
