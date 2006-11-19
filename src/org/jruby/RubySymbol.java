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
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.internal.runtime.methods.DirectInvocationMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
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
        
        public IRubyObject internalCall(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
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
        return getRuntime().newString(":" + 
            (isSymbolName(symbol) ? symbol : getRuntime().newString(symbol).dump().toString())); 
    }

    public IRubyObject to_s() {
        return getRuntime().newString(symbol);
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }
    
    public int hashCode() {
        return symbol.hashCode();
    }
    
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        
        if (other instanceof RubySymbol) {
            RubySymbol sym = (RubySymbol)other;
            
            if (sym.symbol == symbol) {
                return true;
            }
        }
        
        return false;
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
    
    private static boolean isIdentStart(char c) {
        return ((c >= 'a' && c <= 'z')|| (c >= 'A' && c <= 'Z')
                || c == '_');
    }
    private static boolean isIdentChar(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
                || c == '_');
    }
    
    private static boolean isIdentifier(String s) {
        if (s == null || s.length() <= 0) {
            return false;
        } 
        
        if (!isIdentStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!isIdentChar(s.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * is_special_global_name from parse.c.  
     * @param s
     * @return
     */
    private static boolean isSpecialGlobalName(String s) {
        if (s == null || s.length() <= 0) {
            return false;
        }

        int length = s.length();
           
        switch (s.charAt(0)) {        
        case '~': case '*': case '$': case '?': case '!': case '@': case '/': case '\\':        
        case ';': case ',': case '.': case '=': case ':': case '<': case '>': case '\"':        
        case '&': case '`': case '\'': case '+': case '0':
            return length == 1;            
        case '-':
            return (length == 1 || (length == 2 && isIdentChar(s.charAt(1))));
            
        default:
            // we already confirmed above that length > 0
            for (int i = 0; i < length; i++) {
                if (!Character.isDigit(s.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private static boolean isSymbolName(String s) {
        if (s == null || s.length() < 1) {
            return false;
        }

        int length = s.length();

        char c = s.charAt(0);
        switch (c) {
        case '$':
            return length > 1 && isSpecialGlobalName(s.substring(1));
        case '@':
            int offset = 1;
            if (length >= 2 && s.charAt(1) == '@') {
                offset++;
            }

            return isIdentifier(s.substring(offset));
        case '<':
            return (length == 1 || (length == 2 && (s.equals("<<") || s.equals("<="))) || 
                    (length == 3 && s.equals("<=>")));
        case '>':
            return (length == 1) || (length == 2 && (s.equals(">>") || s.equals(">=")));
        case '=':
            return ((length == 2 && (s.equals("==") || s.equals("=~"))) || 
                    (length == 3 && s.equals("===")));
        case '*':
            return (length == 1 || (length == 2 && s.equals("**")));
        case '+':
            return (length == 1 || (length == 2 && s.equals("+@")));
        case '-':
            return (length == 1 || (length == 2 && s.equals("-@")));
        case '|': case '^': case '&': case '/': case '%': case '~': case '`':
            return length == 1;
        case '[':
            return s.equals("[]") || s.equals("[]=");
        }
        
        if (!isIdentStart(c)) {
            return false;
        }

        boolean localID = (c >= 'a' && c <= 'z');
        int last = 1;
        
        for (; last < length; last++) {
            char d = s.charAt(last);
            
            if (!isIdentChar(d)) {
                break;
            }
        }
                    
        if (last == length) {
            return true;
        } else if (localID && last == length - 1) {
            char d = s.charAt(last);
            
            return d == '!' || d == '?' || d == '=';
        }
        
        return false;
    }
    
 

    public static RubySymbol unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubySymbol result = RubySymbol.newSymbol(input.getRuntime(), input.unmarshalString());
        input.registerLinkTarget(result);
        return result;
    }

    public static class SymbolTable {
       
        private Map table = new HashMap();
        
        public IRubyObject[] all_symbols() {
            int length = table.size();
            IRubyObject[] array = new IRubyObject[length];
            System.arraycopy(table.values().toArray(), 0, array, 0, length);
            return array;
        }
        
        public RubySymbol lookup(long symbolId) {
            Iterator iter = table.values().iterator();
            while (iter.hasNext()) {
                RubySymbol symbol = (RubySymbol) iter.next();
                if (symbol != null) {
                    if (symbol.id == symbolId) {
                        return symbol;
                    }
                }
            }
            return null;
        }
        
        public RubySymbol lookup(String name) {
            return (RubySymbol) table.get(name);
        }
        
        public void store(RubySymbol symbol) {
            table.put(symbol.asSymbol(), symbol);
        }
        
    }
    
}
