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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.Passwd;
import jnr.posix.util.Platform;

import org.jcodings.Encoding;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.*;
import org.jruby.ast.util.ArgsUtil;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyString.UTF8;
import static org.jruby.util.io.EncodingUtils.newExternalStringWithEncoding;

/**
 * The Ruby built-in class Dir.
 *
 * @author  jvoegele
 */
@JRubyClass(name = "Dir", include = "Enumerable")
public class RubyDir extends RubyObject implements Closeable {
    private RubyString path;       // What we passed to the constructor for method 'path'
    protected FileResource dir;
    private long lastModified = Long.MIN_VALUE;
    private String[] snapshot;     // snapshot of contents of directory
    private int pos;               // current position in directory
    private boolean isOpen = true;
    private Encoding encoding;

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^(uri|jar|file|classpath):([^:]*:)?//?.*");

    public RubyDir(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static RubyClass createDirClass(Ruby runtime) {
        RubyClass dirClass = runtime.defineClass("Dir", runtime.getObject(), RubyDir::new);

        dirClass.setClassIndex(ClassIndex.DIR);
        dirClass.setReifiedClass(RubyDir.class);

        dirClass.includeModule(runtime.getEnumerable());
        dirClass.defineAnnotatedMethods(RubyDir.class);

        return dirClass;
    }

    private final void checkDir() {
        checkDirIgnoreClosed();

        if (!isOpen) throw getRuntime().newIOError("closed directory");
    }

    private final void checkDirIgnoreClosed() {
        testFrozen("Dir");
        // update snapshot (if changed) :
        if (snapshot == null || dir.exists() && dir.lastModified() > lastModified) {
            lastModified = dir.lastModified();
            snapshot = list(dir);
        }
    }

    /**
     * Creates a new <code>Dir</code>.  This method takes a snapshot of the
     * contents of the directory at creation time, so changes to the contents
     * of the directory will not be reflected during the lifetime of the
     * <code>Dir</code> object returned, so a new <code>Dir</code> instance
     * must be created to reflect changes to the underlying file system.
     *
     * @param context current context
     * @param path target path
     * @return a new Dir object
     */
    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject path) {
        return initializeCommon(context, path, context.nil);
    }

    /**
     * Like {@link #initialize(ThreadContext, IRubyObject)} but accepts an :encoding option.
     *
     * @param context current context
     * @param path target path
     * @param encOpts encoding options
     * @return a new Dir object
     */
    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject path, IRubyObject encOpts) {
        return initializeCommon(context, path, encOpts);
    }

    private RubyDir initializeCommon(ThreadContext context, IRubyObject pathArg, IRubyObject encOpts) {
        Ruby runtime = context.runtime;

        Encoding encoding = null;

        if (!encOpts.isNil()) {
            RubyHash opts = encOpts.convertToHash();
            IRubyObject encodingArg = ArgsUtil.extractKeywordArg(context, opts, "encoding");
            if (encodingArg != null && !encodingArg.isNil()) {
                encoding = runtime.getEncodingService().getEncodingFromObject(encodingArg);
            }
        }

        if (encoding == null) encoding = runtime.getEncodingService().getFileSystemEncoding();

        RubyString newPath = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, pathArg));
        this.path = newPath;
        this.pos = 0;

        this.encoding = encoding;

        String adjustedPath = RubyFile.getAdjustedPath(context, newPath);
        checkDirIsTwoSlashesOnWindows(getRuntime(), adjustedPath);

        this.dir = JRubyFile.createResource(context, adjustedPath);
        this.snapshot = getEntries(context, dir, adjustedPath);

        return this;
    }

    @Deprecated
    public IRubyObject initialize19(ThreadContext context, IRubyObject arg) {
        return initialize(context, arg);
    }

