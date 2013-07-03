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
 * Copyright (C) 2008-2013 Charles Oliver Nutter <headius@headius.com>
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jruby.util.CodegenUtils;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


public class JavaMethodDescriptor {
    public final boolean isStatic;
    public final boolean hasContext;
    public final boolean hasBlock;
    public final boolean hasVarArgs;
    public final int actualRequired;
    public final int arity;
    public final int required;
    public final int optional;
    public final boolean rest;
    public final Class[] parameters;
    public final Class returnClass;
    public final JRubyMethod anno;
    public final int modifiers;
    public final Class declaringClass;
    public final String declaringClassName;
    public final String declaringClassPath;
    public final String name;
    public final String signature;
    public final Class[] argumentTypes;
    
    public JavaMethodDescriptor(Method method) {
        anno = method.getAnnotation(JRubyMethod.class);
        
        modifiers = method.getModifiers();
        declaringClass = method.getDeclaringClass();
        declaringClassName = declaringClass.getName();
        declaringClassPath = CodegenUtils.p(declaringClass);
        name = method.getName();
        isStatic = Modifier.isStatic(modifiers);
        parameters = method.getParameterTypes();
        returnClass = method.getReturnType();
        if (parameters.length > 0) {
            hasContext = parameters[0] == ThreadContext.class;
            hasBlock = parameters[parameters.length - 1] == Block.class;
        } else {
            hasContext = false;
            hasBlock = false;
        }
        
        if (hasContext) {
            if (isStatic && (parameters.length < 2 || parameters[1] != IRubyObject.class)) {
                throw new RuntimeException("static method without self argument: " + method);
            }

            if (hasBlock) {
                // args should be before block
                hasVarArgs = parameters[parameters.length - 2] == IRubyObject[].class;
            } else {
                // args should be at end
                hasVarArgs = parameters[parameters.length - 1] == IRubyObject[].class;
            }
        } else {
            if (isStatic && (parameters.length < 1 || parameters[0] != IRubyObject.class)) {
                throw new RuntimeException("static method without self argument: " + method);
            }

            if (hasBlock) {
                hasVarArgs = parameters.length > 1 && parameters[parameters.length - 2] == IRubyObject[].class;
            } else {
                hasVarArgs = parameters.length > 0 && parameters[parameters.length - 1] == IRubyObject[].class;
            }
        }

        int start = (hasContext ? 1 : 0) + (isStatic ? 1 : 0);
        int end = parameters.length - (hasBlock ? 1 : 0);
        argumentTypes = new Class[end - start];
        System.arraycopy(parameters, start, argumentTypes, 0, end - start);
        
        optional = anno.optional();
        rest = anno.rest();
        required = anno.required();
        
        if (optional == 0 && !rest) {
            int args = parameters.length;
            if (args == 0) {
                actualRequired = 0;
            } else {
                if (isStatic) args--;
                if (hasContext) args--;
                if (hasBlock) args--;

                // TODO: confirm expected args are IRubyObject (or similar)
                actualRequired = args;
            }
        } else {
            // optional args, so we have IRubyObject[]
            // TODO: confirm
            int args = parameters.length;
            if (args == 0) {
                actualRequired = 0;
            } else {
                if (isStatic) args--;
                if (hasContext) args--;
                if (hasBlock) args--;

                // minus one more for IRubyObject[]
                args--;

                // TODO: confirm expected args are IRubyObject (or similar)
                actualRequired = args;
            }
            
            if (actualRequired != 0) {
                throw new RuntimeException("Combining specific args with IRubyObject[] is not yet supported");
            }
        }
        
        int arityRequired = Math.max(required, actualRequired);
        arity = (optional > 0 || rest) ? -(arityRequired + 1) : arityRequired;
        
        signature = CodegenUtils.sig(method.getReturnType(), method.getParameterTypes());
    }
    
    public Class getDeclaringClass() {
        return declaringClass;
    }
    
    public Class[] getParameterClasses() {
        return parameters;
    }

    public Class getReturnClass() {
        return returnClass;
    }
}
