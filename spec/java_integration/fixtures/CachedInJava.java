package java_integration.fixtures;

public class CachedInJava {
    private static final ThreadLocal<CachedInJava> lastInstance = new ThreadLocal<CachedInJava>();

    public CachedInJava() {
        lastInstance.set(this);
    }

    public CachedInJava newInstanceFromInstance() {
        return new CachedInJava();
    }

    public static CachedInJava newInstanceFromStatic() {
        return new CachedInJava();
    }

    public static CachedInJava getLastInstance() {
        return lastInstance.get();
    }
}
