/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class JavaProxy extends RubyObject {
    public JavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    public static RubyClass createJavaProxy(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        RubyClass javaProxy = runtime.defineClass("JavaProxy", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new JavaProxy(runtime, klazz);
            }
        });
        
        RubyClass singleton = javaProxy.getSingletonClass();
        
        singleton.addReadWriteAttribute(context, "java_class");
        
        javaProxy.defineAnnotatedMethods(JavaProxy.class);
        javaProxy.includeModule(runtime.fastGetModule("JavaProxyMethods"));
        
        return javaProxy;
    }
    
    @JRubyMethod(frame = true, meta = true)
    public static IRubyObject inherited(ThreadContext context, IRubyObject recv, IRubyObject subclass) {
        IRubyObject subJavaClass = RuntimeHelpers.invoke(context, subclass, "java_class");
        if (subJavaClass.isNil()) {
            subJavaClass = RuntimeHelpers.invoke(context, recv, "java_class");
            RuntimeHelpers.invoke(context, subclass, "java_class=", subJavaClass);
        }
        return recv.callSuper(context, new IRubyObject[] {subclass}, Block.NULL_BLOCK);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject singleton_class(IRubyObject recv) {
        return ((RubyClass)recv).getSingletonClass();
    }
    
    @JRubyMethod(name = "[]", meta = true, rest = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject javaClass = RuntimeHelpers.invoke(context, recv, "java_class", CallType.VARIABLE);
        if (args.length > 0) {
            // construct new array proxy (ArrayJavaProxy)
            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            newArgs[0] = javaClass;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            return context.getRuntime().fastGetClass("ArrayJavaProxyCreator").newInstance(context, newArgs, Block.NULL_BLOCK);
        } else {
            return Java.get_proxy_class(javaClass, RuntimeHelpers.invoke(context, javaClass, "array_class"));
        }
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject new_instance_for(IRubyObject recv, IRubyObject arg0) {
        return Java.new_instance_for(recv, arg0);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject to_java_object(IRubyObject recv) {
        return Java.to_java_object(recv);
    }
}
