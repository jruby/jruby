/*
 * TestRubySymbol.java
 * Created on 2002-06-13
 * 
 * Copyright (C) 2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore,
 * Benoit Cerrina, Chad Fowler, Anders Bengtsson
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubySymbol;

import java.util.ArrayList;

public class TestRubySymbol extends TestCase {
    private Ruby runtime;

    public TestRubySymbol(String name) {
	super(name);
    }

    public void setUp() {
        runtime = Ruby.getDefaultInstance();
        runtime.getLoadService().init(runtime, new ArrayList());
    }

    public void testSymbolTable() throws Exception {
        RubySymbol.SymbolTable st = new RubySymbol.SymbolTable();

        assertNull(st.lookup("somename"));
        RubySymbol symbol = RubySymbol.newSymbol(runtime, "somename");
        st.store(symbol);
        assertSame(symbol, st.lookup("somename"));
    }
}
