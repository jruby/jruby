/*
 * RubySymbol.java - No description
 * Created on 26. Juli 2001, 00:01
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby;

import java.util.*;
import java.lang.ref.*;
import org.jruby.runtime.*;
import org.jruby.marshal.*;
import org.jruby.exceptions.TypeError;
import org.jruby.util.Asserts;

/**
 *
 * @author  jpetersen
 */
public class RubySymbol extends RubyObject implements IndexCallable {
    private static int lastId = 0;

    private final String symbol;
    private final int id;

    private RubySymbol(Ruby ruby, String symbol) {
        super(ruby, ruby.getClasses().getSymbolClass());
        this.symbol = symbol;

        synchronized(RubySymbol.class) {
            lastId++;
            this.id = lastId;
        }
    }
    
    /** rb_to_id
     *
     */
    public String toId() {
        return symbol;
    }

    public static RubySymbol nilSymbol(Ruby ruby) {
        return newSymbol(ruby, null);
    }
    
    private static final int M_TO_I = 2;
    private static final int M_TO_S = 3;
    private static final int M_EQUAL = 4;
    private static final int M_HASH = 5;
    private static final int M_INSPECT = 6;
    private static final int M_CLONE = 7;

    public static RubyClass createSymbolClass(Ruby ruby) {
        RubyClass symbolClass = ruby.defineClass("Symbol", ruby.getClasses().getObjectClass());
        
        symbolClass.getInternalClass().undefMethod("new");

        symbolClass.defineMethod("to_i", IndexedCallback.create(M_TO_I, 0));
        symbolClass.defineMethod("to_int", IndexedCallback.create(M_TO_I, 0));
        symbolClass.defineMethod("id2name", IndexedCallback.create(M_TO_S, 0));
        symbolClass.defineMethod("to_s", IndexedCallback.create(M_TO_S, 0));
        
        symbolClass.defineMethod("==", IndexedCallback.create(M_EQUAL, 1));
        symbolClass.defineMethod("hash", IndexedCallback.create(M_HASH, 0));
        symbolClass.defineMethod("inspect", IndexedCallback.create(M_INSPECT, 0));
        symbolClass.defineMethod("dup", IndexedCallback.create(M_CLONE, 0));
        symbolClass.defineMethod("clone", IndexedCallback.create(M_CLONE, 0));
        symbolClass.defineMethod("freeze", CallbackFactory.getSelfMethod(0));
        symbolClass.defineMethod("taint", CallbackFactory.getSelfMethod(0));

        return symbolClass;
    }

    public RubyObject callIndexed(int index, RubyObject[] args) {
        switch (index) {
        case M_TO_S:
            return to_s();
        case M_TO_I:
            return to_i();
        case M_EQUAL:
            return equal(args[0]);
        case M_HASH:
            return hash();
        case M_INSPECT:
            return inspect();
        case M_CLONE:
            return rbClone();
        }
        Asserts.assertNotReached();
        return null;
    }

    public static RubySymbol getSymbol(Ruby ruby, long id) {
        RubySymbol result = ruby.symbolTable.lookup(id);
        if (result == null) {
            return nilSymbol(ruby);
        }
        return result;
    }

    /* Symbol class methods.
     * 
     */

    public static RubySymbol newSymbol(Ruby ruby, String name) {
        RubySymbol result = ruby.symbolTable.lookup(name);
        if (result == null) {
            if (name == null) {
                result = new RubySymbol(ruby, null) {
                        public boolean isNil() {
                            return true;
                        }
                    };
            } else {
                result = new RubySymbol(ruby, name);
            }
            ruby.symbolTable.store(result);
        }
        return result;
    }

    public RubyFixnum to_i() {
        return RubyFixnum.newFixnum(ruby, id);
    }
    
    public RubyString inspect() {
        return RubyString.newString(getRuby(), ":" + symbol);
    }
    
    public RubyString to_s() {
        return RubyString.newString(getRuby(), symbol);
    }

    public RubyBoolean equal(RubyObject other) {
        // Symbol table ensures only one instance for every name,
        // so object identity is enough to compare symbols.
        return RubyBoolean.newBoolean(ruby, this == other);
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(ruby, symbol.hashCode());
    }

    public RubyObject rbClone() {
        throw new TypeError(getRuby(), "can't clone Symbol");
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write(':');
        output.dumpString(symbol);
    }

    public static RubySymbol unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        return RubySymbol.newSymbol(input.getRuby(),
                                    input.unmarshalString());
    }


    public static class SymbolTable {
        /* Using Java's GC to keep the table free from unused symbols. */

        private ReferenceQueue unusedSymbols = new ReferenceQueue();
        private Map table = new HashMap();

        public RubySymbol lookup(String name) {
            clean();
            WeakSymbolEntry ref = (WeakSymbolEntry) table.get(name);
            if (ref == null) {
                return null;
            }
            return (RubySymbol) ref.get();
        }

        public RubySymbol lookup(long symbolId) {
            Iterator iter = table.values().iterator();
            while (iter.hasNext()) {
                WeakSymbolEntry entry = (WeakSymbolEntry) iter.next();
                RubySymbol symbol = (RubySymbol) entry.get();
                if (symbol != null) {
                    if (symbol.id == symbolId) {
                        return symbol;
                    }
                }
            }
            return null;
        }

        public void store(RubySymbol symbol) {
            clean();
            table.put(symbol.toId(), new WeakSymbolEntry(symbol));
        }

        private void clean() {
            WeakSymbolEntry ref;
            while ((ref = (WeakSymbolEntry) unusedSymbols.poll()) != null) {
                table.remove(ref.name());
            }
        }

        private class WeakSymbolEntry extends WeakReference {
            private final String name;

            public WeakSymbolEntry(RubySymbol symbol) {
                super(symbol, unusedSymbols);
                this.name = symbol.toId();
            }

            private String name() {
                return name;
            }
        }
    }
}
