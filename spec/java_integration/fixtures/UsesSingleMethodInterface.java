package java_integration.fixtures;

import java.util.concurrent.Callable;

public class UsesSingleMethodInterface {
    public Object result;
    public UsesSingleMethodInterface() {
    }
    
    // coercion during constructor call
    public UsesSingleMethodInterface(SingleMethodInterface obj) {
        result = obj.callIt();
    }
    public UsesSingleMethodInterface(Object a, SingleMethodInterface obj) {
        result = obj.callIt();
    }
    public UsesSingleMethodInterface(Object a, Object b, SingleMethodInterface obj) {
        result = obj.callIt();
    }
    public UsesSingleMethodInterface(Object a, Object b, Object c, SingleMethodInterface obj) {
        result = obj.callIt();
    }
    // 3 normal args is our cutoff for specific-arity optz, so test four
    public UsesSingleMethodInterface(Object a, Object b, Object c, Object d, SingleMethodInterface obj) {
        result = obj.callIt();
    }
    
    // coercion during static call
    public static Object callIt(SingleMethodInterface obj) {
        return obj.callIt();
    }
    public static Object callIt(Object a, SingleMethodInterface obj) {
        return obj.callIt();
    }
    public static Object callIt(Object a, Object b, SingleMethodInterface obj) {
        return obj.callIt();
    }
    public static Object callIt(Object a, Object b, Object c, SingleMethodInterface obj) {
        return obj.callIt();
    }
    // 3 normal args is our cutoff for specific-arity optz, so test four
    public static Object callIt(Object a, Object b, Object c, Object d, SingleMethodInterface obj) {
        return obj.callIt();
    }
    public static Object castAndCallIt(Object obj) {
        return callIt((SingleMethodInterface) obj);
    }
    
    // coercion during instance call
    public Object callIt2(SingleMethodInterface obj) {
        return obj.callIt();
    }
    public Object callIt2(Object a, SingleMethodInterface obj) {
        return obj.callIt();
    }
    public Object callIt2(Object a, Object b, SingleMethodInterface obj) {
        return obj.callIt();
    }
    public Object callIt2(Object a, Object b, Object c, SingleMethodInterface obj) {
        return obj.callIt();
    }
    // 3 normal args is our cutoff for specific-arity optz, so test four
    public Object callIt2(Object a, Object b, Object c, Object d, SingleMethodInterface obj) {
        return obj.callIt();
    }

    public SingleMethodInterface callIt3(Callable callable) throws Exception {
        return (SingleMethodInterface) callable.call();
    }
    public static int hashCode(Object obj) {
        return obj.hashCode();
    }

    public static String toString(Object obj) {
        return obj.toString();
    }
}
