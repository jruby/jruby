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
 * Copyright (c) 2007 Peter Brant <peter.brant@gmail.com>
 * Copyright (C) 2008 The JRuby Community <www.headius.com>
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.List;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.CompiledBlockCallback19;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This MethodFactory uses reflection to provide method handles. Reflection is
 * typically slower than code-generated handles, but it does provide a simple
 * mechanism for binding in environments where code-generation isn't supported.
 * 
 * @see org.jruby.internal.runtime.methods.MethodFactory
 */
public class ReflectionMethodFactory extends MethodFactory {
    /**
     * Use reflection to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethodLazily(RubyModule implementationClass,
            String methodName, Arity arity, Visibility visibility, 
            StaticScope scope, Object scriptObject, CallConfiguration callConfig) {
        return getCompiledMethod(implementationClass, methodName, arity, visibility, scope, scriptObject, callConfig);
    }
    
    /**
     * Use reflection to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethod(RubyModule implementationClass,
            String methodName, Arity arity, Visibility visibility, 
            StaticScope scope, Object scriptObject, CallConfiguration callConfig) {
        try {
            Class scriptClass = scriptObject.getClass();
            Method method = scriptClass.getMethod(methodName, scriptClass, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
            return new ReflectedCompiledMethod(implementationClass, arity, visibility, scope, scriptObject, method, callConfig);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException("No method with name " + methodName + " found in " + scriptObject.getClass());
        }
    }
    
    /**
     * Use reflection to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, JavaMethodDescriptor desc) {
        try {
            if (!Modifier.isPublic(desc.getDeclaringClass().getModifiers())) {
                System.err.println("warning: binding non-public class" + desc.declaringClassName + "; reflected handles won't work");
            }

            Method method = desc.getDeclaringClass().getDeclaredMethod(desc.name, desc.getParameterClasses());
            JavaMethod ic = new ReflectedJavaMethod(implementationClass, method, desc.anno);

            ic.setJavaName(method.getName());
            ic.setSingleton(Modifier.isStatic(method.getModifiers()));
            ic.setCallConfig(CallConfiguration.getCallConfigByAnno(desc.anno));
            return ic;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Use reflection to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.internal.runtime.methods.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, List<JavaMethodDescriptor> descs) {
        try {
            if (!Modifier.isPublic(descs.get(0).getDeclaringClass().getModifiers())) {
                System.err.println("warning: binding non-public class" + descs.get(0).declaringClassName + "; reflected handles won't work");
            }
            
            List<Method> methods = new ArrayList();
            List<JRubyMethod> annotations = new ArrayList();
            
            for (JavaMethodDescriptor desc: descs) {
                methods.add(desc.getDeclaringClass().getDeclaredMethod(desc.name, desc.getParameterClasses()));
                annotations.add(desc.anno);
            }
            Method method0 = methods.get(0);
            JRubyMethod anno0 = annotations.get(0);
            
            JavaMethod ic = new ReflectedJavaMultiMethod(implementationClass, methods, annotations);

            ic.setJavaName(method0.getName());
            ic.setSingleton(Modifier.isStatic(method0.getModifiers()));
            ic.setCallConfig(CallConfiguration.getCallConfigByAnno(anno0));
            return ic;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompiledBlockCallback getBlockCallback(String method, final Object scriptObject) {
        try {
            Class scriptClass = scriptObject.getClass();
            final Method blockMethod = scriptClass.getMethod(method, scriptClass, ThreadContext.class, IRubyObject.class, IRubyObject.class);
            return new CompiledBlockCallback() {
                public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject args) {
                    try {
                        return (IRubyObject)blockMethod.invoke(null, scriptObject, context, self, args);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (InvocationTargetException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else if (cause instanceof Error) {
                            throw (Error) cause;
                        } else {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            };
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    public CompiledBlockCallback19 getBlockCallback19(String method, final Object scriptObject) {
        try {
            Class scriptClass = scriptObject.getClass();
            final Method blockMethod = scriptClass.getMethod(method, scriptClass, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
            return new CompiledBlockCallback19() {
                public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                    try {
                        return (IRubyObject)blockMethod.invoke(null, scriptObject, context, self, args, block);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (InvocationTargetException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else if (cause instanceof Error) {
                            throw (Error) cause;
                        } else {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            };
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }
}
