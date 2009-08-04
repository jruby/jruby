package org.jruby.java.addons;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOChannel;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.PipeException;

public class IOJavaAddons {
    // FIXME This whole thing could probably be implemented as a module and
    // mixed into appropriate classes, especially if it uses either
    // IOInput/OutputStream or is smart about the kind of IO-like object
    // it's being used against.
    
    @JRubyMethod(frame = true)
    public static IRubyObject to_inputstream(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO)self;
        Ruby runtime = context.getRuntime();

        try {
            io.getOpenFile().checkReadable(context.getRuntime());
        } catch (PipeException pe) {
            throw runtime.newErrnoEPIPEError();
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw runtime.newErrnoEINVALError();
        }
        
        return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), io.getInStream());
    }
    
    @JRubyMethod(frame = true)
    public static IRubyObject to_outputstream(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO)self;
        Ruby runtime = context.getRuntime();

        try {
            io.getOpenFile().checkWritable(context.getRuntime());
        } catch (PipeException pe) {
            throw runtime.newErrnoEPIPEError();
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw runtime.newErrnoEBADFError();
        } catch (InvalidValueException e) {
            throw runtime.newErrnoEINVALError();
        }
        
        return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), io.getOutStream());
    }

    @JRubyMethod(frame = true)
    public static IRubyObject to_channel(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO)self;

        return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), io.getChannel());
    }

    public static class AnyIO {
        @JRubyMethod(name = "to_inputstream", frame = true)
        public static IRubyObject any_to_inputstream(ThreadContext context, IRubyObject self) {
            // using IOInputStream may not be the most performance way, but it's easy.
            return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), new IOInputStream(self));
        }

        @JRubyMethod(name = "to_outputstream", frame = true)
        public static IRubyObject any_to_outputstream(ThreadContext context, IRubyObject self) {
            // using IOOutputStream may not be the most performance way, but it's easy.
            return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), new IOOutputStream(self));
        }

        @JRubyMethod(name = "to_channel", frame = true)
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
                    throw context.getRuntime().newTypeError("object does not respond to any of read, write, or <<");
                }
            }
            return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), channel);
        }
    }
}
