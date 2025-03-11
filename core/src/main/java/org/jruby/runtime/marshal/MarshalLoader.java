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
import org.jruby.api.Create;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.io.RubyInputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Access.moduleClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.eofError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.marshal.MarshalCommon.*;
import static org.jruby.util.RubyStringBuilder.inspectIdentifierByteList;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * Unmarshals objects from strings or streams in Ruby's marshal format.
 */
public class MarshalLoader {
    private final List<IRubyObject> links = new ArrayList<>();
    private final List<RubySymbol> symbols = new ArrayList<>();
    private final Map<IRubyObject, IRubyObject> partials = new IdentityHashMap<>();
    private final IRubyObject proc;
    private final boolean freeze;

    public MarshalLoader(ThreadContext context, IRubyObject proc) {
        this(context, false, proc);
    }

    public MarshalLoader(ThreadContext context, boolean freeze, IRubyObject proc) {
        // Older native java ext expects proc can be null (spymem.jruby at least).
        if (proc == null) proc = context.nil;
        
        this.proc = proc;
        this.freeze = freeze;
    }

    public void start(ThreadContext context, RubyInputStream in) {
        int major = in.read(); // Major
        int minor = in.read(); // Minor

        if(major == -1 || minor == -1) {
            throw eofError(context, "marshal data too short");
        }

        if(major != Constants.MARSHAL_MAJOR || minor > Constants.MARSHAL_MINOR) {
            throw typeError(context, String.format("incompatible marshal file format (can't be read)\n\tformat version %d.%d required; %d.%d given", Constants.MARSHAL_MAJOR, Constants.MARSHAL_MINOR, major, minor));
        }
    }

    // r_object
    public IRubyObject unmarshalObject(ThreadContext context, RubyInputStream in) {
        return object0(context, in, new MarshalState(false), false, null);
    }

    // introduced for keeping ivar read state recursively.
    private static class MarshalState {
        private boolean ivarWaiting;

        MarshalState(boolean ivarWaiting) {
            this.ivarWaiting = ivarWaiting;
        }

        boolean isIvarWaiting() {
            return ivarWaiting;
        }

        void setIvarWaiting(boolean ivarWaiting) {
            this.ivarWaiting = ivarWaiting;
        }
    }

    public static RubyModule getModuleFromPath(ThreadContext context, String path) {
        Ruby runtime = context.runtime;
        final RubyModule value = runtime.getClassFromPath(path, runtime.getArgumentError(), false);
        if (value == null) throw argumentError(context, "undefined class/module " + path);
        if ( ! value.isModule() ) throw argumentError(context, path + " does not refer module");
        return value;
    }

    public static RubyClass getClassFromPath(ThreadContext context, String path) {
        Ruby runtime = context.runtime;
        final RubyModule value = runtime.getClassFromPath(path, runtime.getArgumentError(), false);
        if (value == null) throw argumentError(context, "undefined class/module " + path);
        if ( ! value.isClass() ) throw argumentError(context, path + " does not refer class");
        return (RubyClass) value;
    }

    private IRubyObject doCallProcForObj(ThreadContext context, IRubyObject result) {
        if (proc == null || proc.isNil()) return result;

        return Helpers.invoke(context, proc, "call", result);
    }

    private int r_byte(ThreadContext context, RubyInputStream in) {
        return readUnsignedByte(context, in);
    }

    public void ivar(ThreadContext context, RubyInputStream in, MarshalState state, IRubyObject object, boolean[] hasEncoding) {
        int count = unmarshalInt(context, in);
        RubyClass clazz = object.getMetaClass().getRealClass();

        // Keys may be "E", "encoding",or any valid ivar.  Weird code what happens with @encoding that happens to have
        // a string of "US-ASCII"?
        for (int i = 0; i < count; i++) {
            RubySymbol key = symbol(context, in);
            String id = key.idString();
            IRubyObject value = object0(context, in, state, false, null);
            Encoding encoding = symbolToEncoding(context, key, value);

            if (encoding != null) {
                if (object instanceof EncodingCapable) {
                    ((EncodingCapable) object).setEncoding(encoding);
                } else {
                    throw argumentError(context, str(context.runtime, object, "is not enc_capable"));
                }
                if (hasEncoding != null) hasEncoding[0] = true;
            } else if (id.equals(RUBY2_KEYWORDS_FLAG)) {
                if (object instanceof RubyHash) {
                    ((RubyHash) object).setRuby2KeywordHash(true);
                } else {
                    throw argumentError(context, str(context.runtime, "ruby2_keywords flag is given but ", object, " is not a Hash"));
                }
            } else {
                clazz.getVariableAccessorForWrite(key.idString()).set(object, value);
            }
        }
    }

