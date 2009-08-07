
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="FFI::Struct", parent="Object")
public class Struct extends RubyObject implements StructLayout.Storage {
    private final StructLayout layout;
    private final IRubyObject[] referenceCache;
    private IRubyObject memory;
    private IRubyObject[] valueCache;
    
    private static final class Allocator implements ObjectAllocator {
        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Struct(runtime, klass);
        }
        private static final ObjectAllocator INSTANCE = new Allocator();
    }

    /**
     * Registers the StructLayout class in the JRuby runtime.
     * @param runtime The JRuby runtime to register the new class in.
     * @return The new class
     */
    public static RubyClass createStructClass(Ruby runtime, RubyModule module) {
        
        RubyClass result = runtime.defineClassUnder("Struct", runtime.getObject(),
                Allocator.INSTANCE, module);
        result.defineAnnotatedMethods(Struct.class);
        result.defineAnnotatedConstants(Struct.class);
        
        return result;
    }

    /**
     * Creates a new <tt>StructLayout</tt> instance using defaults.
     *
     * @param runtime The runtime for the <tt>StructLayout</tt>
     */
    Struct(Ruby runtime) {
        this(runtime, runtime.fastGetModule("FFI").fastGetClass("Struct"));
    }

    /**
     * Creates a new <tt>StructLayout</tt> instance.
     *
     * @param runtime The runtime for the <tt>StructLayout</tt>
     * @param klass the ruby class to use for the <tt>StructLayout</tt>
     */
    Struct(Ruby runtime, RubyClass klass) {
        this(runtime, klass, getStructLayout(runtime, klass), null);
    }

    /**
     * Creates a new <tt>StructLayout</tt> instance.
     *
     * @param runtime The runtime for the <tt>StructLayout</tt>
     * @param klass the ruby class to use for the <tt>StructLayout</tt>
     */
    Struct(Ruby runtime, RubyClass klass, StructLayout layout, IRubyObject memory) {
        super(runtime, klass);
        this.layout = layout;
        this.memory = memory;
        this.referenceCache = new IRubyObject[layout.getReferenceFieldCount()];
    }

    static final boolean isStruct(Ruby runtime, RubyClass klass) {
        return klass.isKindOfModule(runtime.fastGetModule("FFI").getClass("Struct"));
    }

    static final int getStructSize(Ruby runtime, IRubyObject structClass) {
        return getStructLayout(runtime, structClass).getSize();
    }
    
    static final StructLayout getStructLayout(Ruby runtime, IRubyObject structClass) {
        if (!(structClass instanceof RubyClass)) {
            throw runtime.newTypeError("wrong argument type "
                    + structClass.getMetaClass().getName() + " (expected subclass of Struct");
        }
        try {
            StructLayout layout = (StructLayout) ((RubyClass) structClass).fastGetInstanceVariable("@layout");
            if (layout == null) {
                throw runtime.newRuntimeError("No struct layout set for " + ((RubyClass) structClass).getName());
            }
            return layout;

        } catch (RaiseException ex) {
            throw runtime.newRuntimeError("No layout set for struct " + ((RubyClass) structClass).getName());
        } catch (ClassCastException ex) {
            throw runtime.newRuntimeError("Invalid layout set for struct " + ((RubyClass) structClass).getName());
        }
    }
    
    /*
     * This variant of newStruct is called from StructLayoutBuilder
     */
    static final Struct newStruct(Ruby runtime, RubyClass klass, IRubyObject ptr) {
        return new Struct(runtime, (RubyClass) klass, getStructLayout(runtime, klass), ptr);
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context) {

        memory = MemoryPointer.allocate(context.getRuntime(), layout.getSize(), 1, true);

        return this;
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject ptr) {
        
        if (!(ptr instanceof AbstractMemory)) {
            throw context.getRuntime().newTypeError("wrong argument type "
                    + ptr.getMetaClass().getName() + " (expected Pointer or Buffer)");
        }

        if (((AbstractMemory) ptr).getSize() < layout.getSize()) {
            throw context.getRuntime().newArgumentError("memory object has insufficient space for "
                    + getMetaClass().getName());
        }

        memory = (AbstractMemory) ptr;
        
        return this;
    }

    private static final Struct allocateStruct(ThreadContext context, IRubyObject klass, int flags) {
        Ruby runtime = context.getRuntime();
        StructLayout layout = getStructLayout(runtime, klass);
        return new Struct(runtime, (RubyClass) klass, layout, new Buffer(runtime, layout.getSize(), flags));
    }

    @JRubyMethod(name = { "new_in", "alloc_in" }, meta = true)
    public static IRubyObject allocateIn(ThreadContext context, IRubyObject klass) {
        return allocateStruct(context, klass, Buffer.IN);
    }
    @JRubyMethod(name = { "new_in", "alloc_in" }, meta = true)
    public static IRubyObject allocateIn(ThreadContext context, IRubyObject klass, IRubyObject clearArg) {
        return allocateStruct(context, klass, Buffer.IN);
    }
    @JRubyMethod(name = { "new_out", "alloc_out" }, meta = true)
    public static IRubyObject allocateOut(ThreadContext context, IRubyObject klass) {
        return allocateStruct(context, klass, Buffer.OUT);
    }
    @JRubyMethod(name = { "new_out", "alloc_out" }, meta = true)
    public static IRubyObject allocateOut(ThreadContext context, IRubyObject klass, IRubyObject clearArg) {
        return allocateStruct(context, klass, Buffer.OUT);
    }
    @JRubyMethod(name = { "new_inout", "alloc_inout" }, meta = true)
    public static IRubyObject allocateInOut(ThreadContext context, IRubyObject klass) {
        return allocateStruct(context, klass, Buffer.IN | Buffer.OUT);
    }
    @JRubyMethod(name = { "new_inout", "alloc_inout" }, meta = true)
    public static IRubyObject allocateInOut(ThreadContext context, IRubyObject klass, IRubyObject clearArg) {
        return allocateStruct(context, klass, Buffer.IN | Buffer.OUT);
    }

    @JRubyMethod(name = "[]")
    public IRubyObject getFieldValue(ThreadContext context, IRubyObject fieldName) {
        return layout.getMember(context.getRuntime(), fieldName).get(context.getRuntime(), this, getMemory());
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject setFieldValue(ThreadContext context, IRubyObject fieldName, IRubyObject fieldValue) {
        Ruby runtime = context.getRuntime();
        layout.getMember(runtime, fieldName).put(runtime, this, getMemory(), fieldValue);
        return fieldValue;
    }

    @JRubyMethod(name = { "cspec", "layout" })
    public IRubyObject getLayout(ThreadContext context) {
        return layout;
    }

    @JRubyMethod(name = "pointer")
    public IRubyObject pointer(ThreadContext context) {
        return memory;
    }
    @JRubyMethod(name = "members")
    public IRubyObject members(ThreadContext context) {
        return layout.members(context);
    }

    public final IRubyObject getMemory() {
        return memory;
    }
    final MemoryIO getMemoryIO() {
        return ((AbstractMemory) memory).getMemoryIO();
    }

    public final IRubyObject getCachedValue(StructLayout.Member member) {
        return valueCache != null ? valueCache[layout.getCacheableFieldIndex(member)] : null;
    }

    public final void putCachedValue(StructLayout.Member member, IRubyObject value) {
        if (valueCache == null) {
            valueCache = new IRubyObject[layout.getCacheableFieldCount()];
        }
        valueCache[layout.getCacheableFieldIndex(member)] = value;
    }
    
    public void putReference(StructLayout.Member member, IRubyObject value) {
        referenceCache[layout.getReferenceFieldIndex(member)] = value;
    }
}