// ----- Ruby Class Methods ----------------------------------------------------

    private static ArrayList<ByteList> dirGlobs(ThreadContext context, String cwd, IRubyObject[] args, int flags) {
        ArrayList<ByteList> dirs = new ArrayList<>();

        for ( int i = 0; i < args.length; i++ ) {
            dirs.addAll(Dir.push_glob(context.runtime, cwd, globArgumentAsByteList(context, args[i]), flags));
        }

        return dirs;
    }

    private static RubyArray asRubyStringList(Ruby runtime, List<ByteList> dirs) {
        final int size = dirs.size();
        if ( size == 0 ) return RubyArray.newEmptyArray(runtime);

        IRubyObject[] dirStrings = new IRubyObject[ size ];
        for ( int i = 0; i < size; i++ ) {
            dirStrings[i] = RubyString.newStringNoCopy(runtime, dirs.get(i));
        }
        return RubyArray.newArrayMayCopy(runtime, dirStrings);
    }

    private static String getCWD(Ruby runtime) {
        final String cwd = runtime.getCurrentDirectory();
        // ^(uri|jar|file|classpath):([^:]*:)?//?.*
        if (cwd.startsWith("uri:") || cwd.startsWith("jar:") || cwd.startsWith("file:")) {
            // "classpath:" mapped into "uri:classloader:"
            return cwd;
        }
        try { // NOTE: likely not necessary as we already canonicalized while setting?
            return new JRubyFile(cwd).getCanonicalPath();
        }
        catch (IOException e) {
            return cwd;
        }
    }

    private static final String[] BASE = new String[] { "base" };
    private static final String[] BASE_FLAGS = new String[] { "base", "flags" };

    // returns null (no kwargs present), "" kwargs but no base key, "something" kwargs with base key (which might be "").
    private static String globOptions(ThreadContext context, IRubyObject[] args, int[] flags) {
        Ruby runtime = context.runtime;

        if (args.length > 1) {
            IRubyObject tmp = TypeConverter.checkHashType(runtime, args[args.length - 1]);
            if (tmp == context.nil) {
                if (flags != null) {
                    flags[0] = RubyNumeric.num2int(args[1]);
                }
            } else {
                String[] keys = flags != null ? BASE_FLAGS : BASE;
                IRubyObject[] rets = ArgsUtil.extractKeywordArgs(context, (RubyHash) tmp, keys);
                if (rets[0] == null || rets[0].isNil()) return "";
                RubyString path = RubyFile.get_path(context, rets[0]);
                Encoding[] enc = {path.getEncoding()};
                String base = path.getUnicodeValue();

                // Deep down in glob things are unhappy if base is not absolute.
                if (!base.isEmpty()) {
                    base = RubyFile.expandPath(context, base, enc, runtime.getCurrentDirectory(), true, false);
                }

                if (flags != null) flags[0] = rets[1] == null ? 0 : RubyNumeric.num2int(rets[1]);

                return base;
            }
        }

        return null;
    }

    @JRubyMethod(name = "[]", rest = true, meta = true)
    public static IRubyObject aref(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        String base = globOptions(context, args, null);
        List<ByteList> dirs;

        if (args.length == 1) {
            String dir = base == null || base.isEmpty() ? runtime.getCurrentDirectory() : base;
            dirs = Dir.push_glob(runtime, dir, globArgumentAsByteList(context, args[0]), 0);
        } else {
            IRubyObject[] arefArgs;
            if (base != null) { // kwargs are present
                arefArgs = ArraySupport.newCopy(args, args.length - 1);
            } else {
                arefArgs = args;
                base = "";
            }

            String dir = base.isEmpty() ? runtime.getCurrentDirectory() : base;
            dirs = dirGlobs(context, dir, arefArgs, 0);
        }

        return asRubyStringList(runtime, dirs);
    }

    private static ByteList globArgumentAsByteList(ThreadContext context, IRubyObject arg) {
        return RubyFile.get_path(context, arg).getByteList();
    }

    /**
     * Returns an array of filenames matching the specified wildcard pattern
     * <code>pat</code>. If a block is given, the array is iterated internally
     * with each filename is passed to the block in turn. In this case, Nil is
     * returned.
     */
    @JRubyMethod(required = 1, optional = 2, meta = true)
    public static IRubyObject glob(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        int[] flags = new int[] { 0 };
        String base = globOptions(context, args, flags);
        List<ByteList> dirs;

        if (base != null && !base.isEmpty() && !(JRubyFile.createResource(context, base).exists())){
            dirs = new ArrayList<ByteList>();
        } else {
            IRubyObject tmp = args[0].checkArrayType();
            String dir = base == null || base.isEmpty() ? runtime.getCurrentDirectory() : base;

            if (tmp.isNil()) {
                dirs = Dir.push_glob(runtime, dir, globArgumentAsByteList(context, args[0]), flags[0]);
            } else {
                dirs = dirGlobs(context, dir, ((RubyArray) tmp).toJavaArray(), flags[0]);
            }
        }

        if (block.isGiven()) {
            for (int i = 0; i < dirs.size(); i++) {
                block.yield(context, RubyString.newString(runtime, dirs.get(i)));
            }

            return runtime.getNil();
        }

        return asRubyStringList(runtime, dirs);
    }

    /**
     * @return all entries for this Dir
     */
    @JRubyMethod(name = "entries")
    public RubyArray entries() {
        return newEntryArray(getRuntime(), snapshot, encoding, false);
    }

    /**
     * Returns an array containing all of the filenames in the given directory.
     */
    @JRubyMethod(name = "entries", meta = true)
    public static RubyArray entries(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;

        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, arg));

        return entriesCommon(context, path, runtime.getDefaultFilesystemEncoding(), false);
    }

    @JRubyMethod(name = "entries", meta = true)
    public static RubyArray entries(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject opts) {
        Ruby runtime = context.runtime;

        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, arg));

        Encoding encoding = getEncodingFromOpts(context, opts);

        return entriesCommon(context, path, encoding, false);
    }

    private static RubyArray entriesCommon(ThreadContext context, IRubyObject path, Encoding encoding, final boolean childrenOnly) {
        Ruby runtime = context.runtime;

        String adjustedPath = RubyFile.getAdjustedPath(context, path);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);

        FileResource directory = JRubyFile.createResource(context, adjustedPath);
        String[] files = getEntries(context, directory, adjustedPath);

        return newEntryArray(runtime, files, encoding, childrenOnly);
    }

    private static RubyArray newEntryArray(Ruby runtime, String[] files, Encoding encoding, boolean childrenOnly) {
        RubyArray result = RubyArray.newArray(runtime, files.length);
        for (String file : files) {
            if (childrenOnly) { // removeIf(f -> f.equals(".") || f.equals(".."));
                final int len = file.length();
                if (len == 1 && file.charAt(0) == '.') continue;
                if (len == 2 && file.charAt(0) == '.' && file.charAt(1) == '.') continue;
            }

            result.append(newExternalStringWithEncoding(runtime, file, encoding));
        }
        return result;
    }

    private static final String[] NO_FILES = StringSupport.EMPTY_STRING_ARRAY;

    private static String[] getEntries(ThreadContext context, FileResource dir, String path) {
        if (!dir.isDirectory()) {
            if (dir.exists()) {
                throw context.runtime.newErrnoENOTDIRError(path);
            }
            throw context.runtime.newErrnoENOENTError(path);
        }
        if (!dir.canRead()) throw context.runtime.newErrnoEACCESError(path);

        String[] list = dir.list();

        return list == null ? NO_FILES : list;
    }

    // MRI behavior: just plain '//' or '\\\\' are considered illegal on Windows.
    private static void checkDirIsTwoSlashesOnWindows(Ruby runtime, String path) {
        if (Platform.IS_WINDOWS && ("//".equals(path) || "\\\\".equals(path))) {
            throw runtime.newErrnoEINVALError("Invalid argument - " + path);
        }
    }

    /** Changes the current directory to <code>path</code> */
    @JRubyMethod(optional = 1, meta = true)
    public static IRubyObject chdir(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        RubyString path = args.length == 1 ?
            StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, args[0])) :
            getHomeDirectoryPath(context);

        String adjustedPath = RubyFile.adjustRootPathOnWindows(runtime, path.asJavaString(), null);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);

        adjustedPath = getExistingDir(runtime, adjustedPath).canonicalPath();

        IRubyObject result;
        if (block.isGiven()) {
            final String oldCwd = runtime.getCurrentDirectory();
            // FIXME: Don't allow multiple threads to do this at once
            runtime.setCurrentDirectory(adjustedPath);
            try {
                result = block.yield(context, path);
            } finally {
                getExistingDir(runtime, oldCwd); // needed in case the block deleted the oldCwd
                runtime.setCurrentDirectory(oldCwd);
            }
        } else {
            runtime.setCurrentDirectory(adjustedPath);
            result = runtime.newFixnum(0);
        }

        return result;
    }

    /**
     * Changes the root directory (only allowed by super user).  Not available on all platforms.
     */
    @JRubyMethod(name = "chroot", required = 1, meta = true)
    public static IRubyObject chroot(IRubyObject recv, IRubyObject path) {
        throw recv.getRuntime().newNotImplementedError("chroot not implemented: chroot is non-portable and is not supported.");
    }

    /**
     * Returns an array containing all of the filenames except for "." and ".." in the given directory.
     */
    @JRubyMethod(name = "children")
    public RubyArray children(ThreadContext context) {
        return entriesCommon(context, path, encoding, true);
    }
    
    @JRubyMethod(name = "children", meta = true)
    public static RubyArray children(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return children(context, recv, arg, context.nil);
    }

    @JRubyMethod(name = "children", meta = true)
    public static RubyArray children(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject opts) {
        RubyDir dir = (RubyDir) ((RubyClass) recv).newInstance(context, arg, opts, Block.NULL_BLOCK);

        return dir.children(context);
    }

    /**
     * Deletes the directory specified by <code>path</code>.  The directory must be empty.
     */
    public static IRubyObject rmdir(IRubyObject recv, IRubyObject path) {
        return rmdir19(recv.getRuntime().getCurrentContext(), recv, path);
    }

    @JRubyMethod(name = {"rmdir", "unlink", "delete"}, required = 1, meta = true)
    public static IRubyObject rmdir19(ThreadContext context, IRubyObject recv, IRubyObject path) {
        Ruby runtime = context.runtime;
        RubyString cleanPath = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, path));
        return rmdirCommon(runtime, cleanPath.asJavaString());
    }

    private static RubyFixnum rmdirCommon(Ruby runtime, String path) {
        JRubyFile directory = getDirForRmdir(runtime, path);

        // at this point, only thing preventing delete should be non-emptiness
        if (runtime.getPosix().rmdir(directory.toString()) < 0) {
            throw runtime.newErrnoENOTEMPTYError(path);
        }

        return runtime.newFixnum(0);
    }

    /**
     * Executes the block once for each entry in the directory except
     * for "." and "..", passing the filename of each entry as parameter
     * to the block.
     */
    @JRubyMethod(name = "each_child", meta = true)
    public static IRubyObject each_child(ThreadContext context, IRubyObject recv, IRubyObject arg, Block block) {
        return eachChildCommon(context, recv, RubyFile.get_path(context, arg), null, block);
    }

    /**
     * Executes the block once for each entry in the directory except
     * for "." and "..", passing the filename of each entry as parameter
     * to the block.
     */
    @JRubyMethod(name = "each_child", meta = true)
    public static IRubyObject each_child(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject encOpts, Block block) {
        Encoding encoding = getEncodingFromOpts(context, encOpts);

        return eachChildCommon(context, recv, RubyFile.get_path(context, arg), encOpts, block);
    }

    /**
     * Executes the block once for each file in the directory specified by
     * <code>path</code>.
     */
    @JRubyMethod(name = "foreach", meta = true)
    public static IRubyObject foreach(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        return foreachCommon(context, recv, RubyFile.get_path(context, path), null, block);
    }

    @JRubyMethod(name = "foreach", meta = true)
    public static IRubyObject foreach(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject encOpts, Block block) {
        return foreachCommon(context, recv, RubyFile.get_path(context, path), encOpts, block);
    }

    private static Encoding getEncodingFromOpts(ThreadContext context, IRubyObject encOpts) {
        Ruby runtime = context.runtime;

        Encoding encoding = null;

        if (!encOpts.isNil()) {
            IRubyObject opts = ArgsUtil.getOptionsArg(runtime, encOpts);

            if (opts.isNil()) {
                throw runtime.newArgumentError(2, 1, 1);
            } else {
                IRubyObject encodingArg = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "encoding");
                if (encodingArg != null && !encodingArg.isNil()) {
                    encoding = runtime.getEncodingService().getEncodingFromObject(encodingArg);
                }
            }
        }

        if (encoding == null) encoding = runtime.getDefaultEncoding();

        return encoding;
    }

    private static IRubyObject eachChildCommon(ThreadContext context, IRubyObject recv, RubyString path, IRubyObject encOpts, Block block) {
        final Ruby runtime = context.runtime;

        if (block.isGiven()) {
            Encoding encoding = encOpts == null ? runtime.getDefaultEncoding() : getEncodingFromOpts(context, encOpts);

            RubyDir dir = (RubyDir) runtime.getDir().newInstance(context, path, Block.NULL_BLOCK);

            dir.each_child(context, encoding, block);

            return context.nil;
        }

        if (encOpts == null) {
            return enumeratorize(runtime, recv, "each_child", path);
        }

        return enumeratorize(runtime, recv, "each_child", path, encOpts);
    }

    private static IRubyObject foreachCommon(ThreadContext context, IRubyObject recv, RubyString path, IRubyObject encOpts, Block block) {
        final Ruby runtime = context.runtime;

        if (block.isGiven()) {
            Encoding encoding = encOpts == null ? runtime.getDefaultEncoding() : getEncodingFromOpts(context, encOpts);

            RubyDir dir = (RubyDir) runtime.getDir().newInstance(context, path, Block.NULL_BLOCK);

            dir.each(context, encoding, block);

            return context.nil;
        }

        if (encOpts == null) {
            return enumeratorize(runtime, recv, "foreach", path);
        }

        return enumeratorize(runtime, recv, "foreach", path, encOpts);
    }

    /** Returns the current directory. */
    @JRubyMethod(name = {"getwd", "pwd"}, meta = true)
    public static RubyString getwd(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();

        RubyString pwd = newFilesystemString(runtime, getCWD(runtime));
        pwd.setTaint(true);
        return pwd;
    }

    /**
     * Returns the home directory of the current user or the named user if given.
     */
    @JRubyMethod(name = "home", meta = true)
    public static IRubyObject home(ThreadContext context, IRubyObject recv) {
        return getHomeDirectoryPath(context);
    }

    @JRubyMethod(name = "home", meta = true)
    public static IRubyObject home(ThreadContext context, IRubyObject recv, IRubyObject user) {
        return getHomeDirectoryPath(context, user.convertToString().toString());
    }

    /**
     * Creates the directory specified by <code>path</code>.  Note that the
     * <code>mode</code> parameter is provided only to support existing Ruby
     * code, and is ignored.
     */
    @JRubyMethod(name = "mkdir", required = 1, optional = 1, meta = true)
    public static IRubyObject mkdir(ThreadContext context, IRubyObject recv, IRubyObject... args) {
        Ruby runtime = context.runtime;
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, args[0]));
        return mkdirCommon(runtime, path.asJavaString(), args);
    }

    @Deprecated
    public static IRubyObject mkdir(IRubyObject recv, IRubyObject[] args) {
        return mkdir(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @Deprecated
    public static IRubyObject mkdir19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return mkdir(context, recv, args);
    }

    private static IRubyObject mkdirCommon(Ruby runtime, String path, IRubyObject[] args) {
        if (path.startsWith("uri:")) throw runtime.newErrnoEACCESError(path);

        path = dirFromPath(path, runtime);
        FileResource res = JRubyFile.createResource(runtime, path);
        if (res.exists()) throw runtime.newErrnoEEXISTError(path);

        String name = path.replace('\\', '/');
        boolean startsWithDriveLetterOnWindows = RubyFile.startsWithDriveLetterOnWindows(name);

        // don't attempt to create a dir for drive letters
        if (startsWithDriveLetterOnWindows) {
            // path is just drive letter plus :
            if (path.length() == 2) return RubyFixnum.zero(runtime);
            // path is drive letter plus : plus leading or trailing /
            if (path.length() == 3 && (path.charAt(0) == '/' || path.charAt(2) == '/')) return RubyFixnum.zero(runtime);
            // path is drive letter plus : plus leading and trailing /
            if (path.length() == 4 && (path.charAt(0) == '/' && path.charAt(3) == '/')) return RubyFixnum.zero(runtime);
        }

        File newDir = res.unwrap(File.class);
        if (File.separatorChar == '\\') newDir = new File(newDir.getPath());

        int mode = args.length == 2 ? ((int) args[1].convertToInteger().getLongValue()) : 0777;

        if (runtime.getPosix().mkdir(newDir.getAbsolutePath(), mode) < 0) {
            // FIXME: This is a system error based on errno
            throw runtime.newSystemCallError("mkdir failed");
        }

        return RubyFixnum.zero(runtime);
    }

    /**
     * Returns a new directory object for <code>path</code>.  If a block is
     * provided, a new directory object is passed to the block, which closes the
     * directory object before terminating.
     */
    @JRubyMethod(name = "open", meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        RubyDir directory = (RubyDir) context.runtime.getDir().newInstance(context, path, Block.NULL_BLOCK);

        if (!block.isGiven()) return directory;

        try {
            return block.yield(context, directory);
        } finally {
            directory.close();
        }
    }

    @JRubyMethod(name = "open", meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject encOpts, Block block) {
        RubyDir directory = (RubyDir) context.runtime.getDir().newInstance(context, path, encOpts, Block.NULL_BLOCK);

        if (!block.isGiven()) return directory;

        try {
            return block.yield(context, directory);
        } finally {
            directory.close();
        }
    }

    @Deprecated
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        return open(context, recv, path, block);
    }

