package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class ArrayJavaProxy extends JavaProxy {
    public ArrayJavaProxy(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    public static RubyClass createArrayJavaProxy(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        
        RubyClass arrayJavaProxy = runtime.defineClass("ArrayJavaProxy",
                runtime.getJavaSupport().getJavaProxyClass(),
                new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new ArrayJavaProxy(runtime, klazz);
            }
        });
        
        RubyClass singleton = arrayJavaProxy.getSingletonClass();
        
        final DynamicMethod oldNew = singleton.searchMethod("new");
        
        singleton.addMethod("new", new ArrayNewMethod(singleton, Visibility.PUBLIC, oldNew));
        
        arrayJavaProxy.defineAnnotatedMethods(ArrayJavaProxy.class);
        arrayJavaProxy.includeModule(runtime.getEnumerable());
        
        return arrayJavaProxy;
    }
    
    private JavaArray getJavaArray() {
        return (JavaArray)dataGetStruct();
    }
    
    @JRubyMethod(name = {"length","size"}, backtrace = true)
    public IRubyObject length() {
        return getJavaArray().length();
    }
    
    @JRubyMethod(name = "[]", required = 1, rest = true, backtrace = true)
    public IRubyObject op_aref(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1 && args[0] instanceof RubyInteger) {
            return JavaUtil.java_to_ruby(context.getRuntime(), getJavaArray().aref(args[0]));
        } else {
            RubyModule javaArrayUtilities = context.getRuntime().getJavaSupport().getJavaArrayUtilitiesModule();
            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            System.arraycopy(args, 0, newArgs, 1, args.length);
            newArgs[0] = this;
            return RuntimeHelpers.invoke(context, javaArrayUtilities, "get_range", newArgs);
        }
    }
    
    @JRubyMethod(name = "[]=", backtrace = true)
    public IRubyObject op_aset(ThreadContext context, IRubyObject index, IRubyObject value) {
        Object converted = getJavaArray().getRubyConverter().convert(context, value);
        getJavaArray().setWithExceptionHandling((int)index.convertToInteger().getLongValue(), converted);
        return value;
    }
    
    @JRubyMethod(backtrace = true)
    public IRubyObject at(ThreadContext context, IRubyObject indexObj) {
        RubyFixnum lengthF = getJavaArray().length();
        RubyInteger indexI = indexObj.convertToInteger();
        
        if (indexI.getLongValue() < 0) {
            indexI = RubyFixnum.newFixnum(context.getRuntime(), indexI.getLongValue() + lengthF.getLongValue());
        }
        long index = indexI.getLongValue();
        
        if (index >= 0 && index < lengthF.getLongValue()) {
            return JavaUtil.java_to_ruby(context.getRuntime(), getJavaArray().aref(indexI));
        } else {
            return context.getRuntime().getNil();
        }
    }
    
    public IRubyObject at(int index) {
        return getJavaArray().at(index);
    }
    
    @JRubyMethod(name = "+", backtrace = true)
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        RubyModule javaArrayUtilities = context.getRuntime().getJavaSupport().getJavaArrayUtilitiesModule();
        return RuntimeHelpers.invoke(context, javaArrayUtilities, "concatenate", this, other);
    }
    
    @JRubyMethod(backtrace = true)
    public IRubyObject each(ThreadContext context, Block block) {
        int length = (int)getJavaArray().length().getLongValue();
        for (int i = 0; i < length; i++) {
            block.yield(context, JavaUtil.java_to_ruby(context.getRuntime(), at(i)));
        }
        return this;
    }
    
    @JRubyMethod(name = {"to_a","to_ary"}, backtrace = true)
    public IRubyObject to_a(ThreadContext context) {
        RubyModule javaArrayUtilities = context.getRuntime().getJavaSupport().getJavaArrayUtilitiesModule();
        return RuntimeHelpers.invoke(context, javaArrayUtilities, "java_to_ruby", this);
    }
    
    public static class ArrayNewMethod extends org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOne {
        private DynamicMethod oldNew;
        
        public ArrayNewMethod(RubyModule implClass, Visibility visibility, DynamicMethod oldNew) {
            super(implClass, visibility);
            this.oldNew = oldNew;
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            Ruby runtime = context.getRuntime();
            IRubyObject proxy = oldNew.call(context, self, clazz, "new_proxy");
            
            if (arg0 instanceof JavaArray) {
                proxy.dataWrapStruct(arg0);
                return proxy;
            } else {
                throw runtime.newTypeError(arg0, runtime.getJavaSupport().getJavaArrayClass());
            }
        }
    }
}
