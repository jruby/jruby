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
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.encoding.MarshalEncoding;
import org.jruby.util.ByteList;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.jruby.RubyBasicObject.getMetaClass;
import static org.jruby.api.Convert.castToString;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.marshal.MarshalCommon.SYMBOL_ENCODING;
import static org.jruby.runtime.marshal.MarshalCommon.SYMBOL_ENCODING_SPECIAL;
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
    private final MarshalCache cache;
    private final int depthLimit;
    private int depth = 0;

    public NewMarshal(int depthLimit) {
        this.cache = new MarshalCache();
        this.depthLimit = depthLimit >= 0 ? depthLimit : Integer.MAX_VALUE;
    }

    public static class RubyOutputStream extends OutputStream {
        private final OutputStream wrap;
        private final Consumer<IOException> handler;

        public RubyOutputStream(OutputStream wrap, Consumer<IOException> handler) {
            this.wrap = wrap;
            this.handler = handler;
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
            handler.accept(ioe);
        }
    }

    public void start(RubyOutputStream out) {
        out.write(Constants.MARSHAL_MAJOR);
        out.write(Constants.MARSHAL_MINOR);
    }

    public void dumpObject(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        depth++;
        
        if (depth > depthLimit) throw context.runtime.newArgumentError("exceed depth limit");

        writeAndRegister(context, out, value);

        depth--;
    }

    public void registerLinkTarget(IRubyObject newObject) {
        if (shouldBeRegistered(newObject)) {
            cache.register(newObject);
        }
    }

    public void registerSymbol(ByteList sym) {
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
        return fixnum.getLongValue() <= RubyFixnum.MAX_MARSHAL_FIXNUM && fixnum.getLongValue() >= RubyFixnum.MIN_MARSHAL_FIXNUM;
    }

    private void writeAndRegisterSymbol(ThreadContext context, RubyOutputStream out, ByteList sym) {
        if (cache.isSymbolRegistered(sym)) {
            cache.writeSymbolLink(this, context, out, sym);
        } else {
            registerSymbol(sym);
            dumpSymbol(out, sym);
        }
    }

    private void writeAndRegister(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        if (!(value instanceof RubySymbol) && cache.isRegistered(value)) {
            cache.writeLink(this, context, out, value);
        } else {
            value.getMetaClass().smartDump(this, context, out, value);
        }
    }

    private List<Variable<Object>> getVariables(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        List<Variable<Object>> variables = null;
        if (value instanceof CoreObjectType) {
            ClassIndex nativeClassIndex = ((CoreObjectType)value).getNativeClassIndex();
            
            if (nativeClassIndex != ClassIndex.OBJECT && nativeClassIndex != ClassIndex.BASICOBJECT) {
                if (shouldMarshalEncoding(value) || (
                        !value.isImmediate()
                        && value.hasVariables()
                        && nativeClassIndex != ClassIndex.CLASS
                        && nativeClassIndex != ClassIndex.MODULE
                        )) {
                    // object has instance vars and isn't a class, get a snapshot to be marshalled
                    // and output the ivar header here

                    variables = value.getMarshalVariableList();

                    // check if any of those variables were actually set
                    if (variables.size() > 0 || shouldMarshalEncoding(value)) {
                        // write `I' instance var signet if class is NOT a direct subclass of Object
                        out.write(TYPE_IVAR);
                    } else {
                        // no variables, no encoding
                        variables = null;
                    }
                }
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
                    writeUserClass(context, out, value, type);
                }
            }
        }
        return variables;
    }

    private static boolean shouldMarshalEncoding(IRubyObject value) {
        if (!(value instanceof MarshalEncoding)) return false;
        return ((MarshalEncoding) value).shouldMarshalEncoding();
    }

    public void writeDirectly(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        List<Variable<Object>> variables = getVariables(context, out, value);
        writeObjectData(context, out, value);
        if (variables != null) {
            dumpVariablesWithEncoding(context, out, variables, value);
        }
    }

    public static String getPathFromClass(RubyModule clazz) {
        RubyString path = clazz.rubyName();
        
        if (path.charAt(0) == '#') {
            Ruby runtime = clazz.getRuntime();
            String type = clazz.isClass() ? "class" : "module";
            throw typeError(runtime.getCurrentContext(), str(runtime, "can't dump anonymous " + type + " ", types(runtime, clazz)));
        }
        
        RubyModule real = clazz.isModule() ? clazz : ((RubyClass)clazz).getRealClass();
        Ruby runtime = clazz.getRuntime();

        // FIXME: This is weird why we do this.  rubyName should produce something which can be referred so what example
        // will this fail on?  If there is a failing case then passing asJavaString may be broken since it will not be
        // a properly encoded string.  If this is an issue we should make a clazz.IdPath where all segments are returned
        // by their id names.
        if (runtime.getClassFromPath(path.asJavaString()) != real) {
            throw typeError(runtime.getCurrentContext(), str(runtime, types(runtime, clazz), " can't be referred"));
        }
        return path.asJavaString();
    }
    
    private void writeObjectData(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        // switch on the object's *native type*. This allows use-defined
        // classes that have extended core native types to piggyback on their
        // marshalling logic.
        if (value instanceof CoreObjectType) {
            if (value instanceof DataType) {
                Ruby runtime = value.getRuntime();

                throw typeError(runtime.getCurrentContext(), str(runtime, "no _dump_data is defined for class ", types(runtime, getMetaClass(value))));
            }
            ClassIndex nativeClassIndex = ((CoreObjectType)value).getNativeClassIndex();

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
                    writeInt(out, (int) fixnum.getLongValue());
                    return;
                }
                // FIXME: inefficient; constructing a bignum just for dumping?
                value = RubyBignum.newBignum(value.getRuntime(), fixnum.getLongValue());

                // fall through
            case BIGNUM:
                out.write('l');
                RubyBignum.marshalTo((RubyBignum)value, this, context, out);
                return;
            case CLASS:
                if (((RubyClass)value).isSingleton()) throw typeError(context,"singleton class can't be dumped");
                out.write('c');
                RubyClass.marshalTo((RubyClass)value, this, context, out);
                return;
            case FLOAT:
                out.write('f');
                RubyFloat.marshalTo((RubyFloat)value, this, context, out);
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
                writeAndRegisterSymbol(context, out, ((RubySymbol) value).getBytes());
                return;
            case TRUE:
                out.write('T');
                return;
            default:
                throw typeError(context, str(context.runtime, "can't dump ", types(context.runtime, value.getMetaClass())));
            }
        } else {
            dumpDefaultObjectHeader(context, out, value.getMetaClass());
            value.getMetaClass().getRealClass().marshal(value, this, context, out);
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
        final Ruby runtime = context.runtime;
        writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(runtime, klass.getRealClass().getName()).getBytes());

        IRubyObject marshaled;
        if (entry != null) {
            marshaled = entry.method.call(runtime.getCurrentContext(), value, entry.sourceModule, "marshal_dump");
        } else {
            marshaled = value.callMethod(runtime.getCurrentContext(), "marshal_dump");
        }
        if (marshaled.getMetaClass() == klass) {
            throw runtime.newRuntimeError("marshal_dump returned same class instance");
        }
        dumpObject(context, out, marshaled);
    }

    public void userMarshal(ThreadContext context, RubyOutputStream out, IRubyObject value, CacheEntry entry) {
        userCommon(context, out, value, entry);
    }

    public void userMarshal(ThreadContext context, RubyOutputStream out, IRubyObject value) {
        userCommon(context, out, value, null);
    }

    private void userCommon(ThreadContext context, RubyOutputStream out, IRubyObject value, CacheEntry entry) {
        Ruby runtime = context.runtime;
        RubyFixnum depthLimitFixnum = runtime.newFixnum(depthLimit);
        final RubyClass klass = getMetaClass(value);
        IRubyObject dumpResult;
        if (entry != null) {
            dumpResult = entry.method.call(context, value, entry.sourceModule, "_dump", depthLimitFixnum);
        } else {
            dumpResult = value.callMethod(context, "_dump", depthLimitFixnum);
        }
        
        RubyString marshaled = castToString(context, dumpResult);

        List<Variable<Object>> variables = null;
        if (marshaled.hasVariables()) {
            variables = marshaled.getMarshalVariableList();
            if (variables.size() > 0) {
                out.write(TYPE_IVAR);
            } else {
                variables = null;
            }
        }

        out.write(TYPE_USERDEF);

        writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(runtime, klass.getRealClass().getName()).getBytes());

        writeString(out, marshaled.getByteList());

        if (variables != null) {
            dumpVariables(context, out, variables);
        }

        registerLinkTarget(value);
    }
    
    public void writeUserClass(ThreadContext context, RubyOutputStream out, IRubyObject obj, RubyClass type) {
        out.write(TYPE_UCLASS);
        
        // w_unique
        if (type.getName().charAt(0) == '#') {
            Ruby runtime = obj.getRuntime();

            throw typeError(runtime.getCurrentContext(), str(runtime, "can't dump anonymous class ", types(runtime, type)));
        }
        
        // w_symbol
        writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(context.runtime, type.getName()).getBytes());
    }
    
    public void dumpVariablesWithEncoding(ThreadContext context, RubyOutputStream out, List<Variable<Object>> vars, IRubyObject obj) {
        if (shouldMarshalEncoding(obj)) {
            writeInt(out, vars.size() + 1); // vars preceded by encoding
            writeEncoding(context, out, ((MarshalEncoding)obj).getMarshalEncoding());
        } else {
            writeInt(out, vars.size());
        }
        
        dumpVariablesShared(context, out, vars);
    }

    public void dumpVariables(ThreadContext context, RubyOutputStream out, List<Variable<Object>> vars) {
        writeInt(out, vars.size());
        dumpVariablesShared(context, out, vars);
    }

    private void dumpVariablesShared(ThreadContext context, RubyOutputStream out, List<Variable<Object>> vars) {
        for (Variable<Object> var : vars) {
            if (var.getValue() instanceof IRubyObject) {
                writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(context.runtime, var.getName()).getBytes());
                dumpObject(context, out, (IRubyObject)var.getValue());
            }
        }
    }

    public void writeEncoding(ThreadContext context, RubyOutputStream out, Encoding encoding) {
        Ruby runtime = context.runtime;
        if (encoding == null || encoding == USASCIIEncoding.INSTANCE) {
            writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(runtime, SYMBOL_ENCODING_SPECIAL).getBytes());
            writeObjectData(context, out, runtime.getFalse());
        } else if (encoding == UTF8Encoding.INSTANCE) {
            writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(runtime, SYMBOL_ENCODING_SPECIAL).getBytes());
            writeObjectData(context, out, runtime.getTrue());
        } else {
            writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(runtime, SYMBOL_ENCODING).getBytes());
            RubyString encodingString = new RubyString(runtime, runtime.getString(), encoding.getName());
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
        if(type.isSingleton()) {
            if (hasSingletonMethods(type) || type.hasVariables()) { // any ivars, since we don't have __attached__ ivar now
                throw typeError(type.getRuntime().getCurrentContext(), "singleton can't be dumped");
            }
            type = type.getSuperClass();
        }
        while(type.isIncluded()) {
            out.write('e');
            writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(context.runtime, type.getOrigin().getName()).getBytes());
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
        writeAndRegisterSymbol(context, out, RubySymbol.newSymbol(context.runtime, getPathFromClass(type.getRealClass())).getBytes());
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

    @Deprecated
    public boolean isTainted() {
        return false;
    }

    @Deprecated
    public boolean isUntrusted() {
        return false;
    }
}
