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
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.api.Convert;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.ByteList;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
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
 *
 * @author Anders
 */
public class NewMarshal {
    private final NewMarshalCache cache;
    private final int depthLimit;
    private int depth = 0;

    public NewMarshal(int depthLimit) {
        this.cache = new NewMarshalCache();
        this.depthLimit = depthLimit >= 0 ? depthLimit : Integer.MAX_VALUE;
    }

    public static class RubyOutputStream extends OutputStream {
        private final OutputStream wrap;
        private final ThreadContext context;

        public RubyOutputStream(OutputStream wrap, ThreadContext context) {
            this.wrap = wrap;
            this.context = context;
        }

        public void write(int b) {
            try {
                wrap.write(b);
            } catch (IOException ioe) {
                handle(ioe);
            }
        }

        public void write(byte[] b, int off, int len) {
            try {
                wrap.write(b, off, len);
            } catch (IOException ioe) {
                handle(ioe);
            }
        }

        public void write(byte[] b) {
            try {
                wrap.write(b);
            } catch (IOException ioe) {
                handle(ioe);
            }
        }

        public void handle(IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
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
            cache.register(newObject);
        }
    }

    public void registerSymbol(RubySymbol sym) {
        cache.registerSymbol(sym);
    }

    static boolean shouldBeRegistered(IRubyObject value) {
        if (value.isNil()) {
            return false;
        } else if (value instanceof RubyBoolean) {
            return false;
        } else if (value instanceof RubyFixnum) {
            return ! isMarshalFixnum((RubyFixnum)value);
        }
        return true;
    }

    private static boolean isMarshalFixnum(RubyFixnum fixnum) {
        var value = fixnum.getValue();
        return value <= RubyFixnum.MAX_MARSHAL_FIXNUM && value >= RubyFixnum.MIN_MARSHAL_FIXNUM;
    }

    private void writeAndRegisterSymbol(RubyOutputStream out, RubySymbol sym) {
        if (cache.isSymbolRegistered(sym)) {
            cache.writeSymbolLink(this, out, sym);
        } else {
            registerSymbol(sym);
            dumpSymbol(out, sym.getBytes());
        }
    }

    private void writeAndRegister(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        if (!(value instanceof RubySymbol) && cache.isRegistered(value)) {
            cache.writeLink(this, out, value);
        } else {
            getMetaClass(value).smartDump(this, context, out, value);
        }
    }

    IRubyObject currentValue;
    boolean doVariables;
    int variableCount;

