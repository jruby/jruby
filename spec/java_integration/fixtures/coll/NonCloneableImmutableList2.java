package java_integration.fixtures.coll;

import java.util.*;

public final class NonCloneableImmutableList2 extends AbstractList<Object> {

    public static final NonCloneableImmutableList2 INSTANCE = new NonCloneableImmutableList2(true);

    private final List<Object> storage = Arrays.asList(1);

    public NonCloneableImmutableList2() {
        throw new IllegalStateException("no instances");
    }

    private NonCloneableImmutableList2(boolean hidden) {
        // no-op
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public Object get(int index) {
        return storage.get(index);
    }
}
