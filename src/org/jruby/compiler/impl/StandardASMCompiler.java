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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.jruby.Ruby;
import org.jruby.MetaClass;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.Node;
import org.jruby.ast.executable.Script;
import org.jruby.compiler.*;
import org.jruby.compiler.Compiler;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlock;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.collections.SinglyLinkedList;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author headius
 */
public class StandardASMCompiler implements Compiler, Opcodes {
    private static final CodegenUtils cg = CodegenUtils.instance;
    private static final String THREADCONTEXT = cg.p(ThreadContext.class);
    private static final String RUBY = cg.p(Ruby.class);
    private static final String IRUBYOBJECT = cg.p(IRubyObject.class);
    
    private static final String METHOD_SIGNATURE =
            cg.sig(IRubyObject.class, new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class });
    private static final String CLOSURE_SIGNATURE =
            cg.sig(IRubyObject.class, new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class, IRubyObject[][].class });
    
    private static final int THREADCONTEXT_INDEX = 0;
    private static final int SELF_INDEX = 1;
    private static final int ARGS_INDEX = 2;
    private static final int CLOSURE_INDEX = 3;
    private static final int SCOPE_INDEX = 4;
    private static final int RUNTIME_INDEX = 5;
    private static final int LOCAL_VARS_INDEX = 6;
    
    private Stack methodVisitors = new Stack();
    private Stack arities = new Stack();
    private Stack scopeStarts = new Stack();
    
    private String classname;
    private String sourcename;
    
    //Map classWriters = new HashMacg.p();
    private ClassWriter classWriter;
    ClassWriter currentMultiStub = null;
    int methodIndex = -1;
    int multiStubCount = -1;
    int innerIndex = -1;
    
    int lastLine = -1;
    
    /** Creates a new instance of StandardCompilerContext */
    public StandardASMCompiler(String classname, String sourcename) {
        this.classname = classname;
        this.sourcename = sourcename;
    }
    
    public StandardASMCompiler(Node node) {
        // determine new class name based on filename of incoming node
        // must generate unique classnames for evals, since they could be generated many times in a given run
        classname = "EVAL" + hashCode();
        sourcename = "EVAL" + hashCode();
    }
    
    public Class loadClass(Ruby runtime) throws ClassNotFoundException {
        JRubyClassLoader jcl = runtime.getJRubyClassLoader();
        
        jcl.defineClass(cg.c(classname), classWriter.toByteArray());
        
        return jcl.loadClass(cg.c(classname));
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
    
    public MethodVisitor getMethodVisitor() {
        return (MethodVisitor)methodVisitors.peek();
    }
    
    public MethodVisitor popMethodVisitor() {
        return (MethodVisitor)methodVisitors.pop();
    }
    
    public void pushMethodVisitor(MethodVisitor mv) {
        methodVisitors.push(mv);
    }
    
    public int getArity() {
        return ((Integer)arities.peek()).intValue();
    }
    
    public void pushArity(int arity) {
        arities.push(new Integer(arity));
    }
    
    public int popArity() {
        return ((Integer)arities.pop()).intValue();
    }
    
    public void pushScopeStart(Label start) {
        scopeStarts.push(start);
    }
    
    public Label popScopeStart() {
        return (Label)scopeStarts.pop();
    }
    
    public void startScript() {
        classWriter = new ClassWriter(true);
        
        // Create the class with the appropriate class name and source file
        classWriter.visit(V1_2, ACC_PUBLIC + ACC_SUPER, classname, null, cg.p(Object.class), new String[] {cg.p(Script.class)});
        classWriter.visitSource(sourcename, null);
        
        createConstructor();
    }
    
    public void endScript() {
        // add Script#run impl, used for running this script with a specified threadcontext and self
        // root method of a script is always in __load__ method
        String methodName = "__file__";
        MethodVisitor mv = getClassVisitor().visitMethod(ACC_PUBLIC, "run", METHOD_SIGNATURE, null, null);
        mv.visitCode();
        
        // invoke __file__ with threadcontext, self, args (null), and block (null)
        // These are all +1 because run is an instance method where others are static
        mv.visitVarInsn(ALOAD, THREADCONTEXT_INDEX + 1);
        mv.visitVarInsn(ALOAD, SELF_INDEX + 1);
        mv.visitVarInsn(ALOAD, ARGS_INDEX + 1);
        mv.visitVarInsn(ALOAD, CLOSURE_INDEX + 1);
        
        mv.visitMethodInsn(INVOKESTATIC, classname, methodName, METHOD_SIGNATURE);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
        // add main impl, used for detached or command-line execution of this script with a new runtime
        // root method of a script is always in stub0, method0
        mv = getClassVisitor().visitMethod(ACC_PUBLIC | ACC_STATIC, "main", cg.sig(Void.TYPE, cg.params(String[].class)), null, null);
        mv.visitCode();
        
        // new instance to invoke run against
        mv.visitTypeInsn(NEW, classname);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, classname, "<init>", cg.sig(Void.TYPE));
        
        // invoke run with threadcontext and topself
        mv.visitMethodInsn(INVOKESTATIC, cg.p(Ruby.class), "getDefaultInstance", cg.sig(Ruby.class));
        mv.visitInsn(DUP);
        
        mv.visitMethodInsn(INVOKEVIRTUAL, RUBY, "getCurrentContext", cg.sig(ThreadContext.class));
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, RUBY, "getTopSelf", cg.sig(IRubyObject.class));
        mv.visitFieldInsn(GETSTATIC, cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
        mv.visitFieldInsn(GETSTATIC, cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));
        
        mv.visitMethodInsn(INVOKEVIRTUAL, classname, "run", METHOD_SIGNATURE);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    private void createConstructor() {
        ClassVisitor cv = getClassVisitor();
        
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", cg.sig(Void.TYPE), null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, cg.p(Object.class), "<init>",
                cg.sig(Void.TYPE));
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    public Object beginMethod(String friendlyName, int arity, int localVarCount) {
        MethodVisitor newMethod = getClassVisitor().visitMethod(ACC_PUBLIC | ACC_STATIC, friendlyName, METHOD_SIGNATURE, null, null);
        pushMethodVisitor(newMethod);
        
        newMethod.visitCode();
        
        // set up a local IRuby variable
        newMethod.visitVarInsn(ALOAD, THREADCONTEXT_INDEX);
        invokeThreadContext("getRuntime", cg.sig(Ruby.class));
        newMethod.visitVarInsn(ASTORE, RUNTIME_INDEX);
        
        // check arity
        newMethod.visitLdcInsn(new Integer(arity));
        newMethod.visitMethodInsn(INVOKESTATIC, cg.p(Arity.class), "createArity", cg.sig(Arity.class, cg.params(Integer.TYPE)));
        loadRuntime();
        newMethod.visitVarInsn(ALOAD, ARGS_INDEX);
        newMethod.visitMethodInsn(INVOKEVIRTUAL, cg.p(Arity.class), "checkArity", cg.sig(Void.TYPE, cg.params(Ruby.class, IRubyObject[].class)));
        
        // logic to start off the root node's code with local var slots and all
        newMethod.visitLdcInsn(new Integer(localVarCount));
        newMethod.visitTypeInsn(ANEWARRAY, cg.p(IRubyObject.class));
        
        // store the local vars in a local variable
        newMethod.visitVarInsn(ASTORE, LOCAL_VARS_INDEX);
        
        // arraycopy arguments into local vars array
        newMethod.visitVarInsn(ALOAD, ARGS_INDEX);
        newMethod.visitInsn(ICONST_0);
        newMethod.visitVarInsn(ALOAD, LOCAL_VARS_INDEX);
        newMethod.visitInsn(ICONST_2);
        newMethod.visitLdcInsn(new Integer(arity));
        
        newMethod.visitMethodInsn(INVOKESTATIC, cg.p(System.class), "arraycopy", cg.sig(Void.TYPE, cg.params(Object.class, Integer.TYPE, Object.class, Integer.TYPE, Integer.TYPE)));
        
        // put a null at register 4, for closure creation to know we're at top-level or local scope
        newMethod.visitInsn(ACONST_NULL);
        newMethod.visitVarInsn(ASTORE, SCOPE_INDEX);
        
        // visit a label to start scoping for local vars in this method
        Label start = new Label();
        newMethod.visitLabel(start);
        pushScopeStart(start);
        
        // push down the argument count of this method
        pushArity(arity);
        
        return newMethod;
    }
    
    public void endMethod(Object token) {
        assert token instanceof MethodVisitor;
        
        MethodVisitor mv = (MethodVisitor)token;
        // return last value from execution
        mv.visitInsn(ARETURN);
        
        // end of variable scope
        Label end = new Label();
        mv.visitLabel(end);
        
        // local variable for lvars array
        mv.visitLocalVariable("lvars", cg.ci(IRubyObject[].class), null, popScopeStart(), end, LOCAL_VARS_INDEX);
        
        mv.visitMaxs(1, 1); // automatically calculated by ASM
        mv.visitEnd();
        
        popMethodVisitor();
        popArity();
    }
    
    public void lineNumber(ISourcePosition position) {
        if (lastLine == (lastLine = position.getEndLine())) return; // did not change lines for this node, don't bother relabeling
        
        Label l = new Label();
        MethodVisitor mv = getMethodVisitor();
        mv.visitLabel(l);
        // line numbers are zero-based; add one for them to make sense in an editor
        mv.visitLineNumber(position.getStartLine() + 1, l);
    }
    
    public void invokeAttrAssign(String name) {
        MethodVisitor mv = getMethodVisitor();
        
        // get args[0] and stuff it under the receiver
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        mv.visitInsn(DUP_X2);
        mv.visitInsn(POP);
        
        // FIXME: I use VARIABLE here, but evaluator only does it if receiver == self...
        invokeDynamic(name, true, true, CallType.VARIABLE, null);
        
        // pop result, use args[0] captured above
        mv.visitInsn(POP);
    }
    
    public void invokeDynamic(String name, boolean hasReceiver, boolean hasArgs, CallType callType, ClosureCallback closureArg) {
        MethodVisitor mv = getMethodVisitor();
        String callSig = cg.sig(IRubyObject.class, cg.params(ThreadContext.class, String.class, IRubyObject[].class, IRubyObject.class, CallType.class, Block.class));
        String callSigIndexed = cg.sig(IRubyObject.class, cg.params(ThreadContext.class, Byte.TYPE, String.class, IRubyObject[].class, IRubyObject.class, CallType.class, Block.class));
        
        byte index = MethodIndex.getIndex(name);
        
        if (hasArgs) {
            if (hasReceiver) {
                // Call with args
                // receiver already present
                
                loadThreadContext();
                // put under args
                mv.visitInsn(SWAP);
            } else {
                // FCall
                // no receiver present, use self
                loadSelf();
                // put self under args
                mv.visitInsn(SWAP);
                
                loadThreadContext();
                // put under args
                mv.visitInsn(SWAP);
            }
            
            if (index != 0) {
                // load method index
                mv.visitLdcInsn(new Byte(index));
                // put under args
                mv.visitInsn(SWAP);
            }
            
            mv.visitLdcInsn(name);
            // put under args
            mv.visitInsn(SWAP);
        } else {
            if (hasReceiver) {
                // Call with no args
                // receiver already present
                
                loadThreadContext();
            } else {
                // VCall
                // no receiver present, use self
                loadSelf();
                
                loadThreadContext();
            }
            
            
            if (index != 0) {
                // load method index
                mv.visitLdcInsn(new Byte(index));
            }
            
            mv.visitLdcInsn(name);
            
            // empty args list
            mv.visitFieldInsn(GETSTATIC, cg.p(IRubyObject.class), "NULL_ARRAY", cg.ci(IRubyObject[].class));
        }
        
        // load self for visibility checks
        loadSelf();
        
        mv.visitFieldInsn(GETSTATIC, cg.p(CallType.class), callType.toString(), cg.ci(CallType.class));
        
        if (closureArg == null) {
            mv.visitFieldInsn(GETSTATIC, cg.p(Block.class), "NULL_BLOCK", cg.ci(Block.class));
        } else {
            closureArg.compile(this);
        }
        
        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label tryCatch = new Label();
        if (closureArg != null) {
            // wrap with try/catch for block flow-control exceptions
            mv.visitTryCatchBlock(tryBegin, tryEnd, tryCatch, cg.p(JumpException.class));

            mv.visitLabel(tryBegin);
        }
        
        if (index != 0) {
            invokeIRubyObject("compilerCallMethodWithIndex", callSigIndexed);
        } else {
            invokeIRubyObject("compilerCallMethod", callSig);
        }
        
        if (closureArg != null) {
            mv.visitLabel(tryEnd);

            // no physical break, terminate loop and skip catch block
            Label normalBreak = new Label();
            mv.visitJumpInsn(GOTO, normalBreak);

            mv.visitLabel(tryCatch);
            {
                // this logic is largely to handle the kernel "loop" behavior. See JRUBY-530.
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "getJumpType", cg.sig(JumpException.JumpType.class));
                mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.JumpType.class), "getTypeId", cg.sig(Integer.TYPE));

                Label tryDefault = new Label();
                Label breakLabel = new Label();

                mv.visitLookupSwitchInsn(tryDefault, new int[] {JumpException.JumpType.BREAK}, new Label[] {breakLabel});

                // default is to just re-throw unhandled exception
                mv.visitLabel(tryDefault);
                mv.visitInsn(ATHROW);

                // break just terminates the loop normally, unless it's a block break...
                mv.visitLabel(breakLabel);

                // Check if it's a break from the passed-in block
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "isBreakInKernelLoop", cg.sig(Boolean.TYPE));
                Label notBreakInKernelLoop = new Label();
                mv.visitJumpInsn(IFEQ, notBreakInKernelLoop);
                mv.visitInsn(DUP);
                // compare target with block
                mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "getTarget", cg.sig(Object.class));
                loadClosure();
                Label notBlockBreak = new Label();
                mv.visitJumpInsn(IF_ACMPNE, notBlockBreak);
                mv.visitInsn(DUP);
                mv.visitLdcInsn(Boolean.FALSE);
                mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "setBreakInKernelLoop", cg.sig(Void.TYPE, cg.params(Boolean.TYPE)));

                mv.visitLabel(notBlockBreak);
                // target is not == closure, rethrow
                mv.visitInsn(ATHROW);
                
                mv.visitLabel(notBreakInKernelLoop);
                // not a "loop" break, get out value
                
                mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "getValue", cg.sig(Object.class));
                mv.visitTypeInsn(CHECKCAST, cg.p(IRubyObject.class));
            }

            mv.visitLabel(normalBreak);
        }
    }
    
    public void yield(boolean hasArgs) {
        loadClosure();
        
        MethodVisitor method = getMethodVisitor();
        
        if (hasArgs) {
            method.visitInsn(SWAP);
            
            loadThreadContext();
            method.visitInsn(SWAP);
            
            // args now here
        } else {
            loadThreadContext();
            
            // empty args
            method.visitInsn(ACONST_NULL);
        }
        
        loadSelf();
        getRubyClass();
        method.visitLdcInsn(Boolean.FALSE);
        
        method.visitMethodInsn(INVOKEVIRTUAL, cg.p(Block.class), "yield", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class, IRubyObject.class, RubyModule.class, Boolean.TYPE)));
    }
    
    private void invokeIRubyObject(String methodName, String signature) {
        getMethodVisitor().visitMethodInsn(INVOKEINTERFACE, IRUBYOBJECT, methodName, signature);
    }
    
    public void loadThreadContext() {
        getMethodVisitor().visitVarInsn(ALOAD, THREADCONTEXT_INDEX);
    }
    
    public void loadClosure() {
        getMethodVisitor().visitVarInsn(ALOAD, CLOSURE_INDEX);
    }
    
    public void loadSelf() {
        getMethodVisitor().visitVarInsn(ALOAD, SELF_INDEX);
    }
    
    public void loadRuntime() {
        getMethodVisitor().visitVarInsn(ALOAD, RUNTIME_INDEX);
    }
    
    public void loadNil() {
        loadRuntime();
        invokeIRuby("getNil", cg.sig(IRubyObject.class));
    }
    
    public void loadSymbol(String symbol) {
        loadRuntime();
        
        MethodVisitor mv = getMethodVisitor();
        
        mv.visitLdcInsn(symbol);
        
        invokeIRuby("newSymbol", cg.sig(RubySymbol.class, cg.params(String.class)));
    }
    
    public void consumeCurrentValue() {
        getMethodVisitor().visitInsn(POP);
    }
    
    public void retrieveSelf() {
        loadSelf();
    }
    
    public void retrieveSelfClass() {
        loadSelf();
        invokeIRubyObject("getMetaClass", cg.sig(RubyClass.class));
    }
    
    public void assignLocalVariable(int index) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitInsn(DUP);
        //if ((index - 2) < Math.abs(getArity())) {
            // load from the incoming params
            // index is 2-based, and our zero is runtime
            
            // load args array
        //    mv.visitVarInsn(ALOAD, ARGS_INDEX);
        //    index = index - 2;
        //} else {
            mv.visitVarInsn(ALOAD, LOCAL_VARS_INDEX);
        //}
        
        mv.visitInsn(SWAP);
        mv.visitLdcInsn(new Integer(index));
        mv.visitInsn(SWAP);
        mv.visitInsn(AASTORE);
    }
    
    public void retrieveLocalVariable(int index) {
        MethodVisitor mv = getMethodVisitor();
        
        
        // check if it's an argument
        //if ((index - 2) < Math.abs(getArity())) {
            // load from the incoming params
            // index is 2-based, and our zero is runtime
            
            // load args array
        //    mv.visitVarInsn(ALOAD, ARGS_INDEX);
        //    mv.visitLdcInsn(new Integer(index - 2));
        //    mv.visitInsn(AALOAD);
        //} else {
            mv.visitVarInsn(ALOAD, LOCAL_VARS_INDEX);
            mv.visitLdcInsn(new Integer(index));
            mv.visitInsn(AALOAD);
        //}
    }
    
    public void assignLocalVariable(int index, int depth) {
        if (depth == 0) {
            assignLocalVariable(index);
            return;
        }

        MethodVisitor mv = getMethodVisitor();
        mv.visitInsn(DUP);
        
        // get the appropriate array out of the scopes
        mv.visitVarInsn(ALOAD, SCOPE_INDEX);
        mv.visitLdcInsn(new Integer(depth - 1));
        mv.visitInsn(AALOAD);
        
        // insert the value into the array at the specified index
        mv.visitInsn(SWAP);
        mv.visitLdcInsn(new Integer(index));
        mv.visitInsn(SWAP);
        mv.visitInsn(AASTORE);
    }
    
    public void retrieveLocalVariable(int index, int depth) {
        if (depth == 0) {
            retrieveLocalVariable(index);
            return;
        }
        
        MethodVisitor mv = getMethodVisitor();
        
        // get the appropriate array out of the scopes
        mv.visitVarInsn(ALOAD, SCOPE_INDEX);
        mv.visitLdcInsn(new Integer(depth - 1));
        mv.visitInsn(AALOAD);
        
        // load the value from the array at the specified index
        mv.visitLdcInsn(new Integer(index));
        mv.visitInsn(AALOAD);
    }
    
    public void retrieveConstant(String name) {
        MethodVisitor mv = getMethodVisitor();
        
        // FIXME this doesn't work right yet since TC.getConstant depends on TC state
        loadThreadContext();
        mv.visitLdcInsn(name);
        invokeThreadContext("getConstant", cg.sig(IRubyObject.class, cg.params(String.class)));
    }
    
    public void createNewFixnum(long value) {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        mv.visitLdcInsn(new Long(value));
        
        invokeIRuby("newFixnum", cg.sig(RubyFixnum.class, cg.params(Long.TYPE)));
    }
    
    public void createNewBignum(BigInteger value) {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        mv.visitLdcInsn(value.toString());
        
        mv.visitMethodInsn(INVOKESTATIC,cg.p(RubyBignum.class) , "newBignum", cg.sig(RubyBignum.class,cg.params(Ruby.class,String.class)));
    }
    
    public void createNewString(ByteList value) {
        MethodVisitor mv = getMethodVisitor();
        
        // FIXME: this is sub-optimal, storing string value in a java.lang.String again
        loadRuntime();
        mv.visitLdcInsn(value.toString());
        
        invokeIRuby("newString", cg.sig(RubyString.class, cg.params(String.class)));
    }
    
    public void createNewSymbol(String name) {
        loadRuntime();
        getMethodVisitor().visitLdcInsn(name);
        invokeIRuby("newSymbol", cg.sig(RubySymbol.class, cg.params(String.class)));
    }
    
    public void createNewArray() {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        // put under object array already present
        mv.visitInsn(SWAP);
        
        invokeIRuby("newArray", cg.sig(RubyArray.class, cg.params(IRubyObject[].class)));
    }
    
    public void createEmptyArray() {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        
        invokeIRuby("newArray", cg.sig(RubyArray.class, cg.params()));
    }
    
    public void createObjectArray(Object[] sourceArray, ArrayCallback callback) {
        buildObjectArray(IRUBYOBJECT, sourceArray, callback);
    }
    
    private void buildObjectArray(String type, Object[] sourceArray, ArrayCallback callback) {
        MethodVisitor mv = getMethodVisitor();
        
        mv.visitLdcInsn(new Integer(sourceArray.length));
        mv.visitTypeInsn(ANEWARRAY, type);
        
        for (int i = 0; i < sourceArray.length; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(new Integer(i));
            
            callback.nextValue(this, sourceArray, i);
            
            mv.visitInsn(AASTORE);
        }
    }
    
    public void createEmptyHash() {
        MethodVisitor mv = getMethodVisitor();

        loadRuntime();
        
        mv.visitMethodInsn(INVOKESTATIC, cg.p(RubyHash.class), "newHash", cg.sig(RubyHash.class, cg.params(Ruby.class)));
    }
    
    public void createNewHash(Object elements, ArrayCallback callback, int keyCount) {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        
        // create a new hashmap
        mv.visitTypeInsn(NEW, cg.p(HashMap.class));
        mv.visitInsn(DUP);       
        mv.visitMethodInsn(INVOKESPECIAL, cg.p(HashMap.class), "<init>", cg.sig(Void.TYPE));
        
        for (int i = 0; i < keyCount; i++) {
            mv.visitInsn(DUP);       
            callback.nextValue(this, elements, i);
            mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(HashMap.class), "put", cg.sig(Object.class, cg.params(Object.class, Object.class)));
            mv.visitInsn(POP);
        }
        
        loadNil();
        mv.visitMethodInsn(INVOKESTATIC, cg.p(RubyHash.class), "newHash", cg.sig(RubyHash.class, cg.params(Ruby.class, Map.class, IRubyObject.class)));
    }
    
    public void createNewRange(boolean isExclusive) {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        
        mv.visitInsn(DUP_X2);
        mv.visitInsn(POP);

        mv.visitLdcInsn(new Boolean(isExclusive));
        
        mv.visitMethodInsn(INVOKESTATIC, cg.p(RubyRange.class), "newRange", cg.sig(RubyRange.class, cg.params(Ruby.class, IRubyObject.class, IRubyObject.class, Boolean.TYPE)));
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
        
        MethodVisitor mv = getMethodVisitor();
        
        // call isTrue on the result
        isTrue();
        
        mv.visitJumpInsn(IFEQ, falseJmp); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        mv.visitJumpInsn(GOTO, afterJmp);
        
        // FIXME: optimize for cases where we have no false branch
        mv.visitLabel(falseJmp);
        falseBranch.branch(this);
        
        mv.visitLabel(afterJmp);
    }
    
    public void performLogicalAnd(BranchCallback longBranch) {
        Label afterJmp = new Label();
        Label falseJmp = new Label();
        
        MethodVisitor mv = getMethodVisitor();
        
        // dup it since we need to return appropriately if it's false
        mv.visitInsn(DUP);
        
        // call isTrue on the result
        isTrue();
        
        mv.visitJumpInsn(IFEQ, falseJmp); // EQ == 0 (i.e. false)
        // pop the extra result and replace with the send part of the AND
        mv.visitInsn(POP);
        longBranch.branch(this);
        mv.visitLabel(falseJmp);
    }
    
    public void performLogicalOr(BranchCallback longBranch) {
        Label afterJmp = new Label();
        Label falseJmp = new Label();
        
        MethodVisitor mv = getMethodVisitor();
        
        // dup it since we need to return appropriately if it's false
        mv.visitInsn(DUP);
        
        // call isTrue on the result
        isTrue();
        
        mv.visitJumpInsn(IFNE, falseJmp); // EQ == 0 (i.e. false)
        // pop the extra result and replace with the send part of the AND
        mv.visitInsn(POP);
        longBranch.branch(this);
        mv.visitLabel(falseJmp);
    }
    
    public void performBooleanLoop(BranchCallback condition, BranchCallback body, boolean checkFirst) {
        // FIXME: handle next/continue, break, etc
        MethodVisitor mv = getMethodVisitor();
        
        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label tryCatch = new Label();
        
        mv.visitTryCatchBlock(tryBegin, tryEnd, tryCatch, cg.p(JumpException.class));
        
        mv.visitLabel(tryBegin);
        {
            Label endJmp = new Label();
            if (checkFirst) {
                // calculate condition
                condition.branch(this);
                // call isTrue on the result
                isTrue();

                mv.visitJumpInsn(IFEQ, endJmp); // EQ == 0 (i.e. false)
            }

            Label topJmp = new Label();

            mv.visitLabel(topJmp);

            body.branch(this);

            // clear result after each loop
            mv.visitInsn(POP);

            // calculate condition
            condition.branch(this);
            // call isTrue on the result
            isTrue();

            mv.visitJumpInsn(IFNE, topJmp); // NE == nonzero (i.e. true)

            if (checkFirst) {
                mv.visitLabel(endJmp);
            }
        }
        
        mv.visitLabel(tryEnd);
        
        // no physical break, terminate loop and skip catch block
        Label normalBreak = new Label();
        mv.visitJumpInsn(GOTO, normalBreak);
        
        mv.visitLabel(tryCatch);
        {
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "getJumpType", cg.sig(JumpException.JumpType.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.JumpType.class), "getTypeId", cg.sig(Integer.TYPE));

            Label tryDefault = new Label();
            Label breakLabel = new Label();

            mv.visitLookupSwitchInsn(tryDefault, new int[] {JumpException.JumpType.BREAK}, new Label[] {breakLabel});

            // default is to just re-throw unhandled exception
            mv.visitLabel(tryDefault);
            mv.visitInsn(ATHROW);

            // break just terminates the loop normally, unless it's a block break...
            mv.visitLabel(breakLabel);
            
            // JRUBY-530 behavior
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "getTarget", cg.sig(Object.class));
            loadClosure();
            Label notBlockBreak = new Label();
            mv.visitJumpInsn(IF_ACMPNE, notBlockBreak);
            mv.visitInsn(DUP);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "setTarget", cg.sig(Void.TYPE, cg.params(Object.class)));
            mv.visitInsn(ATHROW);

            mv.visitLabel(notBlockBreak);
            // target is not == closure, normal loop exit, pop remaining exception object
            mv.visitInsn(POP);
        }
        
        mv.visitLabel(normalBreak);
        loadNil();
    }
    
    public static CompiledBlock createBlock(ThreadContext context, IRubyObject self, int arity, IRubyObject[][] scopes, Block block, CompiledBlockCallback callback) {
        return new CompiledBlock(context, self, Arity.createArity(arity), scopes, block, callback);
    }
    
    public void createNewClosure(StaticScope scope, int arity, ClosureCallback body) {
        // FIXME: This isn't quite done yet; waiting to have full support for passing closures so we can test it
        ClassVisitor cv = getClassVisitor();
        MethodVisitor method;
        
        String closureMethodName = "closure" + ++innerIndex;
        String closureFieldName = "_" + closureMethodName;
        
        // declare the field
        cv.visitField(ACC_PRIVATE | ACC_STATIC, closureFieldName, cg.ci(CompiledBlockCallback.class), null, null);
        
        ////////////////////////////
        // closure implementation
        method = cv.visitMethod(ACC_PUBLIC | ACC_STATIC, closureMethodName, CLOSURE_SIGNATURE, null, null);
        pushMethodVisitor(method);
        
        method.visitCode();
        
        // logic to start off the closure with dvar slots
        method.visitLdcInsn(new Integer(scope.getNumberOfVariables()));
        method.visitTypeInsn(ANEWARRAY, cg.p(IRubyObject.class));
        
        // store the dvars in a local variable
        method.visitVarInsn(ASTORE, LOCAL_VARS_INDEX);
        
        // arraycopy arguments into local vars array
        if (arity != 0) {
            // array index OOB for some reason; perhaps because we're not actually handling args right?
            /*method.visitVarInsn(ALOAD, ARGS_INDEX);
            method.visitInsn(ICONST_0);
            method.visitVarInsn(ALOAD, LOCAL_VARS_INDEX);
            mv.visitInsn(ICONST_2);
            method.visitLdcInsn(new Integer(arity));
            method.visitMethodInsn(INVOKESTATIC, cg.p(System.class), "arraycopy", cg.sig(Void.TYPE, cg.params(Object.class, Integer.TYPE, Object.class, Integer.TYPE, Integer.TYPE)));*/
        }
        
        // Containing scopes are passed as IRubyObject[][] in the SCOPE_INDEX var
        
        // set up a local IRuby variable
        method.visitVarInsn(ALOAD, THREADCONTEXT_INDEX);
        invokeThreadContext("getRuntime", cg.sig(Ruby.class));
        method.visitVarInsn(ASTORE, RUNTIME_INDEX);
        
        // start of scoping for closure's vars
        Label start = new Label();
        method.visitLabel(start);
        
        // visit the body of the closure
        body.compile(this);
        
        method.visitInsn(ARETURN);
        
        // end of scoping for closure's vars
        Label end = new Label();
        method.visitLabel(end);
        method.visitMaxs(1, 1);
        method.visitEnd();
        
        popMethodVisitor();
        
        method = getMethodVisitor();
        
        // Now, store a compiled block object somewhere we can access it in the future
        
        // in current method, load the field to see if we've created a BlockCallback yet
        method.visitFieldInsn(GETSTATIC, classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
        Label alreadyCreated = new Label();
        method.visitJumpInsn(IFNONNULL, alreadyCreated);
        
        // no callback, construct it
        getCallbackFactory();
        
        method.visitLdcInsn(closureMethodName);
        method.visitMethodInsn(INVOKEVIRTUAL, cg.p(CallbackFactory.class), "getBlockCallback", cg.sig(CompiledBlockCallback.class, cg.params(String.class)));
        method.visitFieldInsn(PUTSTATIC, classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
        
        method.visitLabel(alreadyCreated);
        
        // Construct the block for passing to the target method
        loadThreadContext();
        loadSelf();
        method.visitLdcInsn(new Integer(arity));
        
        // create an array of scopes to use
        
        // check if we have containing scopes
        method.visitVarInsn(ALOAD, SCOPE_INDEX);
        Label noScopes = new Label();
        Label copyLocals = new Label();
        method.visitJumpInsn(IFNULL, noScopes);
        
        // we have containing scopes, include them
        
        // get length of current scopes array, add one
        method.visitVarInsn(ALOAD, SCOPE_INDEX);
        method.visitInsn(ARRAYLENGTH);
        method.visitInsn(ICONST_1);
        method.visitInsn(IADD);
        
        // create new scopes array
        method.visitTypeInsn(ANEWARRAY, cg.p(IRubyObject[].class));
        
        // copy containing scopes to index one and on
        method.visitInsn(DUP);
        method.visitVarInsn(ALOAD, SCOPE_INDEX);
        method.visitInsn(SWAP);
        method.visitInsn(ICONST_0);
        method.visitInsn(SWAP);
        // new scopes array is here now
        method.visitInsn(ICONST_1);
        method.visitVarInsn(ALOAD, SCOPE_INDEX);
        method.visitInsn(ARRAYLENGTH);
        method.visitMethodInsn(INVOKESTATIC, cg.p(System.class), "arraycopy", cg.sig(Void.TYPE, cg.params(Object.class, Integer.TYPE, Object.class, Integer.TYPE, Integer.TYPE)));

        method.visitJumpInsn(GOTO, copyLocals);
        
        method.visitLabel(noScopes);

        // create new scopes array
        method.visitInsn(ICONST_1);
        method.visitTypeInsn(ANEWARRAY, cg.p(IRubyObject[].class));
        
        method.visitLabel(copyLocals);

        // store local vars at index zero
        method.visitInsn(DUP);
        method.visitInsn(ICONST_0);
        method.visitVarInsn(ALOAD, LOCAL_VARS_INDEX);
        method.visitInsn(AASTORE);
        
        loadClosure();
        
        method.visitFieldInsn(GETSTATIC, classname, closureFieldName, cg.ci(CompiledBlockCallback.class));
        
        method.visitMethodInsn(INVOKESTATIC, cg.p(StandardASMCompiler.class), "createBlock",
                cg.sig(CompiledBlock.class, cg.params(ThreadContext.class, IRubyObject.class, Integer.TYPE, IRubyObject[][].class, Block.class, CompiledBlockCallback.class)));
    }
    
    private void invokeThreadContext(String methodName, String signature) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitMethodInsn(INVOKEVIRTUAL, THREADCONTEXT, methodName, signature);
    }
    
    private void invokeIRuby(String methodName, String signature) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitMethodInsn(INVOKEVIRTUAL, RUBY, methodName, signature);
    }
    
    private void getCallbackFactory() {
        loadRuntime();
        MethodVisitor mv = getMethodVisitor();
        mv.visitLdcInsn(classname);
        mv.visitMethodInsn(INVOKESTATIC, cg.p(Class.class), "forName", cg.sig(Class.class, cg.params(String.class)));
        invokeIRuby("callbackFactory", cg.sig(CallbackFactory.class, cg.params(Class.class)));
    }
    
    private void getRubyClass() {
        loadSelf();
        // FIXME: This doesn't seem *quite* right. If actually within a class...end, is self.getMetaClass the correct class? should be self, no?
        invokeIRubyObject("getMetaClass", cg.sig(RubyClass.class));
    }
    
    private void getCRef() {
        loadThreadContext();
        // FIXME: This doesn't seem *quite* right. If actually within a class...end, is self.getMetaClass the correct class? should be self, no?
        invokeThreadContext("peekCRef", cg.sig(SinglyLinkedList.class));
    }
    
    private void newTypeError(String error) {
        loadRuntime();
        getMethodVisitor().visitLdcInsn(error);
        invokeIRuby("newTypeError", cg.sig(RaiseException.class, cg.params(String.class)));
    }
    
    private void getCurrentVisibility() {
        loadThreadContext();
        invokeThreadContext("getCurrentVisibility", cg.sig(Visibility.class));
    }
    
    private void println() {
        MethodVisitor mv = getMethodVisitor();
        
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETSTATIC, cg.p(System.class), "out", cg.ci(PrintStream.class));
        mv.visitInsn(SWAP);
        
        mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(PrintStream.class), "println", cg.sig(Void.TYPE, cg.params(Object.class)));
    }
    
    public void defineAlias(String newName, String oldName) {
        getRubyClass();
        getMethodVisitor().visitLdcInsn(newName);
        getMethodVisitor().visitLdcInsn(oldName);
        getMethodVisitor().visitMethodInsn(INVOKEVIRTUAL, cg.p(RubyModule.class), "defineAlias", cg.sig(Void.TYPE,cg.params(String.class,String.class)));
        loadNil();
        // TODO: should call method_added, and possibly push nil.
    }
    
    public static IRubyObject def(ThreadContext context, IRubyObject self, Class compiledClass, String name, String javaName, int arity) {
        Ruby runtime = context.getRuntime();
        
        // FIXME: This is what the old def did, but doesn't work in the compiler for top-level methods. Hmm.
        //RubyModule containingClass = context.getRubyClass();
        RubyModule containingClass = self.getMetaClass();
        
        if (containingClass == null) {
            throw runtime.newTypeError("No class to add method.");
        }
        
        if (containingClass == runtime.getObject() && name == "initialize") {
            runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
        }
        
        Visibility visibility = context.getCurrentVisibility();
        if (name == "initialize" || visibility.isModuleFunction() || context.isTopLevel()) {
            visibility = Visibility.PRIVATE;
        }
        
        SinglyLinkedList cref = context.peekCRef();
        
        MethodFactory factory = MethodFactory.createFactory();
        DynamicMethod method = factory.getCompiledMethod(containingClass, compiledClass, javaName, Arity.createArity(arity), visibility, cref);
        
        containingClass.addMethod(name, method);
        
        if (context.getCurrentVisibility().isModuleFunction()) {
            containingClass.getSingletonClass().addMethod(
                    name,
                    new WrapperMethod(containingClass.getSingletonClass(), method,
                    Visibility.PUBLIC));
            containingClass.callMethod(context, "singleton_method_added", runtime.newSymbol(name));
        }
        
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            ((MetaClass) containingClass).getAttachedObject().callMethod(
                    context, "singleton_method_added", runtime.newSymbol(name));
        } else {
            containingClass.callMethod(context, "method_added", runtime.newSymbol(name));
        }
        
        return runtime.getNil();
    }
    
    public void defineNewMethod(String name, int arity, int localVarCount, ClosureCallback body) {
        // TODO: build arg list based on number of args, optionals, etc
        ++methodIndex;
        String methodName = cg.cleanJavaIdentifier(name) + "__" + methodIndex;
        
        beginMethod(methodName, arity, localVarCount);
        
        MethodVisitor mv = getMethodVisitor();
        
        // put a null at register 4, for closure creation to know we're at top-level or local scope
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, SCOPE_INDEX);
        
        // callback to fill in method body
        body.compile(this);
        
        endMethod(mv);
        
        // return to previous method
        mv = getMethodVisitor();
        
        // prepare to call "def" utility method to handle def logic
        loadThreadContext();
        
        loadSelf();
        
        // load the class we're creating, for binding purposes
        mv.visitLdcInsn(classname.replace('/', '.'));
        mv.visitMethodInsn(INVOKESTATIC, cg.p(Class.class), "forName", cg.sig(Class.class, cg.params(String.class)));
        
        mv.visitLdcInsn(name);
        
        mv.visitLdcInsn(methodName);
        
        mv.visitLdcInsn(new Integer(arity));
        
        mv.visitMethodInsn(INVOKESTATIC,
                cg.p(StandardASMCompiler.class),
                "def",
                cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class, Class.class, String.class, String.class, Integer.TYPE)));
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
        
        MethodVisitor mv = getMethodVisitor();
        
        mv.visitLdcInsn(name);
        invokeIRubyObject("getInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class)));
        
        // check if it's null; if so, load nil
        mv.visitInsn(DUP);
        Label notNull = new Label();
        mv.visitJumpInsn(IFNONNULL, notNull);
        
        // pop the dup'ed null
        mv.visitInsn(POP);
        // replace it with nil
        loadNil();
        
        mv.visitLabel(notNull);
    }
    
    public void assignInstanceVariable(String name) {
        MethodVisitor mv = getMethodVisitor();
        
        loadSelf();
        mv.visitInsn(SWAP);
        
        mv.visitLdcInsn(name);
        mv.visitInsn(SWAP);
        
        invokeIRubyObject("setInstanceVariable", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
    }
    
    public void retrieveGlobalVariable(String name) {
        loadRuntime();
        
        MethodVisitor mv = getMethodVisitor();
        
        invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(GlobalVariables.class), "get", cg.sig(IRubyObject.class, cg.params(String.class)));
    }
    
    public void assignGlobalVariable(String name) {
        loadRuntime();
        
        MethodVisitor mv = getMethodVisitor();
        
        invokeIRuby("getGlobalVariables", cg.sig(GlobalVariables.class));
        mv.visitInsn(SWAP);
        mv.visitLdcInsn(name);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(GlobalVariables.class), "set", cg.sig(IRubyObject.class, cg.params(String.class, IRubyObject.class)));
    }
    
    public void negateCurrentValue() {
        MethodVisitor mv = getMethodVisitor();
        
        isTrue();
        Label isTrue = new Label();
        Label end = new Label();
        mv.visitJumpInsn(IFNE, isTrue);
        loadTrue();
        mv.visitJumpInsn(GOTO, end);
        mv.visitLabel(isTrue);
        loadFalse();
        mv.visitLabel(end);
    }
    
    public void splatCurrentValue() {
        MethodVisitor method = getMethodVisitor();
        
        method.visitMethodInsn(INVOKESTATIC, cg.p(EvaluationState.class), "splatValue", cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
    }
    
    public void singlifySplattedValue() {
        MethodVisitor method = getMethodVisitor();
        method.visitMethodInsn(INVOKESTATIC, cg.p(EvaluationState.class), "aValueSplat", cg.sig(IRubyObject.class, cg.params(IRubyObject.class)));
    }
    
    public void ensureRubyArray() {
        MethodVisitor method = getMethodVisitor();
        
        method.visitMethodInsn(INVOKESTATIC, cg.p(StandardASMCompiler.class), "ensureRubyArray", cg.sig(RubyArray.class, cg.params(IRubyObject.class)));
    }
    
    public static RubyArray ensureRubyArray(IRubyObject value) {
        if (!(value instanceof RubyArray)) {
            value = RubyArray.newArray(value.getRuntime(), value);
        }
        return (RubyArray)value;
    }
    
    public void forEachInValueArray(int start, int count, Object source, ArrayCallback callback) {
        MethodVisitor method = getMethodVisitor();
        
        Label noMoreArrayElements = new Label();
        for (; start < count; start++) {
            // confirm we're not past the end of the array
            method.visitInsn(DUP); // dup the original array object
            method.visitMethodInsn(INVOKEVIRTUAL, cg.p(RubyArray.class), "getLength", cg.sig(Integer.TYPE, cg.params()));
            method.visitLdcInsn(new Integer(start));
            method.visitJumpInsn(IFLE, noMoreArrayElements); // if length <= start, end loop
            
            // extract item from array
            method.visitInsn(DUP); // dup the original array object
            method.visitLdcInsn(new Integer(start)); // index for the item
            method.visitMethodInsn(INVOKEVIRTUAL, cg.p(RubyArray.class), "entry",
                    cg.sig(IRubyObject.class, cg.params(Long.TYPE))); // extract item
            callback.nextValue(this, source, start);
        }
        method.visitLabel(noMoreArrayElements);
    }

    public void loadInteger(int value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void performGEBranch(BranchCallback trueBranch,
                                BranchCallback falseBranch) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void performGTBranch(BranchCallback trueBranch,
                                BranchCallback falseBranch) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void performLEBranch(BranchCallback trueBranch,
                                BranchCallback falseBranch) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void performLTBranch(BranchCallback trueBranch,
                                BranchCallback falseBranch) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void loadRubyArraySize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void issueBreakEvent() {
        MethodVisitor mv = getMethodVisitor();
        
        mv.visitTypeInsn(NEW, cg.p(JumpException.class));
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETSTATIC, cg.p(JumpException.JumpType.class), "BreakJump", cg.ci(JumpException.JumpType.class));
        mv.visitMethodInsn(INVOKESPECIAL, cg.p(JumpException.class), "<init>", cg.sig(Void.TYPE, cg.params(JumpException.JumpType.class)));
        
        // set result into jump exception
        mv.visitInsn(DUP_X1);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, cg.p(JumpException.class), "setValue", cg.sig(Void.TYPE, cg.params(Object.class)));
        
        mv.visitInsn(ATHROW);
    }
}
