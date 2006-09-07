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
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.FileMetaClass;
import org.jruby.util.IOHandler;
import org.jruby.util.IOHandlerNull;
import org.jruby.util.IOHandlerSeekable;
import org.jruby.util.IOHandlerUnseekable;
import org.jruby.util.IOModes;
import org.jruby.util.JRubyFile;
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
	
    protected String path;
    
	public RubyFile(IRuby runtime, RubyClass type) {
	    super(runtime, type);
	}

	public RubyFile(IRuby runtime, String path) {
		this(runtime, path, open(runtime, path));
    }

	// use static function because constructor call must be first statement in above constructor 
	private static InputStream open(IRuby runtime, String path) {
		try {
			return new FileInputStream(path);
		} catch (FileNotFoundException e) {
            throw runtime.newIOError(e.getMessage());
        }
	}
    
	// XXX This constructor is a hack to implement the __END__ syntax.
	//     Converting a reader back into an InputStream doesn't generally work.
	public RubyFile(IRuby runtime, String path, final Reader reader) {
		this(runtime, path, new InputStream() {
			public int read() throws IOException {
				return reader.read();
			}
		});
	}
	
	private RubyFile(IRuby runtime, String path, InputStream in) {
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
    
    private FileLock currentLock;
	
	public IRubyObject flock(IRubyObject lockingConstant) {
        FileChannel fileChannel = handler.getFileChannel();
        int lockMode = (int) ((RubyFixnum) lockingConstant.convertToType("Fixnum", "to_int", 
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
	    
	    if (getRuntime().getCurrentContext().isBlockGiven()) {
	        // getRuby().getRuntime().warn("File::new does not take block; use File::open instead");
	    }
	    return this;
	}

    public IRubyObject chmod(IRubyObject[] args) {
        checkArgumentCount(args, 1, 1);
        
        RubyInteger mode = args[0].convertToInteger();
        System.out.println(mode);
        if (!new File(path).exists()) {
            throw getRuntime().newErrnoENOENTError("No such file or directory - " + path);
        }
            
        try {
            Process chown = Runtime.getRuntime().exec("chmod " + FileMetaClass.OCTAL_FORMATTER.sprintf(mode.getLongValue()) + " " + path);
            chown.waitFor();
        } catch (IOException ioe) {
            // FIXME: ignore?
        } catch (InterruptedException ie) {
            // FIXME: ignore?
        }
        
        return getRuntime().newFixnum(0);
    }

    public IRubyObject chown(IRubyObject[] args) {
        checkArgumentCount(args, 1, 1);
        
        RubyInteger owner = args[0].convertToInteger();
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
	
	public RubyString path() {
		return getRuntime().newString(path);
	}

	public IRubyObject stat() {
        return getRuntime().newRubyFileStat(JRubyFile.create(getRuntime().getCurrentDirectory(),path));
	}
	
    public IRubyObject truncate(IRubyObject arg) {
    	RubyFixnum newLength = (RubyFixnum) arg.convertToType("Fixnum", "to_int", true);
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
	private IOModes getModes(IRubyObject object) {
		if (object instanceof RubyString) {
			return new IOModes(getRuntime(), ((RubyString)object).toString());
		} else if (object instanceof RubyFixnum) {
			return new IOModes(getRuntime(), ((RubyFixnum)object).getLongValue());
		}

		throw getRuntime().newTypeError("Invalid type for modes");
	}
}
