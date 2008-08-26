package org.jruby.java.addons;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaUtil;
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
    
    @JRubyMethod
    public static IRubyObject to_channel(ThreadContext context, IRubyObject self) {
        RubyIO io = (RubyIO)self;
        
        return JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), io.getChannel());
    }
}
