/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008-2012 Charles Oliver Nutter <headius@headius.com>
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
package org.jruby.anno;

import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ASTInspector;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.Visibility;

public abstract class TypePopulator {
    public static void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, CallConfiguration callConfig, boolean notImplemented) {
        javaMethod.setIsBuiltin(true);
        javaMethod.setArity(Arity.createArity(arity));
        javaMethod.setJavaName(simpleName);
        javaMethod.setSingleton(isStatic);
        javaMethod.setCallConfig(callConfig);
        javaMethod.setNotImplemented(notImplemented);
    }
    
    public static void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, CallConfiguration callConfig, boolean notImplemented,
            Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeArguments) {
        javaMethod.setIsBuiltin(true);
        javaMethod.setArity(Arity.createArity(arity));
        javaMethod.setJavaName(simpleName);
        javaMethod.setSingleton(isStatic);
        javaMethod.setCallConfig(callConfig);
        javaMethod.setNotImplemented(notImplemented);
        javaMethod.setNativeCall(nativeTarget, nativeName, nativeReturn, nativeArguments, isStatic, false);
    }
    
    public static DynamicMethod populateModuleMethod(RubyModule cls, JavaMethod javaMethod) {
        DynamicMethod moduleMethod = javaMethod.dup();
        moduleMethod.setImplementationClass(cls.getSingletonClass());
        moduleMethod.setVisibility(Visibility.PUBLIC);
        return moduleMethod;
    }
    
    public abstract void populate(RubyModule clsmod, Class clazz);
    
    public static final TypePopulator DEFAULT = new DefaultTypePopulator();
    public static class DefaultTypePopulator extends TypePopulator {
        public void populate(RubyModule clsmod, Class clazz) {
            // fallback on non-pregenerated logic
            MethodFactory methodFactory = MethodFactory.createFactory(clsmod.getRuntime().getJRubyClassLoader());
            Ruby runtime = clsmod.getRuntime();
            
            RubyModule.MethodClumper clumper = new RubyModule.MethodClumper();
            clumper.clump(clazz);

            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAllAnnotatedMethods().entrySet()) {
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    JRubyMethod anno = desc.anno;
                    
                    // check for frame field reads or writes
                    if (anno.frame() || (anno.reads() != null && anno.reads().length >= 1) || (anno.writes() != null && anno.writes().length >= 1)) {
                        // add all names for this annotation
                        ASTInspector.addFrameAwareMethods(anno.name());
                    }
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_8().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_8().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_9().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_9().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }

            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods2_0().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }

            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods2_0().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, entry.getKey());
                }
            }
        }
    }
}
