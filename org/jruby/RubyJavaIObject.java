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

public class RubyJavaIObject extends RubyJavaObject implements InvocationHandler {

    public RubyJavaIObject(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    /** Create the JavaInterface class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createJavaInterfaceObjectClass(Ruby ruby) {
        RubyClass javaInterfaceClass = ruby.defineClass("JavaInterfaceObject", ruby.getClasses().getJavaObjectClass());

        javaInterfaceClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyJavaIObject.class, "newInstance"));
        
        javaInterfaceClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyJavaIObject.class, "initialize"));
        javaInterfaceClass.defineMethod("assign", CallbackFactory.getMethod(RubyJavaIObject.class, "assign", RubyString.class, RubyProc.class));

        return javaInterfaceClass;
    }

    /** This method is called when a method in the interface is called.
     * It invokes the assigned Ruby method or proc.
     * 
     * @see InvocationHandler#invoke(Object, Method, Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RubyObject result = getRuby().getNil();

        RubyHash interfaceProcs = (RubyHash) getInstanceVar("interfaceProcs");

        RubyString methodName = RubyString.newString(getRuby(), method.getName());

        RubyObject proc;
        RubyObject rubyMethod;

        if (!(proc = interfaceProcs.aref(methodName)).isNil()) {
            RubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(getRuby(), args);

            result = ((RubyProc) proc).call(rubyArgs);
        } else if (!(rubyMethod = method(methodName)).isNil()) {
            RubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(getRuby(), args);

            result = ((RubyMethod) rubyMethod).call(rubyArgs);
        } else {
            RubyObject[] rubyArgs = new RubyObject[args.length + 1];

            for (int i = 0; i < args.length; i++) {
                rubyArgs[i + 1] = JavaUtil.convertJavaToRuby(getRuby(), args[i]);
            }

            result = funcall("send", rubyArgs);
        }

        return JavaUtil.convertRubyToJava(getRuby(), result, method.getReturnType());
    }

    public static RubyJavaIObject newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyJavaIObject newInterface = new RubyJavaIObject(ruby, (RubyClass) recv);

        newInterface.callInit(args);

        return newInterface;
    }

    public RubyObject initialize(RubyObject[] args) {
        if (args.length == 0) {
            throw new RubyArgumentException(getRuby(), "");
        }

        Class[] interfaces = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
            interfaces[i] = getRuby().getJavaSupport().loadJavaClass((RubyString)args[i]);
        }

        try {
            setValue(Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, this));
        } catch (IllegalArgumentException iaExcptn) {
        }
        
        setInstanceVar("interfaceProcs", RubyHash.newHash(getRuby()));

        return this;
    }
    
    public RubyObject assign(RubyString methodName, RubyProc proc) {
    	((RubyHash) getInstanceVar("interfaceProcs")).aset(methodName, proc);
    	
    	return this;
    }
}