/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.ffi.jna;

import com.sun.jna.Native;
import java.nio.channels.ByteChannel;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.FFIProvider;
import org.jruby.ext.ffi.Platform;

/**
 *
 * @author wayne
 */
public class JNAFactory extends org.jruby.ext.ffi.Factory {
    private final JNAPlatform platform = new JNAPlatform();
    
    @Override
    public void init(Ruby runtime, RubyModule module) {
        super.init(runtime, module);
        //
        // Hook up the MemoryPointer class if its not already there
        //
        synchronized (module) {
            if (module.fastGetClass(JNAMemoryPointer.MEMORY_POINTER_NAME) == null) {
                JNAMemoryPointer.createMemoryPointerClass(runtime);
            }
            if (module.fastGetClass(JNABuffer.BUFFER_RUBY_CLASS) == null) {
                JNABuffer.createBufferClass(runtime);
            }
        }
    }
    protected FFIProvider newProvider(Ruby runtime) {
        return new JNAProvider(runtime);
    }
    
    @Override
    public <T> T loadLibrary(String libraryName, Class<T> libraryClass) {
        return libraryClass.cast(Native.loadLibrary(libraryName, libraryClass));
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }
    public ByteChannel newByteChannel(int fd) {
        return new FileDescriptorByteChannel(fd);
    }

    
}
