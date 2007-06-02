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
package org.jruby.internal.runtime.methods;

import java.io.File;
import java.io.FileOutputStream;

import org.jruby.Ruby;
import org.jruby.RubyKernel;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class DumpingInvocationMethodFactory extends MethodFactory implements Opcodes {
    private final static Class IRUBY_OBJECT_ARR = IRubyObject[].class;
    private final static String SIMPLE_SUPER_CLASS = SimpleInvocationMethod.class.getName().replace('.','/');
    private final static String COMPILED_SUPER_CLASS = CompiledMethod.class.getName().replace('.','/');
    private final static String FULL_SUPER_CLASS = FullInvocationMethod.class.getName().replace('.','/');
    private final static String IRUB_ID = "Lorg/jruby/runtime/builtin/IRubyObject;";
    private final static String BLOCK_ID = "Lorg/jruby/runtime/Block;";
    private final static String CALL_SIG = "(" + IRUB_ID + "[" + IRUB_ID + BLOCK_ID + ")" + IRUB_ID;
    private final static String CALL_SIG_NB = "(" + IRUB_ID + "[" + IRUB_ID + ")" + IRUB_ID;
    private final static String COMPILED_CALL_SIG = "(Lorg/jruby/runtime/ThreadContext;" + IRUB_ID + "[" + IRUB_ID + BLOCK_ID + ")" + IRUB_ID;
    private final static String SUPER_SIG = "(" + ci(RubyModule.class) + ci(Arity.class) + ci(Visibility.class) + ")V";
    private final static String COMPILED_SUPER_SIG = "(" + ci(RubyModule.class) + ci(Arity.class) + ci(Visibility.class) + ci(SinglyLinkedList.class) + ")V";

    private String dumpPath;
    
    public DumpingInvocationMethodFactory(String path) {
        this.dumpPath = path;
    }

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

    private ClassWriter createCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(true);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", SUPER_SIG, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", SUPER_SIG);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        return cw;
    }

    private ClassWriter createCompiledCtor(String namePath, String sup) throws Exception {
        ClassWriter cw = new ClassWriter(true);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, sup, null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", COMPILED_SUPER_SIG, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", COMPILED_SUPER_SIG);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        return cw;
    }

    private Class tryClass(Ruby runtime, String name) {
        try {
            return Class.forName(name,true,runtime.getJRubyClassLoader());
        } catch(Exception e) {
            return null;
        }
    }

    private Class endCall(Ruby runtime, ClassWriter cw, MethodVisitor mv, String name) {
        mv.visitMaxs(0,0);
        mv.visitEnd();
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        String cname = name.replace('.','/');
        File f = new File(dumpPath,cname+".class");
        f.getParentFile().mkdirs();
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(code);
            fos.close();
        } catch(Exception e) {
        }
        return runtime.getJRubyClassLoader().defineClass(name, code);
    }

    private String getReturnName(Class type, String method, Class[] args) throws Exception {
        String t = ci(type.getMethod(method,args).getReturnType());
        if("void".equalsIgnoreCase(t)) {
            throw new IllegalArgumentException("Method " + method + " has a void return type. This is not allowed in JRuby.");
        }
        return t;
    }

    private DynamicMethod getMethod(RubyModule implementationClass, Class type, String method, Arity arity, Visibility visibility, String sup, boolean block) {
        String typePath = p(type);
        String mname = type.getName() + "Invoker" + method + arity;
        String mnamePath = typePath + "Invoker" + method + arity;
        Class c = tryClass(implementationClass.getRuntime(), mname);
        try {
            if(c == null) {
                ClassWriter cw = createCtor(mnamePath,sup);
                MethodVisitor mv = null;
                if(arity.isFixed()) {
                    int ar_len = arity.getValue();
                    Class[] sign = new Class[ block ? ar_len+1 : ar_len ];
                    java.util.Arrays.fill(sign,RubyKernel.IRUBY_OBJECT);
                    if(block) {
                        sign[sign.length-1] = Block.class;
                    }
                    StringBuffer sbe = new StringBuffer();
                    for(int i=0;i<ar_len;i++) {
                        sbe.append(IRUB_ID);
                    }
                    if(block) {
                        sbe.append(BLOCK_ID);
                    }
                    String ret = getReturnName(type, method, sign);
                    mv = cw.visitMethod(ACC_PUBLIC, "call", block ? CALL_SIG : CALL_SIG_NB, null, null);
                    mv.visitCode();
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitTypeInsn(CHECKCAST, typePath);
                    for(int i=0;i<ar_len;i++) {
                        mv.visitVarInsn(ALOAD, 2);
                        if(i < 6) {
                            mv.visitInsn(ICONST_0 + i);
                        } else {
                            mv.visitIntInsn(BIPUSH,i);
                        }
                        mv.visitInsn(AALOAD);
                    }
                    if(block) {
                        mv.visitVarInsn(ALOAD, 3);
                    }
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "(" + sbe + ")" + ret);
                    mv.visitInsn(ARETURN);
                } else {
                    String ret = getReturnName(type, method, block ? new Class[]{IRUBY_OBJECT_ARR, Block.class} : new Class[]{IRUBY_OBJECT_ARR});
                    mv = cw.visitMethod(ACC_PUBLIC, "call", block ? CALL_SIG : CALL_SIG_NB, null, null);
                    mv.visitCode();
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitTypeInsn(CHECKCAST, typePath);
                    mv.visitVarInsn(ALOAD, 2);
                    if(block) {
                        mv.visitVarInsn(ALOAD, 3);
                    }
                    mv.visitMethodInsn(INVOKEVIRTUAL, typePath, method, "([" + IRUB_ID + (block ? BLOCK_ID : "") + ")" + ret);
                    mv.visitInsn(ARETURN);
                }
                c = endCall(implementationClass.getRuntime(), cw,mv,mname);
            }
            
            return (DynamicMethod)c.getConstructor(new Class[]{RubyModule.class, Arity.class, Visibility.class}).newInstance(new Object[]{implementationClass,arity,visibility});
        } catch(Exception e) {
            e.printStackTrace();
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    private DynamicMethod getCompleteMethod(RubyModule implementationClass, Class type, String method, Arity arity, Visibility visibility, SinglyLinkedList cref, String sup) {
        String typePath = p(type);
        String mname = type.getName() + "Invoker" + method + arity;
        String mnamePath = typePath + "Invoker" + method + arity;
        Class c = tryClass(implementationClass.getRuntime(), mname);
        try {
            if(c == null) {
                ClassWriter cw = createCompiledCtor(mnamePath,sup);
                MethodVisitor mv = null;

                // compiled methods will always return IRubyObject
                //String ret = getReturnName(type, method, new Class[]{ThreadContext.class, RubyKernel.IRUBY_OBJECT, IRUBY_OBJECT_ARR, Block.class});
                String ret = IRUB_ID;
                mv = cw.visitMethod(ACC_PUBLIC, "call", COMPILED_CALL_SIG, null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitVarInsn(ALOAD, 4);
                mv.visitMethodInsn(INVOKESTATIC, typePath, method, "(Lorg/jruby/runtime/ThreadContext;" + IRUB_ID + "[" + IRUB_ID + "Lorg/jruby/runtime/Block;)" + ret);
                mv.visitInsn(ARETURN);
                
                c = endCall(implementationClass.getRuntime(), cw,mv,mname);
            }
            
            return (DynamicMethod)c.getConstructor(new Class[]{RubyModule.class, Arity.class, Visibility.class, SinglyLinkedList.class}).newInstance(new Object[]{implementationClass,arity,visibility,cref});
        } catch(Exception e) {
            e.printStackTrace();
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    public DynamicMethod getFullMethod(RubyModule implementationClass, Class type, String method, Arity arity, Visibility visibility) {
        return getMethod(implementationClass,type,method,arity,visibility,FULL_SUPER_CLASS, true);
    }

    public DynamicMethod getSimpleMethod(RubyModule implementationClass, Class type, String method, Arity arity, Visibility visibility) {
        return getMethod(implementationClass,type,method,arity,visibility,SIMPLE_SUPER_CLASS, false);
    }

    public DynamicMethod getCompiledMethod(RubyModule implementationClass, Class type, String method, Arity arity, Visibility visibility, SinglyLinkedList cref, StaticScope scope) {
        return getCompleteMethod(implementationClass,type,method,arity,visibility,cref,COMPILED_SUPER_CLASS);
    }
}// DumpingInvocationMethodFactory
