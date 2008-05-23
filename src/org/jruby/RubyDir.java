/***** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.ext.posix.util.Platform;

import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Dir;
import org.jruby.util.JRubyFile;
import org.jruby.util.ByteList;

/**
 * .The Ruby built-in class Dir.
 *
 * @author  jvoegele
 */
@JRubyClass(name="Dir", include="Enumerable")
public class RubyDir extends RubyObject {
	// What we passed to the constructor for method 'path'
    private RubyString    path;
    protected JRubyFile      dir;
    private   String[]  snapshot;   // snapshot of contents of directory
    private   int       pos;        // current position in directory
    private boolean isOpen = true;

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

        dirClass.includeModule(runtime.getEnumerable());
        
        dirClass.defineAnnotatedMethods(RubyDir.class);

        return dirClass;
    }
    
    private final void checkDir() {
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) throw getRuntime().newSecurityError("Insecure: operation on untainted Dir");
     
        testFrozen("");
        
        if (!isOpen) throw getRuntime().newIOError("closed directory");
    }    

    /**
     * Creates a new <code>Dir</code>.  This method takes a snapshot of the
     * contents of the directory at creation time, so changes to the contents
     * of the directory will not be reflected during the lifetime of the
     * <code>Dir</code> object returned, so a new <code>Dir</code> instance
     * must be created to reflect changes to the underlying file system.
     */
    @JRubyMethod(name = "initialize", required = 1, frame = true)
    public IRubyObject initialize(IRubyObject _newPath, Block unusedBlock) {
        RubyString newPath = _newPath.convertToString();
        getRuntime().checkSafeString(newPath);

        String adjustedPath = RubyFile.adjustRootPathOnWindows(getRuntime(), newPath.toString(), null);
        checkDirIsTwoSlashesOnWindows(getRuntime(), adjustedPath);

        dir = JRubyFile.create(getRuntime().getCurrentDirectory(), adjustedPath);
        if (!dir.isDirectory()) {
            dir = null;
            throw getRuntime().newErrnoENOENTError(newPath.toString() + " is not a directory");
        }
        path = newPath;
		List<String> snapshotList = new ArrayList<String>();
		snapshotList.add(".");
		snapshotList.add("..");
		snapshotList.addAll(getContents(dir));
		snapshot = (String[]) snapshotList.toArray(new String[snapshotList.size()]);
		pos = 0;

        return this;
    }

