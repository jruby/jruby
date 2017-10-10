/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008 The JRuby Community <www.jruby.org>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InvocationMethodFactory;
import org.jruby.internal.runtime.methods.InvokeDynamicMethodFactory;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.util.List;

/**
 * MethodFactory is used to generate "invokers" or "method handles" given a target
 * class, method name, and other characteristics. In order to bind methods into
 * Ruby's reified class hierarchy, we need a way to treat individual methods as
 * objects. Implementers of this class provide that functionality.
 */
public abstract class MethodFactory {
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
        // otherwise, generate invokers at runtime
        if (Options.INVOKEDYNAMIC_HANDLES.load()) {
            return new InvokeDynamicMethodFactory(classLoader);
        } else {
            return new InvocationMethodFactory(classLoader);
        }
    }

    /**
     * Based on a list of annotated Java methods, generate a method handle using
     * the annotation and the target signatures. The annotation and signatures
     * will be used to dynamically generate the appropriate call logic for the
     * handle. This differs from the single-method version in that it will dispatch
     * multiple specific-arity paths to different target methods.
     *
     * @param implementationClass The target class or module on which the method
     * will be bound.
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
}
