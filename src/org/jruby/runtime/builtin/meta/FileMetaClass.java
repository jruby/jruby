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
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyDir;
import org.jruby.RubyFile;
import org.jruby.RubyFileStat;
import org.jruby.RubyFileTest;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOModes;
import org.jruby.util.JRubyFile;
import org.jruby.util.PrintfFormat;
import org.jruby.util.collections.SinglyLinkedList;

public class FileMetaClass extends IOMetaClass {
    private static final int FNM_NOESCAPE = 1;
    private static final int FNM_PATHNAME = 2;
    private static final int FNM_DOTMATCH = 4;
    private static final int FNM_CASEFOLD = 8;

    public static final PrintfFormat OCTAL_FORMATTER = new PrintfFormat("%o"); 
    
    public FileMetaClass(IRuby runtime) {
        super("File", RubyFile.class, runtime.getClass("IO"));
    }

    public FileMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
        super(name, RubyFile.class, superClass, parentCRef);
    }

    protected class FileMeta extends Meta {
		protected void initializeClass() {
			IRuby runtime = getRuntime();
	        RubyString separator = runtime.newString("/");
	        separator.freeze();
	        defineConstant("SEPARATOR", separator);
	        defineConstant("Separator", separator);
	
	        RubyString altSeparator = runtime.newString(File.separatorChar == '/' ? "\\" : "/");
	        altSeparator.freeze();
	        defineConstant("ALT_SEPARATOR", altSeparator);
	        
	        RubyString pathSeparator = runtime.newString(File.pathSeparator);
	        pathSeparator.freeze();
	        defineConstant("PATH_SEPARATOR", pathSeparator);
            
            // TODO: These were missing, so we're not handling them elsewhere?
	        setConstant("BINARY", runtime.newFixnum(32768));
            setConstant("FNM_NOESCAPE", runtime.newFixnum(FNM_NOESCAPE));
            setConstant("FNM_CASEFOLD", runtime.newFixnum(FNM_CASEFOLD));
            setConstant("FNM_DOTMATCH", runtime.newFixnum(FNM_DOTMATCH));
            setConstant("FNM_PATHNAME", runtime.newFixnum(FNM_PATHNAME));
	        
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
            
            // Create Constants class
            RubyModule constants = defineModuleUnder("Constants");
            
            // TODO: These were missing, so we're not handling them elsewhere?
            constants.setConstant("BINARY", runtime.newFixnum(32768));
            constants.setConstant("FNM_NOESCAPE", runtime.newFixnum(1));
            constants.setConstant("FNM_CASEFOLD", runtime.newFixnum(8));
            constants.setConstant("FNM_DOTMATCH", runtime.newFixnum(4));
            constants.setConstant("FNM_PATHNAME", runtime.newFixnum(2));
            
            // Create constants for open flags
            constants.setConstant("RDONLY", runtime.newFixnum(IOModes.RDONLY));
            constants.setConstant("WRONLY", runtime.newFixnum(IOModes.WRONLY));
            constants.setConstant("RDWR", runtime.newFixnum(IOModes.RDWR));
            constants.setConstant("CREAT", runtime.newFixnum(IOModes.CREAT));
            constants.setConstant("EXCL", runtime.newFixnum(IOModes.EXCL));
            constants.setConstant("NOCTTY", runtime.newFixnum(IOModes.NOCTTY));
            constants.setConstant("TRUNC", runtime.newFixnum(IOModes.TRUNC));
            constants.setConstant("APPEND", runtime.newFixnum(IOModes.APPEND));
            constants.setConstant("NONBLOCK", runtime.newFixnum(IOModes.NONBLOCK));
            
            // Create constants for flock
            constants.setConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
            constants.setConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
            constants.setConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
            constants.setConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));
	
	        // TODO Singleton methods: atime, blockdev?, chardev?, chown, directory? 
	        // TODO Singleton methods: executable?, executable_real?, 
	        // TODO Singleton methods: ftype, grpowned?, lchmod, lchown, link, mtime, owned?
	        // TODO Singleton methods: pipe?, readlink, setgid?, setuid?, socket?, 
	        // TODO Singleton methods: stat, sticky?, symlink, symlink?, umask, utime
	
	        extendObject(runtime.getModule("FileTest"));
	        
			defineFastSingletonMethod("basename", Arity.optional());
            defineFastSingletonMethod("chmod", Arity.required(2));
            defineFastSingletonMethod("chown", Arity.required(2));
	        defineFastSingletonMethod("delete", Arity.optional(), "unlink");
			defineFastSingletonMethod("dirname", Arity.singleArgument());
	        defineFastSingletonMethod("expand_path", Arity.optional());
			defineFastSingletonMethod("extname", Arity.singleArgument());
            defineFastSingletonMethod("fnmatch", Arity.optional());
            defineFastSingletonMethod("fnmatch?", Arity.optional(), "fnmatch");
			defineFastSingletonMethod("join", Arity.optional());
	        defineFastSingletonMethod("lstat", Arity.singleArgument());
            defineFastSingletonMethod("mtime", Arity.singleArgument());
            defineFastSingletonMethod("ctime", Arity.singleArgument());
	        defineSingletonMethod("open", Arity.optional());
	        defineFastSingletonMethod("rename", Arity.twoArguments());
            defineFastSingletonMethod("size?", Arity.singleArgument(), "size_p");
			defineFastSingletonMethod("split", Arity.singleArgument());
	        defineFastSingletonMethod("stat", Arity.singleArgument(), "lstat");
	        defineFastSingletonMethod("symlink?", Arity.singleArgument(), "symlink_p");
			defineFastSingletonMethod("truncate", Arity.twoArguments());
			defineFastSingletonMethod("utime", Arity.optional());
	        defineFastSingletonMethod("unlink", Arity.optional());
			
	        // TODO: Define instance methods: atime, chmod, chown, lchmod, lchown, lstat, mtime
			//defineMethod("flock", Arity.singleArgument());
            defineFastMethod("chmod", Arity.required(1));
            defineFastMethod("chown", Arity.required(1));
            defineFastMethod("ctime", Arity.noArguments());
			defineMethod("initialize", Arity.optional());
			defineFastMethod("path", Arity.noArguments());
	        defineFastMethod("stat", Arity.noArguments());
			defineFastMethod("truncate", Arity.singleArgument());
			defineFastMethod("flock", Arity.singleArgument());
			
	        RubyFileStat.createFileStatClass(runtime);
	    }
    };
    
    protected Meta getMeta() {
    	return new FileMeta();
    }

	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
		return new FileMetaClass(name, this, parentCRef);
	}

	public IRubyObject allocateObject() {
		return new RubyFile(getRuntime(), this);
	}
	
    public IRubyObject basename(IRubyObject[] args) {
    	checkArgumentCount(args, 1, 2);

    	String name = RubyString.stringValue(args[0]).toString(); 
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
			String ext = RubyString.stringValue(args[1]).toString();
			if (".*".equals(ext)) {
				index = name.lastIndexOf('.');
				if (index > 0) {  // -1 no match; 0 it is dot file not extension
					name = name.substring(0, index);
				}
			} else if (name.endsWith(ext)) {
				name = name.substring(0, name.length() - ext.length());
			}
		}
		return getRuntime().newString(name).infectBy(args[0]);
	}

    public IRubyObject chmod(IRubyObject[] args) {
        checkArgumentCount(args, 2, -1);
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw getRuntime().newErrnoENOENTError("No such file or directory - " + filename);
            }
            
            try {
                Process chmod = Runtime.getRuntime().exec("chmod " + OCTAL_FORMATTER.sprintf(mode.getLongValue()) + " " + filename);
                chmod.waitFor();
                int result = chmod.exitValue();
                if (result == 0) {
                    count++;
                }
            } catch (IOException ioe) {
                // FIXME: ignore?
            } catch (InterruptedException ie) {
                // FIXME: ignore?
            }
        }
        
        return getRuntime().newFixnum(count);
    }

    public IRubyObject chown(IRubyObject[] args) {
        checkArgumentCount(args, 2, -1);
        
        int count = 0;
        RubyInteger owner = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw getRuntime().newErrnoENOENTError("No such file or directory - " + filename);
            }
            
            try {
                Process chown = Runtime.getRuntime().exec("chown " + owner + " " + filename);
                chown.waitFor();
                int result = chown.exitValue();
                if (result == 0) {
                    count++;
                }
            } catch (IOException ioe) {
                // FIXME: ignore?
            } catch (InterruptedException ie) {
                // FIXME: ignore?
            }
        }
        
        return getRuntime().newFixnum(count);
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

	public IRubyObject extname(IRubyObject arg) {
		RubyString filename = RubyString.stringValue(arg);
		String name = filename.toString();
        int ix = name.lastIndexOf(".");
        if(ix == -1) {
            return getRuntime().newString("");
        } else {
            return getRuntime().newString(name.substring(ix));
        }
	}
    
    public IRubyObject expand_path(IRubyObject[] args) {
        checkArgumentCount(args, 1, 2);
        String relativePath = RubyString.stringValue(args[0]).toString();
		int pathLength = relativePath.length();
		
		if (pathLength >= 1 && relativePath.charAt(0) == '~') {
			// Enebo : Should ~frogger\\foo work (it doesnt in linux ruby)?
			int userEnd = relativePath.indexOf('/');
			
			if (userEnd == -1) {
				if (pathLength == 1) { 
	                // Single '~' as whole path to expand
					relativePath = RubyDir.getHomeDirectoryPath(this).toString();
				} else {
					// No directory delimeter.  Rest of string is username
					userEnd = pathLength;
				}
			}
			
			if (userEnd == 1) {
				// '~/...' as path to expand 
				relativePath = RubyDir.getHomeDirectoryPath(this).toString() + 
               	    relativePath.substring(1);
			} else if (userEnd > 1){
				// '~user/...' as path to expand
				String user = relativePath.substring(1, userEnd);
				IRubyObject dir = RubyDir.getHomeDirectoryPath(this, user);
					
				if (dir.isNil()) {
					throw getRuntime().newArgumentError("user " + user + " does not exist");
				} 
				
                relativePath = "" + dir + 
                    (pathLength == userEnd ? "" : relativePath.substring(userEnd));
			}
		}

        if (new File(relativePath).isAbsolute()) {
            return getRuntime().newString(relativePath);
        }

        String cwd = getRuntime().getCurrentDirectory();
        if (args.length == 2 && !args[1].isNil()) {
            cwd = RubyString.stringValue(args[1]).toString();
        }

        // Something wrong we don't know the cwd...
        if (cwd == null) {
            return getRuntime().getNil();
        }

        JRubyFile path = JRubyFile.create(cwd, relativePath);

        String extractedPath;
        try {
            extractedPath = path.getCanonicalPath();
        } catch (IOException e) {
            extractedPath = path.getAbsolutePath();
        }
        return getRuntime().newString(extractedPath);
    }
    
    /**
     * Returns true if path matches against pattern The pattern is not a regular expression; 
     * instead it follows rules similar to shell filename globbing. It may contain the following 
     * metacharacters:
     *   *:  Glob - match any sequence chars (re: .*).  If like begins with '.' then it doesn't.   
     *   ?:  Matches a single char (re: .).
     *   [set]:  Matches a single char in a set (re: [...]).
     *    
     */
    // Fixme: implement FNM_PATHNAME, FNM_DOTMATCH, and FNM_CASEFOLD
    public IRubyObject fnmatch(IRubyObject[] args) {
        checkArgumentCount(args, 2, -1);
        String pattern = args[0].convertToString().toString();
        RubyString path = args[1].convertToString();
        int opts = (int) (args.length > 2 ? args[2].convertToInteger().getLongValue() : 0);

        boolean dot = pattern.startsWith(".");
        
        pattern = pattern.replaceAll("(\\.)", "\\\\$1");
        pattern = pattern.replaceAll("(?<=[^\\\\])\\*", ".*");
        pattern = pattern.replaceAll("^\\*", ".*");
        pattern = pattern.replaceAll("(?<=[^\\\\])\\?", ".");
        pattern = pattern.replaceAll("^\\?", ".");
        if ((opts & FNM_NOESCAPE) != FNM_NOESCAPE) {
            pattern = pattern.replaceAll("\\\\([^\\\\*\\\\?])", "$1");
        }
        pattern = pattern.replaceAll("\\{", "\\\\{");
        pattern = pattern.replaceAll("\\}", "\\\\}");
        pattern = "^" + pattern + "$";
        
        if (path.toString().startsWith(".") && !dot) {
            return getRuntime().newBoolean(false);
        }

        return getRuntime().newBoolean(Pattern.matches(pattern, path.toString()));
    }
    
    /*
     * Fixme:  This does not have exact same semantics as RubyArray.join, but they
     * probably could be consolidated (perhaps as join(args[], sep, doChomp)).  
     */
    public RubyString join(IRubyObject[] args) {
    	boolean isTainted = false;
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < args.length; i++) {
			if (args[i].isTaint()) {
				isTainted = true;
			}
			String element;
			if (args[i] instanceof RubyString) {
				element = args[i].toString();
			} else if (args[i] instanceof RubyArray) {
				// Fixme: Need infinite recursion check to put [...] and not go into a loop
				element = join(((RubyArray) args[i]).toJavaArray()).toString();
			} else {
				element = args[i].convertToString().toString();
			}
			
			chomp(buffer);
			if (i > 0 && !element.startsWith("/") && !element.startsWith("\\")) {
				buffer.append("/");
			} 
			buffer.append(element);
		}
        
        RubyString fixedStr = RubyString.newString(getRuntime(), buffer.toString());
        fixedStr.setTaint(isTainted);
        return fixedStr;
    }
    
    private void chomp(StringBuffer buffer) {
    	int lastIndex = buffer.length() - 1;
    	
    	while (lastIndex >= 0 && (buffer.lastIndexOf("/") == lastIndex || buffer.lastIndexOf("\\") == lastIndex)) { 
    		buffer.setLength(lastIndex);
    		lastIndex--;
    	}
    }

    public IRubyObject lstat(IRubyObject filename) {
    	RubyString name = RubyString.stringValue(filename);
    	
        return getRuntime().newRubyFileStat(JRubyFile.create(getRuntime().getCurrentDirectory(),name.toString()));
    }

    public IRubyObject ctime(IRubyObject filename) {
        RubyString name = RubyString.stringValue(filename);
        return getRuntime().newTime(JRubyFile.create(getRuntime().getCurrentDirectory(),name.toString()).getParentFile().lastModified());
    }
    
    public IRubyObject mtime(IRubyObject filename) {
        RubyString name = RubyString.stringValue(filename);

        return getRuntime().newTime(JRubyFile.create(getRuntime().getCurrentDirectory(),name.toString()).lastModified());
    }

	public IRubyObject open(IRubyObject[] args) {
	    return open(args, true);
	}
	
	public IRubyObject open(IRubyObject[] args, boolean tryToYield) {
        checkArgumentCount(args, 1, -1);
        IRuby runtime = getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        
        RubyString pathString = RubyString.stringValue(args[0]);
	    pathString.checkSafeString();
	    String path = pathString.toString();

	    IOModes modes = 
	    	args.length >= 2 ? getModes(args[1]) : new IOModes(runtime, IOModes.RDONLY);
	    RubyFile file = new RubyFile(runtime, this);
        
        RubyInteger fileMode =
            args.length >= 3 ? args[2].convertToInteger() : null;

	    file.openInternal(path, modes);

        if (fileMode != null) {
            chmod(new IRubyObject[] {fileMode, pathString});
        }

        if (tryToYield && tc.isBlockGiven()) {
            IRubyObject value = getRuntime().getNil();
	        try {
	            value = tc.yield(file);
	        } finally {
	            file.close();
	        }
	        
	        return value;
	    }
	    
	    return file;
	}
	
    public IRubyObject rename(IRubyObject oldName, IRubyObject newName) {
    	RubyString oldNameString = RubyString.stringValue(oldName);
    	RubyString newNameString = RubyString.stringValue(newName);
        oldNameString.checkSafeString();
        newNameString.checkSafeString();
        JRubyFile oldFile = JRubyFile.create(getRuntime().getCurrentDirectory(),oldNameString.toString());
        JRubyFile newFile = JRubyFile.create(getRuntime().getCurrentDirectory(),newNameString.toString());

        if (!oldFile.exists() || !newFile.getParentFile().exists()) {
        	throw getRuntime().newErrnoENOENTError("No such file or directory - " + oldNameString + " or " + newNameString);
        }
        oldFile.renameTo(JRubyFile.create(getRuntime().getCurrentDirectory(),newNameString.toString()));
        
        return RubyFixnum.zero(getRuntime());
    }
    
    public IRubyObject size_p(IRubyObject filename) {
        long size = 0;
        
        try {
             FileInputStream fis = new FileInputStream(new File(filename.toString()));
             FileChannel chan = fis.getChannel();
             size = chan.size();
             chan.close();
             fis.close();
        } catch (IOException ioe) {
            // missing files or inability to open should just return nil
        }
        
        if (size == 0) {
            return getRuntime().getNil();
        }
        
        return getRuntime().newFixnum(size);
    }
	
    public RubyArray split(IRubyObject arg) {
    	RubyString filename = RubyString.stringValue(arg);
    	
    	return filename.getRuntime().newArray(dirname(filename),
    		basename(new IRubyObject[] { filename }));
    }
    
    public IRubyObject symlink_p(IRubyObject arg1) {
    	RubyString filename = RubyString.stringValue(arg1);
        
        JRubyFile file = JRubyFile.create(getRuntime().getCurrentDirectory(), filename.toString());
        
        try {
            // Only way to determine symlink is to compare canonical and absolute files
            // However symlinks in containing path must not produce false positives, so we check that first
            File absoluteParent = file.getAbsoluteFile().getParentFile();
            File canonicalParent = file.getAbsoluteFile().getParentFile().getCanonicalFile();

            if (canonicalParent.getAbsolutePath().equals(absoluteParent.getAbsolutePath())) {
                // parent doesn't change when canonicalized, compare absolute and canonical file directly
                return file.getAbsolutePath().equals(file.getCanonicalPath()) ? getRuntime().getFalse() : getRuntime().getTrue();
            }

            // directory itself has symlinks (canonical != absolute), so build new path with canonical parent and compare
            file = JRubyFile.create(getRuntime().getCurrentDirectory(), canonicalParent.getAbsolutePath() + "/" + file.getName());
            return file.getAbsolutePath().equals(file.getCanonicalPath()) ? getRuntime().getFalse() : getRuntime().getTrue();
        } catch (IOException ioe) {
            // problem canonicalizing the file; nothing we can do but return false
            return getRuntime().getFalse();
        }
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

    /**
     * This method does NOT set atime, only mtime, since Java doesn't support anything else.
     */
    public IRubyObject utime(IRubyObject[] args) {
        checkArgumentCount(args, 2, -1);
        
        // Ignore access_time argument since Java does not support it.
        
        long mtime;
        if (args[1] instanceof RubyTime) {
            mtime = ((RubyTime) args[1]).getJavaDate().getTime();
        } else if (args[1] instanceof RubyNumeric) {
            mtime = RubyNumeric.num2long(args[1]);
        } else {
            mtime = 0;
        }
        
        for (int i = 2, j = args.length; i < j; i++) {
            RubyString filename = RubyString.stringValue(args[i]);
            filename.checkSafeString();
            JRubyFile fileToTouch = JRubyFile.create(getRuntime().getCurrentDirectory(),filename.toString());
            
            if (!fileToTouch.exists()) {
                throw getRuntime().newErrnoENOENTError(" No such file or directory - \"" + 
                        filename + "\"");
            }
            
            fileToTouch.setLastModified(mtime);
        }
        
        return getRuntime().newFixnum(args.length - 2);
    }
	
    public IRubyObject unlink(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
        	RubyString filename = RubyString.stringValue(args[i]);
            filename.checkSafeString();
            JRubyFile lToDelete = JRubyFile.create(getRuntime().getCurrentDirectory(),filename.toString());
            if (!lToDelete.exists()) {
				throw getRuntime().newErrnoENOENTError(" No such file or directory - \"" + filename + "\"");
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
			return new IOModes(getRuntime(), ((RubyString)object).toString());
		} else if (object instanceof RubyFixnum) {
			return new IOModes(getRuntime(), ((RubyFixnum)object).getLongValue());
		}

		throw getRuntime().newTypeError("Invalid type for modes");
	}
	

}
