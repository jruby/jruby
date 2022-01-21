package java_integration.fixtures;

/**
 * Reopened Java subclasses should super normally into the parent class, not using reified subclass logic.
 *
 * See jruby/jruby#6968
 */
public class SubclassOfClassWithSimpleMethod extends ClassWithSimpleMethod {
}
