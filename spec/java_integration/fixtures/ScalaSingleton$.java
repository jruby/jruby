package java_integration.fixtures;

/**
 * This simulates the object side of a Scala singleton/object pair
 * object ScalaSingleton { def hello = "Hello" }
 * class ScalaSingleton { def hello = "Goodbye" }
 */
 
public final class ScalaSingleton$ {
    public static final ScalaSingleton$ MODULE$ = new ScalaSingleton$();
    
    public String hello() {
        return "Hello";
    }
}