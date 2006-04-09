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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
package org.jruby.test;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyNil;
import org.jruby.runtime.builtin.IRubyObject;

/**
* @author chadfowler
*/
public class TestRubyNil extends TestCase {
    private IRuby runtime;
    private IRubyObject rubyNil;

    public TestRubyNil(String name) {
        super(name);
    } 
    
    public void setUp() {
        runtime = Ruby.getDefaultInstance();
        rubyNil = runtime.getNil();
    }
    
    public void testIsNil() {
        assertTrue(rubyNil.isNil());
    }

    public void testIsFalseOrTrue() {
        assertTrue(!rubyNil.isTrue());
    }

    public void testToI() {
        assertEquals(RubyFixnum.zero(runtime), RubyNil.to_i(rubyNil));
    }

    public void testToS() {
        assertEquals("", RubyNil.to_s(rubyNil).toString());
    }

    public void testToA() {
        assertEquals(new ArrayList(), RubyNil.to_a(rubyNil).getList());
    }

    public void testInspect() {
        assertEquals("nil", RubyNil.inspect(rubyNil).toString());
    }

    public void testType() {
        assertEquals("NilClass", RubyNil.type(rubyNil).name().toString());
    }

    public void testOpAnd() {
        assertTrue(RubyNil.op_and(rubyNil, rubyNil).isFalse());
    }
  
    public void testOpOr() {
        assertTrue(RubyNil.op_or(rubyNil, runtime.getTrue()).isTrue());
        assertTrue(RubyNil.op_or(rubyNil, runtime.getFalse()).isFalse());
    }

    public void testOpXOr() {
        assertTrue(RubyNil.op_xor(rubyNil, runtime.getTrue()).isTrue());
        assertTrue(RubyNil.op_xor(rubyNil, runtime.getFalse()).isFalse());
    }
}
