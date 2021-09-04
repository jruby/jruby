package java_integration.fixtures;

public class GenericComparable implements Comparable {
    public int compareTo(Integer other) {
        return 0;
    }

    public int compareTo(Object other) {
        return 1;
    }
}