package java_integration.fixtures;

public class AnArrayList extends java.util.ArrayList {
    public static final java.util.Vector CREATED_INSTANCES = new java.util.Vector();

    public AnArrayList() {
        super();
        CREATED_INSTANCES.add(this);
    }

    public AnArrayList(int c) {
        super(c);
        for (int i=0; i<c; i++) this.add(null);
        CREATED_INSTANCES.add(this);
    }

}
