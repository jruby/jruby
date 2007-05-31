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
import java.util.Iterator;
import java.util.List;

import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
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
    
    private static ObjectAllocator DIR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyDir(runtime, klass);
        }
    };

    public static RubyClass createDirClass(Ruby runtime) {
        RubyClass dirClass = runtime.defineClass("Dir", runtime.getObject(), DIR_ALLOCATOR);

        dirClass.includeModule(runtime.getModule("Enumerable"));

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyDir.class);

        dirClass.getMetaClass().defineMethod("glob", callbackFactory.getOptSingletonMethod("glob"));
        dirClass.getMetaClass().defineFastMethod("entries", callbackFactory.getFastSingletonMethod("entries", RubyKernel.IRUBY_OBJECT));
        dirClass.getMetaClass().defineMethod("[]", callbackFactory.getOptSingletonMethod("glob"));
        dirClass.getMetaClass().defineMethod("chdir", callbackFactory.getOptSingletonMethod("chdir"));
        dirClass.getMetaClass().defineFastMethod("chroot", callbackFactory.getFastSingletonMethod("chroot", RubyKernel.IRUBY_OBJECT));
        //dirClass.defineSingletonMethod("delete", callbackFactory.getSingletonMethod(RubyDir.class, "delete", RubyString.class));
        dirClass.getMetaClass().defineMethod("foreach", callbackFactory.getSingletonMethod("foreach", RubyKernel.IRUBY_OBJECT));
        dirClass.getMetaClass().defineFastMethod("getwd", callbackFactory.getFastSingletonMethod("getwd"));
        dirClass.getMetaClass().defineFastMethod("pwd", callbackFactory.getFastSingletonMethod("getwd"));
        // dirClass.defineAlias("pwd", "getwd");
        dirClass.getMetaClass().defineFastMethod("mkdir", callbackFactory.getFastOptSingletonMethod("mkdir"));
        dirClass.getMetaClass().defineMethod("open", callbackFactory.getSingletonMethod("open", RubyKernel.IRUBY_OBJECT));
        dirClass.getMetaClass().defineFastMethod("rmdir", callbackFactory.getFastSingletonMethod("rmdir", RubyKernel.IRUBY_OBJECT));
        dirClass.getMetaClass().defineFastMethod("unlink", callbackFactory.getFastSingletonMethod("rmdir", RubyKernel.IRUBY_OBJECT));
        dirClass.getMetaClass().defineFastMethod("delete", callbackFactory.getFastSingletonMethod("rmdir", RubyKernel.IRUBY_OBJECT));
        // dirClass.defineAlias("unlink", "rmdir");
        // dirClass.defineAlias("delete", "rmdir");

        dirClass.defineFastMethod("close", callbackFactory.getFastMethod("close"));
        dirClass.defineMethod("each", callbackFactory.getMethod("each"));
        dirClass.defineFastMethod("entries", callbackFactory.getFastMethod("entries"));
        dirClass.defineFastMethod("path", callbackFactory.getFastMethod("path"));
        dirClass.defineFastMethod("tell", callbackFactory.getFastMethod("tell"));
        dirClass.defineAlias("pos", "tell");
        dirClass.defineFastMethod("seek", callbackFactory.getFastMethod("seek", RubyKernel.IRUBY_OBJECT));
        dirClass.defineFastMethod("pos=", callbackFactory.getFastMethod("setPos", RubyKernel.IRUBY_OBJECT));
        dirClass.defineFastMethod("read", callbackFactory.getFastMethod("read"));
        dirClass.defineFastMethod("rewind", callbackFactory.getFastMethod("rewind"));
        dirClass.defineMethod("initialize", callbackFactory.getMethod("initialize", RubyKernel.IRUBY_OBJECT));

        return dirClass;
    }

    /**
     * Creates a new <code>Dir</code>.  This method takes a snapshot of the
     * contents of the directory at creation time, so changes to the contents
     * of the directory will not be reflected during the lifetime of the
     * <code>Dir</code> object returned, so a new <code>Dir</code> instance
     * must be created to reflect changes to the underlying file system.
     */
    public IRubyObject initialize(IRubyObject _newPath, Block unusedBlock) {
        RubyString newPath = _newPath.convertToString();
        getRuntime().checkSafeString(newPath);
        dir = JRubyFile.create(getRuntime().getCurrentDirectory(),newPath.toString());
        if (!dir.isDirectory()) {
            dir = null;
            throw getRuntime().newErrnoENOENTError(newPath.toString() + " is not a directory");
        }
        path = newPath;
		List snapshotList = new ArrayList();
		snapshotList.add(".");
		snapshotList.add("..");
		snapshotList.addAll(getContents(dir));
		snapshot = (String[]) snapshotList.toArray(new String[snapshotList.size()]);
		pos = 0;

        return this;
    }