    public IRubyObject entry(IRubyObject value) {
        registerDataLink(value);
        markAsPartialObject(value);

        return value;
    }

    private IRubyObject leave(ThreadContext context, IRubyObject value, boolean partial) {
        if (!partial) {
            noLongerPartial(value);
            if (freeze) {
                RubyClass metaClass = value.getMetaClass();
                if (metaClass == stringClass(context)) {
                    IRubyObject original = value;
                    // FIXME: We need to modify original to be frozen but we also need it to be part of the deduped table.
                    value = context.runtime.freezeAndDedupString((RubyString) value);
                    if (value != original) {
                        original.setFrozen(value.isFrozen());
                    }
                } else if (!value.isModule() && !value.isClass()) {
                    value.setFrozen(true);
                }
            }
            value = postProc(context, value);
        }

        return value;
    }

    private RubyModule mustBeModule(ThreadContext context, IRubyObject value, IRubyObject path) {
        if (!value.isModule()) throw argumentError(context, str(context.runtime, path, " does not refer to module"));

        return (RubyModule) value;
    }

    private IRubyObject object0(ThreadContext context, RubyInputStream in, MarshalState state, boolean partial, List<RubyModule> extendedModules) {
        return objectFor(context, in, r_byte(context, in), state, partial,  extendedModules);
    }

    private IRubyObject objectFor(ThreadContext context, RubyInputStream in, int type, MarshalState state, boolean partial,
                                  List<RubyModule> extendedModules) {
        return switch (type) {
            case TYPE_LINK -> objectForLink(context, in);
            case TYPE_IVAR -> objectForIVar(context, in, state, partial, extendedModules);
            case TYPE_EXTENDED -> objectForExtended(context, in, extendedModules);
            case TYPE_UCLASS -> objectForUClass(context, in, partial, extendedModules);
            case TYPE_NIL -> objectForNil(context);
            case TYPE_TRUE -> objectForTrue(context);
            case TYPE_FALSE -> objectForFalse(context);
            case TYPE_FIXNUM -> objectForFixnum(context, in);
            case TYPE_FLOAT -> objectForFloat(context, in);
            case TYPE_BIGNUM -> objectForBignum(context, in);
            case TYPE_STRING -> objectForString(context, in, partial);
            case TYPE_REGEXP -> objectForRegexp(context, in, state, partial);
            case TYPE_ARRAY -> objectForArray(context, in, partial);
            case TYPE_HASH -> objectForHash(context, in, partial);
            case TYPE_HASH_DEF -> objectForHashDefault(context, in, partial);
            case TYPE_STRUCT -> objectForStruct(context, in, partial);
            case TYPE_USERDEF -> objectForUserDef(context, in, state, partial);
            case TYPE_USRMARSHAL -> objectForUsrMarshal(context, in, extendedModules);
            case TYPE_OBJECT -> objectForObject(context, in, partial);
            case TYPE_DATA -> objectForData(context, in, state, partial, extendedModules);
            case TYPE_MODULE_OLD -> objectForModuleOld(context, in, state, partial);
            case TYPE_CLASS -> objectForClass(context, in, partial);
            case TYPE_MODULE -> objectForModule(context, in, partial);
            case TYPE_SYMBOL -> objectForSymbol(context, in, state, partial);
            case TYPE_SYMLINK -> objectForSymlink(context, in);
            default -> throw argumentError(context, "dump format error(" + (char) type + ")");
        };
    }

    private IRubyObject objectForModuleOld(ThreadContext context, RubyInputStream in, MarshalState state, boolean partial) {
        String name = RubyString.byteListToString(unmarshalString(context, in));
        RubyModule mod = MarshalLoader.getModuleFromPath(context, name);

        prohibitIVar(context, state, name);

        return leave(context, entry(mod), partial);
    }

    private void prohibitIVar(ThreadContext context, MarshalState state, String name) {
        if (state != null && state.isIvarWaiting()) {
            throw typeError(context, "can't override instance variable of class/module '" + name + "'");
        }
    }

    private RubySymbol objectForSymlink(ThreadContext context, RubyInputStream in) {
        return symlink(context, in);
    }

