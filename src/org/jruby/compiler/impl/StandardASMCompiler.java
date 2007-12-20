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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
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
import org.jruby.ast.executable.Script;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.BranchCallback;
import org.jruby.compiler.CompilerCallback;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.compiler.MethodCompiler;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ScriptCompiler;
import org.jruby.compiler.VariableCompiler;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.ReOptions;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 *
 * @author headius
 */
public class StandardASMCompiler implements ScriptCompiler, Opcodes {
    private static final CodegenUtils cg = CodegenUtils.cg;
    
    private static final String THREADCONTEXT = cg.p(ThreadContext.class);
    private static final String RUBY = cg.p(Ruby.class);
    private static final String IRUBYOBJECT = cg.p(IRubyObject.class);

    private static final String METHOD_SIGNATURE = cg.sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class});
    private static final String CLOSURE_SIGNATURE = cg.sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class});

    private static final int THIS = 0;
    private static final int THREADCONTEXT_INDEX = 1;
    private static final int SELF_INDEX = 2;
    private static final int ARGS_INDEX = 3;
    private static final int CLOSURE_INDEX = 4;
    private static final int DYNAMIC_SCOPE_INDEX = 5;
    private static final int RUNTIME_INDEX = 6;
    private static final int VARS_ARRAY_INDEX = 7;
    private static final int NIL_INDEX = 8;
    private static final int EXCEPTION_INDEX = 9;
    private static final int PREVIOUS_EXCEPTION_INDEX = 10;
    
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
    
    Map<String, String> sourcePositions = new HashMap<String, String>();
    Map<String, String> byteLists = new HashMap<String, String>();
    Map<String, String> symbols = new HashMap<String, String>();
    
    /** Creates a new instance of StandardCompilerContext */
    public StandardASMCompiler(String classname, String sourcename) {
        this.classname = classname;
        this.sourcename = sourcename;
    }

    public byte[] getClassByteArray() {
        return classWriter.toByteArray();
    }

    public Class<?> loadClass(JRubyClassLoader classLoader) throws ClassNotFoundException {
        classLoader.defineClass(cg.c(classname), classWriter.toByteArray());
        return classLoader.loadClass(cg.c(classname));
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
        classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        // Create the class with the appropriate class name and source file
        classWriter.visit(V1_4, ACC_PUBLIC + ACC_SUPER, classname, null, cg.p(Object.class), new String[]{cg.p(Script.class)});
        classWriter.visitSource(sourcename, null);
        
        topLevelScope = scope;

        beginInit();
        beginClassInit();
    }

    public void endScript() {
        // add Script#run impl, used for running this script with a specified threadcontext and self
        // root method of a script is always in __file__ method
        String methodName = "__file__";
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC, "run", METHOD_SIGNATURE, null, null));
        method.start();

        // invoke __file__ with threadcontext, self, args (null), and block (null)
        method.aload(THIS);
        method.aload(THREADCONTEXT_INDEX);
        method.aload(SELF_INDEX);
        method.aload(ARGS_INDEX);
        method.aload(CLOSURE_INDEX);

        method.invokevirtual(classname, methodName, METHOD_SIGNATURE);
        method.areturn();
        
        method.end();
        
        // the load method is used for loading as a top-level script, and prepares appropriate scoping around the code
        method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC, "load", METHOD_SIGNATURE, null, null));
        method.start();

        // invoke __file__ with threadcontext, self, args (null), and block (null)
        Label tryBegin = new Label();
        Label tryFinally = new Label();
        
        method.label(tryBegin);
        method.aload(THREADCONTEXT_INDEX);
        buildStaticScopeNames(method, topLevelScope);
        method.invokestatic(cg.p(RuntimeHelpers.class), "preLoad", cg.sig(void.class, ThreadContext.class, String[].class));
        
        method.aload(THIS);
        method.aload(THREADCONTEXT_INDEX);
        method.aload(SELF_INDEX);
        method.aload(ARGS_INDEX);
        method.aload(CLOSURE_INDEX);

        method.invokevirtual(classname, methodName, METHOD_SIGNATURE);
        method.aload(THREADCONTEXT_INDEX);
        method.invokestatic(cg.p(RuntimeHelpers.class), "postLoad", cg.sig(void.class, ThreadContext.class));
        method.areturn();
        
        method.label(tryFinally);
        method.aload(THREADCONTEXT_INDEX);
        method.invokestatic(cg.p(RuntimeHelpers.class), "postLoad", cg.sig(void.class, ThreadContext.class));
        method.athrow();
        
        method.trycatch(tryBegin, tryFinally, tryFinally, null);
        
        method.end();

        // add main impl, used for detached or command-line execution of this script with a new runtime
        // root method of a script is always in stub0, method0
        method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_STATIC, "main", cg.sig(Void.TYPE, cg.params(String[].class)), null, null));
        method.start();

        // new instance to invoke run against
        method.newobj(classname);
        method.dup();
        method.invokespecial(classname, "<init>", cg.sig(Void.TYPE));
        
        // instance config for the script run
        method.newobj(cg.p(RubyInstanceConfig.class));
        method.dup();
        method.invokespecial(cg.p(RubyInstanceConfig.class), "<init>", "()V");
        
        // set argv from main's args
        method.dup();
        method.aload(0);
        method.invokevirtual(cg.p(RubyInstanceConfig.class), "setArgv", cg.sig(void.class, String[].class));

        // invoke run with threadcontext and topself
        method.invokestatic(cg.p(Ruby.class), "newInstance", cg.sig(Ruby.class, RubyInstanceConfig.class));
        method.dup();

        method.invokevirtual(RUBY, "getCurrentContext", cg.sig(ThreadContext.class));
        method.swap();
        method.invokevirtual(RUBY, "getTopSelf", cg.sig(IRubyObject.class));
        method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
        method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));

        method.invokevirtual(classname, "load", METHOD_SIGNATURE);
        method.voidreturn();
        method.end();
        
        endInit();
        endClassInit();
    }

    public void buildStaticScopeNames(SkinnyMethodAdapter method, StaticScope scope) {
        // construct static scope list of names
        method.ldc(new Integer(scope.getNumberOfVariables()));
        method.anewarray(cg.p(String.class));
        for (int i = 0; i < scope.getNumberOfVariables(); i++) {
            method.dup();
            method.ldc(new Integer(i));
            method.ldc(scope.getVariables()[i]);
            method.arraystore();
        }
    }

    private void beginInit() {
        ClassVisitor cv = getClassVisitor();

        initMethod = new SkinnyMethodAdapter(cv.visitMethod(ACC_PUBLIC, "<init>", cg.sig(Void.TYPE), null, null));
        initMethod.start();
        initMethod.aload(THIS);
        initMethod.invokespecial(cg.p(Object.class), "<init>", cg.sig(Void.TYPE));
        
        cv.visitField(ACC_PRIVATE | ACC_FINAL, "$class", cg.ci(Class.class), null, null);
        
        // FIXME: this really ought to be in clinit, but it doesn't matter much
        initMethod.aload(THIS);
        initMethod.ldc(cg.c(classname));
        initMethod.invokestatic(cg.p(Class.class), "forName", cg.sig(Class.class, cg.params(String.class)));
        initMethod.putfield(classname, "$class", cg.ci(Class.class));
    }

    private void endInit() {
        initMethod.voidreturn();
        initMethod.end();
    }

    private void beginClassInit() {
        ClassVisitor cv = getClassVisitor();

        clinitMethod = new SkinnyMethodAdapter(cv.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", cg.sig(Void.TYPE), null, null));
        clinitMethod.start();
    }

    private void endClassInit() {
        clinitMethod.voidreturn();
        clinitMethod.end();
    }
    
    public MethodCompiler startMethod(String friendlyName, CompilerCallback args, StaticScope scope, ASTInspector inspector) {
        ASMMethodCompiler methodCompiler = new ASMMethodCompiler(friendlyName, inspector);
        
        methodCompiler.beginMethod(args, scope);
        
        return methodCompiler;
    }

    public abstract class AbstractMethodCompiler implements MethodCompiler {
        protected SkinnyMethodAdapter method;
        protected VariableCompiler variableCompiler;
        protected InvocationCompiler invocationCompiler;
        
        protected Label[] currentLoopLabels;
        protected Label scopeStart;
        protected Label scopeEnd;
        protected Label redoJump;
        protected boolean withinProtection = false;
        
        // The current local variable count, to use for temporary locals during processing
        protected int localVariable = PREVIOUS_EXCEPTION_INDEX + 1;

        public abstract void beginMethod(CompilerCallback args, StaticScope scope);

        public abstract void endMethod();
        
        public MethodCompiler chainToMethod(String methodName, ASTInspector inspector) {
            // chain to the next segment of this giant method
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            method.aload(ARGS_INDEX);
            if(this instanceof ASMClosureCompiler) {
                pushNull();
            } else {
                loadBlock();
            }
            method.invokevirtual(classname, methodName, cg.sig(IRubyObject.class, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class}));
            endMethod();

            ASMMethodCompiler methodCompiler = new ASMMethodCompiler(methodName, inspector);

            methodCompiler.beginChainedMethod();

            return methodCompiler;
        }
        
        public StandardASMCompiler getScriptCompiler() {
            return StandardASMCompiler.this;
        }

        public void lineNumber(ISourcePosition position) {
            Label line = new Label();
            method.label(line);
            method.visitLineNumber(position.getStartLine() + 1, line);
        }

        public void loadThreadContext() {
            method.aload(THREADCONTEXT_INDEX);
        }

        public void loadSelf() {
            method.aload(SELF_INDEX);
        }

        public void loadRuntime() {
            method.aload(RUNTIME_INDEX);
        }

        public void loadBlock() {
            method.aload(CLOSURE_INDEX);
        }

        public void loadNil() {
            method.aload(NIL_INDEX);
        }
        
        public void loadNull() {
            method.aconst_null();
        }

        public void loadSymbol(String symbol) {
            loadRuntime();

            method.ldc(symbol);

            invokeIRuby("newSymbol", cg.sig(RubySymbol.class, cg.params(String.class)));
        }

        public void loadObject() {
            loadRuntime();

            invokeIRuby("getObject", cg.sig(RubyClass.class, cg.params()));
        }

        /**
         * This is for utility methods used by the compiler, to reduce the amount of code generation
         * necessary.  All of these live in CompilerHelpers.
         */
        public void invokeUtilityMethod(String methodName, String signature) {
            method.invokestatic(cg.p(RuntimeHelpers.class), methodName, signature);
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
            invokeThreadContext("setConstantInCurrent", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void assignConstantInModule(String name) {
            method.ldc(name);
            loadThreadContext();
            invokeUtilityMethod("setConstantInModule", cg.sig(IRubyObject.class, IRubyObject.class, IRubyObject.class, String.class, ThreadContext.class));
        }

        public void assignConstantInObject(String name) {
            // load Object under value
            loadRuntime();
            invokeIRuby("getObject", cg.sig(RubyClass.class, cg.params()));
            method.swap();

            assignConstantInModule(name);
        }

        public void retrieveConstant(String name) {
            loadThreadContext();
            method.ldc(name);
            invokeThreadContext("getConstant", cg.sig(IRubyObject.class, cg.params(String.class)));
        }

        public void retrieveConstantFromModule(String name) {
            method.visitTypeInsn(CHECKCAST, cg.p(RubyModule.class));
            method.ldc(name);
            method.invokevirtual(cg.p(RubyModule.class), "fastGetConstantFrom", cg.sig(IRubyObject.class, cg.params(String.class)));
        }

        public void retrieveClassVariable(String name) {
            loadThreadContext();
            loadRuntime();
            loadSelf();
            method.ldc(name);

            invokeUtilityMethod("fastFetchClassVariable", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class)));
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

            invokeUtilityMethod("fastSetClassVariable", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
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

            invokeUtilityMethod("fastDeclareClassVariable", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
        }

        public void createNewFloat(double value) {
            loadRuntime();
            method.ldc(new Double(value));

            invokeIRuby("newFloat", cg.sig(RubyFloat.class, cg.params(Double.TYPE)));
        }

        public void createNewFixnum(long value) {
            loadRuntime();
            method.ldc(new Long(value));

            invokeIRuby("newFixnum", cg.sig(RubyFixnum.class, cg.params(Long.TYPE)));
        }

        public void createNewBignum(BigInteger value) {
            loadRuntime();
            method.ldc(value.toString());

            method.invokestatic(cg.p(RubyBignum.class), "newBignum", cg.sig(RubyBignum.class, cg.params(Ruby.class, String.class)));
        }

        public void createNewString(ArrayCallback callback, int count) {
            loadRuntime();
            invokeIRuby("newString", cg.sig(RubyString.class, cg.params()));
            for (int i = 0; i < count; i++) {
                callback.nextValue(this, null, i);
                method.invokevirtual(cg.p(RubyString.class), "append", cg.sig(RubyString.class, cg.params(IRubyObject.class)));
            }
        }

        public void createNewSymbol(ArrayCallback callback, int count) {
            loadRuntime();
            invokeIRuby("newString", cg.sig(RubyString.class, cg.params()));
            for (int i = 0; i < count; i++) {
                callback.nextValue(this, null, i);
                method.invokevirtual(cg.p(RubyString.class), "append", cg.sig(RubyString.class, cg.params(IRubyObject.class)));
            }
            toJavaString();
            loadRuntime();
            method.swap();
            invokeIRuby("newSymbol", cg.sig(RubySymbol.class, cg.params(String.class)));
        }

        public void createNewString(ByteList value) {
            // FIXME: this is sub-optimal, storing string value in a java.lang.String again
            String fieldName = cacheByteList(value.toString());
            loadRuntime();
            method.getstatic(classname, fieldName, cg.ci(ByteList.class));

            invokeIRuby("newStringShared", cg.sig(RubyString.class, cg.params(ByteList.class)));
        }

        public void createNewSymbol(String name) {
            method.aload(0);
            loadRuntime();
            String methodName = cacheSymbol(name);
            method.invokevirtual(classname, methodName, 
                    cg.sig(RubySymbol.class, cg.params(Ruby.class)));
        }

        public void createNewArray(boolean lightweight) {
            loadRuntime();
            // put under object array already present
            method.swap();

            if (lightweight) {
                invokeIRuby("newArrayNoCopyLight", cg.sig(RubyArray.class, cg.params(IRubyObject[].class)));
            } else {
                invokeIRuby("newArrayNoCopy", cg.sig(RubyArray.class, cg.params(IRubyObject[].class)));
            }
        }

        public void createEmptyArray() {
            loadRuntime();

            invokeIRuby("newArray", cg.sig(RubyArray.class, cg.params()));
        }

        public void createObjectArray(Object[] sourceArray, ArrayCallback callback) {
            buildObjectArray(IRUBYOBJECT, sourceArray, callback);
        }

        public void createObjectArray(int elementCount) {
            // if element count is less than 6, use helper methods
            if (elementCount < 6) {
                Class[] params = new Class[elementCount];
                Arrays.fill(params, IRubyObject.class);
                invokeUtilityMethod("constructObjectArray", cg.sig(IRubyObject[].class, params));
            } else {
                // This is pretty inefficient for building an array, so just raise an error if someone's using it for a lot of elements
                throw new NotCompilableException("Don't use createObjectArray(int) for more than 5 elements");
            }
        }

        private void buildObjectArray(String type, Object[] sourceArray, ArrayCallback callback) {
            if (sourceArray.length == 0) {
                method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
            } else if (sourceArray.length <= RuntimeHelpers.MAX_SPECIFIC_ARITY_OBJECT_ARRAY) {
                // if we have a specific-arity helper to construct an array for us, use that
                for (int i = 0; i < sourceArray.length; i++) {
                    callback.nextValue(this, sourceArray, i);
                }
                invokeUtilityMethod("constructObjectArray", cg.sig(IRubyObject[].class, cg.params(IRubyObject.class, sourceArray.length)));
            } else {
                // brute force construction inline
                method.ldc(new Integer(sourceArray.length));
                method.anewarray(type);

                for (int i = 0; i < sourceArray.length; i++) {
                    method.dup();
                    method.ldc(new Integer(i));

                    callback.nextValue(this, sourceArray, i);

                    method.arraystore();
                }
            }
        }

        public void createEmptyHash() {
            loadRuntime();

            method.invokestatic(cg.p(RubyHash.class), "newHash", cg.sig(RubyHash.class, cg.params(Ruby.class)));
        }

        public void createNewHash(Object elements, ArrayCallback callback, int keyCount) {
            loadRuntime();
            
            if (keyCount <= RuntimeHelpers.MAX_SPECIFIC_ARITY_HASH) {
                // we have a specific-arity method we can use to construct, so use that
                for (int i = 0; i < keyCount; i++) {
                    callback.nextValue(this, elements, i);
                }
                
                invokeUtilityMethod("constructHash", cg.sig(RubyHash.class, cg.params(Ruby.class, IRubyObject.class, keyCount * 2)));
            } else {
                method.invokestatic(cg.p(RubyHash.class), "newHash", cg.sig(RubyHash.class, cg.params(Ruby.class)));

                for (int i = 0; i < keyCount; i++) {
                    method.dup();
                    callback.nextValue(this, elements, i);
                    method.invokevirtual(cg.p(RubyHash.class), "fastASet", cg.sig(void.class, cg.params(IRubyObject.class, IRubyObject.class)));
                }
            }
        }

        public void createNewRange(boolean isExclusive) {
            loadRuntime();

            // could be more efficient with a callback
            method.dup_x2();
            method.pop();

            method.ldc(new Boolean(isExclusive));

            method.invokestatic(cg.p(RubyRange.class), "newRange", cg.sig(RubyRange.class, cg.params(Ruby.class, IRubyObject.class, IRubyObject.class, Boolean.TYPE)));
        }

        /**
         * Invoke IRubyObject.isTrue
         */
        private void isTrue() {
            invokeIRubyObject("isTrue", cg.sig(Boolean.TYPE));
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
            method.trycatch(tryBegin, tryEnd, catchRedo, cg.p(JumpException.RedoJump.class));
            method.trycatch(tryBegin, tryEnd, catchNext, cg.p(JumpException.NextJump.class));
            method.trycatch(tryBegin, tryEnd, catchBreak, cg.p(JumpException.BreakJump.class));
            if (checkFirst) {
                // only while loops seem to have this RaiseException magic
                method.trycatch(tryBegin, tryEnd, catchRaised, cg.p(RaiseException.class));
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
                    invokeUtilityMethod("breakJumpInWhile", cg.sig(IRubyObject.class, JumpException.BreakJump.class, Block.class));
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
                    invokeUtilityMethod("getLocalJumpTypeOrRethrow", cg.sig(String.class, cg.params(RaiseException.class)));
                    // if we get here we have a RaiseException we know is a local jump error and an error type

                    // is it break?
                    method.dup(); // dup string
                    method.ldc("break");
                    method.invokevirtual(cg.p(String.class), "equals", cg.sig(boolean.class, cg.params(Object.class)));
                    method.ifeq(raiseNext);
                    // pop the extra string, get the break value, and end the loop
                    method.pop();
                    invokeUtilityMethod("unwrapLocalJumpErrorValue", cg.sig(IRubyObject.class, cg.params(RaiseException.class)));
                    method.go_to(done);

                    // is it next?
                    method.label(raiseNext);
                    method.dup();
                    method.ldc("next");
                    method.invokevirtual(cg.p(String.class), "equals", cg.sig(boolean.class, cg.params(Object.class)));
                    method.ifeq(raiseRedo);
                    // pop the extra string and the exception, jump to the condition
                    method.pop2();
                    method.go_to(conditionCheck);

                    // is it redo?
                    method.label(raiseRedo);
                    method.dup();
                    method.ldc("redo");
                    method.invokevirtual(cg.p(String.class), "equals", cg.sig(boolean.class, cg.params(Object.class)));
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

        public void createNewClosure(
                StaticScope scope,
                int arity,
                CompilerCallback body,
                CompilerCallback args,
                boolean hasMultipleArgsHead,
                NodeType argsNodeId,
                ASTInspector inspector) {
            String closureMethodName = "closure" + ++innerIndex;
            String closureFieldName = "_" + closureMethodName;
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, closureFieldName, inspector);
            
            closureCompiler.beginMethod(args, scope);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            /////////////////////////////////////////////////////////////////////////////
            // Now, store a compiled block object somewhere we can access it in the future
            // in current method, load the field to see if we've created a BlockCallback yet
            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated);

            // no callback, construct and cache it
            method.aload(THIS);
            getCallbackFactory();

            method.ldc(closureMethodName);
            method.aload(THIS);
            method.invokevirtual(cg.p(CallbackFactory.class), "getBlockCallback", cg.sig(CompiledBlockCallback.class, cg.params(String.class, Object.class)));
            method.putfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));

            method.label(alreadyCreated);

            // Construct the block for passing to the target method
            loadThreadContext();
            loadSelf();
            method.ldc(new Integer(arity));

            buildStaticScopeNames(method, scope);

            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            method.ldc(Boolean.valueOf(hasMultipleArgsHead));
            method.ldc(Block.asArgumentType(argsNodeId));
            // if there's a sub-closure or there's scope-aware methods, it can't be "light"
            method.ldc(!(inspector.hasClosure() || inspector.hasScopeAwareMethods()));

            invokeUtilityMethod("createBlock", cg.sig(Block.class,
                    cg.params(ThreadContext.class, IRubyObject.class, Integer.TYPE, String[].class, CompiledBlockCallback.class, Boolean.TYPE, Integer.TYPE, boolean.class)));
        }

        public void runBeginBlock(StaticScope scope, CompilerCallback body) {
            String closureMethodName = "closure" + ++innerIndex;
            String closureFieldName = "_" + closureMethodName;
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, closureFieldName, null);
            
            closureCompiler.beginMethod(null, scope);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            /////////////////////////////////////////////////////////////////////////////
            // Now, store a compiled block object somewhere we can access it in the future
            // in current method, load the field to see if we've created a BlockCallback yet
            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated);

            // no callback, construct and cache it
            method.aload(THIS);
            getCallbackFactory();

            method.ldc(closureMethodName);
            method.aload(THIS);
            method.invokevirtual(cg.p(CallbackFactory.class), "getBlockCallback", cg.sig(CompiledBlockCallback.class, cg.params(String.class, Object.class)));
            method.putfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));

            method.label(alreadyCreated);

            // Construct the block for passing to the target method
            loadThreadContext();
            loadSelf();

            buildStaticScopeNames(method, scope);

            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));

            invokeUtilityMethod("runBeginBlock", cg.sig(IRubyObject.class,
                    cg.params(ThreadContext.class, IRubyObject.class, String[].class, CompiledBlockCallback.class)));
        }

        public void createNewForLoop(int arity, CompilerCallback body, CompilerCallback args, boolean hasMultipleArgsHead, NodeType argsNodeId) {
            String closureMethodName = "closure" + ++innerIndex;
            String closureFieldName = "_" + closureMethodName;
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, closureFieldName, null);
            
            closureCompiler.beginMethod(args, null);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            /////////////////////////////////////////////////////////////////////////////
            // Now, store a compiled block object somewhere we can access it in the future
            // in current method, load the field to see if we've created a BlockCallback yet
            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated);

            // no callback, construct and cache it
            method.aload(THIS);
            getCallbackFactory();

            method.ldc(closureMethodName);
            method.aload(THIS);
            method.invokevirtual(cg.p(CallbackFactory.class), "getBlockCallback", cg.sig(CompiledBlockCallback.class, cg.params(String.class, Object.class)));
            method.putfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));

            method.label(alreadyCreated);

            // Construct the block for passing to the target method
            loadThreadContext();
            loadSelf();
            method.ldc(new Integer(arity));

            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            method.ldc(Boolean.valueOf(hasMultipleArgsHead));
            method.ldc(Block.asArgumentType(argsNodeId));

            invokeUtilityMethod("createSharedScopeBlock", cg.sig(Block.class,
                    cg.params(ThreadContext.class, IRubyObject.class, Integer.TYPE, CompiledBlockCallback.class, Boolean.TYPE, Integer.TYPE)));
        }

        public void createNewEndBlock(CompilerCallback body) {
            String closureMethodName = "END_closure" + ++innerIndex;
            String closureFieldName = "_" + closureMethodName;
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, closureFieldName, null);
            
            closureCompiler.beginMethod(null, null);
            
            body.call(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            /////////////////////////////////////////////////////////////////////////////
            // Now, store a compiled block object somewhere we can access it in the future
            // in current method, load the field to see if we've created a BlockCallback yet
            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated);

            // no callback, construct and cache it
            method.aload(THIS);
            getCallbackFactory();

            method.ldc(closureMethodName);
            method.aload(THIS);
            method.invokevirtual(cg.p(CallbackFactory.class), "getBlockCallback", cg.sig(CompiledBlockCallback.class, cg.params(String.class, Object.class)));
            method.putfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));

            method.label(alreadyCreated);

            // Construct the block for passing to the target method
            loadThreadContext();
            loadSelf();
            method.ldc(new Integer(0));

            method.aload(THIS);
            method.getfield(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            method.ldc(false);
            method.ldc(Block.ZERO_ARGS);

            invokeUtilityMethod("createSharedScopeBlock", cg.sig(Block.class,
                    cg.params(ThreadContext.class, IRubyObject.class, Integer.TYPE, CompiledBlockCallback.class, Boolean.TYPE, Integer.TYPE)));
            
            loadRuntime();
            invokeUtilityMethod("registerEndBlock", cg.sig(void.class, Block.class, Ruby.class));
            loadNil();
        }

        private void getCallbackFactory() {
            // FIXME: Perhaps a bit extra code, but only for defn/s; examine
            loadRuntime();
            getCompiledClass();
            method.dup();
            method.invokevirtual(cg.p(Class.class), "getClassLoader", cg.sig(ClassLoader.class));
            method.invokestatic(cg.p(CallbackFactory.class), "createFactory", cg.sig(CallbackFactory.class, cg.params(Ruby.class, Class.class, ClassLoader.class)));
        }

        public void getCompiledClass() {
            method.aload(THIS);
            method.getfield(classname, "$class", cg.ci(Class.class));
        }

        private void getRubyClass() {
            loadThreadContext();
            invokeThreadContext("getRubyClass", cg.sig(RubyModule.class));
        }

        public void println() {
            method.dup();
            method.getstatic(cg.p(System.class), "out", cg.ci(PrintStream.class));
            method.swap();

            method.invokevirtual(cg.p(PrintStream.class), "println", cg.sig(Void.TYPE, cg.params(Object.class)));
        }

        public void defineAlias(String newName, String oldName) {
            loadThreadContext();
            method.ldc(newName);
            method.ldc(oldName);
            invokeUtilityMethod("defineAlias", cg.sig(IRubyObject.class, ThreadContext.class, String.class, String.class));
        }

        public void loadFalse() {
            // TODO: cache?
            loadRuntime();
            invokeIRuby("getFalse", cg.sig(RubyBoolean.class));
        }

        public void loadTrue() {
            // TODO: cache?
            loadRuntime();
            invokeIRuby("getTrue", cg.sig(RubyBoolean.class));
        }

        public void loadCurrentModule() {
            loadThreadContext();
            invokeThreadContext("getCurrentScope", cg.sig(DynamicScope.class));
            method.invokevirtual(cg.p(DynamicScope.class), "getStaticScope", cg.sig(StaticScope.class));
            method.invokevirtual(cg.p(StaticScope.class), "getModule", cg.sig(RubyModule.class));
        }

        public void retrieveInstanceVariable(String name) {
            loadRuntime();
            loadSelf();
            method.ldc(name);
            invokeUtilityMethod("fastGetInstanceVariable", cg.sig(IRubyObject.class, Ruby.class, IRubyObject.class, String.class));
        }

        public void assignInstanceVariable(String name) {
            // FIXME: more efficient with a callback
            loadSelf();
            invokeIRubyObject("getInstanceVariables", cg.sig(InstanceVariables.class));
            method.swap();

            method.ldc(name);
            method.swap();

            method.invokeinterface(cg.p(InstanceVariables.class), "fastSetInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void retrieveGlobalVariable(String name) {
            loadRuntime();

            invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
            method.ldc(name);
            method.invokevirtual(cg.p(GlobalVariables.class), "get", cg.sig(IRubyObject.class, cg.params(String.class)));
        }

        public void assignGlobalVariable(String name) {
            // FIXME: more efficient with a callback
            loadRuntime();

            invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
            method.swap();
            method.ldc(name);
            method.swap();
            method.invokevirtual(cg.p(GlobalVariables.class), "set", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void negateCurrentValue() {
            loadRuntime();
            invokeUtilityMethod("negate", cg.sig(IRubyObject.class, IRubyObject.class, Ruby.class));
        }

        public void splatCurrentValue() {
            loadRuntime();
            method.invokestatic(cg.p(ASTInterpreter.class), "splatValue", cg.sig(RubyArray.class, cg.params(IRubyObject.class, Ruby.class)));
        }

        public void singlifySplattedValue() {
            loadRuntime();
            method.invokestatic(cg.p(ASTInterpreter.class), "aValueSplat", cg.sig(IRubyObject.class, cg.params(IRubyObject.class, Ruby.class)));
        }

        public void aryToAry() {
            loadRuntime();
            method.invokestatic(cg.p(ASTInterpreter.class), "aryToAry", cg.sig(IRubyObject.class, cg.params(IRubyObject.class, Ruby.class)));
        }

        public void ensureRubyArray() {
            invokeUtilityMethod("ensureRubyArray", cg.sig(RubyArray.class, cg.params(IRubyObject.class)));
        }

        public void ensureMultipleAssignableRubyArray(boolean masgnHasHead) {
            loadRuntime();
            method.swap();
            method.ldc(new Boolean(masgnHasHead));
            invokeUtilityMethod("ensureMultipleAssignableRubyArray", cg.sig(RubyArray.class, cg.params(Ruby.class, IRubyObject.class, boolean.class)));
        }

        public void forEachInValueArray(int start, int count, Object source, ArrayCallback callback, ArrayCallback nilCallback, CompilerCallback argsCallback) {
            // FIXME: This could probably be made more efficient
            for (; start < count; start++) {
                Label noMoreArrayElements = new Label();
                Label doneWithElement = new Label();
                
                // confirm we're not past the end of the array
                method.dup(); // dup the original array object
                method.invokevirtual(cg.p(RubyArray.class), "getLength", cg.sig(Integer.TYPE));
                method.ldc(new Integer(start));
                method.if_icmple(noMoreArrayElements); // if length <= start, end loop
                
                // extract item from array
                method.dup(); // dup the original array object
                method.ldc(new Integer(start)); // index for the item
                method.invokevirtual(cg.p(RubyArray.class), "entry", cg.sig(IRubyObject.class, cg.params(Integer.TYPE))); // extract item
                callback.nextValue(this, source, start);
                method.go_to(doneWithElement);
                
                // otherwise no items left available, use the code from nilCallback
                method.label(noMoreArrayElements);
                nilCallback.nextValue(this, source, start);
                
                // end of this element
                method.label(doneWithElement);
                // normal assignment leaves the value; pop it.
                method.pop();
            }
            
            if (argsCallback != null) {
                Label emptyArray = new Label();
                Label readyForArgs = new Label();
                // confirm we're not past the end of the array
                method.dup(); // dup the original array object
                method.invokevirtual(cg.p(RubyArray.class), "getLength", cg.sig(Integer.TYPE));
                method.ldc(new Integer(start));
                method.if_icmple(emptyArray); // if length <= start, end loop
                
                // assign remaining elements as an array for rest args
                method.dup(); // dup the original array object
                method.ldc(start);
                invokeUtilityMethod("createSubarray", cg.sig(RubyArray.class, RubyArray.class, int.class));
                method.go_to(readyForArgs);
                
                // create empty array
                method.label(emptyArray);
                createEmptyArray();
                
                // assign rest args
                method.label(readyForArgs);
                argsCallback.call(this);
                //consume leftover assigned value
                method.pop();
            }
        }

        public void asString() {
            method.invokeinterface(cg.p(IRubyObject.class), "asString", cg.sig(RubyString.class, cg.params()));
        }
        
        public void toJavaString() {
            method.invokevirtual(cg.p(Object.class), "toString", cg.sig(String.class));
        }

        public void nthRef(int match) {
            method.ldc(new Integer(match));
            backref();
            method.invokestatic(cg.p(RubyRegexp.class), "nth_match", cg.sig(IRubyObject.class, cg.params(Integer.TYPE, IRubyObject.class)));
        }

        public void match() {
            method.invokevirtual(cg.p(RubyRegexp.class), "op_match2", cg.sig(IRubyObject.class, cg.params()));
        }

        public void match2() {
            method.invokevirtual(cg.p(RubyRegexp.class), "op_match", cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
        }

        public void match3() {
            loadThreadContext();
            invokeUtilityMethod("match3", cg.sig(IRubyObject.class, RubyRegexp.class, IRubyObject.class, ThreadContext.class));
        }

        public void createNewRegexp(final ByteList value, final int options) {
            String regexpField = getNewConstant(cg.ci(RubyRegexp.class), "lit_reg_");

            // in current method, load the field to see if we've created a Pattern yet
            method.aload(THIS);
            method.getfield(classname, regexpField, cg.ci(RubyRegexp.class));

            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated); //[]

            // load string, for Regexp#source and Regexp#inspect
            String regexpString = value.toString();

            loadRuntime(); //[R]
            method.ldc(regexpString); //[R, rS]
            method.ldc(new Integer(options)); //[R, rS, opts]

            method.invokestatic(cg.p(RubyRegexp.class), "newRegexp", cg.sig(RubyRegexp.class, cg.params(Ruby.class, String.class, Integer.TYPE))); //[reg]

            method.aload(THIS); //[reg, T]
            method.swap(); //[T, reg]
            method.putfield(classname, regexpField, cg.ci(RubyRegexp.class)); //[]
            method.label(alreadyCreated);
            method.aload(THIS); //[T]
            method.getfield(classname, regexpField, cg.ci(RubyRegexp.class)); 
        }

        public void createNewRegexp(CompilerCallback createStringCallback, final int options) {
            boolean onceOnly = (options & ReOptions.RE_OPTION_ONCE) != 0;   // for regular expressions with the /o flag
            Label alreadyCreated = null;
            String regexpField = null;

            // only alter the code if the /o flag was present
            if (onceOnly) {
                regexpField = getNewConstant(cg.ci(RubyRegexp.class), "lit_reg_");
    
                // in current method, load the field to see if we've created a Pattern yet
                method.aload(THIS);
                method.getfield(classname, regexpField, cg.ci(RubyRegexp.class));
    
                alreadyCreated = new Label();
                method.ifnonnull(alreadyCreated);
            }

            loadRuntime();

            createStringCallback.call(this);
            method.ldc(new Integer(options));

            method.invokestatic(cg.p(RubyRegexp.class), "newRegexp", cg.sig(RubyRegexp.class, cg.params(Ruby.class, String.class, Integer.TYPE))); //[reg]

            // only alter the code if the /o flag was present
            if (onceOnly) {
                method.aload(THIS);
                method.swap();
                method.putfield(classname, regexpField, cg.ci(RubyRegexp.class));
                method.label(alreadyCreated);
                method.aload(THIS);
                method.getfield(classname, regexpField, cg.ci(RubyRegexp.class));
            }
        }

        public void pollThreadEvents() {
            loadThreadContext();
            invokeThreadContext("pollThreadEvents", cg.sig(Void.TYPE));
        }

        public void nullToNil() {
            Label notNull = new Label();
            method.dup();
            method.ifnonnull(notNull);
            method.pop();
            method.aload(NIL_INDEX);
            method.label(notNull);
        }

        public void isInstanceOf(Class clazz, BranchCallback trueBranch, BranchCallback falseBranch) {
            method.instance_of(cg.p(clazz));

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
                    method.visitTypeInsn(CHECKCAST, cg.p(RubyMatchData.class));
                    method.dup();
                    method.invokevirtual(cg.p(RubyMatchData.class), "use", cg.sig(void.class));
                    method.ldc(new Long(number));
                    method.invokevirtual(cg.p(RubyMatchData.class), "group", cg.sig(IRubyObject.class, cg.params(long.class)));
                    method.invokeinterface(cg.p(IRubyObject.class), "isNil", cg.sig(boolean.class));
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
            invokeThreadContext("getCurrentFrame", cg.sig(Frame.class));
            method.invokevirtual(cg.p(Frame.class), "getBackRef", cg.sig(IRubyObject.class));
        }

        public void backrefMethod(String methodName) {
            backref();
            method.invokestatic(cg.p(RubyRegexp.class), methodName, cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
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
            return "__ensure_" + (ensureNumber++);
        }

        public void protect(BranchCallback regularCode, BranchCallback protectedCode, Class ret) {

            String mname = getNewEnsureName();
            SkinnyMethodAdapter mv = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, mname, cg.sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class}), null, null));
            SkinnyMethodAdapter old_method = null;
            SkinnyMethodAdapter var_old_method = null;
            SkinnyMethodAdapter inv_old_method = null;
            boolean oldWithinProtection = withinProtection;
            withinProtection = true;
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
                mv.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
                mv.dup();
                mv.astore(RUNTIME_INDEX);
            
                // grab nil for local variables
                mv.invokevirtual(cg.p(Ruby.class), "getNil", cg.sig(IRubyObject.class));
                mv.astore(NIL_INDEX);
            
                mv.invokevirtual(cg.p(ThreadContext.class), "getCurrentScope", cg.sig(DynamicScope.class));
                mv.dup();
                mv.astore(DYNAMIC_SCOPE_INDEX);
                mv.invokevirtual(cg.p(DynamicScope.class), "getValues", cg.sig(IRubyObject[].class));
                mv.astore(VARS_ARRAY_INDEX);

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
                method.astore(EXCEPTION_INDEX);
                method.label(ensureEnd);

                protectedCode.branch(this);

                method.aload(EXCEPTION_INDEX);
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
            }

            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            method.aload(ARGS_INDEX);
            if(this instanceof ASMClosureCompiler) {
                pushNull();
            } else {
                loadBlock();
            }
            method.invokevirtual(classname, mname, cg.sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class}));
        }

        protected String getNewRescueName() {
            return "__rescue_" + (rescueNumber++);
        }

        public void rescue(BranchCallback regularCode, Class exception, BranchCallback catchCode, Class ret) {
            String mname = getNewRescueName();
            SkinnyMethodAdapter mv = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, mname, cg.sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class}), null, null));
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
                mv.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
                mv.dup();
                mv.astore(RUNTIME_INDEX);
                
                // store previous exception for restoration if we rescue something
                loadRuntime();
                invokeUtilityMethod("getErrorInfo", cg.sig(IRubyObject.class, Ruby.class));
                mv.astore(PREVIOUS_EXCEPTION_INDEX);
            
                // grab nil for local variables
                mv.invokevirtual(cg.p(Ruby.class), "getNil", cg.sig(IRubyObject.class));
                mv.astore(NIL_INDEX);
            
                mv.invokevirtual(cg.p(ThreadContext.class), "getCurrentScope", cg.sig(DynamicScope.class));
                mv.dup();
                mv.astore(DYNAMIC_SCOPE_INDEX);
                mv.invokevirtual(cg.p(DynamicScope.class), "getValues", cg.sig(IRubyObject[].class));
                mv.astore(VARS_ARRAY_INDEX);

                Label beforeBody = new Label();
                Label afterBody = new Label();
                Label catchBlock = new Label();
                mv.visitTryCatchBlock(beforeBody, afterBody, catchBlock, cg.p(exception));
                mv.visitLabel(beforeBody);

                regularCode.branch(this);

                mv.label(afterBody);
                mv.go_to(exitRescue);
                mv.label(catchBlock);
                mv.astore(EXCEPTION_INDEX);

                catchCode.branch(this);
                
                mv.label(afterMethodBody);
                mv.go_to(exitRescue);
                
                mv.trycatch(beforeBody, afterMethodBody, catchRetry, cg.p(JumpException.RetryJump.class));
                mv.label(catchRetry);
                mv.pop();
                mv.go_to(beforeBody);
                
                // any exceptions raised must continue to be raised, skipping $! restoration
                mv.trycatch(beforeBody, afterMethodBody, catchRaised, cg.p(RaiseException.class));
                mv.label(catchRaised);
                mv.athrow();
                
                // and remaining jump exceptions should restore $!
                mv.trycatch(beforeBody, afterMethodBody, catchJumps, cg.p(JumpException.class));
                mv.label(catchJumps);
                loadRuntime();
                mv.aload(PREVIOUS_EXCEPTION_INDEX);
                invokeUtilityMethod("setErrorInfo", cg.sig(void.class, Ruby.class, IRubyObject.class));
                mv.athrow();
                
                mv.label(exitRescue);
                
                // restore the original exception
                loadRuntime();
                mv.aload(PREVIOUS_EXCEPTION_INDEX);
                invokeUtilityMethod("setErrorInfo", cg.sig(void.class, Ruby.class, IRubyObject.class));
                
                mv.areturn();
                mv.visitMaxs(1, 1);
                mv.visitEnd();
            } finally {
                withinProtection = oldWithinProtection;
                this.method = old_method;
                getVariableCompiler().setMethodAdapter(var_old_method);
                getInvocationCompiler().setMethodAdapter(inv_old_method);
            }
            
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            method.aload(ARGS_INDEX);
            if(this instanceof ASMClosureCompiler) {
                pushNull();
            } else {
                loadBlock();
            }
            method.invokevirtual(classname, mname, cg.sig(ret, new Class[]{ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class}));
        }

        public void inDefined() {
            method.aload(THREADCONTEXT_INDEX);
            method.iconst_1();
            invokeThreadContext("setWithinDefined", cg.sig(void.class, cg.params(boolean.class)));
        }

        public void outDefined() {
            method.aload(THREADCONTEXT_INDEX);
            method.iconst_0();
            invokeThreadContext("setWithinDefined", cg.sig(void.class, cg.params(boolean.class)));
        }

        public void stringOrNil() {
            loadRuntime();
            loadNil();
            invokeUtilityMethod("stringOrNil", cg.sig(IRubyObject.class, String.class, Ruby.class, IRubyObject.class));
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
            method.invokevirtual(cg.p(RubyClass.class), "isMethodBound", cg.sig(boolean.class, cg.params(String.class, boolean.class)));
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
            method.invokevirtual(cg.p(Block.class), "isGiven", cg.sig(boolean.class));
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
            invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
            method.ldc(name);
            method.invokevirtual(cg.p(GlobalVariables.class), "isDefined", cg.sig(boolean.class, cg.params(String.class)));
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
            invokeThreadContext("getConstantDefined", cg.sig(boolean.class, cg.params(String.class)));
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
            invokeIRubyObject("getInstanceVariables", cg.sig(InstanceVariables.class));
            method.ldc(name);
            //method.invokeinterface(cg.p(IRubyObject.class), "getInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class)));
            method.invokeinterface(cg.p(InstanceVariables.class), "fastHasInstanceVariable", cg.sig(boolean.class, cg.params(String.class)));
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
            method.invokevirtual(cg.p(RubyModule.class), "fastIsClassVarDefined", cg.sig(boolean.class, cg.params(String.class)));
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
            method.invokeinterface(cg.p(IRubyObject.class), "isNil", cg.sig(boolean.class));
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
                        method.instance_of(cg.p(RubyModule.class)); //[C, boolean]

                        Label falseJmp = new Label();
                        Label afterJmp = new Label();
                        Label nextJmp = new Label();
                        Label nextJmpPop = new Label();

                        method.ifeq(nextJmp); // EQ == 0 (i.e. false)   //[C]
                        method.visitTypeInsn(CHECKCAST, cg.p(RubyModule.class));
                        method.dup(); //[C, C]
                        method.ldc(name); //[C, C, String]
                        method.invokevirtual(cg.p(RubyModule.class), "fastGetConstantAt", cg.sig(IRubyObject.class, cg.params(String.class))); //[C, null|C]
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
                        method.invokevirtual(cg.p(RubyClass.class), "isMethodBound", cg.sig(boolean.class, cg.params(String.class, boolean.class)));
                        method.ifeq(falseJmp); // EQ == 0 (i.e. false)
                        
                        isMethod.branch(AbstractMethodCompiler.this);
                        method.go_to(afterJmp);

                        method.label(falseJmp);
                        none.branch(AbstractMethodCompiler.this);
            
                        method.label(afterJmp);
                    }}, JumpException.class, none, String.class);
        }
        
        public void metaclass() {
            invokeIRubyObject("getMetaClass", cg.sig(RubyClass.class));
        }
        
        public void getVisibilityFor(String name) {
            method.ldc(name);
            method.invokevirtual(cg.p(RubyClass.class), "searchMethod", cg.sig(DynamicMethod.class, cg.params(String.class)));
            method.invokevirtual(cg.p(DynamicMethod.class), "getVisibility", cg.sig(Visibility.class));
        }
        
        public void isPrivate(Object gotoToken, int toConsume) {
            method.invokevirtual(cg.p(Visibility.class), "isPrivate", cg.sig(boolean.class));
            Label temp = new Label();
            method.ifeq(temp); // EQ == 0 (i.e. false)
            while((toConsume--) > 0) {
                  method.pop();
            }
            method.go_to((Label)gotoToken);
            method.label(temp);
        }
        
        public void isNotProtected(Object gotoToken, int toConsume) {
            method.invokevirtual(cg.p(Visibility.class), "isProtected", cg.sig(boolean.class));
            Label temp = new Label();
            method.ifne(temp);
            while((toConsume--) > 0) {
                  method.pop();
            }
            method.go_to((Label)gotoToken);
            method.label(temp);
        }
        
        public void selfIsKindOf(Object gotoToken) {
            method.invokevirtual(cg.p(RubyClass.class), "getRealClass", cg.sig(RubyClass.class));
            loadSelf();
            method.invokevirtual(cg.p(RubyModule.class), "isInstance", cg.sig(boolean.class, cg.params(IRubyObject.class)));
            method.ifne((Label)gotoToken); // EQ != 0 (i.e. true)
        }
        
        public void notIsModuleAndClassVarDefined(String name, Object gotoToken) {
            method.dup(); //[?, ?]
            method.instance_of(cg.p(RubyModule.class)); //[?, boolean]
            Label falsePopJmp = new Label();
            Label successJmp = new Label();
            method.ifeq(falsePopJmp);

            method.visitTypeInsn(CHECKCAST, cg.p(RubyModule.class)); //[RubyModule]
            method.ldc(name); //[RubyModule, String]
            
            method.invokevirtual(cg.p(RubyModule.class), "fastIsClassVarDefined", cg.sig(boolean.class, cg.params(String.class))); //[boolean]
            method.ifeq((Label)gotoToken);
            method.go_to(successJmp);
            method.label(falsePopJmp);
            method.pop();
            method.go_to((Label)gotoToken);
            method.label(successJmp);
        }
        
        public void ifSingleton(Object gotoToken) {
            method.invokevirtual(cg.p(RubyModule.class), "isSingleton", cg.sig(boolean.class));
            method.ifne((Label)gotoToken); // EQ == 0 (i.e. false)
        }
        
        public void getInstanceVariable(String name) {
            method.ldc(name);
            invokeIRubyObject("getInstanceVariables", cg.sig(InstanceVariables.class));
            method.invokeinterface(cg.p(InstanceVariables.class), "fastGetInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class)));
        }
        
        public void getFrameName() {
            loadThreadContext();
            invokeThreadContext("getFrameName", cg.sig(String.class));
        }
        
        public void getFrameKlazz() {
            loadThreadContext();
            invokeThreadContext("getFrameKlazz", cg.sig(RubyModule.class));
        }
        
        public void superClass() {
            method.invokevirtual(cg.p(RubyModule.class), "getSuperClass", cg.sig(RubyClass.class));
        }
        public void attached() {
            method.visitTypeInsn(CHECKCAST, cg.p(MetaClass.class));
            method.invokevirtual(cg.p(MetaClass.class), "getAttached", cg.sig(IRubyObject.class));
        }
        public void ifNotSuperMethodBound(Object token) {
            method.swap();
            method.iconst_0();
            method.invokevirtual(cg.p(RubyModule.class), "isMethodBound", cg.sig(boolean.class, cg.params(String.class, boolean.class)));
            method.ifeq((Label)token);
        }
        
        public void concatArrays() {
            method.invokevirtual(cg.p(RubyArray.class), "concat", cg.sig(RubyArray.class, cg.params(IRubyObject.class)));
        }
        
        public void concatObjectArrays() {
            invokeUtilityMethod("concatObjectArrays", cg.sig(IRubyObject[].class, cg.params(IRubyObject[].class, IRubyObject[].class)));
        }
        
        public void appendToArray() {
            method.invokevirtual(cg.p(RubyArray.class), "append", cg.sig(RubyArray.class, cg.params(IRubyObject.class)));
        }
        
        public void appendToObjectArray() {
            invokeUtilityMethod("appendToObjectArray", cg.sig(IRubyObject[].class, cg.params(IRubyObject[].class, IRubyObject.class)));
        }
        
        public void convertToJavaArray() {
            method.invokestatic(cg.p(ArgsUtil.class), "convertToJavaArray", cg.sig(IRubyObject[].class, cg.params(IRubyObject.class)));
        }

        public void aliasGlobal(String newName, String oldName) {
            loadRuntime();
            invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
            method.ldc(newName);
            method.ldc(oldName);
            method.invokevirtual(cg.p(GlobalVariables.class), "alias", cg.sig(Void.TYPE, cg.params(String.class, String.class)));
            loadNil();
        }
        
        public void undefMethod(String name) {
            loadThreadContext();
            invokeThreadContext("getRubyClass", cg.sig(RubyModule.class));
            
            Label notNull = new Label();
            method.dup();
            method.ifnonnull(notNull);
            method.pop();
            loadRuntime();
            method.ldc("No class to undef method '" + name + "'.");
            invokeIRuby("newTypeError", cg.sig(RaiseException.class, cg.params(String.class)));
            method.athrow();
            
            method.label(notNull);
            method.ldc(name);
            method.invokevirtual(cg.p(RubyModule.class), "undef", cg.sig(Void.TYPE, cg.params(String.class)));
            
            loadNil();
        }

        public void defineClass(
                final String name, 
                final StaticScope staticScope, 
                final CompilerCallback superCallback, 
                final CompilerCallback pathCallback, 
                final CompilerCallback bodyCallback, 
                final CompilerCallback receiverCallback) {
            String methodName = "rubyclass__" + JavaNameMangler.mangleStringForCleanJavaIdentifier(name) + "__" + ++methodIndex;

            final ASMMethodCompiler methodCompiler = new ASMMethodCompiler(methodName, null);
            
            CompilerCallback bodyPrep = new CompilerCallback() {
                public void call(MethodCompiler context) {
                    if (receiverCallback == null) {
                        if (superCallback != null) {
                            methodCompiler.loadRuntime();
                            superCallback.call(methodCompiler);

                            methodCompiler.invokeUtilityMethod("prepareSuperClass", cg.sig(RubyClass.class, cg.params(Ruby.class, IRubyObject.class)));
                        } else {
                            methodCompiler.method.aconst_null();
                        }

                        methodCompiler.loadThreadContext();

                        pathCallback.call(methodCompiler);

                        methodCompiler.invokeUtilityMethod("prepareClassNamespace", cg.sig(RubyModule.class, cg.params(ThreadContext.class, IRubyObject.class)));

                        methodCompiler.method.swap();

                        methodCompiler.method.ldc(name);

                        methodCompiler.method.swap();

                        methodCompiler.method.invokevirtual(cg.p(RubyModule.class), "defineOrGetClassUnder", cg.sig(RubyClass.class, cg.params(String.class, RubyClass.class)));
                    } else {
                        methodCompiler.loadRuntime();

                        receiverCallback.call(methodCompiler);

                        methodCompiler.invokeUtilityMethod("getSingletonClass", cg.sig(RubyClass.class, cg.params(Ruby.class, IRubyObject.class)));
                    }

                    // set self to the class
                    methodCompiler.method.dup();
                    methodCompiler.method.astore(SELF_INDEX);

                    // CLASS BODY
                    methodCompiler.loadThreadContext();
                    methodCompiler.method.swap();

                    // static scope
                    buildStaticScopeNames(methodCompiler.method, staticScope);
                    methodCompiler.invokeThreadContext("preCompiledClass", cg.sig(Void.TYPE, cg.params(RubyModule.class, String[].class)));
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
            methodCompiler.invokeThreadContext("postCompiledClass", cg.sig(Void.TYPE, cg.params()));
            
            methodCompiler.method.go_to(noException);
            
            methodCompiler.method.label(after);
            // finally with exception
            methodCompiler.loadThreadContext();
            methodCompiler.invokeThreadContext("postCompiledClass", cg.sig(Void.TYPE, cg.params()));
            methodCompiler.method.athrow();
            
            methodCompiler.method.label(noException);

            methodCompiler.endMethod();

            // prepare to call class definition method
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
            method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));

            method.invokevirtual(classname, methodName, METHOD_SIGNATURE);
        }

        public void defineModule(final String name, final StaticScope staticScope, final CompilerCallback pathCallback, final CompilerCallback bodyCallback) {
            String methodName = "rubyclass__" + JavaNameMangler.mangleStringForCleanJavaIdentifier(name) + "__" + ++methodIndex;

            final ASMMethodCompiler methodCompiler = new ASMMethodCompiler(methodName, null);

            CompilerCallback bodyPrep = new CompilerCallback() {
                public void call(MethodCompiler context) {
                    methodCompiler.loadThreadContext();

                    pathCallback.call(methodCompiler);

                    methodCompiler.invokeUtilityMethod("prepareClassNamespace", cg.sig(RubyModule.class, cg.params(ThreadContext.class, IRubyObject.class)));

                    methodCompiler.method.ldc(name);

                    methodCompiler.method.invokevirtual(cg.p(RubyModule.class), "defineOrGetModuleUnder", cg.sig(RubyModule.class, cg.params(String.class)));

                    // set self to the class
                    methodCompiler.method.dup();
                    methodCompiler.method.astore(SELF_INDEX);

                    // CLASS BODY
                    methodCompiler.loadThreadContext();
                    methodCompiler.method.swap();

                    // static scope
                    buildStaticScopeNames(methodCompiler.method, staticScope);

                    methodCompiler.invokeThreadContext("preCompiledClass", cg.sig(Void.TYPE, cg.params(RubyModule.class, String[].class)));
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
            methodCompiler.invokeThreadContext("postCompiledClass", cg.sig(Void.TYPE, cg.params()));
            methodCompiler.method.athrow();
            
            methodCompiler.method.label(noException);
            methodCompiler.loadThreadContext();
            methodCompiler.invokeThreadContext("postCompiledClass", cg.sig(Void.TYPE, cg.params()));

            methodCompiler.endMethod();

            // prepare to call class definition method
            method.aload(THIS);
            loadThreadContext();
            loadSelf();
            method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
            method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));

            method.invokevirtual(classname, methodName, METHOD_SIGNATURE);
        }
        
        public void unwrapPassedBlock() {
            loadBlock();
            invokeUtilityMethod("getBlockFromBlockPassBody", cg.sig(Block.class, cg.params(IRubyObject.class, Block.class)));
        }
        
        public void performBackref(char type) {
            loadThreadContext();
            switch (type) {
            case '~':
                invokeUtilityMethod("backref", cg.sig(IRubyObject.class, cg.params(ThreadContext.class)));
                break;
            case '&':
                invokeUtilityMethod("backrefLastMatch", cg.sig(IRubyObject.class, cg.params(ThreadContext.class)));
                break;
            case '`':
                invokeUtilityMethod("backrefMatchPre", cg.sig(IRubyObject.class, cg.params(ThreadContext.class)));
                break;
            case '\'':
                invokeUtilityMethod("backrefMatchPost", cg.sig(IRubyObject.class, cg.params(ThreadContext.class)));
                break;
            case '+':
                invokeUtilityMethod("backrefMatchLast", cg.sig(IRubyObject.class, cg.params(ThreadContext.class)));
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
                method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));
            }
            loadSelf();
            
            invokeUtilityMethod("callZSuper", cg.sig(IRubyObject.class, cg.params(Ruby.class, ThreadContext.class, Block.class, IRubyObject.class)));
        }
        
        public void checkIsExceptionHandled() {
            // ruby exception and list of exception types is on the stack
            loadRuntime();
            loadThreadContext();
            loadSelf();
            invokeUtilityMethod("isExceptionHandled", cg.sig(IRubyObject.class, RubyException.class, IRubyObject[].class, Ruby.class, ThreadContext.class, IRubyObject.class));
        }
        
        public void rethrowException() {
            loadException();
            method.athrow();
        }
        
        public void loadClass(String name) {
            loadRuntime();
            method.ldc(name);
            invokeIRuby("getClass", cg.sig(RubyClass.class, String.class));
        }
        
        public void unwrapRaiseException() {
            // RaiseException is on stack, get RubyException out
            method.invokevirtual(cg.p(RaiseException.class), "getException", cg.sig(RubyException.class));
        }
        
        public void loadException() {
            method.aload(EXCEPTION_INDEX);
        }
        
        public void setPosition(ISourcePosition position) {
            // FIXME I'm still not happy with this additional overhead per line,
            // nor about the extra script construction cost, but it will have to do for now.
            loadThreadContext();
            method.getstatic(classname, cachePosition(position.getFile(), position.getEndLine()), cg.ci(ISourcePosition.class));
            invokeThreadContext("setPosition", cg.sig(void.class, ISourcePosition.class));
        }
        
        public void checkWhenWithSplat() {
            loadThreadContext();
            invokeUtilityMethod("isWhenTriggered", cg.sig(RubyBoolean.class, IRubyObject.class, IRubyObject.class, ThreadContext.class));
        }
        
        public void issueRetryEvent() {
            invokeUtilityMethod("retryJump", cg.sig(IRubyObject.class));
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
                methodName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name) + "__" + methodIndex;
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

            method.ldc(methodArity);
            
            // arities
            method.ldc(scope.getRequiredArgs());
            method.ldc(scope.getOptionalArgs());
            method.ldc(scope.getRestArg());
            
            // if method has frame aware methods or frameless compilation is NOT enabled
            if (inspector.hasFrameAwareMethods() || !RubyInstanceConfig.FRAMELESS_COMPILE_ENABLED) {
                if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                    method.getstatic(cg.p(CallConfiguration.class), CallConfiguration.FRAME_AND_SCOPE.name(), cg.ci(CallConfiguration.class));
                } else {
                    method.getstatic(cg.p(CallConfiguration.class), CallConfiguration.FRAME_ONLY.name(), cg.ci(CallConfiguration.class));
                }
            } else {
                if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                    // TODO: call config with scope but no frame
                    method.getstatic(cg.p(CallConfiguration.class), CallConfiguration.SCOPE_ONLY.name(), cg.ci(CallConfiguration.class));
                } else {
                    method.getstatic(cg.p(CallConfiguration.class), CallConfiguration.NO_FRAME_NO_SCOPE.name(), cg.ci(CallConfiguration.class));
                }
            }
            
            if (receiver != null) {
                invokeUtilityMethod("defs", cg.sig(IRubyObject.class, 
                        cg.params(ThreadContext.class, IRubyObject.class, IRubyObject.class, Object.class, String.class, String.class, String[].class, int.class, int.class, int.class, int.class, CallConfiguration.class)));
            } else {
                invokeUtilityMethod("def", cg.sig(IRubyObject.class, 
                        cg.params(ThreadContext.class, IRubyObject.class, Object.class, String.class, String.class, String[].class, int.class, int.class, int.class, int.class, CallConfiguration.class)));
            }
        }

        public void rethrowIfSystemExit() {
            loadRuntime();
            method.ldc("SystemExit");
            method.invokevirtual(cg.p(Ruby.class), "fastGetClass", cg.sig(RubyClass.class, String.class));
            method.swap();
            method.invokevirtual(cg.p(RubyModule.class), "isInstance", cg.sig(boolean.class, cg.params(IRubyObject.class)));
            method.iconst_0();
            Label ifEnd = new Label();
            method.if_icmpeq(ifEnd);
            loadException();
            method.athrow();
            method.label(ifEnd);
        }
    }

    public class ASMClosureCompiler extends AbstractMethodCompiler {
        private String closureMethodName;
        
        public ASMClosureCompiler(String closureMethodName, String closureFieldName, ASTInspector inspector) {
            this.closureMethodName = closureMethodName;

            // declare the field
            getClassVisitor().visitField(ACC_PRIVATE, closureFieldName, cg.ci(CompiledBlockCallback.class), null, null);
            
            method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, closureMethodName, CLOSURE_SIGNATURE, null, null));
            if (inspector == null) {
                variableCompiler = new HeapBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX, CLOSURE_INDEX);
            } else if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                // enable "boxed" variable compilation when only a closure present
                // this breaks using a proc as a binding
                if (Boolean.getBoolean("jruby.compile.boxed") && !inspector.hasScopeAwareMethods()) {
                    variableCompiler = new BoxedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX, CLOSURE_INDEX);
                } else {
                    variableCompiler = new HeapBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX, CLOSURE_INDEX);
                }
            } else {
                variableCompiler = new StackBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, ARGS_INDEX, CLOSURE_INDEX);
            }
            invocationCompiler = new StandardInvocationCompiler(this, method);
        }

        public void beginMethod(CompilerCallback args, StaticScope scope) {
            method.start();

            // set up a local IRuby variable
            method.aload(THREADCONTEXT_INDEX);
            invokeThreadContext("getRuntime", cg.sig(Ruby.class));
            method.dup();
            method.astore(RUNTIME_INDEX);
            
            // grab nil for local variables
            invokeIRuby("getNil", cg.sig(IRubyObject.class));
            method.astore(NIL_INDEX);
            
            variableCompiler.beginClosure(args, scope);

            // start of scoping for closure's vars
            scopeStart = new Label();
            scopeEnd = new Label();
            redoJump = new Label();
            method.label(scopeStart);
        }

        public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
            throw new NotCompilableException("ERROR: closure compiler should not be used for class bodies");
        }

        public void endMethod() {
            // end of scoping for closure's vars
            scopeEnd = new Label();
            method.areturn();
            method.label(scopeEnd);
            
            // handle redos by restarting the block
            method.pop();
            method.go_to(scopeStart);
            
            method.trycatch(scopeStart, scopeEnd, scopeEnd, cg.p(JumpException.RedoJump.class));
            method.end();
        }

        @Override
        public void loadBlock() {
            loadThreadContext();
            invokeThreadContext("getFrameBlock", cg.sig(Block.class));
        }

        protected String getNewRescueName() {
            return closureMethodName + "_" + super.getNewRescueName();
        }

        protected String getNewEnsureName() {
            return closureMethodName + "_" + super.getNewEnsureName();
        }

        public void performReturn() {
            loadThreadContext();
            invokeUtilityMethod("returnJump", cg.sig(IRubyObject.class, IRubyObject.class, ThreadContext.class));
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
            method.ldc(new Integer(index));
            invokeUtilityMethod("processBlockArgument", cg.sig(void.class, cg.params(Ruby.class, ThreadContext.class, Block.class, int.class)));
        }
        
        public void issueBreakEvent(CompilerCallback value) {
            if (withinProtection || currentLoopLabels == null) {
                value.call(this);
                invokeUtilityMethod("breakJump", cg.sig(IRubyObject.class, IRubyObject.class));
            } else {
                value.call(this);
                issueLoopBreak();
            }
        }

        public void issueNextEvent(CompilerCallback value) {
            if (withinProtection || currentLoopLabels == null) {
                value.call(this);
                invokeUtilityMethod("nextJump", cg.sig(IRubyObject.class, IRubyObject.class));
            } else {
                value.call(this);
                issueLoopNext();
            }
        }

        public void issueRedoEvent() {
            // FIXME: This isn't right for within ensured/rescued code
            if (withinProtection) {
                invokeUtilityMethod("redoJump", cg.sig(IRubyObject.class));
            } else if (currentLoopLabels != null) {
                issueLoopRedo();
            } else {
                // jump back to the top of the main body of this closure
                method.go_to(scopeStart);
            }
        }
    }

    public class ASMMethodCompiler extends AbstractMethodCompiler {
        private String friendlyName;

        public ASMMethodCompiler(String friendlyName, ASTInspector inspector) {
            this.friendlyName = friendlyName;

            method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC, friendlyName, METHOD_SIGNATURE, null, null));
            if (inspector == null) {
                variableCompiler = new HeapBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX, CLOSURE_INDEX);
            } else if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                // enable "boxed" variable compilation when only a closure present
                // this breaks using a proc as a binding
                if (Boolean.getBoolean("jruby.compile.boxed") && !inspector.hasScopeAwareMethods()) {
                    variableCompiler = new BoxedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX, CLOSURE_INDEX);
                } else {
                    variableCompiler = new HeapBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX, CLOSURE_INDEX);
                }
            } else {
                variableCompiler = new StackBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, ARGS_INDEX, CLOSURE_INDEX);
            }
            invocationCompiler = new StandardInvocationCompiler(this, method);
        }
        
        public void beginChainedMethod() {
            method.aload(THREADCONTEXT_INDEX);
            method.dup();
            method.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
            method.dup();
            method.astore(RUNTIME_INDEX);

            // grab nil for local variables
            method.invokevirtual(cg.p(Ruby.class), "getNil", cg.sig(IRubyObject.class));
            method.astore(NIL_INDEX);

            method.invokevirtual(cg.p(ThreadContext.class), "getCurrentScope", cg.sig(DynamicScope.class));
            method.dup();
            method.astore(DYNAMIC_SCOPE_INDEX);
            method.invokevirtual(cg.p(DynamicScope.class), "getValues", cg.sig(IRubyObject[].class));
            method.astore(VARS_ARRAY_INDEX);
        }

        public void beginMethod(CompilerCallback args, StaticScope scope) {
            method.start();

            // set up a local IRuby variable
            method.aload(THREADCONTEXT_INDEX);
            invokeThreadContext("getRuntime", cg.sig(Ruby.class));
            method.dup();
            method.astore(RUNTIME_INDEX);
            
            
            // grab nil for local variables
            invokeIRuby("getNil", cg.sig(IRubyObject.class));
            method.astore(NIL_INDEX);
            
            variableCompiler.beginMethod(args, scope);

            // visit a label to start scoping for local vars in this method
            Label start = new Label();
            method.label(start);

            scopeStart = start;
        }

        public void beginClass(CompilerCallback bodyPrep, StaticScope scope) {
            method.start();

            // set up a local IRuby variable
            method.aload(THREADCONTEXT_INDEX);
            invokeThreadContext("getRuntime", cg.sig(Ruby.class));
            method.dup();
            method.astore(RUNTIME_INDEX);
            
            // grab nil for local variables
            invokeIRuby("getNil", cg.sig(IRubyObject.class));
            method.astore(NIL_INDEX);
            
            variableCompiler.beginClass(bodyPrep, scope);

            // visit a label to start scoping for local vars in this method
            Label start = new Label();
            method.label(start);

            scopeStart = start;
        }

        public void endMethod() {
            // return last value from execution
            method.areturn();

            // end of variable scope
            Label end = new Label();
            method.label(end);

            method.end();
        }
        
        public void performReturn() {
            // normal return for method body. return jump for within a begin/rescue/ensure
            if (withinProtection) {
                loadThreadContext();
                invokeUtilityMethod("returnJump", cg.sig(IRubyObject.class, IRubyObject.class, ThreadContext.class));
            } else {
                method.areturn();
            }
        }

        public void issueBreakEvent(CompilerCallback value) {
            if (withinProtection) {
                value.call(this);
                invokeUtilityMethod("breakJump", cg.sig(IRubyObject.class, IRubyObject.class));
            } else if (currentLoopLabels != null) {
                value.call(this);
                issueLoopBreak();
            } else {
                // in method body with no containing loop, issue jump error
                // load runtime and value, issue jump error
                loadRuntime();
                value.call(this);
                invokeUtilityMethod("breakLocalJumpError", cg.sig(IRubyObject.class, Ruby.class, IRubyObject.class));
            }
        }

        public void issueNextEvent(CompilerCallback value) {
            if (withinProtection) {
                value.call(this);
                invokeUtilityMethod("nextJump", cg.sig(IRubyObject.class, IRubyObject.class));
            } else if (currentLoopLabels != null) {
                value.call(this);
                issueLoopNext();
            } else {
                // in method body with no containing loop, issue jump error
                // load runtime and value, issue jump error
                loadRuntime();
                value.call(this);
                invokeUtilityMethod("nextLocalJumpError", cg.sig(IRubyObject.class, Ruby.class, IRubyObject.class));
            }
        }

        public void issueRedoEvent() {
            if (withinProtection) {
                invokeUtilityMethod("redoJump", cg.sig(IRubyObject.class));
            } else if (currentLoopLabels != null) {
                issueLoopRedo();
            } else {
                // in method body with no containing loop, issue jump error
                // load runtime and value, issue jump error
                loadRuntime();
                invokeUtilityMethod("redoLocalJumpError", cg.sig(IRubyObject.class, Ruby.class));
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

    public String getNewStaticConstant(String type, String name_prefix) {
        ClassVisitor cv = getClassVisitor();

        String realName;
        synchronized (this) {
            realName = "__" + constants++;
        }

        // declare the field
        cv.visitField(ACC_PRIVATE | ACC_STATIC, realName, type, null, null).visitEnd();
        return realName;
    }
    
    public String cacheCallSite(String name, CallType callType) {
        String fieldName = getNewConstant(cg.ci(CallSite.class), JavaNameMangler.mangleStringForCleanJavaIdentifier(name));
        
        // retrieve call adapter
        initMethod.aload(THIS);
        initMethod.ldc(name);
        if (callType.equals(CallType.NORMAL)) {
            initMethod.invokestatic(cg.p(MethodIndex.class), "getCallSite", cg.sig(CallSite.class, cg.params(String.class)));
        } else if (callType.equals(CallType.FUNCTIONAL)) {
            initMethod.invokestatic(cg.p(MethodIndex.class), "getFunctionalCallSite", cg.sig(CallSite.class, cg.params(String.class)));
        } else if (callType.equals(CallType.VARIABLE)) {
            initMethod.invokestatic(cg.p(MethodIndex.class), "getVariableCallSite", cg.sig(CallSite.class, cg.params(String.class)));
        }
        initMethod.putfield(classname, fieldName, cg.ci(CallSite.class));
        
        return fieldName;
    }
    
    public String cachePosition(String file, int line) {
        String cleanName = JavaNameMangler.mangleStringForCleanJavaIdentifier(file + "$" + line);
        String fieldName = sourcePositions.get(cleanName);
        if (fieldName == null) {
            fieldName = getNewStaticConstant(cg.ci(ISourcePosition.class), cleanName);
            sourcePositions.put(JavaNameMangler.mangleStringForCleanJavaIdentifier(file + "$" + line), fieldName);

            clinitMethod.ldc(file);
            clinitMethod.ldc(line);
            clinitMethod.invokestatic(cg.p(RuntimeHelpers.class), "constructPosition", cg.sig(ISourcePosition.class, String.class, int.class));
            clinitMethod.putstatic(classname, fieldName, cg.ci(ISourcePosition.class));
        }
        
        return fieldName;
    }
    
    public String cacheByteList(String contents) {
        String fieldName = byteLists.get(contents);
        if (fieldName == null) {
            fieldName = getNewStaticConstant(cg.ci(ByteList.class), "byteList");
            byteLists.put(contents, fieldName);

            clinitMethod.ldc(contents);
            clinitMethod.invokestatic(cg.p(ByteList.class), "create", cg.sig(ByteList.class, CharSequence.class));
            clinitMethod.putstatic(classname, fieldName, cg.ci(ByteList.class));
        }
        
        return fieldName;
    }
    
    public String cacheSymbol(String symbol) {
        String methodName = symbols.get(symbol);
        if (methodName == null) {
            String fieldName = getNewConstant(cg.ci(RubySymbol.class), "symbol");
            
            methodName = "getSymbol" + fieldName;
            symbols.put(symbol, methodName);
            
            ClassVisitor cv = getClassVisitor();
            
            SkinnyMethodAdapter symMethod = new SkinnyMethodAdapter(
                    cv.visitMethod(ACC_PRIVATE | ACC_SYNTHETIC, methodName, 
                            cg.sig(RubySymbol.class, Ruby.class), null, null));
            symMethod.start();
            symMethod.aload(0);
            symMethod.getfield(classname, fieldName, cg.ci(RubySymbol.class));
            symMethod.dup();
            symMethod.astore(2);
            
            Label ifNullEnd = new Label();
            symMethod.ifnull(ifNullEnd);
            symMethod.aload(2);
            symMethod.areturn();
            symMethod.label(ifNullEnd);
            symMethod.aload(0);
            symMethod.aload(1);
            symMethod.ldc(symbol);
            symMethod.invokevirtual(cg.p(Ruby.class), "fastNewSymbol", cg.sig(RubySymbol.class, String.class));
            symMethod.dup_x1();
            symMethod.putfield(classname, fieldName, cg.ci(RubySymbol.class));
            symMethod.areturn();
            symMethod.end();
        }
        
        return methodName;
    }
}
