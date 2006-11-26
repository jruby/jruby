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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Unmarshals objects from strings or streams in Ruby's marsal format.
 *
 * @author Anders
 */
public class UnmarshalStream extends FilterInputStream {
    protected final IRuby runtime;
    private UnmarshalCache cache;

    public UnmarshalStream(IRuby runtime, InputStream in) throws IOException {
        super(in);
        this.runtime = runtime;
        this.cache = new UnmarshalCache(runtime);

        in.read(); // Major
        in.read(); // Minor
    }

    public IRubyObject unmarshalObject() throws IOException {
        return unmarshalObject(null);
    }

    public IRubyObject unmarshalObject(IRubyObject proc) throws IOException {
        int type = readUnsignedByte();
        IRubyObject result;
        if (cache.isLinkType(type)) {
            result = cache.readLink(this, type);
        } else {
        	result = unmarshalObjectDirectly(type, proc);
        }
        return result;
    }

    public void registerLinkTarget(IRubyObject newObject) {
        cache.register(newObject);
    }

    private IRubyObject unmarshalObjectDirectly(int type, IRubyObject proc) throws IOException {
    	IRubyObject rubyObj = null;
        switch (type) {
        	case 'I':
                rubyObj = unmarshalObject(proc);
                defaultInstanceVarsUnmarshal(rubyObj, proc);
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
            case ':' :
                rubyObj = RubySymbol.unmarshalFrom(this);
                break;
            case '[' :
                rubyObj = RubyArray.unmarshalFrom(this);
                break;
            case '{' :
                rubyObj = RubyHash.unmarshalFrom(this);
                break;
            case 'c' :
                rubyObj = RubyClass.unmarshalFrom(this);
                break;
            case 'm' :
                rubyObj = RubyModule.unmarshalFrom(this);
                break;
            case 'l' :
                rubyObj = RubyBignum.unmarshalFrom(this);
                break;
            case 'S' :
                rubyObj = RubyStruct.unmarshalFrom(this);
                break;
            case 'o' :
                rubyObj = defaultObjectUnmarshal(proc);
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
        
        if (proc != null) {
			proc.callMethod(getRuntime().getCurrentContext(), "call", new IRubyObject[] {rubyObj});
		}
        return rubyObj;
    }


    public IRuby getRuntime() {
        return runtime;
    }

    public int readUnsignedByte() throws IOException {
        int result = read();
        if (result == -1) {
            throw new IOException("Unexpected end of stream");
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

    public String unmarshalString() throws IOException {
        int length = unmarshalInt();
        byte[] buffer = new byte[length];
        int bytesRead = read(buffer);
        if (bytesRead != length) {
            throw new IOException("Unexpected end of stream");
        }
        return RubyString.bytesToString(buffer);
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

    private IRubyObject defaultObjectUnmarshal(IRubyObject proc) throws IOException {
        RubySymbol className = (RubySymbol) unmarshalObject();

        // ... FIXME: handle if class doesn't exist ...

        RubyClass type = (RubyClass) runtime.getClassFromPath(className.asSymbol());

        assert type != null : "type shouldn't be null.";

        IRubyObject result = new RubyObject(runtime, type);
        registerLinkTarget(result);

        defaultInstanceVarsUnmarshal(result, proc);

        return result;
    }
    
    private void defaultInstanceVarsUnmarshal(IRubyObject object, IRubyObject proc) throws IOException {
    	int count = unmarshalInt();
    	
    	for (int i = 0; i < count; i++) {
    		object.setInstanceVariable(unmarshalObject().asSymbol(), unmarshalObject(proc));
    	}
    }
    
    private IRubyObject uclassUnmarshall() throws IOException {
    	RubySymbol className = (RubySymbol)unmarshalObject();
    	
    	RubyClass type = (RubyClass)runtime.getClassFromPath(className.asSymbol());
    	
    	IRubyObject result = unmarshalObject();
    	
    	result.setMetaClass(type);
    	
    	return result;
    }

    private IRubyObject userUnmarshal() throws IOException {
        String className = unmarshalObject().asSymbol();
        String marshaled = unmarshalString();
        RubyModule classInstance;
        try {
            classInstance = runtime.getClassFromPath(className);
        } catch (RaiseException e) {
            if (e.getException().isKindOf(runtime.getModule("NameError"))) {
                throw runtime.newArgumentError("undefined class/module " + className);
            } 
                
            throw e;
        }
        if (!classInstance.respondsTo("_load")) {
            throw runtime.newTypeError("class " + classInstance.getName() + " needs to have method `_load'");
        }
        IRubyObject result = classInstance.callMethod(getRuntime().getCurrentContext(),
            "_load", runtime.newString(marshaled));
        registerLinkTarget(result);
        return result;
    }

    private IRubyObject userNewUnmarshal() throws IOException {
        String className = unmarshalObject().asSymbol();
        IRubyObject marshaled = unmarshalObject();
        RubyClass classInstance = runtime.getClass(className);
        IRubyObject result = classInstance.newInstance(new IRubyObject[0]);;
        result.callMethod(getRuntime().getCurrentContext(),"marshal_load", marshaled);
        registerLinkTarget(result);
        return result;
    }
}
