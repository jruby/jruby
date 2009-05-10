/***** BEGIN LICENSE BLOCK *****
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.util.ByteList;

/**
 * Unmarshals objects from strings or streams in Ruby's marshal format.
 *
 * @author Anders
 */
public class UnmarshalStream extends InputStream {
    protected final Ruby runtime;
    private final UnmarshalCache cache;
    private final IRubyObject proc;
    private final InputStream inputStream;

    public UnmarshalStream(Ruby runtime, InputStream in, IRubyObject proc) throws IOException {
        this.runtime = runtime;
        this.cache = new UnmarshalCache(runtime);
        this.proc = proc;
        this.inputStream = in;

        int major = in.read(); // Major
        int minor = in.read(); // Minor

        if(major == -1 || minor == -1) {
            throw new EOFException("Unexpected end of stream");
        }
        
        if(major != Constants.MARSHAL_MAJOR || minor > Constants.MARSHAL_MINOR) {
            throw runtime.newTypeError(String.format("incompatible marshal file format (can't be read)\n\tformat version %d.%d required; %d.%d given", Constants.MARSHAL_MAJOR, Constants.MARSHAL_MINOR, major, minor));
        }
    }

    public IRubyObject unmarshalObject() throws IOException {
        int type = readUnsignedByte();
        IRubyObject result;
        if (cache.isLinkType(type)) {
            result = cache.readLink(this, type);
        } else {
            result = unmarshalObjectDirectly(type);
        }
        return result;
    }

    public void registerLinkTarget(IRubyObject newObject) {
        if (MarshalStream.shouldBeRegistered(newObject)) {
            cache.register(newObject);
        }
    }

    public static RubyModule getModuleFromPath(Ruby runtime, String path) {
        RubyModule value = runtime.getClassFromPath(path);
        if (!value.isModule()) throw runtime.newArgumentError(path + " does not refer module");
        return value;
    }

    public static RubyClass getClassFromPath(Ruby runtime, String path) {
        RubyModule value = runtime.getClassFromPath(path);
        if (!value.isClass()) throw runtime.newArgumentError(path + " does not refer class");
        return (RubyClass)value;
    }

    boolean ivarsWaiting = false;

    private IRubyObject unmarshalObjectDirectly(int type) throws IOException {
    	IRubyObject rubyObj = null;
        switch (type) {
            case 'I':
                ivarsWaiting = true;
                rubyObj = unmarshalObject();
                if (ivarsWaiting) {
                    defaultVariablesUnmarshal(rubyObj);
                    ivarsWaiting = false;
                }
                break;
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
            case '/' :
                rubyObj = RubyRegexp.unmarshalFrom(this);
                break;
            case ':' :
                rubyObj = RubySymbol.unmarshalFrom(this);
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
                RubyModule tp = null;
                try {
                    tp = runtime.getClassFromPath(moduleName.asJavaString());
                } catch (RaiseException e) {
                    if (runtime.fastGetModule("NameError").isInstance(e.getException())) {
                        throw runtime.newArgumentError("undefined class/module " + moduleName.asJavaString());
                    } 
                    throw e;
                }

                rubyObj = unmarshalObject();
                
                tp.extend_object(rubyObj);
                tp.callMethod(runtime.getCurrentContext(),"extended", rubyObj);
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
                rubyObj = userUnmarshal();
                break;
            case 'U' :
                rubyObj = userNewUnmarshal();
                break;
            case 'C' :
            	rubyObj = uclassUnmarshall();
            	break;
            default :
                throw getRuntime().newArgumentError("dump format error(" + (char)type + ")");
        }
        
        if (proc != null && type != ':') {
            // call the proc, but not for symbols
            RuntimeHelpers.invoke(getRuntime().getCurrentContext(), proc, "call", rubyObj);
        }
        return rubyObj;
    }


    public Ruby getRuntime() {
        return runtime;
    }

    public int readUnsignedByte() throws IOException {
        int result = read();
        if (result == -1) {
            throw new EOFException("Unexpected end of stream");
        }
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
        } else if (5 < c && c < 128) {
            return c - 5;
        } else if (-129 < c && c < -5) {
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
        RubySymbol className = (RubySymbol) unmarshalObject();

        RubyClass type = null;
        try {
            type = getClassFromPath(runtime, className.toString());
        } catch (RaiseException e) {
            if (runtime.fastGetModule("NameError").isInstance(e.getException())) {
                throw runtime.newArgumentError("undefined class/module " + className.asJavaString());
            } 
                
            throw e;
        }

        assert type != null : "type shouldn't be null.";

        IRubyObject result = (IRubyObject)type.unmarshal(this);

        return result;
    }
    
    public void defaultVariablesUnmarshal(IRubyObject object) throws IOException {
        int count = unmarshalInt();
        
        List<Variable<Object>> attrs = new ArrayList<Variable<Object>>(count);
        
        for (int i = count; --i >= 0; ) {
            String name = unmarshalObject().asJavaString();
            IRubyObject value = unmarshalObject();
            attrs.add(new VariableEntry<Object>(name, value));
        }
        
        object.syncVariables(attrs);
    }
    
    
    private IRubyObject uclassUnmarshall() throws IOException {
    	RubySymbol className = (RubySymbol)unmarshalObject();
    	
    	RubyClass type = (RubyClass)runtime.getClassFromPath(className.asJavaString());
    	
        // All "C" marshalled objects descend from core classes, which are all RubyObject
    	RubyObject result = (RubyObject)unmarshalObject();
    	
    	result.setMetaClass(type);
    	
    	return result;
    }

    private IRubyObject userUnmarshal() throws IOException {
        String className = unmarshalObject().asJavaString();
        ByteList marshaled = unmarshalString();
        RubyModule classInstance = findClass(className);
        if (!classInstance.respondsTo("_load")) {
            throw runtime.newTypeError("class " + classInstance.getName() + " needs to have method `_load'");
        }
        RubyString data = RubyString.newString(getRuntime(), marshaled);
        if (ivarsWaiting) {
            defaultVariablesUnmarshal(data);
            ivarsWaiting = false;
        }
        IRubyObject result = classInstance.callMethod(getRuntime().getCurrentContext(),
            "_load", data);
        registerLinkTarget(result);
        return result;
    }

    private IRubyObject userNewUnmarshal() throws IOException {
        String className = unmarshalObject().asJavaString();
        RubyClass classInstance = findClass(className);
        IRubyObject result = classInstance.allocate();
        registerLinkTarget(result);
        IRubyObject marshaled = unmarshalObject();
        result.callMethod(getRuntime().getCurrentContext(),"marshal_load", marshaled);
        return result;
    }

    private RubyClass findClass(String className) {
        RubyModule classInstance;
        try {
            classInstance = runtime.getClassFromPath(className);
        } catch (RaiseException e) {
            if (runtime.getModule("NameError").isInstance(e.getException())) {
                throw runtime.newArgumentError("undefined class/module " + className);
            } 
            throw e;
        }
        if (! (classInstance instanceof RubyClass)) {
            throw runtime.newArgumentError(className + " does not refer class"); // sic
        }
        return (RubyClass) classInstance;
    }

    public int read() throws IOException {
        return inputStream.read();
    }
}
