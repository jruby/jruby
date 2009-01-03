
package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="FFI::Struct", parent="Object")
public class Struct extends RubyObject {
    private final StructLayout layout;
    private final IRubyObject memory;
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

    static final int getStructSize(IRubyObject structClass) {
        return RubyNumeric.fix2int(((RubyClass) structClass).fastGetClassVar("@size"));
    }
    static final StructLayout getStructLayout(ThreadContext context, IRubyObject structClass) {
        RubyClass klass = (RubyClass) structClass;
        return klass.fastIsClassVarDefined("@layout")
                ? (StructLayout) klass.fastGetClassVar("@layout")
                : new StructLayout(context.getRuntime());
    }
    private static final Struct newStruct(ThreadContext context, IRubyObject klass, StructLayout layout, IRubyObject ptr) {
        Struct s = new Struct(context.getRuntime(), (RubyClass) klass,
                layout != null ? (StructLayout) layout : getStructLayout(context, klass),
                ptr != null && !ptr.isNil() ? ptr : new Buffer(context.getRuntime(), getStructSize(klass)));
        return s;
    }
    @JRubyMethod(name = "new", meta = true)
    public static Struct newInstance(ThreadContext context, IRubyObject self) {
        return newStruct(context, self, null, null);
    }
    @JRubyMethod(name = "new", meta = true)
    public static Struct newInstance(ThreadContext context, IRubyObject self, IRubyObject ptr) {
        return newStruct(context, self, null, ptr);
    }
    
    @JRubyMethod(name = "new", meta = true, rest = true)
    public static Struct newInstance(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        IRubyObject ptr = args.length > 0 ? args[0] : null;
        StructLayout layout = null;

        if (args.length > 1) {
            IRubyObject[] layoutArgs = new IRubyObject[args.length - 1];
            System.arraycopy(args, 1, layoutArgs, 0, args.length - 1);
            layout = (StructLayout) self.callMethod(context, "layout", layoutArgs);
        }
        return newStruct(context, self, layout, ptr);
    }
    @JRubyMethod(name = "[]")
    public IRubyObject getFieldValue(ThreadContext context, IRubyObject fieldName) {
        return layout.get(context, memory, fieldName);
    }
    @JRubyMethod(name = "[]=")
    public IRubyObject setFieldValue(ThreadContext context, IRubyObject fieldName, IRubyObject fieldValue) {
        return layout.put(context, memory, fieldName, fieldValue);
    }
    @JRubyMethod(name = "cspec")
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
}
