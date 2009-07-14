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
 * Copyright (C) 2008 The JRuby Community <www.jruby.org>
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
package org.jruby.runtime;

import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.ReflectionMethodFactory;
import org.jruby.internal.runtime.methods.InvocationMethodFactory;
import org.jruby.internal.runtime.methods.DumpingInvocationMethodFactory;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.SafePropertyAccessor;

/**
 * MethodFactory is used to generate "invokers" or "method handles" given a target
 * class, method name, and other characteristics. In order to bind methods into
 * Ruby's reified class hierarchy, we need a way to treat individual methods as
 * objects. Implementers of this class provide that functionality.
 */
public abstract class MethodFactory {
    /**
     * A Class[] representing the signature of compiled Ruby method.
     */
    public final static Class[] COMPILED_METHOD_PARAMS = new Class[] {ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class};
    
    /**
     * For batched method construction, the logic necessary to bind resulting
     * method objects into a target module/class must be provided as a callback.
     * This interface should be implemented by code calling any batched methods
     * on this MethodFactory.
     */
    public interface MethodDefiningCallback {
        public void define(RubyModule targetMetaClass, JavaMethodDescriptor desc, DynamicMethod dynamicMethod);
    }

    /**
     * Based on optional properties, create a new MethodFactory. By default,
     * this will create a code-generation-based InvocationMethodFactory. If
     * security restricts code generation, ReflectionMethodFactory will be used.
     * If we are dumping class definitions, DumpingInvocationMethodFactory will
     * be used. See MethodFactory's static initializer for more details.
     * 
     * @param classLoader The classloader to use for searching for and
     * dynamically loading code.
     * @return A new MethodFactory.
     */
    public static MethodFactory createFactory(ClassLoader classLoader) {
        if (reflection) return new ReflectionMethodFactory();
        if (dumping) return new DumpingInvocationMethodFactory(dumpingPath, classLoader);

        return new InvocationMethodFactory(classLoader);
    }
    
    /**
     * Get a new method handle based on the target JRuby-compiled method.
     * Because compiled Ruby methods have additional requirements and
     * characteristics not typically found in Java-based methods, this is
     * provided as a separate way to define such method handles.
     * 
     * @param implementationClass The class to which the method will be bound.
     * @param method The name of the method
     * @param arity The Arity of the method
     * @param visibility The method's visibility on the target type.
     * @param scope The methods static scoping information.
     * @param scriptObject An instace of the target compiled method class.
     * @param callConfig The call configuration to use for this method.
     * @return A new method handle for the target compiled method.
     */
    public abstract DynamicMethod getCompiledMethod(
            RubyModule implementationClass, String method, 
            Arity arity, Visibility visibility, StaticScope scope, 
            Object scriptObject, CallConfiguration callConfig);
    
    /**
     * Like getCompiledMethod, but postpones any heavy lifting involved in
     * creating the method until first invocation. This helps reduce the cost
     * of starting up AOT-compiled code, by spreading out the heavy lifting
     * across the run rather than causing all method handles to be immediately
     * instantiated.
     * 
     * @param implementationClass The class to which the method will be bound.
     * @param method The name of the method
     * @param arity The Arity of the method
     * @param visibility The method's visibility on the target type.
     * @param scope The methods static scoping information.
     * @param scriptObject An instace of the target compiled method class.
     * @param callConfig The call configuration to use for this method.
     * @return A new method handle for the target compiled method.
     */
    public abstract DynamicMethod getCompiledMethodLazily(
            RubyModule implementationClass, String method, 
            Arity arity, Visibility visibility, StaticScope scope, 
            Object scriptObject, CallConfiguration callConfig);
    
    /**
     * Based on a list of annotated Java methods, generate a method handle using
     * the annotation and the target signatures. The annotation and signatures
     * will be used to dynamically generate the appropriate call logic for the
     * handle. This differs from the single-method version in that it will dispatch
     * multiple specific-arity paths to different target methods.
     * 
     * @param implementationClass The target class or module on which the method
     * will be bound.
     * @param descs A list of JavaMethodDescriptors describing the target methods
     * @return A method handle for the target object.
     */
    public abstract DynamicMethod getAnnotatedMethod(RubyModule implementationClass, List<JavaMethodDescriptor> desc);
    
    /**
     * Based on an annotated Java method object, generate a method handle using
     * the annotation and the target signature. The annotation and signature
     * will be used to dynamically generate the appropriate call logic for the
     * handle.
     * 
     * @param implementationClass The target class or module on which the method
     * will be bound.
     * @param desc A JavaMethodDescriptor describing the target method
     * @return A method handle for the target object.
     */
    public abstract DynamicMethod getAnnotatedMethod(RubyModule implementationClass, JavaMethodDescriptor desc);
    
    /**
     * Get a CompiledBlockCallback for the specified block
     *
     * @param method The name of the method
     * @param scriptObject The object in which the method can be found
     * @return A new CompiledBlockCallback for the method
     */
    public abstract CompiledBlockCallback getBlockCallback(String method, Object scriptObject);

    /**
     * Get a CompiledBlockCallback for the specified block
     *
     * @param method The name of the method
     * @param scriptObject The object in which the method can be found
     * @return A new CompiledBlockCallback for the method
     */
    public abstract CompiledBlockCallback19 getBlockCallback19(String method, Object scriptObject);

    /**
     * Use the reflection-based factory.
     */
    private static final boolean reflection;
    /**
     * User the dumping-based factory, which generates .class files as it runs.
     */
    private static final boolean dumping;
    /**
     * The target path for the dumping factory to save the .class files.
     */
    private static final String dumpingPath;
    
    static {
        boolean reflection_ = false, dumping_ = false;
        String dumpingPath_ = null;
        // initialize the static settings to determine which factory to use
        if (Ruby.isSecurityRestricted()) {
            reflection_ = true;
        } else {
            if (SafePropertyAccessor.getProperty("jruby.reflection") != null && SafePropertyAccessor.getBoolean("jruby.reflection")) {
                reflection_ = true;
            }
            if (SafePropertyAccessor.getProperty("jruby.dump_invocations") != null) {
                dumping_ = true;
                dumpingPath_ = SafePropertyAccessor.getProperty("jruby.dump_invocations");
            }
        }
        reflection = reflection_;
        dumping = dumping_;
        dumpingPath = dumpingPath_;
    }
}
