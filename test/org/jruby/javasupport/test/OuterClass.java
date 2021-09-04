package org.jruby.javasupport.test;

public class OuterClass {
    public class PublicInstanceInnerClass {
        public void a() {}
    }
    public static class PublicStaticInnerClass {
        public void a() {}
    }
    protected class ProtectedInstanceInnerClass {
    }
    protected static class ProtectedStaticInnerClass {
    }
    class DefaultInstanceInnerClass {
    }
    static class DefaultStaticInnerClass {
    }
    private class PrivateInstanceInnerClass {
    }
    private static class PrivateStaticInnerClass {
    }
}
