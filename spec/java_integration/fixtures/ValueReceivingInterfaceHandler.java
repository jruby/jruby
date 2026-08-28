package java_integration.fixtures;

public class ValueReceivingInterfaceHandler {
    private ValueReceivingInterface vri;
    
    public ValueReceivingInterfaceHandler(ValueReceivingInterface vri) {
        this.vri = vri;
    }

    public Object receiveObject(Object obj) {
        return vri.receiveObject(obj);
    }

    public String receiveString(String string) {
        return vri.receiveString(string);
    }

    public byte receiveByte(byte b) {
        return vri.receiveByte(b);
    }

    public short receiveShort(short s) {
        return vri.receiveShort(s);
    }

    public char receiveChar(char c) {
        return vri.receiveChar(c);
    }

    public int receiveInt(int i) {
        return vri.receiveInt(i);
    }

    public long receiveLong(long l) {
        return vri.receiveLong(l);
    }

    public float receiveFloat(float f) {
        return vri.receiveFloat(f);
    }

    public double receiveDouble(double d) {
        return vri.receiveDouble(d);
    }

    public boolean receiveTrue(boolean t) {
        return vri.receiveTrue(t);
    }

    public boolean receiveFalse(boolean f) {
        return vri.receiveFalse(f);
    }

    public Object receiveNull(Object nil) {
        return vri.receiveNull(nil);
    }
    
    public String receiveLongAndDouble(long l, double d) {
        return vri.receiveLongAndDouble(l, d);
    }

    public Byte receiveByteObj(Byte b) {
        return vri.receiveByteObj(b);
    }

    public Short receiveShortObj(Short s) {
        return vri.receiveShortObj(s);
    }

    public Character receiveCharObj(Character c) {
        return vri.receiveCharObj(c);
    }

    public Integer receiveIntObj(Integer i) {
        return vri.receiveIntObj(i);
    }

    public Long receiveLongObj(Long l) {
        return vri.receiveLongObj(l);
    }

    public Float receiveFloatObj(Float f) {
        return vri.receiveFloatObj(f);
    }

    public Double receiveDoubleObj(Double d) {
        return vri.receiveDoubleObj(d);
    }

    public Boolean receiveTrueObj(Boolean t) {
        return vri.receiveTrueObj(t);
    }

    public Boolean receiveFalseObj(Boolean f) {
        return vri.receiveFalseObj(f);
    }
}