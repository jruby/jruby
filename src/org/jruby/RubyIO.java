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

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.EOFError;
import org.jruby.exceptions.ErrnoError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.SystemCallError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;
import org.jruby.util.IOHandler;
import org.jruby.util.IOHandlerSeekable;
import org.jruby.util.IOHandlerUnseekable;
import org.jruby.util.IOModes;
import org.jruby.util.IOHandlerProcess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyIO extends RubyObject {
    public final static int STDIN = 0;
    public final static int STDOUT = 1;
    public final static int STDERR = 2;
    
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
        return (IOHandler) (((WeakReference) getRuntime().ioHandlers.get(new Integer(fileno))).get());
    }
    
    protected static int fileno = 2;
    
    public static int getNewFileno() {
        fileno++;
        
        return fileno;
    }

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    protected RubyIO(Ruby ruby, RubyClass type) {
        super(ruby, type);
    }

    public RubyIO(Ruby ruby, OutputStream outputStream) {
        super(ruby, ruby.getClasses().getIoClass());
        
        // We only want IO objects with valid streams (better to error now). 
        if (outputStream == null) {
            throw new IOError(ruby, "Opening invalid stream");
        }
        
        handler = new IOHandlerUnseekable(ruby, null, outputStream);
        modes = handler.getModes();
        
        registerIOHandler(handler);
    }
    
    public RubyIO(Ruby ruby, Process process) {
    	super(ruby, ruby.getClasses().getIoClass());

        modes = new IOModes(ruby, "w+");

    	handler = new IOHandlerProcess(ruby, process, modes);
    	modes = handler.getModes();
    	
    	registerIOHandler(handler);
    }
    
    public RubyIO(Ruby ruby, int descriptor) {
        super(ruby, ruby.getClasses().getIoClass());

        handler = new IOHandlerUnseekable(ruby, descriptor);
        modes = handler.getModes();
        
        registerIOHandler(handler);
    }
    
    public static RubyClass createIOClass(Ruby ruby) {
        RubyClass result = 
            ruby.defineClass("IO", ruby.getClasses().getObjectClass());
        
        result.includeModule(ruby.getClasses().getEnumerableModule());
        
        // TODO: Implement tty? and isatty.  We have no real capability to 
        // determine this from java, but if we could set tty status, then
        // we could invoke jruby differently to allow stdin to return true
        // on this.  This would allow things like cgi.rb to work properly.
        
        CallbackFactory callbackFactory = ruby.callbackFactory();
        result.defineMethod("<<", callbackFactory.getMethod(RubyIO.class, "addString", IRubyObject.class));
        result.defineMethod("clone", callbackFactory.getMethod(RubyIO.class, "clone_IO"));
        result.defineMethod("close", callbackFactory.getMethod(RubyIO.class, "close"));
        result.defineMethod("closed?", callbackFactory.getMethod(RubyIO.class, "closed"));
        result.defineMethod("each", callbackFactory.getOptMethod(RubyIO.class, "each_line"));
        result.defineMethod("each_byte", callbackFactory.getMethod(RubyIO.class, "each_byte"));
        result.defineMethod("each_line", callbackFactory.getOptMethod(RubyIO.class, "each_line"));
        result.defineMethod("eof", callbackFactory.getMethod(RubyIO.class, "eof"));
        result.defineMethod("eof?", callbackFactory.getMethod(RubyIO.class, "eof"));
        result.defineMethod("fileno", callbackFactory.getMethod(RubyIO.class, "fileno"));
        result.defineMethod("flush", callbackFactory.getMethod(RubyIO.class, "flush"));
        result.defineMethod("fsync", callbackFactory.getMethod(RubyIO.class, "fsync"));
        result.defineMethod("getc", callbackFactory.getMethod(RubyIO.class, "getc"));
        result.defineMethod("gets", callbackFactory.getOptMethod(RubyIO.class, "gets"));
        result.defineMethod("initialize", callbackFactory.getOptMethod(RubyIO.class, "initialize", RubyFixnum.class));
        result.defineMethod("lineno", callbackFactory.getMethod(RubyIO.class, "lineno"));
        result.defineMethod("lineno=", callbackFactory.getMethod(RubyIO.class, "lineno_set", RubyFixnum.class));
        result.defineMethod("pid", callbackFactory.getMethod(RubyIO.class, "pid"));
        result.defineMethod("pos", callbackFactory.getMethod(RubyIO.class, "pos"));
        result.defineMethod("pos=", callbackFactory.getMethod(RubyIO.class, "pos_set", RubyFixnum.class));
        result.defineMethod("print", callbackFactory.getOptSingletonMethod(RubyIO.class, "print"));
        result.defineMethod("printf", callbackFactory.getOptSingletonMethod(RubyIO.class, "printf"));
        result.defineMethod("putc", callbackFactory.getMethod(RubyIO.class, "putc", IRubyObject.class));
        result.defineMethod("puts", callbackFactory.getOptSingletonMethod(RubyIO.class, "puts"));
        result.defineMethod("read", callbackFactory.getOptMethod(RubyIO.class, "read"));
        result.defineMethod("readchar", callbackFactory.getMethod(RubyIO.class, "readchar"));
        result.defineMethod("readline", callbackFactory.getOptMethod(RubyIO.class, "readline"));
        result.defineMethod("readlines", callbackFactory.getOptMethod(RubyIO.class, "readlines"));
        result.defineMethod("reopen", callbackFactory.getOptMethod(RubyIO.class, "reopen", IRubyObject.class));
        result.defineMethod("rewind", callbackFactory.getMethod(RubyIO.class, "rewind"));        
        result.defineMethod("seek", callbackFactory.getOptMethod(RubyIO.class, "seek"));
        result.defineMethod("sync", callbackFactory.getMethod(RubyIO.class, "sync"));
        result.defineMethod("sync=", callbackFactory.getMethod(RubyIO.class, "sync_set", RubyBoolean.class));
        result.defineMethod("sysread", callbackFactory.getMethod(RubyIO.class, "sysread", RubyFixnum.class));
        result.defineMethod("syswrite", callbackFactory.getMethod(RubyIO.class, "syswrite", IRubyObject.class));
        result.defineMethod("tell", callbackFactory.getMethod(RubyIO.class, "pos"));
        result.defineMethod("to_i", callbackFactory.getMethod(RubyIO.class, "fileno"));
        result.defineMethod("ungetc", callbackFactory.getMethod(RubyIO.class, "ungetc", RubyFixnum.class));
        result.defineMethod("write", callbackFactory.getMethod(RubyIO.class, "write", IRubyObject.class));

        result.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod(RubyIO.class, "newInstance"));
        result.defineSingletonMethod("foreach", callbackFactory.getOptSingletonMethod(RubyIO.class, "foreach", IRubyObject.class));
        result.defineSingletonMethod("readlines", callbackFactory.getOptSingletonMethod(RubyIO.class, "readlines"));
        
        // Constants for seek
        result.setConstant("SEEK_SET", RubyFixnum.newFixnum(ruby, IOHandler.SEEK_SET));
        result.setConstant("SEEK_CUR", RubyFixnum.newFixnum(ruby, IOHandler.SEEK_CUR));
        result.setConstant("SEEK_END", RubyFixnum.newFixnum(ruby, IOHandler.SEEK_END));
        
        return result;
    }
    
    /**
     * <p>Open a file descriptor, unless it is already open, then return
     * it.</p> 
     */
    public static IRubyObject fdOpen(Ruby ruby, int descriptor) {
        return new RubyIO(ruby, descriptor);
    }

    /*
     * See checkReadable for commentary.
     */
    protected void checkWriteable() {
        if (isOpen() == false || modes.isWriteable() == false) {
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
        if (isOpen() == false || modes.isReadable() == false) {
            throw new IOError(getRuntime(), "not opened for reading");            
        }
    }
    
    protected boolean isOpen() {
        return isOpen;
    }

    public OutputStream getOutStream() {
        return handler.getOutputStream();
    }

    public InputStream getInStream() {
        return handler.getInputStream();
    }
    
    public IRubyObject reopen(IRubyObject arg1, IRubyObject args[]) {
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
                    throw new TypeError(runtime, args[0], 
                            runtime.getClasses().getStringClass());
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
    public RubyString internalGets(IRubyObject[] args) {
        checkReadable();

        IRubyObject sepVal = getRuntime().getGlobalVariables().get("$/");

        if (args.length > 0) {
            sepVal = args[0];
        }

        
        String separator = sepVal.isNil() ? null : ((RubyString) sepVal).getValue();

        if (separator != null && separator.length() == 0) {
            separator = IOHandler.PARAGRAPH_DELIMETER;
        }

        try {
            String newLine = handler.gets(separator);

            if (newLine != null) {
                lineNumber++;
                runtime.getGlobalVariables().set("$.", RubyFixnum.newFixnum(runtime, lineNumber));
                RubyString result = RubyString.newString(getRuntime(), newLine);
                result.taint();
                return result;
            }
        } catch (IOException e) {
            throw IOError.fromException(runtime, e);
        }
        return RubyString.nilString(getRuntime());
    }

    // IO class methods.

    /** rb_io_s_new
     * 
     */
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyIO newObject = new RubyIO(recv.getRuntime(), (RubyClass) recv);
        newObject.callInit(args);
        return newObject;
    }

    /** rb_io_s_foreach
     * 
     */
    public static IRubyObject foreach(IRubyObject recv, IRubyObject filename, IRubyObject[] args) {
        filename.checkSafeString();
        RubyIO io = (RubyIO) RubyFile.open(recv.getRuntime().getClass("File"),
                new IRubyObject[] { filename }, false);
        
        if (!io.isNil() && io.isOpen()) {
            RubyString nextLine = io.internalGets(args);

            while (!nextLine.isNil()) {
                recv.getRuntime().yield(nextLine);
                nextLine = io.internalGets(args);
            }

            io.close();
        }
        
        return recv.getRuntime().getNil();
    }

    /** rb_io_initialize
     * 
     */
    public IRubyObject initialize(RubyFixnum descriptor, IRubyObject[] args) {
        int fileno = RubyFixnum.fix2int(descriptor);
        String mode = null;
        
        if (args.length > 0) {
            mode = ((RubyString) args[0]).getValue();
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
            modes = (mode == null) ? handler.getModes() :
            	new IOModes(getRuntime(), mode);

            // Reset file based on modes.
            handler.reset(modes);
        }
        
        return this;
    }

    public IRubyObject syswrite(IRubyObject obj) {
        return RubyFixnum.newFixnum(runtime, 
                handler.syswrite(obj.toString()));
    }
    
    /** io_write
     * 
     */
    public IRubyObject write(IRubyObject obj) {
        checkWriteable();

        try {
            int totalWritten = handler.write(obj.toString());

            return RubyFixnum.newFixnum(runtime, totalWritten);
        } catch (IOError e) {
            return RubyFixnum.zero(runtime);
        } catch (ErrnoError e) {
            return RubyFixnum.zero(runtime);
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
        return RubyFixnum.newFixnum(getRuntime(), handler.getFileno());
    }
    
    /** Returns the current line number.
     * 
     * @return the current line number.
     */
    public RubyFixnum lineno() {
        return RubyFixnum.newFixnum(getRuntime(), lineNumber);
    }

    /** Sets the current line number.
     * 
     * @param newLineNumber The new line number.
     */
    public RubyFixnum lineno_set(RubyFixnum newLineNumber) {
        lineNumber = RubyFixnum.fix2int(newLineNumber);

        return newLineNumber;
    }

    /** Returns the current sync mode.
     * 
     * @return the current sync mode.
     */
    public RubyBoolean sync() {
        return RubyBoolean.newBoolean(getRuntime(), handler.isSync());
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
        
        return RubyFixnum.newFixnum(getRuntime(), pid);
    }
    
    public RubyFixnum pos() {
        return RubyFixnum.newFixnum(getRuntime(), handler.pos());
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
                throw new TypeError(getRuntime(), 
                        "Cannot convert String to Integer");
            }
        } else if (object.isKindOf(getRuntime().getClasses().getFixnumClass())){
            c = RubyFixnum.fix2int(object);
        } else { // What case will this work for?
            c = RubyFixnum.fix2int(object.callMethod("to_i"));
        }

        try {
            handler.putc(c);
        } catch (ErrnoError e) {
            return RubyFixnum.zero(runtime);
        }
        
        return RubyFixnum.newFixnum(getRuntime(), c);
    }
    
    // This was a getOpt with one mandatory arg, but it did not work
    // so I am parsing it for now.
    public RubyFixnum seek(IRubyObject[] args) {
        if (args == null || args.length == 0) {
            throw new ArgumentError(getRuntime(), "wrong number of arguments");
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
        
        return RubyFixnum.zero(runtime);
    }
    
    public RubyFixnum fsync() {
        checkWriteable();

        try {
            handler.sync();
        } catch (IOException e) {
            throw IOError.fromException(runtime, e);
        }

        return RubyFixnum.zero(runtime);
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
        return isOpen() == true ? getRuntime().getFalse() : 
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
    public RubyString gets(IRubyObject[] args) {
        RubyString result = internalGets(args);

        if (!result.isNil()) {
            getRuntime().setLastline(result);
        }

        return result;
    }

    /** Read a line.
     * 
     */
    public RubyString readline(IRubyObject[] args) {
        RubyString line = gets(args);

        if (line.isNil()) {
            throw new EOFError(runtime);
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
        	RubyFixnum.newFixnum(getRuntime(), c);
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
        String buf = handler.sysread(RubyFixnum.fix2int(number));
        
        return RubyString.newString(getRuntime(), buf);
    }
    
    public IRubyObject read(IRubyObject[] args) {
        String buf = (args.length > 0 ? 
            handler.read(RubyFixnum.fix2int(args[0])) : 
            handler.getsEntireStream());

        return (buf == null ? 
            getRuntime().getNil() : // EOF 
            RubyString.newString(getRuntime(), new String(buf)));
    }

    /** Read a byte. On EOF throw EOFError.
     * 
     */
    public IRubyObject readchar() {
        checkReadable();
        
        int c = handler.getc();
        
        if (c == -1) {
            throw new EOFError(runtime);
        }
        
        return RubyFixnum.newFixnum(getRuntime(), c);
    }

    /** 
     * <p>Invoke a block for each byte.</p>
     */
    public IRubyObject each_byte() {
        for (int c = handler.getc(); c != -1; c = handler.getc()) {
            Asserts.isTrue(c < 256);
            runtime.yield(RubyFixnum.newFixnum(runtime, c));
        }

        return runtime.getNil();
    }

    /** 
     * <p>Invoke a block for each line.</p>
     */
    public RubyIO each_line(IRubyObject[] args) {
        for (RubyString line = internalGets(args); !line.isNil(); 
        	line = internalGets(args)) {
            getRuntime().yield(line);
        }
        
        return this;
    }

    public static IRubyObject puts(IRubyObject recv, IRubyObject args[]) {
        if (args.length == 0) {
            recv.callMethod("write", RubyString.newString(recv.getRuntime(), "\n"));
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
            recv.callMethod("write", RubyString.newString(recv.getRuntime(), line));
            if (!line.endsWith("\n")) {
                recv.callMethod("write", RubyString.newString(recv.getRuntime(), "\n"));
            }
        }
        return recv.getRuntime().getNil();
    }

    /** Print some objects to the stream.
     * 
     */
    public static IRubyObject print(IRubyObject recv, IRubyObject args[]) {
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
                recv.callMethod("write", RubyString.newString(recv.getRuntime(), "nil"));
            } else {
                recv.callMethod("write", args[i]);
            }
        }
        if (!rs.isNil()) {
            recv.callMethod("write", rs);
        }

        return recv.getRuntime().getNil();
    }

    public static IRubyObject printf(IRubyObject recv, IRubyObject args[]) {
        recv.callMethod("write", KernelModule.sprintf(recv, args));
        return recv.getRuntime().getNil();
    }

    public RubyArray readlines(IRubyObject[] args) {
        IRubyObject[] separatorArgument;
        if (args.length > 0) {
            if (!args[0].isKindOf(runtime.getClasses().getNilClass()) &&
                !args[0].isKindOf(runtime.getClasses().getStringClass())) {
                throw new TypeError(runtime, args[0], 
                        runtime.getClasses().getStringClass());
            } 
            separatorArgument = new IRubyObject[] { args[0] };
        } else {
            separatorArgument = IRubyObject.NULL_ARRAY;
        }

        RubyArray result = RubyArray.newArray(runtime);
        IRubyObject line;
        while (! (line = internalGets(separatorArgument)).isNil()) {
            result.append(line);
        }
        return result;
    }

    public static RubyArray readlines(IRubyObject recv, IRubyObject args[]) {
        if (args.length < 1) {
            throw new ArgumentError(recv.getRuntime(), args.length, 1);
        }
        
        if (! args[0].isKindOf(recv.getRuntime().getClasses().getStringClass())) {
            throw new TypeError(recv.getRuntime(), args[0], 
                recv.getRuntime().getClasses().getStringClass());
        }
        
        IRubyObject[] fileArguments = new IRubyObject[] {(RubyString) args[0]};
        IRubyObject[] separatorArguments = (args.length >= 2 ? 
             new IRubyObject[] { args[1] } : IRubyObject.NULL_ARRAY);
        RubyIO file = (RubyIO) KernelModule.open(recv, fileArguments);

        return file.readlines(separatorArguments);
    }
    
    public String toString() {
        return "RubyIO(" + modes + ", " + fileno + ")";
    }
    
    // IO Object should always be closed by jruby internally or
    // by a jruby script (their own fault if they forget).  If an IO 
    // object gets GC'd and it has not been closed yet, then we should 
    // clean up.
    public void finalize() throws Throwable {
        try {
            if (isOpen() == true) {
                close();
            }
        } finally {
            super.finalize();
        }
    }
    
}
