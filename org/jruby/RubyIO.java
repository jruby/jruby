package org.jruby;

import java.io.*;

import org.jruby.exceptions.*;

public class RubyIO extends RubyObject {
    private InputStream inStream = null;
    private OutputStream outStream = null;
    
    private boolean sync = false;
        
	public RubyIO(Ruby ruby) {
	    super(ruby);
	}

	protected void checkWriteable() {
	    if (outStream == null) {
	        throw new IOError(getRuby(), "not opened for writing");
	    }
	}

	protected void checkReadable() {
	    if (inStream == null) {
	        throw new IOError(getRuby(), "not opened for reading");
	    }
	}


	//
	
	/** io_write
	 * 
	 */
	public RubyObject write(RubyObject obj) {
	    getRuby().secure(4);
	    
	    RubyString str = obj.to_s();
	    
	    if (str.getValue().length() == 0) {
	        return RubyFixnum.zero(getRuby());
	    }
	    
	    checkWriteable();
	    
	    try {
	    	outStream.write(str.getValue().getBytes());
	    
	    	if (sync) {
	        	outStream.flush();
	    	}
	    } catch (IOException ioExcptn) {
	        throw new IOError(getRuby(), ioExcptn.getMessage());
	    }
	    
	    return str.length();
	}
	
	/**
	 * 
	 */
	public RubyObject opShiftLeft(RubyObject obj) {
	    
	    return this;
	}
}