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
package org.jruby.runtime.callback;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.CompiledBlockCallback19;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.* ;
import org.jruby.util.JRubyClassLoader;

public class InvocationCallbackFactory extends CallbackFactory implements Opcodes {
    private final Class type;
    final ProtectionDomain protectionDomain;
    protected final JRubyClassLoader classLoader;
    private final String typePath;
    protected final Ruby runtime;

    @Deprecated
    private final static String SUPER_CLASS = p(InvocationCallback.class);
    @Deprecated
    private final static String FAST_SUPER_CLASS = p(FastInvocationCallback.class);
    @Deprecated
    private final static String CALL_SIG = sig(RubyKernel.IRUBY_OBJECT, params(Object.class,
            Object[].class, Block.class));
    @Deprecated
    private final static String FAST_CALL_SIG = sig(RubyKernel.IRUBY_OBJECT, params(
            Object.class, Object[].class));
    private final static String BLOCK_CALL_SIG = sig(RubyKernel.IRUBY_OBJECT, params(
            ThreadContext.class, RubyKernel.IRUBY_OBJECT, IRubyObject.class));
    private final static String BLOCK_CALL_SIG19 = sig(RubyKernel.IRUBY_OBJECT, params(
            ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class));
    private final static String IRUB = p(RubyKernel.IRUBY_OBJECT);
    
    
    public static final int DISPATCHER_THREADCONTEXT_INDEX = 1;
    public static final int DISPATCHER_SELF_INDEX = 2;
    public static final int DISPATCHER_RUBYMODULE_INDEX = 3;
    public static final int DISPATCHER_METHOD_INDEX = 4;
    public static final int DISPATCHER_NAME_INDEX = 5;
    public static final int DISPATCHER_ARGS_INDEX = 6;
    public static final int DISPATCHER_CALLTYPE_INDEX = 7;
    public static final int DISPATCHER_BLOCK_INDEX = 8;
    public static final int DISPATCHER_RUNTIME_INDEX = 9;

    private static final int METHOD_ARGS_INDEX = 2;

