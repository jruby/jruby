/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.anno;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jruby.util.CodegenUtils;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
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
    private final Class[] parameters;
    private final Class returnClass;
    public final JRubyMethod anno;
    public final int modifiers;
    private final Class declaringClass;
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
            if (hasBlock) {
                // args should be before block
                hasVarArgs = parameters[parameters.length - 2] == IRubyObject[].class;
            } else {
                // args should be at end
                hasVarArgs = parameters[parameters.length - 1] == IRubyObject[].class;
            }
        } else {
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
