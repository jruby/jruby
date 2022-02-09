/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jcodings.Encoding;
import org.jcodings.EncodingDB.Entry;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

import static org.jruby.runtime.marshal.MarshalStream.RUBY2_KEYWORDS_FLAG;
import static org.jruby.runtime.marshal.MarshalStream.SYMBOL_ENCODING_SPECIAL;
import static org.jruby.util.RubyStringBuilder.inspectIdentifierByteList;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * Unmarshals objects from strings or streams in Ruby's marshal format.
 *
 * @author Anders
 */
public class UnmarshalStream extends InputStream {
    private static final char TYPE_NIL = '0';
    private static final char TYPE_TRUE = 'T';
    private static final char TYPE_FALSE = 'F';
    private static final char TYPE_FIXNUM = 'i';
    private static final char TYPE_EXTENDED	= 'e';
    private static final char TYPE_UCLASS = 'C';
    private static final char TYPE_OBJECT = 'o';
    private static final char TYPE_DATA = 'd';
    private static final char TYPE_USERDEF = 'u';
    private static final char TYPE_USRMARSHAL = 'U';
    private static final char TYPE_FLOAT = 'f';
    private static final char TYPE_BIGNUM = 'l';
    private static final char TYPE_STRING = '"';
    private static final char TYPE_REGEXP = '/';
    private static final char TYPE_ARRAY = '[';
    private static final char TYPE_HASH = '{';
    private static final char TYPE_HASH_DEF = '}';
    private static final char TYPE_STRUCT = 'S';
    private static final char TYPE_MODULE_OLD = 'M';
    private static final char TYPE_CLASS = 'c';
    private static final char TYPE_MODULE = 'm';
    private static final char TYPE_SYMBOL = ':';
    private static final char TYPE_SYMLINK = ';';
    private static final char TYPE_IVAR	= 'I';
    private static final char TYPE_LINK	= '@';

    protected final Ruby runtime;
    private final UnmarshalCache cache;
    private IRubyObject proc;
    private final InputStream inputStream;
    private final boolean freeze;

    public UnmarshalStream(Ruby runtime, InputStream in, IRubyObject proc) throws IOException {
        this(runtime, in, false, proc);
    }

    public UnmarshalStream(Ruby runtime, InputStream in, boolean freeze, IRubyObject proc) throws IOException {
        assert runtime != null;
        assert in != null;

        // Older native java ext expects proc can be null (spymemcached.jruby at least).
        if (proc == null) proc = runtime.getNil();
        
        this.runtime = runtime;
        this.cache = new UnmarshalCache(runtime);
        this.proc = proc;
        this.inputStream = in;
        this.freeze = freeze;

        int major = in.read(); // Major
        int minor = in.read(); // Minor

        if(major == -1 || minor == -1) {
            throw new EOFException("Unexpected end of stream");
        }

        if(major != Constants.MARSHAL_MAJOR || minor > Constants.MARSHAL_MINOR) {
            throw runtime.newTypeError(String.format("incompatible marshal file format (can't be read)\n\tformat version %d.%d required; %d.%d given", Constants.MARSHAL_MAJOR, Constants.MARSHAL_MINOR, major, minor));
        }
    }

    // r_object
    public IRubyObject unmarshalObject() throws IOException {
        return object0(new MarshalState(false), false, null);
    }

    // r_object
    public IRubyObject unmarshalObject(boolean _callProc) throws IOException { // <-- 100% false by all callers
        // FIXME: We might want to flag proc on/off or just pass it around everywhere.
        IRubyObject savedProc = this.proc;
        try {
            proc = null;
            return object0(new MarshalState(false), false, null);
        } finally {
            proc = savedProc;
        }
    }

    // introduced for keeping ivar read state recursively.
    public static class MarshalState {
        private boolean ivarWaiting;

        MarshalState(boolean ivarWaiting) {
            this.ivarWaiting = ivarWaiting;
        }

        public boolean isIvarWaiting() {
            return ivarWaiting;
        }

