/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 JRuby project
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.io.FileDescriptorIO;
import org.jruby.runtime.load.Library;

/**
 * A factory that can create a FFI Provider
 */
public abstract class Factory {

    private static final class SingletonHolder {

        private static final Factory INSTANCE = getInstance();

        private static final Factory getInstance() {
            final boolean useJNA = Boolean.getBoolean("jruby.ffi.usejna");
            String prefix = FFIProvider.class.getPackage().getName();
            Factory factory = null;
            if (!useJNA) {
                try {
                    factory = (Factory) Class.forName(prefix + ".jffi.JFFIFactory", true, Ruby.getClassLoader()).newInstance();
                } catch (Throwable ex) {
                }
            }
            if (factory == null) {
                try {
                    factory = (Factory) Class.forName(prefix + ".jna.JNAFactory", true, Ruby.getClassLoader()).newInstance();
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
            if (!RubyInstanceConfig.nativeEnabled) {
                throw runtime.newLoadError("Native API access is disabled");
            }
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
        RubyModule ffi = runtime.defineModule("FFI");
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
            if (ffi.fastGetClass("Pointer") == null) {
                Pointer.createPointerClass(runtime);
            }
            if (module.fastGetClass(AbstractMemoryPointer.className) == null) {
                AbstractMemoryPointer.createMemoryPointerClass(runtime);
            }
            if (module.fastGetClass(AbstractBuffer.ABSTRACT_BUFFER_RUBY_CLASS) == null) {
                AbstractBuffer.createBufferClass(runtime);
            }
            if (module.fastGetClass(Callback.CLASS_NAME) == null) {
                Callback.createCallbackClass(runtime);
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
            module.defineConstant("CallbackFactory", provider);
            ffi.defineConstant("LastError", provider);
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
