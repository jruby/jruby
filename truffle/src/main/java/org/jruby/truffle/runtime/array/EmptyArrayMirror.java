package org.jruby.truffle.runtime.array;

public class EmptyArrayMirror extends BasicArrayMirror {

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Object get(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void set(int index, Object value) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public ArrayMirror copyArrayAndMirror(int newLength) {
        return new EmptyArrayMirror();
    }

    @Override
    public void copyTo(ArrayMirror destination, int sourceStart, int destinationStart, int count) {
        if (sourceStart > 0 || count > 0) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void copyTo(Object[] destination, int sourceStart, int destinationStart, int count) {
        if (sourceStart > 0 || count > 0) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public Object getArray() {
        return null;
    }

}
