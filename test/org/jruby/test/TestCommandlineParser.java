/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.util.CommandlineParser;

public class TestCommandlineParser extends TestCase {
    public TestCommandlineParser(String name) {
        super(name);
    }

    public void testParsing() {
        CommandlineParser c = new CommandlineParser(new String[] { "-e", "hello", "-e", "world" });
        assertEquals("hello\nworld\n", c.inlineScript());
        assertNull(c.scriptFileName);
        assertEquals("-e", c.displayedFileName());

        c = new CommandlineParser(new String[] { "--version" });
        assertTrue(c.showVersion);

        c = new CommandlineParser(new String[] { "-n", "myfile.rb" });
        assertTrue(c.assumeLoop);
        assertEquals("myfile.rb", c.scriptFileName);
        assertEquals("myfile.rb", c.displayedFileName());

        c = new CommandlineParser(new String[0]);
        assertEquals("-", c.displayedFileName());
    }
}
