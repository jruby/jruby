/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import java.io.InputStream;
import java.io.OutputStream;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;

import org.jruby.util.ByteList;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;
import org.jruby.util.io.TransparentByteArrayOutputStream;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;

/**
 * Marshal module
 *
 * @author Anders
 */
@JRubyModule(name="Marshal")
public class RubyMarshal {

    public static RubyModule createMarshalModule(ThreadContext context) {
        return defineModule(context, "Marshal").
                defineMethods(context, RubyMarshal.class).
                defineConstant(context, "MAJOR_VERSION", asFixnum(context, Constants.MARSHAL_MAJOR)).
                defineConstant(context, "MINOR_VERSION", asFixnum(context, Constants.MARSHAL_MINOR));
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject dump(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return dumpCommon(context, object, null, -1);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject dump(ThreadContext context, IRubyObject recv, IRubyObject object, IRubyObject ioOrLimit) {
        IRubyObject io = null;
        int depthLimit = -1;

        if (ioOrLimit instanceof RubyIO || sites(context).respond_to_write.respondsTo(context, ioOrLimit, ioOrLimit)) {
            io = ioOrLimit;
        } else if (ioOrLimit instanceof RubyFixnum fixnum) {
            depthLimit = fixnum.asInt(context);
        } else {
            throw typeError(context, "Instance of IO needed");
        }

        return dumpCommon(context, object, io, depthLimit);
    }

    @JRubyMethod(module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject dump(ThreadContext context, IRubyObject recv, IRubyObject object, IRubyObject io, IRubyObject limit) {
        if (!(io instanceof RubyIO || sites(context).respond_to_write.respondsTo(context, io, io))) {
            throw typeError(context, "Instance of IO needed");
        }

        int depthLimit = toInt(context, limit);

        return dumpCommon(context, object, io, depthLimit);
    }

    private static IRubyObject dumpCommon(ThreadContext context, IRubyObject objectToDump, IRubyObject io, int depthLimit) {
        OutputStream outputStream;
        TransparentByteArrayOutputStream stringOutput = null;

        if (io != null) {
            if (io instanceof RubyIO rubyIO) {
                outputStream = rubyIO.getOutStream();
            } else {
                outputStream = outputStream(context, io);
            }
        } else {
            outputStream = stringOutput = new TransparentByteArrayOutputStream();
        }

        dumpToStream(context, objectToDump, outputStream, depthLimit);

        return io != null ? io :
                newString(context, new ByteList(stringOutput.getRawBytes(), 0, stringOutput.size(), false));
    }

    @JRubyMethod(name = {"load", "restore"}, required = 1, optional = 2, checkArity = false, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject load(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        int argc = Arity.checkArgumentCount(context, args, 1, 3);
        IRubyObject in = args[0];
        boolean freeze = false;
        IRubyObject proc = null;

        if (argc > 1) {
            RubyHash kwargs = ArgsUtil.extractKeywords(args[argc - 1]);
            if (kwargs != null) {
                IRubyObject freezeOpt = ArgsUtil.getFreezeOpt(context, kwargs);
                freeze = freezeOpt != null && freezeOpt.isTrue();
                if (argc > 2) proc = args[1];
            } else {
                proc = args[1];
            }
        }

        final IRubyObject str = in.checkStringType();
        InputStream rawInput;
        if (str instanceof RubyString string) {
            if (string.size() == 0) {
                throw argumentError(context, "marshal data too short");
            }
            ByteList bytes = string.getByteList();
            rawInput = new ByteArrayInputStream(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        } else if (sites(context).respond_to_getc.respondsTo(context, in, in) &&
                    sites(context).respond_to_read.respondsTo(context, in, in)) {
            rawInput = inputStream(context, in);
        } else {
            throw typeError(context, "instance of IO needed");
        }

        MarshalLoader loader = new MarshalLoader(context, freeze, proc);
        RubyInputStream rubyIn = new RubyInputStream(context.runtime, rawInput);
        loader.start(context, rubyIn);
        return loader.unmarshalObject(context, rubyIn);
    }

    private static InputStream inputStream(ThreadContext context, IRubyObject in) {
        setBinmodeIfPossible(context, in);
        return new IOInputStream(in, false); // respond_to?(:read) already checked
    }

    private static OutputStream outputStream(ThreadContext context, IRubyObject out) {
        setBinmodeIfPossible(context, out);
        return new IOOutputStream(out, true, false); // respond_to?(:write) already checked
    }

    private static void dumpToStream(ThreadContext context, IRubyObject object, OutputStream rawOutput, int depthLimit) {
        MarshalDumper output = new MarshalDumper(depthLimit);
        RubyOutputStream out = new RubyOutputStream(context.runtime, rawOutput);

        output.start(out);
        output.dumpObject(context, out, object);
    }

    private static void setBinmodeIfPossible(ThreadContext context, IRubyObject io) {
        if (sites(context).respond_to_binmode.respondsTo(context, io, io)) {
            sites(context).binmode.call(context, io, io);
        }
    }

    /**
     * Convenience method for objects that are undumpable. Always throws (a TypeError).
     */
    public static IRubyObject undumpable(ThreadContext context, RubyObject self) {
        throw typeError(context, "can't dump ", self, "");
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject dump(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return dump(((RubyBasicObject) recv).getCurrentContext(), recv, args, unusedBlock);
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject dump(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        int argc = Arity.checkArgumentCount(context, args, 1, 3);
        IRubyObject objectToDump = args[0];
        int depthLimit = -1;

        return switch (argc) {
            case 1 -> dump(context, recv, args[0]);
            case 2 -> dump(context, recv, args[0], args[1]);
            case 3 -> dump(context, recv, args[0], args[1], args[2]);
            default -> dumpCommon(context, objectToDump, null, depthLimit);
        };
    }

    private static JavaSites.MarshalSites sites(ThreadContext context) {
        return context.sites.Marshal;
    }

}
