package org.jruby.ext.ffi;

import org.jruby.*;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The holder of all per-ruby-runtime FFI data
 */
public class FFI {
    public final RubyModule ffiModule;
    public final RubyClass memoryClass, bufferClass, pointerClass, memptrClass;
    public final RubyClass structClass, functionClass, callbackClass;
    public final RubyClass typeClass;
    public final RubyHash typedefs;
    public final Pointer nullPointer;
    private final NullMemoryIO nullMemoryIO;
    private final TypeResolver typeResolver;
    
    /**
     * Reference map to keep libraries open for as long as there is a method mapped
     * to that library.
     */
    private final Map<DynamicMethod, AbstractInvoker> refmap
            = Collections.synchronizedMap(new WeakHashMap<DynamicMethod, AbstractInvoker>());


    public FFI(RubyModule ffiModule) {
        var context = ffiModule.getRuntime().getCurrentContext();
        this.ffiModule = ffiModule;
        this.bufferClass = ffiModule.getClass(context, "Buffer");
        this.pointerClass = ffiModule.getClass(context, "Pointer");
        this.memptrClass = ffiModule.getClass(context, "MemoryPointer");
        this.memoryClass = ffiModule.getClass(context, "AbstractMemory");
        this.structClass = ffiModule.getClass(context, "Struct");
        this.functionClass = ffiModule.getClass(context, "Function");
        this.callbackClass = ffiModule.getClass(context, "Callback");
        this.typeClass = ffiModule.getClass(context, "Type");
        this.typedefs = (RubyHash) ffiModule.getConstant(context, "TypeDefs");
        this.typeResolver = new TypeResolver(this);
        this.nullMemoryIO = new NullMemoryIO(context.runtime);
        this.nullPointer = (Pointer) pointerClass.getConstant(context, "NULL");
    }

    public final TypeResolver getTypeResolver() {
        return typeResolver;
    }

    public NullMemoryIO getNullMemoryIO() {
        return nullMemoryIO;
    }

    public void registerAttachedMethod(DynamicMethod method, AbstractInvoker invoker) {
        refmap.put(method, invoker);
    }
}
