package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaProxyMethods {
    public static RubyModule createJavaProxyMethods(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        RubyModule javaProxyMethods = runtime.defineModule("JavaProxyMethods");
        
        javaProxyMethods.addReadWriteAttribute(context, "java_object");
        
        javaProxyMethods.defineAnnotatedMethods(JavaProxyMethods.class);
        
        return javaProxyMethods;
    }
    
    @JRubyMethod
    public static IRubyObject java_class(ThreadContext context, IRubyObject recv) {
        RubyClass metaClass = recv.getMetaClass();

        if (metaClass.isSingleton()) {
            metaClass = metaClass.getRealClass();
        }

        // TODO: can't we dig this out without a method call?
        return RuntimeHelpers.invoke(context, metaClass, "java_class");
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
