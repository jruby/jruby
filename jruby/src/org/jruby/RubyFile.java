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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Chmod;
import org.jruby.util.IOHandler;
import org.jruby.util.IOHandlerNull;
import org.jruby.util.IOHandlerSeekable;
import org.jruby.util.IOHandlerUnseekable;
import org.jruby.util.IOModes;
import org.jruby.util.JRubyFile;
import org.jruby.util.ShellLauncher;
import org.jruby.util.Sprintf;
import org.jruby.util.IOHandler.InvalidValueException;

/**
 * Ruby File class equivalent in java.
 *
 * @author jpetersen
 **/
public class RubyFile extends RubyIO {
	public static final int LOCK_SH = 1;
	public static final int LOCK_EX = 2;
	public static final int LOCK_NB = 4;
	public static final int LOCK_UN = 8;
    
    private static final int FNM_NOESCAPE = 1;
    private static final int FNM_PATHNAME = 2;
    private static final int FNM_DOTMATCH = 4;
    private static final int FNM_CASEFOLD = 8;
    
    protected String path;
    private FileLock currentLock;
    
	public RubyFile(Ruby runtime, RubyClass type) {
	    super(runtime, type);
	}

	public RubyFile(Ruby runtime, String path) {
		this(runtime, path, open(runtime, path));
    }

	// use static function because constructor call must be first statement in above constructor 
	private static InputStream open(Ruby runtime, String path) {
		try {
			return new FileInputStream(path);
		} catch (FileNotFoundException e) {
            throw runtime.newIOError(e.getMessage());
        }
	}
    
	// XXX This constructor is a hack to implement the __END__ syntax.
	//     Converting a reader back into an InputStream doesn't generally work.
	public RubyFile(Ruby runtime, String path, final Reader reader) {
		this(runtime, path, new InputStream() {
			public int read() throws IOException {
				return reader.read();
			}
		});
	}
	
