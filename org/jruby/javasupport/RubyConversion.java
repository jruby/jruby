package org.jruby.javasupport;

import java.util.*;
import org.jruby.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.*;

public class RubyConversion {
    RubyProxyFactory factory = null;

    public RubyConversion(RubyProxyFactory factory) {
        this.factory = factory;
    }

    public Ruby getRuby() {
        return factory.getRuby();
    }

    public IRubyObject[] convertJavaToRuby(Object[] obj) {
        if (obj == null)
            return new IRubyObject[0];

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

        Class type = JavaUtil.getCanonicalJavaClass(obj.getClass());

        return JavaUtil.convertJavaToRuby(getRuby(), obj, type).toRubyObject();
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
        type = JavaUtil.getCanonicalJavaClass(type);

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

        return JavaUtil.convertRubyToJava(getRuby(), obj, type);
    }

    public Object convertRubyArrayToJava(RubyArray array, Class type)
        throws InstantiationException, IllegalAccessException {
        if (type.isArray()) {
            return JavaUtil.convertRubyToJava(getRuby(), array, type);
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
}
