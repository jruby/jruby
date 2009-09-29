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
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodBlock;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

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
@JRubyClass(name="Method")
public class RubyMethod extends RubyObject implements DataType {
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
        
        methodClass.defineAnnotatedMethods(RubyMethod.class);
        
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
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, Block block) {
        return method.call(context, receiver, implementationModule, methodName, block);
    }
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, IRubyObject arg, Block block) {
        return method.call(context, receiver, implementationModule, methodName, arg, block);
    }
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return method.call(context, receiver, implementationModule, methodName, arg0, arg1, block);
    }
    @JRubyMethod(name = {"call", "[]"})
    public IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return method.call(context, receiver, implementationModule, methodName, arg0, arg1, arg2, block);
    }
    @JRubyMethod(name = {"call", "[]"}, rest = true)
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
        return method.call(context, receiver, implementationModule, methodName, args, block);
    }

    /** Returns the number of arguments a method accepted.
     * 
     * @return the number of arguments of a method.
     */
    @JRubyMethod(name = "arity")
    public RubyFixnum arity() {
        return getRuntime().newFixnum(method.getArity().getValue());
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyMethod)) return context.getRuntime().getFalse();
        RubyMethod otherMethod = (RubyMethod)other;
        return context.getRuntime().newBoolean(implementationModule == otherMethod.implementationModule &&
                                       originModule == otherMethod.originModule &&
                                       receiver == otherMethod.receiver &&
                                       method.getRealMethod() == otherMethod.method.getRealMethod());
    }

    @JRubyMethod(name = "clone")
    @Override
    public RubyMethod rbClone() {
        return newMethod(implementationModule, methodName, originModule, originName, method, receiver);
    }

    /** Create a Proc object.
     * 
     */
    @JRubyMethod(name = "to_proc", frame = true)
    public IRubyObject to_proc(ThreadContext context, Block unusedBlock) {
        Ruby runtime = context.getRuntime();
        DynamicScope currentScope = context.getCurrentScope();
        MethodBlock mb = new MethodBlock(this, currentScope.getStaticScope()) {
            @Override
            public IRubyObject callback(IRubyObject value, IRubyObject method, IRubyObject self, Block block) {
                return bmcall(value, method, self, block);
            }
        };
        Block block = MethodBlock.createMethodBlock(context, runtime.getTopSelf(), context.getCurrentScope(), mb);
        
        while (true) {
            try {
                // FIXME: We should not be regenerating this over and over
                return mproc(context, block);
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
    private IRubyObject mproc(ThreadContext context, Block block) {
        try {
            context.preMproc();
            
            return RubyKernel.proc(context, context.getRuntime().getNil(), block);
        } finally {
            context.postMproc();
        }
    }

    /** Delegate a block call to a bound method call.
     *
     * Used by the RubyMethod#to_proc method.
     *
     */
    public static IRubyObject bmcall(IRubyObject blockArg, IRubyObject arg1,
            IRubyObject self, Block unusedBlock) {
        ThreadContext context = arg1.getRuntime().getCurrentContext();

        if (blockArg == null) {
            return ((RubyMethod) arg1).call(context, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        } else if (blockArg instanceof RubyArray) {
            // ENEBO: Very wrong
            return ((RubyMethod) arg1).call(context, ((RubyArray) blockArg).toJavaArray(), Block.NULL_BLOCK);
        }
        // ENEBO: Very wrong
        return ((RubyMethod) arg1).call(context, new IRubyObject[] { blockArg }, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "unbind", frame = true)
    public RubyUnboundMethod unbind(Block unusedBlock) {
        RubyUnboundMethod unboundMethod =
        	RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, originName, method);
        unboundMethod.infectBy(this);
        
        return unboundMethod;
    }
    
    @JRubyMethod(name = {"inspect", "to_s"})
    @Override
    public IRubyObject inspect() {
        StringBuilder buf = new StringBuilder("#<");
        char delimeter = '#';
        
        buf.append(getMetaClass().getRealClass().getName()).append(": ");
        
        if (implementationModule.isSingleton()) {
            IRubyObject attached = ((MetaClass) implementationModule).getAttached();
            if (receiver == null) {
                buf.append(implementationModule.inspect().toString());
            } else if (receiver == attached) {
                buf.append(attached.inspect().toString());
                delimeter = '.';
            } else {
                buf.append(receiver.inspect().toString());
                buf.append('(').append(attached.inspect().toString()).append(')');
                delimeter = '.';
            }
        } else {
            buf.append(originModule.getName());
            
            if (implementationModule != originModule) {
                buf.append('(').append(implementationModule.getName()).append(')');
            }
        }
        
        buf.append(delimeter).append(methodName).append('>');
        
        RubyString str = getRuntime().newString(buf.toString());
        str.setTaint(isTaint());
        return str;
    }

    @JRubyMethod(name = "name", compat = CompatVersion.RUBY1_8)
    public IRubyObject name(ThreadContext context) {
        return context.getRuntime().newString(methodName);
    }

    @JRubyMethod(name = "name", compat = CompatVersion.RUBY1_9)
    public IRubyObject name19(ThreadContext context) {
        return context.getRuntime().newSymbol(methodName);
    }

    @JRubyMethod(name = "receiver")
    public IRubyObject receiver(ThreadContext context) {
        return receiver;
    }

    @JRubyMethod(name = "owner")
    public IRubyObject owner(ThreadContext context) {
        return implementationModule;
    }
}

