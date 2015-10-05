package java_integration.fixtures.iface;

public interface SingleMethodInterfaceWithArg {
    public static class Caller {
        public static void call(SingleMethodInterfaceWithArg iface) {
            call(42, iface);
        }
        public static <V> void call(V arg, SingleMethodInterfaceWithArg iface) {
            iface.doSome(arg);
        }
    }

    <V> void doSome(final V arg) ;
}
