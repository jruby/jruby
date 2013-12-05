/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;

public class MarshalCache {
    private final Map<IRubyObject, Integer> linkCache = new IdentityHashMap();
    private final Map<String, Integer> symbolCache = new HashMap();

    public boolean isRegistered(IRubyObject value) {
        if (value instanceof RubySymbol) {
            return isSymbolRegistered(((RubySymbol)value).asJavaString());
        }
        return linkCache.containsKey(value);
    }

    public boolean isSymbolRegistered(String sym) {
        return symbolCache.containsKey(sym);
    }

    public void register(IRubyObject value) {
        if (value instanceof RubySymbol) {
            registerSymbol(value.asJavaString());
        } else {
            linkCache.put(value, Integer.valueOf(linkCache.size()));
        }
    }

    public void registerSymbol(String sym) {
        symbolCache.put(sym, symbolCache.size());
    }

    public void writeLink(MarshalStream output, IRubyObject value) throws IOException {
        if (value instanceof RubySymbol) {
            writeSymbolLink(output, ((RubySymbol)value).asJavaString());
        } else {
            output.write('@');
            output.writeInt(registeredIndex(value));
        }
    }

    public void writeSymbolLink(MarshalStream output, String sym) throws IOException {
        output.write(';');
        output.writeInt(registeredSymbolIndex(sym));
    }

    private int registeredIndex(IRubyObject value) {
        if (value instanceof RubySymbol) {
            return registeredSymbolIndex(value.asJavaString());
        } else {
            return linkCache.get(value).intValue();
        }
    }

    private int registeredSymbolIndex(String sym) {
        return symbolCache.get(sym);
    }
}
