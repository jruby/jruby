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

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.Passwd;
import jnr.posix.util.Platform;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.api.Create;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.*;
import org.jruby.ast.util.ArgsUtil;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.RubyFile.filePathConvert;
import static org.jruby.RubyString.UTF8;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Check.checkEmbeddedNulls;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.util.RubyStringBuilder.str;
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

    public RubyDir(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static RubyClass createDirClass(ThreadContext context, RubyClass Object, RubyModule Enumerable) {
        return defineClass(context, "Dir", Object, RubyDir::new).
                reifiedClass(RubyDir.class).
                classIndex(ClassIndex.DIR).
                include(context, Enumerable).
                defineMethods(context, RubyDir.class);
    }

    private void checkDir() {
        checkDirIgnoreClosed();

        if (!isOpen) throw getRuntime().newIOError("closed directory");
    }

    private void checkDirIgnoreClosed() {
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
        Encoding encoding = null;

        if (!encOpts.isNil()) {
            RubyHash opts = encOpts.convertToHash();
            IRubyObject encodingArg = ArgsUtil.extractKeywordArg(context, opts, "encoding");
            if (encodingArg != null && !encodingArg.isNil()) {
                encoding = context.runtime.getEncodingService().getEncodingFromObject(encodingArg);
            }
        }

        if (encoding == null) encoding = context.runtime.getEncodingService().getFileSystemEncoding();

        RubyString newPath = checkEmbeddedNulls(context, RubyFile.get_path(context, pathArg));
        this.path = newPath;
        this.pos = 0;
        this.encoding = encoding;

        String adjustedPath = RubyFile.getAdjustedPath(context, newPath);
        checkDirIsTwoSlashesOnWindows(getRuntime(), adjustedPath);

        this.dir = JRubyFile.createResource(context, adjustedPath);
        this.snapshot = getEntries(context, dir, adjustedPath);

        return this;
    }

// ----- Ruby Class Methods ----------------------------------------------------

    private static ArrayList<ByteList> dirGlobs(ThreadContext context, String cwd, IRubyObject[] args, int flags, boolean sort) {
        ArrayList<ByteList> dirs = new ArrayList<>();

        for ( int i = 0; i < args.length; i++ ) {
            dirs.addAll(Dir.push_glob(context.runtime, cwd, globArgumentAsByteList(context, RubyFile.get_path(context, args[i])), flags, sort));
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

    private static final String[] BASE_KEYWORDS = new String[] { "base", "sort" };
    private static final String[] BASE_FLAGS_KEYWORDS = new String[] { "base", "sort", "flags" };

    private static class GlobOptions {
        String base = null;
        int flags = 0;
        boolean sort = true;
    }

    // returns null (no kwargs present), "" kwargs but no base key, "something" kwargs with base key (which might be "").
    private static void globOptions(ThreadContext context, IRubyObject[] args, String[] keys, GlobOptions options) {
        Ruby runtime = context.runtime;

        if (args.length > 1) {
            IRubyObject tmp = TypeConverter.checkHashType(runtime, args[args.length - 1]);
            boolean processFlags = keys == BASE_FLAGS_KEYWORDS;
            if (tmp == context.nil) {
                if (processFlags) options.flags = RubyNumeric.num2int(args[1]);
            } else {
                IRubyObject[] rets = ArgsUtil.extractKeywordArgs(context, (RubyHash) tmp, keys);

                if (args.length == 3 && processFlags) options.flags = RubyNumeric.num2int(args[1]);
                if (processFlags && rets[2] != null) options.flags |= RubyNumeric.num2int(rets[2]);

                if (rets[1] != null) {
                    if (!(rets[1] instanceof RubyBoolean)) {
                        throw argumentError(context, str(runtime, "expected true or false as sort:", rets[1]));
                    }
                    options.sort = !runtime.getFalse().equals(rets[1]); // weirdly only explicit false is honored for sort.
                }

                if (rets[0] == null || rets[0].isNil()) {
                    options.base = "";
                    return;
                }

                RubyString path = RubyFile.get_path(context, rets[0]);
                Encoding[] enc = {path.getEncoding()};
                String base = path.getUnicodeValue();

                // Deep down in glob things are unhappy if base is not absolute.
                if (!base.isEmpty()) {
                    base = RubyFile.expandPath(context, base, enc, runtime.getCurrentDirectory(), true, false);
                }

                options.base = base;
            }
        }
    }

    @JRubyMethod(name = "[]", rest = true, meta = true, keywords = true)
    public static IRubyObject aref(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        GlobOptions options = new GlobOptions();
        globOptions(context, args, BASE_KEYWORDS, options);
        List<ByteList> dirs;
        String base = options.base;

        if (args.length == 1) {
            String dir = base == null || base.isEmpty() ? runtime.getCurrentDirectory() : base;
            dirs = Dir.push_glob(runtime, dir, globArgumentAsByteList(context, args[0]), 0, options.sort);
        } else {
            IRubyObject[] arefArgs;
            if (base != null) { // kwargs are present
                arefArgs = ArraySupport.newCopy(args, args.length - 1);
            } else {
                arefArgs = args;
                base = "";
            }

            String dir = base.isEmpty() ? runtime.getCurrentDirectory() : base;
            dirs = dirGlobs(context, dir, arefArgs, 0, options.sort);
        }

        return asRubyStringList(runtime, dirs);
    }

    private static ByteList globArgumentAsByteList(ThreadContext context, IRubyObject arg) {
        RubyString str;
        if (!(arg instanceof RubyString)) {
            str = RubyFile.get_path(context, arg);
        } else if (StringSupport.strNullCheck(arg)[0] == null) {
            throw argumentError(context, "nul-separated glob pattern is deprecated");
        } else {
            str = (RubyString) arg;
            // FIXME: It is possible this can just be EncodingUtils.strCompatAndValid() but the spec says specifically it must be ascii compat which is more constrained than that method.
            if (!str.getEncoding().isAsciiCompatible()) {
                throw context.runtime.newEncodingCompatibilityError("incompatible character encodings: " + str.getEncoding() + " and " + USASCIIEncoding.INSTANCE);
            }
        }
        return filePathConvert(context, str).getByteList();
    }

    /**
     * Returns an array of filenames matching the specified wildcard pattern
     * <code>pat</code>. If a block is given, the array is iterated internally
     * with each filename is passed to the block in turn. In this case, Nil is
     * returned.
     */
    @JRubyMethod(required = 1, optional = 2, checkArity = false, meta = true)
    public static IRubyObject glob(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 1, 3);

        Ruby runtime = context.runtime;
        GlobOptions options = new GlobOptions();
        globOptions(context, args, BASE_FLAGS_KEYWORDS, options);
        List<ByteList> dirs;
        String base = options.base;

        if (base != null && !base.isEmpty() && !(JRubyFile.createResource(context, base).exists())){
            dirs = new ArrayList<>();
        } else {
            IRubyObject tmp = args[0].checkArrayType();
            String dir = base == null || base.isEmpty() ? runtime.getCurrentDirectory() : base;

            if (tmp.isNil()) {
                dirs = Dir.push_glob(runtime, dir, globArgumentAsByteList(context, args[0]), options.flags, options.sort);
            } else {
                dirs = dirGlobs(context, dir, ((RubyArray) tmp).toJavaArray(context), options.flags, options.sort);
            }
        }

        if (block.isGiven()) {
            for (int i = 0; i < dirs.size(); i++) {
                block.yield(context, newString(context, dirs.get(i)));
            }

            return context.nil;
        }

        return asRubyStringList(runtime, dirs);
    }

    /**
     * @return all entries for this Dir
     */
    @JRubyMethod(name = "entries")
    public RubyArray entries() {
        return newEntryArray(getRuntime().getCurrentContext(), snapshot, encoding, false);
    }

    /**
     * Returns an array containing all of the filenames in the given directory.
     */
    @JRubyMethod(name = "entries", meta = true)
    public static RubyArray entries(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RubyString path = checkEmbeddedNulls(context, RubyFile.get_path(context, arg));

        return entriesCommon(context, path, context.runtime.getDefaultFilesystemEncoding(), false);
    }

    @JRubyMethod(name = "entries", meta = true)
    public static RubyArray entries(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject opts) {
        RubyString path = checkEmbeddedNulls(context, RubyFile.get_path(context, arg));
        Encoding encoding = getEncodingFromOpts(context, opts);

        return entriesCommon(context, path, encoding, false);
    }

    private static RubyArray entriesCommon(ThreadContext context, IRubyObject path, Encoding encoding, final boolean childrenOnly) {
        Ruby runtime = context.runtime;

        String adjustedPath = RubyFile.getAdjustedPath(context, path);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);

        FileResource directory = JRubyFile.createResource(context, adjustedPath);
        String[] files = getEntries(context, directory, adjustedPath);

        return newEntryArray(context, files, encoding, childrenOnly);
    }

    private static RubyArray newEntryArray(ThreadContext context, String[] files, Encoding encoding, boolean childrenOnly) {
        var result = Create.newRawArray(context, files.length);
        for (String file : files) {
            if (childrenOnly) { // removeIf(f -> f.equals(".") || f.equals(".."));
                final int len = file.length();
                if (len == 1 && file.charAt(0) == '.') continue;
                if (len == 2 && file.charAt(0) == '.' && file.charAt(1) == '.') continue;
            }

            result.append(context, newExternalStringWithEncoding(context.runtime, file, encoding));
        }
        return result.finishRawArray(context);
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
    @JRubyMethod(meta = true)
    public static IRubyObject chdir(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        return chdirCommon(context, block, checkEmbeddedNulls(context, RubyFile.get_path(context, path)));
    }

    /** Changes the current directory to <code>path</code> */
    @JRubyMethod(meta = true)
    public static IRubyObject chdir(ThreadContext context, IRubyObject recv, Block block) {
        RubyHash env = context.runtime.getENV();

        if (env.op_aref(context, newString(context, "LOG_DIR")).isNil() &&
                env.op_aref(context, newString(context, "HOME")).isNil()){
            throw argumentError(context, "HOME/LOGDIR not set");
        }

        RubyString path = getHomeDirectoryPath(context);

        return chdirCommon(context, block, path);
    }

    private static IRubyObject chdirCommon(ThreadContext context, Block block, RubyString path) {
        Ruby runtime = context.runtime;
        String adjustedPath = RubyFile.adjustRootPathOnWindows(runtime, path.asJavaString(), null);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);

        adjustedPath = getExistingDir(runtime, adjustedPath).canonicalPath();

        if (runtime.getChdirThread() != null && context.getThread() != runtime.getChdirThread()) {
            throw runtimeError(context, "conflicting chdir during another chdir block");
        }

        if(!block.isGiven() && runtime.getChdirThread() != null) {
            context.runtime.getWarnings().warn("conflicting chdir during another chdir block");
        }

        IRubyObject result;

        if (block.isGiven()) {
            runtime.setChdirThread(context.getThread());
            final String oldCwd = runtime.getCurrentDirectory();

            runtime.setCurrentDirectory(adjustedPath);
            try {
                result = block.yield(context, path);
            } finally {
                runtime.setChdirThread(null);
                getExistingDir(runtime, oldCwd); // needed in case the block deleted the oldCwd
                runtime.setCurrentDirectory(oldCwd);
            }
        } else {
            runtime.setCurrentDirectory(adjustedPath);
            result = asFixnum(context, 0);
        }

        return result;
    }

    @JRubyMethod(name = "chdir")
    public IRubyObject chdir(ThreadContext context) {
        return chdir(context, this.getMetaClass(), path, Block.NULL_BLOCK);
    }

    /**
     * Changes the root directory (only allowed by super user).  Not available on all platforms.
     */
    @JRubyMethod(name = "chroot", meta = true, notImplemented = true)
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

    @Deprecated
    public static IRubyObject rmdir19(IRubyObject recv, IRubyObject path) {
        return rmdir(recv.getRuntime().getCurrentContext(), recv, path);
    }

    /**
     * Deletes the directory specified by <code>path</code>.  The directory must be empty.
     */
    @JRubyMethod(name = {"rmdir", "unlink", "delete"}, meta = true)
    public static IRubyObject rmdir(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return rmdirCommon(context, checkEmbeddedNulls(context, RubyFile.get_path(context, path)).asJavaString());
    }

    private static RubyFixnum rmdirCommon(ThreadContext context, String path) {
        JRubyFile directory = getDirForRmdir(context.runtime, path);

        // at this point, only thing preventing delete should be non-emptiness
        if (context.runtime.getPosix().rmdir(directory.toString()) < 0) {
            throw context.runtime.newErrnoENOTEMPTYError(path);
        }

        return asFixnum(context, 0);
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
        Encoding encoding = null;

        if (!encOpts.isNil()) {
            IRubyObject opts = ArgsUtil.getOptionsArg(context, encOpts);
            if (opts.isNil()) throw argumentError(context, 2, 1, 1);

            IRubyObject encodingArg = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "encoding");
            if (encodingArg != null && !encodingArg.isNil()) {
                encoding = context.runtime.getEncodingService().getEncodingFromObject(encodingArg);
            }
        }

        if (encoding == null) encoding = context.runtime.getDefaultEncoding();

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

        return encOpts == null ?
                enumeratorize(runtime, recv, "each_child", path) :
                enumeratorize(runtime, recv, "each_child", path, encOpts);
    }

    private static IRubyObject foreachCommon(ThreadContext context, IRubyObject recv, RubyString path, IRubyObject encOpts, Block block) {
        final Ruby runtime = context.runtime;

        if (block.isGiven()) {
            Encoding encoding = encOpts == null ? runtime.getDefaultEncoding() : getEncodingFromOpts(context, encOpts);

            RubyDir dir = (RubyDir) runtime.getDir().newInstance(context, path, Block.NULL_BLOCK);

            dir.each(context, encoding, block);

            return context.nil;
        }

        return encOpts == null ?
                enumeratorize(runtime, recv, "foreach", path) :
                enumeratorize(runtime, recv, "foreach", path, encOpts);
    }

    /** Returns the current directory. */
    @JRubyMethod(name = {"getwd", "pwd"}, meta = true)
    public static RubyString getwd(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();

        return newFilesystemString(runtime, getCWD(runtime));
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
        if (user == null || user.isNil() || user.toString().isEmpty()) {
            return getHomeDirectoryPath(context);
        }

        RubyString userString = user.convertToString();

        userString.verifyAsciiCompatible();

        return getHomeDirectoryPath(context, userString.toString());
    }

    /**
     * Creates the directory specified by <code>path</code>.  Note that the
     * <code>mode</code> parameter is provided only to support existing Ruby
     * code, and is ignored.
     */
    @JRubyMethod(name = "mkdir", required = 1, optional = 1, checkArity = false, meta = true)
    public static IRubyObject mkdir(ThreadContext context, IRubyObject recv, IRubyObject... args) {
        Arity.checkArgumentCount(context, args, 1, 2);
        return mkdirCommon(context, RubyFile.get_path(context, args[0]).asJavaString(), args);
    }

    @Deprecated
    public static IRubyObject mkdir(IRubyObject recv, IRubyObject[] args) {
        return mkdir(recv.getRuntime().getCurrentContext(), recv, args);
    }

    private static IRubyObject mkdirCommon(ThreadContext context, String path, IRubyObject[] args) {
        if (path.startsWith("uri:")) throw context.runtime.newErrnoEACCESError(path);

        path = dirFromPath(path, context.runtime);
        FileResource res = JRubyFile.createResource(context.runtime, path);
        if (res.exists()) throw context.runtime.newErrnoEEXISTError(path);

        String name = path.replace('\\', '/');
        boolean startsWithDriveLetterOnWindows = RubyFile.startsWithDriveLetterOnWindows(name);

        // don't attempt to create a dir for drive letters
        if (startsWithDriveLetterOnWindows) {
            // path is just drive letter plus :
            if (path.length() == 2) return asFixnum(context, 0);
            // path is drive letter plus : plus leading or trailing /
            if (path.length() == 3 && (path.charAt(0) == '/' || path.charAt(2) == '/')) return asFixnum(context, 0);
            // path is drive letter plus : plus leading and trailing /
            if (path.length() == 4 && (path.charAt(0) == '/' && path.charAt(3) == '/')) return asFixnum(context, 0);
        }

        File newDir = res.unwrap(File.class);
        if (File.separatorChar == '\\') newDir = new File(newDir.getPath());

        int mode = args.length == 2 ? ((int) args[1].convertToInteger().getLongValue()) : 0777;

        if (context.runtime.getPosix().mkdir(newDir.getAbsolutePath(), mode) < 0) {
            // FIXME: This is a system error based on errno
            throw context.runtime.newSystemCallError("mkdir failed");
        }

        return asFixnum(context, 0);
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
        return block.isGiven() ?
                each_child(context, block) :
                enumeratorize(context.runtime, children(context), "each");
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
    public RubyInteger tell(ThreadContext context) {
        checkDir();
        return asFixnum(context, pos);
    }

    @Deprecated
    public RubyInteger tell() {
        return tell(getCurrentContext());
    }

    /**
     * Moves to a position <code>d</code>.  <code>pos</code> must be a value
     * returned by <code>tell</code> or 0.
     */

    @JRubyMethod(name = "seek")
    public IRubyObject seek(IRubyObject newPos) {
        checkDir();

        set_pos(newPos);
        return this;
    }

    @JRubyMethod(name = "pos=")
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
        return path == null ? null : path.asJavaString();
    }

    /** Returns the next entry from this directory. */
    @JRubyMethod(name = "read")
    public IRubyObject read() {
        checkDir();

        final String[] snapshot = this.snapshot;
        if (pos >= snapshot.length) return getRuntime().getNil();

        RubyString result = newExternalStringWithEncoding(getRuntime(), snapshot[pos], encoding);
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
    public static IRubyObject empty_p(ThreadContext context, IRubyObject recv, IRubyObject path) {
        RubyFileStat fileStat = context.runtime.newFileStat(RubyFile.get_path(context, path).asJavaString(), false);
        boolean isDirectory = fileStat.directory_p(context).isTrue();
        return asBoolean(context, isDirectory && entries(context, recv, path).getLength() <= 2);
    }

    @JRubyMethod(name = "exist?", meta = true)
    public static IRubyObject exist(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        // Capture previous exception if any.
        IRubyObject exception = context.runtime.getGlobalVariables().get("$!");
        RubyString path = RubyFile.get_path(context, arg);

        try {
            return context.runtime.newFileStat(path.asJavaString(), false).directory_p(context);
        } catch (Exception e) {
            // Restore $!
            context.runtime.getGlobalVariables().set("$!", exception);
            return context.fals;
        }
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

        if (mustExist && (result == null || !result.exists())) throw runtime.newErrnoENOENTError(dir);

        boolean isDirectory = result.isDirectory();

        if (mustExist && !isDirectory) throw runtime.newErrnoENOTDIRError(path);
        if (!mustExist && isDirectory) throw runtime.newErrnoEEXISTError(dir);

        return result;
    }

    private static FileResource getExistingDir(final Ruby runtime, final String path) {
        FileResource result = JRubyFile.createResource(runtime, path);
        if (result == null || !result.exists()) throw runtime.newErrnoENOENTError(path);
        if (!result.isDirectory()) throw runtime.newErrnoENOTDIRError(path);

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
        if (parentFile.exists() && ! parentFile.canWrite()) throw runtime.newErrnoEACCESError(path);

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
    @Deprecated(since = "9.4-")
    protected static List<String> getContents(FileResource directory) {
        final String[] contents = directory.list();

        // If an IO exception occurs (something odd, but possible. A directory may return null.
        return contents != null ? Arrays.asList(contents) : Collections.emptyList();
    }

    /**
     * @deprecated no longer used
     */
    @Deprecated(since = "9.4-")
    protected static List<RubyString> getContents(FileResource directory, Ruby runtime) {
        final String[] contents = directory.list();
        if (contents == null) return Collections.emptyList();

        final List<RubyString> result = new ArrayList<>(contents.length);
        for (int i = 0; i < contents.length; i++) {
            result.add(runtime.newString(contents[i]));
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
                if (passwd != null) return newFilesystemString(runtime, passwd.getHome());
            } catch (Exception e) {
            }
        }

        // fall back on other ways

        if (Platform.IS_WINDOWS) {
            if (user.equals(posix.getlogin())) return getHomeDirectoryPath(context);
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

        throw argumentError(context, "user " + user + " doesn't exist");
    }

    private static RubyString newFilesystemString(Ruby runtime, String home) {
        return newExternalStringWithEncoding(runtime, home, runtime.getDefaultFilesystemEncoding());
    }

    static final ByteList HOME = new ByteList(new byte[] {'H','O','M','E'}, false);

    public static RubyString getHomeDirectoryPath(ThreadContext context) {
        final RubyString homeKey = RubyString.newStringShared(context.runtime, HOME);
        IRubyObject home = context.runtime.getENV().op_aref(context, homeKey);

        return getHomeDirectoryPath(context, home);
    }

    public static Optional<String> getHomeFromEnv(Ruby runtime) {
        final RubyString homeKey = RubyString.newStringShared(runtime, HOME);
        final RubyHash env = runtime.getENV();

        return !env.hasKey(homeKey) ?
                Optional.empty() : Optional.of(env.op_aref(runtime.getCurrentContext(), homeKey).toString());
    }

    private static final ByteList user_home = new ByteList(new byte[] {'u','s','e','r','.','h','o','m','e'}, false);

    static RubyString getHomeDirectoryPath(ThreadContext context, IRubyObject home) {
        RubyHash env = context.runtime.getENV();

        if (home == null || home == context.nil) {
            IRubyObject ENV_JAVA = objectClass(context).getConstant("ENV_JAVA");
            home = ENV_JAVA.callMethod(context, "[]", newString(context, user_home, UTF8));
        }

        if (home == null || home == context.nil) home = env.op_aref(context, newString(context, "LOGDIR"));
        if (home == null || home == context.nil) throw argumentError(context, "user.home/LOGDIR not set");

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
    public static IRubyObject home(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length > 0 && args[0] != context.nil) return getHomeDirectoryPath(context, args[0].toString());

        return getHomeDirectoryPath(context);
    }

    @Deprecated
    public static RubyArray entries(IRubyObject recv, IRubyObject path) {
        return entries(recv.getRuntime().getCurrentContext(), recv, path);
    }

    @Deprecated
    public static RubyArray entries(IRubyObject recv, IRubyObject path, IRubyObject arg, IRubyObject opts) {
        return entries(recv.getRuntime().getCurrentContext(), recv, path, opts);
    }

    @Deprecated
    public static IRubyObject chdir(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 0 -> chdir(context, recv, block);
            case 1 -> chdir(context, recv, args[0], block);
            default -> throw argumentError(context, args.length, 0, 1);
        };
    }

}
