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
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.parser.StaticScope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import static org.jruby.util.CodegenUtils.*;
import static java.lang.System.*;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * In order to avoid the overhead with reflection-based method handles, this
 * MethodFactory uses ASM to generate tiny invoker classes. This allows for
 * better performance and more specialization per-handle than can be supported
 * via reflection. It also allows optimizing away many conditionals that can
 * be determined once ahead of time.
 * 
 * When running in secured environments, this factory may not function. When
 * this can be detected, MethodFactory will fall back on the reflection-based
 * factory instead.
 * 
 * @see org.jruby.internal.runtime.methods.MethodFactory
 */
public class InvocationMethodFactory extends MethodFactory implements Opcodes {
    private static final boolean DEBUG = false;
    
    /** The pathname of the super class for compiled Ruby method handles. */ 
    private final static String COMPILED_SUPER_CLASS = p(CompiledMethod.class);
    
    /** The outward call signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class));
    
    /** The outward call signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ZERO_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ZERO = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ONE_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, Block.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_ONE = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_TWO_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_TWO = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_THREE_BLOCK = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    
    /** The outward arity-zero call-with-block signature for compiled Ruby method handles. */
    private final static String COMPILED_CALL_SIG_THREE = sig(IRubyObject.class,
            params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
    
    /** The super constructor signature for Java-based method handles. */
    private final static String JAVA_SUPER_SIG = sig(Void.TYPE, params(RubyModule.class, Visibility.class));
    
    /** The super constructor signature for indexed Java-based method handles. */
    private final static String JAVA_INDEXED_SUPER_SIG = sig(Void.TYPE, params(RubyModule.class, Visibility.class, int.class));
    
    /** The lvar index of "this" */
    public static final int THIS_INDEX = 0;
    
    /** The lvar index of the passed-in ThreadContext */
    public static final int THREADCONTEXT_INDEX = 1;
    
    /** The lvar index of the method-receiving object */
    public static final int RECEIVER_INDEX = 2;
    
    /** The lvar index of the RubyClass being invoked against */
    public static final int CLASS_INDEX = 3;
    
    /** The lvar index method name being invoked */
    public static final int NAME_INDEX = 4;
    
    /** The lvar index of the method args on the call */
    public static final int ARGS_INDEX = 5;
    
    /** The lvar index of the passed-in Block on the call */
    public static final int BLOCK_INDEX = 6;

    /** The classloader to use for code loading */
    protected JRubyClassLoader classLoader;
    
    /**
     * Whether this factory has seen undefined methods already. This is used to
     * detect likely method handle collisions when we expect to create a new
     * handle for each call.
     */
    private boolean seenUndefinedClasses = false;

    /**
     * Whether we've informed the user that we've seen undefined methods; this
     * is to avoid a flood of repetitive information.
     */
    private boolean haveWarnedUser = false;
    
    /**
     * Construct a new InvocationMethodFactory using the specified classloader
     * to load code. If the target classloader is not an instance of
     * JRubyClassLoader, it will be wrapped with one.
     * 
     * @param classLoader The classloader to use, or to wrap if it is not a
     * JRubyClassLoader instance.
     */
    public InvocationMethodFactory(ClassLoader classLoader) {
        if (classLoader instanceof JRubyClassLoader) {
            this.classLoader = (JRubyClassLoader)classLoader;
        } else {
           this.classLoader = new JRubyClassLoader(classLoader);
        }
    }

    /**
     * Use code generation to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethodLazily(
            RubyModule implementationClass, String method, Arity arity, 
            Visibility visibility, StaticScope scope, Object scriptObject, CallConfiguration callConfig) {
        return new CompiledMethod.LazyCompiledMethod(implementationClass, method, arity, visibility, scope, scriptObject, callConfig,
                new InvocationMethodFactory(classLoader));
    }
            

    /**
     * Use code generation to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethod(
            RubyModule implementationClass, String method, Arity arity, 
            Visibility visibility, StaticScope scope, Object scriptObject, CallConfiguration callConfig) {
        String sup = COMPILED_SUPER_CLASS;
        Class scriptClass = scriptObject.getClass();
        String mname = scriptClass.getName() + "Invoker" + method + arity;
        synchronized (classLoader) {
            Class generatedClass = tryClass(mname, scriptClass);

            try {
                if (generatedClass == null) {
                    String typePath = p(scriptClass);
                    String mnamePath = typePath + "Invoker" + method + arity;
                    ClassWriter cw = createCompiledCtor(mnamePath,sup);
                    SkinnyMethodAdapter mv = null;
                    String signature = null;
                    boolean specificArity = false;
                    
                    if (scope.getRestArg() >= 0 || scope.getOptionalArgs() > 0 || scope.getRequiredArgs() > 3) {
                        signature = COMPILED_CALL_SIG_BLOCK;
                        mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", signature, null, null));
                    } else {
                        specificArity = true;
                        
                        mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", COMPILED_CALL_SIG_BLOCK, null, null));
                        mv.start();
                        
                        // check arity
                        mv.aload(1);
                        mv.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                        mv.aload(5);
                        mv.pushInt(scope.getRequiredArgs());
                        mv.pushInt(scope.getRequiredArgs());
                        mv.invokestatic(p(Arity.class), "checkArgumentCount", sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
                        mv.pop();
                        
                        mv.aload(0);
                        mv.aload(1);
                        mv.aload(2);
                        mv.aload(3);
                        mv.aload(4);
                        for (int i = 0; i < scope.getRequiredArgs(); i++) {
                            mv.aload(5);
                            mv.ldc(i);
                            mv.arrayload();
                        }
                        mv.aload(6);

                        switch (scope.getRequiredArgs()) {
                        case 0:
                            signature = COMPILED_CALL_SIG_ZERO_BLOCK;
                            break;
                        case 1:
                            signature = COMPILED_CALL_SIG_ONE_BLOCK;
                            break;
                        case 2:
                            signature = COMPILED_CALL_SIG_TWO_BLOCK;
                            break;
                        case 3:
                            signature = COMPILED_CALL_SIG_THREE_BLOCK;
                            break;
                        }
                        
                        mv.invokevirtual(mnamePath, "call", signature);
                        mv.areturn();
                        mv.end();
                        
                        mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", signature, null, null));
                    }

                    mv.visitCode();
                    Label line = new Label();
                    mv.visitLineNumber(0, line);
                    
//                    // check arity
//                    checkArity(mv, scope);

                    // invoke pre method stuff
                    if (!callConfig.isNoop()) {
                        if (specificArity) {
                            invokeCallConfigPre(mv, COMPILED_SUPER_CLASS, scope.getRequiredArgs(), true, callConfig);
                        } else {
                            invokeCallConfigPre(mv, COMPILED_SUPER_CLASS, -1, true, callConfig);
                        }
                    }

                    Label tryBegin = new Label();
                    Label tryEnd = new Label();
                    Label doFinally = new Label();
                    Label doReturnFinally = new Label();
                    Label doRedoFinally = new Label();
                    Label catchReturnJump = new Label();
                    Label catchRedoJump = new Label();

                    if (callConfig != CallConfiguration.FRAME_AND_DUMMY_SCOPE) {
                        mv.trycatch(tryBegin, tryEnd, catchReturnJump, p(JumpException.ReturnJump.class));
                    }
                    mv.trycatch(tryBegin, tryEnd, catchRedoJump, p(JumpException.RedoJump.class));
                    mv.trycatch(tryBegin, tryEnd, doFinally, null);
                    if (callConfig != CallConfiguration.FRAME_AND_DUMMY_SCOPE) {
                        mv.trycatch(catchReturnJump, doReturnFinally, doFinally, null);
                    }
                    mv.trycatch(catchRedoJump, doRedoFinally, doFinally, null);
                    mv.label(tryBegin);
                    {
                        mv.aload(0);
                        // FIXME we want to eliminate these type casts when possible
                        mv.getfield(mnamePath, "$scriptObject", ci(Object.class));
                        mv.checkcast(typePath);
                        mv.aload(THREADCONTEXT_INDEX);
                        mv.aload(RECEIVER_INDEX);
                        if (specificArity) {
                            for (int i = 0; i < scope.getRequiredArgs(); i++) {
                                mv.aload(ARGS_INDEX + i);
                            }
                            mv.aload(ARGS_INDEX + scope.getRequiredArgs());
                            mv.invokevirtual(typePath, method, StandardASMCompiler.METHOD_SIGNATURES[scope.getRequiredArgs()]);
                        } else {
                            mv.aload(ARGS_INDEX);
                            mv.aload(BLOCK_INDEX);
                            mv.invokevirtual(typePath, method, StandardASMCompiler.METHOD_SIGNATURES[4]);
                        }
                    }
                    mv.label(tryEnd);
                    
                    // normal exit, perform finally and return
                    {
                        if (!callConfig.isNoop()) {
                            invokeCallConfigPost(mv, COMPILED_SUPER_CLASS, callConfig);
                        }
                        mv.visitInsn(ARETURN);
                    }

                    // return jump handling
                    if (callConfig != CallConfiguration.FRAME_AND_DUMMY_SCOPE) {
                        mv.label(catchReturnJump);
                        {
                            mv.aload(0);
                            mv.swap();
                            mv.aload(1);
                            mv.swap();
                            mv.invokevirtual(COMPILED_SUPER_CLASS, "handleReturn", sig(IRubyObject.class, ThreadContext.class, JumpException.ReturnJump.class));
                            mv.label(doReturnFinally);

                            // finally
                            if (!callConfig.isNoop()) {
                                invokeCallConfigPost(mv, COMPILED_SUPER_CLASS, callConfig);
                            }

                            // return result if we're still good
                            mv.areturn();
                        }
                    }

                    // redo jump handling
                    mv.label(catchRedoJump);
                    {
                        // clear the redo
                        mv.pop();
                        
                        // get runtime, create jump error, and throw it
                        mv.aload(1);
                        mv.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                        mv.invokevirtual(p(Ruby.class), "newRedoLocalJumpError", sig(RaiseException.class));
                        mv.label(doRedoFinally);
                        
                        // finally
                        if (!callConfig.isNoop()) {
                            invokeCallConfigPost(mv, COMPILED_SUPER_CLASS, callConfig);
                        }
                        
                        // throw redo error if we're still good
                        mv.athrow();
                    }

                    // finally handling for abnormal exit
                    {
                        mv.label(doFinally);

                        //call post method stuff (exception raised)
                        if (!callConfig.isNoop()) {
                            invokeCallConfigPost(mv, COMPILED_SUPER_CLASS, callConfig);
                        }

                        // rethrow exception
                        mv.athrow(); // rethrow it
                    }

                    generatedClass = endCall(cw,mv,mname);
                }

                CompiledMethod compiledMethod = (CompiledMethod)generatedClass.newInstance();
                compiledMethod.init(implementationClass, arity, visibility, scope, scriptObject, callConfig);
                return compiledMethod;
            } catch(Exception e) {
                e.printStackTrace();
                throw implementationClass.getRuntime().newLoadError(e.getMessage());
            }
        }
    }
    
    private class DescriptorInfo {
        private int min;
        private int max;
        private boolean frame;
        private boolean scope;
        private boolean backtrace;
        private boolean rest;
        private boolean block;
        
        public DescriptorInfo(List<JavaMethodDescriptor> descs) {
            min = Integer.MAX_VALUE;
            max = 0;
            frame = false;
            scope = false;
            backtrace = false;
            rest = false;
            block = false;

            for (JavaMethodDescriptor desc: descs) {
                int specificArity = -1;
                if (desc.hasVarArgs) {
                    if (desc.optional == 0 && !desc.rest) {
                        throw new RuntimeException("IRubyObject[] args but neither of optional or rest specified for method " + desc.declaringClassName + "." + desc.name);
                    }
                    rest = true;
                } else {
                    if (desc.optional == 0 && !desc.rest) {
                        if (desc.required == 0) {
                            // No required specified, check actual number of required args
                            if (desc.actualRequired <= 3) {
                                // actual required is less than 3, so we use specific arity
                                specificArity = desc.actualRequired;
                            } else {
                                // actual required is greater than 3, raise error (we don't support actual required > 3)
                                throw new RuntimeException("Invalid specific-arity number of arguments (" + desc.actualRequired + ") on method " + desc.declaringClassName + "." + desc.name);
                            }
                        } else if (desc.required >= 0 && desc.required <= 3) {
                            if (desc.actualRequired != desc.required) {
                                throw new RuntimeException("Specified required args does not match actual on method " + desc.declaringClassName + "." + desc.name);
                            }
                            specificArity = desc.required;
                        }
                    }

                    if (specificArity < min) {
                        min = specificArity;
                    }

                    if (specificArity > max) {
                        max = specificArity;
                    }
                }

                frame |= desc.anno.frame();
                scope |= desc.anno.scope();
                backtrace |= desc.anno.backtrace();
                block |= desc.hasBlock;
            }
        }
        
        public boolean isBacktrace() {
            return backtrace;
        }

        public boolean isFrame() {
            return frame;
        }

        public int getMax() {
            return max;
        }

        public int getMin() {
            return min;
        }

        public boolean isScope() {
            return scope;
        }
        
        public boolean isRest() {
            return rest;
        }
        
        public boolean isBlock() {
            return block;
        }
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, List<JavaMethodDescriptor> descs) {
        JavaMethodDescriptor desc1 = descs.get(0);
        String javaMethodName = desc1.name;
        
        if (DEBUG) out.println("Binding multiple: " + desc1.declaringClassName + "." + javaMethodName);
        
        synchronized (classLoader) {
            try {
                Class c = getAnnotatedMethodClass(descs);
                
                DescriptorInfo info = new DescriptorInfo(descs);
                if (DEBUG) out.println(" min: " + info.getMin() + ", max: " + info.getMax());

                JavaMethod ic = (JavaMethod)c.getConstructor(new Class[]{RubyModule.class, Visibility.class}).newInstance(new Object[]{implementationClass, desc1.anno.visibility()});

                ic.setArity(Arity.OPTIONAL);
                ic.setJavaName(javaMethodName);
                ic.setSingleton(desc1.isStatic);
                ic.setCallConfig(CallConfiguration.getCallConfig(info.isFrame(), info.isScope(), info.isBacktrace()));
                return ic;
            } catch(Exception e) {
                e.printStackTrace();
                throw implementationClass.getRuntime().newLoadError(e.getMessage());
            }
        }
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method. Return the resulting generated or loaded class.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getAnnotatedMethod
     */
    public Class getAnnotatedMethodClass(List<JavaMethodDescriptor> descs) throws Exception {
        if (descs.size() == 1) {
            // simple path, no multimethod
            return getAnnotatedMethodClass(descs.get(0));
        }
        
        JavaMethodDescriptor desc1 = descs.get(0);
        String javaMethodName = desc1.name;
        
        if (DEBUG) out.println("Binding multiple: " + desc1.declaringClassName + "." + javaMethodName);
        
        String generatedClassName = CodegenUtils.getAnnotatedBindingClassName(javaMethodName, desc1.declaringClassName, desc1.isStatic, desc1.actualRequired, desc1.optional, true, desc1.anno.frame());
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // in debug mode we append _DBG to class name to force it to regenerate (or use pre-generated debug version)
            generatedClassName += "_DBG";
        }
        String generatedClassPath = generatedClassName.replace('.', '/');
        
        synchronized (classLoader) {
            Class c = tryClass(generatedClassName, desc1.getDeclaringClass());

            DescriptorInfo info = new DescriptorInfo(descs);
            if (DEBUG) out.println(" min: " + info.getMin() + ", max: " + info.getMax() + ", hasBlock: " + info.isBlock() + ", rest: " + info.isRest());

            if (c == null) {
                String superClass = null;
                switch (info.getMin()) {
                case 0:
                    switch (info.getMax()) {
                    case 1:
                        if (info.isRest()) {
                            if (info.isBlock()) {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrNBlock.class);
                            } else {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrN.class);
                            }
                        } else {
                            if (info.isBlock()) {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneBlock.class);
                            } else {
                                superClass = p(JavaMethod.JavaMethodZeroOrOne.class);
                            }
                        }
                        break;
                    case 2:
                        if (info.isRest()) {
                            if (info.isBlock()) {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwoOrNBlock.class);
                            } else {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwoOrN.class);
                            }
                        } else {
                            if (info.isBlock()) {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwoBlock.class);
                            } else {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwo.class);
                            }
                        }
                        break;
                    case 3:
                        if (info.isRest()) {
                            if (info.isBlock()) {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwoOrThreeOrNBlock.class);
                            } else {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwoOrThreeOrN.class);
                            }
                        } else {
                            if (info.isBlock()) {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwoOrThreeBlock.class);
                            } else {
                                superClass = p(JavaMethod.JavaMethodZeroOrOneOrTwoOrThree.class);
                            }
                        }
                        break;
                    }
                    break;
                case 1:
                    switch (info.getMax()) {
                    case 2:
                        if (info.isBlock()) {
                            superClass = p(JavaMethod.JavaMethodOneOrTwoBlock.class);
                        } else {
                            superClass = p(JavaMethod.JavaMethodOneOrTwo.class);
                        }
                        break;
                    case 3:
                        if (info.isBlock()) {
                            superClass = p(JavaMethod.JavaMethodOneOrTwoOrThreeBlock.class);
                        } else {
                            superClass = p(JavaMethod.JavaMethodOneOrTwoOrThree.class);
                        }
                        break;
                    }
                    break;
                case 2:
                    switch (info.getMax()) {
                    case 3:
                        superClass = p(JavaMethod.JavaMethodTwoOrThree.class);
                        break;
                    }
                    break;
                case -1:
                    // rest arg, use normal JavaMethod since N case will be defined
                    superClass = p(JavaMethod.JavaMethodNoBlock.class);
                    break;
                }
                if (superClass == null) throw new RuntimeException("invalid multi combination");
                ClassWriter cw = createJavaMethodCtor(generatedClassPath, superClass);

                for (JavaMethodDescriptor desc: descs) {
                    int specificArity = -1;
                    if (desc.optional == 0 && !desc.rest) {
                        if (desc.required == 0) {
                            if (desc.actualRequired <= 3) {
                                specificArity = desc.actualRequired;
                            } else {
                                specificArity = -1;
                            }
                        } else if (desc.required >= 0 && desc.required <= 3) {
                            specificArity = desc.required;
                        }
                    }

                    boolean hasBlock = desc.hasBlock;
                    SkinnyMethodAdapter mv = null;

                    mv = beginMethod(cw, "call", specificArity, hasBlock);
                    mv.visitCode();
                    Label line = new Label();
                    mv.visitLineNumber(0, line);

                    createAnnotatedMethodInvocation(desc, mv, superClass, specificArity, hasBlock);

                    endMethod(mv);
                }

                c = endClass(cw, generatedClassName);
            }

            return c;
        }
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, JavaMethodDescriptor desc) {
        String javaMethodName = desc.name;
        
        String generatedClassName = CodegenUtils.getAnnotatedBindingClassName(javaMethodName, desc.declaringClassName, desc.isStatic, desc.actualRequired, desc.optional, false, desc.anno.frame());
        String generatedClassPath = generatedClassName.replace('.', '/');
        
        synchronized (classLoader) {
            try {
                Class c = getAnnotatedMethodClass(desc);

                JavaMethod ic = (JavaMethod)c.getConstructor(new Class[]{RubyModule.class, Visibility.class}).newInstance(new Object[]{implementationClass, desc.anno.visibility()});

                ic.setArity(Arity.fromAnnotation(desc.anno, desc.actualRequired));
                ic.setJavaName(javaMethodName);
                ic.setSingleton(desc.isStatic);
                ic.setCallConfig(CallConfiguration.getCallConfigByAnno(desc.anno));
                return ic;
            } catch(Exception e) {
                e.printStackTrace();
                throw implementationClass.getRuntime().newLoadError(e.getMessage());
            }
        }
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getAnnotatedMethod
     */
    public Class getAnnotatedMethodClass(JavaMethodDescriptor desc) throws Exception {
        String javaMethodName = desc.name;
        
        String generatedClassName = CodegenUtils.getAnnotatedBindingClassName(javaMethodName, desc.declaringClassName, desc.isStatic, desc.actualRequired, desc.optional, false, desc.anno.frame());
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // in debug mode we append _DBG to class name to force it to regenerate (or use pre-generated debug version)
            generatedClassName += "_DBG";
        }
        String generatedClassPath = generatedClassName.replace('.', '/');
        
        synchronized (classLoader) {
            Class c = tryClass(generatedClassName, desc.getDeclaringClass());

            if (c == null) {
                int specificArity = -1;
                if (desc.optional == 0 && !desc.rest) {
                    if (desc.required == 0) {
                        if (desc.actualRequired <= 3) {
                            specificArity = desc.actualRequired;
                        } else {
                            specificArity = -1;
                        }
                    } else if (desc.required >= 0 && desc.required <= 3) {
                        specificArity = desc.required;
                    }
                }

                boolean block = desc.hasBlock;

                String superClass = p(selectSuperClass(specificArity, block));

                ClassWriter cw = createJavaMethodCtor(generatedClassPath, superClass);
                SkinnyMethodAdapter mv = null;

                mv = beginMethod(cw, "call", specificArity, block);
                mv.visitCode();
                Label line = new Label();
                mv.visitLineNumber(0, line);

                createAnnotatedMethodInvocation(desc, mv, superClass, specificArity, block);

                endMethod(mv);

                c = endClass(cw, generatedClassName);
            }
            
            return c;
        }
    }

    /**
     * Use code generation to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getAnnotatedMethod
     */
    public void prepareAnnotatedMethod(RubyModule implementationClass, JavaMethod javaMethod, JavaMethodDescriptor desc) {
        String javaMethodName = desc.name;
        
        javaMethod.setArity(Arity.fromAnnotation(desc.anno, desc.actualRequired));
        javaMethod.setJavaName(javaMethodName);
        javaMethod.setSingleton(desc.isStatic);
        javaMethod.setCallConfig(CallConfiguration.getCallConfigByAnno(desc.anno));
    }

    /**
     * Use code generation to generate a set of method handles based on all
     * annotated methods in the target class.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#defineIndexedAnnotatedMethods
     */
    @Deprecated
    public void defineIndexedAnnotatedMethods(RubyModule implementationClass, Class type, MethodDefiningCallback callback) {
        String typePath = p(type);
        String superClass = p(JavaMethod.class);
        
        String generatedClassName = type.getName() + "Invoker";
        String generatedClassPath = typePath + "Invoker";
        
        synchronized (classLoader) {
            Class c = tryClass(generatedClassName, type);

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
                    ClassWriter cw = createIndexedJavaMethodCtor(generatedClassPath, superClass);
                    SkinnyMethodAdapter mv = null;

                    mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", COMPILED_CALL_SIG_BLOCK, null, null));
                    mv.visitCode();
                    Label line = new Label();
                    mv.visitLineNumber(0, line);

                    Label defaultCase = new Label();
                    Label[] cases = new Label[sortedMethods.size()];
                    for (int i = 0; i < cases.length; i++) cases[i] = new Label();

                    // load method index
                    mv.aload(THIS_INDEX);
                    mv.getfield(generatedClassPath, "methodIndex", ci(int.class));

                    mv.tableswitch(0, cases.length - 1, defaultCase, cases);

                    for (int i = 0; i < sortedMethods.size(); i++) {
                        mv.label(cases[i]);
                        String callName = getAnnotatedMethodForIndex(cw, sortedMethods.get(i), i, superClass);

                        // invoke call#_method for method
                        mv.aload(THIS_INDEX);
                        mv.aload(THREADCONTEXT_INDEX);
                        mv.aload(RECEIVER_INDEX);
                        mv.aload(CLASS_INDEX);
                        mv.aload(NAME_INDEX);
                        mv.aload(ARGS_INDEX);
                        mv.aload(BLOCK_INDEX);

                        mv.invokevirtual(generatedClassPath, callName, COMPILED_CALL_SIG_BLOCK);
                        mv.areturn();
                    }

                    // if we fall off the switch, error.
                    mv.label(defaultCase);
                    mv.aload(THREADCONTEXT_INDEX);
                    mv.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
                    mv.ldc("Error: fell off switched invoker for class: " + implementationClass.getBaseName());
                    mv.invokevirtual(p(Ruby.class), "newRuntimeError", sig(RaiseException.class, String.class));
                    mv.athrow();

                    c = endCall(cw, mv, generatedClassName);
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

                    ic.setArity(Arity.fromAnnotation(jrubyMethod));
                    ic.setJavaName(method.getName());
                    ic.setSingleton(Modifier.isStatic(method.getModifiers()));
                    ic.setCallConfig(CallConfiguration.getCallConfigByAnno(jrubyMethod));

                    callback.define(implementationClass, new JavaMethodDescriptor(method), ic);
                }
            } catch(Exception e) {
                e.printStackTrace();
                throw implementationClass.getRuntime().newLoadError(e.getMessage());
            }
        }
    }

    /**
     * Emit code to check the arity of a call to a Ruby-based method.
     * 
     * @param jrubyMethod The annotation of the called method
     * @param method The code generator for the handle being created
     */
    private void checkArity(SkinnyMethodAdapter method, StaticScope scope) {
        Label arityError = new Label();
        Label noArityError = new Label();

        if (scope.getRestArg() >= 0) {
            if (scope.getRequiredArgs() > 0) {
                // just confirm minimum args provided
                method.aload(ARGS_INDEX);
                method.arraylength();
                method.ldc(scope.getRequiredArgs());
                method.if_icmplt(arityError);
            }
        } else if (scope.getOptionalArgs() > 0) {
            if (scope.getRequiredArgs() > 0) {
                // confirm minimum args provided
                method.aload(ARGS_INDEX);
                method.arraylength();
                method.ldc(scope.getRequiredArgs());
                method.if_icmplt(arityError);
            }

            // confirm maximum not greater than optional
            method.aload(ARGS_INDEX);
            method.arraylength();
            method.ldc(scope.getRequiredArgs() + scope.getOptionalArgs());
            method.if_icmpgt(arityError);
        } else {
            // just confirm args length == required
            method.aload(ARGS_INDEX);
            method.arraylength();
            method.ldc(scope.getRequiredArgs());
            method.if_icmpne(arityError);
        }

        method.go_to(noArityError);

        // Raise an error if arity does not match requirements
        method.label(arityError);
        method.aload(THREADCONTEXT_INDEX);
        method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
        method.aload(ARGS_INDEX);
        method.ldc(scope.getRequiredArgs());
        method.ldc(scope.getRequiredArgs() + scope.getOptionalArgs());
        method.invokestatic(p(Arity.class), "checkArgumentCount", sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
        method.pop();

        method.label(noArityError);
    }

    /**
     * Emit code to check the arity of a call to a Java-based method.
     * 
     * @param jrubyMethod The annotation of the called method
     * @param method The code generator for the handle being created
     */
    private void checkArity(JRubyMethod jrubyMethod, SkinnyMethodAdapter method, int specificArity) {
        Label arityError = new Label();
        Label noArityError = new Label();
        
        switch (specificArity) {
        case 0:
        case 1:
        case 2:
        case 3:
            // for zero, one, two, three arities, JavaMethod.JavaMethod*.call(...IRubyObject[] args...) will check
            return;
        default:
            if (jrubyMethod.rest()) {
                if (jrubyMethod.required() > 0) {
                    // just confirm minimum args provided
                    method.aload(ARGS_INDEX);
                    method.arraylength();
                    method.ldc(jrubyMethod.required());
                    method.if_icmplt(arityError);
                }
            } else if (jrubyMethod.optional() > 0) {
                if (jrubyMethod.required() > 0) {
                    // confirm minimum args provided
                    method.aload(ARGS_INDEX);
                    method.arraylength();
                    method.ldc(jrubyMethod.required());
                    method.if_icmplt(arityError);
                }

                // confirm maximum not greater than optional
                method.aload(ARGS_INDEX);
                method.arraylength();
                method.ldc(jrubyMethod.required() + jrubyMethod.optional());
                method.if_icmpgt(arityError);
            } else {
                // just confirm args length == required
                method.aload(ARGS_INDEX);
                method.arraylength();
                method.ldc(jrubyMethod.required());
                method.if_icmpne(arityError);
            }

            method.go_to(noArityError);

            // Raise an error if arity does not match requirements
            method.label(arityError);
            method.aload(THREADCONTEXT_INDEX);
            method.invokevirtual(p(ThreadContext.class), "getRuntime", sig(Ruby.class));
            method.aload(ARGS_INDEX);
            method.ldc(jrubyMethod.required());
            method.ldc(jrubyMethod.required() + jrubyMethod.optional());
            method.invokestatic(p(Arity.class), "checkArgumentCount", sig(int.class, Ruby.class, IRubyObject[].class, int.class, int.class));
            method.pop();

            method.label(noArityError);
        }
    }

    private ClassWriter createCompiledCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        cw.visitSource(namePath.replace('.', '/') + ".gen", null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", "()V");
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        return cw;
    }

    private ClassWriter createJavaMethodCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        cw.visitSource(namePath.replace('.', '/') + ".gen", null);
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

    @Deprecated
    private ClassWriter createIndexedJavaMethodCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        cw.visitSource(namePath.replace('.', '/') + ".gen", null);
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

    private void invokeCallConfigPost(SkinnyMethodAdapter mv, String superClass, CallConfiguration callConfig) {
        if (callConfig != CallConfiguration.NO_FRAME_NO_SCOPE) {
            mv.aload(0);
            mv.aload(1);
            if (callConfig == CallConfiguration.FRAME_AND_SCOPE) {
                mv.invokevirtual(superClass, "postFrameAndScope", sig(void.class, params(ThreadContext.class)));
            } else if (callConfig == CallConfiguration.FRAME_AND_DUMMY_SCOPE) {
                mv.invokevirtual(superClass, "postFrameAndScope", sig(void.class, params(ThreadContext.class)));
            } else if (callConfig == CallConfiguration.FRAME_ONLY) {
                mv.invokevirtual(superClass, "postFrameOnly", sig(void.class, params(ThreadContext.class)));
            } else if (callConfig == CallConfiguration.SCOPE_ONLY) {
                mv.invokevirtual(superClass, "postScopeOnly", sig(void.class, params(ThreadContext.class)));
            } else if (callConfig == CallConfiguration.BACKTRACE_ONLY) {
                mv.invokevirtual(superClass, "postBacktraceOnly", sig(void.class, params(ThreadContext.class)));
            } else if (callConfig == CallConfiguration.BACKTRACE_AND_SCOPE) {
                mv.invokevirtual(superClass, "postBacktraceAndScope", sig(void.class, params(ThreadContext.class)));
            }
        }
    }

    private void invokeCallConfigPre(SkinnyMethodAdapter mv, String superClass, int specificArity, boolean block, CallConfiguration callConfig) {
        // invoke pre method stuff
        if (callConfig != CallConfiguration.NO_FRAME_NO_SCOPE) {
            mv.aload(0); 
            mv.aload(THREADCONTEXT_INDEX); // tc


            if (callConfig == CallConfiguration.FRAME_AND_SCOPE) {
                mv.aload(RECEIVER_INDEX); // self
                mv.aload(NAME_INDEX); // name
                loadBlockForPre(mv, specificArity, block);
                mv.invokevirtual(superClass, "preFrameAndScope", sig(void.class, params(ThreadContext.class, IRubyObject.class, String.class, Block.class)));
            } else if (callConfig == CallConfiguration.FRAME_AND_DUMMY_SCOPE) {
                mv.aload(RECEIVER_INDEX); // self
                mv.aload(NAME_INDEX); // name
                loadBlockForPre(mv, specificArity, block);
                mv.invokevirtual(superClass, "preFrameAndDummyScope", sig(void.class, params(ThreadContext.class, IRubyObject.class, String.class, Block.class)));
            } else if (callConfig == CallConfiguration.FRAME_ONLY) {
                mv.aload(RECEIVER_INDEX); // self
                mv.aload(NAME_INDEX); // name
                loadBlockForPre(mv, specificArity, block);
                mv.invokevirtual(superClass, "preFrameOnly", sig(void.class, params(ThreadContext.class, IRubyObject.class, String.class, Block.class)));
            } else if (callConfig == CallConfiguration.SCOPE_ONLY) {
                mv.invokevirtual(superClass, "preScopeOnly", sig(void.class, params(ThreadContext.class)));
            } else if (callConfig == CallConfiguration.BACKTRACE_ONLY) {
                mv.aload(NAME_INDEX); // name
                mv.invokevirtual(superClass, "preBacktraceOnly", sig(void.class, params(ThreadContext.class, String.class)));
            } else if (callConfig == CallConfiguration.BACKTRACE_AND_SCOPE) {
                mv.aload(NAME_INDEX); // name
                mv.invokevirtual(superClass, "preBacktraceAndScope", sig(void.class, params(ThreadContext.class, String.class)));
            }
        }
    }

    private void loadArguments(SkinnyMethodAdapter mv, JRubyMethod jrubyMethod, int specificArity) {
        switch (specificArity) {
        default:
        case -1:
            mv.aload(ARGS_INDEX);
            break;
        case 0:
            // no args
            break;
        case 1:
            mv.aload(ARGS_INDEX);
            break;
        case 2:
            mv.aload(ARGS_INDEX);
            mv.aload(ARGS_INDEX + 1);
            break;
        case 3:
            mv.aload(ARGS_INDEX);
            mv.aload(ARGS_INDEX + 1);
            mv.aload(ARGS_INDEX + 2);
            break;
        }
    }

    private void loadBlockForPre(SkinnyMethodAdapter mv, int specificArity, boolean getsBlock) {
        switch (specificArity) {
        default:
        case -1:
            if (getsBlock) {
                // variable args with block
                mv.visitVarInsn(ALOAD, BLOCK_INDEX);
            } else {
                // variable args no block, load null block
                mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            }
            break;
        case 0:
            if (getsBlock) {
                // zero args with block
                // FIXME: omit args index; subtract one from normal block index
                mv.visitVarInsn(ALOAD, BLOCK_INDEX - 1);
            } else {
                // zero args, no block; load NULL_BLOCK
                mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            }
            break;
        case 1:
            if (getsBlock) {
                // one arg with block
                mv.visitVarInsn(ALOAD, BLOCK_INDEX);
            } else {
                // one arg, no block; load NULL_BLOCK
                mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            }
            break;
        case 2:
            if (getsBlock) {
                // two args with block
                mv.visitVarInsn(ALOAD, BLOCK_INDEX + 1);
            } else {
                // two args, no block; load NULL_BLOCK
                mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            }
            break;
        case 3:
            if (getsBlock) {
                // three args with block
                mv.visitVarInsn(ALOAD, BLOCK_INDEX + 2);
            } else {
                // three args, no block; load NULL_BLOCK
                mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
            }
            break;
        }
    }

    private void loadBlock(SkinnyMethodAdapter mv, int specificArity, boolean getsBlock) {
        // load block if it accepts block
        switch (specificArity) {
        default:
        case -1:
            if (getsBlock) {
                // all other arg cases with block
                mv.visitVarInsn(ALOAD, BLOCK_INDEX);
            } else {
                // all other arg cases without block
            }
            break;
        case 0:
            if (getsBlock) {
                mv.visitVarInsn(ALOAD, BLOCK_INDEX - 1);
            } else {
                // zero args, no block; do nothing
            }
            break;
        case 1:
            if (getsBlock) {
                mv.visitVarInsn(ALOAD, BLOCK_INDEX);
            } else {
                // one arg, no block; do nothing
            }
            break;
        case 2:
            if (getsBlock) {
                mv.visitVarInsn(ALOAD, BLOCK_INDEX + 1);
            } else {
                // two args, no block; do nothing
            }
            break;
        case 3:
            if (getsBlock) {
                mv.visitVarInsn(ALOAD, BLOCK_INDEX + 2);
            } else {
                // three args, no block; do nothing
            }
            break;
        }
    }

    private void loadReceiver(String typePath, JavaMethodDescriptor desc, SkinnyMethodAdapter mv) {
        // load target for invocations
        if (Modifier.isStatic(desc.modifiers)) {
            if (desc.hasContext) {
                mv.aload(THREADCONTEXT_INDEX);
            }
            
            // load self object as IRubyObject, for recv param
            mv.aload(RECEIVER_INDEX);
        } else {
            // load receiver as original type for virtual invocation
            mv.aload(RECEIVER_INDEX);
            mv.checkcast(typePath);
            
            if (desc.hasContext) {
                mv.aload(THREADCONTEXT_INDEX);
            }
        }
    }
    private Class tryClass(String name, Class targetClass) {
        try {
            Class c = null;
            if (classLoader == null) {
                c = Class.forName(name, true, classLoader);
            } else {
                c = classLoader.loadClass(name);
            }
            
            if (c != null && seenUndefinedClasses && !haveWarnedUser) {
                haveWarnedUser = true;
                System.err.println("WARNING: while creating new bindings for " + targetClass + ",\n" +
                        "found an existing binding; you may want to run a clean build.");
            }
            
            return c;
        } catch(Exception e) {
            seenUndefinedClasses = true;
            return null;
        }
    }

    protected Class endCall(ClassWriter cw, MethodVisitor mv, String name) {
        endMethod(mv);
        return endClass(cw, name);
    }

    protected void endMethod(MethodVisitor mv) {
        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    protected Class endClass(ClassWriter cw, String name) {
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        if (DEBUG) CheckClassAdapter.verify(new ClassReader(code), false, new PrintWriter(System.err));
         
        return classLoader.defineClass(name, code);
    }
    
    private void loadArgument(MethodVisitor mv, int argsIndex, int argIndex) {
        mv.visitVarInsn(ALOAD, argsIndex);
        mv.visitLdcInsn(new Integer(argIndex));
        mv.visitInsn(AALOAD);
    }
    
    private SkinnyMethodAdapter beginMethod(ClassWriter cw, String methodName, int specificArity, boolean block) {
        switch (specificArity) {
        default:
        case -1:
            if (block) {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_BLOCK, null, null));
            } else {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG, null, null));
            }
        case 0:
            if (block) {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ZERO_BLOCK, null, null));
            } else {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ZERO, null, null));
            }
        case 1:
            if (block) {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ONE_BLOCK, null, null));
            } else {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_ONE, null, null));
            }
        case 2:
            if (block) {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_TWO_BLOCK, null, null));
            } else {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_TWO, null, null));
            }
        case 3:
            if (block) {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_THREE_BLOCK, null, null));
            } else {
                return new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_THREE, null, null));
            }
        }
    }
    
    private Class selectSuperClass(int specificArity, boolean block) {
        switch (specificArity) {
        default:
        case -1:
            if (block) {
                return JavaMethod.class;
            } else {
                return JavaMethod.JavaMethodNoBlock.class;
            }
        case 0:
            if (block) {
                return JavaMethod.JavaMethodZeroBlock.class;
            } else {
                return JavaMethod.JavaMethodZero.class;
            }
        case 1:
            if (block) {
                return JavaMethod.JavaMethodOneBlock.class;
            } else {
                return JavaMethod.JavaMethodOne.class;
            }
        case 2:
            if (block) {
                return JavaMethod.JavaMethodTwoBlock.class;
            } else {
                return JavaMethod.JavaMethodTwo.class;
            }
        case 3:
            if (block) {
                return JavaMethod.JavaMethodThreeBlock.class;
            } else {
                return JavaMethod.JavaMethodThree.class;
            }
        }
    }

    @Deprecated
    private String getAnnotatedMethodForIndex(ClassWriter cw, Method method, int index, String superClass) {
        String methodName = "call" + index + "_" + method.getName();
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, methodName, COMPILED_CALL_SIG_BLOCK, null, null));
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        // TODO: indexed methods do not use specific arity yet
        createAnnotatedMethodInvocation(new JavaMethodDescriptor(method), mv, superClass, -1, true);
        endMethod(mv);
        
        return methodName;
    }

    private void createAnnotatedMethodInvocation(JavaMethodDescriptor desc, SkinnyMethodAdapter method, String superClass, int specificArity, boolean block) {
        String typePath = desc.declaringClassPath;
        String javaMethodName = desc.name;

        checkArity(desc.anno, method, specificArity);
        
        CallConfiguration callConfig = CallConfiguration.getCallConfigByAnno(desc.anno);
        if (!callConfig.isNoop()) {
            invokeCallConfigPre(method, superClass, specificArity, block, callConfig);
        }
        
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            invokeCCallTrace(method);
        }

        Label tryBegin = new Label();
        Label tryEnd = new Label();
        Label doFinally = new Label();
        Label doRedoFinally = new Label();
        Label catchRedoJump = new Label();

        if (!callConfig.isNoop()) {
            method.trycatch(tryBegin, tryEnd, doFinally, null);
        }
        
        method.label(tryBegin);
        {
            loadReceiver(typePath, desc, method);
            
            loadArguments(method, desc.anno, specificArity);
            
            loadBlock(method, specificArity, block);

            if (Modifier.isStatic(desc.modifiers)) {
                // static invocation
                method.invokestatic(typePath, javaMethodName, desc.signature);
            } else {
                // virtual invocation
                method.invokevirtual(typePath, javaMethodName, desc.signature);
            }
        }
        method.label(tryEnd);
        
        // normal finally and exit
        {
            if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
                invokeCReturnTrace(method);
            }
            
            if (!callConfig.isNoop()) {
                invokeCallConfigPost(method, superClass, callConfig);
            }

            // return
            method.visitInsn(ARETURN);
        }
        
        // these are only needed if we have a non-noop call config
        if (!callConfig.isNoop()) {
            // finally handling for abnormal exit
            {
                method.label(doFinally);
                
                if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
                    invokeCReturnTrace(method);
                }

                //call post method stuff (exception raised)
                if (!callConfig.isNoop()) {
                    invokeCallConfigPost(method, superClass, callConfig);
                }

                // rethrow exception
                method.athrow(); // rethrow it
            }
        }
    }
    
    private void invokeCCallTrace(SkinnyMethodAdapter method) {
        method.aload(0); // method itself
        method.aload(1); // ThreadContext
        method.aload(4); // invoked name
        method.invokevirtual(p(JavaMethod.class), "callTrace", sig(void.class, ThreadContext.class, String.class));
    }
    
    private void invokeCReturnTrace(SkinnyMethodAdapter method) {
        method.aload(0); // method itself
        method.aload(1); // ThreadContext
        method.aload(4); // invoked name
        method.invokevirtual(p(JavaMethod.class), "returnTrace", sig(void.class, ThreadContext.class, String.class));
    }
}
