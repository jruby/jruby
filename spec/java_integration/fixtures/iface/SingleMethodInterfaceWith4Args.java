package java_integration.fixtures.iface;

public interface SingleMethodInterfaceWith4Args {
    public static class Caller {
        public static Object[] call(SingleMethodInterfaceWith4Args iface) {
            return iface.doIt(Caller.class, "hello", "world", 42);
        }
    }

    <T> Object[] doIt(final T arg1, String arg2, Object arg3, int arg4) ;
}
