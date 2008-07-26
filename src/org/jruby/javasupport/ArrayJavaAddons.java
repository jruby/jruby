package org.jruby.javasupport;

import org.jruby.RubyArray;
import org.jruby.anno.JRubyMethod;
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
        JavaUtil.RubyConverter converter = JavaUtil.getArrayConverter(targetType);
        
        if (!fillValue.isNil()) {
            fillJavaObject = converter.convert(context, fillValue);
        }
        
        RubyArray array = null;
        int rubyLength;
        if (rubyArray instanceof RubyArray) {
            array = (RubyArray)rubyArray;
            rubyLength = ((RubyArray)rubyArray).getLength();
        } else {
            rubyLength = 0;
            fillJavaObject = converter.convert(context, rubyArray);
        }
        
        int i = 0;
        for (; i < rubyLength && i < javaLength; i++) {
            javaArrayJavaObj.setWithExceptionHandling(i, converter.convert(context, array.entry(i)));
        }
        
        if (i < javaLength && fillJavaObject != null) {
            javaArrayJavaObj.fillWithExceptionHandling(i, javaLength, fillJavaObject);
        }
        
        return javaArray;
    }
}
