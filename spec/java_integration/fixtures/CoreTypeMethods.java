package java_integration.fixtures;

import java.math.BigInteger;
import java.io.Serializable;

public class CoreTypeMethods {
    public static String getString() {
        return "foo";
    }
    
    public static byte getByte() {
        return (byte)1;
    }
    
    public static short getShort() {
        return (short)2;
    }
    
    public static char getChar() {
        return (char)2;
    }
    
    public static int getInt() {
        return 4;
    }
    
    public static long getLong() {
        return (long)8;
    }
    
    public static float getFloat() {
        return (float)4.5;
    }
    
    public static double getDouble() {
        return 8.5;
    }
    
    public static boolean getBooleanTrue() {
        return true;
    }
    
    public static boolean getBooleanFalse() {
        return false;
    }
    
    public static Object getNull() {
        return null;
    }
    
    public static void getVoid() {
        return;
    }

    public static BigInteger getBigInteger() {
        return new BigInteger("1234567890123456789012345678901234567890");
    }
    
    public static String setString(String s) {
        return s;
    }
    
    public static String setByte(byte b) {
        return String.valueOf(b);
    }
    
    public static String setShort(short s) {
        return String.valueOf(s);
    }
    
    public static String setChar(char c) {
        return String.valueOf(c);
    }
    
    public static String setInt(int i) {
        return String.valueOf(i);
    }
    
    public static String setLong(long l) {
        return String.valueOf(l);
    }
    
    public static String setFloat(float f) {
        return String.valueOf(f);
    }
    
    public static String setDouble(double d) {
        return String.valueOf(d);
    }
    
    public static String setBooleanTrue(boolean t) {
        return String.valueOf(t);
    }
    
    public static String setBooleanFalse(boolean f) {
        return String.valueOf(f);
    }
    
    public static String setNull(Object nil) {
        return String.valueOf(nil);
    }

    public static String setBigInteger(BigInteger bi) {
        return String.valueOf(bi);
    }

    public static String setByteObj(Byte b) {
        return String.valueOf(b);
    }

    public static String setShortObj(Short s) {
        return String.valueOf(s);
    }

    public static String setCharObj(Character c) {
        return String.valueOf(c);
    }

    public static String setIntObj(Integer i) {
        return String.valueOf(i);
    }

    public static String setLongObj(Long l) {
        return String.valueOf(l);
    }

    public static String setFloatObj(Float f) {
        return String.valueOf(f);
    }

    public static String setDoubleObj(Double d) {
        return String.valueOf(d);
    }

    public static String setBooleanTrueObj(Boolean t) {
        return String.valueOf(t);
    }

    public static String setBooleanFalseObj(Boolean f) {
        return String.valueOf(f);
    }

    public static String setNumber(Number n) {
        return n.getClass().getName();
    }

    public static String setSerializable(Serializable n) {
        return n.getClass().getName();
    }
    
    public static String getType(byte b) {
        return "byte";
    }
    
    public static String getType(short s) {
        return "short";
    }
    
    public static String getType(char c) {
        return "char";
    }
    
    public static String getType(int i) {
        return "int";
    }
    
    public static String getType(long l) {
        return "long";
    }
    
    public static String getType(float f) {
        return "float";
    }
    
    public static String getType(double d) {
        return "double";
    }
    
    public static String getType(CharSequence cs) {
        return "CharSequence";
    }
    
    public static String getType(String s) {
        return "String";
    }

    public static String getType(boolean b) {
        return "boolean";
    }

    public static String getType(BigInteger b) {
        return "BigInteger";
    }
    
    public String getTypeInstance(byte b) {
        return "byte";
    }
    
    public String getTypeInstance(short s) {
        return "short";
    }
    
    public String getTypeInstance(char c) {
        return "char";
    }
    
    public String getTypeInstance(int i) {
        return "int";
    }
    
    public String getTypeInstance(long l) {
        return "long";
    }
    
    public String getTypeInstance(float f) {
        return "float";
    }
    
    public String getTypeInstance(double d) {
        return "double";
    }
    
    public String getTypeInstance(CharSequence cs) {
        return "CharSequence";
    }
    
    public String getTypeInstance(String s) {
        return "String";
    }

    public String getTypeInstance(boolean b) {
        return "boolean";
    }

    public String getTypeInstance(BigInteger b) {
        return "BigInteger";
    }
    
    public static String getObjectType(Object obj) {
        return obj.getClass().toString();
    }

    public static String getType(double i, Object o) {
        return "double,object";
    }

    public static String getType(long i, Object o) {
        return "long,object";
    }

    public static String getType(double i, Object o, Object o2) {
        return "double,object,object";
    }

    public static String getType(long i, Object o, Object o2) {
        return "long,object,object";
    }

    public static String getType(double i, Object o, Object o2, Object o3) {
        return "double,object,object,object";
    }

    public static String getType(long i, Object o, Object o2, Object o3) {
        return "long,object,object,object";
    }

    public static String getType(double i, String o) {
        return "double,string";
    }

    public static String getType(long i, String o) {
        return "long,string";
    }

    public static String getType(double i, String o, String o2) {
        return "double,string,string";
    }

    public static String getType(long i, String o, String o2) {
        return "long,string,string";
    }

    public static String getType(double i, String o, String o2, String o3) {
        return "double,string,string,string";
    }

    public static String getType(long i, String o, String o2, String o3) {
        return "long,string,string,string";
    }

    public String getTypeInstance(double i, Object o) {
        return "double,object";
    }

    public String getTypeInstance(long i, Object o) {
        return "long,object";
    }

    public String getTypeInstance(double i, Object o, Object o2) {
        return "double,object,object";
    }

    public String getTypeInstance(long i, Object o, Object o2) {
        return "long,object,object";
    }

    public String getTypeInstance(double i, Object o, Object o2, Object o3) {
        return "double,object,object,object";
    }

    public String getTypeInstance(long i, Object o, Object o2, Object o3) {
        return "long,object,object,object";
    }

    public String getTypeInstance(double i, String o) {
        return "double,string";
    }

    public String getTypeInstance(long i, String o) {
        return "long,string";
    }

    public String getTypeInstance(double i, String o, String o2) {
        return "double,string,string";
    }

    public String getTypeInstance(long i, String o, String o2) {
        return "long,string,string";
    }

    public String getTypeInstance(double i, String o, String o2, String o3) {
        return "double,string,string,string";
    }

    public String getTypeInstance(long i, String o, String o2, String o3) {
        return "long,string,string,string";
    }
}
