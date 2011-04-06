/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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

package org.jruby.compiler.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CacheCompiler;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.BodyCompiler;
import org.jruby.compiler.ScriptCompiler;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.SafePropertyAccessor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 *
 * @author headius
 */
public class StandardASMCompiler implements ScriptCompiler, Opcodes {
    public static final String THREADCONTEXT = p(ThreadContext.class);
    public static final String RUBY = p(Ruby.class);
    public static final String IRUBYOBJECT = p(IRubyObject.class);
    public static final boolean VERIFY_CLASSFILES = true;

    public static String getStaticMethodSignature(String classname, int args) {
        switch (args) {
        case 0:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, Block.class);
        case 1:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 2:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 3:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 4:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
        default:
            throw new RuntimeException("unsupported arity: " + args);
        }
    }

    public static Class[] getStaticMethodParams(Class target, int args) {
        switch (args) {
        case 0:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, Block.class};
        case 1:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 2:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 3:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 4:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class};
        default:
            throw new RuntimeException("unsupported arity: " + args);
        }
    }

    public static String getMethodSignature(int args) {
        switch (args) {
        case 0:
            return sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, Block.class);
        case 1:
            return sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 2:
            return sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 3:
            return sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 4:
            return sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
        default:
            throw new RuntimeException("unsupported arity: " + args);
        }
    }

    public static String getStaticClosureSignature(String classdesc) {
        return sig(IRubyObject.class, "L" + classdesc + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class);
    }

    public static String getStaticClosure19Signature(String classdesc) {
        return sig(IRubyObject.class, "L" + classdesc + ";", ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
    }

    public static String getClosureSignature() {
        return sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class);
    }

    public static String getClosure19Signature() {
        return sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
    }

    public static final int THIS = 0;
    public static final int THREADCONTEXT_INDEX = 1;
    public static final int SELF_INDEX = 2;
    public static final int ARGS_INDEX = 3;
    
    public static final int CLOSURE_OFFSET = 0;
    public static final int DYNAMIC_SCOPE_OFFSET = 1;
    public static final int VARS_ARRAY_OFFSET = 2;
    public static final int EXCEPTION_OFFSET = 3;
    public static final int PREVIOUS_EXCEPTION_OFFSET = 4;
    public static final int FIRST_TEMP_OFFSET = 5;

    public static final int STARTING_DSTR_SIZE = 20;
    
    private String classname;
    private String sourcename;

    private Integer javaVersion;

    private ClassWriter classWriter;
    private SkinnyMethodAdapter initMethod;
    private SkinnyMethodAdapter clinitMethod;
    private int methodIndex = 0;
    private int innerIndex = 0;
    private int rescueNumber = 1;
    private int ensureNumber = 1;
    StaticScope topLevelScope;
    
    private CacheCompiler cacheCompiler;
    
    public static final Constructor invDynInvCompilerConstructor;

    private List<InvokerDescriptor> invokerDescriptors = new ArrayList<InvokerDescriptor>();
    private List<BlockCallbackDescriptor> blockCallbackDescriptors = new ArrayList<BlockCallbackDescriptor>();
    private List<BlockCallbackDescriptor> blockCallback19Descriptors = new ArrayList<BlockCallbackDescriptor>();

    static {
        Constructor compilerConstructor = null;
        Method installerMethod = null;
        try {
            if (SafePropertyAccessor.getBoolean("jruby.compile.invokedynamic")) {
                // if that succeeds, the others should as well
                Class compiler =
                        Class.forName("org.jruby.compiler.impl.InvokeDynamicInvocationCompiler");
                Class support =
                        Class.forName("org.jruby.runtime.invokedynamic.InvokeDynamicSupport");
                compilerConstructor = compiler.getConstructor(BaseBodyCompiler.class, SkinnyMethodAdapter.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // leave it null and fall back on our normal invocation logic
        }
        invDynInvCompilerConstructor = compilerConstructor;
    }
    
    /** Creates a new instance of StandardCompilerContext */
    public StandardASMCompiler(String classname, String sourcename) {
        this.classname = classname;
        this.sourcename = sourcename;
    }

    public void setJavaVersion(Integer javaVersion) {
        this.javaVersion = javaVersion;
    }

    public byte[] getClassByteArray() {
        return classWriter.toByteArray();
    }

    public Class<?> loadClass(JRubyClassLoader classLoader) throws ClassNotFoundException {
        classLoader.defineClass(c(getClassname()), classWriter.toByteArray());
        return classLoader.loadClass(c(getClassname()));
    }
    
    public void dumpClass(PrintStream out) {
        PrintWriter pw = new PrintWriter(out);

        try {
            TraceClassVisitor tcv = new TraceClassVisitor(pw);
            new ClassReader(classWriter.toByteArray()).accept(tcv, 0);
        } finally {
            pw.close();
        }
    }

    public void writeClass(File destination) throws IOException {
        writeClass(getClassname(), destination, classWriter);
    }

    public void writeInvokers(File destination) throws IOException {
        for (InvokerDescriptor descriptor : invokerDescriptors) {
            byte[] invokerBytes = RuntimeHelpers.defOffline(
                    descriptor.getName(),
                    descriptor.getClassname(),
                    descriptor.getInvokerName(),
                    descriptor.getArity(),
                    descriptor.getScope(),
                    descriptor.getCallConfig(),
                    descriptor.getFile(),
                    descriptor.getLine());

            if (VERIFY_CLASSFILES) CheckClassAdapter.verify(new ClassReader(invokerBytes), false, new PrintWriter(System.err));

            writeClassFile(destination, invokerBytes, descriptor.getInvokerName());
        }

        for (BlockCallbackDescriptor descriptor : blockCallbackDescriptors) {
            byte[] callbackBytes = RuntimeHelpers.createBlockCallbackOffline(
                    descriptor.getClassname(),
                    descriptor.getMethod(),
                    descriptor.getFile(),
                    descriptor.getLine());

            if (VERIFY_CLASSFILES) CheckClassAdapter.verify(new ClassReader(callbackBytes), false, new PrintWriter(System.err));

            writeClassFile(destination, callbackBytes, descriptor.getCallbackName());
        }

        for (BlockCallbackDescriptor descriptor : blockCallback19Descriptors) {
            byte[] callbackBytes = RuntimeHelpers.createBlockCallback19Offline(
                    descriptor.getClassname(),
                    descriptor.getMethod(),
                    descriptor.getFile(),
                    descriptor.getLine());

            if (VERIFY_CLASSFILES) CheckClassAdapter.verify(new ClassReader(callbackBytes), false, new PrintWriter(System.err));

            writeClassFile(destination, callbackBytes, descriptor.getCallbackName());
        }
    }

    private void writeClass(String classname, File destination, ClassWriter writer) throws IOException {
        // verify the class
        byte[] bytecode = writer.toByteArray();
        if (VERIFY_CLASSFILES) CheckClassAdapter.verify(new ClassReader(bytecode), false, new PrintWriter(System.err));

        writeClassFile(destination, bytecode, classname);
    }

    private void writeClassFile(File destination, byte[] bytecode, String classname) throws IOException {
        String fullname = classname + ".class";
        String filename = null;
        String path = null;

        if (fullname.lastIndexOf("/") == -1) {
            filename = fullname;
            path = "";
        } else {
            filename = fullname.substring(fullname.lastIndexOf("/") + 1);
            path = fullname.substring(0, fullname.lastIndexOf("/"));
        }
        // create dir if necessary
        File pathfile = new File(destination, path);
        pathfile.mkdirs();

        FileOutputStream out = new FileOutputStream(new File(pathfile, filename));

        try {
            out.write(bytecode);
        } finally {
            out.close();
        }
    }

    public static class InvokerDescriptor {
        private final String name;
        private final String classname;
        private final String invokerName;
        private final Arity arity;
        private final StaticScope scope;
        private final CallConfiguration callConfig;
        private final String file;
        private final int line;
        
        public InvokerDescriptor(String name, String classname, String invokerName, Arity arity, StaticScope scope, CallConfiguration callConfig, String file, int line) {
            this.name = name;
            this.classname = classname;
            this.invokerName = invokerName;
            this.arity = arity;
            this.scope = scope;
            this.callConfig = callConfig;
            this.file = file;
            this.line = line;
        }

        public Arity getArity() {
            return arity;
        }

        public CallConfiguration getCallConfig() {
            return callConfig;
        }

        public String getClassname() {
            return classname;
        }

        public String getFile() {
            return file;
        }

        public String getInvokerName() {
            return invokerName;
        }

        public int getLine() {
            return line;
        }

        public String getName() {
            return name;
        }

        public StaticScope getScope() {
            return scope;
        }
    }

    private static class BlockCallbackDescriptor {
        private final String method;
        private final String classname;
        private final String callbackName;
        private final String file;
        private final int line;

        public BlockCallbackDescriptor(String method, String classname, String file, int line) {
            this.method = method;
            this.classname = classname;
            this.callbackName = classname + "BlockCallback$" + method + "xx1";
            this.file = file;
            this.line = line;
        }

        public String getClassname() {
            return classname;
        }

        public String getMethod() {
            return method;
        }

        public String getCallbackName() {
            return callbackName;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }
    }

    public void addInvokerDescriptor(String newMethodName, int methodArity, StaticScope scope, CallConfiguration callConfig, String filename, int line) {
        String classPath = classname.replaceAll("/", "_");
        Arity arity = Arity.createArity(methodArity);
        String invokerName = classPath + "Invoker" + newMethodName + arity;
        InvokerDescriptor descriptor = new InvokerDescriptor(newMethodName, classname, invokerName, arity, scope, callConfig, filename, line);

        invokerDescriptors.add(descriptor);
    }

    public void addBlockCallbackDescriptor(String method, String file, int line) {
        blockCallbackDescriptors.add(new BlockCallbackDescriptor(method, classname, file, line));
    }

    public void addBlockCallback19Descriptor(String method, String file, int line) {
        blockCallback19Descriptors.add(new BlockCallbackDescriptor(method, classname, file, line));
    }

    public String getClassname() {
        return classname;
    }

    public String getSourcename() {
        return sourcename;
    }

    public ClassVisitor getClassVisitor() {
        return classWriter;
    }
    
    public void startScript(StaticScope scope) {
        classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        // Create the class with the appropriate class name and source file
        classWriter.visit(javaVersion == null ? RubyInstanceConfig.JAVA_VERSION : javaVersion,
                ACC_PUBLIC + ACC_SUPER,getClassname(), null, p(AbstractScript.class), null);

        // add setPosition impl, which stores filename as constant to speed updates
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor(), ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "setPosition", sig(Void.TYPE, params(ThreadContext.class, int.class)), null, null);
        method.start();

        method.aload(0); // thread context
        method.ldc(sourcename);
        method.iload(1); // line number
        method.invokevirtual(p(ThreadContext.class), "setFileAndLine", sig(void.class, String.class, int.class));
        method.voidreturn();
        method.end();
        
        topLevelScope = scope;

        beginInit();
        
        cacheCompiler = new InheritedCacheCompiler(this);

        // This code was originally used to provide debugging info using JSR-45
        // "SMAP" format. However, it breaks using normal Java traces to
        // generate Ruby traces, since the original path is lost. Reverting
        // to full path for now.
//        String sourceNoPath;
//        if (sourcename.indexOf("/") >= 0) {
//            String[] pathElements = sourcename.split("/");
//            sourceNoPath = pathElements[pathElements.length - 1];
//        } else if (sourcename.indexOf("\\") >= 0) {
//            String[] pathElements = sourcename.split("\\\\");
//            sourceNoPath = pathElements[pathElements.length - 1];
//        } else {
//            sourceNoPath = sourcename;
//        }

        final File sourceFile = new File(getSourcename());
        // Revert to using original sourcename here, so that jitted traces match
        // interpreted traces.
        classWriter.visitSource(sourcename, sourceFile.getAbsolutePath());
    }

    public void endScript(boolean generateLoad, boolean generateMain) {
        // add Script#run impl, used for running this script with a specified threadcontext and self
        // root method of a script is always in __file__ method
        String methodName = "__file__";
        
        if (generateLoad || generateMain) {
            // the load method is used for loading as a top-level script, and prepares appropriate scoping around the code
            SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor(), ACC_PUBLIC, "load", getMethodSignature(4), null, null);
            method.start();

            // invoke __file__ with threadcontext, self, args (null), and block (null)
            Label tryBegin = new Label();
            Label tryFinally = new Label();

            method.label(tryBegin);
            method.aload(THREADCONTEXT_INDEX);
            String scopeNames = RuntimeHelpers.encodeScope(topLevelScope);
            method.ldc(scopeNames);
            method.invokestatic(p(RuntimeHelpers.class), "preLoad", sig(void.class, ThreadContext.class, String.class));

            method.aload(THIS);
            method.aload(THREADCONTEXT_INDEX);
            method.aload(SELF_INDEX);
            method.aload(ARGS_INDEX);
            // load always uses IRubyObject[], so simple closure offset calculation here
            method.aload(ARGS_INDEX + 1 + CLOSURE_OFFSET);

            method.invokestatic(getClassname(),methodName, getStaticMethodSignature(getClassname(), 4));
            method.aload(THREADCONTEXT_INDEX);
            method.invokestatic(p(RuntimeHelpers.class), "postLoad", sig(void.class, ThreadContext.class));
            method.areturn();

            method.label(tryFinally);
            method.aload(THREADCONTEXT_INDEX);
            method.invokestatic(p(RuntimeHelpers.class), "postLoad", sig(void.class, ThreadContext.class));
            method.athrow();

            method.trycatch(tryBegin, tryFinally, tryFinally, null);

            method.end();
        }
        
        if (generateMain) {
            // add main impl, used for detached or command-line execution of this script with a new runtime
            // root method of a script is always in stub0, method0
            SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor(), ACC_PUBLIC | ACC_STATIC, "main", sig(Void.TYPE, params(String[].class)), null, null);
            method.start();

            // new instance to invoke run against
            method.newobj(getClassname());
            method.dup();
            method.invokespecial(getClassname(), "<init>", sig(Void.TYPE));

            // set filename for the loaded script class (JRUBY-4825)
            method.dup();
            method.ldc(Type.getType("L" + getClassname() + ";"));
            method.invokevirtual(p(Class.class), "getClassLoader", sig(ClassLoader.class));
            method.ldc(getClassname() + ".class");
            method.invokevirtual(p(ClassLoader.class), "getResource", sig(URL.class, String.class));
            method.invokevirtual(p(Object.class), "toString", sig(String.class));
            method.astore(1);
            method.aload(1);
            method.invokevirtual(p(AbstractScript.class), "setFilename", sig(void.class, String.class));

            // instance config for the script run
            method.newobj(p(RubyInstanceConfig.class));
            method.dup();
            method.invokespecial(p(RubyInstanceConfig.class), "<init>", "()V");

            // set argv from main's args
            method.dup();
            method.aload(0);
            method.invokevirtual(p(RubyInstanceConfig.class), "setArgv", sig(void.class, String[].class));

            // set script filename ($0)
            method.dup();
            method.aload(1);
            method.invokevirtual(p(RubyInstanceConfig.class), "setScriptFileName", sig(void.class, String.class));

            // invoke run with threadcontext and topself
            method.invokestatic(p(Ruby.class), "newInstance", sig(Ruby.class, RubyInstanceConfig.class));
            method.dup();

            method.invokevirtual(RUBY, "getCurrentContext", sig(ThreadContext.class));
            method.swap();
            method.invokevirtual(RUBY, "getTopSelf", sig(IRubyObject.class));
            method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

            method.invokevirtual(getClassname(), "load", getMethodSignature(4));
            method.voidreturn();
            method.end();
        }

        getCacheCompiler().finish();
        
        endInit();
        endClassInit();
    }

    public static String buildStaticScopeNames(StaticScope scope) {
        return RuntimeHelpers.encodeScope(scope);
    }

    private void beginInit() {
        ClassVisitor cv = getClassVisitor();

        initMethod = new SkinnyMethodAdapter(cv, ACC_PUBLIC, "<init>", sig(Void.TYPE), null, null);
        initMethod.start();
        initMethod.aload(THIS);
        initMethod.invokespecial(p(AbstractScript.class), "<init>", sig(Void.TYPE));
        
        // JRUBY-3014: make __FILE__ dynamically determined at load time, but
        // we provide a reasonable default here
        initMethod.aload(THIS);
        initMethod.ldc(getSourcename());
        initMethod.putfield(getClassname(), "filename", ci(String.class));
    }

    private void endInit() {
        initMethod.voidreturn();
        initMethod.end();
    }

    private void beginClassInit() {
        ClassVisitor cv = getClassVisitor();

        clinitMethod = new SkinnyMethodAdapter(cv, ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(Void.TYPE), null, null);
        clinitMethod.start();
    }

    private void endClassInit() {
        if (clinitMethod != null) {
            clinitMethod.voidreturn();
            clinitMethod.end();
        }
    }
    
    public SkinnyMethodAdapter getInitMethod() {
        return initMethod;
    }
    
    public SkinnyMethodAdapter getClassInitMethod() {
        // lazily create class init only if necessary
        if (clinitMethod == null) {
            beginClassInit();
        }
        return clinitMethod;
    }
    
    public CacheCompiler getCacheCompiler() {
        return cacheCompiler;
    }
    
    public BodyCompiler startMethod(String rubyName, String javaName, CompilerCallback args, StaticScope scope, ASTInspector inspector) {
        RootScopedBodyCompiler methodCompiler = new MethodBodyCompiler(this, rubyName, javaName, inspector, scope);
        
        methodCompiler.beginMethod(args, scope);
        
        return methodCompiler;
    }

    public BodyCompiler startFileMethod(CompilerCallback args, StaticScope scope, ASTInspector inspector) {
        MethodBodyCompiler methodCompiler = new MethodBodyCompiler(this, "__file__", "__file__", inspector, scope);
        
        methodCompiler.beginMethod(args, scope);
        
        // boxed arg list __file__
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor(), ACC_PUBLIC, "__file__", getMethodSignature(4), null, null);
        method.start();

        // invoke static __file__
        method.aload(THIS);
        method.aload(THREADCONTEXT_INDEX);
        method.aload(SELF_INDEX);
        method.aload(ARGS_INDEX);
        method.aload(ARGS_INDEX + 1); // block
        method.invokestatic(getClassname(), "__file__", getStaticMethodSignature(getClassname(), 4));

        method.areturn();
        method.end();
        
        if (methodCompiler.isSpecificArity()) {
            // exact arg list __file__
            method = new SkinnyMethodAdapter(getClassVisitor(), ACC_PUBLIC, "__file__", getMethodSignature(scope.getRequiredArgs()), null, null);
            method.start();

            // invoke static __file__
            method.aload(THIS);
            method.aload(THREADCONTEXT_INDEX);
            method.aload(SELF_INDEX);
            for (int i = 0; i < scope.getRequiredArgs(); i++) {
                method.aload(ARGS_INDEX + i);
            }
            method.aload(ARGS_INDEX + scope.getRequiredArgs()); // block
            method.invokestatic(getClassname(), "__file__", getStaticMethodSignature(getClassname(), scope.getRequiredArgs()));

            method.areturn();
            method.end();
        }

        return methodCompiler;
    }

    public BodyCompiler startRoot(String rubyName, String javaName, StaticScope scope, ASTInspector inspector) {
        RootScopedBodyCompiler methodCompiler = new MethodBodyCompiler(this, rubyName, javaName, inspector, scope);

        methodCompiler.beginMethod(null, scope);

        return methodCompiler;
    }

    public int getMethodIndex() {
        return methodIndex;
    }
    
    public int getAndIncrementMethodIndex() {
        return methodIndex++;
    }

    public int getInnerIndex() {
        return innerIndex;
    }

    public int getAndIncrementInnerIndex() {
        return innerIndex++;
    }

    public int getRescueNumber() {
        return rescueNumber;
    }

    public int getAndIncrementRescueNumber() {
        return rescueNumber++;
    }

    public int getEnsureNumber() {
        return ensureNumber;
    }

    public int getAndIncrementEnsureNumber() {
        return ensureNumber++;
    }

    private int constants = 0;

    public String getNewConstant(String type, String name_prefix) {
        return getNewConstant(type, name_prefix, null);
    }

    public synchronized String getNewConstantName() {
        return "_" + constants++;
    }

    public String getNewConstant(String type, String name_prefix, Object init) {
        ClassVisitor cv = getClassVisitor();

        String realName = getNewConstantName();

        // declare the field
        cv.visitField(ACC_PRIVATE, realName, type, null, null).visitEnd();

        if(init != null) {
            initMethod.aload(THIS);
            initMethod.ldc(init);
            initMethod.putfield(getClassname(),realName, type);
        }

        return realName;
    }

    public String getNewField(String type, String name, Object init) {
        ClassVisitor cv = getClassVisitor();

        // declare the field
        cv.visitField(ACC_PRIVATE, name, type, null, null).visitEnd();

        if(init != null) {
            initMethod.aload(THIS);
            initMethod.ldc(init);
            initMethod.putfield(getClassname(),name, type);
        }

        return name;
    }

    public String getNewStaticConstant(String type, String name_prefix) {
        ClassVisitor cv = getClassVisitor();

        String realName;
        synchronized (this) {
            realName = "__" + constants++;
        }

        // declare the field
        cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, realName, type, null, null).visitEnd();
        return realName;
    }
}
