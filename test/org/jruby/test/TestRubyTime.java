/*
 * TestRubyTime.java - No description
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

import java.util.Date;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyTime;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author chadfowler
 */
public class TestRubyTime extends TestCase {
    private Ruby ruby;
    private RubyClass rubyTime;
    private RubyTime nineTeenSeventy;

    public TestRubyTime(String name) {
        super(name);
    }

    public void setUp() {
        if (ruby == null) {
            ruby = Ruby.getDefaultInstance();
        }
        rubyTime = ruby.getClasses().getTimeClass();
        IRubyObject[] args = new IRubyObject[1];
        args[0] = RubyFixnum.newFixnum(ruby, 18000000);
        nineTeenSeventy = RubyTime.s_at(rubyTime, args);
    }

    public void testTimeCreated() {
        assertTrue(rubyTime != null);
        assertEquals(rubyTime.getClassname(), "Time");
    }

    public void testTimeNow() {
        RubyTime myTime = RubyTime.s_new(rubyTime);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException");
        }
        Date now = new Date();
        assertTrue(now.after(myTime.getJavaDate()));
    }

    public void testTimeAt() {
        Date myDate = new Date(18000000);
        assertEquals(myDate, nineTeenSeventy.getJavaDate());
    }

    public void testGmtimeAndZone() {
        assertEquals("GMT", nineTeenSeventy.gmtime().zone().getValue());
    }
}
