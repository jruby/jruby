
package org.jruby.ext.ffi;


import java.nio.ByteOrder;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.Visibility.*;


@JRubyClass(name = "FFI::Buffer", parent = "FFI::" + AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS)
public final class Buffer extends AbstractMemory {
    /** Indicates that the Buffer is used for data copied IN to native memory */
    public static final int IN = 0x1;

    /** Indicates that the Buffer is used for data copied OUT from native memory */
    public static final int OUT = 0x2;
    
    private int inout;

    public static RubyClass createBufferClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Buffer",
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                BufferAllocator.INSTANCE);
        result.defineAnnotatedMethods(Buffer.class);
        result.defineAnnotatedConstants(Buffer.class);
//        result.getSingletonClass().addMethod("new", new NewInstanceMethod(result.getRealClass()));

        return result;
    }

    private static final class BufferAllocator implements ObjectAllocator {
        static final ObjectAllocator INSTANCE = new BufferAllocator();

        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new Buffer(runtime, klazz);
        }
    }

    private static final class NewInstanceMethod extends DynamicMethod {
        private final NativeCall new1, new2, new3;
        private NewInstanceMethod(RubyModule implementationClass) {
            super(implementationClass, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone);

            new1 = new NativeCall(Buffer.class, "newInstance", IRubyObject.class,
                    new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class }, true, false);
            new2 = new NativeCall(Buffer.class, "newInstance", IRubyObject.class,
                    new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class }, true, false);
            new3 = new NativeCall(Buffer.class, "newInstance", IRubyObject.class,
                    new Class[] { ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class }, true, false);
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }

        @Override
        public Arity getArity() {
            return Arity.fixed(1);
        }

        @Override
        public NativeCall getNativeCall() {
            return new1;
        }

        @Override
        public void setHandle(Object handle) {}

        @Override
        public NativeCall getNativeCall(int args, boolean block) {
            switch (args) {
                case 1:
                    return new1;
                case 2:
                    return new2;
                case 3:
                    return new3;
            }
            return super.getNativeCall(args, block);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            switch (args.length) {
                case 1:
                    return call(context, self, clazz, name, args[0]);

                case 2:
                case 3:
                    return call(context, self, clazz, name, args[0], args[1]);

                default:
                    return ((RubyClass) self).newInstance(context, args, block);
            }
        }


        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                                IRubyObject arg0) {
            return Buffer.newInstance(context, self, arg0);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                                IRubyObject arg0, IRubyObject arg1) {
            return Buffer.newInstance(context, self, arg0, arg1);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name,
                                IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return Buffer.newInstance(context, self, arg0, arg1);
        }
    }

    public Buffer(Ruby runtime, RubyClass klass) {
        super(runtime, klass, runtime.getFFI().getNullMemoryIO(), 0, 0);
        this.inout = IN | OUT;
    }
    
    public Buffer(Ruby runtime, int size) {
        this(runtime, size, IN | OUT);
    }
    
    public Buffer(Ruby runtime, int size, int flags) {
        this(runtime, runtime.getFFI().bufferClass,
            allocateMemoryIO(runtime, size), size, 1, flags);
    }

    private Buffer(Ruby runtime, IRubyObject klass, MemoryIO io, long size, int typeSize, int inout) {
        super(runtime, (RubyClass) klass, io, size, typeSize);
        this.inout = inout;
    }
    
    private static final int getCount(IRubyObject countArg) {
        return countArg instanceof RubyFixnum ? RubyFixnum.fix2int(countArg) : 1;
    }
    
    private static Buffer allocate(ThreadContext context, IRubyObject recv, 
            IRubyObject sizeArg, int count, int flags) {
        final int typeSize = calculateTypeSize(context, sizeArg);
        final int total = typeSize * count;
        return new Buffer(context.getRuntime(), recv,
                allocateMemoryIO(context.getRuntime(), total), total, typeSize, flags);
    }

    private IRubyObject init(ThreadContext context, IRubyObject rbTypeSize, int count, int flags) {
        this.typeSize = calculateTypeSize(context, rbTypeSize);
        this.size = this.typeSize * count;
        this.inout = flags;
        setMemoryIO(allocateMemoryIO(context.getRuntime(), (int) this.size));

        return this;
    }

    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject klass, IRubyObject sizeArg) {
        if (klass == context.runtime.getFFI().bufferClass) {
            return allocate(context, klass, sizeArg, 1, IN | OUT);

        } else {
            return ((RubyClass) klass).newInstance(context, sizeArg, Block.NULL_BLOCK);
        }
    }

    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject klass, IRubyObject sizeArg,
                                          IRubyObject countArg) {

        if (klass == context.runtime.getFFI().bufferClass) {
            return allocate(context, klass, sizeArg, RubyFixnum.fix2int(countArg), IN | OUT);

        } else {
            return ((RubyClass) klass).newInstance(context, sizeArg, countArg, Block.NULL_BLOCK);
        }
    }

    @JRubyMethod(name = "new", meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject klass, IRubyObject sizeArg,
                                          IRubyObject countArg, IRubyObject clear) {
        if (klass == context.runtime.getFFI().bufferClass) {
            return allocate(context, klass, sizeArg, RubyFixnum.fix2int(countArg), IN | OUT);

        } else {
            return ((RubyClass) klass).newInstance(context, sizeArg, countArg, clear, Block.NULL_BLOCK);
        }
    }

    @JRubyMethod(name = "new", meta = true, rest = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject klass, IRubyObject[] args) {
        if (klass == context.runtime.getFFI().bufferClass) {
            switch (args.length) {
                case 1:
                    return newInstance(context, klass, args[0]);
                case 2:
                case 3:
                    return newInstance(context, klass, args[0], args[1]);

                default:
                    return ((RubyClass) klass).newInstance(context, args, Block.NULL_BLOCK);
            }
        } else {
            return ((RubyClass) klass).newInstance(context, args, Block.NULL_BLOCK);
        }
    }


    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject sizeArg) {
        return sizeArg instanceof RubyFixnum
                ? init(context, RubyFixnum.one(context.getRuntime()), 
                    RubyFixnum.fix2int(sizeArg), IN | OUT)
                : init(context, sizeArg, 1, IN | OUT);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject sizeArg, IRubyObject arg2) {
        return init(context, sizeArg, getCount(arg2), IN | OUT);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject sizeArg,
            IRubyObject countArg, IRubyObject clearArg) {
        return init(context, sizeArg, RubyFixnum.fix2int(countArg), IN | OUT);
    }
    
    /**
     * 
     */
    @JRubyMethod(required = 1, visibility=PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        if (this == other) {
            return this;
        }
        Buffer orig = (Buffer) other;
        this.typeSize = orig.typeSize;
        this.size = orig.size;
        this.inout = orig.inout;
        
        setMemoryIO(orig.getMemoryIO().dup());
        return this;
    }

    @JRubyMethod(name = { "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        return allocate(context, recv, sizeArg, 1, IN | OUT);
    }

    @JRubyMethod(name = { "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject arg2) {
        return allocate(context, recv, sizeArg, getCount(arg2), IN | OUT);
    }

    @JRubyMethod(name = { "alloc_inout", "__alloc_inout" }, meta = true)
    public static Buffer allocateInOut(ThreadContext context, IRubyObject recv, 
            IRubyObject sizeArg, IRubyObject countArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, RubyFixnum.fix2int(countArg), IN | OUT);
    }

    @JRubyMethod(name = { "new_in", "alloc_in", "__alloc_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv, IRubyObject arg) {       
        return allocate(context, recv, arg, 1, IN);
    }

    @JRubyMethod(name = { "new_in", "alloc_in", "__alloc_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject arg2) {
        return allocate(context, recv, sizeArg, getCount(arg2), IN);
    }

    @JRubyMethod(name = { "new_in", "alloc_in", "__alloc_in" }, meta = true)
    public static Buffer allocateInput(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject countArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, RubyFixnum.fix2int(countArg), IN);
    }

    @JRubyMethod(name = {  "new_out", "alloc_out", "__alloc_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg) {
        return allocate(context, recv, sizeArg, 1, OUT);
    }

    @JRubyMethod(name = {  "new_out", "alloc_out", "__alloc_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv, IRubyObject sizeArg, IRubyObject arg2) {
        return allocate(context, recv, sizeArg, getCount(arg2), OUT);
    }

    @JRubyMethod(name = {  "new_out", "alloc_out", "__alloc_out" }, meta = true)
    public static Buffer allocateOutput(ThreadContext context, IRubyObject recv,
            IRubyObject sizeArg, IRubyObject countArg, IRubyObject clearArg) {
        return allocate(context, recv, sizeArg, RubyFixnum.fix2int(countArg), OUT);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return RubyString.newString(context.getRuntime(),
                String.format("#<Buffer size=%d>", size));
    }

    
    public final AbstractMemory order(Ruby runtime, ByteOrder order) {
        return new Buffer(runtime, getMetaClass(),
                order.equals(getMemoryIO().order()) ? getMemoryIO() : new SwappedMemoryIO(runtime, getMemoryIO()),
                size, typeSize, inout);
    }

    protected AbstractMemory slice(Ruby runtime, long offset) {
        return new Buffer(runtime, getMetaClass(), this.io.slice(offset), this.size - offset, this.typeSize, this.inout);
    }

    protected AbstractMemory slice(Ruby runtime, long offset, long size) {
        return new Buffer(runtime, getMetaClass(), this.io.slice(offset, size), size, this.typeSize, this.inout);
    }

    protected Pointer getPointer(Ruby runtime, long offset) {
        return new Pointer(runtime, getMemoryIO().getMemoryIO(offset));
    }
    public int getInOutFlags() {
        return inout;
    }
    
    private static MemoryIO allocateMemoryIO(Ruby runtime, int size) {
        return Factory.getInstance().allocateTransientDirectMemory(runtime, size, 8, true);
    }
}
