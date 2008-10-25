package java_integration.fixtures;

public class ConcreteWithVirtualCall {
    public String callVirtualMethod() {
        return virtualMethod();
    }

    public String virtualMethod() {
        return "base";
    }
}