    private IRubyObject objectForSymbol(ThreadContext context, RubyInputStream in, MarshalState state, boolean partial) {
        IRubyObject obj = symreal(context, in, state);

        if (state != null && state.isIvarWaiting()) state.setIvarWaiting(false);

        return leave(context, obj, partial);
    }

    private IRubyObject objectForModule(ThreadContext context, RubyInputStream in, boolean partial) {
        return leave(context, entry(RubyModule.unmarshalFrom(context, in, this)), partial);
    }

    private IRubyObject objectForClass(ThreadContext context, RubyInputStream in, boolean partial) {
        return leave(context, entry(RubyClass.unmarshalFrom(context, in, this)), partial);
    }

    private IRubyObject objectForData(ThreadContext context, RubyInputStream in, MarshalState state, boolean partial,
                                      List<RubyModule> extendedModules) {
        IRubyObject name = unique(context, in);
        RubyClass klass = getClassFromPath(context, name.asJavaString());
        IRubyObject obj = entry(klass.allocate(context));
        // FIXME: Missing T_DATA error check?

        if (!obj.respondsTo("_load_data")) {
            throw typeError(context, str(context.runtime, name, " needs to have instance method _load_data"));
        }

        IRubyObject arg = object0(context, in, state, partial, extendedModules);
        obj.callMethod(context, "_load_data", arg);
        return leave(context, obj, partial);
    }

    private IRubyObject objectForObject(ThreadContext context, RubyInputStream in, boolean partial) {
        RubySymbol className = symbol(context, in);
        RubyClass type = getClassFromPath(context, className.idString());

        IRubyObject obj = (IRubyObject) type.unmarshal(context, in, this);
        return leave(context, obj, partial);
    }

    private IRubyObject objectForUserDef(ThreadContext context, RubyInputStream in, MarshalState state, boolean partial) {
        IRubyObject obj = userUnmarshal(context, in, state);

        if (!partial) obj = postProc(context, obj);

        return obj;
    }

    private IRubyObject objectForStruct(ThreadContext context, RubyInputStream in, boolean partial) {
        return leave(context, RubyStruct.unmarshalFrom(context, in, this), partial);
    }

    private IRubyObject objectForHashDefault(ThreadContext context, RubyInputStream in, boolean partial) {
        return leave(context, RubyHash.unmarshalFrom(context, in, this, true), partial);
    }

    private IRubyObject objectForHash(ThreadContext context, RubyInputStream in, boolean partial) {
        return leave(context, RubyHash.unmarshalFrom(context, in, this, false), partial);
    }

    private IRubyObject objectForArray(ThreadContext context, RubyInputStream in, boolean partial) {
        return leave(context, RubyArray.unmarshalFrom(context, in, this), partial);
    }

    private IRubyObject objectForRegexp(ThreadContext context, RubyInputStream in, MarshalState state, boolean partial) {
        return leave(context, entry(unmarshalRegexp(context, in, state)), partial);
    }

    private IRubyObject objectForString(ThreadContext context, RubyInputStream in, boolean partial) {
        return leave(context, entry(RubyString.unmarshalFrom(context, in, this)), partial);
    }

    private IRubyObject objectForBignum(ThreadContext context, RubyInputStream in) {
        return leave(context, entry(RubyBignum.unmarshalFrom(context, in, this)), false);
    }

    private IRubyObject objectForFloat(ThreadContext context, RubyInputStream in) {
        return leave(context, entry(RubyFloat.unmarshalFrom(context, in, this)), false);
    }

