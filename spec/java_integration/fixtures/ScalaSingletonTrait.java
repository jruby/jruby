package java_integration.fixtures;

/**
 * This simulates a the trait side of a singleton/trait pair.
 * object ScalaSingletonTrait { def hello = "Hello" }
 * trait ScalaSingletonTrait { def hello: String}
 */
public interface ScalaSingletonTrait {
    String hello();
}