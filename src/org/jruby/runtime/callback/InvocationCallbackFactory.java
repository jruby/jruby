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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.Dispatcher;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;

public class InvocationCallbackFactory extends CallbackFactory implements Opcodes {
    private final static CodegenUtils cg = CodegenUtils.cg;

    private final Class type;
    protected final JRubyClassLoader classLoader;
    private final String typePath;
    protected final Ruby runtime;

    private final static String SUPER_CLASS = cg.p(InvocationCallback.class);
    private final static String FAST_SUPER_CLASS = cg.p(FastInvocationCallback.class);
    private final static String CALL_SIG = cg.sig(RubyKernel.IRUBY_OBJECT, cg.params(Object.class,
            Object[].class, Block.class));
    private final static String FAST_CALL_SIG = cg.sig(RubyKernel.IRUBY_OBJECT, cg.params(
            Object.class, Object[].class));
    private final static String BLOCK_CALL_SIG = cg.sig(RubyKernel.IRUBY_OBJECT, cg.params(
            ThreadContext.class, RubyKernel.IRUBY_OBJECT, IRubyObject[].class));
    private final static String IRUB = cg.p(RubyKernel.IRUBY_OBJECT);
    
    
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

    public InvocationCallbackFactory(Ruby runtime, Class type, ClassLoader classLoader) {
        this.type = type;
        if (classLoader instanceof JRubyClassLoader) {
            this.classLoader = (JRubyClassLoader)classLoader;
        } else {
           this.classLoader = new JRubyClassLoader(classLoader);
        }
        this.typePath = cg.p(type);
        this.runtime = runtime;
    }

    private Class getReturnClass(String method, Class[] args) throws Exception {
        return type.getMethod(method, args).getReturnType();
    }

    private ClassWriter createCtor(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, SUPER_CLASS, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER_CLASS, "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return cw;
    }

