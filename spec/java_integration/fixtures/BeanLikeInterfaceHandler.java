package java_integration.fixtures;

public class BeanLikeInterfaceHandler {
    private BeanLikeInterface bri;
    
    public BeanLikeInterfaceHandler(BeanLikeInterface bri) {
        this.bri = bri;
    }
    // simple property "value"
    public Object getValue() {
        return bri.getValue();
    }
    public void setValue(Object foo) {
        bri.setValue(foo);
    }
    
    // simple property "myValue"
    public Object getMyValue() {
        return bri.getMyValue();
    }
    public void setMyValue(Object foo) {
        bri.setMyValue(foo);
    }
    
    // boolean property "foo"
    public boolean isFoo() {
        return bri.isFoo();
    }
    public void setFoo(boolean foo) {
        bri.setFoo(foo);
    }
    
    // boolean property "myFoo"
    public boolean isMyFoo() {
        return bri.isMyFoo();
    }
    public void setMyFoo(boolean foo) {
        bri.setMyFoo(foo);
    }
    
    // non-property boolean methods
    public boolean friendly() {
        return bri.friendly();
    }
    public boolean supahFriendly() {
        return bri.supahFriendly();
    }
    
    // non-property get/set methods
    public Object getSomethingFoo(Object foo) {
        return bri.getSomethingFoo(foo);
    }
    
    public void setSomethingFoo(Object something, Object foo) {
        bri.setSomethingFoo(something, foo);
    }
}