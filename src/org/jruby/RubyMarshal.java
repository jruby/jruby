/*
 * RubyMarshal.java
 * Created on Mar 20 22:20:56 2002
 * 
 * Copyright (C) 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Anders Bengtsson
 * Copyright (C) 2003 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Thomas E Enebo <enebo@acm.org>
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

package org.jruby;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.CallbackFactory;

/**
 * Marshal module
 *
 * @author Anders
 * @version $Revision$
 */
public class RubyMarshal {

    public static RubyModule createMarshalModule(Ruby runtime) {
        RubyModule module = runtime.defineModule("Marshal");
        CallbackFactory callbackFactory = runtime.callbackFactory();

        module.defineSingletonMethod("dump", callbackFactory.getOptSingletonMethod(RubyMarshal.class, "dump"));
        module.defineSingletonMethod("load", callbackFactory.getOptSingletonMethod(RubyMarshal.class, "load"));
        module.defineSingletonMethod("restore", callbackFactory.getOptSingletonMethod(RubyMarshal.class, "load"));

        return module;
    }

    public static IRubyObject dump(IRubyObject recv, IRubyObject[] args) {
        if (args.length < 1) {
            throw new ArgumentError(recv.getRuntime(), "wrong # of arguments(at least 1)");
        }
        IRubyObject objectToDump = args[0];

        RubyIO io = null;
        int depthLimit = -1;

        if (args.length >= 2) {
            if (args[1] instanceof RubyIO) {
                io = (RubyIO) args[1];
            } else if (args[1] instanceof RubyFixnum) {
                depthLimit = (int) ((RubyFixnum) args[1]).getLongValue();
            }
            if (args.length == 3) {
                depthLimit = (int) ((RubyFixnum) args[2]).getLongValue();
            }
        }

        try {
            if (io != null) {
                dumpToStream(objectToDump, io.getOutStream(), depthLimit);
                return io;
            } else {
                ByteArrayOutputStream stringOutput = new ByteArrayOutputStream();
                dumpToStream(objectToDump, stringOutput, depthLimit);
                return RubyString.newString(recv.getRuntime(), stringOutput.toByteArray());
            }

        } catch (IOException ioe) {
            throw IOError.fromException(recv.getRuntime(), ioe);
        }

    }

    public static IRubyObject load(IRubyObject recv, IRubyObject[] args) {
        try {
            if (args.length < 1) {
                throw new ArgumentError(recv.getRuntime(), "wrong # of arguments(at least 1)");
            }

            // FIXME: handle more parameters

            IRubyObject in = args[0];

            InputStream rawInput;
            if (in instanceof RubyIO) {
                rawInput = ((RubyIO) in).getInStream();
            } else if (in.respondsTo("to_str")) {
                RubyString inString = (RubyString) in.callMethod("to_str");
                rawInput = new ByteArrayInputStream(inString.toByteArray());
            } else {
                throw new TypeError(recv.getRuntime(), "instance of IO needed");
            }

            UnmarshalStream input = new UnmarshalStream(recv.getRuntime(), rawInput);

            return input.unmarshalObject();

        } catch (IOException ioe) {
            throw IOError.fromException(recv.getRuntime(), ioe);
        }
    }

    private static void dumpToStream(IRubyObject object, OutputStream rawOutput, int depthLimit)
        throws IOException
    {
        MarshalStream output = new MarshalStream(object.getRuntime(), rawOutput, depthLimit);
        output.dumpObject(object);
    }
}
