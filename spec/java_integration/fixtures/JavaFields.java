package java_integration.fixtures;

import java.math.BigInteger;

public class JavaFields {
    public static String stringStaticField = "000";
    public static byte byteStaticField = (byte)1;
    public static short shortStaticField = (short)2;
    public static char charStaticField = (char)'3';
    public static int intStaticField = 4;
    public static long longStaticField = 5;
    public static float floatStaticField = 6.0f;
    public static double doubleStaticField = 7.2d;
    public static boolean trueStaticField = true;
    public static boolean falseStaticField = false;
    public static Object nullStaticField = null;
    public static BigInteger bigIntegerStaticField = new BigInteger("111111111111111111110");
    
    public static Byte byteObjStaticField = Byte.valueOf(byteStaticField);
    public static Short shortObjStaticField = Short.valueOf(shortStaticField);
    public static Character charObjStaticField = Character.valueOf(charStaticField);
    public static Integer intObjStaticField = Integer.valueOf(intStaticField);
    public static Long longObjStaticField = Long.valueOf(longStaticField);
    public static Float floatObjStaticField = Float.valueOf(floatStaticField);
    public static Double doubleObjStaticField = Double.valueOf(doubleStaticField);
    public static Boolean trueObjStaticField = Boolean.TRUE;
    public static Boolean falseObjStaticField = Boolean.FALSE;

    public static String $LEADING = "leading";
    public static Boolean TRAILING$ = Boolean.TRUE;

    public String stringField = stringStaticField;
    public byte byteField = (byte)1;
    public short shortField = (short)2;
    public char charField = (char)'T';
    public int intField = 4;
    public long longField = 5;
    public float floatField = floatStaticField;
    public double doubleField = doubleStaticField;
    public boolean trueField = true;
    public boolean falseField = false;
    public final Object nullField = null;
    public BigInteger bigIntegerField = new BigInteger("111111111111111111111");
    
    public Byte byteObjField = Byte.valueOf(byteField);
    public Short shortObjField = Short.valueOf(shortField);
    public Character charObjField = Character.valueOf(charField);
    public Integer intObjField = Integer.valueOf(intField);
    public Long longObjField = Long.valueOf(longField);
    public Float floatObjField = Float.valueOf(floatField);
    public Double doubleObjField = Double.valueOf(doubleField);
    public Boolean trueObjField = Boolean.TRUE;
    public Boolean falseObjField = Boolean.FALSE;

    private final int privateIntField = 1;

    Object field1 = this;
    Object[] aryField2 = new Object[] { this };

}
