package java_integration.fixtures;
// for testing JRUBY-4680

public class ClassWithMultipleSignaturesWithPrimitiveArgs {
    public static String foo1(float arg) {
        return "float";
    }
    
    public static String foo1(int arg) {
        return "int";
    }
    
    public static String foo2(Object o, float arg) {
        return "float";
    }
    
    public static String foo2(Object o, int arg) {
        return "int";
    }
}