package org.jruby;

import java.io.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

public class RubyFile extends RubyIO {

	public RubyFile(Ruby ruby, RubyClass type) {
	    super(ruby, type);
	}

    public static RubyClass createFileClass(Ruby ruby) {
        RubyClass fileClass = ruby.defineClass("File", ruby.getClasses().getIoClass());
        
        fileClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyFile.class, "newInstance"));
        fileClass.defineSingletonMethod("open", CallbackFactory.getOptSingletonMethod(RubyFile.class, "open"));

        fileClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyFile.class, "initialize"));
        
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
        		this.outStream = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath(), append));
            }
        } catch (IOException ioExcptn) {
            throw new IOError(getRuby(), ioExcptn.getMessage());
        }
    }

	/*
	 *  File class methods
	 */
	
	public static RubyObject newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
	    RubyFile file = new RubyFile(ruby, (RubyClass)recv);
	    
	    file.callInit(args);
	    
	    return file;
	}
	
	public RubyObject initialize(RubyObject[] args) {
	    if (args.length == 0) {
	        throw new RubyArgumentException(getRuby(), "");
	    }
	    
	    args[0].checkSafeString();
	    path = args[0].toString();
	    
	    String mode = "r";
	    
	    if (args.length > 1 && args[1] instanceof RubyString) {
	        mode = ((RubyString)args[1]).getValue();
	    }
	    
	    closeStreams();
	    
	    openInternal(path, mode);
	    
	    if (getRuby().isBlockGiven()) {
	        // getRuby().getRuntime().warn("File::new does not take block; use File::open instead");
	    }
	    
	    return this;
	}
	
	public static RubyObject open(Ruby ruby, RubyObject recv, RubyObject[] args) {
	    RubyFile file = new RubyFile(ruby, (RubyClass)recv);
	    
	    if (args.length == 0) {
	        throw new RubyArgumentException(ruby, "");
	    }
	    
	    args[0].checkSafeString();
	    file.path = args[0].toString();
	    
	    String mode = "r";
	    
	    if (args.length > 1 && args[1] instanceof RubyString) {
	        mode = ((RubyString)args[1]).getValue();
	    }
	    
	    file.closeStreams();
	    
	    file.openInternal(file.path, mode);
	    
	    if (ruby.isBlockGiven()) {
	        try {
	            ruby.yield(file);
	        } finally {
	            file.closeStreams();
	        }
	    }
	    
	    return file;
	}
}