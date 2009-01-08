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

package org.jruby.ext.ffi.jna;

import com.sun.jna.Native;
import java.nio.channels.ByteChannel;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.FFIProvider;
import org.jruby.ext.ffi.MemoryIO;
import org.jruby.ext.ffi.Pointer;

/**
 * An implementation of FFI for JNA
 */
public class Factory extends org.jruby.ext.ffi.Factory {
    
    @Override
    public void init(Ruby runtime, RubyModule ffi) {
        super.init(runtime, ffi);
        //
        // Hook up the MemoryPointer class if its not already there
        //
        synchronized (ffi) {
            if (ffi.fastGetClass(JNABasePointer.JNA_POINTER_NAME) == null) {
                JNABasePointer.createJNAPointerClass(runtime, ffi);
            }
            if (ffi.fastGetClass(JNAMemoryPointer.MEMORY_POINTER_NAME) == null) {
                JNAMemoryPointer.createMemoryPointerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("VariadicInvoker") == null) {
                JNAVariadicInvoker.createVariadicInvokerClass(runtime, ffi);
            }
            if (ffi.fastGetClass("DynamicLibrary") == null) {
                DynamicLibrary.createDynamicLibraryClass(runtime, ffi);
            }
        }
    }
    protected FFIProvider newProvider(Ruby runtime) {
        return new JNAProvider(runtime);
    }
    
    @Override
    public <T> T loadLibrary(String libraryName, Class<T> libraryClass) {
        try {
            return libraryClass.cast(Native.loadLibrary(libraryName, libraryClass));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ByteChannel newByteChannel(int fd) {
        return new FileDescriptorByteChannel(fd);
    }

    @Override
    public MemoryIO allocateHeapMemory(int size, boolean clear) {
        return new HeapMemoryIO(size);
    }

    @Override
    public Pointer newPointer(Ruby runtime, MemoryIO io) {
        return new JNABasePointer(runtime, io, 0, Long.MAX_VALUE);
    }
}
