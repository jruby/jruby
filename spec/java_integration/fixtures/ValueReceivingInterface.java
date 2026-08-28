package java_integration.fixtures;

public interface ValueReceivingInterface {
  public Object receiveObject(Object obj);
  public String receiveString(String string);
  public byte receiveByte(byte b);
  public short receiveShort(short s);
  public char receiveChar(char s);
  public int receiveInt(int i);
  public long receiveLong(long l);
  public float receiveFloat(float f);
  public double receiveDouble(double d);
  public boolean receiveTrue(boolean t);
  public boolean receiveFalse(boolean f);
  public Object receiveNull(Object nil);
  public String receiveLongAndDouble(long l, double d);
  
  public Byte receiveByteObj(Byte b);
  public Short receiveShortObj(Short s);
  public Character receiveCharObj(Character c);
  public Integer receiveIntObj(Integer i);
  public Long receiveLongObj(Long l);
  public Float receiveFloatObj(Float f);
  public Double receiveDoubleObj(Double d);
  public Boolean receiveTrueObj(Boolean t);
  public Boolean receiveFalseObj(Boolean f);
}