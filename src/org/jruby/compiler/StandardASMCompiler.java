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

package org.jruby.compiler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author headius
 */
public class StandardASMCompiler implements Compiler {
    private static final String THREADCONTEXT = "org/jruby/runtime/ThreadContext";
    private static final String IRUBY = "org/jruby/IRuby";
    private static final String IRUBYOBJECT = "org/jruby/runtime/builtin/IRubyObject";
    
    private static final String MULTISTUB_SIGNATURE =
            "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/BlockCallback2;)Lorg/jruby/runtime/builtin/IRubyObject;";
    private static final String CLOSURE_SIGNATURE =
            "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;";
    
    private static final int THREADCONTEXT_INDEX = 1;
    private static final int SELF_INDEX = 2;
    private static final int ARGS_INDEX = 3;
    private static final int CLOSURE_INDEX = 4;
    private static final int RUNTIME_INDEX = 5;
    private static final int LOCAL_VARS_INDEX = 6;
    
    private Stack classVisitors = new Stack();
    private Stack methodVisitors = new Stack();
    private Stack arities = new Stack();
    private Stack scopeStarts = new Stack();
    
    private String classname;
    private String sourcename;
    
    Map classWriters = new HashMap();
    ClassWriter currentMultiStub = null;
    int multiStubIndex = -1;
    int multiStubCount = -1;
    int innerIndex = 1;
    
    int lastLine = -1;
    
    /** Creates a new instance of StandardCompilerContext */
    public StandardASMCompiler(String classname, String sourcename) {
        this.classname = classname;
        this.sourcename = sourcename;
    }
    
    public StandardASMCompiler(Node node) {
        // determine new class name based on filename of incoming node
        ISourcePosition position = node.getPosition();
        
        if (position != null) {
            classname = position.getFile();
            if (classname.endsWith(".rb")) {
                classname = classname.substring(0, classname.length() - 3);
            }
            sourcename = position.getFile();
        } else {
            classname = "EVAL";
            sourcename = "EVAL";
        }
    }
    
    public Class loadClass() throws ClassNotFoundException {
        JRubyClassLoader jcl = new JRubyClassLoader();
        
        for (Iterator iter = classWriters.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String)entry.getKey();
            ClassWriter writer = (ClassWriter)entry.getValue();
            
            jcl.defineClass(key.replaceAll("/", "."), writer.toByteArray());
        }

