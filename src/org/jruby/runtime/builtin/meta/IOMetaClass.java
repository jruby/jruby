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
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.runtime.builtin.meta;

import java.io.IOException;

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
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.NotImplementedError;
import org.jruby.exceptions.ThreadError;
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
        defineSingletonMethod("popen", Arity.optional(), "popen");

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
        	try {
	            IRubyObject[] newArgs = new IRubyObject[count - 1];
	            System.arraycopy(args, 1, newArgs, 0, count - 1);
	
	            IRubyObject nextLine = io.internalGets(newArgs);
	            while (!nextLine.isNil()) {
	                getRuntime().yield(nextLine);
	                nextLine = io.internalGets(newArgs);
	            }
        	} finally {
        		io.close();
        	}
        }
        
        return getRuntime().getNil();
    }

    public RubyArray readlines(IRubyObject[] args) {
        int count = checkArgumentCount(args, 1, 2);

        IRubyObject[] fileArguments = new IRubyObject[] {args[0]};
        IRubyObject[] separatorArguments = count >= 2 ? new IRubyObject[]{args[1]} : IRubyObject.NULL_ARRAY;
        RubyIO file = (RubyIO) RubyKernel.open(this, fileArguments);
        try {
        	return file.readlines(separatorArguments);
        } finally {
        	file.close();
        }
    }
    
    //XXX Hacked incomplete popen implementation to make
    public IRubyObject popen(IRubyObject[] args) {
    	Ruby runtime = getRuntime();
    	checkArgumentCount(args, 1, 2);
    	IRubyObject cmdObj = args[0].convertToString();
    	cmdObj.checkSafeString();
    	String command = cmdObj.toString();

    	// only r works so throw error if anything else specified.
        if (args.length >= 2) {
            String mode = args[1].convertToString().toString();
            if (!mode.equals("r")) {
                throw new NotImplementedError(runtime, "only 'r' currently supported");
            }
        }
    	
    	try {
    		//TODO Unify with runInShell()
	    	Process process;
	    	String shell = System.getProperty("jruby.shell");
	        if (shell != null) {
	            String shellSwitch = "-c";
	            if (!shell.endsWith("sh")) {
	                shellSwitch = "/c";
	            }
	            process = Runtime.getRuntime().exec(new String[] { shell, shellSwitch, command });
	        } else {
	            process = Runtime.getRuntime().exec(command);
	        }
	    	
	    	RubyIO io = new RubyIO(runtime, process);
	    	
	    	if (runtime.isBlockGiven()) {
		        try {
		        	runtime.yield(io);
	    	        return runtime.getNil();
		        } finally {
		            io.close();
		            runtime.getGlobalVariables().set("$?", runtime.newFixnum(process.waitFor() * 256));
		        }
		    }
	    	return io;
    	} catch (IOException e) {
            throw IOError.fromException(runtime, e);
        } catch (InterruptedException e) {
        	throw new ThreadError(runtime, "unexpected interrupt");
        }
    }
}