    private ClassWriter createCtorDispatcher(String namePath, Map switchMap) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, cg.p(Dispatcher.class), null);
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", cg.sig(Void.TYPE, cg.params(Ruby.class)), null, null));
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, cg.p(Dispatcher.class), "<init>", "()V");
        
        
        // create our array
        mv.aload(0);
        mv.ldc(new Integer(MethodIndex.NAMES.size()));
        mv.newarray(T_BYTE);
        mv.putfield(cg.p(Dispatcher.class), "switchTable", cg.ci(byte[].class));
        
        // for each switch value, set it into the table
        mv.aload(0);
        mv.getfield(cg.p(Dispatcher.class), "switchTable", cg.ci(byte[].class));
        
        for (Iterator switchIter = switchMap.keySet().iterator(); switchIter.hasNext();) {
            Integer switchValue = (Integer)switchIter.next();
            mv.dup();
            
            // method index
            mv.ldc(new Integer(MethodIndex.getIndex((String)switchMap.get(switchValue))));
            // switch value is one-based, add one
            mv.ldc(switchValue);
            
            // store
            mv.barraystore();
        }
        
        // clear the extra table on stack
        mv.pop();
        
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return cw;
    }

    private ClassWriter createCtorFast(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, FAST_SUPER_CLASS, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, FAST_SUPER_CLASS, "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return cw;
    }

    private ClassWriter createBlockCtor(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, cg.p(Object.class),
                new String[] { cg.p(CompiledBlockCallback.class) });
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "$scriptObject", cg.ci(Object.class), null, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", cg.sig(Void.TYPE, cg.params(Object.class)), null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, cg.p(Object.class), "<init>", "()V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, namePath, "$scriptObject", cg.ci(Object.class));
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return cw;
    }

    private Class tryClass(String name) {
        try {
            return classLoader.loadClass(name);
        } catch (Exception e) {
            return null;
        }
    }

    private MethodVisitor startCall(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", CALL_SIG, null, null);
        ;
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typePath);
        return mv;
    }

    private MethodVisitor startCallS(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", CALL_SIG, null, null);
        ;
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        checkCast(mv, IRubyObject.class);
        return mv;
    }

    private MethodVisitor startCallFast(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", FAST_CALL_SIG, null, null);
        ;
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typePath);
        return mv;
    }

    private MethodVisitor startDispatcher(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "callMethod", cg.sig(IRubyObject.class, cg.params(ThreadContext.class, IRubyObject.class, RubyModule.class, Integer.TYPE, String.class,
                IRubyObject[].class, CallType.class, Block.class)), null, null);
        ;
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, typePath);
        return mv;
    }

    private MethodVisitor startCallSFast(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", FAST_CALL_SIG, null, null);
        ;
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, IRUB);
        return mv;
    }

    private MethodVisitor startBlockCall(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", BLOCK_CALL_SIG, null, null);
        ;
        mv.visitCode();
        return mv;
    }

    protected Class endCall(ClassWriter cw, MethodVisitor mv, String name) {
        mv.visitEnd();
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        return classLoader.defineClass(name, code);
    }

    public Callback getMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "_0";
        String mnamePath = typePath + "Invoker$" + method + "_0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getMethod(String method, Class arg1) {
        String mname = type.getName() + "Invoker$" + method + "_1";
        String mnamePath = typePath + "Invoker$" + method + "_1";
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
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Invoker$" + method + "_2";
        String mnamePath = typePath + "Invoker$" + method + "_2";
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
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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
    
    public Callback getMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Invoker$" + method + "_3";
        String mnamePath = typePath + "Invoker$" + method + "_3";
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
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getSingletonMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "S0";
        String mnamePath = typePath + "Invoker$" + method + "S0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT, Block.class };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getSingletonMethod(String method, Class arg1) {
        String mname = type.getName() + "Invoker$" + method + "_S1";
        String mnamePath = typePath + "Invoker$" + method + "_S1";
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
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getSingletonMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Invoker$" + method + "_S2";
        String mnamePath = typePath + "Invoker$" + method + "_S2";
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
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Invoker$" + method + "_S3";
        String mnamePath = typePath + "Invoker$" + method + "_S3";
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
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getBlockMethod(String method) {
        // TODO: This is probably BAD...
        return new ReflectionCallback(type, method, new Class[] { RubyKernel.IRUBY_OBJECT,
                RubyKernel.IRUBY_OBJECT }, false, true, Arity.fixed(2), false);
    }

    public CompiledBlockCallback getBlockCallback(String method, Object scriptObject) {
        Class type = scriptObject.getClass();
        String typePath = cg.p(type);
        String mname = type.getName() + "Block" + method + "xx1";
        String mnamePath = typePath + "Block" + method + "xx1";
        synchronized (classLoader) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    ClassWriter cw = createBlockCtor(mnamePath);
                    MethodVisitor mv = startBlockCall(cw);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, mnamePath, "$scriptObject", cg.ci(Object.class));
                    mv.visitTypeInsn(CHECKCAST, cg.p(type));
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(
                            RubyKernel.IRUBY_OBJECT, cg.params(ThreadContext.class,
                                    RubyKernel.IRUBY_OBJECT, IRubyObject[].class)));
                    mv.visitInsn(ARETURN);
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

    public Callback getOptSingletonMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "_Sopt";
        String mnamePath = typePath + "Invoker$" + method + "_Sopt";
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
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getOptMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "_opt";
        String mnamePath = typePath + "Invoker$" + method + "_opt";
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
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "_F0";
        String mnamePath = typePath + "Invoker$" + method + "_F0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class ret = getReturnClass(method, new Class[0]);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret));
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

    public Callback getFastMethod(String method, Class arg1) {
        String mname = type.getName() + "Invoker$" + method + "_F1";
        String mnamePath = typePath + "Invoker$" + method + "_F1";
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

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Invoker$" + method + "_F2";
        String mnamePath = typePath + "Invoker$" + method + "_F2";
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

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Invoker$" + method + "_F3";
        String mnamePath = typePath + "Invoker$" + method + "_F3";
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

                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastSingletonMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "_FS0";
        String mnamePath = typePath + "Invoker$" + method + "_FS0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    Class[] signature = new Class[] { RubyKernel.IRUBY_OBJECT };
                    Class ret = getReturnClass(method, signature);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastSingletonMethod(String method, Class arg1) {
        String mname = type.getName() + "Invoker$" + method + "_FS1";
        String mnamePath = typePath + "Invoker$" + method + "_FS1";
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

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Invoker$" + method + "_FS2";
        String mnamePath = typePath + "Invoker$" + method + "_FS2";
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

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Invoker$" + method + "_FS3";
        String mnamePath = typePath + "Invoker$" + method + "_FS3";
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

                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastOptMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "_Fopt";
        String mnamePath = typePath + "Invoker$" + method + "_Fopt";
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
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, cg.sig(ret, signature));
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

    public Callback getFastOptSingletonMethod(String method) {
        String mname = type.getName() + "Invoker$" + method + "_FSopt";
        String mnamePath = typePath + "Invoker$" + method + "_FSopt";
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
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(ret, signature));
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
    
    public Dispatcher createDispatcher(RubyClass metaClass) {
        String className = type.getName() + "Dispatcher_for_" + metaClass.getBaseName();
        String classPath = typePath + "Dispatcher_for_" + metaClass.getBaseName();
        
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(className);
            try {
                if (c == null) {
                    // build a map of all methods from the module and all its parents
                    Map allMethods = new HashMap();
                    RubyModule current = metaClass;
                    
                    while (current != null) {
                        for (Iterator methodIter = current.getMethods().entrySet().iterator(); methodIter.hasNext();) {
                            Map.Entry entry = (Map.Entry)methodIter.next();
                            
                            if (allMethods.containsKey(entry.getKey())) continue;
                            
                            DynamicMethod dynamicMethod = (DynamicMethod)entry.getValue();
                            if (!(dynamicMethod instanceof JavaMethod)) {
                                // method is not a simple/fast method, don't add to our big switch
                                // FIXME: eventually, we'll probably want to add it to the switch for fast non-hash dispatching
                                continue;
                            } else {
                                // TODO: skip singleton methods for now; we'll figure out fast dispatching for them in a future patch
                                JavaMethod javaMethod = (JavaMethod)dynamicMethod;
                                        
                                // singleton methods require doing a static invocation, etc...disabling again for now
                                if (javaMethod.isSingleton() || javaMethod.getCallConfig() != CallConfiguration.JAVA_FAST) continue;
                                
                                // skipping non-public methods for now, to avoid visibility checks in STI
                                if (dynamicMethod.getVisibility() != Visibility.PUBLIC) continue;
                            }
                            
                            allMethods.put(entry.getKey(), entry.getValue());
                        }
                        current = current.getSuperClass();
                        while (current != null && current.isIncluded()) current = current.getSuperClass();
                    }
                    
                    // switches are 1-based, so add one
                    Label[] labels = new Label[allMethods.size()];
                    Label defaultLabel = new Label();

                    int switchValue = 0;
                    Map switchMap = new HashMap();
                    
                    // NOTE: We sort the list of keys here to ensure they're encountered in the same order from run to run
                    // This will aid AOT compilation, since a given revision of JRuby should always generate the same
                    // sequence of method indices, and code compiled for that revision should continue to work.
                    // FIXME: This will not aid compiling once and running across JRuby versions, since method indices
                    // could be generated in a different order on a different revision (adds, removes, etc over time)
                    List methodKeys = new ArrayList(allMethods.keySet());
                    Collections.sort(methodKeys);
                    for (Iterator methodIter = methodKeys.iterator(); methodIter.hasNext();) {
                        String indexKey = (String)methodIter.next();
                        switchValue++;
                        
                        switchMap.put(new Integer(switchValue), indexKey);
                        // switches are one-based, so subtract one
                        labels[switchValue - 1] = new Label();
                    }

                    ClassWriter cw = createCtorDispatcher(classPath, switchMap);
                    SkinnyMethodAdapter mv = new SkinnyMethodAdapter(startDispatcher(cw));
                    
                    // store runtime
                    mv.aload(DISPATCHER_THREADCONTEXT_INDEX);
                    mv.invokevirtual(cg.p(ThreadContext.class), "getRuntime", cg.sig(Ruby.class));
                    mv.astore(DISPATCHER_RUNTIME_INDEX);
                    
                    Label tryBegin = new Label();
                    Label tryEnd = new Label();
                    Label tryCatch = new Label();
                    mv.trycatch(tryBegin, tryEnd, tryCatch, cg.p(StackOverflowError.class));
                    mv.label(tryBegin);
                    
                    // invoke directly

                    // receiver is already loaded by startDispatcher
                    
                    // check if tracing is on
                    mv.aload(DISPATCHER_RUNTIME_INDEX);
                    mv.invokevirtual(cg.p(Ruby.class), "hasEventHooks", cg.sig(boolean.class));
                    mv.ifne(defaultLabel);
                    
                    // if no switch values, go straight to default
                    if (switchValue == 0) {
                        mv.go_to(defaultLabel);
                    } else {
                        // load switch value
                        mv.aload(0);
                        mv.getfield(cg.p(Dispatcher.class), "switchTable", cg.ci(byte[].class));
                        
                        // ensure size isn't too large
                        mv.dup();
                        mv.arraylength();
                        mv.iload(DISPATCHER_METHOD_INDEX);
                        Label ok = new Label();
                        mv.if_icmpgt(ok);
                        
                        // size is too large, remove extra table and go to default
                        mv.pop();
                        mv.go_to(defaultLabel);
                        
                        // size is ok, retrieve from table and switch on the result
                        mv.label(ok);
                        mv.iload(DISPATCHER_METHOD_INDEX);
                        mv.barrayload();
                        
                        // perform switch
                        mv.tableswitch(1, switchValue, defaultLabel, labels);
                        
                        for (int i = 0; i < labels.length; i++) {
                            String rubyName = (String)switchMap.get(new Integer(i + 1));
                            DynamicMethod dynamicMethod = (DynamicMethod)allMethods.get(rubyName);
                            
                            mv.label(labels[i]);
    
                            // based on the check above, it's a fast method, we can fast dispatch
                            JavaMethod javaMethod = (JavaMethod)dynamicMethod;
                            String method = javaMethod.getJavaName();
                            Arity arity = javaMethod.getArity();
                            Class[] descriptor = javaMethod.getArgumentTypes();
                            
                            // arity check
                            checkArity(mv, arity);
                            
                            // if singleton load self/recv
                            if (javaMethod.isSingleton()) {
                                mv.aload(DISPATCHER_SELF_INDEX);
                            }
                            
                            switch (arity.getValue()) {
                            case 3:
                                loadArguments(mv, DISPATCHER_ARGS_INDEX, 3, descriptor);
                                break;
                            case 2:
                                loadArguments(mv, DISPATCHER_ARGS_INDEX, 2, descriptor);
                                break;
                            case 1:
                                loadArguments(mv, DISPATCHER_ARGS_INDEX, 1, descriptor);
                                break;
                            case 0:
                                break;
                            default: // this should catch all opt/rest cases
                                mv.aload(DISPATCHER_ARGS_INDEX);
                                checkCast(mv, IRubyObject[].class);
                                break;
                            }
                            
                            Class ret = getReturnClass(method, descriptor);
                            String callSig = cg.sig(ret, descriptor);

                            // if block, pass it
                            if (descriptor.length > 0 && descriptor[descriptor.length - 1] == Block.class) {
                                mv.aload(DISPATCHER_BLOCK_INDEX);
                            }
                            
                            mv.invokevirtual(typePath, method, callSig);
                            mv.areturn();
                        }
                    }
                    
                    // done with cases, handle default case by getting method object and invoking it
                    mv.label(defaultLabel);
                    Label afterCall = new Label();
                    
                    dispatchWithoutSTI(mv, afterCall);

                    mv.label(tryEnd);
                    mv.go_to(afterCall);

                    mv.label(tryCatch);
                    mv.aload(DISPATCHER_RUNTIME_INDEX);
                    mv.ldc("stack level too deep");
                    mv.invokevirtual(cg.p(Ruby.class), "newSystemStackError", cg.sig(RaiseException.class, cg.params(String.class)));
                    mv.athrow();

                    // calls done, return
                    mv.label(afterCall);
                    
                    mv.areturn();
                    mv.visitMaxs(1, 1);
                    c = endCall(cw, mv, className);
                }
                Dispatcher dispatcher = (Dispatcher)c.getConstructor(new Class[] {Ruby.class}).newInstance(new Object[] {runtime});
                return dispatcher;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }
    
    private void dispatchWithoutSTI(SkinnyMethodAdapter mv, Label afterCall) {
        // retrieve method
        mv.aload(DISPATCHER_RUBYMODULE_INDEX); // module
        mv.aload(DISPATCHER_NAME_INDEX); // name
        mv.invokevirtual(cg.p(RubyModule.class), "searchMethod", cg.sig(DynamicMethod.class, cg.params(String.class)));

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
        mv.invokevirtual(cg.p(DynamicMethod.class), "call",
                cg.sig(IRubyObject.class, 
                cg.params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class)));
    }
    
    public void callMethodMissingIfNecessary(SkinnyMethodAdapter mv, Label afterCall, Label okCall) {
        Label methodMissing = new Label();

        // if undefined, branch to method_missing
        mv.dup();
        mv.invokevirtual(cg.p(DynamicMethod.class), "isUndefined", cg.sig(boolean.class));
        mv.ifne(methodMissing);

        // if we're not attempting to invoke method_missing and method is not visible, branch to method_missing
        mv.aload(DISPATCHER_NAME_INDEX);
        mv.ldc("method_missing");
        // if it's method_missing, just invoke it
        mv.invokevirtual(cg.p(String.class), "equals", cg.sig(boolean.class, cg.params(Object.class)));
        mv.ifne(okCall);
        // check visibility
        mv.dup(); // dup method
        mv.aload(DISPATCHER_THREADCONTEXT_INDEX);
        mv.invokevirtual(cg.p(ThreadContext.class), "getFrameSelf", cg.sig(IRubyObject.class));
        mv.aload(DISPATCHER_CALLTYPE_INDEX);
        mv.invokevirtual(cg.p(DynamicMethod.class), "isCallableFrom", cg.sig(boolean.class, cg.params(IRubyObject.class, CallType.class)));
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
        mv.invokevirtual(cg.p(ThreadContext.class), "getFrameSelf", cg.sig(IRubyObject.class));

        mv.aload(DISPATCHER_CALLTYPE_INDEX); // calltype
        mv.aload(DISPATCHER_BLOCK_INDEX); // block

        // invoke callMethodMissing method directly
        // TODO: this could be further optimized, since some DSLs hit method_missing pretty hard...
        mv.invokestatic(cg.p(RuntimeHelpers.class), "callMethodMissing", cg.sig(IRubyObject.class, 
                cg.params(ThreadContext.class, IRubyObject.class, DynamicMethod.class, String.class, 
                                    IRubyObject[].class, IRubyObject.class, CallType.class, Block.class)));
        // if no exception raised, jump to end to leave result on stack for return
        mv.go_to(afterCall);
    }
    
    private void loadArguments(MethodVisitor mv, int argsIndex, int count, Class[] types) {
        for (int i = 0; i < count; i++) {
            loadArgument(mv, argsIndex, i, types[i]);
        }
    }
    
    private void loadArgument(MethodVisitor mv, int argsIndex, int argIndex, Class type1) {
        mv.visitVarInsn(ALOAD, argsIndex);
        mv.visitLdcInsn(new Integer(argIndex));
        mv.visitInsn(AALOAD);
        checkCast(mv, type1);
    }

    private void checkCast(MethodVisitor mv, Class clazz) {
        mv.visitTypeInsn(CHECKCAST, cg.p(clazz));
    }

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

            mv.invokevirtual(cg.p(Ruby.class), "newArgumentError", cg.sig(RaiseException.class, cg.params(int.class, int.class)));
            mv.athrow();

            // arity ok, continue
            mv.label(arityOk);
        }
    }
} //InvocationCallbackFactory
