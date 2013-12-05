package java_integration.fixtures;

/**
 * This simulates a the object side of a singleton/trait pair.
 * object ScalaSingletonTrait { def hello = "Hello" }
 * trait ScalaSingletonTrait { def hello: String}
 */
 
public final class ScalaSingletonTrait$ {
    public static final ScalaSingletonTrait$ MODULE$ = new ScalaSingletonTrait$();
    
    public String hello() {
        return "Hello";
    }
}