        public void setIvarWaiting(boolean ivarWaiting) {
            this.ivarWaiting = ivarWaiting;
        }
    }

    // r_object0
    @Deprecated
    private IRubyObject unmarshalObject(MarshalState state) throws IOException {
        return unmarshalObject(state, true);
    }

    // r_object0
    @Deprecated
    private IRubyObject unmarshalObject(MarshalState state, boolean callProc) throws IOException {
        final int type = readUnsignedByte();
        final IRubyObject result;
        if (cache.isLinkType(type)) {
            result = cache.readLink(this, type);
            if (callProc) return doCallProcForLink(result, type);
        } else {
            result = unmarshalObjectDirectly(type, state, callProc);
        }

        return result;
    }

    @Deprecated
    public void registerLinkTarget(IRubyObject newObject) {
        if (MarshalStream.shouldBeRegistered(newObject)) {
            cache.register(newObject);
        }
    }

    public static RubyModule getModuleFromPath(Ruby runtime, String path) {
        final RubyModule value = runtime.getClassFromPath(path, runtime.getArgumentError(), false);
        if (value == null) throw runtime.newArgumentError("undefined class/module " + path);
        if ( ! value.isModule() ) throw runtime.newArgumentError(path + " does not refer module");
        return value;
    }

    public static RubyClass getClassFromPath(Ruby runtime, String path) {
        final RubyModule value = runtime.getClassFromPath(path, runtime.getArgumentError(), false);
        if (value == null) throw runtime.newArgumentError("undefined class/module " + path);
        if ( ! value.isClass() ) throw runtime.newArgumentError(path + " does not refer class");
        return (RubyClass) value;
    }

    @Deprecated
    private IRubyObject doCallProcForLink(IRubyObject result, int type) {
        if (!proc.isNil() && type != ';') {
            // return the result of the proc, but not for symbols
            return Helpers.invoke(getRuntime().getCurrentContext(), proc, "call", result);
        }
        return result;
    }

    private IRubyObject doCallProcForObj(IRubyObject result) {
        if (proc == null || proc.isNil()) return result;

        return Helpers.invoke(getRuntime().getCurrentContext(), proc, "call", result);
    }


    private int r_byte() throws IOException {
        return readUnsignedByte();
    }

    public void ivar(MarshalState state, IRubyObject object, boolean[] hasEncoding) throws IOException {
        int count = unmarshalInt();
        RubyClass clazz = object.getMetaClass().getRealClass();

        // Keys may be "E", "encoding",or any valid ivar.  Weird code what happens with @encoding that happens to have
        // a string of "US-ASCII"?
        for (int i = 0; i < count; i++) {
            RubySymbol key = symbol(null);
            String id = key.idString();
            IRubyObject value = object0(state, false, null);
            Encoding encoding = symbolToEncoding(key, value);

            if (encoding != null) {
                if (object instanceof EncodingCapable) {
                    if (encoding != null) ((EncodingCapable) object).setEncoding(encoding);
                } else {
                    throw runtime.newArgumentError(str(runtime, object, "is not enc_capable"));
                }
                if (hasEncoding != null) hasEncoding[0] = true;
            } else if (id.equals(RUBY2_KEYWORDS_FLAG)) {
                if (object instanceof RubyHash) {
                    ((RubyHash) object).setRuby2KeywordHash(true);
                } else {
                    throw runtime.newArgumentError(str(runtime, "ruby2_keywords flag is given but ", object, " is not a Hash"));
                }
            } else {
                clazz.getVariableAccessorForWrite(key.idString()).set(object, value);
            }
        }
    }

    public IRubyObject entry(IRubyObject value) {
        cache.registerDataLink(value);
        cache.markAsPartialObject(value);

        return value;
    }

