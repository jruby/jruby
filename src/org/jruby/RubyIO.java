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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.IOHandler;
import org.jruby.util.IOHandlerJavaIO;
import org.jruby.util.IOHandlerNio;
import org.jruby.util.IOHandlerNull;
import org.jruby.util.IOHandlerProcess;
import org.jruby.util.IOHandlerSeekable;
import org.jruby.util.IOHandlerUnseekable;
import org.jruby.util.IOModes;
import org.jruby.util.ShellLauncher;

/**
 * 
 * @author jpetersen
 */
public class RubyIO extends RubyObject {
    public static final int STDIN = 0;
    public static final int STDOUT = 1;
    public static final int STDERR = 2;
    
    protected IOHandler handler;
    protected IOModes modes = null;
    protected int lineNumber = 0;
    
    // Does THIS IO object think it is still open
    // as opposed to the IO Handler which knows the
    // actual truth.  If two IO objects share the
    // same IO Handler, then it is possible for
    // one object to think that the handler is open
    // when it really isn't.  Keeping track of this yields
    // the right errors.
    protected boolean isOpen = true;
    private boolean atEOF = false;

    /*
     * Random notes:
     *  
     * 1. When a second IO object is created with the same fileno odd
     * concurrency issues happen when the underlying implementation
     * commits data.   So:
     * 
     * f = File.new("some file", "w")
     * f.puts("heh")
     * g = IO.new(f.fileno)
     * g.puts("hoh")
     * ... more operations of g and f ...
     * 
     * Will generate a mess in "some file".  The problem is that most
     * operations are buffered.  When those buffers flush and get
     * written to the physical file depends on the implementation
     * (semantically I would think that it should be last op wins -- but 
     * it isn't).  I doubt java could mimic ruby in this way.  I also 
     * doubt many people are taking advantage of this.  How about 
     * syswrite/sysread though?  I think the fact that sysread/syswrite 
     * are defined to be a low-level system calls, allows implementations 
     * to be somewhat different?
     * 
     * 2. In the case of:
     * f = File.new("some file", "w")
     * f.puts("heh")
     * print f.pos
     * g = IO.new(f.fileno)
     * print g.pos
     * Both printed positions will be the same.  But:
     * f = File.new("some file", "w")
     * f.puts("heh")
     * g = IO.new(f.fileno)
     * print f.pos, g.pos
     * won't be the same position.  Seem peculiar enough not to touch
     * (this involves pos() actually causing a seek?)
     * 
     * 3. All IO objects reference a IOHandler.  If multiple IO objects
     * have the same fileno, then they also share the same IOHandler.
     * It is possible that some IO objects may share the same IOHandler
     * but not have the same permissions.  However, all subsequent IO
     * objects created after the first must be a subset of the original
     * IO Object (see below for an example). 
     *
     * The idea that two or more IO objects can have different access
     * modes means that IO objects must keep track of their own
     * permissions.  In addition the IOHandler itself must know what
     * access modes it has.
     * 
     * The above sharing situation only occurs in a situation like:
     * f = File.new("some file", "r+")
     * g = IO.new(f.fileno, "r")
     * Where g has reduced (subset) permissions.
     * 
     * On reopen, the fileno's IOHandler gets replaced by a new handler. 
     */
    
    /*
     * I considered making all callers of this be moved into IOHandlers
     * constructors (since it would be less error prone to forget there).
     * However, reopen() makes doing this a little funky. 
     */
    public void registerIOHandler(IOHandler newHandler) {
        getRuntime().getIoHandlers().put(new Integer(newHandler.getFileno()), new WeakReference(newHandler));
    }
    
    public void unregisterIOHandler(int aFileno) {
        getRuntime().getIoHandlers().remove(new Integer(aFileno));
    }
    
    public IOHandler getIOHandlerByFileno(int aFileno) {
        return (IOHandler) ((WeakReference) getRuntime().getIoHandlers().get(new Integer(aFileno))).get();
    }
    
    // FIXME can't use static; would interfere with other runtimes in the same JVM
    protected static int fileno = 2;
    
