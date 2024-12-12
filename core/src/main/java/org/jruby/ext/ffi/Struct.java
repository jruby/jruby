
package org.jruby.ext.ffi;

import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Error.*;
import static org.jruby.runtime.Visibility.*;

@JRubyClass(name="FFI::Struct", parent="Object")
public class Struct extends MemoryObject implements StructLayout.Storage {
    private StructLayout layout;
    private AbstractMemory memory;
    private volatile Object[] referenceCache;
    private volatile IRubyObject[] valueCache;

    /**
     * Registers the StructLayout class in the JRuby runtime.
     * @param context the current thread context
     * @param FFI reference to FFI module
     * @return The new class
     */
    public static RubyClass createStructClass(ThreadContext context, RubyModule FFI) {
        ObjectAllocator allocator = Options.REIFY_FFI.load() ? new ReifyingAllocator(Struct.class): Struct::new;
        return FFI.defineClassUnder(context, "Struct", objectClass(context), allocator).
                reifiedClass(Struct.class).
                kindOf(new RubyModule.KindOf() {
                    @Override
                    public boolean isKindOf(IRubyObject obj, RubyModule type) {
                        return obj instanceof Struct && super.isKindOf(obj, type);
                    }
                }).
                defineMethods(context, Struct.class).
                defineConstants(context, Struct.class);
    }

    /**
     * Creates a new <code>StructLayout</code> instance using defaults.
     *
     * @param runtime The runtime for the <code>StructLayout</code>
     */
    Struct(Ruby runtime) {
        this(runtime, runtime.getFFI().structClass);
    }

    /**
     * Creates a new <code>StructLayout</code> instance.
     *
     * @param runtime The runtime for the <code>StructLayout</code>
     * @param klass the ruby class to use for the <code>StructLayout</code>
     */
    public Struct(Ruby runtime, RubyClass klass) {
        this(runtime, klass, getStructLayout(runtime.getCurrentContext(), klass), null);
    }

    /**
     * Creates a new <code>StructLayout</code> instance.
     *
     * @param runtime The runtime for the <code>StructLayout</code>
     * @param klass the ruby class to use for the <code>StructLayout</code>
     */
    Struct(Ruby runtime, RubyClass klass, StructLayout layout, IRubyObject memory) {
        super(runtime, klass);
        this.layout = layout;

        if (!(memory == null || memory instanceof AbstractMemory)) {
            throw typeError(runtime.getCurrentContext(), memory, "Pointer or Buffer");
        }

        this.memory = (AbstractMemory) memory;
    }

    static final boolean isStruct(ThreadContext context, RubyClass klass) {
        return klass.isKindOfModule(context.runtime.getFFI().structClass);
    }

    static final int getStructSize(ThreadContext context, IRubyObject structClass) {
        return getStructLayout(context, structClass).getSize();
    }
    
    static final StructLayout getStructLayout(ThreadContext context, IRubyObject structClass) {
        try {
            RubyClass klass = (RubyClass) structClass;
            Object layout = klass.getFFIHandle();
            if (layout instanceof StructLayout sl) return sl;

            while (klass != objectClass(context) && !((layout = klass.getInstanceVariable("@layout")) instanceof StructLayout)) {
                klass = klass.getSuperClass();
            }

            if (layout instanceof StructLayout sl) {
                klass.setFFIHandle(layout); // Cache the layout for faster retrieval next time
                return sl;
            }
            throw runtimeError(context, "no valid struct layout for " + klass.getName());
        } catch (ClassCastException ex) {
            if (!(structClass instanceof RubyClass sc)) throw typeError(context, structClass, "subclass of Struct");

            throw runtimeError(context, "invalid layout set for struct " + sc.getName());
        }
    }

