package java_integration.fixtures;

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
}
