package org.jruby.java.addons;

import org.jruby.javasupport.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StringJavaAddons {
    @JRubyMethod
    public static IRubyObject to_java_bytes(ThreadContext context, IRubyObject self) {
        return JavaArrayUtilities.ruby_string_to_bytes(context, self, self);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject from_java_bytes(ThreadContext context, IRubyObject self, IRubyObject bytes) {
        return JavaArrayUtilities.bytes_to_ruby_string(context, bytes, bytes);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject from_java_bytes(ThreadContext context, IRubyObject self, IRubyObject bytes, IRubyObject encoding) {
        return JavaArrayUtilities.bytes_to_ruby_string(context, bytes, bytes, encoding);
    }

    @JRubyMethod
    public static IRubyObject to_java_string(ThreadContext context, IRubyObject self) {
        return Java.getInstance(context.runtime, self.convertToString().asJavaString());
    }
}
