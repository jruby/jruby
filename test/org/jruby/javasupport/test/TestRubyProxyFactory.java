package org.jruby.javasupport.test;

import org.jruby.Ruby;
import org.jruby.javasupport.ReflectionClassMap;
import org.jruby.javasupport.RubyProxyFactory;
import org.jruby.javasupport.RubyToJavaClassMap;

import java.io.IOException;
import java.net.URL;

public class TestRubyProxyFactory extends RubyTestCase {
    private static final double EPSILON = 0.0;
    private static final String RUBY_PACKAGE = "org.jruby.javasupport.test";
    private static final String RUBY_FILE = "test.rb";

    private Ruby runtime = null;
    private RubyToJavaClassMap classMap = null;
    private RubyProxyFactory factory = null;
    private RubyTestObject test = null;

    public TestRubyProxyFactory(String name) {
        super(name);
    }

    public void setUp() throws IOException {
        URL testFile = getClass().getResource(RUBY_FILE);
        if (testFile == null) {
            fail("Couldn't locate test file: " + RUBY_FILE);
        }
        runtime = createRuby(testFile);
        classMap = new ReflectionClassMap(RUBY_PACKAGE);
        factory = new RubyProxyFactory(runtime, classMap);
        test = (RubyTestObject) factory.newProxyObject(RubyTestObject.class);
    }

    public void tearDown() {
        runtime = null;
        classMap = null;
        factory = null;
        test = null;
    }

    /*
      New javasupport doesn't automatically translate lists and arrays.
      Maybe this test needs to be rewritten to reflect that change,
      i don't know. --Anders
    
    
      public void testList ()
      {
          List l = new ArrayList();
    
          test.setList(l);
          assertEquals("empty list", l, test.getList());
    
          l.add("obj1");
    
          assertTrue("unset list", !l.equals(test.getList()));
    
          test.setList(l);
          assertEquals("one-element list", l, test.getList()); // HANGS
    
          l.add("obj2");
    
          test.setList(l);
          assertEquals("two-element list", l, test.getList()); // HANGS
    
          assertEquals("joined list", "obj1,obj2", test.joinList());
      }
    
    */

    public void testNewProxyObject() {
        assertNotNull(test);
    }

    public void testDuplicateProxyObjects() {
        RubyTestObject test2 =
            (RubyTestObject) factory.newProxyObject(RubyTestObject.class);

        assertNotNull(test2);
        assertTrue(test != test2);
    }

    public void testNoArgs() {
        test.noArgs();
    }

    public void testInteger() {
        test.setNumber(10);
        assertEquals(10, test.getNumberAsInt());
    }

    public void testDouble() {
        test.setNumber(3.1415D);
        assertEquals(3.1415D, test.getNumberAsDouble(), EPSILON); // FAILS
    }

    public void testLong() {
        test.setNumber((long) Integer.MAX_VALUE + 2);
        assertEquals((long) Integer.MAX_VALUE + 2, test.getNumberAsLong());
        assertTrue(test.getNumberAsLong() > Integer.MAX_VALUE);
    }

    public void testNumberObjs() {
        test.setNumber(new Integer(30));
        assertEquals(new Integer(30), test.getNumberAsIntObj());

        test.setNumber(new Double(30.145D));
        assertEquals(new Double(30.145D), test.getNumberAsDoubleObj());
        // FAILS

        test.setNumber(new Long(100000000000001L));
        assertEquals(new Long(100000000000001L), test.getNumberAsLongObj());
    }

    public void testNumberBoundaries() {
        test.setNumber(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, test.getNumberAsInt());

        test.setNumber(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, test.getNumberAsInt());

        test.setNumber(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, test.getNumberAsLong());

        test.setNumber(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, test.getNumberAsLong());

        test.setNumber(0D);
        assertEquals(0D, test.getNumberAsDouble(), EPSILON);

        test.setNumber(-0D);
        assertEquals(-0D, test.getNumberAsDouble(), EPSILON);

        test.setNumber(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, test.getNumberAsDouble(), EPSILON);
        // FAILS

        test.setNumber(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, test.getNumberAsDouble(), EPSILON);
        // FAILS

        test.setNumber(Double.NaN);
        assertTrue(Double.isNaN(test.getNumberAsDouble())); // FAILS

        test.setNumber(Double.POSITIVE_INFINITY);
        assertEquals(
            Double.POSITIVE_INFINITY,
            test.getNumberAsDouble(),
            EPSILON);
        // FAILS

        test.setNumber(Double.NEGATIVE_INFINITY);
        assertEquals(
            Double.NEGATIVE_INFINITY,
            test.getNumberAsDouble(),
            EPSILON);
        // FAILS
    }

    public void testString() {
        test.setString("test string");
        assertEquals("test string", test.getString());
    }

    public void testBool() {
        test.setBool(true);
        assertEquals(true, test.getBool());

        test.setBool(false);
        assertEquals(false, test.getBool());
    }

