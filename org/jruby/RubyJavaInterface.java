/*
 * RubyJavaInterface.java - No description
 * Created on 1. December 2001, 17:49
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.exceptions.*;
import org.jruby.javasupport.*;
import org.jruby.runtime.*;

public class RubyJavaInterface extends RubyJavaObject {

    public RubyJavaInterface(Ruby ruby, Object proxyObject) {
        super(ruby, ruby.getClasses().getJavaInterfaceClass());
        
        setValue(proxyObject);
    }

    /** Create the JavaInterface class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createJavaInterfaceClass(Ruby ruby) {
        RubyClass javaInterfaceClass = ruby.defineClass("JavaInterface", ruby.getClasses().getJavaObjectClass());
        
        // javaInterfaceClass.undef("new");

        return javaInterfaceClass;
    }
    
    public static RubyJavaInterface newJavaInterface(final Ruby ruby, final Method interfaceMethod, final RubyProc proc) {
        return new RubyJavaInterface(ruby, Proxy.newProxyInstance(null, new Class[]{interfaceMethod.getDeclaringClass()}, new InvocationHandler() {
            public Object invoke(Object recv, Method imethod, Object[] args) {
                return JavaUtil.convertJavaToRuby(ruby, proc.call(JavaUtil.convertJavaArrayToRuby(ruby, args)));
            }
        }));
    }

    public static RubyJavaInterface newJavaInterface(final Ruby ruby, final Method interfaceMethod, final RubyMethod method) {
        return new RubyJavaInterface(ruby, Proxy.newProxyInstance(null, new Class[]{interfaceMethod.getDeclaringClass()}, new InvocationHandler() {
            public Object invoke(Object recv, Method imethod, Object[] args) {
                return JavaUtil.convertJavaToRuby(ruby, method.call(JavaUtil.convertJavaArrayToRuby(ruby, args)));
            }
        }));
    }

    public static RubyJavaInterface newJavaInterface(final Ruby ruby, final Method interfaceMethod, final RubyObject receiver, final RubyString method) {
        return new RubyJavaInterface(ruby, Proxy.newProxyInstance(null, new Class[]{interfaceMethod.getDeclaringClass()}, new InvocationHandler() {
            public Object invoke(Object recv, Method imethod, Object[] args) {
                return JavaUtil.convertJavaToRuby(ruby, receiver.send(method, JavaUtil.convertJavaArrayToRuby(ruby, args)));
            }
        }));
    }

    public static RubyJavaInterface newJavaInterface(final Ruby ruby, final Class javaInterface, final RubyObject receiver) {
        return new RubyJavaInterface(ruby, Proxy.newProxyInstance(null, new Class[]{javaInterface}, new InvocationHandler() {
            public Object invoke(Object recv, Method method, Object[] args) {
                return JavaUtil.convertJavaToRuby(ruby, receiver.send(RubyString.newString(ruby, method.getName()), JavaUtil.convertJavaArrayToRuby(ruby, args)));
            }
        }));
    }
}