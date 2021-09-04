package org.jruby.test;

public class JRUBY_2480_A {
    private JRUBY_2480_B b;

    public JRUBY_2480_A(JRUBY_2480_B b) {
	this.b = b;
    }
	
    public Object doIt(Object a) {
	return b.foo(a);
    }
}
