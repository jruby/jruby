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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2007 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

import org.jruby.util.ByteList;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;

/**
 * Marshal module
 *
 * @author Anders
 */
@JRubyModule(name="Marshal")
public class RubyMarshal {

    public static RubyModule createMarshalModule(Ruby runtime) {
        RubyModule module = runtime.defineModule("Marshal");
        runtime.setMarshal(module);

        module.defineAnnotatedMethods(RubyMarshal.class);
        module.defineConstant("MAJOR_VERSION", runtime.newFixnum(Constants.MARSHAL_MAJOR));
        module.defineConstant("MINOR_VERSION", runtime.newFixnum(Constants.MARSHAL_MINOR));

        return module;
    }

    @JRubyMethod(name = "dump", required = 1, optional = 2, frame = true, module = true)
    public static IRubyObject dump(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        if (args.length < 1) {
            throw recv.getRuntime().newArgumentError("wrong # of arguments(at least 1)");
        }
        IRubyObject objectToDump = args[0];

        IRubyObject io = null;
        int depthLimit = -1;

        if (args.length >= 2) {
            if (args[1].respondsTo("write")) {
                io = args[1];
            } else if (args[1] instanceof RubyFixnum) {
                depthLimit = (int) ((RubyFixnum) args[1]).getLongValue();
            } else {
                throw recv.getRuntime().newTypeError("Instance of IO needed");
            }
            if (args.length == 3) {
                depthLimit = (int) args[2].convertToInteger().getLongValue();
            }
        }

        try {
            if (io != null) {
                dumpToStream(objectToDump, outputStream(io), depthLimit);
                return io;
            }
            
            ByteArrayOutputStream stringOutput = new ByteArrayOutputStream();
            dumpToStream(objectToDump, stringOutput, depthLimit);

            return RubyString.newString(recv.getRuntime(), new ByteList(stringOutput.toByteArray(),false));

        } catch (IOException ioe) {
            throw recv.getRuntime().newIOErrorFromException(ioe);
        }

    }

    private static OutputStream outputStream(IRubyObject out) {
        setBinmodeIfPossible(out);
        if (out instanceof RubyIO) {
            return ((RubyIO) out).getOutStream();
        }
        return new IOOutputStream(out);
    }

    private static void setBinmodeIfPossible(IRubyObject io) {
        if (io.respondsTo("binmode")) {
            io.callMethod(io.getRuntime().getCurrentContext(), "binmode");
        }
    }

    @JRubyMethod(name = {"load", "restore"}, required = 1, optional = 1, frame = true, module = true)
    public static IRubyObject load(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        try {
            if (args.length < 1) {
                throw recv.getRuntime().newArgumentError("wrong number of arguments (0 for 1)");
            }
            
            if (args.length > 2) {
            	throw recv.getRuntime().newArgumentError("wrong number of arguments (" + args.length + " for 2)");
            }
            
            IRubyObject in = null;
            IRubyObject proc = null;

            switch (args.length) {
            case 2:
            	proc = args[1];
            case 1:
            	in = args[0];
            }

            InputStream rawInput;
            if (in != null && in.respondsTo("read")) {
                rawInput = inputStream(in);
            } else if (in != null && in.respondsTo("to_str")) {
                RubyString inString = (RubyString) RuntimeHelpers.invoke(context, in, "to_str");
                ByteList bytes = inString.getByteList();
                rawInput = new ByteArrayInputStream(bytes.unsafeBytes(), bytes.begin(), bytes.length());
            } else {
                throw recv.getRuntime().newTypeError("instance of IO needed");
            }
            
            UnmarshalStream input = new UnmarshalStream(recv.getRuntime(), rawInput, proc);

            return input.unmarshalObject();

        } catch (EOFException ee) {
            throw recv.getRuntime().newEOFError();
        } catch (IOException ioe) {
            throw recv.getRuntime().newIOErrorFromException(ioe);
        }
    }

    private static InputStream inputStream(IRubyObject in) {
        setBinmodeIfPossible(in);
        if (in instanceof RubyIO) {
            return ((RubyIO) in).getInStream();
        }
        return new IOInputStream(in);
    }

    private static void dumpToStream(IRubyObject object, OutputStream rawOutput, int depthLimit)
        throws IOException
    {
        MarshalStream output = new MarshalStream(object.getRuntime(), rawOutput, depthLimit);
        output.dumpObject(object);
    }
}
