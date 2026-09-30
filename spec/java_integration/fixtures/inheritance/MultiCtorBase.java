package java_integration.fixtures.inheritance;

import java.util.ArrayList;

/**
 * Fixture for exercising split-constructor selection across multiple overloads.
 *
 * Used by inheritance_invalidation_spec.rb and inheritance_variants_spec.rb to validate that
 * the SplitCtorPlan caches resolve to the correct Java super constructor for each Ruby call
 * shape and that those caches stay valid across class generation changes.
 */
public class MultiCtorBase {
    private final ArrayList<String> trace = new ArrayList<String>();
    public final String ctor;

    public ArrayList<String> getTrace() {
        return trace;
    }

    public MultiCtorBase() {
        this.ctor = "()";
        trace.add("Java () ctor");
    }

    public MultiCtorBase(String s) {
        this.ctor = "(String)";
        trace.add("Java (String) ctor with " + s);
    }

    public MultiCtorBase(int i) {
        this.ctor = "(int)";
        trace.add("Java (int) ctor with " + i);
    }

    public MultiCtorBase(String s, int i) {
        this.ctor = "(String,int)";
        trace.add("Java (String,int) ctor with " + s + "," + i);
    }

    public MultiCtorBase(int a, int b, int c) {
        this.ctor = "(int,int,int)";
        trace.add("Java (int,int,int) ctor with " + a + "," + b + "," + c);
    }
}
