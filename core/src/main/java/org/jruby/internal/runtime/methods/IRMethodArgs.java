package org.jruby.internal.runtime.methods;

import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Signature;

/**
 * Represents a method object that can return a Signature and an array of ArgumentDescriptors.
 */
public interface IRMethodArgs {
    // FIXME: Should get pushed to DynamicMethod

    /**
     * Get the Signature for this method.
     * @return this methods signature
     */
    Signature getSignature();

    /**
     * Get the array of ArgumentDescriptors that represent the arguments to this method.
     * @return this methods argument descriptors
     */
    ArgumentDescriptor[] getArgumentDescriptors();

    void setRuby2Keywords();
}
