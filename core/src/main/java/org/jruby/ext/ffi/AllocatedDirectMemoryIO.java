
package org.jruby.ext.ffi;

abstract public interface AllocatedDirectMemoryIO {
    public void free();
    public void setAutoRelease(boolean autorelease);
    public boolean isAutoRelease();
}