// ----- Ruby Class Methods ----------------------------------------------------

    /**
     * Returns an array of filenames matching the specified wildcard pattern
     * <code>pat</code>. If a block is given, the array is iterated internally
     * with each filename is passed to the block in turn. In this case, Nil is
     * returned.  
     */
    public static IRubyObject glob(IRubyObject recv, IRubyObject[] args, Block block) {
        String cwd = recv.getRuntime().getCurrentDirectory();
        int flags = 0;
        if(Arity.checkArgumentCount(recv.getRuntime(),args,1,2) == 2) {
            flags = RubyNumeric.num2int(args[1]);
        }
        ByteList pt = args[0].convertToString().getByteList();

        String cwd2;
        try {
            cwd2 = new org.jruby.util.NormalizedFile(cwd).getCanonicalPath();
        } catch(Exception e) {
            cwd2 = cwd;
        }

        List l = Dir.push_glob(cwd2, pt.bytes, 0, pt.realSize, flags);
        
        if(block.isGiven()) {
            ThreadContext context = recv.getRuntime().getCurrentContext();
            for(Iterator iter = l.iterator(); iter.hasNext(); ) {
                block.yield(context, RubyString.newString(recv.getRuntime(),(ByteList)iter.next()));
            }
            return recv.getRuntime().getNil();
        }
        IRubyObject[] l2 = new IRubyObject[l.size()];
        int i=0;
        for(Iterator iter = l.iterator(); iter.hasNext(); i++) {
            l2[i] = RubyString.newString(recv.getRuntime(),(ByteList)iter.next());
        }
        return recv.getRuntime().newArrayNoCopy(l2);
    }

    /**
     * @return all entries for this Dir
     */
    public RubyArray entries() {
        return getRuntime().newArrayNoCopy(JavaUtil.convertJavaArrayToRuby(getRuntime(), snapshot));
    }
    
    /**
     * Returns an array containing all of the filenames in the given directory.
     */
    public static RubyArray entries(IRubyObject recv, IRubyObject path) {
        final JRubyFile directory = JRubyFile.create(recv.getRuntime().getCurrentDirectory(),path.convertToString().toString());
        
        if (!directory.isDirectory()) {
            throw recv.getRuntime().newErrnoENOENTError("No such directory");
        }
        List fileList = getContents(directory);
		fileList.add(0,".");
		fileList.add(1,"..");
        Object[] files = fileList.toArray();
        return recv.getRuntime().newArrayNoCopy(JavaUtil.convertJavaArrayToRuby(recv.getRuntime(), files));
    }

    /** Changes the current directory to <code>path</code> */
    public static IRubyObject chdir(IRubyObject recv, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(recv.getRuntime(), args, 0, 1);
        RubyString path = args.length == 1 ? 
            (RubyString) args[0].convertToString() : getHomeDirectoryPath(recv); 
        JRubyFile dir = getDir(recv.getRuntime(), path.toString(), true);
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
                result = block.yield(recv.getRuntime().getCurrentContext(), path);
            } finally {
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
    public static IRubyObject chroot(IRubyObject recv, IRubyObject path) {
        throw recv.getRuntime().newNotImplementedError("chroot not implemented: chroot is non-portable and is not supported.");
    }

    /**
     * Deletes the directory specified by <code>path</code>.  The directory must
     * be empty.
     */
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
    public static IRubyObject foreach(IRubyObject recv, IRubyObject _path, Block block) {
        RubyString path = _path.convertToString();
        recv.getRuntime().checkSafeString(path);

        RubyClass dirClass = recv.getRuntime().getClass("Dir");
        RubyDir dir = (RubyDir) dirClass.newInstance(new IRubyObject[] { path }, block);
        
        dir.each(block);
        return recv.getRuntime().getNil();
    }

    /** Returns the current directory. */
    public static RubyString getwd(IRubyObject recv) {
        return recv.getRuntime().newString(recv.getRuntime().getCurrentDirectory());
    }

    /**
     * Creates the directory specified by <code>path</code>.  Note that the
     * <code>mode</code> parameter is provided only to support existing Ruby
     * code, and is ignored.
     */
    public static IRubyObject mkdir(IRubyObject recv, IRubyObject[] args) {
        if (args.length < 1) {
            throw recv.getRuntime().newArgumentError(args.length, 1);
        }
        if (args.length > 2) {
            throw recv.getRuntime().newArgumentError(args.length, 2);
        }

        recv.getRuntime().checkSafeString(args[0]);
        String path = args[0].toString();

        File newDir = getDir(recv.getRuntime(), path, false);
        if (File.separatorChar == '\\') {
            newDir = new File(newDir.getPath());
        }
        
        return newDir.mkdirs() ? RubyFixnum.zero(recv.getRuntime()) :
            RubyFixnum.one(recv.getRuntime());
    }

    /**
     * Returns a new directory object for <code>path</code>.  If a block is
     * provided, a new directory object is passed to the block, which closes the
     * directory object before terminating.
     */
    public static IRubyObject open(IRubyObject recv, IRubyObject path, Block block) {
        RubyDir directory = 
            (RubyDir) recv.getRuntime().getClass("Dir").newInstance(
                    new IRubyObject[] { path }, Block.NULL_BLOCK);

        if (!block.isGiven()) return directory;
        
        try {
            block.yield(recv.getRuntime().getCurrentContext(), directory);
        } finally {
            directory.close();
        }
            
        return recv.getRuntime().getNil();
    }

// ----- Ruby Instance Methods -------------------------------------------------

    /**
     * Closes the directory stream.
     */
    public IRubyObject close() {
        // Make sure any read()s after close fail.
        isOpen = false;

        return getRuntime().getNil();
    }

    /**
     * Executes the block once for each entry in the directory.
     */
    public IRubyObject each(Block block) {
        String[] contents = snapshot;
        ThreadContext context = getRuntime().getCurrentContext();
        for (int i=0; i<contents.length; i++) {
            block.yield(context, getRuntime().newString(contents[i]));
        }
        return this;
    }

    /**
     * Returns the current position in the directory.
     */
    public RubyInteger tell() {
        return getRuntime().newFixnum(pos);
    }

    /**
     * Moves to a position <code>d</code>.  <code>pos</code> must be a value
     * returned by <code>tell</code> or 0.
     */
    public IRubyObject seek(IRubyObject newPos) {
        setPos(newPos);
        return this;
    }
    
    public IRubyObject setPos(IRubyObject newPos) {
        this.pos = RubyNumeric.fix2int(newPos);
        return newPos;
    }

    public IRubyObject path() {
        if (!isOpen) {
            throw getRuntime().newIOError("closed directory");
        }
        
        return path;
    }

    /** Returns the next entry from this directory. */
    public IRubyObject read() {
	if (!isOpen) {
	    throw getRuntime().newIOError("Directory already closed");
	}

        if (pos >= snapshot.length) {
            return getRuntime().getNil();
        }
        RubyString result = getRuntime().newString(snapshot[pos]);
        pos++;
        return result;
    }

    /** Moves position in this directory to the first entry. */
    public IRubyObject rewind() {
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) throw getRuntime().newSecurityError("Insecure: can't close");
        
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
        boolean isDirectory = result.isDirectory();
        
        if (mustExist && !isDirectory) {
            throw runtime.newErrnoENOENTError(path + " is not a directory");
        } else if (!mustExist && isDirectory) {
            throw runtime.newErrnoEEXISTError("File exists - " + path); 
        }

        return result;
    }

    /**
     * Returns the contents of the specified <code>directory</code> as an
     * <code>ArrayList</code> containing the names of the files as Java Strings.
     */
    protected static List getContents(File directory) {
        String[] contents = directory.list();
        List result = new ArrayList();

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
    protected static List getContents(File directory, Ruby runtime) {
        List result = new ArrayList();
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
	public static IRubyObject getHomeDirectoryPath(IRubyObject recv, 
			String user) {
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
			passwd = new String(bytes);
		} catch (IOException e) {
			return recv.getRuntime().getNil();
		}
		
		String[] rows = passwd.split("\n");
		int rowCount = rows.length;
		for (int i = 0; i < rowCount; i++) {
			String[] fields = rows[i].split(":");
			if (fields[0].equals(user)) {
				return recv.getRuntime().newString(fields[5]);
			}
		}
		
		throw recv.getRuntime().newArgumentError("user " + user + " doesn't exist");
	}
	
	public static RubyString getHomeDirectoryPath(IRubyObject recv) {
        RubyHash systemHash = (RubyHash) recv.getRuntime().getObject().getConstant("ENV_JAVA");
        RubyHash envHash = (RubyHash) recv.getRuntime().getObject().getConstant("ENV");
        IRubyObject  home = envHash.aref(recv.getRuntime().newString("HOME"));

        if (home == null || home.isNil()) {
            home = systemHash.aref(recv.getRuntime().newString("user.home"));
        }
        
		if (home == null || home.isNil()) {
			home = envHash.aref(recv.getRuntime().newString("LOGDIR"));
		}
		
		if (home == null || home.isNil()) {
			throw recv.getRuntime().newArgumentError("user.home/LOGDIR not set");
		}
		
		return (RubyString) home;
	}
}
