package org.jruby.java.addons;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.javasupport.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.PipeException;

public class IOJavaAddons {
    @JRubyMethod
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
    
    @JRubyMethod
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
    
    @JRubyMethod(meta = true)
    public static IRubyObject from_inputstream(ThreadContext context, IRubyObject self, IRubyObject stream) {
        Ruby runtime = context.getRuntime();
        Object obj = JavaUtil.unwrapJavaObject(stream);
        if (!(obj instanceof InputStream)) {
            throw runtime.newTypeError(stream, (RubyClass)Java.getProxyClass(runtime, JavaClass.get(runtime, InputStream.class)));
        }
        return new RubyIO(context.getRuntime(), (InputStream)obj);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject from_outputstream(ThreadContext context, IRubyObject self, IRubyObject stream) {
        Ruby runtime = context.getRuntime();
        Object obj = JavaUtil.unwrapJavaObject(stream);
        if (!(obj instanceof OutputStream)) {
            throw runtime.newTypeError(stream, (RubyClass)Java.getProxyClass(runtime, JavaClass.get(runtime, OutputStream.class)));
        }
        return new RubyIO(context.getRuntime(), (OutputStream)obj);
    }
    
    @JRubyMethod
    public static IRubyObject to_channel(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO)self;
        
        return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), io.getChannel());
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject from_channel(ThreadContext context, IRubyObject self, IRubyObject stream) {
        Ruby runtime = context.getRuntime();
        Object obj = JavaUtil.unwrapJavaObject(stream);
        if (!(obj instanceof Channel)) {
            throw runtime.newTypeError(stream, (RubyClass)Java.getProxyClass(runtime, JavaClass.get(runtime, Channel.class)));
        }
        return new RubyIO(context.getRuntime(), (Channel)obj);
    }
}
