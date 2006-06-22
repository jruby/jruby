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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Derek Berner <derek.berner@state.nm.us>
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
package org.jruby;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.internal.runtime.methods.DirectInvocationMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubySymbol extends RubyObject {
	// FIXME can't use static; would interfere with other runtimes in the same JVM
    private static int lastId = 0;

    private final String symbol;
    private final int id;

    public static abstract class SymbolMethod extends DirectInvocationMethod {
        public SymbolMethod(RubyModule implementationClass, Arity arity, Visibility visibility) {
            super(implementationClass, arity, visibility);
        }
        
        public IRubyObject internalCall(IRuby runtime, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
            RubySymbol s = (RubySymbol)receiver;
            
            return invoke(s, args);
        }
        
        public abstract IRubyObject invoke(RubySymbol target, IRubyObject[] args);
        
    };
    
    private RubySymbol(IRuby runtime, String symbol) {
        super(runtime, runtime.getClass("Symbol"));
        this.symbol = symbol;

        lastId++;
        this.id = lastId;
    }

    /** rb_to_id
     * 
     * @return a String representation of the symbol 
     */
    public String asSymbol() {
        return symbol;
    }

    public boolean isImmediate() {
    	return true;
    }
    
    public boolean singletonMethodsAllowed() {
        return false;
    }

    public static String getSymbol(IRuby runtime, long id) {
        RubySymbol result = runtime.getSymbolTable().lookup(id);
        if (result != null) {
            return result.symbol;
        }
        return null;
    }

    /* Symbol class methods.
     * 
     */

    public static RubySymbol newSymbol(IRuby runtime, String name) {
        RubySymbol result;
        synchronized (RubySymbol.class) {
            // Locked to prevent the creation of multiple instances of
            // the same symbol. Most code depends on them being unique.

            result = runtime.getSymbolTable().lookup(name);
            if (result == null) {
                result = new RubySymbol(runtime, name);
                runtime.getSymbolTable().store(result);
            }
        }
        return result;
    }

    public RubyFixnum to_i() {
        return getRuntime().newFixnum(id);
    }

    public IRubyObject inspect() {
        return getRuntime().newString(":" + symbol);
    }

    public IRubyObject to_s() {
        return getRuntime().newString(symbol);
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(symbol.hashCode());
    }
    
    public IRubyObject to_sym() {
        return this;
    }

    // TODO: Should all immediate classes be subclassed so that clone etc...can inherit 
    // immediate behavior like this.
    public IRubyObject rbClone() {
        throw getRuntime().newTypeError("can't clone Symbol");
    }

    public IRubyObject freeze() {
        return this;
    }

    public IRubyObject taint() {
        return this;
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write(':');
        output.dumpString(symbol);
    }

    public static RubySymbol unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubySymbol result = RubySymbol.newSymbol(input.getRuntime(), input.unmarshalString());
        input.registerLinkTarget(result);
        return result;
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
            table.put(symbol.asSymbol(), new WeakSymbolEntry(symbol, unusedSymbols));
        }

        private void clean() {
            WeakSymbolEntry ref;
            while ((ref = (WeakSymbolEntry) unusedSymbols.poll()) != null) {
                table.remove(ref.name());
            }
        }

        private class WeakSymbolEntry extends WeakReference {
            private final String name;

            public WeakSymbolEntry(RubySymbol symbol, ReferenceQueue queue) {
                super(symbol, queue);
                this.name = symbol.asSymbol();
            }

            public String name() {
                return name;
            }
        }
    }
    
}
