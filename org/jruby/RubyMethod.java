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

import org.jruby.ast.ArgsNode;
import org.jruby.ast.AttrSetNode;
import org.jruby.ast.InstVarNode;
import org.jruby.internal.runtime.methods.CallbackMethod;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.EvaluateMethod;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * The RubyMethod class represents a Method object.
 * 
 * You can get such a method by calling the "method" method of an object.
 * 
 * @author  jpetersen
 * @since 0.2.3
 */
public class RubyMethod extends RubyObject {
    private RubyClass receiverClass;
    private IRubyObject receiver;
    private String methodId;
    private ICallable method;
    private RubyClass originalClass;
    private String originalId;

    public static class Nil extends RubyMethod {
        public Nil(Ruby ruby) {
            super(ruby, ruby.getClasses().getNilClass());
        }
    }

    public RubyMethod(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    /** Create the Method class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createMethodClass(Ruby ruby) {
        RubyClass methodClass = ruby.defineClass("Method", ruby.getClasses().getObjectClass());

        methodClass.defineMethod("arity", CallbackFactory.getMethod(RubyMethod.class, "arity"));
        methodClass.defineMethod("[]", CallbackFactory.getOptMethod(RubyMethod.class, "call"));
        methodClass.defineMethod("call", CallbackFactory.getOptMethod(RubyMethod.class, "call"));
        methodClass.defineMethod("to_proc", CallbackFactory.getMethod(RubyMethod.class, "to_proc"));

        return methodClass;
    }

    /**
     * Gets the methodId
     * @return Returns a RubyId
     */
    public String getMethodId() {
        return methodId;
    }

    /**
     * Sets the methodId
     * @param methodId The methodId to set
     */
    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

    /**
     * Gets the originalClass
     * @return Returns a RubyClass
     */
    public RubyClass getOriginalClass() {
        return originalClass;
    }

    /**
     * Sets the originalClass
     * @param originalClass The originalClass to set
     */
    public void setOriginalClass(RubyClass originalClass) {
        this.originalClass = originalClass;
    }

    /**
     * Gets the originalId
     * @return Returns a RubyId
     */
    public String getOriginalId() {
        return originalId;
    }

    /**
     * Sets the originalId
     * @param originalId The originalId to set
     */
    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    /**
     * Gets the receiver
     * @return Returns a RubyObject
     */
    public IRubyObject getReceiver() {
        return receiver;
    }

    /**
     * Sets the receiver
     * @param receiver The receiver to set
     */
    public void setReceiver(IRubyObject receiver) {
        this.receiver = receiver;
    }

    /**
     * Gets the receiverClass
     * @return Returns a RubyClass
     */
    public RubyClass getReceiverClass() {
        return receiverClass;
    }

    /**
     * Sets the receiverClass
     * @param receiverClass The receiverClass to set
     */
    public void setReceiverClass(RubyClass receiverClass) {
        this.receiverClass = receiverClass;
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
            return getReceiverClass().call0(getReceiver(), getMethodId(), args, getMethod(), false);
        } finally {
            getRuntime().getIterStack().pop();
        }
    }

    /**Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    public RubyFixnum arity() {
        if (method instanceof EvaluateMethod) {
            if (((EvaluateMethod) method).getNode() instanceof AttrSetNode) {
                return RubyFixnum.one(getRuntime());
            } else if (((EvaluateMethod) method).getNode() instanceof InstVarNode) {
                return RubyFixnum.zero(getRuntime());
            }
        } else if (method instanceof DefaultMethod) {
            ArgsNode args = ((DefaultMethod) method).getArgsNode();

            if (args == null) {
                return RubyFixnum.zero(getRuntime());
            }
            int n = args.getArgsCount();
            if (args.getOptArgs() != null || args.getRestArg() >= 0) {
                n = -n - 1;
            }
            return RubyFixnum.newFixnum(getRuntime(), n);
        } else if (method instanceof CallbackMethod) {
            Arity arity = ((CallbackMethod) method).getCallback().getArity();
            return RubyFixnum.newFixnum(getRuntime(), arity.getValue());
        }

        return RubyFixnum.newFixnum(getRuntime(), -1);
    }

    /**
     * Gets the method.
     * @return Returns a IMethod
     */
    public ICallable getMethod() {
        return method;
    }

    /**
     * Sets the method.
     * @param method The method to set
     */
    public void setMethod(ICallable method) {
        this.method = method;
    }

    /** Create a Proc object.
     * 
     */
    public IRubyObject to_proc() {
        return runtime.iterate(
            CallbackFactory.getSingletonMethod(RubyMethod.class, "mproc"),
            null,
            CallbackFactory.getBlockMethod(RubyMethod.class, "bmcall"),
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

    /** Delegate a block call to a method call.
     * 
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject bmcall(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        if (blockArg instanceof RubyArray) {
            return ((RubyMethod) arg1).call(((RubyArray) blockArg).toJavaArray());
        } else {
            return ((RubyMethod) arg1).call(new IRubyObject[] { blockArg });
        }
    }
}
