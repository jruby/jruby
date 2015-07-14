package java_integration.fixtures.iface;

public interface SingleMethodIfaceWith2Args {
    public static class Caller {
        public static void call(SingleMethodIfaceWith2Args iface) {
            iface.doSome("hello", 42);
        }
    }

    <V> void doSome(final String arg1, final V arg2);
}
