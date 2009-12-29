package java_integration.fixtures;

public class ClassWithVarargs {
    private int constructor;
    public ClassWithVarargs(Object... args) {
        constructor = 0;
    }
    public ClassWithVarargs(String a, Object... args) {
        constructor = 1;
    }
    public ClassWithVarargs(String a, String b, Object... args) {
        constructor = 2;
    }
    public ClassWithVarargs(String a, String b, String c, Object... args) {
        constructor = 3;
    }
    public int getConstructor() {
        return constructor;
    }

    public static int varargsStatic(Object... args) {
        return 0;
    }
    public static int varargsStatic(String a, Object... args) {
        return 1;
    }
    public static int varargsStatic(String a, String b, Object... args) {
        return 2;
    }
    public static int varargsStatic(String a, String b, String c, Object... args) {
        return 3;
    }

    public int varargs(Object... args) {
        return 0;
    }
    public int varargs(String a, Object... args) {
        return 1;
    }
    public int varargs(String a, String b, Object... args) {
        return 2;
    }
    public int varargs(String a, String b, String c, Object... args) {
        return 3;
    }
}
