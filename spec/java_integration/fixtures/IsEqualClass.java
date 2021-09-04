package java_integration.fixtures;

public class IsEqualClass {
    private boolean called = false;
    public boolean getCalled() {
        return called;
    }
    public boolean isEqual(IsEqualClass other) {
        called = true;
        return true;
    }
}