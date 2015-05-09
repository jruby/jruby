/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Derek Berner <derek.berner@state.nm.us>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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
package org.jruby;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.compiler.Constantizable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ContextAwareBlockBody;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;
import org.jruby.util.PerlHash;
import org.jruby.util.SipHashInline;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.jruby.util.StringSupport.codeLength;
import static org.jruby.util.StringSupport.codePoint;

/**
 * Represents a Ruby symbol (e.g. :bar)
 */
@JRubyClass(name="Symbol")
public class RubySymbol extends RubyObject implements MarshalEncoding, Constantizable {
    public static final long symbolHashSeedK0 = 5238926673095087190l;

    private final String symbol;
    private final int id;
    private final ByteList symbolBytes;
    private final int hashCode;
    private Object constant;
    
    /**
     * 
     * @param runtime
     * @param internedSymbol the String value of the new Symbol. This <em>must</em>
     *                       have been previously interned
     */
    private RubySymbol(Ruby runtime, String internedSymbol, ByteList symbolBytes) {
        super(runtime, runtime.getSymbol(), false);
        // symbol string *must* be interned

        //        assert internedSymbol == internedSymbol.intern() : internedSymbol + " is not interned";

        this.symbol = internedSymbol;
        this.symbolBytes = symbolBytes;
        this.id = runtime.allocSymbolId();

        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(
                symbolHashSeedK0, 0, symbolBytes.getUnsafeBytes(),
                symbolBytes.getBegin(), symbolBytes.getRealSize()) :
                PerlHash.hash(symbolHashSeedK0, symbolBytes.getUnsafeBytes(),
                symbolBytes.getBegin(), symbolBytes.getRealSize());
        this.hashCode = (int) hash;
        setFrozen(true);
    }

    private RubySymbol(Ruby runtime, String internedSymbol) {
        this(runtime, internedSymbol, symbolBytesFromString(runtime, internedSymbol));
    }

    public static RubyClass createSymbolClass(Ruby runtime) {
        RubyClass symbolClass = runtime.defineClass("Symbol", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setSymbol(symbolClass);
        RubyClass symbolMetaClass = symbolClass.getMetaClass();
        symbolClass.setClassIndex(ClassIndex.SYMBOL);
        symbolClass.setReifiedClass(RubySymbol.class);
        symbolClass.kindOf = new RubyModule.JavaClassKindOf(RubySymbol.class);

        symbolClass.defineAnnotatedMethods(RubySymbol.class);
        symbolMetaClass.undefineMethod("new");

        symbolClass.includeModule(runtime.getComparable());
        
        return symbolClass;
    }
    
    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.SYMBOL;
    }

