package org.jruby;

import java.io.*;

import org.jruby.exceptions.*;

public class RubyIO extends RubyObject {
    private InputStream inStream = null;
    private OutputStream outStream = null;
    
    private boolean sync = false;
    
    private boolean readable = false;
    private boolean writeable = false;
        
	public RubyIO(Ruby ruby) {
	    super(ruby, ruby.getClasses().getIoClass());
	}

	public RubyIO(Ruby ruby, RubyClass type) {
	    super(ruby, type);
	}

	protected void checkWriteable() {
	    if (!writeable || outStream == null) {
	        throw new IOError(getRuby(), "not opened for writing");
	    }
	}

	protected void checkReadable() {
	    if (!readable || inStream == null) {
	        throw new IOError(getRuby(), "not opened for reading");
	    }
	}
	
	protected boolean isReadable() {
	    return readable;
	}
	
	protected boolean isWriteable() {
	    return writeable;
	}
	
	protected void closeStreams() {
	    if (inStream != null) {
	        try {
	        	inStream.close();
	        } catch (IOException ioExcptn) {
	        }
	    }

	    if (outStream != null) {
	        try {
	        	outStream.close();
	        } catch (IOException ioExcptn) {
	        }
	    }
	    
	    inStream = null;
	    outStream = null;
	}
	
	/** rb_io_write
	 * 
	 */
	protected RubyObject callWrite(RubyObject anObject) {
	    return funcall("write", anObject);
	}

	/** rb_io_mode_flags
	 * 
	 */	
	protected void setMode(String mode) {
	    if (mode.length() == 0) {
	        throw new RubyArgumentException(getRuby(), "illegal access mode");
	    }
	    
	    switch (mode.charAt(0)) {
	        case 'r':
	        	readable = true;
	        	break;
        	case 'w':
			case 'a':
        		writeable = true;
        		break;
       		default:
	        	throw new RubyArgumentException(getRuby(), "illegal access mode " + mode);
	    }
	    
	    if (mode.length() > 1) {
	        int i = mode.charAt(1) == 'b' ? 2 : 1;
	        
	        if (mode.length() > i) {
	            if (mode.charAt(i) == '+') {
	                readable = true;
	                writeable = true;
	            } else {
	                throw new RubyArgumentException(getRuby(), "illegal access mode " + mode);
	            }
	        }
	    }
	}
	
	/** rb_fdopen
	 * 
	 */
	protected void fdOpen(int fd) {
		switch (fd) {
		    case 0:
		    	inStream = getRuby().getRuntime().getInputStream();
		    	break;
			case 1:
		    	outStream = getRuby().getRuntime().getOutputStream();
		    	break;
			case 2:
		    	outStream = getRuby().getRuntime().getErrorStream();
		    	break;
			default:
				throw new IOError(getRuby(), "file descriptor " + fd + " is not supported by JRuby.");
		}
	}


	// IO class methods.
	
	/** rb_io_s_new
	 * 
	 */
	public static RubyObject newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
	    RubyIO newObject = new RubyIO(ruby, (RubyClass)recv);
	    
	    newObject.callInit(args);
	    
	    return newObject;
	}
	
	/** rb_io_initialize
	 * 
	 */
	public RubyObject initialize(RubyObject[] args) {
	    closeStreams();

		String mode = "r";
		
		if (args.length > 1) {
		    mode = ((RubyString)args[1]).getValue();
		}
		
		setMode(mode);
		
		fdOpen(RubyFixnum.fix2int(args[0]));
		
		return this;
	}
	
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
	
	/** rb_io_addstr
	 * 
	 */
	public RubyObject addString(RubyObject anObject) {
	    callWrite(anObject);
	    
	    return this;
	}
}