    private IRubyObject objectForFixnum(ThreadContext context, RubyInputStream in) {
        return leave(context, RubyFixnum.unmarshalFrom(context, in, this), false);
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

    private IRubyObject objectForUClass(ThreadContext context, RubyInputStream in, boolean partial, List<RubyModule> extendedModules) {
        RubyClass c = getClassFromPath(context, unique(context, in).asJavaString());

        if (c.isSingleton()) throw typeError(context, "singleton can't be loaded");

        int type = r_byte(context, in);
        if (c == hashClass(context) && (type == TYPE_HASH || type == TYPE_HASH_DEF)) {
            // FIXME: Missing logic to make the following methods use compare_by_identity (and construction of that)
            return type == TYPE_HASH ? objectForHash(context, in, partial) : objectForHashDefault(context, in, partial);
        }

        IRubyObject obj = objectFor(context, in, type, null, partial, extendedModules);
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

    private IRubyObject objectForExtended(ThreadContext context, RubyInputStream in, List<RubyModule> extendedModules) {
        IRubyObject path = unique(context, in);
        IRubyObject m = getModuleFromPath(context, path.asJavaString());

        if (extendedModules == null) extendedModules = new ArrayList<>();

        IRubyObject obj;
        if (m instanceof RubyClass) {
            obj = object0(context, in, null, true, null);
            RubyClass cls = obj.getMetaClass();
            if (cls != m || cls.isSingleton()) {
                throw argumentError(context, str(context.runtime, "prepended class ", path, " differs from class ", cls));
            }

            cls = obj.singletonClass(context);

            for (RubyModule mod: extendedModules) {
                cls.prependModule(context, mod);
            }
        } else {
            extendedModules.add(mustBeModule(context, m, path));

            obj = object0(context, in, null, true, extendedModules);

            appendExtendedModules(context, obj, extendedModules);
        }

        return obj;
    }

    private IRubyObject objectForIVar(ThreadContext context, RubyInputStream in, MarshalState state, boolean partial,
                                      List<RubyModule> extendedModules) {
        MarshalState state1 = new MarshalState(true);
        IRubyObject obj = object0(context, in, state1, true, extendedModules);

        if (state1.ivarWaiting) ivar(context, in, state, obj, null);

        return leave(context, obj, partial);
    }

    private IRubyObject objectForLink(ThreadContext context, RubyInputStream in) {
        IRubyObject obj = readDataLink(context, in, this);

        if (!isPartialObject(obj)) obj = postProc(context, obj);

        return obj;
    }

    private IRubyObject postProc(ThreadContext context, IRubyObject value) {
        return doCallProcForObj(context, value);
    }

    public RubySymbol symbol(ThreadContext context, RubyInputStream in) {
        boolean ivar = false;

        while (true) {
            int type = r_byte(context, in);

            switch (type) {
                case TYPE_IVAR:
                    ivar = true;
                    continue;
                case TYPE_SYMBOL: {
                    MarshalState state1 = new MarshalState(ivar);
                    return symreal(context, in, state1);
                }
                case TYPE_SYMLINK:
                    if (ivar) throw argumentError(context, "dump format error (symlink with encoding)");
                    return symlink(context, in);
                default:
                    throw argumentError(context, "dump format error for symbol(0x" + Integer.toHexString(type) + ")");
            }
        }
    }

    private RubySymbol symlink(ThreadContext context, RubyInputStream in) {
        return (RubySymbol) readSymbolLink(context, in, this);
    }

    private RubySymbol symreal(ThreadContext context, RubyInputStream in, MarshalState state) {
        ByteList byteList = unmarshalString(context, in);
        // FIXME: needs to only do this is only us-ascii
        byteList.setEncoding(ASCIIEncoding.INSTANCE);
        final MarshalLoader input = this;

        return RubySymbol.newSymbol(context.runtime, byteList,
                (sym, newSym) -> {
                    Encoding encoding = null;
                    registerSymbolLink(sym);

                    if (state != null && state.isIvarWaiting()) {
                        int num = input.unmarshalInt(context, in);
                        for (int i = 0; i < num; i++) {
                            RubySymbol sym2 = input.symbol(context, in);
                            encoding = symbolToEncoding(context, sym2, input.unmarshalObject(context, in));
                        }
                    }

                    if (encoding != null) {
                        sym.getBytes().setEncoding(encoding);

                        if (StringSupport.codeRangeScan(encoding, sym.getBytes()) == StringSupport.CR_BROKEN) {
                            throw argumentError(context, str(context.runtime, "invalid byte sequence in " + encoding + ": ",
                                    inspectIdentifierByteList(context.runtime, sym.getBytes())));
                        }
                    }
                });
    }

    public RubySymbol unique(ThreadContext context, RubyInputStream in) {
        return symbol(context, in);
    }

    private IRubyObject unmarshalRegexp(ThreadContext context, RubyInputStream in, MarshalState state) {
        ByteList byteList = unmarshalString(context, in);
        byte opts = readSignedByte(context, in);
        RegexpOptions reOpts = RegexpOptions.fromJoniOptions(opts);

        RubyRegexp regexp = (RubyRegexp) context.runtime.getRegexp().allocate(context);

        IRubyObject ivarHolder = null;
        boolean[] hasEncoding = new boolean[] { false };

        if (state != null && state.isIvarWaiting()) {
            RubyString tmpStr = RubyString.newString(context.runtime, byteList);

            ivar(context, in, state, tmpStr, hasEncoding);
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

    public int readUnsignedByte(ThreadContext context, RubyInputStream in) {
        int result = in.read();
        if (result == -1) throw eofError(context, "marshal data too short");
        return result;
    }

    public byte readSignedByte(ThreadContext context, RubyInputStream in) {
        int b = readUnsignedByte(context, in);
        if (b > 127) {
            return (byte) (b - 256);
        }
        return (byte) b;
    }

    public ByteList unmarshalString(ThreadContext context, RubyInputStream in) {
        int length = unmarshalInt(context, in);
        byte[] buffer = new byte[length];

        int readLength = 0;
        while (readLength < length) {
            int read = in.read(buffer, readLength, length - readLength);
            if (read == -1) throw argumentError(context, "marshal data too short");

            readLength += read;
        }

        return new ByteList(buffer,false);
    }

    public int unmarshalInt(ThreadContext context, RubyInputStream in) {
        int c = readSignedByte(context, in);
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
                result |= (long) readUnsignedByte(context, in) << (8 * i);
            }
        } else {
            c = -c;
            result = -1;
            for (int i = 0; i < c; i++) {
                result &= ~((long) 0xff << (8 * i));
                result |= (long) readUnsignedByte(context, in) << (8 * i);
            }
        }
        return (int) result;
    }

    // MRI: sym2encidx
    private Encoding symbolToEncoding(ThreadContext context, RubySymbol symbol, IRubyObject value) {
        if (symbol.getEncoding() != USASCIIEncoding.INSTANCE) return null;

        String id = symbol.idString();

        if (id.equals("encoding")) {
            String encodingNameStr = value.asJavaString();
            ByteList encodingName = new ByteList(ByteList.plain(encodingNameStr));

            Entry entry = context.runtime.getEncodingService().findEncodingOrAliasEntry(encodingName);
            if (entry == null) throw argumentError(context, str(context.runtime, "encoding ", value, " is not registered"));

            return entry.getEncoding();
        } else if (id.equals(SYMBOL_ENCODING_SPECIAL)) {
            return value.isTrue() ? UTF8Encoding.INSTANCE : USASCIIEncoding.INSTANCE;
        }

        return null;
    }

    private IRubyObject userUnmarshal(ThreadContext context, RubyInputStream in, MarshalState state) {
        String className = unique(context, in).asJavaString();
        RubyClass classInstance = getClassFromPath(context, className);
        RubyString data = Create.newString(context, unmarshalString(context, in));
        IRubyObject unmarshaled;

        // Special case Encoding so they are singletons
        // See https://bugs.ruby-lang.org/issues/11760
        if (classInstance == context.runtime.getEncoding()) {
            unmarshaled = RubyEncoding.find(context, classInstance, data);
        } else {
            if (state != null && state.isIvarWaiting()) {
                ivar(context, in, state, data, null);
                state.setIvarWaiting(false);
            }
            unmarshaled = classInstance.smartLoadOldUser(data);
        }

        return entry(unmarshaled);
    }

    // FIXME: This is missing a much more complicated set of logic in tracking old compatibility allocators. See MRI for more details
    private IRubyObject objectForUsrMarshal(ThreadContext context, RubyInputStream in, List<RubyModule> extendedModules) {
        RubyClass classInstance = getClassFromPath(context, unique(context, in).asJavaString());
        IRubyObject obj = classInstance.allocate(context);

        if (extendedModules != null) appendExtendedModules(context, obj, extendedModules);

        obj = entry(obj);
        IRubyObject marshaled = unmarshalObject(context, in);
        obj = classInstance.smartLoadNewUser(obj, marshaled);
        obj = fixupCompat(obj);
        //obj = copyIVars(obj, marshaled);
        obj = postProc(context, obj);

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

    private boolean isPartialObject(IRubyObject value) {
        return partials.containsKey(value);
    }

    private void markAsPartialObject(IRubyObject value) {
        partials.put(value, value);
    }

    private void noLongerPartial(IRubyObject value) {
        partials.remove(value);
    }

    private IRubyObject readSymbolLink(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        try {
            return symbols.get(input.unmarshalInt(context, in));
        } catch (IndexOutOfBoundsException e) {
            throw typeError(context,"bad symbol");
        }
    }

    private IRubyObject readDataLink(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        int index = input.unmarshalInt(context, in);
        try {
            return links.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw argumentError(context, "dump format error (unlinked, index: " + index + ")");
        }
    }

    private void registerDataLink(IRubyObject value) {
        links.add(value);
    }

    private void registerSymbolLink(RubySymbol value) {
        symbols.add(value);
    }
}
