
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

public abstract class CallbackManager {
    @Deprecated(since = "10.0")
    public Pointer getCallback(Ruby runtime, CallbackInfo cbInfo, Object proc) {
        return getCallback(runtime.getCurrentContext(), cbInfo, proc);
    }

    public abstract Pointer getCallback(ThreadContext context, CallbackInfo cbInfo, Object proc);
}
