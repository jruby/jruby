/*
 * RubyMethod.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Charles O Nutter
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Charles O Nutter <headius@headius.com>
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

import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * The RubyMethod class represents a RubyMethod object.
 * 
 * You can get such a method by calling the "method" method of an object.
 * 
 * Note: This was renamed from Method.java
 * 
 * @author  jpetersen
 * @since 0.2.3
 */
public class RubyMethod extends RubyObject {
    protected RubyModule implementationModule;
    protected String methodName;
    protected RubyModule originModule;
    protected String originName;
    protected ICallable method;
    protected IRubyObject receiver;

    protected RubyMethod(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    /** Create the RubyMethod class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createMethodClass(Ruby runtime) {
		RubyClass methodClass = runtime.defineClass("RubyMethod", runtime.getClasses().getObjectClass());
    	
		CallbackFactory callbackFactory = runtime.callbackFactory();
        
		methodClass.defineMethod("arity", 
				callbackFactory.getMethod(RubyMethod.class, "arity"));
		methodClass.defineMethod("to_proc", 
				callbackFactory.getMethod(RubyMethod.class, "to_proc"));
		methodClass.defineMethod("unbind", 
				callbackFactory.getMethod(RubyMethod.class, "unbind"));
		methodClass.defineMethod("call", 
				callbackFactory.getOptMethod(RubyMethod.class, "call"));
		methodClass.defineMethod("[]", 
				callbackFactory.getOptMethod(RubyMethod.class, "call"));

		return methodClass;
    }

    public static RubyMethod newMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        ICallable method,
        IRubyObject receiver) {
        Ruby runtime = implementationModule.getRuntime();
        RubyMethod newMethod = new RubyMethod(runtime, runtime.getClass("RubyMethod"));

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.method = method;
        newMethod.receiver = receiver;

        return newMethod;
    }

    /** Call the method.
     * 
     */
    public IRubyObject call(IRubyObject[] args) {
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }

        if (args.length != method.getArity().getValue()) {
        	throw new ArgumentError(getRuntime(), "");
        }
        
        getRuntime().getIterStack().push(getRuntime().isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
        try {
            return implementationModule.call0(receiver, methodName, args, method, false);
        } finally {
            getRuntime().getIterStack().pop();
        }
    }

    /** Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    public RubyFixnum arity() {
        return RubyFixnum.newFixnum(getRuntime(), method.getArity().getValue());
    }

    /** Create a Proc object.
     * 
     */
    public IRubyObject to_proc() {
        return runtime.iterate(
            callbackFactory().getSingletonMethod(RubyMethod.class, "mproc"),
            runtime.getNil(),
            callbackFactory().getBlockMethod(RubyMethod.class, "bmcall"),
            this);
    }

    /** Create a Proc object which is called like a ruby method.
     *
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject mproc(IRubyObject recv) {
        try {
            recv.getRuntime().getIterStack().push(Iter.ITER_CUR);
            recv.getRuntime().getFrameStack().push();
            return RubyKernel.proc(recv);
        } finally {
            recv.getRuntime().getFrameStack().pop();
            recv.getRuntime().getIterStack().pop();
        }
    }

    /** Delegate a block call to a bound method call.
     *
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject bmcall(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        if (blockArg instanceof RubyArray) {
            return ((RubyMethod) arg1).call(((RubyArray) blockArg).toJavaArray());
        }
        return ((RubyMethod) arg1).call(new IRubyObject[] { blockArg });
    }

    public RubyUnboundMethod unbind() {
        RubyUnboundMethod unboundMethod =
        	RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, originName, method);
        unboundMethod.receiver = this;
        unboundMethod.infectBy(this);
        
        return unboundMethod;
    }
}

