/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2001 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.exceptions.RaiseException;

/**
 * @author chadfowler
 */
public class TestRubyHash extends TestRubyBase {

    private String result;

    public TestRubyHash(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        eval("$h = {'foo' => 'bar'}");
    }

    /**
     * Test literal constructor {}, Hash::[], and Hash::new with and
     * without the optional default-value argument.
     */
    public void testConstructors() throws Exception {
        result = eval("hash = Hash['b', 200]; p hash");
        assertEquals("{\"b\"=>200}", result);
        result = eval("hash = Hash.new(); p hash['test']");
        assertEquals("nil", result);
        result = eval("hash = Hash.new('default'); p hash['test']");
        assertEquals("\"default\"", result);
    }

    /**
     * Test Hash#[]= (store) and Hash#[] (retrieve).  Also test whether
     * Object#== is properly defined for each class.
     */
    public void testLookups() throws Exception {
        // value equality
        result = eval("key = 'a'; hash = {key => 'one'}; hash.store('a', 'two'); puts hash[key]");
        assertEquals("two", result);
        result = eval("key = [1,2]; hash = {key => 'one'}; hash[[1,2]] = 'two'; puts hash[key]");
        assertEquals("two", result);
        result = eval("key = :a; hash = {key => 'one'}; hash[:a] = 'two'; puts hash[key]");
        assertEquals("two", result);
        result = eval("key = 1234; hash = {key => 'one'}; hash[1234] = 'two'; puts hash[key]");
        assertEquals("two", result);
        result = eval("key = 12.4; hash = {key => 'one'}; hash[12.4] = 'two'; puts hash[key]");
        assertEquals("two", result);
        result = eval("key = 19223372036854775807; hash = {key => 'one'}; hash[19223372036854775807] = 'two'; puts hash[key]");
        assertEquals("two", result);
        // identity equality
        result = eval("key = /a/; hash = {key => 'one'}; hash[/a/] = 'two'; puts hash[key]");
        assertEquals("two", result);
        result = eval("key = (1..3); hash = {key => 'one'}; hash[(1..3)] = 'two'; puts hash[key]");
        assertEquals("two", result);
    }

    /**
     * Hash#to_s,  Hash#to_a, Hash#to_hash
     */
    public void testConversions() throws Exception {
        result = eval("p $h.to_s");
        assertEquals("\"{\\\"foo\\\"=>\\\"bar\\\"}\"", result);
        result = eval("p $h.to_a");
        assertEquals("[[\"foo\", \"bar\"]]", result);
        result = eval("p $h.to_hash");
        assertEquals("{\"foo\"=>\"bar\"}", result);
    }

    /**
     * Hash#size,  Hash#length, Hash#empty?
     */
    public void testSizeRelated() throws Exception {
        assertEquals("1", eval("p $h.size"));
        assertEquals("1", eval("p $h.length"));
        assertEquals("false", eval("p $h.empty?"));
        assertEquals("true", eval("p Hash.new().empty?"));
    }

    /**
     * Hash#each, Hash#each_pair, Hash#each_value, Hash#each_key
     */
    public void testIterating() throws Exception {
        assertEquals("[\"foo\", \"bar\"]", eval("$h.each {|pair| p pair}"));
        assertEquals("{\"foo\"=>\"bar\"}", eval("p $h.each {|pair| }"));
        assertTrue(eval("$h.each_pair {|pair| p pair}").indexOf("[\"foo\", \"bar\"]") != -1);
        assertTrue(eval("p $h.each_pair {|pair| }").indexOf("{\"foo\"=>\"bar\"}") != -1);

        assertEquals("\"foo\"", eval("$h.each_key {|k| p k}"));
        assertEquals("{\"foo\"=>\"bar\"}", eval("p $h.each_key {|k| }"));

        assertEquals("\"bar\"", eval("$h.each_value {|v| p v}"));
        assertEquals("{\"foo\"=>\"bar\"}", eval("p $h.each_value {|v| }"));
    }

    /**
     * Hash#delete, Hash#delete_if, Hash#reject, Hash#reject!
     */
    public void testDeleting() throws Exception {
        eval("$delete_h = {1=>2,3=>4}");
        assertEquals("2", eval("p $delete_h.delete(1)"));
        assertEquals("{3=>4}", eval("p $delete_h"));
        assertEquals("nil", eval("p $delete_h.delete(100)"));
        assertEquals("100", eval("$delete_h.delete(100) {|x| p x }"));

        eval("$delete_h = {1=>2,3=>4,5=>6}");
        assertEquals("{1=>2}", eval("p $delete_h.delete_if {|k,v| k >= 3}"));
        assertEquals("{1=>2}", eval("p $delete_h"));

        eval("$delete_h.clear");
        assertEquals("{}", eval("p $delete_h"));

        eval("$delete_h = {1=>2,3=>4,5=>6}");
        assertEquals("{1=>2}", eval("p $delete_h.reject {|k,v| k >= 3}"));
        assertEquals("3", eval("p $delete_h.size"));

        eval("$delete_h = {1=>2,3=>4,5=>6}");
        eval("p $delete_h");

        assertEquals("{1=>2}", eval("p $delete_h.reject! {|k,v| k >= 3}"));
        assertEquals("1", eval("p $delete_h.size"));
        assertEquals("nil", eval("p $delete_h.reject! {|k,v| false}"));
    }

    /**
     * Hash#default, Hash#default=
     */
    public void testDefault() throws Exception {
        assertEquals("nil", eval("p $h['njet']"));
        assertEquals("nil", eval("p $h.default"));
        eval("$h.default = 'missing'");
        assertEquals("\"missing\"", eval("p $h['njet']"));
        assertEquals("\"missing\"", eval("p $h.default"));
    }

    /**
     * Hash#sort, Hash#invert
     */
    public void testRestructuring() throws Exception {
	eval("$h_sort = {\"a\"=>20,\"b\"=>30,\"c\"=>10}");
	assertEquals("[[\"a\", 20], [\"b\", 30], [\"c\", 10]]",
		     eval("p $h_sort.sort"));
	assertEquals("[[\"c\", 10], [\"a\", 20], [\"b\", 30]]",
		     eval("p $h_sort.sort {|a,b| a[1]<=>b[1]}"));

	eval("$h_invert = {\"n\"=>100,\"y\"=>300,\"d\"=>200,\"a\"=>0}");
	assertEquals("[[0, \"a\"], [100, \"n\"], [200, \"d\"], [300, \"y\"]]",
		     eval("p $h_invert.invert.sort"));
    }
    
    public void testGet() {
        RubyHash rubyHash = new RubyHash(Ruby.getGlobalRuntime());
        assertEquals(null, rubyHash.get("Non matching key"));
    }

    // https://github.com/jruby/jruby/issues/2591
    public void testDoubleQuotedUtf8HashKey() throws Exception {
        assertEquals("UTF-8", eval("# encoding: utf-8\n h = { \"Ãa1\": true }\n puts h.keys.first.encoding"));
    }
}