    public void testEmptyString() {
        test.setString("");
        assertEquals("", test.getString());
    }

    public void testObject() {
        Object obj = new Object();

        test.setObject(obj);

        assertEquals(
            obj.getClass().getName(),
            test.getObject().getClass().getName());
        assertEquals(obj, test.getObject());
        assertTrue(obj == test.getObject());
    }

    public void testToString() {
        test.setString("foobar");
        assertEquals("<foobar>", test.toString());
    }

    public void testNil() {
        assertNull("implicit nil value", test.getString());
        test.setString("test");
        assertNotNull("non-nil value", test.getString());
        test.setString(null);
        assertNull("explicit nil value", test.getString());
    }

    public void testOutcomingProxy() {
        test.setString("str");

        RubyTestObject test2 = test.duplicate();
        assertTrue(test != test2);
        assertEquals("str", test2.getString());
    }

    public void testIngoingProxy() {
        assertTrue(test.isSelf(test)); // FAILS
        //          assertTrue(!test.isSelf(test));
        assertTrue(!test.isSelf(test.duplicate()));
    }

    public void testGlobalVariable() throws ClassNotFoundException {
        RubyTestObject global =
            (RubyTestObject) factory.getProxyForGlobal("$global_test");

        assertNotNull(global);
        assertEquals("global obj", global.getString());
    }

    public void testNilGlobalVariable() {
        RubyTestObject nilGlobal =
            (RubyTestObject) factory.getProxyForGlobal("$nil_global");

        assertNull(nilGlobal);
    }

    /*
      public void testListConversion ()
      {
          List l = new LinkedList();
          l.add("obj1");
          l.add("obj2");
          l.add("obj3");
    
          test.setList(l);
          assertTrue(l instanceof LinkedList);
          assertTrue(!(l instanceof ArrayList));
    
          List l2 = test.getList(); // HANGS
    
            assertTrue(l2 instanceof ArrayList);
            assertTrue(!(l2 instanceof LinkedList));
      }
    */

    /*
      public void testMapConversion ()
      {
          Map m = new TreeMap();
          m.put("obj1", new Integer(1));
          m.put("obj2", new Integer(2));
          m.put("obj3", new Integer(3));
    
          test.setMap(m);
          Map m2 = test.getMap();
    
          assertTrue(m instanceof TreeMap);
          assertTrue(!(m instanceof HashMap));
          assertTrue(m2 instanceof HashMap);
          assertTrue(!(m2 instanceof TreeMap));
      }
    */
    /*
      public void testListWithDuplicates ()
      {
          List l = new ArrayList();
          l.add("obj1");
          l.add("obj1");
          l.add("obj2");
          l.add("obj3");
          l.add("obj2");
    
          test.setList(l);
          assertEquals("obj1,obj1,obj2,obj3,obj2", test.joinList());
    
          List l2 = test.getList(); // HANGS
          assertTrue(l != l2);
          assertEquals(l, l2);
      }
    */

    /*
    public void testSetOperations ()
    {
        List l = new ArrayList();
        l.add("obj1");
    
        test.setList(l);
        assertEquals("single object", "obj1", test.joinList());
    
        test.addToList("obj2");
        assertEquals("added object", "obj1,obj2", test.joinList());
    
        test.removeFromList("obj1");
        assertEquals("removed object", "obj2", test.joinList());
    }
    */
    /*
    public void testListAsArray ()
    {
        Object[] str = new Object[] {"obj1", "obj2"};
    
        test.setList(Arrays.asList(str));
        assertTrue(Arrays.equals(str, test.getListAsArray()));
        assertEquals("obj1,obj2", test.joinList());
    }
    */

    /*
    public void testListAsListToStringArray ()
    {
        String[] str = new String[] {"obj1", "obj2"};
    
        test.setList(Arrays.asList(str));
        assertTrue(Arrays.equals(str, test.getListAsStringArray()));
        assertEquals("obj1,obj2", test.joinList());
    }
    */
    /*
    public void testListAsStringArray ()
    {
        String[] str = new String[] {"obj1", "obj2"};
    
        test.setList(str);
        assertTrue(Arrays.equals(str, test.getListAsStringArray()));
        assertEquals("obj1,obj2", test.joinList());
    }
    */
    /*
    public void testListAsListToIntegerArray ()
    {
        Integer[] arr = new Integer[] {new Integer(1), new Integer(2)};
        
        test.setList(Arrays.asList(arr));
        assertTrue(Arrays.equals(arr, test.getListAsIntegerArray()));
        assertEquals("1,2", test.joinList());
    
        assertTrue(Arrays.equals(new int[] {1, 2}, test.getListAsIntArray()));
    }
    */

    /*
    public void testListAsIntArray ()
    {
        int[] arr = new int[] {1, 2, 1};
        
        test.setList(arr);
        assertTrue(Arrays.equals(arr, test.getListAsIntArray()));
        assertEquals("1,2,1", test.joinList());
    }
    */

