package java_integration.fixtures.inheritance;

public class MultiCtorSubclass extends MultiCtorBase {

    public MultiCtorSubclass(String s) {
        super(s);
        getTrace().add("Java MultiCtorSubclass(String) ctor with " + s);
    }

    public MultiCtorSubclass(int i) {
        super(i);
        getTrace().add("Java MultiCtorSubclass(int) ctor with " + i);
    }

    public MultiCtorSubclass() {
        super();
        getTrace().add("Java MultiCtorSubclass() ctor");
    }
}
