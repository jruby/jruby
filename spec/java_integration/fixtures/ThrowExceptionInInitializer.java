package java_integration.fixtures;

public class ThrowExceptionInInitializer {
    public void test() {
        new ThrowExceptionInStatic();
    }
}

class ThrowExceptionInStatic  {
    static {
        if (1 == (1 + 0)) {
          throw new RuntimeException("lets cause an init exception");
        }
    }
}