package java_integration.fixtures;

import java.math.BigInteger;

public class JavaFieldsExt extends JavaFields {

    public JavaFieldsExt(Object field1) {
        this.field1 = field1;
        field2List.add(field1);
    }

    private JavaFieldsExt() {
        this(null);
        this.field1 = this;
    }

    private final JavaFields field1Ext = new JavaFieldsExt();

    transient java.util.List<Object> field2List = new java.util.ArrayList<>();

    public java.util.List getField2List() { return field2List; }

}