    public static int getNewFileno() {
        fileno++;
        
        return fileno;
    }

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public RubyIO(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public RubyIO(Ruby runtime, OutputStream outputStream) {
        super(runtime, runtime.getClass("IO"));
        
        // We only want IO objects with valid streams (better to error now). 
        if (outputStream == null) {
            throw runtime.newIOError("Opening invalid stream");
        }
        
        try {
            handler = new IOHandlerUnseekable(runtime, null, outputStream);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
        modes = handler.getModes();
        
        registerIOHandler(handler);
    }
    
    public RubyIO(Ruby runtime, InputStream inputStream) {
        super(runtime, runtime.getClass("IO"));
        
        if (inputStream == null) {
            throw runtime.newIOError("Opening invalid stream");
        }
        
        try {
            handler = new IOHandlerUnseekable(runtime, inputStream, null);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
        
        modes = handler.getModes();
        
        registerIOHandler(handler);
    }
    
    public RubyIO(Ruby runtime, Channel channel) {
        super(runtime, runtime.getClass("IO"));
        
        // We only want IO objects with valid streams (better to error now). 
        if (channel == null) {
            throw runtime.newIOError("Opening invalid stream");
        }
        
        try {
            handler = new IOHandlerNio(runtime, channel);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
        modes = handler.getModes();
        
        registerIOHandler(handler);
    }

    public RubyIO(Ruby runtime, Process process) {
    	super(runtime, runtime.getClass("IO"));

        modes = new IOModes(runtime, "w+");

        try {
    	    handler = new IOHandlerProcess(runtime, process, modes);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
    	modes = handler.getModes();
    	
    	registerIOHandler(handler);
    }
    
    public RubyIO(Ruby runtime, int descriptor) {
        super(runtime, runtime.getClass("IO"));

        try {
            handler = new IOHandlerUnseekable(runtime, descriptor);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
        modes = handler.getModes();
        
        registerIOHandler(handler);
    }
    
    private static ObjectAllocator IO_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyIO(runtime, klass);
        }
    };

    public static RubyClass createIOClass(Ruby runtime) {
        RubyClass ioClass = runtime.defineClass("IO", runtime.getObject(), IO_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyIO.class);   
        RubyClass ioMetaClass = ioClass.getMetaClass();

        ioClass.includeModule(runtime.getModule("Enumerable"));
        
        // TODO: Implement tty? and isatty.  We have no real capability to
        // determine this from java, but if we could set tty status, then
        // we could invoke jruby differently to allow stdin to return true
        // on this.  This would allow things like cgi.rb to work properly.
        
        ioMetaClass.defineMethod("foreach", callbackFactory.getOptSingletonMethod("foreach"));
        ioMetaClass.defineMethod("read", callbackFactory.getOptSingletonMethod("read"));
        ioMetaClass.defineMethod("readlines", callbackFactory.getOptSingletonMethod("readlines"));
        ioMetaClass.defineMethod("popen", callbackFactory.getOptSingletonMethod("popen"));
        ioMetaClass.defineFastMethod("select", callbackFactory.getFastOptSingletonMethod("select"));
        ioMetaClass.defineFastMethod("pipe", callbackFactory.getFastSingletonMethod("pipe"));
        
        ioClass.defineFastMethod("<<", callbackFactory.getFastMethod("addString", IRubyObject.class));
        ioClass.defineFastMethod("binmode", callbackFactory.getFastMethod("binmode"));
        ioClass.defineFastMethod("close", callbackFactory.getFastMethod("close"));
        ioClass.defineFastMethod("closed?", callbackFactory.getFastMethod("closed"));
        ioClass.defineMethod("each", callbackFactory.getOptMethod("each_line"));
        ioClass.defineMethod("each_byte", callbackFactory.getMethod("each_byte"));
        ioClass.defineMethod("each_line", callbackFactory.getOptMethod("each_line"));
        ioClass.defineFastMethod("eof", callbackFactory.getFastMethod("eof"));
        ioClass.defineAlias("eof?", "eof");
        ioClass.defineFastMethod("fcntl", callbackFactory.getFastMethod("fcntl", IRubyObject.class, IRubyObject.class));
        ioClass.defineFastMethod("fileno", callbackFactory.getFastMethod("fileno"));
        ioClass.defineFastMethod("flush", callbackFactory.getFastMethod("flush"));
        ioClass.defineFastMethod("fsync", callbackFactory.getFastMethod("fsync"));
        ioClass.defineFastMethod("getc", callbackFactory.getFastMethod("getc"));
        ioClass.defineFastMethod("gets", callbackFactory.getFastOptMethod("gets"));
        ioClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        ioClass.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("initialize_copy", IRubyObject.class));
        ioClass.defineFastMethod("lineno", callbackFactory.getFastMethod("lineno"));
        ioClass.defineFastMethod("lineno=", callbackFactory.getFastMethod("lineno_set", IRubyObject.class));
        ioClass.defineFastMethod("pid", callbackFactory.getFastMethod("pid"));
        ioClass.defineFastMethod("pos", callbackFactory.getFastMethod("pos"));
        ioClass.defineFastMethod("pos=", callbackFactory.getFastMethod("pos_set", IRubyObject.class));
        ioClass.defineFastMethod("print", callbackFactory.getFastOptMethod("print"));
        ioClass.defineFastMethod("printf", callbackFactory.getFastOptMethod("printf"));
        ioClass.defineFastMethod("putc", callbackFactory.getFastMethod("putc", IRubyObject.class));
        ioClass.defineFastMethod("puts", callbackFactory.getFastOptMethod("puts"));
        ioClass.defineFastMethod("readpartial", callbackFactory.getFastOptMethod("readpartial"));
        ioClass.defineFastMethod("read", callbackFactory.getFastOptMethod("read"));
        ioClass.defineFastMethod("readchar", callbackFactory.getFastMethod("readchar"));
        ioClass.defineFastMethod("readline", callbackFactory.getFastOptMethod("readline"));
        ioClass.defineFastMethod("readlines", callbackFactory.getFastOptMethod("readlines"));
        ioClass.defineFastMethod("reopen", callbackFactory.getFastOptMethod("reopen"));
        ioClass.defineFastMethod("rewind", callbackFactory.getFastMethod("rewind"));
        ioClass.defineFastMethod("seek", callbackFactory.getFastOptMethod("seek"));
        ioClass.defineFastMethod("sync", callbackFactory.getFastMethod("sync"));
        ioClass.defineFastMethod("sync=", callbackFactory.getFastMethod("sync_set", IRubyObject.class));
        ioClass.defineFastMethod("sysread", callbackFactory.getFastMethod("sysread", IRubyObject.class));
        ioClass.defineFastMethod("syswrite", callbackFactory.getFastMethod("syswrite", IRubyObject.class));
        ioClass.defineAlias("tell", "pos");
        ioClass.defineAlias("to_i", "fileno");
        ioClass.defineFastMethod("to_io", callbackFactory.getFastMethod("to_io"));
        ioClass.defineFastMethod("ungetc", callbackFactory.getFastMethod("ungetc", IRubyObject.class));
        ioClass.defineFastMethod("write", callbackFactory.getFastMethod("write", IRubyObject.class));
        ioClass.defineFastMethod("tty?", callbackFactory.getFastMethod("tty"));
        ioClass.defineAlias("isatty", "tty?");

        // Constants for seek
        ioClass.setConstant("SEEK_SET", runtime.newFixnum(IOHandler.SEEK_SET));
        ioClass.setConstant("SEEK_CUR", runtime.newFixnum(IOHandler.SEEK_CUR));
        ioClass.setConstant("SEEK_END", runtime.newFixnum(IOHandler.SEEK_END));

        return ioClass;
    }    
    
    /**
     * <p>Open a file descriptor, unless it is already open, then return
     * it.</p> 
     */
    public static IRubyObject fdOpen(Ruby runtime, int descriptor) {
        return new RubyIO(runtime, descriptor);
    }

    /*
     * See checkReadable for commentary.
     */
    protected void checkWriteable() {
        if (!isOpen() || !modes.isWriteable()) {
            throw getRuntime().newIOError("not opened for writing");
        }
    }

    /*
     * What the IO object "thinks" it can do.  If two IO objects share
     * the same fileno (IOHandler), then it is possible for one to pull
     * the rug out from the other.  This will make the second object still
     * "think" that the file is open.  Secondly, if two IO objects share
     * the same fileno, but the second one only has a subset of the access
     * permissions, then it will "think" that it cannot do certain 
     * operations.
     */
    protected void checkReadable() {
        if (!isOpen() || !modes.isReadable()) {
            throw getRuntime().newIOError("not opened for reading");            
        }
    }
    
    public boolean isOpen() {
        return isOpen;
    }

    public OutputStream getOutStream() {
        if(handler instanceof IOHandlerJavaIO) {
            return ((IOHandlerJavaIO) handler).getOutputStream();
        } else {
            return null;
        }
    }

    public InputStream getInStream() {
        if (handler instanceof IOHandlerJavaIO) {
            return ((IOHandlerJavaIO) handler).getInputStream();
        } else {
            return null;
        }
    }

    public Channel getChannel() {
        if (handler instanceof IOHandlerNio) {
            return ((IOHandlerNio) handler).getChannel();
        } else {
            return null;
        }
    }

    public IRubyObject reopen(IRubyObject[] args) {
    	if (args.length < 1) {
            throw getRuntime().newArgumentError("wrong number of arguments");
    	}
    	
        if (args[0].isKindOf(getRuntime().getClass("IO"))) {
            RubyIO ios = (RubyIO) args[0];

            int keepFileno = handler.getFileno();
            
            // close the old handler before it gets overwritten
            if (handler.isOpen()) {
                try {
                    handler.close();
                } catch (IOHandler.BadDescriptorException e) {
                    throw getRuntime().newErrnoEBADFError();
                } catch (EOFException e) {
                    return getRuntime().getNil();
                } catch (IOException e) {
                    throw getRuntime().newIOError(e.getMessage());
                }
            }

            // When we reopen, we want our fileno to be preserved even
            // though we have a new IOHandler.
            // Note: When we clone we get a new fileno...then we replace it.
            // This ends up incrementing our fileno index up, which makes the
            // fileno we choose different from ruby.  Since this seems a bit
            // too implementation specific, I did not bother trying to get
            // these to agree (what scary code would depend on fileno generating
            // a particular way?)
            try {
                handler = ios.handler.cloneIOHandler();
            } catch (IOHandler.InvalidValueException e) {
            	throw getRuntime().newErrnoEINVALError();
            } catch (IOHandler.PipeException e) {
            	throw getRuntime().newErrnoESPIPEError();
            } catch (FileNotFoundException e) {
            	throw getRuntime().newErrnoENOENTError();
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
            handler.setFileno(keepFileno);

            // Update fileno list with our new handler
            registerIOHandler(handler);
        } else if (args[0].isKindOf(getRuntime().getString())) {
            String path = ((RubyString) args[0]).toString();
            IOModes newModes = null;

            if (args.length > 1) {
                if (!args[1].isKindOf(getRuntime().getString())) {
                    throw getRuntime().newTypeError(args[1], getRuntime().getString());
                }
                    
                newModes = new IOModes(getRuntime(), ((RubyString) args[1]).toString());
            }

            try {
                if (handler != null) {
                    close();
                }

                if (newModes != null) {
                	modes = newModes;
                }
                if ("/dev/null".equals(path)) {
                	handler = new IOHandlerNull(getRuntime(), modes);
                } else {
                	handler = new IOHandlerSeekable(getRuntime(), path, modes);
                }
                
                registerIOHandler(handler);
            } catch (IOHandler.InvalidValueException e) {
            	throw getRuntime().newErrnoEINVALError();
            } catch (IOException e) {
                throw getRuntime().newIOError(e.toString());
            }
        }
        
        // A potentially previously close IO is being 'reopened'.
        isOpen = true;
        return this;
    }
    /** Read a line.
     * 
     */
    // TODO: Most things loop over this and always pass it the same arguments
    // meaning they are an invariant of the loop.  Think about fixing this.
    public IRubyObject internalGets(IRubyObject[] args) {
        checkReadable();

        IRubyObject sepVal = getRuntime().getGlobalVariables().get("$/");

        if (args.length > 0) {
            sepVal = args[0];
        }

        
        ByteList separator = sepVal.isNil() ? null : ((RubyString) sepVal).getByteList();

        if (separator != null && separator.realSize == 0) {
            separator = IOHandler.PARAGRAPH_DELIMETER;
        }

        try {
            ByteList newLine = handler.gets(separator);

		    if (newLine != null) {
		        lineNumber++;
		        getRuntime().getGlobalVariables().set("$.", getRuntime().newFixnum(lineNumber));
		        RubyString result = RubyString.newString(getRuntime(), newLine);
		        result.taint();
		        
		        return result;
		    }
		    
            return getRuntime().getNil();
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }

    // IO class methods.

    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        int count = Arity.checkArgumentCount(getRuntime(), args, 1, 2);
        int newFileno = RubyNumeric.fix2int(args[0]);
        String mode = null;
        
        if (count > 1) {
            mode = args[1].convertToString().toString();
        }

        // See if we already have this descriptor open.
        // If so then we can mostly share the handler (keep open
        // file, but possibly change the mode).
        IOHandler existingIOHandler = getIOHandlerByFileno(newFileno);
        
        if (existingIOHandler == null) {
            if (mode == null) {
                mode = "r";
            }
            
            try {
                handler = new IOHandlerUnseekable(getRuntime(), newFileno, mode);
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
            modes = new IOModes(getRuntime(), mode);
            
            registerIOHandler(handler);
        } else {
            // We are creating a new IO object that shares the same
            // IOHandler (and fileno).  
            handler = existingIOHandler;
            
            // Inherit if no mode specified otherwise create new one
            modes = mode == null ? handler.getModes() :
            	new IOModes(getRuntime(), mode);

            // Reset file based on modes.
            try {
                handler.reset(modes);
            } catch (IOHandler.InvalidValueException e) {
            	throw getRuntime().newErrnoEINVALError();
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
        }
        
        return this;
    }
	
	// This appears to be some windows-only mode.  On a java platform this is a no-op
	public IRubyObject binmode() {
		return this;
	}

    public IRubyObject syswrite(IRubyObject obj) {
        try {
            if (obj instanceof RubyString) {
                return getRuntime().newFixnum(handler.syswrite(((RubyString)obj).getByteList()));
            } else {
                // FIXME: unlikely to be efficient, but probably correct
                return getRuntime().newFixnum(
                        handler.syswrite(
                        ((RubyString)obj.callMethod(
                            obj.getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s")).getByteList()));
            }
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newSystemCallError(e.getMessage());
        }
    }
    
    /** io_write
     * 
     */
    public IRubyObject write(IRubyObject obj) {
        checkWriteable();

        try {
            if (obj instanceof RubyString) {
                return getRuntime().newFixnum(handler.write(((RubyString)obj).getByteList()));
            } else {
                // FIXME: unlikely to be efficient, but probably correct
                return getRuntime().newFixnum(
                        handler.write(
                        ((RubyString)obj.callMethod(
                            obj.getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s")).getByteList()));
            }
        } catch (IOHandler.BadDescriptorException e) {
            return RubyFixnum.zero(getRuntime());
        } catch (IOException e) {
            return RubyFixnum.zero(getRuntime());
        }
    }

    /** rb_io_addstr
     * 
     */
    public IRubyObject addString(IRubyObject anObject) {
        // Claims conversion is done via 'to_s' in docs.
        IRubyObject strObject = anObject.callMethod(getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s");

        write(strObject);
        
        return this; 
    }

    public RubyFixnum fileno() {
        return getRuntime().newFixnum(handler.getFileno());
    }
    
    /** Returns the current line number.
     * 
     * @return the current line number.
     */
    public RubyFixnum lineno() {
        return getRuntime().newFixnum(lineNumber);
    }

    /** Sets the current line number.
     * 
     * @param newLineNumber The new line number.
     */
    public RubyFixnum lineno_set(IRubyObject newLineNumber) {
        lineNumber = RubyNumeric.fix2int(newLineNumber);

        return (RubyFixnum) newLineNumber;
    }

    /** Returns the current sync mode.
     * 
     * @return the current sync mode.
     */
    public RubyBoolean sync() {
        return getRuntime().newBoolean(handler.isSync());
    }
    
    /**
     * <p>Return the process id (pid) of the process this IO object
     * spawned.  If no process exists (popen was not called), then
     * nil is returned.  This is not how it appears to be defined
     * but ruby 1.8 works this way.</p>
     * 
     * @return the pid or nil
     */
    public IRubyObject pid() {
        int pid = handler.pid();
        
        return pid == -1 ? getRuntime().getNil() : getRuntime().newFixnum(pid); 
    }
    
    public boolean hasPendingBuffered() {
        return handler.hasPendingBuffered();
    }
    
    public RubyFixnum pos() {
        try {
            return getRuntime().newFixnum(handler.pos());
        } catch (IOHandler.PipeException e) {
        	throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }
    
    public RubyFixnum pos_set(IRubyObject newPosition) {
        long offset = RubyNumeric.fix2long(newPosition);

        if (offset < 0) {
            throw getRuntime().newSystemCallError("Negative seek offset");
        }
        
        try {
            handler.seek(offset, IOHandler.SEEK_SET);
        } catch (IOHandler.InvalidValueException e) {
        	throw getRuntime().newErrnoEINVALError();
        } catch (IOHandler.PipeException e) {
        	throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        
        return (RubyFixnum) newPosition;
    }
    
    /** Print some objects to the stream.
     * 
     */
    public IRubyObject print(IRubyObject[] args) {
        if (args.length == 0) {
            args = new IRubyObject[] { getRuntime().getCurrentContext().getLastline() };
        }

        IRubyObject fs = getRuntime().getGlobalVariables().get("$,");
        IRubyObject rs = getRuntime().getGlobalVariables().get("$\\");
        ThreadContext context = getRuntime().getCurrentContext();
        
        for (int i = 0; i < args.length; i++) {
            if (i > 0 && !fs.isNil()) {
                callMethod(context, "write", fs);
            }
            if (args[i].isNil()) {
                callMethod(context, "write", getRuntime().newString("nil"));
            } else {
                callMethod(context, "write", args[i]);
            }
        }
        if (!rs.isNil()) {
            callMethod(context, "write", rs);
        }

        return getRuntime().getNil();
    }

    public IRubyObject printf(IRubyObject[] args) {
    	Arity.checkArgumentCount(getRuntime(), args, 1, -1);
        callMethod(getRuntime().getCurrentContext(), "write", RubyKernel.sprintf(this, args));
        return getRuntime().getNil();
    }
    
    public IRubyObject putc(IRubyObject object) {
        int c;
        
        if (object.isKindOf(getRuntime().getString())) {
            String value = ((RubyString) object).toString();
            
            if (value.length() > 0) {
                c = value.charAt(0);
            } else {
                throw getRuntime().newTypeError("Cannot convert String to Integer");
            }
        } else if (object.isKindOf(getRuntime().getFixnum())){
            c = RubyNumeric.fix2int(object);
        } else { // What case will this work for?
            c = RubyNumeric.fix2int(object.callMethod(getRuntime().getCurrentContext(), MethodIndex.TO_I, "to_i"));
        }

        try {
            handler.putc(c);
        } catch (IOHandler.BadDescriptorException e) {
            return RubyFixnum.zero(getRuntime());
        } catch (IOException e) {
            return RubyFixnum.zero(getRuntime());
        }
        
        return object;
    }
    
    // This was a getOpt with one mandatory arg, but it did not work
    // so I am parsing it for now.
    public RubyFixnum seek(IRubyObject[] args) {
        if (args.length == 0) {
            throw getRuntime().newArgumentError("wrong number of arguments");
        }
        
        long offset = RubyNumeric.fix2long(args[0]);
        int type = IOHandler.SEEK_SET;
        
        if (args.length > 1) {
            type = RubyNumeric.fix2int(args[1].convertToInteger());
        }
        
        try {
            handler.seek(offset, type);
        } catch (IOHandler.InvalidValueException e) {
        	throw getRuntime().newErrnoEINVALError();
        } catch (IOHandler.PipeException e) {
        	throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        
        return RubyFixnum.zero(getRuntime());
    }

    public RubyFixnum rewind() {
        try {
		    handler.rewind();
        } catch (IOHandler.InvalidValueException e) {
        	throw getRuntime().newErrnoEINVALError();
        } catch (IOHandler.PipeException e) {
        	throw getRuntime().newErrnoESPIPEError();
	    } catch (IOException e) {
	        throw getRuntime().newIOError(e.getMessage());
	    }

        // Must be back on first line on rewind.
        lineNumber = 0;
        
        return RubyFixnum.zero(getRuntime());
    }
    
    public RubyFixnum fsync() {
        checkWriteable();

        try {
            handler.sync();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }

        return RubyFixnum.zero(getRuntime());
    }

    /** Sets the current sync mode.
     * 
     * @param newSync The new sync mode.
     */
    public IRubyObject sync_set(IRubyObject newSync) {
        handler.setIsSync(newSync.isTrue());

        return this;
    }

    public RubyBoolean eof() {
        try {
            boolean isEOF = handler.isEOF(); 
            return isEOF ? getRuntime().getTrue() : getRuntime().getFalse();
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }

    public RubyBoolean tty() {
        // TODO: this is less than ideal but might be as close as we'll get
        int fileno = handler.getFileno();
        if (fileno == STDOUT || fileno == STDIN || fileno == STDERR) {
            return getRuntime().getTrue();
        } else {
            return getRuntime().getFalse();
        }
    }
    
    public IRubyObject initialize_copy(IRubyObject original){
        if (this == original) return this;

        RubyIO originalIO = (RubyIO) original;
        
        // Two pos pointers?  
        // http://blade.nagaokaut.ac.jp/ruby/ruby-talk/81513
        // So if I understand this correctly, the descriptor level stuff
        // shares things like position, but the higher level stuff uses
        // a different set of libc functions (FILE*), which does not share
        // position.  Our current implementation implements our higher 
        // level operations on top of our 'sys' versions.  So we could in
        // fact share everything.  Unfortunately, we want to clone ruby's
        // behavior (i.e. show how this interface bleeds their 
        // implementation). So our best bet, is to just create a yet another
        // copy of the handler.  In fact, ruby 1.8 must do this as the cloned
        // resource is in fact a different fileno.  What is clone for again?        
        
        handler = originalIO.handler;
        modes = (IOModes) originalIO.modes.clone();
        
        return this;
    }
    
    /** Closes the IO.
     * 
     * @return The IO.
     */
    public RubyBoolean closed() {
        return isOpen() ? getRuntime().getFalse() : getRuntime().getTrue();
    }

    /** 
     * <p>Closes all open resources for the IO.  It also removes
     * it from our magical all open file descriptor pool.</p>
     * 
     * @return The IO.
     */
    public IRubyObject close() {
        isOpen = false;
        
        try {
            handler.close();
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        
        unregisterIOHandler(handler.getFileno());
        
        return this;
    }

    /** Flushes the IO output stream.
     * 
     * @return The IO.
     */
    public RubyIO flush() {
        try { 
            handler.flush();
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }

        return this;
    }

    /** Read a line.
     * 
     */
    public IRubyObject gets(IRubyObject[] args) {
        IRubyObject result = internalGets(args);

        if (!result.isNil()) getRuntime().getCurrentContext().setLastline(result);

        return result;
    }

    public boolean getBlocking() {
        if (!(handler instanceof IOHandlerNio)) return true;

        return ((IOHandlerNio) handler).getBlocking();
     }

     public IRubyObject fcntl(IRubyObject cmd, IRubyObject arg) throws IOException {
        long realCmd = cmd.convertToInteger().getLongValue();
        
        // FIXME: Arg may also be true, false, and nil and still be valid.  Strangely enough, 
        // protocol conversion is not happening in Ruby on this arg?
        if (!(arg instanceof RubyNumeric)) return getRuntime().newFixnum(0);
        
        long realArg = ((RubyNumeric)arg).getLongValue();

        // Fixme: Only F_SETFL is current supported
        if (realCmd == 1L) {  // cmd is F_SETFL
            boolean block = true;
            
            if ((realArg & IOModes.NONBLOCK) == IOModes.NONBLOCK) {
                block = false;
            }
            
            if (!(handler instanceof IOHandlerNio)) {
                // cryptic for the uninitiated...
                throw getRuntime().newNotImplementedError("FCNTL only works with Nio based handlers");
            }

            try {
                ((IOHandlerNio) handler).setBlocking(block);
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
        }
        
        return getRuntime().newFixnum(0);
    }

    public IRubyObject puts(IRubyObject[] args) {
    	Arity.checkArgumentCount(getRuntime(), args, 0, -1);
        
    	ThreadContext context = getRuntime().getCurrentContext();
        
        if (args.length == 0) {
            callMethod(context, "write", getRuntime().newString("\n"));
            return getRuntime().getNil();
        }

        for (int i = 0; i < args.length; i++) {
            String line = null;
            if (args[i].isNil()) {
                line = "nil";
            } else if (args[i] instanceof RubyArray) {
                puts(((RubyArray) args[i]).toJavaArray());
                continue;
            } else {
                line = args[i].toString();
            }
            callMethod(getRuntime().getCurrentContext(), "write", getRuntime().newString(line+
            		(line.endsWith("\n") ? "" : "\n")));
        }
        return getRuntime().getNil();
    }

    /** Read a line.
     * 
     */
    public IRubyObject readline(IRubyObject[] args) {
        IRubyObject line = gets(args);

        if (line.isNil()) {
            throw getRuntime().newEOFError();
        }
        
        return line;
    }

    /** Read a byte. On EOF returns nil.
     * 
     */
    public IRubyObject getc() {
        checkReadable();
        
        try {
            int c = handler.getc();
        
            return c == -1 ? getRuntime().getNil() : getRuntime().newFixnum(c);
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }
    
    /** 
     * <p>Pushes char represented by int back onto IOS.</p>
     * 
     * @param number to push back
     */
    public IRubyObject ungetc(IRubyObject number) {
        handler.ungetc(RubyNumeric.fix2int(number));

        return getRuntime().getNil();
    }
    
    public IRubyObject readpartial(IRubyObject[] args) {
        if(!(handler instanceof IOHandlerNio)) {
            // cryptic for the uninitiated...
            throw getRuntime().newNotImplementedError("readpartial only works with Nio based handlers");
        }
    	try {
            ByteList buf = ((IOHandlerNio)handler).readpartial(RubyNumeric.fix2int(args[0]));
            IRubyObject strbuf = RubyString.newString(getRuntime(), buf == null ? new ByteList(ByteList.NULL_ARRAY) : buf);
            if(args.length > 1) {
                args[1].callMethod(getRuntime().getCurrentContext(),MethodIndex.OP_LSHIFT, "<<", strbuf);
                return args[1];
            } 

            return strbuf;
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            return getRuntime().getNil();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }

    public IRubyObject sysread(IRubyObject number) {
        try {
            ByteList buf = handler.sysread(RubyNumeric.fix2int(number));
        
            return RubyString.newString(getRuntime(), buf);
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
    		throw getRuntime().newEOFError();
    	} catch (IOException e) {
    		// All errors to sysread should be SystemCallErrors, but on a closed stream
    		// Ruby returns an IOError.  Java throws same exception for all errors so
    		// we resort to this hack...
    		if ("File not open".equals(e.getMessage())) {
    			throw getRuntime().newIOError(e.getMessage());
    		}
    	    throw getRuntime().newSystemCallError(e.getMessage());
    	}
    }
    
    public IRubyObject read(IRubyObject[] args) {
               
        int argCount = Arity.checkArgumentCount(getRuntime(), args, 0, 2);
        RubyString callerBuffer = null;
        boolean readEntireStream = (argCount == 0 || args[0].isNil());

        try {
            // Reads when already at EOF keep us at EOF
            // We do retain the possibility of un-EOFing if the handler
            // gets new data
            if (atEOF && handler.isEOF()) throw new EOFException();

            if (argCount == 2) {
                // rdocs say the second arg must be a String if present,
                // it can't just walk and quack like one
                if (!(args[1] instanceof RubyString)) {
                    getRuntime().newTypeError(args[1], getRuntime().getString());
                }
                callerBuffer = (RubyString) args[1];
            }

            ByteList buf = readEntireStream ? handler.getsEntireStream() : 
                handler.read(RubyNumeric.fix2int(args[0]));
            
            if (buf == null) throw new EOFException();

            // If we get here then no EOFException was thrown in the handler.  We
            // might still need to set our atEOF flag back to true depending on
            // whether we were reading the entire stream (see the finally block below)
            atEOF = false;
            if (callerBuffer != null) {
                callerBuffer.setValue(buf);
                return callerBuffer;
            }
            
            return RubyString.newString(getRuntime(), buf);
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            // on EOF, IO#read():
            // with no args or a nil first arg will return an empty string
            // with a non-nil first arg will return nil
            atEOF = true;
            if (callerBuffer != null) {
                callerBuffer.setValue("");
                return readEntireStream ? callerBuffer : getRuntime().getNil();
            }

            return readEntireStream ? getRuntime().newString("") : getRuntime().getNil();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        } finally {
            // reading the entire stream always puts us at EOF
            if (readEntireStream) {
                atEOF = true;
            }
        }
    }

    /** Read a byte. On EOF throw EOFError.
     * 
     */
    public IRubyObject readchar() {
        checkReadable();
        
        try {
            int c = handler.getc();
        
            if (c == -1) throw getRuntime().newEOFError();
        
            return getRuntime().newFixnum(c);
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }

    /** 
     * <p>Invoke a block for each byte.</p>
     */
    public IRubyObject each_byte(Block block) {
    	try {
            ThreadContext context = getRuntime().getCurrentContext();
            for (int c = handler.getc(); c != -1; c = handler.getc()) {
                assert c < 256;
                block.yield(context, getRuntime().newFixnum(c));
            }

            return getRuntime().getNil();
        } catch (IOHandler.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            return getRuntime().getNil();
    	} catch (IOException e) {
    	    throw getRuntime().newIOError(e.getMessage());
        }
    }

    /** 
     * <p>Invoke a block for each line.</p>
     */
    public RubyIO each_line(IRubyObject[] args, Block block) {
        ThreadContext context = getRuntime().getCurrentContext();
        for (IRubyObject line = internalGets(args); !line.isNil(); 
        	line = internalGets(args)) {
            block.yield(context, line);
        }
        
        return this;
    }


    public RubyArray readlines(IRubyObject[] args) {
        IRubyObject[] separatorArgument;
        if (args.length > 0) {
            if (!args[0].isKindOf(getRuntime().getNilClass()) &&
                !args[0].isKindOf(getRuntime().getString())) {
                throw getRuntime().newTypeError(args[0], 
                        getRuntime().getString());
            } 
            separatorArgument = new IRubyObject[] { args[0] };
        } else {
            separatorArgument = IRubyObject.NULL_ARRAY;
        }

        RubyArray result = getRuntime().newArray();
        IRubyObject line;
        while (! (line = internalGets(separatorArgument)).isNil()) {
            result.append(line);
        }
        return result;
    }
    
    public RubyIO to_io() {
    	return this;
    }

    public String toString() {
        return "RubyIO(" + modes + ", " + fileno + ")";
    }
    
    /* class methods for IO */
    
    /** rb_io_s_foreach
    *
    */
   public static IRubyObject foreach(IRubyObject recv, IRubyObject[] args, Block block) {
       Ruby runtime = recv.getRuntime();
       int count = Arity.checkArgumentCount(runtime, args, 1, -1);
       IRubyObject filename = args[0].convertToString();
       runtime.checkSafeString(filename);
       RubyIO io = (RubyIO) RubyFile.open(recv, new IRubyObject[] { filename }, false, block);
       
       if (!io.isNil() && io.isOpen()) {
           try {
               IRubyObject[] newArgs = new IRubyObject[count - 1];
               System.arraycopy(args, 1, newArgs, 0, count - 1);
               
               IRubyObject nextLine = io.internalGets(newArgs);
               while (!nextLine.isNil()) {
                   block.yield(runtime.getCurrentContext(), nextLine);
                   nextLine = io.internalGets(newArgs);
               }
           } finally {
               io.close();
           }
       }
       
       return runtime.getNil();
   }
   
   private static RubyIO registerSelect(Selector selector, IRubyObject obj, int ops) throws IOException {
       RubyIO ioObj;
       
       if (!(obj instanceof RubyIO)) {
           // invoke to_io
           if (!obj.respondsTo("to_io")) return null;

           ioObj = (RubyIO) obj.callMethod(obj.getRuntime().getCurrentContext(), "to_io");
       } else {
           ioObj = (RubyIO) obj;
       }
       
       Channel channel = ioObj.getChannel();
       if (channel == null || !(channel instanceof SelectableChannel)) {
           return null;
       }
       
       ((SelectableChannel) channel).configureBlocking(false);
       int real_ops = ((SelectableChannel) channel).validOps() & ops;
       SelectionKey key = ((SelectableChannel) channel).keyFor(selector);
       
       if (key == null) {
           ((SelectableChannel) channel).register(selector, real_ops, obj);
       } else {
           key.interestOps(key.interestOps()|real_ops);
       }
       
       return ioObj;
   }
   
   public static IRubyObject select(IRubyObject recv, IRubyObject[] args) {
       return select_static(recv.getRuntime(), args);
   }
   
   public static IRubyObject select_static(Ruby runtime, IRubyObject[] args) {
       try {
           boolean atLeastOneDescriptor = false;
           
           Set pending = new HashSet();
           Selector selector = Selector.open();
           if (!args[0].isNil()) {
               atLeastOneDescriptor = true;
               
               // read
               for (Iterator i = ((RubyArray) args[0]).getList().iterator(); i.hasNext(); ) {
                   IRubyObject obj = (IRubyObject) i.next();
                   RubyIO ioObj = registerSelect(selector, obj, 
                           SelectionKey.OP_READ | SelectionKey.OP_ACCEPT);
                   
                   if (ioObj!=null && ioObj.hasPendingBuffered()) pending.add(obj);
               }
           }
           if (args.length > 1 && !args[1].isNil()) {
               atLeastOneDescriptor = true;
               // write
               for (Iterator i = ((RubyArray) args[1]).getList().iterator(); i.hasNext(); ) {
                   IRubyObject obj = (IRubyObject) i.next();
                   registerSelect(selector, obj, SelectionKey.OP_WRITE);
               }
           }
           if (args.length > 2 && !args[2].isNil()) {
               atLeastOneDescriptor = true;
               // Java's select doesn't do anything about this, so we leave it be.
           }
           
           long timeout = 0;
           if(args.length > 3 && !args[3].isNil()) {
               if (args[3] instanceof RubyFloat) {
                   timeout = Math.round(((RubyFloat) args[3]).getDoubleValue() * 1000);
               } else {
                   timeout = Math.round(((RubyFixnum) args[3]).getDoubleValue() * 1000);
               }
               
               if (timeout < 0) {
                   throw runtime.newArgumentError("negative timeout given");
               }
           }
           
           if (!atLeastOneDescriptor) {
               return runtime.getNil();
           }
           
           if (pending.isEmpty()) {
               if (args.length > 3) {
                   if (timeout==0) {
                       selector.selectNow();
                   } else {
                       selector.select(timeout);                       
                   }
               } else {
                   selector.select();
               }
           } else {
               selector.selectNow();               
           }
           
           List r = new ArrayList();
           List w = new ArrayList();
           List e = new ArrayList();
           for (Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
               SelectionKey key = (SelectionKey) i.next();
               if ((key.interestOps() & key.readyOps()
                       & (SelectionKey.OP_READ|SelectionKey.OP_ACCEPT|SelectionKey.OP_CONNECT)) != 0) {
                   r.add(key.attachment());
                   pending.remove(key.attachment());
               }
               if ((key.interestOps() & key.readyOps() & (SelectionKey.OP_WRITE)) != 0) {
                   w.add(key.attachment());
               }
           }
           r.addAll(pending);
           
           // make all sockets blocking as configured again
           for (Iterator i = selector.keys().iterator(); i.hasNext(); ) {
               SelectionKey key = (SelectionKey) i.next();
               SelectableChannel channel = key.channel();
               synchronized(channel.blockingLock()) {
                   boolean blocking = ((RubyIO) key.attachment()).getBlocking();
                   key.cancel();
                   channel.configureBlocking(blocking);
               }
           }
           selector.close();
           
           if (r.size() == 0 && w.size() == 0 && e.size() == 0) {
               return runtime.getNil();
           }
           
           List ret = new ArrayList();
           
           ret.add(RubyArray.newArray(runtime, r));
           ret.add(RubyArray.newArray(runtime, w));
           ret.add(RubyArray.newArray(runtime, e));
           
           return RubyArray.newArray(runtime, ret);
       } catch(IOException e) {
           throw runtime.newIOError(e.getMessage());
       }
   }
   
   public static IRubyObject read(IRubyObject recv, IRubyObject[] args, Block block) {
       Ruby runtime = recv.getRuntime();
       Arity.checkArgumentCount(runtime, args, 1, 3);
       IRubyObject[] fileArguments = new IRubyObject[] {args[0]};
       RubyIO file = (RubyIO) RubyKernel.open(recv, fileArguments, block);
       IRubyObject[] readArguments;
       
       if (args.length >= 2) {
           readArguments = new IRubyObject[] {args[1].convertToType(runtime.getFixnum(), MethodIndex.TO_INT, "to_int", true)};
       } else {
           readArguments = new IRubyObject[] {};
       }
       
       try {
           
           if (args.length == 3) {
               file.seek(new IRubyObject[] {args[2].convertToType(runtime.getFixnum(), MethodIndex.TO_INT, "to_int", true)});
           }
           
           return file.read(readArguments);
       } finally {
           file.close();
       }
   }
   
   public static RubyArray readlines(IRubyObject recv, IRubyObject[] args, Block block) {
       int count = Arity.checkArgumentCount(recv.getRuntime(), args, 1, 2);
       
       IRubyObject[] fileArguments = new IRubyObject[] {args[0]};
       IRubyObject[] separatorArguments = count >= 2 ? new IRubyObject[]{args[1]} : IRubyObject.NULL_ARRAY;
       RubyIO file = (RubyIO) RubyKernel.open(recv, fileArguments, block);
       try {
           return file.readlines(separatorArguments);
       } finally {
           file.close();
       }
   }
   
   //XXX Hacked incomplete popen implementation to make
   public static IRubyObject popen(IRubyObject recv, IRubyObject[] args, Block block) {
       Ruby runtime = recv.getRuntime();
       Arity.checkArgumentCount(runtime, args, 1, 2);
       IRubyObject cmdObj = args[0].convertToString();
       runtime.checkSafeString(cmdObj);
       
       try {
           Process process = new ShellLauncher(runtime).run(cmdObj);            
           RubyIO io = new RubyIO(runtime, process);
           
           if (block.isGiven()) {
               try {
                   block.yield(runtime.getCurrentContext(), io);
                   return runtime.getNil();
               } finally {
                   io.close();
                   runtime.getGlobalVariables().set("$?",  RubyProcess.RubyStatus.newProcessStatus(runtime, (process.waitFor() * 256)));
               }
           }
           return io;
       } catch (IOException e) {
           throw runtime.newIOErrorFromException(e);
       } catch (InterruptedException e) {
           throw runtime.newThreadError("unexpected interrupt");
       }
   }
   
   // NIO based pipe
   public static IRubyObject pipe(IRubyObject recv) throws Exception {
       Ruby runtime = recv.getRuntime();
       Pipe pipe = Pipe.open();
       return runtime.newArrayNoCopy(new IRubyObject[]{
           new RubyIO(runtime, pipe.source()),
           new RubyIO(runtime, pipe.sink())
       });
   }    
}
