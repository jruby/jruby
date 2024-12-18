/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import static org.jruby.api.Access.*;
import static org.jruby.api.Create.newHash;

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
                    factory = (Factory) Class.forName(className, true, Ruby.getClassLoader()).getConstructor().newInstance();
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
     * Gets an instance of <code>FFIProvider</code>
     * 
     * @return an instance of <code>FFIProvider</code>
     */
    public static final Factory getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    /**
     * Registers FFI ruby classes/modules
     * 
     * @param runtime The ruby runtime
     * @param FFI the module to register the classes under
     */
    public void init(Ruby runtime, RubyModule FFI) {
        synchronized (FFI) {
            var context = runtime.getCurrentContext();
            var Object = objectClass(context);
            var _Type = FFI.getClass("Type");
            var _Struct = FFI.getClass("Struct");
            var _AbstractMemory = FFI.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS);
            var _Pointer = FFI.getClass("Pointer");
            var _DataConverter = DataConverter.createDataConverterModule(context, FFI);

            if (_Type == null) _Type = Type.createTypeClass(context, FFI, Object);
            if (_AbstractMemory == null) _AbstractMemory = AbstractMemory.createAbstractMemoryClass(context, FFI, Object);
            if (FFI.getClass("Buffer") == null) Buffer.createBufferClass(context, FFI, _AbstractMemory);
            if (_Pointer == null) _Pointer = Pointer.createPointerClass(context, FFI, _AbstractMemory);
            if (FFI.getClass("AutoPointer") == null) AutoPointer.createAutoPointerClass(context, FFI, _Pointer);
            if (FFI.getClass("MemoryPointer") == null) MemoryPointer.createMemoryPointerClass(context, FFI, _Pointer);
            if (_Struct == null) _Struct = Struct.createStructClass(context, FFI);
            if (FFI.getClass(StructLayout.CLASS_NAME) == null) {
                StructLayout.createStructLayoutClass(context, FFI, Object, enumerableModule(context), _Type, _Struct);
            }
            if (FFI.getClass("StructByValue") == null) StructByValue.createStructByValueClass(context, FFI, _Type);
            if (FFI.getClass(AbstractInvoker.CLASS_NAME) == null) {
                AbstractInvoker.createAbstractInvokerClass(context, FFI, _Pointer);
            }
            if (FFI.getClass(CallbackInfo.CLASS_NAME) == null) CallbackInfo.createCallbackInfoClass(context, FFI, _Type);
            if (FFI.getClass("Enums") == null) Enums.createEnumsClass(context, FFI, Object, _DataConverter);
            if (_Type.getClass("Mapped") == null) MappedType.createConverterTypeClass(context, _Type);
            if (FFI.getClass(FileDescriptorIO.CLASS_NAME) == null) {
                FileDescriptorIO.createFileDescriptorIOClass(context, FFI, ioClass(context));
            }

            FFI.defineConstant(context, "TypeDefs", newHash(context));

            Platform.createPlatformModule(context, FFI);
            IOModule.createIOModule(context, FFI);
        }
    }
    
    /**
     * Allocates memory on the native C heap and wraps it in a <code>MemoryIO</code> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param clear If the memory should be cleared.
     * @return A new <code>AllocatedDirectMemoryIO</code>.
     */
    public abstract MemoryIO allocateDirectMemory(Ruby runtime, int size, boolean clear);

    /**
     * Allocates memory on the native C heap and wraps it in a <code>MemoryIO</code> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param align The minimum alignment of the memory
     * @param clear If the memory should be cleared.
     * @return A new <code>AllocatedDirectMemoryIO</code>.
     */
    public abstract MemoryIO allocateDirectMemory(Ruby runtime, int size, int align, boolean clear);

    /**
     * Allocates transient native memory (not from C heap) and wraps it in a <code>MemoryIO</code> accessor.
     *
     * @param size The number of bytes to allocate.
     * @param align The minimum alignment of the memory
     * @param clear If the memory should be cleared.
     * @return A new <code>AllocatedDirectMemoryIO</code>.
     */
    public abstract MemoryIO allocateTransientDirectMemory(Ruby runtime, int size, int align, boolean clear);

    /**
     * Wraps a  native C memory address in a <code>MemoryIO</code> accessor.
     *
     * @param address The native address to wrap.
     * 
     * @return A new <code>MemoryIO</code>.
     */
    public abstract MemoryIO wrapDirectMemory(Ruby runtime, long address);


    public abstract CallbackManager getCallbackManager();

    public abstract AbstractInvoker newFunction(Ruby runtime, Pointer address, CallbackInfo cbInfo);

    public abstract int sizeOf(NativeType type);
    public abstract int alignmentOf(NativeType type);
}
