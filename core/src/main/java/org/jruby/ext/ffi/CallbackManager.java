
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

public abstract class CallbackManager {
    /**
     * @param runtime
     * @param cbInfo
     * @param proc
     * @return ""
     * @deprecated Use {@link CallbackManager#getCallback(ThreadContext, CallbackInfo, Object)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public Pointer getCallback(Ruby runtime, CallbackInfo cbInfo, Object proc) {
        return getCallback(runtime.getCurrentContext(), cbInfo, proc);
    }

    public abstract Pointer getCallback(ThreadContext context, CallbackInfo cbInfo, Object proc);
}
