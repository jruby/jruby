package org.jruby.ext.io.nonblock;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.ioClass;
import static org.jruby.api.Convert.asBoolean;

public class IONonBlock {

    public static void load(Ruby runtime) {
        var context = runtime.getCurrentContext();
        ioClass(context).defineMethods(context, IONonBlock.class);
    }

    @JRubyMethod(name = "nonblock?")
    public static IRubyObject nonblock_p(ThreadContext context, IRubyObject io) {
        return asBoolean(context, !getIO(io).getBlocking());
    }

    @JRubyMethod(name = "nonblock=")
    public static IRubyObject nonblock_set(ThreadContext context, IRubyObject io, IRubyObject nonblocking) {
        final boolean nonblock = nonblocking.isTrue();
        getIO(io).setBlocking(!nonblock);
        return asBoolean(context, nonblock); // NOTE: MRI seems to return io
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
        return asBoolean(context, oldBlocking);
    }

    private static RubyIO getIO(IRubyObject io) {
        return (RubyIO) io;
    }

}
