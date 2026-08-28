package java_integration.fixtures;

// See JRUBY-4451
public class ComplexPrivateConstructor {
    private String result;

    // the order of these is important
    private ComplexPrivateConstructor(String str, int a, float b) {
        result = "String: " + str + ", int: " + a + ", float: " + b;
    }
    
    public ComplexPrivateConstructor(String str, int a, int b) {
        result = "String: " + str + ", int: " + a + ", int: " + b;
    }

    public String getResult() {
        return result;
    }
}
