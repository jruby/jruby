/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

public class TestBSF extends RubyTestCase {
    private static final String RUBY_SCRIPT = "SimpleInterfaceImpl.rb";

    BSFManager manager = null;
    
	public TestBSF(String name) {
		super(name);
	}
	
	public void setUp() throws IOException {
    	try {
    	    BSFManager.registerScriptingEngine("ruby", "org.jruby.javasupport.bsf.JRubyEngine", new String[] { "rb" });
    	    
    	    manager = new BSFManager();
    	    manager.exec("ruby", "(java)", 1, 1, loadScript(RUBY_SCRIPT));
    	} catch (BSFException e) {
            fail("Unable to initialize BSF: " + e);
    	}
	}
	
    public void tearDown() {
        manager = null;
    }
 
    
    
    public void testList() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "SimpleInterfaceImpl.new");
			
			for (Iterator e = si.getList().iterator(); e.hasNext(); ) {
				assertTrue(e.next().getClass() == Long.class);
			}	
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }
    
    public void testModifyList() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "MODIFY_LIST = SimpleInterfaceImpl.new");
			List list = si.getList();

			list.set(1, "FOO");
			Boolean answer = (Boolean) manager.eval("ruby", "(java)", 1, 1, "[1, 'FOO', 3] == MODIFY_LIST.getList");
			assertTrue(answer.booleanValue());
			
			list.add(new Long(4));
			answer = (Boolean) manager.eval("ruby", "(java)", 1, 1, "[1, 'FOO', 3, 4] == MODIFY_LIST.getList");
			assertTrue(answer.booleanValue());
			
			list.add(1, new Integer(2));
			answer = (Boolean) manager.eval("ruby", "(java)", 1, 1, "[1, 2, 'FOO', 3, 4] == MODIFY_LIST.getList");
			assertTrue(answer.booleanValue());
			
			list.remove("FOO");
			answer = (Boolean) manager.eval("ruby", "(java)", 1, 1, "[1, 2, 3, 4] == MODIFY_LIST.getList");
			assertTrue(answer.booleanValue());
			
			assertTrue(list.contains(new Long(3)));
			assertTrue(list.indexOf(new Long(3)) == 2);
			assertTrue(list.lastIndexOf(new Long(3)) == 2);
			
			Object[] array = list.toArray();
			
			assertTrue(array.length == 4);
			assertTrue(((Long) array[2]).longValue() == 3);
			
			List subList = list.subList(0, 2);
			assertTrue(subList.size() == 3);
			
			//subList.clear();
			// Sublist is supposed to share same backing store as list...TODO in RubyArray.
			//assertTrue(list.size() == 1);
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }
    
    public void testEmptyList() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "EMPTY_LIST = SimpleInterfaceImpl.new");
			List list = si.getEmptyList();
			
			assertTrue(list.size() == 0);
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }
    
    public void testNilList() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "EMPTY_LIST = SimpleInterfaceImpl.new");
			List list = si.getNilList();
			
			assertTrue(list == null);
			
			si.setNilList(null);
			
			assertTrue(si.isNilListNil());
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }

    public void testNestedList() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "NESTED_LIST = SimpleInterfaceImpl.new");
			List list = si.getNestedList();
			
			assertTrue(list.size() == 3);
			List list2 = (List) list.get(0);
			
			assertTrue(list2.size() == 2);
			assertTrue(list2.indexOf(new Long(1)) == 0);

			si.modifyNestedList();
			assertTrue("FOO".equals(list.get(0)));
			
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }

    /**
     * Tests the use of RubyHash when used from java.
     * Tests:
     *  RubyHash#keySet()
     *  RubyHash#get()
     *  RubyHash#keySet()#iterator()#hasNext()
     *  RubyHash#keySet()#iterator()#next()
     *  RubyHash#keySet()#remove()
     *  RubyHash#keySet()#contains()
     *  RubyHash#keySet()#containsAll()
     *  RubyHash#values()
     *  RubyHash#values()#iterator()
     *  RubyHash#values()#iterator()#hasNext()
     *  RubyHash#values()#iterator()#next()
     *  RubyHash#values()#contains()
     *  RubyHash#values()#containsAll()
     *  RubyHash#values()#remove()
     */
    public void testMap() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "SimpleInterfaceImpl.new");
			Map map = si.getMap();
			
			List values = new ArrayList();
			List keys = new ArrayList();
			

			Iterator valuesIterator = map.values().iterator();
			assertTrue(valuesIterator.hasNext());

			// Iterate over the RubyHash keySet, simultaneously iterating over the values()  
			for (Iterator keySetIterator = map.keySet().iterator(); keySetIterator.hasNext(); ) {
				Object key = keySetIterator.next();
				
				// Get the value from the map via the key
				Object value = map.get(key);
				
				assertTrue(key.getClass() == String.class);
				assertTrue(value.getClass() == Long.class);
				
				// Get the value from the map via the values iterator
	            Object valueViaValuesIterator = valuesIterator.next();
	            
	            // Check the 2 values obtained via different means
				assertTrue(value.equals(valueViaValuesIterator));
				
				// Note that WE CAN'T say the following, because of the on-the-fly conversion of Fixnum to Long 
				// assertTrue(value == valueViaValuesIterator);
	            
				assertTrue(map.keySet().contains(key));
				assertTrue(map.values().contains(value));
			}	
			assertFalse(valuesIterator.hasNext());
			
			assertTrue(map.keySet().containsAll(keys));
			assertTrue(map.values().containsAll(values));

			assertTrue(map.keySet().contains("A"));
			assertTrue(map.values().contains(new Long(1)));
			assertFalse(map.keySet().remove("-"));
			assertTrue(map.keySet().remove("A"));
			assertFalse(map.keySet().contains("A"));
			assertFalse(map.values().contains(new Long(1)));
			
			assertTrue(map.keySet().contains("B"));
			assertTrue(map.values().contains(new Long(2)));
			assertFalse(map.values().remove("-"));
			assertTrue(map.values().remove(new Long(2)));
			assertFalse(map.values().contains(new Long(2)));
			assertFalse(map.keySet().contains("B"));
		
		
		} catch (BSFException e) {
			fail("Problem evaluating Map Test: " + e);
		}
    }

    /**
     * Tests the use of RubyHash when used from java.
     * Tests:
     *  RubyHash#entrySet()
     *  RubyHash#entrySet()#iterator()#hasNext()
     *  RubyHash#entrySet()#iterator()#next()
     *  RubyHash#entrySet()#iterator()#next()#setValue()
     */
    public void testMapEntrySetIterator() {
    	
    	class TestMapValue { private int i; private String s; TestMapValue() {i = 1; s="2";} public String toString(){ return s + i; } } 
    	
        try {
            SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "SimpleInterfaceImpl.new");
            Map map = si.getMap();
            int iteration = 1;
            for (Iterator e = map.entrySet().iterator(); e.hasNext();) {
                Object o = e.next();
                assertNotNull(o);
                Map.Entry entry = (Map.Entry) o;
                assertTrue(entry.getKey().getClass() == String.class);
                assertTrue(entry.getValue().getClass() == Long.class);
                if (iteration++ == 1) {
                    assertEquals("A", entry.getKey());
                    assertEquals(new Long(1L), entry.getValue());
                    // Set a value in the RubyHash
                    entry.setValue(new Long(3));
                } else {
                    assertEquals("B", entry.getKey());
                    assertEquals(new Long(2L), entry.getValue());
                    // Set a value in the RubyHash
                    entry.setValue(new TestMapValue());
                }
            }
            // Check the entry.setValue values come back out ok
            
            iteration = 1;
            for (Iterator e = map.entrySet().iterator(); e.hasNext();) {
                Object o = e.next();
                assertNotNull(o);
                Map.Entry entry = (Map.Entry) o;
                assertTrue(entry.getKey().getClass() == String.class);
                if (iteration++ == 1) {
                    assertTrue(entry.getValue().getClass() == Long.class);
                    assertEquals("A", entry.getKey());
                    assertEquals(new Long(3L), entry.getValue());
                } else {
                    assertTrue(entry.getValue().getClass() == TestMapValue.class);
                    assertEquals("B", entry.getKey());
                    assertEquals("21", entry.getValue().toString());
                }
            }
        } catch (BSFException e) {
            fail("Problem evaluating testMapEntrySetIterator Test: " + e);
        }
    }

    /**
     * Tests the use of RubyHash when used from java.
     * Tests:
     *  RubyHash#entrySet()#contains()
     *  RubyHash#entrySet()#remove()
     */
    public void testMapEntrySetContainsAndRemove() {
        try {
            SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "SimpleInterfaceImpl.new");
            Map map = si.getMap();
            Set entrySet = map.entrySet();
			Iterator e = entrySet.iterator();
            Object next1 = e.next();
            Object next2 = e.next();
            assertFalse(e.hasNext());
			assertTrue(entrySet.contains(next1));
			assertTrue(entrySet.contains(next2));
			entrySet.remove(next1);
			assertFalse(entrySet.contains(next1));
			entrySet.remove(next2);			
			assertFalse(entrySet.contains(next2));
        } catch (BSFException e) {
            fail("Problem evaluating testMapEntrySetContainsAndRemove Test: " + e);
        }
    }            
    
    public void testModifyMap() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "MODIFY_MAP = SimpleInterfaceImpl.new");
			Map map = si.getMap();

			for (Iterator e = map.keySet().iterator(); e.hasNext(); ) {
				Object key = e.next();
				Object value = map.get(key);
				assertTrue(key.getClass() == String.class);
				assertTrue(value.getClass() == Long.class);
				
				map.put(key, new Long(((Long) value).longValue() + 1));
			}
			
			Boolean answer = (Boolean) manager.eval("ruby", "(java)", 1, 1, "{'A'=> 2, 'B' => 3} == MODIFY_MAP.getMap");
			assertTrue(answer.booleanValue());
			
			assertTrue(map.size() == 2);
			
			Long value = (Long) map.get("B");
			assertTrue(value.longValue() == 3);
			
			map.remove("B");
			assertTrue(map.size() == 1);
			assertTrue(map.containsKey("A"));
			assertTrue(map.containsValue(new Long(2)));
			assertTrue(!map.isEmpty());
			
			map.put("C", new Long(4));
			assertTrue(map.containsKey("C"));
			
			HashMap newMap = new HashMap();
			newMap.put("D", "E");
			map.putAll(newMap);
			
			assertTrue(map.size() == 3);
			
			map.clear();
			assertTrue(map.size() == 0);
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }
	
    public void testEmptyMap() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "EMPTY_MAP = SimpleInterfaceImpl.new");
			Map map = si.getEmptyMap();
			
			assertTrue(map.size() == 0);
			assertTrue(map.isEmpty());
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }
    
    public void testNilMap() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "SimpleInterfaceImpl.new");
			Map map = si.getNilMap();
			
			assertTrue(map == null);
			
			si.setNilMap(null);
			
			assertTrue(si.isNilMapNil());
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }


    private String loadScript(String fileName) {
        try {
            Reader in = new InputStreamReader(getClass().getResourceAsStream(fileName));
            StringBuffer result = new StringBuffer();
            int length;
            char[] buf = new char[8096];
            while ((length = in.read(buf, 0, buf.length)) >= 0) {
            	result.append(buf, 0, length);
            }
            in.close();

            return result.toString();
        } catch (Exception ex) {}
        
        return null;
    }

}