        return jcl.loadClass(classname.replaceAll("/", "."));
    }
    
    public String getClassname() {
        return classname;
    }
    
    public String getSourcename() {
        return sourcename;
    }

    public ClassVisitor getClassVisitor() {
        return (ClassVisitor)classVisitors.peek();
    }

    public MethodVisitor getMethodVisitor() {
        return (MethodVisitor)methodVisitors.peek();
    }

    public ClassVisitor popClassVisitor() {
        return (ClassVisitor)classVisitors.pop();
    }

    public MethodVisitor popMethodVisitor() {
        return (MethodVisitor)methodVisitors.pop();
    }

    public void pushClassVisitor(ClassVisitor cv) {
        classVisitors.push(cv);
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
        ClassVisitor cv = new ClassWriter(true);
        
        // put into classwriter map for later generation/loading
        classWriters.put(classname, cv);
        
        pushClassVisitor(cv);
        
        // Create the class with the appropriate class name and source file
        cv.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, classname, null, "java/lang/Object", new String[] {"org/jruby/ast/executable/Script"});
        cv.visitSource(sourcename, null);
        
        createConstructor();
    }
    
    public void endScript() {
        closeOutMultiStub();

        // add Script#run impl
        // root method of a script is always in stub0, method0
        String stubName = classname + "$MultiStub0";
        String methodName = "method0";
        MethodVisitor mv = getClassVisitor().visitMethod(Opcodes.ACC_PUBLIC, "run", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;", null, null);
        mv.visitTypeInsn(Opcodes.NEW, stubName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, stubName, "<init>", "()V");
        
        // invoke method0 with threadcontext, self, args (null), and block (null)
        mv.visitVarInsn(Opcodes.ALOAD, THREADCONTEXT_INDEX);
        mv.visitVarInsn(Opcodes.ALOAD, SELF_INDEX);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ACONST_NULL);
        
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, stubName, methodName, MULTISTUB_SIGNATURE);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
    
    private void createConstructor() {
        ClassVisitor cv = getClassVisitor();
        
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>",
                "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public Object beginMethod(int arity, int localVarCount) {
        // create a new MultiStub-based method impl and provide the method visitor for it
        if (currentMultiStub == null || multiStubIndex == 9) {
            if (currentMultiStub != null) {
                // FIXME can we end if there's still a method in flight?
                currentMultiStub.visitEnd();
            }
            
            multiStubCount++;
            
            ClassVisitor cv = getClassVisitor();
            currentMultiStub = new org.objectweb.asm.ClassWriter(true);
            currentMultiStub.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_STATIC,
                    classname + "$MultiStub" + multiStubCount, null, "java/lang/Object", new String[] {"org/jruby/internal/runtime/methods/MultiStub2"});
            cv.visitInnerClass(classname + "$MultiStub" + multiStubCount, classname, "MultiStub" + multiStubCount, Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC);
            multiStubIndex = 0;
            classWriters.put(classname + "$MultiStub" + multiStubCount, currentMultiStub);
            currentMultiStub.visitSource(sourcename, null);

            MethodVisitor stubConstructor = currentMultiStub.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            stubConstructor.visitCode();
            stubConstructor.visitVarInsn(Opcodes.ALOAD, 0);
            stubConstructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>",
                    "()V");
            stubConstructor.visitInsn(Opcodes.RETURN);
            stubConstructor.visitMaxs(1, 1);
            stubConstructor.visitEnd();
        } else {
            multiStubIndex++;
        }
        
        MethodVisitor newMethod = currentMultiStub.visitMethod(Opcodes.ACC_PUBLIC, "method" + multiStubIndex, MULTISTUB_SIGNATURE, null, null);
        pushMethodVisitor(newMethod);
        
        newMethod.visitCode();
        
        // logic to start off the root node's code with local var slots and all
        newMethod.visitLdcInsn(new Integer(localVarCount));
        newMethod.visitTypeInsn(Opcodes.ANEWARRAY, "org/jruby/runtime/builtin/IRubyObject");
        
        // store the local vars in a local variable
        newMethod.visitVarInsn(Opcodes.ASTORE, LOCAL_VARS_INDEX);
        
        // set up a local IRuby variable
        newMethod.visitVarInsn(Opcodes.ALOAD, THREADCONTEXT_INDEX);
        invokeThreadContext("getRuntime", "()Lorg/jruby/IRuby;");
        newMethod.visitVarInsn(Opcodes.ASTORE, RUNTIME_INDEX);
        
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
        mv.visitInsn(Opcodes.ARETURN);
        
        // end of variable scope
        Label end = new Label();
        mv.visitLabel(end);
        
        // local variable for lvars array
        mv.visitLocalVariable("lvars", "[L" + IRUBYOBJECT + ";", null, popScopeStart(), end, LOCAL_VARS_INDEX);
        
        mv.visitMaxs(1, 1); // automatically calculated by ASM
        mv.visitEnd();
        
        popMethodVisitor();
        popArity();
    }
    
    public void closeOutMultiStub() {
        if (currentMultiStub != null) {
            while (multiStubIndex < 9) {
                multiStubIndex++;
                MethodVisitor multiStubMethod = currentMultiStub.visitMethod(Opcodes.ACC_PUBLIC, "method" + multiStubIndex, MULTISTUB_SIGNATURE, null, null);
                multiStubMethod.visitCode();
                multiStubMethod.visitInsn(Opcodes.ACONST_NULL);
                multiStubMethod.visitInsn(Opcodes.ARETURN);
                multiStubMethod.visitMaxs(1, 1);
                multiStubMethod.visitEnd();
            }
        }
    }
    
    public void lineNumber(ISourcePosition position) {
        if (lastLine == (lastLine = position.getEndLine())) return; // did not change lines for this node, don't bother relabeling
        
        Label l = new Label();
        MethodVisitor mv = getMethodVisitor();
        mv.visitLabel(l);
        mv.visitLineNumber(position.getEndLine(), l);
    }
    
    public void invokeDynamic(String name, boolean hasReceiver, boolean hasArgs) {
        MethodVisitor mv = getMethodVisitor();
        String callType = null;
        String callSig = "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;";
        
        if (hasArgs) {
            if (hasReceiver) {
                // Call with args
                // receiver already present

                loadThreadContext();
                // put under args
                mv.visitInsn(Opcodes.SWAP);
                
                // FIXME: if calling against "self", this should be VARIABLE
                callType = "NORMAL";
            } else {
                // FCall
                // no receiver present, use self
                loadSelf();
                // put self under args
                mv.visitInsn(Opcodes.SWAP);
                
                loadThreadContext();
                // put under args
                mv.visitInsn(Opcodes.SWAP);
                
                callType = "FUNCTIONAL";
            }

            mv.visitLdcInsn(name);
            // put under args
            mv.visitInsn(Opcodes.SWAP);
        } else {
            if (hasReceiver) {
                // Call with no args
                // receiver already present
                
                loadThreadContext();
                
                callType = "FUNCTIONAL";
            } else {
                // VCall
                // no receiver present, use self
                loadSelf();
                
                loadThreadContext();
                
                callType = "VARIABLE";
            }

            mv.visitLdcInsn(name);
                
            // empty args list
            mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/builtin/IRubyObject", "NULL_ARRAY", "[Lorg/jruby/runtime/builtin/IRubyObject;");
        }

        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", callType, "Lorg/jruby/runtime/CallType;");

        invokeIRubyObject("callMethod", callSig);
    }

    private void invokeIRubyObject(String methodName, String signature) {
        getMethodVisitor().visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBYOBJECT, methodName, signature);
    }
    
    public void loadThreadContext() {
        getMethodVisitor().visitVarInsn(Opcodes.ALOAD, THREADCONTEXT_INDEX);
    }
    
    public void loadSelf() {
        getMethodVisitor().visitVarInsn(Opcodes.ALOAD, SELF_INDEX);
    }
    
    public void loadRuntime() {
        getMethodVisitor().visitVarInsn(Opcodes.ALOAD, RUNTIME_INDEX);
    }
    
    public void loadNil() {
        loadRuntime();
        invokeIRuby("getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
    }
    
    public void consumeCurrentValue() {
        getMethodVisitor().visitInsn(Opcodes.POP);
    }
    
    public void retrieveSelf() {
        loadSelf();
    }
    
    public void assignLocalVariable(int index) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitInsn(Opcodes.DUP);
        if ((index - 2) < Math.abs(getArity())) {
            // load from the incoming params
            // index is 2-based, and our zero is runtime
            
            // load args array
            mv.visitVarInsn(Opcodes.ALOAD, ARGS_INDEX);
            index = index - 2;
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, LOCAL_VARS_INDEX);
        }
        
        mv.visitInsn(Opcodes.SWAP);
        mv.visitLdcInsn(new Integer(index));
        mv.visitInsn(Opcodes.SWAP);
        mv.visitInsn(Opcodes.AASTORE);
    }
    
    public void retrieveLocalVariable(int index) {
        MethodVisitor mv = getMethodVisitor();
        
        // check if it's an argument
        if ((index - 2) < Math.abs(getArity())) {
            // load from the incoming params
            // index is 2-based, and our zero is runtime
            
            // load args array
            mv.visitVarInsn(Opcodes.ALOAD, ARGS_INDEX);
            mv.visitLdcInsn(new Integer(index - 2));
            mv.visitInsn(Opcodes.AALOAD);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, LOCAL_VARS_INDEX);
            mv.visitLdcInsn(new Integer(index));
            mv.visitInsn(Opcodes.AALOAD);
        }
    }
    
    public void createNewFixnum(long value) {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        mv.visitLdcInsn(new Long(value));
        
        invokeIRuby("newFixnum", "(J)Lorg/jruby/RubyFixnum;");
    }
    
    public void createNewString(String value) {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        mv.visitLdcInsn(value);
        
        invokeIRuby("newString", "(Ljava/lang/String;)Lorg/jruby/RubyString;");
    }
    
    public void createNewArray() {
        MethodVisitor mv = getMethodVisitor();
        
        loadRuntime();
        // put under object array already present
        mv.visitInsn(Opcodes.SWAP);
        
        invokeIRuby("newArray", "([Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/RubyArray;");
    }
    
    public void createObjectArray(Object[] sourceArray, ArrayCallback callback) {
        buildObjectArray(IRUBYOBJECT, sourceArray, callback);
    }
    
    private void buildObjectArray(String type, Object[] sourceArray, ArrayCallback callback) {
        MethodVisitor mv = getMethodVisitor();
        
        mv.visitLdcInsn(new Integer(sourceArray.length));
        mv.visitTypeInsn(Opcodes.ANEWARRAY, type);
        
        for (int i = 0; i < sourceArray.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(new Integer(i));
            
            callback.nextValue(this, sourceArray, i);
            
            mv.visitInsn(Opcodes.AASTORE);
        }
    }
    
    public void performBooleanBranch(BranchCallback trueBranch, BranchCallback falseBranch) {
        Label afterJmp = new Label();
        Label falseJmp = new Label();
        
        MethodVisitor mv = getMethodVisitor();
        
        // call isTrue on the result
        invokeIRubyObject("isTrue", "()Z");
        
        mv.visitJumpInsn(Opcodes.IFEQ, falseJmp); // EQ == 0 (i.e. false)
        trueBranch.branch(this);
        mv.visitJumpInsn(Opcodes.GOTO, afterJmp);

        // FIXME: optimize for cases where we have no false branch
        mv.visitLabel(falseJmp);
        falseBranch.branch(this);

        mv.visitLabel(afterJmp);
    }
    
    public void performBooleanLoop(BranchCallback condition, BranchCallback body, boolean checkFirst) {
        // FIXME: handle next/continue, break, etc
        MethodVisitor mv = getMethodVisitor();
        
        Label endJmp = new Label();
        if (checkFirst) {
            // calculate condition
            condition.branch(this);
            // call isTrue on the result
            invokeIRubyObject("isTrue", "()Z");
        
            mv.visitJumpInsn(Opcodes.IFEQ, endJmp); // EQ == 0 (i.e. false)
        }

        Label topJmp = new Label();

        mv.visitLabel(topJmp);
            
        body.branch(this);
        
        // clear result after each loop
        mv.visitInsn(Opcodes.POP);

        // calculate condition
        condition.branch(this);
        // call isTrue on the result
        invokeIRubyObject("isTrue", "()Z");

        mv.visitJumpInsn(Opcodes.IFNE, topJmp); // NE == nonzero (i.e. true)
        
        if (checkFirst) {
            mv.visitLabel(endJmp);
        }
        
        loadNil();
    }
    
    public void createNewClosure(StaticScope scope, ClosureCallback body) {
        ClassVisitor closureVisitor = new ClassWriter(true);
        FieldVisitor fv;
        MethodVisitor method;

        String closureClassName = classname + "$Closure" + innerIndex;
        String closureClassShortName = "Closure" + innerIndex;
        innerIndex++;
        
        closureVisitor.visit(Opcodes.V1_4, Opcodes.ACC_SUPER, closureClassName, null, "java/lang/Object", new String[] { "org/jruby/runtime/BlockCallback2" });
        pushClassVisitor(closureVisitor);
        classWriters.put(closureClassName, closureVisitor);
        
        closureVisitor.visitSource(sourcename, null);
        
        // closure is an inner class
        closureVisitor.visitInnerClass(closureClassName, null, closureClassShortName, Opcodes.ACC_PRIVATE);
        innerIndex++;

        // this$0 field points at the containing class
        // val$variables points at the containing scope's instance variables
        fv = closureVisitor.visitField(Opcodes.ACC_FINAL + Opcodes.ACC_SYNTHETIC, "this$0", "L" + classname + ";", null, null);
        fv = closureVisitor.visitField(Opcodes.ACC_FINAL + Opcodes.ACC_SYNTHETIC, "val$variables", "[L" + IRUBYOBJECT +";", null, null);
        fv.visitEnd();
        
        ///////////////////////////////////////////
        // constructor for closure object
        // note that this accepts an array of IRubyObject; this is the
        // local variables from the containing scope. The current compiler won't work
        // with more than a single containing scope
        method = closureVisitor.visitMethod(0, "<init>", "(L" + classname + ";[L" + IRUBYOBJECT + ";)V", null, null);
        method.visitCode();
        Label l0 = new Label();
        method.visitLabel(l0);
        method.visitLineNumber(7, l0);
        
        // store the containing "this"
        // FIXME: need to do some stack magic here to support nested closures
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitFieldInsn(Opcodes.PUTFIELD, closureClassName, "this$0", "L" + classname + ";");
        
        // store the containing scope's local variables
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitFieldInsn(Opcodes.PUTFIELD, closureClassName, "val$variables", "[L" + IRUBYOBJECT +";");
        
        // call super constructor
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        method.visitInsn(Opcodes.RETURN);
        Label l1 = new Label();
        method.visitLabel(l1);
        method.visitLocalVariable("this", "L" + closureClassName + ";", null, l0, l1, 0);
        method.visitMaxs(1,1);
        method.visitEnd();
        
        ////////////////////////////
        // closure implementation
        method = closureVisitor.visitMethod(Opcodes.ACC_PUBLIC, "call", CLOSURE_SIGNATURE, null, null);
        pushMethodVisitor(method);
        
        method.visitCode();
        
        // logic to start off the closure with dvar slots
        method.visitLdcInsn(new Integer(scope.getNumberOfVariables()));
        method.visitTypeInsn(Opcodes.ANEWARRAY, "org/jruby/runtime/builtin/IRubyObject");
        
        // store the dvars in a local variable
        method.visitVarInsn(Opcodes.ASTORE, LOCAL_VARS_INDEX);
        
        // set up local variables for containing scopes
        // containing scope's local vars are stored in LOCAL_VARS_INDEX + <depth>
        // where <depth> is the number of scopes we are away from that.
        // Eventually this will want to handle multiple nested scopes
        method.visitFieldInsn(Opcodes.GETFIELD, closureClassName, "val$variables", "[L" + IRUBYOBJECT +";");
        method.visitVarInsn(Opcodes.ASTORE, LOCAL_VARS_INDEX + 1);
        
        // set up a local IRuby variable
        method.visitVarInsn(Opcodes.ALOAD, THREADCONTEXT_INDEX);
        invokeThreadContext("getRuntime", "()Lorg/jruby/IRuby;");
        method.visitVarInsn(Opcodes.ASTORE, RUNTIME_INDEX);
        
        // start of scoping for closure's vars
        Label start = new Label();
        method.visitLabel(start);
        
        // visit the body of the closure
        body.compile(this);
        
        method.visitInsn(Opcodes.ARETURN);
        
        // end of scoping for closure's vars
        Label end = new Label();
        method.visitLabel(end);
        method.visitLocalVariable("this", "Lorg/jruby/FooBar$1$Figlet;", null, start, end, 0);
        method.visitLocalVariable("dvars" + closureClassShortName, "[L" + IRUBYOBJECT + ";", null, start, end, LOCAL_VARS_INDEX);
        // in the future this should be indexed by depth and have no distinction
        // between lvars and dvars
        method.visitLocalVariable("lvars" + closureClassShortName, "[L" + IRUBYOBJECT + ";", null, start, end, LOCAL_VARS_INDEX + 1);
        method.visitMaxs(1, 1);
        method.visitEnd();
        
        popMethodVisitor();

        closureVisitor.visitEnd();

        popClassVisitor();
    }

    private void invokeThreadContext(String methodName, String signature) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, methodName, signature);
    }

    private void invokeIRuby(String methodName, String signature) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, methodName, signature);
    }
    
    private void getRubyClass() {
        loadSelf();
        // FIXME: This doesn't seem *quite* right. If actually within a class...end, is self.getMetaClass the correct class? should be self, no?
        invokeIRubyObject("getMetaClass", "()Lorg/jruby/RubyClass;");
    }

    private void newTypeError(String error) {
        loadRuntime();
        getMethodVisitor().visitLdcInsn(error);
        invokeIRuby("newTypeError", "(Ljava/lang/String;)Lorg/jruby/exceptions/RaiseException;");
    }

    private void getCurrentVisibility() {
        loadThreadContext();
        invokeThreadContext("getCurrentVisibility", "()Lorg/jruby/runtime/Visibility;");
    }
    
    private void println() {
        MethodVisitor mv = getMethodVisitor();
        
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitInsn(Opcodes.SWAP);
        
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V");
    }
    
    public void defineNewMethod(String name, int arity, int localVarCount, ClosureCallback body) {
        // TODO: build arg list based on number of args, optionals, etc
        beginMethod(arity, localVarCount);
        
        MethodVisitor mv = getMethodVisitor();

        mv.visitCode();
        
        // arraycopy arguments into local vars array
        mv.visitVarInsn(Opcodes.ALOAD, ARGS_INDEX);
        mv.visitLdcInsn(new Integer(0));
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_VARS_INDEX);
        mv.visitLdcInsn(new Integer(0));
        mv.visitLdcInsn(new Integer(arity));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
        
        // callback to fill in method body
        body.compile(this);

        endMethod(mv);
        
        // return to previous method
        mv = getMethodVisitor();
        
        // method compiled, add to class
        getRubyClass();
        
        Label classAvailable = new Label();
        // if class is null, throw error
        mv.visitJumpInsn(Opcodes.IFNONNULL, classAvailable);
        newTypeError("No class to add method.");
        mv.visitInsn(Opcodes.ATHROW);
        
        mv.visitLabel(classAvailable);
        
        // only do Object#initialize check if necessary
        // this warns the user if they try to redefine initialize on Object, which would be bad
        if (name.equals("initialize")) {
            Label notObjectClass = new Label();
    
            // get class, compare to Object
            getRubyClass();
            loadRuntime();
            invokeIRuby("getObject", "()Lorg/jruby/RubyClass;");
            
            // if class == Object
            mv.visitJumpInsn(Opcodes.IF_ACMPNE, notObjectClass);
            loadRuntime();
            
            // display warning about redefining Object#initialize
            invokeIRuby("getWarnings", "()Lorg/jruby/common/RubyWarnings;");
            mv.visitLdcInsn("redefining Object#initialize may cause infinite loop");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/common/RubyWarnings", "warn", "(Ljava/lang/String;)V");
        
            mv.visitLabel(notObjectClass);
        }
        
        // TODO: fix this section for initialize visibility
