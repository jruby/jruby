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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;

/**
 *
 * @author Jan Arne Petersen, Alan Moore
 */
public class JavaUtil {

    public static Object convertRubyToJava(IRubyObject rubyObject) {
        return convertRubyToJava(rubyObject, null);
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
                        context, MethodIndex.TO_I, "to_i")).getLongValue());
            }
            return new Byte((byte) 0);
        }
    };
    
    public static final RubyConverter RUBY_SHORT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Short((short) ((RubyNumeric) rubyObject.callMethod(
                        context, MethodIndex.TO_I, "to_i")).getLongValue());
            }
            return new Short((short) 0);
        }
    };
    
    public static final RubyConverter RUBY_INTEGER_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Integer((int) ((RubyNumeric) rubyObject.callMethod(
                        context, MethodIndex.TO_I, "to_i")).getLongValue());
            }
            return new Integer(0);
        }
    };
    
    public static final RubyConverter RUBY_LONG_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_i")) {
                return new Long(((RubyNumeric) rubyObject.callMethod(
                        context, MethodIndex.TO_I, "to_i")).getLongValue());
            }
            return new Long(0);
        }
    };
    
    public static final RubyConverter RUBY_FLOAT_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return new Float((float) ((RubyNumeric) rubyObject.callMethod(
                        context, MethodIndex.TO_F, "to_f")).getDoubleValue());
            }
            return new Float(0.0);
        }
    };
    
    public static final RubyConverter RUBY_DOUBLE_CONVERTER = new RubyConverter() {
        public Object convert(ThreadContext context, IRubyObject rubyObject) {
            if (rubyObject.respondsTo("to_f")) {
                return new Double(((RubyNumeric) rubyObject.callMethod(
                        context, MethodIndex.TO_F, "to_f")).getDoubleValue());
            }
            return new Double(0.0);
        }
    };
    
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

    public static Object convertRubyToJava(IRubyObject rubyObject, Class javaClass) {
        if (rubyObject == null || rubyObject.isNil()) {
            return null;
        }
        
        ThreadContext context = rubyObject.getRuntime().getCurrentContext();
        
        if (rubyObject.respondsTo("java_object")) {
        	rubyObject = rubyObject.callMethod(context, "java_object");
        }

        if (rubyObject.respondsTo("to_java_object")) {
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

            // XXX this probably isn't good enough -AM
            String s = ((RubyString) rubyObject.callMethod(context, MethodIndex.TO_S, "to_s")).toString();
            if (s.length() > 0) {
                return new Character(s.charAt(0));
            }
			return new Character('\0');
        } else if (javaClass == String.class) {
            RubyString rubyString = (RubyString) rubyObject.callMethod(context, MethodIndex.TO_S, "to_s");
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
         		RubyNumeric rubyNumeric = ((RubyNumeric)rubyObject.callMethod(context,MethodIndex.TO_F, "to_f"));
 				return  BigInteger.valueOf (rubyNumeric.getLongValue());
         	}
        } else if (javaClass == BigDecimal.class && !(rubyObject instanceof JavaObject)) {
         	if (rubyObject.respondsTo("to_f")) {
             	double double_value = ((RubyNumeric)rubyObject.callMethod(context,MethodIndex.TO_F, "to_f")).getDoubleValue();
             	return new BigDecimal(double_value);
         	}
        }
        try {
            return ((JavaObject) rubyObject).getValue();
        } catch (ClassCastException ex) {
            if (rubyObject.getRuntime().getDebug().isTrue()) ex.printStackTrace();
            return null;
        }
    }

    public static IRubyObject[] convertJavaArrayToRuby(Ruby runtime, Object[] objects) {
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
        }
        return convertJavaToRuby(runtime, object, object.getClass());
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
                IRubyObject javaUtilities = runtime.getJavaSupport().getJavaUtilitiesModule();
                IRubyObject javaInterfaceModule = Java.get_interface_module(javaUtilities, JavaClass.get(runtime, parameterType));
                if (!((RubyModule)javaInterfaceModule).isInstance(rubyObject)) {
                    rubyObject.extend(new IRubyObject[] {javaInterfaceModule});
                }
                ThreadContext context = runtime.getCurrentContext();
                if (rubyObject instanceof RubyProc) {
                    // Proc implementing an interface, pull in the catch-all code that lets the proc get invoked
                    // no matter what method is called on the interface
                    rubyObject.instance_eval(context, runtime.newString("extend Proc::CatchAll"), Block.NULL_BLOCK);
                }
                JavaObject jo = (JavaObject) rubyObject.instance_eval(context, runtime.newString("send :__jcreate_meta!"), Block.NULL_BLOCK);
                return jo.getValue();
            }
        }
        return argument;
    }
    
    public static boolean isDuckTypeConvertable(Class providedArgumentType, Class parameterType) {
        return parameterType.isInterface() && !parameterType.isAssignableFrom(providedArgumentType) 
            && RubyObject.class.isAssignableFrom(providedArgumentType);
    }
}
