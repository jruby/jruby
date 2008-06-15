/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.anno;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Arity;

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
    
    public abstract void populate(RubyModule clsmod);
}
