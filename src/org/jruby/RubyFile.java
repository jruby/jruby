package org.jruby;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.IOError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyInputStream;

/**
 * Ruby File class equivalent in java.
 *
 * @author jpetersen
 * @version $Revision$
 **/
public class RubyFile extends RubyIO {

	public RubyFile(Ruby ruby, RubyClass type) {
	    super(ruby, type);
	}

    public static RubyClass createFileClass(Ruby ruby) {
        RubyClass fileClass = ruby.defineClass("File", ruby.getClass("IO"));

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

        fileClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyFile.class, "newInstance"));
        fileClass.defineSingletonMethod("open", CallbackFactory.getOptSingletonMethod(RubyFile.class, "open"));
        fileClass.defineSingletonMethod("chmod", CallbackFactory.getOptSingletonMethod(RubyFile.class, "chmod", RubyInteger.class));
        fileClass.defineSingletonMethod("lstat", CallbackFactory.getSingletonMethod(RubyFile.class, "lstat", RubyString.class));

        fileClass.defineSingletonMethod("exist?", CallbackFactory.getSingletonMethod(RubyFile.class, "exist", RubyString.class));
        fileClass.defineSingletonMethod("exists?", CallbackFactory.getSingletonMethod(RubyFile.class, "exist", RubyString.class));
        fileClass.defineSingletonMethod("unlink", CallbackFactory.getOptSingletonMethod(RubyFile.class, "unlink"));
        fileClass.defineSingletonMethod("rename", CallbackFactory.getSingletonMethod(RubyFile.class, "rename", IRubyObject.class, IRubyObject.class));
        fileClass.defineSingletonMethod("delete", CallbackFactory.getOptSingletonMethod(RubyFile.class, "unlink"));
		fileClass.defineSingletonMethod("dirname", CallbackFactory.getSingletonMethod(RubyFile.class, "dirname", RubyString.class));
		fileClass.defineSingletonMethod("join", CallbackFactory.getOptSingletonMethod(RubyFile.class, "join"));
		fileClass.defineSingletonMethod("directory?", CallbackFactory.getSingletonMethod(RubyFile.class, "directory", RubyString.class));
		fileClass.defineSingletonMethod("basename", CallbackFactory.getOptSingletonMethod(RubyFile.class, "basename"));

		fileClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyFile.class, "initialize"));

        // Works around a strange-ish implementation that uses a static method on the superclass.
        // It broke when moved to indexed callbacks, so added this line:
        fileClass.defineMethod("print", CallbackFactory.getOptSingletonMethod(RubyIO.class, "print"));

        return fileClass;
    }
    
    protected void openInternal(String path, String mode) {
        this.path = path;
        setMode(mode);
        File file = new File(path);
        try {
            if (isReadable()) {
                this.inStream = new RubyInputStream(new BufferedInputStream(new FileInputStream(file)));
            }
            if (isWriteable()) {
                FileOutputStream fileOutput = new FileOutputStream(file.getAbsolutePath(), append);
                this.outStream = new BufferedOutputStream(fileOutput);
                this.outFileDescriptor = fileOutput.getFD();
            }
        } catch (IOException e) {
            throw IOError.fromException(runtime, e);
        }
    }

	private static String separator() {
		return java.io.File.separator;
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
	    closeStreams();
	    openInternal(path, mode);
	    
	    if (getRuntime().isBlockGiven()) {
	        // getRuby().getRuntime().warn("File::new does not take block; use File::open instead");
	    }
	    return this;
	}
	
	public static IRubyObject open(IRubyObject recv, IRubyObject[] args) {
	    RubyFile file = new RubyFile(recv.getRuntime(), (RubyClass)recv);
	    if (args.length == 0) {
	        throw new ArgumentError(recv.getRuntime(), 0, 1);
	    }
	    args[0].checkSafeString();
	    file.path = args[0].toString();
	    String mode = "r";
	    if (args.length > 1 && args[1] instanceof RubyString) {
	        mode = ((RubyString)args[1]).getValue();
	    }
	    file.closeStreams();
	    file.openInternal(file.path, mode);
	    if (recv.getRuntime().isBlockGiven()) {
	        try {
	            recv.getRuntime().yield(file);
	        } finally {
	            file.closeStreams();
	        }
	    }
	    return file;
	}

    public static IRubyObject chmod(IRubyObject recv, RubyInteger mode, IRubyObject[] names) {
        // no-op for now
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
                throw new IOError(recv.getRuntime(), " No such file or directory - \"" + args[i].toString() + "\"");
            if (!lToDelete.delete()) {
                return recv.getRuntime().getFalse();
            }
        }
        return RubyFixnum.newFixnum(recv.getRuntime(), args.length);
    }

    public static IRubyObject rename(IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        oldName.checkSafeString();
        newName.checkSafeString();
        new File(oldName.asSymbol()).renameTo(new File(newName.asSymbol()));
        return RubyFixnum.zero(recv.getRuntime());
    }

    public static IRubyObject exist(IRubyObject recv, RubyString filename) {
        return RubyBoolean.newBoolean(recv.getRuntime(), new File(filename.toString()).exists());
    }

    public static RubyString dirname(IRubyObject recv, RubyString filename) {
		String name = filename.toString();
		int index = name.lastIndexOf(separator());
		if (index == -1) {
			return RubyString.newString(recv.getRuntime(), ".");
		} else if (index == 0) {
			return RubyString.newString(recv.getRuntime(), separator());
		}
		return RubyString.newString(recv.getRuntime(), name.substring(0, index));
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

    public static RubyString join(IRubyObject recv, IRubyObject[] args) {
		RubyArray argArray = RubyArray.newArray(recv.getRuntime(), args);
		return argArray.join(RubyString.newString(recv.getRuntime(), separator()));
    }

    public static RubyBoolean directory(IRubyObject recv, RubyString filename) {
		return RubyBoolean.newBoolean(recv.getRuntime(), new File(filename.toString()).isDirectory());
	}
}
