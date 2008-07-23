package java_integration.fixtures;

public class JavaFields {
    public static String stringStaticField = "foo";
    public static byte byteStaticField = (byte)1;
    public static short shortStaticField = (short)2;
    public static char charStaticField = (char)2;
    public static int intStaticField = 4;
    public static long longStaticField = 8;
    public static float floatStaticField = 4.0f;
    public static double doubleStaticField = 8.0;
    public static boolean trueStaticField = true;
    public static boolean falseStaticField = false;
    public static Object nullStaticField = null;
    
    public static Byte byteObjStaticField = Byte.valueOf(byteStaticField);
    public static Short shortObjStaticField = Short.valueOf(shortStaticField);
    public static Character charObjStaticField = Character.valueOf(charStaticField);
    public static Integer intObjStaticField = Integer.valueOf(intStaticField);
    public static Long longObjStaticField = Long.valueOf(longStaticField);
    public static Float floatObjStaticField = Float.valueOf(floatStaticField);
    public static Double doubleObjStaticField = Double.valueOf(doubleStaticField);
    public static Boolean trueObjStaticField = Boolean.TRUE;
    public static Boolean falseObjStaticField = Boolean.FALSE;

    public String stringField = "foo";
    public byte byteField = (byte)1;
    public short shortField = (short)2;
    public char charField = (char)2;
    public int intField = 4;
    public long longField = 8;
    public float floatField = 4.0f;
    public double doubleField = 8.0;
    public boolean trueField = true;
    public boolean falseField = false;
    public Object nullField = null;
    
    public Byte byteObjField = Byte.valueOf(byteField);
    public Short shortObjField = Short.valueOf(shortField);
    public Character charObjField = Character.valueOf(charField);
    public Integer intObjField = Integer.valueOf(intField);
    public Long longObjField = Long.valueOf(longField);
    public Float floatObjField = Float.valueOf(floatField);
    public Double doubleObjField = Double.valueOf(doubleField);
    public Boolean trueObjField = Boolean.TRUE;
    public Boolean falseObjField = Boolean.FALSE;
}
