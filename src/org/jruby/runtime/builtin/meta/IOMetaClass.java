/*
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.runtime.builtin.meta;

import org.jruby.BuiltinClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandler;

public class IOMetaClass extends BuiltinClass {

    public IOMetaClass(Ruby runtime) {
        super("IO", RubyIO.class, runtime.getClasses().getObjectClass());
    }

    public IOMetaClass(String name, RubyClass superClass, RubyModule parentModule) {
        super(name, RubyIO.class, superClass, parentModule);
    }

    protected void initializeClass() {
        includeModule(getRuntime().getClasses().getEnumerableModule());

        // TODO: Implement tty? and isatty.  We have no real capability to 
        // determine this from java, but if we could set tty status, then
        // we could invoke jruby differently to allow stdin to return true
        // on this.  This would allow things like cgi.rb to work properly.

        defineSingletonMethod("foreach", Arity.optional(), "foreach");
        defineSingletonMethod("readlines", Arity.optional(), "readlines");

        CallbackFactory callbackFactory = getRuntime().callbackFactory(RubyIO.class);
        defineMethod("<<", callbackFactory.getMethod("addString", IRubyObject.class));
        defineMethod("clone", callbackFactory.getMethod("clone_IO"));
        defineMethod("close", callbackFactory.getMethod("close"));
        defineMethod("closed?", callbackFactory.getMethod("closed"));
        defineMethod("each", callbackFactory.getOptMethod("each_line"));
        defineMethod("each_byte", callbackFactory.getMethod("each_byte"));
        defineMethod("each_line", callbackFactory.getOptMethod("each_line"));
        defineMethod("eof", callbackFactory.getMethod("eof"));
        defineMethod("eof?", callbackFactory.getMethod("eof"));
        defineMethod("fileno", callbackFactory.getMethod("fileno"));
        defineMethod("flush", callbackFactory.getMethod("flush"));
        defineMethod("fsync", callbackFactory.getMethod("fsync"));
        defineMethod("getc", callbackFactory.getMethod("getc"));
        defineMethod("gets", callbackFactory.getOptMethod("gets"));
        defineMethod("initialize", Arity.optional(), "initialize");
        defineMethod("lineno", callbackFactory.getMethod("lineno"));
        defineMethod("lineno=", callbackFactory.getMethod("lineno_set", RubyFixnum.class));
        defineMethod("pid", callbackFactory.getMethod("pid"));
        defineMethod("pos", callbackFactory.getMethod("pos"));
        defineMethod("pos=", callbackFactory.getMethod("pos_set", RubyFixnum.class));
        defineMethod("print", callbackFactory.getOptSingletonMethod("print"));
        defineMethod("printf", callbackFactory.getOptSingletonMethod("printf"));
        defineMethod("putc", callbackFactory.getMethod("putc", IRubyObject.class));
        defineMethod("puts", callbackFactory.getOptSingletonMethod("puts"));
        defineMethod("read", callbackFactory.getOptMethod("read"));
        defineMethod("readchar", callbackFactory.getMethod("readchar"));
        defineMethod("readline", callbackFactory.getOptMethod("readline"));
        defineMethod("readlines", callbackFactory.getOptMethod("readlines"));
        defineMethod("reopen", callbackFactory.getOptMethod("reopen", IRubyObject.class));
        defineMethod("rewind", callbackFactory.getMethod("rewind"));        
        defineMethod("seek", callbackFactory.getOptMethod("seek"));
        defineMethod("sync", callbackFactory.getMethod("sync"));
        defineMethod("sync=", callbackFactory.getMethod("sync_set", RubyBoolean.class));
        defineMethod("sysread", callbackFactory.getMethod("sysread", RubyFixnum.class));
        defineMethod("syswrite", callbackFactory.getMethod("syswrite", IRubyObject.class));
        defineMethod("tell", callbackFactory.getMethod("pos"));
        defineMethod("to_i", callbackFactory.getMethod("fileno"));
        defineMethod("ungetc", callbackFactory.getMethod("ungetc", RubyFixnum.class));
        defineMethod("write", callbackFactory.getMethod("write", IRubyObject.class));
        
        // Constants for seek
        setConstant("SEEK_SET", getRuntime().newFixnum(IOHandler.SEEK_SET));
        setConstant("SEEK_CUR", getRuntime().newFixnum(IOHandler.SEEK_CUR));
        setConstant("SEEK_END", getRuntime().newFixnum(IOHandler.SEEK_END));
    }

    public RubyClass newSubClass(String name, RubyModule parentModule) {
        return new IOMetaClass(name, this, parentModule);
    }

    public IRubyObject allocateObject() {
        return new RubyIO(getRuntime(), this); 
    }

    /** rb_io_s_foreach
     * 
     */
    public IRubyObject foreach(IRubyObject[] args) {
        int count = checkArgumentCount(args, 1, -1);
        IRubyObject filename = args[0].convertToString();
        filename.checkSafeString();
        RubyIO io = (RubyIO) RubyFile.open(getRuntime().getClasses().getFileClass(), new IRubyObject[] { filename }, false);

        if (!io.isNil() && io.isOpen()) {
            IRubyObject[] newArgs = new IRubyObject[count - 1];
            System.arraycopy(args, 1, newArgs, 0, count - 1);

            IRubyObject nextLine = io.internalGets(newArgs);
            while (!nextLine.isNil()) {
                getRuntime().yield(nextLine);
                nextLine = io.internalGets(newArgs);
            }

            io.close();
        }
        
        return getRuntime().getNil();
    }

    public RubyArray readlines(IRubyObject[] args) {
        int count = checkArgumentCount(args, 1, 2);

        IRubyObject[] fileArguments = new IRubyObject[] {args[0]};
        IRubyObject[] separatorArguments = count >= 2 ? new IRubyObject[]{args[1]} : IRubyObject.NULL_ARRAY;
        RubyIO file = (RubyIO) RubyKernel.open(this, fileArguments);

        return file.readlines(separatorArguments);
    }
}