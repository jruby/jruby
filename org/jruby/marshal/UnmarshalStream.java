/*
 * UnmarshalStream.java
 * Created on 29 Mar 2002
 * 
 * Copyright (C) 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Anders Bengtsson
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

package org.jruby.marshal;

import java.io.*;
import java.util.*;
import org.jruby.*;
import org.jruby.util.*;
import org.jruby.exceptions.*;

/**
 * Unmarshals objects from strings or streams in Ruby's marsal format.
 *
 * @author Anders
 * $Revision$
 */

public class UnmarshalStream extends FilterInputStream {

    private final Ruby ruby;

    public UnmarshalStream(Ruby ruby, InputStream in) throws IOException {
        super(in);
        this.ruby = ruby;
        in.read(); // Major
        in.read(); // Minor
    }

    public RubyObject unmarshalObject() throws IOException {
        int type = readUnsignedByte();

        if (type == '0') {
            return RubyObject.nilObject(ruby);
        } else if (type == 'T') {
            return RubyBoolean.newBoolean(ruby, true);
        } else if (type == 'F') {
            return RubyBoolean.newBoolean(ruby, false);
        } else if (type == '"') {
            return RubyString.unmarshalFrom(this);
        } else if (type == 'i') {
            return RubyFixnum.unmarshalFrom(this);
        } else if (type == ':') {
            return RubySymbol.unmarshalFrom(this);
        } else if (type == '[') {
            return RubyArray.unmarshalFrom(this);
        } else if (type == '{') {
            return RubyHash.unmarshalFrom(this);
        } else if (type == 'c') {
            return RubyClass.unmarshalFrom(this);
        } else if (type == 'm') {
            return RubyModule.unmarshalFrom(this);
        } else if (type == 'l') {
            return RubyBignum.unmarshalFrom(this);
        } else if (type == 'o') {
            return defaultObjectUnmarshal();
        } else if (type == 'u') {
            return userUnmarshal();
        }

        throw new NotImplementedError(); // FIXME
    }

    public Ruby getRuby() {
        return ruby;
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
        } else {
            return (byte) b;
        }
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

    private RubyObject defaultObjectUnmarshal() throws IOException {
        RubySymbol className = (RubySymbol) unmarshalObject();
        int variableCount = unmarshalInt();

        RubyMap variables = new RubyHashMap(variableCount);
        for (int i = 0; i < variableCount; i++) {
            RubySymbol name = (RubySymbol) unmarshalObject();
            RubyObject value = unmarshalObject();
            variables.put(name.toId(), value);
        }

        // ... FIXME: handle if class doesn't exist ...

        RubyClass rubyClass = (RubyClass) ruby.getRubyClass(className.toId());
        RubyObject result = new RubyObject(ruby, rubyClass);
        result.setInstanceVariables(variables);
        return result;
    }

    private RubyObject userUnmarshal() throws IOException {
        String className = ((RubySymbol) unmarshalObject()).toId();
        String marshaled = unmarshalString();
        RubyModule classInstance = ruby.getRubyModule(className);
        return classInstance.funcall("_load", RubyString.newString(ruby, marshaled));
    }
}
