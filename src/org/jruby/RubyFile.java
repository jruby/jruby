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

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandlerSeekable;
import org.jruby.util.IOHandlerUnseekable;
import org.jruby.util.IOModes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Ruby File class equivalent in java.
 *
 * @author jpetersen
 * @version $Revision$
 **/
public class RubyFile extends RubyIO {
    protected String path;
    
    public RubyFile(Ruby ruby, String path) {
        super(ruby, ruby.getClasses().getFileClass());

        this.path = path;
        
        try {
            FileInputStream inputStream = new FileInputStream(new File(path));
            handler = new IOHandlerUnseekable(getRuntime(), inputStream, null);
            modes = new IOModes(ruby, "r");
        } catch (FileNotFoundException e) {
            throw new IOError(runtime, e.getMessage());
        }
        
        registerIOHandler(handler);
    }
    
	public RubyFile(Ruby ruby, RubyClass type) {
	    super(ruby, type);
	}

    public static RubyClass createFileClass(Ruby ruby) {
        RubyClass fileClass = ruby.defineClass("File", 
                ruby.getClasses().getIoClass());

        RubyString separator = RubyString.newString(ruby, separator());
        separator.freeze();
        fileClass.defineConstant("SEPARATOR", separator);
        fileClass.defineConstant("Separator", separator);
        RubyString altSeparator = RubyString.newString(ruby, (File.separatorChar == '/'? "\\" : "/"));
        altSeparator.freeze();
        fileClass.defineConstant("ALT_SEPARATOR", altSeparator);
        RubyString pathSeparator = RubyString.newString(ruby, File.pathSeparator);
        pathSeparator.freeze();
        fileClass.defineConstant("PATH_SEPARATOR", pathSeparator);

        CallbackFactory callbackFactory = ruby.callbackFactory();

        fileClass.extendObject(ruby.getClasses().getFileTestModule());
        
        fileClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod(RubyFile.class, "newInstance"));
        fileClass.defineSingletonMethod("open", callbackFactory.getOptSingletonMethod(RubyFile.class, "open"));
        fileClass.defineSingletonMethod("chmod", callbackFactory.getOptSingletonMethod(RubyFile.class, "chmod", RubyInteger.class));
        fileClass.defineSingletonMethod("lstat", callbackFactory.getSingletonMethod(RubyFile.class, "lstat", RubyString.class));
        fileClass.defineSingletonMethod("expand_path", callbackFactory.getOptSingletonMethod(RubyFile.class, "expand_path"));
        fileClass.defineSingletonMethod("unlink", callbackFactory.getOptSingletonMethod(RubyFile.class, "unlink"));
        fileClass.defineSingletonMethod("rename", callbackFactory.getSingletonMethod(RubyFile.class, "rename", IRubyObject.class, IRubyObject.class));
        fileClass.defineSingletonMethod("delete", callbackFactory.getOptSingletonMethod(RubyFile.class, "unlink"));
		fileClass.defineSingletonMethod("dirname", callbackFactory.getSingletonMethod(RubyFile.class, "dirname", RubyString.class));
		fileClass.defineSingletonMethod("join", callbackFactory.getOptSingletonMethod(RubyFile.class, "join"));
		fileClass.defineSingletonMethod("basename", callbackFactory.getOptSingletonMethod(RubyFile.class, "basename"));
		fileClass.defineSingletonMethod("truncate", callbackFactory.getSingletonMethod(RubyFile.class, "truncate", RubyString.class, RubyFixnum.class));
		
		fileClass.defineMethod("initialize", callbackFactory.getOptMethod(RubyFile.class, "initialize"));
		fileClass.defineMethod("truncate", callbackFactory.getMethod(RubyFile.class, "truncate", RubyFixnum.class));
		
        // Works around a strange-ish implementation that uses a static method on the superclass.
        // It broke when moved to indexed callbacks, so added this line:
        fileClass.defineMethod("print", callbackFactory.getOptSingletonMethod(RubyIO.class, "print"));