    private IRubyObject leave(IRubyObject value, boolean partial) {
        if (!partial) {
            cache.noLongerPartial(value);
            if (freeze) {
                RubyClass metaClass = value.getMetaClass();
                if (metaClass == runtime.getString()) {
                    IRubyObject original = value;
                    // FIXME: We need to modify original to be frozen but we also need it to be part of the deduped table.
                    value = runtime.freezeAndDedupString((RubyString) value);
                    if (value != original) {
                        original.setFrozen(value.isFrozen());
                    }
                } else if (!value.isModule() && !value.isClass()) {
                    value.setFrozen(true);
                }
            }
            value = postProc(value);
        }

        return value;
    }

    private RubyModule mustBeModule(Ruby runtime, IRubyObject value, IRubyObject path) {
        if (!value.isModule()) throw runtime.newArgumentError(str(runtime, path, " does not refer to module"));

        return (RubyModule) value;
    }

    private IRubyObject object0(MarshalState state, boolean partial, List<RubyModule> extendedModules) throws IOException {
        return objectFor(r_byte(), state, partial,  extendedModules);
    }

    private IRubyObject objectFor(int type, MarshalState state, boolean partial, List<RubyModule> extendedModules) throws IOException {
        IRubyObject obj;

        switch (type) {
            case TYPE_LINK:
                obj = objectForLink();
                break;
            case TYPE_IVAR:
                obj = objectForIVar(state, partial, extendedModules);
                break;
            case TYPE_EXTENDED:
                obj = objectForExtended(extendedModules);
                break;
            case TYPE_UCLASS:
                obj = objectForUClass(partial, extendedModules);
                break;
            case TYPE_NIL:
                obj = objectForNil();
                break;
            case TYPE_TRUE:
                obj = objectForTrue();
                break;
            case TYPE_FALSE:
                obj = objectForFalse();
                break;
            case TYPE_FIXNUM:
                obj = objectForFixnum();
                break;
            case TYPE_FLOAT:
                obj = objectForFloat();
                break;
            case TYPE_BIGNUM:
                obj = objectForBignum();
                break;
            case TYPE_STRING:
                obj = objectForString(partial);
                break;
            case TYPE_REGEXP:
                obj = objectForRegexp(state, partial);
                break;
            case TYPE_ARRAY:
                obj = objectForArray(partial);
                break;
            case TYPE_HASH:
                obj = objectForHash(partial);
                break;
            case TYPE_HASH_DEF:
                obj = objectForHashDefault(partial);
                break;
            case TYPE_STRUCT:
                obj = objectForStruct(partial);
                break;
            case TYPE_USERDEF:
                obj = objectForUserDef(state, partial);
                break;
            case TYPE_USRMARSHAL:
                obj = objectForUsrMarshal(state, partial, extendedModules);
                break;
            case TYPE_OBJECT:
                obj = objectForObject(partial);
                break;
            case TYPE_DATA:
                obj = objectForData(state, partial, extendedModules);
                break;
            case TYPE_MODULE_OLD:
                obj = objectForModuleOld(state, partial);
                break;
            case TYPE_CLASS:
                obj = objectForClass(partial);
                break;
            case TYPE_MODULE:
                obj = objectForModule(partial);
                break;
            case TYPE_SYMBOL:
                obj = objectForSymbol(state, partial);
                break;
            case TYPE_SYMLINK:
                obj = objectForSymlink();
                break;
            default :
                throw getRuntime().newArgumentError("dump format error(" + (char)type + ")");
        }

        return obj;
    }

    private IRubyObject objectForModuleOld(MarshalState state, boolean partial) throws IOException {
        String name = RubyString.byteListToString(unmarshalString());
        RubyModule mod = UnmarshalStream.getModuleFromPath(getRuntime(), name);

        prohibitIVar(state, "class/module", name);

        return leave(entry(mod), partial);
    }

    public void prohibitIVar(MarshalState state, String label, String name) {
        if (state != null && state.isIvarWaiting()) {
            runtime.newTypeError("can't override instance variable of " + label + "`" + name + "'");
        }
    }

    private RubySymbol objectForSymlink() throws IOException {
        return symlink();
    }

