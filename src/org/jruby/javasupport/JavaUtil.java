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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
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
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

public class JavaUtil {

    public static Object convertRubyToJava(IRubyObject rubyObject) {
        return convertRubyToJava(rubyObject, Object.class);
    }
    
    public interface RubyConverter {
        public Object convert(ThreadContext context, IRubyObject rubyObject);
    }
    
    public static final RubyConverter RUBY_BOOLEAN_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            return Boolean.valueOf(rubyObject.isTrue());
        }
    };
    
    public static final RubyConverter RUBY_BYTE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Byte((byte) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return new Byte((byte) 0);
        }
    };
    
    public static final RubyConverter RUBY_SHORT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Short((short) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return new Short((short) 0);
        }
    };
    
    public static final RubyConverter RUBY_CHAR_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Character((char) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return new Character((char) 0);
        }
    };
    
    public static final RubyConverter RUBY_INTEGER_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Integer((int) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return new Integer(0);
        }
    };
    
    public static final RubyConverter RUBY_LONG_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Long(((RubyNumeric) rubyObject.callMethod(
                        context, "to_i")).getLongValue());
            }
            return new Long(0);
        }
    };
    
    public static final RubyConverter RUBY_FLOAT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return new Float((float) ((RubyNumeric) rubyObject.callMethod(
                        context, "to_f")).getDoubleValue());
            }
            return new Float(0.0);
        }
    };
    
    public static final RubyConverter RUBY_DOUBLE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return new Double(((RubyNumeric) rubyObject.callMethod(
                        context, "to_f")).getDoubleValue());
            }
            return new Double(0.0);
        }
    };
    
    public static final RubyConverter ARRAY_BOOLEAN_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject == context.getRuntime().getFalse() || rubyObject.isNil()) {
                return Boolean.FALSE;
            } else if (rubyObject == context.getRuntime().getTrue()) {
                return Boolean.TRUE;
            } else if (rubyObject instanceof RubyNumeric) {
                return ((RubyNumeric)rubyObject).getLongValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
            } else if (rubyObject instanceof RubyString) {
                return Boolean.valueOf(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return integer.getLongValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_BYTE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return Byte.valueOf((byte)((RubyNumeric)rubyObject).getLongValue());
            } else if (rubyObject instanceof RubyString) {
                return Byte.valueOf(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return Byte.valueOf((byte)integer.getLongValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_SHORT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return Short.valueOf((short)((RubyNumeric)rubyObject).getLongValue());
            } else if (rubyObject instanceof RubyString) {
                return Short.valueOf(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return Short.valueOf((short)integer.getLongValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_CHAR_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return Character.valueOf((char)((RubyNumeric)rubyObject).getLongValue());
            } else if (rubyObject instanceof RubyString) {
                return Character.valueOf(rubyObject.asJavaString().charAt(0));
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return Character.valueOf((char)integer.getLongValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_INT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return Integer.valueOf((int)((RubyNumeric)rubyObject).getLongValue());
            } else if (rubyObject instanceof RubyString) {
                return Integer.valueOf(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return Integer.valueOf((int)integer.getLongValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_LONG_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return Long.valueOf(((RubyNumeric)rubyObject).getLongValue());
            } else if (rubyObject instanceof RubyString) {
                return Long.valueOf(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return Long.valueOf(integer.getLongValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_FLOAT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return Float.valueOf((float)((RubyNumeric)rubyObject).getDoubleValue());
            } else if (rubyObject instanceof RubyString) {
                return Float.valueOf(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return Float.valueOf((float)integer.getDoubleValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_DOUBLE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return Double.valueOf(((RubyNumeric)rubyObject).getDoubleValue());
            } else if (rubyObject instanceof RubyString) {
                return Double.valueOf(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return Double.valueOf(integer.getDoubleValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_OBJECT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyInteger) {
                long value = ((RubyInteger)rubyObject).getLongValue();
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    return Integer.valueOf((int)value);
                } else if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
                    return Long.valueOf(value);
                } else {
                    return new BigInteger(rubyObject.toString());
                }
            } else if (rubyObject instanceof RubyFloat) {
                return Double.valueOf(((RubyFloat)rubyObject).getDoubleValue());
            } else if (rubyObject instanceof JavaProxy) {
                return ((JavaProxy)rubyObject).unwrap();
            } else {
                return convertRubyToJava(rubyObject);
            }
        }
    };
    
    public static final RubyConverter ARRAY_CLASS_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof JavaClass) {
                return ((JavaClass)rubyObject).javaClass();
            } else {
                return convertRubyToJava(rubyObject);
            }
        }
    };

    public static final RubyConverter ARRAY_STRING_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyString) {
                return ((RubyString)rubyObject).getUnicodeValue();
            } else {
                return rubyObject.toString();
            }
        }
    };
    
    public static final RubyConverter ARRAY_BIGINTEGER_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return BigInteger.valueOf(((RubyNumeric)rubyObject).getLongValue());
            } else if (rubyObject instanceof RubyString) {
                return new BigDecimal(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return BigInteger.valueOf(integer.getLongValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final RubyConverter ARRAY_BIGDECIMAL_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject instanceof RubyNumeric) {
                return BigDecimal.valueOf(((RubyNumeric)rubyObject).getDoubleValue());
            } else if (rubyObject instanceof RubyString) {
                return new BigDecimal(rubyObject.asJavaString());
            } else if (rubyObject instanceof JavaProxy) {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            } else if (rubyObject.respondsTo("to_f")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_f");
                return BigDecimal.valueOf(integer.getDoubleValue());
            } else if (rubyObject.respondsTo("to_i")) {
                RubyInteger integer = (RubyInteger)RuntimeHelpers.invoke(context, rubyObject, "to_i");
                return BigDecimal.valueOf(integer.getLongValue());
            } else {
                return ruby_to_java(rubyObject, rubyObject, Block.NULL_BLOCK);
            }
        }
    };
    
    public static final Map<Class, RubyConverter> RUBY_CONVERTERS = new HashMap<Class, RubyConverter>();
    public static final Map<Class, RubyConverter> ARRAY_CONVERTERS = new HashMap<Class, RubyConverter>();
    
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
    
    public static RubyConverter getArrayConverter(Class type) {
        RubyConverter converter = ARRAY_CONVERTERS.get(type);
        if (converter == null) {
            return ARRAY_OBJECT_CONVERTER;
        }
        return converter;
    }
    
    public static byte convertRubyToJavaByte(IRubyObject rubyObject) {
        return ((Byte)convertRubyToJava(rubyObject, byte.class)).byteValue();
    }
    
    public static short convertRubyToJavaShort(IRubyObject rubyObject) {
        return ((Short)convertRubyToJava(rubyObject, short.class)).shortValue();
    }
    
    public static char convertRubyToJavaChar(IRubyObject rubyObject) {
        return ((Character)convertRubyToJava(rubyObject, char.class)).charValue();
    }
    
    public static int convertRubyToJavaInt(IRubyObject rubyObject) {
        return ((Integer)convertRubyToJava(rubyObject, int.class)).intValue();
    }
    
    public static long convertRubyToJavaLong(IRubyObject rubyObject) {
        return ((Long)convertRubyToJava(rubyObject, long.class)).longValue();
    }
    
    public static float convertRubyToJavaFloat(IRubyObject rubyObject) {
        return ((Float)convertRubyToJava(rubyObject, float.class)).floatValue();
    }
    
    public static double convertRubyToJavaDouble(IRubyObject rubyObject) {
        return ((Double)convertRubyToJava(rubyObject, double.class)).doubleValue();
    }
    
    public static boolean convertRubyToJavaBoolean(IRubyObject rubyObject) {
        return ((Boolean)convertRubyToJava(rubyObject, boolean.class)).booleanValue();
    }

    public static Object convertRubyToJava(IRubyObject rubyObject, Class javaClass) {
        if (javaClass == void.class || rubyObject == null || rubyObject.isNil()) {
            return null;
        }
        
        ThreadContext context = rubyObject.getRuntime().getCurrentContext();
        
        if (rubyObject.dataGetStruct() instanceof JavaObject) {
            rubyObject = (JavaObject) rubyObject.dataGetStruct();
        } else if (rubyObject.respondsTo("java_object")) {
            rubyObject = rubyObject.callMethod(context, "java_object");
        } else if (rubyObject.respondsTo("to_java_object")) {
            rubyObject = rubyObject.callMethod(context, "to_java_object");
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
        
        if (javaClass.isPrimitive()) {
            RubyConverter converter = RUBY_CONVERTERS.get(javaClass);
            if (converter != null) {
                return converter.convert(context, rubyObject);
            }

            String s = ((RubyString)TypeConverter.convertToType(rubyObject, rubyObject.getRuntime().getString(), "to_s", true)).getUnicodeValue();
            if (s.length() > 0) {
                return new Character(s.charAt(0));
            }
            return new Character('\0');
        } else if (javaClass == String.class) {
            RubyString rubyString = (RubyString) rubyObject.callMethod(context, "to_s");
            ByteList bytes = rubyString.getByteList();
            try {
                return new String(bytes.unsafeBytes(), bytes.begin(), bytes.length(), "UTF8");
            } catch (UnsupportedEncodingException uee) {
                return new String(bytes.unsafeBytes(), bytes.begin(), bytes.length());
            }
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

    public static IRubyObject[] convertJavaArrayToRuby(Ruby runtime, Object[] objects) {
        if (objects == null) return IRubyObject.NULL_ARRAY;
        
        IRubyObject[] rubyObjects = new IRubyObject[objects.length];
        for (int i = 0; i < objects.length; i++) {
            rubyObjects[i] = convertJavaToRuby(runtime, objects[i]);
        }
        return rubyObjects;
    }
    
    public interface JavaConverter {
        public IRubyObject convert(Ruby runtime, Object object);
    }
    
    public static final JavaConverter JAVA_DEFAULT_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) {
                return runtime.getNil();
            }

            if (object instanceof IRubyObject) {
                return (IRubyObject) object;
            }
 
            // Note: returns JavaObject instance, which is not
            // directly usable. probably too late to change this now,
            // supplying alternate method convertJavaToUsableRubyObject
            return JavaObject.wrap(runtime, object);
        }
    };
    
    public static final JavaConverter JAVA_BOOLEAN_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyBoolean.newBoolean(runtime, ((Boolean)object).booleanValue());
        }
    };
    
    public static final JavaConverter JAVA_FLOAT_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFloat.newFloat(runtime, ((Float)object).doubleValue());
        }
    };
    
    public static final JavaConverter JAVA_DOUBLE_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFloat.newFloat(runtime, ((Double)object).doubleValue());
        }
    };
    
    public static final JavaConverter JAVA_CHAR_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Character)object).charValue());
        }
    };
    
    public static final JavaConverter JAVA_BYTE_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Byte)object).byteValue());
        }
    };
    
    public static final JavaConverter JAVA_SHORT_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Short)object).shortValue());
        }
    };
    
    public static final JavaConverter JAVA_INT_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Integer)object).intValue());
        }
    };
    
    public static final JavaConverter JAVA_LONG_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyFixnum.newFixnum(runtime, ((Long)object).longValue());
        }
    };
    
    public static final JavaConverter JAVA_STRING_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyString.newUnicodeString(runtime, (String)object);
        }
    };
    
    public static final JavaConverter BYTELIST_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyString.newString(runtime, (ByteList)object);
        }
    };
    
    public static final JavaConverter JAVA_BIGINTEGER_CONVERTER = new JavaConverter() {
        public IRubyObject convert(Ruby runtime, Object object) {
            if (object == null) return runtime.getNil();
            return RubyBignum.newBignum(runtime, (BigInteger)object);
        }
    };
    
    private static final Map<Class,JavaConverter> JAVA_CONVERTERS =
        new HashMap<Class,JavaConverter>();
    
    static {
        JAVA_CONVERTERS.put(Byte.class, JAVA_BYTE_CONVERTER);
        JAVA_CONVERTERS.put(Byte.TYPE, JAVA_BYTE_CONVERTER);
        JAVA_CONVERTERS.put(Short.class, JAVA_SHORT_CONVERTER);
        JAVA_CONVERTERS.put(Short.TYPE, JAVA_SHORT_CONVERTER);
        JAVA_CONVERTERS.put(Character.class, JAVA_CHAR_CONVERTER);
        JAVA_CONVERTERS.put(Character.TYPE, JAVA_CHAR_CONVERTER);
        JAVA_CONVERTERS.put(Integer.class, JAVA_INT_CONVERTER);
        JAVA_CONVERTERS.put(Integer.TYPE, JAVA_INT_CONVERTER);
        JAVA_CONVERTERS.put(Long.class, JAVA_LONG_CONVERTER);
        JAVA_CONVERTERS.put(Long.TYPE, JAVA_LONG_CONVERTER);
        JAVA_CONVERTERS.put(Float.class, JAVA_FLOAT_CONVERTER);
        JAVA_CONVERTERS.put(Float.TYPE, JAVA_FLOAT_CONVERTER);
        JAVA_CONVERTERS.put(Double.class, JAVA_DOUBLE_CONVERTER);
        JAVA_CONVERTERS.put(Double.TYPE, JAVA_DOUBLE_CONVERTER);
        JAVA_CONVERTERS.put(Boolean.class, JAVA_BOOLEAN_CONVERTER);
        JAVA_CONVERTERS.put(Boolean.TYPE, JAVA_BOOLEAN_CONVERTER);
        
        JAVA_CONVERTERS.put(String.class, JAVA_STRING_CONVERTER);
        
        JAVA_CONVERTERS.put(ByteList.class, BYTELIST_CONVERTER);
        
        JAVA_CONVERTERS.put(BigInteger.class, JAVA_BIGINTEGER_CONVERTER);

    }
    
    public static JavaConverter getJavaConverter(Class clazz) {
        JavaConverter converter = JAVA_CONVERTERS.get(clazz);
        
        if (converter == null) {
            converter = JAVA_DEFAULT_CONVERTER;
        }
        
        return converter;
    }

    /**
     * Converts object to the corresponding Ruby type; however, for non-primitives,
     * a JavaObject instance is returned. This must be subsequently wrapped by
     * calling one of Java.wrap, Java.java_to_ruby, Java.new_instance_for, or
     * Java.getInstance, depending on context.
     * 
     * @param runtime
     * @param object 
     * @return corresponding Ruby type, or a JavaObject instance
     */
    public static IRubyObject convertJavaToRuby(Ruby runtime, Object object) {
        if (object == null) {
            return runtime.getNil();
        } else if (object instanceof IRubyObject) {
            return (IRubyObject)object;
        }
        return convertJavaToRuby(runtime, object, object.getClass());
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

    public static IRubyObject convertJavaToRuby(Ruby runtime, Object object, Class javaClass) {
        return getJavaConverter(javaClass).convert(runtime, object);
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
        if (object == null) return runtime.getNil();
        
        // if it's already IRubyObject, don't re-wrap (JRUBY-2480)
        if (object instanceof IRubyObject) {
            return (IRubyObject)object;
        }
        
        JavaConverter converter = JAVA_CONVERTERS.get(object.getClass());
        if (converter == null || converter == JAVA_DEFAULT_CONVERTER) {
            return Java.getInstance(runtime, object);
        }
        return converter.convert(runtime, object);
    }

    public static Class<?> primitiveToWrapper(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == Integer.TYPE) {
                return Integer.class;
            } else if (type == Double.TYPE) {
                return Double.class;
            } else if (type == Boolean.TYPE) {
                return Boolean.class;
            } else if (type == Byte.TYPE) {
                return Byte.class;
            } else if (type == Character.TYPE) {
                return Character.class;
            } else if (type == Float.TYPE) {
                return Float.class;
            } else if (type == Long.TYPE) {
                return Long.class;
            } else if (type == Void.TYPE) {
                return Void.class;
            } else if (type == Short.TYPE) {
                return Short.class;
            }
        }
        return type;
    }

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
        if (type == Void.class) {
            return null;
        }
        if (argument instanceof Number) {
            final Number number = (Number) argument;
            if (type == Long.class) {
                return new Long(number.longValue());
            } else if (type == Integer.class) {
                return new Integer(number.intValue());
            } else if (type == Byte.class) {
                return new Byte(number.byteValue());
            } else if (type == Character.class) {
                return new Character((char) number.intValue());
            } else if (type == Double.class) {
                return new Double(number.doubleValue());
            } else if (type == Float.class) {
                return new Float(number.floatValue());
            } else if (type == Short.class) {
                return new Short(number.shortValue());
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
    
    public static boolean isDuckTypeConvertable(Class providedArgumentType, Class parameterType) {
        return parameterType.isInterface() && !parameterType.isAssignableFrom(providedArgumentType) 
            && RubyObject.class.isAssignableFrom(providedArgumentType);
    }
    
    public static Object convertProcToInterface(ThreadContext context, RubyObject rubyObject, Class target) {
        Ruby runtime = context.getRuntime();
        IRubyObject javaUtilities = runtime.getJavaSupport().getJavaUtilitiesModule();
        IRubyObject javaInterfaceModule = Java.get_interface_module(javaUtilities, JavaClass.get(runtime, target));
        if (!((RubyModule) javaInterfaceModule).isInstance(rubyObject)) {
            rubyObject.extend(new IRubyObject[]{javaInterfaceModule});
        }

        if (rubyObject instanceof RubyProc) {
            // Proc implementing an interface, pull in the catch-all code that lets the proc get invoked
            // no matter what method is called on the interface
            RubyClass singletonClass = rubyObject.getSingletonClass();

            singletonClass.addMethod("method_missing", new DynamicMethod(singletonClass, Visibility.PUBLIC, CallConfiguration.NO_FRAME_NO_SCOPE) {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    if (!(self instanceof RubyProc)) {
                        throw context.getRuntime().newTypeError("interface impl method_missing for block used with non-Proc object");
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
        JavaObject jo = (JavaObject) RuntimeHelpers.invoke(context, rubyObject, "__jcreate_meta!");
        return jo.getValue();
    }

    public static Object convertArgumentToType(ThreadContext context, IRubyObject arg, Class target) {
        if (arg instanceof JavaObject) {
            return coerceJavaObjectToType(context, ((JavaObject)arg).getValue(), target);
        } else if (arg.dataGetStruct() instanceof JavaObject) {
            JavaObject innerWrapper = (JavaObject)arg.dataGetStruct();
            
            // ensure the object is associated with the wrapper we found it in,
            // so that if it comes back we don't re-wrap it
            context.getRuntime().getJavaSupport().getObjectProxyCache().put(innerWrapper.getValue(), arg);

            return innerWrapper.getValue();
        } else {
            switch (arg.getMetaClass().index) {
            case ClassIndex.NIL:
                return coerceNilToType((RubyNil)arg, target);
            case ClassIndex.FIXNUM:
                return coerceFixnumToType((RubyFixnum)arg, target);
            case ClassIndex.BIGNUM:
                return coerceBignumToType((RubyBignum)arg, target);
            case ClassIndex.FLOAT:
                return coerceFloatToType((RubyFloat)arg, target);
            case ClassIndex.STRING:
                return coerceStringToType((RubyString)arg, target);
            case ClassIndex.TRUE:
                return Boolean.TRUE;
            case ClassIndex.FALSE:
                return Boolean.FALSE;
            case ClassIndex.TIME:
                return ((RubyTime) arg).getJavaDate();
            default:
                return coerceOtherToType(context, arg, target);
            }
        }
    }
    
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
    
    public static Object coerceNilToType(RubyNil nil, Class target) {
        if(target.isPrimitive()) {
            throw nil.getRuntime().newTypeError("primitives do not accept null");
        } else {
            return null;
        }
    }
    
    public static Object coerceFixnumToType(RubyFixnum fixnum, Class target) {
        return coerceNumericToType(fixnum, target);
    }
        
    public static Object coerceBignumToType(RubyBignum bignum, Class target) {
        return coerceNumericToType(bignum, target);
    }
    
    public static Object coerceFloatToType(RubyFloat flote, Class target) {
        return coerceNumericToType(flote, target);
    }

    public static Object coerceNumericToType(RubyNumeric numeric, Class target) {
        // TODO: this could be faster
        if (target.isPrimitive()) {
            if (target == Byte.TYPE) {
                return Byte.valueOf((byte)numeric.getLongValue());
            } else if (target == Short.TYPE) {
                return Short.valueOf((short)numeric.getLongValue());
            } else if (target == Character.TYPE) {
                return Character.valueOf((char)numeric.getLongValue());
            } else if (target == Integer.TYPE) {
                return Integer.valueOf((int)numeric.getLongValue());
            } else if (target == Long.TYPE) {
                return Long.valueOf(numeric.getLongValue());
            } else if (target == Double.TYPE) {
                return Double.valueOf(numeric.getDoubleValue());
            } else if (target == Float.TYPE) {
                return Float.valueOf((float)numeric.getDoubleValue());
            }
        } else if (target == Byte.class) {
            return Byte.valueOf((byte)numeric.getLongValue());
        } else if (target == Short.class) {
            return Short.valueOf((short)numeric.getLongValue());
        } else if (target == Character.class) {
            return Character.valueOf((char)numeric.getLongValue());
        } else if (target == Integer.class) {
            return Integer.valueOf((int)numeric.getLongValue());
        } else if (target == Long.class) {
            return Long.valueOf((long)numeric.getLongValue());
        } else if (target == Float.class) {
            return Float.valueOf((float)numeric.getDoubleValue());
        } else if (target == Double.class) {
            return Double.valueOf((double)numeric.getDoubleValue());
        } else if (target == Object.class) {
            // for Object, default to natural wrapper type
            if (numeric instanceof RubyFixnum) {
                return Long.valueOf(numeric.getLongValue());
            } else if (numeric instanceof RubyFloat) {
                return Double.valueOf(numeric.getDoubleValue());
            } else if (numeric instanceof RubyBignum) {
                return ((RubyBignum)numeric).getValue();
            }
        }
        throw numeric.getRuntime().newTypeError("could not coerce " + numeric.getMetaClass() + " to " + target);
    }
    
    public static Object coerceStringToType(RubyString string, Class target) {
        try {
            ByteList bytes = string.getByteList();
            return new String(bytes.unsafeBytes(), bytes.begin(), bytes.length(), "UTF8");
        } catch (UnsupportedEncodingException uee) {
            return string.toString();
        }
    }
    
    public static Object coerceOtherToType(ThreadContext context, IRubyObject arg, Class target) {
        Ruby runtime = context.getRuntime();
        
        if (isDuckTypeConvertable(arg.getClass(), target)) {
            RubyObject rubyObject = (RubyObject) arg;
            if (!rubyObject.respondsTo("java_object")) {
                return convertProcToInterface(context, rubyObject, target);
            }
        } else if (arg.respondsTo("to_java_object")) {
            Object javaObject = arg.callMethod(context, "to_java_object");
            if (javaObject instanceof JavaObject) {
                runtime.getJavaSupport().getObjectProxyCache().put(((JavaObject) javaObject).getValue(), arg);
                javaObject = ((JavaObject)javaObject).getValue();
            }
            return javaObject;
        }

        // it's either as converted as we can make it via above logic or it's
        // not one of the types we convert, so just pass it out as-is without wrapping
        return arg;
    }

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
            javaObject = new Long(((RubyFixnum) object).getLongValue());
            break;
        case ClassIndex.BIGNUM:
            javaObject = ((RubyBignum) object).getValue();
            break;
        case ClassIndex.FLOAT:
            javaObject = new Double(((RubyFloat) object).getValue());
            break;
        case ClassIndex.STRING:
            try {
                ByteList bytes = ((RubyString) object).getByteList();
                javaObject = new String(bytes.unsafeBytes(), bytes.begin(), bytes.length(), "UTF8");
            } catch (UnsupportedEncodingException uee) {
                javaObject = object.toString();
            }
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

    /**
     * High-level object conversion utility function 'java_to_primitive' is the low-level version 
     */
    public static IRubyObject java_to_ruby(Ruby runtime, IRubyObject object) {
        if (object instanceof JavaObject) {
            return JavaUtil.convertJavaToUsableRubyObject(runtime, ((JavaObject) object).getValue());
        }
        return object;
    }

    // TODO: Formalize conversion mechanisms between Java and Ruby
    /**
     * High-level object conversion utility. 
     */
    public static IRubyObject ruby_to_java(final IRubyObject recv, IRubyObject object, Block unusedBlock) {
        if (object.respondsTo("to_java_object")) {
            IRubyObject result = (JavaObject)object.dataGetStruct();
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

    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        if (object instanceof JavaObject) {
            return JavaUtil.convertJavaToRuby(recv.getRuntime(), ((JavaObject) object).getValue());
        }

        return object;
    }
    
    public static boolean isJavaObject(IRubyObject candidate) {
        return candidate.dataGetStruct() instanceof JavaObject;
    }
    
    public static Object unwrapJavaObject(IRubyObject object) {
        return ((JavaObject)object.dataGetStruct()).getValue();
    }

    private static final Pattern JAVA_PROPERTY_CHOPPER = Pattern.compile("(get|set|is)([A-Z0-9])(.*)");
    public static String getJavaPropertyName(String beanMethodName) {
        Matcher m = JAVA_PROPERTY_CHOPPER.matcher(beanMethodName);

        if (!m.find()) return null;
        String javaPropertyName = m.group(2).toLowerCase() + m.group(3);
        return javaPropertyName;
    }

    private static final Pattern CAMEL_CASE_SPLITTER = Pattern.compile("([a-z][0-9]*)([A-Z])");    
    public static String getRubyCasedName(String javaCasedName) {
        Matcher m = CAMEL_CASE_SPLITTER.matcher(javaCasedName);
        return m.replaceAll("$1_$2").toLowerCase();
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
     * @param simpleName
     * @param nameSet
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

    public static Object unwrapJavaValue(Ruby runtime, IRubyObject obj, String errorMessage) {
        if(obj instanceof JavaMethod) {
            return ((JavaMethod)obj).getValue();
        } else if(obj instanceof JavaConstructor) {
            return ((JavaConstructor)obj).getValue();
        } else if(obj instanceof JavaField) {
            return ((JavaField)obj).getValue();
        } else if(obj instanceof JavaObject) {
            return ((JavaObject)obj).getValue();
        } else if(obj.dataGetStruct() != null && (obj.dataGetStruct() instanceof IRubyObject)) {
            return unwrapJavaValue(runtime, ((IRubyObject)obj.dataGetStruct()), errorMessage);
        } else {
            throw runtime.newTypeError(errorMessage);
        }
    }
}
