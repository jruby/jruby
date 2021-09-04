/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class TypePopulator {

    public static void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, boolean notImplemented) {
        javaMethod.setIsBuiltin(true);
        javaMethod.setSignature(Signature.fromArityValue(arity));
        javaMethod.setJavaName(simpleName);
        javaMethod.setSingleton(isStatic);
        javaMethod.setNotImplemented(notImplemented);
    }
    
    public static void populateMethod(JavaMethod javaMethod, int arity, String simpleName, boolean isStatic, boolean notImplemented,
            Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeArguments) {
        javaMethod.setIsBuiltin(true);
        javaMethod.setSignature(Signature.fromArityValue(arity));
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

        final List<String> classAndSubs = new ArrayList<>();

        public ReflectiveTypePopulator(Class clazz) {
            this.clazz = clazz;
            this.clumper = new RubyModule.MethodClumper();
            clumper.clump(clazz);

            classAndSubs.add(clazz.getCanonicalName());

            Optional.ofNullable((JRubyClass) clazz.getAnnotation(JRubyClass.class))
                    .ifPresent(classAnno -> AnnotationHelper.addSubclassNames(classAndSubs, classAnno));
        }

        public void populate(final RubyModule target, final Class clazz) {
            assert clazz == this.clazz : "populator for " + this.clazz + " used for " + clazz;

            // fallback on non-pregenerated logic

            // populate method index; this is done statically in generated code
            AnnotationHelper.populateMethodIndex(clumper.readGroups, MethodIndex::addMethodReadFieldsPacked);
            AnnotationHelper.populateMethodIndex(clumper.writeGroups, MethodIndex::addMethodWriteFieldsPacked);

            final Ruby runtime = target.getRuntime();
            final MethodFactory methodFactory = MethodFactory.createFactory(runtime.getJRubyClassLoader());

            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getStaticAnnotatedMethods().entrySet()) {
                final String name = entry.getKey();
                final List<JavaMethodDescriptor> methods = entry.getValue();
                target.defineAnnotatedMethod(name, methods, methodFactory);
                addBoundMethodsUnlessOmitted(runtime, name, methods);
            }

            for (Map.Entry<String, List<JavaMethodDescriptor>> entry : clumper.getAnnotatedMethods().entrySet()) {
                final String name = entry.getKey();
                final List<JavaMethodDescriptor> methods = entry.getValue();
                target.defineAnnotatedMethod(name, methods, methodFactory);
                addBoundMethodsUnlessOmitted(runtime, name, methods);
            }
        }

        private void addBoundMethodsUnlessOmitted(final Ruby runtime, final String name, final List<JavaMethodDescriptor> methods) {
            final int size = methods.size();
            final List<String> classAndSubs = this.classAndSubs;

            for ( int i=0; i<size; i++ ) {
                final JavaMethodDescriptor desc = methods.get(i);
                if (!desc.anno.omit()) {
                    String javaName = desc.name;
                    for (int j = 0; j < classAndSubs.size(); j++) {
                        runtime.addBoundMethod(classAndSubs.get(j), javaName, name);
                    }
                }
            }
        }
    }

    // used by generated code (populators) - @see AnnorationBinder

    protected static final Class[] ARG0 = new Class[] {};
    protected static final Class[] ARG1 = new Class[] { IRubyObject.class };
    protected static final Class[] ARG2 = new Class[] { IRubyObject.class, IRubyObject.class };
    protected static final Class[] ARG3 = new Class[] { IRubyObject.class, IRubyObject.class, IRubyObject.class };
    protected static final Class[] ARG4 = new Class[] { IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class };

    protected static final Class[] ARG0_ARY = new Class[] { IRubyObject[].class };
    protected static final Class[] ARG1_ARY = new Class[] { IRubyObject.class, IRubyObject[].class };
    //protected static final Class[] ARG2_ARY = new Class[] { IRubyObject.class, IRubyObject.class, IRubyObject[].class };

    protected static final Class[] CONTEXT_ARG0 = new Class[] { ThreadContext.class };
    protected static final Class[] CONTEXT_ARG1 = new Class[] { ThreadContext.class, IRubyObject.class };
    protected static final Class[] CONTEXT_ARG2 = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class };
    protected static final Class[] CONTEXT_ARG3 = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class };
    protected static final Class[] CONTEXT_ARG4 = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class };

    protected static final Class[] CONTEXT_ARG0_ARY = new Class[] { ThreadContext.class, IRubyObject[].class };
    protected static final Class[] CONTEXT_ARG1_ARY = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject[].class };
    //protected static final Class[] CONTEXT_ARG2_ARY = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class };

    protected static final Class[] ARG0_BLOCK = new Class[] { Block.class };
    protected static final Class[] ARG1_BLOCK = new Class[] { IRubyObject.class, Block.class };
    protected static final Class[] ARG2_BLOCK = new Class[] { IRubyObject.class, IRubyObject.class, Block.class };
    protected static final Class[] ARG3_BLOCK = new Class[] { IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class };
    protected static final Class[] ARG4_BLOCK = new Class[] { IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class };

    protected static final Class[] ARG0_ARY_BLOCK = new Class[] { IRubyObject[].class, Block.class };
    protected static final Class[] ARG1_ARY_BLOCK = new Class[] { IRubyObject.class, IRubyObject[].class, Block.class };
    //protected static final Class[] ARG2_ARY_BLOCK = new Class[] { IRubyObject.class, IRubyObject.class, IRubyObject[].class, Block.class };

    protected static final Class[] CONTEXT_ARG0_BLOCK = new Class[] { ThreadContext.class, Block.class };
    protected static final Class[] CONTEXT_ARG1_BLOCK = new Class[] { ThreadContext.class, IRubyObject.class, Block.class };
    protected static final Class[] CONTEXT_ARG2_BLOCK = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class };
    protected static final Class[] CONTEXT_ARG3_BLOCK = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class };
    protected static final Class[] CONTEXT_ARG4_BLOCK = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class };

    protected static final Class[] CONTEXT_ARG0_ARY_BLOCK = new Class[] { ThreadContext.class, IRubyObject[].class, Block.class };
    protected static final Class[] CONTEXT_ARG1_ARY_BLOCK = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class };
    //protected static final Class[] CONTEXT_ARG2_ARY_BLOCK = new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject[].class, Block.class };

}
