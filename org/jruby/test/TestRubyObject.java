package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.regexp.GNURegexpAdapter;

public class TestRubyObject extends TestCase {
    private Ruby ruby;
    private RubyObject rubyObject;

    public TestRubyObject(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance(GNURegexpAdapter.class);
        rubyObject = new RubyObject(ruby, ruby.getClasses().getObjectClass());
    }
    
    public void testNil() {
        assertTrue(!rubyObject.isNil());
    }
    
    public void testTrue() {
        assertTrue(rubyObject.isTrue());
    }
    
    public void testFalse() {
        assertTrue(!rubyObject.isFalse());
    }
    
    public void testEqual() {
        assertTrue(rubyObject.equal(rubyObject).isTrue());
    }
    
    public void testEquals() {
        assertTrue(rubyObject.equals(rubyObject));
    }
    
    public void testClone() {
        assertTrue(rubyObject.rbClone().type() == rubyObject.type());
    }
    
    public void testDup() {
        assertTrue(rubyObject.dup().type() == rubyObject.type());
    }
    
    public void testType() {
        assertEquals("Object", rubyObject.type().name().toString());
    }
    
    public void testFreeze() {
	    assertTrue(rubyObject.frozen().isFalse());
        rubyObject.freeze();
        assertTrue(rubyObject.frozen().isTrue());
    }
    
    public void testTaint() {
	    assertTrue(rubyObject.tainted().isFalse());
        rubyObject.taint();
        assertTrue(rubyObject.tainted().isTrue());
        rubyObject.untaint();
        assertTrue(rubyObject.tainted().isFalse());
    }
    
    public void test_to_s() {
        assertTrue(rubyObject.to_s().toString().startsWith("#<Object:0x"));
    }

    public void test_instance_of() {
        assertTrue(rubyObject.instance_of(ruby.getClasses().getObjectClass()).isTrue());
        assertTrue(rubyObject.instance_of(ruby.getClasses().getStringClass()).isFalse());
    }
    
    public void test_kind_of() {
        assertTrue(rubyObject.kind_of(ruby.getClasses().getObjectClass()).isTrue());
        // assertTrue(rubyObject.kind_of(ruby.getClasses().getStringClass()).isFalse());
    }
}