        return fileClass;
    }
    
    protected void openInternal(String path, String mode) {
        this.path = path;

        try {
            handler = new IOHandlerSeekable(getRuntime(), path, mode);
            modes = new IOModes(getRuntime(), mode);
            
            registerIOHandler(handler);
        } catch (IOException e) {
            throw IOError.fromException(runtime, e);
        }
    }

	private static String separator() {
		return "/";
	}

	/*
	 *  File class methods
	 */
	
	public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
	    RubyFile file = new RubyFile(recv.getRuntime(), (RubyClass)recv);
	    file.callInit(args);
	    return file;
	}
	
	public IRubyObject initialize(IRubyObject[] args) {
	    if (args.length == 0) {
	        throw new ArgumentError(getRuntime(), 0, 1);
	    }
	    
	    args[0].checkSafeString();
	    path = args[0].toString();
	    
	    String mode = "r";
	    if (args.length > 1 && args[1] instanceof RubyString) {
	        mode = ((RubyString)args[1]).getValue();
	    }
	    
	    // One of the few places where handler may be null.
	    // If handler is not null, it indicates that this object
	    // is being reused.
	    if (handler != null) {
	        close();
	    }
	    openInternal(path, mode);
	    
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
	        throw new ArgumentError(recv.getRuntime(), 0, 1);
	    }
	    args[0].checkSafeString();
	    String path = args[0].toString();
	    String mode = "r";
	    if (args.length > 1 && args[1] instanceof RubyString) {
	        mode = ((RubyString)args[1]).getValue();
	    }
	    
	    RubyFile file = new RubyFile(recv.getRuntime(), (RubyClass)recv);
	    file.openInternal(path, mode);

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
        return RubyFixnum.newFixnum(recv.getRuntime(), 0);
    }

    public static IRubyObject lstat(IRubyObject recv, RubyString name) {
        return new FileStatClass(recv.getRuntime(), new File(name.getValue()));
    }

    public static IRubyObject unlink(IRubyObject recv, IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            args[i].checkSafeString();
            File lToDelete = new File(args[i].toString());
            if (!lToDelete.exists())
                throw ErrnoError.getErrnoError(recv.getRuntime(), "ENOENT",
                        " No such file or directory - \"" + args[i].toString() + "\"");
            if (!lToDelete.delete()) {
                return recv.getRuntime().getFalse();
            }
        }
        return RubyFixnum.newFixnum(recv.getRuntime(), args.length);
    }

    public static IRubyObject rename(IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        oldName.checkSafeString();
        newName.checkSafeString();
        File oldFile = new File(oldName.asSymbol());
        
        if (oldFile.exists() == false) {
            throw ErrnoError.getErrnoError(recv.getRuntime(), "ENOENT",
                    "No such file: " + oldName.asSymbol());
        }
        oldFile.renameTo(new File(newName.asSymbol()));
        return RubyFixnum.zero(recv.getRuntime());
    }

    public static IRubyObject expand_path(IRubyObject recv, IRubyObject[] args) {
        if (args.length <= 0) {
        }
        
        String relativePath = RubyString.stringValue(args[0]).getValue();
        String cwd = args.length == 2 ?
           args[1].isNil() ? System.getProperty("user.dir") :
           RubyString.stringValue(args[1]).getValue() :
           System.getProperty("user.dir");
                                      
           // Something wrong we don't know the cwd...
           if (cwd == null) {
               return recv.getRuntime().getNil();
           }
                                      
           File expandedPath = new File(cwd, relativePath);
                                      
           try {
               return RubyString.newString(recv.getRuntime(), expandedPath.getCanonicalPath());
           } catch (IOException e) {}
           
           return RubyString.newString(recv.getRuntime(), expandedPath.getAbsolutePath());
    }
    
	public static RubyString dirname(IRubyObject recv, RubyString filename) {
		String name = filename.toString();
		
		int index = name.lastIndexOf(separator());
		boolean alt = false;
				
		if (index == -1) {
			index = name.lastIndexOf(altSeparator());
			
			if (index == -1) {
				return RubyString.newString(recv.getRuntime(), "."); 
			} else
			{
				alt = true;
			}
		}
		
		
		if (index == 0) {
			return RubyString.newString(recv.getRuntime(), alt ? altSeparator() : separator());
		} else {		
			return RubyString.newString(recv.getRuntime(), name.substring(0, index));
		}
	}
	
	/**
	 * @return alternate DOS separator
	 */
	private static String altSeparator()
	{
		return "\\";
	}

    public static RubyString basename(IRubyObject recv, IRubyObject[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new ArgumentError(recv.getRuntime(), "This method expected 1 or 2 arguments."); // XXX
        }

		String name = args[0].toString();
		
		name = new File(name).getName();
		
		if (args.length == 2 && name.endsWith(args[1].toString())) {
		    name = name.substring(0, name.length() - args[1].toString().length());
		}

		return RubyString.newString(recv.getRuntime(), name);
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
                RubyString.newString(recv.getRuntime(), "w+")};
        RubyFile file = (RubyFile) RubyFile.open(recv, args, false);
        file.truncate(newLength);
        file.close();
        
        return RubyFixnum.zero(recv.getRuntime());
    }

    public static RubyString join(IRubyObject recv, IRubyObject[] args) {
		RubyArray argArray = RubyArray.newArray(recv.getRuntime(), args);
		return argArray.join(RubyString.newString(recv.getRuntime(), separator()));
    }
    
    public String toString() {
        return "RubyFile(" + path + ", " + modes.getModeString() + ", " +
           fileno + ")";
    }
}
