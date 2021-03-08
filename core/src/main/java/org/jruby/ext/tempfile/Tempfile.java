/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004-2009 Thomas E Enebo <enebo@acm.org>
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

package org.jruby.ext.tempfile;

import jnr.constants.platform.Errno;
import jnr.constants.platform.OpenFlags;
import jnr.posix.POSIX;
import org.jruby.Finalizable;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFile;
import org.jruby.RubyFileStat;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.RubySystemCallError;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;
import java.io.IOException;

/**
 * An implementation of tempfile.rb in Java.
 */
@JRubyClass(name="Tempfile", parent="File")
public class Tempfile extends RubyFile implements Finalizable {
    public static RubyClass createTempfileClass(Ruby runtime) {
        RubyClass tempfileClass = runtime.defineClass("Tempfile", runtime.getFile(), Tempfile::new);

        tempfileClass.defineAnnotatedMethods(Tempfile.class);

        return tempfileClass;
    }

    private File tmpFile = null;
    private IRubyObject tmpname;
    private IRubyObject opts;
    private IRubyObject mode;

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public Tempfile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(optional = 4, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block unused) {
        if (args.length == 0) {
            args = new IRubyObject[] { RubyString.newEmptyString(context.runtime) };
        }

        IRubyObject Tmpname = context.runtime.getDir().getConstant("Tmpname", false);
        Block block = CallBlock19.newCallClosure(this, getMetaClass(), Signature.OPTIONAL, new TempfileCallback(), context);
        context.sites.Tempfile.create.call(context, Tmpname, Tmpname, args, block);
        // ::Dir::Tmpname.create(basename, tmpdir=nil, **options) { |tmpname, n, opts| ... }

        // GH#1905: don't use JDK's deleteOnExit because it grows a set without bounds
        context.runtime.addInternalFinalizer(Tempfile.this);
        
        return context.nil;
    }
    
    private class TempfileCallback implements BlockCallback {
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
            Ruby runtime = context.runtime;
            IRubyObject tmpname = args[0], opts = args.length > 2 ? args[2] : context.nil;

            // MRI uses CREAT also, but we create the file ourselves before opening it
            int mode = OpenFlags.O_RDWR.intValue() /*| OpenFlags.O_CREAT.intValue()*/ | OpenFlags.O_EXCL.intValue();
            IRubyObject perm = runtime.newFixnum(0600);

            // check for trailing options hash and prepare it
            if (!opts.isNil()) {
                RubyHash options = (RubyHash)opts;
                IRubyObject optsMode = options.delete(context, runtime.newSymbol("mode"), Block.NULL_BLOCK);
                if (!optsMode.isNil()) {
                    mode |= optsMode.convertToInteger().getIntValue();
                }
                options.op_aset(context, runtime.newSymbol("perm"), perm);
            } else {
                opts = perm;
            }

            try {
                // This is our logic for creating a delete-on-exit tmpfile using JDK features

                File tmp = new File(tmpname.convertToString().toString());
                if (tmp.createNewFile()) {
                    runtime.getPosix().chmod(tmp.getAbsolutePath(), 0600);
                    tmpFile = tmp;
                } else {
                    throw context.runtime.newErrnoEEXISTError(getPath());
                }
            } catch (IOException e) {
                throw context.runtime.newIOErrorFromException(e);
            }

            // Logic from tempfile.rb starts again here

            // let RubyFile do its init logic to open the channel
            Tempfile.super.initialize(context, new IRubyObject[] { tmpname, runtime.newFixnum(mode), opts }, Block.NULL_BLOCK);
            Tempfile.this.tmpname = tmpname;

            Tempfile.this.mode = runtime.newFixnum(mode & ~(OpenFlags.O_CREAT.intValue() | OpenFlags.O_EXCL.intValue()));
            Tempfile.this.opts = opts;

            return opts;
        }
    }

    @JRubyMethod
    public IRubyObject open(ThreadContext context) {
        if (!isClosed()) rbIoClose(context);

        // MRI doesn't do this, but we need to reset to blank slate
        openFile = null;

        Tempfile.super.initialize(context, new IRubyObject[]{tmpname, mode, opts}, Block.NULL_BLOCK);

        return this;
    }

    @JRubyMethod(visibility = Visibility.PROTECTED)
    public IRubyObject _close(ThreadContext context) {
        return !isClosed() ? super.close(context) : context.nil;
    }

    @JRubyMethod(optional = 1)
    public IRubyObject close(ThreadContext context, IRubyObject[] args, Block block) {
        boolean unlink = args.length == 1 ? args[0].isTrue() : false;
        return unlink ? close_bang(context) : _close(context);
    }

    @JRubyMethod(name = "close!")
    public IRubyObject close_bang(ThreadContext context) {
        _close(context);
        unlink(context);
        return context.nil;
    }

    @JRubyMethod(name = {"unlink", "delete"})
    public IRubyObject unlink(ThreadContext context) {
        if (openFile.getPath() == null) return context.nil;
        
        Ruby runtime = context.runtime;
        POSIX posix = runtime.getPosix();

        if (posix.isNative() && !Platform.IS_WINDOWS) {
            IRubyObject oldExc = context.runtime.getGlobalVariables().get("$!"); // Save $!
            try {
                RubyFile.unlink(context, this);
            } catch (RaiseException re) {
                RubyException excp = re.getException();
                if (!(excp instanceof RubySystemCallError)) throw re;

                int errno = (int)((RubySystemCallError)excp).errno().convertToInteger().getLongValue();
                if (errno != Errno.ENOENT.intValue() && errno != Errno.EACCES.intValue()) {
                    throw re;
                }
                context.runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
            }
            openFile.setPath(null);
            tmpname = context.nil;
        } else {
            // JRUBY-6688: delete when closed, warn otherwise
            if (isClosed()) {
                // the user intends to delete the file immediately, so do it
                if (!tmpFile.exists() || tmpFile.delete()) {
                    openFile.setPath(null);
                    tmpname = context.nil;
                }
            } else {
                // else, no-op, since we can't unlink the file without breaking stat et al
                context.runtime.getWarnings().warn("Tempfile#unlink or delete called on open file; ignoring");
            }
        }
        return context.nil;
    }

    @JRubyMethod(name = {"size", "length"})
    @Override
    public IRubyObject size(ThreadContext context) {
        if (!isClosed()) {
            flush(context);
            RubyFileStat stat = (RubyFileStat)stat(context);
            return stat.size();
        } else if (tmpname != null && !tmpname.isNil()) {
            RubyFileStat stat = (RubyFileStat)stat(context, getMetaClass(), tmpname);
            return stat.size();
        } else {
            return RubyFixnum.zero(context.runtime);
        }
    }

    @Deprecated
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return open(context, recv, args, block);
    }

    @JRubyMethod(optional = 4, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass) recv;
        Tempfile tempfile = (Tempfile) klass.newInstance(context, args, block);

        if (block.isGiven()) {
            try {
                return block.yield(context, tempfile);
            } finally {
                if (!tempfile.isClosed()) tempfile.close();
            }
        } else {
            return tempfile;
        }
    }

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        return super.inspect(context);
    }

    @Override
    public void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            tmpFile.delete();
        }
    }

}
