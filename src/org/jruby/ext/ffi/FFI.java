package org.jruby.ext.ffi;

import org.jruby.*;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collections;
import java.util.Map;

/**
 * The holder of all per-ruby-runtime FFI data
 */
public class FFI {
    public final RubyModule ffiModule;
    public final RubyClass memoryClass, bufferClass, pointerClass, memptrClass;
    public final RubyClass structClass, functionClass, callbackClass;
    public final RubyClass typeClass;
    public final RubyHash typedefs;
    private final NullMemoryIO nullMemoryIO;
    private final TypeSizeMapper sizeMapper;


    public FFI(RubyModule ffiModule) {
        this.ffiModule = ffiModule;
        this.bufferClass = ffiModule.getClass("Buffer");
        this.pointerClass = ffiModule.getClass("Pointer");
        this.memptrClass = ffiModule.getClass("MemoryPointer");
        this.memoryClass = ffiModule.getClass("AbstractMemory");
        this.structClass = ffiModule.getClass("Struct");
        this.functionClass = ffiModule.getClass("Function");
        this.callbackClass = ffiModule.getClass("Callback");
        this.typeClass = ffiModule.getClass("Type");
        this.typedefs = (RubyHash) ffiModule.getConstant("TypeDefs");
        this.sizeMapper = new TypeSizeMapper(this);
        this.nullMemoryIO = new NullMemoryIO(ffiModule.getRuntime());
    }

    public TypeSizeMapper getSizeMapper() {
        return sizeMapper;
    }

    public NullMemoryIO getNullMemoryIO() {
        return nullMemoryIO;
    }
}
