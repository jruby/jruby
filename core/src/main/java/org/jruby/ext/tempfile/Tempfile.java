/*
 ***** BEGIN LICENSE BLOCK *****
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

import java.io.File;
import java.io.IOException;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockCallback;
import org.jruby.runtime.CallBlock19;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.IOOptions;
import org.jruby.util.io.ModeFlags;

/**
 * An implementation of tempfile.rb in Java.
 */
@JRubyClass(name="Tempfile", parent="File")
public class Tempfile extends org.jruby.RubyFile {
    private static ObjectAllocator TEMPFILE_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFile instance = new Tempfile(runtime, klass);

            return instance;
        }
    };

    public static RubyClass createTempfileClass(Ruby runtime) {
        RubyClass tempfileClass = runtime.defineClass("Tempfile", runtime.getFile(), TEMPFILE_ALLOCATOR);

        tempfileClass.defineAnnotatedMethods(Tempfile.class);

        return tempfileClass;
    }

    private File tmpFile = null;
    protected IRubyObject opts;

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public Tempfile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(required = 1, optional = 1, visibility = PRIVATE, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        return initializeCommon(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(required = 1, optional = 2, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject initialize19(ThreadContext context, IRubyObject[] args, Block block) {
        return initializeCommon(context, args);
    }
    
    private IRubyObject initializeCommon(ThreadContext context, IRubyObject[] args) {
        BlockCallback body = new TempfileCallback();
        
        // #create and #make_tmpname come from Dir::Tmpname, included into
        // tempfile in lib/ruby/shared/tempfile.rb. We use create here to
        // match filename algorithm and allow them to be overridden.
        callMethod(context, "create", args, CallBlock19.newCallClosure(this, this.getMetaClass(), Arity.OPTIONAL, body, context));
        
        return context.nil;
    }
    
    private class TempfileCallback implements BlockCallback {
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
            Ruby runtime = context.runtime;
            
            IRubyObject tmpname = args[0];
            IOOptions ioOptions = newIOOptions(runtime, ModeFlags.RDWR | ModeFlags.EXCL);
            
            if (context.is19) {
                // check for trailing hash
                if (args.length > 1) {
                    if (args[args.length - 1] instanceof RubyHash) {
                        // TODO: encoding options do not appear to actually get passed through to file init logic
                        RubyHash options = (RubyHash)args[args.length - 1];
                        ioOptions = updateIOOptionsFromOptions(context, options, ioOptions);
                        EncodingUtils.ioExtractEncodingOption(context, Tempfile.this, options, null);
                    }
                }
            }

            try {
                File tmp = new File(tmpname.convertToString().toString());
                if (tmp.createNewFile()) {
                    tmpFile = tmp;
                    path = tmp.getPath();
                    try {
                        tmpFile.deleteOnExit();
                    } catch (NullPointerException npe) {
                        // See JRUBY-4624.
                        // Due to JDK bug, NPE could be thrown
                        // when shutdown is in progress.
                        // Do nothing.
                    } catch (IllegalStateException ise) {
                        // do nothing, shutdown in progress
                    }
                    initializeOpen(ioOptions);
                } else {
                    throw context.runtime.newErrnoEEXISTError(path);
                }
            } catch (IOException e) {
                throw context.runtime.newIOErrorFromException(e);
            }

            return context.nil;
        }
    }

    private void initializeOpen(IOOptions ioOptions) {
        getRuntime().getPosix().chmod(path, 0600);
        MakeOpenFile();
        
        openFile.setMode(ioOptions.getModeFlags().getOpenFileFlags());
        openFile.setPath(path);
            
        sysopenInternal19(path, ioOptions.getModeFlags().getOpenFileFlags(), 0600);
    }

    @JRubyMethod(visibility = PUBLIC)
    public IRubyObject open() {
        if (!isClosed()) ioClose(getRuntime());

        openInternal(path, openFile.getModeAsString(getRuntime()));

        return this;
    }

    @JRubyMethod(visibility = PROTECTED)
    public IRubyObject _close(ThreadContext context) {
        return !isClosed() ? super.close() : context.runtime.getNil();
    }

    @JRubyMethod(optional = 1, visibility = PUBLIC)
    public IRubyObject close(ThreadContext context, IRubyObject[] args, Block block) {
        boolean unlink = args.length == 1 ? args[0].isTrue() : false;
        return unlink ? close_bang(context) : _close(context);
    }

    @JRubyMethod(name = "close!", visibility = PUBLIC)
    public IRubyObject close_bang(ThreadContext context) {
        _close(context);
        tmpFile.delete();
        return context.runtime.getNil();
    }

    @JRubyMethod(name = {"unlink", "delete"})
    public IRubyObject unlink(ThreadContext context) {
        // JRUBY-6688: delete when closed, warn otherwise
        if (isClosed()) {
            // the user intends to delete the file immediately, so do it
            if (!tmpFile.exists() || tmpFile.delete()) {
                path = null;
            }
        } else {
            // else, no-op, since we can't unlink the file without breaking stat et al
            context.runtime.getWarnings().warn("Tempfile#unlink or delete called on open file; ignoring");
        }
        return context.runtime.getNil();
    }

    @JRubyMethod(name = {"size", "length"}, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject size(ThreadContext context) {
        if (!isClosed()) {
            flush();
            return context.runtime.newFileStat(path, false).size();
        }

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod(name = {"size", "length"}, compat = CompatVersion.RUBY1_9)
    public IRubyObject size19(ThreadContext context) {
        if (!isClosed()) {
            flush();
        }
        return context.runtime.newFileStat(path, false).size();
    }

    @JRubyMethod(required = 1, optional = 1, meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        RubyClass klass = (RubyClass) recv;
        Tempfile tempfile = (Tempfile) klass.newInstance(context, args, block);

        if (block.isGiven()) {
            try {
                block.yield(context, tempfile);
            } finally {
                if (!tempfile.isClosed()) tempfile.close();
            }
            return runtime.getNil();
        }

        return tempfile;
    }

    @JRubyMethod(required = 1, optional = 1, meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
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
    @Override
    public IRubyObject inspect() {
        StringBuilder val = new StringBuilder();
        val.append("#<Tempfile:").append(path);
        if(!openFile.isOpen()) {
            val.append(" (closed)");
        }
        val.append(">");
        return getRuntime().newString(val.toString());
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            tmpFile.delete();
        }
    }
}
