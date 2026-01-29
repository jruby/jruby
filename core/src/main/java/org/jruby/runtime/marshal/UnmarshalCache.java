/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

package org.jruby.runtime.marshal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Error.typeError;

@Deprecated(since = "10.0.0.0", forRemoval = true)
@SuppressWarnings("removal")
public class UnmarshalCache {
    private final Ruby runtime;
    private final List<IRubyObject> links = new ArrayList<>();
    private final List<RubySymbol> symbols = new ArrayList<>();
    private final Map<IRubyObject, IRubyObject> partials = new IdentityHashMap<>();

    public UnmarshalCache(Ruby runtime) {
        this.runtime = runtime;
    }

    public boolean isPartialObject(IRubyObject value) {
        return partials.containsKey(value);
    }

    public void markAsPartialObject(IRubyObject value) {
        partials.put(value, value);
    }

    public void noLongerPartial(IRubyObject value) {
        partials.remove(value);
    }

    public IRubyObject readSymbolLink(UnmarshalStream input) throws IOException {
        try {
            return symbols.get(input.unmarshalInt());
        } catch (IndexOutOfBoundsException e) {
            throw typeError(runtime.getCurrentContext(),"bad symbol");
        }
    }

    public IRubyObject readDataLink(UnmarshalStream input) throws IOException {
        int index = input.unmarshalInt();
        try {
            return links.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newArgumentError("dump format error (unlinked, index: " + index + ")");
        }
    }

    public void registerDataLink(IRubyObject value) {
        links.add(value);
    }

    public void registerSymbolLink(RubySymbol value) {
        symbols.add(value);
    }

    // Deprecated: Use readSymbolLink OR readDataLink directly
    @Deprecated(since = "9.4.0.0")
    public IRubyObject readLink(UnmarshalStream input, int type) throws IOException {
        return type == '@' ? readDataLink(input) : readSymbolLink(input);
    }

    // Deprecated: Use registerDataLink or registerSymbolLink directly.
    @Deprecated(since = "9.4.0.0")
    public void register(IRubyObject value) {
        selectCache(value).add(value);
    }

    @Deprecated(since = "9.4.0.0")
    public boolean isLinkType(int c) {
        return c == ';' || c == '@';
    }

    @Deprecated(since = "9.4.0.0")
    private List selectCache(IRubyObject value) {
        return (value instanceof RubySymbol) ? symbols : links;
    }
}
