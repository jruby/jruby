package org.jruby.java.addons;

import org.jruby.RubyIO;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOChannel;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;

import static org.jruby.api.Error.typeError;

public class IOJavaAddons {
    // FIXME This whole thing could probably be implemented as a module and
    // mixed into appropriate classes, especially if it uses either
    // IOInput/OutputStream or is smart about the kind of IO-like object
    // it's being used against.
    
    @JRubyMethod(name = {"to_input_stream", "to_inputstream"})
    public static IRubyObject to_inputstream(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO) self;

        io.getOpenFile().checkReadable(context);

        return JavaUtil.convertJavaToUsableRubyObject(context.runtime, io.getInStream());
    }
    
    @JRubyMethod(name = {"to_output_stream", "to_outputstream"})
    public static IRubyObject to_outputstream(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO) self;

        io.getOpenFile().checkWritable(context);

        return JavaUtil.convertJavaToUsableRubyObject(context.runtime, io.getOutStream());
    }

    @JRubyMethod
    public static IRubyObject to_channel(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO) self;

        return JavaUtil.convertJavaToUsableRubyObject(context.runtime, io.getChannel());
    }

    public static class AnyIO {
        @JRubyMethod(name = {"to_input_stream", "to_inputstream"})
        public static IRubyObject any_to_inputstream(ThreadContext context, IRubyObject self) {
            // using IOInputStream may not be the most performance way, but it's easy.
            return JavaUtil.convertJavaToUsableRubyObject(context.runtime, new IOInputStream(self));
        }

        @JRubyMethod(name = {"to_output_stream", "to_outputstream"})
        public static IRubyObject any_to_outputstream(ThreadContext context, IRubyObject self) {
            // using IOOutputStream may not be the most performance way, but it's easy.
            return JavaUtil.convertJavaToUsableRubyObject(context.runtime, new IOOutputStream(self));
        }

        @JRubyMethod(name = "to_channel")
        public static IRubyObject any_to_channel(ThreadContext context, IRubyObject self) {
            // using IOChannel may not be the most performant way, but it's easy.
            IOChannel channel;
            if (self.respondsTo("read")) {
                if (self.respondsTo("write") || self.respondsTo("<<")) {
                    channel = new IOChannel.IOReadableWritableByteChannel(self);
                } else {
                    channel = new IOChannel.IOReadableByteChannel(self);
                }
            } else {
                if (self.respondsTo("write") || self.respondsTo("<<")) {
                    channel = new IOChannel.IOWritableByteChannel(self);
                } else {
                    throw typeError(context, self.inspect(context) + " does not respond to any of read, write, or <<");
                }
            }
            return JavaUtil.convertJavaToUsableRubyObject(context.runtime, channel);
        }
    }
}
