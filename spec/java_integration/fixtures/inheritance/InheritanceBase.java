package java_integration.fixtures.inheritance;

import java.util.ArrayList;

public class InheritanceBase {
    private final ArrayList<String> trace = new ArrayList<String>();

    public ArrayList<String> getTrace() {
        return trace;
    }

    public InheritanceBase(String s) {
        getTrace().add("Java base constructor called with " + s);
    }

    public InheritanceBase() {
        this("no args");
    }

}