    @Override
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        memory = MemoryPointer.allocate(context.runtime, layout.getSize(), 1, true);
        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject ptr) {
        
        if (!(ptr instanceof AbstractMemory)) {
            if (ptr.isNil()) return initialize(context);

            throw typeError(context, ptr, "Pointer or Buffer");
        }

        if (((AbstractMemory) ptr).getSize() < layout.getSize()) {
            throw argumentError(context, "memory object has insufficient space for " + getMetaClass().getName());
        }

        memory = (AbstractMemory) ptr;
        setMemoryIO(memory.getMemoryIO());
        
        return this;
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE, rest = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            default: // > 1
                IRubyObject result = getMetaClass().callMethod(context, "layout", args[1] instanceof RubyArray
                        ? ((RubyArray) args[1]).toJavaArrayUnsafe()
                        : java.util.Arrays.copyOfRange(args, 1, args.length));
                if (result instanceof StructLayout struct) {
                    layout = struct;
                } else {
                    throw typeError(context, "Struct.layout did not return a FFI::StructLayout instance");
                }
            case 1: return initialize(context, args[0]);
            case 0: return initialize(context);
        }
    }

    @JRubyMethod(name = "initialize_copy", visibility = PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        if (other == this) return this;
        if (!(other instanceof Struct orig)) throw typeError(context, "not an instance of Struct");

        memory = (AbstractMemory) orig.getMemory().slice(context.runtime, 0, layout.getSize()).dup();
        if (orig.referenceCache != null) {
            referenceCache = new Object[layout.getReferenceFieldCount()];
            System.arraycopy(orig.referenceCache, 0, referenceCache, 0, referenceCache.length);
        }
        setMemoryIO(memory.getMemoryIO());

        return this;
    }


    private static final Struct allocateStruct(ThreadContext context, IRubyObject klass, int flags) {
        StructLayout layout = getStructLayout(context, klass);
        return new Struct(context.runtime, (RubyClass) klass, layout, new Buffer(context.runtime, layout.getSize(), flags));
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
        RubyClass klass = castAsClass(context, structClass);

        Object obj = klass.getFFIHandle();
        if (obj instanceof StructLayout) {
            return ((StructLayout) obj).size(context);
        }

        if ((obj = ((RubyClass) structClass).getInstanceVariable("@layout")) instanceof StructLayout) {
            return ((StructLayout) obj).size(context);

        } else {
            obj = ((RubyClass) structClass).getInstanceVariable("@size");
        }

        return obj instanceof RubyFixnum ? (RubyFixnum) obj : RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod(name = "alignment", meta = true)
    public static IRubyObject alignment(ThreadContext context, IRubyObject structClass) {
        return getStructLayout(context, structClass).alignment(context);
    }

    @JRubyMethod(name = { "layout=" }, meta = true)
    public static IRubyObject set_layout(ThreadContext context, IRubyObject structClass, IRubyObject layout) {
        RubyClass klass = castAsClass(context, structClass);

        if (!(layout instanceof StructLayout)) {
            throw typeError(context, layout, context.runtime.getModule("FFI").getClass("StructLayout"));
        }

        klass.setFFIHandle(layout);
        klass.setInstanceVariable("@layout", layout);

        return structClass;
    }

    @JRubyMethod(name = "members", meta = true)
    public static IRubyObject members(ThreadContext context, IRubyObject structClass) {
        return getStructLayout(context, structClass).members(context);
    }

    @JRubyMethod(name = "offsets", meta = true)
    public static IRubyObject offsets(ThreadContext context, IRubyObject structClass) {
        return getStructLayout(context, structClass).offsets(context);
    }

    @JRubyMethod(name = "offset_of", meta = true)
    public static IRubyObject offset_of(ThreadContext context, IRubyObject structClass, IRubyObject fieldName) {
        return getStructLayout(context, structClass).offset_of(context, fieldName);
    }


    /* ------------- instance methods ------------- */

    @JRubyMethod(name = "[]")
    public IRubyObject getFieldValue(ThreadContext context, IRubyObject fieldName) {
        return layout.getMember(context, fieldName).get(context, this, getMemory());
    }

    @JRubyMethod(name = "[]=")
    public IRubyObject setFieldValue(ThreadContext context, IRubyObject fieldName, IRubyObject fieldValue) {
        layout.getMember(context, fieldName).put(context, this, getMemory(), fieldValue);

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

        return RubyArray.newArrayMayCopy(context.runtime, values);
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
        return asBoolean(context, getMemory().getMemoryIO().isNull());
    }

    @JRubyMethod(name = "order")
    public final IRubyObject order(ThreadContext context) {
        return asSymbol(context, getMemoryIO().order().equals(ByteOrder.LITTLE_ENDIAN) ? "little" : "big");
    }

    @JRubyMethod(name = "order")
    public final IRubyObject order(ThreadContext context, IRubyObject byte_order) {
        ByteOrder order = Util.parseByteOrder(context.runtime, byte_order);
        return new Struct(context.runtime, getMetaClass(), layout, getMemory().order(context.runtime, order));
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context) {
        getMemoryIO().setMemory(0, layout.size, (byte) 0);
        return this;
    }


    public final AbstractMemory getMemory() {
        return memory != null ? memory : (memory = MemoryPointer.allocate(getRuntime(), layout.getSize(), 1, true));
    }

    protected final MemoryIO allocateMemoryIO() {
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
