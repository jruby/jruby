/*
 * GlobalVariable.java
 * Created on May 2, 2002
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina,
 * Chad Fowler, Anders Bengtsson
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

package org.jruby.runtime;

import org.jruby.*;
import org.jruby.util.Asserts;
import org.jruby.runtime.builtin.IRubyObject;

public class GlobalVariable {
    protected final Ruby ruby;

    private final String name;
    private IRubyObject value;

    public GlobalVariable(Ruby ruby, String name, IRubyObject value) {
        Asserts.assertExpression(name.startsWith("$"));
        this.ruby = ruby;
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public IRubyObject get() {
        return value;
    }

    public IRubyObject set(IRubyObject value) {
        this.value = value;
        return value;
    }
}
