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

import org.jruby.Ruby;
import org.jruby.RubyKernel;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;

public class InvocationCallbackFactory extends CallbackFactory implements Opcodes {
    private final static CodegenUtils cg = CodegenUtils.cg;

    private final Class type;
    private final JRubyClassLoader classLoader;
    private final String typePath;
    private final Ruby runtime;

    private final static String SUPER_CLASS = cg.p(InvocationCallback.class);
    private final static String FAST_SUPER_CLASS = cg.p(FastInvocationCallback.class);
    private final static String BLOCK_ID = cg.ci(Block.class);
    private final static String CALL_SIG = cg.sig(RubyKernel.IRUBY_OBJECT, cg.params(Object.class,
            Object[].class, Block.class));
    private final static String FAST_CALL_SIG = cg.sig(RubyKernel.IRUBY_OBJECT, cg.params(
            Object.class, Object[].class));
    private final static String BLOCK_CALL_SIG = cg.sig(RubyKernel.IRUBY_OBJECT, cg.params(
            ThreadContext.class, RubyKernel.IRUBY_OBJECT, IRubyObject[].class));
    private final static String IRUB = cg.p(RubyKernel.IRUBY_OBJECT);
    private final static String IRUB_ID = cg.ci(RubyKernel.IRUBY_OBJECT);

    public InvocationCallbackFactory(Ruby runtime, Class type, JRubyClassLoader classLoader) {
        this.type = type;
        this.classLoader = classLoader;
        this.typePath = cg.p(type);
        this.runtime = runtime;
    }

    private String getReturnName(String method, Class[] args) throws Exception {
        String t = type.getMethod(method, args).getReturnType().getName().replace('.', '/');
        if ("void".equalsIgnoreCase(t)) {
            throw new IllegalArgumentException("Method " + method
                    + " has a void return type. This is not allowed in JRuby.");
        }
        return t;
    }

    private ClassWriter createCtor(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(true);
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

    private ClassWriter createCtorFast(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(true);
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
        ClassWriter cw = new ClassWriter(true);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, cg.p(Object.class),
                new String[] { cg.p(CompiledBlockCallback.class) });
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, cg.p(Object.class), "<init>", "()V");
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
        mv.visitTypeInsn(CHECKCAST, IRUB);
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

    private Class endCall(ClassWriter cw, MethodVisitor mv, String name) {
        mv.visitEnd();
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        return classLoader.defineClass(name, code);
    }

