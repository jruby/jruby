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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodBlock;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
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
    protected DynamicMethod method;
    protected IRubyObject receiver;

    protected RubyMethod(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    /** Create the RubyMethod class and add it to the Ruby runtime.
     * 
     */
    public static RubyClass createMethodClass(Ruby runtime) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here. Confirm. JRUBY-415
        RubyClass methodClass = runtime.defineClass("Method", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setMethod(methodClass);
        
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyMethod.class);
        
        methodClass.defineAnnotatedMethods(RubyMethod.class, callbackFactory);
        
        return methodClass;
    }

    public static RubyMethod newMethod(
        RubyModule implementationModule,
        String methodName,
        RubyModule originModule,
        String originName,
        DynamicMethod method,
        IRubyObject receiver) {
        Ruby runtime = implementationModule.getRuntime();
        RubyMethod newMethod = new RubyMethod(runtime, runtime.getMethod());

        newMethod.implementationModule = implementationModule;
        newMethod.methodName = methodName;
        newMethod.originModule = originModule;
        newMethod.originName = originName;
        newMethod.method = method.getRealMethod();
        newMethod.receiver = receiver;

        return newMethod;
    }

    /** Call the method.
     * 
     */
    @JRubyMethod(name = "call", name2 = "[]", rest = true, frame = true)
    public IRubyObject call(IRubyObject[] args, Block block) {
    	assert args != null;
        ThreadContext tc = getRuntime().getCurrentContext();

    	method.getArity().checkArity(getRuntime(), args);
        
        // FIXME: should lastClass be implementation module for a Method?
        return method.call(tc, receiver, implementationModule, methodName, args, block);
    }

    /** Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    @JRubyMethod(name = "arity")
    public RubyFixnum arity() {
        return getRuntime().newFixnum(method.getArity().getValue());
    }

    /** Create a Proc object.
     * 
     */
    @JRubyMethod(name = "to_proc", frame = true)
    public IRubyObject to_proc(Block unusedBlock) {
        CallbackFactory f = getRuntime().callbackFactory(RubyMethod.class);
        Ruby r = getRuntime();
        ThreadContext tc = r.getCurrentContext();
        Block block = MethodBlock.createMethodBlock(tc, tc.getCurrentScope().cloneScope(), f.getBlockMethod("bmcall"), this, r.getTopSelf());
        
        while (true) {
            try {
                // FIXME: We should not be regenerating this over and over
                return f.getSingletonMethod("mproc").execute(getRuntime().getNil(), 
                        IRubyObject.NULL_ARRAY, block);
            } catch (JumpException.BreakJump bj) {
                    return (IRubyObject) bj.getValue();
            } catch (JumpException.ReturnJump rj) {
                    return (IRubyObject) rj.getValue();
            } catch (JumpException.RetryJump rj) {
                    // Execute iterateMethod again.
            }
        }
    }

    /** Create a Proc object which is called like a ruby method.
     *
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject mproc(IRubyObject recv, Block block) {
    	Ruby runtime = recv.getRuntime();
    	ThreadContext tc = runtime.getCurrentContext();
        
        tc.preMproc();
        
        try {
            return RubyKernel.proc(recv, block);
        } finally {
            tc.postMproc();
        }
    }

    /** Delegate a block call to a bound method call.
     *
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject bmcall(IRubyObject blockArg, IRubyObject arg1, IRubyObject self, Block unusedBlock) {
        if (blockArg instanceof RubyArray) {
            // ENEBO: Very wrong
            return ((RubyMethod) arg1).call(((RubyArray) blockArg).toJavaArray(), Block.NULL_BLOCK);
        }
        // ENEBO: Very wrong
        return ((RubyMethod) arg1).call(new IRubyObject[] { blockArg }, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "unbind", frame = true)
    public RubyUnboundMethod unbind(Block unusedBlock) {
        RubyUnboundMethod unboundMethod =
        	RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, originName, method);
        unboundMethod.receiver = this;
        unboundMethod.infectBy(this);
        
        return unboundMethod;
    }
    
    @JRubyMethod(name = "inspect", name2 = "to_s")
    public IRubyObject inspect() {
        String cname = getMetaClass().getRealClass().getName();
        RubyString str = getRuntime().newString("#<" + cname + ": " + originModule.getName() + "#" + methodName + ">");
        str.setTaint(isTaint());
        return str;
    }
}

