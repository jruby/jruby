/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
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

    @JRubyMethod(required = 1, optional = 2, module = true)
    public static IRubyObject dump(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        Ruby runtime = recv.getRuntime();
        IRubyObject objectToDump = args[0];
        IRubyObject io = null;
        int depthLimit = -1;

        if (args.length >= 2) {
            if (args[1].respondsTo("write")) {
                io = args[1];
            } else if (args[1] instanceof RubyFixnum) {
                depthLimit = (int) ((RubyFixnum) args[1]).getLongValue();
            } else {
                throw runtime.newTypeError("Instance of IO needed");
            }
            if (args.length == 3) {
                depthLimit = (int) args[2].convertToInteger().getLongValue();
            }
        }

        try {
            if (io != null) {
                dumpToStream(runtime, objectToDump, outputStream(runtime.getCurrentContext(), io), depthLimit);
                return io;
            }
            
            ByteArrayOutputStream stringOutput = new ByteArrayOutputStream();
            boolean[] taintUntrust = dumpToStream(runtime, objectToDump, stringOutput, depthLimit);
            RubyString result = RubyString.newString(runtime, new ByteList(stringOutput.toByteArray()));
            
            if (taintUntrust[0]) result.setTaint(true);
            if (taintUntrust[1]) result.setUntrusted(true);

            return result;
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }

    }

    private static OutputStream outputStream(ThreadContext context, IRubyObject out) {
        setBinmodeIfPossible(context, out);
        return new IOOutputStream(out);
    }

    private static void setBinmodeIfPossible(ThreadContext context, IRubyObject io) {
        if (io.respondsTo("binmode")) io.callMethod(context, "binmode");
    }

    @JRubyMethod(name = {"load", "restore"}, required = 1, optional = 1, module = true)
    public static IRubyObject load(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        Ruby runtime = context.runtime;
        IRubyObject in = args[0];
        IRubyObject proc = args.length == 2 ? args[1] : null;
        
        try {
            InputStream rawInput;
            boolean tainted;
            boolean untrusted;
            IRubyObject v = in.checkStringType();
            
            if (!v.isNil()) {
                tainted = in.isTaint();
                untrusted = in.isUntrusted();
                ByteList bytes = ((RubyString) v).getByteList();
                rawInput = new ByteArrayInputStream(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
            } else if (in.respondsTo("getc") && in.respondsTo("read")) {
                tainted = true;
                untrusted = true;
                rawInput = inputStream(context, in);
            } else {
                throw runtime.newTypeError("instance of IO needed");
            }

            return new UnmarshalStream(runtime, rawInput, proc, tainted, untrusted).unmarshalObject();
        } catch (EOFException e) {
            if (in.respondsTo("to_str")) throw runtime.newArgumentError("marshal data too short");

            throw runtime.newEOFError();
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    private static InputStream inputStream(ThreadContext context, IRubyObject in) {
        setBinmodeIfPossible(context, in);
        return new IOInputStream(in);
    }

    private static boolean[] dumpToStream(Ruby runtime, IRubyObject object, OutputStream rawOutput,
            int depthLimit) throws IOException {
        MarshalStream output = new MarshalStream(runtime, rawOutput, depthLimit);
        output.dumpObject(object);
        return new boolean[] {output.isTainted(), output.isUntrusted()};
    }
}
