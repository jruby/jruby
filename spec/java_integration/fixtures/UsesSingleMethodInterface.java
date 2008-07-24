package java_integration.fixtures;

public class UsesSingleMethodInterface {
    public static Object callIt(SingleMethodInterface obj) {
        return obj.callIt();
    }
    public static Object castAndCallIt(Object obj) {
        return callIt((SingleMethodInterface) obj);
    }
    
    public Object callIt2(SingleMethodInterface obj) {
        return obj.callIt();
    }
}