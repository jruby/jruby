package org.jruby.util.collections;

public class IntList {
    private int[] ints;
    private int size;

    public IntList(int initialCapacity) {
        ints = new int[initialCapacity];
    }

    public IntList() {
        this(10);
    }

    public void add(int i) {
        if (size == ints.length) {
            int[] newInts = new int[(int) (ints.length * 1.5 + 1)];
            System.arraycopy(ints, 0, newInts, 0, ints.length);
            ints = newInts;
        }
        ints[size++] = i;
    }

    public boolean contains(int i) {
        for (int j = 0; j < size; j++) {
            if (ints[j] == i) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public int get(int index) {
        if (index >= size) return -1;

        return ints[index];
    }

    public void clear() {
        size = 0;
    }

    public int[] toIntArray() {
        int[] newInts = new int[size];
        System.arraycopy(ints, 0, newInts, 0, size);
        return newInts;
    }
}
