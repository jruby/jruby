package org.jruby.java.proxies;

import java.lang.reflect.Array;
import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.util.ArrayUtils;
import org.jruby.javasupport.JavaArray;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class ArrayJavaProxy extends JavaProxy {
    private final JavaUtil.JavaConverter converter;
    
    public ArrayJavaProxy(Ruby runtime, RubyClass klazz, Object ary) {
        this(runtime, klazz, ary, JavaUtil.getJavaConverter(ary.getClass().getComponentType()));
    }
    
    public ArrayJavaProxy(Ruby runtime, RubyClass klazz, Object ary, JavaUtil.JavaConverter converter) {
        super(runtime, klazz, ary);
        this.converter = converter;
    }
    
    public static RubyClass createArrayJavaProxy(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        RubyClass arrayJavaProxy = runtime.defineClass("ArrayJavaProxy",
                runtime.getJavaSupport().getJavaProxyClass(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        
        RubyClass singleton = arrayJavaProxy.getSingletonClass();
        
        final DynamicMethod oldNew = singleton.searchMethod("new");
        
        singleton.addMethod("new", new ArrayNewMethod(singleton, Visibility.PUBLIC, oldNew));
        
        arrayJavaProxy.defineAnnotatedMethods(ArrayJavaProxy.class);
        arrayJavaProxy.includeModule(runtime.getEnumerable());
        
        return arrayJavaProxy;
    }
    
    public JavaArray getJavaArray() {
        JavaArray javaArray = (JavaArray)dataGetStruct();
        
        if (javaArray == null) {
            javaArray = new JavaArray(getRuntime(), getObject());
            dataWrapStruct(javaArray);
        }
        
        return javaArray;
    }
    
    @JRubyMethod(name = {"length","size"})
    public IRubyObject length(ThreadContext context) {
        return context.runtime.newFixnum(Array.getLength(this.getObject()));
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty(ThreadContext context) {
        return context.runtime.newBoolean(Array.getLength(this.getObject()) == 0);
    }
    
    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        if (arg instanceof RubyInteger) {
            int index = (int)((RubyInteger)arg).getLongValue();
            return ArrayUtils.arefDirect(context.runtime, getObject(), converter, index);
        } else {
            return getRange(context, arg);
        }
    }

    @JRubyMethod(name = "[]", required = 1, rest = true)
    public IRubyObject op_aref(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1 && args[0] instanceof RubyInteger) {
            int index = (int)((RubyInteger)args[0]).getLongValue();
            return ArrayUtils.arefDirect(context.runtime, getObject(), converter, index);
        } else {
            return getRange(context, args);
        }
    }
    
    @JRubyMethod(name = "[]=")
    public IRubyObject op_aset(ThreadContext context, IRubyObject index, IRubyObject value) {
        ArrayUtils.asetDirect(context.runtime, getObject(), converter, (int)((RubyInteger)index).getLongValue(), value);
        return value;
    }
    
    @JRubyMethod
    public IRubyObject at(ThreadContext context, IRubyObject indexObj) {
        Ruby runtime = context.runtime;
        Object array = getObject();
        int length = Array.getLength(array);
        long index = indexObj.convertToInteger().getLongValue();
        
        if (index < 0) {
            index = index + length;
        }
        
        if (index >= 0 && index < length) {
            return ArrayUtils.arefDirect(runtime, array, converter, (int)index);
        } else {
            return context.nil;
        }
    }
    
    @JRubyMethod(name = "+")
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof ArrayJavaProxy) {
            Object otherArray = ((ArrayJavaProxy)other).getObject();
            
            if (getObject().getClass().getComponentType().isAssignableFrom(otherArray.getClass().getComponentType())) {
                return ArrayUtils.concatArraysDirect(context, getObject(), otherArray);
            }
        }
        return ArrayUtils.concatArraysDirect(context, getObject(), other);
    }
    
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        int length = Array.getLength(getObject());

        for (int i = 0; i < length; i++) {
            IRubyObject rubyObj = ArrayUtils.arefDirect(runtime, getObject(), converter, i);
            block.yield(context, rubyObj);
        }
        return this;
    }
    
    @JRubyMethod(name = {"to_a","to_ary"})
    public IRubyObject to_a(ThreadContext context) {
        return JavaUtil.convertJavaArrayToRubyWithNesting(context, this.getObject());
    }
    
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        StringBuffer buffer = new StringBuffer();
        Class componentClass = getObject().getClass().getComponentType();
        
        buffer.append(componentClass.getName());
        
        if (componentClass.isPrimitive()) {
            switch (componentClass.getName().charAt(0)) {
                case 'b':
                    if (componentClass == byte.class) buffer.append(Arrays.toString((byte[])getObject()));
                    if (componentClass == boolean.class) buffer.append(Arrays.toString((boolean[])getObject()));
                    break;
                case 's':
                    if (componentClass == short.class) buffer.append(Arrays.toString((short[])getObject()));
                    break;
                case 'c':
                    if (componentClass == char.class) buffer.append(Arrays.toString((char[])getObject()));
                    break;
                case 'i':
                    if (componentClass == int.class) buffer.append(Arrays.toString((int[])getObject()));
                    break;
                case 'l':
                    if (componentClass == long.class) buffer.append(Arrays.toString((long[])getObject()));
                    break;
                case 'f':
                    if (componentClass == float.class) buffer.append(Arrays.toString((float[])getObject()));
                    break;
                case 'd':
                    if (componentClass == double.class) buffer.append(Arrays.toString((double[])getObject()));
                    break;
            }
        } else {
            buffer.append(Arrays.toString((Object[])getObject()));
        }
        buffer.append('@')
                .append(Integer.toHexString(inspectHashCode()));
        return context.runtime.newString(buffer.toString());        
    }
    
    public IRubyObject getRange(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1) {
            return getRange(context, args[0]);
        } else if (args.length == 2) {
            return getRange(context, args[0], args[1]);
        } else {
            throw context.runtime.newArgumentError(args.length, 1);
        }
    }
    
    public IRubyObject getRange(ThreadContext context, IRubyObject arg0) {
        int length = Array.getLength(getObject());
        
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
                    return ArrayUtils.emptyJavaArrayDirect(context, getObject().getClass().getComponentType());
                }
        
                return ArrayUtils.javaArraySubarrayDirect(context, getObject(), first, newLength);
            } else {
                throw context.runtime.newTypeError("only Fixnum ranges supported");
            }
        } else {
            throw context.runtime.newTypeError(arg0, context.runtime.getRange());
        }
    }
    
    public IRubyObject getRange(ThreadContext context, IRubyObject firstObj, IRubyObject lengthObj) {
        if (firstObj instanceof RubyFixnum && lengthObj instanceof RubyFixnum) {
            int first = (int)((RubyFixnum)firstObj).getLongValue();
            int length = (int)((RubyFixnum)lengthObj).getLongValue();

            if (length > Array.getLength(getObject())) {
                throw context.runtime.newIndexError("length specifed is longer than array");
            }

            first = first >= 0 ? first : Array.getLength(getObject()) + first;

            if (length <= 0) {
                return ArrayUtils.emptyJavaArrayDirect(context, getObject().getClass().getComponentType());
            }

            return ArrayUtils.javaArraySubarrayDirect(context, getObject(), first, length);
        } else {
            throw context.runtime.newTypeError("only Fixnum ranges supported");
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
            Ruby runtime = context.runtime;
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
