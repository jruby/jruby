package java_integration.fixtures;

public interface SingleMethodInterface {
    public static class Caller {
        public static Object call(SingleMethodInterface obj) {
            return obj.callIt();
        }
    }
    Object callIt();
}
