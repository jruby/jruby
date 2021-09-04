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
     */
    public Signature getSignature();

    /**
     * Get the array of ArgumentDescriptors that represent the arguments to this method.
     */
    public ArgumentDescriptor[] getArgumentDescriptors();

}
