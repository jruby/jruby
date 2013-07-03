package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaProxyMethods {
    public static RubyModule createJavaProxyMethods(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyModule javaProxyMethods = runtime.defineModule("JavaProxyMethods");
        
        javaProxyMethods.defineAnnotatedMethods(JavaProxyMethods.class);
        
        return javaProxyMethods;
    }
    
    @JRubyMethod
    public static IRubyObject java_class(ThreadContext context, IRubyObject recv) {
        return recv.getMetaClass().getRealClass().getInstanceVariable("@java_class");
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
        if (recv instanceof JavaProxy) {
            return JavaObject.op_equal((JavaProxy)recv, rhs);
        }
        return ((JavaObject)recv.dataGetStruct()).op_equal(rhs);
    }
    
    @JRubyMethod
    public static IRubyObject to_s(IRubyObject recv) {
        if (recv instanceof JavaProxy) {
            return JavaObject.to_s(recv.getRuntime(), ((JavaProxy)recv).getObject());
        } else if (recv.dataGetStruct() != null) {
            return ((JavaObject)recv.dataGetStruct()).to_s();
        } else {
            return ((RubyObject)recv).to_s();
        }
    }

    @JRubyMethod
    public static IRubyObject inspect(IRubyObject recv) {
        if (recv instanceof RubyBasicObject) {
            return ((RubyBasicObject)recv).hashyInspect();
        } else {
            return recv.inspect();
        }
    }
    
    @JRubyMethod(name = "eql?")
    public static IRubyObject op_eql(IRubyObject recv, IRubyObject rhs) {
        return op_equal(recv, rhs);
    }
    
    @JRubyMethod
    public static IRubyObject hash(IRubyObject recv) {
        if (recv instanceof JavaProxy) {
            return RubyFixnum.newFixnum(recv.getRuntime(), ((JavaProxy)recv).getObject().hashCode());
        }
        return ((JavaObject)recv.dataGetStruct()).hash();
    }

    @JRubyMethod
    public static IRubyObject to_java_object(IRubyObject recv) {
        return (JavaObject)recv.dataGetStruct();
    }
    
    @JRubyMethod(name = "synchronized")
    public static IRubyObject rbSynchronized(ThreadContext context, IRubyObject recv, Block block) {
        if (recv instanceof JavaProxy) {
            return JavaObject.ruby_synchronized(context, ((JavaProxy)recv).getObject(), block);
        }
        return ((JavaObject)recv.dataGetStruct()).ruby_synchronized(context, block);
    }
}
