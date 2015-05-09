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

import static org.jruby.RubyEnumerator.enumeratorize;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import jnr.posix.FileStat;

import jnr.posix.POSIX;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import jnr.posix.util.Platform;
import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;

import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.Dir;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * .The Ruby built-in class Dir.
 *
 * @author  jvoegele
 */
@JRubyClass(name = "Dir", include = "Enumerable")
public class RubyDir extends RubyObject {
    private RubyString path;       // What we passed to the constructor for method 'path'
    protected FileResource dir;
    private long lastModified = Long.MIN_VALUE;
    private String[] snapshot;     // snapshot of contents of directory
    private int pos;               // current position in directory
    private boolean isOpen = true;

    private final static Encoding UTF8 = UTF8Encoding.INSTANCE;

    public RubyDir(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    private static final ObjectAllocator DIR_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyDir(runtime, klass);
        }
    };

    public static RubyClass createDirClass(Ruby runtime) {
        RubyClass dirClass = runtime.defineClass("Dir", runtime.getObject(), DIR_ALLOCATOR);
        runtime.setDir(dirClass);

        dirClass.setClassIndex(ClassIndex.DIR);
        dirClass.setReifiedClass(RubyDir.class);

        dirClass.includeModule(runtime.getEnumerable());
        dirClass.defineAnnotatedMethods(RubyDir.class);

        return dirClass;
    }

    private final void checkDir() {
        testFrozen("Dir");
        update();

        if (!isOpen) throw getRuntime().newIOError("closed directory");
    }

    private void update() {
        if (snapshot == null || dir.exists() && dir.lastModified() > lastModified) {
            lastModified = dir.lastModified();
            List<String> snapshotList = new ArrayList<String>();
            snapshotList.addAll(getContents(dir));
            snapshot = snapshotList.toArray(new String[snapshotList.size()]);
        }
    }

    /**
     * Creates a new <code>Dir</code>.  This method takes a snapshot of the
     * contents of the directory at creation time, so changes to the contents
     * of the directory will not be reflected during the lifetime of the
     * <code>Dir</code> object returned, so a new <code>Dir</code> instance
     * must be created to reflect changes to the underlying file system.
     */
    public IRubyObject initialize(ThreadContext context, IRubyObject arg) {
        return initialize19(context, arg);
    }

    @JRubyMethod(name = "initialize")
    public IRubyObject initialize19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        RubyString newPath = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, arg));
        path = newPath;
        pos = 0;

        String adjustedPath = RubyFile.adjustRootPathOnWindows(runtime, newPath.toString(), null);
        checkDirIsTwoSlashesOnWindows(getRuntime(), adjustedPath);

        dir = JRubyFile.createResource(context, adjustedPath);
        snapshot = getEntries(context, dir, adjustedPath);

        return this;
    }

