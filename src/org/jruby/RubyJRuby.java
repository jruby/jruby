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

import java.io.IOException;
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
import org.jruby.runtime.load.Library;
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
import org.jruby.util.TypeConverter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.util.Map;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.ObjectAllocator;

/**
 * Module which defines JRuby-specific methods for use. 
 */
@JRubyModule(name="JRuby")
public class RubyJRuby {
    public static RubyModule createJRuby(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        runtime.getKernel().callMethod(context, "require", runtime.newString("java"));
        RubyModule jrubyModule = runtime.defineModule("JRuby");
        
        jrubyModule.defineAnnotatedMethods(RubyJRuby.class);

        RubyClass compiledScriptClass = jrubyModule.defineClassUnder("CompiledScript",runtime.getObject(), runtime.getObject().getAllocator());

        compiledScriptClass.attr_accessor(context, new IRubyObject[]{runtime.newSymbol("name"), runtime.newSymbol("class_name"), runtime.newSymbol("original_script"), runtime.newSymbol("code")});
        compiledScriptClass.defineAnnotatedMethods(JRubyCompiledScript.class);

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

    public static class ExtLibrary implements Library {
        public void load(Ruby runtime, boolean wrap) throws IOException {
            RubyJRuby.createJRubyExt(runtime);
            
            runtime.getMethod().defineAnnotatedMethods(MethodExtensions.class);
        }
    }

    public static class CoreExtLibrary implements Library {
        public void load(Ruby runtime, boolean wrap) throws IOException {
            RubyJRuby.createJRubyCoreExt(runtime);
        }
    }
    
    public static class TypeLibrary implements Library {
        public void load(Ruby runtime, boolean wrap) throws IOException {
            RubyModule jrubyType = runtime.defineModule("Type");
            jrubyType.defineAnnotatedMethods(TypeLibrary.class);
        }
        
        @JRubyMethod(module = true)
        public static IRubyObject coerce_to(ThreadContext context, IRubyObject self, IRubyObject object, IRubyObject clazz, IRubyObject method) {
            Ruby ruby = object.getRuntime();
            
            if (!(clazz instanceof RubyClass)) {
                throw ruby.newTypeError(clazz, ruby.getClassClass());
            }
            if (!(method instanceof RubySymbol)) {
                throw ruby.newTypeError(method, ruby.getSymbol());
            }
            
            RubyClass rubyClass = (RubyClass)clazz;
            RubySymbol methodSym = (RubySymbol)method;
            
            return TypeConverter.convertToTypeOrRaise(object, rubyClass, methodSym.asJavaString());
        }
    }

    @JRubyMethod(module = true)
    public static void gc(IRubyObject recv) {
        System.gc();
    }
    
    @JRubyMethod(name = "runtime", frame = true, module = true)
    public static IRubyObject runtime(IRubyObject recv, Block unusedBlock) {
        return JavaUtil.convertJavaToUsableRubyObject(recv.getRuntime(), recv.getRuntime());
    }

    @JRubyMethod(frame = true, module = true)
    public static IRubyObject with_current_runtime_as_global(ThreadContext context, IRubyObject recv, Block block) {
        Ruby currentRuntime = context.getRuntime();
        Ruby globalRuntime = Ruby.getGlobalRuntime();
        try {
            if (globalRuntime != currentRuntime) {
                currentRuntime.useAsGlobalRuntime();
            }
            block.yieldSpecific(context);
        } finally {
            if (Ruby.getGlobalRuntime() != globalRuntime) {
                globalRuntime.useAsGlobalRuntime();
            }
        }
        return currentRuntime.getNil();
    }

    @JRubyMethod(name = "objectspace", frame = true, module = true)
    public static IRubyObject getObjectSpaceEnabled(IRubyObject recv, Block b) {
        Ruby runtime = recv.getRuntime();
        return RubyBoolean.newBoolean(
                runtime, runtime.isObjectSpaceEnabled());
    }

    @JRubyMethod(name = "objectspace=", required = 1, frame = true, module = true)
    public static IRubyObject setObjectSpaceEnabled(
            IRubyObject recv, IRubyObject arg, Block b) {
        Ruby runtime = recv.getRuntime();
        runtime.setObjectSpaceEnabled(arg.isTrue());
        return runtime.getNil();
    }

    @JRubyMethod(name = {"parse", "ast_for"}, optional = 3, frame = true, module = true)
    public static IRubyObject parse(IRubyObject recv, IRubyObject[] args, Block block) {
        if(block.isGiven()) {
            if(block.getBody() instanceof org.jruby.runtime.CompiledBlock) {
                throw new RuntimeException("Cannot compile an already compiled block. Use -J-Djruby.jit.enabled=false to avoid this problem.");
            }
            Arity.checkArgumentCount(recv.getRuntime(),args,0,0);
            return JavaUtil.convertJavaToUsableRubyObject(recv.getRuntime(), ((InterpretedBlock)block.getBody()).getBodyNode());
        } else {
            Arity.checkArgumentCount(recv.getRuntime(),args,1,3);
            String filename = "-";
            boolean extraPositionInformation = false;
            RubyString content = args[0].convertToString();
            if(args.length>1) {
                filename = args[1].convertToString().toString();
                if(args.length>2) {
                    extraPositionInformation = args[2].isTrue();
                }
            }
            return JavaUtil.convertJavaToUsableRubyObject(recv.getRuntime(),
               recv.getRuntime().parse(content.getByteList(), filename, null, 0, extraPositionInformation));
        }
    }

    @JRubyMethod(name = "compile", optional = 3, frame = true, module = true)
    public static IRubyObject compile(IRubyObject recv, IRubyObject[] args, Block block) {
        Node node;
        String filename;
        RubyString content;
        if(block.isGiven()) {
            Arity.checkArgumentCount(recv.getRuntime(),args,0,0);
            if(block.getBody() instanceof org.jruby.runtime.CompiledBlock) {
                throw new RuntimeException("Cannot compile an already compiled block. Use -J-Djruby.jit.enabled=false to avoid this problem.");
            }
            content = RubyString.newEmptyString(recv.getRuntime());
            Node bnode = ((InterpretedBlock)block.getBody()).getBodyNode();
            node = new org.jruby.ast.RootNode(bnode.getPosition(), block.getBinding().getDynamicScope(), bnode);
            filename = "__block_" + node.getPosition().getFile();
        } else {
            Arity.checkArgumentCount(recv.getRuntime(),args,1,3);
            filename = "-";
            boolean extraPositionInformation = false;
            content = args[0].convertToString();
            if(args.length>1) {
                filename = args[1].convertToString().toString();
                if(args.length>2) {
                    extraPositionInformation = args[2].isTrue();
                }
            }

            node = recv.getRuntime().parse(content.getByteList(), filename, null, 0, extraPositionInformation);
        }

        String classname;
        if (filename.equals("-e")) {
            classname = "__dash_e__";
        } else {
            classname = filename.replace('\\', '/').replaceAll(".rb", "").replaceAll("-","_dash_");
        }

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(node);
            
        StandardASMCompiler asmCompiler = new StandardASMCompiler(classname, filename);
        ASTCompiler compiler = recv.getRuntime().getInstanceConfig().newCompiler();
        compiler.compileRoot(node, asmCompiler, inspector);
        byte[] bts = asmCompiler.getClassByteArray();

        IRubyObject compiledScript = ((RubyModule)recv).fastGetConstant("CompiledScript").callMethod(recv.getRuntime().getCurrentContext(),"new");
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "name=", recv.getRuntime().newString(filename));
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "class_name=", recv.getRuntime().newString(classname));
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "original_script=", content);
        compiledScript.callMethod(recv.getRuntime().getCurrentContext(), "code=", JavaUtil.convertJavaToUsableRubyObject(recv.getRuntime(), bts));

        return compiledScript;
    }

    @JRubyMethod(name = "reference", required = 1, module = true)
    public static IRubyObject reference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        Ruby runtime = context.getRuntime();

        return Java.wrap(runtime, JavaObject.wrap(runtime, obj));
    }

    @JRubyMethod(name = "dereference", required = 1, module = true)
    public static IRubyObject dereference(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        if (!(obj.dataGetStruct() instanceof JavaObject)) {
            throw context.getRuntime().newTypeError("got " + obj + ", expected wrapped Java object");
        }
        
        Object unwrapped = JavaUtil.unwrapJavaObject(obj);

        if (!(unwrapped instanceof IRubyObject)) {
            throw context.getRuntime().newTypeError("got " + obj + ", expected Java-wrapped Ruby object");
        }

        return (IRubyObject)unwrapped;
    }

    @JRubyClass(name="JRuby::CompiledScript")
    public static class JRubyCompiledScript {
        @JRubyMethod(name = "to_s")
        public static IRubyObject compiled_script_to_s(IRubyObject recv) {
            return recv.getInstanceVariables().fastGetInstanceVariable("@original_script");
        }

        @JRubyMethod(name = "inspect")
        public static IRubyObject compiled_script_inspect(IRubyObject recv) {
            return recv.getRuntime().newString("#<JRuby::CompiledScript " + recv.getInstanceVariables().fastGetInstanceVariable("@name") + ">");
        }

        @JRubyMethod(name = "inspect_bytecode")
        public static IRubyObject compiled_script_inspect_bytecode(IRubyObject recv) {
            StringWriter sw = new StringWriter();
            ClassReader cr = new ClassReader((byte[])recv.getInstanceVariables().fastGetInstanceVariable("@code").toJava(byte[].class));
            TraceClassVisitor cv = new TraceClassVisitor(new PrintWriter(sw));
            cr.accept(cv, ClassReader.SKIP_DEBUG);
            return recv.getRuntime().newString(sw.toString());
        }
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
                    context.getRuntime().newTypeError(args[0], context.getRuntime().fastGetClass("Boolean"));
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

            if (args.length > 0) {
                clazz.reify(args[0].convertToString().asJavaString());
            } else {
                clazz.reify();
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

            if (clazz.getReifiedClass() == null) {
                return context.getRuntime().getNil();
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
            double system = threadBean.getCurrentThreadCpuTime() / 1000000000.0;
            double user = threadBean.getCurrentThreadUserTime() / 1000000000.0;
            RubyFloat zero = runtime.newFloat(0.0);
            return RubyStruct.newStruct(runtime.getTmsStruct(),
                    new IRubyObject[] { RubyFloat.newFloat(runtime, user), RubyFloat.newFloat(runtime, system), zero, zero },
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
            
            DynamicMethod method = rubyMethod.method;
            
            if (method instanceof MethodArgs) {
                MethodArgs interpMethod = (MethodArgs)method;
                ArgsNode args = interpMethod.getArgsNode();
                RubyArray argsArray = RubyArray.newArray(runtime);
                
                RubyArray reqArray = RubyArray.newArray(runtime);
                ListNode requiredArgs = args.getPre();
                for (int i = 0; requiredArgs != null && i < requiredArgs.size(); i++) {
                    reqArray.append(getNameFrom(runtime, (INameNode) requiredArgs.get(i)));
                }
                argsArray.append(reqArray);
                
                RubyArray optArray = RubyArray.newArray(runtime);
                ListNode optArgs = args.getOptArgs();
                for (int i = 0; optArgs != null && i < optArgs.size(); i++) {
                    optArray.append(getNameFrom(runtime, (INameNode) optArgs.get(i)));
                }
                argsArray.append(optArray);

                argsArray.append(getNameFrom(runtime, args.getRestArgNode()));
                argsArray.append(getNameFrom(runtime, args.getBlock()));
                
                return argsArray;
            }
            
            throw runtime.newTypeError("Method args are only available for standard interpreted or jitted methods");
        }
    }

    private static IRubyObject getNameFrom(Ruby runtime, INameNode node) {
        return node == null ? runtime.getNil() : RubySymbol.newSymbol(runtime, node.getName());
    }
}
