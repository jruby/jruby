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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jruby.exceptions.ArgumentError;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

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
        IRubyObject result = getRuntime().getNil();

        RubyHash interfaceProcs = (RubyHash) getInstanceVariable("interfaceProcs");

        RubyString methodName = RubyString.newString(getRuntime(), method.getName());

        IRubyObject proc;
        IRubyObject rubyMethod;

        if (!(proc = interfaceProcs.aref(methodName)).isNil()) {
            IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(getRuntime(), args);

            result = ((RubyProc) proc).call(rubyArgs).toRubyObject();
        } else if (!(rubyMethod = method(methodName)).isNil()) {
            IRubyObject[] rubyArgs = JavaUtil.convertJavaArrayToRuby(getRuntime(), args);

            result = ((RubyMethod) rubyMethod).call(rubyArgs);
        } else {
            IRubyObject[] rubyArgs = new IRubyObject[args.length + 1];

            for (int i = 0; i < args.length; i++) {
                rubyArgs[i + 1] = JavaUtil.convertJavaToRuby(getRuntime(), args[i]);
            }

            result = callMethod("send", rubyArgs);
        }

        return JavaUtil.convertRubyToJava(getRuntime(), result, method.getReturnType());
    }

    public static RubyJavaIObject newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyJavaIObject newInterface = new RubyJavaIObject(recv.getRuntime(), (RubyClass) recv);

        newInterface.callInit(args);

        return newInterface;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if (args.length == 0) {
            throw new ArgumentError(getRuntime(), "");
        }

        Class[] interfaces = new Class[args.length];

        for (int i = 0; i < args.length; i++) {
			String name = ((RubyString) args[i]).getValue();
            interfaces[i] = getRuntime().getJavaSupport().loadJavaClass(name);
        }

        try {
            setValue(Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, this));
        } catch (IllegalArgumentException iaExcptn) {
        }
        
        setInstanceVariable("interfaceProcs", RubyHash.newHash(getRuntime()));

        return this;
    }
    
    public IRubyObject assign(RubyString methodName, RubyProc proc) {
    	((RubyHash) getInstanceVariable("interfaceProcs")).aset(methodName, proc);
    	
    	return this;
    }
}