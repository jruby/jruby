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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
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
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.ByteList;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.encoding.MarshalEncoding;

import static org.jruby.RubyBasicObject.getMetaClass;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

/**
 * Marshals objects into Ruby's binary marshal format.
 *
 * @author Anders
 */
public class MarshalStream extends FilterOutputStream {
    private final Ruby runtime;
    private final MarshalCache cache;
    private final int depthLimit;
    private boolean tainted = false;

    private int depth = 0;

    private final static char TYPE_IVAR = 'I';
    private final static char TYPE_USRMARSHAL = 'U';
    private final static char TYPE_USERDEF = 'u';
    private final static char TYPE_UCLASS = 'C';
    public final static String SYMBOL_ENCODING_SPECIAL = "E";
    private final static String SYMBOL_ENCODING = "encoding";

    public MarshalStream(Ruby runtime, OutputStream out, int depthLimit) throws IOException {
        super(out);

        this.runtime = runtime;
        this.depthLimit = depthLimit >= 0 ? depthLimit : Integer.MAX_VALUE;
        this.cache = new MarshalCache();

        out.write(Constants.MARSHAL_MAJOR);
        out.write(Constants.MARSHAL_MINOR);
    }

    public void dumpObject(IRubyObject value) throws IOException {
        depth++;
        
        if (depth > depthLimit) throw runtime.newArgumentError("exceed depth limit");

        tainted |= value.isTaint();

        writeAndRegister(value);

        depth--;
        if (depth == 0) out.flush(); // flush afer whole dump is complete
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

    private void writeAndRegisterSymbol(ByteList sym) throws IOException {
        if (cache.isSymbolRegistered(sym)) {
            cache.writeSymbolLink(this, sym);
        } else {
            registerSymbol(sym);
            dumpSymbol(sym);
        }
    }

    private void writeAndRegister(IRubyObject value) throws IOException {
        if (!(value instanceof RubySymbol) && cache.isRegistered(value)) {
            cache.writeLink(this, value);
        } else {
            value.getMetaClass().smartDump(this, value);
        }
    }

    private List<Variable<Object>> getVariables(IRubyObject value) throws IOException {
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

                    variables = value.getVariableList();

                    // check if any of those variables were actually set
                    if (variables.size() > 0 || shouldMarshalEncoding(value)) {
                        // write `I' instance var signet if class is NOT a direct subclass of Object
                        write(TYPE_IVAR);
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
                    type = dumpExtended(meta);
                    break;
                }

                if (nativeClassIndex != meta.getClassIndex() &&
                        nativeClassIndex != ClassIndex.STRUCT &&
                        nativeClassIndex != ClassIndex.FIXNUM &&
                        nativeClassIndex != ClassIndex.BIGNUM) {
                    // object is a custom class that extended one of the native types other than Object
                    writeUserClass(value, type);
                }
            }
        }
        return variables;
    }

    private static boolean shouldMarshalEncoding(IRubyObject value) {
        if (!(value instanceof MarshalEncoding)) return false;
        return ((MarshalEncoding) value).shouldMarshalEncoding();
    }

    public void writeDirectly(IRubyObject value) throws IOException {
        List<Variable<Object>> variables = getVariables(value);
        writeObjectData(value);
        if (variables != null) {
            dumpVariablesWithEncoding(variables, value);
        }
    }

    public static String getPathFromClass(RubyModule clazz) {
        RubyString path = clazz.rubyName();
        
        if (path.charAt(0) == '#') {
            Ruby runtime = clazz.getRuntime();
            String type = clazz.isClass() ? "class" : "module";
            throw runtime.newTypeError(str(runtime, "can't dump anonymous " + type + " ", types(runtime, clazz)));
        }
        
        RubyModule real = clazz.isModule() ? clazz : ((RubyClass)clazz).getRealClass();
        Ruby runtime = clazz.getRuntime();

        // FIXME: This is weird why we do this.  rubyName should produce something which can be referred so what example
        // will this fail on?  If there is a failing case then passing asJavaString may be broken since it will not be
        // a properly encoded string.  If this is an issue we should make a clazz.IdPath where all segments are returned
        // by their id names.
        if (runtime.getClassFromPath(path.asJavaString()) != real) {
            throw runtime.newTypeError(str(runtime, types(runtime, clazz), " can't be referred"));
        }
        return path.asJavaString();
    }
    
    private void writeObjectData(IRubyObject value) throws IOException {
        // switch on the object's *native type*. This allows use-defined
        // classes that have extended core native types to piggyback on their
        // marshalling logic.
        if (value instanceof CoreObjectType) {
            if (value instanceof DataType) {
                Ruby runtime = value.getRuntime();

                throw runtime.newTypeError(str(runtime, "no _dump_data is defined for class ", types(runtime, getMetaClass(value))));
            }
            ClassIndex nativeClassIndex = ((CoreObjectType)value).getNativeClassIndex();

            switch (nativeClassIndex) {
            case ARRAY:
                write('[');
                RubyArray.marshalTo((RubyArray)value, this);
                return;
            case FALSE:
                write('F');
                return;
            case FIXNUM:
                RubyFixnum fixnum = (RubyFixnum)value;

                if (isMarshalFixnum(fixnum)) {
                    write('i');
                    writeInt((int) fixnum.getLongValue());
                    return;
                }
                // FIXME: inefficient; constructing a bignum just for dumping?
                value = RubyBignum.newBignum(value.getRuntime(), fixnum.getLongValue());

                // fall through
            case BIGNUM:
                write('l');
                RubyBignum.marshalTo((RubyBignum)value, this);
                return;
            case CLASS:
                if (((RubyClass)value).isSingleton()) throw runtime.newTypeError("singleton class can't be dumped");
                write('c');
                RubyClass.marshalTo((RubyClass)value, this);
                return;
            case FLOAT:
                write('f');
                RubyFloat.marshalTo((RubyFloat)value, this);
                return;
            case HASH: {
                RubyHash hash = (RubyHash)value;

                if(hash.getIfNone() == RubyBasicObject.UNDEF){
                    write('{');
                } else if (hash.hasDefaultProc()) {
                    throw hash.getRuntime().newTypeError("can't dump hash with default proc");
                } else {
                    write('}');
                }

                RubyHash.marshalTo(hash, this);
                return;
            }
            case MODULE:
                write('m');
                RubyModule.marshalTo((RubyModule)value, this);
                return;
            case NIL:
                write('0');
                return;
            case OBJECT:
            case BASICOBJECT:
                final RubyClass type = getMetaClass(value);
                dumpDefaultObjectHeader(type);
                type.getRealClass().marshal(value, this);
                return;
            case REGEXP:
                write('/');
                RubyRegexp.marshalTo((RubyRegexp)value, this);
                return;
            case STRING:
                registerLinkTarget(value);
                write('"');
                writeString(value.convertToString().getByteList());
                return;
            case STRUCT:
                RubyStruct.marshalTo((RubyStruct)value, this);
                return;
            case SYMBOL:
                writeAndRegisterSymbol(((RubySymbol) value).getBytes());
                return;
            case TRUE:
                write('T');
                return;
            default:
                throw runtime.newTypeError(str(runtime, "can't dump ", types(runtime, value.getMetaClass())));
            }
        } else {
            dumpDefaultObjectHeader(value.getMetaClass());
            value.getMetaClass().getRealClass().marshal(value, this);
        }
    }

    public void userNewMarshal(IRubyObject value, CacheEntry entry) throws IOException {
        userNewCommon(value, entry);
    }

    public void userNewMarshal(IRubyObject value) throws IOException {
        userNewCommon(value, null);
    }

    private void userNewCommon(IRubyObject value, CacheEntry entry) throws IOException {
        registerLinkTarget(value);
        write(TYPE_USRMARSHAL);
        final RubyClass klass = getMetaClass(value);
        writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, klass.getRealClass().getName()).getBytes());

        IRubyObject marshaled;
        if (entry != null) {
            marshaled = entry.method.call(runtime.getCurrentContext(), value, entry.sourceModule, "marshal_dump");
        } else {
            marshaled = value.callMethod(runtime.getCurrentContext(), "marshal_dump");
        }
        if (marshaled.getMetaClass() == klass) {
            throw runtime.newRuntimeError("marshal_dump returned same class instance");
        }
        dumpObject(marshaled);
    }

    public void userMarshal(IRubyObject value, CacheEntry entry) throws IOException {
        userCommon(value, entry);
    }

    public void userMarshal(IRubyObject value) throws IOException {
        userCommon(value, null);
    }

    private void userCommon(IRubyObject value, CacheEntry entry) throws IOException {
        RubyFixnum depthLimitFixnum = runtime.newFixnum(depthLimit);
        final RubyClass klass = getMetaClass(value);
        IRubyObject dumpResult;
        if (entry != null) {
            dumpResult = entry.method.call(runtime.getCurrentContext(), value, entry.sourceModule, "_dump", depthLimitFixnum);
        } else {
            dumpResult = value.callMethod(runtime.getCurrentContext(), "_dump", depthLimitFixnum);
        }
        
        if (!(dumpResult instanceof RubyString)) {
            throw runtime.newTypeError(dumpResult, runtime.getString());
        }
        RubyString marshaled = (RubyString)dumpResult;

        List<Variable<Object>> variables = null;
        if (marshaled.hasVariables()) {
            variables = marshaled.getVariableList();
            if (variables.size() > 0) {
                write(TYPE_IVAR);
            } else {
                variables = null;
            }
        }

        write(TYPE_USERDEF);

        writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, klass.getRealClass().getName()).getBytes());

        writeString(marshaled.getByteList());

        if (variables != null) {
            dumpVariables(variables);
        }

        registerLinkTarget(value);
    }
    
    public void writeUserClass(IRubyObject obj, RubyClass type) throws IOException {
        write(TYPE_UCLASS);
        
        // w_unique
        if (type.getName().charAt(0) == '#') {
            Ruby runtime = obj.getRuntime();

            throw runtime.newTypeError(str(runtime, "can't dump anonymous class ", types(runtime, type)));
        }
        
        // w_symbol
        writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, type.getName()).getBytes());
    }
    
    public void dumpVariablesWithEncoding(List<Variable<Object>> vars, IRubyObject obj) throws IOException {
        if (shouldMarshalEncoding(obj)) {
            writeInt(vars.size() + 1); // vars preceded by encoding
            writeEncoding(((MarshalEncoding)obj).getMarshalEncoding());
        } else {
            writeInt(vars.size());
        }
        
        dumpVariablesShared(vars);
    }

    public void dumpVariables(List<Variable<Object>> vars) throws IOException {
        writeInt(vars.size());
        dumpVariablesShared(vars);
    }

    private void dumpVariablesShared(List<Variable<Object>> vars) throws IOException {
        for (Variable<Object> var : vars) {
            if (var.getValue() instanceof IRubyObject) {
                writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, var.getName()).getBytes());
                dumpObject((IRubyObject)var.getValue());
            }
        }
    }

    public void writeEncoding(Encoding encoding) throws IOException {
        if (encoding == null || encoding == USASCIIEncoding.INSTANCE) {
            writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, SYMBOL_ENCODING_SPECIAL).getBytes());
            writeObjectData(runtime.getFalse());
        } else if (encoding == UTF8Encoding.INSTANCE) {
            writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, SYMBOL_ENCODING_SPECIAL).getBytes());
            writeObjectData(runtime.getTrue());
        } else {
            writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, SYMBOL_ENCODING).getBytes());
            RubyString encodingString = new RubyString(runtime, runtime.getString(), encoding.getName());
            writeObjectData(encodingString);
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
    private RubyClass dumpExtended(RubyClass type) throws IOException {
        if(type.isSingleton()) {
            if (hasSingletonMethods(type) || type.hasVariables()) { // any ivars, since we don't have __attached__ ivar now
                throw type.getRuntime().newTypeError("singleton can't be dumped");
            }
            type = type.getSuperClass();
        }
        while(type.isIncluded()) {
            write('e');
            writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, type.getOrigin().getName()).getBytes());
            type = type.getSuperClass();
        }
        return type;
    }

    public void dumpDefaultObjectHeader(RubyClass type) throws IOException {
        dumpDefaultObjectHeader('o',type);
    }

    public void dumpDefaultObjectHeader(char tp, RubyClass type) throws IOException {
        dumpExtended(type);
        write(tp);
        writeAndRegisterSymbol(RubySymbol.newSymbol(runtime, getPathFromClass(type.getRealClass())).getBytes());
    }

    public void writeString(String value) throws IOException {
        writeInt(value.length());
        // FIXME: should preserve unicode?
        out.write(RubyString.stringToBytes(value));
    }

    public void writeString(ByteList value) throws IOException {
        int len = value.length();
        writeInt(len);
        out.write(value.getUnsafeBytes(), value.begin(), len);
    }

    public void dumpSymbol(ByteList value) throws IOException {
        write(':');
        int len = value.length();
        writeInt(len);
        out.write(value.getUnsafeBytes(), value.begin(), len);
    }

    public void writeInt(int value) throws IOException {
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
                buf[i] = (byte)(value & 0xff);
                
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

    public void writeByte(int value) throws IOException {
        out.write(value);
    }

    public boolean isTainted() {
        return tainted;
    }

    @Deprecated
    public boolean isUntrusted() {
        return tainted;
    }
}
