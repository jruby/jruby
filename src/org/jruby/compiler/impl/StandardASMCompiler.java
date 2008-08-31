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
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Arrays;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyMatchData;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.NodeType;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.BranchCallback;
import org.jruby.compiler.CacheCompiler;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.compiler.MethodCompiler;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ScriptCompiler;
import org.jruby.compiler.VariableCompiler;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.ReOptions;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.util.ByteList;
import static org.jruby.util.CodegenUtils.*;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 *
 * @author headius
 */
public class StandardASMCompiler implements ScriptCompiler, Opcodes {
    private static final String THREADCONTEXT = p(ThreadContext.class);
    private static final String RUBY = p(Ruby.class);
    private static final String IRUBYOBJECT = p(IRubyObject.class);

    public static final String[] METHOD_SIGNATURES = {
        sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, Block.class}),
        sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class}),
        sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class}),
        sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class}),
        sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class}),
    };
    private static final String CLOSURE_SIGNATURE = sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject.class});

    public static final int THIS = 0;
    public static final int THREADCONTEXT_INDEX = 1;
    public static final int SELF_INDEX = 2;
    public static final int ARGS_INDEX = 3;
    
    public static final int CLOSURE_OFFSET = 0;
    public static final int DYNAMIC_SCOPE_OFFSET = 1;
    public static final int RUNTIME_OFFSET = 2;
    public static final int VARS_ARRAY_OFFSET = 3;
    public static final int NIL_OFFSET = 4;
    public static final int EXCEPTION_OFFSET = 5;
    public static final int PREVIOUS_EXCEPTION_OFFSET = 6;
    public static final int FIRST_TEMP_OFFSET = 7;
    
    private String classname;
    private String sourcename;

    private ClassWriter classWriter;
    private SkinnyMethodAdapter initMethod;
    private SkinnyMethodAdapter clinitMethod;
    int methodIndex = -1;
    int innerIndex = -1;
    int fieldIndex = 0;
    int rescueNumber = 1;
    int ensureNumber = 1;
    StaticScope topLevelScope;
    
    CacheCompiler cacheCompiler;
    
    /** Creates a new instance of StandardCompilerContext */
    public StandardASMCompiler(String classname, String sourcename) {
        this.classname = classname;
        this.sourcename = sourcename;
    }

    public byte[] getClassByteArray() {
        return classWriter.toByteArray();
    }

    public Class<?> loadClass(JRubyClassLoader classLoader) throws ClassNotFoundException {
        classLoader.defineClass(c(classname), classWriter.toByteArray());
        return classLoader.loadClass(c(classname));
    }
    
    public void dumpClass(PrintStream out) {
        TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(out));
        new ClassReader(classWriter.toByteArray()).accept(tcv, 0);
        
        tcv.print(new PrintWriter(out));
    }

    public void writeClass(File destination) throws IOException {
        writeClass(classname, destination, classWriter);
    }

    private void writeClass(String classname, File destination, ClassWriter writer) throws IOException {
        String fullname = classname + ".class";
        String filename = null;
        String path = null;
        
        // verify the class
        byte[] bytecode = writer.toByteArray();
        CheckClassAdapter.verify(new ClassReader(bytecode), false, new PrintWriter(System.err));
        
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

        out.write(bytecode);
        out.close();
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
        classWriter.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, classname, null, p(AbstractScript.class), null);
        
        topLevelScope = scope;

        beginInit();
        beginClassInit();
        
        cacheCompiler = new InheritedCacheCompiler(this);

        String sourceNoPath;
        if (sourcename.indexOf("/") >= 0) {
            String[] pathElements = sourcename.split("/");
            sourceNoPath = pathElements[pathElements.length - 1];
        } else if (sourcename.indexOf("\\") >= 0) {
            String[] pathElements = sourcename.split("\\\\");
            sourceNoPath = pathElements[pathElements.length - 1];
        } else {
            sourceNoPath = sourcename;
        }
        
        StringBuffer smap = new StringBuffer();
        smap.append("SMAP\n")
                .append(sourceNoPath).append("\n")
                .append("Ruby\n")
                .append("*S Ruby\n")
                .append("*F\n")
                .append("+ 1 ").append(sourceNoPath).append("\n")
                .append(sourcename).append("\n")
                .append("*L\n")
                .append("1#1,999999:1,1\n")
                .append("*E\n");

        
        classWriter.visitSource(sourceNoPath, smap.toString());
    }

    public void endScript(boolean generateLoad, boolean generateMain) {
        // add Script#run impl, used for running this script with a specified threadcontext and self
        // root method of a script is always in __file__ method
        String methodName = "__file__";
        
        if (generateLoad || generateMain) {
            // the load method is used for loading as a top-level script, and prepares appropriate scoping around the code
            SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC, "load", METHOD_SIGNATURES[4], null, null));
            method.start();

            // invoke __file__ with threadcontext, self, args (null), and block (null)
            Label tryBegin = new Label();
            Label tryFinally = new Label();

            method.label(tryBegin);
            method.aload(THREADCONTEXT_INDEX);
            buildStaticScopeNames(method, topLevelScope);
            method.invokestatic(p(RuntimeHelpers.class), "preLoad", sig(void.class, ThreadContext.class, String[].class));

            method.aload(THIS);
            method.aload(THREADCONTEXT_INDEX);
            method.aload(SELF_INDEX);
            method.aload(ARGS_INDEX);
            // load always uses IRubyObject[], so simple closure offset calculation here
            method.aload(ARGS_INDEX + 1 + CLOSURE_OFFSET);

            method.invokevirtual(classname, methodName, METHOD_SIGNATURES[4]);
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
            SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_STATIC, "main", sig(Void.TYPE, params(String[].class)), null, null));
            method.start();

            // new instance to invoke run against
            method.newobj(classname);
            method.dup();
            method.invokespecial(classname, "<init>", sig(Void.TYPE));

            // instance config for the script run
            method.newobj(p(RubyInstanceConfig.class));
            method.dup();
            method.invokespecial(p(RubyInstanceConfig.class), "<init>", "()V");

            // set argv from main's args
            method.dup();
            method.aload(0);
            method.invokevirtual(p(RubyInstanceConfig.class), "setArgv", sig(void.class, String[].class));

            // invoke run with threadcontext and topself
            method.invokestatic(p(Ruby.class), "newInstance", sig(Ruby.class, RubyInstanceConfig.class));
            method.dup();

            method.invokevirtual(RUBY, "getCurrentContext", sig(ThreadContext.class));
            method.swap();
            method.invokevirtual(RUBY, "getTopSelf", sig(IRubyObject.class));
            method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

            method.invokevirtual(classname, "load", METHOD_SIGNATURES[4]);
            method.voidreturn();
            method.end();
        }
        
        // add setPosition impl, which stores filename as constant to speed updates
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "setPosition", sig(Void.TYPE, params(ThreadContext.class, int.class)), null, null));
        method.start();

        method.aload(0); // thread context
        method.ldc(sourcename);
        method.iload(1); // line number
        method.invokevirtual(p(ThreadContext.class), "setFileAndLine", sig(void.class, String.class, int.class));
        method.voidreturn();
        method.end();

        cacheCompiler.finish();
        
        endInit();
        endClassInit();
    }

    public static void buildStaticScopeNames(SkinnyMethodAdapter method, StaticScope scope) {
        // construct static scope list of names
        String signature = null;
        switch (scope.getNumberOfVariables()) {
        case 0:
            method.pushInt(0);
            method.anewarray(p(String.class));
            break;
        case 1: case 2: case 3: case 4: case 5:
        case 6: case 7: case 8: case 9: case 10:
            signature = sig(String[].class, params(String.class, scope.getNumberOfVariables()));
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                method.ldc(scope.getVariables()[i]);
            }
            method.invokestatic(p(RuntimeHelpers.class), "constructStringArray", signature);
            break;
        default:
            method.pushInt(scope.getNumberOfVariables());
            method.anewarray(p(String.class));
            for (int i = 0; i < scope.getNumberOfVariables(); i++) {
                method.dup();
                method.pushInt(i);
                method.ldc(scope.getVariables()[i]);
                method.arraystore();
            }
            break;
        }
    }

    private void beginInit() {
        ClassVisitor cv = getClassVisitor();

        initMethod = new SkinnyMethodAdapter(cv.visitMethod(ACC_PUBLIC, "<init>", sig(Void.TYPE), null, null));
        initMethod.start();
        initMethod.aload(THIS);
        initMethod.invokespecial(p(AbstractScript.class), "<init>", sig(Void.TYPE));
        
        cv.visitField(ACC_PRIVATE | ACC_FINAL, "$class", ci(Class.class), null, null);
        
        // FIXME: this really ought to be in clinit, but it doesn't matter much
        initMethod.aload(THIS);
        initMethod.ldc(c(classname));
        initMethod.invokestatic(p(Class.class), "forName", sig(Class.class, params(String.class)));
        initMethod.putfield(classname, "$class", ci(Class.class));
    }

    private void endInit() {
        initMethod.voidreturn();
        initMethod.end();
    }

    private void beginClassInit() {
        ClassVisitor cv = getClassVisitor();

        clinitMethod = new SkinnyMethodAdapter(cv.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(Void.TYPE), null, null));
        clinitMethod.start();
    }

    private void endClassInit() {
        clinitMethod.voidreturn();
        clinitMethod.end();
    }
    
    public SkinnyMethodAdapter getInitMethod() {
        return initMethod;
    }
    
    public SkinnyMethodAdapter getClassInitMethod() {
        return clinitMethod;
    }
    
    public CacheCompiler getCacheCompiler() {
        return cacheCompiler;
    }
    
    public MethodCompiler startMethod(String friendlyName, CompilerCallback args, StaticScope scope, ASTInspector inspector) {
        ASMMethodCompiler methodCompiler = new ASMMethodCompiler(friendlyName, inspector, scope);
        
        methodCompiler.beginMethod(args, scope);
        
        // Emite a nop, to mark the end of the method preamble
        methodCompiler.method.nop();
        
        return methodCompiler;
    }

    public abstract class AbstractMethodCompiler implements MethodCompiler {
        protected SkinnyMethodAdapter method;
        protected VariableCompiler variableCompiler;
        protected InvocationCompiler invocationCompiler;
        
        protected int argParamCount;
        
        protected Label[] currentLoopLabels;
        protected Label scopeStart = new Label();
        protected Label scopeEnd = new Label();
        protected Label redoJump;
        protected boolean withinProtection = false;
        private int lastLine = -1;
        private int lastPositionLine = -1;
        protected StaticScope scope;
        protected ASTInspector inspector;
        protected String methodName;
        
        public AbstractMethodCompiler(StaticScope scope, ASTInspector inspector, String methodName) {
            this.scope = scope;
            this.inspector = inspector;
            this.methodName = methodName;
            if (scope.getRestArg() >= 0 || scope.getOptionalArgs() > 0 || scope.getRequiredArgs() > 3) {
                argParamCount = 1; // use IRubyObject[]
            } else {
                argParamCount = scope.getRequiredArgs(); // specific arity
            }
            
            method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC, methodName, getSignature(), null, null));
            
            createVariableCompiler();
            invocationCompiler = new StandardInvocationCompiler(this, method);
        }
        
        protected abstract String getSignature();
        
        protected abstract void createVariableCompiler();

        public abstract void beginMethod(CompilerCallback args, StaticScope scope);

        public abstract void endMethod();
        
        public class ASMMethodContinuationCompiler extends ASMMethodCompiler {
            public ASMMethodContinuationCompiler(String methodName, ASTInspector inspector, StaticScope scope) {
                super(methodName, inspector, scope);
            }

            public void endMethod() {
                // return last value from execution
                method.areturn();

                // end of variable scope
                Label end = new Label();
                method.label(end);

                method.end();
            }
        }
        
        public MethodCompiler chainToMethod(String methodName, ASTInspector inspector) {
            MethodCompiler compiler = outline(methodName, inspector);
            endMethod();
            return compiler;
        }
        
        public MethodCompiler outline(String methodName, ASTInspector inspector) {
            // chain to the next segment of this giant method
            method.aload(THIS);
            
            // load all arguments straight through
            for (int i = 1; i <= getClosureIndex(); i++) {
                method.aload(i);
            }
            method.invokevirtual(classname, methodName, getSignature());

            ASMMethodContinuationCompiler methodCompiler = new ASMMethodContinuationCompiler(methodName, inspector, scope);

            methodCompiler.beginChainedMethod();

            return methodCompiler;
        }
        
        public StandardASMCompiler getScriptCompiler() {
            return StandardASMCompiler.this;
        }

        public void lineNumber(ISourcePosition position) {
            int thisLine = position.getStartLine();
            
            // No point in updating number if last number was same value.
            if (thisLine != lastLine) {
                lastLine = thisLine;
            } else {
                return;
            }
            
            Label line = new Label();
            method.label(line);
            method.visitLineNumber(thisLine + 1, line);
        }

        public void loadThreadContext() {
            method.aload(THREADCONTEXT_INDEX);
        }

        public void loadSelf() {
            method.aload(SELF_INDEX);
        }
        
        protected int getClosureIndex() {
            return ARGS_INDEX + argParamCount + CLOSURE_OFFSET;
        }
        
        protected int getRuntimeIndex() {
            return ARGS_INDEX + argParamCount + RUNTIME_OFFSET;
        }
        
        protected int getNilIndex() {
            return ARGS_INDEX + argParamCount + NIL_OFFSET;
        }
        
        protected int getPreviousExceptionIndex() {
            return ARGS_INDEX + argParamCount + PREVIOUS_EXCEPTION_OFFSET;
        }
        
        protected int getDynamicScopeIndex() {
            return ARGS_INDEX + argParamCount + DYNAMIC_SCOPE_OFFSET;
        }
        
        protected int getVarsArrayIndex() {
            return ARGS_INDEX + argParamCount + VARS_ARRAY_OFFSET;
        }
        
        protected int getFirstTempIndex() {
            return ARGS_INDEX + argParamCount + FIRST_TEMP_OFFSET;
        }
        
        protected int getExceptionIndex() {
            return ARGS_INDEX + argParamCount + EXCEPTION_OFFSET;
        }
        
        public void loadThis() {
            method.aload(THIS);
        }

        public void loadRuntime() {
            method.aload(getRuntimeIndex());
        }

        public void loadBlock() {
            method.aload(getClosureIndex());
        }

        public void loadNil() {
            method.aload(getNilIndex());
        }
        
        public void loadNull() {
            method.aconst_null();
        }

        public void loadSymbol(String symbol) {
            loadRuntime();

            method.ldc(symbol);

            invokeIRuby("newSymbol", sig(RubySymbol.class, params(String.class)));
        }

        public void loadObject() {
            loadRuntime();

            invokeIRuby("getObject", sig(RubyClass.class, params()));
        }

        /**
         * This is for utility methods used by the compiler, to reduce the amount of code generation
         * necessary.  All of these live in CompilerHelpers.
         */
        public void invokeUtilityMethod(String methodName, String signature) {
            method.invokestatic(p(RuntimeHelpers.class), methodName, signature);
        }

        public void invokeThreadContext(String methodName, String signature) {
            method.invokevirtual(THREADCONTEXT, methodName, signature);
        }

        public void invokeIRuby(String methodName, String signature) {
            method.invokevirtual(RUBY, methodName, signature);
        }

        public void invokeIRubyObject(String methodName, String signature) {
            method.invokeinterface(IRUBYOBJECT, methodName, signature);
        }

        public void consumeCurrentValue() {
            method.pop();
        }

        public void duplicateCurrentValue() {
            method.dup();
        }

        public void swapValues() {
            method.swap();
        }

        public void retrieveSelf() {
            loadSelf();
        }

        public void retrieveSelfClass() {
            loadSelf();
            metaclass();
        }
        
        public VariableCompiler getVariableCompiler() {
            return variableCompiler;
        }
        
        public InvocationCompiler getInvocationCompiler() {
            return invocationCompiler;
        }

        public void assignConstantInCurrent(String name) {
            loadThreadContext();
            method.ldc(name);
            method.dup2_x1();
            method.pop2();
            invokeThreadContext("setConstantInCurrent", sig(IRubyObject.class, params(String.class, IRubyObject.class)));
        }

        public void assignConstantInModule(String name) {
            method.ldc(name);
            loadThreadContext();
            invokeUtilityMethod("setConstantInModule", sig(IRubyObject.class, IRubyObject.class, IRubyObject.class, String.class, ThreadContext.class));
        }

        public void assignConstantInObject(String name) {
            // load Object under value
            loadRuntime();
            invokeIRuby("getObject", sig(RubyClass.class, params()));
            method.swap();

            assignConstantInModule(name);
        }

        public void retrieveConstant(String name) {
            loadThreadContext();
            method.ldc(name);
            invokeThreadContext("getConstant", sig(IRubyObject.class, params(String.class)));
        }

        public void retrieveConstantFromModule(String name) {
            method.visitTypeInsn(CHECKCAST, p(RubyModule.class));
            method.ldc(name);
            method.invokevirtual(p(RubyModule.class), "fastGetConstantFrom", sig(IRubyObject.class, params(String.class)));
        }

        public void retrieveClassVariable(String name) {
            loadThreadContext();
            loadRuntime();
            loadSelf();
            method.ldc(name);

            invokeUtilityMethod("fastFetchClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class)));
        }

        public void assignClassVariable(String name) {
            loadThreadContext();
            method.swap();
            loadRuntime();
            method.swap();
            loadSelf();
            method.swap();
            method.ldc(name);
            method.swap();

            invokeUtilityMethod("fastSetClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
        }

        public void assignClassVariable(String name, CompilerCallback value) {
            loadThreadContext();
            loadRuntime();
            loadSelf();
            method.ldc(name);
            value.call(this);

            invokeUtilityMethod("fastSetClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
        }

        public void declareClassVariable(String name) {
            loadThreadContext();
            method.swap();
            loadRuntime();
            method.swap();
            loadSelf();
            method.swap();
            method.ldc(name);
            method.swap();

            invokeUtilityMethod("fastDeclareClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
        }

        public void declareClassVariable(String name, CompilerCallback value) {
            loadThreadContext();
            loadRuntime();
            loadSelf();
            method.ldc(name);
            value.call(this);

            invokeUtilityMethod("fastDeclareClassVariable", sig(IRubyObject.class, params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
        }

        public void createNewFloat(double value) {
            loadRuntime();
            method.ldc(new Double(value));

            invokeIRuby("newFloat", sig(RubyFloat.class, params(Double.TYPE)));
        }

        public void createNewFixnum(long value) {
            cacheCompiler.cacheFixnum(this, value);
        }

        public void createNewBignum(BigInteger value) {
            loadRuntime();
            getCacheCompiler().cacheBigInteger(this, value);
            method.invokestatic(p(RubyBignum.class), "newBignum", sig(RubyBignum.class, params(Ruby.class, BigInteger.class)));
        }

        public void createNewString(ArrayCallback callback, int count) {
            loadRuntime();
            invokeIRuby("newString", sig(RubyString.class, params()));
            for (int i = 0; i < count; i++) {
                callback.nextValue(this, null, i);
                method.invokevirtual(p(RubyString.class), "append", sig(RubyString.class, params(IRubyObject.class)));
            }
        }

        public void createNewSymbol(ArrayCallback callback, int count) {
            loadRuntime();
            createNewString(callback, count);
            toJavaString();
            invokeIRuby("newSymbol", sig(RubySymbol.class, params(String.class)));
        }

        public void createNewString(ByteList value) {
            // FIXME: this is sub-optimal, storing string value in a java.lang.String again
            loadRuntime();
            getCacheCompiler().cacheByteList(this, value.toString());

            invokeIRuby("newStringShared", sig(RubyString.class, params(ByteList.class)));
        }

        public void createNewSymbol(String name) {
            getCacheCompiler().cacheSymbol(this, name);
        }

        public void createNewArray(boolean lightweight) {
            loadRuntime();
            // put under object array already present
            method.swap();

            if (lightweight) {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopyLight", sig(RubyArray.class, params(Ruby.class, IRubyObject[].class)));
            } else {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopy", sig(RubyArray.class, params(Ruby.class, IRubyObject[].class)));
            }
        }

        public void createNewArray(Object[] sourceArray, ArrayCallback callback, boolean lightweight) {
            loadRuntime();
            
            createObjectArray(sourceArray, callback);

            if (lightweight) {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopyLight", sig(RubyArray.class, params(Ruby.class, IRubyObject[].class)));
            } else {
                method.invokestatic(p(RubyArray.class), "newArrayNoCopy", sig(RubyArray.class, params(Ruby.class, IRubyObject[].class)));
            }
        }

        public void createEmptyArray() {
            loadRuntime();

            invokeIRuby("newArray", sig(RubyArray.class, params()));
        }

        public void createObjectArray(Object[] sourceArray, ArrayCallback callback) {
            buildObjectArray(IRUBYOBJECT, sourceArray, callback);
        }

        public void createObjectArray(int elementCount) {
            // if element count is less than 6, use helper methods
            if (elementCount < 6) {
                Class[] params = new Class[elementCount];
                Arrays.fill(params, IRubyObject.class);
                invokeUtilityMethod("constructObjectArray", sig(IRubyObject[].class, params));
            } else {
                // This is pretty inefficient for building an array, so just raise an error if someone's using it for a lot of elements
                throw new NotCompilableException("Don't use createObjectArray(int) for more than 5 elements");
            }
        }

        private void buildObjectArray(String type, Object[] sourceArray, ArrayCallback callback) {
            if (sourceArray.length == 0) {
                method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            } else if (sourceArray.length <= RuntimeHelpers.MAX_SPECIFIC_ARITY_OBJECT_ARRAY) {
                // if we have a specific-arity helper to construct an array for us, use that
                for (int i = 0; i < sourceArray.length; i++) {
                    callback.nextValue(this, sourceArray, i);
                }
                invokeUtilityMethod("constructObjectArray", sig(IRubyObject[].class, params(IRubyObject.class, sourceArray.length)));
            } else {
                // brute force construction inline
                method.pushInt(sourceArray.length);
                method.anewarray(type);

                for (int i = 0; i < sourceArray.length; i++) {
                    method.dup();
                    method.pushInt(i);

                    callback.nextValue(this, sourceArray, i);

                    method.arraystore();
                }
            }
        }

        public void createEmptyHash() {
            loadRuntime();

            method.invokestatic(p(RubyHash.class), "newHash", sig(RubyHash.class, params(Ruby.class)));
        }

        public void createNewHash(Object elements, ArrayCallback callback, int keyCount) {
            loadRuntime();
            
            if (keyCount <= RuntimeHelpers.MAX_SPECIFIC_ARITY_HASH) {
                // we have a specific-arity method we can use to construct, so use that
                for (int i = 0; i < keyCount; i++) {
                    callback.nextValue(this, elements, i);
                }
                
                invokeUtilityMethod("constructHash", sig(RubyHash.class, params(Ruby.class, IRubyObject.class, keyCount * 2)));
            } else {
                method.invokestatic(p(RubyHash.class), "newHash", sig(RubyHash.class, params(Ruby.class)));

                for (int i = 0; i < keyCount; i++) {
                    method.dup();
                    callback.nextValue(this, elements, i);
                    method.invokevirtual(p(RubyHash.class), "fastASet", sig(void.class, params(IRubyObject.class, IRubyObject.class)));
                }
            }
        }

        public void createNewRange(boolean isExclusive) {
            loadRuntime();
            loadThreadContext();

            // could be more efficient with a callback
            method.dup2_x2();
            method.pop2();

            if (isExclusive) {
                method.invokestatic(p(RubyRange.class), "newExclusiveRange", sig(RubyRange.class, params(Ruby.class, ThreadContext.class, IRubyObject.class, IRubyObject.class)));
            } else {
                method.invokestatic(p(RubyRange.class), "newInclusiveRange", sig(RubyRange.class, params(Ruby.class, ThreadContext.class, IRubyObject.class, IRubyObject.class)));
            }
        }

        /**
         * Invoke IRubyObject.isTrue
         */
        private void isTrue() {
            invokeIRubyObject("isTrue", sig(Boolean.TYPE));
        }

        public void performBooleanBranch(BranchCallback trueBranch, BranchCallback falseBranch) {
            Label afterJmp = new Label();
            Label falseJmp = new Label();

            // call isTrue on the result
            isTrue();

            method.ifeq(falseJmp); // EQ == 0 (i.e. false)
            trueBranch.branch(this);
            method.go_to(afterJmp);

            // FIXME: optimize for cases where we have no false branch
            method.label(falseJmp);
            falseBranch.branch(this);

            method.label(afterJmp);
        }

        public void performLogicalAnd(BranchCallback longBranch) {
            Label falseJmp = new Label();

            // dup it since we need to return appropriately if it's false
            method.dup();

            // call isTrue on the result
            isTrue();

            method.ifeq(falseJmp); // EQ == 0 (i.e. false)
            // pop the extra result and replace with the send part of the AND
            method.pop();
            longBranch.branch(this);
            method.label(falseJmp);
        }

        public void performLogicalOr(BranchCallback longBranch) {
            // FIXME: after jump is not in here.  Will if ever be?
            //Label afterJmp = new Label();
            Label falseJmp = new Label();

            // dup it since we need to return appropriately if it's false
            method.dup();

            // call isTrue on the result
            isTrue();

            method.ifne(falseJmp); // EQ == 0 (i.e. false)
            // pop the extra result and replace with the send part of the AND
            method.pop();
            longBranch.branch(this);
            method.label(falseJmp);
        }

        public void performBooleanLoopSafe(BranchCallback condition, BranchCallback body, boolean checkFirst) {
            String mname = getNewRescueName();
            String signature = sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, Block.class});
            SkinnyMethodAdapter mv = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, mname, signature, null, null));
            SkinnyMethodAdapter old_method = null;
            SkinnyMethodAdapter var_old_method = null;
            SkinnyMethodAdapter inv_old_method = null;
            boolean oldWithinProtection = withinProtection;
            withinProtection = true;
            Label[] oldLoopLabels = currentLoopLabels;
            currentLoopLabels = null;
            int oldArgCount = argParamCount;
            argParamCount = 0; // synthetic methods always have zero arg parameters
            try {
                old_method = this.method;
                var_old_method = getVariableCompiler().getMethodAdapter();
                inv_old_method = getInvocationCompiler().getMethodAdapter();
                this.method = mv;
                getVariableCompiler().setMethodAdapter(mv);
                getInvocationCompiler().setMethodAdapter(mv);

                mv.visitCode();

                // set up a local IRuby variable
                mv.aload(THREADCONTEXT_INDEX);
                mv.dup();
                mv.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                mv.dup();
                mv.astore(getRuntimeIndex());
                
                // store previous exception for restoration if we rescue something
                loadRuntime();
                invokeUtilityMethod("getErrorInfo", sig(IRubyObject.class, Ruby.class));
                mv.astore(getPreviousExceptionIndex());
            
                // grab nil for local variables
                mv.invokevirtual(p(Ruby.class), "getNil", sig(IRubyObject.class));
                mv.astore(getNilIndex());
            
                mv.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
                mv.dup();
                mv.astore(getDynamicScopeIndex());
                mv.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
                mv.astore(getVarsArrayIndex());

                performBooleanLoop(condition, body, checkFirst);
                
                mv.areturn();
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            } finally {
                withinProtection = oldWithinProtection;
                this.method = old_method;
                getVariableCompiler().setMethodAdapter(var_old_method);
                getInvocationCompiler().setMethodAdapter(inv_old_method);
                currentLoopLabels = oldLoopLabels;
                argParamCount = oldArgCount;
            }
            
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            if(this instanceof ASMClosureCompiler) {
                pushNull();
            } else {
                loadBlock();
            }
            method.invokevirtual(classname, mname, signature);
        }

        public void performBooleanLoop(BranchCallback condition, BranchCallback body, boolean checkFirst) {
            // FIXME: handle next/continue, break, etc
            Label tryBegin = new Label();
            Label tryEnd = new Label();
            Label catchRedo = new Label();
            Label catchNext = new Label();
            Label catchBreak = new Label();
            Label catchRaised = new Label();
            Label endOfBody = new Label();
            Label conditionCheck = new Label();
            Label topOfBody = new Label();
            Label done = new Label();
            Label normalLoopEnd = new Label();
            method.trycatch(tryBegin, tryEnd, catchRedo, p(JumpException.RedoJump.class));
            method.trycatch(tryBegin, tryEnd, catchNext, p(JumpException.NextJump.class));
            method.trycatch(tryBegin, tryEnd, catchBreak, p(JumpException.BreakJump.class));
            if (checkFirst) {
                // only while loops seem to have this RaiseException magic
                method.trycatch(tryBegin, tryEnd, catchRaised, p(RaiseException.class));
            }
            
            method.label(tryBegin);
            {
                
                Label[] oldLoopLabels = currentLoopLabels;
                
                currentLoopLabels = new Label[] {endOfBody, topOfBody, done};
                
                // FIXME: if we terminate immediately, this appears to break while in method arguments
                // we need to push a nil for the cases where we will never enter the body
                if (checkFirst) {
                    method.go_to(conditionCheck);
                }

                method.label(topOfBody);

                body.branch(this);
                
                method.label(endOfBody);

                // clear body or next result after each successful loop
                method.pop();
                
                method.label(conditionCheck);
                
                // check the condition
                condition.branch(this);
                isTrue();
                method.ifne(topOfBody); // NE == nonzero (i.e. true)
                
                currentLoopLabels = oldLoopLabels;
            }

            method.label(tryEnd);
            // skip catch block
            method.go_to(normalLoopEnd);

            // catch logic for flow-control exceptions
            {
                // redo jump
                {
                    method.label(catchRedo);
                    method.pop();
                    method.go_to(topOfBody);
                }

                // next jump
                {
                    method.label(catchNext);
                    method.pop();
                    // exceptionNext target is for a next that doesn't push a new value, like this one
                    method.go_to(conditionCheck);
                }

                // break jump
                {
                    method.label(catchBreak);
                    loadBlock();
                    loadThreadContext();
                    invokeUtilityMethod("breakJumpInWhile", sig(IRubyObject.class, JumpException.BreakJump.class, Block.class, ThreadContext.class));
                    method.go_to(done);
                }

                // FIXME: This generates a crapload of extra code that is frequently *never* needed
                // raised exception
                if (checkFirst) {
                    // only while loops seem to have this RaiseException magic
                    method.label(catchRaised);
                    Label raiseNext = new Label();
                    Label raiseRedo = new Label();
                    Label raiseRethrow = new Label();
                    method.dup();
                    invokeUtilityMethod("getLocalJumpTypeOrRethrow", sig(String.class, params(RaiseException.class)));
                    // if we get here we have a RaiseException we know is a local jump error and an error type

                    // is it break?
                    method.dup(); // dup string
                    method.ldc("break");
                    method.invokevirtual(p(String.class), "equals", sig(boolean.class, params(Object.class)));
                    method.ifeq(raiseNext);
                    // pop the extra string, get the break value, and end the loop
                    method.pop();
                    invokeUtilityMethod("unwrapLocalJumpErrorValue", sig(IRubyObject.class, params(RaiseException.class)));
                    method.go_to(done);

                    // is it next?
                    method.label(raiseNext);
                    method.dup();
                    method.ldc("next");
                    method.invokevirtual(p(String.class), "equals", sig(boolean.class, params(Object.class)));
                    method.ifeq(raiseRedo);
                    // pop the extra string and the exception, jump to the condition
                    method.pop2();
                    method.go_to(conditionCheck);

                    // is it redo?
                    method.label(raiseRedo);
                    method.dup();
                    method.ldc("redo");
                    method.invokevirtual(p(String.class), "equals", sig(boolean.class, params(Object.class)));
                    method.ifeq(raiseRethrow);
                    // pop the extra string and the exception, jump to the condition
                    method.pop2();
                    method.go_to(topOfBody);

                    // just rethrow it
                    method.label(raiseRethrow);
                    method.pop(); // pop extra string
                    method.athrow();
                }
            }
            
            method.label(normalLoopEnd);
            loadNil();
            method.label(done);
        }

        public void performBooleanLoopLight(BranchCallback condition, BranchCallback body, boolean checkFirst) {
            Label endOfBody = new Label();
            Label conditionCheck = new Label();
            Label topOfBody = new Label();
            Label done = new Label();
            
            Label[] oldLoopLabels = currentLoopLabels;

            currentLoopLabels = new Label[] {endOfBody, topOfBody, done};

            // FIXME: if we terminate immediately, this appears to break while in method arguments
            // we need to push a nil for the cases where we will never enter the body
            if (checkFirst) {
                method.go_to(conditionCheck);
            }

            method.label(topOfBody);

            body.branch(this);

            method.label(endOfBody);

            // clear body or next result after each successful loop
            method.pop();

            method.label(conditionCheck);

            // check the condition
            condition.branch(this);
            isTrue();
            method.ifne(topOfBody); // NE == nonzero (i.e. true)

            currentLoopLabels = oldLoopLabels;
            
            loadNil();
            method.label(done);
        }

        public void createNewClosure(
                int line,
                StaticScope scope,
                int arity,
                CompilerCallback body,
                CompilerCallback args,
                boolean hasMultipleArgsHead,
                NodeType argsNodeId,
                ASTInspector inspector) {
            String closureMethodName = "block_" + ++innerIndex + "$RUBY$" + "__block__";
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, inspector, scope);
            
            closureCompiler.beginMethod(args, scope);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            
            loadThreadContext();
            loadSelf();
            cacheCompiler.cacheClosure(this, closureMethodName, arity, scope, hasMultipleArgsHead, argsNodeId, inspector);

            invokeUtilityMethod("createBlock", sig(Block.class,
                    params(ThreadContext.class, IRubyObject.class, BlockBody.class)));
        }

        public void runBeginBlock(StaticScope scope, CompilerCallback body) {
            String closureMethodName = "block_" + ++innerIndex + "$RUBY$__begin__";
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, null, scope);
            
            closureCompiler.beginMethod(null, scope);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            loadThreadContext();
            loadSelf();

            buildStaticScopeNames(method, scope);
            
            cacheCompiler.cacheClosureOld(this, closureMethodName);

            invokeUtilityMethod("runBeginBlock", sig(IRubyObject.class,
                    params(ThreadContext.class, IRubyObject.class, String[].class, CompiledBlockCallback.class)));
        }

        public void createNewForLoop(int arity, CompilerCallback body, CompilerCallback args, boolean hasMultipleArgsHead, NodeType argsNodeId) {
            String closureMethodName = "block_" + ++innerIndex + "$RUBY$__for__";
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, null, scope);
            
            closureCompiler.beginMethod(args, null);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            loadThreadContext();
            loadSelf();
            method.pushInt(arity);
            
            cacheCompiler.cacheClosureOld(this, closureMethodName);
            
            method.ldc(Boolean.valueOf(hasMultipleArgsHead));
            method.ldc(BlockBody.asArgumentType(argsNodeId));

            invokeUtilityMethod("createSharedScopeBlock", sig(Block.class,
                    params(ThreadContext.class, IRubyObject.class, Integer.TYPE, CompiledBlockCallback.class, Boolean.TYPE, Integer.TYPE)));
        }

        public void createNewEndBlock(CompilerCallback body) {
            String closureMethodName = "block_" + ++innerIndex + "$RUBY$__end__";
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, null, scope);
            
            closureCompiler.beginMethod(null, null);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            loadThreadContext();
            loadSelf();
            method.iconst_0();
            
            cacheCompiler.cacheClosureOld(this, closureMethodName);
            
            method.iconst_0(); // false
            method.iconst_0(); // zero

            invokeUtilityMethod("createSharedScopeBlock", sig(Block.class,
                    params(ThreadContext.class, IRubyObject.class, Integer.TYPE, CompiledBlockCallback.class, Boolean.TYPE, Integer.TYPE)));
            
            loadRuntime();
            invokeUtilityMethod("registerEndBlock", sig(void.class, Block.class, Ruby.class));
            loadNil();
        }

        public void getCompiledClass() {
            method.aload(THIS);
            method.getfield(classname, "$class", ci(Class.class));
        }

        public void println() {
            method.dup();
            method.getstatic(p(System.class), "out", ci(PrintStream.class));
            method.swap();

            method.invokevirtual(p(PrintStream.class), "println", sig(Void.TYPE, params(Object.class)));
        }

        public void defineAlias(String newName, String oldName) {
            loadThreadContext();
            method.ldc(newName);
            method.ldc(oldName);
            invokeUtilityMethod("defineAlias", sig(IRubyObject.class, ThreadContext.class, String.class, String.class));
        }

        public void loadFalse() {
            // TODO: cache?
            loadRuntime();
            invokeIRuby("getFalse", sig(RubyBoolean.class));
        }

        public void loadTrue() {
            // TODO: cache?
            loadRuntime();
            invokeIRuby("getTrue", sig(RubyBoolean.class));
        }

        public void loadCurrentModule() {
            loadThreadContext();
            invokeThreadContext("getCurrentScope", sig(DynamicScope.class));
            method.invokevirtual(p(DynamicScope.class), "getStaticScope", sig(StaticScope.class));
            method.invokevirtual(p(StaticScope.class), "getModule", sig(RubyModule.class));
        }

        public void retrieveInstanceVariable(String name) {
            loadRuntime();
            loadSelf();
            method.ldc(name);
            invokeUtilityMethod("fastGetInstanceVariable", sig(IRubyObject.class, Ruby.class, IRubyObject.class, String.class));
        }

        public void assignInstanceVariable(String name) {
            // FIXME: more efficient with a callback
            loadSelf();
            invokeIRubyObject("getInstanceVariables", sig(InstanceVariables.class));
            method.swap();

            method.ldc(name);
            method.swap();

            method.invokeinterface(p(InstanceVariables.class), "fastSetInstanceVariable", sig(IRubyObject.class, params(String.class, IRubyObject.class)));
        }

        public void assignInstanceVariable(String name, CompilerCallback value) {
            // FIXME: more efficient with a callback
            loadSelf();
            invokeIRubyObject("getInstanceVariables", sig(InstanceVariables.class));

            method.ldc(name);
            value.call(this);

            method.invokeinterface(p(InstanceVariables.class), "fastSetInstanceVariable", sig(IRubyObject.class, params(String.class, IRubyObject.class)));
        }

        public void retrieveGlobalVariable(String name) {
            loadRuntime();

            invokeIRuby("getGlobalVariables", sig(GlobalVariables.class));
            method.ldc(name);
            method.invokevirtual(p(GlobalVariables.class), "get", sig(IRubyObject.class, params(String.class)));
        }

        public void assignGlobalVariable(String name) {
            // FIXME: more efficient with a callback
            loadRuntime();

            invokeIRuby("getGlobalVariables", sig(GlobalVariables.class));
            method.swap();
            method.ldc(name);
            method.swap();
            method.invokevirtual(p(GlobalVariables.class), "set", sig(IRubyObject.class, params(String.class, IRubyObject.class)));
        }

        public void assignGlobalVariable(String name, CompilerCallback value) {
            // FIXME: more efficient with a callback
            loadRuntime();

            invokeIRuby("getGlobalVariables", sig(GlobalVariables.class));
            method.ldc(name);
            value.call(this);
            method.invokevirtual(p(GlobalVariables.class), "set", sig(IRubyObject.class, params(String.class, IRubyObject.class)));
        }

        public void negateCurrentValue() {
            loadRuntime();
            invokeUtilityMethod("negate", sig(IRubyObject.class, IRubyObject.class, Ruby.class));
        }

        public void splatCurrentValue() {
            method.invokestatic(p(RuntimeHelpers.class), "splatValue", sig(RubyArray.class, params(IRubyObject.class)));
        }

        public void singlifySplattedValue() {
            method.invokestatic(p(RuntimeHelpers.class), "aValueSplat", sig(IRubyObject.class, params(IRubyObject.class)));
        }

        public void aryToAry() {
            method.invokestatic(p(RuntimeHelpers.class), "aryToAry", sig(IRubyObject.class, params(IRubyObject.class)));
        }

        public void ensureRubyArray() {
            invokeUtilityMethod("ensureRubyArray", sig(RubyArray.class, params(IRubyObject.class)));
        }

        public void ensureMultipleAssignableRubyArray(boolean masgnHasHead) {
            loadRuntime();
            method.pushBoolean(masgnHasHead);
            invokeUtilityMethod("ensureMultipleAssignableRubyArray", sig(RubyArray.class, params(IRubyObject.class, Ruby.class, boolean.class)));
        }

        public void forEachInValueArray(int start, int count, Object source, ArrayCallback callback, ArrayCallback nilCallback, CompilerCallback argsCallback) {
            // FIXME: This could probably be made more efficient
            for (; start < count; start++) {
                method.dup(); // dup the original array object
                loadNil();
                method.pushInt(start);
                invokeUtilityMethod("arrayEntryOrNil", sig(IRubyObject.class, RubyArray.class, IRubyObject.class, int.class));
                callback.nextValue(this, source, start);
                method.pop();
            }
            
            if (argsCallback != null) {
                method.dup(); // dup the original array object
                loadRuntime();
                method.pushInt(start);
                invokeUtilityMethod("subarrayOrEmpty", sig(RubyArray.class, RubyArray.class, Ruby.class, int.class));
                argsCallback.call(this);
                method.pop();
            }
        }

        public void asString() {
            method.invokeinterface(p(IRubyObject.class), "asString", sig(RubyString.class));
        }
        
        public void toJavaString() {
            method.invokevirtual(p(Object.class), "toString", sig(String.class));
        }

        public void nthRef(int match) {
            method.pushInt(match);
            backref();
            method.invokestatic(p(RubyRegexp.class), "nth_match", sig(IRubyObject.class, params(Integer.TYPE, IRubyObject.class)));
        }

        public void match() {
            loadThreadContext();
            method.invokevirtual(p(RubyRegexp.class), "op_match2", sig(IRubyObject.class, params(ThreadContext.class)));
        }

        public void match2() {
            loadThreadContext();
            method.swap();
            method.invokevirtual(p(RubyRegexp.class), "op_match", sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class)));
        }

        public void match3() {
            loadThreadContext();
            invokeUtilityMethod("match3", sig(IRubyObject.class, RubyRegexp.class, IRubyObject.class, ThreadContext.class));
        }

        public void createNewRegexp(final ByteList value, final int options) {
            String regexpField = getNewConstant(ci(RubyRegexp.class), "lit_reg_");

            // in current method, load the field to see if we've created a Pattern yet
            method.aload(THIS);
            method.getfield(classname, regexpField, ci(RubyRegexp.class));

            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated); //[]

            // load string, for Regexp#source and Regexp#inspect
            String regexpString = value.toString();

            loadRuntime(); //[R]
            method.ldc(regexpString); //[R, rS]
            method.pushInt(options); //[R, rS, opts]

            method.invokestatic(p(RubyRegexp.class), "newRegexp", sig(RubyRegexp.class, params(Ruby.class, String.class, Integer.TYPE))); //[reg]

            method.aload(THIS); //[reg, T]
            method.swap(); //[T, reg]
            method.putfield(classname, regexpField, ci(RubyRegexp.class)); //[]
            method.label(alreadyCreated);
            method.aload(THIS); //[T]
            method.getfield(classname, regexpField, ci(RubyRegexp.class)); 
        }

        public void createNewRegexp(CompilerCallback createStringCallback, final int options) {
            boolean onceOnly = (options & ReOptions.RE_OPTION_ONCE) != 0;   // for regular expressions with the /o flag
            Label alreadyCreated = null;
            String regexpField = null;

            // only alter the code if the /o flag was present
            if (onceOnly) {
                regexpField = getNewConstant(ci(RubyRegexp.class), "lit_reg_");
    
                // in current method, load the field to see if we've created a Pattern yet
                method.aload(THIS);
                method.getfield(classname, regexpField, ci(RubyRegexp.class));
    
                alreadyCreated = new Label();
                method.ifnonnull(alreadyCreated);
            }

            loadRuntime();

            createStringCallback.call(this);
            method.invokevirtual(p(RubyString.class), "getByteList", sig(ByteList.class));
            method.pushInt(options);

            method.invokestatic(p(RubyRegexp.class), "newRegexp", sig(RubyRegexp.class, params(Ruby.class, ByteList.class, Integer.TYPE))); //[reg]

            // only alter the code if the /o flag was present
            if (onceOnly) {
                method.aload(THIS);
                method.swap();
                method.putfield(classname, regexpField, ci(RubyRegexp.class));
                method.label(alreadyCreated);
                method.aload(THIS);
                method.getfield(classname, regexpField, ci(RubyRegexp.class));
            }
        }

        public void pollThreadEvents() {
            if (!RubyInstanceConfig.THREADLESS_COMPILE_ENABLED) {
                loadThreadContext();
                invokeThreadContext("pollThreadEvents", sig(Void.TYPE));
            }
        }

        public void nullToNil() {
            Label notNull = new Label();
            method.dup();
            method.ifnonnull(notNull);
            method.pop();
            loadNil();
            method.label(notNull);
        }

        public void isInstanceOf(Class clazz, BranchCallback trueBranch, BranchCallback falseBranch) {
            method.instance_of(p(clazz));

            Label falseJmp = new Label();
            Label afterJmp = new Label();

            method.ifeq(falseJmp); // EQ == 0 (i.e. false)
            trueBranch.branch(this);

            method.go_to(afterJmp);
            method.label(falseJmp);

            falseBranch.branch(this);

            method.label(afterJmp);
        }

        public void isCaptured(final int number, final BranchCallback trueBranch, final BranchCallback falseBranch) {
            backref();
            method.dup();
            isInstanceOf(RubyMatchData.class, new BranchCallback() {

                public void branch(MethodCompiler context) {
                    method.visitTypeInsn(CHECKCAST, p(RubyMatchData.class));
                    method.dup();
                    method.invokevirtual(p(RubyMatchData.class), "use", sig(void.class));
                    method.pushInt(number);
                    method.invokevirtual(p(RubyMatchData.class), "group", sig(IRubyObject.class, params(int.class)));
                    method.invokeinterface(p(IRubyObject.class), "isNil", sig(boolean.class));
                    Label isNil = new Label();
                    Label after = new Label();

                    method.ifne(isNil);
                    trueBranch.branch(context);
                    method.go_to(after);

                    method.label(isNil);
                    falseBranch.branch(context);
                    method.label(after);
                }
            }, new BranchCallback() {

                public void branch(MethodCompiler context) {
                    method.pop();
                    falseBranch.branch(context);
                }
            });
        }

        public void branchIfModule(CompilerCallback receiverCallback, BranchCallback moduleCallback, BranchCallback notModuleCallback) {
            receiverCallback.call(this);
            isInstanceOf(RubyModule.class, moduleCallback, notModuleCallback);
        }

        public void backref() {
            loadThreadContext();
            invokeThreadContext("getCurrentFrame", sig(Frame.class));
            method.invokevirtual(p(Frame.class), "getBackRef", sig(IRubyObject.class));
        }

        public void backrefMethod(String methodName) {
            backref();
            method.invokestatic(p(RubyRegexp.class), methodName, sig(IRubyObject.class, params(IRubyObject.class)));
        }
        
        public void issueLoopBreak() {
            // inside a loop, break out of it
            // go to end of loop, leaving break value on stack
            method.go_to(currentLoopLabels[2]);
        }
        
        public void issueLoopNext() {
            // inside a loop, jump to conditional
            method.go_to(currentLoopLabels[0]);
        }
        
        public void issueLoopRedo() {
            // inside a loop, jump to body
            method.go_to(currentLoopLabels[1]);
        }

        protected String getNewEnsureName() {
            return "ensure_" + (ensureNumber++) + "$RUBY$__ensure__";
        }

        public void protect(BranchCallback regularCode, BranchCallback protectedCode, Class ret) {

            String mname = getNewEnsureName();
            SkinnyMethodAdapter mv = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, mname, sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, Block.class}), null, null));
            SkinnyMethodAdapter old_method = null;
            SkinnyMethodAdapter var_old_method = null;
            SkinnyMethodAdapter inv_old_method = null;
            boolean oldWithinProtection = withinProtection;
            withinProtection = true;
            Label[] oldLoopLabels = currentLoopLabels;
            currentLoopLabels = null;
            int oldArgCount = argParamCount;
            argParamCount = 0; // synthetic methods always have zero arg parameters
            try {
                old_method = this.method;
                var_old_method = getVariableCompiler().getMethodAdapter();
                inv_old_method = getInvocationCompiler().getMethodAdapter();
                this.method = mv;
                getVariableCompiler().setMethodAdapter(mv);
                getInvocationCompiler().setMethodAdapter(mv);

                mv.visitCode();
                // set up a local IRuby variable

                mv.aload(THREADCONTEXT_INDEX);
                mv.dup();
                mv.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                mv.dup();
                mv.astore(getRuntimeIndex());
            
                // grab nil for local variables
                mv.invokevirtual(p(Ruby.class), "getNil", sig(IRubyObject.class));
                mv.astore(getNilIndex());
            
                mv.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
                mv.dup();
                mv.astore(getDynamicScopeIndex());
                mv.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
                mv.astore(getVarsArrayIndex());

                Label codeBegin = new Label();
                Label codeEnd = new Label();
                Label ensureBegin = new Label();
                Label ensureEnd = new Label();
                method.label(codeBegin);

                regularCode.branch(this);

                method.label(codeEnd);

                protectedCode.branch(this);
                mv.areturn();

                method.label(ensureBegin);
                method.astore(getExceptionIndex());
                method.label(ensureEnd);

                protectedCode.branch(this);

                method.aload(getExceptionIndex());
                method.athrow();
                
                method.trycatch(codeBegin, codeEnd, ensureBegin, null);
                method.trycatch(ensureBegin, ensureEnd, ensureBegin, null);

                mv.visitMaxs(1, 1);
                mv.visitEnd();
            } finally {
                this.method = old_method;
                getVariableCompiler().setMethodAdapter(var_old_method);
                getInvocationCompiler().setMethodAdapter(inv_old_method);
                withinProtection = oldWithinProtection;
                currentLoopLabels = oldLoopLabels;
                argParamCount = oldArgCount;
            }

            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            if(this instanceof ASMClosureCompiler) {
                pushNull();
            } else {
                loadBlock();
            }
            method.invokevirtual(classname, mname, sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, Block.class}));
        }

        protected String getNewRescueName() {
            return "rescue_" + (rescueNumber++) + "$RUBY$__rescue__";
        }
        
        public void rescue(BranchCallback regularCode, Class exception, BranchCallback catchCode, Class ret) {
            String mname = getNewRescueName();
            SkinnyMethodAdapter mv = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, mname, sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, Block.class}), null, null));
            SkinnyMethodAdapter old_method = null;
            SkinnyMethodAdapter var_old_method = null;
            SkinnyMethodAdapter inv_old_method = null;
            Label afterMethodBody = new Label();
            Label catchRetry = new Label();
            Label catchRaised = new Label();
            Label catchJumps = new Label();
            Label exitRescue = new Label();
            boolean oldWithinProtection = withinProtection;
            withinProtection = true;
            Label[] oldLoopLabels = currentLoopLabels;
            currentLoopLabels = null;
            int oldArgCount = argParamCount;
            argParamCount = 0; // synthetic methods always have zero arg parameters
            try {
                old_method = this.method;
                var_old_method = getVariableCompiler().getMethodAdapter();
                inv_old_method = getInvocationCompiler().getMethodAdapter();
                this.method = mv;
                getVariableCompiler().setMethodAdapter(mv);
                getInvocationCompiler().setMethodAdapter(mv);

                mv.visitCode();

                // set up a local IRuby variable
                mv.aload(THREADCONTEXT_INDEX);
                mv.dup();
                mv.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                mv.dup();
                mv.astore(getRuntimeIndex());
                
                // store previous exception for restoration if we rescue something
                loadRuntime();
                invokeUtilityMethod("getErrorInfo", sig(IRubyObject.class, Ruby.class));
                mv.astore(getPreviousExceptionIndex());
            
                // grab nil for local variables
                mv.invokevirtual(p(Ruby.class), "getNil", sig(IRubyObject.class));
                mv.astore(getNilIndex());
            
                mv.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
                mv.dup();
                mv.astore(getDynamicScopeIndex());
                mv.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
                mv.astore(getVarsArrayIndex());

                Label beforeBody = new Label();
                Label afterBody = new Label();
                Label catchBlock = new Label();
                mv.visitTryCatchBlock(beforeBody, afterBody, catchBlock, p(exception));
                mv.visitLabel(beforeBody);

                regularCode.branch(this);

                mv.label(afterBody);
                mv.go_to(exitRescue);
                mv.label(catchBlock);
                mv.astore(getExceptionIndex());

                catchCode.branch(this);
                
                mv.label(afterMethodBody);
                mv.go_to(exitRescue);
                
                // retry handling in the rescue block
                mv.trycatch(catchBlock, afterMethodBody, catchRetry, p(JumpException.RetryJump.class));
                mv.label(catchRetry);
                mv.pop();
                mv.go_to(beforeBody);
                
                // any exceptions raised must continue to be raised, skipping $! restoration
                mv.trycatch(beforeBody, afterMethodBody, catchRaised, p(RaiseException.class));
                mv.label(catchRaised);
                mv.athrow();
                
                // and remaining jump exceptions should restore $!
                mv.trycatch(beforeBody, afterMethodBody, catchJumps, p(JumpException.class));
                mv.label(catchJumps);
                loadRuntime();
                mv.aload(getPreviousExceptionIndex());
                invokeUtilityMethod("setErrorInfo", sig(void.class, Ruby.class, IRubyObject.class));
                mv.athrow();
                
                mv.label(exitRescue);
                
                // restore the original exception
                loadRuntime();
                mv.aload(getPreviousExceptionIndex());
                invokeUtilityMethod("setErrorInfo", sig(void.class, Ruby.class, IRubyObject.class));
                
                mv.areturn();
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            } finally {
                withinProtection = oldWithinProtection;
                this.method = old_method;
                getVariableCompiler().setMethodAdapter(var_old_method);
                getInvocationCompiler().setMethodAdapter(inv_old_method);
                currentLoopLabels = oldLoopLabels;
                argParamCount = oldArgCount;
            }
            
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            if(this instanceof ASMClosureCompiler) {
                pushNull();
            } else {
                loadBlock();
            }
            method.invokevirtual(classname, mname, sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, Block.class}));
        }

        public void performRescue(BranchCallback regularCode, BranchCallback rubyCatchCode, BranchCallback javaCatchCode) {
            String mname = getNewRescueName();
            SkinnyMethodAdapter mv = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, mname, sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, Block.class}), null, null));
            SkinnyMethodAdapter old_method = null;
            SkinnyMethodAdapter var_old_method = null;
            SkinnyMethodAdapter inv_old_method = null;
            Label afterRubyCatchBody = new Label();
            Label afterJavaCatchBody = new Label();
            Label rubyCatchRetry = new Label();
            Label rubyCatchRaised = new Label();
            Label rubyCatchJumps = new Label();
            Label javaCatchRetry = new Label();
            Label javaCatchRaised = new Label();
            Label javaCatchJumps = new Label();
            Label exitRescue = new Label();
            boolean oldWithinProtection = withinProtection;
            withinProtection = true;
            Label[] oldLoopLabels = currentLoopLabels;
            currentLoopLabels = null;
            int oldArgCount = argParamCount;
            argParamCount = 0; // synthetic methods always have zero arg parameters
            try {
                old_method = this.method;
                var_old_method = getVariableCompiler().getMethodAdapter();
                inv_old_method = getInvocationCompiler().getMethodAdapter();
                this.method = mv;
                getVariableCompiler().setMethodAdapter(mv);
                getInvocationCompiler().setMethodAdapter(mv);

                mv.visitCode();

                // set up a local IRuby variable
                mv.aload(THREADCONTEXT_INDEX);
                mv.dup();
                mv.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                mv.dup();
                mv.astore(getRuntimeIndex());
                
                // store previous exception for restoration if we rescue something
                loadRuntime();
                invokeUtilityMethod("getErrorInfo", sig(IRubyObject.class, Ruby.class));
                mv.astore(getPreviousExceptionIndex());
            
                // grab nil for local variables
                mv.invokevirtual(p(Ruby.class), "getNil", sig(IRubyObject.class));
                mv.astore(getNilIndex());
            
                mv.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
                mv.dup();
                mv.astore(getDynamicScopeIndex());
                mv.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
                mv.astore(getVarsArrayIndex());

                Label beforeBody = new Label();
                Label afterBody = new Label();
                Label rubyCatchBlock = new Label();
                Label flowCatchBlock = new Label();
                Label javaCatchBlock = new Label();
                mv.visitTryCatchBlock(beforeBody, afterBody, rubyCatchBlock, p(RaiseException.class));
                mv.visitTryCatchBlock(beforeBody, afterBody, flowCatchBlock, p(JumpException.FlowControlException.class));
                mv.visitTryCatchBlock(beforeBody, afterBody, javaCatchBlock, p(Exception.class));
                
                mv.visitLabel(beforeBody);
                {
                    regularCode.branch(this);
                }
                mv.label(afterBody);
                mv.go_to(exitRescue);
                
                // first handle Ruby exceptions (RaiseException)
                mv.label(rubyCatchBlock);
                {
                    mv.astore(getExceptionIndex());

                    rubyCatchCode.branch(this);
                    mv.label(afterRubyCatchBody);
                    mv.go_to(exitRescue);

                    // retry handling in the rescue block
                    mv.trycatch(rubyCatchBlock, afterRubyCatchBody, rubyCatchRetry, p(JumpException.RetryJump.class));
                    mv.label(rubyCatchRetry);
                    {
                        mv.pop();
                    }
                    mv.go_to(beforeBody);

                    // any exceptions raised must continue to be raised, skipping $! restoration
                    mv.trycatch(beforeBody, afterRubyCatchBody, rubyCatchRaised, p(RaiseException.class));
                    mv.label(rubyCatchRaised);
                    {
                        mv.athrow();
                    }

                    // and remaining jump exceptions should restore $!
                    mv.trycatch(beforeBody, afterRubyCatchBody, rubyCatchJumps, p(JumpException.class));
                    mv.label(rubyCatchJumps);
                    {
                        loadRuntime();
                        mv.aload(getPreviousExceptionIndex());
                        invokeUtilityMethod("setErrorInfo", sig(void.class, Ruby.class, IRubyObject.class));
                        mv.athrow();
                    }
                }
                
                // Next handle Flow exceptions, just propagating them
                mv.label(flowCatchBlock);
                {
                    // restore the original exception
                    loadRuntime();
                    mv.aload(getPreviousExceptionIndex());
                    invokeUtilityMethod("setErrorInfo", sig(void.class, Ruby.class, IRubyObject.class));

                    // rethrow
                    mv.athrow();
                }
                
                // now handle Java exceptions
                mv.label(javaCatchBlock);
                {
                    mv.astore(getExceptionIndex());
                    
                    javaCatchCode.branch(this);
                    mv.label(afterJavaCatchBody);
                    mv.go_to(exitRescue);

                    // retry handling in the rescue block
                    mv.trycatch(javaCatchBlock, afterJavaCatchBody, javaCatchRetry, p(JumpException.RetryJump.class));
                    mv.label(javaCatchRetry);
                    {
                        mv.pop();
                    }
                    mv.go_to(beforeBody);

                    // any exceptions raised must continue to be raised, skipping $! restoration
                    mv.trycatch(javaCatchBlock, afterJavaCatchBody, javaCatchRaised, p(RaiseException.class));
                    mv.label(javaCatchRaised);
                    {
                        mv.athrow();
                    }

                    // and remaining jump exceptions should restore $!
                    mv.trycatch(javaCatchBlock, afterJavaCatchBody, javaCatchJumps, p(JumpException.class));
                    mv.label(javaCatchJumps);
                    {
                        loadRuntime();
                        mv.aload(getPreviousExceptionIndex());
                        invokeUtilityMethod("setErrorInfo", sig(void.class, Ruby.class, IRubyObject.class));
                        mv.athrow();
                    }
                }
                
                mv.label(exitRescue);
                
                // restore the original exception
                loadRuntime();
                mv.aload(getPreviousExceptionIndex());
                invokeUtilityMethod("setErrorInfo", sig(void.class, Ruby.class, IRubyObject.class));
                
                mv.areturn();
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            } finally {
                withinProtection = oldWithinProtection;
                this.method = old_method;
                getVariableCompiler().setMethodAdapter(var_old_method);
                getInvocationCompiler().setMethodAdapter(inv_old_method);
                currentLoopLabels = oldLoopLabels;
                argParamCount = oldArgCount;
            }
            
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            if(this instanceof ASMClosureCompiler) {
                pushNull();
            } else {
                loadBlock();
            }
            method.invokevirtual(classname, mname, sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, Block.class}));
        }
        
        public void wrapJavaException() {
            loadRuntime();
            loadException();
            wrapJavaObject();
        }
        
        public void wrapJavaObject() {
            method.invokestatic(p(JavaUtil.class), "convertJavaToUsableRubyObject", sig(IRubyObject.class, Ruby.class, Object.class));
        }

        public void inDefined() {
            method.aload(THREADCONTEXT_INDEX);
            method.iconst_1();
            invokeThreadContext("setWithinDefined", sig(void.class, params(boolean.class)));
        }

        public void outDefined() {
            method.aload(THREADCONTEXT_INDEX);
            method.iconst_0();
            invokeThreadContext("setWithinDefined", sig(void.class, params(boolean.class)));
        }

        public void stringOrNil() {
            loadRuntime();
            loadNil();
            invokeUtilityMethod("stringOrNil", sig(IRubyObject.class, String.class, Ruby.class, IRubyObject.class));
        }

        public void pushNull() {
            method.aconst_null();
        }

        public void pushString(String str) {
            method.ldc(str);
        }

        public void isMethodBound(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
            metaclass();
            method.ldc(name);
            method.iconst_0(); // push false
            method.invokevirtual(p(RubyClass.class), "isMethodBound", sig(boolean.class, params(String.class, boolean.class)));
            Label falseLabel = new Label();
            Label exitLabel = new Label();
            method.ifeq(falseLabel); // EQ == 0 (i.e. false)
            trueBranch.branch(this);
            method.go_to(exitLabel);
            method.label(falseLabel);
            falseBranch.branch(this);
            method.label(exitLabel);
        }

        public void hasBlock(BranchCallback trueBranch, BranchCallback falseBranch) {
            loadBlock();
            method.invokevirtual(p(Block.class), "isGiven", sig(boolean.class));
            Label falseLabel = new Label();
            Label exitLabel = new Label();
            method.ifeq(falseLabel); // EQ == 0 (i.e. false)
            trueBranch.branch(this);
            method.go_to(exitLabel);
            method.label(falseLabel);
            falseBranch.branch(this);
            method.label(exitLabel);
        }
        
        public void isGlobalDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
            loadRuntime();
            invokeIRuby("getGlobalVariables", sig(GlobalVariables.class));
            method.ldc(name);
            method.invokevirtual(p(GlobalVariables.class), "isDefined", sig(boolean.class, params(String.class)));
            Label falseLabel = new Label();
            Label exitLabel = new Label();
            method.ifeq(falseLabel); // EQ == 0 (i.e. false)
            trueBranch.branch(this);
            method.go_to(exitLabel);
            method.label(falseLabel);
            falseBranch.branch(this);
            method.label(exitLabel);
        }
        
        public void isConstantDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
            loadThreadContext();
            method.ldc(name);
            invokeThreadContext("getConstantDefined", sig(boolean.class, params(String.class)));
            Label falseLabel = new Label();
            Label exitLabel = new Label();
            method.ifeq(falseLabel); // EQ == 0 (i.e. false)
            trueBranch.branch(this);
            method.go_to(exitLabel);
            method.label(falseLabel);
            falseBranch.branch(this);
            method.label(exitLabel);
        }
        
        public void isInstanceVariableDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch) {
            loadSelf();
            invokeIRubyObject("getInstanceVariables", sig(InstanceVariables.class));
            method.ldc(name);
            //method.invokeinterface(p(IRubyObject.class), "getInstanceVariable", sig(IRubyObject.class, params(String.class)));
            method.invokeinterface(p(InstanceVariables.class), "fastHasInstanceVariable", sig(boolean.class, params(String.class)));
            Label trueLabel = new Label();
            Label exitLabel = new Label();
            //method.ifnonnull(trueLabel);
            method.ifne(trueLabel);
            falseBranch.branch(this);
            method.go_to(exitLabel);
            method.label(trueLabel);
            trueBranch.branch(this);
            method.label(exitLabel);
        }
        
        public void isClassVarDefined(String name, BranchCallback trueBranch, BranchCallback falseBranch){
            method.ldc(name);
            method.invokevirtual(p(RubyModule.class), "fastIsClassVarDefined", sig(boolean.class, params(String.class)));
            Label trueLabel = new Label();
            Label exitLabel = new Label();
            method.ifne(trueLabel);
            falseBranch.branch(this);
            method.go_to(exitLabel);
            method.label(trueLabel);
            trueBranch.branch(this);
            method.label(exitLabel);
        }
        
        public Object getNewEnding() {
            return new Label();
        }
        
        public void isNil(BranchCallback trueBranch, BranchCallback falseBranch) {
            method.invokeinterface(p(IRubyObject.class), "isNil", sig(boolean.class));
            Label falseLabel = new Label();
            Label exitLabel = new Label();
            method.ifeq(falseLabel); // EQ == 0 (i.e. false)
            trueBranch.branch(this);
            method.go_to(exitLabel);
            method.label(falseLabel);
            falseBranch.branch(this);
            method.label(exitLabel);
        }
        
        public void isNull(BranchCallback trueBranch, BranchCallback falseBranch) {
            Label falseLabel = new Label();
            Label exitLabel = new Label();
            method.ifnonnull(falseLabel);
            trueBranch.branch(this);
            method.go_to(exitLabel);
            method.label(falseLabel);
            falseBranch.branch(this);
            method.label(exitLabel);
        }
        
        public void ifNull(Object gotoToken) {
            method.ifnull((Label)gotoToken);
        }
        
        public void ifNotNull(Object gotoToken) {
            method.ifnonnull((Label)gotoToken);
        }
        
        public void setEnding(Object endingToken){
            method.label((Label)endingToken);
        }
        
        public void go(Object gotoToken) {
            method.go_to((Label)gotoToken);
        }
        
        public void isConstantBranch(final BranchCallback setup, final BranchCallback isConstant, final BranchCallback isMethod, final BranchCallback none, final String name) {
            rescue(new BranchCallback() {
                    public void branch(MethodCompiler context) {
                        setup.branch(AbstractMethodCompiler.this);
                        method.dup(); //[C,C]
                        method.instance_of(p(RubyModule.class)); //[C, boolean]

                        Label falseJmp = new Label();
                        Label afterJmp = new Label();
                        Label nextJmp = new Label();
                        Label nextJmpPop = new Label();

                        method.ifeq(nextJmp); // EQ == 0 (i.e. false)   //[C]
                        method.visitTypeInsn(CHECKCAST, p(RubyModule.class));
                        method.dup(); //[C, C]
                        method.ldc(name); //[C, C, String]
                        method.invokevirtual(p(RubyModule.class), "fastGetConstantAt", sig(IRubyObject.class, params(String.class))); //[C, null|C]
                        method.dup();
                        method.ifnull(nextJmpPop);
                        method.pop(); method.pop();

                        isConstant.branch(AbstractMethodCompiler.this);

                        method.go_to(afterJmp);
                        
                        method.label(nextJmpPop);
                        method.pop();

                        method.label(nextJmp); //[C]

                        metaclass();
                        method.ldc(name);
                        method.iconst_1(); // push true
                        method.invokevirtual(p(RubyClass.class), "isMethodBound", sig(boolean.class, params(String.class, boolean.class)));
                        method.ifeq(falseJmp); // EQ == 0 (i.e. false)
                        
                        isMethod.branch(AbstractMethodCompiler.this);
                        method.go_to(afterJmp);

                        method.label(falseJmp);
                        none.branch(AbstractMethodCompiler.this);
            
                        method.label(afterJmp);
                    }}, JumpException.class, none, String.class);
        }
        
        public void metaclass() {
            invokeIRubyObject("getMetaClass", sig(RubyClass.class));
        }
        
        public void aprintln() {
            method.aprintln();
        }
        
        public void getVisibilityFor(String name) {
            method.ldc(name);
            method.invokevirtual(p(RubyClass.class), "searchMethod", sig(DynamicMethod.class, params(String.class)));
            method.invokevirtual(p(DynamicMethod.class), "getVisibility", sig(Visibility.class));
        }
        
        public void isPrivate(Object gotoToken, int toConsume) {
            method.getstatic(p(Visibility.class), "PRIVATE", ci(Visibility.class));
            Label temp = new Label();
            method.if_acmpne(temp);
            while((toConsume--) > 0) {
                  method.pop();
            }
            method.go_to((Label)gotoToken);
            method.label(temp);
        }
        
        public void isNotProtected(Object gotoToken, int toConsume) {
            method.getstatic(p(Visibility.class), "PROTECTED", ci(Visibility.class));
            Label temp = new Label();
            method.if_acmpeq(temp);
            while((toConsume--) > 0) {
                  method.pop();
            }
            method.go_to((Label)gotoToken);
            method.label(temp);
        }
        
        public void selfIsKindOf(Object gotoToken) {
            method.invokevirtual(p(RubyClass.class), "getRealClass", sig(RubyClass.class));
            loadSelf();
            method.invokevirtual(p(RubyModule.class), "isInstance", sig(boolean.class, params(IRubyObject.class)));
            method.ifne((Label)gotoToken); // EQ != 0 (i.e. true)
        }
        
        public void notIsModuleAndClassVarDefined(String name, Object gotoToken) {
            method.dup(); //[?, ?]
            method.instance_of(p(RubyModule.class)); //[?, boolean]
            Label falsePopJmp = new Label();
            Label successJmp = new Label();
            method.ifeq(falsePopJmp);

            method.visitTypeInsn(CHECKCAST, p(RubyModule.class)); //[RubyModule]
            method.ldc(name); //[RubyModule, String]
            
            method.invokevirtual(p(RubyModule.class), "fastIsClassVarDefined", sig(boolean.class, params(String.class))); //[boolean]
            method.ifeq((Label)gotoToken);
            method.go_to(successJmp);
            method.label(falsePopJmp);
            method.pop();
            method.go_to((Label)gotoToken);
            method.label(successJmp);
        }
        
        public void ifSingleton(Object gotoToken) {
            method.invokevirtual(p(RubyModule.class), "isSingleton", sig(boolean.class));
            method.ifne((Label)gotoToken); // EQ == 0 (i.e. false)
        }
        
        public void getInstanceVariable(String name) {
            method.ldc(name);
            invokeIRubyObject("getInstanceVariables", sig(InstanceVariables.class));
            method.invokeinterface(p(InstanceVariables.class), "fastGetInstanceVariable", sig(IRubyObject.class, params(String.class)));
        }
        
        public void getFrameName() {
            loadThreadContext();
            invokeThreadContext("getFrameName", sig(String.class));
        }
        
        public void getFrameKlazz() {
            loadThreadContext();
            invokeThreadContext("getFrameKlazz", sig(RubyModule.class));
        }
        
        public void superClass() {
            method.invokevirtual(p(RubyModule.class), "getSuperClass", sig(RubyClass.class));
        }
        public void attached() {
            method.visitTypeInsn(CHECKCAST, p(MetaClass.class));
            method.invokevirtual(p(MetaClass.class), "getAttached", sig(IRubyObject.class));
        }
        public void ifNotSuperMethodBound(Object token) {
            method.swap();
            method.iconst_0();
            method.invokevirtual(p(RubyModule.class), "isMethodBound", sig(boolean.class, params(String.class, boolean.class)));
            method.ifeq((Label)token);
        }
        
        public void concatArrays() {
            method.invokevirtual(p(RubyArray.class), "concat", sig(RubyArray.class, params(IRubyObject.class)));
        }
        
        public void concatObjectArrays() {
            invokeUtilityMethod("concatObjectArrays", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject[].class)));
        }
        
        public void appendToArray() {
            method.invokevirtual(p(RubyArray.class), "append", sig(RubyArray.class, params(IRubyObject.class)));
        }
        
        public void appendToObjectArray() {
            invokeUtilityMethod("appendToObjectArray", sig(IRubyObject[].class, params(IRubyObject[].class, IRubyObject.class)));
        }
        
        public void convertToJavaArray() {
            method.invokestatic(p(ArgsUtil.class), "convertToJavaArray", sig(IRubyObject[].class, params(IRubyObject.class)));
        }

        public void aliasGlobal(String newName, String oldName) {
            loadRuntime();
            invokeIRuby("getGlobalVariables", sig(GlobalVariables.class));
            method.ldc(newName);
            method.ldc(oldName);
            method.invokevirtual(p(GlobalVariables.class), "alias", sig(Void.TYPE, params(String.class, String.class)));
            loadNil();
        }
        
        public void undefMethod(String name) {
            loadThreadContext();
            invokeThreadContext("getRubyClass", sig(RubyModule.class));
            
            Label notNull = new Label();
            method.dup();
            method.ifnonnull(notNull);
            method.pop();
            loadRuntime();
            method.ldc("No class to undef method '" + name + "'.");
            invokeIRuby("newTypeError", sig(RaiseException.class, params(String.class)));
            method.athrow();
            
            method.label(notNull);
            loadThreadContext();
            method.ldc(name);
            method.invokevirtual(p(RubyModule.class), "undef", sig(Void.TYPE, params(ThreadContext.class, String.class)));
            
            loadNil();
        }

        public void defineClass(
                final String name, 
                final StaticScope staticScope, 
                final CompilerCallback superCallback, 
                final CompilerCallback pathCallback, 
                final CompilerCallback bodyCallback, 
                final CompilerCallback receiverCallback) {
            String methodName = null;
            if (receiverCallback == null) {
                String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
                methodName = "class_" + ++methodIndex + "$RUBY$" + mangledName;
            } else {
                methodName = "sclass_" + ++methodIndex + "$RUBY$__singleton__";
            }

            final ASMMethodCompiler methodCompiler = new ASMMethodCompiler(methodName, null, staticScope);
            
            CompilerCallback bodyPrep = new CompilerCallback() {
                public void call(MethodCompiler context) {
                    if (receiverCallback == null) {
                        if (superCallback != null) {
                            methodCompiler.loadRuntime();
                            superCallback.call(methodCompiler);

                            methodCompiler.invokeUtilityMethod("prepareSuperClass", sig(RubyClass.class, params(Ruby.class, IRubyObject.class)));
                        } else {
                            methodCompiler.method.aconst_null();
                        }

                        methodCompiler.loadThreadContext();

                        pathCallback.call(methodCompiler);

                        methodCompiler.invokeUtilityMethod("prepareClassNamespace", sig(RubyModule.class, params(ThreadContext.class, IRubyObject.class)));

                        methodCompiler.method.swap();

                        methodCompiler.method.ldc(name);

                        methodCompiler.method.swap();

                        methodCompiler.method.invokevirtual(p(RubyModule.class), "defineOrGetClassUnder", sig(RubyClass.class, params(String.class, RubyClass.class)));
                    } else {
                        methodCompiler.loadRuntime();

                        // we re-set self to the class, but store the old self in a temporary local variable
                        // this is to prevent it GCing in case the singleton is short-lived
                        methodCompiler.method.aload(SELF_INDEX);
                        int selfTemp = methodCompiler.getVariableCompiler().grabTempLocal();
                        methodCompiler.getVariableCompiler().setTempLocal(selfTemp);
                        methodCompiler.method.aload(SELF_INDEX);

                        methodCompiler.invokeUtilityMethod("getSingletonClass", sig(RubyClass.class, params(Ruby.class, IRubyObject.class)));
                    }

                    // set self to the class
                    methodCompiler.method.dup();
                    methodCompiler.method.astore(SELF_INDEX);

                    // CLASS BODY
                    methodCompiler.loadThreadContext();
                    methodCompiler.method.swap();

                    // static scope
                    buildStaticScopeNames(methodCompiler.method, staticScope);
                    methodCompiler.invokeThreadContext("preCompiledClass", sig(Void.TYPE, params(RubyModule.class, String[].class)));
                }
            };

            // Here starts the logic for the class definition
            Label start = new Label();
            Label end = new Label();
            Label after = new Label();
            Label noException = new Label();
            methodCompiler.method.trycatch(start, end, after, null);

            methodCompiler.beginClass(bodyPrep, staticScope);

            methodCompiler.method.label(start);

            bodyCallback.call(methodCompiler);
            methodCompiler.method.label(end);
            // finally with no exception
            methodCompiler.loadThreadContext();
            methodCompiler.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));
            
            methodCompiler.method.go_to(noException);
            
            methodCompiler.method.label(after);
            // finally with exception
            methodCompiler.loadThreadContext();
            methodCompiler.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));
            methodCompiler.method.athrow();
            
            methodCompiler.method.label(noException);

            methodCompiler.endMethod();

            // prepare to call class definition method
            method.aload(THIS);
            loadThreadContext();
            if (receiverCallback == null) {
                // if there's no receiver, there could potentially be a superclass like class Foo << self
                // so we pass in self here
                method.aload(SELF_INDEX);
            } else {
                // otherwise, there's a receiver, so we pass that in directly for the sclass logic
                receiverCallback.call(this);
            }
            method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

            method.invokevirtual(classname, methodName, METHOD_SIGNATURES[0]);
        }

        public void defineModule(final String name, final StaticScope staticScope, final CompilerCallback pathCallback, final CompilerCallback bodyCallback) {
            String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
            String methodName = "module__" + ++methodIndex + "$RUBY$" + mangledName;

            final ASMMethodCompiler methodCompiler = new ASMMethodCompiler(methodName, null, staticScope);

            CompilerCallback bodyPrep = new CompilerCallback() {
                public void call(MethodCompiler context) {
                    methodCompiler.loadThreadContext();

                    pathCallback.call(methodCompiler);

                    methodCompiler.invokeUtilityMethod("prepareClassNamespace", sig(RubyModule.class, params(ThreadContext.class, IRubyObject.class)));

                    methodCompiler.method.ldc(name);

                    methodCompiler.method.invokevirtual(p(RubyModule.class), "defineOrGetModuleUnder", sig(RubyModule.class, params(String.class)));

                    // set self to the class
                    methodCompiler.method.dup();
                    methodCompiler.method.astore(SELF_INDEX);

                    // CLASS BODY
                    methodCompiler.loadThreadContext();
                    methodCompiler.method.swap();

                    // static scope
                    buildStaticScopeNames(methodCompiler.method, staticScope);

                    methodCompiler.invokeThreadContext("preCompiledClass", sig(Void.TYPE, params(RubyModule.class, String[].class)));
                }
            };

            // Here starts the logic for the class definition
            Label start = new Label();
            Label end = new Label();
            Label after = new Label();
            Label noException = new Label();
            methodCompiler.method.trycatch(start, end, after, null);
            
            methodCompiler.beginClass(bodyPrep, staticScope);

            methodCompiler.method.label(start);

            bodyCallback.call(methodCompiler);
            methodCompiler.method.label(end);
            
            methodCompiler.method.go_to(noException);
            
            methodCompiler.method.label(after);
            methodCompiler.loadThreadContext();
            methodCompiler.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));
            methodCompiler.method.athrow();
            
            methodCompiler.method.label(noException);
            methodCompiler.loadThreadContext();
            methodCompiler.invokeThreadContext("postCompiledClass", sig(Void.TYPE, params()));

            methodCompiler.endMethod();

            // prepare to call class definition method
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            method.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
            method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

            method.invokevirtual(classname, methodName, METHOD_SIGNATURES[4]);
        }
        
        public void unwrapPassedBlock() {
            loadBlock();
            invokeUtilityMethod("getBlockFromBlockPassBody", sig(Block.class, params(IRubyObject.class, Block.class)));
        }
        
        public void performBackref(char type) {
            loadThreadContext();
            switch (type) {
            case '~':
                invokeUtilityMethod("backref", sig(IRubyObject.class, params(ThreadContext.class)));
                break;
            case '&':
                invokeUtilityMethod("backrefLastMatch", sig(IRubyObject.class, params(ThreadContext.class)));
                break;
            case '`':
                invokeUtilityMethod("backrefMatchPre", sig(IRubyObject.class, params(ThreadContext.class)));
                break;
            case '\'':
                invokeUtilityMethod("backrefMatchPost", sig(IRubyObject.class, params(ThreadContext.class)));
                break;
            case '+':
                invokeUtilityMethod("backrefMatchLast", sig(IRubyObject.class, params(ThreadContext.class)));
                break;
            default:
                throw new NotCompilableException("ERROR: backref with invalid type");
            }
        }
        
        public void callZSuper(CompilerCallback closure) {
            loadRuntime();
            loadThreadContext();
            if (closure != null) {
                closure.call(this);
            } else {
                method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            }
            loadSelf();
            
            invokeUtilityMethod("callZSuper", sig(IRubyObject.class, params(Ruby.class, ThreadContext.class, Block.class, IRubyObject.class)));
        }
        
        public void checkIsExceptionHandled() {
            // ruby exception and list of exception types is on the stack
            loadRuntime();
            loadThreadContext();
            loadSelf();
            invokeUtilityMethod("isExceptionHandled", sig(IRubyObject.class, RubyException.class, IRubyObject[].class, Ruby.class, ThreadContext.class, IRubyObject.class));
        }
        
        public void checkIsJavaExceptionHandled() {
            // ruby exception and list of exception types is on the stack
            loadRuntime();
            loadThreadContext();
            loadSelf();
            invokeUtilityMethod("isJavaExceptionHandled", sig(IRubyObject.class, Exception.class, IRubyObject[].class, Ruby.class, ThreadContext.class, IRubyObject.class));
        }
        
        public void rethrowException() {
            loadException();
            method.athrow();
        }
        
        public void loadClass(String name) {
            loadRuntime();
            method.ldc(name);
            invokeIRuby("getClass", sig(RubyClass.class, String.class));
        }
        
        public void unwrapRaiseException() {
            // RaiseException is on stack, get RubyException out
            method.invokevirtual(p(RaiseException.class), "getException", sig(RubyException.class));
        }
        
        public void loadException() {
            method.aload(getExceptionIndex());
        }
        
        public void setFilePosition(ISourcePosition position) {
            if (!RubyInstanceConfig.POSITIONLESS_COMPILE_ENABLED) {
                loadThreadContext();
                method.ldc(position.getFile());
                invokeThreadContext("setFile", sig(void.class, params(String.class)));
            }
        }

        public void setLinePosition(ISourcePosition position) {
            if (!RubyInstanceConfig.POSITIONLESS_COMPILE_ENABLED) {
                if (lastPositionLine == position.getStartLine()) {
                    // updating position for same line; skip
                    return;
                } else {
                    lastPositionLine = position.getStartLine();
                    loadThreadContext();
                    method.pushInt(position.getStartLine());
                    method.invokestatic(classname, "setPosition", sig(void.class, params(ThreadContext.class, int.class)));
                }
            }
        }
        
        public void checkWhenWithSplat() {
            loadThreadContext();
            invokeUtilityMethod("isWhenTriggered", sig(RubyBoolean.class, IRubyObject.class, IRubyObject.class, ThreadContext.class));
        }
        
        public void issueRetryEvent() {
            invokeUtilityMethod("retryJump", sig(IRubyObject.class));
        }

        public void defineNewMethod(String name, int methodArity, StaticScope scope, 
                CompilerCallback body, CompilerCallback args, 
                CompilerCallback receiver, ASTInspector inspector, boolean root) {
            // TODO: build arg list based on number of args, optionals, etc
            ++methodIndex;
            String methodName;
            if (root && Boolean.getBoolean("jruby.compile.toplevel")) {
                methodName = name;
            } else {
                String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
                methodName = "method__" + methodIndex + "$RUBY$" + mangledName;
            }

            MethodCompiler methodCompiler = startMethod(methodName, args, scope, inspector);

            // callbacks to fill in method body
            body.call(methodCompiler);

            methodCompiler.endMethod();

            // prepare to call "def" utility method to handle def logic
            loadThreadContext();

            loadSelf();
            
            if (receiver != null) receiver.call(this);
            
            // script object
            method.aload(THIS);

            method.ldc(name);

            method.ldc(methodName);

            buildStaticScopeNames(method, scope);

            method.pushInt(methodArity);
            
            // arities
            method.pushInt(scope.getRequiredArgs());
            method.pushInt(scope.getOptionalArgs());
            method.pushInt(scope.getRestArg());
            
            // if method has frame aware methods or frameless compilation is NOT enabled
            if (inspector.hasFrameAwareMethods() || !(inspector.noFrame() || RubyInstanceConfig.FRAMELESS_COMPILE_ENABLED)) {
                if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                    method.getstatic(p(CallConfiguration.class), CallConfiguration.FRAME_AND_SCOPE.name(), ci(CallConfiguration.class));
                } else {
                    method.getstatic(p(CallConfiguration.class), CallConfiguration.FRAME_ONLY.name(), ci(CallConfiguration.class));
                }
            } else {
                if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                    // TODO: call config with scope but no frame
                    if (RubyInstanceConfig.FASTEST_COMPILE_ENABLED) {
                        method.getstatic(p(CallConfiguration.class), CallConfiguration.SCOPE_ONLY.name(), ci(CallConfiguration.class));
                    } else {
                        method.getstatic(p(CallConfiguration.class), CallConfiguration.BACKTRACE_AND_SCOPE.name(), ci(CallConfiguration.class));
                    }
                } else {
                    if (RubyInstanceConfig.FASTEST_COMPILE_ENABLED || inspector.noFrame()) {
                        method.getstatic(p(CallConfiguration.class), CallConfiguration.NO_FRAME_NO_SCOPE.name(), ci(CallConfiguration.class));
                    } else {
                        method.getstatic(p(CallConfiguration.class), CallConfiguration.BACKTRACE_ONLY.name(), ci(CallConfiguration.class));
                    }
                }
            }
            
            if (receiver != null) {
                invokeUtilityMethod("defs", sig(IRubyObject.class, 
                        params(ThreadContext.class, IRubyObject.class, IRubyObject.class, Object.class, String.class, String.class, String[].class, int.class, int.class, int.class, int.class, CallConfiguration.class)));
            } else {
                invokeUtilityMethod("def", sig(IRubyObject.class, 
                        params(ThreadContext.class, IRubyObject.class, Object.class, String.class, String.class, String[].class, int.class, int.class, int.class, int.class, CallConfiguration.class)));
            }
        }

        public void rethrowIfSystemExit() {
            loadRuntime();
            method.ldc("SystemExit");
            method.invokevirtual(p(Ruby.class), "fastGetClass", sig(RubyClass.class, String.class));
            method.swap();
            method.invokevirtual(p(RubyModule.class), "isInstance", sig(boolean.class, params(IRubyObject.class)));
            method.iconst_0();
            Label ifEnd = new Label();
            method.if_icmpeq(ifEnd);
            loadException();
            method.athrow();
            method.label(ifEnd);
        }
    }

    public class ASMClosureCompiler extends AbstractMethodCompiler {
        public ASMClosureCompiler(String closureMethodName, ASTInspector inspector, StaticScope scope) {
            super(scope, inspector, closureMethodName);
        }
        
        protected String getSignature() {
            return CLOSURE_SIGNATURE;
        }
        
        protected void createVariableCompiler() {
            if (inspector == null) {
                variableCompiler = new HeapBasedVariableCompiler(this, method, scope, false, ARGS_INDEX, getFirstTempIndex());
            } else if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                // enable "boxed" variable compilation when only a closure present
                // this breaks using a proc as a binding
                if (RubyInstanceConfig.BOXED_COMPILE_ENABLED && !inspector.hasScopeAwareMethods()) {
                    variableCompiler = new BoxedVariableCompiler(this, method, scope, false, ARGS_INDEX, getFirstTempIndex());
                } else {
                    variableCompiler = new HeapBasedVariableCompiler(this, method, scope, false, ARGS_INDEX, getFirstTempIndex());
                }
            } else {
                variableCompiler = new StackBasedVariableCompiler(this, method, scope, false, ARGS_INDEX, getFirstTempIndex());
            }
        }

        public void beginMethod(CompilerCallback args, StaticScope scope) {
            method.start();

            // set up a local IRuby variable
            method.aload(THREADCONTEXT_INDEX);
            invokeThreadContext("getRuntime", sig(Ruby.class));
            method.dup();
            method.astore(getRuntimeIndex());
            
            // grab nil for local variables
            invokeIRuby("getNil", sig(IRubyObject.class));
            method.astore(getNilIndex());
            
            variableCompiler.beginClosure(args, scope);

            // start of scoping for closure's vars
            redoJump = new Label();
            method.label(scopeStart);
        }

        public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
            throw new NotCompilableException("ERROR: closure compiler should not be used for class bodies");
        }

        public void endMethod() {
            // end of scoping for closure's vars
            method.areturn();
            method.label(scopeEnd);
            
            // handle redos by restarting the block
            method.pop();
            method.go_to(scopeStart);
            
            method.trycatch(scopeStart, scopeEnd, scopeEnd, p(JumpException.RedoJump.class));
            
            // method is done, declare all variables
            variableCompiler.declareLocals(scope, scopeStart, scopeEnd);
            
            method.end();
        }

        @Override
        public void loadBlock() {
            loadThreadContext();
            invokeThreadContext("getFrameBlock", sig(Block.class));
        }

        public void performReturn() {
            loadThreadContext();
            invokeUtilityMethod("returnJump", sig(JumpException.ReturnJump.class, IRubyObject.class, ThreadContext.class));
            method.athrow();
        }

        public void processRequiredArgs(Arity arity, int requiredArgs, int optArgs, int restArg) {
            throw new NotCompilableException("Shouldn't be calling this...");
        }

        public void assignOptionalArgs(Object object, int expectedArgsCount, int size, ArrayCallback optEval) {
            throw new NotCompilableException("Shouldn't be calling this...");
        }

        public void processRestArg(int startIndex, int restArg) {
            throw new NotCompilableException("Shouldn't be calling this...");
        }

        public void processBlockArgument(int index) {
            loadRuntime();
            loadThreadContext();
            loadBlock();
            method.pushInt(index);
            invokeUtilityMethod("processBlockArgument", sig(void.class, params(Ruby.class, ThreadContext.class, Block.class, int.class)));
        }
        
        public void issueBreakEvent(CompilerCallback value) {
            if (currentLoopLabels != null) {
                value.call(this);
                issueLoopBreak();
            } else {
                loadThreadContext();
                value.call(this);
                invokeUtilityMethod("breakJump", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
            }
        }

        public void issueNextEvent(CompilerCallback value) {
            if (currentLoopLabels != null) {
                value.call(this);
                issueLoopNext();
            } else {
                value.call(this);
                invokeUtilityMethod("nextJump", sig(IRubyObject.class, IRubyObject.class));
            }
        }

        public void issueRedoEvent() {
            // FIXME: This isn't right for within ensured/rescued code
            if (currentLoopLabels != null) {
                issueLoopRedo();
            } else if (withinProtection) {
                invokeUtilityMethod("redoJump", sig(IRubyObject.class));
            } else {
                // jump back to the top of the main body of this closure
                method.go_to(scopeStart);
            }
        }
    }

    public class ASMMethodCompiler extends AbstractMethodCompiler {
        private boolean specificArity;

        public ASMMethodCompiler(String friendlyName, ASTInspector inspector, StaticScope scope) {
            super(scope, inspector, friendlyName);
        }
        
        protected String getSignature() {
            if (scope.getRestArg() >= 0 || scope.getOptionalArgs() > 0 || scope.getRequiredArgs() > 3) {
                specificArity = false;
                return METHOD_SIGNATURES[4];
            } else {
                specificArity = true;
                return METHOD_SIGNATURES[scope.getRequiredArgs()];
            }
        }
        
        protected void createVariableCompiler() {
            if (inspector == null) {
                variableCompiler = new HeapBasedVariableCompiler(this, method, scope, specificArity, ARGS_INDEX, getFirstTempIndex());
            } else if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                // enable "boxed" variable compilation when only a closure present
                // this breaks using a proc as a binding
                if (RubyInstanceConfig.BOXED_COMPILE_ENABLED && !inspector.hasScopeAwareMethods()) {
                    variableCompiler = new BoxedVariableCompiler(this, method, scope, specificArity, ARGS_INDEX, getFirstTempIndex());
                } else {
                    variableCompiler = new HeapBasedVariableCompiler(this, method, scope, specificArity, ARGS_INDEX, getFirstTempIndex());
                }
            } else {
                variableCompiler = new StackBasedVariableCompiler(this, method, scope, specificArity, ARGS_INDEX, getFirstTempIndex());
            }
        }
        
        public void beginChainedMethod() {
            method.start();
            
            method.aload(THREADCONTEXT_INDEX);
            method.dup();
            method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
            method.dup();
            method.astore(getRuntimeIndex());

            // grab nil for local variables
            method.invokevirtual(p(Ruby.class), "getNil", sig(IRubyObject.class));
            method.astore(getNilIndex());

            method.invokevirtual(p(ThreadContext.class), "getCurrentScope", sig(DynamicScope.class));
            method.dup();
            method.astore(getDynamicScopeIndex());
            method.invokevirtual(p(DynamicScope.class), "getValues", sig(IRubyObject[].class));
            method.astore(getVarsArrayIndex());

            // visit a label to start scoping for local vars in this method
            method.label(scopeStart);
        }

        public void beginMethod(CompilerCallback args, StaticScope scope) {
            method.start();

            // set up a local IRuby variable
            method.aload(THREADCONTEXT_INDEX);
            invokeThreadContext("getRuntime", sig(Ruby.class));
            method.dup();
            method.astore(getRuntimeIndex());
            
            
            // grab nil for local variables
            invokeIRuby("getNil", sig(IRubyObject.class));
            method.astore(getNilIndex());
            
            variableCompiler.beginMethod(args, scope);

            // visit a label to start scoping for local vars in this method
            method.label(scopeStart);
        }

        public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
            method.start();

            // set up a local IRuby variable
            method.aload(THREADCONTEXT_INDEX);
            invokeThreadContext("getRuntime", sig(Ruby.class));
            method.dup();
            method.astore(getRuntimeIndex());
            
            // grab nil for local variables
            invokeIRuby("getNil", sig(IRubyObject.class));
            method.astore(getNilIndex());
            
            variableCompiler.beginClass(bodyPrep, scope);

            // visit a label to start scoping for local vars in this method
            method.label(scopeStart);
        }

        public void endMethod() {
            // return last value from execution
            method.areturn();

            // end of variable scope
            method.label(scopeEnd);
            
            // method is done, declare all variables
            variableCompiler.declareLocals(scope, scopeStart, scopeEnd);

            method.end();
            
            if (specificArity) {// add a default [] version of the method that calls the specific version
                method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC, methodName, METHOD_SIGNATURES[4], null, null));
                method.start();
                        
                // check arity in the variable-arity version
                method.aload(1);
                method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                method.aload(3);
                method.pushInt(scope.getRequiredArgs());
                method.pushInt(scope.getRequiredArgs());
                method.invokestatic(p(Arity.class), "checkArgumentCount", sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
                method.pop();
                
                loadThis();
                loadThreadContext();
                loadSelf();
                // FIXME: missing arity check
                for (int i = 0; i < scope.getRequiredArgs(); i++) {
                    method.aload(ARGS_INDEX);
                    method.ldc(i);
                    method.arrayload();
                }
                method.aload(ARGS_INDEX + 1); // load block from [] version of method
                
                method.invokevirtual(classname, methodName, getSignature());
                method.areturn();
                method.end();
            }
        }
        
        public void performReturn() {
            // normal return for method body. return jump for within a begin/rescue/ensure
            if (withinProtection) {
                loadThreadContext();
                invokeUtilityMethod("returnJump", sig(JumpException.ReturnJump.class, IRubyObject.class, ThreadContext.class));
                method.athrow();
            } else {
                method.areturn();
            }
        }

        public void issueBreakEvent(CompilerCallback value) {
            if (currentLoopLabels != null) {
                value.call(this);
                issueLoopBreak();
            } else if (withinProtection) {
                loadThreadContext();
                value.call(this);
                invokeUtilityMethod("breakJump", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
            } else {
                // in method body with no containing loop, issue jump error
                // load runtime and value, issue jump error
                loadRuntime();
                value.call(this);
                invokeUtilityMethod("breakLocalJumpError", sig(IRubyObject.class, Ruby.class, IRubyObject.class));
            }
        }

        public void issueNextEvent(CompilerCallback value) {
            if (currentLoopLabels != null) {
                value.call(this);
                issueLoopNext();
            } else if (withinProtection) {
                value.call(this);
                invokeUtilityMethod("nextJump", sig(IRubyObject.class, IRubyObject.class));
            } else {
                // in method body with no containing loop, issue jump error
                // load runtime and value, issue jump error
                loadRuntime();
                value.call(this);
                invokeUtilityMethod("nextLocalJumpError", sig(IRubyObject.class, Ruby.class, IRubyObject.class));
            }
        }

        public void issueRedoEvent() {
            if (currentLoopLabels != null) {
                issueLoopRedo();
            } else if (withinProtection) {
                invokeUtilityMethod("redoJump", sig(IRubyObject.class));
            } else {
                // in method body with no containing loop, issue jump error
                // load runtime and value, issue jump error
                loadRuntime();
                invokeUtilityMethod("redoLocalJumpError", sig(IRubyObject.class, Ruby.class));
            }
        }
    }

    private int constants = 0;

    public String getNewConstant(String type, String name_prefix) {
        return getNewConstant(type, name_prefix, null);
    }

    public String getNewConstant(String type, String name_prefix, Object init) {
        ClassVisitor cv = getClassVisitor();

        String realName;
        synchronized (this) {
            realName = "_" + constants++;
        }

        // declare the field
        cv.visitField(ACC_PRIVATE, realName, type, null, null).visitEnd();

        if(init != null) {
            initMethod.aload(THIS);
            initMethod.ldc(init);
            initMethod.putfield(classname, realName, type);
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
            initMethod.putfield(classname, name, type);
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
