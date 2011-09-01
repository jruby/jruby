/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2009 MenTaLguY <mental@rydia.net>
 * Copyright (C) 2010 Charles Oliver Nutter <headius@headius.com>
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

import org.jruby.ast.RestArgNode;
import org.jruby.ext.jruby.JRubyUtilLibrary;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.anno.JRubyClass;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.ListNode;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;

import org.jruby.ast.Node;
import org.jruby.ast.types.INameNode;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.internal.runtime.methods.MethodArgs;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.InterpretedBlock;
import org.jruby.runtime.ThreadContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.util.Map;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.internal.runtime.methods.MethodArgs2;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.ObjectAllocator;
import static org.jruby.runtime.Visibility.*;

/**
 * Module which defines JRuby-specific methods for use. 
 */
@JRubyModule(name="JRuby")
public class RubyJRuby {
    public static RubyModule createJRuby(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        runtime.getKernel().callMethod(context, "require", runtime.newString("java"));
        RubyModule jrubyModule = runtime.getOrCreateModule("JRuby");

        jrubyModule.defineAnnotatedMethods(RubyJRuby.class);
        jrubyModule.defineAnnotatedMethods(JRubyUtilLibrary.class);

        RubyClass threadLocalClass = jrubyModule.defineClassUnder("ThreadLocal", runtime.getObject(), JRubyThreadLocal.ALLOCATOR);
        threadLocalClass.defineAnnotatedMethods(JRubyExecutionContextLocal.class);

        RubyClass fiberLocalClass = jrubyModule.defineClassUnder("FiberLocal", runtime.getObject(), JRubyFiberLocal.ALLOCATOR);
        fiberLocalClass.defineAnnotatedMethods(JRubyExecutionContextLocal.class);

        return jrubyModule;
    }

    public static RubyModule createJRubyExt(Ruby runtime) {
        runtime.getKernel().callMethod(runtime.getCurrentContext(),"require", runtime.newString("java"));
        RubyModule mJRubyExt = runtime.getOrCreateModule("JRuby").defineModuleUnder("Extensions");
        
        mJRubyExt.defineAnnotatedMethods(JRubyExtensions.class);

        runtime.getObject().includeModule(mJRubyExt);

        return mJRubyExt;
    }

    public static void createJRubyCoreExt(Ruby runtime) {
        runtime.getClassClass().defineAnnotatedMethods(JRubyClassExtensions.class);
        runtime.getThread().defineAnnotatedMethods(JRubyThreadExtensions.class);
        runtime.getString().defineAnnotatedMethods(JRubyStringExtensions.class);
    }

    @JRubyMethod(module = true)
    public static IRubyObject reference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Ruby runtime = context.getRuntime();

