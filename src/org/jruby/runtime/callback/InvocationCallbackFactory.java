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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class InvocationCallbackFactory extends CallbackFactory implements Opcodes {
    public static class InvokeClassLoader extends ClassLoader {
        public static final InvokeClassLoader INSTANCE = new InvokeClassLoader();

        public Class define(String name, byte[] code) throws ClassFormatError {
            return defineClass(name,code,0,code.length);
        }
    }

    private final Class type;
    private final String typePath;

    private final static String SUPER_CLASS = InvocationCallback.class.getName().replace('.','/');
    private final static String CALL_SIG = "(Ljava/lang/Object;[Ljava/lang/Object;)Lorg/jruby/runtime/builtin/IRubyObject;";
    private final static String IRUB = "org/jruby/runtime/builtin/IRubyObject";
    private final static String IRUB_ID = "Lorg/jruby/runtime/builtin/IRubyObject;";

    /**
     * Creates a class path name, from a Class.
     */
    private static String p(Class n) {
        return n.getName().replace('.','/');
    }

    /**
     * Creates a class identifier of form Labc/abc;, from a Class.
     */
    private static String ci(Class n) {
        return "L" + p(n) + ";";
    }

    public InvocationCallbackFactory(Class type) {
        this.type = type;
        this.typePath = p(type);
    }

    private String getReturnName(String method, Class[] args) throws Exception {
        return type.getMethod(method,args).getReturnType().getName().replace('.','/');
    }

    private ClassWriter createCtor(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(false);
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

    private Class tryClass(String name) {
        try {
            return Class.forName(name,true,InvokeClassLoader.INSTANCE);
        } catch(Exception e) {
            return null;
        }
    }

    private MethodVisitor startCall(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", CALL_SIG, null, null);;
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typePath);
        return mv;
    }

    private MethodVisitor startCallS(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", CALL_SIG, null, null);;
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, IRUB);
        return mv;
    }

    private Class endCall(ClassWriter cw, MethodVisitor mv, String name) {
        mv.visitEnd();
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        return InvokeClassLoader.INSTANCE.define(name, code);
    }

    public Callback getMethod(String method) {
        String mname = type.getName() + "Invoker" + method + "0";
        String mnamePath = typePath + "Invoker" + method + "0";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,null);
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCall(cw);
                mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "()L" + ret + ";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(1, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.noArguments());
            return ic;
        } catch(Exception e) {
            return null;
        }
    }

    public Callback getMethod(String method, Class arg1) {
        String mname = type.getName() + "Invoker" + method + "1";
        String mnamePath = typePath + "Invoker" + method + "1";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{arg1});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCall(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg1));
                mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "("+ci(arg1)+")L"+ret+";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(3, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.singleArgument());
            return ic;
        } catch(Exception e) {
            return null;
        }
    }

    public Callback getMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "Invoker" + method + "2";
        String mnamePath = typePath + "Invoker" + method + "2";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{arg1,arg2});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCall(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg1));
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg2));
                mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "("+ci(arg1)+ci(arg2)+")L"+ret+";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(4, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.twoArguments());
            return ic;
        } catch(Exception e) {
            return null;
        }
    }
    
    public Callback getMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "Invoker" + method + "3";
        String mnamePath = typePath + "Invoker" + method + "3";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{arg1,arg2,arg3});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCall(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg1));
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg2));
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_2);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg3));
                mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "("+ci(arg1)+ci(arg2)+ci(arg3)+")L"+ret+";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(5, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.fixed(3));
            return ic;
        } catch(Exception e) {
            return null;
        }
    }

    public Callback getSingletonMethod(String method) {
        String mname = type.getName() + "InvokerS" + method + "0";
        String mnamePath = typePath + "InvokerS" + method + "0";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{IRubyObject.class});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCallS(cw);
                mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + ")L" + ret +";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(1, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.noArguments());
            return ic;
        } catch(Exception e) {
            return null;
        }
    }

    public Callback getSingletonMethod(String method, Class arg1) {
        String mname = type.getName() + "InvokerS" + method + "1";
        String mnamePath = typePath + "InvokerS" + method + "1";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{IRubyObject.class,arg1});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCallS(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg1));
                mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + ci(arg1) + ")L" + ret + ";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(3, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.singleArgument());
            return ic;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Callback getSingletonMethod(String method, Class arg1, Class arg2) {
        String mname = type.getName() + "InvokerS" + method + "2";
        String mnamePath = typePath + "InvokerS" + method + "2";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{IRubyObject.class,arg1,arg2});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCallS(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg1));
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg2));
                mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + ci(arg1) + ci(arg2) + ")L" + ret + ";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(4, 4);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.twoArguments());
            return ic;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Callback getSingletonMethod(String method, Class arg1, Class arg2, Class arg3) {
        String mname = type.getName() + "InvokerS" + method + "3";
        String mnamePath = typePath + "InvokerS" + method + "3";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{IRubyObject.class,arg1,arg2,arg3});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCallS(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_0);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg1));
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg2));
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_2);
                mv.visitInsn(AALOAD);
                mv.visitTypeInsn(CHECKCAST, p(arg3));
                mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + ci(arg1) + ci(arg2) + ci(arg3) + ")L" + ret + ";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(5, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.fixed(3));
            return ic;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Callback getBlockMethod(String method) {
        return new ReflectionCallback(
            type,
            method,
            new Class[] { IRubyObject.class, IRubyObject.class },
            false,
            true,
            Arity.fixed(2));
    }

    public Callback getOptSingletonMethod(String method) {
        String mname = type.getName() + "InvokerS" + method + "xx1";
        String mnamePath = typePath + "InvokerS" + method + "xx1";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{IRubyObject.class,IRubyObject[].class});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCallS(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(" + IRUB_ID + "[" + IRUB_ID + ")L"+ret+";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(2, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.optional());
            return ic;
        } catch(Exception e) {
            return null;
        }
    }

    public Callback getOptMethod(String method) {
        String mname = type.getName() + "Invoker" + method + "xx1";
        String mnamePath = typePath + "Invoker" + method + "xx1";
        Class c = tryClass(mname);
        try {
            if(c == null) {
                String ret = getReturnName(method,new Class[]{IRubyObject[].class});
                ClassWriter cw = createCtor(mnamePath);
                MethodVisitor mv = startCall(cw);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                mv.visitTypeInsn(CHECKCAST, "[" + IRUB_ID);
                mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "([" + IRUB_ID + ")L" + ret + ";");
                mv.visitInsn(ARETURN);
                mv.visitMaxs(2, 3);
                c = endCall(cw,mv,mname);
            }
            InvocationCallback ic = (InvocationCallback)c.newInstance();
            ic.setArity(Arity.optional());
            return ic;
        } catch(Exception e) {
            return null;
        }
    }
} //InvocationCallbackFactory

