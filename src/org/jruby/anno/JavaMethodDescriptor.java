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

/**
 *
 * @author headius
 */
public class JavaMethodDescriptor {
    public final boolean isStatic;
    public final boolean hasContext;
    public final boolean hasBlock;
    public final int actualRequired;
    public final int arity;
    public final int required;
    public final int optional;
    public final boolean rest;
    private final Class[] parameters;
    public final JRubyMethod anno;
    public final int modifiers;
    private final Class declaringClass;
    public final String declaringClassName;
    public final String declaringClassPath;
    public final String name;
    public final String signature;
    
    public JavaMethodDescriptor(Method method) {
        anno = method.getAnnotation(JRubyMethod.class);
        
        modifiers = method.getModifiers();
        declaringClass = method.getDeclaringClass();
        declaringClassName = declaringClass.getName();
        declaringClassPath = CodegenUtils.p(declaringClass);
        name = method.getName();
        isStatic = Modifier.isStatic(modifiers);
        parameters = method.getParameterTypes();
        if (parameters.length > 0) {
            hasContext = parameters[0] == ThreadContext.class;
            hasBlock = parameters[parameters.length - 1] == Block.class;
        } else {
            hasContext = false;
            hasBlock = false;
        }
        
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
}
