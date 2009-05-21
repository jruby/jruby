/*
 ***** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.marshal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
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
import org.jruby.IncludedModuleWrapper;
import org.jruby.RubyBasicObject;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.util.ByteList;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.methods.DynamicMethod;

/**
 * Marshals objects into Ruby's binary marshal format.
 *
 * @author Anders
 */
public class MarshalStream extends FilterOutputStream {
    private final Ruby runtime;
    private final MarshalCache cache;
    private final int depthLimit;
    
    private int depth = 0;

    private final static char TYPE_IVAR = 'I';
    private final static char TYPE_USRMARSHAL = 'U';
    private final static char TYPE_USERDEF = 'u';
    private final static char TYPE_UCLASS = 'C';

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
        
        if (depth > depthLimit) {
            throw runtime.newArgumentError("exceed depth limit");
        }
        
        writeAndRegister(value);

        depth--;
        if (depth == 0) {
            out.flush(); // flush afer whole dump is complete
        }
    }

    public void registerLinkTarget(IRubyObject newObject) {
        if (shouldBeRegistered(newObject)) {
            cache.register(newObject);
        }
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

    private void writeAndRegister(IRubyObject value) throws IOException {
        if (cache.isRegistered(value)) {
            cache.writeLink(this, value);
        } else {
            if (hasNewUserDefinedMarshaling(value)) {
                userNewMarshal(value);
            } else if (hasUserDefinedMarshaling(value)) {
                userMarshal(value);
            } else {
                writeDirectly(value);
            }
        }
    }

    private List<Variable<Object>> getVariables(IRubyObject value) throws IOException {
        List<Variable<Object>> variables = null;
        if (value instanceof CoreObjectType) {
            int nativeTypeIndex = ((CoreObjectType)value).getNativeTypeIndex();
            
            if (nativeTypeIndex != ClassIndex.OBJECT) {
                if (!value.isImmediate() && value.hasVariables() && nativeTypeIndex != ClassIndex.CLASS && nativeTypeIndex != ClassIndex.MODULE) {
                    // object has instance vars and isn't a class, get a snapshot to be marshalled
                    // and output the ivar header here

                    variables = value.getVariableList();

                    // write `I' instance var signet if class is NOT a direct subclass of Object
                    write(TYPE_IVAR);
                }
                RubyClass type = value.getMetaClass();
                switch(nativeTypeIndex) {
                case ClassIndex.STRING:
                case ClassIndex.REGEXP:
                case ClassIndex.ARRAY:
                case ClassIndex.HASH:
                    type = dumpExtended(type);
                    break;
                }

                if (nativeTypeIndex != value.getMetaClass().index && nativeTypeIndex != ClassIndex.STRUCT) {
                    // object is a custom class that extended one of the native types other than Object
                    writeUserClass(value, type);
                }
            }
        }
        return variables;
    }

    private void writeDirectly(IRubyObject value) throws IOException {
        List<Variable<Object>> variables = getVariables(value);
        writeObjectData(value);
        if (variables != null) {
            dumpVariables(variables);
        }
    }

    public static String getPathFromClass(RubyModule clazz) {
        String path = clazz.getName();
        
        if (path.charAt(0) == '#') {
            String classOrModule = clazz.isClass() ? "class" : "module";
            throw clazz.getRuntime().newTypeError("can't dump anonymous " + classOrModule + " " + path);
        }
        
        RubyModule real = clazz.isModule() ? clazz : ((RubyClass)clazz).getRealClass();

        if (clazz.getRuntime().getClassFromPath(path) != real) {
            throw clazz.getRuntime().newTypeError(path + " can't be referred");
        }
        return path;
    }
    
    private void writeObjectData(IRubyObject value) throws IOException {
        // switch on the object's *native type*. This allows use-defined
        // classes that have extended core native types to piggyback on their
        // marshalling logic.
        if (value instanceof CoreObjectType) {
            if (value instanceof DataType) {
                throw value.getRuntime().newTypeError("no marshal_dump is defined for class " + value.getMetaClass().getName());
            }
            int nativeTypeIndex = ((CoreObjectType)value).getNativeTypeIndex();

            switch (nativeTypeIndex) {
            case ClassIndex.ARRAY:
                write('[');
                RubyArray.marshalTo((RubyArray)value, this);
                return;
            case ClassIndex.FALSE:
                write('F');
                return;
            case ClassIndex.FIXNUM: {
                RubyFixnum fixnum = (RubyFixnum)value;

                if (isMarshalFixnum(fixnum)) {
                    write('i');
                    writeInt((int) fixnum.getLongValue());
                    return;
                }
                // FIXME: inefficient; constructing a bignum just for dumping?
                value = RubyBignum.newBignum(value.getRuntime(), fixnum.getLongValue());

                // fall through
            }
            case ClassIndex.BIGNUM:
                write('l');
                RubyBignum.marshalTo((RubyBignum)value, this);
                return;
            case ClassIndex.CLASS:
                if (((RubyClass)value).isSingleton()) throw runtime.newTypeError("singleton class can't be dumped");
                write('c');
                RubyClass.marshalTo((RubyClass)value, this);
                return;
            case ClassIndex.FLOAT:
                write('f');
                RubyFloat.marshalTo((RubyFloat)value, this);
                return;
            case ClassIndex.HASH: {
                RubyHash hash = (RubyHash)value;

                if(hash.getIfNone().isNil()){
                    write('{');
                }else if (hash.hasDefaultProc()) {
                    throw hash.getRuntime().newTypeError("can't dump hash with default proc");
                } else {
                    write('}');
                }

                RubyHash.marshalTo(hash, this);
                return;
            }
            case ClassIndex.MODULE:
                write('m');
                RubyModule.marshalTo((RubyModule)value, this);
                return;
            case ClassIndex.NIL:
                write('0');
                return;
            case ClassIndex.OBJECT:
                dumpDefaultObjectHeader(value.getMetaClass());
                value.getMetaClass().getRealClass().marshal(value, this);
                return;
            case ClassIndex.REGEXP:
                write('/');
                RubyRegexp.marshalTo((RubyRegexp)value, this);
                return;
            case ClassIndex.STRING:
                registerLinkTarget(value);
                write('"');
                writeString(value.convertToString().getByteList());
                return;
            case ClassIndex.STRUCT:
                //            write('S');
                RubyStruct.marshalTo((RubyStruct)value, this);
                return;
            case ClassIndex.SYMBOL:
                registerLinkTarget(value);
                write(':');
                writeString(value.toString());
                return;
            case ClassIndex.TRUE:
                write('T');
                return;
            default:
                throw runtime.newTypeError("can't dump " + value.getMetaClass().getName());
            }
        } else {
            dumpDefaultObjectHeader(value.getMetaClass());
            value.getMetaClass().getRealClass().marshal(value, this);
        }
        
    }

    private boolean hasNewUserDefinedMarshaling(IRubyObject value) {
        return value.respondsTo("marshal_dump");
    }

    private void userNewMarshal(IRubyObject value) throws IOException {
        registerLinkTarget(value);
        write(TYPE_USRMARSHAL);
        RubyClass metaclass = value.getMetaClass().getRealClass();
        dumpObject(RubySymbol.newSymbol(runtime, metaclass.getName()));

        IRubyObject marshaled = value.callMethod(runtime.getCurrentContext(), "marshal_dump"); 
        dumpObject(marshaled);
    }

    private boolean hasUserDefinedMarshaling(IRubyObject value) {
        return value.respondsTo("_dump");
    }

    private void userMarshal(IRubyObject value) throws IOException {
        registerLinkTarget(value);
        IRubyObject dumpResult = value.callMethod(runtime.getCurrentContext(), "_dump", runtime.newFixnum(depthLimit));
        if (!(dumpResult instanceof RubyString)) {
            throw runtime.newTypeError(dumpResult, runtime.getString());
        }
        RubyString marshaled = (RubyString)dumpResult;

        boolean hasVars;
        if (hasVars = marshaled.hasVariables()) {
            write(TYPE_IVAR);
        }

        write(TYPE_USERDEF);
        RubyClass metaclass = value.getMetaClass().getRealClass();

        dumpObject(RubySymbol.newSymbol(runtime, metaclass.getName()));

        writeString(marshaled.getByteList());

        if (hasVars) {
            dumpVariables(marshaled.getVariableList());
        }
    }
    
    public void writeUserClass(IRubyObject obj, RubyClass type) throws IOException {
        write(TYPE_UCLASS);
        
        // w_unique
        if (type.getName().charAt(0) == '#') {
            throw obj.getRuntime().newTypeError("can't dump anonymous class " + type.getName());
        }
        
        // w_symbol
        dumpObject(runtime.newSymbol(type.getName()));
    }
    
    /**
     * @deprecated superseded by {@link #dumpVariables()} 
     */
    public void dumpInstanceVars(Map instanceVars) throws IOException {

        runtime.getWarnings().warn(ID.DEPRECATED_METHOD, "internal: deprecated dumpInstanceVars() called", "dumpInstanceVars");

        writeInt(instanceVars.size());
        for (Iterator iter = instanceVars.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            IRubyObject value = (IRubyObject)instanceVars.get(name);
            writeAndRegister(runtime.newSymbol(name));
            dumpObject(value);
        }
    }
    
    public void dumpVariables(List<Variable<Object>> vars) throws IOException {
        writeInt(vars.size());
        for (Variable<Object> var : vars) {
            if (var.getValue() instanceof IRubyObject) {
                writeAndRegister(runtime.newSymbol(var.getName()));
                dumpObject((IRubyObject)var.getValue());
            }
        }
    }
    
    private boolean hasSingletonMethods(RubyClass type) {
        for(Iterator iter = type.getMethods().entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            DynamicMethod method = (DynamicMethod) entry.getValue();
            // We do not want to capture cached methods
            if(method.getImplementationClass() == type) {
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
            dumpObject(RubySymbol.newSymbol(runtime, ((IncludedModuleWrapper)type).getNonIncludedClass().getName()));
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
        RubySymbol classname = RubySymbol.newSymbol(runtime, getPathFromClass(type.getRealClass()));
        dumpObject(classname);
    }

    public void writeString(String value) throws IOException {
        writeInt(value.length());
        out.write(RubyString.stringToBytes(value));
    }

    public void writeString(ByteList value) throws IOException {
        int len = value.length();
        writeInt(len);
        out.write(value.unsafeBytes(), value.begin(), len);
    }

    public void dumpSymbol(String value) throws IOException {
        write(':');
        writeInt(value.length());
        out.write(RubyString.stringToBytes(value));
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
}
