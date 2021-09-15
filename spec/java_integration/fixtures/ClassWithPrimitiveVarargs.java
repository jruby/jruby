package java_integration.fixtures;

import java.util.Arrays;

public class ClassWithPrimitiveVarargs {
    private String constructor;
    public ClassWithPrimitiveVarargs(int... args) {
        constructor = "0: " + Arrays.toString(args);
    }
    public ClassWithPrimitiveVarargs(String a, int... args) {
        constructor = "1: " + Arrays.toString(args);
    }
    public ClassWithPrimitiveVarargs(String a, String b, int... args) {
        constructor = "2: " + Arrays.toString(args);
    }
    public ClassWithPrimitiveVarargs(String a, String b, String c, int... args) {
        constructor = "3: " + Arrays.toString(args);
    }
    public String getConstructor() {
        return constructor;
    }

    public static String primitiveVarargsStatic(int... args) {
        return "0: " + Arrays.toString(args);
    }
    public static String primitiveVarargsStatic(String a, int... args) {
        return "1: " + Arrays.toString(args);
    }
    public static String primitiveVarargsStatic(String a, String b, int... args) {
        return "2: " + Arrays.toString(args);
    }
    public static String primitiveVarargsStatic(String a, String b, String c, int... args) {
        return "3: " + Arrays.toString(args);
    }

    public String primitiveVarargs(int... args) {
        return "0: " + Arrays.toString(args);
    }
    public String primitiveVarargs(String a, int... args) {
        return "1: " + Arrays.toString(args);
    }
    public String primitiveVarargs(String a, String b, int... args) {
        return "2: " + Arrays.toString(args);
    }
    public String primitiveVarargs(String a, String b, String c, int... args) {
        return "3: " + Arrays.toString(args);
    }
}
