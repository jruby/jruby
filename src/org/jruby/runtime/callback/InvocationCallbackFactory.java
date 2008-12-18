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
import org.jruby.RubyKernel;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.runtime.AbstractCompiledBlockCallback;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.CompiledBlockCallback;
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

    private final static String BLOCK_CALL_SIG = sig(RubyKernel.IRUBY_OBJECT, params(
            ThreadContext.class, RubyKernel.IRUBY_OBJECT, IRubyObject.class));

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

    private ClassWriter createBlockCtor(String namePath) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, namePath, null, p(AbstractCompiledBlockCallback.class), null);
        cw.visitField(ACC_PRIVATE | ACC_FINAL, "$scriptObject", ci(Object.class), null, null);
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", sig(Void.TYPE, params(Object.class)), null, null));
        mv.start();
        mv.aload(0);
        mv.aload(1);
        mv.invokespecial(p(AbstractCompiledBlockCallback.class), "<init>", sig(void.class, Object.class));
        mv.voidreturn();
        mv.end();
        
        return cw;
    }

    private Class tryClass(String name) {
        try {
            return classLoader.loadClass(name);
        } catch (Exception e) {
            return null;
        }
    }

    private SkinnyMethodAdapter startBlockCall(ClassWriter cw) {
        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL, "call", BLOCK_CALL_SIG, null, null));
        
        mv.visitCode();
        Label line = new Label();
        mv.visitLineNumber(0, line);
        return mv;
    }

    protected Class endCall(ClassWriter cw, MethodVisitor mv, String name) {
        mv.visitEnd();
        cw.visitEnd();
        byte[] code = cw.toByteArray();
        return classLoader.defineClass(name, code, protectionDomain);
    }

    public CompiledBlockCallback getBlockCallback(String method, Object scriptObject) {
        Class typeClass = scriptObject.getClass();
        String typePathString = p(typeClass);
        String mname = typeClass.getName() + "BlockCallback$" + method + "xx1";
        String mnamePath = typePathString + "BlockCallback$" + method + "xx1";
        synchronized (classLoader) {
            Class c = tryClass(mname);
            try {
                if (c == null) {
                    ClassWriter cw = createBlockCtor(mnamePath);
                    SkinnyMethodAdapter mv = startBlockCall(cw);
                    mv.aload(0);
                    mv.getfield(p(AbstractCompiledBlockCallback.class), "$scriptObject", ci(Object.class));
                    mv.checkcast(p(typeClass));
                    mv.aload(1);
                    mv.aload(2);
                    mv.aload(3);
                    mv.invokevirtual(typePathString, method, sig(
                            RubyKernel.IRUBY_OBJECT, params(ThreadContext.class,
                                    RubyKernel.IRUBY_OBJECT, IRubyObject.class)));
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
} //InvocationCallbackFactory
