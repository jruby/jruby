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
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class UnmarshalCache {
    private final Ruby runtime;
    private final List<IRubyObject> links = new ArrayList<>();
    private final List<SymbolTuple> symbols = new ArrayList<>();

    /**
     * Representation of a linked symbol from the unmarshal stream.
     *
     * In order to unmarshal a new symbol, we must first pull in the bytes and register them in this cache, before we
     * can proceed to unmarshal any encoding information that follows them. The cache holds a tuple of ByteList,
     * RubySymbol so this is possible. Once the ByteList has been registered, we proceed to unmarshal the encoding,
     * apply it to the ByteList, and then register the symbol with the global symbol table. By updating the tuple, we
     * avoid having to look up the symbol again for a subsequent link as in CRuby.
     */
    public class SymbolTuple {
        final ByteList symbolBytes;
        RubySymbol symbol;

        SymbolTuple(ByteList symbolBytes) {
            this.symbolBytes = symbolBytes;
        }

        public RubySymbol getSymbol() {
            RubySymbol symbol = this.symbol;

            if (symbol == null) {
                symbol = this.symbol = RubySymbol.newSymbol(runtime, symbolBytes);
            }

            return symbol;
        }
    }

    public UnmarshalCache(Ruby runtime) {
        this.runtime = runtime;
    }

    public void register(IRubyObject value) {
        links.add(value);
    }

    public SymbolTuple registerSymbol(ByteList value) {
        SymbolTuple tuple = new SymbolTuple(value);
        symbols.add(tuple);
        return tuple;
    }

    public boolean isLinkType(int c) {
        return c == '@';
    }

    public boolean isSymbolType(int c) {
        return c == ';';
    }

    public IRubyObject readLink(UnmarshalStream input) throws IOException {
        return linkedByIndex(input.unmarshalInt());
    }

    public RubySymbol readSymbol(UnmarshalStream input) throws IOException {
        return symbolByIndex(input.unmarshalInt());
    }

    private IRubyObject linkedByIndex(int index) {
        try {
            return links.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newArgumentError("dump format error (unlinked, index: " + index + ")");
        }
    }

    private RubySymbol symbolByIndex(int index) {
        try {
            return symbols.get(index).getSymbol();
        } catch (IndexOutOfBoundsException e) {
            throw runtime.newTypeError("bad symbol");
        }
    }
}
