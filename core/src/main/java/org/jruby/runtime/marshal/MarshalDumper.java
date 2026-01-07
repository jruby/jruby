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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
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

package org.jruby.runtime.marshal;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyData;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.ByteList;
import org.jruby.util.collections.HashMapInt;
import org.jruby.util.io.RubyOutputStream;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.jruby.RubyBasicObject.getMetaClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Error.*;
import static org.jruby.runtime.marshal.MarshalCommon.TYPE_IVAR;
import static org.jruby.runtime.marshal.MarshalCommon.TYPE_UCLASS;
import static org.jruby.runtime.marshal.MarshalCommon.TYPE_USERDEF;
import static org.jruby.runtime.marshal.MarshalCommon.TYPE_USRMARSHAL;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

/**
 * Marshals objects into Ruby's binary marshal format.
 */
public class MarshalDumper {
    private static final ByteList HASH_BYTELIST = new ByteList("Hash".getBytes(StandardCharsets.US_ASCII), false);
    private final int depthLimit;
    private int depth = 0;
    private final HashMapInt<IRubyObject> linkCache = new HashMapInt<>(4, true);
    // lazy for simple cases that encounter no symbols
    private HashMapInt<RubySymbol> symbolCache;

    public MarshalDumper(int depthLimit) {
        this.depthLimit = depthLimit >= 0 ? depthLimit : Integer.MAX_VALUE;
    }

    public void start(RubyOutputStream out) {
        out.write(Constants.MARSHAL_MAJOR);
        out.write(Constants.MARSHAL_MINOR);
    }

    public void dumpObject(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        depth++;
        
        if (depth > depthLimit) throw argumentError(context, "exceed depth limit");

        writeAndRegister(context, out, value);

        depth--;
    }

    public void registerLinkTarget(IRubyObject newObject) {
        if (shouldBeRegistered(newObject)) {
            register(newObject);
        }
    }

    private static boolean shouldBeRegistered(IRubyObject value) {
        return !(value.isNil() || value instanceof RubyBoolean || (value instanceof RubyFixnum fixnum && isMarshalFixnum(fixnum)));
    }

    private static boolean isMarshalFixnum(RubyFixnum fixnum) {
        var value = fixnum.getValue();
        return value <= RubyFixnum.MAX_MARSHAL_FIXNUM && value >= RubyFixnum.MIN_MARSHAL_FIXNUM;
    }

    private void writeAndRegisterSymbol(RubyOutputStream out, RubySymbol sym) {
        if (!getSymbolCache().ifPresent(out, sym, MarshalDumper::writeSymbolLink)) {
            registerSymbol(sym);
            dumpSymbol(out, sym.getBytes());
        }
    }

    private void writeAndRegister(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        if (value instanceof RubySymbol ||
                !linkCache.ifPresent(out, value, MarshalDumper::writeLink)) {
            getMetaClass(value).smartDump(context, out, this, value);
        }
    }

    IRubyObject currentValue;
    boolean doVariables;
    int variableCount;

    public void writeDirectly(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        ClassIndex nativeClassIndex;
        boolean shouldMarshalEncoding;
        boolean shouldMarshalRuby2Keywords;

        if (!(value instanceof CoreObjectType coreObjectType)
                || (nativeClassIndex = coreObjectType.getNativeClassIndex()) == ClassIndex.OBJECT
                || nativeClassIndex == ClassIndex.BASICOBJECT) {

            writeObjectData(context, out, value);

        } else {
            shouldMarshalEncoding = (value instanceof MarshalEncoding marshalEncoding && marshalEncoding.shouldMarshalEncoding());
            RubyHash keywordsHash = (value instanceof RubyHash hash && hash.isRuby2KeywordHash()) ? hash : null;
            shouldMarshalRuby2Keywords = keywordsHash != null;
            if (!shouldMarshalEncoding
                    && !shouldMarshalRuby2Keywords
                    && (value.isImmediate()
                    || !value.hasVariables()
                    || nativeClassIndex == ClassIndex.CLASS
                    || nativeClassIndex == ClassIndex.MODULE)) {

                dumpBaseObject(context, out, value, nativeClassIndex);

            } else {

                var ivarAccessors = checkVariables(value, shouldMarshalEncoding, shouldMarshalRuby2Keywords);
                int size = variableCount;
                clearVariableState();

                if (doVariables) {
                    // object has instance vars and isn't a class, get a snapshot to be marshalled
                    // and output the ivar header here

                    // write `I' instance var signet if class is NOT a direct subclass of Object
                    out.write(TYPE_IVAR);
                    dumpBaseObject(context, out, value, nativeClassIndex);

                    if (shouldMarshalEncoding) {
                        writeInt(out, size + 1); // vars preceded by encoding
                        writeEncoding(context, out, ((MarshalEncoding) value).getMarshalEncoding());
                    } else if (shouldMarshalRuby2Keywords) {
                        writeInt(out, size + 1); // vars preceded by ruby2_keywords_hash

                        var symbolTable = context.runtime.getSymbolTable();
                        writeAndRegisterSymbol(out, symbolTable.getRuby2KeywordsHashSymbolK());
                        writeObjectData(context, out, asBoolean(context, keywordsHash.isRuby2KeywordHash()));
                    } else {
                        writeInt(out, size);
                    }

                    ivarAccessors.forEach(new VariableDumper(context, out, value));
                } else {
                    // no variables, no encoding
                    dumpBaseObject(context, out, value, nativeClassIndex);
                }

            }
        }
    }

