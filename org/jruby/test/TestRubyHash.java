/*
 * RubyHash.java - No description
 * Created on 28. Nov 2001, 15:18
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.RubyHash;

/**
 * @author chadfowler
 * @version $Revision$
 */
public class TestRubyHash extends TestRubyBase {

    private RubyHash rubyHash;
    private String result;

    public TestRubyHash(String name) {
        super(name);
    }

    public void setUp() {
        ruby = Ruby.getDefaultInstance(null);
        eval("$h = {'foo' => 'bar'}");
    }

    public void tearDown() {
        super.tearDown();
    }

    /**
     * Test literal constructor {}, Hash::[], and Hash::new with and
     * without the optional default-value argument.
     */
    public void testConstructors() {
        result = eval("hash = {'a', 100}; p hash");
        assertEquals("{\"a\"=>100}", result);
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
    public void testLookups() {
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
        assertEquals("one", result);
        result = eval("key = (1..3); hash = {key => 'one'}; hash[(1..3)] = 'two'; puts hash[key]");
        assertEquals("one", result);
    }

    /**
     * Hash#to_s,  Hash#to_a, Hash#to_hash
     */
    public void testConversions() {
        result = eval("p $h.to_s");
        assertEquals("\"foobar\"", result);
        result = eval("p $h.to_a");
        assertEquals("[[\"foo\", \"bar\"]]", result);
        result = eval("p $h.to_hash");
        assertEquals("{\"foo\"=>\"bar\"}", result);
    }

    /**
     * Hash#size,  Hash#length, Hash#empty?
     */
    public void testSizeRelated() {
        assertEquals("1", eval("p $h.size"));
        assertEquals("1", eval("p $h.length"));
        assertEquals("false", eval("p $h.empty?"));
        assertEquals("true", eval("p Hash.new().empty?"));
    }

    /**
     * Hash#each, Hash#each_pair, Hash#each_value, Hash#each_key
     */
    public void testIterating() {
        assertEquals("[\"foo\", \"bar\"]", eval("$h.each {|pair| p pair}"));
        assertEquals("{\"foo\"=>\"bar\"}", eval("p $h.each {|pair| }"));
        assertEquals("[\"foo\", \"bar\"]", eval("$h.each_pair {|pair| p pair}"));
        assertEquals("{\"foo\"=>\"bar\"}", eval("p $h.each_pair {|pair| }"));

        assertEquals("\"foo\"", eval("$h.each_key {|k| p k}"));
        assertEquals("{\"foo\"=>\"bar\"}", eval("p $h.each_key {|k| }"));

        assertEquals("\"bar\"", eval("$h.each_value {|v| p v}"));
        assertEquals("{\"foo\"=>\"bar\"}", eval("p $h.each_value {|v| }"));
    }

    /**
     * Hash#delete, Hash#delete_if, Hash#reject, Hash#reject!
     */
    public void testDeleting() {
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
    public void testDefault() {
        assertEquals("nil", eval("p $h['njet']"));
        assertEquals("nil", eval("p $h.default"));
        eval("$h.default = 'missing'");
        assertEquals("\"missing\"", eval("p $h['njet']"));
        assertEquals("\"missing\"", eval("p $h.default"));
    }

    /**
     * Hash#sort, Hash#invert
     */
    public void testRestructuring() {
	eval("$h_sort = {\"a\"=>20,\"b\"=>30,\"c\"=>10}");
	assertEquals("[[\"a\", 20], [\"b\", 30], [\"c\", 10]]",
		     eval("p $h_sort.sort"));
	assertEquals("[[\"c\", 10], [\"a\", 20], [\"b\", 30]]",
		     eval("p $h_sort.sort {|a,b| a[1]<=>b[1]}"));

	eval("$h_invert = {\"n\"=>100,\"y\"=>300,\"d\"=>200,\"a\"=>0}");
	assertEquals("[[0, \"a\"], [100, \"n\"], [200, \"d\"], [300, \"y\"]]",
		     eval("p $h_invert.invert.sort"));
    }
}
