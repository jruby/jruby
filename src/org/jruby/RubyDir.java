/*
 * RubyDir.java - No description
 * Created on 18.03.2002, 15:19:50
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 * Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.NotImplementedError;
import org.jruby.exceptions.SystemCallError;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Glob;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * .The Ruby built-in class Dir.
 *
 * @author  jvoegele
 * @version $Revision$
 */
public class RubyDir extends RubyObject {
    protected String    path;
    protected File      dir;
    private   String[]  snapshot;   // snapshot of contents of directory
    private   int       pos;        // current position in directory
    private boolean isOpen = true;

    public RubyDir(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static RubyClass createDirClass(Ruby runtime) {
        RubyClass dirClass = runtime.defineClass("Dir", runtime.getClasses().getObjectClass());

        dirClass.includeModule(runtime.getModule("Enumerable"));

        CallbackFactory callbackFactory = runtime.callbackFactory();

		dirClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod(RubyDir.class, "newInstance"));
        dirClass.defineSingletonMethod("glob", callbackFactory.getSingletonMethod(RubyDir.class, "glob", RubyString.class));
        dirClass.defineSingletonMethod("entries", callbackFactory.getSingletonMethod(RubyDir.class, "entries", RubyString.class));
        dirClass.defineSingletonMethod("[]", callbackFactory.getSingletonMethod(RubyDir.class, "glob", RubyString.class));
        // dirClass.defineAlias("[]", "glob");
        dirClass.defineSingletonMethod("chdir", callbackFactory.getSingletonMethod(RubyDir.class, "chdir", RubyString.class));
        dirClass.defineSingletonMethod("chroot", callbackFactory.getSingletonMethod(RubyDir.class, "chroot", RubyString.class));
        //dirClass.defineSingletonMethod("delete", callbackFactory.getSingletonMethod(RubyDir.class, "delete", RubyString.class));
        dirClass.defineSingletonMethod("foreach", callbackFactory.getSingletonMethod(RubyDir.class, "foreach", RubyString.class));
        dirClass.defineSingletonMethod("getwd", callbackFactory.getSingletonMethod(RubyDir.class, "getwd"));
        dirClass.defineSingletonMethod("pwd", callbackFactory.getSingletonMethod(RubyDir.class, "getwd"));
        // dirClass.defineAlias("pwd", "getwd");
        dirClass.defineSingletonMethod("mkdir", callbackFactory.getOptSingletonMethod(RubyDir.class, "mkdir"));
        dirClass.defineSingletonMethod("open", callbackFactory.getSingletonMethod(RubyDir.class, "open", RubyString.class));
        dirClass.defineSingletonMethod("rmdir", callbackFactory.getSingletonMethod(RubyDir.class, "rmdir", RubyString.class));
        dirClass.defineSingletonMethod("unlink", callbackFactory.getSingletonMethod(RubyDir.class, "rmdir", RubyString.class));
        dirClass.defineSingletonMethod("delete", callbackFactory.getSingletonMethod(RubyDir.class, "rmdir", RubyString.class));
        // dirClass.defineAlias("unlink", "rmdir");
        // dirClass.defineAlias("delete", "rmdir");

        dirClass.defineMethod("close", callbackFactory.getMethod(RubyDir.class, "close"));
        dirClass.defineMethod("each", callbackFactory.getMethod(RubyDir.class, "each"));
        dirClass.defineMethod("entries", callbackFactory.getMethod(RubyDir.class, "entries"));
        dirClass.defineMethod("tell", callbackFactory.getMethod(RubyDir.class, "tell"));
        dirClass.defineAlias("pos", "tell");
        dirClass.defineMethod("seek", callbackFactory.getMethod(RubyDir.class, "seek", RubyFixnum.class));
        dirClass.defineAlias("pos=", "seek");
        dirClass.defineMethod("read", callbackFactory.getMethod(RubyDir.class, "read"));
        dirClass.defineMethod("rewind", callbackFactory.getMethod(RubyDir.class, "rewind"));
		dirClass.defineMethod("initialize", callbackFactory.getMethod(RubyDir.class, "initialize", RubyString.class));

        return dirClass;
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyDir result = new RubyDir(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    /**
     * Creates a new <code>Dir</code>.  This method takes a snapshot of the
     * contents of the directory at creation time, so changes to the contents
     * of the directory will not be reflected during the lifetime of the
     * <code>Dir</code> object returned, so a new <code>Dir</code> instance
     * must be created to reflect changes to the underlying file system.
     */
    public IRubyObject initialize(RubyString path) {
        path.checkSafeString();

        dir = new File(path.getValue());
        if (!dir.isDirectory()) {
            dir = null;
            throw ErrnoError.getErrnoError(getRuntime(), "ENOENT", path.getValue() + " is not a directory");
        }
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
     * <code>pat</code>.
     */
    public static RubyArray glob(IRubyObject recv, RubyString pat) {
        String pattern = pat.toString();
        String[] files = new Glob(pattern).getNames();
        return RubyArray.newArray(recv.getRuntime(), JavaUtil.convertJavaArrayToRuby(recv.getRuntime(), files));
    }

    /**
     * @return all entries for this Dir
     */
    public RubyArray entries() {
        return RubyArray.newArray(getRuntime(), JavaUtil.convertJavaArrayToRuby(getRuntime(), snapshot));
    }
    
    /**
     * Returns an array containing all of the filenames in the given directory.
     */
    public static RubyArray entries(IRubyObject recv, RubyString path) {
        if (".".equals(path.toString().trim())) {
            path = new RubyString(recv.getRuntime(), System.getProperty("user.dir"));
        }
        
        File directory = new File(path.toString());
        
        if (!directory.isDirectory()) {
            throw ErrnoError.getErrnoError(recv.getRuntime(), "ENOENT", 
            	"No such directory");
        }
        List fileList = getContents(directory);
		fileList.add(0,".");
		fileList.add(1,"..");
        Object[] files = fileList.toArray();
        return RubyArray.newArray(recv.getRuntime(), JavaUtil.convertJavaArrayToRuby(recv.getRuntime(), files));
    }

    /** Changes the current directory to <code>path</code> */
    public static IRubyObject chdir(IRubyObject recv, RubyString path) {
        File dir = getDir(recv.getRuntime(), path.toString());
        String realPath = null;
    
        // We get canonical path to try and flatten the path out.
        // a dir '/subdir/..' should return as '/' 
        try {
            realPath = dir.getCanonicalPath();
        } catch (IOException e) {
            realPath = dir.getAbsolutePath();
        }
        
        System.setProperty("user.dir", realPath);
        
        return RubyFixnum.newFixnum(recv.getRuntime(), 0);
    }

    /**
     * Changes the root directory (only allowed by super user).  Not available
     * on all platforms.
     */
    public static IRubyObject chroot(IRubyObject recv, RubyString path) {
        throw new NotImplementedError(recv.getRuntime(), "chroot not implemented: chroot is non-portable and is not supported.");
    }

    /**
     * Deletes the directory specified by <code>path</code>.  The directory must
     * be empty.
     */
    public static IRubyObject rmdir(IRubyObject recv, RubyString path) {
        File directory = new File(path.toString());
        
        if (!directory.delete()) {
            throw new SystemCallError(recv.getRuntime(), "No such directory");
        }
        
        return RubyFixnum.newFixnum(recv.getRuntime(), 0);
    }

    /**
     * Executes the block once for each file in the directory specified by
     * <code>path</code>.
     */
    public static IRubyObject foreach(IRubyObject recv, RubyString path) {
        path.checkSafeString();

        RubyDir dir = (RubyDir) newInstance(recv.getRuntime().getClass("Dir"),
                                            new IRubyObject[] { path });
        
        dir.each();
        return recv.getRuntime().getNil();
    }

    /** Returns the current directory. */
    public static RubyString getwd(IRubyObject recv) {
        return new RubyString(recv.getRuntime(), System.getProperty("user.dir"));
    }

    /**
     * Creates the directory specified by <code>path</code>.  Note that the
     * <code>mode</code> parameter is provided only to support existing Ruby
     * code, and is ignored.
     */
    public static IRubyObject mkdir(IRubyObject recv, IRubyObject[] args) {
        if (args.length < 1) {
            throw new ArgumentError(recv.getRuntime(), args.length, 1);
        }
        if (args.length > 2) {
            throw new ArgumentError(recv.getRuntime(), args.length, 2);
        }

        args[0].checkSafeString();
        String path = args[0].toString();

        File newDir = new File(path);
        if (newDir.exists()) {
            throw new IOError(recv.getRuntime(), path + " already exists");
        }

        return newDir.mkdir() ? RubyFixnum.zero(recv.getRuntime()) :
            RubyFixnum.one(recv.getRuntime());
    }

    /**
     * Returns a new directory object for <code>path</code>.  If a block is
     * provided, a new directory object is passed to the block, which closes the
     * directory object before terminating.
     */
    public static IRubyObject open(IRubyObject recv, RubyString path) {
        RubyDir directory = 
            (RubyDir) newInstance(recv.getRuntime().getClasses().getDirClass(),
                    new IRubyObject[] { path });

        if (recv.getRuntime().isBlockGiven()) {
            try {
                recv.getRuntime().yield(directory);
            } finally {
                directory.close();
            }
            
            return recv.getRuntime().getNil();
        }
        
        return directory;
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
    public IRubyObject each() {
        String[] contents = snapshot;
        for (int i=0; i<contents.length; i++) {
            getRuntime().yield(RubyString.newString(getRuntime(), contents[i]));
        }
        return this;
    }

    /**
     * Returns the current position in the directory.
     */
    public RubyInteger tell() {
        return RubyFixnum.newFixnum(getRuntime(), pos);
    }

    /**
     * Moves to a position <code>d</code>.  <code>pos</code> must be a value
     * returned by <code>tell</code> or 0.
     */
    public IRubyObject seek(RubyFixnum pos) {
        this.pos = (int) pos.getLongValue();
        return this;
    }

    /** Returns the next entry from this directory. */
    public RubyString read() {
	if (!isOpen) {
	    throw new IOError(getRuntime(), "Directory already closed");
	}

        if (pos >= snapshot.length) {
            return RubyString.nilString(runtime);
        }
        RubyString result = new RubyString(getRuntime(), snapshot[pos]);
        pos++;
        return result;
    }

    /** Moves position in this directory to the first entry. */
    public IRubyObject rewind() {
        pos = 0;
        return RubyFixnum.newFixnum(getRuntime(), pos);
    }

// ----- Helper Methods --------------------------------------------------------

    /** Returns a Java <code>File</code> object for the specified path.  If
     * <code>path</code> is not a directory, throws <code>IOError</code>.
     *
     * @param   path path for which to return the <code>File</code> object.
     * @throws  IOError if <code>path</code> is not a directory.
     */
    protected static File getDir(Ruby runtime, String path) {
        File result = new File(path);

        if (!result.isDirectory()) {
            throw ErrnoError.getErrnoError(runtime, "ENOENT", 
					   path + " is not a directory");
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
        for (int i=0; i<contents.length; i++) {
            result.add(new RubyString(runtime, contents[i]));
        }
        return result;
    }
}
