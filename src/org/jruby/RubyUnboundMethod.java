/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * Note: This was renamed from UnboundMethod.java
 * 
 * @author jpetersen
 */
public class RubyUnboundMethod extends RubyMethod {
    protected RubyUnboundMethod(IRuby runtime) {
        super(runtime, runtime.getClass("UnboundMethod"));
    }

    public static RubyUnboundMethod newUnboundMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        DynamicMethod method) {
        RubyUnboundMethod newMethod = new RubyUnboundMethod(implementationModule.getRuntime());

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.method = method;

        return newMethod;
    }

    public static RubyClass defineUnboundMethodClass(IRuby runtime) {
        RubyClass newClass = 
        	runtime.defineClass("UnboundMethod", runtime.getClass("Method"));

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyUnboundMethod.class);
        newClass.defineMethod("[]", callbackFactory.getOptMethod("call"));
        newClass.defineMethod("bind", callbackFactory.getMethod("bind", IRubyObject.class));
        newClass.defineMethod("call", callbackFactory.getOptMethod("call"));
        newClass.defineMethod("to_proc", callbackFactory.getMethod("to_proc"));
        newClass.defineMethod("unbind", callbackFactory.getMethod("unbind"));

        return newClass;
    }

    /**
     * @see org.jruby.RubyMethod#call(IRubyObject[])
     */
    public IRubyObject call(IRubyObject[] args) {
        throw getRuntime().newTypeError("you cannot call unbound method; bind first");
    }

    /**
     * @see org.jruby.RubyMethod#unbind()
     */
    public RubyUnboundMethod unbind() {
        return this;
    }

    public RubyMethod bind(IRubyObject aReceiver) {
        RubyClass receiverClass = aReceiver.getMetaClass();
        
        if (!aReceiver.isKindOf(originModule)) {
            if (originModule instanceof MetaClass) {
                throw getRuntime().newTypeError("singleton method called for a different object");
            } else if (receiverClass instanceof MetaClass && receiverClass.getMethods().containsKey(originName)) {
                throw getRuntime().newTypeError("method `" + originName + "' overridden");
            } else if (
                !(originModule.isModule() ? aReceiver.isKindOf(originModule) : aReceiver.getType() == originModule)) {
                // FIX replace type() == ... with isInstanceOf(...)
                throw getRuntime().newTypeError("bind argument must be an instance of " + originModule.getName());
            }
        }
        return RubyMethod.newMethod(implementationModule, methodName, receiverClass, originName, method, aReceiver);
    }
}