// ----- Ruby Class Methods ----------------------------------------------------

    private static List<ByteList> dirGlobs(ThreadContext context, String cwd, IRubyObject[] args, int flags) {
        List<ByteList> dirs = new ArrayList<ByteList>();

        for (int i = 0; i < args.length; i++) {
            dirs.addAll(Dir.push_glob(context.runtime, cwd, globArgumentAsByteList(context, args[i]), flags));
        }

        return dirs;
    }

    private static IRubyObject asRubyStringList(Ruby runtime, List<ByteList> dirs) {
        List<RubyString> allFiles = new ArrayList<RubyString>();
        Encoding enc = runtime.getDefaultExternalEncoding();
        if (enc == null) {
            enc = UTF8;
        }

        for (ByteList dir : dirs) {
            allFiles.add(RubyString.newString(runtime, dir, enc));
        }

        IRubyObject[] tempFileList = new IRubyObject[allFiles.size()];
        allFiles.toArray(tempFileList);

        return runtime.newArrayNoCopy(tempFileList);
    }

    private static String getCWD(Ruby runtime) {
        if (runtime.getCurrentDirectory().startsWith("uri:")) {
            return runtime.getCurrentDirectory();
        }
        try {
            return new org.jruby.util.NormalizedFile(runtime.getCurrentDirectory()).getCanonicalPath();
        } catch (Exception e) {
            return runtime.getCurrentDirectory();
        }
    }

    @JRubyMethod(name = "[]", rest = true, meta = true)
    public static IRubyObject aref(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        List<ByteList> dirs;
        if (args.length == 1) {
            dirs = Dir.push_glob(runtime, getCWD(runtime), globArgumentAsByteList(context, args[0]), 0);
        } else {
            dirs = dirGlobs(context, getCWD(runtime), args, 0);
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
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject glob(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        int flags = args.length == 2 ? RubyNumeric.num2int(args[1]) : 0;

        List<ByteList> dirs;
        IRubyObject tmp = args[0].checkArrayType();
        if (tmp.isNil()) {
            dirs = Dir.push_glob(runtime, runtime.getCurrentDirectory(), globArgumentAsByteList(context, args[0]), flags);
        } else {
            dirs = dirGlobs(context, getCWD(runtime), ((RubyArray) tmp).toJavaArray(), flags);
        }

        if (block.isGiven()) {
            for (int i = 0; i < dirs.size(); i++) {
                Encoding enc = runtime.getDefaultExternalEncoding();
                if (enc == null) {
                    enc = UTF8;
                }
                block.yield(context, RubyString.newString(runtime, dirs.get(i), enc));
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
        return getRuntime().newArrayNoCopy(JavaUtil.convertJavaArrayToRuby(getRuntime(), snapshot));
    }

    /**
     * Returns an array containing all of the filenames in the given directory.
     */
    public static RubyArray entries(IRubyObject recv, IRubyObject path) {
        return entries19(recv.getRuntime().getCurrentContext(), recv, path);
    }

    @JRubyMethod(name = "entries", meta = true)
    public static RubyArray entries19(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RubyString path = StringSupport.checkEmbeddedNulls(context.runtime, RubyFile.get_path(context, arg));
        return entriesCommon(context, path.asJavaString());
    }

    @JRubyMethod(name = "entries", meta = true)
    public static RubyArray entries19(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject opts) {
        RubyString path = StringSupport.checkEmbeddedNulls(context.runtime, RubyFile.get_path(context, arg));
        // FIXME: do something with opts
        return entriesCommon(context, path.asJavaString());
    }

    private static RubyArray entriesCommon(ThreadContext context, String path) {
        Ruby runtime = context.runtime;
        String adjustedPath = RubyFile.adjustRootPathOnWindows(runtime, path, null);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);

        FileResource directory = JRubyFile.createResource(context, path);
        Object[] files = getEntries(context, directory, adjustedPath);

        return runtime.newArrayNoCopy(JavaUtil.convertJavaArrayToRuby(runtime, files));
    }

    private static final String[] NO_FILES = new String[] {};
    private static String[] getEntries(ThreadContext context, FileResource dir, String path) {
        if (!dir.isDirectory()) throw context.runtime.newErrnoENOENTError("No such directory: " + path);
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
        String realPath = null;
        String oldCwd = runtime.getCurrentDirectory();
        if (adjustedPath.startsWith("uri:")){
            realPath = adjustedPath;
        }
        else {
            JRubyFile dir = getDir(runtime, adjustedPath, true);

            // We get canonical path to try and flatten the path out.
            // a dir '/subdir/..' should return as '/'
            // cnutter: Do we want to flatten path out?
            try {
                realPath = dir.getCanonicalPath();
            } catch (IOException e) {
                realPath = dir.getAbsolutePath();
            }
        }

        IRubyObject result = null;
        if (block.isGiven()) {
            // FIXME: Don't allow multiple threads to do this at once
            runtime.setCurrentDirectory(realPath);
            try {
                result = block.yield(context, path);
            } finally {
                getDir(runtime, oldCwd, true); // needed in case the block deleted the oldCwd
                runtime.setCurrentDirectory(oldCwd);
            }
        } else {
            runtime.setCurrentDirectory(realPath);
            result = runtime.newFixnum(0);
        }

        return result;
    }

    /**
     * Changes the root directory (only allowed by super user).  Not available
     * on all platforms.
     */
    @JRubyMethod(name = "chroot", required = 1, meta = true)
    public static IRubyObject chroot(IRubyObject recv, IRubyObject path) {
        throw recv.getRuntime().newNotImplementedError("chroot not implemented: chroot is non-portable and is not supported.");
    }

    /**
     * Deletes the directory specified by <code>path</code>.  The directory must
     * be empty.
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

    private static IRubyObject rmdirCommon(Ruby runtime, String path) {
        JRubyFile directory = getDirForRmdir(runtime, path);

        // at this point, only thing preventing delete should be non-emptiness
        if (runtime.getPosix().rmdir(directory.toString()) < 0) {
            throw runtime.newErrnoENOTEMPTYError(path);
        }

        return runtime.newFixnum(0);
    }

    /**
     * Executes the block once for each file in the directory specified by
     * <code>path</code>.
     */
    public static IRubyObject foreach(ThreadContext context, IRubyObject recv, IRubyObject _path, Block block) {
        return foreach19(context, recv, _path, block);
    }

    @JRubyMethod(name = "foreach", meta = true)
    public static IRubyObject foreach19(ThreadContext context, IRubyObject recv, IRubyObject arg, Block block) {
        RubyString pathString = RubyFile.get_path(context, arg);

        return foreachCommon(context, recv, context.runtime, pathString, null, block);
    }

    @JRubyMethod(name = "foreach", meta = true)
    public static IRubyObject foreach19(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject enc, Block block) {
        RubyString pathString = RubyFile.get_path(context, path);
        RubyEncoding encoding;

        if (enc instanceof RubyEncoding) {
            encoding = (RubyEncoding) enc;
        } else {
            throw context.runtime.newTypeError(enc, context.runtime.getEncoding());
        }

        return foreachCommon(context, recv, context.runtime, pathString, encoding, block);
    }

    private static IRubyObject foreachCommon(ThreadContext context, IRubyObject recv, Ruby runtime, RubyString _path, RubyEncoding encoding, Block block) {
        if (block.isGiven()) {
            RubyClass dirClass = runtime.getDir();
            RubyDir dir = (RubyDir) dirClass.newInstance(context, new IRubyObject[]{_path}, block);

            dir.each(context, block);
            return runtime.getNil();
        }

        if (encoding == null) {
            return enumeratorize(runtime, recv, "foreach", _path);
        } else {
            return enumeratorize(runtime, recv, "foreach", new IRubyObject[]{_path, encoding});
        }
    }

    /** Returns the current directory. */
    @JRubyMethod(name = {"getwd", "pwd"}, meta = true)
    public static RubyString getwd(IRubyObject recv) {
        Ruby ruby = recv.getRuntime();

        RubyString pwd = RubyString.newUnicodeString(ruby, getCWD(ruby));
        pwd.setTaint(true);
        return pwd;
    }

    /**
     * Returns the home directory of the current user or the named user if given.
     */
    @JRubyMethod(name = "home", optional = 1, meta = true)
    public static IRubyObject home(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length > 0 && !args[0].isNil()) return getHomeDirectoryPath(context, args[0].toString());

        return getHomeDirectoryPath(context);
    }

    /**
     * Creates the directory specified by <code>path</code>.  Note that the
     * <code>mode</code> parameter is provided only to support existing Ruby
     * code, and is ignored.
     */
    public static IRubyObject mkdir(IRubyObject recv, IRubyObject[] args) {
        return mkdir19(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "mkdir", required = 1, optional = 1, meta = true)
    public static IRubyObject mkdir19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, args[0]));
        return mkdirCommon(runtime, path.asJavaString(), args);
    }

    private static IRubyObject mkdirCommon(Ruby runtime, String path, IRubyObject[] args) {
        File newDir = getDir(runtime, path, false);
        
        
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
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        return open19(context, recv, path, block);
    }

    @JRubyMethod(name = "open", meta = true)
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        RubyDir directory = (RubyDir) context.runtime.getDir().newInstance(context,
                new IRubyObject[] { RubyFile.get_path(context, path) }, Block.NULL_BLOCK);

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
    public IRubyObject close() {
        // Make sure any read()s after close fail.
        checkDir();

        isOpen = false;

        return getRuntime().getNil();
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
    public IRubyObject each(ThreadContext context, Block block) {
        return each(context, context.runtime.getDefaultInternalEncoding(), block);
    }

    @JRubyMethod(name = "each")
    public IRubyObject each19(ThreadContext context, Block block) {
        return block.isGiven() ? each(context, block) : enumeratorize(context.runtime, this, "each");
    }

    @JRubyMethod(name = "each")
    public IRubyObject each19(ThreadContext context, IRubyObject encoding, Block block) {
        if (!(encoding instanceof RubyEncoding)) throw context.runtime.newTypeError(encoding, context.runtime.getEncoding());

        return block.isGiven() ? each(context, ((RubyEncoding)encoding).getEncoding(), block) : enumeratorize(context.runtime, this, "each", encoding);
    }

    @Override
    @JRubyMethod
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        StringBuilder part = new StringBuilder();
        String cname = getMetaClass().getRealClass().getName();
        part.append("#<").append(cname).append(":");
        if (path != null) { part.append(path.asJavaString()); }
        part.append(">");

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
        return path == null ? context.runtime.getNil() : path.strDup(context.runtime);
    }
    
    @JRubyMethod
    public IRubyObject to_path(ThreadContext context) {
        return path(context);
    }    

    /** Returns the next entry from this directory. */
    @JRubyMethod(name = "read")
    public IRubyObject read() {
        checkDir();

        if (pos >= snapshot.length) return getRuntime().getNil();

        RubyString result = getRuntime().newString(snapshot[pos]);
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
     */
    protected static JRubyFile getDir(final Ruby runtime, final String path, final boolean mustExist) {
        String dir = dirFromPath(path, runtime);

        JRubyFile result = JRubyFile.create(runtime.getCurrentDirectory(), dir);

        if (mustExist && !result.exists()) {
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
        if (directory.getParentFile().exists() &&
                !directory.getParentFile().canWrite()) {
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
            if (pathParts[0].equals("file:") && pathParts[1].length() > 0 && pathParts[1].indexOf("!/") == -1) {
                dir = pathParts[1];
            } else {
                throw runtime.newErrnoENOTDIRError(dir);
            }
        }
        return dir;
    }

    /**
     * Returns the contents of the specified <code>directory</code> as an
     * <code>ArrayList</code> containing the names of the files as Java Strings.
     */
    protected static List<String> getContents(FileResource directory) {
        String[] contents = directory.list();
        List<String> result = new ArrayList<String>();

        // If an IO exception occurs (something odd, but possible)
        // A directory may return null.
        if (contents != null) result.addAll(Arrays.asList(contents));

        return result;
    }

    /**
     * Returns the contents of the specified <code>directory</code> as an
     * <code>ArrayList</code> containing the names of the files as Ruby Strings.
     */
    protected static List<RubyString> getContents(FileResource directory, Ruby runtime) {
        List<RubyString> result = new ArrayList<RubyString>();
        String[] contents = directory.list();

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

        try {
            // try to use POSIX for this first
            return runtime.newString(runtime.getPosix().getpwnam(user).getHome());
        } catch (Exception e) {
            // otherwise fall back on the old way
            String passwd;
            try {
                FileInputStream stream = new FileInputStream("/etc/passwd");
                int totalBytes = stream.available();
                byte[] bytes = new byte[totalBytes];
                stream.read(bytes);
                stream.close();
                passwd = new String(bytes);
            } catch (IOException ioe) {
                return runtime.getNil();
            }

            String[] rows = passwd.split("\n");
            int rowCount = rows.length;
            for (int i = 0; i < rowCount; i++) {
                String[] fields = rows[i].split(":");
                if (fields[0].equals(user)) {
                    return runtime.newString(fields[5]);
                }
            }
        }

        throw runtime.newArgumentError("user " + user + " doesn't exist");
    }

    public static RubyString getHomeDirectoryPath(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject systemHash = runtime.getObject().getConstant("ENV_JAVA");
        RubyHash envHash = (RubyHash) runtime.getObject().getConstant("ENV");
        IRubyObject home = null;

        if (home == null || home.isNil()) {
            home = envHash.op_aref(context, runtime.newString("HOME"));
        }

        if (home == null || home.isNil()) {
            home = systemHash.callMethod(context, "[]", runtime.newString("user.home"));
        }

        if (home == null || home.isNil()) {
            home = envHash.op_aref(context, runtime.newString("LOGDIR"));
        }

        if (home == null || home.isNil()) {
            throw runtime.newArgumentError("user.home/LOGDIR not set");
        }

        return (RubyString) home;
    }
}