    private void clearVariableState() {
        currentValue = null;
    }

    private Map<String, VariableAccessor> checkVariables(IRubyObject value, boolean shouldMarshalEncoding, boolean shouldMarshalRuby2Keywords) {
        currentValue = value;
        doVariables = shouldMarshalEncoding | shouldMarshalRuby2Keywords;
        variableCount = 0;

        // check if any variables are set and collect size
        var ivarAccessors = getMetaClass(value).getVariableAccessorsForRead();
        ivarAccessors.forEach(this::checkVariablesForMarshal);
        return ivarAccessors;
    }

    private void dumpBaseObject(ThreadContext context, RubyOutputStream out, IRubyObject value, ClassIndex nativeClassIndex) {
        dumpType(context, out, value, nativeClassIndex);
        writeObjectData(context, out, value);
    }

    private void dumpType(ThreadContext context, RubyOutputStream out, IRubyObject value, ClassIndex nativeClassIndex) {
        final RubyClass meta = getMetaClass(value);
        RubyClass type = switch (nativeClassIndex) {
            case STRING, REGEXP, ARRAY, HASH -> dumpExtended(context, out, meta);
            default -> meta;
        };

        if (nativeClassIndex != meta.getClassIndex() &&
                nativeClassIndex != ClassIndex.STRUCT &&
                nativeClassIndex != ClassIndex.DATA &&
                nativeClassIndex != ClassIndex.FIXNUM &&
                nativeClassIndex != ClassIndex.BIGNUM) {
            // object is a custom class that extended one of the native types other than Object
            writeUserClass(context, out, type);
        }
    }

    public static RubySymbol getPathFromClass(ThreadContext context, RubyModule clazz) {
        Ruby runtime = context.runtime;
        RubySymbol pathSym = clazz.symbolName(context);

        if (pathSym == null) {
            String type = clazz.isClass() ? "class" : "module";
            throw typeError(context, str(runtime, "can't dump anonymous " + type + " ", types(runtime, clazz)));
        }

        String path = pathSym.idString();
        
        RubyModule real = clazz.isModule() ? clazz : ((RubyClass)clazz).getRealClass();

        // FIXME: This is weird why we do this.  rubyName should produce something which can be referred so what example
        // will this fail on?  If there is a failing case then passing asJavaString may be broken since it will not be
        // a properly encoded string.  If this is an issue we should make a clazz.IdPath where all segments are returned
        // by their id names.
        if (runtime.getClassFromPath(path) != real) {
            throw typeError(context, str(runtime, types(runtime, clazz), " can't be referred"));
        }
        return pathSym;
    }
    
