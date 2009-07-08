package org.jruby.ext.ffi;

import org.jruby.Ruby;

public final class FreedMemoryIO extends InvalidMemoryIO implements AllocatedDirectMemoryIO {

    public FreedMemoryIO(Ruby runtime) {
        super(runtime, "Attempting to access freed memory");
    }

    public boolean isNull() {
        return false;
    }

    public boolean isDirect() {
        return true;
    }

    public void free() {
        throw ex();
    }

    public void setAutoRelease(boolean autorelease) {
        throw ex();
    }

    public long getAddress() {
        throw ex();
    }
}
