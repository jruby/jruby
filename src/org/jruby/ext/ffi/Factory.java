/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.ffi;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.io.FileDescriptorIO;
import org.jruby.runtime.load.Library;

/**
 *
 * @author wayne
 */
public abstract class Factory {

    private static final class SingletonHolder {

        private static final Factory INSTANCE = getInstance();

        private static final Factory getInstance() {
            final boolean useJNA = Boolean.getBoolean("ffi.usejna");
            String prefix = FFIProvider.class.getPackage().getName();
            Factory factory = null;
            if (!useJNA) {
                try {
                    factory = (Factory) Class.forName(prefix + ".jffi.JFFIFactory").newInstance();
                } catch (Throwable ex) {
                }
            }
            if (factory == null) {
                try {
                    factory = (Factory) Class.forName(prefix + ".jna.JNAFactory").newInstance();
                } catch (Throwable ex) {
                    throw new RuntimeException("Could not load FFI provider", ex);
                }
            }
            return factory;
        }
    }

    protected Factory() {
    }

    public static class Service implements Library {

        public void load(final Ruby runtime, boolean wrap) throws IOException {
            RubyModule ffi = runtime.defineModuleUnder("FFI", runtime.defineModule("JRuby"));
            Factory factory = Factory.getInstance();
            factory.init(runtime, ffi);
        }
    }

    /**
     * Gets an instance of <tt>FFIProvider</tt>
     * 
     * @return an instance of <tt>FFIProvider</tt>
     */
    public static final Factory getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    /**
     * Registers FFI ruby classes/modules
     * 
     * @param module the module to register the classes under
     */
    public void init(Ruby runtime, RubyModule module) {
        synchronized (module) {
            if (module.fastGetClass(FFIProvider.CLASS_NAME) == null) {
                FFIProvider.createProviderClass(runtime);
            }
            if (module.fastGetClass(Invoker.CLASS_NAME) == null) {
                Invoker.createInvokerClass(runtime);
            }
            if (module.fastGetClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS) == null) {
                AbstractMemory.createAbstractMemoryClass(runtime);
            }
            if (module.fastGetClass(AbstractMemoryPointer.className) == null) {
                AbstractMemoryPointer.createMemoryPointerClass(runtime);
            }
            if (module.fastGetClass(AbstractBuffer.ABSTRACT_BUFFER_RUBY_CLASS) == null) {
                AbstractBuffer.createBufferClass(runtime);
            }
            if (module.fastGetClass(StructLayout.CLASS_NAME) == null) {
                StructLayout.createStructLayoutClass(runtime);
            }
            if (module.fastGetClass(StructLayoutBuilder.CLASS_NAME) == null) {
                StructLayoutBuilder.createStructLayoutBuilderClass(runtime);
            }
            if (module.fastGetClass(FileDescriptorIO.CLASS_NAME) == null) {
                FileDescriptorIO.createFileDescriptorIOClass(runtime);
            }
            FFIProvider provider = newProvider(runtime);
            module.defineConstant("InvokerFactory", provider);
            module.defineConstant("LastError", provider);
            RubyModule nativeType = module.defineModuleUnder("NativeType");
            for (NativeType type : NativeType.values()) {
                nativeType.defineConstant(type.name(), runtime.newFixnum(type.ordinal()));
            }
            Platform.getPlatform().init(runtime, module);
        }
    }
    
    protected abstract FFIProvider newProvider(Ruby runtime);
    
    /**
     * Loads a native library.
     *
     * @param <T>
     * @param libraryName The name of the library to load.
     * @param libraryClass The interface class to map to the library functions.
     * @return A new instance of <tt>libraryClass</tt> that an access the library.
     */
    public abstract <T> T loadLibrary(String libraryName, Class<T> libraryClass);

    /**
     * Gets the platform info for this <tt>FFIProvider</tt>.
     *
     * @return A platform information instance.
     */
    public abstract Platform getPlatform();

    /**
     * Wraps a {@link java.nio.ByteChannel} around a native file descriptor
     */
    public abstract ByteChannel newByteChannel(int fd);
    
    
}
