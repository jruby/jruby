package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class RubyConversion {
    RubyProxyFactory factory = null;

    public RubyConversion(RubyProxyFactory factory) {
        this.factory = factory;
    }

    public Ruby getRuby() {
        return factory.getRuby();
    }

    public IRubyObject[] convertJavaToRuby(Object[] obj) {
        if (obj == null || obj.length == 0)
            return IRubyObject.NULL_ARRAY;

        IRubyObject[] ret = new IRubyObject[obj.length];

        for (int i = 0; i < obj.length; i++) {
            ret[i] = convertJavaToRuby(obj[i]);
        }

        return ret;
    }

    public IRubyObject convertJavaToRuby(Object obj) {
        if (obj == null)
            return getRuby().getNil();
        if (obj instanceof Set)
            obj = new ArrayList((Set) obj);

        Class type = getCanonicalJavaClass(obj.getClass());

        return JavaUtil.convertJavaToRuby(getRuby(), obj, type);
    }

    public Object[] convertRubyToJava(IRubyObject[] obj) {
        Object[] ret = new Object[obj.length];

        for (int i = 0; i < obj.length; i++) {
            ret[i] = convertRubyToJava(obj[i]);
        }

        return ret;
    }

    public Object convertRubyToJava(IRubyObject obj) {
        return convertRubyToJava(obj, null);
    }

    public Object convertRubyToJava(IRubyObject obj, Class type) {
        type = getCanonicalJavaClass(type);

        if (type != null)
            type.getName(); // HACK TO FIX HANG

        if (type == Void.TYPE || obj == null || obj.isNil())
            return null;

        if (obj instanceof RubyArray) {
            try {
                return convertRubyArrayToJava((RubyArray) obj, type);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Yuck...
        try {
            RubyProxy proxy = factory.getProxyForObject(obj, type);
            if (proxy != null) {
                return proxy;
            }
        } catch (Exception ex) {
        }

        return JavaUtil.convertRubyToJava(obj, type);
    }

    public Object convertRubyArrayToJava(RubyArray array, Class type)
        throws InstantiationException, IllegalAccessException {
        if (type.isArray()) {
            return JavaUtil.convertRubyToJava(array, type);
        }

        if (Collection.class.isAssignableFrom(type)) {
            try {
                Collection ret = (Collection) type.newInstance();

                Iterator it = array.getList().iterator();
                while (it.hasNext()) {
                    Object obj = it.next();
                    ret.add(convertRubyToJava((IRubyObject) obj));
                }

                return ret;
            } catch (UnsupportedOperationException ex) {
                // Collection.add will throw this for Map's and other
                // complex structures.
                throw ex;
            }
        }

        throw new UnsupportedOperationException(
            type.getName() + " not supported");
    }

    private static Class getCanonicalJavaClass(Class type) {
        // Replace wrapper classes with the primitive class that each
        // represents.
        if (type == Double.class)
            return Double.TYPE;
        if (type == Float.class)
            return Float.TYPE;
        if (type == Integer.class)
            return Integer.TYPE;
        if (type == Long.class)
            return Long.TYPE;
        if (type == Short.class)
            return Short.TYPE;
        if (type == Byte.class)
            return Byte.TYPE;
        if (type == Character.class)
            return Character.TYPE;
        if (type == Void.class)
            return Void.TYPE;
        if (type == Boolean.class)
            return Boolean.TYPE;
        return type;
    }
}
