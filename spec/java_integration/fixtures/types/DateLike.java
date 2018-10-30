package java_integration.fixtures.types;

public class DateLike extends java.util.Date {

    public DateLike() { super(); }

    public Object inspect() {
        return new StringBuilder("inspect:").append(System.identityHashCode(this));
    }

}
