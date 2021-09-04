package java_integration.fixtures;

import java.util.Arrays;

public class ClassWithVarargs {
    private String constructor;
    public ClassWithVarargs(Object... args) {
        constructor = "0: " + Arrays.toString(args);
    }
    public ClassWithVarargs(String a, Object... args) {
        constructor = "1: " + Arrays.toString(args);
    }
    public ClassWithVarargs(String a, String b, Object... args) {
        constructor = "2: " + Arrays.toString(args);
    }
    public ClassWithVarargs(String a, String b, String c, Object... args) {
        constructor = "3: " + Arrays.toString(args);
    }
    public String getConstructor() {
        return constructor;
    }

    public static String varargsStatic(Object... args) {
        return "0: " + Arrays.toString(args);
    }
    public static String varargsStatic(String a, Object... args) {
        return "1: " + Arrays.toString(args);
    }
    public static String varargsStatic(String a, String b, Object... args) {
        return "2: " + Arrays.toString(args);
    }
    public static String varargsStatic(String a, String b, String c, Object... args) {
        return "3: " + Arrays.toString(args);
    }

    public String varargs(Object... args) {
        return "0: " + Arrays.toString(args);
    }
    public String varargs(String a, Object... args) {
        return "1: " + Arrays.toString(args);
    }
    public String varargs(String a, String b, Object... args) {
        return "2: " + Arrays.toString(args);
    }
    public String varargs(String a, String b, String c, Object... args) {
        return "3: " + Arrays.toString(args);
    }
}
