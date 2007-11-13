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
 * Copyright (C) 2006 The JRuby Community <www.jruby.org>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.internal.runtime.methods;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.parser.StaticScope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.util.CheckClassAdapter;

public class InvocationMethodFactory extends MethodFactory implements Opcodes {
    public final static CodegenUtils cg = CodegenUtils.cg;
    private final static String COMPILED_SUPER_CLASS = CompiledMethod.class.getName().replace('.','/');
    private final static String COMPILED_CALL_SIG = cg.sig(IRubyObject.class,
            cg.params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
    private final static String COMPILED_SUPER_SIG = 
            cg.sig(Void.TYPE, RubyModule.class, Arity.class, Visibility.class, StaticScope.class, Object.class, CallConfiguration.class);
    private final static String JAVA_SUPER_SIG = cg.sig(Void.TYPE, cg.params(RubyModule.class, Visibility.class));
    private final static String JAVA_INDEXED_SUPER_SIG = cg.sig(Void.TYPE, cg.params(RubyModule.class, Visibility.class, int.class));

    private JRubyClassLoader classLoader;
    
    public InvocationMethodFactory() {
    }
    
    public InvocationMethodFactory(ClassLoader classLoader) {
        if (classLoader instanceof JRubyClassLoader) {
            this.classLoader = (JRubyClassLoader)classLoader;
        } else {
           this.classLoader = new JRubyClassLoader(classLoader);
        }
    }
    
    /**
     * Creates a class path name, from a Class.
     */
    private static String p(Class n) {
        return n.getName().replace('.','/');
    }

    private void checkArity(JRubyMethod jrubyMethod, SkinnyMethodAdapter mv) {

        // check arity
        Label arityError = new Label();
        Label noArityError = new Label();

        if (jrubyMethod.rest()) {
            if (jrubyMethod.required() > 0) {
                // just confirm minimum args provided
                mv.aload(ARGS_INDEX);
                mv.arraylength();
                mv.ldc(jrubyMethod.required());
                mv.if_icmplt(arityError);
            }
        } else if (jrubyMethod.optional() > 0) {
            if (jrubyMethod.required() > 0) {
                // confirm minimum args provided
                mv.aload(ARGS_INDEX);
                mv.arraylength();
                mv.ldc(jrubyMethod.required());
                mv.if_icmplt(arityError);
            }

            // confirm maximum not greater than optional
            mv.aload(ARGS_INDEX);
            mv.arraylength();
            mv.ldc(jrubyMethod.required() + jrubyMethod.optional());
            mv.if_icmpgt(arityError);
        } else {
            // just confirm args length == required
            mv.aload(ARGS_INDEX);
            mv.arraylength();
            mv.ldc(jrubyMethod.required());
            mv.if_icmpne(arityError);
        }

        mv.go_to(noArityError);

        mv.label(arityError);
        mv.aload(THREADCONTEXT_INDEX);
        mv.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
        mv.aload(ARGS_INDEX);
        mv.ldc(jrubyMethod.required());
        mv.ldc(jrubyMethod.required() + jrubyMethod.optional());
        mv.invokestatic(cg.p(Arity.class), "checkArgumentCount", cg.sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
        mv.pop();

        mv.label(noArityError);
    }

    private ClassWriter createCompiledCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", COMPILED_SUPER_SIG, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", COMPILED_SUPER_SIG);
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        return cw;
    }

