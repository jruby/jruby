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

import java.math.BigDecimal;
import org.jruby.IRuby;
import org.jruby.RubyBoolean;
import org.jruby.RubyFloat;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Jan Arne Petersen, Alan Moore
 */
public class JavaUtil {

    public static Object convertRubyToJava(IRubyObject rubyObject) {
        return convertRubyToJava(rubyObject, null);
    }

    public static Object convertRubyToJava(IRubyObject rubyObject, Class javaClass) {
        if (rubyObject == null || rubyObject.isNil()) {
            return null;
        }
        
        ThreadContext context = rubyObject.getRuntime().getCurrentContext();
        
        if (rubyObject.respondsTo("java_object")) {
        	rubyObject = rubyObject.callMethod(context, "java_object");
        }

        if (rubyObject instanceof JavaObject) {
            return ((JavaObject) rubyObject).getValue();
        } else if (javaClass == Object.class || javaClass == null) {
            /* The Java method doesn't care what class it is, but we need to
               know what to convert it to, so we use the object's own class.
               If that doesn't help, we use String to force a call to the
               object's "to_s" method. */
            javaClass = rubyObject.getJavaClass();
            if (javaClass == IRubyObject.class) {
                javaClass = String.class;
            }
        }

        if (javaClass.isInstance(rubyObject)) {
            // rubyObject is already of the required jruby class (or subclass)
            return rubyObject;
        }

        if (javaClass.isPrimitive()) {
            String cName = javaClass.getName();
            if (cName == "boolean") {
                return Boolean.valueOf(rubyObject.isTrue());
            } else if (cName == "float") {
                if (rubyObject.respondsTo("to_f")) {
                    return new Float(((RubyNumeric) rubyObject.callMethod(context, "to_f")).getDoubleValue());
                }
				return new Float(0.0);
            } else if (cName == "double") {
                if (rubyObject.respondsTo("to_f")) {
                    return new Double(((RubyNumeric) rubyObject.callMethod(context, "to_f")).getDoubleValue());
                }
				return new Double(0.0);
            } else if (cName == "long") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Long(((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue());
                }
				return new Long(0);
            } else if (cName == "int") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Integer((int) ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue());
                }
				return new Integer(0);
            } else if (cName == "short") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Short((short) ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue());
                }
				return new Short((short) 0);
            } else if (cName == "byte") {
                if (rubyObject.respondsTo("to_i")) {
                    return new Byte((byte) ((RubyNumeric) rubyObject.callMethod(context, "to_i")).getLongValue());
                }
				return new Byte((byte) 0);
            }

            // XXX this probably isn't good enough -AM
            String s = ((RubyString) rubyObject.callMethod(context, "to_s")).toString();
            if (s.length() > 0) {
                return new Character(s.charAt(0));
            }
			return new Character('\0');
        } else if (javaClass == String.class) {
            return ((RubyString) rubyObject.callMethod(context, "to_s")).toString();
        } else {
            return ((JavaObject) rubyObject).getValue();
        }
    }

    public static IRubyObject[] convertJavaArrayToRuby(IRuby runtime, Object[] objects) {
        IRubyObject[] rubyObjects = new IRubyObject[objects.length];
        for (int i = 0; i < objects.length; i++) {
            rubyObjects[i] = convertJavaToRuby(runtime, objects[i]);
        }
        return rubyObjects;
    }

    public static IRubyObject convertJavaToRuby(IRuby runtime, Object object) {
        if (object == null) {
            return runtime.getNil();
        }
        return convertJavaToRuby(runtime, object, object.getClass());
    }

    public static IRubyObject convertJavaToRuby(IRuby runtime, Object object, Class javaClass) {
        if (object == null) {
            return runtime.getNil();
        }
        
        if (object instanceof IRubyObject) {
        	return (IRubyObject) object;
        }

        if (javaClass.isPrimitive()) {
            String cName = javaClass.getName();
            if (cName == "boolean") {
                return RubyBoolean.newBoolean(runtime, ((Boolean) object).booleanValue());
            } else if (cName == "float" || cName == "double") {
                return RubyFloat.newFloat(runtime, ((Number) object).doubleValue());
            } else if (cName == "char") {
                return runtime.newFixnum(((Character) object).charValue());
            } else {
                // else it's one of the integral types
                return runtime.newFixnum(((Number) object).longValue());
            }
        } else if (javaClass == Boolean.class) {
            return RubyBoolean.newBoolean(runtime, ((Boolean) object).booleanValue());
        } else if (javaClass == Float.class || javaClass == Double.class) {
            return RubyFloat.newFloat(runtime, ((Number) object).doubleValue());
        } else if (javaClass == Character.class) {
            return runtime.newFixnum(((Character) object).charValue());
        } else if (Number.class.isAssignableFrom(javaClass) && javaClass != BigDecimal.class) {
            return runtime.newFixnum(((Number) object).longValue());
        } else if (javaClass == String.class) {
            return runtime.newString(object.toString());
        } else if (IRubyObject.class.isAssignableFrom(javaClass)) {
            return (IRubyObject) object;
        } else {
            return JavaObject.wrap(runtime, object);
        }
    }

    public static Class primitiveToWrapper(Class type) {
        if (type == Double.TYPE) {
            return Double.class;
        } else if (type == Float.TYPE) {
            return Float.class;
        } else if (type == Integer.TYPE) {
            return Integer.class;
        } else if (type == Long.TYPE) {
            return Long.class;
        } else if (type == Short.TYPE) {
            return Short.class;
        } else if (type == Byte.TYPE) {
            return Byte.class;
        } else if (type == Character.TYPE) {
            return Character.class;
        } else if (type == Void.TYPE) {
            return Void.class;
        } else if (type == Boolean.TYPE) {
            return Boolean.class;
        } else {
            return type;
        }
    }

    public static Object convertArgument(Object argument, Class parameterType) {
        if (argument instanceof JavaObject) {
            argument = ((JavaObject) argument).getValue();
            if (argument == null) {
                return null;
            }
        }
        Class type = primitiveToWrapper(parameterType);
        if (type == Void.class) {
            return null;
        }
        if (argument instanceof Number) {
            final Number number = (Number) argument;
            if (type == Long.class) {
                return new Long(number.longValue());
            } else if (type == Integer.class) {
                return new Integer(number.intValue());
            } else if (type == Short.class) {
                return new Short(number.shortValue());
            } else if (type == Byte.class) {
                return new Byte(number.byteValue());
            } else if (type == Character.class) {
                return new Character((char) number.intValue());
            } else if (type == Double.class) {
                return new Double(number.doubleValue());
            } else if (type == Float.class) {
                return new Float(number.floatValue());
            }
        }
        return argument;
    }
}