    public Callback getMethod(String method) {
        String mname = type.getName() + "Invoker" + method + "0";
        String mnamePath = typePath + "Invoker" + method + "0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + BLOCK_ID + ")L" + ret
                            + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getMethod(String method, Class arg1) {
        String mname = type.getName() + "Invoker" + method + "1";
        String mnamePath = typePath + "Invoker" + method + "1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { arg1, Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + cg.ci(arg1)
                            + BLOCK_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Invoker" + method + "2";
        String mnamePath = typePath + "Invoker" + method + "2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { arg1, arg2, Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + cg.ci(arg1)
                            + cg.ci(arg2) + BLOCK_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Invoker" + method + "3";
        String mnamePath = typePath + "Invoker" + method + "3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method,
                            new Class[] { arg1, arg2, arg3, Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_2);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg3));
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + cg.ci(arg1)
                            + cg.ci(arg2) + cg.ci(arg3) + BLOCK_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getSingletonMethod(String method) {
        String mname = type.getName() + "InvokerS" + method + "0";
        String mnamePath = typePath + "InvokerS" + method + "0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT,
                            Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + BLOCK_ID
                            + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getSingletonMethod(String method, Class arg1) {
        String mname = type.getName() + "InvokerS" + method + "1";
        String mnamePath = typePath + "InvokerS" + method + "1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT, arg1,
                            Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + cg.ci(arg1)
                            + BLOCK_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getSingletonMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "InvokerS" + method + "2";
        String mnamePath = typePath + "InvokerS" + method + "2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT, arg1,
                            arg2, Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + cg.ci(arg1)
                            + cg.ci(arg2) + BLOCK_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 4);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "InvokerS" + method + "3";
        String mnamePath = typePath + "InvokerS" + method + "3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT, arg1,
                            arg2, arg3, Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_2);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg3));
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + cg.ci(arg1)
                            + cg.ci(arg2) + cg.ci(arg3) + BLOCK_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
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

    public CompiledBlockCallback getBlockCallback(String method) {
        String mname = type.getName() + "Block" + method + "xx1";
        String mnamePath = typePath + "Block" + method + "xx1";
        synchronized (classLoader) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    ClassWriter cw = createBlockCtor(mnamePath);
                    MethodVisitor mv = startBlockCall(cw);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, cg.sig(
                            RubyKernel.IRUBY_OBJECT, cg.params(ThreadContext.class,
                                    RubyKernel.IRUBY_OBJECT, IRubyObject[].class)));
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                CompiledBlockCallback ic = (CompiledBlockCallback) c.newInstance();
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getOptSingletonMethod(String method) {
        String mname = type.getName() + "InvokerS" + method + "xx1";
        String mnamePath = typePath + "InvokerS" + method + "xx1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT,
                            IRubyObject[].class, Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCallS(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + "["
                            + IRUB_ID + BLOCK_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getOptMethod(String method) {
        String mname = type.getName() + "Invoker" + method + "xx1";
        String mnamePath = typePath + "Invoker" + method + "xx1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { IRubyObject[].class,
                            Block.class });
                    ClassWriter cw = createCtor(mnamePath);
                    MethodVisitor mv = startCall(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "([" + IRUB_ID + BLOCK_ID
                            + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                InvocationCallback ic = (InvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastMethod(String method) {
        String mname = type.getName() + "Invoker" + method + "0";
        String mnamePath = typePath + "Invoker" + method + "0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, null);
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "()L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastMethod(String method, Class arg1) {
        String mname = type.getName() + "Invoker" + method + "1";
        String mnamePath = typePath + "Invoker" + method + "1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { arg1 });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + cg.ci(arg1) + ")L"
                            + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Invoker" + method + "2";
        String mnamePath = typePath + "Invoker" + method + "2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { arg1, arg2 });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + cg.ci(arg1)
                            + cg.ci(arg2) + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Invoker" + method + "3";
        String mnamePath = typePath + "Invoker" + method + "3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { arg1, arg2, arg3 });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_2);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg3));
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + cg.ci(arg1)
                            + cg.ci(arg2) + cg.ci(arg3) + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastSingletonMethod(String method) {
        String mname = type.getName() + "InvokerS" + method + "0";
        String mnamePath = typePath + "InvokerS" + method + "0";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + ")L" + ret
                            + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.noArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastSingletonMethod(String method, Class arg1) {
        String mname = type.getName() + "InvokerS" + method + "1";
        String mnamePath = typePath + "InvokerS" + method + "1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method,
                            new Class[] { RubyKernel.IRUBY_OBJECT, arg1 });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + cg.ci(arg1)
                            + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(3, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.singleArgument());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "InvokerS" + method + "2";
        String mnamePath = typePath + "InvokerS" + method + "2";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT, arg1,
                            arg2 });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + cg.ci(arg1)
                            + cg.ci(arg2) + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(4, 4);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.twoArguments());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "InvokerS" + method + "3";
        String mnamePath = typePath + "InvokerS" + method + "3";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT, arg1,
                            arg2, arg3 });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg1));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg2));
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitInsn(ICONST_2);
                    mv.visitInsn(AALOAD);
                    mv.visitTypeInsn(CHECKCAST, cg.p(arg3));
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + cg.ci(arg1)
                            + cg.ci(arg2) + cg.ci(arg3) + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(5, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.fixed(3));
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastOptMethod(String method) {
        String mname = type.getName() + "Invoker" + method + "xx1";
        String mnamePath = typePath + "Invoker" + method + "xx1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { IRubyObject[].class });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "([" + IRUB_ID + ")L" + ret
                            + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public Callback getFastOptSingletonMethod(String method) {
        String mname = type.getName() + "InvokerS" + method + "xx1";
        String mnamePath = typePath + "InvokerS" + method + "xx1";
        synchronized (runtime.getJRubyClassLoader()) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    String ret = getReturnName(method, new Class[] { RubyKernel.IRUBY_OBJECT,
                            IRubyObject[].class });
                    ClassWriter cw = createCtorFast(mnamePath);
                    MethodVisitor mv = startCallSFast(cw);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                    mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + "["
                            + IRUB_ID + ")L" + ret + ";");
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(2, 3);
                    c = endCall(cw, mv, mname);
                }
                FastInvocationCallback ic = (FastInvocationCallback) c.newInstance();
                ic.setArity(Arity.optional());
                return ic;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }
} //InvocationCallbackFactory