    /** rb_to_id
     * 
     * @return a String representation of the symbol 
     */
    @Override
    public String asJavaString() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }

    final ByteList getBytes() {
        return symbolBytes;
    }

    /**
     * RubySymbol is created by passing in a String and bytes are extracted from that.  We will
     * pass in encoding of that string after construction but before use so it does not forget
     * what it is.
     */
    public void associateEncoding(Encoding encoding) {
        symbolBytes.setEncoding(encoding);
    }

    /** short circuit for Symbol key comparison
     * 
     */
    @Override
    public final boolean eql(IRubyObject other) {
        return other == this;
    }

    @Override
    public boolean isImmediate() {
    	return true;
    }

    @Override
    public RubyClass getSingletonClass() {
        throw getRuntime().newTypeError("can't define singleton");
    }

    public static RubySymbol getSymbolLong(Ruby runtime, long id) {
        return runtime.getSymbolTable().lookup(id);
    }
    
    /* Symbol class methods.
     * 
     */
    
    public static RubySymbol newSymbol(Ruby runtime, IRubyObject name) {
        if (!(name instanceof RubyString)) return newSymbol(runtime, name.asJavaString());
        
        return runtime.getSymbolTable().getSymbol(((RubyString) name).getByteList());
    }

    public static RubySymbol newSymbol(Ruby runtime, String name) {
        return runtime.getSymbolTable().getSymbol(name);
    }

    // FIXME: same bytesequences will fight over encoding of the symbol once cached.  I think largely
    // this will only happen in some ISO_8859_?? encodings making symbols at the same time so it should
    // be pretty rare.
    public static RubySymbol newSymbol(Ruby runtime, String name, Encoding encoding) {
        RubySymbol newSymbol = newSymbol(runtime, name);

        newSymbol.associateEncoding(encoding);

        return newSymbol;
    }

    /**
     * @see org.jruby.compiler.Constantizable
     */
    @Override
    public Object constant() {
        return constant == null ?
                constant = OptoFactory.newConstantWrapper(IRubyObject.class, this) :
                constant;
    }

    @Deprecated
    @Override
    public IRubyObject inspect() {
        return inspect19(getRuntime().getCurrentContext());
    }

    public IRubyObject inspect(ThreadContext context) {
        return inspect19(context.runtime);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect19(ThreadContext context) {
        return inspect19(context.runtime);
    }

    private final IRubyObject inspect19(Ruby runtime) {
        ByteList result = new ByteList(symbolBytes.getRealSize() + 1);
        result.setEncoding(symbolBytes.getEncoding());
        result.append((byte)':');
        result.append(symbolBytes);

        RubyString str = RubyString.newString(runtime, result); 
        // TODO: 1.9 rb_enc_symname_p
        Encoding resenc = runtime.getDefaultInternalEncoding();
        if (resenc == null) {
            resenc = runtime.getDefaultExternalEncoding();
        }

        if (isPrintable() && (resenc.equals(symbolBytes.getEncoding()) || str.isAsciiOnly()) && isSymbolName19(symbol)) {
            return str;
        }
    
        str = (RubyString)str.inspect19();
        ByteList bytes = str.getByteList();
        bytes.set(0, ':');
        bytes.set(1, '"');
        
        return str;
    }

    @Override
    public IRubyObject to_s() {
        return to_s(getRuntime());
    }
    
    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        return to_s(context.runtime);
    }
    
    private final IRubyObject to_s(Ruby runtime) {
        return RubyString.newStringShared(runtime, symbolBytes);
    }

    public IRubyObject id2name() {
        return to_s(getRuntime());
    }
    
    @JRubyMethod
    public IRubyObject id2name(ThreadContext context) {
        return to_s(context);
    }

    @JRubyMethod(name = "===", required = 1)
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        return context.runtime.newBoolean(this == other);
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        return context.runtime.newBoolean(this == other);
    }

    @Deprecated
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) {
        return context.runtime.newFixnum(hashCode());
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

    public int getId() {
        return id;
    }
    
    @Override
    public boolean equals(Object other) {
        return other == this;
    }
    
    @JRubyMethod(name = "to_sym")
    public IRubyObject to_sym() {
        return this;
    }

    @JRubyMethod(name = "intern")
    public IRubyObject to_sym19() {
        return this;
    }

    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }

    private RubyString newShared(Ruby runtime) {
        return RubyString.newStringShared(runtime, symbolBytes);
    }

    private RubyString rubyStringFromString(Ruby runtime) {
        return RubyString.newString(runtime, symbol);
    }

    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).succ19(context).toString());
    }

    @JRubyMethod(name = "<=>")
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        
        return !(other instanceof RubySymbol) ? runtime.getNil() :
                newShared(runtime).op_cmp(context, ((RubySymbol)other).newShared(runtime));
    }

    @JRubyMethod
    public IRubyObject casecmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        
        return !(other instanceof RubySymbol) ? runtime.getNil() :
                newShared(runtime).casecmp19(context, ((RubySymbol) other).newShared(runtime));
    }

    @JRubyMethod(name = {"=~", "match"})
    @Override
    public IRubyObject op_match19(ThreadContext context, IRubyObject other) {
        return newShared(context.runtime).op_match19(context, other);
    }

    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        return newShared(context.runtime).op_aref19(context, arg);
    }

    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return newShared(context.runtime).op_aref19(context, arg1, arg2);
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length() {
        return newShared(getRuntime()).length19();
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return newShared(context.runtime).empty_p(context);
    }

    @JRubyMethod
    public IRubyObject upcase(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        return newSymbol(runtime, rubyStringFromString(runtime).upcase19(context).toString());
    }

    @JRubyMethod
    public IRubyObject downcase(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        return newSymbol(runtime, rubyStringFromString(runtime).downcase19(context).toString());
    }

    @JRubyMethod
    public IRubyObject capitalize(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        return newSymbol(runtime, rubyStringFromString(runtime).capitalize19(context).toString());
    }

    @JRubyMethod
    public IRubyObject swapcase(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        return newSymbol(runtime, rubyStringFromString(runtime).swapcase19(context).toString());
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return context.runtime.getEncodingService().getEncoding(symbolBytes.getEncoding());
    }
    
    @JRubyMethod
    public IRubyObject to_proc(ThreadContext context) {
        StaticScope scope = context.runtime.getStaticScopeFactory().getDummyScope();
        final CallSite site = new FunctionalCachingCallSite(symbol);
        BlockBody body = new ContextAwareBlockBody(scope, Signature.OPTIONAL) {
            private IRubyObject yieldInner(ThreadContext context, RubyArray array, Block block) {
                if (array.isEmpty()) {
                    throw context.runtime.newArgumentError("no receiver given");
                }

                IRubyObject self = array.shift(context);

                return site.call(context, self, self, array.toJavaArray(), block);
            }

            @Override
            public IRubyObject yield(ThreadContext context, IRubyObject[] args, IRubyObject self,
                                     Binding binding, Type type, Block block) {
                RubyProc.prepareArgs(context, type, block.getBody(), args);
                return yieldInner(context, context.runtime.newArrayNoCopyLight(args), block);
            }

            @Override
            public IRubyObject yield(ThreadContext context, IRubyObject value,
                    Binding binding, Block.Type type, Block block) {
                return yieldInner(context, ArgsUtil.convertToRubyArray(context.runtime, value, false), block);
            }
            
            @Override
            protected IRubyObject doYield(ThreadContext context, IRubyObject value, Binding binding, Type type) {
                return yieldInner(context, ArgsUtil.convertToRubyArray(context.runtime, value, false), Block.NULL_BLOCK);
            }

            @Override
            protected IRubyObject doYield(ThreadContext context, IRubyObject[] args, IRubyObject self, Binding binding, Type type) {
                return yieldInner(context, context.runtime.newArrayNoCopyLight(args), Block.NULL_BLOCK);
            }

            @Override
            public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
                return site.call(context, arg0, arg0);
            }

            @Override
            public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
                return site.call(context, arg0, arg0, arg1);
            }

            @Override
            public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
                return site.call(context, arg0, arg0, arg1, arg2);
            }

            @Override
            public String getFile() {
                return symbol;
            }

            @Override
            public int getLine() {
                return -1;
            }
        };

        return RubyProc.newProc(context.runtime,
                                new Block(body, context.currentBinding()),
                                Block.Type.PROC);
    }
    
    private static boolean isIdentStart(char c) {
        return ((c >= 'a' && c <= 'z')|| (c >= 'A' && c <= 'Z') || c == '_' || !(c < 128));
    }
    
    private static boolean isIdentChar(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '_' || !(c < 128));
    }
    
    private static boolean isIdentifier(String s) {
        if (s == null || s.length() <= 0 || !isIdentStart(s.charAt(0))) return false;

        for (int i = 1; i < s.length(); i++) {
            if (!isIdentChar(s.charAt(i))) return false;
        }
        
        return true;
    }
    
    /**
     * is_special_global_name from parse.c.  
     * @param s
     * @return
     */
    private static boolean isSpecialGlobalName(String s) {
        if (s == null || s.length() <= 0) return false;

        int length = s.length();
           
        switch (s.charAt(0)) {        
        case '~': case '*': case '$': case '?': case '!': case '@': case '/': case '\\':        
        case ';': case ',': case '.': case '=': case ':': case '<': case '>': case '\"':        
        case '&': case '`': case '\'': case '+': case '0':
            return length == 1;            
        case '-':
            return (length == 1 || (length == 2 && isIdentChar(s.charAt(1))));
            
        default:
            for (int i = 0; i < length; i++) {
                if (!Character.isDigit(s.charAt(i))) return false;
            }
        }
        
        return true;
    }

    private boolean isPrintable() {
        Ruby runtime = getRuntime();
        int p = symbolBytes.getBegin();
        int end = p + symbolBytes.getRealSize();
        byte[]bytes = symbolBytes.getUnsafeBytes();
        Encoding enc = symbolBytes.getEncoding();

        while (p < end) {
            int c = codePoint(runtime, enc, bytes, p, end);
            
            if (!enc.isPrint(c)) return false;
            
            p += codeLength(enc, c);
        }
        
        return true;
    }

    private static boolean isSymbolName19(String s) {
        if (s == null || s.length() < 1) return false;

        int length = s.length();
        char c = s.charAt(0);
        
        return isSymbolNameCommon(s, c, length) || 
                (c == '!' && (length == 1 ||
                             (length == 2 && (s.charAt(1) == '~' || s.charAt(1) == '=')))) ||
                isSymbolLocal(s, c, length);
    }

    private static boolean isSymbolNameCommon(String s, char c, int length) {        
        switch (c) {
        case '$':
            if (length > 1 && isSpecialGlobalName(s.substring(1))) return true;

            return isIdentifier(s.substring(1));
        case '@':
            int offset = 1;
            if (length >= 2 && s.charAt(1) == '@') offset++;

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
        return false;
    }

    private static boolean isSymbolLocal(String s, char c, int length) {
        if (!isIdentStart(c)) return false;

        boolean localID = (c >= 'a' && c <= 'z');
        int last = 1;

        for (; last < length; last++) {
            char d = s.charAt(last);

            if (!isIdentChar(d)) break;
        }

        if (last == length) return true;
        if (localID && last == length - 1) {
            char d = s.charAt(last);

            return d == '!' || d == '?' || d == '=';
        }

        return false;
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject all_symbols(ThreadContext context, IRubyObject recv) {
        return context.runtime.getSymbolTable().all_symbols();
    }
    @Deprecated
    public static IRubyObject all_symbols(IRubyObject recv) {
        return recv.getRuntime().getSymbolTable().all_symbols();
    }

    public static RubySymbol unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubySymbol result = newSymbol(input.getRuntime(), RubyString.byteListToString(input.unmarshalString()));
        
        input.registerLinkTarget(result);
        
        return result;
    }

    @Override
    public Object toJava(Class target) {
        if (target == String.class || target == CharSequence.class) return symbol;

        return super.toJava(target);
    }

    public static ByteList symbolBytesFromString(Ruby runtime, String internedSymbol) {
        return new ByteList(ByteList.plain(internedSymbol), USASCIIEncoding.INSTANCE, false);
    }

    public static final class SymbolTable {
        static final int DEFAULT_INITIAL_CAPACITY = 1 << 10; // *must* be power of 2!
        static final int MAXIMUM_CAPACITY = 1 << 16; // enough for a 64k buckets; if you need more than this, something's wrong
        static final float DEFAULT_LOAD_FACTOR = 0.75f;
        
        private final ReentrantLock tableLock = new ReentrantLock();
        private volatile SymbolEntry[] symbolTable;
        private int size;
        private int threshold;
        private final float loadFactor;
        private final Ruby runtime;
        
        public SymbolTable(Ruby runtime) {
            this.runtime = runtime;
            this.loadFactor = DEFAULT_LOAD_FACTOR;
            this.threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
            this.symbolTable = new SymbolEntry[DEFAULT_INITIAL_CAPACITY];
        }
        
        // note all fields are final -- rehash creates new entries when necessary.
        // as documented in java.util.concurrent.ConcurrentHashMap.java, that will
        // statistically affect only a small percentage (< 20%) of entries for a given rehash.
        static class SymbolEntry {
            final int hash;
            final String name;
            final ByteList bytes;
            final WeakReference<RubySymbol> symbol;
            SymbolEntry next;
            
            SymbolEntry(int hash, String name, ByteList bytes, WeakReference<RubySymbol> symbol, SymbolEntry next) {
                this.hash = hash;
                this.name = name;
                this.bytes = bytes;
                this.symbol = symbol;
                this.next = next;
            }
        }

        public RubySymbol getSymbol(String name) {
            int hash = javaStringHashCode(name);
            RubySymbol symbol = null;
            
            for (SymbolEntry e = getEntryFromTable(symbolTable, hash); e != null; e = e.next) {
                if (isSymbolMatch(name, hash, e)) {
                    symbol = e.symbol.get();
                    break;
                }
            }
            
            if (symbol == null) symbol = createSymbol(name, symbolBytesFromString(runtime, name), hash);

            return symbol;
        }

        public RubySymbol getSymbol(ByteList bytes) {
            RubySymbol symbol = null;
            int hash = javaStringHashCode(bytes);

            for (SymbolEntry e = getEntryFromTable(symbolTable, hash); e != null; e = e.next) {
                if (isSymbolMatch(bytes, hash, e)) {
                    symbol = e.symbol.get();
                    break;
                }
            }

            if (symbol == null) {
                bytes = bytes.dup();
                symbol = createSymbol(bytes.toString(), bytes, hash);
            }

            return symbol;
        }

        public RubySymbol fastGetSymbol(String internedName) {
            RubySymbol symbol = null;

            for (SymbolEntry e = getEntryFromTable(symbolTable, internedName.hashCode()); e != null; e = e.next) {
                if (isSymbolMatch(internedName, e)) {
                    symbol = e.symbol.get();
                    break;
                }
            }
            
            if (symbol == null) {
                symbol = fastCreateSymbol(internedName);
            }

            return symbol;
        }

        private static SymbolEntry getEntryFromTable(SymbolEntry[] table, int hash) {
            return table[hash & (table.length - 1)];
        }

        private static boolean isSymbolMatch(String name, int hash, SymbolEntry entry) {
            return hash == entry.hash && name.equals(entry.name);
        }

        private static boolean isSymbolMatch(ByteList bytes, int hash, SymbolEntry entry) {
            return hash == entry.hash && bytes.equals(entry.bytes);
        }

        private static boolean isSymbolMatch(String internedName, SymbolEntry entry) {
            return internedName == entry.name;
        }

        private RubySymbol createSymbol(final String name, final ByteList value, final int hash) {
            ReentrantLock lock;
            (lock = tableLock).lock();
            try {
                final SymbolEntry[] table = size > threshold ? rehash() : symbolTable;
                final int index = hash & (table.length - 1);
                RubySymbol symbol = null;

                // try lookup again under lock
                for (SymbolEntry last = null, curr = table[index]; curr != null; curr = curr.next) {
                    RubySymbol localSymbol = curr.symbol.get();

                    if (localSymbol == null) {
                        removeDeadEntry(table, index, last, curr);

                        // if it's not our entry, proceed to next
                        if (hash != curr.hash || !name.equals(curr.name)) continue;
                    }

                    // update last entry that was either not dead or not the one we want
                    last = curr;

                    // if have a matching entry -- even if symbol has gone away -- exit the loop
                    if (hash == curr.hash && name.equals(curr.name)) {
                        symbol = localSymbol;
                        break;
                    }
                }

                if (symbol == null) {
                    String internedName = name.intern();
                    symbol = new RubySymbol(runtime, internedName, value);
                    table[index] = new SymbolEntry(hash, internedName, value, new WeakReference(symbol), table[index]);
                    size++;
                    // write-volatile
                    symbolTable = table;
                }
                return symbol;
            } finally {
                lock.unlock();
            }
        }

        private void removeDeadEntry(SymbolEntry[] table, int index, SymbolEntry last, SymbolEntry e) {
            if (last == null) {
                table[index] = e.next; // shift head of bucket
            } else {
                last.next = e.next; // remove collected bucket entry
            }

            size--; // reduce current size because we lost one somewhere
        }

        private RubySymbol fastCreateSymbol(final String internedName) {
            ReentrantLock lock;
            (lock = tableLock).lock();
            try {
                final SymbolEntry[] table = size + 1 > threshold ? rehash() : symbolTable;
                final int hash = internedName.hashCode();
                final int index = hash & (table.length - 1);
                RubySymbol symbol = null;

                // try lookup again under lock
                for (SymbolEntry last = null, curr = table[index]; curr != null; curr = curr.next) {
                    RubySymbol localSymbol = curr.symbol.get();

                    if (localSymbol == null) {
                        removeDeadEntry(table, index, last, curr);

                        // if it's not our entry, proceed to next
                        if (internedName != curr.name) continue;
                    }

                    // update last entry that was either not dead or not the one we want
                    last = curr;

                    // if have a matching entry -- even if symbol has gone away -- exit the loop
                    if (internedName == curr.name) {
                        symbol = localSymbol;
                        break;
                    }
                }

                if (symbol == null) {
                    symbol = new RubySymbol(runtime, internedName);
                    table[index] = new SymbolEntry(hash, internedName, symbol.getBytes(), new WeakReference(symbol), table[index]);
                    size++;
                    // write-volatile
                    symbolTable = table;
                }
                return symbol;
            } finally {
                lock.unlock();
            }
        }
        
        public RubySymbol lookup(long id) {
            SymbolEntry[] table = symbolTable;
            RubySymbol symbol = null;
            
            for (int i = table.length; --i >= 0; ) {
                for (SymbolEntry e = table[i]; e != null; e = e.next) {
                    symbol = e.symbol.get();
                    if (symbol != null && id == symbol.id) return symbol;
                }
            }

            return null;
        }
        
        public RubyArray all_symbols() {
            SymbolEntry[] table = this.symbolTable;
            RubyArray array = runtime.newArray(this.size);
            RubySymbol symbol;

            for (int i = table.length; --i >= 0; ) {
                for (SymbolEntry e = table[i]; e != null; e = e.next) {
                    symbol = e.symbol.get();
                    if (symbol != null) array.append(symbol);
                }
            }
            return array;
        }
        
        private SymbolEntry[] rehash() {
            SymbolEntry[] oldTable = symbolTable;
            int oldCapacity = oldTable.length;
            
            if (oldCapacity >= MAXIMUM_CAPACITY) return oldTable;
            
            int newCapacity = oldCapacity << 1;
            SymbolEntry[] newTable = new SymbolEntry[newCapacity];
            threshold = (int)(newCapacity * loadFactor);
            int sizeMask = newCapacity - 1;
            SymbolEntry e;
            for (int i = oldCapacity; --i >= 0; ) {
                // We need to guarantee that any existing reads of old Map can
                //  proceed. So we cannot yet null out each bin.
                e = oldTable[i];
                if (e == null) continue;

                SymbolEntry next = e.next;
                int idx = e.hash & sizeMask;

                //  Single node on list, reuse it
                if (next == null) {
                    newTable[idx] = e;
                } else {
                    // Reuse trailing consecutive sequence at same slot
                    SymbolEntry lastRun = e;
                    int lastIdx = idx;
                    for (SymbolEntry last = next;
                         last != null;
                         last = last.next) {
                        int k = last.hash & sizeMask;
                        if (k != lastIdx) {
                            lastIdx = k;
                            lastRun = last;
                        }
                    }
                    newTable[lastIdx] = lastRun;

                    // Clone all remaining nodes
                    for (SymbolEntry p = e; p != lastRun; p = p.next) {
                        int k = p.hash & sizeMask;
                        SymbolEntry n = newTable[k];
                        newTable[k] = new SymbolEntry(p.hash, p.name, p.bytes, p.symbol, n);
                    }
                }
            }
            symbolTable = newTable;
            return newTable;
        }

        // backwards-compatibility, but threadsafe now
        @Deprecated
        public RubySymbol lookup(String name) {
            int hash = name.hashCode();
            SymbolEntry[] table = symbolTable;
            RubySymbol symbol = null;

            SymbolEntry e = table[hash & (table.length - 1)];
            while (e != null) {
                if (hash == e.hash && name.equals(e.name)) {
                    symbol = e.symbol.get();
                    if (symbol != null) break;
                }
                e = e.next;
            }

            return symbol;
        }

        // not so backwards-compatible here, but no one should have been
        // calling this anyway.
        @Deprecated
        public void store(RubySymbol symbol) {
            throw new UnsupportedOperationException();
        }
    }

    private static int javaStringHashCode(String str) {
        return str.hashCode();
    }

    private static int javaStringHashCode(ByteList iso8859) {
        int h = 0;
        int length = iso8859.length();
        if (h == 0 && length > 0) {
            byte val[] = iso8859.getUnsafeBytes();
            int begin = iso8859.begin();

            for (int i = 0; i < length; i++) {
                h = 31 * h + val[begin + i];
            }
        }
        return h;
    }

    @Override
    public boolean shouldMarshalEncoding() {
        return getMarshalEncoding() != USASCIIEncoding.INSTANCE;
    }

    @Override
    public Encoding getMarshalEncoding() {
        return symbolBytes.getEncoding();
    }
    
    /**
     * Properly stringify an object for the current "raw bytes" representation
     * of a symbol.
     * 
     * Symbols are represented internally as a Java string, but decoded using
     * raw bytes in ISO-8859-1 representation. This means they do not in their
     * normal String form represent a readable Java string, but it does allow
     * differently-encoded strings to map to different symbol objects.
     * 
     * See #736
     * 
     * @param object the object to symbolify
     * @return the symbol string associated with the object's string representation
     */
    public static String objectToSymbolString(IRubyObject object) {
        if (object instanceof RubySymbol) {
            return ((RubySymbol)object).toString();
        } else if (object instanceof RubyString) {
            return ((RubyString)object).getByteList().toString();
        } else {
            return object.convertToString().getByteList().toString();
        }
    }
}
