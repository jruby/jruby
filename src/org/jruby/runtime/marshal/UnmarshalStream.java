/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jruby.runtime.marshal;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

/**
 * Unmarshals objects from strings or streams in Ruby's marsal format.
 *
 * @author Anders
 * $Revision$
 */
public class UnmarshalStream extends FilterInputStream {
    protected final Ruby runtime;
    private UnmarshalCache cache;

    public UnmarshalStream(Ruby runtime, InputStream in) throws IOException {
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
            case '0' :
                rubyObj = runtime.getNil();
                break;
            case 'T' :
                rubyObj = RubyBoolean.newBoolean(runtime, true);
                break;
            case 'F' :
                rubyObj = RubyBoolean.newBoolean(runtime, false);
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
            default :
                throw new ArgumentError(getRuntime(), "dump format error(" + type + ")");
        }
        
        if (proc != null) {
			proc.callMethod("call", new IRubyObject[] {rubyObj});
		}
        return rubyObj;
    }


    public Ruby getRuntime() {
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

        RubyClass type = (RubyClass) runtime.getClasses().getClassFromPath(className.asSymbol());

        Asserts.notNull(type, "type shouldn't be null.");

        IRubyObject result = new RubyObject(runtime, type);
        registerLinkTarget(result);

        for (int i = 0, count = unmarshalInt(); i < count; i++) {
            result.setInstanceVariable(unmarshalObject().asSymbol(), unmarshalObject(proc));
        }

        return result;
    }

    private IRubyObject userUnmarshal() throws IOException {
        String className = unmarshalObject().asSymbol();
        String marshaled = unmarshalString();
        RubyModule classInstance = runtime.getModule(className);
        IRubyObject result = classInstance.callMethod(
            "_load",
            RubyString.newString(runtime, marshaled));
        registerLinkTarget(result);
        return result;
    }
}
