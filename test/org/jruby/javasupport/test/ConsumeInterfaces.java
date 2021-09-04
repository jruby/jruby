package org.jruby.javasupport.test;

public class ConsumeInterfaces {
    public void addInterface1(Interface1 interface1) {
        interface1.method1();
    }
    
    public void addInterface2(Interface2 interface2) {
        interface2.method2();
    }
}
