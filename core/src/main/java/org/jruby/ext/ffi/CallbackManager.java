
package org.jruby.ext.ffi;

import org.jruby.Ruby;

public abstract class CallbackManager {
    public abstract Pointer getCallback(Ruby runtime, CallbackInfo cbInfo, Object proc);
}
