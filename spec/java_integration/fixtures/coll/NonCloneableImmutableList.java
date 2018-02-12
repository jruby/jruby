package java_integration.fixtures.coll;

import java.util.*;

public final class NonCloneableImmutableList extends AbstractList<Object> {
    private final List<Integer> storage = Arrays.asList(1, 2, 3);

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public Object get(int index) {
        return storage.get(index);
    }
}