    /*
      public void testListAsIntegerArray ()
      {
          Integer[] arr = new Integer[] {new Integer(1), new Integer(2)};
        
          test.setList(arr);
          assertTrue(Arrays.equals(arr, test.getListAsIntegerArray()));
          assertEquals("1,2", test.joinList());
      }
    */
    /*
      public void testListAsSet ()
      {
          Set s = new HashSet();
          s.add("obj1");
          s.add("obj2");
          s.add("obj2");
          s.add("obj3");
    
          test.setList(s);
          Set s3 = test.getListAsSet(); // HANGS
          
          assertTrue(s != s3);
          assertEquals(s, s3);
    
          Set s2 = new HashSet();
          s2.add("obj3");
          s2.add("obj1");
          s2.add("obj2");
    
          assertEquals(s2, s3);
      }
    */

    /*
      public void testListAsCollection ()
      {
          List l = new ArrayList();
          l.add("obj1");
          l.add("obj2");
          l.add("obj2");
          l.add("obj3");
    
          test.setList(l);
          assertEquals("obj1,obj2,obj2,obj3", test.joinList());
    
          assertEquals(new LinkedList(l), test.getListAsCollection()); // HANGS
      }
    */

    /*
      public void testListOfJavaObjs ()
      {
          List l = new ArrayList();
          TestIntWrapper obj;
    
          for (int i = 1; i <= 3; i++) {
              l.add(new TestIntWrapperImpl(i));
          }
         
          test.setList(l);
          List l2 = test.getList();
    
          assertTrue(l != l2);
          assertEquals(l, l2);
    
          for (int i = 1; i <= 3; i++) {
              obj = (TestIntWrapper)l2.get(i - 1);
              assertEquals(i, obj.getInteger());
              assertEquals("<java: " + i + ">", obj.toString());
              assertTrue(l.contains(obj));
          }
      }
    */

    /*
        public void testListOfProxies ()
        {
            List l = new ArrayList();
            TestIntWrapper obj;
    
            for (int i = 1; i <= 3; i++) {
                obj = (TestIntWrapper)factory.newProxyObject(TestIntWrapper.class,
                                                             new Long[] {
                                                                 new Long(i)
                                                             });
    
                l.add(obj);
            }
        
            test.setList(l);
            List l2 = test.getList(); // HANGS
    
            assertTrue(l != l2);
            assertEquals(l, l2);
    
            for (int i = 1; i <= 3; i++) {
                obj = (TestIntWrapper)l2.get(i - 1);
                assertEquals(i, obj.getInteger());
                assertEquals("<ruby: " + i + ">", obj.toString());
                assertTrue(l.contains(obj));
            }
        }
    */

    /*
        public void testMap ()
        {
            Map m = new HashMap();
    
            m.put("key1", "value1");
            m.put("key2", "value2");
            m.put("key1", "value3");
    
            test.setMap(m);
            Map m2 = test.getMap();
    
            assertTrue(m != m2);
            assertEquals(m, m2);
        }
    */

    /*
        public void testMapOfJavaObjs ()
        {
            Map m = new HashMap();
            TestIntWrapper obj;
    
            for (int i = 1; i <= 3; i++) {
                m.put(new Long(i), new TestIntWrapperImpl(i));
            }
    
            test.setMap(m);
            Map m2 = test.getMap();
    
            assertTrue(m != m2);
            assertEquals(m, m2);
    
            for (int i = 1; i <= 3; i++) {
                obj = (TestIntWrapper)m2.get(new Long(i));
                assertEquals(i, obj.getInteger());
                assertEquals("<java: " + i + ">", obj.toString());
            }
        }
    */
    /*
        public void testMapOfProxies ()
        {
            Map m = new HashMap();
            TestIntWrapper obj;
    
            for (int i = 1; i <= 3; i++) {
                obj = (TestIntWrapper)factory.newProxyObject(TestIntWrapper.class,
                                                             new Long[] {
                                                                 new Long(i)
                                                             });
    
                m.put(new Long(i), obj);
            }
    
            test.setMap(m);
            Map m2 = test.getMap();
    
            assertTrue(m != m2);
    //            assertEquals(m, m2); // FAILS
    
            for (int i = 1; i <= 3; i++) {
    //                obj = (TestIntWrapper)m2.get(new Long(i));
    //               assertEquals(i, obj.getInteger());
    //                assertEquals("<ruby: " + i + ">", obj.toString());
            }
      }
    */
    /*****************************************************
           Type Coercion Tests -- Not Yet Implemented
     *****************************************************/

    //      public void testDoubleToInt ()
    //      {
    //          test.setNumber(3.1415);
    //          assertEquals(3, test.getNumberAsInt());
    //      }

    //      public void testIntToDouble ()
    //      {
    //          test.setNumber(10);
    //          assertEquals(10.0, test.getNumberAsDouble(), EPSILON);
    //      }
}
