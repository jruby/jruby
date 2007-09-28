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
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.javasupport.util.CompilerHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;
import org.objectweb.asm.Label;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class InvocationMethodFactory extends MethodFactory implements Opcodes {
    public final static CodegenUtils cg = CodegenUtils.cg;
    private final static String COMPILED_SUPER_CLASS = CompiledMethod.class.getName().replace('.','/');
    private final static String COMPILED_CALL_SIG = cg.sig(IRubyObject.class,
            cg.params(ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
    private final static String COMPILED_SUPER_SIG = cg.sig(Void.TYPE, cg.params(RubyModule.class, Arity.class, Visibility.class, StaticScope.class, Object.class));

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

    /**
     * Creates a class identifier of form Labc/abc;, from a Class.
     */
    private static String ci(Class n) {
        return "L" + p(n) + ";";
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
    
    public static final int SCRIPT_INDEX = 0;
    public static final int THREADCONTEXT_INDEX = 1;
    public static final int RECEIVER_INDEX = 2;
    public static final int CLASS_INDEX = 3;
    public static final int NAME_INDEX = 4;
    public static final int ARGS_INDEX = 5;
    public static final int BLOCK_INDEX = 6;

    private DynamicMethod getCompleteMethod(RubyModule implementationClass, String method, Arity arity, Visibility visibility, StaticScope scope, String sup, Object scriptObject) {
        Class scriptClass = scriptObject.getClass();
        String typePath = p(scriptClass);
        String mname = scriptClass.getName() + "Invoker" + method + arity;
        String mnamePath = typePath + "Invoker" + method + arity;
        Class c = tryClass(implementationClass.getRuntime(), mname);
        
        try {
            if (c == null) {
                ClassWriter cw = createCompiledCtor(mnamePath,sup);
                SkinnyMethodAdapter mv = null;
                
                mv = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "call", COMPILED_CALL_SIG, null, null));
                mv.visitCode();
                
                // invoke pre method stuff
                mv.aload(0); // load method to get callconfig
                mv.getfield(cg.p(CompiledMethod.class), "callConfig", cg.ci(CallConfiguration.class));
                mv.aload(THREADCONTEXT_INDEX); // tc
                mv.aload(RECEIVER_INDEX); // self
                
                // determine the appropriate class, for super calls to work right
                mv.aload(CLASS_INDEX); // klazz
                mv.aload(0);
                mv.invokevirtual(cg.p(CompiledMethod.class), "getImplementationClass", cg.sig(RubyModule.class));
                mv.invokestatic(cg.p(CompilerHelpers.class), "findImplementerIfNecessary", cg.sig(RubyModule.class, RubyModule.class, RubyModule.class));
                
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
                mv.aload(0); // load method to get callconfig
                mv.getfield(cg.p(DynamicMethod.class), "callConfig", cg.ci(CallConfiguration.class));
                mv.aload(1);
                mv.invokevirtual(cg.p(CallConfiguration.class), "post", cg.sig(void.class, cg.params(ThreadContext.class)));
                // reload and return result
                mv.aload(8);
                mv.visitInsn(ARETURN);

                // return jump handling
                {
                    mv.label(tryReturnJump);
                    
                    // dup return jump, get target, compare to this method object
                    mv.dup();
                    mv.invokevirtual(cg.p(JumpException.FlowControlException.class), "getTarget", cg.sig(Object.class));
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
                    mv.aload(0); // load method to get callconfig
                    mv.getfield(cg.p(DynamicMethod.class), "callConfig", cg.ci(CallConfiguration.class));
                    mv.aload(1);
                    mv.invokevirtual(cg.p(CallConfiguration.class), "post", cg.sig(void.class, cg.params(ThreadContext.class)));

                    // rethrow exception
                    mv.athrow(); // rethrow it
                }
                
                c = endCall(implementationClass.getRuntime(), cw,mv,mname);
            }
            
            return (DynamicMethod)c.getConstructor(new Class[]{RubyModule.class, Arity.class, Visibility.class, StaticScope.class, Object.class}).newInstance(new Object[]{implementationClass,arity,visibility,scope,scriptObject});
        } catch(Exception e) {
            e.printStackTrace();
            throw implementationClass.getRuntime().newLoadError(e.getMessage());
        }
    }

    public DynamicMethod getCompiledMethod(RubyModule implementationClass, String method, Arity arity, Visibility visibility, StaticScope scope, Object scriptObject) {
        return getCompleteMethod(implementationClass,method,arity,visibility,scope, COMPILED_SUPER_CLASS, scriptObject);
    }
}
