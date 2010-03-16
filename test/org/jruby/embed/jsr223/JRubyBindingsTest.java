/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.jsr223;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.script.ScriptEngine;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Yoko Harada
 */
public class JRubyBindingsTest {
    String basedir = System.getProperty("user.dir");
    
    static Logger logger0 = Logger.getLogger(JRubyBindingsTest.class.getName());
    static Logger logger1 = Logger.getLogger(JRubyBindingsTest.class.getName());
    static OutputStream outStream = null;

    public JRubyBindingsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        outStream.close();
    }

    @Before
    public void setUp() throws FileNotFoundException {
        outStream = new FileOutputStream(basedir + "/build/test-results/run-junit-embed.log", true);
        Handler handler = new StreamHandler(outStream, new SimpleFormatter());
        logger0.addHandler(handler);
        logger0.setUseParentHandlers(false);
        logger0.setLevel(Level.INFO);
        logger1.setUseParentHandlers(false);
        logger1.addHandler(new ConsoleHandler());
        logger1.setLevel(Level.WARNING);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of size method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testSize() {
        logger1.info("size");
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        int expResult = 0;
        int result = instance.size();
        assertEquals(expResult, result);
        instance.put("abc", "abc");
        instance.put("$abc", "aabc");
        instance.put(ScriptEngine.FILENAME, "abc");
        expResult = 3;
        result = instance.size();
        assertEquals(expResult, result);

        instance.clear();
    }

    /**
     * Test of isEmpty method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testIsEmpty() {
        logger1.info("isEmpty");
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        boolean expResult = true;
        boolean result = instance.isEmpty();
        assertEquals(expResult, result);
        instance.put(ScriptEngine.FILENAME, "abc");
        expResult = false;
        result = instance.isEmpty();
        assertEquals(expResult, result);
        instance.clear();
        expResult = true;
        result = instance.isEmpty();
        assertEquals(expResult, result);
    }

    /**
     * Test of containsKey method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testContainsKey() {
        logger1.info("containsKey");
        String key = "abc";
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        boolean expResult = false;
        boolean result = instance.containsKey(key);
        assertEquals(expResult, result);
        instance.put("@abc", "ahhh");
        instance.put(ScriptEngine.FILENAME, "filename");
        expResult = true;
        key = ScriptEngine.FILENAME;
        result = instance.containsKey(key);
        assertEquals(expResult, result);
        key = "@abc";
        result = instance.containsKey(key);
        assertEquals(expResult, result);
    }

    /**
     * Test of containsValue method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testContainsValue() {
        logger1.info("containsValue");
        Object value = null;
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        boolean expResult = false;
        boolean result = instance.containsValue(value);
        assertEquals(expResult, result);
        instance.put(ScriptEngine.FILENAME, "filename");
        expResult = true;
        value = "filename";
        result = instance.containsValue(value);
        assertEquals(expResult, result);
        instance.put("filename", "filename");
        result = instance.containsValue(value);
        assertEquals(expResult, result);
    }

    /**
     * Test of get method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testGet() {
        logger1.info("get");
        String key = null;
        Object result = null;
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        Object expResult = null;
        try {
            result = instance.get(key);
        } catch (NullPointerException e) {
            expResult = "key is null";
            result = e.getMessage();
            assertEquals(expResult, result);
        }
        instance.put("abc", "aabc");
        instance.put("@abc", "abbc");
        instance.put(ScriptEngine.FILENAME, "defdef");
        expResult = "defdef";
        result = instance.get(ScriptEngine.FILENAME);
        assertEquals(expResult, result);
        expResult = "abbc";
        result = instance.get("@abc");
        assertEquals(expResult, result);
    }

    /**
     * Test of put method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testPut() {
        logger1.info("put");
        String key = "";
        Object value = null;
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        Object expResult = null;
        Object result = null;
        try {
            result = instance.put(key, value);
        } catch (IllegalArgumentException e) {
            expResult = "key is empty";
            result = e.getMessage();
            assertEquals(expResult, result);
        }
        instance.put("@abc", "aabc");
        result = instance.put("@abc", "abcd");
        expResult = "aabc";
        assertEquals(expResult, result);
        instance.put(ScriptEngine.LANGUAGE, "ruby");
        expResult = "ruby";
        result = instance.put(ScriptEngine.LANGUAGE, "jruby");
        assertEquals(expResult, result);
    }

    /**
     * Test of remove method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testRemove() {
        logger1.info("remove");
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        instance.put(ScriptEngine.FILENAME, "filename");
        String key = ScriptEngine.FILENAME;
        Object expResult = "filename";
        Object result = instance.remove(key);
        assertEquals(expResult, result);
        expResult = null;
        key = "$abc";
        result = instance.remove(key);
        assertEquals(expResult, result);
    }

    /**
     * Test of putAll method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testPutAll() {
        logger1.info("putAll");
        Map t = null;
        Object expResult = null;
        Object result = null;
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        try {
            instance.putAll(t);
        } catch (NullPointerException e) {
            expResult = "map is null";
            assertEquals(expResult, e.getMessage());
        }
        instance.put("ABC", "aaaa");
        t = new HashMap();
        t.put("abc", "aabc");
        t.put("@abc", "abbc");
        t.put("$abc", "abcc");
        instance.putAll(t);
        expResult = 4;
        result = instance.size();
        assertEquals(expResult, result);
        t.put(ScriptEngine.FILENAME, "filename");
        instance.putAll(t);
        expResult = 5;
        result = instance.size();
        assertEquals(expResult, result);
    }

    /**
     * Test of clear method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testClear() {
        logger1.info("clear");
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        instance.put("@abc", "abbc");
        instance.put("$abc", "abcc");
        instance.put(ScriptEngine.FILENAME, "filename");
        assertEquals(3, instance.size());
        instance.clear();
        Object expResult = 0;
        Object result = instance.size();
        assertEquals(expResult, result);
    }

    /**
     * Test of keySet method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testKeySet() {
        logger1.info("keySet");
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        instance.put("@abc", "abbc");
        instance.put("$abc", "abcc");
        instance.put(ScriptEngine.FILENAME, "filename");
        Set result = instance.keySet();
        assertEquals(3, result.size());
        for (Object key : result) {
            logger1.info(key.toString());
        }
    }

    /**
     * Test of values method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testValues() {
        logger1.info("values");
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        instance.put("@abc", "abbc");
        instance.put("$abc", "abcc");
        instance.put(ScriptEngine.FILENAME, "filename");
        Collection result = instance.values();
        assertEquals(3, result.size());
        for (Object value : result) {
            logger1.info(value.toString());
        }
    }

    /**
     * Test of entrySet method, of class Jsr223JRubyBindings.
     */
    @Test
    public void testEntrySet() {
        logger1.info("entrySet");
        JRubyBindings instance = new JRubyBindings(new ScriptingContainer(LocalContextScope.THREADSAFE));
        instance.put("@abc", "abbc");
        instance.put("$abc", "abcc");
        instance.put(ScriptEngine.FILENAME, "filename");
        Set<Map.Entry> result = instance.entrySet();
        assertEquals(3, result.size());
        for (Map.Entry entry : result) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            logger1.info(key + ": " + value);
        }
    }
}