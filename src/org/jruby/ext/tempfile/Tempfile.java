/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004-2009 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.ext.tempfile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;
import org.jruby.util.PhantomReferenceReaper;
import org.jruby.util.io.EncodingOption;
import org.jruby.util.io.IOOptions;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;

/**
 * An implementation of tempfile.rb in Java.
 */
@JRubyClass(name="Tempfile", parent="File")
@SuppressWarnings("deprecation")
public class Tempfile extends org.jruby.RubyTempfile {

    /** Keep strong references to the Reaper until cleanup */
    private static final ConcurrentMap<Reaper, Boolean> referenceSet
            = new ConcurrentHashMap<Reaper, Boolean>();
    private transient volatile Reaper reaper;

    private static ObjectAllocator TEMPFILE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFile instance = new Tempfile(runtime, klass);

            return instance;
        }
    };

    public static RubyClass createTempfileClass(Ruby runtime) {
        RubyClass tempfileClass = runtime.defineClass("Tempfile", runtime.getFile(), TEMPFILE_ALLOCATOR);

        RubyKernel.require(tempfileClass, runtime.newString("tmpdir"), Block.NULL_BLOCK);

        tempfileClass.defineAnnotatedMethods(Tempfile.class);

        return tempfileClass;
    }

    private final static String DEFAULT_TMP_DIR;
    private static final Object tmpFileLock = new Object();
    private static int counter = -1;
    private final static java.util.Random RND = new java.util.Random();

    static {
        String tmpDir;
        if (Platform.IS_WINDOWS) {
           tmpDir = System.getProperty("java.io.tmpdir");
           if (tmpDir == null) tmpDir = System.getenv("TEMP");
           if (tmpDir == null) tmpDir = System.getenv("TMP");
           if (tmpDir == null) tmpDir = "C:\\Windows\\Temp";
        } else {
            tmpDir = "/tmp";
        }
        DEFAULT_TMP_DIR = tmpDir;
    }

    private File tmpFile = null;

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public Tempfile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(required = 1, optional = 1, visibility = PRIVATE, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        Ruby runtime = getRuntime();
        IRubyObject basename = args[0];
        IRubyObject dir = defaultTmpDir(runtime, args);

        File tmp = null;
        synchronized(tmpFileLock) {
            while (true) {
                try {
                    if (counter == -1) {
                        counter = RND.nextInt() & 0xffff;
                    }
                    counter++;

                    // We do this b/c make_tmpname might be overridden
                    IRubyObject tmpname = callMethod(runtime.getCurrentContext(),
                                                     "make_tmpname", new IRubyObject[] {basename, runtime.newFixnum(counter)});
                    tmp = JRubyFile.create(getRuntime().getCurrentDirectory(),
                                           new File(dir.convertToString().toString(), tmpname.convertToString().toString()).getPath());
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
                        initializeOpen();
                        referenceSet.put(reaper = new Reaper(this, runtime, tmpFile, openFile), Boolean.TRUE);
                        return this;
                    }
                } catch (IOException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            }
        }
    }

    @JRubyMethod(required = 1, optional = 2, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject initialize19(ThreadContext context, IRubyObject[] args, Block block) {
        RubyHash options = null;

        // check for trailing hash
        if (args.length > 1) {
            if (args[args.length - 1] instanceof RubyHash) {
                options = (RubyHash)args[args.length - 1];
                args = Arrays.copyOfRange(args, 0, args.length - 1);
            }
        }

        initialize(args, block);

        if (options != null) {
            EncodingOption encodingOption = EncodingOption.getEncodingOptionFromObject(options);
            if (encodingOption != null) {
                setEncodingFromOptions(encodingOption);
            }
        }

        return this;
    }

    private IRubyObject defaultTmpDir(Ruby runtime, IRubyObject[] args) {
        IRubyObject dir = null;
        if (args.length == 2) {
            dir = args[1];
        } else {
            // Dir::tmpdir
            runtime.getLoadService().require("tmpdir");
            dir = runtime.getDir().callMethod(runtime.getCurrentContext(), "tmpdir");
        }
        if (runtime.getSafeLevel() > 0 && dir.isTaint()) {
            dir = runtime.newString(DEFAULT_TMP_DIR);
        }
        return dir;
    }

    private void initializeOpen() {
        Ruby runtime = getRuntime();

        IOOptions ioOptions = newIOOptions(runtime, ModeFlags.RDWR | ModeFlags.EXCL);
        getRuntime().getPosix().chmod(path, 0600);
        sysopenInternal(path, ioOptions.getModeFlags(), 0600);
    }

    /**
     * Compatibility with Tempfile#make_tmpname(basename, n) in MRI
     */
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject make_tmpname(ThreadContext context, IRubyObject basename, IRubyObject n, Block block) {
        Ruby runtime = context.getRuntime();
        IRubyObject[] newargs = new IRubyObject[5];

        IRubyObject base, suffix;
        if (basename instanceof RubyArray) {
            RubyArray array = (RubyArray) basename;
            int length = array.getLength();

            base = length > 0 ? array.eltInternal(0) : runtime.getNil();
            suffix = length > 0 ? array.eltInternal(1) : runtime.getNil();
        } else {
            base = basename;
            suffix = runtime.newString("");
        }

        newargs[0] = runtime.newString("%s.%d.%d%s");
        newargs[1] = base;
        newargs[2] = runtime.getGlobalVariables().get("$$"); // PID
        newargs[3] = n;
        newargs[4] = suffix;
        return callMethod(context, "sprintf", newargs);
    }

    @JRubyMethod(visibility = PUBLIC)
    public IRubyObject open() {
        if (!isClosed()) close();

        openInternal(path, "r+");

        return this;
    }

    @JRubyMethod(visibility = PROTECTED)
    public IRubyObject _close(ThreadContext context) {
        return !isClosed() ? super.close() : context.getRuntime().getNil();
    }

    @JRubyMethod(optional = 1, visibility = PUBLIC)
    public IRubyObject close(ThreadContext context, IRubyObject[] args, Block block) {
        boolean unlink = args.length == 1 ? args[0].isTrue() : false;
        return unlink ? close_bang(context) : _close(context);
    }

    @JRubyMethod(name = "close!", visibility = PUBLIC)
    public IRubyObject close_bang(ThreadContext context) {
         referenceSet.remove(reaper);
         reaper.released = true;
        _close(context);
        tmpFile.delete();
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = {"unlink", "delete"})
    public IRubyObject unlink(ThreadContext context) {
        // JRUBY-6688: delete when closed, warn otherwise
        if (isClosed()) {
            // the user intends to delete the file immediately, so do it
            if (!tmpFile.exists() || tmpFile.delete()) {
                referenceSet.remove(reaper);
                reaper.released = true;
                path = null;
            }
        } else {
            // else, no-op, since we can't unlink the file without breaking stat et al
            context.runtime.getWarnings().warn("Tempfile#unlink or delete called on open file; ignoring");
        }
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = {"size", "length"})
    public IRubyObject size(ThreadContext context) {
        if (!isClosed()) {
            flush();
            return context.getRuntime().newFileStat(path, false).size();
        }

        return RubyFixnum.zero(context.getRuntime());
    }

    @JRubyMethod(required = 1, optional = 1, meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
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
        Ruby runtime = context.getRuntime();
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

    // FIXME This reaper never actually runs; delete it or implement it properly
    // For JRUBY-6477, I add the finalize above to do basically what this was
    // intended to do.
    private static final class Reaper extends PhantomReferenceReaper<Tempfile> implements Runnable {
        private volatile boolean released = false;
        private final Ruby runtime;
        private final File tmpFile;
        private final OpenFile openFile;

        Reaper(Tempfile file, Ruby runtime, File tmpFile, OpenFile openFile) {
            super(file);
            this.runtime = runtime;
            this.tmpFile = tmpFile;
            this.openFile = openFile;
        }

        public final void run() {
            referenceSet.remove(this);
            release();
            clear();
        }

        final void release() {
            if (!released) {
                released = true;
                if (openFile != null) {
                    openFile.cleanup(runtime, false);
                }
                if (tmpFile.exists()) {
                    boolean deleted = tmpFile.delete();
                    if (runtime.getDebug().isTrue()) {
                        String msg = "removing " + tmpFile.getPath() + " ... ";
                        if (deleted) {
                            runtime.getErr().println(msg + "done");
                        } else {
                            runtime.getErr().println(msg + "can't delete");
                        }
                    }
                }
            }
        }
    }
}