    private void writeObjectData(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        // switch on the object's *native type*. This allows use-defined
        // classes that have extended core native types to piggyback on their
        // marshalling logic.
        if (value instanceof CoreObjectType) {
            if (value instanceof DataType) {
                Ruby runtime = context.runtime;

                throw typeError(context, str(runtime, "no _dump_data is defined for class ", types(runtime, getMetaClass(value))));
            }
            ClassIndex nativeClassIndex = ((CoreObjectType) value).getNativeClassIndex();
            Ruby runtime;

            switch (nativeClassIndex) {
                case ARRAY:
                    out.write('[');
                    RubyArray.marshalTo(context, out, (RubyArray<?>) value, this);
                    return;
                case FALSE:
                    out.write('F');
                    return;
                case FIXNUM:
                    RubyFixnum fixnum = (RubyFixnum) value;

                    if (isMarshalFixnum(fixnum)) {
                        out.write('i');
                        writeInt(out, fixnum.asInt(context));
                        return;
                    }
                    // FIXME: inefficient; constructing a bignum just for dumping?
                    value = RubyBignum.newBignum(context.runtime, fixnum.getValue());

                    // fall through
                case BIGNUM:
                    out.write('l');
                    RubyBignum.marshalTo(context, out, (RubyBignum) value, this);
                    return;
                case CLASS:
                    if (((RubyClass) value).isSingleton()) throw typeError(context, "singleton class can't be dumped");
                    out.write('c');
                    RubyClass.marshalTo(context, out, (RubyClass) value, this);
                    return;
                case FLOAT:
                    out.write('f');
                    RubyFloat.marshalTo(context, out, (RubyFloat) value, this);
                    return;
                case HASH: {
                    RubyHash hash = (RubyHash) value;

                    if (hash.isComparedByIdentity()) {
                        out.write(TYPE_UCLASS);
                        dumpSymbol(out, HASH_BYTELIST);
                    }
                    if (hash.getIfNone() == RubyBasicObject.UNDEF) {
                        out.write('{');
                    } else if (hash.hasDefaultProc()) {
                        throw typeError(context, "can't dump hash with default proc");
                    } else {
                        out.write('}');
                    }

                    RubyHash.marshalTo(context, out, hash, this);
                    return;
                }
                case MODULE:
                    out.write('m');
                    RubyModule.marshalTo(context, out, (RubyModule) value, this);
                    return;
                case NIL:
                    out.write('0');
                    return;
                case OBJECT:
                case BASICOBJECT:
                    final RubyClass type = getMetaClass(value);

                    /*
                     Data is supposed to look like Struct, but does not actually extend it. That prevents us from
                     overriding CodeDataType.getNativeClassIndex to return a different ClassIndex, so we have to check
                     for Data using kind_of.
                     */
                    if (type.isKindOfModule(context.runtime.getData())) {
                        RubyData.marshalTo(context, out, value, this);
                        return;
                    }

                    dumpDefaultObjectHeader(context, out, type);
                    type.getRealClass().marshal(context, out, value, this);
                    return;
                case REGEXP:
                    out.write('/');
                    RubyRegexp.marshalTo(context, (RubyRegexp) value, this, out);
                    return;
                case STRING:
                    registerLinkTarget(value);
                    out.write('"');
                    writeString(out, value.convertToString().getByteList());
                    return;
                case STRUCT:
                    RubyStruct.marshalTo(context, out, (RubyStruct) value, this);
                    return;
                case SYMBOL:
                    writeAndRegisterSymbol(out, ((RubySymbol) value));
                    return;
                case TRUE:
                    out.write('T');
                    return;
                default:
                    runtime = context.runtime;
                    throw typeError(context, str(runtime, "can't dump ", types(runtime, getMetaClass(value))));
            }
        } else {
            RubyClass metaClass = getMetaClass(value);
            dumpDefaultObjectHeader(context, out, metaClass);
            metaClass.getRealClass().marshal(context, out, value, this);
        }
    }

    public void userNewMarshal(ThreadContext context, RubyOutputStream out, IRubyObject value, CacheEntry entry) {
        userNewCommon(context, out, value, entry);
    }

    public void userNewMarshal(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        userNewCommon(context, out, value, null);
    }

    private void userNewCommon(ThreadContext context, RubyOutputStream out, IRubyObject value, CacheEntry entry) {
        registerLinkTarget(value);
        out.write(TYPE_USRMARSHAL);
        final RubyClass klass = getMetaClass(value);
        writeAndRegisterSymbol(out, asSymbol(context, klass.getRealClass().getName(context)));

        IRubyObject marshaled = entry != null ?
                entry.method.call(context, value, entry.sourceModule, "marshal_dump") :
                value.callMethod(context, "marshal_dump");

        if (getMetaClass(marshaled) == klass) throw runtimeError(context, "marshal_dump returned same class instance");

        dumpObject(context, out, marshaled);
    }

    public void userMarshal(ThreadContext context, RubyOutputStream out, IRubyObject value, CacheEntry entry) {
        userCommon(context, out, value, entry);
    }

