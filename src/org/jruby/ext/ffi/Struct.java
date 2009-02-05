
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
public class Struct extends RubyObject {
    private final StructLayout layout;
    private final IRubyObject memory;
    private StructLayout.Cache cache;
    
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
        this(runtime, FFIProvider.getModule(runtime).fastGetClass("Struct"));
    }

    /**
     * Creates a new <tt>StructLayout</tt> instance.
     *
     * @param runtime The runtime for the <tt>StructLayout</tt>
     * @param klass the ruby class to use for the <tt>StructLayout</tt>
     */
    Struct(Ruby runtime, RubyClass klass) {
        this(runtime, klass, null, null);
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
    }

    static final boolean isStruct(Ruby runtime, RubyClass klass) {
        return klass.isKindOfModule(FFIProvider.getModule(runtime).getClass("Struct"));
    }
    static final int getStructSize(Ruby runtime, IRubyObject structClass) {
        return getStructLayout(runtime, structClass).getSize();
    }
    static final StructLayout getStructLayout(Ruby runtime, IRubyObject structClass) {
        try {
            return (StructLayout) ((RubyClass) structClass).fastGetClassVar("@layout");
        } catch (RaiseException ex) {
            return new StructLayout(runtime);
        }
    }
    
    private static final Struct allocateStruct(ThreadContext context, IRubyObject klass, int flags) {
        Ruby runtime = context.getRuntime();
        StructLayout layout = getStructLayout(runtime, klass);
        return new Struct(runtime, (RubyClass) klass, layout, new Buffer(runtime, layout.getSize(), flags));
    }

    /*
     * This variant of newStruct is called from StructLayoutBuilder
     */
    static final Struct newStruct(Ruby runtime, RubyClass klass, IRubyObject ptr) {
        return new Struct(runtime, (RubyClass) klass, getStructLayout(runtime, klass), ptr);
    }
    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject self) {
        return allocateStruct(context, self, Buffer.IN | Buffer.OUT);
    }
    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject self, IRubyObject ptr) {
        return new Struct(context.getRuntime(), (RubyClass) self, getStructLayout(context.getRuntime(), self), ptr);
    }
    
    @JRubyMethod(name = "new", meta = true, rest = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        StructLayout layout = null;

        if (args.length > 1) {
            IRubyObject[] layoutArgs = new IRubyObject[args.length - 1];
            System.arraycopy(args, 1, layoutArgs, 0, args.length - 1);
            layout = (StructLayout) self.callMethod(context, "layout", layoutArgs);
        } else {
            layout = getStructLayout(context.getRuntime(), self);
        }
        IRubyObject ptr = args.length > 0 && !args[0].isNil()
                ? args[0] : new Buffer(context.getRuntime(), layout.getSize());
        return new Struct(context.getRuntime(), (RubyClass) self, layout, ptr);
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
        return layout.get(context.getRuntime(), this, fieldName);
    }
    @JRubyMethod(name = "[]=")
    public IRubyObject setFieldValue(ThreadContext context, IRubyObject fieldName, IRubyObject fieldValue) {
        return layout.put(context, memory, fieldName, fieldValue);
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
    final IRubyObject getCachedValue(StructLayout.Member member) {
        return cache != null ? cache.get(member) : null;
    }
    final void putCachedValue(StructLayout.Member member, IRubyObject value) {
        if (cache == null) {
            cache = new StructLayout.Cache(layout);
        }
        cache.put(member, value);
    }
}
