/*
 * TestStringToken.java - No description
 * Created on 16. Dec. 2001, 10:27 
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
package org.jruby.scanner.test;
import junit.framework.TestCase;

import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.IOException;

import org.jruby.scanner.StringToken;

/**
 * @author chadfowler
 */
public class TestStringToken extends TestCase {

    public TestStringToken(String name)
    {
	super(name);
    }

    public void testReplaceAll() {
         StringToken tok = new StringToken(0,"");
         String inString = "This is a\ntest";
         String expected = "This is a\\ntest";
         String newString = tok.replaceAll(inString, "\n", "\\n");
         assertEquals(expected, newString);
    }

    public void testToString() {
         StringToken tok = new StringToken(0,"This is a test line.\nAnd this is the next");
         String expected = "StringToken: String = \"This is a test line.\\nAnd this is the next\"";
         String newString = tok.toString();
         assertEquals(expected, newString);
    }

}
