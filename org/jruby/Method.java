/*
 * RubyMethod.java - No description
 * Created on 04. Juli 2001, 22:53
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.IndexedCallback;
import org.jruby.runtime.Iter;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * The RubyMethod class represents a Method object.
 * 
 * You can get such a method by calling the "method" method of an object.
 * 
 * @author  jpetersen
 * @since 0.2.3
 */
public class Method extends RubyObject {
    protected RubyModule implementationModule;
    protected String methodName;
    protected RubyModule originModule;
    protected String originName;
    protected ICallable method;
    protected IRubyObject receiver;
    
    private static final int ARITY = 0x10000;
    private static final int CALL = 0x10001;
    private static final int TO_PROC = 0x10002;
    private static final int UNBIND = 0x10003;

    protected Method(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    /** Create the Method class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createMethodClass(Ruby ruby) {
        RubyClass methodClass = ruby.defineClass("Method", ruby.getClasses().getObjectClass());

        methodClass.defineMethod("[]", IndexedCallback.createOptional(CALL));
        methodClass.defineMethod("arity", IndexedCallback.create(ARITY, 0));
        methodClass.defineMethod("call", IndexedCallback.createOptional(CALL));
        methodClass.defineMethod("to_proc", IndexedCallback.create(TO_PROC, 0));
        methodClass.defineMethod("unbind", IndexedCallback.create(UNBIND, 0));

        return methodClass;
    }

    public static Method newMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        ICallable method,
        IRubyObject receiver) {
        Ruby runtime = implementationModule.getRuntime();
        Method newMethod = new Method(runtime, runtime.getClasses().getMethodClass());

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
            args = new IRubyObject[0];
        }
        getRuntime().getIterStack().push(getRuntime().isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
        try {
            return implementationModule.call0(receiver, methodName, args, method, false);
        } finally {
            getRuntime().getIterStack().pop();
        }
    }

    /**Returns the number of arguments a method accepted.
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
            CallbackFactory.getSingletonMethod(Method.class, "mproc"),
            runtime.getNil(),
            CallbackFactory.getBlockMethod(Method.class, "bmcall"),
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
            return KernelModule.proc(recv);
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
            return ((Method) arg1).call(((RubyArray) blockArg).toJavaArray());
        } else {
            return ((Method) arg1).call(new IRubyObject[] { blockArg });
        }
    }
    /**
     * @see org.jruby.runtime.IndexCallable#callIndexed(int, IRubyObject[])
     */
    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case ARITY :
                return arity();
            case CALL :
                return call(args);
            case TO_PROC :
                return to_proc();
            case UNBIND :
                return unbind();
            default:
                return super.callIndexed(index, args);
        }
    }
    
    public UnboundMethod unbind() {
        UnboundMethod unboundMethod = UnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, originName, method);
        unboundMethod.receiver = this;
        unboundMethod.infectBy(this);
        return unboundMethod;
    }

}
