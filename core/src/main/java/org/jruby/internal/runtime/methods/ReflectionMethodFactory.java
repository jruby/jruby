/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.anno.TypePopulator;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * This MethodFactory uses reflection to provide method handles. Reflection is
 * typically slower than code-generated handles, but it does provide a simple
 * mechanism for binding in environments where code-generation isn't supported.
 * 
 * @see org.jruby.runtime.MethodFactory
 */
public class ReflectionMethodFactory extends MethodFactory {
    private static final Logger LOG = LoggerFactory.getLogger("ReflectionMethodFactory");

    /**
     * Use reflection to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.runtime.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethodLazily(RubyModule implementationClass,
            String rubyName, String javaName, Visibility visibility,
            StaticScope scope, Object scriptObject, CallConfiguration callConfig,
            ISourcePosition position, String parameterDesc,
            MethodNodes methodNodes) {

        return getCompiledMethod(
                implementationClass,
                rubyName,
                javaName,
                visibility,
                scope,
                scriptObject,
                callConfig,
                position,
                parameterDesc,
                methodNodes);
    }
    
    /**
     * Use reflection to provide a method handle for a compiled Ruby method.
     * 
     * @see org.jruby.runtime.MethodFactory#getCompiledMethod
     */
    public DynamicMethod getCompiledMethod(RubyModule implementationClass,
            String rubyName, String javaName, Visibility visibility,
            StaticScope scope, Object scriptObject, CallConfiguration callConfig,
            ISourcePosition position, String parameterDesc,
            MethodNodes methodNodes) {
        try {
            Class scriptClass = scriptObject.getClass();
            Method method = scriptClass.getMethod(javaName, scriptClass, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
            return new ReflectedCompiledMethod(
                    implementationClass,
                    visibility,
                    scope,
                    scriptObject,
                    method,
                    callConfig,
                    position,
                    parameterDesc);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException("No method with name " + javaName + " found in " + scriptObject.getClass());
        }
    }
    
    /**
     * Use reflection to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.runtime.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, JavaMethodDescriptor desc) {
        try {
            if (!Modifier.isPublic(desc.getDeclaringClass().getModifiers())) {
                LOG.warn("warning: binding non-public class {}; reflected handles won't work", desc.declaringClassName);
            }

            Method method = desc.getDeclaringClass().getDeclaredMethod(desc.name, desc.getParameterClasses());
            JavaMethod ic = new ReflectedJavaMethod(implementationClass, method, desc.anno);

            TypePopulator.populateMethod(
                    ic,
                    ic.getArity().getValue(),
                    method.getName(),
                    Modifier.isStatic(method.getModifiers()),
                    CallConfiguration.getCallConfigByAnno(desc.anno),
                    desc.anno.notImplemented());
                
            return ic;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Use reflection to provide a method handle based on an annotated Java
     * method.
     * 
     * @see org.jruby.runtime.MethodFactory#getAnnotatedMethod
     */
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, List<JavaMethodDescriptor> descs) {
        try {
            if (!Modifier.isPublic(descs.get(0).getDeclaringClass().getModifiers())) {
                LOG.warn("warning: binding non-public class {}; reflected handles won't work", descs.get(0).declaringClassName);
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

            TypePopulator.populateMethod(
                    ic,
                    ic.getArity().getValue(),
                    method0.getName(),
                    Modifier.isStatic(method0.getModifiers()),
                    CallConfiguration.getCallConfigByAnno(anno0),
                    anno0.notImplemented());
            return ic;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