    private ClassWriter createJavaMethodCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", JAVA_SUPER_SIG, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", JAVA_SUPER_SIG);
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        return cw;
    }

    private ClassWriter createIndexedJavaMethodCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", JAVA_INDEXED_SUPER_SIG, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", JAVA_INDEXED_SUPER_SIG);
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        return cw;
    }

    private void handleRedo(Label tryRedoJump, SkinnyMethodAdapter mv, Label tryFinally) {

        // redo jump handling
        {
            mv.label(tryRedoJump);

            // clear the redo
            mv.pop();

            // get runtime, dup it
            mv.aload(1);
            mv.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
            mv.dup();

            // get nil
            mv.invokevirtual(cg.p(Ruby.class), "getNil", cg.sig(IRubyObject.class));

            // load "redo" under nil
            mv.ldc("redo");
            mv.swap();

            // load "unexpected redo" message
            mv.ldc("unexpected redo");

            mv.invokevirtual(cg.p(Ruby.class), "newLocalJumpError", cg.sig(RaiseException.class, cg.params(String.class, IRubyObject.class, String.class)));
            mv.go_to(tryFinally);
        }

        // finally handling for abnormal exit
    }

    private void handleReturn(SkinnyMethodAdapter mv, Label tryFinally, Label tryReturnJump) {

        // return jump handling
        {
            mv.label(tryReturnJump);

            // dup return jump, get target, compare to this method object
            mv.dup();
            mv.invokevirtual(cg.p(JumpException.FlowControlException.class), "getTarget", cg.sig(JumpTarget.class));
            mv.aload(0);
            Label rethrow = new Label();
            mv.if_acmpne(rethrow);

            // this is the target, store return value and branch to normal exit
            mv.invokevirtual(cg.p(JumpException.FlowControlException.class), "getValue", cg.sig(Object.class));

            mv.areturn();

            // this is not the target, rethrow
            mv.label(rethrow);
            mv.go_to(tryFinally);
        }
    }

    private void invokeCallConfigPost(SkinnyMethodAdapter mv) {
        //call post method stuff (non-finally)
        mv.aload(0); // load method to get callconfig
        mv.getfield(cg.p(JavaMethod.class), "callConfig", cg.ci(CallConfiguration.class));
        mv.aload(1);
        mv.invokevirtual(cg.p(CallConfiguration.class), "post", cg.sig(void.class, cg.params(ThreadContext.class)));
    }

    private void invokeCallConfigPre(SkinnyMethodAdapter mv) {
        // invoke pre method stuff
        mv.aload(0); // load method to get callconfig
        mv.getfield(cg.p(DynamicMethod.class), "callConfig", cg.ci(CallConfiguration.class));

        // load pre params
        mv.aload(THREADCONTEXT_INDEX); // tc
        mv.aload(RECEIVER_INDEX); // self
        mv.aload(0);
        mv.invokevirtual(cg.p(DynamicMethod.class), "getImplementationClass", cg.sig(RubyModule.class)); // clazz
        mv.aload(0);
        mv.getfield(cg.p(JavaMethod.class), "arity", cg.ci(Arity.class)); // arity
        mv.aload(NAME_INDEX); // name
        mv.aload(ARGS_INDEX); // args
        mv.aload(BLOCK_INDEX); // block
        mv.aconst_null(); // scope
        mv.aload(0); // jump target
        mv.invokevirtual(cg.p(CallConfiguration.class), "pre", cg.sig(void.class, cg.params(ThreadContext.class, IRubyObject.class, RubyModule.class, Arity.class, String.class, IRubyObject[].class, Block.class, StaticScope.class, JumpTarget.class)));
    }

    private void loadArguments(SkinnyMethodAdapter mv, JRubyMethod jrubyMethod) {
        // load args
        if (jrubyMethod.optional() == 0 && !jrubyMethod.rest()) {
            // only required args
            loadArguments(mv, ARGS_INDEX, jrubyMethod.required());
        } else {
            // load args as-is
            mv.visitVarInsn(ALOAD, ARGS_INDEX);
        }
    }

    private void loadBlock(boolean getsBlock, SkinnyMethodAdapter mv) {
        // load block if it accepts block
        if (getsBlock) {
            mv.visitVarInsn(ALOAD, BLOCK_INDEX);
        }
    }

    private void loadReceiver(String typePath, Method method, SkinnyMethodAdapter mv) {
        // load target for invocations
        if (Modifier.isStatic(method.getModifiers())) {
            // load self object as IRubyObject, for recv param
            mv.aload(RECEIVER_INDEX);
        } else {
            // load receiver as original type for virtual invocation
            mv.aload(RECEIVER_INDEX);
            mv.checkcast(typePath);
        }
    }

    private Class tryClass(Ruby runtime, String name) {
        try {
            if (classLoader == null) {
                return Class.forName(name, true, runtime.getJRubyClassLoader());
            }
             
            return classLoader.loadClass(name);
        } catch(Exception e) {
            return null;
        }
    }

    protected Class endCall(Ruby runtime, ClassWriter cw, MethodVisitor mv, String name) {
        endMethod(mv);
        return endClass(runtime, cw, name);
    }

    protected void endMethod(MethodVisitor mv) {
        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    protected Class endClass(Ruby runtime, ClassWriter cw, String name) {
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        CheckClassAdapter.verify(new ClassReader(code), false, new PrintWriter(System.err));
        if (classLoader == null) classLoader = runtime.getJRubyClassLoader();
         
        return classLoader.defineClass(name, code);
    }
    
    public static final int SCRIPT_INDEX = 0;
    public static final int THIS_INDEX = 0;
    public static final int THREADCONTEXT_INDEX = 1;
    public static final int RECEIVER_INDEX = 2;
    public static final int CLASS_INDEX = 3;
    public static final int NAME_INDEX = 4;
    public static final int ARGS_INDEX = 5;
    public static final int BLOCK_INDEX = 6;
    
    private void loadArguments(MethodVisitor mv, int argsIndex, int count) {
        for (int i = 0; i < count; i++) {
            loadArgument(mv, argsIndex, i);
        }
    }
    
    private void loadArgument(MethodVisitor mv, int argsIndex, int argIndex) {
        mv.visitVarInsn(ALOAD, argsIndex);
        mv.visitLdcInsn(new Integer(argIndex));
        mv.visitInsn(AALOAD);
    }

    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, Method method) {
        Class type = method.getDeclaringClass();
        JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);
        String typePath = p(type);
        String javaMethodName = method.getName();
        
        String generatedClassName = type.getName() + "Invoker$" + javaMethodName + "_method_" + jrubyMethod.required() + "_" + jrubyMethod.optional();
        String generatedClassPath = typePath + "Invoker$" + javaMethodName + "_method_" + jrubyMethod.required() + "_" + jrubyMethod.optional();
        
        Class c = tryClass(implementationClass.getRuntime(), generatedClassName);
        
        try {
            if (c == null) {
                ClassWriter cw = createJavaMethodCtor(generatedClassPath, cg.p(JavaMethod.class));
                SkinnyMethodAdapter mv = null;
                
                mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", COMPILED_CALL_SIG, null, null));
                mv.visitCode();
                Label line = new Label();
                mv.visitLineNumber(0, line);
                
                createAnnotatedMethodInvocation(method, mv);
                
                c = endCall(implementationClass.getRuntime(), cw, mv, generatedClassName);
            }
                
            JavaMethod ic = (JavaMethod)c.getConstructor(new Class[]{RubyModule.class, Visibility.class}).newInstance(new Object[]{implementationClass, jrubyMethod.visibility()});

            boolean fast = !(jrubyMethod.frame() || jrubyMethod.scope());
            ic.setArity(Arity.fromAnnotation(jrubyMethod));
            ic.setJavaName(javaMethodName);
            ic.setArgumentTypes(method.getParameterTypes());
            ic.setSingleton(Modifier.isStatic(method.getModifiers()));
            if (fast) {
                ic.setCallConfig(CallConfiguration.NO_FRAME_NO_SCOPE);
            } else {
                ic.setCallConfig(CallConfiguration.FRAME_ONLY);
            }
            return ic;
        } catch(Exception e) {
            e.printStackTrace();
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    public void defineIndexedAnnotatedMethods(RubyModule implementationClass, Class type, MethodDefiningCallback callback) {
        String typePath = p(type);
        
        String generatedClassName = type.getName() + "Invoker";
        String generatedClassPath = typePath + "Invoker";
        
        Class c = tryClass(implementationClass.getRuntime(), generatedClassName);
        
        try {
            ArrayList<Method> annotatedMethods = new ArrayList();
            Method[] methods = type.getDeclaredMethods();
            for (Method method : methods) {
                JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);

                if (jrubyMethod == null) continue;

                annotatedMethods.add(method);
            }
            // To ensure the method cases are generated the same way every time, we make a second sorted list
            ArrayList<Method> sortedMethods = new ArrayList(annotatedMethods);
            Collections.sort(sortedMethods, new Comparator<Method>() {
                public int compare(Method a, Method b) {
                    return a.getName().compareTo(b.getName());
                }
            });
            // But when binding the methods, we want to use the order from the original class, so we save the indices
            HashMap<Method,Integer> indexMap = new HashMap();
            for (int index = 0; index < sortedMethods.size(); index++) {
                indexMap.put(sortedMethods.get(index), index);
            }
            
            if (c == null) {
                ClassWriter cw = createIndexedJavaMethodCtor(generatedClassPath, cg.p(JavaMethod.class));
                SkinnyMethodAdapter mv = null;
                
                mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", COMPILED_CALL_SIG, null, null));
                mv.visitCode();
                Label line = new Label();
                mv.visitLineNumber(0, line);
                
                Label defaultCase = new Label();
                Label[] cases = new Label[sortedMethods.size()];
                for (int i = 0; i < cases.length; i++) cases[i] = new Label();
                
                // load method index
                mv.aload(THIS_INDEX);
                mv.getfield(generatedClassPath, "methodIndex", cg.ci(int.class));
                
                mv.tableswitch(0, cases.length - 1, defaultCase, cases);
                
                for (int i = 0; i < sortedMethods.size(); i++) {
                    mv.label(cases[i]);
                    String callName = getAnnotatedMethodForIndex(cw, sortedMethods.get(i), i);
                    
                    // invoke call#_method for method
                    mv.aload(THIS_INDEX);
                    mv.aload(THREADCONTEXT_INDEX);
                    mv.aload(RECEIVER_INDEX);
                    mv.aload(CLASS_INDEX);
                    mv.aload(NAME_INDEX);
                    mv.aload(ARGS_INDEX);
                    mv.aload(BLOCK_INDEX);
                    
                    mv.invokevirtual(generatedClassPath, callName, COMPILED_CALL_SIG);
                    mv.areturn();
                }
                
                // if we fall off the switch, error.
                mv.label(defaultCase);
                mv.aload(THREADCONTEXT_INDEX);
                mv.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
                mv.ldc("Error: fell off switched invoker for class: " + implementationClass.getBaseName());
                mv.invokevirtual(cg.p(Ruby.class), "newRuntimeError", cg.sig(RaiseException.class, String.class));
                mv.athrow();
                
                c = endCall(implementationClass.getRuntime(), cw, mv, generatedClassName);
            }

            for (int i = 0; i < annotatedMethods.size(); i++) {
                Method method = annotatedMethods.get(i);
                JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);
                
                if (jrubyMethod.frame()) {
                    for (String name : jrubyMethod.name()) {
                        ASTInspector.FRAME_AWARE_METHODS.add(name);
                    }
                }
                
                int index = indexMap.get(method);
                JavaMethod ic = (JavaMethod)c.getConstructor(new Class[]{RubyModule.class, Visibility.class, int.class}).newInstance(new Object[]{implementationClass, jrubyMethod.visibility(), index});

                boolean fast = !(jrubyMethod.frame() || jrubyMethod.scope());
                ic.setArity(Arity.fromAnnotation(jrubyMethod));
                ic.setJavaName(method.getName());
                ic.setArgumentTypes(method.getParameterTypes());
                ic.setSingleton(Modifier.isStatic(method.getModifiers()));
                if (fast) {
                    ic.setCallConfig(CallConfiguration.NO_FRAME_NO_SCOPE);
                } else {
                    ic.setCallConfig(CallConfiguration.FRAME_ONLY);
                }

                callback.define(implementationClass, method, ic);
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    private String getAnnotatedMethodForIndex(ClassWriter cw, Method method, int index) {
        String methodName = "call" + index + "_" + method.getName();
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG, null, null));
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        createAnnotatedMethodInvocation(method, mv);
        endMethod(mv);
        
        return methodName;
    }

    private void createAnnotatedMethodInvocation(Method javaMethod, SkinnyMethodAdapter method) {
        JRubyMethod jrubyMethod = javaMethod.getAnnotation(JRubyMethod.class);
        String typePath = p(javaMethod.getDeclaringClass());
        String javaMethodName = javaMethod.getName();
        Class[] signature = javaMethod.getParameterTypes();
        Class ret = javaMethod.getReturnType();

        checkArity(jrubyMethod,method);
        
        boolean fast = !(jrubyMethod.frame() || jrubyMethod.scope());
        if (!fast) {
            invokeCallConfigPre(method);
        }

        boolean getsBlock = signature.length > 0 && signature[signature.length - 1] == Block.class;

        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label tryFinally = new Label();
        Label tryReturnJump = new Label();
        Label tryRedoJump = new Label();
        Label normalExit = new Label();

        method.trycatch(tryBegin, tryEnd, tryReturnJump, cg.p(JumpException.ReturnJump.class));
        method.trycatch(tryBegin, tryEnd, tryRedoJump, cg.p(JumpException.RedoJump.class));
        method.trycatch(tryBegin, tryEnd, tryFinally, null);
        
        method.label(tryBegin);
        {
            loadReceiver(typePath, javaMethod, method);
            loadArguments(method, jrubyMethod);
            loadBlock(getsBlock, method);

            if (Modifier.isStatic(javaMethod.getModifiers())) {
                // static invocation
                method.visitMethodInsn(INVOKESTATIC, typePath, javaMethodName, cg.sig(ret, signature));
            } else {
                // virtual invocation
                method.visitMethodInsn(INVOKEVIRTUAL, typePath, javaMethodName, cg.sig(ret, signature));
            }
        }
        method.label(tryEnd);
        
        method.label(normalExit);
        
        if (!fast) {
            invokeCallConfigPost(method);
        }
        
        method.visitInsn(ARETURN);
        
        handleReturn(method, tryFinally, tryReturnJump);
        
        handleRedo(tryRedoJump, method, tryFinally);

        // finally handling for abnormal exit
        method.label(tryFinally);
        {
            if (!fast) {
                invokeCallConfigPost(method);
            }

            // rethrow exception
            method.athrow(); // rethrow it
        }
    }

    private DynamicMethod getCompleteMethod(
            RubyModule implementationClass, String method, Arity arity, 
            Visibility visibility, StaticScope scope, String sup, 
            Object scriptObject, CallConfiguration callConfig) {
        
        Class scriptClass = scriptObject.getClass();
        String typePath = p(scriptClass);
        String mname = scriptClass.getName() + "Invoker" + method + arity;
        String mnamePath = typePath + "Invoker" + method + arity;
        Class generatedClass = tryClass(implementationClass.getRuntime(), mname);
        
        try {
            if (generatedClass == null) {
                ClassWriter cw = createCompiledCtor(mnamePath,sup);
                SkinnyMethodAdapter mv = null;
                
                mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", COMPILED_CALL_SIG, null, null));
                mv.visitCode();
                Label line = new Label();
                mv.visitLineNumber(0, line);
                
                // invoke pre method stuff
                if (callConfig != CallConfiguration.NO_FRAME_NO_SCOPE) {
                    mv.aload(0); // load method to get callconfig
                    mv.getfield(cg.p(CompiledMethod.class), "callConfig", cg.ci(CallConfiguration.class));
                    mv.aload(THREADCONTEXT_INDEX); // tc
                    mv.aload(RECEIVER_INDEX); // self

                    // determine the appropriate class, for super calls to work right
                    mv.aload(0);
                    mv.invokevirtual(cg.p(CompiledMethod.class), "getImplementationClass", cg.sig(RubyModule.class));

                    mv.aload(0);
                    mv.getfield(cg.p(CompiledMethod.class), "arity", cg.ci(Arity.class)); // arity
                    mv.aload(NAME_INDEX); // name
                    mv.aload(ARGS_INDEX); // args
                    mv.aload(BLOCK_INDEX); // block
                    mv.aload(0);
                    mv.getfield(cg.p(CompiledMethod.class), "staticScope", cg.ci(StaticScope.class));
                    // static scope
                    mv.aload(0); // jump target
                    mv.invokevirtual(cg.p(CallConfiguration.class), "pre", 
                            cg.sig(void.class, 
                            cg.params(ThreadContext.class, IRubyObject.class, RubyModule.class, Arity.class, String.class, IRubyObject[].class, Block.class, 
                            StaticScope.class, JumpTarget.class)));
                }
                
                // store null for result var
                mv.aconst_null();
                mv.astore(8);
                    
                Label tryBegin = new Label();
                Label tryEnd = new Label();
                Label tryFinally = new Label();
                Label tryReturnJump = new Label();
                Label tryRedoJump = new Label();
                Label normalExit = new Label();
                
                mv.trycatch(tryBegin, tryEnd, tryReturnJump, cg.p(JumpException.ReturnJump.class));
                mv.trycatch(tryBegin, tryEnd, tryRedoJump, cg.p(JumpException.RedoJump.class));
                mv.trycatch(tryBegin, tryEnd, tryFinally, null);
                mv.label(tryBegin);
                
                mv.aload(0);
                // FIXME we want to eliminate these type casts when possible
                mv.getfield(mnamePath, "$scriptObject", cg.ci(Object.class));
                mv.checkcast(typePath);
                mv.aload(THREADCONTEXT_INDEX);
                mv.aload(RECEIVER_INDEX);
                mv.aload(ARGS_INDEX);
                mv.aload(BLOCK_INDEX);
                mv.invokevirtual(typePath, method, cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class)));
                
                // store result in temporary variable 8
                mv.astore(8);

                mv.label(tryEnd);

                //call post method stuff (non-finally)
                mv.label(normalExit);
                if (callConfig != CallConfiguration.NO_FRAME_NO_SCOPE) {
                    mv.aload(0); // load method to get callconfig
                    mv.getfield(cg.p(DynamicMethod.class), "callConfig", cg.ci(CallConfiguration.class));
                    mv.aload(1);
                    mv.invokevirtual(cg.p(CallConfiguration.class), "post", cg.sig(void.class, cg.params(ThreadContext.class)));
                }
                // reload and return result
                mv.aload(8);
                mv.visitInsn(ARETURN);

                // return jump handling
                {
                    mv.label(tryReturnJump);
                    
                    // dup return jump, get target, compare to this method object
                    mv.dup();
                    mv.invokevirtual(cg.p(JumpException.FlowControlException.class), "getTarget", cg.sig(JumpTarget.class));
                    mv.aload(0);
                    Label rethrow = new Label();
                    mv.if_acmpne(rethrow);

                    // this is the target, store return value and branch to normal exit
                    mv.invokevirtual(cg.p(JumpException.FlowControlException.class), "getValue", cg.sig(Object.class));
                    
                    mv.astore(8);
                    mv.go_to(normalExit);

                    // this is not the target, rethrow
                    mv.label(rethrow);
                    mv.go_to(tryFinally);
                }

                // redo jump handling
                {
                    mv.label(tryRedoJump);
                    
                    // clear the redo
                    mv.pop();
                    
                    // get runtime, dup it
                    mv.aload(1);
                    mv.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
                    mv.dup();
                    
                    // get nil
                    mv.invokevirtual(cg.p(Ruby.class), "getNil", cg.sig(IRubyObject.class));
                    
                    // load "redo" under nil
                    mv.ldc("redo");
                    mv.swap();
                    
                    // load "unexpected redo" message
                    mv.ldc("unexpected redo");
                    
                    mv.invokevirtual(cg.p(Ruby.class), "newLocalJumpError", cg.sig(RaiseException.class, cg.params(String.class, IRubyObject.class, String.class)));
                    mv.go_to(tryFinally);
                }

                // finally handling for abnormal exit
                {
                    mv.label(tryFinally);

                    //call post method stuff (exception raised)
                    if (callConfig != CallConfiguration.NO_FRAME_NO_SCOPE) {
                        mv.aload(0); // load method to get callconfig
                        mv.getfield(cg.p(DynamicMethod.class), "callConfig", cg.ci(CallConfiguration.class));
                        mv.aload(1);
                        mv.invokevirtual(cg.p(CallConfiguration.class), "post", cg.sig(void.class, cg.params(ThreadContext.class)));
                    }

                    // rethrow exception
                    mv.athrow(); // rethrow it
                }
                
                generatedClass = endCall(implementationClass.getRuntime(), cw,mv,mname);
            }
            
            return (DynamicMethod)generatedClass
                    .getConstructor(RubyModule.class, Arity.class, Visibility.class, StaticScope.class, Object.class, CallConfiguration.class)
                    .newInstance(implementationClass, arity, visibility, scope, scriptObject, callConfig);
        } catch(Exception e) {
            e.printStackTrace();
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    public DynamicMethod getCompiledMethod(
            RubyModule implementationClass, String method, Arity arity, 
            Visibility visibility, StaticScope scope, Object scriptObject, CallConfiguration callConfig) {
        return getCompleteMethod(implementationClass,method,arity,visibility,scope, COMPILED_SUPER_CLASS, scriptObject, callConfig);
    }
}
