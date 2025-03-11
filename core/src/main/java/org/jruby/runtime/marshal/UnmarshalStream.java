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

import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Access.moduleClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.marshal.MarshalCommon.*;
import static org.jruby.util.RubyStringBuilder.inspectIdentifierByteList;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * Unmarshals objects from strings or streams in Ruby's marshal format.
 *
 * @author Anders
 */
@Deprecated(since = "10.0", forRemoval = true)
@SuppressWarnings("removal")
public class UnmarshalStream extends InputStream {

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
            throw typeError(runtime.getCurrentContext(), String.format("incompatible marshal file format (can't be read)\n\tformat version %d.%d required; %d.%d given", Constants.MARSHAL_MAJOR, Constants.MARSHAL_MINOR, major, minor));
        }
    }

    // r_object
    public IRubyObject unmarshalObject() throws IOException {
        return object0(runtime.getCurrentContext(), new MarshalState(false), false, null);
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

    public static RubyModule getModuleFromPath(Ruby runtime, String path) {
        final RubyModule value = runtime.getClassFromPath(path, runtime.getArgumentError(), false);
        if (value == null) throw argumentError(runtime.getCurrentContext(), "undefined class/module " + path);
        if ( ! value.isModule() ) throw argumentError(runtime.getCurrentContext(), path + " does not refer module");
        return value;
    }

    public static RubyClass getClassFromPath(Ruby runtime, String path) {
        final RubyModule value = runtime.getClassFromPath(path, runtime.getArgumentError(), false);
        if (value == null) throw argumentError(runtime.getCurrentContext(), "undefined class/module " + path);
        if ( ! value.isClass() ) throw argumentError(runtime.getCurrentContext(), path + " does not refer class");
        return (RubyClass) value;
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
        var context = runtime.getCurrentContext();

        // Keys may be "E", "encoding",or any valid ivar.  Weird code what happens with @encoding that happens to have
        // a string of "US-ASCII"?
        for (int i = 0; i < count; i++) {
            RubySymbol key = symbol();
            String id = key.idString();
            IRubyObject value = object0(context, state, false, null);
            Encoding encoding = symbolToEncoding(key, value);

            if (encoding != null) {
                if (object instanceof EncodingCapable) {
                    ((EncodingCapable) object).setEncoding(encoding);
                } else {
                    throw argumentError(context, str(runtime, object, "is not enc_capable"));
                }
                if (hasEncoding != null) hasEncoding[0] = true;
            } else if (id.equals(RUBY2_KEYWORDS_FLAG)) {
                if (object instanceof RubyHash) {
                    ((RubyHash) object).setRuby2KeywordHash(true);
                } else {
                    throw argumentError(context, str(runtime, "ruby2_keywords flag is given but ", object, " is not a Hash"));
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

    private IRubyObject leave(ThreadContext context, IRubyObject value, boolean partial) {
        if (!partial) {
            cache.noLongerPartial(value);
            if (freeze) {
                RubyClass metaClass = value.getMetaClass();
                if (metaClass == stringClass(context)) {
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

    private RubyModule mustBeModule(ThreadContext context, IRubyObject value, IRubyObject path) {
        if (!value.isModule()) throw argumentError(context, str(context.runtime, path, " does not refer to module"));

        return (RubyModule) value;
    }

    private IRubyObject object0(ThreadContext context, MarshalState state, boolean partial, List<RubyModule> extendedModules) throws IOException {
        return objectFor(context, r_byte(), state, partial,  extendedModules);
    }

    private IRubyObject objectFor(ThreadContext context, int type, MarshalState state, boolean partial,
                                  List<RubyModule> extendedModules) throws IOException {
        IRubyObject obj;

        switch (type) {
            case TYPE_LINK -> obj = objectForLink();
            case TYPE_IVAR -> obj = objectForIVar(context, state, partial, extendedModules);
            case TYPE_EXTENDED -> obj = objectForExtended(context, extendedModules);
            case TYPE_UCLASS -> obj = objectForUClass(context, partial, extendedModules);
            case TYPE_NIL -> obj = objectForNil(context);
            case TYPE_TRUE -> obj = objectForTrue(context);
            case TYPE_FALSE -> obj = objectForFalse(context);
            case TYPE_FIXNUM -> obj = objectForFixnum(context);
            case TYPE_FLOAT -> obj = objectForFloat(context);
            case TYPE_BIGNUM -> obj = objectForBignum(context);
            case TYPE_STRING -> obj = objectForString(context, partial);
            case TYPE_REGEXP -> obj = objectForRegexp(context, state, partial);
            case TYPE_ARRAY -> obj = objectForArray(context, partial);
            case TYPE_HASH -> obj = objectForHash(context, partial);
            case TYPE_HASH_DEF -> obj = objectForHashDefault(context, partial);
            case TYPE_STRUCT -> obj = objectForStruct(context, partial);
            case TYPE_USERDEF -> obj = objectForUserDef(state, partial);
            case TYPE_USRMARSHAL -> obj = objectForUsrMarshal(context, state, partial, extendedModules);
            case TYPE_OBJECT -> obj = objectForObject(context, partial);
            case TYPE_DATA -> obj = objectForData(context, state, partial, extendedModules);
            case TYPE_MODULE_OLD -> obj = objectForModuleOld(context, state, partial);
            case TYPE_CLASS -> obj = objectForClass(context, partial);
            case TYPE_MODULE -> obj = objectForModule(context, partial);
            case TYPE_SYMBOL -> obj = objectForSymbol(context, state, partial);
            case TYPE_SYMLINK -> obj = objectForSymlink();
            default -> throw argumentError(runtime.getCurrentContext(), "dump format error(" + (char) type + ")");
        }

        return obj;
    }

    private IRubyObject objectForModuleOld(ThreadContext context, MarshalState state, boolean partial) throws IOException {
        String name = RubyString.byteListToString(unmarshalString());
        RubyModule mod = UnmarshalStream.getModuleFromPath(getRuntime(), name);

        prohibitIVar(state, "class/module", name);

        return leave(context, entry(mod), partial);
    }

    public void prohibitIVar(MarshalState state, String label, String name) {
        if (state != null && state.isIvarWaiting()) {
            throw typeError(runtime.getCurrentContext(), "can't override instance variable of " + label + "'" + name + "'");
        }
    }

    private RubySymbol objectForSymlink() throws IOException {
        return symlink();
    }

    private IRubyObject objectForSymbol(ThreadContext context, MarshalState state, boolean partial) throws IOException {
        IRubyObject obj = symreal(state);

        if (state != null && state.isIvarWaiting()) state.setIvarWaiting(false);

        return leave(context, obj, partial);
    }

    private IRubyObject objectForModule(ThreadContext context, boolean partial) throws IOException {
        return leave(context, entry(RubyModule.unmarshalFrom(this)), partial);
    }

    private IRubyObject objectForClass(ThreadContext context, boolean partial) throws IOException {
        return leave(context, entry(RubyClass.unmarshalFrom(this)), partial);
    }

    private IRubyObject objectForData(ThreadContext context, MarshalState state, boolean partial,
                                      List<RubyModule> extendedModules) throws IOException {
        IRubyObject name = unique();
        RubyClass klass = getClassFromPath(context.runtime, name.asJavaString());
        IRubyObject obj = entry(klass.allocate(context));
        // FIXME: Missing T_DATA error check?

        if (!obj.respondsTo("_load_data")) {
            throw typeError(context, str(context.runtime, name, " needs to have instance method _load_data"));
        }

        IRubyObject arg = object0(context, state, partial, extendedModules);
        obj.callMethod(context, "_load_data", arg);
        return leave(context, obj, partial);
    }

    private IRubyObject objectForObject(ThreadContext context, boolean partial) throws IOException {
        RubySymbol className = symbol();
        RubyClass type = getClassFromPath(context.runtime, className.idString());

        IRubyObject obj = (IRubyObject) type.unmarshal(this);
        return leave(context, obj, partial);
    }

    private IRubyObject objectForUserDef(MarshalState state, boolean partial) throws IOException {
        IRubyObject obj = userUnmarshal(state);

        if (!partial) obj = postProc(obj);

        return obj;
    }

    private IRubyObject objectForStruct(ThreadContext context, boolean partial) throws IOException {
        return leave(context, RubyStruct.unmarshalFrom(this), partial);
    }

    private IRubyObject objectForHashDefault(ThreadContext context, boolean partial) throws IOException {
        return leave(context, RubyHash.unmarshalFrom(this, true), partial);
    }

    private IRubyObject objectForHash(ThreadContext context, boolean partial) throws IOException {
        return leave(context, RubyHash.unmarshalFrom(this, false), partial);
    }

    private IRubyObject objectForArray(ThreadContext context, boolean partial) throws IOException {
        return leave(context, RubyArray.unmarshalFrom(this), partial);
    }

    private IRubyObject objectForRegexp(ThreadContext context, MarshalState state, boolean partial) throws IOException {
        return leave(context, entry(unmarshalRegexp(state)), partial);
    }

    private IRubyObject objectForString(ThreadContext context, boolean partial) throws IOException {
        return leave(context, entry(RubyString.unmarshalFrom(this)), partial);
    }

    private IRubyObject objectForBignum(ThreadContext context) throws IOException {
        return leave(context, entry(RubyBignum.unmarshalFrom(this)), false);
    }

    private IRubyObject objectForFloat(ThreadContext context) throws IOException {
        return leave(context, entry(RubyFloat.unmarshalFrom(this)), false);
    }

    private IRubyObject objectForFixnum(ThreadContext context) throws IOException {
        return leave(context, RubyFixnum.unmarshalFrom(this), false);
    }

    private IRubyObject objectForFalse(ThreadContext context) {
        return leave(context, context.fals, false);
    }

    private IRubyObject objectForTrue(ThreadContext context) {
        return leave(context, context.tru, false);
    }

    private IRubyObject objectForNil(ThreadContext context) {
        return leave(context, context.nil, false);
    }

    private IRubyObject objectForUClass(ThreadContext context, boolean partial, List<RubyModule> extendedModules) throws IOException {
        RubyClass c = getClassFromPath(runtime, unique().asJavaString());

        if (c.isSingleton()) throw typeError(context, "singleton can't be loaded");

        int type = r_byte();
        if (c == hashClass(context) && (type == TYPE_HASH || type == TYPE_HASH_DEF)) {
            // FIXME: Missing logic to make the following methods use compare_by_identity (and construction of that)
            return type == TYPE_HASH ? objectForHash(context, partial) : objectForHashDefault(context, partial);
        }

        IRubyObject obj = objectFor(context, type, null, partial, extendedModules);
        // if result is a module or type doesn't extend result's class...
        if (obj.getMetaClass() == moduleClass(context) || !c.isKindOfModule(obj.getMetaClass())) {
            // if allocators do not match, error
            // Note: MRI is a bit different here, and tests TYPE(type.allocate()) != TYPE(result)
            if (c.getAllocator() != obj.getMetaClass().getRealClass().getAllocator()) {
                throw argumentError(context, "dump format error (user class)");
            }
        }

        ((RubyObject) obj).setMetaClass(c);
        return obj;
    }

    private IRubyObject objectForExtended(ThreadContext context, List<RubyModule> extendedModules) throws IOException {
        IRubyObject path = unique();
        IRubyObject m = getModuleFromPath(runtime, path.asJavaString());

        if (extendedModules == null) extendedModules = new ArrayList<>();

        IRubyObject obj;
        if (m instanceof RubyClass) {
            obj = object0(context, null, true, null);
            RubyClass cls = obj.getMetaClass();
            if (cls != m || cls.isSingleton()) {
                throw argumentError(context, str(runtime, "prepended class ", path, " differs from class ", cls));
            }

            cls = obj.singletonClass(context);

            for (RubyModule mod: extendedModules) {
                cls.prependModule(context, mod);
            }
        } else {
            extendedModules.add(mustBeModule(context, m, path));

            obj = object0(context, null, true, extendedModules);

            appendExtendedModules(context, obj, extendedModules);
        }

        return obj;
    }

    private IRubyObject objectForIVar(ThreadContext context, MarshalState state, boolean partial,
                                      List<RubyModule> extendedModules) throws IOException {
        MarshalState state1 = new MarshalState(true);
        IRubyObject obj = object0(context, state1, true, extendedModules);

        if (state1.ivarWaiting) ivar(state, obj, null);

        return leave(context, obj, partial);
    }

    private IRubyObject objectForLink() throws IOException {
        IRubyObject obj = cache.readDataLink(this);

        if (!cache.isPartialObject(obj)) obj = postProc(obj);

        return obj;
    }

    private IRubyObject postProc(IRubyObject value) {
        return doCallProcForObj(value);
    }

    public RubySymbol symbol() throws IOException {
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
                    if (ivar) throw argumentError(runtime.getCurrentContext(), "dump format error (symlink with encoding)");
                    return symlink();
                default:
                    throw argumentError(runtime.getCurrentContext(), "dump format error for symbol(0x" + Integer.toHexString(type) + ")");
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
        final IOException exception[] = new IOException[] { null };

        RubySymbol symbol = RubySymbol.newSymbol(runtime, byteList,
                (sym, newSym) -> {
                    try {
                        Encoding encoding = null;
                        cache.registerSymbolLink(sym);

                        if (state != null && state.isIvarWaiting()) {
                            int num = input.unmarshalInt();
                            for (int i = 0; i < num; i++) {
                                RubySymbol sym2 = input.symbol();
                                encoding = symbolToEncoding(sym2, input.unmarshalObject());
                            }
                        }

                        if (encoding != null) {
                            sym.getBytes().setEncoding(encoding);

                            if (StringSupport.codeRangeScan(encoding, sym.getBytes()) == StringSupport.CR_BROKEN) {
                                throw argumentError(runtime.getCurrentContext(), str(runtime, "invalid byte sequence in " + encoding + ": ",
                                        inspectIdentifierByteList(runtime, sym.getBytes())));
                            }
                        }
                    } catch (IOException e) {
                        exception[0] = e;
                    }
                });

        if (exception[0] != null) throw exception[0];

        return symbol;
    }

    public RubySymbol unique() throws IOException {
        return symbol();
    }

    private IRubyObject unmarshalRegexp(MarshalState state) throws IOException {
        ByteList byteList = unmarshalString();
        byte opts = readSignedByte();
        RegexpOptions reOpts = RegexpOptions.fromJoniOptions(opts);

        RubyRegexp regexp = (RubyRegexp) runtime.getRegexp().allocate(runtime.getCurrentContext());

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

        regexp.regexpInitialize(byteList, byteList.getEncoding(), reOpts, null);

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
            if (read == -1) throw argumentError(runtime.getCurrentContext(), "marshal data too short");

            readLength += read;
        }

        return new ByteList(buffer,false);
    }

    public int unmarshalInt() throws IOException {
        int c = readSignedByte();
        if (c == 0) {
            return 0;
        } else if (4 < c) {
            return c - 5;
        } else if (c < -4) {
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

    // MRI: sym2encidx
    public Encoding symbolToEncoding(RubySymbol symbol, IRubyObject value) {
        if (symbol.getEncoding() != USASCIIEncoding.INSTANCE) return null;

        String id = symbol.idString();

        if (id.equals("encoding")) {
            String encodingNameStr = value.asJavaString();
            ByteList encodingName = new ByteList(ByteList.plain(encodingNameStr));

            Entry entry = runtime.getEncodingService().findEncodingOrAliasEntry(encodingName);
            if (entry == null) throw argumentError(runtime.getCurrentContext(), str(runtime, "encoding ", value, " is not registered"));

            return entry.getEncoding();
        } else if (id.equals(SYMBOL_ENCODING_SPECIAL)) {
            return value.isTrue() ? UTF8Encoding.INSTANCE : USASCIIEncoding.INSTANCE;
        }

        return null;
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
    private IRubyObject objectForUsrMarshal(ThreadContext context, MarshalState state, boolean partial,
                                            List<RubyModule> extendedModules) throws IOException {
        RubyClass classInstance = getClassFromPath(context.runtime, unique().asJavaString());
        IRubyObject obj = classInstance.allocate(context);

        if (extendedModules != null) appendExtendedModules(context, obj, extendedModules);

        obj = entry(obj);
        IRubyObject marshaled = unmarshalObject();
        obj = classInstance.smartLoadNewUser(obj, marshaled);
        obj = fixupCompat(obj);
        //obj = copyIVars(obj, marshaled);
        obj = postProc(obj);

        if (extendedModules != null) extendedModules.clear();

        return obj;
    }

    private IRubyObject fixupCompat(IRubyObject obj) {

        /* FIXME: we need to store allocators and then potentially use the ones associated with compat class values.
        IRubyObject compatObj = shared.hasCompatValue(obj);
        if (compatObj != null) {

        }*/
        return obj;
    }

    private void appendExtendedModules(ThreadContext context, IRubyObject obj, List<RubyModule> extendedModules) {
        Collections.reverse(extendedModules);
        for (RubyModule module: extendedModules) {
            module.extend_object(context, obj);
        }
    }

    public int read() throws IOException {
        return inputStream.read();
    }

    @Deprecated
    public void defaultVariablesUnmarshal(MarshalState state, IRubyObject object) throws IOException {
        ivar(state, object, null);
    }

    // r_object
    @Deprecated
    public IRubyObject unmarshalObject(boolean _callProc) throws IOException { // <-- 100% false by all callers
        IRubyObject savedProc = this.proc;
        try {
            proc = null;
            return object0(runtime.getCurrentContext(), new MarshalState(false), false, null);
        } finally {
            proc = savedProc;
        }
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
