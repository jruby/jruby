/*
 * RubyHash.java - No description
 * Created on 28. Nov 2001, 15:18
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import junit.framework.*;

import java.io.*;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.regexp.*;

/**
 * @author chadfowler
 */
public class TestRubyHash extends TestRubyBase {

    private RubyHash rubyHash;

    private String result;

    public TestRubyHash(String name) {
	super(name);
    }

    public void setUp() {
	ruby = Ruby.getDefaultInstance(GNURegexpAdapter.class);
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



}