// ----- Ruby Instance Methods -------------------------------------------------

    /**
     * Closes the directory stream.
     */
    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        close();
        return context.nil;
    }

    public final void close() {
        // Make sure any read()s after close fail.
        checkDirIgnoreClosed();
        isOpen = false;
    }

    /**
     * Executes the block once for each entry in the directory.
     */
    public IRubyObject each(ThreadContext context, Encoding enc, Block block) {
        checkDir();

        String[] contents = snapshot;
        for (pos = 0; pos < contents.length; pos++) {
            block.yield(context, RubyString.newString(context.runtime, contents[pos], enc));
        }

        return this;
    }

    /**
     * Executes the block once for each entry in the directory.
     */
    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, Block block) {
        return block.isGiven() ? each(context, encoding, block) : enumeratorize(context.runtime, this, "each");
    }

    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject encOpts, Block block) {
        Encoding encoding = getEncodingFromOpts(context, encOpts);

        return block.isGiven() ? each(context, encoding, block) : enumeratorize(context.runtime, this, "each", encOpts);
    }

    @Deprecated
    public IRubyObject each19(ThreadContext context, Block block) {
        return each(context, block);
    }

    @Deprecated
    public IRubyObject each19(ThreadContext context, IRubyObject encoding, Block block) {
        return each(context, encoding, block);
    }

    /**
     * Executes the block once for each child in the directory
     * (i.e. all the directory entries except for "." and "..").
     */
    public IRubyObject each_child(ThreadContext context, Encoding enc, Block block) {
        checkDir();

        String[] contents = snapshot;
        for (pos = 0; pos < contents.length; pos++) {
            if (StringSupport.contentEquals(contents[pos], '.')) continue; /* current dir */
            if (StringSupport.contentEquals(contents[pos], '.', '.')) continue; /* parent dir */
            block.yield(context, RubyString.newString(context.runtime, contents[pos], enc));
        }

        return this;
    }

    /**
     * Executes the block once for each child in the directory
     * (i.e. all the directory entries except for "." and "..").
     */
    public IRubyObject each_child(ThreadContext context, Block block) {
        return each_child(context, encoding, block);
    }

    @JRubyMethod(name = "each_child")
    public IRubyObject rb_each_child(ThreadContext context, Block block) {
        if (block.isGiven()) {
            return each_child(context, block);
        }

        return enumeratorize(context.runtime, children(context), "each");
    }

    @Override
    @JRubyMethod
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        StringBuilder part = new StringBuilder();
        String cname = getMetaClass().getRealClass().getName();
        part.append("#<").append(cname).append(':');
        if (path != null) { part.append(path.asJavaString()); }
        part.append('>');

        return runtime.newString(part.toString());
    }

    /**
     * Returns the current position in the directory.
     */
    @JRubyMethod(name = {"tell", "pos"})
    public RubyInteger tell() {
        checkDir();
        return getRuntime().newFixnum(pos);
    }

    /**
     * Moves to a position <code>d</code>.  <code>pos</code> must be a value
     * returned by <code>tell</code> or 0.
     */

    @JRubyMethod(name = "seek", required = 1)
    public IRubyObject seek(IRubyObject newPos) {
        checkDir();

        set_pos(newPos);
        return this;
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject newPos) {
        int pos2 = RubyNumeric.fix2int(newPos);
        if (pos2 >= 0) this.pos = pos2;
        return newPos;
    }

    @JRubyMethod(name = {"path", "to_path"})
    public IRubyObject path(ThreadContext context) {
        return path == null ? context.nil : path.strDup(context.runtime);
    }

    @JRubyMethod
    public IRubyObject to_path(ThreadContext context) {
        return path(context);
    }

    public String getPath() {
        if (path == null) return null;
        return path.asJavaString();
    }

    /** Returns the next entry from this directory. */
    @JRubyMethod(name = "read")
    public IRubyObject read() {
        checkDir();

        final String[] snapshot = this.snapshot;
        if (pos >= snapshot.length) return getRuntime().getNil();

        RubyString result = RubyString.newString(getRuntime(), snapshot[pos], encoding);
        pos++;
        return result;
    }

    /** Moves position in this directory to the first entry. */
    @JRubyMethod(name = "rewind")
    public IRubyObject rewind() {
        checkDir();

        pos = 0;
        return this;
    }

    @JRubyMethod(name = "empty?", meta = true)
    public static IRubyObject empty_p(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, arg));
        RubyFileStat fileStat = runtime.newFileStat(path.asJavaString(), false);
        boolean isDirectory = fileStat.directory_p().isTrue();
        return runtime.newBoolean(isDirectory && entries(context, recv, arg).getLength() <= 2);
    }

    @JRubyMethod(name = "exist?", meta = true)
    public static IRubyObject exist(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        // Capture previous exception if any.
        IRubyObject exception = runtime.getGlobalVariables().get("$!");
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, arg));

        try {
            return runtime.newFileStat(path.asJavaString(), false).directory_p();
        } catch (Exception e) {
            // Restore $!
            runtime.getGlobalVariables().set("$!", exception);
            return runtime.newBoolean(false);
        }
    }

    @JRubyMethod(name = "exists?", meta = true)
    public static IRubyObject exists_p(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        if (context.runtime.warningsEnabled()) {
            context.runtime.getWarnings().warn("Dir.exists? is a deprecated name, use Dir.exist? instead");
        }

        return exist(context, recv, arg);
    }

    @JRubyMethod(name = "fileno", notImplemented = true)
    public IRubyObject fileno(ThreadContext context) {
        throw context.runtime.newNotImplementedError("Dir#fileno");
    }