    public void userMarshal(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        userCommon(context, out, value, null);
    }

    private void userCommon(ThreadContext context, RubyOutputStream out, IRubyObject value, CacheEntry cacheEntry) {
        RubyFixnum depthLimitFixnum = asFixnum(context, depthLimit);
        final RubyClass klass = getMetaClass(value);
        IRubyObject dumpResult;
        if (cacheEntry != null) {
            dumpResult = cacheEntry.method.call(context, value, cacheEntry.sourceModule, "_dump", depthLimitFixnum);
        } else {
            dumpResult = value.callMethod(context, "_dump", depthLimitFixnum);
        }
        
        RubyString marshaled = castAsString(context, dumpResult);

        if (marshaled.hasVariables()) {
            var ivarAccessors = countVariables(marshaled);
            int size = variableCount;
            clearVariableState();

            if (size > 0) {
                out.write(TYPE_IVAR);
                dumpUserdefBase(context, out, klass, marshaled);

                writeInt(out, size);

                ivarAccessors.forEach(new VariableDumper(context, out, marshaled));
            } else {
                dumpUserdefBase(context, out, klass, marshaled);
            }
        } else {
            dumpUserdefBase(context, out, klass, marshaled);
        }

        registerLinkTarget(value);
    }

    private void dumpUserdefBase(ThreadContext context, RubyOutputStream out, RubyClass klass, RubyString marshaled) {
        out.write(TYPE_USERDEF);
        writeAndRegisterSymbol(out, asSymbol(context, klass.getRealClass().getName(context)));
        writeString(out, marshaled.getByteList());
    }

    public void writeUserClass(ThreadContext context, RubyOutputStream out, RubyClass type) {
        out.write(TYPE_UCLASS);

        // w_unique
        if (type.getName(context).charAt(0) == '#') {
            Ruby runtime = context.runtime;
            throw typeError(context, str(runtime, "can't dump anonymous class ", types(runtime, type)));
        }
        
        // w_symbol
        writeAndRegisterSymbol(out, asSymbol(context, type.getName(context)));
    }

    public void dumpVariables(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        dumpVariables(context, out, value, 0);
    }

    public void dumpVariables(ThreadContext context, RubyOutputStream out, IRubyObject value, int extraSize) {
        Map<String, VariableAccessor> ivarAccessors = countVariables(value, extraSize);
        int size = variableCount;
        clearVariableState();

        writeInt(out, size);

        ivarAccessors.forEach(new VariableDumper(context, out, value));
    }

    private Map<String, VariableAccessor> countVariables(IRubyObject value) {
        return countVariables(value, 0);
    }

    private Map<String, VariableAccessor> countVariables(IRubyObject value, int extraSize) {
        currentValue = value;
        variableCount = extraSize;

        var ivarAccessors = getMetaClass(value).getVariableAccessorsForRead();

        ivarAccessors.forEach(this::countVariablesForMarshal);

        return ivarAccessors;
    }

    public interface VariableReceiver {
        void receive(MarshalDumper marshal, ThreadContext context, RubyOutputStream out, String name, IRubyObject value);
    }

    public interface VariableSupplier<T> {
        void forEach(MarshalDumper marshal, ThreadContext context, RubyOutputStream out, T value, VariableReceiver receiver);
    }

    public <T extends IRubyObject> void dumpVariables(ThreadContext context, RubyOutputStream out, T value, int extraSize, VariableSupplier<T> extra) {
        dumpVariables(context, out, value, extraSize);
        extra.forEach(this, context, out, value, (m, c, o, name, v) -> dumpVariable(c, o, m, name, v));
    }

    private static void dumpVariable(ThreadContext context, RubyOutputStream out, MarshalDumper marshal, String name, Object value) {
        if (value instanceof IRubyObject) {
            marshal.writeAndRegisterSymbol(out, asSymbol(context, name));
            marshal.dumpObject(context, out, (IRubyObject) value);
        }
    }

