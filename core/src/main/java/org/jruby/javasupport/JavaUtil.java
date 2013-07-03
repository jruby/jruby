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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import static java.lang.Character.isLetter;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isDigit;
import static java.lang.Character.toLowerCase;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
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
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.java.proxies.RubyObjectHolderProxy;
import org.jruby.javasupport.proxy.InternalJavaProxy;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.TypeConverter;

public class JavaUtil {
    public static IRubyObject[] convertJavaArrayToRuby(Ruby runtime, Object[] objects) {
        if (objects == null) return IRubyObject.NULL_ARRAY;
        
        IRubyObject[] rubyObjects = new IRubyObject[objects.length];
        for (int i = 0; i < objects.length; i++) {
            rubyObjects[i] = convertJavaToUsableRubyObject(runtime, objects[i]);
        }
        return rubyObjects;
    }

    public static RubyArray convertJavaArrayToRubyWithNesting(ThreadContext context, Object array) {
        int length = Array.getLength(array);
        RubyArray outer = context.runtime.newArray(length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            if (element instanceof ArrayJavaProxy) {
                outer.append(convertJavaArrayToRubyWithNesting(context, ((ArrayJavaProxy)element).getObject()));
            } else {
                outer.append(JavaUtil.convertJavaToUsableRubyObject(context.runtime, element));
            }
        }
        return outer;
    }

