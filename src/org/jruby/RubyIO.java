/*
 * RubyIO.java - No description
 * Created on 12.01.2002, 19:14:58
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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

import java.io.*;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.*;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyIO extends RubyObject {
    protected RubyInputStream inStream = null;
    protected OutputStream outStream = null;

    protected boolean sync = false;

    protected boolean readable = false;
    protected boolean writeable = false;
    protected boolean append = false;

    protected int lineNumber = 0;

    protected String path;

    public RubyIO(Ruby ruby) {
        super(ruby, ruby.getClasses().getIoClass());
    }

    public RubyIO(Ruby ruby, RubyClass type) {
        super(ruby, type);
    }

    public static RubyClass createIOClass(Ruby ruby) {
        RubyClass ioClass = ruby.defineClass("IO", ruby.getClasses().getObjectClass());
        ioClass.includeModule(ruby.getClasses().getEnumerableModule());

        ioClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyIO.class, "newInstance"));
        ioClass.defineSingletonMethod("foreach", CallbackFactory.getOptSingletonMethod(RubyIO.class, "foreach", RubyString.class));
        ioClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyIO.class, "initialize"));

        ioClass.defineMethod("write", CallbackFactory.getMethod(RubyIO.class, "write", IRubyObject.class));

        ioClass.defineMethod("<<", CallbackFactory.getMethod(RubyIO.class, "addString", IRubyObject.class));

        ioClass.defineMethod("each", CallbackFactory.getOptMethod(RubyIO.class, "each_line"));
        ioClass.defineMethod("each_line", CallbackFactory.getOptMethod(RubyIO.class, "each_line"));
        ioClass.defineMethod("each_byte", CallbackFactory.getMethod(RubyIO.class, "each_byte"));
        ioClass.defineMethod("getc", CallbackFactory.getMethod(RubyIO.class, "getc"));
        ioClass.defineMethod("readchar", CallbackFactory.getMethod(RubyIO.class, "readchar"));
        ioClass.defineMethod("gets", CallbackFactory.getOptMethod(RubyIO.class, "gets"));
        ioClass.defineMethod("readline", CallbackFactory.getOptMethod(RubyIO.class, "readline"));

        ioClass.defineMethod("lineno", CallbackFactory.getMethod(RubyIO.class, "lineno"));
        ioClass.defineMethod("lineno=", CallbackFactory.getMethod(RubyIO.class, "lineno_set", RubyFixnum.class));
        ioClass.defineMethod("sync", CallbackFactory.getMethod(RubyIO.class, "sync"));
        ioClass.defineMethod("sync=", CallbackFactory.getMethod(RubyIO.class, "sync_set", RubyBoolean.class));

        ioClass.defineMethod("close", CallbackFactory.getMethod(RubyIO.class, "close"));
        ioClass.defineMethod("eof?", CallbackFactory.getMethod(RubyIO.class, "eof"));
        ioClass.defineMethod("flush", CallbackFactory.getMethod(RubyIO.class, "flush"));

        ioClass.defineMethod("print", CallbackFactory.getOptSingletonMethod(RubyIO.class, "print"));
        ioClass.defineMethod("printf", CallbackFactory.getOptSingletonMethod(RubyIO.class, "printf"));
        ioClass.defineMethod("puts", CallbackFactory.getOptSingletonMethod(RubyIO.class, "puts"));

        ioClass.defineMethod("readlines", CallbackFactory.getOptMethod(RubyIO.class, "readlines"));
        ioClass.defineSingletonMethod("readlines", CallbackFactory.getOptSingletonMethod(RubyIO.class, "readlines"));

        return ioClass;
    }

    public static IRubyObject stdin(Ruby ruby, RubyClass rubyClass, InputStream inStream) {
        RubyIO io = new RubyIO(ruby, rubyClass);

        io.inStream = new RubyInputStream(inStream);
        io.readable = true;

        return io;
    }

    public static IRubyObject stdout(Ruby ruby, RubyClass rubyClass, OutputStream outStream) {
        RubyIO io = new RubyIO(ruby, rubyClass);

        io.outStream = outStream;
        io.writeable = true;

        return io;
    }

    public static IRubyObject stderr(Ruby ruby, RubyClass rubyClass, OutputStream errStream) {
        RubyIO io = new RubyIO(ruby, rubyClass);

        io.outStream = errStream;
        io.writeable = true;

        return io;
    }

    protected void checkWriteable() {
        if (!writeable || outStream == null) {
            throw new IOError(getRuntime(), "not opened for writing");
        }
    }

    protected void checkReadable() {
        if (!readable || inStream == null) {
            throw new IOError(getRuntime(), "not opened for reading");
        }
    }

    protected boolean isReadable() {
        return readable;
    }

    protected boolean isWriteable() {
        return writeable;
    }
    
    public OutputStream getOutStream() {
        return outStream;
    }

    public InputStream getInStream() {
        return inStream;
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
    public IRubyObject callWrite(IRubyObject anObject) {
        return callMethod("write", anObject);
    }

    /** rb_io_mode_flags
     * 
     */
    protected void setMode(String mode) {
        readable = false;
        writeable = false;
        append = false;

        if (mode.length() == 0) {
            throw new ArgumentError(getRuntime(), "illegal access mode");
        }

        switch (mode.charAt(0)) {
            case 'r' :
                readable = true;
                break;
            case 'a' :
                append = true;
            case 'w' :
                writeable = true;
                break;
            default :
                throw new ArgumentError(getRuntime(), "illegal access mode " + mode);
        }

        if (mode.length() > 1) {
            int i = mode.charAt(1) == 'b' ? 2 : 1;

            if (mode.length() > i) {
                if (mode.charAt(i) == '+') {
                    readable = true;
                    writeable = true;
                } else {
                    throw new ArgumentError(getRuntime(), "illegal access mode " + mode);
                }
            }
        }
    }

    /** rb_fdopen
     * 
     */
    protected void fdOpen(int fd) {
        switch (fd) {
            case 0 :
                inStream = new RubyInputStream(getRuntime().getInputStream());
                break;
            case 1 :
                outStream = getRuntime().getOutputStream();
                break;
            case 2 :
                outStream = getRuntime().getErrorStream();
                break;
            default :
                throw new IOError(getRuntime(), "Bad file descriptor");
        }
    }

    /** Read a line.
     * 
     */
    public RubyString internalGets(IRubyObject[] args) {
        checkReadable();

        IRubyObject sepVal = getRuntime().getGlobalVariables().get("$/");

        if (args.length > 0) {
            sepVal = args[0];
        }

        String separator = sepVal.isNil() ? null : ((RubyString) sepVal).getValue();

        if (separator == null) {
        } else if (separator.length() == 0) {
            separator = "\n\n";
        }

        try {
            String newLine = inStream.gets(separator);

            if (newLine != null) {
                lineNumber++;

                // XXX
                runtime.getGlobalVariables().set("$.", RubyFixnum.newFixnum(runtime, lineNumber));
                // XXX

                RubyString result = RubyString.newString(getRuntime(), newLine);
                result.taint();

                return result;
            }
        } catch (IOException ioExcptn) {
        }

        return RubyString.nilString(getRuntime());
    }

    public void initIO(RubyInputStream inStream, OutputStream outStream, String path) {
        readable = inStream != null;
        writeable = outStream != null;

        this.inStream = inStream;
        this.outStream = outStream;

        this.path = path;
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
    public static IRubyObject foreach(IRubyObject recv, RubyString filename, IRubyObject[] args) {
        filename.checkSafeString();

        RubyIO io = (RubyIO) KernelModule.open(recv, new IRubyObject[] { filename });

        if (!io.isNil()) {
            try {
                io.callMethod("each", args);
            } finally {
                io.callMethod("close");
            }
        }
        return recv.getRuntime().getNil();
    }

    /** rb_io_initialize
     * 
     */
    public IRubyObject initialize(IRubyObject[] args) {
        closeStreams();

        String mode = "r";

        if (args.length > 1) {
            mode = ((RubyString) args[1]).getValue();
        }

        setMode(mode);

        fdOpen(RubyFixnum.fix2int(args[0]));

        return this;
    }

    /** io_write
     * 
     */
    public IRubyObject write(IRubyObject obj) {
        getRuntime().secure(4);

        String str = obj.toString();

        if (str.length() == 0) {
            return RubyFixnum.zero(getRuntime());
        }

        checkWriteable();

        try {
            outStream.write(str.getBytes());

            if (sync) {
                outStream.flush();
            }
        } catch (IOException ioExcptn) {
            throw new IOError(getRuntime(), ioExcptn.getMessage());
        }

        return RubyFixnum.newFixnum(runtime, str.length());
    }

    /** rb_io_addstr
     * 
     */
    public IRubyObject addString(IRubyObject anObject) {
        callWrite(anObject);

        return this;
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
    public IRubyObject lineno_set(RubyFixnum newLineNumber) {
        lineNumber = RubyFixnum.fix2int(newLineNumber);

        return this;
    }

    /** Returns the current sync mode.
     * 
     * @return the current sync mode.
     */
    public RubyBoolean sync() {
        return RubyBoolean.newBoolean(getRuntime(), sync);
    }

    /** Sets the current sync mode.
     * 
     * @param newSync The new sync mode.
     */
    public IRubyObject sync_set(RubyBoolean newSync) {
        sync = newSync.isTrue();

        return this;
    }

    public RubyBoolean eof() {
        checkReadable();

        try {
            int c = inStream.read();

            if (c == -1) {
                return runtime.getTrue();
            } else {
                inStream.unread(c);
                return runtime.getFalse();
            }
        } catch (IOException ioExcptn) {
            throw new IOError(runtime, ioExcptn.getMessage());
        }
    }

    /** Closes the IO.
     * 
     * @return The IO.
     */
    public IRubyObject close() {
        closeStreams();

        return this;
    }

    /** Flushes the IO output stream.
     * 
     * @return The IO.
     */
    public RubyIO flush() {
        checkWriteable();

        try {
            outStream.flush();
        } catch (IOException ioExcptn) {
            throw new IOError(runtime, ioExcptn.getMessage());
        }

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
        } else {
            return line;
        }
    }

    /** Read a byte. On EOF returns nil.
     * 
     */
    public IRubyObject getc() {
        checkReadable();

        try {
            int c = inStream.read();
            return c != -1 ? RubyFixnum.newFixnum(runtime, c & 0xff) : runtime.getNil();
        } catch (IOException ioExcptn) {
            throw new IOError(runtime, ioExcptn.getMessage());
        }
    }

    /** Read a byte. On EOF throw EOFError.
     * 
     */
    public IRubyObject readchar() {
        IRubyObject obj = getc();

        if (obj.isNil()) {
            throw new EOFError(runtime);
        } else {
            return obj;
        }
    }

    /** Invoke a block for each byte.
     * 
     */
    public IRubyObject each_byte() {
        checkReadable();

        int c;

        try {
            while ((c = inStream.read()) != -1) {
                runtime.yield(RubyFixnum.newFixnum(runtime, c & 0xff));
            }
        } catch (IOException ioExcptn) {
            throw new IOError(runtime, ioExcptn.getMessage());
        }

        return runtime.getNil();
    }

    /** Invoke a block for each line.
     * 
     */
    public RubyIO each_line(IRubyObject[] args) {
        RubyString nextLine = internalGets(args);

        while (!nextLine.isNil()) {
            getRuntime().yield(nextLine);
            nextLine = internalGets(args);
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
            if (! args[0].isKindOf(runtime.getClasses().getStringClass())) {
                throw new TypeError(runtime, args[0], runtime.getClasses().getStringClass());
            }
            separatorArgument = new IRubyObject[] { args[0] };
        } else {
            separatorArgument = new IRubyObject[0];
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
            throw new TypeError(recv.getRuntime(), args[0], recv.getRuntime().getClasses().getStringClass());
        }
        RubyString fileName = (RubyString) args[0];

        IRubyObject[] separatorArguments;
        if (args.length >= 2) {
            separatorArguments = new IRubyObject[] { args[1] };
        } else {
            separatorArguments = new IRubyObject[0];
        }

        RubyIO file = (RubyIO) KernelModule.open(recv, new IRubyObject[] { fileName });

        return file.readlines(separatorArguments);
    }
}
