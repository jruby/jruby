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
 * Copyright (C) 2012 The JRuby Community <www.jruby.org>
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
package org.jruby.internal.runtime.methods;

import com.headius.invokebinder.Binder;
import com.headius.invokebinder.Signature;
import com.headius.invokebinder.SmartBinder;
import com.headius.invokebinder.SmartHandle;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.jruby.Ruby;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static org.jruby.runtime.Helpers.arrayOf;

/**
 * In order to avoid the overhead with reflection-based method handles, this
 * MethodFactory uses ASM to generate tiny invoker classes. This allows for
 * better performance and more specialization per-handle than can be supported
 * via reflection. It also allows optimizing away many conditionals that can
 * be determined once ahead of time.
 * 
 * When running in secured environments, this factory may not function. When
 * this can be detected, MethodFactory will fall back on the reflection-based
 * factory instead.
 * 
 * @see org.jruby.runtime.MethodFactory
 */
public class InvokeDynamicMethodFactory extends InvocationMethodFactory {

    private static final Logger LOG = LoggerFactory.getLogger("InvokeDynamicMethodFactory");
    
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    
    /**
     */
    public InvokeDynamicMethodFactory(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public DynamicMethod getAnnotatedMethod(final RubyModule implementationClass, final List<JavaMethodDescriptor> descs) {
        JavaMethodDescriptor desc1 = descs.get(0);
        
        if (desc1.anno.frame()) {
            // super logic does not work yet because we need to take impl class
            // and method name from the DynamicMethod#call call, so punt to
            // generated class for now
            return super.getAnnotatedMethod(implementationClass, descs);
        }

        if (!Modifier.isPublic(desc1.getDeclaringClass().getModifiers())) {
            LOG.warn("warning: binding non-public class {}; reflected handles won't work", desc1.declaringClassName);
        }

        int min = Integer.MAX_VALUE;
        int max = 0;
        boolean notImplemented = false;

        for (JavaMethodDescriptor desc: descs) {
            int specificArity = -1;
            if (desc.optional == 0 && !desc.rest) {
                if (desc.required == 0) {
                    if (desc.actualRequired <= 3) {
                        specificArity = desc.actualRequired;
                    }
                } else if (desc.required >= 0 && desc.required <= 3) {
                    specificArity = desc.required;
                }
            }

            if (specificArity != -1) {
                if (specificArity < min) min = specificArity;
                if (specificArity > max) max = specificArity;
            } else {
                if (desc.required < min) min = desc.required;
                if (desc.rest) max = Integer.MAX_VALUE;
                if (desc.required + desc.optional > max) max = desc.required + desc.optional;
            }

            notImplemented = notImplemented || desc.anno.notImplemented();
        }

        DescriptorInfo info = new DescriptorInfo(descs);
        Callable<MethodHandle[]> targetsGenerator = new Callable<MethodHandle[]>() {
            @Override
            public MethodHandle[] call() throws Exception {
                return buildAnnotatedMethodHandles(implementationClass.getRuntime(), descs, implementationClass);
            }
        };
        
        return new HandleMethod(
                implementationClass,
                desc1.anno.visibility(),
                CallConfiguration.getCallConfig(info.isFrame(), info.isScope()),
                targetsGenerator,
                (min == max) ?
                        org.jruby.runtime.Signature.from(min, 0, 0, 0, 0, org.jruby.runtime.Signature.Rest.NONE, false) :
                        org.jruby.runtime.Signature.OPTIONAL,
                true,
                notImplemented,
                info.getParameterDesc());
    }

    private MethodHandle[] buildAnnotatedMethodHandles(Ruby runtime, List<JavaMethodDescriptor> descs, RubyModule implementationClass) {
        MethodHandle[] targets = new MethodHandle[5];

        int min = Integer.MAX_VALUE;
        int max = 0;

        JavaMethodDescriptor desc1 = descs.get(0);
        String rubyName;

        if (desc1.anno.name() != null && desc1.anno.name().length > 0) {
            // FIXME: Using this for super may super up the wrong name
            rubyName = desc1.anno.name()[0];
        } else {
            rubyName = desc1.name;
        }
        
        for (JavaMethodDescriptor desc: descs) {
            int specificArity = -1;
            if (desc.optional == 0 && !desc.rest) {
                if (desc.required == 0) {
                    if (desc.actualRequired <= 3) {
                        specificArity = desc.actualRequired;
                    }
                } else if (desc.required >= 0 && desc.required <= 3) {
                    specificArity = desc.required;
                }
            }

            if (specificArity != -1) {
                if (specificArity < min) min = specificArity;
                if (specificArity > max) max = specificArity;
            } else {
                if (desc.required < min) min = desc.required;
                if (desc.rest) max = Integer.MAX_VALUE;
                if (desc.required + desc.optional > max) max = desc.required + desc.optional;
            }

            String javaMethodName = desc.name;

            SmartBinder targetBinder;
            SmartHandle target;
            Signature baseSignature;
            if (specificArity >= 0) {
                baseSignature = SPECIFIC_ARITY_SIGNATURES[specificArity];
            } else {
                baseSignature = VARIABLE_ARITY_SIGNATURE;
            }
            
            targetBinder = SmartBinder.from(baseSignature);
            
            MethodHandle returnFilter = null;
            boolean castReturn = false;
            if (desc.returnClass != IRubyObject.class) {
                if (desc.returnClass == void.class) {
                    returnFilter = MethodHandles.constant(IRubyObject.class, runtime.getNil());
                } else {
                    castReturn = true;
                }
            }
            
            if (desc.isStatic) {
                if (desc.hasContext) {
                    if (desc.hasBlock) {
                        // straight through with no permutation necessary
                    } else {
                        targetBinder = targetBinder.exclude("block");
                    }
                } else {
                    if (desc.hasBlock) {
                        targetBinder = targetBinder.exclude("context");
                    } else {
                        targetBinder = targetBinder.exclude("context", "block");
                    }
                }
                
                if (returnFilter != null) {
                    targetBinder = targetBinder
                            .filterReturn(returnFilter);
                } else if (castReturn) {
                    targetBinder = targetBinder
                            .castReturn(desc.returnClass);
                }
            } else {
                if (desc.hasContext) {
                    if (desc.hasBlock) {
                        targetBinder = targetBinder.permute("self", "context", "arg*", "block");
                    } else {
                        targetBinder = targetBinder.permute("self", "context", "arg*");
                    }
                } else {
                    if (desc.hasBlock) {
                        targetBinder = targetBinder.permute("self", "arg*", "block");
                    } else {
                        targetBinder = targetBinder.permute("self", "arg*");
                    }
                }
                
                if (returnFilter != null) {
                    targetBinder = targetBinder
                            .filterReturn(returnFilter);
                } else if (castReturn) {
                    targetBinder = targetBinder
                            .castReturn(desc.returnClass);
                }
                targetBinder = targetBinder
                        .castArg("self", desc.getDeclaringClass());
            }
            
            if (desc.isStatic) {
                target = targetBinder
                        .invokeStaticQuiet(LOOKUP, desc.getDeclaringClass(), javaMethodName);
            } else {
                target = targetBinder
                        .invokeVirtualQuiet(LOOKUP, javaMethodName);
            }

            CallConfiguration callConfig = CallConfiguration.getCallConfigByAnno(desc.anno);
            if (!callConfig.isNoop()) {
                target = SmartHandle
                        .from(target.signature(), InvocationLinker.wrapWithFraming(baseSignature, callConfig, implementationClass, rubyName, target.handle(), null));
            }
            
            if (specificArity >= 0) {
                targets[specificArity] = target.handle();
            } else {
                targets[4] = target.handle();
            }
        }

        if (targets[4] == null) {
            // provide a variable-arity path for all specific-arity targets, or error path

            MethodHandle[] varargsTargets = new MethodHandle[4];
            for (int i = 0; i < 4; i++) {
                if (targets[i] == null) continue; // will never be retrieved; arity check will error first

                if (i == 0) {
                    varargsTargets[i] = MethodHandles.dropArguments(targets[i], 2, IRubyObject[].class);
                } else {
                    varargsTargets[i] = SmartBinder
                            .from(VARIABLE_ARITY_SIGNATURE)
                            .permute("context", "self", "block", "args")
                            .spread("arg", i)
                            .permute("context", "self", "arg*", "block")
                            .invoke(targets[i]).handle();
                }
            }
            
            SmartHandle HANDLE_GETTER = SmartBinder
                    .from(Signature.returning(MethodHandle.class).appendArg("targets", MethodHandle[].class).appendArg("arity", int.class))
                    .arrayGet();
            
            SmartHandle handleLookup = SmartBinder
                    .from(Signature.returning(MethodHandle.class).appendArg("args", IRubyObject[].class))
                    .filterReturn(HANDLE_GETTER.bindTo(varargsTargets))
                    .cast(int.class, Object.class)
                    .invokeStaticQuiet(LOOKUP, Array.class, "getLength");

            SmartHandle variableCall = SmartBinder
                    .from(VARIABLE_ARITY_SIGNATURE)
                    .fold("handle", handleLookup)
                    .invoker();
            
            targets[4] = variableCall.handle();
        }

        targets[4] = SmartBinder
                .from(VARIABLE_ARITY_SIGNATURE)
                .foldVoid(SmartBinder
                        .from(VARIABLE_ARITY_SIGNATURE.changeReturn(int.class))
                        .permute("context", "args")
                        .append(arrayOf("min", "max", "name"), arrayOf(int.class, int.class, String.class), min, max, rubyName)
                        .invokeStaticQuiet(LOOKUP, Arity.class, "checkArgumentCount")
                        .handle())
                .invoke(targets[4])
                .handle();

        // TODO: tracing
        
        return targets;
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.runtime.MethodFactory#getAnnotatedMethod
     */
    @Override
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, JavaMethodDescriptor desc) {
        return getAnnotatedMethod(implementationClass, Arrays.asList(desc));
    }
    
    public static final Signature VARIABLE_ARITY_SIGNATURE = Signature
            .returning(IRubyObject.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("self", IRubyObject.class)
            .appendArg("args", IRubyObject[].class)
            .appendArg("block", Block.class);
    
    public static final Signature ARITY_CHECK_FOLD = Signature
            .returning(void.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("args", IRubyObject[].class);
    
    public static final Signature ARITY_CHECK_SIGNATURE = Signature
            .returning(int.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("args", IRubyObject[].class)
            .appendArg("min", int.class)
            .appendArg("max", int.class);
    
    public static final Signature[] SPECIFIC_ARITY_SIGNATURES;
    static {
            Signature[] specifics = new Signature[4];
            Signature specific = Signature
                    .returning(IRubyObject.class)
                    .appendArg("context", ThreadContext.class)
                    .appendArg("self", IRubyObject.class);
            
            specifics[0] = specific.appendArg("block", Block.class);
            
            for (int i = 0; i < 3; i++) {
                specific = specific
                        .appendArg("arg" + i, IRubyObject.class);
                specifics[i + 1] = specific.appendArg("block", Block.class);
            }
            SPECIFIC_ARITY_SIGNATURES = specifics;
    }
    
    private static final SmartBinder[] SPREAD_BINDERS = new SmartBinder[4];
    static {
        for (int i = 0; i < 4; i++) {
            SPREAD_BINDERS[i] = SmartBinder
                    .from(VARIABLE_ARITY_SIGNATURE)
                    .permute("context", "self", "block", "args")
                    .spread("arg", i)
                    .permute("context", "self", "arg*", "block");
        }
    }

    static final MethodHandle ARITY_ERROR_HANDLE = SmartBinder
            .from(VARIABLE_ARITY_SIGNATURE.appendArgs(arrayOf("min", "max"), int.class, int.class))
            .filterReturn(Binder.from(IRubyObject.class, int.class).drop(0).constant(null))
            .permute(ARITY_CHECK_SIGNATURE)
            .invokeStaticQuiet(LOOKUP, Arity.class, "checkArgumentCount").handle();
}
