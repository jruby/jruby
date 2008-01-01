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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class ReflectionMethodFactory extends MethodFactory {
    private final static Class[] COMPILED_METHOD_PARAMS = new Class[] {ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class};
    
    public DynamicMethod getCompiledMethod(RubyModule implementationClass,
            String methodName, Arity arity, Visibility visibility, 
            StaticScope scope, Object scriptObject, CallConfiguration callConfig) {
        try {
            Method method = scriptObject.getClass().getMethod(methodName, COMPILED_METHOD_PARAMS);
            return new ReflectedCompiledMethod(implementationClass, arity, visibility, scope, scriptObject, method, callConfig);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException("No method with name " + methodName + " found in " + scriptObject.getClass());
        }
    }
    
    public DynamicMethod getAnnotatedMethod(RubyModule implementationClass, Method method) {
        JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);
        JavaMethod ic = new ReflectedJavaMethod(implementationClass, method, jrubyMethod);

        boolean fast = !(jrubyMethod.frame() || jrubyMethod.scope());
        ic.setArity(Arity.fromAnnotation(jrubyMethod));
        ic.setJavaName(method.getName());
        ic.setArgumentTypes(method.getParameterTypes());
        ic.setSingleton(Modifier.isStatic(method.getModifiers()));
        if (fast) {
            ic.setCallConfig(CallConfiguration.NO_FRAME_NO_SCOPE);
        } else {
            ic.setCallConfig(CallConfiguration.FRAME_ONLY);
        }
        return ic;
    }

    @Override
    public void defineIndexedAnnotatedMethods(RubyModule implementationClass, Class type, MethodDefiningCallback callback) {
        Method[] methods = type.getDeclaredMethods();
        for (Method method : methods) {
            JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);

            if (jrubyMethod == null) continue;
            
            callback.define(implementationClass, method, getAnnotatedMethod(implementationClass, method));
        }
    }

}