//        mv.visitLdcInsn(iVisited.getName());
//        mv.visitLdcInsn("initialize");
//        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
//        Label l341 = new Label();
//        mv.visitJumpInsn(Opcodes.IFNE, l341);
//        getCurrentVisibility();
//        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/runtime/Visibility", "isModuleFunction", "()Z");
//        Label l342 = new Label();
//        mv.visitJumpInsn(Opcodes.IFEQ, l342);
//        mv.visitLabel(l341);
//        mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/Visibility", "PRIVATE", "Lorg/jruby/runtime/Visibility;");
//        mv.visitVarInsn(ASTORE, 7);
//        mv.visitLabel(l342);
        
        // Create a new method object to call the stub, passing it a new instance of our
        // current multistub and the appropriate index to call
        
        // get the class we're binding it to (this is the receiver for the addMethod
        // call way down below...)
        getRubyClass();
        
        // method name for addMethod call
        mv.visitLdcInsn(name);
        
        // new MultiStubMethod2
        mv.visitTypeInsn(Opcodes.NEW, "org/jruby/internal/runtime/methods/MultiStubMethod2");
        mv.visitInsn(Opcodes.DUP);
        
        { // this section sets up the parameters to the MultiStubMethod2 constructor
            // new multistub object
            mv.visitTypeInsn(Opcodes.NEW, classname + "$MultiStub" + multiStubCount);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classname + "$MultiStub" + multiStubCount, "<init>", "()V");

            // index for stub method
            mv.visitLdcInsn(new Integer(multiStubIndex));

            // implementingClass parameter
            getRubyClass();

            // TODO: handle args some way? maybe unnecessary with method compiled?
    //        mv.visitVarInsn(ALOAD, 4);
    //        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/ast/DefnNode", "getArgsNode", "()Lorg/jruby/ast/Node;");
    //        mv.visitTypeInsn(CHECKCAST, "org/jruby/ast/ArgsNode");
            mv.visitLdcInsn(new Integer(arity));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/jruby/runtime/Arity", "createArity", "(I)Lorg/jruby/runtime/Arity;");
            getCurrentVisibility();
        }
        
        // invoke MultiStubMethod2 constructor
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jruby/internal/runtime/methods/MultiStubMethod2", "<init>", "(Lorg/jruby/internal/runtime/methods/MultiStub2;ILorg/jruby/RubyModule;Lorg/jruby/runtime/Arity;Lorg/jruby/runtime/Visibility;)V");

        // add the method to the class, and we're set!
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jruby/RubyModule", "addMethod", "(Ljava/lang/String;Lorg/jruby/runtime/DynamicMethod;)V");

        // FIXME: this part is for invoking method_added or singleton_method_added
