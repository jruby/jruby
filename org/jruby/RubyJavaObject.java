/*
 * RubyJavaObject.java - No description
 * Created on 21. September 2001, 14:43
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby;

import java.lang.reflect.*;
import java.util.*;

import org.jruby.core.*;
import org.jruby.javasupport.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyJavaObject extends RubyObject {
    private Object value;
    
    private static Map loadedClassMap = new HashMap();

    public RubyJavaObject(Ruby ruby, RubyModule rubyClass) {
        this(ruby, rubyClass, null);
    }
    
    public RubyJavaObject(Ruby ruby, RubyModule rubyClass, Object value) {
        super(ruby, rubyClass);
        
        this.value = value;
    }
    
    public Class getJavaClass() {
        return value.getClass();
    }
    
    /** Getter for property value.
     * @return Value of property value.
     */
    public Object getValue() {
        return value;
    }
    
    /** Setter for property value.
     * @param value New value of property value.
     */
    public void setValue(Object value) {
        this.value = value;
    }
    
    public static RubyModule getRubyClass(Ruby ruby, Class javaClass) {
        Map classMap = (Map)loadedClassMap.get(ruby);
        if (classMap != null) {
            return (RubyModule)classMap.get(javaClass);
        }
        return null;
    }
    
    public static void putRubyClass(Ruby ruby, Class javaClass, RubyModule rubyClass) {
        Map classMap = (Map)loadedClassMap.get(ruby);
        if (classMap == null) {
            classMap = new HashMap();
            loadedClassMap.put(ruby, classMap);
        }
        classMap.put(javaClass, rubyClass);
    }
    
    public static RubyModule loadClass(Ruby ruby, Class javaClass, String rubyName) {
        RubyModule newRubyClass = getRubyClass(ruby, javaClass);
        if (newRubyClass != null) {
            return newRubyClass;
        }
        if (rubyName == null) {
            String javaName = javaClass.getName();
            rubyName = javaName.substring(javaName.lastIndexOf('.') + 1);
        }
        Map methodMap = new HashMap();
        Map singletonMethodMap = new HashMap();
        
        Method[] methods = javaClass.getMethods();
        
        for (int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            if (methods[i].getDeclaringClass() != Object.class) {
                if (Modifier.isStatic(methods[i].getModifiers())) {
                    if (singletonMethodMap.get(methods[i].getName()) == null) {
                        singletonMethodMap.put(methods[i].getName(), new LinkedList());
                    }
                    ((List)singletonMethodMap.get(methods[i].getName())).add(methods[i]);
                } else {
                    if (methodMap.get(methods[i].getName()) == null) {
                        methodMap.put(methods[i].getName(), new LinkedList());
                    }
                    ((List)methodMap.get(methods[i].getName())).add(methods[i]);
                }
            }
        }
        
        newRubyClass = ruby.defineClass(rubyName, (RubyClass)ruby.getRubyClass("JavaObject"));
        
        newRubyClass.defineSingletonMethod("new", new JavaConstructor(javaClass.getConstructors()));
        
        Iterator iter = methodMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            methods = (Method[])((List)entry.getValue()).toArray(new Method[((List)entry.getValue()).size()]);
            newRubyClass.defineMethod((String)entry.getKey(), new JavaMethod(methods));
        }
        
        iter = singletonMethodMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            methods = (Method[])((List)entry.getValue()).toArray(new Method[((List)entry.getValue()).size()]);
            newRubyClass.defineSingletonMethod((String)entry.getKey(), new JavaMethod(methods, true));
        }
        
        putRubyClass(ruby, javaClass, newRubyClass);
        
        return newRubyClass;
    }
    
    // JavaObject methods
    
    public static RubyObject m_load_class(Ruby ruby, RubyObject recv, RubyString className, RubyObject[] args) {
        String javaName = className.getValue();
        String rubyName = (args.length > 0) ? ((RubyString)args[0]).getValue() : null;
        
        try {
            Class c = ruby.getJavaClassLoader().loadClass(javaName);
            return loadClass(ruby, c, rubyName);
        } catch (ClassNotFoundException cnfExcptn) {
            throw new RubyNameException("cannot find Java class: " + javaName);
        }
    }
    
    public RubyString m_to_s() {
        return RubyString.m_newString(getRuby(), getValue() != null ? getValue().toString() : "null");
    }

    public RubyBoolean m_equal(RubyObject other) {
        if (other instanceof RubyJavaObject) {
            return (getValue() != null && getValue().equals(((RubyJavaObject)other).getValue())) ? getRuby().getTrue() : getRuby().getFalse();
        }
        return getRuby().getFalse();
    }
}