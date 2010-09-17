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
 * Copyright (C) 2010 Wayne Meissner, Tim Felgentreff
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



package org.jruby.cext;

import org.jruby.RubyArray;

/**
 * An {@link RArray} is an object tied to a {@link RubyArray} for synchronization
 * of array contents between C and Ruby.
 */
public final class RArray extends Cleaner {
    
    private final long address;
    
    private RArray(RubyArray ary, long address) {
        super(ary);
        this.address = address;
    }

    static RArray newRArray(RubyArray ary, long address) {
        RArray rarray = new RArray(ary, address);
        Cleaner.register(rarray);

        return rarray;
    }

    static RArray valueOf(RubyArray ary) {
        RArray rarray = ary.getRArray();
        if (rarray != null) {
            return rarray;
        }
        ary.setRArray(rarray = RArray.newRArray(ary, Native.newRArray()));

        return rarray;
    }

    final long address() {
        return address;
    }

    @Override
    void dispose() {
        Native.freeRArray(address);
    }
}
