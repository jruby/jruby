package org.jruby.util.collections.test;

import java.util.Iterator;
import java.util.Map;


import junit.framework.TestCase;

@SuppressWarnings("deprecation")
public class SlottedHashMapTest extends TestCase {
    public SlottedHashMapTest(String arg0) {
        super(arg0);
    }
    
    public void test1() {
        org.jruby.util.collections.SlottedHashMap top = new org.jruby.util.collections.SlottedHashMap("top");
        
        top.put("one", "top one");
        top.put("two", "top two");
        top.put("three", "top three");
        
        display(top, "top");
        
        org.jruby.util.collections.SlottedHashMap a = new org.jruby.util.collections.SlottedHashMap("a", top);
        org.jruby.util.collections.SlottedHashMap b = new org.jruby.util.collections.SlottedHashMap("b", top);
        
        a.put("three", "a three");
        a.put("four", "a four");
        a.put("five", "a five");
        
        b.put("two", "b two");
        b.put("four", "b four");
        b.put("five", "b five");
        
        display(a, "a");
        display(b, "b");
        
        org.jruby.util.collections.SlottedHashMap aa = new org.jruby.util.collections.SlottedHashMap("aa", a);
        
        aa.put("one", "aa one");
        aa.put("four", "aa four");
        aa.put("six", "aa six");
        
        display(aa, "aa");
        
        assertEquals("top one", top.get("one"));
        assertEquals("a three", a.get("three"));
        assertEquals("b two", b.get("two"));
        assertEquals("aa one", aa.get("one"));
        assertEquals("top two", aa.get("two"));
        assertEquals("a three", aa.get("three"));
        
        top.put("one", "new top one");
        top.put("five", "new top five");
        a.put("four", "new a four");
        
        display(top, "top");
        display(a, "top");
        display(b, "b");
        display(aa, "aa");

        assertEquals("new top one", a.get("one"));
        assertEquals("aa one", aa.get("one"));
        assertEquals("a five", a.get("five"));
        assertEquals("a five", aa.get("five"));
        assertEquals("aa four", aa.get("four"));
        
        assertEquals("new top one", a.remove("one"));
        assertEquals("new top one", a.remove("one"));
    }
    @SuppressWarnings("deprecation")
    public void display(Map shm, String id) {
        System.out.println("SHM '" + id + "' contains:");
        for (Iterator iter = shm.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry)iter.next();

            System.out.println("\t" + entry.getKey() + " = " + entry.getValue());
        }
    }
}
