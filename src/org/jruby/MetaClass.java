/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby;

import org.jruby.exceptions.FrozenError;
import org.jruby.exceptions.TypeError;
import org.jruby.util.Asserts;

public class MetaClass extends RubyClass implements IMetaClass {
    public RubyClass type;

    public MetaClass(Ruby runtime, RubyClass type) {
        super(runtime, runtime.getClasses().getClassClass(), type);
        Asserts.notNull(type);

        this.type = type;
    }

    public MetaClass(Ruby runtime, RubyClass type, RubyClass superClass) {
        super(runtime, type, superClass);
    }
    public boolean isSingleton() {
        return true;
    }

    protected RubyClass subclass() {
        throw new TypeError(runtime, "can't make subclass of virtual class");
    }

    protected void testFrozen() {
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "object");
        }
    }

}