    public static JavaConverter getJavaConverter(Class clazz) {
        JavaConverter converter = JAVA_CONVERTERS.get(clazz);

        if (converter == null) {
            converter = JAVA_DEFAULT_CONVERTER;
        }

        return converter;
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
            IRubyObject x = convertJavaToUsableRubyObject(runtime, ((Object[])array)[i]);
            return x;
        }
        return converter.get(runtime, array, i);
    }

    public static Class<?> primitiveToWrapper(Class<?> type) {
        if (type.isPrimitive()) {
            return CodegenUtils.getBoxType(type);
        }
        return type;
    }

    public static boolean isDuckTypeConvertable(Class providedArgumentType, Class parameterType) {
        return
                parameterType.isInterface() &&
                !parameterType.isAssignableFrom(providedArgumentType) &&
                RubyObject.class.isAssignableFrom(providedArgumentType);
    }

    public static Object convertProcToInterface(ThreadContext context, RubyObject rubyObject, Class target) {
        return convertProcToInterface(context, (RubyBasicObject)rubyObject, target);
    }

    public static Object convertProcToInterface(ThreadContext context, RubyBasicObject rubyObject, Class target) {
        Ruby runtime = context.runtime;
        RubyModule javaInterfaceModule = (RubyModule)Java.get_interface_module(runtime, JavaClass.get(runtime, target));
        if (!((RubyModule) javaInterfaceModule).isInstance(rubyObject)) {
            javaInterfaceModule.callMethod(context, "extend_object", rubyObject);
            javaInterfaceModule.callMethod(context, "extended", rubyObject);
        }

        if (rubyObject instanceof RubyProc) {
            // Proc implementing an interface, pull in the catch-all code that lets the proc get invoked
            // no matter what method is called on the interface
            RubyClass singletonClass = rubyObject.getSingletonClass();

            singletonClass.addMethod("method_missing", new DynamicMethod(singletonClass, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    if (!(self instanceof RubyProc)) {
                        throw context.runtime.newTypeError("interface impl method_missing for block used with non-Proc object");
                    }
                    RubyProc proc = (RubyProc)self;
                    IRubyObject[] newArgs;
                    if (args.length == 1) {
                        newArgs = IRubyObject.NULL_ARRAY;
                    } else {
                        newArgs = new IRubyObject[args.length - 1];
                        System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    }
                    return proc.call(context, newArgs);
                }

                @Override
                public DynamicMethod dup() {
                    return this;
                }
            });
        }
        JavaObject jo = (JavaObject) Helpers.invoke(context, rubyObject, "__jcreate_meta!");
        return jo.getValue();
    }

    public static NumericConverter getNumericConverter(Class target) {
        NumericConverter converter = NUMERIC_CONVERTERS.get(target);
        if (converter == null) {
            return NUMERIC_TO_OTHER;
        }
        return converter;
    }

    public static boolean isJavaObject(IRubyObject candidate) {
        return candidate instanceof JavaProxy || candidate.dataGetStruct() instanceof JavaObject;
    }

    public static Object unwrapJavaObject(IRubyObject object) {
        if (object instanceof JavaProxy) {
            return ((JavaProxy)object).getObject();
        }
        return ((JavaObject)object.dataGetStruct()).getValue();
    }

    public static Object unwrapJavaValue(Ruby runtime, IRubyObject obj, String errorMessage) {
        if (obj instanceof JavaProxy) {
            return ((JavaProxy)obj).getObject();
        } else if (obj instanceof JavaObject) {
            return ((JavaObject)obj).getValue();
        } else if(obj.dataGetStruct() != null && (obj.dataGetStruct() instanceof IRubyObject)) {
            return unwrapJavaValue(runtime, ((IRubyObject)obj.dataGetStruct()), errorMessage);
        } else {
            throw runtime.newTypeError(errorMessage);
        }
    }

    /**
     * For methods that match /(get|set|is)([A-Z0-9])(.*)/, return the "name"
     * part of the property with leading lower-case.
     *
     * Does not use regex for performance reasons.
     *
     * @param beanMethodName the bean method from which to extract a name
     * @return the bean property name
     */
    public static String getJavaPropertyName(String beanMethodName) {
        int length = beanMethodName.length();
        char ch;
        if ((beanMethodName.startsWith("get") || beanMethodName.startsWith("set")) && length > 3) {
            if (isUpperDigit(ch = beanMethodName.charAt(3))) {
                if (length == 4) {
                    return Character.toString(toLowerCase(ch));
                } else {
                    return "" + toLowerCase(ch) + beanMethodName.substring(4);
                }
            }
        } else if (beanMethodName.startsWith("is") && length > 2) {
            if (isUpperDigit(ch = beanMethodName.charAt(2))) {
                if (length == 3) {
                    return Character.toString(toLowerCase(ch));
                } else {
                    return "" + toLowerCase(ch) + beanMethodName.substring(3);
                }
            }
        }
        return null;
    }

    /**
     * For methods that match /(([a-z0-9])([A-Z])|([A-Za-z0-9])([A-Z][a-z]))/,
     * return the snake-cased equivalent (inserting _ between groups 2 and 3 or
     * 4 and 5).
     *
     * Does not use regex for performance reasons.
     *
     * @param javaCasedName the camelCased name to convert
     * @return the snake_cased result
     */
    public static String getRubyCasedName(String javaCasedName) {
        StringBuilder b = new StringBuilder();
        char[] chars = javaCasedName.toCharArray();
        int behind = 0;
        for (int i = 0; i < chars.length; i++) {
            if (behind < 2) {
                behind++;
            } else {
                behind = consume(b, chars, i);
            }
        }

        if (behind == 2) {
            b.append(toLowerCase(chars[chars.length - 2]));
            if (isUpperCase(chars[chars.length - 1]) && !isUpperCase(chars[chars.length - 2])) b.append('_');
            b.append(toLowerCase(chars[chars.length - 1]));
        } else if (behind > 0) {
            if (behind > 1) {
                b.append(toLowerCase(chars[chars.length - 2]));
            }
            b.append(toLowerCase(chars[chars.length - 1]));
        }
        return b.toString();
    }
    
    private static int consume(StringBuilder b, char[] chars, int i) {
        char cur, prev, prev2;
        if (isLowerDigit(prev2 = chars[i - 2]) && isUpperCase(prev = chars[i - 1])) {
            b.append(prev2).append('_').append(toLowerCase(prev));
            return 1;
        } else if (isLetterDigit(prev2) && isUpperCase(prev = chars[i - 1]) && isLowerCase(cur = chars[i])) {
            b.append(toLowerCase(prev2)).append('_').append(toLowerCase(prev)).append(cur);
            return 0;
        } else {
            b.append(toLowerCase(prev2));
            return 2;
        }
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
     */
    public static Set<String> getRubyNamesForJavaName(String javaName, List<Method> methods) {
        String javaPropertyName = JavaUtil.getJavaPropertyName(javaName);
        String rubyName = JavaUtil.getRubyCasedName(javaName);
        Set<String> nameSet = new LinkedHashSet<String>();
        nameSet.add(javaName);
        nameSet.add(rubyName);
        String rubyPropertyName = null;
        for (Method method: methods) {
            Class<?>[] argTypes = method.getParameterTypes();
            Class<?> resultType = method.getReturnType();
            int argCount = argTypes.length;

            // Add property name aliases
            if (javaPropertyName != null) {
                if (rubyName.startsWith("get_")) {
                    rubyPropertyName = rubyName.substring(4);
                    if (argCount == 0 ||                                // getFoo      => foo
                        argCount == 1 && argTypes[0] == int.class) {    // getFoo(int) => foo(int)

                        nameSet.add(javaPropertyName);
                        nameSet.add(rubyPropertyName);
                        if (resultType == boolean.class) {              // getFooBar() => fooBar?, foo_bar?(*)
                            nameSet.add(javaPropertyName + '?');
                            nameSet.add(rubyPropertyName + '?');
                        }
                    }
                } else if (rubyName.startsWith("set_")) {
                    rubyPropertyName = rubyName.substring(4);
                    if (argCount == 1 && resultType == void.class) {    // setFoo(Foo) => foo=(Foo)
                        nameSet.add(javaPropertyName + '=');
                        nameSet.add(rubyPropertyName + '=');
                    }
                } else if (rubyName.startsWith("is_")) {
                    rubyPropertyName = rubyName.substring(3);
                    if (resultType == boolean.class) {                  // isFoo() => foo, isFoo(*) => foo(*)
                        nameSet.add(javaPropertyName);
                        nameSet.add(rubyPropertyName);
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

        return nameSet;
    }
    
    public static abstract class JavaConverter {
        private final Class type;
        public JavaConverter(Class type) {this.type = type;}
        public abstract IRubyObject convert(Ruby runtime, Object object);
        public abstract IRubyObject get(Ruby runtime, Object array, int i);
        public abstract void set(Ruby runtime, Object array, int i, IRubyObject value);
        public String toString() {return type.getName() + " converter";}
    }

    public interface NumericConverter {
        public Object coerce(RubyNumeric numeric, Class target);
    }

    private static IRubyObject trySimpleConversions(Ruby runtime, Object object) {
        if (object == null) {
            return runtime.getNil();
        }

        if (object instanceof IRubyObject) {
            return (IRubyObject) object;
        }

        if (object instanceof RubyObjectHolderProxy) {
            return ((RubyObjectHolderProxy) object).__ruby_object();
        }

        if (object instanceof InternalJavaProxy) {
            InternalJavaProxy internalJavaProxy = (InternalJavaProxy) object;
            IRubyObject orig = internalJavaProxy.___getInvocationHandler().getOrig();

            if (orig != null) {
                return orig;
            }
        }

        return null;
    }
    
    private static final JavaConverter JAVA_DEFAULT_CONVERTER = new JavaConverter(Object.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            IRubyObject result = trySimpleConversions(runtime, object);

            if (result != null) return result;
            
            return JavaObject.wrap(runtime, object);
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((Object[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((Object[])array)[i] = value.toJava(Object.class);
        }
    };
    
    private static final JavaConverter JAVA_BOOLEAN_CONVERTER = new JavaConverter(Boolean.class) {
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
    
    private static final JavaConverter JAVA_FLOAT_CONVERTER = new JavaConverter(Float.class) {
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
    
    private static final JavaConverter JAVA_DOUBLE_CONVERTER = new JavaConverter(Double.class) {
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
    
    private static final JavaConverter JAVA_CHAR_CONVERTER = new JavaConverter(Character.class) {
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
    
    private static final JavaConverter JAVA_BYTE_CONVERTER = new JavaConverter(Byte.class) {
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
    
    private static final JavaConverter JAVA_SHORT_CONVERTER = new JavaConverter(Short.class) {
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
    
    private static final JavaConverter JAVA_INT_CONVERTER = new JavaConverter(Integer.class) {
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
    
    private static final JavaConverter JAVA_LONG_CONVERTER = new JavaConverter(Long.class) {
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

    private static final JavaConverter JAVA_BOOLEANPRIM_CONVERTER = new JavaConverter(boolean.class) {
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

    private static final JavaConverter JAVA_FLOATPRIM_CONVERTER = new JavaConverter(float.class) {
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

    private static final JavaConverter JAVA_DOUBLEPRIM_CONVERTER = new JavaConverter(double.class) {
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

    private static final JavaConverter JAVA_CHARPRIM_CONVERTER = new JavaConverter(char.class) {
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

    private static final JavaConverter JAVA_BYTEPRIM_CONVERTER = new JavaConverter(byte.class) {
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

    private static final JavaConverter JAVA_SHORTPRIM_CONVERTER = new JavaConverter(short.class) {
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

    private static final JavaConverter JAVA_INTPRIM_CONVERTER = new JavaConverter(int.class) {
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

    private static final JavaConverter JAVA_LONGPRIM_CONVERTER = new JavaConverter(long.class) {
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
    
    private static final JavaConverter JAVA_STRING_CONVERTER = new JavaConverter(String.class) {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyString.newString(runtime, (String)object);
        }
        public IRubyObject get(Ruby runtime, Object array, int i) {
            return convert(runtime, ((String[]) array)[i]);
        }
        public void set(Ruby runtime, Object array, int i, IRubyObject value) {
            ((String[])array)[i] = (String)value.toJava(String.class);
        }
    };
    
    private static final JavaConverter JAVA_CHARSEQUENCE_CONVERTER = new JavaConverter(String.class) {
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
    
    private static final JavaConverter BYTELIST_CONVERTER = new JavaConverter(ByteList.class) {
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
    
    private static final JavaConverter JAVA_BIGINTEGER_CONVERTER = new JavaConverter(BigInteger.class) {
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
    
    private static final Map<Class,JavaConverter> JAVA_CONVERTERS =
        new HashMap<Class,JavaConverter>();
    
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

    private static final NumericConverter NUMERIC_TO_BYTE = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            long value = numeric.getLongValue();
            if (isLongByteable(value)) {
                return Byte.valueOf((byte)value);
            }
            throw numeric.getRuntime().newRangeError("too big for byte: " + numeric);
        }
    };
    private static final NumericConverter NUMERIC_TO_SHORT = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            long value = numeric.getLongValue();
            if (isLongShortable(value)) {
                return Short.valueOf((short)value);
            }
            throw numeric.getRuntime().newRangeError("too big for short: " + numeric);
        }
    };
    private static final NumericConverter NUMERIC_TO_CHARACTER = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            long value = numeric.getLongValue();
            if (isLongCharable(value)) {
                return Character.valueOf((char)value);
            }
            throw numeric.getRuntime().newRangeError("too big for char: " + numeric);
        }
    };
    private static final NumericConverter NUMERIC_TO_INTEGER = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            long value = numeric.getLongValue();
            if (isLongIntable(value)) {
                return Integer.valueOf((int)value);
            }
            throw numeric.getRuntime().newRangeError("too big for int: " + numeric);
        }
    };
    private static final NumericConverter NUMERIC_TO_LONG = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            return Long.valueOf(numeric.getLongValue());
        }
    };
    private static final NumericConverter NUMERIC_TO_FLOAT = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            double value = numeric.getDoubleValue();
            // many cases are ok to convert to float; if not one of these, error
            if (isDoubleFloatable(value)) {
                return Float.valueOf((float)value);
            } else {
                throw numeric.getRuntime().newTypeError("too big for float: " + numeric);
            }
        }
    };
    private static final NumericConverter NUMERIC_TO_DOUBLE = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            return Double.valueOf(numeric.getDoubleValue());
        }
    };
    private static final NumericConverter NUMERIC_TO_BIGINTEGER = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            return numeric.getBigIntegerValue();
        }
    };
    private static final NumericConverter NUMERIC_TO_OBJECT = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
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
        }
    };
    private static final NumericConverter NUMERIC_TO_OTHER = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            if (target.isAssignableFrom(numeric.getClass())) {
                // just return as-is, since we can't do any coercion
                return numeric;
            }
            // otherwise, error; no conversion available
            throw numeric.getRuntime().newTypeError("could not coerce " + numeric.getMetaClass() + " to " + target);
        }
    };
    private static final NumericConverter NUMERIC_TO_VOID = new NumericConverter() {
        public Object coerce(RubyNumeric numeric, Class target) {
            return null;
        }
    };
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
    
    private static final Map<Class, NumericConverter> NUMERIC_CONVERTERS = new HashMap<Class, NumericConverter>();

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

    @Deprecated
    public static Object convertRubyToJava(IRubyObject rubyObject) {
        return convertRubyToJava(rubyObject, Object.class);
    }

    @Deprecated
    public static Object convertRubyToJava(IRubyObject rubyObject, Class javaClass) {
        if (javaClass == void.class || rubyObject == null || rubyObject.isNil()) {
            return null;
        }

        ThreadContext context = rubyObject.getRuntime().getCurrentContext();
        IRubyObject origObject = rubyObject;
        if (rubyObject.dataGetStruct() instanceof JavaObject) {
            rubyObject = (IRubyObject)rubyObject.dataGetStruct();
            if(rubyObject == null) {
                throw new RuntimeException("dataGetStruct returned null for " + origObject.getType().getName());
            }
        } else if (rubyObject.respondsTo("java_object")) {
            rubyObject = rubyObject.callMethod(context, "java_object");
            if(rubyObject == null) {
                throw new RuntimeException("java_object returned null for " + origObject.getType().getName());
            }
        }

        if (rubyObject instanceof JavaObject) {
            Object value =  ((JavaObject) rubyObject).getValue();

            return convertArgument(rubyObject.getRuntime(), value, value.getClass());

        } else if (javaClass == Object.class || javaClass == null) {
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
            String s = ((RubyString)TypeConverter.convertToType(rubyObject, rubyObject.getRuntime().getString(), "to_s", true)).getUnicodeValue();
            if (s.length() > 0) {
                return Character.valueOf(s.charAt(0));
            }
            return Character.valueOf('\0');
        } else if (javaClass == String.class) {
            RubyString rubyString = (RubyString) rubyObject.callMethod(context, "to_s");
            ByteList bytes = rubyString.getByteList();
            return RubyEncoding.decodeUTF8(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        } else if (javaClass == ByteList.class) {
            return rubyObject.convertToString().getByteList();
        } else if (javaClass == BigInteger.class) {
         	if (rubyObject instanceof RubyBignum) {
         		return ((RubyBignum)rubyObject).getValue();
         	} else if (rubyObject instanceof RubyNumeric) {
 				return  BigInteger.valueOf (((RubyNumeric)rubyObject).getLongValue());
         	} else if (rubyObject.respondsTo("to_i")) {
         		RubyNumeric rubyNumeric = ((RubyNumeric)rubyObject.callMethod(context, "to_f"));
 				return  BigInteger.valueOf (rubyNumeric.getLongValue());
         	}
        } else if (javaClass == BigDecimal.class && !(rubyObject instanceof JavaObject)) {
         	if (rubyObject.respondsTo("to_f")) {
             	double double_value = ((RubyNumeric)rubyObject.callMethod(context, "to_f")).getDoubleValue();
             	return new BigDecimal(double_value);
         	}
        }

        try {
            if (isDuckTypeConvertable(rubyObject.getClass(), javaClass)) {
                return convertProcToInterface(context, (RubyObject) rubyObject, javaClass);
            }
            return ((JavaObject) rubyObject).getValue();
        } catch (ClassCastException ex) {
            if (rubyObject.getRuntime().getDebug().isTrue()) ex.printStackTrace();
            return null;
        }
    }

    @Deprecated
    public static byte convertRubyToJavaByte(IRubyObject rubyObject) {
        return ((Byte)convertRubyToJava(rubyObject, byte.class)).byteValue();
    }

    @Deprecated
    public static short convertRubyToJavaShort(IRubyObject rubyObject) {
        return ((Short)convertRubyToJava(rubyObject, short.class)).shortValue();
    }

    @Deprecated
    public static char convertRubyToJavaChar(IRubyObject rubyObject) {
        return ((Character)convertRubyToJava(rubyObject, char.class)).charValue();
    }

    @Deprecated
    public static int convertRubyToJavaInt(IRubyObject rubyObject) {
        return ((Integer)convertRubyToJava(rubyObject, int.class)).intValue();
    }

    @Deprecated
    public static long convertRubyToJavaLong(IRubyObject rubyObject) {
        return ((Long)convertRubyToJava(rubyObject, long.class)).longValue();
    }

    @Deprecated
    public static float convertRubyToJavaFloat(IRubyObject rubyObject) {
        return ((Float)convertRubyToJava(rubyObject, float.class)).floatValue();
    }

    @Deprecated
    public static double convertRubyToJavaDouble(IRubyObject rubyObject) {
        return ((Double)convertRubyToJava(rubyObject, double.class)).doubleValue();
    }

    @Deprecated
    public static boolean convertRubyToJavaBoolean(IRubyObject rubyObject) {
        return ((Boolean)convertRubyToJava(rubyObject, boolean.class)).booleanValue();
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
            return Boolean.valueOf(rubyObject.isTrue());
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_BYTE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return Byte.valueOf((byte) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return Byte.valueOf((byte) 0);
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_SHORT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return Short.valueOf((short) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return Short.valueOf((short) 0);
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_CHAR_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return Character.valueOf((char) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return Character.valueOf((char) 0);
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_INTEGER_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return Integer.valueOf((int) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return Integer.valueOf(0);
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_LONG_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return Long.valueOf(((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return Long.valueOf(0);
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_FLOAT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return new Float((float) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_f")).getDoubleValue());
            }
            return new Float(0.0);
        }
    };

    @Deprecated
    public static final RubyConverter RUBY_DOUBLE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return new Double(((RubyNumeric) rubyObject.callMethod(
                        context, "to_f")).getDoubleValue());
            }
            return new Double(0.0);
        }
    };

    @Deprecated
    public static final Map<Class, RubyConverter> RUBY_CONVERTERS = new HashMap<Class, RubyConverter>();
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
    public static final Map<Class, RubyConverter> ARRAY_CONVERTERS = new HashMap<Class, RubyConverter>();
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
        switch (object.getMetaClass().index) {
        case ClassIndex.NIL:
            javaObject = null;
            break;
        case ClassIndex.FIXNUM:
            javaObject = Long.valueOf(((RubyFixnum) object).getLongValue());
            break;
        case ClassIndex.BIGNUM:
            javaObject = ((RubyBignum) object).getValue();
            break;
        case ClassIndex.FLOAT:
            javaObject = new Double(((RubyFloat) object).getValue());
            break;
        case ClassIndex.STRING:
            ByteList bytes = ((RubyString) object).getByteList();
            javaObject = RubyEncoding.decodeUTF8(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
            break;
        case ClassIndex.TRUE:
            javaObject = Boolean.TRUE;
            break;
        case ClassIndex.FALSE:
            javaObject = Boolean.FALSE;
            break;
        case ClassIndex.TIME:
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
            if (string.getRuntime().is1_9()) {
                return new String(bytes.getUnsafeBytes(), bytes.begin(), bytes.length(), string.getEncoding().toString());
            }

            return RubyEncoding.decodeUTF8(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
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
}
