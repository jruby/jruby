package org.jruby.ext.ffi;

import org.jruby.Ruby;

public final class FreedMemoryIO extends InvalidMemoryIO implements AllocatedDirectMemoryIO {

    public FreedMemoryIO(Ruby runtime) {
        super(runtime, false, 0xfee1deadcafebabeL, "attempting to access freed memory");
    }

    public void free() {
        throw ex();
    }

    public void setAutoRelease(boolean autorelease) {
        throw ex();
    }

    public boolean isAutoRelease() {
        throw ex();
    }
}
