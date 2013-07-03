/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.CompatVersion.*;

/**
 * 
 * Note: This was renamed from UnboundMethod.java
 * 
 * @author jpetersen
 */
@JRubyClass(name="UnboundMethod", parent="Method")
public class RubyUnboundMethod extends RubyMethod {
    protected RubyUnboundMethod(Ruby runtime) {
        super(runtime, runtime.getUnboundMethod());
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

    public static RubyClass defineUnboundMethodClass(Ruby runtime) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here. Confirm. JRUBY-415
        RubyClass newClass = 
        	runtime.defineClass("UnboundMethod", runtime.getMethod(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setUnboundMethod(newClass);

        newClass.index = ClassIndex.UNBOUNDMETHOD;
        newClass.setReifiedClass(RubyUnboundMethod.class);

        newClass.defineAnnotatedMethods(RubyUnboundMethod.class);

        return newClass;
    }

    /**
     * @see org.jruby.RubyMethod#call(IRubyObject[])
     */
    @JRubyMethod(name = {"call", "[]"}, rest = true)
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
        throw context.runtime.newTypeError("you cannot call unbound method; bind first");
    }

    /**
     * @see org.jruby.RubyMethod#unbind()
     */
    @JRubyMethod
    @Override
    public RubyUnboundMethod unbind() {
        return this;
    }

    @JRubyMethod
    public RubyMethod bind(ThreadContext context, IRubyObject aReceiver) {
        RubyClass receiverClass = aReceiver.getMetaClass();
        
        receiverClass.checkValidBindTargetFrom(context, originModule);
        
        return RubyMethod.newMethod(implementationModule, methodName, receiverClass, originName, method, aReceiver);
    }

    @JRubyMethod(name = "clone")
    @Override
    public RubyMethod rbClone() {
        return newUnboundMethod(implementationModule, methodName, originModule, originName, method);
    }

    @JRubyMethod
    @Override
    public IRubyObject to_proc(ThreadContext context, Block unusedBlock) {
        return super.to_proc(context, unusedBlock);
    }

    @JRubyMethod(compat = RUBY1_8)
    public IRubyObject name(ThreadContext context) {
        return context.runtime.newString(methodName);
    }

    @JRubyMethod(name = "name", compat = RUBY1_9)
    public IRubyObject name19(ThreadContext context) {
        return context.runtime.newSymbol(methodName);
    }

    @JRubyMethod(name = "owner")
    public IRubyObject owner(ThreadContext context) {
        return implementationModule;
    }
}