    private IRubyObject objectForSymbol(MarshalState state, boolean partial) throws IOException {
        IRubyObject obj = symreal(state);

        if (state != null && state.isIvarWaiting()) state.setIvarWaiting(false);

        return leave(obj, partial);
    }

    private IRubyObject objectForModule(boolean partial) throws IOException {
        return leave(entry(RubyModule.unmarshalFrom(this)), partial);
    }

    private IRubyObject objectForClass(boolean partial) throws IOException {
        return leave(entry(RubyClass.unmarshalFrom(this)), partial);
    }

    private IRubyObject objectForData(MarshalState state, boolean partial, List<RubyModule> extendedModules) throws IOException {
        IRubyObject name = unique();
        RubyClass klass = getClassFromPath(runtime, name.asJavaString());
        IRubyObject obj = entry(klass.allocate());
        // FIXME: Can/Should we be doing T_DATA error check?

        // obj = entry(obj);
        if (!obj.respondsTo("_load_data")) throw runtime.newTypeError(str(runtime, name, " needs to have instance method _load_data"));

        IRubyObject arg = object0(state, partial, extendedModules);
        obj.callMethod(runtime.getCurrentContext(), "_load_data", arg);
        return leave(obj, partial);
    }

    private IRubyObject objectForObject(boolean partial) throws IOException {
        return leave(defaultObjectUnmarshal(), partial);
    }

    private IRubyObject objectForUserDef(MarshalState state, boolean partial) throws IOException {
        IRubyObject obj = userUnmarshal(state);

        if (!partial) obj = postProc(obj);

        return obj;
    }

    private IRubyObject objectForStruct(boolean partial) throws IOException {
        return leave(RubyStruct.unmarshalFrom(this), partial);
    }

    private IRubyObject objectForHashDefault(boolean partial) throws IOException {
        return leave(RubyHash.unmarshalFrom(this, true), partial);
    }

    private IRubyObject objectForHash(boolean partial) throws IOException {
        return leave(RubyHash.unmarshalFrom(this, false), partial);
    }

    private IRubyObject objectForArray(boolean partial) throws IOException {
        return leave(RubyArray.unmarshalFrom(this), partial);
    }

    private IRubyObject objectForRegexp(MarshalState state, boolean partial) throws IOException {
        return leave(entry(unmarshalRegexp(state)), partial); // FIXME: MRI uses entry_0 here which may be more complicated???
    }

    private IRubyObject objectForString(boolean partial) throws IOException {
        return leave(entry(RubyString.unmarshalFrom(this)), partial);
    }

    private IRubyObject objectForBignum() throws IOException {
        return leave(entry(RubyBignum.unmarshalFrom(this)), false);
    }

    private IRubyObject objectForFloat() throws IOException {
        return leave(entry(RubyFloat.unmarshalFrom(this)), false);
    }

    private IRubyObject objectForFixnum() throws IOException {
        return leave(RubyFixnum.unmarshalFrom(this), false);
    }

    private IRubyObject objectForFalse() {
        return leave(runtime.getFalse(), false);
    }

    private IRubyObject objectForTrue() {
        return leave(runtime.getTrue(), false);
    }

    private IRubyObject objectForNil() {
        return leave(runtime.getNil(), false);
    }

    private IRubyObject objectForUClass(boolean partial, List<RubyModule> extendedModules) throws IOException {
        RubyClass c = getClassFromPath(runtime, unique().asJavaString());

        if (c.isSingleton()) throw runtime.newTypeError("singleton can't be loaded");

        int type = r_byte();
        if (c == runtime.getHash() && (type == TYPE_HASH || type == TYPE_HASH_DEF)) {

        }

        IRubyObject obj = objectFor(type, null, partial, extendedModules);
        // if result is a module or type doesn't extend result's class...
        if (obj.getMetaClass() == runtime.getModule() || !c.isKindOfModule(obj.getMetaClass())) {
            // if allocators do not match, error
            // Note: MRI is a bit different here, and tests TYPE(type.allocate()) != TYPE(result)
            if (c.getAllocator() != obj.getMetaClass().getRealClass().getAllocator()) {
                throw runtime.newArgumentError("dump format error (user class)");
            }
        }

        ((RubyObject) obj).setMetaClass(c);
        return obj;
    }

