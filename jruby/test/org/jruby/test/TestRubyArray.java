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
 * Copyright (C) 2006 Ola Bini <Ola.Bini@ki.se>
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
package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.RubyArray;

/**
 * Test case for functionality in RubyArray
 */
public class TestRubyArray extends TestRubyBase {
    private String result;

    public TestRubyArray(final String name) {
        super(name);
    }

    public void setUp() throws Exception {
        if (runtime == null) {
        	runtime = Ruby.getDefaultInstance();
        }
        eval("$h = ['foo','bar']");
    }

    public void tearDown() {
        super.tearDown();
    }

    /**
     * Test literal constructor [], Array[], Array::[], and Array::new with all forms of parameters.
     */
    public void testConstructors() throws Exception {
        result = eval("arr = ['a', 100]; p arr");
        assertEquals("[\"a\", 100]", result);
        result = eval("arr = Array['b', 200]; p arr");
        assertEquals("[\"b\", 200]", result);
        /*
        result = eval("arr = Array::['c', 200]; p arr");
        assertEquals("[\"c\", 200]", result);
        result = eval("arr = Array.['d', 200]; p arr");
        assertEquals("[\"d\", 200]", result);
        */
        result = eval("arr = Array.new(); p arr");
        assertEquals("[]", result);
        result = eval("arr = Array.new(2); p arr");
        assertEquals("[nil, nil]", result);
        result = eval("arr = Array.new(3, 'a'); p arr"); // Init
        assertEquals("[\"a\", \"a\", \"a\"]", result);
        result = eval("arr = Array.new(5) {|i| i*i}; p arr"); // Block
        assertEquals("[0, 1, 4, 9, 16]", result);
        result = eval("arr = Array.new(Array.new(3, 'a')); p arr"); // Copy constructor
        assertEquals("[\"a\", \"a\", \"a\"]", result);
    }

    /**
     * Test Array#[]= (store) and Array#[] (retrieve).  
     */
    public void testLookups() throws Exception {
        // value equality
        //result = eval("key = 3; arr = []; arr[key] = 'one'; arr.store(3, 'two'); puts arr[key]");
        //assertEquals("two", result);
    }

    /**
     * Array#to_s,  Array#to_a
     */
    public void testConversions() throws Exception {
        result = eval("p $h.to_s");
        assertEquals("\"foobar\"", result);
        result = eval("p $h.to_a");
        assertEquals("[\"foo\", \"bar\"]", result);
    }

    /**
     * Array#size,  Array#length, Array#empty?
     */
    public void testSizeRelated() throws Exception {
        assertEquals("2", eval("p $h.size"));
        assertEquals("2", eval("p $h.length"));
        assertEquals("false", eval("p $h.empty?"));
        assertEquals("true", eval("p Array.new().empty?"));
    }

    /**
     * Array#each
     */
    public void testIterating() throws Exception {
        //assertEquals("\"foo\"\n\"bar\"", eval("$h.each {|val| p val}"));
        //assertEquals("[\"foo\", \"bar\"]", eval("p $h.each {|val| }"));
    }

    /**
     * This tests toArray-functionality
     */
    public void testToArray() throws Exception {
        final RubyArray arr = (RubyArray)runtime.evalScript("$h = ['foo','bar']");
        final String val1 = "foo";
        final String val2 = "bar";
        final Object[] outp = arr.toArray();
        assertTrue("toArray should not return null",null != outp);
        assertTrue("toArray should not return empty array",0 != outp.length);
        assertEquals("first element should be \"foo\"",val1,outp[0]);
        assertEquals("second element should be \"bar\"",val2,outp[1]);
        final String[] outp2 = (String[])arr.toArray(new String[0]);
        assertTrue("toArray should not return null",null != outp2);
        assertTrue("toArray should not return empty array",0 != outp2.length);
        assertEquals("first element should be \"foo\"",val1,outp2[0]);
        assertEquals("second element should be \"bar\"",val2,outp2[1]);
        final String[] outp3 = (String[])arr.toArray(new String[arr.size()]);
        assertTrue("toArray should not return null",null != outp3);
        assertTrue("toArray should not return empty array",0 != outp3.length);
        assertEquals("first element should be \"foo\"",val1,outp3[0]);
        assertEquals("second element should be \"bar\"",val2,outp3[1]);
    }
}
