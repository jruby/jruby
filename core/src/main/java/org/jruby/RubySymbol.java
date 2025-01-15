/*
 ***** BEGIN LICENSE BLOCK *****
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
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.compiler.Constantizable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ContextAwareBlockBody;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.RefinedCachingCallSite;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;
import org.jruby.util.IdUtil;
import org.jruby.util.PerlHash;
import org.jruby.util.SipHashInline;
import org.jruby.util.SymbolNameType;
import org.jruby.util.TypeConverter;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.StringSupport.CR_7BIT;
import static org.jruby.util.StringSupport.codeLength;
import static org.jruby.util.StringSupport.codePoint;
import static org.jruby.util.StringSupport.codeRangeScan;

/**
 * Represents a Ruby symbol (e.g. :bar)
 */
@JRubyClass(name = "Symbol", include = "Enumerable")
public class RubySymbol extends RubyObject implements MarshalEncoding, EncodingCapable, Constantizable {
    @Deprecated
    public static final long symbolHashSeedK0 = 5238926673095087190l;

    private final String symbol;
    private final int id;
    private final ByteList symbolBytes;
    private final int hashCode;
    private String decodedString;
    private RubyString rubyString;
    private transient Object constant;
    private SymbolNameType type;

    /**
     *
     * @param runtime
     * @param internedSymbol the String value of the new Symbol. This <em>must</em>
     *                       have been previously interned
     * @param symbolBytes the ByteList of the symbol's string representation
     */
    private RubySymbol(Ruby runtime, String internedSymbol, ByteList symbolBytes) {
        super(runtime, runtime.getSymbol(), false);

        assert internedSymbol == internedSymbol.intern() : internedSymbol + " is not interned";

        this.symbol = internedSymbol;
        if (symbolBytes.getEncoding() != USASCIIEncoding.INSTANCE &&
                codeRangeScan(symbolBytes.getEncoding(), symbolBytes) == CR_7BIT) {
            symbolBytes = symbolBytes.dup();
            symbolBytes.setEncoding(USASCIIEncoding.INSTANCE);
        }
        this.symbolBytes = symbolBytes;
        this.id = runtime.allocSymbolId();

        long k0 = Helpers.hashStart(runtime, id);
        long hash = runtime.isSiphashEnabled() ? SipHashInline.hash24(
                k0, 0, symbolBytes.getUnsafeBytes(),
                symbolBytes.getBegin(), symbolBytes.getRealSize()) :
                PerlHash.hash(k0, symbolBytes.getUnsafeBytes(),
                symbolBytes.getBegin(), symbolBytes.getRealSize());
        this.hashCode = (int) hash;
        this.type = IdUtil.determineSymbolNameType(runtime, symbolBytes);
        setFrozen(true);
    }

    private RubySymbol(Ruby runtime, String internedSymbol) {
        this(runtime, internedSymbol, symbolBytesFromString(runtime, internedSymbol));
    }

