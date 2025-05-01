/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jruby.runtime.Block;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jruby.Ruby;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.runtime.invokedynamic.InvocationLinker;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

/**
 * This invoker uses MethodHandle for all bindings to Java code, rather than generating
 * stubs or using reflection.
 *
 * @see org.jruby.runtime.MethodFactory
 * @see HandleMethod
 */
public class InvokeDynamicMethodFactory extends InvocationMethodFactory {

    private static final Logger LOG = LoggerFactory.getLogger(InvokeDynamicMethodFactory.class);

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     */
    public InvokeDynamicMethodFactory(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public DynamicMethod getAnnotatedMethod(final RubyModule implementationClass, final List<JavaMethodDescriptor> descs, String name) {
        JavaMethodDescriptor desc1 = descs.get(0);
        DescriptorInfo info = new DescriptorInfo(desc1);

        if (desc1.anno.frame()) {
            // super logic does not work yet because we need to take impl class
            // and method name from the DynamicMethod#call call, so punt to
            // generated class for now
            return super.getAnnotatedMethod(implementationClass, descs, name);
        }

        if (!Modifier.isPublic(desc1.declaringClass.getModifiers())) {
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

        Supplier<MethodHandle>[] generators = buildAnnotatedMethodHandles(implementationClass.getRuntime(), descs, implementationClass);

        return new HandleMethod(
                implementationClass,
                desc1.anno.visibility().getDefaultVisibilityFor(desc1.name),
                desc1.name,
                (min == max) ?
                        org.jruby.runtime.Signature.from(min, 0, 0, 0, 0, org.jruby.runtime.Signature.Rest.NONE, -1).encode() :
                        org.jruby.runtime.Signature.OPTIONAL.encode(),
                true,
                notImplemented,
                info.getParameterDesc(),
                min,
                max,
                desc1.anno.checkArity(),
                generators[0],
                generators[1],
                generators[2],
                generators[3],
                generators[4]);
    }

    private Supplier<MethodHandle>[] buildAnnotatedMethodHandles(Ruby runtime, List<JavaMethodDescriptor> descs, RubyModule implementationClass) {
        int min = Integer.MAX_VALUE;
        int max = 0;

        // FIXME: Using desc.anno.name()[0] for super may super up the wrong name
        final String rubyName = descs.get(0).rubyName;

        Supplier<MethodHandle>[] targets = new Supplier[5];

        for (JavaMethodDescriptor desc: descs) {
            MethodHandle method;

            if (desc.isStatic) {
                method = Binder.from(desc.returnClass, desc.parameters).invokeStaticQuiet(LOOKUP, desc.declaringClass, desc.name);
            } else {
                method = Binder.from(desc.returnClass, desc.declaringClass, desc.parameters).invokeVirtualQuiet(LOOKUP, desc.name);
            }

            Supplier<MethodHandle> target = adaptHandle(method, runtime, desc.actualRequired, desc.required, desc.optional, desc.rest, rubyName, desc.declaringClass, desc.isStatic, desc.hasContext, desc.hasBlock, desc.anno.frame(), implementationClass);
            int specificArity = -1;
            if (desc.required < 4 && desc.optional == 0 && !desc.rest) {
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

            if (specificArity >= 0) {
                targets[specificArity] = target;
            } else {
                targets[4] = target;
            }
        }

        return targets;
    }

    public static SmartBinder preAdaptHandle(int specificArity, final boolean isStatic, final boolean hasContext, final boolean hasBlock) {
        SmartBinder targetBinder;

        Signature baseSignature;
        if (specificArity >= 0) {
            baseSignature = SPECIFIC_ARITY_SIGNATURES[specificArity];
        } else {
            baseSignature = VARIABLE_ARITY_SIGNATURE;
        }

        targetBinder = SmartBinder.from(baseSignature);

        // unused by Java-based methods
        targetBinder = targetBinder.exclude("class", "name");

        if (isStatic) {
            if (hasContext) {
                if (hasBlock) {
                    // straight through with no permutation necessary
                } else {
                    targetBinder = targetBinder.exclude("block");
                }
            } else {
                if (hasBlock) {
                    targetBinder = targetBinder.exclude("context");
                } else {
                    targetBinder = targetBinder.exclude("context", "block");
                }
            }
        } else {
            if (hasContext) {
                if (hasBlock) {
                    targetBinder = targetBinder.permute("self", "context", "arg*", "block");
                } else {
                    targetBinder = targetBinder.permute("self", "context", "arg*");
                }
            } else {
                if (hasBlock) {
                    targetBinder = targetBinder.permute("self", "arg*", "block");
                } else {
                    targetBinder = targetBinder.permute("self", "arg*");
                }
            }
        }

        return targetBinder;
    }

    public static MethodHandle finishAdapting(final SmartBinder binder, final RubyModule implementationClass, String rubyName, final MethodHandle method, final Class declaringClass, final Ruby runtime, boolean isStatic, boolean frame) {
        Class returnClass = method.type().returnType();
        SmartBinder targetBinder = binder;

        if (returnClass != IRubyObject.class) {
            if (returnClass == void.class) {
                targetBinder = targetBinder.filterReturn(MethodHandles.constant(IRubyObject.class, runtime.getNil()));
            } else {
                targetBinder = targetBinder
                        .castReturn(returnClass);
            }
        }

        if (!isStatic) {
            targetBinder = targetBinder
                    .castArg("self", declaringClass);
        }

        SmartHandle smartTarget = targetBinder.invoke(method);
        if (frame) {
            smartTarget = SmartHandle
                    .from(smartTarget.signature(), InvocationLinker.wrapWithFrameOnly(binder.baseSignature(), implementationClass, rubyName, smartTarget.handle()));
        }

        MethodHandle target = smartTarget.handle();

        // Add tracing of "C" call/return
        if (Options.DEBUG_FULLTRACE.load()) {
            MethodHandle traceCall = Binder
                    .from(target.type().changeReturnType(void.class))
                    .permute(0, 3, 2) // context, name, class
                    .insert(1, RubyEvent.C_CALL)
                    .invokeVirtualQuiet(LOOKUP, "trace");

            MethodHandle traceReturn = Binder
                    .from(target.type().changeReturnType(void.class))
                    .permute(0, 3, 2) // context, name, class
                    .insert(1, RubyEvent.C_RETURN)
                    .invokeVirtualQuiet(LOOKUP, "trace");

            target = Binder
                    .from(target.type())
                    .foldVoid(traceCall)
                    .tryFinally(traceReturn)
                    .invoke(target);
        }

        return target;
    }

    public static Supplier<MethodHandle> adaptHandle(final MethodHandle method, final Ruby runtime, final int actualRequired, final int required, final int optional, final boolean rest, final String rubyName, final Class declaringClass, final boolean isStatic, final boolean hasContext, final boolean hasBlock, final boolean frame, final RubyModule implementationClass) {
        return () -> {
            int specificArity = -1;
            if (optional == 0 && !rest) {
                if (required == 0) {
                    if (actualRequired <= 3) {
                        specificArity = actualRequired;
                    }
                } else if (required >= 0 && required <= 3) {
                    specificArity = required;
                }
            }

            SmartBinder targetBinder = getBinder(specificArity, isStatic, hasContext, hasBlock);

            return finishAdapting(targetBinder, implementationClass, rubyName, method, declaringClass, runtime, isStatic, frame);
        };
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     *
     * @see org.jruby.runtime.MethodFactory#getAnnotatedMethod
     */
    @Override
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, JavaMethodDescriptor desc, String name) {
        return getAnnotatedMethod(implementationClass, Collections.singletonList(desc), name);
    }

    public static final Signature VARIABLE_ARITY_SIGNATURE = Signature
            .returning(IRubyObject.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("self", IRubyObject.class)
            .appendArg("class", RubyModule.class)
            .appendArg("name", String.class)
            .appendArg("args", IRubyObject[].class)
            .appendArg("block", Block.class);

    public static final Signature[] SPECIFIC_ARITY_SIGNATURES;
    static {
            Signature[] specifics = new Signature[4];
            Signature specific = Signature
                    .returning(IRubyObject.class)
                    .appendArg("context", ThreadContext.class)
                    .appendArg("self", IRubyObject.class)
                    .appendArg("class", RubyModule.class)
                    .appendArg("name", String.class);

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

    private static final Map<String, SmartBinder> BINDERS;
    public static SmartBinder getBinder(int specific, final boolean b1, final boolean b2, final boolean b3) {
        return BINDERS.get("" + specific + b1 + b2 + b3);
    }
    static {
        Map<String, SmartBinder> binders = new HashMap<>(40);
        for (int specific = -1; specific <= 3; specific++) {
            for (boolean b1 : new boolean[] {false, true}) {
                for (boolean b2 : new boolean[] {false, true}) {
                    for (boolean b3 : new boolean[]{false, true}) {
                        binders.put("" + specific + b1 + b2 + b3, preAdaptHandle(specific, b1, b2, b3));
                    }
                }
            }
        }
        BINDERS = binders;
    }
}
