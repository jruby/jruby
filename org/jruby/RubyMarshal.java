/*
 * RubyMarshal.java
 * Created on Mar 20 22:20:56 2002
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

package org.jruby;

import java.io.*;
import java.util.*;
import org.jruby.runtime.CallbackFactory;
import org.jruby.exceptions.*;
import org.jruby.marshal.*;

/**
 * Marshal module
 *
 * @author Anders
 * @version $Revision$
 */
public class RubyMarshal {

    public static RubyModule createMarshalModule(Ruby ruby) {
        RubyModule marshalModule = ruby.defineModule("Marshal");

        marshalModule.defineSingletonMethod("dump", CallbackFactory.getOptSingletonMethod(RubyMarshal.class, "dump"));
        marshalModule.defineSingletonMethod("load", CallbackFactory.getOptSingletonMethod(RubyMarshal.class, "load"));
        marshalModule.defineSingletonMethod("restore", CallbackFactory.getOptSingletonMethod(RubyMarshal.class, "load"));
        return marshalModule;
    }

    public static RubyObject dump(Ruby ruby, RubyObject recv, RubyObject[] args) {
        if (args.length < 1) {
            throw new ArgumentError(ruby, "wrong # of arguments(at least 1)");
        }
        RubyObject objectToDump = args[0];

        try {
            if (args.length >= 2) {
                RubyIO io = (RubyIO) args[1];
                MarshalStream output = new MarshalStream(io.getOutStream());
                output.dumpObject(objectToDump);
                return io;

            } else {
                ByteArrayOutputStream stringOutput = new ByteArrayOutputStream();
                MarshalStream output = new MarshalStream(stringOutput);
                output.dumpObject(objectToDump);
                return RubyString.newString(ruby, stringOutput.toString());
            }

        } catch (IOException ioe) {
            RubyBugException exception = new RubyBugException(ioe.getMessage());
            exception.initCause(ioe);
            throw exception;
        }

    }

    public static RubyObject load(Ruby ruby, RubyObject recv, RubyObject[] args) {
        try {
            if (args.length < 1) {
                throw new ArgumentError(ruby, "wrong # of arguments(at least 1)");
            }

            // FIXME: handle more parameters

            RubyObject in = args[0];  // ..... could be either an IO or something with to_str() ...
            if (in instanceof RubyIO) {
                throw new NotImplementedError();
            }
            String inString = in.toString(); // FIXME: should use to_str()

            InputStream rawInput = new ByteArrayInputStream(inString.getBytes());
            UnmarshalStream input = new UnmarshalStream(ruby, rawInput);

            return input.unmarshalObject();

        } catch (IOException ioe) {
            // FIXME: throw appropriate ruby exception ..
            RubyBugException exception = new RubyBugException(ioe.getMessage());
            exception.initCause(ioe);
            throw exception;
        }
    }
}