    private IRubyObject objectForExtended(List<RubyModule> extendedModules) throws IOException {
        IRubyObject path = unique();
        IRubyObject m = getModuleFromPath(runtime, path.asJavaString());

        if (extendedModules == null) extendedModules = new ArrayList<>();

        IRubyObject obj;
        if (m instanceof RubyClass) {
            obj = object0(null, true, null);
            RubyClass cls = obj.getMetaClass();
            if (cls != m || cls.isSingleton()) {
                throw runtime.newArgumentError(str(runtime, "prepended class ", path, " differs from class ", cls));
            }

            cls = obj.getSingletonClass();

            for (RubyModule mod: extendedModules) {
                cls.prependModule(mod);
            }
        } else {
            extendedModules.add(mustBeModule(runtime, m, path));

            obj = object0(null, true, extendedModules);

            appendExtendedModules(obj, extendedModules);
        }

        return obj;
    }

    private IRubyObject objectForIVar(MarshalState state, boolean partial, List<RubyModule> extendedModules) throws IOException {
        MarshalState state1 = new MarshalState(true);
        IRubyObject obj = object0(state1, true, extendedModules);

        if (state1.ivarWaiting) ivar(state, obj, null);

        return leave(obj, partial);
    }

    private IRubyObject objectForLink() throws IOException {
        IRubyObject obj = cache.readDataLink(this);

        if (!cache.isPartialObject(obj)) obj = postProc(obj);

        return obj;
    }

    private IRubyObject postProc(IRubyObject value) {
        return doCallProcForObj(value);
    }

    private RubySymbol symbol(MarshalState state) throws IOException {
        boolean ivar = false;

        while (true) {
            int type = r_byte();

            switch (type) {
                case TYPE_IVAR:
                    ivar = true;
                    continue;
                case TYPE_SYMBOL: {
                    MarshalState state1 = new MarshalState(ivar);
                    return symreal(state1);
                }
                case TYPE_SYMLINK:
                    if (ivar) throw runtime.newArgumentError("dump format error (symlink with encoding)");
                    return symlink();
                default:
                    throw runtime.newArgumentError("dump format error for symbol(0x" + Integer.toHexString(type) + ")");
            }
        }
    }

    private RubySymbol symlink() throws IOException {
        return (RubySymbol) cache.readSymbolLink(this);
    }

    private RubySymbol symreal(MarshalState state) throws IOException {
        ByteList byteList = unmarshalString();
        // FIXME: needs to onyl do this is only us-ascii
        byteList.setEncoding(ASCIIEncoding.INSTANCE);
        final UnmarshalStream input = this;

        RubySymbol symbol = RubySymbol.newSymbol(runtime, byteList,
                (sym, newSym) -> {
                    try {
                        Encoding encoding = null;
                        cache.registerSymbolLink(sym);

                        if (state != null && state.isIvarWaiting()) {
                            int num = input.unmarshalInt();
                            for (int i = 0; i < num; i++) {
                                RubySymbol sym2 = input.symbol(null);
                                encoding = symbolToEncoding(sym2, input.unmarshalObject());
                            }
                        }

                        if (encoding != null) {
                            sym.getBytes().setEncoding(encoding);

                            if (StringSupport.codeRangeScan(encoding, sym.getBytes()) == StringSupport.CR_BROKEN) {
                                throw runtime.newArgumentError(str(runtime, "invalid byte sequence in " + encoding + ": ",
                                        inspectIdentifierByteList(runtime, sym.getBytes())));
                            }
                        }
                    } catch (IOException e) {
                    }
                });

        return symbol;
    }

    private IRubyObject unique() throws IOException {
        return symbol(null);
    }