    public static RubyClass createSymbolClass(Ruby runtime) {
        RubyClass symbolClass = runtime.defineClass("Symbol", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

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
    public final String asJavaString() {
        return symbol;
    }

    /**
     * Return an id string (e.g. raw ISO-8859_1 charset String) for use with our method tables etc.
     */
    public String idString() {
        return symbol;
    }

    /**
     * Print a string for internal debugging purposes.  This does a half-hearted attempt at representing the string in
     * a displayable fashion for for error messages you should use RubyStringBuilder.str + ids + types to build up the
     * error message.  For identifier strings you should use idString().  For non-identifier strings where you want a
     * raw String you should use asJavaString().
     *
     * @return a String
     */
    @Override
    public final String toString() {
        String decoded = decodedString;
        if (decoded == null) {
            decodedString = decoded = RubyEncoding.decodeRaw(getBytes());
        }
        return decoded;
    }

    public final ByteList getBytes() {
        return symbolBytes;
    }

    /**
     * Make an instance variable out of this symbol (e.g. :foo will generate :foo=).
     * @return the new symbol
     */
    public RubySymbol asWriter() {
        ByteList bytes = getBytes();
        ByteList dup = bytes.dup(bytes.length() + 1);

        dup.append((byte) '=');

        return newIDSymbol(metaClass.runtime, dup);
    }

    public RubySymbol asInstanceVariable() {
        ByteList bytes = getBytes();

        ByteList dup = new ByteList(bytes.length() + 1);
        dup.setEncoding(ByteList.safeEncoding(bytes.getEncoding()));
        dup.append((byte) '@');
        dup.append(bytes);

        return newIDSymbol(metaClass.runtime, dup);
    }

    /**
     * When we know we need an entry in the symbol table because the provided name will be needed to be
     * accessed as a valid identifier later we can call this.  If there is not already an entry we will
     * return a new symbol.  Otherwise, the existing entry.
     *
     * @param name to get symbol table entry for (it may be a symbol already)
     * @return the symbol table entry.
     */
    public static RubySymbol retrieveIDSymbol(IRubyObject name) {
        return name instanceof RubySymbol ?
                (RubySymbol) name : newIDSymbol(name.getRuntime(), name.convertToString().getByteList());
    }

    /**
     * Retrieve an ID symbol but call the handler before any new symbol is added to the symbol table.
     * This can be used for verifying the symbol is valid.
     *
     * @param name to get symbol table entry for (it may be a symbol already)
     * @return the symbol table entry.
     */
    public static RubySymbol retrieveIDSymbol(IRubyObject name, ObjBooleanConsumer<RubySymbol> handler) {
        if (name instanceof RubySymbol) {
            RubySymbol sym = (RubySymbol) name;
            handler.accept(sym, false);
            return sym;
        }

        return newIDSymbol(name.getRuntime(), name.convertToString().getByteList(), handler);
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

    /**
     * Is the string this constant represents a valid constant identifier name.
     */
    public boolean validConstantName() {
        return type == SymbolNameType.CONST;
    }

    /**
     * Is the string this constant represents a valid constant identifier name.
     */
    public boolean validInstanceVariableName() {
        return type == SymbolNameType.INSTANCE;
    }

    /**
     * Is the string this constant represents a valid constant identifier name.
     */
    public boolean validClassVariableName() {
        return type == SymbolNameType.CLASS;
    }


    public boolean validLocalVariableName() {
        return type == SymbolNameType.LOCAL && !"nil".equals(idString());
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
    @Deprecated
    public static RubySymbol newSymbol(Ruby runtime, IRubyObject name) {
        if (name instanceof RubySymbol) {
            return runtime.getSymbolTable().getSymbol(((RubySymbol) name).getBytes(), false);
        } else if (name instanceof RubyString) {
            return runtime.getSymbolTable().getSymbol(((RubyString) name).getByteList(), false);
        } else {
            return newSymbol(runtime, name.asString().getByteList());
        }
    }

    public static RubySymbol newHardSymbol(Ruby runtime, IRubyObject name) {
        if (name instanceof RubySymbol) {
            return runtime.getSymbolTable().getSymbol(((RubySymbol) name).getBytes(), true);
        } else if (name instanceof RubyString) {
            return runtime.getSymbolTable().getSymbol(((RubyString) name).getByteList(), true);
        }

        return newSymbol(runtime, name.asString().getByteList());
    }

    public static RubySymbol newSymbol(Ruby runtime, String name) {
        return runtime.getSymbolTable().getSymbol(name, false);
    }

    public static RubySymbol newSymbol(Ruby runtime, ByteList bytes) {
        return runtime.getSymbolTable().getSymbol(bytes, false);
    }

    public static RubySymbol newHardSymbol(Ruby runtime, ByteList bytes) {
        return runtime.getSymbolTable().getSymbol(bytes, true);
    }

    public static RubySymbol newHardSymbol(Ruby runtime, ByteList bytes, ObjBooleanConsumer<RubySymbol> handler) {
        return runtime.getSymbolTable().getSymbol(bytes, handler, true);
    }

    public static RubySymbol newHardSymbol(Ruby runtime, String name) {
        return runtime.getSymbolTable().getSymbol(name, true);
    }

    /**
     * Return the symbol in the symbol table if it exists, null otherwise.
     * This method will not create the symbol if it does not exist.
     * @param runtime
     * @param bytes
     * @return
     */
    public static RubySymbol newSymbol(Ruby runtime, ByteList bytes, ObjBooleanConsumer<RubySymbol> handler) {
        return runtime.getSymbolTable().getSymbol(bytes, handler, false);
    }

    @FunctionalInterface
    public interface ObjBooleanConsumer<T> {
        void accept(T t, boolean b);
    }

    /**
     * Generic identifier symbol creation (or retrieval) method.
     *
     * @param runtime of this Ruby instance.
     * @param bytes to be made into a symbol (or to help retreive existing symbol)
     * @return a new or existing symbol
     */
    public static RubySymbol newIDSymbol(Ruby runtime, ByteList bytes) {
        return newHardSymbol(runtime, bytes);
    }

    /**
     * Generic identifier symbol creation (or retrieval) method that invokes a handler before storing new symbols.
     *
     * @param runtime of this Ruby instance.
     * @param bytes to be made into a symbol (or to help retreive existing symbol)
     * @return a new or existing symbol
     */
    public static RubySymbol newIDSymbol(Ruby runtime, ByteList bytes, ObjBooleanConsumer<RubySymbol> handler) {
        return newHardSymbol(runtime, bytes, handler);
    }

    /**
     * Create a symbol whose intention is to be used as a constant.  This will not
     * only guarantee a symbol entry in the table but it will also verify the symbol
     * conforms as a valid constant identifier.
     *
     * @param runtime of this Ruby instance.
     * @param fqn if this constant symbol is part of a broader chain this is used for full name error reporting.
     * @param bytes to be made into a symbol (or to help retreive existing symbol)
     * @return a new or existing symbol
     */
    public static RubySymbol newConstantSymbol(Ruby runtime, IRubyObject fqn, ByteList bytes) {
        if (bytes.length() == 0) {
            throw runtime.newNameError(str(runtime, "wrong constant name ", ids(runtime, fqn)), runtime.newSymbol(""));
        }

        RubySymbol symbol = runtime.newSymbol(bytes);

        if (!symbol.validConstantName()) {
            throw runtime.newNameError(str(runtime, "wrong constant name ", ids(runtime, fqn)), symbol);
        }

        return symbol;
    }

    public static RubySymbol newSymbol(Ruby runtime, String name, Encoding encoding) {
        RubySymbol newSymbol = runtime.getSymbolTable().getSymbol(RubyString.encodeBytelist(name, encoding));

        return newSymbol;
    }

    public static RubySymbol newHardSymbol(Ruby runtime, String name, Encoding encoding) {
        RubySymbol newSymbol = runtime.getSymbolTable().getSymbol(RubyString.encodeBytelist(name, encoding));

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

    @Override
    public IRubyObject inspect() {
        return inspect(metaClass.runtime);
    }

    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return inspect(context.runtime);
    }

    final RubyString inspect(final Ruby runtime) {
        // TODO: 1.9 rb_enc_symname_p
        Encoding resenc = runtime.getDefaultInternalEncoding();
        if (resenc == null) resenc = runtime.getDefaultExternalEncoding();

        RubyString str = RubyString.newString(runtime, getBytes());

        if (!(isPrintable(runtime) && (resenc.equals(getBytes().getEncoding()) || str.isAsciiOnly()) &&
                isSymbolName(symbol))) {
            str = str.inspect(runtime);
        }

        ByteList result = new ByteList(str.getByteList().getRealSize() + 1);
        result.setEncoding(str.getEncoding());
        result.append((byte)':');
        result.append(str.getByteList());

        return RubyString.newString(runtime, result);
    }

    @Deprecated
    public IRubyObject inspect19(ThreadContext context) {
        return inspect(context);
    }

    @Override
    public IRubyObject to_s() {
        return to_s(metaClass.runtime);
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        return to_s(context.runtime);
    }

    final RubyString to_s(Ruby runtime) {
        return RubyString.newStringShared(runtime, getBytes());
    }

    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
      // FIXME: A race here where two in-flight to_s on same symbol can return a different instance.
      if (rubyString == null) {
        rubyString = RubyString.newStringShared(context.runtime, getBytes());
        rubyString.setFrozen(true);
      }

      return rubyString;
    }

    public IRubyObject id2name() {
        return to_s(metaClass.runtime);
    }

    @JRubyMethod
    public IRubyObject id2name(ThreadContext context) {
        return to_s(context);
    }

    @Override
    public RubyString asString() {
        return to_s(metaClass.runtime);
    }

    @JRubyMethod(name = "===")
    @Override
    public IRubyObject op_eqq(ThreadContext context, IRubyObject other) {
        return RubyBoolean.newBoolean(context, this == other);
    }

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        return RubyBoolean.newBoolean(context, this == other);
    }

    @Deprecated
    @Override
    public RubyFixnum hash() {
        return metaClass.runtime.newFixnum(hashCode());
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

    /**
     * @see RubyBasicObject#compareTo(IRubyObject)
     */
    @Override
    public int compareTo(final IRubyObject that) {
        // NOTE: we're expecting RubySymbol to always be Java sortable
        if ( that instanceof RubySymbol ) {
            return this.symbol.compareTo( ((RubySymbol) that).symbol );
        }
        return 0; // our <=> contract is to return 0 on non-comparables
    }

    @JRubyMethod(name = { "to_sym", "intern" })
    public IRubyObject to_sym() { return this; }

    @Deprecated
    public IRubyObject to_sym19() { return this; }

    private RubyString newShared(Ruby runtime) {
        return RubyString.newStringShared(runtime, symbolBytes);
    }

    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).succ(context).asString().getByteList());
    }

