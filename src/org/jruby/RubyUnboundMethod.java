/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>, Charles O Nutter <headius@headius.com>
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
 * Note: This was renamed from UnboundMethod.java
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyUnboundMethod extends RubyMethod {
    protected RubyUnboundMethod(Ruby runtime) {
        super(runtime, runtime.getClass("RubyUnboundMethod"));
    }

    public static RubyUnboundMethod newUnboundMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        ICallable method) {
        Ruby runtime = implementationModule.getRuntime();
        RubyUnboundMethod newMethod = new RubyUnboundMethod(runtime);

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.method = method;

        return newMethod;
    }

    public static RubyClass defineUnboundMethodClass(Ruby runtime) {
        RubyClass newClass = runtime.defineClass("RubyUnboundMethod", runtime.getClass("RubyMethod"));

        CallbackFactory callbackFactory = runtime.callbackFactory();
        newClass.defineMethod("[]", callbackFactory.getOptMethod(RubyUnboundMethod.class, "call"));
        newClass.defineMethod("bind", callbackFactory.getMethod(RubyUnboundMethod.class, "bind", IRubyObject.class));
        newClass.defineMethod("call", callbackFactory.getOptMethod(RubyUnboundMethod.class, "call"));
        newClass.defineMethod("to_proc", callbackFactory.getMethod(RubyUnboundMethod.class, "to_proc"));
        newClass.defineMethod("unbind", callbackFactory.getMethod(RubyUnboundMethod.class, "unbind"));

        return newClass;
    }

    /**
     * @see org.jruby.RubyMethod#call(IRubyObject[])
     */
    public IRubyObject call(IRubyObject[] args) {
        throw new TypeError(runtime, "you cannot call unbound method; bind first");
    }

    /**
     * @see org.jruby.RubyMethod#unbind()
     */
    public RubyUnboundMethod unbind() {
        return this;
    }

    public RubyMethod bind(IRubyObject receiver) {
        RubyClass receiverClass = receiver.getMetaClass();
        if (originModule != receiverClass) {
            if (originModule instanceof MetaClass) {
                throw new TypeError(runtime, "singleton method called for a different object");
            } else if (receiverClass instanceof MetaClass && receiverClass.getMethods().containsKey(originName)) {
                throw new TypeError(runtime, "method `" + originName + "' overridden");
            } else if (
                !(originModule.isModule() ? receiver.isKindOf(originModule) : receiver.getType() == originModule)) {
                // FIX replace type() == ... with isInstanceOf(...)
                throw new TypeError(runtime, "bind argument must be an instance of " + originModule.getName());
            }
        }
        return RubyMethod.newMethod(implementationModule, methodName, receiverClass, originName, method, receiver);
    }
}