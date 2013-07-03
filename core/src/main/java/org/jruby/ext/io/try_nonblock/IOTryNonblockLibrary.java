package org.jruby.ext.io.try_nonblock;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

/**
 * Adds try_read_nonblock and try_write_nonblock to IO.
 */
public class IOTryNonblockLibrary implements Library {
    @Override
    public void load(Ruby runtime, boolean wrap) throws IOException {
        runtime.getIO().defineAnnotatedMethods(IOTryNonblockLibrary.class);
    }
    
    @JRubyMethod(name = "try_read_nonblock", required = 1, optional = 1)
    public static IRubyObject try_read_nonblock(ThreadContext context, IRubyObject io, IRubyObject[] args) {
        return ((RubyIO)io).doReadNonblock(context, args, false);
    }
    
    @JRubyMethod(name = "try_write_nonblock", required = 1)
    public static IRubyObject try_write_nonblock(ThreadContext context, IRubyObject io, IRubyObject obj) {
        return ((RubyIO)io).doWriteNonblock(context, obj, false);
    }
}
