/*
 * JavaObject.java - No description
 * Created on 21. September 2001, 14:43
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

    public RubyJavaObject(Ruby ruby, RubyModule rubyClass) {
        this(ruby, rubyClass, null);
    }
    
    public RubyJavaObject(Ruby ruby, RubyModule rubyClass, Object value) {
        super(ruby, rubyClass);
        
        this.value = value;
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
    
    // JavaObject methods
    
    public static RubyObject m_load_class(Ruby ruby, RubyObject recv, RubyString className, RubyObject[] args) {
        String javaName = className.getString();
        String rubyName = javaName.substring(javaName.lastIndexOf('.') + 1);
        if (args.length > 0) {
            rubyName = ((RubyString)args[0]).getString();
        }
        
        try {
            Class c = Class.forName(javaName);
            
            Map methodMap = new HashMap();
            Map singletonMethodMap = new HashMap();
            
            Method[] methods = c.getMethods();
            
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
            
            RubyClass newRubyClass = ruby.defineClass(rubyName, (RubyClass)ruby.getRubyClass("JavaObject"));
            
            newRubyClass.defineSingletonMethod("new", new JavaConstructor(c.getConstructors()));
            
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
            
            return newRubyClass;
        } catch (ClassNotFoundException cnfExcptn) {
            throw new RubyNameException("cannot found Java class: " + javaName);
        } catch (SecurityException sExcptn) {
        }
        
        return ruby.getNil();
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