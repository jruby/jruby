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

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

public class ObjectClass {
    private IRubyObject object;

    private IMetaClass metaClass;
    private RubyClass type;

    public ObjectClass(IRubyObject object) {
        Asserts.notNull(object);

        this.object = object;
    }

    public IMetaClass getMetaClass() {
        if (metaClass == null) {
            // create a new metaclass.
            metaClass = (IMetaClass)((RubyObject)object).makeMetaClass(type);
        }

        return metaClass;
    }

    public RubyClass getType() {
        return type;
    }

    public void setType(RubyClass type) {
        Asserts.notNull(type);

        this.type = type;
    }
}