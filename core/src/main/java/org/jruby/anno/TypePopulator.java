/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.Visibility;

public abstract class TypePopulator {
    public static void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, boolean notImplemented) {
        javaMethod.setIsBuiltin(true);
        javaMethod.setArity(Arity.createArity(arity));
        javaMethod.setJavaName(simpleName);
        javaMethod.setSingleton(isStatic);
        javaMethod.setNotImplemented(notImplemented);
    }
    
    public static void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, boolean notImplemented,
            Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeArguments) {
        javaMethod.setIsBuiltin(true);
        javaMethod.setArity(Arity.createArity(arity));
        javaMethod.setJavaName(simpleName);
        javaMethod.setSingleton(isStatic);
        javaMethod.setNotImplemented(notImplemented);
        javaMethod.setNativeCall(nativeTarget, nativeName, nativeReturn, nativeArguments, isStatic, false);
    }
    
    public static DynamicMethod populateModuleMethod(RubyModule cls, DynamicMethod javaMethod) {
        DynamicMethod moduleMethod = javaMethod.dup();
        moduleMethod.setImplementationClass(cls.getSingletonClass());
        moduleMethod.setVisibility(Visibility.PUBLIC);
        return moduleMethod;
    }
    
    public abstract void populate(RubyModule clsmod, Class clazz);
    
    public static final TypePopulator DEFAULT = new DefaultTypePopulator();
    public static class DefaultTypePopulator extends TypePopulator {
        public void populate(RubyModule clsmod, Class clazz) {
            ReflectiveTypePopulator populator = new ReflectiveTypePopulator(clazz);
            populator.populate(clsmod, clazz);
        }
    }

    public static final class ReflectiveTypePopulator extends TypePopulator {
        private final Class clazz;
        private final RubyModule.MethodClumper clumper;

        public ReflectiveTypePopulator(Class clazz) {
            this.clazz = clazz;
            this.clumper = new RubyModule.MethodClumper();
            clumper.clump(clazz);
        }

        public void populate(final RubyModule target, final Class clazz) {
            assert clazz == this.clazz : "populator for " + this.clazz + " used for " + clazz;

            // fallback on non-pregenerated logic
            final Ruby runtime = target.getRuntime();
            final MethodFactory methodFactory = MethodFactory.createFactory(runtime.getJRubyClassLoader());

            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods().entrySet()) {
                final String name = entry.getKey();
                final List<JavaMethodDescriptor> methods = entry.getValue();
                target.defineAnnotatedMethod(name, methods, methodFactory);
                addBoundMethodsUnlessOmited(runtime, name, methods);
            }

            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods().entrySet()) {
                final String name = entry.getKey();
                final List<JavaMethodDescriptor> methods = entry.getValue();
                target.defineAnnotatedMethod(name, methods, methodFactory);
                addBoundMethodsUnlessOmited(runtime, name, methods);
            }
        }

        private static void addBoundMethodsUnlessOmited(final Ruby runtime, final String name, final List<JavaMethodDescriptor> methods) {
            final int size = methods.size();
            if ( size == 1 ) {
                final JavaMethodDescriptor desc = methods.get(0);
                if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, name);
                return;
            }
            for ( int i=0; i<size; i++ ) {
                final JavaMethodDescriptor desc = methods.get(i);
                if (!desc.anno.omit()) runtime.addBoundMethod(desc.declaringClassName, desc.name, name);
            }
        }
    }
}
