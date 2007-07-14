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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import jregex.Pattern;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.ArrayCallback;
import org.jruby.compiler.BranchCallback;
import org.jruby.compiler.ClosureCallback;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.compiler.MethodCompiler;
import org.jruby.compiler.ScriptCompiler;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.VariableCompiler;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.javasupport.util.CompilerHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.ReOptions;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlock;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.Dispatcher;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

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

    private static final int THREADCONTEXT_INDEX = 0;
    private static final int SELF_INDEX = 1;
    private static final int ARGS_INDEX = 2;
    private static final int CLOSURE_INDEX = 3;
    private static final int DYNAMIC_SCOPE_INDEX = 4;
    private static final int RUNTIME_INDEX = 5;
    private static final int VARS_ARRAY_INDEX = 6;
    private static final int NIL_INDEX = 7;

    private String classname;
    private String sourcename;

    private ClassWriter classWriter;
    int methodIndex = -1;
    int innerIndex = -1;

    /** Creates a new instance of StandardCompilerContext */
    public StandardASMCompiler(String classname, String sourcename) {
        this.classname = classname;
        this.sourcename = sourcename;
    }

    public Class loadClass(JRubyClassLoader classLoader) throws ClassNotFoundException {
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

        out.write(writer.toByteArray());
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

    public void startScript() {
        classWriter = new ClassWriter(true);

        // Create the class with the appropriate class name and source file
        classWriter.visit(V1_4, ACC_PUBLIC + ACC_SUPER, classname, null, cg.p(Object.class), new String[]{cg.p(Script.class)});
        classWriter.visitSource(sourcename, null);

        createClassInit();
        createConstructor();
    }

    public void endScript() {
        // add Script#run impl, used for running this script with a specified threadcontext and self
        // root method of a script is always in __load__ method
        String methodName = "__file__";
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC, "run", METHOD_SIGNATURE, null, null));
        method.start();

        // invoke __file__ with threadcontext, self, args (null), and block (null)
        // These are all +1 because run is an instance method where others are static
        method.aload(THREADCONTEXT_INDEX + 1);
        method.aload(SELF_INDEX + 1);
        method.aload(ARGS_INDEX + 1);
        method.aload(CLOSURE_INDEX + 1);

        method.invokestatic(classname, methodName, METHOD_SIGNATURE);
        method.areturn();
        method.end();

        // add main impl, used for detached or command-line execution of this script with a new runtime
        // root method of a script is always in stub0, method0
        method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_STATIC, "main", cg.sig(Void.TYPE, cg.params(String[].class)), null, null));
        method.start();

        // new instance to invoke run against
        method.newobj(classname);
        method.dup();
        method.invokespecial(classname, "<init>", cg.sig(Void.TYPE));

        // invoke run with threadcontext and topself
        method.invokestatic(cg.p(Ruby.class), "getDefaultInstance", cg.sig(Ruby.class));
        method.dup();

        method.invokevirtual(RUBY, "getCurrentContext", cg.sig(ThreadContext.class));
        method.swap();
        method.invokevirtual(RUBY, "getTopSelf", cg.sig(IRubyObject.class));
        method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
        method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));

        method.invokevirtual(classname, "run", METHOD_SIGNATURE);
        method.voidreturn();
        method.end();
    }

    private void createConstructor() {
        ClassVisitor cv = getClassVisitor();

        SkinnyMethodAdapter method = new SkinnyMethodAdapter(cv.visitMethod(ACC_PUBLIC, "<init>", cg.sig(Void.TYPE), null, null));
        method.start();
        method.aload(0);
        method.invokespecial(cg.p(Object.class), "<init>", cg.sig(Void.TYPE));
        method.voidreturn();
        method.end();
    }

    private void createClassInit() {
        ClassVisitor cv = getClassVisitor();

        cv.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, "$isClassLoaded", cg.ci(Boolean.TYPE), null, Boolean.FALSE);
        cv.visitField(ACC_STATIC | ACC_PRIVATE | ACC_FINAL, "$class", cg.ci(Class.class), null, null);

        SkinnyMethodAdapter method = new SkinnyMethodAdapter(cv.visitMethod(ACC_PUBLIC, "<clinit>", cg.sig(Void.TYPE), null, null));
        method.start();

        // This is a little hacky...since clinit recurses, set a boolean so we don't continue trying to load class
        method.getstatic(classname, "$isClassLoaded", cg.ci(Boolean.TYPE));
        Label doNotLoadClass = new Label();
        method.ifne(doNotLoadClass);

        method.ldc(Boolean.TRUE);
        method.putstatic(classname, "$isClassLoaded", cg.ci(Boolean.TYPE));
        method.ldc(cg.c(classname));
        method.invokestatic(cg.p(Class.class), "forName", cg.sig(Class.class, cg.params(String.class)));
        method.putstatic(classname, "$class", cg.ci(Class.class));

        method.label(doNotLoadClass);
        method.voidreturn();
        method.end();
    }
    
    public MethodCompiler startMethod(String friendlyName, ClosureCallback args, StaticScope scope, ASTInspector inspector) {
        ASMMethodCompiler methodCompiler = new ASMMethodCompiler(friendlyName, inspector);
        
        methodCompiler.beginMethod(args, scope);
        
        return methodCompiler;
    }

    public abstract class AbstractMethodCompiler implements MethodCompiler {
        protected SkinnyMethodAdapter method;
        protected VariableCompiler variableCompiler;
        protected InvocationCompiler invocationCompiler;
        
        protected Label[] currentLoopLabels;
        
        // The current local variable count, to use for temporary locals during processing
        protected int localVariable = NIL_INDEX + 1;

        public abstract void beginMethod(ClosureCallback args, StaticScope scope);

        public abstract void endMethod();

        public void lineNumber(ISourcePosition position) {
            Label line = new Label();
            method.label(line);
            method.visitLineNumber(position.getStartLine() + 1, line);
        }

        public void loadThreadContext() {
            method.aload(THREADCONTEXT_INDEX);
        }

        public void loadClosure() {
            loadThreadContext();
            invokeThreadContext("getFrameBlock", cg.sig(Block.class));
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
            loadRuntime();
            invokeIRuby("getNil", cg.sig(IRubyObject.class));
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
            method.invokestatic(cg.p(CompilerHelpers.class), methodName, signature);
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
            invokeIRubyObject("getMetaClass", cg.sig(RubyClass.class));
        }
        
        public VariableCompiler getVariableCompiler() {
            return variableCompiler;
        }
        
        public InvocationCompiler getInvocationCompiler() {
            return invocationCompiler;
        }

        public void assignLocalVariableBlockArg(int argIndex, int varIndex) {
            // this is copying values, but it would be more efficient to just use the args in-place
            method.aload(DYNAMIC_SCOPE_INDEX);
            method.ldc(new Integer(varIndex));
            method.aload(ARGS_INDEX);
            method.ldc(new Integer(argIndex));
            method.arrayload();
            method.iconst_0();
            method.invokevirtual(cg.p(DynamicScope.class), "setValue", cg.sig(Void.TYPE, cg.params(Integer.TYPE, IRubyObject.class, Integer.TYPE)));
        }

        public void assignLocalVariableBlockArg(int argIndex, int varIndex, int depth) {
            if (depth == 0) {
                assignLocalVariableBlockArg(argIndex, varIndex);
                return;
            }

            method.aload(DYNAMIC_SCOPE_INDEX);
            method.ldc(new Integer(varIndex));
            method.aload(ARGS_INDEX);
            method.ldc(new Integer(argIndex));
            method.arrayload();
            method.ldc(new Integer(depth));
            method.invokevirtual(cg.p(DynamicScope.class), "setValue", cg.sig(Void.TYPE, cg.params(Integer.TYPE, IRubyObject.class, Integer.TYPE)));
        }

        public void assignConstantInCurrent(String name) {
            loadThreadContext();
            method.ldc(name);
            method.dup2_x1();
            method.pop2();
            invokeThreadContext("setConstantInCurrent", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void assignConstantInModule(String name) {
            loadThreadContext();
            method.ldc(name);
            method.swap2();
            invokeThreadContext("setConstantInCurrent", cg.sig(IRubyObject.class, cg.params(String.class, RubyModule.class, IRubyObject.class)));
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
            method.invokevirtual(cg.p(RubyModule.class), "getConstantFrom", cg.sig(IRubyObject.class, cg.params(String.class)));
        }

        public void retrieveClassVariable(String name) {
            loadThreadContext();
            loadRuntime();
            loadSelf();
            method.ldc(name);

            invokeUtilityMethod("fetchClassVariable", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class)));
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

            invokeUtilityMethod("setClassVariable", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, Ruby.class, IRubyObject.class, String.class, IRubyObject.class)));
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

        public void createNewString(ByteList value) {
            // FIXME: this is sub-optimal, storing string value in a java.lang.String again
            loadRuntime();
            method.ldc(value.toString());

            invokeIRuby("newString", cg.sig(RubyString.class, cg.params(String.class)));
        }

        public void createNewSymbol(String name) {
            loadRuntime();
            method.ldc(name);
            invokeIRuby("newSymbol", cg.sig(RubySymbol.class, cg.params(String.class)));
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
                invokeUtilityMethod("createObjectArray", cg.sig(IRubyObject[].class, params));
            } else {
                // This is pretty inefficient for building an array, so just raise an error if someone's using it for a lot of elements
                throw new NotCompilableException("Don't use createObjectArray(int) for more than 5 elements");
            }
        }

        private void buildObjectArray(String type, Object[] sourceArray, ArrayCallback callback) {
            method.ldc(new Integer(sourceArray.length));
            method.anewarray(type);

            for (int i = 0; i < sourceArray.length; i++) {
                method.dup();
                method.ldc(new Integer(i));

                callback.nextValue(this, sourceArray, i);

                method.arraystore();
            }
        }

        public void createEmptyHash() {
            loadRuntime();

            method.invokestatic(cg.p(RubyHash.class), "newHash", cg.sig(RubyHash.class, cg.params(Ruby.class)));
        }

        public void createNewHash(Object elements, ArrayCallback callback, int keyCount) {
            loadRuntime();

            // create a new hashmap
            method.newobj(cg.p(HashMap.class));
            method.dup();
            method.invokespecial(cg.p(HashMap.class), "<init>", cg.sig(Void.TYPE));

            for (int i = 0; i < keyCount; i++) {
                method.dup();
                callback.nextValue(this, elements, i);
                method.invokevirtual(cg.p(HashMap.class), "put", cg.sig(Object.class, cg.params(Object.class, Object.class)));
                method.pop();
            }

            loadNil();
            method.invokestatic(cg.p(RubyHash.class), "newHash", cg.sig(RubyHash.class, cg.params(Ruby.class, Map.class, IRubyObject.class)));
        }

        public void createNewRange(boolean isExclusive) {
            loadRuntime();

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
            Label afterJmp = new Label();
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
            Label afterJmp = new Label();
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
            //Label tryBegin = new Label();
            //Label tryEnd = new Label();
            //Label tryCatch = new Label();
            //method.trycatch(tryBegin, tryEnd, tryCatch, cg.p(JumpException.class));
            //method.label(tryBegin);
            {
                Label condJmp = new Label();
                Label topJmp = new Label();
                Label endJmp = new Label();
                
                Label[] oldLoopLabels = currentLoopLabels;
                
                currentLoopLabels = new Label[] {condJmp, topJmp, endJmp};
                
                // start loop off with nil
                loadNil();
                
                if (checkFirst) {
                    // calculate condition
                    condition.branch(this);
                    // call isTrue on the result
                    isTrue();

                    method.ifeq(endJmp); // EQ == 0 (i.e. false)
                }

                method.label(topJmp);

                body.branch(this);

                // clear result after each successful loop, resetting to nil
                method.pop();
                
                // check the condition
                method.label(condJmp);
                condition.branch(this);
                
                // call isTrue on the result
                isTrue();

                method.ifne(topJmp); // NE == nonzero (i.e. true)
                
                // exiting loop normally, leave nil on the stack
                
                method.label(endJmp);
                
                currentLoopLabels = oldLoopLabels;
            }

            //method.label(tryEnd);
            // no physical break, terminate loop and skip catch block
//        Label normalBreak = new Label();
//        method.go_to(normalBreak);
//
//        method.label(tryCatch);
//        {
//            method.dup();
//            method.invokevirtual(cg.p(JumpException.class), "getJumpType", cg.sig(JumpException.JumpType.class));
//            method.invokevirtual(cg.p(JumpException.JumpType.class), "getTypeId", cg.sig(Integer.TYPE));
//
//            Label tryDefault = new Label();
//            Label breakLabel = new Label();
//
//            method.lookupswitch(tryDefault, new int[] {JumpException.JumpType.BREAK}, new Label[] {breakLabel});
//
//            // default is to just re-throw unhandled exception
//            method.label(tryDefault);
//            method.athrow();
//
//            // break just terminates the loop normally, unless it's a block break...
//            method.label(breakLabel);
//
//            // JRUBY-530 behavior
//            method.dup();
//            method.invokevirtual(cg.p(JumpException.class), "getTarget", cg.sig(Object.class));
//            loadClosure();
//            Label notBlockBreak = new Label();
//            method.if_acmpne(notBlockBreak);
//            method.dup();
//            method.aconst_null();
//            method.invokevirtual(cg.p(JumpException.class), "setTarget", cg.sig(Void.TYPE, cg.params(Object.class)));
//            method.athrow();
//
//            method.label(notBlockBreak);
//            // target is not == closure, normal loop exit, pop remaining exception object
//            method.pop();
//        }
//
//        method.label(normalBreak);
        }

        public void createNewClosure(StaticScope scope, int arity, ClosureCallback body, ClosureCallback args) {
            String closureMethodName = "closure" + ++innerIndex;
            String closureFieldName = "_" + closureMethodName;
            
            ASMClosureCompiler closureCompiler = new ASMClosureCompiler(closureMethodName, closureFieldName);
            
            closureCompiler.beginMethod(args, scope);
            
            body.compile(closureCompiler);
            
            closureCompiler.endMethod();

            // Done with closure compilation
            /////////////////////////////////////////////////////////////////////////////
            // Now, store a compiled block object somewhere we can access it in the future
            // in current method, load the field to see if we've created a BlockCallback yet
            method.getstatic(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated);

            // no callback, construct it
            getCallbackFactory();

            method.ldc(closureMethodName);
            method.invokevirtual(cg.p(CallbackFactory.class), "getBlockCallback", cg.sig(CompiledBlockCallback.class, cg.params(String.class)));
            method.putstatic(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));

            method.label(alreadyCreated);

            // Construct the block for passing to the target method
            loadThreadContext();
            loadSelf();
            method.ldc(new Integer(arity));

            buildStaticScopeNames(method, scope);

            method.getstatic(classname, closureFieldName, cg.ci(CompiledBlockCallback.class));

            invokeUtilityMethod("createBlock", cg.sig(CompiledBlock.class, cg.params(ThreadContext.class, IRubyObject.class, Integer.TYPE, String[].class, CompiledBlockCallback.class)));
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

        private void getCallbackFactory() {
            loadRuntime();
            getCompiledClass();
            method.dup();
            method.invokevirtual(cg.p(Class.class), "getClassLoader", cg.sig(ClassLoader.class));
            method.invokestatic(cg.p(CallbackFactory.class), "createFactory", cg.sig(CallbackFactory.class, cg.params(Ruby.class, Class.class, ClassLoader.class)));
        }

        public void getCompiledClass() {
            method.getstatic(classname, "$class", cg.ci(Class.class));
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
            getRubyClass();
            method.ldc(newName);
            method.ldc(oldName);
            method.invokevirtual(cg.p(RubyModule.class), "defineAlias", cg.sig(Void.TYPE, cg.params(String.class, String.class)));
            loadNil();
            // TODO: should call method_added, and possibly push nil.
        }

        public void loadFalse() {
            loadRuntime();
            invokeIRuby("getFalse", cg.sig(RubyBoolean.class));
        }

        public void loadTrue() {
            loadRuntime();
            invokeIRuby("getTrue", cg.sig(RubyBoolean.class));
        }

        public void retrieveInstanceVariable(String name) {
            loadSelf();

            method.ldc(name);
            invokeIRubyObject("getInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class)));

            // check if it's null; if so, load nil
            method.dup();
            Label notNull = new Label();
            method.ifnonnull(notNull);

            // pop the dup'ed null
            method.pop();
            // replace it with nil
            loadNil();

            method.label(notNull);
        }

        public void assignInstanceVariable(String name) {
            loadSelf();
            method.swap();

            method.ldc(name);
            method.swap();

            invokeIRubyObject("setInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void assignInstanceVariableBlockArg(int argIndex, String name) {
            loadSelf();
            method.ldc(name);

            method.aload(ARGS_INDEX);
            method.ldc(new Integer(argIndex));
            method.arrayload();

            invokeIRubyObject("setInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void retrieveGlobalVariable(String name) {
            loadRuntime();

            invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
            method.ldc(name);
            method.invokevirtual(cg.p(GlobalVariables.class), "get", cg.sig(IRubyObject.class, cg.params(String.class)));
        }

        public void assignGlobalVariable(String name) {
            loadRuntime();

            invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
            method.swap();
            method.ldc(name);
            method.swap();
            method.invokevirtual(cg.p(GlobalVariables.class), "set", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void assignGlobalVariableBlockArg(int argIndex, String name) {
            loadRuntime();

            invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
            method.ldc(name);

            method.aload(ARGS_INDEX);
            method.ldc(new Integer(argIndex));
            method.arrayload();

            method.invokevirtual(cg.p(GlobalVariables.class), "set", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
        }

        public void negateCurrentValue() {
            isTrue();
            Label isTrue = new Label();
            Label end = new Label();
            method.ifne(isTrue);
            loadTrue();
            method.go_to(end);
            method.label(isTrue);
            loadFalse();
            method.label(end);
        }

        public void splatCurrentValue() {
            method.invokestatic(cg.p(EvaluationState.class), "splatValue", cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
        }

        public void singlifySplattedValue() {
            method.invokestatic(cg.p(EvaluationState.class), "aValueSplat", cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
        }

        public void ensureRubyArray() {
            invokeUtilityMethod("ensureRubyArray", cg.sig(RubyArray.class, cg.params(IRubyObject.class)));
        }

        public void forEachInValueArray(int start, int count, Object source, ArrayCallback callback) {
            Label noMoreArrayElements = new Label();
            for (; start < count; start++) {
                // confirm we're not past the end of the array
                method.dup(); // dup the original array object
                method.invokevirtual(cg.p(RubyArray.class), "getLength", cg.sig(Integer.TYPE, cg.params()));
                method.ldc(new Integer(start));
                method.ifle(noMoreArrayElements); // if length <= start, end loop
                // extract item from array
                method.dup(); // dup the original array object
                method.ldc(new Integer(start)); // index for the item
                method.invokevirtual(cg.p(RubyArray.class), "entry", cg.sig(IRubyObject.class, cg.params(Long.TYPE))); // extract item
                callback.nextValue(this, source, start);
            }
            method.label(noMoreArrayElements);
        }

        public void loadInteger(int value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void performGEBranch(BranchCallback trueBranch, BranchCallback falseBranch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void performGTBranch(BranchCallback trueBranch, BranchCallback falseBranch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void performLEBranch(BranchCallback trueBranch, BranchCallback falseBranch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void performLTBranch(BranchCallback trueBranch, BranchCallback falseBranch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void loadRubyArraySize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void asString() {
            method.invokeinterface(cg.p(IRubyObject.class), "asString", cg.sig(RubyString.class, cg.params()));
        }

        public void nthRef(int match) {
            method.ldc(new Integer(match));
            loadThreadContext();
            invokeThreadContext("getBackref", cg.sig(IRubyObject.class, cg.params()));
            method.invokestatic(cg.p(RubyRegexp.class), "nth_match", cg.sig(IRubyObject.class, cg.params(Integer.TYPE, IRubyObject.class)));
        }

        public void match() {
            method.invokevirtual(cg.p(RubyRegexp.class), "match2", cg.sig(IRubyObject.class, cg.params()));
        }

        public void match2() {
            method.invokevirtual(cg.p(RubyRegexp.class), "match", cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
        }

        public void match3() {
            method.dup();
            method.instance_of(cg.p(RubyString.class));

            Label l0 = new Label();
            method.ifeq(l0);

            method.invokevirtual(cg.p(RubyRegexp.class), "match", cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));

            Label l1 = new Label();
            method.go_to(l1);
            method.label(l0);

            method.swap();
            loadThreadContext();
            method.swap();
            method.ldc("=~");
            method.swap();

            method.invokeinterface(cg.p(IRubyObject.class), "callMethod", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, String.class, IRubyObject.class)));
            method.label(l1);
        }

        public void createNewRegexp(final ByteList value, final int options, final String lang) {
            String regname = getNewConstant(cg.ci(RubyRegexp.class), "literal_reg_");
            String name = getNewConstant(cg.ci(Pattern.class), "literal_re_");
            String name_flags = getNewConstant(cg.ci(Integer.TYPE), "literal_re_flags_");

            // in current method, load the field to see if we've created a Pattern yet
            method.getstatic(classname, name, cg.ci(Pattern.class));


            Label alreadyCreated = new Label();
            method.ifnonnull(alreadyCreated);

            loadRuntime();

            // load string, for Regexp#source and Regexp#inspect
            String regexpString = null;
            if ((options & ReOptions.RE_UNICODE) > 0) {
                regexpString = value.toUtf8String();
            } else {
                regexpString = value.toString();
            }
            method.ldc(regexpString);

            method.ldc(new Integer(options));
            invokeUtilityMethod("regexpLiteralFlags", cg.sig(Integer.TYPE, cg.params(Integer.TYPE)));
            method.putstatic(classname, name_flags, cg.ci(Integer.TYPE));

            loadRuntime();
            method.ldc(regexpString);
            method.ldc(new Integer(options));
            invokeUtilityMethod("regexpLiteral", cg.sig(Pattern.class, cg.params(Ruby.class, String.class, Integer.TYPE)));
            method.dup();

            method.putstatic(classname, name, cg.ci(Pattern.class));

            method.getstatic(classname, name_flags, cg.ci(Integer.TYPE));
            if (null == lang) {
                method.aconst_null();
            } else {
                method.ldc(lang);
            }

            method.invokestatic(cg.p(RubyRegexp.class), "newRegexp", cg.sig(RubyRegexp.class, cg.params(Ruby.class, String.class, Pattern.class, Integer.TYPE, String.class)));

            method.putstatic(classname, regname, cg.ci(RubyRegexp.class));
            method.label(alreadyCreated);
            method.getstatic(classname, regname, cg.ci(RubyRegexp.class));
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

        public void branchIfModule(ClosureCallback receiverCallback, BranchCallback moduleCallback, BranchCallback notModuleCallback) {
            receiverCallback.compile(this);
            method.instance_of(cg.p(RubyModule.class));

            Label falseJmp = new Label();
            Label afterJmp = new Label();

            method.ifeq(falseJmp); // EQ == 0 (i.e. false)
            moduleCallback.branch(this);

            method.go_to(afterJmp);
            method.label(falseJmp);

            notModuleCallback.branch(this);

            method.label(afterJmp);
        }

        public void backref() {
            invokeThreadContext("getBackref", cg.sig(IRubyObject.class, cg.params()));
        }

        public void backrefMethod(String methodName) {
            invokeThreadContext("getBackref", cg.sig(IRubyObject.class, cg.params()));
            method.invokestatic(cg.p(RubyRegexp.class), methodName, cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
        }
        
        public void issueLoopBreak() {
            // inside a loop, break out of it
            method.swap();
            method.pop();

            // go to end of loop, leaving break value on stack
            method.go_to(currentLoopLabels[2]);
        }
        
        public void issueLoopNext() {
            // inside a loop, pop result and jump to conditional
            method.pop();

            // go to condition
            method.go_to(currentLoopLabels[0]);
        }
        
        public void issueLoopRedo() {
            // inside a loop, jump to body
            method.go_to(currentLoopLabels[1]);
        }
    }

    public class ASMClosureCompiler extends AbstractMethodCompiler {
        private String closureMethodName;
        private Label startScope;
        
        public ASMClosureCompiler(String closureMethodName, String closureFieldName) {
            this.closureMethodName = closureMethodName;

            // declare the field
            getClassVisitor().visitField(ACC_PRIVATE | ACC_STATIC, closureFieldName, cg.ci(CompiledBlockCallback.class), null, null);
            
            method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_STATIC, closureMethodName, CLOSURE_SIGNATURE, null, null));
            variableCompiler = new HeapBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX);
            invocationCompiler = new StandardInvocationCompiler(this, method);
        }

        public void beginMethod(ClosureCallback args, StaticScope scope) {
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

            // start of scoping for closure's vars
            startScope = new Label();
            method.label(startScope);
        }

        public void endMethod() {
            method.areturn();

            // end of scoping for closure's vars
            Label end = new Label();
            method.label(end);
            method.end();
        }

        public void performReturn() {
            throw new NotCompilableException("Can\'t compile non-local return");
        }

        public void defineNewMethod(String name, StaticScope scope, ClosureCallback body, ClosureCallback args, ASTInspector inspector) {
            throw new NotCompilableException("Can\'t compile def within closure yet");
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
        
        public void issueBreakEvent() {
            if (currentLoopLabels != null) {
                issueLoopBreak();
            } else {
                throw new NotCompilableException("Can't compile non-local break yet");
            }
            // needs to be rewritten for new jump exceptions
//        method.newobj(cg.p(JumpException.BreakJump.class));
//        method.dup();
//        method.dup_x2();
//        method.invokespecial(cg.p(JumpException.class), "<init>", cg.sig(Void.TYPE, cg.params(JumpException.JumpType.class)));
//
//        // set result into jump exception
//        method.dup_x1();
//        method.swap();
//        method.invokevirtual(cg.p(JumpException.class), "setValue", cg.sig(Void.TYPE, cg.params(Object.class)));
//
//        method.athrow();
        }

        public void issueNextEvent() {
            if (currentLoopLabels != null) {
                issueLoopNext();
            } else {
                throw new NotCompilableException("Can't compile non-local next yet");
            }
        }

        public void issueRedoEvent() {
            if (currentLoopLabels != null) {
                issueLoopRedo();
            } else {
                throw new NotCompilableException("Can't compile non-local next yet");
            }
        }
    }

    public class ASMMethodCompiler extends AbstractMethodCompiler {
        private String friendlyName;
        private Label scopeStart;

        public ASMMethodCompiler(String friendlyName, ASTInspector inspector) {
            this.friendlyName = friendlyName;

            method = new SkinnyMethodAdapter(getClassVisitor().visitMethod(ACC_PUBLIC | ACC_STATIC, friendlyName, METHOD_SIGNATURE, null, null));
            if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                variableCompiler = new HeapBasedVariableCompiler(this, method, DYNAMIC_SCOPE_INDEX, VARS_ARRAY_INDEX, ARGS_INDEX);
            } else {
                variableCompiler = new StackBasedVariableCompiler(this, method, ARGS_INDEX);
            }
            invocationCompiler = new StandardInvocationCompiler(this, method);
        }

        public void beginMethod(ClosureCallback args, StaticScope scope) {
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

        public void endMethod() {
            // return last value from execution
            method.areturn();

            // end of variable scope
            Label end = new Label();
            method.label(end);

            // local variable for lvars array
            method.visitLocalVariable("lvars", cg.ci(IRubyObject[].class), null, scopeStart, end, DYNAMIC_SCOPE_INDEX);

            method.end();
        }

        public void defineNewMethod(String name, StaticScope scope, ClosureCallback body, ClosureCallback args, ASTInspector inspector) {
            // TODO: build arg list based on number of args, optionals, etc
            ++methodIndex;
            String methodName = cg.cleanJavaIdentifier(name) + "__" + methodIndex;

            MethodCompiler methodCompiler = startMethod(methodName, args, scope, inspector);

            // callbacks to fill in method body
            body.compile(methodCompiler);

            methodCompiler.endMethod();

            // prepare to call "def" utility method to handle def logic
            loadThreadContext();

            loadSelf();

            // load the class we're creating, for binding purposes
            getCompiledClass();

            method.ldc(name);

            method.ldc(methodName);

            buildStaticScopeNames(method, scope);

            method.ldc(new Integer(0));
            
            if (inspector.hasClosure() || inspector.hasScopeAwareMethods()) {
                method.getstatic(cg.p(CallConfiguration.class), "RUBY_FULL", cg.ci(CallConfiguration.class));
            } else {
                method.getstatic(cg.p(CallConfiguration.class), "JAVA_FULL", cg.ci(CallConfiguration.class));
            }
            
            invokeUtilityMethod("def", cg.sig(IRubyObject.class, 
                    cg.params(ThreadContext.class, IRubyObject.class, Class.class, String.class, String.class, String[].class, Integer.TYPE, CallConfiguration.class)));
        }
        
        public void performReturn() {
            // normal return for method body
            method.areturn();
        }

        public void issueBreakEvent() {
            if (currentLoopLabels != null) {
                issueLoopBreak();
            } else {
                // in method body with no containing loop, issue jump error
                
                // load runtime under break value
                loadRuntime();
                method.swap();
                
                // load "break" jump error type under value
                method.ldc("break");
                method.swap();
                
                // load break jump error message
                method.ldc("unexpected break");
                
                // create and raise local jump error
                invokeIRuby("newLocalJumpError", cg.sig(RaiseException.class, cg.params(String.class, IRubyObject.class, String.class)));
                method.athrow();
            }
        }

        public void issueNextEvent() {
            if (currentLoopLabels != null) {
                issueLoopNext();
            } else {
                // in method body with no containing loop, issue jump error
                
                // load runtime under next value
                loadRuntime();
                method.swap();
                
                // load "next" jump error type under value
                method.ldc("next");
                method.swap();
                
                // load next jump error message
                method.ldc("unexpected next");
                
                // create and raise local jump error
                invokeIRuby("newLocalJumpError", cg.sig(RaiseException.class, cg.params(String.class, IRubyObject.class, String.class)));
                method.athrow();
            }
        }

        public void issueRedoEvent() {
            if (currentLoopLabels != null) {
                issueLoopRedo();
            } else {
                // in method body with no containing loop, issue jump error
                
                // load runtime
                loadRuntime();
                
                // load "redo" jump error type
                method.ldc("redo");
                
                loadNil();
                
                // load break jump error message
                method.ldc("unexpected redo");
                
                // create and raise local jump error
                invokeIRuby("newLocalJumpError", cg.sig(RaiseException.class, cg.params(String.class, IRubyObject.class, String.class)));
                method.athrow();
            }
        }
    }

    private int constants = 0;

    private String getNewConstant(String type, String name_prefix) {
        ClassVisitor cv = getClassVisitor();

        String realName;
        synchronized (this) {
            realName = name_prefix + constants++;
        }

        // declare the field
        cv.visitField(ACC_PRIVATE | ACC_STATIC, realName, type, null, null).visitEnd();
        return realName;
    }

    public void defineClass(String name, StaticScope staticScope, ClosureCallback superCallback, ClosureCallback pathCallback, ClosureCallback bodyCallback) {
        // TODO: build arg list based on number of args, optionals, etc
        ++methodIndex;
        String methodName = "rubyclass__" + cg.cleanJavaIdentifier(name) + "__" + methodIndex;

        /* incomplete...worked with old method stuff, but needs to be reworked and refactored
        MethodCompiler methodCompiler = new ASMMethodCompiler(methodName);
        methodCompiler.beginMethod(null);

        // class def bodies default to public visibility
        method.getstatic(cg.p(Visibility.class), "PUBLIC", cg.ci(Visibility.class));
        method.astore(VISIBILITY_INDEX);

        // Here starts the logic for the class definition
        loadRuntime();

        superCallback.compile(this);

        invokeUtilityMethod("prepareSuperClass", cg.sig(RubyClass.class, cg.params(Ruby.class, IRubyObject.class)));

        loadThreadContext();

        pathCallback.compile(this);

        invokeUtilityMethod("prepareClassNamespace", cg.sig(RubyModule.class, cg.params(ThreadContext.class, IRubyObject.class)));

        method.swap();

        method.ldc(name);

        method.swap();

        method.invokevirtual(cg.p(RubyModule.class), "defineOrGetClassUnder", cg.sig(RubyClass.class, cg.params(String.class, RubyClass.class)));

        // set self to the class
        method.dup();
        method.astore(SELF_INDEX);

        // CLASS BODY
        loadThreadContext();
        method.swap();

        // FIXME: this should be in a try/finally
        invokeThreadContext("preCompiledClass", cg.sig(Void.TYPE, cg.params(RubyModule.class)));

        bodyCallback.compile(this);

        loadThreadContext();
        invokeThreadContext("postCompiledClass", cg.sig(Void.TYPE, cg.params()));

        endMethod(mv);

        // return to previous method
        mv = getMethodAdapter();

        // prepare to call class definition method
        loadThreadContext();
        loadSelf();
        method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
        method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));

        method.invokestatic(classname, methodName, METHOD_SIGNATURE);*/
    }

    public void defineModule(String name, StaticScope staticScope, ClosureCallback pathCallback, ClosureCallback bodyCallback) {
        // TODO: build arg list based on number of args, optionals, etc
        ++methodIndex;
        String methodName = "rubymodule__" + cg.cleanJavaIdentifier(name) + "__" + methodIndex;

        /* incomplete, needs to be reworked and refactored
        beginMethod(methodName, null);

        SkinnyMethodAdapter mv = getMethodAdapter();

        // module def bodies default to public visibility
        method.getstatic(cg.p(Visibility.class), "PUBLIC", cg.ci(Visibility.class));
        method.astore(VISIBILITY_INDEX);

        // Here starts the logic for the module definition
        loadThreadContext();

        pathCallback.compile(this);

        invokeUtilityMethod("prepareClassNamespace", cg.sig(RubyModule.class, cg.params(ThreadContext.class, IRubyObject.class)));

        method.ldc(name);

        method.invokevirtual(cg.p(RubyModule.class), "defineModuleUnder", cg.sig(RubyModule.class, cg.params(String.class)));

        // set self to the module
        method.dup();
        method.astore(SELF_INDEX);

        // MODULE BODY
        loadThreadContext();
        method.swap();

        // FIXME: this should be in a try/finally
        invokeThreadContext("preCompiledClass", cg.sig(Void.TYPE, cg.params(RubyModule.class)));

        bodyCallback.compile(this);

        loadThreadContext();
        invokeThreadContext("postCompiledClass", cg.sig(Void.TYPE, cg.params()));

        endMethod(mv);

        // return to previous method
        mv = getMethodAdapter();

        // prepare to call class definition method
        loadThreadContext();
        loadSelf();
        method.getstatic(cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
        method.getstatic(cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));

        method.invokestatic(classname, methodName, METHOD_SIGNATURE);*/
    }
}