// ----- Helper Methods --------------------------------------------------------
    /** Returns a Java <code>File</code> object for the specified path.  If
     * <code>path</code> is not a directory, throws <code>IOError</code>.
     *
     * @param   path path for which to return the <code>File</code> object.
     * @param   mustExist is true the directory must exist.  If false it must not.
     */ // split out - no longer used
    protected static FileResource getDir(final Ruby runtime, final String path, final boolean mustExist) {
        String dir = dirFromPath(path, runtime);

        FileResource result = JRubyFile.createResource(runtime, dir);

        if (mustExist && (result == null || !result.exists())) {
            throw runtime.newErrnoENOENTError(dir);
        }

        boolean isDirectory = result.isDirectory();

        if (mustExist && !isDirectory) {
            throw runtime.newErrnoENOTDIRError(path);
        }

        if (!mustExist && isDirectory) {
            throw runtime.newErrnoEEXISTError(dir);
        }

        return result;
    }

    private static FileResource getExistingDir(final Ruby runtime, final String path) {
        FileResource result = JRubyFile.createResource(runtime, path);
        if (result == null || !result.exists()) {
            throw runtime.newErrnoENOENTError(path);
        }
        if (!result.isDirectory()) {
            throw runtime.newErrnoENOTDIRError(path);
        }
        return result;
    }

    /**
     * Similar to getDir, but performs different checks to match rmdir behavior.
     * @param runtime
     * @param path
     * @return
     */
    protected static JRubyFile getDirForRmdir(final Ruby runtime, final String path) {
        String dir = dirFromPath(path, runtime);

        JRubyFile directory = JRubyFile.create(runtime.getCurrentDirectory(), dir);

        // Order is important here...File.exists() will return false if the parent
        // dir can't be read, so we check permissions first

        // no permission
        File parentFile = directory.getParentFile();
        if (parentFile.exists() && ! parentFile.canWrite()) {
            throw runtime.newErrnoEACCESError(path);
        }

        // Since we transcode we depend on posix to lookup stat stuff since
        // java.io.File does not seem to cut it.  A failed stat will throw ENOENT.
        FileStat stat = runtime.getPosix().stat(directory.toString());

        // is not directory
        if (!stat.isDirectory()) throw runtime.newErrnoENOTDIRError(path);

        return directory;
    }

    private static String dirFromPath(final String path, final Ruby runtime) throws RaiseException {
        String dir = path;
        String[] pathParts = RubyFile.splitURI(path);
        if (pathParts != null) {
            if (pathParts[0].startsWith("file:") && pathParts[1].length() > 0 && pathParts[1].indexOf(".jar!/") == -1) {
                dir = pathParts[1];
            } else {
                throw runtime.newErrnoENOTDIRError(dir);
            }
        }
        return dir;
    }

    private static String[] list(FileResource directory) {
        final String[] contents = directory.list();
        // If an IO exception occurs (something odd, but possible)
        // A directory may return null.
        return contents == null ? NO_FILES : contents;
    }

    /**
     * @deprecated no longer used
     */
    protected static List<String> getContents(FileResource directory) {
        final String[] contents = directory.list();

        final List<String> result;
        // If an IO exception occurs (something odd, but possible)
        // A directory may return null.
        if (contents != null) {
            result = Arrays.asList(contents);
        }
        else {
             result = Collections.emptyList();
        }
        return result;
    }

    /**
     * @deprecated no longer used
     */
    protected static List<RubyString> getContents(FileResource directory, Ruby runtime) {
        final String[] contents = directory.list();

        final List<RubyString> result;
        if (contents != null) {
            result = new ArrayList<>(contents.length);
            for (int i = 0; i < contents.length; i++) {
                result.add( runtime.newString(contents[i]) );
            }
        }
        else {
            result = Collections.emptyList();
        }

        return result;
    }

    /**
     * Returns the home directory of the specified <code>user</code> on the
     * system. If the home directory of the specified user cannot be found,
     * an <code>ArgumentError it thrown</code>.
     */
    public static IRubyObject getHomeDirectoryPath(ThreadContext context, String user) {
        /*
         * TODO: This version is better than the hackish previous one. Windows
         *       behavior needs to be defined though. I suppose this version
         *       could be improved more too.
         * TODO: /etc/passwd is also inadequate for MacOSX since it does not
         *       use /etc/passwd for regular user accounts
         */
        Ruby runtime = context.runtime;
        POSIX posix = runtime.getNativePosix();

        if (posix != null) {
            try {
                // try to use POSIX for this first
                Passwd passwd = runtime.getPosix().getpwnam(user);
                if (passwd != null) {
                    String home = passwd.getHome();
                    return newFilesystemString(runtime, home);
                }
            } catch (Exception e) {
            }
        }

        // fall back on other ways

        if (Platform.IS_WINDOWS) {
            if (user.equals(posix.getlogin())) {
                return getHomeDirectoryPath(context);
            }
        } else {
            String passwd;
            try {
                FileInputStream stream = new FileInputStream("/etc/passwd");
                int readBytes = stream.available();
                byte[] bytes = new byte[readBytes];
                readBytes = stream.read(bytes);
                stream.close();
                passwd = new String(bytes, 0, readBytes);
            } catch (IOException ioe) {
                return runtime.getNil();
            }

            List<String> rows = StringSupport.split(passwd, '\n');
            for (int i = 0; i < rows.size(); i++) {
                List<String> fields = StringSupport.split(rows.get(i), ':');
                if (fields.get(0).equals(user)) {
                    String home = fields.get(5);
                    return newFilesystemString(runtime, home);
                }
            }
        }

        throw runtime.newArgumentError("user " + user + " doesn't exist");
    }

    private static RubyString newFilesystemString(Ruby runtime, String home) {
        return newExternalStringWithEncoding(runtime, home, runtime.getDefaultFilesystemEncoding());
    }

    static final ByteList HOME = new ByteList(new byte[] {'H','O','M','E'}, false);

    public static RubyString getHomeDirectoryPath(ThreadContext context) {
        final RubyString homeKey = RubyString.newStringShared(context.runtime, HOME);
        return getHomeDirectoryPath(context, context.runtime.getENV().op_aref(context, homeKey));
    }

    public static Optional<String> getHomeFromEnv(Ruby runtime) {
        final RubyString homeKey = RubyString.newStringShared(runtime, HOME);
        final RubyHash env = runtime.getENV();

        if (env.has_key_p(homeKey).isFalse()) {
            return Optional.empty();
        } else {
            return Optional.of(env.op_aref(runtime.getCurrentContext(), homeKey).toString());
        }
    }

    private static final ByteList user_home = new ByteList(new byte[] {'u','s','e','r','.','h','o','m','e'}, false);

    static RubyString getHomeDirectoryPath(ThreadContext context, IRubyObject home) {
        final Ruby runtime = context.runtime;

        if (home == null || home == context.nil) {
            IRubyObject ENV_JAVA = runtime.getObject().getConstant("ENV_JAVA");
            home = ENV_JAVA.callMethod(context, "[]", RubyString.newString(runtime, user_home, UTF8));
        }

        if (home == null || home == context.nil) {
            home = context.runtime.getENV().op_aref(context, runtime.newString("LOGDIR"));
        }

        if (home == null || home == context.nil) {
            throw runtime.newArgumentError("user.home/LOGDIR not set");
        }

        return (RubyString) home.dup();
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target == File.class) {
            final String path = getPath();
            return path == null ? null : (T) new File(path);
        }
        if (target == Path.class || target == Watchable.class) {
            final String path = getPath();
            return path == null ? null : (T) FileSystems.getDefault().getPath(path);
        }
        return super.toJava(target);
    }

    @Deprecated
    public static RubyArray entries19(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return entries(context, recv, arg);
    }

    @Deprecated
    public static RubyArray entries19(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject opts) {
        return entries(context, recv, arg, opts);
    }

    @Deprecated
    public static IRubyObject home(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length > 0 && args[0] != context.nil) return getHomeDirectoryPath(context, args[0].toString());

        return getHomeDirectoryPath(context);
    }

    @Deprecated
    public static IRubyObject foreach19(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        return foreachCommon(context, recv, RubyFile.get_path(context, path), null, block);
    }

    @Deprecated
    public static IRubyObject foreach19(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject enc, Block block) {
        return foreachCommon(context, recv, RubyFile.get_path(context, path), RubyHash.newKwargs(context.runtime, "encoding", enc), block);
    }

    @Deprecated
    public static RubyArray entries(IRubyObject recv, IRubyObject path) {
        return entries(recv.getRuntime().getCurrentContext(), recv, path);
    }

    @Deprecated
    public static RubyArray entries(IRubyObject recv, IRubyObject path, IRubyObject arg, IRubyObject opts) {
        return entries(recv.getRuntime().getCurrentContext(), recv, path, opts);
    }

}