    private IRubyObject unmarshalObjectDirectly(int type, MarshalState state, boolean callProc) throws IOException {
        IRubyObject rubyObj;
        switch (type) {
            case 'I':
                MarshalState childState = new MarshalState(true);
                rubyObj = unmarshalObject(childState);
                if (childState.isIvarWaiting()) {
                    defaultVariablesUnmarshal(state, rubyObj);
                }
                return rubyObj;
            case '0' :
                rubyObj = runtime.getNil();
                break;
            case 'T' :
                rubyObj = runtime.getTrue();
                break;
            case 'F' :
                rubyObj = runtime.getFalse();
                break;
            case '"' :
                rubyObj = RubyString.unmarshalFrom(this);
                break;
            case 'i' :
                rubyObj = RubyFixnum.unmarshalFrom(this);
                break;
            case 'f' :
                rubyObj = RubyFloat.unmarshalFrom(this);
                break;
            case '/':
                rubyObj = unmarshalRegexp(state);
                break;
            case ':' :
                rubyObj = RubySymbol.unmarshalFrom(this, state);
                break;
            case '[' :
                rubyObj = RubyArray.unmarshalFrom(this);
                break;
            case '{' :
                rubyObj = RubyHash.unmarshalFrom(this, false);
                break;
            case '}' :
                // "hashdef" object, a hash with a default
                rubyObj = RubyHash.unmarshalFrom(this, true);
                break;
            case 'c' :
                rubyObj = RubyClass.unmarshalFrom(this);
                break;
            case 'm' :
                rubyObj = RubyModule.unmarshalFrom(this);
                break;
            case 'e':
                RubySymbol moduleName = (RubySymbol) unmarshalObject();
                final RubyModule tp = getModuleFromPath(runtime, moduleName.asJavaString());

                rubyObj = unmarshalObject();

                tp.extend_object(rubyObj);
                tp.callMethod(runtime.getCurrentContext(), "extended", rubyObj);
                break;
            case 'l' :
                rubyObj = RubyBignum.unmarshalFrom(this);
                break;
            case 'S' :
                rubyObj = RubyStruct.unmarshalFrom(this);
                break;
            case 'o' :
                rubyObj = defaultObjectUnmarshal();
                break;
            case 'u' :
                rubyObj = userUnmarshal(state);
                break;
            case 'U' :
                rubyObj = objectForUsrMarshal(state, false, null);
                break;
            case 'C' :
                rubyObj = uclassUnmarshall();
                break;
            default :
                throw getRuntime().newArgumentError("dump format error(" + (char)type + ")");
        }

        return callProc ? doCallProcForObj(rubyObj) : rubyObj;
    }

