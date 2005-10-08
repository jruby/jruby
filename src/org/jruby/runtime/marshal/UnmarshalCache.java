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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.marshal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.IRuby;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;

public class UnmarshalCache {
    private final IRuby runtime;
    private List links = new ArrayList();
    private List symbols = new ArrayList();

    public UnmarshalCache(IRuby runtime) {
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
