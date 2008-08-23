package java_integration.fixtures;

public class ArrayReturningInterfaceConsumer {
    public Object[] eat(ArrayReturningInterface foo) {
	return foo.blah();
    }
}
