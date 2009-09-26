package org.jruby.java.proxies;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaArray;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
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
    
    public JavaArray getJavaArray() {
        return (JavaArray)dataGetStruct();
    }
    
    @JRubyMethod(name = {"length","size"}, backtrace = true)
    public IRubyObject length() {
        return getJavaArray().length();
    }
    
    @JRubyMethod(name = "[]", backtrace = true)
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        if (arg instanceof RubyInteger) {
            int index = (int)((RubyInteger)arg).getLongValue();
            return getJavaArray().arefDirect(index);
        } else {
            return getRange(context, arg);
        }
    }

    @JRubyMethod(name = "[]", required = 1, rest = true, backtrace = true)
    public IRubyObject op_aref(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1 && args[0] instanceof RubyInteger) {
            int index = (int)((RubyInteger)args[0]).getLongValue();
            return getJavaArray().arefDirect(index);
        } else {
            return getRange(context, args);
        }
    }
    
    @JRubyMethod(name = "[]=", backtrace = true)
    public IRubyObject op_aset(ThreadContext context, IRubyObject index, IRubyObject value) {
        Object converted = value.toJava(getJavaArray().getComponentType());
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
            return getJavaArray().arefDirect((int)indexI.getLongValue());
        } else {
            return context.getRuntime().getNil();
        }
    }
    
    @JRubyMethod(name = "+", backtrace = true)
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        JavaClass arrayClass = JavaClass.get(context.getRuntime(), getJavaArray().getComponentType());
        if (other instanceof ArrayJavaProxy) {
            JavaArray otherArray = ((ArrayJavaProxy)other).getJavaArray();
            
            if (getJavaArray().getComponentType().isAssignableFrom(otherArray.getComponentType())) {
                return arrayClass.concatArrays(context, getJavaArray(), otherArray);
            }
        }
        return arrayClass.concatArrays(context, getJavaArray(), other);
    }
    
    @JRubyMethod(backtrace = true)
    public IRubyObject each(ThreadContext context, Block block) {
        int length = (int)getJavaArray().length().getLongValue();
        for (int i = 0; i < length; i++) {

            IRubyObject rubyObj = getJavaArray().arefDirect(i);
            block.yield(context, rubyObj);
        }
        return this;
    }
    
    @JRubyMethod(name = {"to_a","to_ary"}, backtrace = true)
    public IRubyObject to_a(ThreadContext context) {
        RubyModule javaArrayUtilities = context.getRuntime().getJavaSupport().getJavaArrayUtilitiesModule();
        return RuntimeHelpers.invoke(context, javaArrayUtilities, "java_to_ruby", this);
    }
    
    public IRubyObject getRange(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1) {
            return getRange(context, args[0]);
        } else if (args.length == 2) {
            return getRange(context, args[0], args[1]);
        } else {
            throw context.getRuntime().newArgumentError(args.length, 1);
        }
    }
    
    public IRubyObject getRange(ThreadContext context, IRubyObject arg0) {
        int length = (int)getJavaArray().length().getLongValue();
        JavaClass arrayClass = JavaClass.get(context.getRuntime(), getJavaArray().getComponentType());
        
        if (arg0 instanceof RubyRange) {
            RubyRange range = (RubyRange)arg0;
            if (range.first() instanceof RubyFixnum && range.last() instanceof RubyFixnum) {
                int first = (int)((RubyFixnum)range.first()).getLongValue();
                int last = (int)((RubyFixnum)range.last()).getLongValue();
                
                first = first >= 0 ? first : length + first;
                last = last >= 0 ? last : length + last;
                int newLength = last - first;
                if (range.exclude_end_p().isFalse()) newLength += 1;
                
                if (newLength <= 0) {
                    return arrayClass.emptyJavaArray(context);
                }
        
                return arrayClass.javaArraySubarray(context, getJavaArray(), first, newLength);
            } else {
                throw context.getRuntime().newTypeError("only Fixnum ranges supported");
            }
        } else {
            throw context.getRuntime().newTypeError(arg0, context.getRuntime().getRange());
        }
    }
    
    public IRubyObject getRange(ThreadContext context, IRubyObject firstObj, IRubyObject lengthObj) {
        JavaClass arrayClass = JavaClass.get(context.getRuntime(), getJavaArray().getComponentType());
        
        if (firstObj instanceof RubyFixnum && lengthObj instanceof RubyFixnum) {
            int first = (int)((RubyFixnum)firstObj).getLongValue();
            int length = (int)((RubyFixnum)lengthObj).getLongValue();

            if (length > getJavaArray().length().getLongValue()) {
                throw context.getRuntime().newIndexError("length specifed is longer than array");
            }

            first = first >= 0 ? first : (int)getJavaArray().length().getLongValue() + first;

            if (length <= 0) {
                return arrayClass.emptyJavaArray(context);
            }

            return arrayClass.javaArraySubarray(context, getJavaArray(), first, length);
        } else {
            throw context.getRuntime().newTypeError("only Fixnum ranges supported");
        }
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

    @Deprecated
    public IRubyObject at(int index) {
        return getJavaArray().at(index);
    }
}