    public void writeEncoding(ThreadContext context, RubyOutputStream out, Encoding encoding) {
        var symbolTable = context.runtime.getSymbolTable();
        if (encoding == null || encoding == USASCIIEncoding.INSTANCE) {
            writeAndRegisterSymbol(out, symbolTable.getEncodingSymbolE());
            writeObjectData(context, out, context.fals);
        } else if (encoding == UTF8Encoding.INSTANCE) {
            writeAndRegisterSymbol(out, symbolTable.getEncodingSymbolE());
            writeObjectData(context, out, context.tru);
        } else {
            writeAndRegisterSymbol(out, symbolTable.getEncodingSymbol());
            RubyString encodingString = new RubyString(context.runtime, stringClass(context), encoding.getName());
            writeObjectData(context, out, encodingString);
        }
    }
    
    private boolean hasSingletonMethods(RubyClass type) {
        for(DynamicMethod method : type.getMethods().values()) {
            // We do not want to capture cached methods
            if(method.isImplementedBy(type)) {
                return true;
            }
        }
        return false;
    }

    /** w_extended
     * 
     */
    private RubyClass dumpExtended(ThreadContext context, RubyOutputStream out, RubyClass type) {
        if (type.isSingleton()) {
            // any ivars, since we don't have __attached__ ivar now
            if (hasSingletonMethods(type) || type.hasVariables()) throw typeError(context, "singleton can't be dumped");
            type = type.getSuperClass();
        }
        while(type.isIncluded()) {
            out.write('e');
            writeAndRegisterSymbol(out, asSymbol(context, type.getOrigin().getName(context)));
            type = type.getSuperClass();
        }
        return type;
    }

    public void dumpDefaultObjectHeader(ThreadContext context, RubyOutputStream out, RubyClass type) {
        dumpDefaultObjectHeader(context, out, 'o',type);
    }

    public void dumpDefaultObjectHeader(ThreadContext context, RubyOutputStream out, char tp, RubyClass type) {
        dumpExtended(context, out, type);
        out.write(tp);
        writeAndRegisterSymbol(out, getPathFromClass(context, type.getRealClass()));
    }

    public void writeString(RubyOutputStream out, String value) {
        writeInt(out, value.length());
        // FIXME: should preserve unicode?
        out.write(RubyString.stringToBytes(value));
    }

    public void writeString(RubyOutputStream out, ByteList value) {
        int len = value.length();
        writeInt(out, len);
        out.write(value.getUnsafeBytes(), value.begin(), len);
    }

    public void dumpSymbol(RubyOutputStream out, ByteList value) {
        out.write(':');
        int len = value.length();
        writeInt(out, len);
        out.write(value.getUnsafeBytes(), value.begin(), len);
    }

    public void writeInt(RubyOutputStream out, int value) {
        out.writeMarshalInt(value);
    }

    public void writeByte(RubyOutputStream out, int value) {
        out.write(value);
    }

    private void checkVariablesForMarshal(String name, VariableAccessor accessor) {
        Object varValue = accessor.get(currentValue);
        if (!(varValue instanceof Serializable)) return;
        doVariables = true;
        variableCount++;
    }

    private void countVariablesForMarshal(String name, VariableAccessor accessor) {
        Object varValue = accessor.get(currentValue);
        if (!(varValue instanceof Serializable)) return;
        variableCount++;
    }

    private HashMapInt<RubySymbol> getSymbolCache() {
        HashMapInt<RubySymbol> symbolCache = this.symbolCache;
        if (symbolCache == null) {
            this.symbolCache = symbolCache = new HashMapInt<>(true);
        }
        return symbolCache;
    }

    private void register(IRubyObject value) {
        assert !(value instanceof RubySymbol) : "Use registeredSymbolIndex for symbols";

        linkCache.put(value, linkCache.size());
    }

    private void registerSymbol(RubySymbol sym) {
        getSymbolCache().put(sym, getSymbolCache().size());
    }

    private static void writeLink(RubyOutputStream out, int link) {
        out.write('@');
        out.writeMarshalInt(link);
    }

    private static void writeSymbolLink(RubyOutputStream out, int link) {
        out.write(';');
        out.writeMarshalInt(link);
    }

    private class VariableDumper implements BiConsumer<String, VariableAccessor> {
        private final ThreadContext context;
        private final RubyOutputStream out;
        private final IRubyObject value;

        public VariableDumper(ThreadContext context, RubyOutputStream out, IRubyObject value) {
            this.context = context;
            this.out = out;
            this.value = value;
        }

        @Override
        public void accept(String name, VariableAccessor accessor) {
            Object varValue = accessor.get(value);
            if (!(varValue instanceof Serializable)) return;
            dumpVariable(context, out, MarshalDumper.this, name, varValue);
        }
    }

}
