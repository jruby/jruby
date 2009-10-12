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
 * Copyright (C) 2002-2007 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.apache.bsf.BSFManager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestBSF extends RubyTestCase {
    private static final String RUBY_SCRIPT = "SimpleInterfaceImpl.rb";
    
    BSFManager manager = null;
    
    public TestBSF(String name) {
        super(name);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        BSFManager.registerScriptingEngine("ruby", "org.jruby.javasupport.bsf.JRubyEngine", new String[] { "rb" });
        
        manager = new BSFManager();
        String expression = loadScript(RUBY_SCRIPT);
        assertNotNull("Script loaded from " + RUBY_SCRIPT + " should exist", expression);
        manager.exec("ruby", "(java)", 1, 1, expression);
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
        manager = null;
    }
    
    public void testList() throws Exception {
        SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "SimpleInterfaceImpl.new");
        
        for (Iterator e = si.getList().iterator(); e.hasNext(); ) {
            assertTrue(e.next().getClass() == Long.class);
        }
    }
    
    public void testModifyList() throws Exception{
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
        assertEquals(2, list.indexOf(new Long(3)));
        assertEquals(2, list.lastIndexOf(new Long(3)));
        
        Object[] array = list.toArray();
        
        assertEquals(4, array.length);
        assertEquals(3, ((Number) array[2]).longValue());
        
        List subList = list.subList(0, 2);
        assertEquals(3, subList.size());
        
        //subList.clear();
        // Sublist is supposed to share same backing store as list...TODO in RubyArray.
        //assertTrue(list.size() == 1);
    }
    
    public void testEmptyList() throws Exception {
        SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "EMPTY_LIST = SimpleInterfaceImpl.new");
        List list = si.getEmptyList();
        
        assertEquals(0, list.size());
    }
    
    public void testNilList() throws Exception {
        SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "EMPTY_LIST = SimpleInterfaceImpl.new");
        List list = si.getNilList();
        
        assertTrue(list == null);
        
        si.setNilList(null);
        
        assertTrue(si.isNilListNil());
    }
    
    public void testNestedList() throws Exception {
        SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "NESTED_LIST = SimpleInterfaceImpl.new");
        List list = si.getNestedList();
        
        assertEquals(3, list.size());
        List list2 = (List) list.get(0);
        
        assertEquals(2, list2.size());
        assertEquals(0, list2.indexOf(new Long(1)));
        
        si.modifyNestedList();
        assertEquals("FOO", list.get(0));
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
    public void testMap() throws Exception {
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
            assertEquals(value, valueViaValuesIterator);
            
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
    }
    
    /**
     * Tests the use of RubyHash when used from java.
     * Tests:
     *  RubyHash#entrySet()
     *  RubyHash#entrySet()#iterator()#hasNext()
     *  RubyHash#entrySet()#iterator()#next()
     *  RubyHash#entrySet()#iterator()#next()#setValue()
     */
    public void testMapEntrySetIterator() throws Exception {
        
        class TestMapValue { private int i; private String s; TestMapValue() {i = 1; s="2";} public String toString(){ return s + i; } }
        
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
                assertEquals(1L, entry.getValue());
                // Set a value in the RubyHash
                entry.setValue(new Long(3));
            } else {
                assertEquals("B", entry.getKey());
                assertEquals(2L, entry.getValue());
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
                assertEquals(3L, entry.getValue());
            } else {
                assertTrue(entry.getValue().getClass() == TestMapValue.class);
                assertEquals("B", entry.getKey());
                assertEquals("21", entry.getValue().toString());
            }
        }
    }
    
    /**
     * Tests the use of RubyHash when used from java.
     * Tests:
     *  RubyHash#entrySet()#contains()
     *  RubyHash#entrySet()#remove()
     */
    public void testMapEntrySetContainsAndRemove() throws Exception {
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
    }
    
    public void testModifyMap() throws Exception {
        SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "MODIFY_MAP = SimpleInterfaceImpl.new");
        Map map = si.getMap();
        
        for (Iterator e = map.keySet().iterator(); e.hasNext(); ) {
            Object key = e.next();
            Object value = map.get(key);
            assertTrue(key.getClass() == String.class);
            assertTrue(value.getClass() == Long.class);
            
            map.put(key, new Long(((Number) value).longValue() + 1));
        }
        
        Boolean answer = (Boolean) manager.eval("ruby", "(java)", 1, 1, "{'A'=> 2, 'B' => 3} == MODIFY_MAP.getMap");
        assertTrue(answer.booleanValue());
        
        assertEquals(2, map.size());
        
        Number value = (Number) map.get("B");
        assertEquals(3, value.longValue());
        
        map.remove("B");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsValue(new Long(2)));
        assertTrue(!map.isEmpty());
        
        map.put("C", new Long(4));
        assertTrue(map.containsKey("C"));
        
        HashMap newMap = new HashMap();
        newMap.put("D", "E");
        map.putAll(newMap);
        
        assertEquals(3, map.size());
        
        map.clear();
        assertEquals(0, map.size());
    }
    
    public void testEmptyMap() throws Exception {
        SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "EMPTY_MAP = SimpleInterfaceImpl.new");
        Map map = si.getEmptyMap();
        
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }
    
    public void testNilMap() throws Exception {
        SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "SimpleInterfaceImpl.new");
        Map map = si.getNilMap();
        
        assertTrue(map == null);
        
        si.setNilMap(null);
        
        assertTrue(si.isNilMapNil());
    }
    
    private String loadScript(String fileName) throws Exception {
        InputStream stream = getClass().getResourceAsStream(fileName);
        if (stream == null) {
            // If we're running from within an IDE we may not have
            // the .rb files in our classpath. Try to find them
            // in the filesystem instead.
            stream = new FileInputStream("test/org/jruby/javasupport/test/" + fileName);
        }
        Reader in = new InputStreamReader(stream);
        StringBuffer result = new StringBuffer();
        int length;
        char[] buf = new char[8096];
        while ((length = in.read(buf, 0, buf.length)) >= 0) {
            result.append(buf, 0, length);
        }
        in.close();
        
        return result.toString();
    }
}
