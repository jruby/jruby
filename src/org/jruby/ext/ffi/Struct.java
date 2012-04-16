
package org.jruby.ext.ffi;

import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.Visibility.*;

@JRubyClass(name="FFI::Struct", parent="Object")
public class Struct extends RubyObject implements StructLayout.Storage {
    private final StructLayout layout;
    private AbstractMemory memory;
    private volatile Object[] referenceCache;
    private volatile IRubyObject[] valueCache;
    
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
        this(runtime, runtime.getFFI().structClass);
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

        if (!(memory == null || memory instanceof AbstractMemory)) {
            throw runtime.newTypeError("wrong argument type "
                    + memory.getMetaClass().getName() + " (expected Pointer or Buffer)");
        }

        this.memory = (AbstractMemory) memory;
    }

    static final boolean isStruct(Ruby runtime, RubyClass klass) {
        return klass.isKindOfModule(runtime.getFFI().structClass);
    }

    static final int getStructSize(Ruby runtime, IRubyObject structClass) {
        return getStructLayout(runtime, structClass).getSize();
    }
    
    static final StructLayout getStructLayout(Ruby runtime, IRubyObject structClass) {
        try {
            Object layout = ((RubyClass) structClass).getFFIHandle();
            if (layout instanceof StructLayout) {
                return (StructLayout) layout;
            }

            layout = ((RubyClass) structClass).getInstanceVariable("@layout");
            if (!(layout instanceof StructLayout)) {
                throw runtime.newRuntimeError("no valid struct layout for " + ((RubyClass) structClass).getName());
            }

            // Cache the layout on the Struct metaclass for faster retrieval next time
            ((RubyClass) structClass).setFFIHandle(layout);
            return (StructLayout) layout;

        } catch (RaiseException ex) {
            throw runtime.newRuntimeError("No layout set for struct " + ((RubyClass) structClass).getName());

        } catch (ClassCastException ex) {
            if (!(structClass instanceof RubyClass)) {
                throw runtime.newTypeError("wrong argument type "
                        + structClass.getMetaClass().getName() + " (expected subclass of Struct)");
            }
            throw runtime.newRuntimeError("invalid layout set for struct " + ((RubyClass) structClass).getName());
        }
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context) {

        memory = MemoryPointer.allocate(context.getRuntime(), layout.getSize(), 1, true);

        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
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

    @JRubyMethod(name = "initialize_copy", visibility = PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        if (other == this) {
            return this;
        }
        if (!(other instanceof Struct)) {
            throw context.getRuntime().newTypeError("not an instance of Struct");
        }
        Struct orig = (Struct) other;
        memory = (AbstractMemory) orig.getMemory().slice(context.getRuntime(), 0, layout.getSize()).dup();
        if (orig.referenceCache != null) {
            referenceCache = new Object[layout.getReferenceFieldCount()];
            System.arraycopy(orig.referenceCache, 0, referenceCache, 0, referenceCache.length);
        }
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

    @JRubyMethod(name = { "size" }, meta = true)
    public static IRubyObject size(ThreadContext context, IRubyObject structClass) {
        if (!(structClass instanceof RubyClass)) {
            throw context.getRuntime().newTypeError(structClass, context.getRuntime().getClassClass());
        }
        RubyClass klass = (RubyClass) structClass;

        Object obj = klass.getFFIHandle();
        if (obj instanceof StructLayout) {
            return ((StructLayout) obj).size(context);
        }

        if ((obj = ((RubyClass) structClass).getInstanceVariable("@layout")) instanceof StructLayout) {
            return ((StructLayout) obj).size(context);

        } else {
            obj = ((RubyClass) structClass).getInstanceVariable("@size");
        }

        return obj instanceof RubyFixnum ? (RubyFixnum) obj : RubyFixnum.zero(context.getRuntime());
    }

    @JRubyMethod(name = { "alignment", "align" }, meta = true)
    public static IRubyObject alignment(ThreadContext context, IRubyObject structClass) {
        return getStructLayout(context.getRuntime(), structClass).alignment(context);
    }

    @JRubyMethod(name = { "layout=" }, meta = true)
    public static IRubyObject set_layout(ThreadContext context, IRubyObject structClass, IRubyObject layout) {
        if (!(structClass instanceof RubyClass)) {
            throw context.getRuntime().newTypeError(structClass, context.getRuntime().getClassClass());
        }

        if (!(layout instanceof StructLayout)) {
            throw context.getRuntime().newTypeError(layout,
                    context.getRuntime().getModule("FFI").getClass("StructLayout"));
        }

        RubyClass klass = (RubyClass) structClass;
        klass.setFFIHandle(layout);
        klass.setInstanceVariable("@layout", layout);

        return structClass;
    }

    @JRubyMethod(name = "members", meta = true)
    public static IRubyObject members(ThreadContext context, IRubyObject structClass) {
        return getStructLayout(context.getRuntime(), structClass).members(context);
    }

    @JRubyMethod(name = "offsets", meta = true)
    public static IRubyObject offsets(ThreadContext context, IRubyObject structClass) {
        return getStructLayout(context.getRuntime(), structClass).offsets(context);
    }

    @JRubyMethod(name = "offset_of", meta = true)
    public static IRubyObject offset_of(ThreadContext context, IRubyObject structClass, IRubyObject fieldName) {
        return getStructLayout(context.getRuntime(), structClass).offset_of(context, fieldName);
    }


    /* ------------- instance methods ------------- */

    @JRubyMethod(name = "[]")
    public IRubyObject getFieldValue(ThreadContext context, IRubyObject fieldName) {
        return layout.getValue(context, fieldName, this, getMemory());
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject setFieldValue(ThreadContext context, IRubyObject fieldName, IRubyObject fieldValue) {
        layout.putValue(context, fieldName, this, getMemory(), fieldValue);

        return fieldValue;
    }

    @JRubyMethod(name = { "cspec", "layout" })
    public IRubyObject getLayout(ThreadContext context) {
        return layout;
    }

    @JRubyMethod(name = { "pointer", "to_ptr" })
    public IRubyObject pointer(ThreadContext context) {
        return getMemory();
    }
    
    @JRubyMethod(name = "members")
    public IRubyObject members(ThreadContext context) {
        return layout.members(context);
    }


    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context) {
        IRubyObject[] values = new IRubyObject[layout.getFieldCount()];

        int i = 0;
        for (StructLayout.Member m : layout.getMembers()) {
            values[i++] = m.get(context, this, getMemory());
        }

        return RubyArray.newArrayNoCopy(context.getRuntime(), values);
    }

    @JRubyMethod(name = "offsets")
    public IRubyObject offsets(ThreadContext context) {
        return layout.offsets(context);
    }

    @JRubyMethod(name = "offset_of")
    public IRubyObject offset_of(ThreadContext context, IRubyObject fieldName) {
        return layout.offset_of(context, fieldName);
    }


    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        return layout.size(context);
    }

    @JRubyMethod(name = { "alignment" })
    public IRubyObject alignment(ThreadContext context) {
        return layout.alignment(context);
    }

    @JRubyMethod(name="null?")
    public IRubyObject null_p(ThreadContext context) {
        return context.getRuntime().newBoolean(getMemory().getMemoryIO().isNull());
    }

    @JRubyMethod(name = "order", required = 0)
    public final IRubyObject order(ThreadContext context) {
        return context.getRuntime().newSymbol(getMemoryIO().order().equals(ByteOrder.LITTLE_ENDIAN) ? "little" : "big");
    }

    @JRubyMethod(name = "order", required = 1)
    public final IRubyObject order(ThreadContext context, IRubyObject byte_order) {
        ByteOrder order = Util.parseByteOrder(context.getRuntime(), byte_order);
        return new Struct(context.getRuntime(), getMetaClass(), layout,
                getMemory().order(context.getRuntime(), order));
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context) {
        getMemoryIO().setMemory(0, layout.size, (byte) 0);
        return this;
    }


    public final AbstractMemory getMemory() {
        return memory != null ? memory : (memory = MemoryPointer.allocate(getRuntime(), layout.getSize(), 1, true));
    }

    final MemoryIO getMemoryIO() {
        return getMemory().getMemoryIO();
    }

    public final IRubyObject getCachedValue(StructLayout.Member member) {
        return valueCache != null ? valueCache[layout.getCacheableFieldIndex(member)] : null;
    }

    public final void putCachedValue(StructLayout.Member member, IRubyObject value) {
        getValueCacheForWrite()[layout.getCacheableFieldIndex(member)] = value;
    }

    private IRubyObject[] getValueCacheForWrite() {
        return valueCache != null ? valueCache : initValueCache();
    }

    private static final AtomicReferenceFieldUpdater<Struct, IRubyObject[]> valueCacheUpdater
            = AtomicReferenceFieldUpdater.newUpdater(Struct.class, IRubyObject[].class, "valueCache");

    private IRubyObject[] initValueCache() {
        valueCacheUpdater.compareAndSet(this, null, new IRubyObject[layout.getCacheableFieldCount()]);
        return valueCache;
    }

    private Object[] getReferenceCache() {
        return referenceCache != null ? referenceCache : initReferenceCache();
    }

    private static final AtomicReferenceFieldUpdater<Struct, Object[]> referenceCacheUpdater
            = AtomicReferenceFieldUpdater.newUpdater(Struct.class, Object[].class, "referenceCache");

    private Object[] initReferenceCache() {
        referenceCacheUpdater.compareAndSet(this, null, new Object[layout.getReferenceFieldCount()]);
        return referenceCache;
    }
    
    public void putReference(StructLayout.Member member, Object value) {
        getReferenceCache()[layout.getReferenceFieldIndex(member)] = value;
    }
}
