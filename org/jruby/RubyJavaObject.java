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

import org.jruby.core.*;
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
    
    public static RubyObject m_load_class(Ruby ruby, RubyModule rubyClass, RubyString className, RubyObject[] args) {
        String javaName = className.getString();
        String rubyName = javaName.substring(javaName.lastIndexOf('.') + 1);
        if (args.length > 0) {
            rubyName = ((RubyString)args[0]).getString();
        }
        
        try {
            final Class c = Class.forName(javaName);
            
            final RubyClass newRubyClass = ruby.defineClass(rubyName, (RubyClass)ruby.getRubyClass("JavaObject"));
            
            Method[] methods = c.getMethods();
            
            for (int i = 0; i < methods.length; i++) {
                String methodName = methods[i].getName();
                if (methods[i].getDeclaringClass() != Object.class) {
                    newRubyClass.defineMethod(methodName, new JavaReflectionMethod(methods[i]));
                }
            }
            
            newRubyClass.defineSingletonMethod("new", new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    try {
                        Object value = c.newInstance();
                        RubyJavaObject javaObject = new RubyJavaObject(ruby, (RubyModule)recv, value);
                        javaObject.callInit(args);
                        return javaObject;
                    } catch (Exception excptn) {
                    }
                    return ruby.getNil();
                }
            });
            
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
}