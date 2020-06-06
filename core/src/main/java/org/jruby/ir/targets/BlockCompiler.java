package org.jruby.ir.targets;

import org.jruby.ir.IRClosure;
import org.objectweb.asm.Handle;

public interface BlockCompiler {
    /**
     * Prepare a block for a subsequent call.
     * <p>
     * Stack required: context, self, dynamicScope
     */
    public abstract void prepareBlock(IRClosure closure, String parentScopeField, Handle handle, String file, int line, String encodedArgumentDescriptors,
                                      org.jruby.runtime.Signature signature);
}
