/*
 * RubyIO.java - No description
 * Created on 12.01.2002, 19:14:58
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Copyright (C) 2004 Thomas E Enebo, Charles O Nutter
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 * Thomas E Enebo <enebo@acm.org>
 * Charles O Nutter <headius@headius.com>
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import org.jruby.exceptions.EOFError;
import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.SystemCallError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandler;
import org.jruby.util.IOHandlerProcess;
import org.jruby.util.IOHandlerSeekable;
import org.jruby.util.IOHandlerUnseekable;
import org.jruby.util.IOModes;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyIO extends RubyObject {
    public static final int STDIN = 0;
    public static final int STDOUT = 1;
    public static final int STDERR = 2;
    
    protected IOHandler handler = null;
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
    public void registerIOHandler(IOHandler handler) {
        getRuntime().ioHandlers.put(new Integer(handler.getFileno()), new WeakReference(handler));
    }
    
    public void unregisterIOHandler(int fileno) {
        getRuntime().ioHandlers.remove(new Integer(fileno));
    }
    
    public IOHandler getIOHandlerByFileno(int fileno) {
        return (IOHandler) ((WeakReference) getRuntime().ioHandlers.get(new Integer(fileno))).get();
    }
    
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
        super(runtime, runtime.getClasses().getIoClass());
        
        // We only want IO objects with valid streams (better to error now). 
        if (outputStream == null) {
            throw new IOError(runtime, "Opening invalid stream");
        }
        
        handler = new IOHandlerUnseekable(runtime, null, outputStream);
        modes = handler.getModes();
        
        registerIOHandler(handler);
    }
    
    public RubyIO(Ruby runtime, Process process) {
    	super(runtime, runtime.getClasses().getIoClass());

        modes = new IOModes(runtime, "w+");

    	handler = new IOHandlerProcess(runtime, process, modes);
    	modes = handler.getModes();
    	
    	registerIOHandler(handler);
    }
    
    public RubyIO(Ruby runtime, int descriptor) {
        super(runtime, runtime.getClasses().getIoClass());

        handler = new IOHandlerUnseekable(runtime, descriptor);
        modes = handler.getModes();
        
        registerIOHandler(handler);
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
            throw new IOError(getRuntime(), "not opened for writing");
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
            throw new IOError(getRuntime(), "not opened for reading");            
        }
    }
    
    public boolean isOpen() {
        return isOpen;
    }

    public OutputStream getOutStream() {
        return handler.getOutputStream();
    }

    public InputStream getInStream() {
        return handler.getInputStream();
    }
    
    public IRubyObject reopen(IRubyObject arg1, IRubyObject[] args) {
        if (arg1.isKindOf(getRuntime().getClasses().getIoClass())) {
            RubyIO ios = (RubyIO) arg1;

            int keepFileno = handler.getFileno();
            // When we reopen, we want our fileno to be preserved even
            // though we have a new IOHandler.
            // Note: When we clone we get a new fileno...then we replace it.
            // This ends up incrementing our fileno index up, which makes the
            // fileno we choose different from ruby.  Since this seems a bit
            // too implementation specific, I did not bother trying to get
            // these to agree (what scary code would depend on fileno generating
            // a particular way?)
            handler = ios.handler.cloneIOHandler();
            handler.setFileno(keepFileno);
            
            // Update fileno list with our new handler
            registerIOHandler(handler);
        } else if (arg1.isKindOf(getRuntime().getClasses().getStringClass())) {
            String path = ((RubyString) arg1).getValue();
            String mode = "r";
            if (args != null && args.length > 0) {
                if (!args[0].isKindOf(getRuntime().getClasses().getStringClass())) {
                    throw getRuntime().newTypeError(args[0], 
                            getRuntime().getClasses().getStringClass());
                }
                    
                mode = ((RubyString) args[0]).getValue();
            }

            try {
                if (handler != null) {
                    close();
                }
                modes = new IOModes(getRuntime(), mode);
                handler = new IOHandlerSeekable(getRuntime(), path, modes);
                
                registerIOHandler(handler);
            } catch (IOException e) {
                throw new IOError(getRuntime(), e.toString());
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

        
        String separator = sepVal.isNil() ? null : ((RubyString) sepVal).getValue();

        if (separator != null && separator.length() == 0) {
            separator = IOHandler.PARAGRAPH_DELIMETER;
        }

        String newLine = handler.gets(separator);

		if (newLine != null) {
		    lineNumber++;
		    getRuntime().getGlobalVariables().set("$.", getRuntime().newFixnum(lineNumber));
		    RubyString result = getRuntime().newString(newLine);
		    result.taint();
		    return result;
		}
        return getRuntime().getNil();
    }

    // IO class methods.

    /** rb_io_initialize
     * 
     */
    public IRubyObject initialize(IRubyObject[] args) {
        int count = checkArgumentCount(args, 1, 2);
        int fileno = RubyNumeric.fix2int(args[0]);
        String mode = null;
        
        if (count > 1) {
            mode = ((RubyString)args[1].convertToString()).getValue();
        }

        // See if we already have this descriptor open.
        // If so then we can mostly share the handler (keep open
        // file, but possibly change the mode).
        IOHandler existingIOHandler = getIOHandlerByFileno(fileno);
        
        if (existingIOHandler == null) {
            if (mode == null) {
                mode = "r";
            }
            handler = new IOHandlerUnseekable(getRuntime(), fileno, mode);
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
            handler.reset(modes);
        }
        
        return this;
    }

    public IRubyObject syswrite(IRubyObject obj) {
        return getRuntime().newFixnum(handler.syswrite(obj.toString()));
    }
    
    /** io_write
     * 
     */
    public IRubyObject write(IRubyObject obj) {
        checkWriteable();

        try {
            int totalWritten = handler.write(obj.toString());

            return getRuntime().newFixnum(totalWritten);
        } catch (IOError e) {
            return RubyFixnum.zero(getRuntime());
        } catch (ErrnoError e) {
            return RubyFixnum.zero(getRuntime());
        }
    }

    /** rb_io_addstr
     * 
     */
    public IRubyObject addString(IRubyObject anObject) {
        // Claims conversion is done via 'to_s' in docs.
        IRubyObject strObject = anObject.callMethod("to_s");

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
    public RubyFixnum lineno_set(RubyFixnum newLineNumber) {
        lineNumber = RubyNumeric.fix2int(newLineNumber);

        return newLineNumber;
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
        
        if (pid == -1) {
            return getRuntime().getNil();
        }
        
        return getRuntime().newFixnum(pid);
    }
    
    public RubyFixnum pos() {
        return getRuntime().newFixnum(handler.pos());
    }
    
    public RubyFixnum pos_set(RubyFixnum newPosition) {
        long offset = RubyNumeric.fix2long(newPosition);

        if (offset < 0) {
            throw new SystemCallError(getRuntime(), "Negative seek offset");
        }
        
        handler.seek(offset, IOHandler.SEEK_SET);
        
        return newPosition;
    }
    
    public IRubyObject putc(IRubyObject object) {
        int c;
        
        if (object.isKindOf(getRuntime().getClasses().getStringClass())) {
            String value = ((RubyString) object).toString();
            
            if (value.length() > 0) {
                c = value.charAt(0);
            } else {
                throw getRuntime().newTypeError(
                        "Cannot convert String to Integer");
            }
        } else if (object.isKindOf(getRuntime().getClasses().getFixnumClass())){
            c = RubyNumeric.fix2int(object);
        } else { // What case will this work for?
            c = RubyNumeric.fix2int(object.callMethod("to_i"));
        }

        try {
            handler.putc(c);
        } catch (ErrnoError e) {
            return RubyFixnum.zero(getRuntime());
        }
        
        return getRuntime().newFixnum(c);
    }
    
    // This was a getOpt with one mandatory arg, but it did not work
    // so I am parsing it for now.
    public RubyFixnum seek(IRubyObject[] args) {
        if (args == null || args.length == 0) {
            throw getRuntime().newArgumentError("wrong number of arguments");
        }
        
        RubyFixnum offsetValue = (RubyFixnum) args[0];
        long offset = RubyNumeric.fix2long(offsetValue);
        int type = IOHandler.SEEK_SET;
        
        if (args.length > 1) {
            type = RubyNumeric.fix2int(RubyNumeric.numericValue(args[1]));
        }
        
        handler.seek(offset, type);
        
        return RubyFixnum.zero(getRuntime());
    }

    public RubyFixnum rewind() {
        handler.rewind();

        // Must be back on first line on rewind.
        lineNumber = 0;
        
        return RubyFixnum.zero(getRuntime());
    }
    
    public RubyFixnum fsync() {
        checkWriteable();

        try {
            handler.sync();
        } catch (IOException e) {
            throw IOError.fromException(getRuntime(), e);
        }

        return RubyFixnum.zero(getRuntime());
    }

    /** Sets the current sync mode.
     * 
     * @param newSync The new sync mode.
     */
    public IRubyObject sync_set(RubyBoolean newSync) {
        handler.setIsSync(newSync.isTrue());

        return this;
    }

    public RubyBoolean eof() {
        boolean isEOF = handler.isEOF(); 
        return isEOF ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    public RubyIO clone_IO() {
        RubyIO io = new RubyIO(getRuntime(), getMetaClass());

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
        io.handler = handler;
        
        return io;
    }
    
    /** Closes the IO.
     * 
     * @return The IO.
     */
    public RubyBoolean closed() {
        return isOpen() ? getRuntime().getFalse() :
            getRuntime().getTrue();
    }

    /** 
     * <p>Closes all open resources for the IO.  It also removes
     * it from our magical all open file descriptor pool.</p>
     * 
     * @return The IO.
     */
    public IRubyObject close() {
        isOpen = false;
        handler.close();
        
        unregisterIOHandler(handler.getFileno());
        
        return this;
    }

    /** Flushes the IO output stream.
     * 
     * @return The IO.
     */
    public RubyIO flush() {
        handler.flush();

        return this;
    }

    /** Read a line.
     * 
     */
    public IRubyObject gets(IRubyObject[] args) {
        IRubyObject result = internalGets(args);

        if (!result.isNil()) {
            getRuntime().setLastline(result);
        }

        return result;
    }

    /** Read a line.
     * 
     */
    public IRubyObject readline(IRubyObject[] args) {
        IRubyObject line = gets(args);

        if (line.isNil()) {
            throw new EOFError(getRuntime());
        }
        
        return line;
    }

    /** Read a byte. On EOF returns nil.
     * 
     */
    public IRubyObject getc() {
        checkReadable();
        
        int c = handler.getc();
        
        return c == -1 ? 
            getRuntime().getNil() : // EOF
        	getRuntime().newFixnum(c);
    }
    
    /** 
     * <p>Pushes char represented by int back onto IOS.</p>
     * 
     * @param number to push back
     */
    public IRubyObject ungetc(RubyFixnum number) {
        handler.ungetc(RubyNumeric.fix2int(number));

        return getRuntime().getNil();
    }
    
    public IRubyObject sysread(RubyFixnum number) {
        String buf = handler.sysread(RubyNumeric.fix2int(number));
        
        return getRuntime().newString(buf);
    }
    
    public IRubyObject read(IRubyObject[] args) {
        String buf = args.length > 0 ? handler.read(RubyNumeric.fix2int(args[0])) : handler.getsEntireStream();

        return buf == null ? getRuntime().getNil() : getRuntime().newString(buf);
    }

    /** Read a byte. On EOF throw EOFError.
     * 
     */
    public IRubyObject readchar() {
        checkReadable();
        
        int c = handler.getc();
        
        if (c == -1) {
            throw new EOFError(getRuntime());
        }
        
        return getRuntime().newFixnum(c);
    }

    /** 
     * <p>Invoke a block for each byte.</p>
     */
    public IRubyObject each_byte() {
        for (int c = handler.getc(); c != -1; c = handler.getc()) {
            assert c < 256;
            getRuntime().yield(getRuntime().newFixnum(c));
        }

        return getRuntime().getNil();
    }

    /** 
     * <p>Invoke a block for each line.</p>
     */
    public RubyIO each_line(IRubyObject[] args) {
        for (IRubyObject line = internalGets(args); !line.isNil(); 
        	line = internalGets(args)) {
            getRuntime().yield(line);
        }
        
        return this;
    }

    public static IRubyObject puts(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            recv.callMethod("write", recv.getRuntime().newString("\n"));
            return recv.getRuntime().getNil();
        }

        for (int i = 0; i < args.length; i++) {
            String line = null;
            if (args[i].isNil()) {
                line = "nil";
            } else if (args[i] instanceof RubyArray) {
                puts(recv, ((RubyArray) args[i]).toJavaArray());
                continue;
            } else {
                line = args[i].toString();
            }
            recv.callMethod("write", recv.getRuntime().newString(line));
            if (!line.endsWith("\n")) {
                recv.callMethod("write", recv.getRuntime().newString("\n"));
            }
        }
        return recv.getRuntime().getNil();
    }

    /** Print some objects to the stream.
     * 
     */
    public static IRubyObject print(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            args = new IRubyObject[] { recv.getRuntime().getLastline()};
        }

        IRubyObject fs = recv.getRuntime().getGlobalVariables().get("$,");
        IRubyObject rs = recv.getRuntime().getGlobalVariables().get("$\\");

        for (int i = 0; i < args.length; i++) {
            if (i > 0 && !fs.isNil()) {
                recv.callMethod("write", fs);
            }
            if (args[i].isNil()) {
                recv.callMethod("write", recv.getRuntime().newString("nil"));
            } else {
                recv.callMethod("write", args[i]);
            }
        }
        if (!rs.isNil()) {
            recv.callMethod("write", rs);
        }

        return recv.getRuntime().getNil();
    }

    public static IRubyObject printf(IRubyObject recv, IRubyObject[] args) {
        recv.callMethod("write", RubyKernel.sprintf(recv, args));
        return recv.getRuntime().getNil();
    }

    public RubyArray readlines(IRubyObject[] args) {
        IRubyObject[] separatorArgument;
        if (args.length > 0) {
            if (!args[0].isKindOf(getRuntime().getClasses().getNilClass()) &&
                !args[0].isKindOf(getRuntime().getClasses().getStringClass())) {
                throw getRuntime().newTypeError(args[0], 
                        getRuntime().getClasses().getStringClass());
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

    public String toString() {
        return "RubyIO(" + modes + ", " + fileno + ")";
    }
    
    // IO Object should always be closed by jruby internally or
    // by a jruby script (their own fault if they forget).  If an IO 
    // object gets GC'd and it has not been closed yet, then we should 
    // clean up.
    protected void finalize() throws Throwable {
        try {
            if (isOpen()) {
                close();
            }
        } finally {
            super.finalize();
        }
    }
    
}
