/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby;

import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class UnboundMethod extends Method {

    protected UnboundMethod(Ruby runtime) {
        super(runtime, runtime.getClass("UnboundMethod"));
    }

    public static UnboundMethod newUnboundMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        ICallable method) {
        Ruby runtime = implementationModule.getRuntime();
        UnboundMethod newMethod = new UnboundMethod(runtime);

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.method = method;

        return newMethod;
    }

    public static RubyClass defineUnboundMethodClass(Ruby runtime) {
        RubyClass newClass = runtime.defineClass("UnboundMethod", runtime.getClass("Method"));

        CallbackFactory callbackFactory = runtime.callbackFactory();
        newClass.defineMethod("[]", callbackFactory.getOptMethod(UnboundMethod.class, "call"));
        newClass.defineMethod("bind", callbackFactory.getMethod(UnboundMethod.class, "bind", IRubyObject.class));
        newClass.defineMethod("call", callbackFactory.getOptMethod(UnboundMethod.class, "call"));
        newClass.defineMethod("to_proc", callbackFactory.getMethod(UnboundMethod.class, "to_proc"));
        newClass.defineMethod("unbind", callbackFactory.getMethod(UnboundMethod.class, "unbind"));

        return newClass;
    }

    /**
     * @see org.jruby.Method#call(IRubyObject[])
     */
    public IRubyObject call(IRubyObject[] args) {
        throw new TypeError(runtime, "you cannot call unbound method; bind first");
    }

    /**
     * @see org.jruby.Method#to_proc()
     */
    public IRubyObject to_proc() {
        return super.to_proc();
    }

    /**
     * @see org.jruby.Method#unbind()
     */
    public UnboundMethod unbind() {
        return this;
    }

    public Method bind(IRubyObject receiver) {
        RubyClass receiverClass = receiver.getMetaClass();
        if (originModule != receiverClass) {
            if (originModule instanceof MetaClass) {
                throw new TypeError(runtime, "singleton method called for a different object");
            } else if (receiverClass instanceof MetaClass && receiverClass.getMethods().containsKey(originName)) {
                throw new TypeError(runtime, "method `" + originName + "' overridden");
            } else if (
                !(originModule.isModule() ? receiver.isKindOf(originModule) : receiver.getType() == originModule)) {
                // FIX replace type() == ... with isInstanceOf(...)
                throw new TypeError(runtime, "bind argument must be an instance of " + originModule.toName());
            }
        }
        return Method.newMethod(implementationModule, methodName, receiverClass, originName, method, receiver);
    }
}