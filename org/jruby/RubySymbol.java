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

/**
 *
 * @author  jpetersen
 */
public class RubySymbol extends RubyObject {
    private final String symbol;

    public RubySymbol(Ruby ruby, String symbol) {
        super(ruby, ruby.getClasses().getSymbolClass());
        this.symbol = symbol;
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
    
    public static RubyClass createSymbolClass(Ruby ruby) {
        RubyClass symbolClass = ruby.defineClass("Symbol", ruby.getClasses().getObjectClass());
        
        symbolClass.getRubyClass().undefMethod("new");
        
        symbolClass.defineMethod("id", CallbackFactory.getMethod(RubySymbol.class, "id"));
        symbolClass.defineMethod("to_i", CallbackFactory.getMethod(RubySymbol.class, "to_i"));
        symbolClass.defineMethod("to_int", CallbackFactory.getMethod(RubySymbol.class, "to_i"));
        symbolClass.defineMethod("id2name", CallbackFactory.getMethod(RubySymbol.class, "to_s"));
        symbolClass.defineMethod("to_s", CallbackFactory.getMethod(RubySymbol.class, "to_s"));
        
        symbolClass.defineMethod("==", CallbackFactory.getMethod(RubySymbol.class, "equal", RubyObject.class));
		symbolClass.defineMethod("hash", CallbackFactory.getMethod(RubySymbol.class, "hash"));
        symbolClass.defineMethod("inspect", CallbackFactory.getMethod(RubySymbol.class, "inspect"));
        symbolClass.defineMethod("dup", CallbackFactory.getMethod(RubySymbol.class, "rbClone"));
        symbolClass.defineMethod("clone", CallbackFactory.getMethod(RubySymbol.class, "rbClone"));
        symbolClass.defineMethod("freeze", CallbackFactory.getSelfMethod(0));
        symbolClass.defineMethod("taint", CallbackFactory.getSelfMethod(0));

        return symbolClass;
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(ruby, symbol.hashCode());
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

    public RubyFixnum id() {
        return RubyFixnum.newFixnum(getRuby(), symbol.hashCode());
    }

    public RubyFixnum to_i() {
        return id();
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
