package org.jruby.java.addons;

import java.lang.reflect.Array;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ArrayJavaAddons {
    @JRubyMethod
    public static IRubyObject copy_data(
            ThreadContext context, IRubyObject rubyArray, IRubyObject javaArray,
            IRubyObject fillValue) {
        JavaArray javaArrayJavaObj = (JavaArray)javaArray.dataGetStruct();
        Object fillJavaObject = null;
        int javaLength = (int)javaArrayJavaObj.length().getLongValue();
        Class targetType = javaArrayJavaObj.getComponentType();
        
        if (!fillValue.isNil()) {
            fillJavaObject = fillValue.toJava(targetType);
        }
        
        RubyArray array = null;
        int rubyLength;
        if (rubyArray instanceof RubyArray) {
            array = (RubyArray)rubyArray;
            rubyLength = ((RubyArray)rubyArray).getLength();
        } else {
            rubyLength = 0;
            fillJavaObject = rubyArray.toJava(targetType);
        }
        
        int i = 0;
        for (; i < rubyLength && i < javaLength; i++) {
            javaArrayJavaObj.setWithExceptionHandling(i, array.entry(i).toJava(targetType));
        }
        
        if (i < javaLength && fillJavaObject != null) {
            javaArrayJavaObj.fillWithExceptionHandling(i, javaLength, fillJavaObject);
        }
        
        return javaArray;
    }
    
    @JRubyMethod
    public static IRubyObject copy_data_simple(
            ThreadContext context, IRubyObject from, IRubyObject to) {
        JavaArray javaArray = (JavaArray)to.dataGetStruct();
        RubyArray rubyArray = (RubyArray)from;
        
        copyDataToJavaArray(context, rubyArray, javaArray);
        
        return to;
    }
    
    public static void copyDataToJavaArray(
            ThreadContext context, RubyArray rubyArray, JavaArray javaArray) {
        int javaLength = (int)javaArray.length().getLongValue();
        Class targetType = javaArray.getComponentType();
        
        int rubyLength = rubyArray.getLength();
        
        int i = 0;
        for (; i < rubyLength && i < javaLength; i++) {
            javaArray.setWithExceptionHandling(i, rubyArray.entry(i).toJava(targetType));
        }
    }
    
    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject maybeArray) {
        Ruby runtime = context.runtime;
        if (!(maybeArray instanceof RubyArray)) {
            return runtime.newEmptyArray();
        }
        RubyArray rubyArray = (RubyArray)maybeArray;
        RubyArray dims = runtime.newEmptyArray();
        
        return dimsRecurse(context, rubyArray, dims, 0);
    }
    
    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject maybeArray, IRubyObject dims) {
        Ruby runtime = context.runtime;
        if (!(maybeArray instanceof RubyArray)) {
            return runtime.newEmptyArray();
        }
        assert dims instanceof RubyArray;
        
        RubyArray rubyArray = (RubyArray)maybeArray;
        
        return dimsRecurse(context, rubyArray, (RubyArray)dims, 0);
    }
    
    @JRubyMethod
    public static IRubyObject dimensions(ThreadContext context, IRubyObject maybeArray, IRubyObject dims, IRubyObject index) {
        Ruby runtime = context.runtime;
        if (!(maybeArray instanceof RubyArray)) {
            return runtime.newEmptyArray();
        }
        assert dims instanceof RubyArray;
        assert index instanceof RubyFixnum;
        
        RubyArray rubyArray = (RubyArray)maybeArray;
        
        return dimsRecurse(context, rubyArray, (RubyArray)dims, (int)((RubyFixnum)index).getLongValue());
    }
    
    private static RubyArray dimsRecurse(ThreadContext context, RubyArray rubyArray, RubyArray dims, int index) {
        Ruby runtime = context.runtime;

        while (dims.size() <= index) {
            dims.append(RubyFixnum.zero(runtime));
        }
        
        if (rubyArray.size() > ((RubyFixnum)dims.eltInternal(index)).getLongValue()) {
            dims.eltInternalSet(index, RubyFixnum.newFixnum(runtime, rubyArray.size()));
        }
        
        for (int i = 0; i < rubyArray.size(); i++) {
            if (rubyArray.eltInternal(i) instanceof RubyArray) {
                dimsRecurse(context, (RubyArray)rubyArray.eltInternal(i), dims, 1);
            }
        }
        
        return dims;
    }
}
