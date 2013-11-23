/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi;

import java.util.ArrayList;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.io.FileDescriptorIO;

/**
 * A factory that can create a FFI Provider
 */
public abstract class Factory {

    private static final class SingletonHolder {

        private static final Factory INSTANCE = getInstance();

        private static final Factory getInstance() {
            final String providerName = System.getProperty("ffi.factory");
            Factory factory = null;
            List<String> providerNames = new ArrayList<String>();
            List<Throwable> errors = new ArrayList<Throwable>();

            if (providerName != null) {
                providerNames.add(providerName);
            }
            final String prefix = Factory.class.getPackage().getName();
            providerNames.add(prefix + ".jffi.Factory");
            for (String className : providerNames) {
                try {
                    factory = (Factory) Class.forName(className, true, Ruby.getClassLoader()).newInstance();
                    break;
                } catch (Throwable ex) {
                    errors.add(ex);
                }
            }

            if (factory == null) {
                StringBuilder sb = new StringBuilder();
                for (Throwable t : errors) {
                    sb.append(t.getLocalizedMessage()).append('\n');
                }

                factory = new NoImplFactory(sb.toString());
            }
            return factory;
        }
    }

    protected Factory() {
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
    public void init(Ruby runtime, RubyModule ffi) {
        synchronized (ffi) {
            if (ffi.getClass("Type") == null) {
                Type.createTypeClass(runtime, ffi);
            }
            DataConverter.createDataConverterModule(runtime, ffi);

            if (ffi.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS) == null) {
                AbstractMemory.createAbstractMemoryClass(runtime, ffi);
            }
            if (ffi.getClass("Buffer") == null) {
                Buffer.createBufferClass(runtime, ffi);
            }
            if (ffi.getClass("Pointer") == null) {
                Pointer.createPointerClass(runtime, ffi);
            }
            if (ffi.getClass("AutoPointer") == null) {
                AutoPointer.createAutoPointerClass(runtime, ffi);
            }
            if (ffi.getClass("MemoryPointer") == null) {
                MemoryPointer.createMemoryPointerClass(runtime, ffi);
            }
            if (ffi.getClass("Struct") == null) {
                Struct.createStructClass(runtime, ffi);
            }
            if (ffi.getClass(StructLayout.CLASS_NAME) == null) {
                StructLayout.createStructLayoutClass(runtime, ffi);
            }
            if (ffi.getClass("StructByValue") == null) {
                StructByValue.createStructByValueClass(runtime, ffi);
            }
            if (ffi.getClass(AbstractInvoker.CLASS_NAME) == null) {
                AbstractInvoker.createAbstractInvokerClass(runtime, ffi);
            }
            if (ffi.getClass(CallbackInfo.CLASS_NAME) == null) {
                CallbackInfo.createCallbackInfoClass(runtime, ffi);
            }
            if (ffi.getClass("Enum") == null) {
                Enum.createEnumClass(runtime, ffi);
            }
            if (ffi.getClass("Type").getClass("Mapped") == null) {
                MappedType.createConverterTypeClass(runtime, ffi);
            }
            if (ffi.getClass(FileDescriptorIO.CLASS_NAME) == null) {
                FileDescriptorIO.createFileDescriptorIOClass(runtime, ffi);
            }

            ffi.setConstant("TypeDefs", RubyHash.newHash(runtime));

            Platform.createPlatformModule(runtime, ffi);
            IOModule.createIOModule(runtime, ffi);
            
            StructByReference.createStructByReferenceClass(runtime, ffi);
        }
    }
    
    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param clear If the memory should be cleared.
     * @return A new <tt>AllocatedDirectMemoryIO</tt>.
     */
    public abstract MemoryIO allocateDirectMemory(Ruby runtime, int size, boolean clear);

    /**
     * Allocates memory on the native C heap and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param align The minimum alignment of the memory
     * @param clear If the memory should be cleared.
     * @return A new <tt>AllocatedDirectMemoryIO</tt>.
     */
    public abstract MemoryIO allocateDirectMemory(Ruby runtime, int size, int align, boolean clear);

    /**
     * Allocates transient native memory (not from C heap) and wraps it in a <tt>MemoryIO</tt> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param align The minimum alignment of the memory
     * @param clear If the memory should be cleared.
     * @return A new <tt>AllocatedDirectMemoryIO</tt>.
     */
    public abstract MemoryIO allocateTransientDirectMemory(Ruby runtime, int size, int align, boolean clear);

    /**
     * Wraps a  native C memory address in a <tt>MemoryIO</tt> accessor.
     *
     * @param address The native address to wrap.
     * 
     * @return A new <tt>MemoryIO</tt>.
     */
    public abstract MemoryIO wrapDirectMemory(Ruby runtime, long address);


    public abstract CallbackManager getCallbackManager();

    public abstract AbstractInvoker newFunction(Ruby runtime, Pointer address, CallbackInfo cbInfo);

    public abstract int sizeOf(NativeType type);
    public abstract int alignmentOf(NativeType type);
}
