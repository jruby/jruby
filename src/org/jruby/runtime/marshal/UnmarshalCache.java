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
package org.jruby.runtime.marshal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;

public class UnmarshalCache {
    private final Ruby runtime;
    private List links = new ArrayList();
    private List symbols = new ArrayList();

    public UnmarshalCache(Ruby runtime) {
        this.runtime = runtime;
    }

    public void register(IRubyObject value) {
        selectCache(value).add(value);
    }

    private List selectCache(IRubyObject value) {
        return (value instanceof RubySymbol) ? symbols : links;
    }

    public boolean isLinkType(int c) {
        return c == ';' || c == '@';
    }

    public IRubyObject readLink(UnmarshalStream input, int type) throws IOException {
        if (type == '@') {
            return linkedByIndex(input.unmarshalInt());
        }
        assert type == ';';
        return symbolByIndex(input.unmarshalInt());
    }

    private IRubyObject linkedByIndex(int index) {
        try {
            return (IRubyObject) links.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newArgumentError("dump format error (unlinked, index: " + index + ")");
        }
    }

    private RubySymbol symbolByIndex(int index) {
        try {
            return (RubySymbol) symbols.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newTypeError("bad symbol");
        }
    }
}