// ----- Ruby Class Methods ----------------------------------------------------
    
    private static List<ByteList> dirGlobs(String cwd, IRubyObject[] args, int flags) {
        List<ByteList> dirs = new ArrayList<ByteList>();
        
        for (int i = 0; i < args.length; i++) {
            ByteList globPattern = args[i].convertToString().getByteList();
            dirs.addAll(Dir.push_glob(cwd, globPattern, flags));
        }
        
        return dirs;
    }
    
    private static IRubyObject asRubyStringList(Ruby runtime, List<ByteList> dirs) {
        List<RubyString> allFiles = new ArrayList<RubyString>();

        for (ByteList dir: dirs) {
            allFiles.add(RubyString.newString(runtime, dir));
        }            

        IRubyObject[] tempFileList = new IRubyObject[allFiles.size()];
        allFiles.toArray(tempFileList);
         
        return runtime.newArrayNoCopy(tempFileList);
    }
    
    private static String getCWD(Ruby runtime) {
        try {
            return new org.jruby.util.NormalizedFile(runtime.getCurrentDirectory()).getCanonicalPath();
        } catch(Exception e) {
            return runtime.getCurrentDirectory();
        }
    }

    @JRubyMethod(name = "[]", required = 1, optional = 1, meta = true)
    public static IRubyObject aref(IRubyObject recv, IRubyObject[] args) {
        List<ByteList> dirs;
        if (args.length == 1) {
            ByteList globPattern = args[0].convertToString().getByteList();
            dirs = Dir.push_glob(getCWD(recv.getRuntime()), globPattern, 0);
        } else {
            dirs = dirGlobs(getCWD(recv.getRuntime()), args, 0);
        }

        return asRubyStringList(recv.getRuntime(), dirs);
    }
    
    /**
     * Returns an array of filenames matching the specified wildcard pattern
     * <code>pat</code>. If a block is given, the array is iterated internally
     * with each filename is passed to the block in turn. In this case, Nil is
     * returned.  
     */
    @JRubyMethod(name = "glob", required = 1, optional = 1, frame = true, meta = true)
    public static IRubyObject glob(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        int flags = args.length == 2 ?  RubyNumeric.num2int(args[1]) : 0;

        List<ByteList> dirs;
        IRubyObject tmp = args[0].checkArrayType();
        if (tmp.isNil()) {
            ByteList globPattern = args[0].convertToString().getByteList();
            dirs = Dir.push_glob(recv.getRuntime().getCurrentDirectory(), globPattern, flags);
        } else {
            dirs = dirGlobs(getCWD(runtime), ((RubyArray) tmp).toJavaArray(), flags);
        }
        
        if (block.isGiven()) {
            for (int i = 0; i < dirs.size(); i++) {
                block.yield(context, RubyString.newString(runtime, dirs.get(i)));
            }
        
            return recv.getRuntime().getNil();
        }

        return asRubyStringList(recv.getRuntime(), dirs);
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
    @JRubyMethod(name = "entries", required = 1, meta = true)
    public static RubyArray entries(IRubyObject recv, IRubyObject path) {
        Ruby runtime = recv.getRuntime();

        String adjustedPath = RubyFile.adjustRootPathOnWindows(
                runtime, path.convertToString().toString(), null);
        checkDirIsTwoSlashesOnWindows(runtime, adjustedPath);

        final JRubyFile directory = JRubyFile.create(
                recv.getRuntime().getCurrentDirectory(), adjustedPath);

        if (!directory.isDirectory()) {
            throw recv.getRuntime().newErrnoENOENTError("No such directory");
        }
        List<String> fileList = getContents(directory);
		fileList.add(0, ".");
		fileList.add(1, "..");
        Object[] files = fileList.toArray();
        return recv.getRuntime().newArrayNoCopy(JavaUtil.convertJavaArrayToRuby(recv.getRuntime(), files));
    }
    
    // MRI behavior: just plain '//' or '\\\\' are considered illegal on Windows.
    private static void checkDirIsTwoSlashesOnWindows(Ruby runtime, String path) {
        if (Platform.IS_WINDOWS && ("//".equals(path) || "\\\\".equals(path))) {
            throw runtime.newErrnoEINVALError("Invalid argument - " + path);
        }
    }

    /** Changes the current directory to <code>path</code> */
    @JRubyMethod(name = "chdir", optional = 1, frame = true, meta = true)
    public static IRubyObject chdir(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString path = args.length == 1 ? 
            (RubyString) args[0].convertToString() : getHomeDirectoryPath(context);
        String adjustedPath = RubyFile.adjustRootPathOnWindows(
                recv.getRuntime(), path.toString(), null);
        checkDirIsTwoSlashesOnWindows(recv.getRuntime(), adjustedPath);
        JRubyFile dir = getDir(recv.getRuntime(), adjustedPath, true);
        String realPath = null;
        String oldCwd = recv.getRuntime().getCurrentDirectory();
        
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
            recv.getRuntime().setCurrentDirectory(realPath);
            try {
                result = block.yield(context, path);
            } finally {
                dir = getDir(recv.getRuntime(), oldCwd, true);
                recv.getRuntime().setCurrentDirectory(oldCwd);
            }
        } else {
        	recv.getRuntime().setCurrentDirectory(realPath);
        	result = recv.getRuntime().newFixnum(0);
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
    @JRubyMethod(name = {"rmdir", "unlink", "delete"}, required = 1, meta = true)
    public static IRubyObject rmdir(IRubyObject recv, IRubyObject path) {
        JRubyFile directory = getDir(recv.getRuntime(), path.convertToString().toString(), true);
        
        if (!directory.delete()) {
            throw recv.getRuntime().newSystemCallError("No such directory");
        }
        
        return recv.getRuntime().newFixnum(0);
    }

    /**
     * Executes the block once for each file in the directory specified by
     * <code>path</code>.
     */
    @JRubyMethod(name = "foreach", required = 1, frame = true, meta = true)
    public static IRubyObject foreach(ThreadContext context, IRubyObject recv, IRubyObject _path, Block block) {
        RubyString path = _path.convertToString();
        recv.getRuntime().checkSafeString(path);

        RubyClass dirClass = recv.getRuntime().getDir();
        RubyDir dir = (RubyDir) dirClass.newInstance(context, new IRubyObject[] { path }, block);
        
        dir.each(context, block);
        return recv.getRuntime().getNil();
    }

    /** Returns the current directory. */
    @JRubyMethod(name = {"getwd", "pwd"}, meta = true)
    public static RubyString getwd(IRubyObject recv) {
        Ruby ruby = recv.getRuntime();
        
        return RubyString.newUnicodeString(ruby, ruby.getCurrentDirectory());
    }

    /**
     * Creates the directory specified by <code>path</code>.  Note that the
     * <code>mode</code> parameter is provided only to support existing Ruby
     * code, and is ignored.
     */
    @JRubyMethod(name = "mkdir", required = 1, optional = 1, meta = true)
    public static IRubyObject mkdir(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        runtime.checkSafeString(args[0]);
        String path = args[0].toString();

        File newDir = getDir(runtime, path, false);
        if (File.separatorChar == '\\') {
            newDir = new File(newDir.getPath());
        }
        
        int mode = args.length == 2 ? ((int) args[1].convertToInteger().getLongValue()) : 0777;

        if (runtime.getPosix().mkdir(newDir.getAbsolutePath(), mode) < 0) {
            // FIXME: This is a system error based on errno
            throw recv.getRuntime().newSystemCallError("mkdir failed");
        }
        
        return RubyFixnum.zero(recv.getRuntime());
    }

    /**
     * Returns a new directory object for <code>path</code>.  If a block is
     * provided, a new directory object is passed to the block, which closes the
     * directory object before terminating.
     */
    @JRubyMethod(name = "open", required = 1, frame = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        RubyDir directory = 
            (RubyDir) recv.getRuntime().getDir().newInstance(context,
                    new IRubyObject[] { path }, Block.NULL_BLOCK);

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
    @JRubyMethod(name = "each", frame = true)
    public IRubyObject each(ThreadContext context, Block block) {
        checkDir();
        
        String[] contents = snapshot;
        for (int i=0; i<contents.length; i++) {
            block.yield(context, getRuntime().newString(contents[i]));
        }
        return this;
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
        this.pos = RubyNumeric.fix2int(newPos);
        return newPos;
    }

    @JRubyMethod(name = "path")
    public IRubyObject path() {
        checkDir();
        
        return path.strDup();
    }

    /** Returns the next entry from this directory. */
    @JRubyMethod(name = "read")
    public IRubyObject read() {
        checkDir();

        if (pos >= snapshot.length) {
            return getRuntime().getNil();
        }
        RubyString result = getRuntime().newString(snapshot[pos]);
        pos++;
        return result;
    }

    /** Moves position in this directory to the first entry. */
    @JRubyMethod(name = "rewind")
    public IRubyObject rewind() {
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) throw getRuntime().newSecurityError("Insecure: can't close");
        checkDir();

        pos = 0;
        return this;
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
        JRubyFile result = JRubyFile.create(runtime.getCurrentDirectory(),path);
        if (mustExist && !result.exists()) {
            throw runtime.newErrnoENOENTError("No such file or directory - " + path);
        }
        boolean isDirectory = result.isDirectory();
        
        if (mustExist && !isDirectory) {
            throw runtime.newErrnoENOTDIRError(path + " is not a directory");
        } else if (!mustExist && isDirectory) {
            throw runtime.newErrnoEEXISTError("File exists - " + path); 
        }

        return result;
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
            for (int i=0; i<contents.length; i++) {
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

        String passwd = null;
        try {
            FileInputStream stream = new FileInputStream("/etc/passwd");
            int totalBytes = stream.available();
            byte[] bytes = new byte[totalBytes];
            stream.read(bytes);
            stream.close();
            passwd = new String(bytes);
        } catch (IOException e) {
            return context.getRuntime().getNil();
        }

        String[] rows = passwd.split("\n");
        int rowCount = rows.length;
        for (int i = 0; i < rowCount; i++) {
            String[] fields = rows[i].split(":");
            if (fields[0].equals(user)) {
                return context.getRuntime().newString(fields[5]);
            }
        }

        throw context.getRuntime().newArgumentError("user " + user + " doesn't exist");
    }

    public static RubyString getHomeDirectoryPath(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        RubyHash systemHash = (RubyHash) runtime.getObject().fastGetConstant("ENV_JAVA");
        RubyHash envHash = (RubyHash) runtime.getObject().fastGetConstant("ENV");
        IRubyObject home = envHash.op_aref(context, runtime.newString("HOME"));

        if (home == null || home.isNil()) {
            home = systemHash.op_aref(context, runtime.newString("user.home"));
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