//        Label l345 = new Label();
//        mv.visitLabel(l345);
//        mv.visitLineNumber(538, l345);
//        loadThreadContext();
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/runtime/ThreadContext", "getCurrentVisibility", "()Lorg/jruby/runtime/Visibility;");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/runtime/Visibility", "isModuleFunction", "()Z");
//        Label l346 = new Label();
//        mv.visitJumpInsn(IFEQ, l346);
//        Label l347 = new Label();
//        mv.visitLabel(l347);
//        mv.visitLineNumber(539, l347);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "getSingletonClass", "()Lorg/jruby/MetaClass;");
//        mv.visitLdcInsn(iVisited.getName());
//        mv.visitTypeInsn(NEW, "org/jruby/internal/runtime/methods/WrapperCallable");
//        mv.visitInsn(DUP);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "getSingletonClass", "()Lorg/jruby/MetaClass;");
//        mv.visitVarInsn(ALOAD, 8);
//        mv.visitFieldInsn(GETSTATIC, "org/jruby/runtime/Visibility", "PUBLIC", "Lorg/jruby/runtime/Visibility;");
//        mv.visitMethodInsn(INVOKESPECIAL, "org/jruby/internal/runtime/methods/WrapperCallable", "<init>", "(Lorg/jruby/RubyModule;Lorg/jruby/runtime/ICallable;Lorg/jruby/runtime/Visibility;)V");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/MetaClass", "addMethod", "(Ljava/lang/String;Lorg/jruby/runtime/ICallable;)V");
//        Label l348 = new Label();
//        mv.visitLabel(l348);
//        mv.visitLineNumber(543, l348);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitLdcInsn("singleton_method_added");
//        loadRuntime();
//        mv.visitLdcInsn(iVisited.getName());
//        invokeIRuby("newSymbol", "(Ljava/lang/String;)Lorg/jruby/RubySymbol;");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitInsn(POP);
//        mv.visitLabel(l346);
//        mv.visitLineNumber(547, l346);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "isSingleton", "()Z");
//        Label l349 = new Label();
//        mv.visitJumpInsn(IFEQ, l349);
//        Label l350 = new Label();
//        mv.visitLabel(l350);
//        mv.visitLineNumber(548, l350);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitTypeInsn(CHECKCAST, "org/jruby/MetaClass");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/MetaClass", "getAttachedObject", "()Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitLdcInsn("singleton_method_added");
//        loadRuntime();
//        mv.visitVarInsn(ALOAD, 4);
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/ast/DefnNode", "getName", "()Ljava/lang/String;");
//        invokeIRuby("newSymbol", "(Ljava/lang/String;)Lorg/jruby/RubySymbol;");
//        mv.visitMethodInsn(INVOKEINTERFACE, "org/jruby/runtime/builtin/IRubyObject", "callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitInsn(POP);
//        Label l351 = new Label();
//        mv.visitJumpInsn(GOTO, l351);
//        mv.visitLabel(l349);
//        mv.visitLineNumber(551, l349);
//        mv.visitVarInsn(ALOAD, 5);
//        mv.visitLdcInsn("method_added");
//        loadRuntime();
//        mv.visitLdcInsn(iVisited.getName());
//        invokeIRuby("newSymbol", "(Ljava/lang/String;)Lorg/jruby/RubySymbol;");
//        mv.visitMethodInsn(INVOKEVIRTUAL, "org/jruby/RubyModule", "callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
//        mv.visitInsn(POP);
//        mv.visitLabel(l351);
//        mv.visitLineNumber(554, l351);
        
        // don't leave the stack hanging!
        loadRuntime();
        invokeIRuby("getNil", "()Lorg/jruby/runtime/builtin/IRubyObject;");
    }
}
