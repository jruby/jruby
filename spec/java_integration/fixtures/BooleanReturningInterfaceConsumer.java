package java_integration.fixtures;

public class BooleanReturningInterfaceConsumer {
    public boolean consume(BooleanReturningInterface i) {
        if (i.bar()) {
            return true;
        }
        return false;
    }
}
