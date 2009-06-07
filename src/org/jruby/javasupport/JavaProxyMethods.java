package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaProxyMethods {
    public static RubyModule createJavaProxyMethods(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        RubyModule javaProxyMethods = runtime.defineModule("JavaProxyMethods");
        
        javaProxyMethods.defineAnnotatedMethods(JavaProxyMethods.class);
        
        return javaProxyMethods;
    }
    
    @JRubyMethod
    public static IRubyObject java_class(ThreadContext context, IRubyObject recv) {
        return recv.getMetaClass().getRealClass().fastGetInstanceVariable("@java_class");
    }

    @JRubyMethod
    public static IRubyObject java_object(ThreadContext context, IRubyObject recv) {
        return (IRubyObject)recv.dataGetStruct();
    }

    @JRubyMethod(name = "java_object=")
    public static IRubyObject java_object_set(ThreadContext context, IRubyObject recv, IRubyObject obj) {
        // XXX: Check if it's appropriate type?
        recv.dataWrapStruct(obj);
        return obj;
    }

    @JRubyMethod(name = {"=="})
    public static IRubyObject op_equal(IRubyObject recv, IRubyObject rhs) {
        return ((JavaObject)recv.dataGetStruct()).op_equal(rhs);
    }
    
    @JRubyMethod
    public static IRubyObject to_s(IRubyObject recv) {
        if(recv.dataGetStruct() != null) {
            return ((JavaObject)recv.dataGetStruct()).to_s();
        } else {
            return ((RubyObject)recv).to_s();
        }
    }
    
    @JRubyMethod(name = "eql?")
    public static IRubyObject op_eql(IRubyObject recv, IRubyObject rhs) {
        return ((JavaObject)recv.dataGetStruct()).op_equal(rhs);
    }
    
    @JRubyMethod
    public static IRubyObject hash(IRubyObject recv) {
        return ((JavaObject)recv.dataGetStruct()).hash();
    }
    
    @JRubyMethod
    public static IRubyObject to_java_object(IRubyObject recv) {
        return (JavaObject)recv.dataGetStruct();
    }
    
    @JRubyMethod(name = "synchronized")
    public static IRubyObject rbSynchronized(ThreadContext context, IRubyObject recv, Block block) {
        return ((JavaObject)recv.dataGetStruct()).ruby_synchronized(context, block);
    }
}