        return Java.getInstance(runtime, obj, true);
    }

    @JRubyMethod(module = true)
    public static IRubyObject reference0(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Ruby runtime = context.getRuntime();

        return Java.getInstance(runtime, obj);
    }

    @JRubyMethod(module = true)
    public static IRubyObject dereference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Object unwrapped;

        if (obj instanceof JavaProxy) {
            unwrapped = ((JavaProxy)obj).getObject();
        } else if (obj.dataGetStruct() instanceof JavaObject) {
            unwrapped = JavaUtil.unwrapJavaObject(obj);
        } else {
            throw context.getRuntime().newTypeError("got " + obj + ", expected wrapped Java object");
        }

        if (!(unwrapped instanceof IRubyObject)) {
            throw context.getRuntime().newTypeError("got " + obj + ", expected Java-wrapped Ruby object");
        }

        return (IRubyObject)unwrapped;
    }

    public abstract static class JRubyExecutionContextLocal extends RubyObject {
        private IRubyObject default_value;
        private RubyProc default_proc;

        public JRubyExecutionContextLocal(Ruby runtime, RubyClass type) {
            super(runtime, type);
            default_value = runtime.getNil();
            default_proc = null;
        }

        @JRubyMethod(name="initialize", required=0, optional=1)
        public IRubyObject rubyInitialize(ThreadContext context, IRubyObject args[], Block block) {
            if (block.isGiven()) {
                if (args.length != 0) {
                    throw context.getRuntime().newArgumentError("wrong number of arguments");
                }
                default_proc = block.getProcObject();
                if (default_proc == null) {
                    default_proc = RubyProc.newProc(context.getRuntime(), block, block.type);
                }
            } else {
                if (args.length == 1) {
                    default_value = args[0];
                } else if (args.length != 0) {
                    throw context.getRuntime().newArgumentError("wrong number of arguments");
                }
            }
            return context.getRuntime().getNil();
        }

        @JRubyMethod(name="default", required=0)
        public IRubyObject getDefault(ThreadContext context) {
            return default_value;
        }

        @JRubyMethod(name="default_proc", required=0)
        public IRubyObject getDefaultProc(ThreadContext context) {
            if (default_proc != null) {
                return default_proc;
            } else {
                return context.getRuntime().getNil();
            }
        }

        private static final IRubyObject[] EMPTY_ARGS = new IRubyObject[]{};

        @JRubyMethod(name="value", required=0)
        public IRubyObject getValue(ThreadContext context) {
            final IRubyObject value;
            final Map<Object, IRubyObject> contextVariables;
            contextVariables = getContextVariables(context);
            value = contextVariables.get(this);
            if (value != null) {
                return value;
            } else if (default_proc != null) {
                // pre-set for the sake of terminating recursive calls
                contextVariables.put(this, context.getRuntime().getNil());

                final IRubyObject new_value;
                new_value = default_proc.call(context, EMPTY_ARGS, null, Block.NULL_BLOCK);
                contextVariables.put(this, new_value);
                return new_value;
            } else {
                return default_value;
            }
        }

        @JRubyMethod(name="value=", required=1)
        public IRubyObject setValue(ThreadContext context, IRubyObject value) {
            getContextVariables(context).put(this, value);
            return value;
        }

        protected final Map<Object, IRubyObject> getContextVariables(ThreadContext context) {
            return getExecutionContext(context).getContextVariables();
        }

        protected abstract ExecutionContext getExecutionContext(ThreadContext context);
    }

    @JRubyClass(name="JRuby::ThreadLocal")
    public final static class JRubyThreadLocal extends JRubyExecutionContextLocal {
        public static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass type) {
                return new JRubyThreadLocal(runtime, type);
            }
        };

        public JRubyThreadLocal(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        protected final ExecutionContext getExecutionContext(ThreadContext context) {
            return context.getThread();
        }
    }

    @JRubyClass(name="JRuby::FiberLocal")
    public final static class JRubyFiberLocal extends JRubyExecutionContextLocal {
        public static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass type) {
                return new JRubyFiberLocal(runtime, type);
            }
        };

        public JRubyFiberLocal(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(name="with_value", required=1)
        public IRubyObject withValue(ThreadContext context, IRubyObject value, Block block) {
            final Map<Object, IRubyObject> contextVariables;
            contextVariables = getContextVariables(context);
            final IRubyObject old_value;
            old_value = contextVariables.get(this);
            contextVariables.put(this, value);
            try {
                return block.yieldSpecific(context);
            } finally {
                contextVariables.put(this, old_value);
            }
        }

        protected final ExecutionContext getExecutionContext(ThreadContext context) {
            final ExecutionContext fiber;
            fiber = context.getFiber();
            if (fiber != null) {
                return fiber;
            } else {
                /* root fiber */
                return context.getThread();
            }
        }
    }

    @JRubyModule(name="JRubyExtensions")
    public static class JRubyExtensions {
        @JRubyMethod(name = "steal_method", required = 2, module = true)
        public static IRubyObject steal_method(IRubyObject recv, IRubyObject type, IRubyObject methodName) {
            RubyModule to_add = null;
            if(recv instanceof RubyModule) {
                to_add = (RubyModule)recv;
            } else {
                to_add = recv.getSingletonClass();
            }
            String name = methodName.toString();
            if(!(type instanceof RubyModule)) {
                throw recv.getRuntime().newArgumentError("First argument must be a module/class");
            }

            DynamicMethod method = ((RubyModule)type).searchMethod(name);
            if(method == null || method.isUndefined()) {
                throw recv.getRuntime().newArgumentError("No such method " + name + " on " + type);
            }

            to_add.addMethod(name, method);
            return recv.getRuntime().getNil();
        }

        @JRubyMethod(name = "steal_methods", required = 1, rest = true, module = true)
        public static IRubyObject steal_methods(IRubyObject recv, IRubyObject[] args) {
            IRubyObject type = args[0];
            for(int i=1;i<args.length;i++) {
                steal_method(recv, type, args[i]);
            }
            return recv.getRuntime().getNil();
        }
    }
    
    public static class JRubyClassExtensions {
        // TODO: Someday, enable.
        @JRubyMethod(name = "subclasses", optional = 1)
        public static IRubyObject subclasses(ThreadContext context, IRubyObject maybeClass, IRubyObject[] args) {
            RubyClass clazz;
            if (maybeClass instanceof RubyClass) {
                clazz = (RubyClass)maybeClass;
            } else {
                throw context.getRuntime().newTypeError(maybeClass, context.getRuntime().getClassClass());
            }

            boolean recursive = false;
            if (args.length == 1) {
                if (args[0] instanceof RubyBoolean) {
                    recursive = args[0].isTrue();
                } else {
                    context.getRuntime().newTypeError(args[0], context.getRuntime().getClass("Boolean"));
                }
            }

            return RubyArray.newArray(context.getRuntime(), clazz.subclasses(recursive)).freeze(context);
        }

        @JRubyMethod(name = "become_java!", optional = 1)
        public static IRubyObject become_java_bang(ThreadContext context, IRubyObject maybeClass, IRubyObject[] args) {
            RubyClass clazz;
            if (maybeClass instanceof RubyClass) {
                clazz = (RubyClass)maybeClass;
            } else {
                throw context.getRuntime().newTypeError(maybeClass, context.getRuntime().getClassClass());
            }

            String dumpDir = null;
            boolean childLoader = true;
            if (args.length > 0) {
                if (args[0] instanceof RubyString) {
                    dumpDir = args[0].convertToString().asJavaString();
                    if (args.length > 1) {
                        childLoader = args[1].isTrue();
                    }
                } else {
                    childLoader = args[0].isTrue();
                    if (args.length > 1) {
                        dumpDir = args[0].convertToString().asJavaString();
                    }
                }
                clazz.reifyWithAncestors(dumpDir, childLoader);
            } else {
                clazz.reifyWithAncestors();
            }

            return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), clazz.getReifiedClass());
        }

        @JRubyMethod
        public static IRubyObject java_class(ThreadContext context, IRubyObject maybeClass) {
            RubyClass clazz;
            if (maybeClass instanceof RubyClass) {
                clazz = (RubyClass)maybeClass;
            } else {
                throw context.getRuntime().newTypeError(maybeClass, context.getRuntime().getClassClass());
            }

            for (RubyClass current = clazz; current != null; current = current.getSuperClass()) {
                if (current.getReifiedClass() != null) {
                    clazz = current;
                    break;
                }
            }

            return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), clazz.getReifiedClass());
        }

        @JRubyMethod
        public static IRubyObject add_method_annotation(ThreadContext context, IRubyObject maybeClass, IRubyObject methodName, IRubyObject annoMap) {
            RubyClass clazz = getRubyClass(maybeClass, context);
            String method = methodName.convertToString().asJavaString();

            Map<Class,Map<String,Object>> annos = (Map<Class,Map<String,Object>>)annoMap;

            for (Map.Entry<Class,Map<String,Object>> entry : annos.entrySet()) {
                Map<String,Object> value = entry.getValue();
                if (value == null) value = Collections.EMPTY_MAP;
                clazz.addMethodAnnotation(method, getAnnoClass(context, entry.getKey()), value);
            }

            return context.getRuntime().getNil();
        }

        @JRubyMethod
        public static IRubyObject add_parameter_annotations(ThreadContext context, IRubyObject maybeClass, IRubyObject methodName, IRubyObject paramAnnoMaps) {
            RubyClass clazz = getRubyClass(maybeClass, context);
            String method = methodName.convertToString().asJavaString();
            List<Map<Class,Map<String,Object>>> annos = (List<Map<Class,Map<String,Object>>>) paramAnnoMaps;

            for (int i = annos.size() - 1; i >= 0; i--) {
                Map<Class, Map<String, Object>> paramAnnos = annos.get(i);
                for (Map.Entry<Class,Map<String,Object>> entry : paramAnnos.entrySet()) {
                    Map<String,Object> value = entry.getValue();
                    if (value == null) value = Collections.EMPTY_MAP;
                    clazz.addParameterAnnotation(method, i, getAnnoClass(context, entry.getKey()), value);
                }
            }
            return context.getRuntime().getNil();
        }

        @JRubyMethod
        public static IRubyObject add_class_annotation(ThreadContext context, IRubyObject maybeClass, IRubyObject annoMap) {
            RubyClass clazz = getRubyClass(maybeClass, context);
            Map<Class,Map<String,Object>> annos = (Map<Class,Map<String,Object>>)annoMap;

            for (Map.Entry<Class,Map<String,Object>> entry : annos.entrySet()) {
                Map<String,Object> value = entry.getValue();
                if (value == null) value = Collections.EMPTY_MAP;
                clazz.addClassAnnotation(getAnnoClass(context, entry.getKey()), value);
            }

            return context.getRuntime().getNil();
        }

        @JRubyMethod
        public static IRubyObject add_method_signature(ThreadContext context, IRubyObject maybeClass, IRubyObject methodName, IRubyObject clsList) {
            RubyClass clazz = getRubyClass(maybeClass, context);
            List<Class> types = new ArrayList<Class>();
            for (Iterator i = ((List)clsList).iterator(); i.hasNext();) {
                types.add(getAnnoClass(context, i.next()));
            }

            clazz.addMethodSignature(methodName.convertToString().asJavaString(), types.toArray(new Class[types.size()]));

            return context.getRuntime().getNil();
        }

        private static Class getAnnoClass(ThreadContext context, Object annoClass) {
            if (annoClass instanceof Class) {
                return (Class) annoClass;
            } else if (annoClass instanceof IRubyObject) {
                IRubyObject annoMod = (IRubyObject) annoClass;
                if (annoMod.respondsTo("java_class")) {
                    return (Class) annoMod.callMethod(context, "java_class").toJava(Object.class);
                }
            }
            throw context.getRuntime().newArgumentError("must supply java class argument instead of " + annoClass.toString());
        }

        private static RubyClass getRubyClass(IRubyObject maybeClass, ThreadContext context) throws RaiseException {
            RubyClass clazz;
            if (maybeClass instanceof RubyClass) {
                clazz = (RubyClass) maybeClass;
            } else {
                throw context.getRuntime().newTypeError(maybeClass, context.getRuntime().getClassClass());
            }
            return clazz;
        }
    }

    public static class JRubyThreadExtensions {
        private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        @JRubyMethod(name = "times", module = true)
        public static IRubyObject times(IRubyObject recv, Block unusedBlock) {
            Ruby runtime = recv.getRuntime();
            long cpu = threadBean.getCurrentThreadCpuTime();
            long user = threadBean.getCurrentThreadUserTime();
            if (cpu == -1) {
                cpu = 0L;
            }
            if (user == -1) {
                user = 0L;
            }
            double system_d = (cpu - user) / 1000000000.0;
            double user_d = user / 1000000000.0;
            RubyFloat zero = runtime.newFloat(0.0);
            return RubyStruct.newStruct(runtime.getTmsStruct(),
                    new IRubyObject[] { RubyFloat.newFloat(runtime, user_d), RubyFloat.newFloat(runtime, system_d), zero, zero },
                    Block.NULL_BLOCK);
        }
    }
    
    public static class JRubyStringExtensions {
        @JRubyMethod(name = "alloc", meta = true)
        public static IRubyObject alloc(ThreadContext context, IRubyObject recv, IRubyObject size) {
            return RubyString.newStringLight(context.getRuntime(), (int)size.convertToInteger().getLongValue());
        }
    }
    
    public static class MethodExtensions {
        @JRubyMethod(name = "args")
        public static IRubyObject methodArgs(IRubyObject recv) {
            Ruby runtime = recv.getRuntime();
            RubyMethod rubyMethod = (RubyMethod)recv;
            RubyArray argsArray = RubyArray.newArray(runtime);
            DynamicMethod method = rubyMethod.method;
            RubySymbol req = runtime.newSymbol("req");
            RubySymbol opt = runtime.newSymbol("opt");
            RubySymbol rest = runtime.newSymbol("rest");
            RubySymbol block = runtime.newSymbol("block");

            if (method instanceof MethodArgs2) {
                return RuntimeHelpers.parameterListToParameters(runtime, ((MethodArgs2)method).getParameterList(), true);
            } else if (method instanceof MethodArgs) {
                MethodArgs interpMethod = (MethodArgs)method;
                ArgsNode args = interpMethod.getArgsNode();
                
                ListNode requiredArgs = args.getPre();
                for (int i = 0; requiredArgs != null && i < requiredArgs.size(); i++) {
                    Node argNode = requiredArgs.get(i);
                    if (argNode instanceof MultipleAsgn19Node) {
                        argsArray.append(RubyArray.newArray(runtime, req));
                    } else {
                        argsArray.append(RubyArray.newArray(runtime, req, getNameFrom(runtime, (INameNode)argNode)));
                    }
                }
                
                ListNode optArgs = args.getOptArgs();
                for (int i = 0; optArgs != null && i < optArgs.size(); i++) {
                    argsArray.append(RubyArray.newArray(runtime, opt, getNameFrom(runtime, (INameNode) optArgs.get(i))));
                }

                if (args.getRestArg() >= 0) {
                    RestArgNode restArg = (RestArgNode) args.getRestArgNode();

                    if (restArg instanceof UnnamedRestArgNode) {
                        if (((UnnamedRestArgNode) restArg).isStar()) {
                            argsArray.append(RubyArray.newArray(runtime, rest));
                        }
                    } else {
                        argsArray.append(RubyArray.newArray(runtime, rest, getNameFrom(runtime, args.getRestArgNode())));
                    }
                }
                
                ListNode requiredArgsPost = args.getPost();
                for (int i = 0; requiredArgsPost != null && i < requiredArgsPost.size(); i++) {
                    Node argNode = requiredArgsPost.get(i);
                    if (argNode instanceof MultipleAsgn19Node) {
                        argsArray.append(RubyArray.newArray(runtime, req));
                    } else {
                        argsArray.append(RubyArray.newArray(runtime, req, getNameFrom(runtime, (INameNode) requiredArgsPost.get(i))));
                    }
                }

                if (args.getBlock() != null) {
                    argsArray.append(RubyArray.newArray(runtime, block, getNameFrom(runtime, args.getBlock())));
                }
            } else {
                if (method.getArity() == Arity.OPTIONAL) {
                    argsArray.append(RubyArray.newArray(runtime, rest));
                }
            }

            return argsArray;
        }
    }

    private static IRubyObject getNameFrom(Ruby runtime, INameNode node) {
        return node == null ? runtime.getNil() : RubySymbol.newSymbol(runtime, node.getName());
    }
}
