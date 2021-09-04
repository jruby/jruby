package java_integration.fixtures.coll;

import java.util.*;

public final class NonCloneableImmutableList3 extends AbstractList<Object> {

    public static final NonCloneableImmutableList3 INSTANCE = new NonCloneableImmutableList3();

    private final List<Object> storage = Arrays.asList(1);

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public Object get(int index) {
        return storage.get(index);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("no instances"); // ignored since Cloneable not implemented
    }

}
