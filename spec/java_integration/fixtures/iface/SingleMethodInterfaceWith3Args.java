package java_integration.fixtures.iface;

public interface SingleMethodInterfaceWith3Args {
    public static class Caller {
        public static Object call(SingleMethodInterfaceWith3Args iface) {
            return iface.doIt("alpha", 1, true);
        }
    }

    <T> Object doIt(final T arg1, Integer arg2, Boolean arg3);
}
