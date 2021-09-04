package java_integration.fixtures;

public class JavaTypeMethods {
    private static Object staticObject;
    private Object object;

    public static Object staticNewObject() {
        return new Object();
    }

    public static void staticSetObject(Object obj) {
        staticObject = obj;
    }

    public static Object staticGetObject() {
        return staticObject;
    }

    public Object newObject() {
        return new Object();
    }

    public void setObject(Object obj) {
        object = obj;
    }

    public Object getObject() {
        return object;
    }
}