    private IRubyObject unmarshalRegexp(MarshalState state) throws IOException {
        ByteList byteList = unmarshalString();
        byte opts = readSignedByte();
        RegexpOptions reOpts = RegexpOptions.fromJoniOptions(opts);

        RubyRegexp regexp = (RubyRegexp) runtime.getRegexp().allocate();

        IRubyObject ivarHolder = null;
        boolean[] hasEncoding = new boolean[] { false };

        if (state != null && state.isIvarWaiting()) {
            RubyString tmpStr = RubyString.newString(runtime, byteList);

            ivar(state, tmpStr, hasEncoding);
            byteList = tmpStr.getByteList();
            state.setIvarWaiting(false);
            ivarHolder = tmpStr;
        }
        if (!hasEncoding[0]) {
            /* 1.8 compatibility; remove escapes undefined in 1.8 */
            byte[] ptrBytes = byteList.unsafeBytes();
            int ptr = byteList.begin();
            int dst = ptr;
            int src = ptr;
            int len = byteList.realSize();
            long bs = 0;
            for (; len-- > 0; ptrBytes[dst++] = ptrBytes[src++]) {
                switch (ptrBytes[src]) {
                    case '\\':
                        bs++;
                        break;
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'u':
                    case 'y':
                    case 'E':
                    case 'F':
                    case 'H':
                    case 'I':
                    case 'J':
                    case 'K':
                    case 'L':
                    case 'N':
                    case 'O':
                    case 'P':
                    case 'Q':
                    case 'R':
                    case 'S':
                    case 'T':
                    case 'U':
                    case 'V':
                    case 'X':
                    case 'Y':
                        if ((bs & 1) != 0) --dst;
                    default:
                        bs = 0;
                        break;
                }
            }
            byteList.setRealSize(dst - ptr);
        }

        regexp.regexpInitialize(byteList, byteList.getEncoding(), reOpts);

        if (ivarHolder != null) {
            ivarHolder.getInstanceVariables().copyInstanceVariablesInto(regexp);
        }

        return regexp;
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public int readUnsignedByte() throws IOException {
        int result = read();
        if (result == -1) throw new EOFException("Unexpected end of stream");
        return result;
    }

    public byte readSignedByte() throws IOException {
        int b = readUnsignedByte();
        if (b > 127) {
            return (byte) (b - 256);
        }
        return (byte) b;
    }

    public ByteList unmarshalString() throws IOException {
        int length = unmarshalInt();
        byte[] buffer = new byte[length];

        int readLength = 0;
        while (readLength < length) {
            int read = inputStream.read(buffer, readLength, length - readLength);

            if (read == -1) {
                throw getRuntime().newArgumentError("marshal data too short");
            }
            readLength += read;
        }

        return new ByteList(buffer,false);
    }

    public int unmarshalInt() throws IOException {
        int c = readSignedByte();
        if (c == 0) {
            return 0;
        } else if (4 < c && c < 128) {
            return c - 5;
        } else if (-129 < c && c < -4) {
            return c + 5;
        }
        long result;
        if (c > 0) {
            result = 0;
            for (int i = 0; i < c; i++) {
                result |= (long) readUnsignedByte() << (8 * i);
            }
        } else {
            c = -c;
            result = -1;
            for (int i = 0; i < c; i++) {
                result &= ~((long) 0xff << (8 * i));
                result |= (long) readUnsignedByte() << (8 * i);
            }
        }
        return (int) result;
    }

    private IRubyObject defaultObjectUnmarshal() throws IOException {
        RubySymbol className = (RubySymbol) unmarshalObject(false);

        RubyClass type = getClassFromPath(runtime, className.idString());

        return (IRubyObject)type.unmarshal(this);
    }

    public void defaultVariablesUnmarshal(MarshalState state, IRubyObject object) throws IOException {
        ivar(state, object, null);
    }

    // MRI: sym2encidx
    public Encoding symbolToEncoding(RubySymbol symbol, IRubyObject value) {
        if (symbol.getEncoding() != USASCIIEncoding.INSTANCE) return null;

        String id = symbol.idString();

        if (id.equals("encoding")) {
            String encodingNameStr = value.asJavaString();
            ByteList encodingName = new ByteList(ByteList.plain(encodingNameStr));

            Entry entry = runtime.getEncodingService().findEncodingOrAliasEntry(encodingName);
            if (entry == null) throw runtime.newArgumentError(str(runtime, "encoding ", value, " is not registered"));

            return entry.getEncoding();
        } else if (id.equals(SYMBOL_ENCODING_SPECIAL)) {
            return value.isTrue() ? UTF8Encoding.INSTANCE : USASCIIEncoding.INSTANCE;
        }

        return null;
    }

    public Encoding getEncodingFromUnmarshaled(IRubyObject key) throws IOException {
        Encoding enc = null;

        if (key.asJavaString().equals(MarshalStream.SYMBOL_ENCODING_SPECIAL)) {

            // special case for USASCII and UTF8
            if (unmarshalObject().isTrue()) {
                enc = UTF8Encoding.INSTANCE;
            } else {
                enc = USASCIIEncoding.INSTANCE;
            }

        } else if (key.asJavaString().equals("encoding")) {

            IRubyObject encodingNameObj = unmarshalObject(false);
            String encodingNameStr = encodingNameObj.asJavaString();
            ByteList encodingName = new ByteList(ByteList.plain(encodingNameStr));

            Entry entry = runtime.getEncodingService().findEncodingOrAliasEntry(encodingName);
            if (entry == null) {
                throw runtime.newArgumentError("invalid encoding in marshaling stream: " + encodingName);
            }
            enc = entry.getEncoding();
        }

        return enc;
    }

    private IRubyObject uclassUnmarshall() throws IOException {
        RubySymbol className = (RubySymbol)unmarshalObject(false);

        RubyClass type = getClassFromPath(runtime, className.asJavaString());

        // singleton, raise error
        if (type.isSingleton()) throw runtime.newTypeError("singleton can't be loaded");

        // All "C" marshalled objects descend from core classes, which are all at least RubyObject
        RubyObject result = (RubyObject)unmarshalObject();

        // if result is a module or type doesn't extend result's class...
        if (result.getMetaClass() == runtime.getModule() || !type.isKindOfModule(result.getMetaClass())) {
            // if allocators do not match, error
            // Note: MRI is a bit different here, and tests TYPE(type.allocate()) != TYPE(result)
            if (type.getAllocator() != result.getMetaClass().getRealClass().getAllocator()) {
                throw runtime.newArgumentError("dump format error (user class)");
            }
        }

        result.setMetaClass(type);

        return result;
    }

    private IRubyObject userUnmarshal(MarshalState state) throws IOException {
        String className = unique().asJavaString();
        RubyClass classInstance = getClassFromPath(runtime, className);
        RubyString data = RubyString.newString(runtime, unmarshalString());
        IRubyObject unmarshaled;

        // Special case Encoding so they are singletons
        // See https://bugs.ruby-lang.org/issues/11760
        if (classInstance == runtime.getEncoding()) {
            unmarshaled = RubyEncoding.find(runtime.getCurrentContext(), classInstance, data);
        } else {
            if (state != null && state.isIvarWaiting()) {
                ivar(state, data, null);
                state.setIvarWaiting(false);
            }
            unmarshaled = classInstance.smartLoadOldUser(data);
        }

        return entry(unmarshaled);
    }

    // FIXME: This is missing a much more complicated set of logic in tracking old compatibility allocators. See MRI for more details
    private IRubyObject objectForUsrMarshal(MarshalState state, boolean partial, List<RubyModule> extendedModules) throws IOException {
        RubyClass classInstance = getClassFromPath(runtime, unique().asJavaString());
        IRubyObject obj = classInstance.allocate();

        if (extendedModules != null) appendExtendedModules(obj, extendedModules);

        obj = entry(obj);
        IRubyObject marshaled = unmarshalObject();
        obj = classInstance.smartLoadNewUser(obj, marshaled);
        obj = fixupCompat(obj);
        //obj = copyIVars(obj, marshaled);
        obj = postProc(obj);

        if (extendedModules != null) {
            extendedModules = null;
        }

        return obj;
    }

    private IRubyObject fixupCompat(IRubyObject obj) {

        /* FIXME: we need to store allocators and then potentially use the ones associated with compat class values.
        IRubyObject compatObj = shared.hasCompatValue(obj);
        if (compatObj != null) {

        }*/
        return obj;
    }

    private void appendExtendedModules(IRubyObject obj, List<RubyModule> extendedModules) {
        Collections.reverse(extendedModules);
        for (RubyModule module: extendedModules) {
            module.extend_object(obj);
        }
    }

    public int read() throws IOException {
        return inputStream.read();
    }

    @Deprecated
    public UnmarshalStream(Ruby runtime, InputStream in, IRubyObject proc, boolean taint, boolean untrust) throws IOException {
        this(runtime, in, proc);
    }

    @Deprecated
    public UnmarshalStream(Ruby runtime, InputStream in, IRubyObject proc, boolean taint) throws IOException {
        this(runtime, in, proc);
    }
}
