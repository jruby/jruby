/*
 * RubyTime.java - No description
 * Created on 1. Dec 2001, 15:53
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
package org.jruby;

import java.text.*;
import java.util.*;

import org.jruby.runtime.*;

/**
 * @author chadfowler
 */
public class RubyTime extends RubyObject {
    private Calendar cal;

    public RubyTime(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public static RubyClass createTimeClass(Ruby ruby) {
        RubyClass timeClass = ruby.defineClass("Time", ruby.getClasses().getObjectClass());
        Callback s_new = new ReflectionCallbackMethod(RubyTime.class, "s_new", false, true);

        timeClass.defineSingletonMethod("new", s_new);
        timeClass.defineSingletonMethod("now", s_new);
        
        timeClass.defineMethod("-", CallbackFactory.getMethod(RubyTime.class, "op_minus", RubyObject.class));

        timeClass.defineMethod("to_s", CallbackFactory.getMethod(RubyTime.class, "to_s"));
        timeClass.defineMethod("inspect", CallbackFactory.getMethod(RubyTime.class, "to_s"));

        return timeClass;
    }

    public static RubyTime s_new(Ruby ruby, RubyObject rubyClass) {
        RubyObject[] args = new RubyObject[1];
        args[0] = new RubyFixnum(ruby, new Date().getTime());
        return s_at(ruby, rubyClass, args);
    }

    public static RubyTime s_at(Ruby ruby, RubyObject rubyClass, RubyObject[] args) {
        long secs = RubyNumeric.num2long(args[0]);
        RubyTime time = new RubyTime(ruby, (RubyClass) rubyClass);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date(secs));
        time.setJavaCalendar(cal);
        return time;
    }

    public RubyTime gmtime(Ruby ruby) {
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        return this;
    }

    public RubyString zone(Ruby ruby) {
        return RubyString.newString(ruby, cal.getTimeZone().getID());
    }
    
    public RubyObject op_minus(RubyObject other) {
        long time = cal.getTimeInMillis();

        if (other instanceof RubyTime) {
            time -= ((RubyTime)other).cal.getTimeInMillis();
        } else {
            time -= (RubyNumeric.fix2long(other) * 1000);
        }
        
        RubyTime newTime = new RubyTime(ruby, getRubyClass());
        newTime.cal = Calendar.getInstance();
        newTime.cal.setTime(new Date(time));
        
		return newTime;
    }
    
    public RubyString to_s() {
        String result = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy").format(cal.getTime());

        return RubyString.newString(ruby, result);
    }

    public void setJavaCalendar(Calendar cal) {
        this.cal = cal;
    }

    public Date getJavaDate() {
        return this.cal.getTime();
    }
}