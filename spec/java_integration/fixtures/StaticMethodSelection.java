package java_integration.fixtures;

class SuperClass {
    public static SuperClass produce() {
        return new SuperClass(); 
    }
}

public class StaticMethodSelection extends SuperClass {
    public static StaticMethodSelection produce() {
        return new StaticMethodSelection(); 
    }
}