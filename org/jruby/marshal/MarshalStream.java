/*
 * MarshalStream.java
 * Created on 20 Mar 2002
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
import org.jruby.exceptions.ArgumentError;

/**
 * Marshals objects into Ruby's binary marshal format.
 *
 * @author Anders
 * $Revision$
 */

public class MarshalStream extends FilterOutputStream {

    private static final int MARSHAL_MAJOR = 4;
    private static final int MARSHAL_MINOR = 5;

    private final Ruby ruby;
    private final int depthLimit;
    private int depth = 0;
    private Map dumpedObjects = new HashMap();
    private Map dumpedSymbols = new HashMap();

    public MarshalStream(Ruby ruby, OutputStream out, int depthLimit) throws IOException {
		super(out);
        this.ruby = ruby;
        this.depthLimit = (depthLimit >= 0 ? depthLimit : Integer.MAX_VALUE);

		out.write(MARSHAL_MAJOR);
		out.write(MARSHAL_MINOR);
    }

    public void dumpObject(RubyObject value) throws IOException {
        depth++;
        if (depth > depthLimit) {
            throw new ArgumentError(ruby, "exceed depth limit");
        }
		if (value.isNil()) {
			out.write('0');
        } else {
            writeAndRegister(value);
        }
        depth--;
    }

    private void writeAndRegister(RubyObject value) throws IOException {
        writeAndRegister(dumpedObjects, '@', value);
    }

    private void writeAndRegister(RubySymbol value) throws IOException {
        writeAndRegister(dumpedSymbols, ';', value);
    }

    private void writeAndRegister(Map registry, char linkSymbol, RubyObject value) throws IOException {
        if (registry.containsKey(value)) {
            out.write(linkSymbol);
            dumpInt(((Integer) registry.get(value)).intValue());
        } else {
            registry.put(value, new Integer(registry.size()));
            value.marshalTo(this);
        }
    }

    public void dumpString(String value) throws IOException {
		dumpInt(value.length());
		out.write(value.getBytes());
    }

    public void dumpInt(int value) throws IOException {
		if (value == 0) {
			out.write(0);
		} else if (0 < value && value < 123) {
			out.write(value + 5);
		} else if (-124 < value && value < 0) {
			out.write((value - 5) & 0xff);
		} else {
			int[] buf = new int[4];
			int i;
			for (i = 0; i < buf.length; i++) {
				buf[i] = value & 0xff;
				value = value >> 8;
				if (value == 0 || value == -1) {
					break;
				}
			}
			int len = i + 1;
			out.write(value < 0 ? -len : len);
			for (i = 0; i < len; i++) {
				out.write(buf[i]);
			}
		}
    }
}
