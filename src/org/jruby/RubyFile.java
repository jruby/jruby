/*
 * RubyFile.java - No description
 * Created on 12.01.2002, 19:14:58
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandlerSeekable;
import org.jruby.util.IOHandlerUnseekable;
import org.jruby.util.IOModes;

/**
 * Ruby File class equivalent in java.
 *
 * @author jpetersen
 * @version $Revision$
 **/
public class RubyFile extends RubyIO {
    protected String path;
    
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
            throw new IOError(runtime, e.getMessage());
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
		super(runtime, runtime.getClasses().getFileClass());
		this.path = path;
		this.handler = new IOHandlerUnseekable(runtime, in, null);
		this.modes = handler.getModes();
		registerIOHandler(handler);
	}

    public static RubyClass createFileClass(Ruby runtime) {
        RubyClass fileClass = runtime.defineClass("File", 
                runtime.getClasses().getIoClass());

        RubyString separator = runtime.newString(File.separator);
        separator.freeze();
        fileClass.defineConstant("SEPARATOR", separator);
        fileClass.defineConstant("Separator", separator);
        RubyString altSeparator = runtime.newString(File.separatorChar == '/' ? "\\" : "/");
        altSeparator.freeze();
        fileClass.defineConstant("ALT_SEPARATOR", altSeparator);
        RubyString pathSeparator = runtime.newString(File.pathSeparator);
        pathSeparator.freeze();
        fileClass.defineConstant("PATH_SEPARATOR", pathSeparator);
        
        // Create constants for open flags
        fileClass.setConstant("RDONLY", 
        		runtime.newFixnum(IOModes.RDONLY));
        fileClass.setConstant("WRONLY", 
        		runtime.newFixnum(IOModes.WRONLY));
        fileClass.setConstant("RDWR", 
        		runtime.newFixnum(IOModes.RDWR));
        fileClass.setConstant("CREAT", 
        		runtime.newFixnum(IOModes.CREAT));
        fileClass.setConstant("EXCL", 
        		runtime.newFixnum(IOModes.EXCL));
        fileClass.setConstant("NOCTTY", 
        		runtime.newFixnum(IOModes.NOCTTY));
        fileClass.setConstant("TRUNC", 
        		runtime.newFixnum(IOModes.TRUNC));
        fileClass.setConstant("APPEND", 
        		runtime.newFixnum(IOModes.APPEND));
        fileClass.setConstant("NONBLOCK", 
        		runtime.newFixnum(IOModes.NONBLOCK));

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFile.class);

        fileClass.extendObject(runtime.getClasses().getFileTestModule());
        
        fileClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        fileClass.defineSingletonMethod("open", callbackFactory.getOptSingletonMethod("open"));
        fileClass.defineSingletonMethod("chmod", callbackFactory.getOptSingletonMethod("chmod", RubyInteger.class));
        fileClass.defineSingletonMethod("lstat", callbackFactory.getSingletonMethod("lstat", RubyString.class));
        fileClass.defineSingletonMethod("expand_path", callbackFactory.getOptSingletonMethod("expand_path"));
        fileClass.defineSingletonMethod("unlink", callbackFactory.getOptSingletonMethod("unlink"));
        fileClass.defineSingletonMethod("rename", callbackFactory.getSingletonMethod("rename", IRubyObject.class, IRubyObject.class));
        fileClass.defineSingletonMethod("delete", callbackFactory.getOptSingletonMethod("unlink"));
		fileClass.defineSingletonMethod("dirname", callbackFactory.getSingletonMethod("dirname", RubyString.class));
		fileClass.defineSingletonMethod("join", callbackFactory.getOptSingletonMethod("join"));
		fileClass.defineSingletonMethod("basename", callbackFactory.getOptSingletonMethod("basename"));
		fileClass.defineSingletonMethod("truncate", callbackFactory.getSingletonMethod("truncate", RubyString.class, RubyFixnum.class));
		fileClass.defineSingletonMethod("split", callbackFactory.getSingletonMethod("split", RubyString.class));
		fileClass.defineSingletonMethod("stat", callbackFactory.getSingletonMethod("lstat", RubyString.class));
		
		fileClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
		fileClass.defineMethod("truncate", callbackFactory.getMethod("truncate", RubyFixnum.class));
		
        // Works around a strange-ish implementation that uses a static method on the superclass.
        // It broke when moved to indexed callbacks, so added this line:
        fileClass.defineMethod("print", callbackFactory.getOptSingletonMethod("print"));

        return fileClass;
    }
    
    protected void openInternal(String path, IOModes modes) {
        this.path = path;
        this.modes = modes;

        try {
            handler = new IOHandlerSeekable(getRuntime(), path, modes);
            
            registerIOHandler(handler);
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }
    }

	/*
	 *  File class methods
	 */
	
	public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
	    RubyFile file = new RubyFile(recv.getRuntime(), (RubyClass)recv);
	    file.callInit(args);
	    return file;
	}
	
	private static IOModes getModes(IRubyObject object) {
		if (object instanceof RubyString) {
			return new IOModes(object.getRuntime(), ((RubyString)object).getValue());
		} else if (object instanceof RubyFixnum) {
			return new IOModes(object.getRuntime(), ((RubyFixnum)object).getLongValue());
		}

		throw object.getRuntime().newTypeError("Invalid type for modes");
	}
	
	public IRubyObject initialize(IRubyObject[] args) {
	    if (args.length == 0) {
	        throw getRuntime().newArgumentError(0, 1);
	    }

	    args[0].checkSafeString();
	    path = args[0].toString();
	    modes = args.length > 1 ? getModes(args[1]) :
	    	new IOModes(getRuntime(), IOModes.RDONLY);
	    
	    // One of the few places where handler may be null.
	    // If handler is not null, it indicates that this object
	    // is being reused.
	    if (handler != null) {
	        close();
	    }
	    openInternal(path, modes);
	    
	    if (getRuntime().isBlockGiven()) {
	        // getRuby().getRuntime().warn("File::new does not take block; use File::open instead");
	    }
	    return this;
	}
	
	public static IRubyObject open(IRubyObject recv, IRubyObject[] args) {
	    return open(recv, args, true);
	}
	
	public static IRubyObject open(IRubyObject recv, IRubyObject[] args,
	        boolean tryToYield) {
	    if (args.length == 0) {
	        throw recv.getRuntime().newArgumentError(0, 1);
	    }
	    args[0].checkSafeString();
	    String path = args[0].toString();
	    IOModes modes = args.length > 1 ? getModes(args[1]) :
	    	new IOModes(recv.getRuntime(), IOModes.RDONLY);
	    RubyFile file = new RubyFile(recv.getRuntime(), (RubyClass)recv);
	    
	    file.openInternal(path, modes);

	    if (tryToYield && recv.getRuntime().isBlockGiven()) {
	        try {
	            recv.getRuntime().yield(file);
	        } finally {
	            file.close();
	        }
	        
	        return recv.getRuntime().getNil();
	    }
	    
	    return file;
	}

    public static IRubyObject chmod(IRubyObject recv, RubyInteger mode, IRubyObject[] names) {
        // Java has no ability to chmod directly.  Once popen support
        // is added, we can create a ruby script to perform chmod and then
        // put it in our base distribution.  It could also be some embedded
        // eval? 
        return recv.getRuntime().newFixnum(0);
    }

    public static IRubyObject lstat(IRubyObject recv, RubyString name) {
        return new RubyFileStat(recv.getRuntime(), new File(name.getValue()));
    }

    public static IRubyObject unlink(IRubyObject recv, IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            args[i].checkSafeString();
            File lToDelete = new File(args[i].toString());
            if (!lToDelete.exists()) {
				throw ErrnoError.getErrnoError(recv.getRuntime(), "ENOENT",
                        " No such file or directory - \"" + args[i].toString() + "\"");
			}
            if (!lToDelete.delete()) {
                return recv.getRuntime().getFalse();
            }
        }
        return recv.getRuntime().newFixnum(args.length);
    }

    public static IRubyObject rename(IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        oldName.checkSafeString();
        newName.checkSafeString();
        File oldFile = new File(oldName.asSymbol());
        
        if (!oldFile.exists()) {
            throw ErrnoError.getErrnoError(recv.getRuntime(), "ENOENT",
                    "No such file: " + oldName.asSymbol());
        }
        oldFile.renameTo(new File(newName.asSymbol()));
        return RubyFixnum.zero(recv.getRuntime());
    }

    public static IRubyObject expand_path(IRubyObject recv, IRubyObject[] args) {
        int length = recv.checkArgumentCount(args, 1, 2);
        String relativePath = RubyString.stringValue(args[0]).getValue();

        if (new File(relativePath).isAbsolute()) {
            return recv.getRuntime().newString(relativePath);
        }

        String cwd = System.getProperty("user.dir");
        if (length == 2 && !args[1].isNil()) {
            cwd = RubyString.stringValue(args[1]).getValue();
        }

        // Something wrong we don't know the cwd...
        if (cwd == null) {
            return recv.getRuntime().getNil();
        }

        File path = new File(cwd, relativePath);

        String extractedPath;
        try {
            extractedPath = path.getCanonicalPath();
        } catch (IOException e) {
            extractedPath = path.getAbsolutePath();
        }
        return recv.getRuntime().newString(extractedPath);
    }
    
	public static IRubyObject dirname(IRubyObject recv, RubyString filename) {
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
			return recv.getRuntime().newString("."); 
		}
		if (index == 0) {
			return recv.getRuntime().newString("/");
		}
		return recv.getRuntime().newString(name.substring(0, index)).infectBy(filename);
	}
	
    public static IRubyObject basename(IRubyObject recv, IRubyObject[] args) {
        if (args.length < 1 || args.length > 2) {
            throw recv.getRuntime().newArgumentError("This method expected 1 or 2 arguments."); // XXX
        }
		String name = args[0].toString();
		if (name.length() > 1 && name.charAt(name.length() - 1) == '/') {
			name = name.substring(0, name.length() - 1);
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
			String ext = args[1].toString();
			if (name.endsWith(ext)) {
				name = name.substring(0, name.length() - ext.length());
			}
		}
		return recv.getRuntime().newString(name).infectBy(args[0]);
	}
    
    public IRubyObject truncate(RubyFixnum newLength) {
        try {
            handler.truncate(newLength.getLongValue());
        } catch (IOException e) {
            // Should we do anything?
        }
        
        return RubyFixnum.zero(getRuntime());
    }
    
    // Can we produce IOError which bypasses a close?
    public static IRubyObject truncate(IRubyObject recv, RubyString filename, RubyFixnum newLength) {
        IRubyObject[] args = new IRubyObject[] {filename, 
                recv.getRuntime().newString("w+")};
        RubyFile file = (RubyFile) RubyFile.open(recv, args, false);
        file.truncate(newLength);
        file.close();
        
        return RubyFixnum.zero(recv.getRuntime());
    }

    public static RubyString join(IRubyObject recv, IRubyObject[] args) {
		RubyArray argArray = recv.getRuntime().newArray(args);
		return argArray.join(recv.getRuntime().newString(File.separator));
    }
    
    public static RubyArray split(IRubyObject recv, RubyString filename) {
    	return filename.getRuntime().newArray(
    			dirname(recv, filename),
    			basename(recv, new IRubyObject[]{ filename }));
    }
    
    public String toString() {
        return "RubyFile(" + path + ", " + modes + ", " + fileno + ")";
    }
}
