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
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyConversion {
    RubyProxyFactory factory = null;

    public RubyConversion(RubyProxyFactory factory) {
        this.factory = factory;
    }

    public Ruby getRuby() {
        return factory.getRuntime();
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
