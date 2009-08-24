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
package org.jruby;

import java.io.File;
import java.io.IOException;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.ModeFlags;

/**
 * An implementation of tempfile.rb in Java.
 */
@JRubyClass(name="Tempfile", parent="File")
public class RubyTempfile extends RubyFile {
    private static ObjectAllocator TEMPFILE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFile instance = new RubyTempfile(runtime, klass);

            instance.setMetaClass(klass);

            return instance;
        }
    };

    public static RubyClass createTempfileClass(Ruby runtime) {
        RubyClass tempfileClass = runtime.defineClass("Tempfile", runtime.getFile(), TEMPFILE_ALLOCATOR);

        RubyKernel.require(tempfileClass, runtime.newString("tmpdir"), Block.NULL_BLOCK);

        tempfileClass.defineAnnotatedMethods(RubyTempfile.class);

        return tempfileClass;
    }

    private final static String DEFAULT_TMP_DIR;
    private static final Object tmpFileLock = new Object();
    private static int counter = -1;

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
    public RubyTempfile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(required = 1, optional = 1, frame = true, visibility = Visibility.PRIVATE)
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
                        counter = new java.util.Random().nextInt() & 0xffff;
                    }
                    counter++;

                    // We do this b/c make_tmpname might be overridden
                    IRubyObject tmpname = callMethod(runtime.getCurrentContext(),
                                                     "make_tmpname", new IRubyObject[] {basename, runtime.newFixnum(counter)});
                    tmp = new File(dir.convertToString().toString(), tmpname.convertToString().toString());
                    if (tmp.createNewFile()) {
                        tmpFile = tmp;
                        path = tmp.getPath();
                        tmpFile.deleteOnExit();
                        initializeOpen();
                        return this;
                    }
                } catch (IOException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            }
        }
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
        try {
            ModeFlags modeFlags = new ModeFlags(ModeFlags.RDWR | ModeFlags.EXCL);
            getRuntime().getPosix().chmod(path, 0600);
            sysopenInternal(path, modeFlags, 0600);
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
    }

    /**
     * Compatibility with Tempfile#make_tmpname(basename, n) in MRI
     */
    @JRubyMethod(frame = true, required = 2, visibility = Visibility.PRIVATE)
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

    @JRubyMethod(frame = true, visibility = Visibility.PUBLIC)
    public IRubyObject open() {
        if (!isClosed()) close();
        try {
            openInternal(path, "r+");
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        }

        return this;
    }

    @JRubyMethod(frame = true, visibility = Visibility.PROTECTED)
    public IRubyObject _close(ThreadContext context) {
        return !isClosed() ? super.close() : context.getRuntime().getNil();
    }

    @JRubyMethod(optional = 1, frame = true, visibility = Visibility.PUBLIC)
    public IRubyObject close(ThreadContext context, IRubyObject[] args, Block block) {
        boolean unlink = args.length == 1 ? args[0].isTrue() : false;
        return unlink ? close_bang(context) : _close(context);
    }

    @JRubyMethod(name = "close!", frame = true, visibility = Visibility.PUBLIC)
    public IRubyObject close_bang(ThreadContext context) {
        _close(context);
        tmpFile.delete();
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = {"unlink", "delete"}, frame = true)
    public IRubyObject unlink(ThreadContext context) {
        if (tmpFile.exists()) tmpFile.delete();
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = {"size", "length"}, frame = true)
    public IRubyObject size(ThreadContext context) {
        if (!isClosed()) {
            flush();
            return context.getRuntime().newFileStat(path, false).size();
        }

        return RubyFixnum.zero(context.getRuntime());
    }

    @JRubyMethod(required = 1, optional = 1, frame = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
        RubyClass klass = (RubyClass) recv;
        RubyTempfile tempfile = (RubyTempfile) klass.newInstance(context, args, block);

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
}
