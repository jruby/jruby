package org.jruby.internal.runtime.methods;

/**
 * Get a list of argument descriptors in prefixed form.
 * 
 * The prefixed form has a single leading character to indicate the type of argument
 * followed immediately by the name of the argument.
 * 
 * @see IRMethodArgs
 * @see org.jruby.runtime.ArgumentType
 */
public interface MethodArgs2 {
    public String[] getParameterList();
}