    public InvocationCallbackFactory(Ruby runtime, final Class type, ClassLoader classLoader) {
        this.type = type;
        if (classLoader instanceof JRubyClassLoader) {
            this.classLoader = (JRubyClassLoader)classLoader;
        } else {
           this.classLoader = new JRubyClassLoader(classLoader);
        }
        this.typePath = p(type);
        this.runtime = runtime;
        
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            this.protectionDomain = type.getProtectionDomain();
        } else {
            this.protectionDomain = AccessController.doPrivileged(
                    new PrivilegedAction<ProtectionDomain>() {
                        public ProtectionDomain run() {
                            return type.getProtectionDomain();
                        }
                    });
        }
    }

    @Deprecated
    private Class getReturnClass(String method, Class[] args) throws Exception {
        return type.getMethod(method, args).getReturnType();
    }

    @Deprecated
    private ClassWriter createCtor(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, SUPER_CLASS, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER_CLASS, "<init>", "()V");
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return cw;
    }

    @Deprecated
    private ClassWriter createCtorFast(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, FAST_SUPER_CLASS, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, FAST_SUPER_CLASS, "<init>", "()V");
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return cw;
    }

    @Deprecated
    private ClassWriter createBlockCtor(String namePath, Class fieldClass) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, namePath, null, p(CompiledBlockCallback.class), null);
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "$scriptObject", ci(fieldClass), null, null);
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", sig(Void.TYPE, params(Object.class)), null, null));
        mv.start();
        mv.aload(0);
        mv.invokespecial(p(CompiledBlockCallback.class), "<init>", sig(void.class));
        mv.aload(0);
        mv.aload(1);
        mv.checkcast(p(fieldClass));
        mv.putfield(namePath, "$scriptObject", ci(fieldClass));
        mv.voidreturn();
        mv.end();

        return cw;
    }

    @Deprecated
    private ClassWriter createBlockCtor19(String namePath, Class fieldClass) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, namePath, null, p(Object.class), new String[] {p(CompiledBlockCallback19.class)});
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "$scriptObject", ci(fieldClass), null, null);
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", sig(Void.TYPE, params(Object.class)), null, null));
        mv.start();
        mv.aload(0);
        mv.invokespecial(p(Object.class), "<init>", sig(void.class));
        mv.aload(0);
        mv.aload(1);
        mv.checkcast(p(fieldClass));
        mv.putfield(namePath, "$scriptObject", ci(fieldClass));
        mv.voidreturn();
        mv.end();
        
        return cw;
    }

    @Deprecated
    private Class tryClass(String name) {
        try {
            return classLoader.loadClass(name);
        } catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    private MethodVisitor startCall(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", CALL_SIG, null, null);
        
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typePath);
        return mv;
    }

    @Deprecated
    private MethodVisitor startCallS(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", CALL_SIG, null, null);
        
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitVarInsn(ALOAD, 1);
        checkCast(mv, IRubyObject.class);
        return mv;
    }

    @Deprecated
    private MethodVisitor startCallFast(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", FAST_CALL_SIG, null, null);
        
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typePath);
        return mv;
    }

    @Deprecated
    private MethodVisitor startDispatcher(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "callMethod", sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, RubyClass.class, Integer.TYPE, String.class,
                IRubyObject[].class, CallType.class, Block.class)), null, null);
        
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, typePath);
        return mv;
    }

    @Deprecated
    private MethodVisitor startCallSFast(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", FAST_CALL_SIG, null, null);
        
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, IRUB);
        return mv;
    }

    @Deprecated
    private SkinnyMethodAdapter startBlockCall(ClassWriter cw) {
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL, "call", BLOCK_CALL_SIG, null, null));
        
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        return mv;
    }

    @Deprecated
    private SkinnyMethodAdapter startBlockCall19(ClassWriter cw) {
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL, "call", BLOCK_CALL_SIG19, null, null));

        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        return mv;
    }

    @Deprecated
    protected Class endCall(ClassWriter cw, MethodVisitor mv, String name) {
        mv.visitEnd();
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        return classLoader.defineClass(name, code, protectionDomain);
    }

    @Deprecated
    public Callback getMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "_0";
        String mnamePath = typePath + "Callback$" + method + "_0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getMethod(String method, Class arg1) {
        String mname = type.getName() + "Callback$" + method + "_1";
        String mnamePath = typePath + "Callback$" + method + "_1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] {arg1};
                if (c == null) {
                    Class[] signature = new Class[] { arg1, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 1, descriptor);

                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Callback$" + method + "_2";
        String mnamePath = typePath + "Callback$" + method + "_2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] { arg1, arg2 };
                if (c == null) {
                    Class[] signature = new Class[] { arg1, arg2, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 2, descriptor);

                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }
    
    @Deprecated
    public Callback getMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Callback$" + method + "_3";
        String mnamePath = typePath + "Callback$" + method + "_3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] { arg1, arg2, arg3 }; 
                if (c == null) {
                    Class[] signature = new Class[] { arg1, arg2, arg3, Block.class }; 
                    Class ret = getReturnClass(method,
                            descriptor);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 3, descriptor);
                    
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getSingletonMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "S0";
        String mnamePath = typePath + "Callback$" + method + "S0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getSingletonMethod(String method, Class arg1) {
        String mname = type.getName() + "Callback$" + method + "_S1";
        String mnamePath = typePath + "Callback$" + method + "_S1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] {arg1};
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, arg1, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 1, descriptor);

                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getSingletonMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Callback$" + method + "_S2";
        String mnamePath = typePath + "Callback$" + method + "_S2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] {arg1, arg2};
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, arg1, arg2, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 2, descriptor);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 4);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Callback$" + method + "_S3";
        String mnamePath = typePath + "Callback$" + method + "_S3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] {arg1, arg2, arg3};
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, arg1, arg2, arg3, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 3, descriptor);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getBlockMethod(String method) {
        // TODO: This is probably BAD...
        return new ReflectionCallback(type, method, new Class[] { RubyKernel.IRUBY_OBJECT,
                RubyKernel.IRUBY_OBJECT }, false, true, Arity.fixed(2), false);
    }

    @Deprecated
    public CompiledBlockCallback getBlockCallback(String method, Object scriptObject) {
        Class typeClass = scriptObject.getClass();
        String typePathString = p(typeClass);
        String mname = typeClass.getName() + "BlockCallback$" + method + "xx1";
        String mnamePath = typePathString + "BlockCallback$" + method + "xx1";
        synchronized (classLoader) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    ClassWriter cw = createBlockCtor(mnamePath, typeClass);
                    SkinnyMethodAdapter mv = startBlockCall(cw);
                    mv.aload(0);
                    mv.getfield(mnamePath, "$scriptObject", ci(typeClass));
                    mv.aload(1);
                    mv.aload(2);
                    mv.aload(3);
                    mv.invokestatic(typePathString, method, sig(
                            RubyKernel.IRUBY_OBJECT, "L" + typePathString + ";", ThreadContext.class,
                                    RubyKernel.IRUBY_OBJECT, IRubyObject.class));
                    mv.areturn();
                    
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                CompiledBlockCallback ic = (CompiledBlockCallback) c.getConstructor(Object.class).newInstance(scriptObject);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public CompiledBlockCallback19 getBlockCallback19(String method, Object scriptObject) {
        Class typeClass = scriptObject.getClass();
        String typePathString = p(typeClass);
        String mname = typeClass.getName() + "BlockCallback$" + method + "xx1";
        String mnamePath = typePathString + "BlockCallback$" + method + "xx1";
        synchronized (classLoader) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    ClassWriter cw = createBlockCtor19(mnamePath, typeClass);
                    SkinnyMethodAdapter mv = startBlockCall19(cw);
                    mv.aload(0);
                    mv.getfield(mnamePath, "$scriptObject", ci(typeClass));
                    mv.aload(1);
                    mv.aload(2);
                    mv.aload(3);
                    mv.aload(4);
                    mv.invokestatic(typePathString, method, sig(
                            IRubyObject.class, "L" + typePathString + ";", ThreadContext.class,
                                    IRubyObject.class, IRubyObject[].class, Block.class));
                    mv.areturn();

                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                CompiledBlockCallback19 ic = (CompiledBlockCallback19) c.getConstructor(Object.class).newInstance(scriptObject);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getOptSingletonMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "_Sopt";
        String mnamePath = typePath + "Callback$" + method + "_Sopt";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, IRubyObject[].class, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    
                    mv.visitVarInsn(ALOAD, METHOD_ARGS_INDEX);
                    checkCast(mv, IRubyObject[].class);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                ic.setArgumentTypes(InvocationCallback.OPTIONAL_ARGS);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getOptMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "_opt";
        String mnamePath = typePath + "Callback$" + method + "_opt";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { IRubyObject[].class, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    
                    mv.visitVarInsn(ALOAD, METHOD_ARGS_INDEX);
                    checkCast(mv, IRubyObject[].class);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                ic.setArgumentTypes(InvocationCallback.OPTIONAL_ARGS);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "_F0";
        String mnamePath = typePath + "Callback$" + method + "_F0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class ret = getReturnClass(method, new Class[0]);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastMethod(String method, Class arg1) {
        String mname = type.getName() + "Callback$" + method + "_F1";
        String mnamePath = typePath + "Callback$" + method + "_F1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] { arg1 };
                if (c == null) {
                    Class[] signature = descriptor;
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 1, descriptor);

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Callback$" + method + "_F2";
        String mnamePath = typePath + "Callback$" + method + "_F2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] { arg1, arg2 };
                if (c == null) {
                    Class[] signature = descriptor;
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 2, descriptor);

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Callback$" + method + "_F3";
        String mnamePath = typePath + "Callback$" + method + "_F3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] { arg1, arg2, arg3 };
                if (c == null) {
                    Class[] signature = descriptor;
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 3, descriptor);

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "_FS0";
        String mnamePath = typePath + "Callback$" + method + "_FS0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method, Class arg1) {
        String mname = type.getName() + "Callback$" + method + "_FS1";
        String mnamePath = typePath + "Callback$" + method + "_FS1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] {arg1};
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, arg1 };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 1, descriptor);

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Callback$" + method + "_FS2";
        String mnamePath = typePath + "Callback$" + method + "_FS2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] {arg1, arg2};
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, arg1, arg2 };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 2, descriptor);

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 4);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Callback$" + method + "_FS3";
        String mnamePath = typePath + "Callback$" + method + "_FS3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                Class[] descriptor = new Class[] {arg1, arg2, arg3};
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, arg1, arg2, arg3 };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    
                    loadArguments(mv, METHOD_ARGS_INDEX, 3, descriptor);

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
                ic.setArgumentTypes(descriptor);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastOptMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "_Fopt";
        String mnamePath = typePath + "Callback$" + method + "_Fopt";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { IRubyObject[].class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    
                    mv.visitVarInsn(ALOAD, METHOD_ARGS_INDEX);
                    checkCast(mv, IRubyObject[].class);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                ic.setArgumentTypes(InvocationCallback.OPTIONAL_ARGS);
                ic.setJavaName(method);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    @Deprecated
    public Callback getFastOptSingletonMethod(String method) {
        String mname = type.getName() + "Callback$" + method + "_FSopt";
        String mnamePath = typePath + "Callback$" + method + "_FSopt";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, IRubyObject[].class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    
                    mv.visitVarInsn(ALOAD, METHOD_ARGS_INDEX);
                    checkCast(mv, IRubyObject[].class);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, sig(ret, signature));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                ic.setArgumentTypes(InvocationCallback.OPTIONAL_ARGS);
                ic.setJavaName(method);
                ic.setSingleton(true);
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }
    
    @Deprecated
    private void dispatchWithoutSTI(SkinnyMethodAdapter mv, Label afterCall) {
        // retrieve method
        mv.aload(DISPATCHER_RUBYMODULE_INDEX); // module
        mv.aload(DISPATCHER_NAME_INDEX); // name
        mv.invokevirtual(p(RubyModule.class), "searchMethod", sig(DynamicMethod.class, params(String.class)));

        Label okCall = new Label();
        
        callMethodMissingIfNecessary(mv, afterCall, okCall);

        // call is ok, punch it!
        mv.label(okCall);

        // method object already present, push various args
        mv.aload(DISPATCHER_THREADCONTEXT_INDEX); // tc
        mv.aload(DISPATCHER_SELF_INDEX); // self
        mv.aload(DISPATCHER_RUBYMODULE_INDEX); // klazz
        mv.aload(DISPATCHER_NAME_INDEX); // name
        mv.aload(DISPATCHER_ARGS_INDEX);
        mv.aload(DISPATCHER_BLOCK_INDEX);
        mv.invokevirtual(p(DynamicMethod.class), "call",
                sig(IRubyObject.class, 
                params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class)));
    }
    
    @Deprecated
    public void callMethodMissingIfNecessary(SkinnyMethodAdapter mv, Label afterCall, Label okCall) {
        Label methodMissing = new Label();

        // if undefined, branch to method_missing
        mv.dup();
        mv.invokevirtual(p(DynamicMethod.class), "isUndefined", sig(boolean.class));
        mv.ifne(methodMissing);

        // if we're not attempting to invoke method_missing and method is not visible, branch to method_missing
        mv.aload(DISPATCHER_NAME_INDEX);
        mv.ldc("method_missing");
        // if it's method_missing, just invoke it
        mv.invokevirtual(p(String.class), "equals", sig(boolean.class, params(Object.class)));
        mv.ifne(okCall);
        // check visibility
        mv.dup(); // dup method
        mv.aload(DISPATCHER_THREADCONTEXT_INDEX);
        mv.invokevirtual(p(ThreadContext.class), "getFrameSelf", sig(IRubyObject.class));
        mv.aload(DISPATCHER_CALLTYPE_INDEX);
        mv.invokevirtual(p(DynamicMethod.class), "isCallableFrom", sig(boolean.class, params(IRubyObject.class, CallType.class)));
        mv.ifne(okCall);

        // invoke callMethodMissing
        mv.label(methodMissing);

        mv.aload(DISPATCHER_THREADCONTEXT_INDEX); // tc
        mv.swap(); // under method
        mv.aload(DISPATCHER_SELF_INDEX); // self
        mv.swap(); // under method
        mv.aload(DISPATCHER_NAME_INDEX); // name
        mv.aload(DISPATCHER_ARGS_INDEX); // args

        // caller
        mv.aload(DISPATCHER_THREADCONTEXT_INDEX);
        mv.invokevirtual(p(ThreadContext.class), "getFrameSelf", sig(IRubyObject.class));

        mv.aload(DISPATCHER_CALLTYPE_INDEX); // calltype
        mv.aload(DISPATCHER_BLOCK_INDEX); // block

        // invoke callMethodMissing method directly
        // TODO: this could be further optimized, since some DSLs hit method_missing pretty hard...
        mv.invokestatic(p(RuntimeHelpers.class), "callMethodMissing", sig(IRubyObject.class, 
                params(ThreadContext.class, IRubyObject.class, DynamicMethod.class, String.class, 
                                    IRubyObject[].class, IRubyObject.class, CallType.class, Block.class)));
        // if no exception raised, jump to end to leave result on stack for return
        mv.go_to(afterCall);
    }
    
    @Deprecated
    private void loadArguments(MethodVisitor mv, int argsIndex, int count, Class[] types) {
        loadArguments(mv, argsIndex, count, types, false);
    }
    
    @Deprecated
    private void loadArguments(MethodVisitor mv, int argsIndex, int count, Class[] types, boolean contextProvided) {
        for (int i = 0; i < count; i++) {
            loadArgument(mv, argsIndex, i, types[i + (contextProvided ? 1 : 0)]);
        }
    }
    
    @Deprecated
    private void loadArgument(MethodVisitor mv, int argsIndex, int argIndex, Class type1) {
        mv.visitVarInsn(ALOAD, argsIndex);
        mv.visitLdcInsn(new Integer(argIndex));
        mv.visitInsn(AALOAD);
        checkCast(mv, type1);
    }

    @Deprecated
    private void checkCast(MethodVisitor mv, Class clazz) {
        mv.visitTypeInsn(CHECKCAST, p(clazz));
    }

    @Deprecated
    private void checkArity(SkinnyMethodAdapter mv, Arity arity) {
        if (arity.getValue() >= 0) {
            Label arityOk = new Label();
            // check arity
            mv.aload(6);
            mv.arraylength();
            
            // load arity for check
            switch (arity.getValue()) {
            case 3: mv.iconst_3(); break;
            case 2: mv.iconst_2(); break;
            case 1: mv.iconst_1(); break;
            case 0: mv.iconst_0(); break;
            default: mv.ldc(new Integer(arity.getValue()));
            }
   
            mv.if_icmpeq(arityOk);
            
            // throw
            mv.aload(9);
            mv.aload(6);
            mv.arraylength();

            // load arity for error
            switch (arity.getValue()) {
            case 3: mv.iconst_3(); break;
            case 2: mv.iconst_2(); break;
            case 1: mv.iconst_1(); break;
            case 0: mv.iconst_0(); break;
            default: mv.ldc(new Integer(arity.getValue()));
            }

            mv.invokevirtual(p(Ruby.class), "newArgumentError", sig(RaiseException.class, params(int.class, int.class)));
            mv.athrow();

            // arity ok, continue
            mv.label(arityOk);
        }
    }
} //InvocationCallbackFactory