    @JRubyMethod(name = "<=>")
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;

        return !(other instanceof RubySymbol) ? context.nil :
                newShared(runtime).op_cmp(context, ((RubySymbol)other).newShared(runtime));
    }

    @JRubyMethod
    public IRubyObject casecmp(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;

        return !(other instanceof RubySymbol) ? context.nil :
                newShared(runtime).casecmp(context, ((RubySymbol) other).newShared(runtime));
    }

    @JRubyMethod(name = "casecmp?")
    public IRubyObject casecmp_p(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;

        return !(other instanceof RubySymbol) ? context.nil :
            newShared(runtime).casecmp_p(context, ((RubySymbol) other).newShared(runtime));
    }

    @JRubyMethod(name = "=~")
    @Override
    public IRubyObject op_match(ThreadContext context, IRubyObject other) {
        return newShared(context.runtime).op_match(context, other);
    }

    @JRubyMethod(name = "match")
    public IRubyObject match_m(ThreadContext context, IRubyObject other, Block block) {
        return newShared(context.runtime).match19(context, other, block);
    }

    @JRubyMethod(name = "match")
    public IRubyObject match_m(ThreadContext context, IRubyObject other, IRubyObject pos, Block block) {
        return newShared(context.runtime).match19(context, other, pos, block);
    }

    @JRubyMethod(name = "match", required = 1, rest = true, checkArity = false)
    public IRubyObject match_m(ThreadContext context, IRubyObject[] args, Block block) {
        return newShared(context.runtime).match19(context, args, block);
    }

    @JRubyMethod(name = "match?")
    public IRubyObject match_p(ThreadContext context, IRubyObject other) {
        return newShared(context.runtime).match_p(context, other);
    }

    @JRubyMethod(name = "match?")
    public IRubyObject match_p(ThreadContext context, IRubyObject other, IRubyObject pos) {
        return newShared(context.runtime).match_p(context, other, pos);
    }

    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
        return newShared(context.runtime).op_aref(context, arg);
    }

    @JRubyMethod(name = {"[]", "slice"})
    public IRubyObject op_aref(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        return newShared(context.runtime).op_aref(context, arg1, arg2);
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length() {
        final Ruby runtime = metaClass.runtime;
        return RubyFixnum.newFixnum(runtime, newShared(runtime).strLength());
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return newShared(context.runtime).empty_p(context);
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context) {
        return context.fals;
    }

    @JRubyMethod(name = "start_with?")
    public IRubyObject start_with_p(ThreadContext context, IRubyObject arg) {
        if (arg instanceof RubyRegexp) {
            return ((RubyRegexp) arg).startsWith(context, to_s(context.runtime)) ? context.tru : context.fals;
        }
        return to_s(context.runtime).startsWith(arg.convertToString()) ? context.tru : context.fals;
    }

    @JRubyMethod(name = "start_with?", rest = true)
    public IRubyObject start_with_p(ThreadContext context, IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            if (start_with_p(context, args[i]).isTrue()) return context.tru;
        }
        return context.fals;
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context) {
        return context.fals;
    }

    @JRubyMethod(name = "end_with?")
    public IRubyObject end_with_p(ThreadContext context, IRubyObject arg) {
        return to_s(context.runtime).endWith(arg) ? context.tru : context.fals;
    }

    @JRubyMethod(name = "end_with?", rest = true)
    public IRubyObject end_with_p(ThreadContext context, IRubyObject[]args) {
        RubyString str = to_s(context.runtime);
        for (int i = 0; i < args.length; i++) {
            if (str.endWith(args[i])) return context.tru;
        }
        return context.fals;
    }

    @JRubyMethod
    public IRubyObject upcase(ThreadContext context) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).upcase(context).getByteList());
    }

    @JRubyMethod
    public IRubyObject upcase(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).upcase(context, arg).getByteList());
    }

    @JRubyMethod
    public IRubyObject upcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).upcase(context, arg0, arg1).getByteList());
    }

    @JRubyMethod
    public IRubyObject downcase(ThreadContext context) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).downcase(context).getByteList());
    }

    @JRubyMethod
    public IRubyObject downcase(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).downcase(context, arg).getByteList());
    }

    @JRubyMethod
    public IRubyObject downcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).downcase(context, arg0, arg1).getByteList());
    }

    @JRubyMethod
    public IRubyObject swapcase(ThreadContext context) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).swapcase(context).getByteList());
    }

    @JRubyMethod
    public IRubyObject swapcase(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).swapcase(context, arg).getByteList());
    }

    @JRubyMethod
    public IRubyObject swapcase(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).swapcase(context, arg0, arg1).getByteList());
    }

    @JRubyMethod
    public IRubyObject capitalize(ThreadContext context) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).capitalize(context).getByteList());
    }

    @JRubyMethod
    public IRubyObject capitalize(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).capitalize(context, arg).getByteList());
    }

    @JRubyMethod
    public IRubyObject capitalize(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.runtime;
        return newSymbol(runtime, newShared(runtime).capitalize(context, arg0, arg1).getByteList());
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return context.runtime.getEncodingService().getEncoding(getEncoding());
    }

    @JRubyMethod
    public IRubyObject to_proc(ThreadContext context) {
        BlockBody body = new SymbolProcBody(context.runtime, symbol);

        return RubyProc.newProc(context.runtime,
                                new Block(body, Block.NULL_BLOCK.getBinding()),
                                Block.Type.LAMBDA);
    }

    public IRubyObject toRefinedProc(ThreadContext context, StaticScope scope) {
        BlockBody body = new SymbolProcBody(context.runtime, symbol, scope);

        return RubyProc.newProc(context.runtime,
                new Block(body, Block.NULL_BLOCK.getBinding()),
                Block.Type.LAMBDA);
    }

    private static boolean isIdentStart(char c) {
        return ((c >= 'a' && c <= 'z')|| (c >= 'A' && c <= 'Z') || c == '_' || !(c < 128));
    }

    private static boolean isIdentChar(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '_' || !(c < 128));
    }

    private static boolean isIdentifier(String s) {
        if (s.isEmpty() || !isIdentStart(s.charAt(0))) return false;

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
        if (s.isEmpty()) return false;

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

    private boolean isPrintable(final Ruby runtime) {
        ByteList symbolBytes = getBytes();
        int p = symbolBytes.getBegin();
        int end = p + symbolBytes.getRealSize();
        byte[] bytes = symbolBytes.getUnsafeBytes();
        Encoding enc = symbolBytes.getEncoding();

        while (p < end) {
            int c = codePoint(runtime, enc, bytes, p, end);

            if (!enc.isPrint(c)) return false;

            p += codeLength(enc, c);
        }

        return true;
    }

    private static boolean isSymbolName(final String str) {
        if (str == null || str.isEmpty()) return false;

        int length = str.length();
        char c = str.charAt(0);

        return isSymbolNameCommon(str, c, length) ||
                (c == '!' && (length == 1 ||
                             (length == 2 && (str.charAt(1) == '~' || str.charAt(1) == '=')))) ||
                isSymbolLocal(str, c, length);
    }

    private static boolean isSymbolNameCommon(final String str, final char first, final int length) {
        switch (first) {
        case '$':
            if (length > 1 && isSpecialGlobalName(str.substring(1))) return true;

            return isIdentifier(str.substring(1));
        case '@':
            int offset = 1;
            if (length >= 2 && str.charAt(1) == '@') offset++;

            return isIdentifier(str.substring(offset));
        case '<':
            return (length == 1 || (length == 2 && (str.equals("<<") || str.equals("<="))) ||
                    (length == 3 && str.equals("<=>")));
        case '>':
            return (length == 1) || (length == 2 && (str.equals(">>") || str.equals(">=")));
        case '=':
            return ((length == 2 && (str.equals("==") || str.equals("=~"))) ||
                    (length == 3 && str.equals("===")));
        case '*':
            return (length == 1 || (length == 2 && str.equals("**")));
        case '+':
            return (length == 1 || (length == 2 && str.equals("+@")));
        case '-':
            return (length == 1 || (length == 2 && str.equals("-@")));
        case '|': case '^': case '&': case '/': case '%': case '~': case '`':
            return length == 1;
        case '[':
            return str.equals("[]") || str.equals("[]=");
        }
        return false;
    }

    private static boolean isSymbolLocal(final String str, final char first, final int length) {
        if (!isIdentStart(first)) return false;

        boolean localID = isIdentStart(first);
        int last = 1;

        for (; last < length; last++) {
            char d = str.charAt(last);

            if (!isIdentChar(d)) break;
        }

        if (last == length) return true;
        if (localID && last == length - 1) {
            char d = str.charAt(last);

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

    public static RubySymbol unmarshalFrom(UnmarshalStream input, UnmarshalStream.MarshalState state) throws java.io.IOException {
        ByteList byteList = input.unmarshalString();
        byteList.setEncoding(ASCIIEncoding.INSTANCE);

        // Extra complicated interaction...
        // We initially set to binary as newSymbol will make it US-ASCII if possible and code below will set explicit
        // encoding otherwise.  Motivation here is binary is capable of being walked during symbol type calculation
        // (and at this point the bytelist can be data of any encoding without us knowing what).  The proper type
        // calculation will happen for non-US-ASCII below when it decodes the encoding.  setEncoding will recalculate
        // symbol type properly.  This is all to work around registerLinkTarget being an ordered list.  So a symbol
        // retrieved while decoding the encoding would make this symbol get registered out of order...

        // Need symbol to register before encoding, so pass in a lambda for remaining unmarshal logic
        RubySymbol result = newSymbol(input.getRuntime(), byteList,
                (sym, newSym) -> {
                    // get encoding from stream and set into symbol
                    if (state != null && state.isIvarWaiting()) {
                        try {
                            input.unmarshalInt(); // throw-away, always single ivar of encoding
                            IRubyObject value = input.unmarshalObject();

                            Encoding enc = input.symbolToEncoding(sym, value);
                            if (enc == null) throw new RuntimeException("BUG: No encoding found in marshal stream");

                            // only change encoding if the symbol has been newly-created
                            if (newSym) sym.setEncoding(enc);

                            state.setIvarWaiting(false);
                        } catch (Throwable t) {
                            Helpers.throwException(t);
                        }
                    }
                });

        return result;
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target == String.class || target == CharSequence.class) {
            return target.cast(symbol);
        }

        return super.toJava(target);
    }

    public static ByteList symbolBytesFromString(Ruby runtime, String internedSymbol) {
        return new ByteList(ByteList.plain(internedSymbol), USASCIIEncoding.INSTANCE, false);
    }

    @Override
    public Encoding getEncoding() {
        return getBytes().getEncoding();
    }

    @Override
    public void setEncoding(Encoding e) {
        getBytes().setEncoding(e);
        type = IdUtil.determineSymbolNameType(getRuntime(), getBytes());
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
            reset();
        }

        // note all fields are final -- rehash creates new entries when necessary.
        // as documented in java.util.concurrent.ConcurrentHashMap.java, that will
        // statistically affect only a small percentage (< 20%) of entries for a given rehash.
        static final class SymbolEntry {
            final int hash;
            final String name;
            final ByteList bytes;
            final WeakReference<RubySymbol> symbol;
            RubySymbol hardReference; // only read in this
            SymbolEntry next;

            SymbolEntry(int hash, String name, ByteList bytes, RubySymbol symbol, SymbolEntry next, boolean hard) {
                this.hash = hash;
                this.name = name;
                this.bytes = bytes;
                this.symbol = new WeakReference<RubySymbol>(symbol);
                this.next = next;
                if (hard) hardReference = symbol;
            }

            /**
             * Force an existing weak symbol to become a hard symbol, so it never goes away.
             */
            public void setHardReference() {
                if (hardReference == null) {
                    hardReference = symbol.get();
                }
            }
        }

        public RubySymbol getSymbol(String name) {
            return getSymbol(name, false);
        }

        public RubySymbol getSymbol(String name, boolean hard) {
            int hash = javaStringHashCode(name);
            RubySymbol symbol = null;

            for (SymbolEntry e = getEntryFromTable(symbolTable, hash); e != null; e = e.next) {
                if (isSymbolMatch(name, hash, e)) {
                    if (hard) e.setHardReference();
                    symbol = e.symbol.get();
                    break;
                }
            }

            if (symbol == null) symbol = createSymbol(name, symbolBytesFromString(runtime, name), hash, hard);

            return symbol;
        }

        public RubySymbol getSymbol(ByteList bytes) {
            return getSymbol(bytes, false);
        }

        public RubySymbol getSymbol(ByteList bytes, boolean hard) {
            int hash = javaStringHashCode(bytes);

            RubySymbol symbol = findSymbol(bytes, hash, hard);

            if (symbol == null) {
                bytes = bytes.dup();
                symbol = createSymbol(
                        RubyEncoding.decodeRaw(bytes),
                        bytes,
                        hash,
                        hard);
            }

            return symbol;
        }

        /**
         * Get or retrieve an existing symbol from the table, invoking the given handler before return.
         * In the case of a new symbol, the handler will be invoked before the symbol is registered, so it can be
         * manipulated without leaking changes.
         *
         * @param bytes the symbol bytes
         * @param handler the handler to invoke
         * @param hard whether to hold a hard reference to the symbol
         * @return the new or existing symbol
         */
        public RubySymbol getSymbol(ByteList bytes, ObjBooleanConsumer<RubySymbol> handler, boolean hard) {
            int hash = javaStringHashCode(bytes);

            RubySymbol symbol = findSymbol(bytes, hash, hard);

            if (symbol == null) {
                bytes = bytes.dup();
                return createSymbol(
                        RubyEncoding.decodeRaw(bytes),
                        bytes,
                        handler,
                        hash,
                        hard);
            }

            handler.accept(symbol, false);

            return symbol;
        }

        private RubySymbol findSymbol(ByteList bytes, int hash, boolean hard) {
            RubySymbol symbol = null;

            for (SymbolEntry e = getEntryFromTable(symbolTable, hash); e != null; e = e.next) {
                if (isSymbolMatch(bytes, hash, e)) {
                    if (hard) e.setHardReference();
                    symbol = e.symbol.get();
                    break;
                }
            }

            return symbol;
        }

        public RubySymbol fastGetSymbol(String internedName) {
            return fastGetSymbol(internedName, false);
        }

        public RubySymbol fastGetSymbol(String internedName, boolean hard) {
            RubySymbol symbol = null;
            int hash = javaStringHashCode(internedName);

            for (SymbolEntry e = getEntryFromTable(symbolTable, hash); e != null; e = e.next) {
                if (isSymbolMatch(internedName, hash, e)) {
                    if (hard) e.setHardReference();
                    symbol = e.symbol.get();
                    break;
                }
            }

            if (symbol == null) {
                symbol = fastCreateSymbol(internedName, hard);
            }

            return symbol;
        }

        private static SymbolEntry getEntryFromTable(SymbolEntry[] table, int hash) {
            return table[getIndex(hash, table)];
        }

        private static boolean isSymbolMatch(String name, int hash, SymbolEntry entry) {
            return hash == entry.hash && name.equals(entry.name);
        }

        private static boolean isSymbolMatch(ByteList bytes, int hash, SymbolEntry entry) {
            return hash == entry.hash && bytes.equals(entry.bytes);
        }

        private RubySymbol createSymbol(final String name, final ByteList value, final int hash, boolean hard) {
            ReentrantLock lock = tableLock;

            lock.lock();
            try {
                final SymbolEntry[] table = getTableForCreate();
                final int index = getIndex(hash, table);

                // try lookup again under lock
                RubySymbol symbol = lookupSymbol(name, table, hash, index);

                if (symbol == null) {
                    String internedName = name.intern();
                    symbol = new RubySymbol(runtime, internedName, value);
                    storeSymbol(value, hash, hard, table, index, symbol, internedName);
                }
                return symbol;
            } finally {
                lock.unlock();
            }
        }

        /**
         * @see #newSymbol(Ruby, ByteList, ObjBooleanConsumer)
         *
         * @param name encoded symbol name
         * @param value symbol bytes
         * @param handler the handler to call
         * @param hash the hash for the symbol
         * @param hard whether to hold a hard reference to this symbol for the lifetime of the symbol table
         * @return the new or existing symbol
         */
        private RubySymbol createSymbol(final String name, final ByteList value, ObjBooleanConsumer<RubySymbol> handler, final int hash, boolean hard) {
            ReentrantLock lock = tableLock;

            lock.lock();
            try {
                final SymbolEntry[] table = getTableForCreate();
                final int index = getIndex(hash, table);

                // try lookup again under lock
                RubySymbol symbol = lookupSymbol(name, table, hash, index);

                if (symbol == null) {
                    String internedName = name.intern();
                    symbol = new RubySymbol(runtime, internedName, value);

                    // Pass to handler before storing
                    handler.accept(symbol, true);

                    storeSymbol(value, hash, hard, table, index, symbol, internedName);
                } else {
                    handler.accept(symbol, false);
                }
                return symbol;
            } finally {
                lock.unlock();
            }
        }

        private void storeSymbol(ByteList value, int hash, boolean hard, SymbolEntry[] table, int index, RubySymbol symbol, String internedName) {
            table[index] = new SymbolEntry(hash, internedName, value, symbol, table[index], hard);
            size++;
            // write-volatile
            symbolTable = table;
        }

        private RubySymbol fastCreateSymbol(final String internedName, boolean hard) {
            ReentrantLock lock;
            (lock = tableLock).lock();
            try {
                final SymbolEntry[] table = getTableForCreate();
                final int hash = internedName.hashCode();
                final int index = getIndex(hash, table);

                // try lookup again under lock
                RubySymbol symbol = lookupSymbolByString(internedName, table, index);

                if (symbol == null) {
                    symbol = new RubySymbol(runtime, internedName);
                    storeSymbol(symbol.getBytes(), hash, hard, table, index, symbol, internedName);
                }
                return symbol;
            } finally {
                lock.unlock();
            }
        }

        private static int getIndex(int hash, SymbolEntry[] table) {
            return hash & (table.length - 1);
        }

        private SymbolEntry[] getTableForCreate() {
            return size > threshold ? rehash() : symbolTable;
        }

        private RubySymbol lookupSymbol(String name, SymbolEntry[] table, int hash, int index) {
            RubySymbol symbol = null;
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
            return symbol;
        }

        private RubySymbol lookupSymbolByString(String internedName, SymbolEntry[] table, int index) {
            RubySymbol symbol = null;
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
            return symbol;
        }

        private void removeDeadEntry(SymbolEntry[] table, int index, SymbolEntry last, SymbolEntry e) {
            if (last == null) {
                table[index] = e.next; // shift head of bucket
            } else {
                last.next = e.next; // remove collected bucket entry
            }

            size--; // reduce current size because we lost one somewhere
        }

        public RubySymbol lookup(long id) {
            SymbolEntry[] table = symbolTable;
            RubySymbol symbol;

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

        public int size() {
            return size;
        }

        public void clear() {
            reset();
        }

        private void reset() {
            this.threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
            this.symbolTable = new SymbolEntry[DEFAULT_INITIAL_CAPACITY];
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
                        newTable[k] = new SymbolEntry(p.hash, p.name, p.bytes, p.symbol.get(), n, p.hardReference != null);
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

            SymbolEntry e = table[getIndex(hash, table)];
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

    // This should be identical to iso8859.toString().hashCode().
    public static int javaStringHashCode(ByteList iso8859) {
        int h = 0;
        int begin = iso8859.begin();
        int end = begin + iso8859.realSize();
        byte[] bytes = iso8859.unsafeBytes();
        for (int i = begin; i < end; i++) {
            int v = bytes[i] & 0xFF;
            h = 31 * h + v;
        }
        return h;
    }

    @Override
    public Class getJavaClass() {
        return String.class;
    }

    @Override
    public boolean shouldMarshalEncoding() {
        Encoding enc = getMarshalEncoding();

        return enc != USASCIIEncoding.INSTANCE && enc != ASCIIEncoding.INSTANCE;
    }

    @Override
    public Encoding getMarshalEncoding() {
        return getBytes().getEncoding();
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
            return ((RubySymbol) object).idString();
        }
        if (object instanceof RubyString) {
            return ((RubyString) object).getByteList().toString();
        }
        return object.convertToString().getByteList().toString();
    }

    @Deprecated
    public static String checkID(IRubyObject object) {
        return idStringFromObject(object.getRuntime().getCurrentContext(), object);
    }

    // MRI: rb_check_id but producing a Java String
    public static String idStringFromObject(ThreadContext context, IRubyObject object) {
        IRubyObject symOrStr = prepareID(object.getRuntime().getCurrentContext(), object);

        if (symOrStr instanceof RubySymbol) return ((RubySymbol) symOrStr).idString();

        return ((RubyString) symOrStr).getByteList().toString();
    }

    // MRI: rb_check_id
    public static RubySymbol idSymbolFromObject(ThreadContext context, IRubyObject object) {
        IRubyObject symOrStr = prepareID(context, object);

        if (symOrStr instanceof RubySymbol) return (RubySymbol) symOrStr;

        return newSymbol(context.runtime, ((RubyString) symOrStr).getByteList());
    }

    /**
     * Return the given object if it is a Symbol or String, or convert it to a String.
     *
     * @param context the current context
     * @param object the object
     * @return the object, if it is a Symbol or String, or a String produced by calling #to_str on the object.
     */
    public static IRubyObject prepareID(ThreadContext context, IRubyObject object) {
        if (object instanceof RubySymbol || object instanceof RubyString) return object;

        Ruby runtime = context.runtime;

        IRubyObject tmp = TypeConverter.checkStringType(context, sites(context).to_str_checked, object);

        if (tmp.isNil()) {
            throw runtime.newTypeError(str(runtime, "", object, " is not a symbol nor a string"));
        }

        return tmp;
    }

    public static final class SymbolProcBody extends ContextAwareBlockBody {
        private final CallSite site;
        private final String id;

        public SymbolProcBody(Ruby runtime, String id) {
            super(runtime.getStaticScopeFactory().getDummyScope(), Signature.OPTIONAL);
            this.site = MethodIndex.getFunctionalCallSite(id);
            this.id = id;
        }

        public SymbolProcBody(Ruby runtime, String id, StaticScope scope) {
            super(scope, Signature.OPTIONAL);
            this.site = new RefinedCachingCallSite(id, scope, CallType.FUNCTIONAL);
            this.id = id;
        }

        private IRubyObject yieldInner(ThreadContext context, RubyArray array, Block blockArg) {
            if (array.isEmpty()) {
                throw context.runtime.newArgumentError("no receiver given");
            }

            final IRubyObject self = array.shift(context);
            return site.call(context, self, self, array.toJavaArray(), blockArg);
        }

        @Override
        public IRubyObject yield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self, Block blockArg) {
            return yieldInner(context, RubyArray.newArrayMayCopy(context.runtime, args), blockArg);
        }

        @Override
        public IRubyObject yield(ThreadContext context, Block block, IRubyObject value, Block blockArg) {
            return yieldInner(context, ArgsUtil.convertToRubyArray(context.runtime, value, false), blockArg);
        }

        @Override
        protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
            return yieldInner(context, ArgsUtil.convertToRubyArray(context.runtime, value, false), Block.NULL_BLOCK);
        }

        @Override
        protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
            if (args.length == 1) {
                return yieldSpecific(context, Block.NULL_BLOCK, args[0]);
            }
            return yieldInner(context, RubyArray.newArrayMayCopy(context.runtime, args), Block.NULL_BLOCK);
        }

        @Override
        public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
            return site.call(context, arg0, arg0);
        }

        @Override
        public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1) {
            return site.call(context, arg0, arg0, arg1);
        }

        @Override
        public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            return site.call(context, arg0, arg0, arg1, arg2);
        }

        @Override
        public String getFile() {
            return site.methodName;
        }

        @Override
        public int getLine() {
            return -1;
        }

        public String getId() {
            return id;
        }

        @Override
        public ArgumentDescriptor[] getArgumentDescriptors() {
            return ArgumentDescriptor.SYMBOL_PROC;
        }
    }

    private static JavaSites.SymbolSites sites(ThreadContext context) {
        return context.sites.Symbol;
    }

    @Deprecated
    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }
}
