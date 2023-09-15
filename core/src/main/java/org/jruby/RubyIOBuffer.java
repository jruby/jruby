package org.jruby;

import jnr.ffi.Platform;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyIOBuffer extends RubyObject {
    public static RubyClass createIOBufferClass(Ruby runtime) {
        RubyClass IOBuffer = runtime.getIO().defineClassUnder("Buffer", runtime.getObject(), RubyIOBuffer::new);

        IOBuffer.includeModule(runtime.getComparable());

        IOBuffer.defineAnnotatedMethods(RubyIOBuffer.class);
        IOBuffer.defineAnnotatedConstants(RubyIOBuffer.class);

        return IOBuffer;
    }

    @JRubyConstant
    public static final int PAGE_SIZE = 8196;
    @JRubyConstant
    public static final int DEFAULT_SIZE = 8196;
    @JRubyConstant
    public static final int EXTERNAL = 1;
    @JRubyConstant
    public static final int INTERNAL = 2;
    @JRubyConstant
    public static final int MAPPED = 4;
    @JRubyConstant
    public static final int SHARED = 8;
    @JRubyConstant
    public static final int LOCKED = 32;
    @JRubyConstant
    public static final int PRIVATE = 64;
    @JRubyConstant
    public static final int READONLY = 128;
    @JRubyConstant
    public static final int LITTLE_ENDIAN = 4;
    @JRubyConstant
    public static final int BIG_ENDIAN = 8;
    @JRubyConstant
    public static final int HOST_ENDIAN = Platform.getNativePlatform().isBigEndian() ? BIG_ENDIAN : LITTLE_ENDIAN;
    @JRubyConstant
    public static final int NETWORK_ENDIAN = BIG_ENDIAN;

    public static RubyIOBuffer newBuffer(Ruby runtime, byte[] base, int size, int flags) {
        return new RubyIOBuffer(runtime, runtime.getIO(), base, size, flags);
    }

    public RubyIOBuffer(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyIOBuffer(Ruby runtime, RubyClass metaClass, byte[] base, int size, int flags) {
        super(runtime, metaClass);

        this.base = base;
        this.size = size;
        this.flags = flags;
    }

    @JRubyMethod(name = "for")
    public static IRubyObject rbFor(ThreadContext context, IRubyObject self, IRubyObject string) {
        return context.nil;
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject size) {
        return context.nil;
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject size, IRubyObject flags) {
        return context.nil;
    }

    @JRubyMethod(name = "initialize_copy")
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        return context.nil;
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "hexdump")
    public IRubyObject hexdump(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "size")
    public IRubyObject size(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "valid?")
    public IRubyObject valid_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "transfer")
    public IRubyObject transfer(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "null?")
    public IRubyObject null_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "external?")
    public IRubyObject external_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "internal?")
    public IRubyObject internal_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "mapped?")
    public IRubyObject mapped_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "shared?")
    public IRubyObject shared_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "locked?")
    public IRubyObject locked_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "readonly?")
    public IRubyObject readonly_p(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "locked")
    public IRubyObject locked(ThreadContext context, Block block) {
        return context.nil;
    }

    public IRubyObject lock(ThreadContext context) {
        if ((flags & LOCKED) == LOCKED) {
            throw context.runtime.newBufferLockedError("Buffer already locked!");
        }

        flags |= LOCKED;

        return this;
    }

    public IRubyObject unlock(ThreadContext context) {
        if ((flags & LOCKED) == 0) {
            throw context.runtime.newBufferLockedError("Buffer not locked!");
        }

        flags &= ~LOCKED;

        return this;
    }

    @JRubyMethod(name = "slice")
    public IRubyObject slice(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "slice")
    public IRubyObject slice(ThreadContext context, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "slice")
    public IRubyObject slice(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return context.nil;
    }

    @JRubyMethod(name = "resize")
    public IRubyObject resize(ThreadContext context, IRubyObject size) {
        return context.nil;
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject value) {
        return context.nil;
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject value, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "clear")
    public IRubyObject clear(ThreadContext context, IRubyObject value, IRubyObject offset, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "free")
    public IRubyObject free(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "size_of")
    public static IRubyObject size_of(ThreadContext context, IRubyObject self, IRubyObject dataType) {
        return context.nil;
    }

    @JRubyMethod(name = "get_value")
    public IRubyObject get_value(ThreadContext context, IRubyObject type, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "get_values")
    public IRubyObject get_values(ThreadContext context, IRubyObject data_types, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject dataType, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject dataType, IRubyObject offset, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject dataType, IRubyObject offset, IRubyObject count, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context, IRubyObject dataType) {
        return context.nil;
    }

    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context, IRubyObject dataType, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "values")
    public IRubyObject values(ThreadContext context, IRubyObject dataType, IRubyObject offset, IRubyObject count) {
        return context.nil;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, IRubyObject offset, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte(ThreadContext context, IRubyObject offset, IRubyObject count, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "set_value")
    public IRubyObject set_value(ThreadContext context, IRubyObject dataType, IRubyObject offset, IRubyObject value) {
        return context.nil;
    }

    @JRubyMethod(name = "set_values")
    public IRubyObject set_values(ThreadContext context, IRubyObject dataTypes, IRubyObject offset, IRubyObject values) {
        return context.nil;
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source) {
        return context.nil;
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "copy")
    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject offset, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "copy", required = 1, optional = 3)
    public IRubyObject copy(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 3);

        switch (args.length) {
            case 1:
                return copy(context, args[0]);
            case 2:
                return copy(context, args[0], args[1]);
            case 3:
                return copy(context, args[0], args[1], args[2]);
            case 4:
                return copy(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject copy(ThreadContext context, IRubyObject source, IRubyObject offset, IRubyObject length, IRubyObject sourceOffset) {
        return context.nil;
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject offset, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "get_string")
    public IRubyObject get_string(ThreadContext context, IRubyObject offset, IRubyObject length, IRubyObject encoding) {
        return context.nil;
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject string) {
        return context.nil;
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject string, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "set_string")
    public IRubyObject set_string(ThreadContext context, IRubyObject string, IRubyObject offset, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "set_string", required = 1, optional = 3)
    public IRubyObject set_string(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 3);

        switch (args.length) {
            case 1:
                return set_string(context, args[0]);
            case 2:
                return set_string(context, args[0], args[1]);
            case 3:
                return set_string(context, args[0], args[1], args[2]);
            case 4:
                return set_string(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject set_string(ThreadContext context, IRubyObject string, IRubyObject offset, IRubyObject length, IRubyObject encoding) {
        return context.nil;
    }

    @JRubyMethod(name = "&")
    public IRubyObject op_and(ThreadContext context, IRubyObject mask) {
        return context.nil;
    }

    @JRubyMethod(name = "|")
    public IRubyObject op_or(ThreadContext context, IRubyObject mask) {
        return context.nil;
    }

    @JRubyMethod(name = "^")
    public IRubyObject op_xor(ThreadContext context, IRubyObject mask) {
        return context.nil;
    }

    @JRubyMethod(name = "~")
    public IRubyObject op_not(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject io, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "read")
    public IRubyObject read(ThreadContext context, IRubyObject io, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "pread")
    public IRubyObject pread(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "pread", required = 1, optional = 3)
    public IRubyObject pread(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 3, 4);

        switch (args.length) {
            case 3:
                return pread(context, args[0], args[1], args[2]);
            case 4:
                return pread(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject pread(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject io, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject io, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }
    
    @JRubyMethod(name = "pwrite")
    public IRubyObject pwrite(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length) {
        return context.nil;
    }

    @JRubyMethod(name = "pwrite", required = 1, optional = 3)
    public IRubyObject pwrite(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 3, 4);

        switch (args.length) {
            case 3:
                return pwrite(context, args[0], args[1], args[2]);
            case 4:
                return pwrite(context, args[0], args[1], args[2], args[3]);
        }

        return context.nil;
    }

    public IRubyObject pwrite(ThreadContext context, IRubyObject io, IRubyObject from, IRubyObject length, IRubyObject offset) {
        return context.nil;
    }

    private byte[] base;
    private int size;
    private int flags;
}
