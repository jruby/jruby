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

import java.nio.channels.ByteChannel;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.io.FileDescriptorIO;

/**
 * Base class for all FFI providers
 */
public abstract class FFIProvider {
    
    /**
     * The name of the module to place all the classes/methods under.
     */
    public static final String MODULE_NAME = "JRuby::FFI";

    private static final class SingletonHolder {
        private static final FFIProvider INSTANCE = getInstance();
        private static final FFIProvider getInstance() {
            final boolean useJNA = Boolean.getBoolean("ffi.usejna");
            String prefix = FFIProvider.class.getPackage().getName();
            FFIProvider provider = null;
            if (!useJNA) {
                try {
                    provider = (FFIProvider) Class.forName(prefix + ".jffi.JFFIProvider").newInstance();
                } catch (Throwable ex) {
                }
            }
            if (provider == null) {
                try {
                    provider = (FFIProvider) Class.forName(prefix + ".jna.JNAProvider").newInstance();
                } catch (Throwable ex) {
                    throw new RuntimeException("Could not load FFI provider", ex);
                }
            }
            return provider;
        }
    }
    protected FFIProvider() {}

    /**
     * Gets an instance of <tt>FFIProvider</tt>
     * 
     * @return an instance of <tt>FFIProvider</tt>
     */
    public static final FFIProvider getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static RubyModule getModule(Ruby runtime) {
        return (RubyModule) runtime.fastGetModule("JRuby").fastGetConstantAt("FFI");
    }
    /**
     * Registers FFI ruby classes/modules
     * 
     * @param module the module to register the classes under
     */
    public void setup(RubyModule module) {
        synchronized (module) {
            if (module.fastGetClass(AbstractMemoryPointer.className) == null) {
                AbstractMemoryPointer.createMemoryPointerClass(module.getRuntime());
            }
            if (module.fastGetClass(StructLayout.CLASS_NAME) == null) {
                StructLayout.createStructLayoutClass(module.getRuntime());
            }
            if (module.fastGetClass(StructLayoutBuilder.CLASS_NAME) == null) {
                StructLayoutBuilder.createStructLayoutBuilderClass(module.getRuntime());
            }
            if (module.fastGetClass(FileDescriptorIO.CLASS_NAME) == null) {
                FileDescriptorIO.createFileDescriptorIOClass(module.getRuntime());
            }
        }
    }

    /**
     * Creates a new invoker for a native function.
     * 
     * @param libraryName The library that contains the function.
     * @param functionName The function name.
     * @param returnType The return type of the function.
     * @param parameterTypes The parameter types the function takes.
     * @return a new <tt>Invoker</tt> instance.
     */
    public abstract Invoker createInvoker(String libraryName, String functionName, NativeType returnType,
            NativeType[] parameterTypes, String convention);
    
    /**
     * Gets the last native error code.
     * <p>
     * This returns the errno value that was set at the time of the last native 
     * function call.
     * 
     * @return The errno value.
     */
    public abstract int getLastError();
    
    /**
     * Sets the native error code.
     * 
     * @param error The value to set errno to.
     */
    public abstract void setLastError(int error);

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
