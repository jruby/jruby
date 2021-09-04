package java_integration.fixtures;

public class MethodNames {
    public static void lowercase1() {}
    public static void camelCase1() {}
    public static void camelWithUPPER1() {}
    public static void camelWITHUpper1() {}
    public static void CAMELWithUpper1() {}

    public static Object getValue1() {return null;}
    public static Object getValues1(Object otherValue) {return null;}
    public static void setValue1(Object value) {}
    public static void setValues1(Object value, Object otherValue) {}

    /** Should preserve these names by downcasing all trailing caps */
    public static Object getValueOBJS() {return null;}
    public static void setValueOBJS(Object value) {}
    public static Object getValueOBJSHere() {return null;}
    public static void setValueOBJSHere(Object value) {}

    public static boolean isFirst1() {return false;}
    public static boolean isSecond1(Object something) {return false;}
    public static boolean hasThird1() {return false;}
    public static boolean hasFourth1(Object something) {return false;}
    public static String __send__() { return "foo"; }

    public void lowercase2() {}
    public void camelCase2() {}
    public void camelWithUPPER2() {}
    public void camelWITHUpper2() {}
    public void CAMELWithUpper2() {}

    public Object getValue2() {return null;}
    public Object getValues2(Object something) {return null;}
    public void setValue2(Object value) {}
    public void setValues2(Object value, Object otherValue) {}

    // Single letter method should exist (1.6.0 regression)
    public double getX() { return 1.0; }
    public int bigO() { return 0; }

    // Another case [A-Z].[A-Z] that is tricky to get split correctly
    public Object getMyValue() {return null;}
    public void setMyValue(Object value) {}

    // Should preserve these names by downcasing all trailing caps.
    public Object getValueOBJ() {return null;}
    public void setValueOBJ(Object value) {}
    public Object getValueOBJHere() {return null;}
    public void setValueOBJHere(Object value) {}

    public Object getJConsecutiveCaps() {return null;}
    public void setJConsecutiveCaps(Object value) {}

    public boolean isFirst2() {return false;}
    public boolean isSecond2(Object something) {return false;}
    public boolean hasThird2() {return false;}
    public boolean hasFourth2(Object something) {return false;}

    public String initialize() {return "foo";}
    public String type() {return "foo";}
    public String __type__() {return "foo";}
    public String id() {return "foo";}
    public String __id__() {return "foo";}

    public int getFoo() { return 42; }
    public boolean isFoo() { return false; }

}
