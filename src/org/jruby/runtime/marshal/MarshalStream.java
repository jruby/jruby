/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Marshals objects into Ruby's binary marshal format.
 *
 * @author Anders
 * $Revision$
 */
public class MarshalStream extends FilterOutputStream {
    private final Ruby runtime;
    private final int depthLimit;
    private int depth = 0;
    private MarshalCache cache;

    public MarshalStream(Ruby ruby, OutputStream out, int depthLimit) throws IOException {
        super(out);

        this.runtime = ruby;
        this.depthLimit = (depthLimit >= 0 ? depthLimit : Integer.MAX_VALUE);
        this.cache = new MarshalCache();

        out.write(Constants.MARSHAL_MAJOR);
        out.write(Constants.MARSHAL_MINOR);
    }

    public void dumpObject(IRubyObject value) throws IOException {
        depth++;
        if (depth > depthLimit) {
            throw new ArgumentError(runtime, "exceed depth limit");
        }
        if (! shouldBeRegistered(value)) {
            writeDirectly(value);
        } else if (hasUserDefinedMarshaling(value)) {
            userMarshal(value);
        } else {
            writeAndRegister(value);
        }
        depth--;
        if (depth == 0) out.flush(); // flush afer whole dump is complete
    }

    private void writeDirectly(IRubyObject value) throws IOException {
        if (value.isNil()) {
            out.write('0');
        } else {
            value.marshalTo(this);
        }
    }

    private boolean shouldBeRegistered(IRubyObject value) {
        if (value.isNil()) {
            return false;
        } else if (value instanceof RubyBoolean) {
            return false;
        } else if (value instanceof RubyFixnum) {
            return false;
        }
        return true;
    }

    private void writeAndRegister(IRubyObject value) throws IOException {
        if (cache.isRegistered(value)) {
            cache.writeLink(this, value);
        } else {
            cache.register(value);
            value.marshalTo(this);
        }
    }

    private boolean hasUserDefinedMarshaling(IRubyObject value) {
        return value.respondsTo("_dump");
    }

    private void userMarshal(IRubyObject value) throws IOException {
        out.write('u');
        dumpObject(RubySymbol.newSymbol(runtime, value.getMetaClass().getName()));

        RubyInteger depth = RubyFixnum.newFixnum(runtime, depthLimit);
        RubyString marshaled = (RubyString) value.callMethod("_dump", depth);
        dumpString(marshaled.getValue());
    }

    public void dumpString(String value) throws IOException {
        dumpInt(value.length());
        out.write(RubyString.stringToBytes(value));
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