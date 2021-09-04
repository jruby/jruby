package java_integration.fixtures;

public interface BeanLikeInterface {
    // simple property "value"
    public Object getValue();
    public void setValue(Object foo);
    
    // simple property "myValue"
    public Object getMyValue();
    public void setMyValue(Object foo);
    
    // boolean property "foo"
    public boolean isFoo();
    public void setFoo(boolean foo);
    
    // boolean property "myFoo"
    public boolean isMyFoo();
    public void setMyFoo(boolean foo);
    
    // non-property boolean methods
    public boolean friendly();
    public boolean supahFriendly();
    
    // non-property get/set methods
    public Object getSomethingFoo(Object foo);
    public void setSomethingFoo(Object something, Object foo);
}