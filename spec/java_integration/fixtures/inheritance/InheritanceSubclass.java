package java_integration.fixtures.inheritance;

public class InheritanceSubclass extends InheritanceBase {

    public InheritanceSubclass(String s) {
        super(s);
        getTrace().add("Java subclass constructor called with " + s);
    }

}
