package org.jruby.util.invoke;

import org.jruby.javasupport.ext.JavaLangReflect;

import java.util.List;

public class MethodType {

    private Class<?> returnType;
    private Class<?>[] parameterTypes;

    private MethodType(Class<?> returnType, Class<?>[] parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    public static MethodType methodType(Class<?> returnType) {
        Class<?>[] parameterTypes = new Class<?>[0];
        return new MethodType(returnType, parameterTypes);
    }

    public static MethodType methodType(Class<?> returnType, Class<?> parameterType) {
        Class<?>[] parameterTypes = new Class<?>[1];
        parameterTypes[0] = parameterType;
        return new MethodType(returnType, parameterTypes);
    }

    public static MethodType methodType(Class<?> returnType, Class<?>[] parameterTypes) {
        return new MethodType(returnType, parameterTypes);
    }

    public static MethodType methodType(Class<?> returnType, List<Class<?>> parameterTypes) {
        Class<?>[] parameterArray = new Class<?>[0];
        parameterArray = parameterTypes.toArray(parameterArray);
        return new MethodType(returnType, parameterArray);
    }

    public static MethodType methodType(Class<?> returnType, Class<?> param0, Class<?>... params) {
        Class<?>[] parameterArray = new Class<?>[params.length + 1];
        parameterArray[0] = param0;
        System.arraycopy(params, 0, parameterArray, 1, params.length);
        return new MethodType(returnType, parameterArray);
    }

    public Class<?> returnType() {
        return this.returnType;
    }

    public Class<?>[] parameterArray() {
        return this.parameterTypes;
    }

}
