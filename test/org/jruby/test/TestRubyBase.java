/*
 * RubyHash.java - No description
 * Created on 28. Nov 2001, 15:18
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <japetersen@web.de>
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
import org.jruby.RubyIO;
import junit.framework.TestCase;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

/**
 * @author Benoit
 */
public class TestRubyBase extends TestCase {
    protected Ruby ruby;
    private PrintStream out;

    public TestRubyBase(String name) {
        super(name);
    }

    /**
     * evaluate a string and returns the standard output.
     * @param script the String to eval as a String
     * @return the value printed out on  stdout and stderr by 
     **/
    protected String eval(String script) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        out = new PrintStream(result);
        RubyIO lStream = new RubyIO(ruby);
        lStream.initIO(null, out, null);
        ruby.getGlobalVariables().set("$stdout", lStream);
        ruby.getGlobalVariables().set("$>", lStream);
        lStream = (RubyIO) ruby.getGlobalVariables().get("$stderr");
        lStream.initIO(null, out, null);
        ruby.getGlobalVariables().set("$stderr", lStream);
        
        ruby.loadScript("test", new StringReader(script), false);
        
        StringBuffer sb = new StringBuffer(new String(result.toByteArray()));
        for (int idx = sb.indexOf("\n"); idx != -1; idx = sb.indexOf("\n")) {
            sb.deleteCharAt(idx);
        }
        
        return sb.toString();
    }

    public void tearDown() {
        if (out != null)
            out.close();
    }
}