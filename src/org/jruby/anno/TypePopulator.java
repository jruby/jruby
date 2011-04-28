/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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

/**
 *
 * @author headius
 */
public abstract class TypePopulator {
    public static void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, CallConfiguration callConfig, boolean notImplemented) {
        javaMethod.setIsBuiltin(true);
        javaMethod.setArity(Arity.createArity(arity));
        javaMethod.setJavaName(simpleName);
        javaMethod.setSingleton(isStatic);
        javaMethod.setCallConfig(callConfig);
        javaMethod.setNotImplemented(notImplemented);
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
                        // TODO: separate scope-aware and frame-aware
                        ASTInspector.addScopeAwareMethods(anno.name());
                    }
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName + "." + desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName + "." + desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_8().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName + "." + desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_8().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName + "." + desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_9().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName + "." + desc.name, entry.getKey());
                }
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_9().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
                for (JavaMethodDescriptor desc : entry.getValue()) {
                    if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName + "." + desc.name, entry.getKey());
                }
            }
        }
    }
}
