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
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import jnr.posix.FileStat;

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
import org.jruby.util.Dir;
import org.jruby.util.JRubyFile;
import org.jruby.util.ByteList;
import static org.jruby.CompatVersion.*;

/**
 * .The Ruby built-in class Dir.
 *
 * @author  jvoegele
 */
@JRubyClass(name = "Dir", include = "Enumerable")
public class RubyDir extends RubyObject {
    private RubyString path;       // What we passed to the constructor for method 'path'
    protected JRubyFile dir;
    private long lastModified = Long.MIN_VALUE;
    private String[] snapshot;     // snapshot of contents of directory
    private int pos;               // current position in directory
    private boolean isOpen = true;

    private final static Encoding UTF8 = UTF8Encoding.INSTANCE;

    public RubyDir(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    private static final ObjectAllocator DIR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyDir(runtime, klass);
        }
    };

    public static RubyClass createDirClass(Ruby runtime) {
        RubyClass dirClass = runtime.defineClass("Dir", runtime.getObject(), DIR_ALLOCATOR);
        runtime.setDir(dirClass);

        dirClass.index = ClassIndex.DIR;
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
            snapshotList.add(".");
            snapshotList.add("..");
            snapshotList.addAll(getContents(dir));
            snapshot = (String[]) snapshotList.toArray(new String[snapshotList.size()]);
        }
    }

    /**
     * Creates a new <code>Dir</code>.  This method takes a snapshot of the
     * contents of the directory at creation time, so changes to the contents
     * of the directory will not be reflected during the lifetime of the
     * <code>Dir</code> object returned, so a new <code>Dir</code> instance
     * must be created to reflect changes to the underlying file system.
     */
    @JRubyMethod(compat = RUBY1_8)
    public IRubyObject initialize(IRubyObject arg) {
        RubyString newPath = arg.convertToString();
        path = newPath;
        pos = 0;

        String adjustedPath = RubyFile.adjustRootPathOnWindows(getRuntime(), newPath.toString(), null);
        checkDirIsTwoSlashesOnWindows(getRuntime(), adjustedPath);

        dir = JRubyFile.create(getRuntime().getCurrentDirectory(), adjustedPath);
        List<String> snapshotList = RubyDir.getEntries(getRuntime(), adjustedPath);
        snapshot = (String[]) snapshotList.toArray(new String[snapshotList.size()]);

        return this;
    }

    @JRubyMethod(name = "initialize", compat = RUBY1_9)
    public IRubyObject initialize19(IRubyObject arg) {
        return initialize(RubyFile.get_path(getRuntime().getCurrentContext(), arg));
    }

