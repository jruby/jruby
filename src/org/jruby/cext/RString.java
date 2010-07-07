/*
 * Copyright (C) 2010 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.jruby.cext;

import org.jruby.RubyString;

/**
 *
 */
final class RString extends Cleaner {
    private final long address;
    
    private RString(RubyString str, long address) {
        super(str);
        this.address = address;
    }

    static RString newRString(RubyString str, long address) {
        RString rstring = new RString(str, address);
        Cleaner.register(rstring);

        return rstring;
    }

    final long address() {
        return address;
    }

    @Override
    void dispose() {
        Native.freeRString(address);
    }
}
