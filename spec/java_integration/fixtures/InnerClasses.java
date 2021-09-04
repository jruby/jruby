package java_integration.fixtures;

import java.io.Serializable;

public class InnerClasses {

    public static class CapsInnerClass {
        public static int value() { return 1; }
        public static class CapsInnerClass2 {
            public static int value() { return 1; }
        }
        public static class lowerInnerClass2 {
            public static int value() { return 1; }
        }
        public interface CapsInnerInterface2 {}
        public interface lowerInnerInterface2 {}
    }
    public static class lowerInnerClass {
        public static int value() { return 1; }
        public static class CapsInnerClass3 {
            public static int value() { return 1; }
        }
        public static class lowerInnerClass3 {
            public static int value() { return 1; }
        }
        public interface CapsInnerInterface3 {}
        public interface lowerInnerInterface3 {}
    }

    public interface CapsInnerInterface {
        public static class CapsInnerClass4 {
            public static int value() { return 1; }
        }
        public static class lowerInnerClass4 {
            public static int value() { return 1; }
        }
        public interface CapsInnerInterface4 {}
        public interface lowerInnerInterface4 {}
    }

    public interface lowerInnerInterface {
        public static class CapsInnerClass5 {
            public static int value() { return 1; }
        }
        public static class lowerInnerClass5 {
            public static int value() { return 1; }
        }
        public interface CapsInnerInterface5 {}
        public interface lowerInnerInterface5 {}
    }

    protected static interface ProtectedInner {
        public static String VALUE = ProtectedInner.class.getName();

        class Nested extends PackageInner {}
    }

    static class PackageInner {
        protected static String VALUE = PackageInner.class.getName();
    }

    private static class PrivateInner {
        static String VALUE = PrivateInner.class.getName();
    }

    private static int capsImplCounter;

    public static CapsInnerInterface localMethodClass() {
        class CapsImpl implements CapsInnerSerial {
            private final int counter;
            CapsImpl(int counter) { this.counter = counter; }
            public String capsMethod() { return "CapsImpl" + counter; }

        }
        return new CapsImpl(++capsImplCounter);
    }

    public static CapsInnerInterface localMethodClass2() {
        class CapsImpl implements CapsInnerInterface, Serializable {
            private final int counter;
            CapsImpl(int counter) { this.counter = counter; }
            public String capsMethod() { return "CapsImpl2" + counter; }

        }
        return new CapsImpl(++capsImplCounter);
    }

    private static int capsAnonCounter;

    public static CapsInnerInterface anonymousMethodClass() {
        return new CapsInnerSerial() {
            private final int counter = ++capsAnonCounter;
            public String capsMethod() { return "CapsAnon" + counter; }
        };
    }

    public static interface CapsInnerSerial extends CapsInnerInterface, Serializable {}

    public static class ConflictsWithStaticFinalField {
        public boolean ok() { return true; }
    }

    public static final ConflictsWithStaticFinalField ConflictsWithStaticFinalField = new ConflictsWithStaticFinalField();

}
