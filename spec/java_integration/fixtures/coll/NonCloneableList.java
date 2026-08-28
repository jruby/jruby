package java_integration.fixtures.coll;

import java.util.*;

public class NonCloneableList extends AbstractList<Object> {

    final List<Object> storage = new ArrayList();

    public NonCloneableList() {} // newInstance + addAll works for dup

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public Object get(int index) {
        return storage.get(index);
    }

    @Override
    public boolean add(Object e) {
        storage.add(e);
        return true;
    }

}
