/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

public class TestBSF extends RubyTestCase {
    private static final String RUBY_SCRIPT = "require 'java'; include_class 'org.jruby.javasupport.test.SimpleInterface'; class Foo < SimpleInterface; def getList; [1,2,3]; end; def getMap; {'A'=>1, 'B' =>2}; end; end";

    BSFManager manager = null;
    
	public TestBSF(String name) {
		super(name);
	}
	
	public void setUp() throws IOException {
    	try {
    	    BSFManager.registerScriptingEngine("ruby", "org.jruby.javasupport.bsf.JRubyEngine", new String[] { "rb" });
    	    
    	    manager = new BSFManager();
    	    manager.exec("ruby", "(java)", 1, 1, RUBY_SCRIPT);
    	} catch (BSFException e) {
            fail("Unable to initialize BSF: " + e);
    	}
	}
	
    public void tearDown() {
        manager = null;
    }
    
    public void testList() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "Foo.new");
			
			for (Iterator e = si.getList().iterator(); e.hasNext(); ) {
				assertTrue(e.next().getClass() == Long.class);
			}	
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }
    
    public void testMap() {
		try {
			SimpleInterface si = (SimpleInterface) manager.eval("ruby", "(java)", 1, 1, "Foo.new");
			Map map = si.getMap();
			
			for (Iterator e = map.keySet().iterator(); e.hasNext(); ) {
				Object key = e.next();
				Object value = map.get(key);
				assertTrue(key.getClass() == String.class);
				assertTrue(value.getClass() == Long.class);
			}	
		} catch (BSFException e) {
			fail("Problem evaluating List Test: " + e);
		}
    }

}
