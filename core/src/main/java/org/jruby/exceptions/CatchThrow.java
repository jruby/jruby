package org.jruby.exceptions;

import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A flow-control exception used internally for Ruby's catch/throw functionality.
 */
public class CatchThrow extends RuntimeException implements Unrescuable {
    public IRubyObject[] args = IRubyObject.NULL_ARRAY;
    public final IRubyObject tag;

    public CatchThrow() {
        tag = null;
    }

    public CatchThrow(IRubyObject tag) {
        this.tag = tag;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public static IRubyObject enter(ThreadContext context, IRubyObject yielded, Block block) {
        CatchThrow catchThrow = new CatchThrow(yielded);
        context.pushCatch(catchThrow);
        try {
            return block.yield(context, yielded);
        } catch (CatchThrow c) {
            if (c != catchThrow) throw c;

            IRubyObject[] args = catchThrow.args;
            switch (args.length) {
                case 0:
                    return context.nil;
                case 1:
                    return args[0];
                default:
                    return RubyArray.newArrayMayCopy(context.runtime, args);
            }
        } finally {
            context.popCatch();
        }
    }
}
