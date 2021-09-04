package java_integration.fixtures;

/**
 * This simulates a the class side of a singleton/class pair.
 * object ScalaSingleton { def hello = "Hello" }
 * class ScalaSingleton { def hello = "Goodbye" }
 */
public final class ScalaSingleton {
    public String hello() {
        return "Goodbye";
    }
}