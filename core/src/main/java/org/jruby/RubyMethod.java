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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.ext.jruby.JRubyLibrary;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.IRMethodArgs;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.CompiledBlockCallback19;
import org.jruby.runtime.CompiledBlockLight;
import org.jruby.runtime.CompiledBlockLight19;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
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
@JRubyClass(name="Method")
public class RubyMethod extends AbstractRubyMethod {
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

        methodClass.setClassIndex(ClassIndex.METHOD);
        methodClass.setReifiedClass(RubyMethod.class);

        methodClass.defineAnnotatedMethods(AbstractRubyMethod.class);
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
        newMethod.method = method;
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
    @JRubyMethod
    public RubyFixnum arity() {
        int value;
        if (method instanceof IRMethodArgs) {
            value = ((IRMethodArgs) method).getSignature().arityValue();
        } else {
            value = method.getArity().getValue();
        }

        return getRuntime().newFixnum(value);
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public RubyBoolean op_equal(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyMethod)) {
            return context.runtime.getFalse();
        }
        if (method instanceof ProcMethod) {
            return context.runtime.newBoolean(((ProcMethod) method).isSame(((RubyMethod) other).getMethod()));
        }
        RubyMethod otherMethod = (RubyMethod)other;
        return context.runtime.newBoolean(receiver == otherMethod.receiver &&
                originModule == otherMethod.originModule &&
                (isMethodMissingMatch(otherMethod.getMethod().getRealMethod()) || isSerialMatch(otherMethod.getMethod().getRealMethod()))
        );
    }

    private boolean isMethodMissingMatch(DynamicMethod other) {
        return (method.getRealMethod() instanceof RubyModule.RespondToMissingMethod) &&
                ((RubyModule.RespondToMissingMethod)method.getRealMethod()).equals(other);
    }

    private boolean isSerialMatch(DynamicMethod otherMethod) {
        return method.getRealMethod().getSerialNumber() == otherMethod.getRealMethod().getSerialNumber();
    }

    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject op_eql19(ThreadContext context, IRubyObject other) {
        return op_equal(context, other);
    }

    @JRubyMethod(name = "clone")
    @Override
    public RubyMethod rbClone() {
        return newMethod(implementationModule, methodName, originModule, originName, method, receiver);
    }

    /** Create a Proc object.
     * 
     */
    @JRubyMethod
    public IRubyObject to_proc(ThreadContext context, Block unusedBlock) {
        Ruby runtime = context.runtime;
        CompiledBlockCallback19 callback = new CompiledBlockCallback19() {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                return method.call(context, receiver, originModule, originName, args, block);
            }

            @Override
            public String getFile() {
                return getFilename();
            }

            @Override
            public int getLine() {
                return RubyMethod.this.getLine();
            }
        };

        BlockBody body;
        if (method instanceof IRMethodArgs) {
            Signature signature = ((IRMethodArgs) method).getSignature();
            int argumentType = resolveArgumentType(signature.isFixed(), signature.required());
            body = CompiledBlockLight19.newCompiledBlockLight(((IRMethodArgs) method).getSignature(),
                    runtime.getStaticScopeFactory().getDummyScope(), callback, false, argumentType, JRubyLibrary.MethodExtensions.methodParameters(runtime, method));
        } else {
            Arity arity = method.getArity();
            int argumentType = resolveArgumentType(arity.isFixed(), arity.required());
            body = CompiledBlockLight19.newCompiledBlockLight(method.getArity(),
                    runtime.getStaticScopeFactory().getDummyScope(), callback, false, argumentType, JRubyLibrary.MethodExtensions.methodParameters(runtime, method));
        }
        Block b = new Block(body, context.currentBinding(receiver, Visibility.PUBLIC));
        
        return RubyProc.newProc(runtime, b, Block.Type.LAMBDA);
    }

    private int resolveArgumentType(boolean isFixed, int required) {
        if (!isFixed) return BlockBody.MULTIPLE_ASSIGNMENT;
        if (required > 0) return BlockBody.MULTIPLE_ASSIGNMENT;
        return BlockBody.ZERO_ARGS;
    }

    @JRubyMethod
    public RubyUnboundMethod unbind() {
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

    @JRubyMethod
    public IRubyObject receiver(ThreadContext context) {
        return receiver;
    }

    @JRubyMethod
    public IRubyObject owner(ThreadContext context) {
        return implementationModule;
    }

    @JRubyMethod
    public IRubyObject source_location(ThreadContext context) {
        Ruby runtime = context.runtime;

        String filename = getFilename();
        if (filename != null) {
            return runtime.newArray(runtime.newString(filename), runtime.newFixnum(getLine()));
        }

        return context.runtime.getNil();
    }

    public String getFilename() {
        DynamicMethod realMethod = method.getRealMethod(); // Follow Aliases
        if (realMethod instanceof PositionAware) {
            PositionAware poser = (PositionAware) realMethod;
            return poser.getFile();
        }
        return null;
    }

    public int getLine() {
        DynamicMethod realMethod = method.getRealMethod(); // Follow Aliases
        if (realMethod instanceof PositionAware) {
            PositionAware poser = (PositionAware) realMethod;
            return poser.getLine() + 1;
        }
        return -1;
    }

    @JRubyMethod
    public IRubyObject parameters(ThreadContext context) {
        return JRubyLibrary.MethodExtensions.methodArgs(this);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject curry(ThreadContext context, IRubyObject[] args) {
        return to_proc(context, Block.NULL_BLOCK).callMethod(context, "curry", args);
    }

    @JRubyMethod
    public IRubyObject super_method(ThreadContext context) {
        RubyModule superClass = Helpers.findImplementerIfNecessary(receiver.getMetaClass(), implementationModule).getSuperClass();
        return super_method(context, receiver, superClass);
    }

    @JRubyMethod
    public IRubyObject original_name(ThreadContext context) {
        if (method instanceof AliasMethod) {
            return context.runtime.newString(((AliasMethod)method).getOldName());
        }
        return name(context);
    }
}

