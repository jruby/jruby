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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

import java.util.ArrayList;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubySymbol;

public class TestRubySymbol extends TestCase {
    private Ruby runtime;

    public TestRubySymbol(String name) {
	super(name);
    }

    public void setUp() {
        runtime = Ruby.newInstance();
    }

    public void testSymbolTable() throws Exception {
        RubySymbol.SymbolTable st = runtime.getSymbolTable();

        assertNull(st.lookup("somename"));
        
        RubySymbol symbol = RubySymbol.newSymbol(runtime, "somename");
        assertSame(symbol, st.lookup("somename"));
        assertSame(symbol, st.getSymbol("somename"));
        assertSame(symbol, st.fastGetSymbol("somename"));
        
        RubySymbol another = st.fastGetSymbol("another_name");
        assertSame(another, st.lookup("another_name"));
        assertSame(another, st.getSymbol("another_name"));
        assertSame(another, st.fastGetSymbol("another_name"));
    }
    
    public void testSymbolHashCode() {
        RubySymbol sym = RubySymbol.newSymbol(runtime, "somename");
        assertTrue(sym.hashCode() != 0);
        assertTrue(sym.hashCode() != sym.getId());
        if (runtime.isSiphashEnabled()) {
            assertEquals(1706472664, sym.hashCode());
        }
    }
}
