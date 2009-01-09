
package org.jruby.ext.ffi.jffi;

import java.nio.channels.ByteChannel;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.FFIProvider;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.Pointer;

public class Factory extends org.jruby.ext.ffi.Factory {

    public Factory() {
        if (!com.kenai.jffi.Platform.getPlatform().isSupported()) {
            throw new UnsatisfiedLinkError("JFFI backend not available");
        }
    }

    @Override
    public void init(Ruby runtime, RubyModule ffi) {
        super.init(runtime, ffi);
        //
        // Hook up the MemoryPointer class if its not already there
        //
        synchronized (ffi) {
            if (ffi.fastGetClass("BasePointer") == null) {
                BasePointer.createJNAPointerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("MemoryPointer") == null) {
                MemoryPointer.createMemoryPointerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("DynamicLibrary") == null) {
                DynamicLibrary.createDynamicLibraryClass(runtime, ffi);
            }
            if (ffi.fastGetClass("Invoker") == null) {
                JFFIInvoker.createInvokerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("VariadicInvoker") == null) {
                VariadicInvoker.createVariadicInvokerClass(runtime, ffi);
            }
        }
    }
    @Override
    protected FFIProvider newProvider(Ruby runtime) {
        return new JFFIProvider(runtime);
    }

    @Override
    public <T> T loadLibrary(String libraryName, Class<T> libraryClass) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ByteChannel newByteChannel(int fd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MemoryIO allocateHeapMemory(int size, boolean clear) {
        return new ArrayMemoryIO(size);
    }

    @Override
    public Pointer newPointer(Ruby runtime, MemoryIO io) {
        return new BasePointer(runtime, io, 0, Long.MAX_VALUE);
    }

}
