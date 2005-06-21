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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby.runtime.builtin.meta;

import java.io.File;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyDir;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.exceptions.ErrnoError;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOModes;

public class FileMetaClass extends IOMetaClass {
    public FileMetaClass(Ruby runtime) {
        super("File", RubyFile.class, runtime.getClasses().getIoClass());
    }

    public FileMetaClass(String name, RubyClass superClass, RubyModule parentModule) {
        super(name, RubyFile.class, superClass, parentModule);
    }

	protected void initializeClass() {
		Ruby runtime = getRuntime();
        RubyString separator = runtime.newString(File.separator);
        separator.freeze();
        defineConstant("SEPARATOR", separator);
        defineConstant("Separator", separator);

        RubyString altSeparator = runtime.newString(File.separatorChar == '/' ? "\\" : "/");
        altSeparator.freeze();
        defineConstant("ALT_SEPARATOR", altSeparator);
        
        RubyString pathSeparator = runtime.newString(File.pathSeparator);
        pathSeparator.freeze();
        defineConstant("PATH_SEPARATOR", pathSeparator);
        
        // Create constants for open flags
        setConstant("RDONLY", runtime.newFixnum(IOModes.RDONLY));
        setConstant("WRONLY", runtime.newFixnum(IOModes.WRONLY));
        setConstant("RDWR", runtime.newFixnum(IOModes.RDWR));
        setConstant("CREAT", runtime.newFixnum(IOModes.CREAT));
        setConstant("EXCL", runtime.newFixnum(IOModes.EXCL));
        setConstant("NOCTTY", runtime.newFixnum(IOModes.NOCTTY));
        setConstant("TRUNC", runtime.newFixnum(IOModes.TRUNC));
        setConstant("APPEND", runtime.newFixnum(IOModes.APPEND));
        setConstant("NONBLOCK", runtime.newFixnum(IOModes.NONBLOCK));
		
		// Create constants for flock
		setConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
		setConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
		setConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
		setConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));

        // TODO Singleton methods: atime, blockdev?, chardev?, chown, ctime, directory? 
        // TODO Singleton methods: executable?, executable_real?, extname, fnmatch, fnmatch?
        // TODO Singleton methods: ftype, grpowned?, lchmod, lchown, link, mtime, owned?
        // TODO Singleton methods: pipe?, readlink, setgid?, setuid?, size?, socket?, 
        // TODO Singleton methods: stat, sticky?, symlink, symlink?, umask, utime

        extendObject(runtime.getClasses().getFileTestModule());
        
		defineSingletonMethod("basename", Arity.optional());
        defineSingletonMethod("chmod", Arity.twoArguments());
        defineSingletonMethod("delete", Arity.optional(), "unlink");
		defineSingletonMethod("dirname", Arity.singleArgument());
        defineSingletonMethod("expand_path", Arity.optional());
		defineSingletonMethod("join", Arity.optional());
        defineSingletonMethod("lstat", Arity.singleArgument());
        defineSingletonMethod("open", Arity.optional());
        defineSingletonMethod("rename", Arity.twoArguments());
		defineSingletonMethod("split", Arity.singleArgument());
        defineSingletonMethod("stat", Arity.singleArgument(), "lstat");
        defineSingletonMethod("symlink?", Arity.singleArgument(), "symlink_p");
		defineSingletonMethod("truncate", Arity.twoArguments());
        defineSingletonMethod("unlink", Arity.optional());
		
        // TODO: Define instance methods: atime, chmod, chown, ctime, lchmod, lchown, lstat, mtime
		//defineMethod("flock", Arity.singleArgument());
		defineMethod("initialize", Arity.optional());
		defineMethod("path", Arity.noArguments());
		defineMethod("truncate", Arity.singleArgument());
    }

	public RubyClass newSubClass(String name, RubyModule parentModule) {
		return new FileMetaClass(name, this, parentModule);
	}

	public IRubyObject allocateObject() {
		return new RubyFile(getRuntime(), this);
	}
	
    public IRubyObject basename(IRubyObject[] args) {
    	checkArgumentCount(args, 1, 2);

    	String name = RubyString.stringValue(args[0]).getValue(); 
		if (name.length() > 1 && name.charAt(name.length() - 1) == '/') {
			name = name.substring(0, name.length() - 1);
		}
		
		// Paths which end in "/" or "\\" must be stripped off.
		int slashCount = 0;
		int length = name.length();
		for (int i = length - 1; i >= 0; i--) {
			char c = name.charAt(i); 
			if (c != '/' && c != '\\') {
				break;
			}
			slashCount++;
		}
		if (slashCount > 0 && length > 1) {
			name = name.substring(0, name.length() - slashCount);
		}
		
		int index = name.lastIndexOf('/');
		if (index == -1) {
			// XXX actually only on windows...
			index = name.lastIndexOf('\\');
		}
		
		if (!name.equals("/") && index != -1) {
			name = name.substring(index + 1);
		}
		
		if (args.length == 2) {
			String ext = RubyString.stringValue(args[1]).getValue();
			if (".*".equals(ext)) {
				index = name.lastIndexOf('.');
				if (index != -1) {
					name = name.substring(0, index);
				}
			} else if (name.endsWith(ext)) {
				name = name.substring(0, name.length() - ext.length());
			}
		}
		return getRuntime().newString(name).infectBy(args[0]);
	}

    public IRubyObject chmod(IRubyObject file, IRubyObject value) {
        // Java has no ability to chmod directly.  Once popen support
        // is added, we can create a ruby script to perform chmod and then
        // put it in our base distribution.  It could also be some embedded
        // eval? 
        return getRuntime().newFixnum(0);
    }
    
	public IRubyObject dirname(IRubyObject arg) {
		RubyString filename = RubyString.stringValue(arg);
		String name = filename.toString();
		if (name.length() > 1 && name.charAt(name.length() - 1) == '/') {
			name = name.substring(0, name.length() - 1);
		}
		//TODO deal with drive letters A: and UNC names 
		int index = name.lastIndexOf('/');
		if (index == -1) {
			// XXX actually, only on windows...
			index = name.lastIndexOf('\\');
		}
		if (index == -1) {
			return getRuntime().newString("."); 
		}
		if (index == 0) {
			return getRuntime().newString("/");
		}
		return getRuntime().newString(name.substring(0, index)).infectBy(filename);
	}
    
    public IRubyObject expand_path(IRubyObject[] args) {
        checkArgumentCount(args, 1, 2);
        String relativePath = RubyString.stringValue(args[0]).getValue();
		int pathLength = relativePath.length();
		
		if (pathLength >= 1 && relativePath.charAt(0) == '~') {
			// Enebo : Should ~frogger\\foo work (it doesnt in linux ruby)?
			int userEnd = relativePath.indexOf('/');
			
			if (userEnd == -1) {
				if (pathLength == 1) { 
	                // Single '~' as whole path to expand
					relativePath = RubyDir.getHomeDirectoryPath(this).getValue();
				} else {
					// No directory delimeter.  Rest of string is username
					userEnd = pathLength;
				}
			}
			
			if (userEnd == 1) {
				// '~/...' as path to expand 
				relativePath = RubyDir.getHomeDirectoryPath(this).getValue() + 
               	    relativePath.substring(1);
			} else if (userEnd > 1){
				// '~user/...' as path to expand
				String user = relativePath.substring(1, userEnd);
				IRubyObject dir = RubyDir.getHomeDirectoryPath(this, user);
					
				if (dir.isNil()) {
					throw getRuntime().newArgumentError("user " + user + " does not exist");
				} else {
					relativePath = "" + dir + 
					    (pathLength == userEnd ? "" : relativePath.substring(userEnd));
				}
			}
		}

        if (new File(relativePath).isAbsolute()) {
            return getRuntime().newString(relativePath);
        }

        String cwd = System.getProperty("user.dir");
        if (args.length == 2 && !args[1].isNil()) {
            cwd = RubyString.stringValue(args[1]).getValue();
        }

        // Something wrong we don't know the cwd...
        if (cwd == null) {
            return getRuntime().getNil();
        }

        File path = new File(cwd, relativePath);

        String extractedPath;
        try {
            extractedPath = path.getCanonicalPath();
        } catch (IOException e) {
            extractedPath = path.getAbsolutePath();
        }
        return getRuntime().newString(extractedPath);
    }
    
    public RubyString join(IRubyObject[] args) {
		RubyArray argArray = getRuntime().newArray(args);
		
		return argArray.join(getRuntime().newString(File.separator));
    }

    public IRubyObject lstat(IRubyObject filename) {
    	RubyString name = RubyString.stringValue(filename);

        return getRuntime().newRubyFileStat(new File(name.getValue()));
    }

	public IRubyObject open(IRubyObject[] args) {
	    return open(args, true);
	}
	
	public IRubyObject open(IRubyObject[] args, boolean tryToYield) {
        checkArgumentCount(args, 1, -1);
        Ruby runtime = getRuntime();
        
        RubyString pathString = RubyString.stringValue(args[0]);
	    pathString.checkSafeString();
	    String path = pathString.getValue();
	    IOModes modes = 
	    	args.length >= 2 ? getModes(args[1]) : new IOModes(runtime, IOModes.RDONLY);
	    RubyFile file = new RubyFile(runtime, this);

	    file.openInternal(path, modes);

	    if (tryToYield && getRuntime().isBlockGiven()) {
	        try {
	            getRuntime().yield(file);
	        } finally {
	            file.close();
	        }
	        
	        return getRuntime().getNil();
	    }
	    
	    return file;
	}
	
    public IRubyObject rename(IRubyObject oldName, IRubyObject newName) {
    	RubyString oldNameString = RubyString.stringValue(oldName);
    	RubyString newNameString = RubyString.stringValue(newName);
        oldNameString.checkSafeString();
        newNameString.checkSafeString();
        File oldFile = new File(oldNameString.getValue());
        
        if (!oldFile.exists()) {
            throw ErrnoError.getErrnoError(getRuntime(), "ENOENT",
                    "No such file: " + oldNameString.getValue());
        }
        oldFile.renameTo(new File(newNameString.getValue()));
        
        return RubyFixnum.zero(getRuntime());
    }
	
    public RubyArray split(IRubyObject arg) {
    	RubyString filename = RubyString.stringValue(arg);
    	
    	return filename.getRuntime().newArray(dirname(filename),
    		basename(new IRubyObject[] { filename }));
    }
    
    public IRubyObject symlink_p(IRubyObject arg1) {
    	// FIXME if possible, make this return something real (Java's lack of support for symlinks notwithstanding)
    	return getRuntime().getFalse();
    }

    // Can we produce IOError which bypasses a close?
    public IRubyObject truncate(IRubyObject arg1, IRubyObject arg2) { 
        RubyString filename = RubyString.stringValue(arg1);
        RubyFixnum newLength = (RubyFixnum) arg2.convertToType("Fixnum", "to_int", true);
        IRubyObject[] args = new IRubyObject[] { filename, getRuntime().newString("w+") };
        RubyFile file = (RubyFile) open(args, false);
        file.truncate(newLength);
        file.close();
        
        return RubyFixnum.zero(getRuntime());
    }
	
    public IRubyObject unlink(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
        	RubyString filename = RubyString.stringValue(args[i]);
            filename.checkSafeString();
            File lToDelete = new File(filename.getValue());
            if (!lToDelete.exists()) {
				throw ErrnoError.getErrnoError(getRuntime(), "ENOENT",
                        " No such file or directory - \"" + filename.getValue() + "\"");
			}
            if (!lToDelete.delete()) {
                return getRuntime().getFalse();
            }
        }
        return getRuntime().newFixnum(args.length);
    }
	
    // TODO: Figure out to_str and to_int conversion + precedence here...
	private IOModes getModes(IRubyObject object) {
		if (object instanceof RubyString) {
			return new IOModes(getRuntime(), ((RubyString)object).getValue());
		} else if (object instanceof RubyFixnum) {
			return new IOModes(getRuntime(), ((RubyFixnum)object).getLongValue());
		}

		throw getRuntime().newTypeError("Invalid type for modes");
	}
	

}
