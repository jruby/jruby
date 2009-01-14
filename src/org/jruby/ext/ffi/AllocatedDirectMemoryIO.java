
package org.jruby.ext.ffi;

public interface AllocatedDirectMemoryIO extends DirectMemoryIO {
    public void free();
    public void setAutoRelease(boolean autorelease);
}
