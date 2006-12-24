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
import org.jruby.ast.RootNode;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
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
    
    private static final int THREADCONTEXT_INDEX = 1;
    private static final int SELF_INDEX = 2;
    private static final int ARGS_INDEX = 3;
    private static final int RUNTIME_INDEX = 4;
    private static final int LOCAL_VARS_INDEX = 5;
    
    private Stack classVisitors = new Stack();
    private Stack methodVisitors = new Stack();
    private Stack arities = new Stack();
    
    private String classname;
    private String sourcename;
    
    Map classWriters = new HashMap();
    ClassWriter currentMultiStub = null;
    int multiStubIndex = -1;
    int multiStubCount = -1;
    
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
        mv.visitVarInsn(Opcodes.ALOAD, THREADCONTEXT_INDEX);
        mv.visitVarInsn(Opcodes.ALOAD, SELF_INDEX);
        mv.visitInsn(Opcodes.ACONST_NULL);
        
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, stubName, methodName, "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;");
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
                    classname + "$MultiStub" + multiStubCount, null, "java/lang/Object", new String[] {"org/jruby/internal/runtime/methods/MultiStub"});
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
        
        MethodVisitor newMethod = currentMultiStub.visitMethod(Opcodes.ACC_PUBLIC, "method" + multiStubIndex, "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;", null, null);
        pushMethodVisitor(newMethod);
        
        newMethod.visitCode();
        
        // logic to start off the root node's code with local var slots and all
        newMethod.visitLdcInsn(new Integer(localVarCount));
        newMethod.visitTypeInsn(Opcodes.ANEWARRAY, "org/jruby/runtime/builtin/IRubyObject");
        // FIXME: use constant for index of local vars
        newMethod.visitVarInsn(Opcodes.ASTORE, LOCAL_VARS_INDEX);
        
        // set up a local IRuby variable
        newMethod.visitVarInsn(Opcodes.ALOAD, THREADCONTEXT_INDEX);
        invokeThreadContext("getRuntime", "()Lorg/jruby/IRuby;");
        newMethod.visitVarInsn(Opcodes.ASTORE, RUNTIME_INDEX);
        
        // push down the argument count of this method
        pushArity(arity);
        
        return newMethod;
    }
    
    public void endMethod(Object token) {
        assert token instanceof MethodVisitor;
        
        MethodVisitor mv = (MethodVisitor)token;
        // return last value from execution
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1); // automatically calculated by ASM
        mv.visitEnd();
        
        popMethodVisitor();
        popArity();
    }
    
    public void closeOutMultiStub() {
        if (currentMultiStub != null) {
            while (multiStubIndex < 9) {
                multiStubIndex++;
                MethodVisitor multiStubMethod = currentMultiStub.visitMethod(Opcodes.ACC_PUBLIC, "method" + multiStubIndex, "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/runtime/builtin/IRubyObject;[Lorg/jruby/runtime/builtin/IRubyObject;)Lorg/jruby/runtime/builtin/IRubyObject;", null, null);
                multiStubMethod.visitCode();
                multiStubMethod.visitInsn(Opcodes.ACONST_NULL);
                multiStubMethod.visitInsn(Opcodes.ARETURN);
                multiStubMethod.visitMaxs(1, 1);
                multiStubMethod.visitEnd();
            }
        }
    }
    
    public void lineNumber(Node node) {
        if (lastLine == (lastLine = node.getPosition().getEndLine())) return; // did not change lines for this node, don't bother relabeling
        
        Label l = new Label();
        MethodVisitor mv = getMethodVisitor();
        mv.visitLabel(l);
        mv.visitLineNumber(node.getPosition().getEndLine(), l);
    }
    
    public void invokeDynamicFunction(String name, int argCount) {
        if (argCount == 0) {
            loadSelf();
            loadThreadContext();
            
            MethodVisitor mv = getMethodVisitor();

            mv.visitLdcInsn(name);

            mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/builtin/IRubyObject", "NULL_ARRAY", "[Lorg/jruby/runtime/builtin/IRubyObject;");
            mv.visitFieldInsn(Opcodes.GETSTATIC, "org/jruby/runtime/CallType", "FUNCTIONAL", "Lorg/jruby/runtime/CallType;");

            invokeIRubyObject("callMethod", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;[Lorg/jruby/runtime/builtin/IRubyObject;Lorg/jruby/runtime/CallType;)Lorg/jruby/runtime/builtin/IRubyObject;");
        }
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
    
    public void consumeCurrentValue() {
        getMethodVisitor().visitInsn(Opcodes.POP);
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

    private void invokeThreadContext(String methodName, String signature) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THREADCONTEXT, methodName, signature);
    }

    private void invokeIRuby(String methodName, String signature) {
        MethodVisitor mv = getMethodVisitor();
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, IRUBY, methodName, signature);
    }
}
