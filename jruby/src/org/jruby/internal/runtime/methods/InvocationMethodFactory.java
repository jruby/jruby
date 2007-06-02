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

import org.jruby.Ruby;
import org.jruby.parser.StaticScope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.Visibility;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class InvocationMethodFactory extends MethodFactory implements Opcodes {
    private final static String COMPILED_SUPER_CLASS = CompiledMethod.class.getName().replace('.','/');
    private final static String IRUB_ID = "Lorg/jruby/runtime/builtin/IRubyObject;";
    private final static String BLOCK_ID = "Lorg/jruby/runtime/Block;";
    private final static String COMPILED_CALL_SIG = "(Lorg/jruby/runtime/ThreadContext;" + IRUB_ID + "[" + IRUB_ID + BLOCK_ID + ")" + IRUB_ID;
    private final static String COMPILED_SUPER_SIG = "(" + ci(RubyModule.class) + ci(Arity.class) + ci(Visibility.class) + ci(SinglyLinkedList.class) + ci(StaticScope.class) + ")V";

    private JRubyClassLoader classLoader;
    
    public InvocationMethodFactory() {
    }
    
    public InvocationMethodFactory(JRubyClassLoader classLoader) {
        this.classLoader = classLoader;
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
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKESPECIAL, sup, "<init>", COMPILED_SUPER_SIG);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
        return cw;
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

    private Class endCall(Ruby runtime, ClassWriter cw, MethodVisitor mv, String name) {
        mv.visitMaxs(0,0);
        mv.visitEnd();
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        if (classLoader == null) classLoader = runtime.getJRubyClassLoader();
         
        return classLoader.defineClass(name, code);
    }

    private DynamicMethod getCompleteMethod(RubyModule implementationClass, Class type, String method, Arity arity, Visibility visibility, SinglyLinkedList cref, StaticScope scope, String sup) {
        String typePath = p(type);
        String mname = type.getName() + "Invoker" + method + arity;
        String mnamePath = typePath + "Invoker" + method + arity;
        Class c = tryClass(implementationClass.getRuntime(), mname);
        try {
            if (c == null) {
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
            
            return (DynamicMethod)c.getConstructor(new Class[]{RubyModule.class, Arity.class, Visibility.class, SinglyLinkedList.class, StaticScope.class}).newInstance(new Object[]{implementationClass,arity,visibility,cref,scope});
        } catch(Exception e) {
            e.printStackTrace();
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    public DynamicMethod getCompiledMethod(RubyModule implementationClass, Class type, String method, Arity arity, Visibility visibility, SinglyLinkedList cref, StaticScope scope) {
        return getCompleteMethod(implementationClass,type,method,arity,visibility,cref,scope, COMPILED_SUPER_CLASS);
    }
}