// ----- Ruby Class Methods ----------------------------------------------------

    private static List<ByteList> dirGlobs(ThreadContext context, String cwd, IRubyObject[] args, int flags) {
        List<ByteList> dirs = new ArrayList<ByteList>();

        for (int i = 0; i < args.length; i++) {
            dirs.addAll(Dir.push_glob(cwd, globArgumentAsByteList(context, args[i]), flags));
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
        try {
            return new org.jruby.util.NormalizedFile(runtime.getCurrentDirectory()).getCanonicalPath();
        } catch (Exception e) {
            return runtime.getCurrentDirectory();
        }
    }

    @JRubyMethod(name = "[]", required = 1, rest = true, meta = true)
    public static IRubyObject aref(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        List<ByteList> dirs;
        if (args.length == 1) {
            Pattern pattern = Pattern.compile("file:(.*)!/(.*)");
            String glob = args[0].toString();
            Matcher matcher = pattern.matcher(glob);
            if (matcher.find()) {
                String jarFileName = matcher.group(1);
                String jarUri = "file:" + jarFileName + "!/";
                String fileGlobString = matcher.group(2);
                String filePatternString = convertGlobToRegEx(fileGlobString);
                Pattern filePattern = Pattern.compile(filePatternString);
                try {
                    JarFile jarFile = new JarFile(jarFileName);
                    List<RubyString> allFiles = new ArrayList<RubyString>();
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        String entry = entries.nextElement().getName();
                        String chomped_entry = entry.endsWith("/") ? entry.substring(0, entry.length() - 1) : entry;
                        if (filePattern.matcher(chomped_entry).find()) {
                            allFiles.add(RubyString.newString(runtime, jarUri + chomped_entry.toString()));
                        }
                    }
                    IRubyObject[] tempFileList = new IRubyObject[allFiles.size()];
                    allFiles.toArray(tempFileList);
                    return runtime.newArrayNoCopy(tempFileList);
                } catch (IOException e) {
                    return runtime.newArrayNoCopy(new IRubyObject[0]);
                }
            }

            dirs = Dir.push_glob(getCWD(runtime), globArgumentAsByteList(context, args[0]), 0);
        } else {
            dirs = dirGlobs(context, getCWD(runtime), args, 0);
        }

        return asRubyStringList(runtime, dirs);
    }

    private static ByteList globArgumentAsByteList(ThreadContext context, IRubyObject arg) {
        if (context.runtime.is1_9()) return RubyFile.get_path(context, arg).getByteList();

        return arg.convertToString().getByteList();
    }

    private static String convertGlobToRegEx(String line) {
        line = line.trim();
        StringBuilder sb = new StringBuilder(line.length());
        sb.append("^");
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray()) {
            switch (currentChar) {
            case '*':
                if (escaping)
                    sb.append("\\*");
                else
                    sb.append("[^/]*");
                escaping = false;
                break;
            case '?':
                if (escaping)
                    sb.append("\\?");
                else
                    sb.append('.');
                escaping = false;
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                sb.append('\\');
                sb.append(currentChar);
                escaping = false;
                break;
            case '\\':
                if (escaping) {
                    sb.append("\\\\");
                    escaping = false;
                } else
                    escaping = true;
                break;
            case '{':
                if (escaping) {
                    sb.append("\\{");
                } else {
                    sb.append('(');
                    inCurlies++;
                }
                escaping = false;
                break;
            case '}':
                if (inCurlies > 0 && !escaping) {
                    sb.append(')');
                    inCurlies--;
                } else if (escaping)
                    sb.append("\\}");
                else
                    sb.append("}");
                escaping = false;
                break;
            case ',':
                if (inCurlies > 0 && !escaping) {
                    sb.append('|');
                } else if (escaping)
                    sb.append("\\,");
                else
                    sb.append(",");
                break;
            default:
                escaping = false;
                sb.append(currentChar);
            }
        }
        sb.append("$");
        return sb.toString().replace("[^/]*[^/]*/", ".*").replace("[^/]*[^/]*", ".*");
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
            dirs = Dir.push_glob(runtime.getCurrentDirectory(), globArgumentAsByteList(context, args[0]), flags);
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
    @JRubyMethod(name = "entries", meta = true, compat = RUBY1_8)
    public static RubyArray entries(IRubyObject recv, IRubyObject path) {
        return entriesCommon(recv.getRuntime(), path.convertToString().getUnicodeValue());
    }

    @JRubyMethod(name = "entries", meta = true, compat = RUBY1_9)
    public static RubyArray entries19(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return entriesCommon(context.runtime, RubyFile.get_path(context, arg).asJavaString());
    }

    @JRubyMethod(name = "entries", meta = true, compat = RUBY1_9)
    public static RubyArray entries19(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject opts) {
        // FIXME: do something with opts
        return entriesCommon(context.runtime, RubyFile.get_path(context, arg).asJavaString());
    }

    private static RubyArray entriesCommon(Ruby runtime, String path) {
        String adjustedPath = RubyFile.adjustRootPathOnWindows(runtime, path, null);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);

        Object[] files = getEntries(runtime, adjustedPath).toArray();
        return runtime.newArrayNoCopy(JavaUtil.convertJavaArrayToRuby(runtime, files));
    }

    private static List<String> getEntries(Ruby runtime, String path) {
        if (!RubyFileTest.directory_p(runtime, RubyString.newString(runtime, path)).isTrue()) {
            throw runtime.newErrnoENOENTError("No such directory: " + path);
        }

        if (path.startsWith("jar:")) path = path.substring(4);
        if (path.startsWith("file:")) return entriesIntoAJarFile(runtime, path);

        return entriesIntoADirectory(runtime, path);
    }

    private static List<String> entriesIntoADirectory(Ruby runtime, String path) {
        final JRubyFile directory = JRubyFile.create(runtime.getCurrentDirectory(), path);

        List<String> fileList = getContents(directory);
        fileList.add(0, ".");
        fileList.add(1, "..");
        return fileList;
    }

    private static List<String> entriesIntoAJarFile(Ruby runtime, String path) {
        String file = path.substring(5);
        int bang = file.indexOf('!');
        if (bang == -1) {
          return entriesIntoADirectory(runtime, path.substring(5));
        }
        if (bang == file.length() - 1) {
            file = file + "/";
        }
        String jar = file.substring(0, bang);
        String after = file.substring(bang + 2);
        if (after.length() > 0 && after.charAt(after.length() - 1) != '/') {
            after = after + "/";
        }
        JarFile jf;
        try {
            jf = new JarFile(jar);
        } catch (IOException e) {
            throw new RuntimeException("Valid JAR file expected", e);
        }

        List<String> fileList = new ArrayList<String>();
        Enumeration<? extends ZipEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            String zipEntry = entries.nextElement().getName();
            if (zipEntry.matches(after + "[^/]+(/.*)?")) {
                int end_index = zipEntry.indexOf('/', after.length());
                if (end_index == -1) {
                    end_index = zipEntry.length();
                }
                String entry_str = zipEntry.substring(after.length(), end_index);
                if (!fileList.contains(entry_str)) {
                    fileList.add(entry_str);
                }
            }
        }

        return fileList;
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
            RubyFile.get_path(context, args[0]) : getHomeDirectoryPath(context);
        String adjustedPath = RubyFile.adjustRootPathOnWindows(runtime, path.asJavaString(), null);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);
        JRubyFile dir = getDir(runtime, adjustedPath, true);
        String realPath = null;
        String oldCwd = runtime.getCurrentDirectory();

        // We get canonical path to try and flatten the path out.
        // a dir '/subdir/..' should return as '/'
        // cnutter: Do we want to flatten path out?
        try {
            realPath = dir.getCanonicalPath();
        } catch (IOException e) {
            realPath = dir.getAbsolutePath();
        }

        IRubyObject result = null;
        if (block.isGiven()) {
            // FIXME: Don't allow multiple threads to do this at once
            runtime.setCurrentDirectory(realPath);
            try {
                result = block.yield(context, path);
            } finally {
                dir = getDir(runtime, oldCwd, true);
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
    @JRubyMethod(name = {"rmdir", "unlink", "delete"}, required = 1, meta = true, compat = RUBY1_8)
    public static IRubyObject rmdir(IRubyObject recv, IRubyObject path) {
        return rmdirCommon(recv.getRuntime(), path.convertToString().getUnicodeValue());
    }

    @JRubyMethod(name = {"rmdir", "unlink", "delete"}, required = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject rmdir19(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return rmdirCommon(context.runtime, RubyFile.get_path(context, path).asJavaString());
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
    @JRubyMethod(meta = true, compat = RUBY1_8)
    public static IRubyObject foreach(ThreadContext context, IRubyObject recv, IRubyObject _path, Block block) {
        RubyString pathString = _path.convertToString();

        return foreachCommon(context, recv, context.runtime, pathString, block);
    }

    @JRubyMethod(name = "foreach", meta = true, compat = RUBY1_9)
    public static IRubyObject foreach19(ThreadContext context, IRubyObject recv, IRubyObject arg, Block block) {
        RubyString pathString = RubyFile.get_path(context, arg);

        return foreachCommon(context, recv, context.runtime, pathString, block);
    }

    private static IRubyObject foreachCommon(ThreadContext context, IRubyObject recv, Ruby runtime, RubyString _path, Block block) {
        if (block.isGiven()) {
            RubyClass dirClass = runtime.getDir();
            RubyDir dir = (RubyDir) dirClass.newInstance(context, new IRubyObject[]{_path}, block);

            dir.each(context, block);
            return runtime.getNil();
        }

        return enumeratorize(runtime, recv, "foreach", _path);
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
    @JRubyMethod(name = "home", optional = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject home(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length > 0 && !args[0].isNil()) return getHomeDirectoryPath(context, args[0].toString());

        return getHomeDirectoryPath(context);
    }

    /**
     * Creates the directory specified by <code>path</code>.  Note that the
     * <code>mode</code> parameter is provided only to support existing Ruby
     * code, and is ignored.
     */
    @JRubyMethod(name = "mkdir", required = 1, optional = 1, meta = true, compat = RUBY1_8)
    public static IRubyObject mkdir(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        RubyString stringArg = args[0].convertToString();

        return mkdirCommon(runtime, stringArg.getUnicodeValue(), args);
    }

    @JRubyMethod(name = "mkdir", required = 1, optional = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject mkdir19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return mkdirCommon(context.runtime, RubyFile.get_path(context, args[0]).asJavaString(), args);
    }

    private static IRubyObject mkdirCommon(Ruby runtime, String path, IRubyObject[] args) {
        File newDir = getDir(runtime, path, false);

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
    @JRubyMethod(meta = true, compat = RUBY1_8)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        RubyDir directory = (RubyDir) context.runtime.getDir().newInstance(context,
                new IRubyObject[]{path}, Block.NULL_BLOCK);

        if (!block.isGiven()) return directory;

        try {
            return block.yield(context, directory);
        } finally {
            directory.close();
        }
    }

    @JRubyMethod(name = "open", meta = true, compat = RUBY1_9)
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        return open(context, recv, RubyFile.get_path(context, path), block);
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
    public IRubyObject each(ThreadContext context, Block block) {
        checkDir();

        String[] contents = snapshot;
        for (pos = 0; pos < contents.length; pos++) {
            block.yield(context, getRuntime().newString(contents[pos]));
        }

        return this;
    }

    @JRubyMethod(name = "each")
    public IRubyObject each19(ThreadContext context, Block block) {
        return block.isGiven() ? each(context, block) : enumeratorize(context.runtime, this, "each");
    }

    @Override
    @JRubyMethod
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        StringBuilder part = new StringBuilder();
        String cname = getMetaClass().getRealClass().getName();
        part.append("#<").append(cname).append(":").append(path.asJavaString()).append(">");

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

    @JRubyMethod(name = "path")
    public IRubyObject path(ThreadContext context) {
        return path.strDup(context.runtime);
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

    @JRubyMethod(name = {"exists?", "exist?"}, meta = true, compat = RUBY1_9)
    public static IRubyObject exist(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        // Capture previous exception if any.
        IRubyObject exception = context.runtime.getGlobalVariables().get("$!");
        try {
            return context.runtime.newFileStat(RubyFile.get_path(context, arg).asJavaString(), false).directory_p();
        } catch (Exception e) {
            // Restore $!
            context.runtime.getGlobalVariables().set("$!", exception);
            return context.runtime.newBoolean(false);
        }
    }

// ----- Helper Methods --------------------------------------------------------
    /** Returns a Java <code>File</code> object for the specified path.  If
     * <code>path</code> is not a directory, throws <code>IOError</code>.
     *
     * @param   path path for which to return the <code>File</code> object.
     * @param   mustExist is true the directory must exist.  If false it must not.
     * @throws  IOError if <code>path</code> is not a directory.
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
     * @param mustExist
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
    protected static List<String> getContents(File directory) {
        String[] contents = directory.list();
        List<String> result = new ArrayList<String>();

        // If an IO exception occurs (something odd, but possible)
        // A directory may return null.
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                result.add(contents[i]);
            }
        }
        return result;
    }

    /**
     * Returns the contents of the specified <code>directory</code> as an
     * <code>ArrayList</code> containing the names of the files as Ruby Strings.
     */
    protected static List<RubyString> getContents(File directory, Ruby runtime) {
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
            String passwd = null;
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
            home = envHash.op_aref(context, runtime.newString("LOGDIR"));
        }

        if (home == null || home.isNil()) {
            home = envHash.op_aref(context, runtime.newString("HOME"));
        }

        if (home == null || home.isNil()) {
            home = systemHash.callMethod(context, "[]", runtime.newString("user.home"));
        }

        if (home == null || home.isNil()) {
            throw runtime.newArgumentError("user.home/LOGDIR not set");
        }

        return (RubyString) home;
    }
}
