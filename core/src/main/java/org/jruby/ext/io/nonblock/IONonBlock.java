package org.jruby.ext.io.nonblock;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.OpenFile;

public class IONonBlock {

    public static void load(Ruby runtime) {
        runtime.getIO().defineAnnotatedMethods(IONonBlock.class);
    }

    @JRubyMethod(name = "nonblock?")
    public static IRubyObject nonblock_p(ThreadContext context, IRubyObject io) {
        return context.runtime.newBoolean( !getIO(io).getBlocking() );
    }

    @JRubyMethod(name = "nonblock=")
    public static IRubyObject nonblock_set(ThreadContext context, IRubyObject io, IRubyObject nonblocking) {
        final boolean nonblock = nonblocking.isTrue();
        getIO(io).setBlocking(!nonblock);
        return context.runtime.newBoolean(nonblock); // NOTE: MRI seems to return io
    }

    @JRubyMethod(name = "nonblock")
    public static IRubyObject nonblock(ThreadContext context, IRubyObject io, Block block) {
        return nonblock(context, io, context.tru, block); // nonblocking = true
    }

    @JRubyMethod(name = "nonblock")
    public static IRubyObject nonblock(ThreadContext context, IRubyObject io, IRubyObject nonblocking, Block block) {
        final RubyIO ioObj = getIO(io);
        final boolean oldBlocking = ioObj.getBlocking();
        ioObj.setBlocking(!nonblocking.isTrue());
        if (block.isGiven()) {
            try {
                block.yield(context, io);
            } finally {
                ioObj.setBlocking(oldBlocking);
            }
        }
        return context.runtime.newBoolean(oldBlocking);
    }

    private static RubyIO getIO(IRubyObject io) {
        return (RubyIO) io;
    }

}