    public void writeDirectly(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        ClassIndex nativeClassIndex;
        boolean shouldMarshalEncoding;

        if (!(value instanceof CoreObjectType coreObjectType)
                || (nativeClassIndex = coreObjectType.getNativeClassIndex()) == ClassIndex.OBJECT
                || nativeClassIndex == ClassIndex.BASICOBJECT) {

            writeObjectData(context, out, value);

        } else if (!(shouldMarshalEncoding = (value instanceof MarshalEncoding marshalEncoding && marshalEncoding.shouldMarshalEncoding()))
                && (value.isImmediate()
                        || !value.hasVariables()
                        || nativeClassIndex == ClassIndex.CLASS
                        || nativeClassIndex == ClassIndex.MODULE)) {

            dumpBaseObject(context, out, value, nativeClassIndex);

        } else {

            var ivarAccessors = checkVariables(value, shouldMarshalEncoding);
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

    private void clearVariableState() {
        currentValue = null;
    }

    private Map<String, VariableAccessor> checkVariables(IRubyObject value, boolean shouldMarshalEncoding) {
        currentValue = value;
        doVariables = shouldMarshalEncoding;
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
        RubyClass type = meta;
        switch(nativeClassIndex) {
        case STRING:
        case REGEXP:
        case ARRAY:
        case HASH:
            type = dumpExtended(context, out, meta);
            break;
        }

        if (nativeClassIndex != meta.getClassIndex() &&
                nativeClassIndex != ClassIndex.STRUCT &&
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
            ClassIndex nativeClassIndex = ((CoreObjectType)value).getNativeClassIndex();
            Ruby runtime;

            switch (nativeClassIndex) {
            case ARRAY:
                out.write('[');
                RubyArray.marshalTo((RubyArray)value, this, context, out);
                return;
            case FALSE:
                out.write('F');
                return;
            case FIXNUM:
                RubyFixnum fixnum = (RubyFixnum)value;

                if (isMarshalFixnum(fixnum)) {
                    out.write('i');
                    writeInt(out, (int) fixnum.getValue());
                    return;
                }
                // FIXME: inefficient; constructing a bignum just for dumping?
                runtime = context.runtime;
                value = RubyBignum.newBignum(runtime, fixnum.getValue());

                // fall through
            case BIGNUM:
                out.write('l');
                RubyBignum.marshalTo((RubyBignum)value, this, out);
                return;
            case CLASS:
                if (((RubyClass)value).isSingleton()) throw typeError(context,"singleton class can't be dumped");
                out.write('c');
                RubyClass.marshalTo((RubyClass)value, this, out);
                return;
            case FLOAT:
                out.write('f');
                RubyFloat.marshalTo((RubyFloat)value, this, out);
                return;
            case HASH: {
                RubyHash hash = (RubyHash)value;

                if(hash.getIfNone() == RubyBasicObject.UNDEF){
                    out.write('{');
                } else if (hash.hasDefaultProc()) {
                    throw typeError(context, "can't dump hash with default proc");
                } else {
                    out.write('}');
                }

                RubyHash.marshalTo(hash, this, context, out);
                return;
            }
            case MODULE:
                out.write('m');
                RubyModule.marshalTo((RubyModule)value, this, context, out);
                return;
            case NIL:
                out.write('0');
                return;
            case OBJECT:
            case BASICOBJECT:
                final RubyClass type = getMetaClass(value);
                dumpDefaultObjectHeader(context, out, type);
                type.getRealClass().marshal(value, this, context, out);
                return;
            case REGEXP:
                out.write('/');
                RubyRegexp.marshalTo((RubyRegexp)value, this, out);
                return;
            case STRING:
                registerLinkTarget(value);
                out.write('"');
                writeString(out, value.convertToString().getByteList());
                return;
            case STRUCT:
                RubyStruct.marshalTo((RubyStruct)value, this, context, out);
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
            metaClass.getRealClass().marshal(value, this, context, out);
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
                dumpUserdefBase(out, context, klass, marshaled);

                writeInt(out, size);

                ivarAccessors.forEach(new VariableDumper(context, out, marshaled));
            } else {
                dumpUserdefBase(out, context, klass, marshaled);
            }
        } else {
            dumpUserdefBase(out, context, klass, marshaled);
        }

        registerLinkTarget(value);
    }

    private void dumpUserdefBase(RubyOutputStream out, ThreadContext context, RubyClass klass, RubyString marshaled) {
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
        void receive(NewMarshal marshal, ThreadContext context, RubyOutputStream out, String name, IRubyObject value);
    }

    public interface VariableSupplier<T> {
        void forEach(NewMarshal marshal, ThreadContext context, RubyOutputStream out, T value, VariableReceiver receiver);
    }

    public <T extends IRubyObject> void dumpVariables(ThreadContext context, RubyOutputStream out, T value, int extraSize, VariableSupplier<T> extra) {
        dumpVariables(context, out, value, extraSize);
        extra.forEach(this, context, out, value, (m, c, o, name, v) -> dumpVariable(m, c, o, name, v));
    }

    private static void dumpVariable(NewMarshal marshal, ThreadContext context, RubyOutputStream out, String name, Object value) {
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
        if (value == 0) {
            out.write(0);
        } else if (0 < value && value < 123) {
            out.write(value + 5);
        } else if (-124 < value && value < 0) {
            out.write((value - 5) & 0xff);
        } else {
            byte[] buf = new byte[4];
            int i = 0;
            for (; i < buf.length; i++) {
                buf[i] = (byte) (value & 0xff);

                value = value >> 8;
                if (value == 0 || value == -1) {
                    break;
                }
            }
            int len = i + 1;
            out.write(value < 0 ? -len : len);
            out.write(buf, 0, i + 1);
        }
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
            dumpVariable(NewMarshal.this, context, out, name, varValue);
        }
    }
}