	private RubyFile(Ruby runtime, String path, InputStream in) {
        super(runtime, runtime.getClass("File"));
        this.path = path;
		try {
            this.handler = new IOHandlerUnseekable(runtime, in, null);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());  
        }
        this.modes = handler.getModes();
        registerIOHandler(handler);
	}
    
    private static ObjectAllocator FILE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFile instance = new RubyFile(runtime, klass);
            
            instance.setMetaClass(klass);
            
            return instance;
        }
    };
    
    public static RubyClass createFileClass(Ruby runtime) {
        RubyClass fileClass = runtime.defineClass("File", runtime.getClass("IO"), FILE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFile.class);   
        RubyClass fileMetaClass = fileClass.getMetaClass();
        RubyString separator = runtime.newString("/");
        
        separator.freeze();
        fileClass.defineConstant("SEPARATOR", separator);
        fileClass.defineConstant("Separator", separator);
        
        RubyString altSeparator = runtime.newString(File.separatorChar == '/' ? "\\" : "/");
        altSeparator.freeze();
        fileClass.defineConstant("ALT_SEPARATOR", altSeparator);
        
        RubyString pathSeparator = runtime.newString(File.pathSeparator);
        pathSeparator.freeze();
        fileClass.defineConstant("PATH_SEPARATOR", pathSeparator);
        
        // TODO: These were missing, so we're not handling them elsewhere?
        // FIXME: The old value, 32786, didn't match what IOModes expected, so I reference
        // the constant here. THIS MAY NOT BE THE CORRECT VALUE.
        fileClass.setConstant("BINARY", runtime.newFixnum(IOModes.BINARY));
        fileClass.setConstant("FNM_NOESCAPE", runtime.newFixnum(FNM_NOESCAPE));
        fileClass.setConstant("FNM_CASEFOLD", runtime.newFixnum(FNM_CASEFOLD));
        fileClass.setConstant("FNM_DOTMATCH", runtime.newFixnum(FNM_DOTMATCH));
        fileClass.setConstant("FNM_PATHNAME", runtime.newFixnum(FNM_PATHNAME));
        
        // Create constants for open flags
        fileClass.setConstant("RDONLY", runtime.newFixnum(IOModes.RDONLY));
        fileClass.setConstant("WRONLY", runtime.newFixnum(IOModes.WRONLY));
        fileClass.setConstant("RDWR", runtime.newFixnum(IOModes.RDWR));
        fileClass.setConstant("CREAT", runtime.newFixnum(IOModes.CREAT));
        fileClass.setConstant("EXCL", runtime.newFixnum(IOModes.EXCL));
        fileClass.setConstant("NOCTTY", runtime.newFixnum(IOModes.NOCTTY));
        fileClass.setConstant("TRUNC", runtime.newFixnum(IOModes.TRUNC));
        fileClass.setConstant("APPEND", runtime.newFixnum(IOModes.APPEND));
        fileClass.setConstant("NONBLOCK", runtime.newFixnum(IOModes.NONBLOCK));
        
        // Create constants for flock
        fileClass.setConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
        fileClass.setConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
        fileClass.setConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
        fileClass.setConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));
        
        // Create Constants class
        RubyModule constants = fileClass.defineModuleUnder("Constants");
        
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
        
        runtime.getModule("FileTest").extend_object(fileClass);
        
        fileMetaClass.defineFastMethod("basename", callbackFactory.getFastOptSingletonMethod("basename"));
        fileMetaClass.defineFastMethod("chmod", callbackFactory.getFastOptSingletonMethod("chmod"));
        fileMetaClass.defineFastMethod("chown", callbackFactory.getFastOptSingletonMethod("chown"));
        fileMetaClass.defineFastMethod("delete", callbackFactory.getFastOptSingletonMethod("unlink"));
        fileMetaClass.defineFastMethod("dirname", callbackFactory.getFastSingletonMethod("dirname", IRubyObject.class));
        fileMetaClass.defineFastMethod("expand_path", callbackFactory.getFastOptSingletonMethod("expand_path"));
        fileMetaClass.defineFastMethod("extname", callbackFactory.getFastSingletonMethod("extname", IRubyObject.class));
        fileMetaClass.defineFastMethod("fnmatch", callbackFactory.getFastOptSingletonMethod("fnmatch"));
        fileMetaClass.defineFastMethod("fnmatch?", callbackFactory.getFastOptSingletonMethod("fnmatch"));
        fileMetaClass.defineFastMethod("join", callbackFactory.getFastOptSingletonMethod("join"));
        fileMetaClass.defineFastMethod("lstat", callbackFactory.getFastSingletonMethod("lstat", IRubyObject.class));
        fileMetaClass.defineFastMethod("mtime", callbackFactory.getFastSingletonMethod("mtime", IRubyObject.class));
        fileMetaClass.defineFastMethod("ctime", callbackFactory.getFastSingletonMethod("ctime", IRubyObject.class));
        fileMetaClass.defineMethod("open", callbackFactory.getOptSingletonMethod("open"));
        fileMetaClass.defineFastMethod("rename", callbackFactory.getFastSingletonMethod("rename", IRubyObject.class, IRubyObject.class));
        fileMetaClass.defineFastMethod("size?", callbackFactory.getFastSingletonMethod("size_p", IRubyObject.class));
        fileMetaClass.defineFastMethod("split", callbackFactory.getFastSingletonMethod("split", IRubyObject.class));
        fileMetaClass.defineFastMethod("stat", callbackFactory.getFastSingletonMethod("lstat", IRubyObject.class));
        fileMetaClass.defineFastMethod("symlink", callbackFactory.getFastSingletonMethod("symlink", IRubyObject.class, IRubyObject.class));
        fileMetaClass.defineFastMethod("symlink?", callbackFactory.getFastSingletonMethod("symlink_p", IRubyObject.class));
        fileMetaClass.defineFastMethod("truncate", callbackFactory.getFastSingletonMethod("truncate", IRubyObject.class, IRubyObject.class));
        fileMetaClass.defineFastMethod("utime", callbackFactory.getFastOptSingletonMethod("utime"));
        fileMetaClass.defineFastMethod("unlink", callbackFactory.getFastOptSingletonMethod("unlink"));
        
        // TODO: Define instance methods: atime, chmod, chown, lchmod, lchown, lstat, mtime
        //defineMethod("flock", Arity.singleArgument());
        fileClass.defineFastMethod("chmod", callbackFactory.getFastMethod("chmod", IRubyObject.class));
        fileClass.defineFastMethod("chown", callbackFactory.getFastMethod("chown", IRubyObject.class));
        fileClass.defineFastMethod("ctime", callbackFactory.getFastMethod("ctime"));
        fileClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        fileClass.defineFastMethod("path", callbackFactory.getFastMethod("path"));
        fileClass.defineFastMethod("stat", callbackFactory.getFastMethod("stat"));
        fileClass.defineFastMethod("truncate", callbackFactory.getFastMethod("truncate", IRubyObject.class));
        fileClass.defineFastMethod("flock", callbackFactory.getFastMethod("flock", IRubyObject.class));
        
        RubyFileStat.createFileStatClass(runtime);
        
        return fileClass;
    }
    
    public void openInternal(String newPath, IOModes newModes) {
        this.path = newPath;
        this.modes = newModes;
        
        try {
            if (newPath.equals("/dev/null")) {
                handler = new IOHandlerNull(getRuntime(), newModes);
            } else {
                handler = new IOHandlerSeekable(getRuntime(), newPath, newModes);
            }
            
            registerIOHandler(handler);
        } catch (InvalidValueException e) {
        	throw getRuntime().newErrnoEINVALError();
        } catch (FileNotFoundException e) {
        	throw getRuntime().newErrnoENOENTError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
		}
    }
    
    public IRubyObject close() {
        // Make sure any existing lock is released before we try and close the file
        if (currentLock != null) {
            try {
                currentLock.release();
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
        }
        return super.close();
    }

	public IRubyObject flock(IRubyObject lockingConstant) {
        FileChannel fileChannel = handler.getFileChannel();
        int lockMode = (int) ((RubyFixnum) lockingConstant.convertToType(getRuntime().getFixnum(), MethodIndex.TO_INT, "to_int", 
            true)).getLongValue();

        try {
			switch(lockMode) {
			case LOCK_UN:
				if (currentLock != null) {
					currentLock.release();
					currentLock = null;
					
					return getRuntime().newFixnum(0);
				}
				break;
			case LOCK_EX:
			case LOCK_EX | LOCK_NB:
				if (currentLock != null) {
					currentLock.release();
					currentLock = null;
				}
				currentLock = fileChannel.tryLock();
				if (currentLock != null) {
					return getRuntime().newFixnum(0);
				}

				break;
			case LOCK_SH:
			case LOCK_SH | LOCK_NB:
				if (currentLock != null) {
					currentLock.release();
					currentLock = null;
				}
				
				currentLock = fileChannel.tryLock(0L, Long.MAX_VALUE, true);
				if (currentLock != null) {
					return getRuntime().newFixnum(0);
				}

				break;
			default:	
			}
        } catch (IOException ioe) {
        	throw new RaiseException(new NativeException(getRuntime(), getRuntime().getClass("IOError"), ioe));
        }
		
		return getRuntime().getFalse();
	}

	public IRubyObject initialize(IRubyObject[] args, Block block) {
	    if (args.length == 0) {
	        throw getRuntime().newArgumentError(0, 1);
	    }

	    getRuntime().checkSafeString(args[0]);
	    path = args[0].toString();
	    modes = args.length > 1 ? getModes(getRuntime(), args[1]) :
	    	new IOModes(getRuntime(), IOModes.RDONLY);
	    
	    // One of the few places where handler may be null.
	    // If handler is not null, it indicates that this object
	    // is being reused.
	    if (handler != null) {
	        close();
	    }
	    openInternal(path, modes);
	    
	    if (block.isGiven()) {
	        // getRuby().getRuntime().warn("File::new does not take block; use File::open instead");
	    }
	    return this;
	}

    public IRubyObject chmod(IRubyObject arg) {
        RubyInteger mode = arg.convertToInteger();
        File file = new File(path);

        if (!file.exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + path);
        }
        
        String modeString = Sprintf.sprintf(getRuntime(), "%o", mode.getLongValue()).toString();
        Chmod.chmod(file, modeString);
        
        return getRuntime().newFixnum(0);
    }

    public IRubyObject chown(IRubyObject arg) {
        RubyInteger owner = arg.convertToInteger();
        if (!new File(path).exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + path);
        }
            
        try {
            Process chown = Runtime.getRuntime().exec("chown " + owner + " " + path);
            chown.waitFor();
        } catch (IOException ioe) {
            // FIXME: ignore?
        } catch (InterruptedException ie) {
            // FIXME: ignore?
        }
        
        return getRuntime().newFixnum(0);
    }

    public IRubyObject ctime() {
        return getRuntime().newTime(JRubyFile.create(getRuntime().getCurrentDirectory(),this.path).getParentFile().lastModified());
    }

	public RubyString path() {
		return getRuntime().newString(path);
	}

	public IRubyObject stat() {
        return getRuntime().newRubyFileStat(path);
	}
	
    public IRubyObject truncate(IRubyObject arg) {
    	RubyFixnum newLength = (RubyFixnum) arg.convertToType(getRuntime().getFixnum(), MethodIndex.TO_INT, "to_int", true);
        try {
            handler.truncate(newLength.getLongValue());
        } catch (IOHandler.PipeException e) {
        	throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            // Should we do anything?
        }
        
        return RubyFixnum.zero(getRuntime());
    }
    
    public String toString() {
        return "RubyFile(" + path + ", " + modes + ", " + fileno + ")";
    }

    // TODO: This is also defined in the MetaClass too...Consolidate somewhere.
	private static IOModes getModes(Ruby runtime, IRubyObject object) {
		if (object instanceof RubyString) {
			return new IOModes(runtime, ((RubyString)object).toString());
		} else if (object instanceof RubyFixnum) {
			return new IOModes(runtime, ((RubyFixnum)object).getLongValue());
		}

		throw runtime.newTypeError("Invalid type for modes");
	}

    public IRubyObject inspect() {
        StringBuffer val = new StringBuffer();
        val.append("#<File:").append(path);
        if(!isOpen()) {
            val.append(" (closed)");
        }
        val.append(">");
        return getRuntime().newString(val.toString());
    }
    
    /* File class methods */
    
    public static IRubyObject basename(IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(recv.getRuntime(), args, 1, 2);
        
        String name = RubyString.stringValue(args[0]).toString();
        while (name.length() > 1 && name.charAt(name.length() - 1) == '/') {
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
        return recv.getRuntime().newString(name).infectBy(args[0]);
    }
    
    public static IRubyObject chmod(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        Arity.checkArgumentCount(runtime, args, 2, -1);
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw runtime.newErrnoENOENTError("No such file or directory - " + filename);
            }
            
            String modeString = Sprintf.sprintf(runtime, "%o", mode.getLongValue()).toString();
            boolean result = Chmod.chmod(JRubyFile.create(runtime.getCurrentDirectory(), filename.toString()), modeString);
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }
    
    public static IRubyObject chown(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        Arity.checkArgumentCount(runtime, args, 2, -1);
        
        int count = 0;
        RubyInteger owner = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            IRubyObject filename = args[i];
            
            if (!RubyFileTest.exist_p(filename, filename.convertToString()).isTrue()) {
                throw runtime.newErrnoENOENTError("No such file or directory - " + filename);
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
        
        return runtime.newFixnum(count);
    }
    
    public static IRubyObject dirname(IRubyObject recv, IRubyObject arg) {
        RubyString filename = RubyString.stringValue(arg);
        String name = filename.toString().replace('\\', '/');
        while (name.length() > 1 && name.charAt(name.length() - 1) == '/') {
            name = name.substring(0, name.length() - 1);
        }
        //TODO deal with drive letters A: and UNC names
        int index = name.lastIndexOf('/');
        if (index == -1) return recv.getRuntime().newString(".");
        if (index == 0) return recv.getRuntime().newString("/");

        return recv.getRuntime().newString(name.substring(0, index)).infectBy(filename);
    }
    
    public static IRubyObject extname(IRubyObject recv, IRubyObject arg) {
        RubyString filename = RubyString.stringValue(arg);
        
        String name = filename.toString();
        
        // trim off dir name, since it may have dots in it
        //TODO deal with drive letters A: and UNC names
        int index = name.lastIndexOf('/');
        // XXX actually, only on windows...
        if (index == -1) index = name.lastIndexOf('\\');

        name = name.substring(index + 1);
        
        int ix = name.lastIndexOf(".");
        
        return recv.getRuntime().newString(ix == -1 ? "" : name.substring(ix));
    }
    
    public static IRubyObject expand_path(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        Arity.checkArgumentCount(runtime, args, 1, 2);
        String relativePath = RubyString.stringValue(args[0]).toString();
        int pathLength = relativePath.length();
        
        if (pathLength >= 1 && relativePath.charAt(0) == '~') {
            // Enebo : Should ~frogger\\foo work (it doesnt in linux ruby)?
            int userEnd = relativePath.indexOf('/');
            
            if (userEnd == -1) {
                if (pathLength == 1) {
                    // Single '~' as whole path to expand
                    relativePath = RubyDir.getHomeDirectoryPath(recv).toString();
                } else {
                    // No directory delimeter.  Rest of string is username
                    userEnd = pathLength;
                }
            }
            
            if (userEnd == 1) {
                // '~/...' as path to expand
                relativePath = RubyDir.getHomeDirectoryPath(recv).toString() +
                        relativePath.substring(1);
            } else if (userEnd > 1){
                // '~user/...' as path to expand
                String user = relativePath.substring(1, userEnd);
                IRubyObject dir = RubyDir.getHomeDirectoryPath(recv, user);
                
                if (dir.isNil()) {
                    throw runtime.newArgumentError("user " + user + " does not exist");
                }
                
                relativePath = "" + dir +
                        (pathLength == userEnd ? "" : relativePath.substring(userEnd));
            }
        }
        
        String cwd = runtime.getCurrentDirectory();
        if (args.length == 2 && !args[1].isNil()) {
            cwd = RubyString.stringValue(args[1]).toString();
        }
        
        // Something wrong we don't know the cwd...
        if (cwd == null) return runtime.getNil();
        
        /* The counting of slashes that follows is simply a way to adhere to 
         * Ruby's UNC (or something) compatibility. When Ruby's expand_path is 
         * called with "//foo//bar" it will return "//foo/bar". JRuby uses 
         * java.io.File, and hence returns "/foo/bar". In order to retain 
         * java.io.File in the lower layers and provide full Ruby 
         * compatibility, the number of extra slashes must be counted and 
         * prepended to the result.
         */ 
        
        // Find out which string to check.
        String stringToCheck = null;
        if (relativePath.length() > 0 && relativePath.charAt(0) == '/') {
        	stringToCheck = relativePath;
        } else if (cwd.length() > 0 && cwd.charAt(0) == '/') {
        	stringToCheck = cwd;
        }
        
        String padSlashes = "";
        if (stringToCheck != null) {
            // Count number of extra slashes in the beginning of the string.
            int slashCount = 0;
            for (int i = 0; i < stringToCheck.length(); i++) {
            	if (stringToCheck.charAt(i) == '/') {
            		slashCount++;
            	} else {
            		break;
            	}
            }

            // If there are N slashes, then we want N-1.
            if (slashCount > 0) {
            	slashCount--;
            }
            
            // Prepare a string with the same number of redundant slashes so that 
            // we easily can prepend it to the result.
            byte[] slashes = new byte[slashCount];
            for (int i = 0; i < slashCount; i++) {
                slashes[i] = '/';
            }
            padSlashes = new String(slashes); 
        }
        
        JRubyFile path = JRubyFile.create(cwd, relativePath);
        
        return runtime.newString(canonicalize(padSlashes + path.getAbsolutePath()));
    }

    private static String canonicalize(String path) {
        return canonicalize(null, path);
    }

    private static String canonicalize(String canonicalPath, String remaining) {
        if (remaining == null) return canonicalPath;

        String child;
        int slash = remaining.indexOf('/');
        if (slash == -1) {
            child = remaining;
            remaining = null;
        } else {
            child = remaining.substring(0, slash);
            remaining = remaining.substring(slash + 1);
        }

        if (child.equals(".")) {
            // skip it
            if (canonicalPath != null && (canonicalPath.length() == 0 || canonicalPath.endsWith("/"))) canonicalPath += "/";
        } else if (child.equals("..")) {
            if (canonicalPath == null) throw new IllegalArgumentException("Cannot have .. at the start of an absolute path");
            int lastDir = canonicalPath.lastIndexOf('/');
            if (lastDir == -1) {
                canonicalPath = null;
            } else {
                canonicalPath = canonicalPath.substring(0, lastDir);
            }
        } else if (canonicalPath == null) {
            canonicalPath = child;
        } else {
            canonicalPath += "/" + child;
        }

        return canonicalize(canonicalPath, remaining);
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
    public static IRubyObject fnmatch(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        int flags;
        if (Arity.checkArgumentCount(runtime, args, 2, 3) == 3) {
            flags = RubyNumeric.num2int(args[2]);
        } else {
            flags = 0;
        }
        
        ByteList pattern = args[0].convertToString().getByteList();
        ByteList path = args[1].convertToString().getByteList();
        if (org.jruby.util.Dir.fnmatch(pattern.bytes, 0, pattern.realSize , path.bytes, 0, path.realSize, flags) == 0) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    /*
     * Fixme:  This does not have exact same semantics as RubyArray.join, but they
     * probably could be consolidated (perhaps as join(args[], sep, doChomp)).
     */
    public static RubyString join(IRubyObject recv, IRubyObject[] args) {
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
                element = join(recv, ((RubyArray) args[i]).toJavaArray()).toString();
            } else {
                element = args[i].convertToString().toString();
            }
            
            chomp(buffer);
            if (i > 0 && !element.startsWith("/") && !element.startsWith("\\")) {
                buffer.append("/");
            }
            buffer.append(element);
        }
        
        RubyString fixedStr = RubyString.newString(recv.getRuntime(), buffer.toString());
        fixedStr.setTaint(isTainted);
        return fixedStr;
    }
    
    private static void chomp(StringBuffer buffer) {
        int lastIndex = buffer.length() - 1;
        
        while (lastIndex >= 0 && (buffer.lastIndexOf("/") == lastIndex || buffer.lastIndexOf("\\") == lastIndex)) {
            buffer.setLength(lastIndex);
            lastIndex--;
        }
    }
    
    public static IRubyObject lstat(IRubyObject recv, IRubyObject filename) {
        RubyString name = RubyString.stringValue(filename);
        return recv.getRuntime().newRubyFileStat(name.toString());
    }
    
    public static IRubyObject ctime(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        RubyString name = RubyString.stringValue(filename);
        return runtime.newTime(JRubyFile.create(runtime.getCurrentDirectory(), name.toString()).getParentFile().lastModified());
    }
    
    public static IRubyObject mtime(IRubyObject recv, IRubyObject filename) {
        Ruby runtime = recv.getRuntime();
        RubyString name = RubyString.stringValue(filename);
        
        return runtime.newTime(JRubyFile.create(runtime.getCurrentDirectory(), name.toString()).lastModified());
    }
    
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        return open(recv, args, true, block);
    }
    
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, boolean tryToYield, Block block) {
        Ruby runtime = recv.getRuntime();
        Arity.checkArgumentCount(runtime, args, 1, -1);
        ThreadContext tc = runtime.getCurrentContext();
        
        RubyString pathString = RubyString.stringValue(args[0]);
        runtime.checkSafeString(pathString);
        String path = pathString.toString();
        
        IOModes modes =
                args.length >= 2 ? getModes(runtime, args[1]) : new IOModes(runtime, IOModes.RDONLY);
        RubyFile file = new RubyFile(runtime, (RubyClass) recv);
        
        RubyInteger fileMode =
                args.length >= 3 ? args[2].convertToInteger() : null;
        
        file.openInternal(path, modes);
        
        if (fileMode != null) {
            chmod(recv, new IRubyObject[] {fileMode, pathString});
        }
        
        if (tryToYield && block.isGiven()) {
            try {
                return block.yield(tc, file);
            } finally {
                file.close();
            }
        }
        
        return file;
    }
    
    public static IRubyObject rename(IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        Ruby runtime = recv.getRuntime();
        RubyString oldNameString = RubyString.stringValue(oldName);
        RubyString newNameString = RubyString.stringValue(newName);
        runtime.checkSafeString(oldNameString);
        runtime.checkSafeString(newNameString);
        JRubyFile oldFile = JRubyFile.create(runtime.getCurrentDirectory(), oldNameString.toString());
        JRubyFile newFile = JRubyFile.create(runtime.getCurrentDirectory(), newNameString.toString());
        
        if (!oldFile.exists() || !newFile.getParentFile().exists()) {
            throw runtime.newErrnoENOENTError("No such file or directory - " + oldNameString + " or " + newNameString);
        }
        oldFile.renameTo(JRubyFile.create(runtime.getCurrentDirectory(), newNameString.toString()));
        
        return RubyFixnum.zero(runtime);
    }
    
    public static IRubyObject size_p(IRubyObject recv, IRubyObject filename) {
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
        
        if (size == 0) return recv.getRuntime().getNil();
        
        return recv.getRuntime().newFixnum(size);
    }
    
    public static RubyArray split(IRubyObject recv, IRubyObject arg) {
        RubyString filename = RubyString.stringValue(arg);
        
        return filename.getRuntime().newArray(dirname(recv, filename),
                basename(recv, new IRubyObject[] { filename }));
    }
    
    public static IRubyObject symlink(IRubyObject recv, IRubyObject from, IRubyObject to) {
        Ruby runtime = recv.getRuntime();
        
        try {
            int result = new ShellLauncher(runtime).runAndWait(new IRubyObject[] {
                runtime.newString("ln"), runtime.newString("-s"), from, to
            });
            return runtime.newFixnum(result);
        } catch (Exception e) {
            throw runtime.newNotImplementedError("symlinks");
        }
    }

    public static IRubyObject symlink_p(IRubyObject recv, IRubyObject arg1) {
        Ruby runtime = recv.getRuntime();
        RubyString filename = RubyString.stringValue(arg1);
        
        JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(), filename.toString());
        
        try {
            // Only way to determine symlink is to compare canonical and absolute files
            // However symlinks in containing path must not produce false positives, so we check that first
            File absoluteParent = file.getAbsoluteFile().getParentFile();
            File canonicalParent = file.getAbsoluteFile().getParentFile().getCanonicalFile();
            
            if (canonicalParent.getAbsolutePath().equals(absoluteParent.getAbsolutePath())) {
                // parent doesn't change when canonicalized, compare absolute and canonical file directly
                return file.getAbsolutePath().equals(file.getCanonicalPath()) ? runtime.getFalse() : runtime.getTrue();
            }
            
            // directory itself has symlinks (canonical != absolute), so build new path with canonical parent and compare
            file = JRubyFile.create(runtime.getCurrentDirectory(), canonicalParent.getAbsolutePath() + "/" + file.getName());
            return file.getAbsolutePath().equals(file.getCanonicalPath()) ? runtime.getFalse() : runtime.getTrue();
        } catch (IOException ioe) {
            // problem canonicalizing the file; nothing we can do but return false
            return runtime.getFalse();
        }
    }
    
    // Can we produce IOError which bypasses a close?
    public static IRubyObject truncate(IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = recv.getRuntime();
        RubyString filename = RubyString.stringValue(arg1);
        RubyFixnum newLength = (RubyFixnum) arg2.convertToType(runtime.getFixnum(), MethodIndex.TO_INT, "to_int", true);
        IRubyObject[] args = new IRubyObject[] { filename, runtime.newString("w+") };
        RubyFile file = (RubyFile) open(recv, args, false, null);
        file.truncate(newLength);
        file.close();
        
        return RubyFixnum.zero(runtime);
    }
    
    /**
     * This method does NOT set atime, only mtime, since Java doesn't support anything else.
     */
    public static IRubyObject utime(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        Arity.checkArgumentCount(runtime, args, 2, -1);
        
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
            runtime.checkSafeString(filename);
            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(),filename.toString());
            
            if (!fileToTouch.exists()) {
                throw runtime.newErrnoENOENTError(" No such file or directory - \"" + filename + "\"");
            }
            
            fileToTouch.setLastModified(mtime);
        }
        
        return runtime.newFixnum(args.length - 2);
    }
    
    public static IRubyObject unlink(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        
        for (int i = 0; i < args.length; i++) {
            RubyString filename = RubyString.stringValue(args[i]);
            runtime.checkSafeString(filename);
            JRubyFile lToDelete = JRubyFile.create(runtime.getCurrentDirectory(),filename.toString());
            
            if (!lToDelete.exists()) {
                throw runtime.newErrnoENOENTError(" No such file or directory - \"" + filename + "\"");
            }
            
            if (!lToDelete.delete()) return runtime.getFalse();
        }
        
        return runtime.newFixnum(args.length);
    }
}
