/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.anno;

import java.util.List;
import java.util.Map;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
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
    public void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, CallConfiguration callConfig) {
        javaMethod.setArity(Arity.createArity(arity));
        javaMethod.setJavaName(simpleName);
        javaMethod.setSingleton(isStatic);
        javaMethod.setCallConfig(callConfig);
    }
    
    public DynamicMethod populateModuleMethod(RubyModule cls, JavaMethod javaMethod) {
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
            
            RubyModule.MethodClumper clumper = new RubyModule.MethodClumper();
            clumper.clump(clazz);
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_8().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_8().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods1_9().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
            }
            
            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods1_9().entrySet()) {
                clsmod.defineAnnotatedMethod(entry.getKey(), entry.getValue(), methodFactory);
            }
        }
    }
}
