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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.Channel;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyClass;
import org.jruby.RubyIO;
import org.jruby.RubyKernel;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOHandler;
import org.jruby.util.collections.SinglyLinkedList;

public class IOMetaClass extends ObjectMetaClass {

    public IOMetaClass(IRuby runtime) {
        this("IO", RubyIO.class, runtime.getObject());
    }

    public IOMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
        this(name, RubyIO.class, superClass, parentCRef);
    }
    
    public IOMetaClass(String name, Class clazz, RubyClass superClass) {
    	super(name, clazz, superClass);
    }

    public IOMetaClass(String name, Class clazz, RubyClass superClass, SinglyLinkedList parentCRef) {
    	super(name, clazz, superClass, parentCRef);
    }

    protected class IOMeta extends Meta {
	    protected void initializeClass() {
	        includeModule(getRuntime().getModule("Enumerable"));
	
	        // TODO: Implement tty? and isatty.  We have no real capability to 
	        // determine this from java, but if we could set tty status, then
	        // we could invoke jruby differently to allow stdin to return true
	        // on this.  This would allow things like cgi.rb to work properly.
	
	        defineSingletonMethod("foreach", Arity.optional());
			defineSingletonMethod("read", Arity.optional());
	        defineSingletonMethod("readlines", Arity.optional());
	        defineSingletonMethod("popen", Arity.optional());
            defineSingletonMethod("select", Arity.optional());
	
	        defineMethod("<<", Arity.singleArgument(), "addString");
			defineMethod("binmode", Arity.noArguments());
	        defineMethod("clone", Arity.noArguments(), "clone_IO");
	        defineMethod("close", Arity.noArguments());
	        defineMethod("closed?", Arity.noArguments(), "closed");
	        defineMethod("each", Arity.optional(), "each_line");
	        defineMethod("each_byte", Arity.noArguments());
	        defineMethod("each_line", Arity.optional());
	        defineMethod("eof", Arity.noArguments());
	        defineAlias("eof?", "eof");
	        defineMethod("fcntl", Arity.twoArguments());
	        defineMethod("fileno", Arity.noArguments());
	        defineMethod("flush", Arity.noArguments());
	        defineMethod("fsync", Arity.noArguments());
	        defineMethod("getc", Arity.noArguments());
	        defineMethod("gets", Arity.optional());
	        defineMethod("initialize", Arity.optional());
	        defineMethod("lineno", Arity.noArguments());
	        defineMethod("lineno=", Arity.singleArgument(), "lineno_set");
	        defineMethod("pid", Arity.noArguments());
	        defineMethod("pos", Arity.noArguments());
	        defineMethod("pos=", Arity.singleArgument(), "pos_set");
	        defineMethod("print", Arity.optional());
	        defineMethod("printf", Arity.optional());
	        defineMethod("putc", Arity.singleArgument());
	        defineMethod("puts", Arity.optional());
	        defineMethod("read", Arity.optional());
	        defineMethod("readchar", Arity.noArguments());
	        defineMethod("readline", Arity.optional());
	        defineMethod("readlines", Arity.optional());
	        defineMethod("reopen", Arity.optional());
	        defineMethod("rewind", Arity.noArguments());        
	        defineMethod("seek", Arity.optional());
	        defineMethod("sync", Arity.noArguments());
	        defineMethod("sync=", Arity.singleArgument(), "sync_set");
	        defineMethod("sysread", Arity.singleArgument());
	        defineMethod("syswrite", Arity.singleArgument());
	        defineAlias("tell", "pos");
	        defineAlias("to_i", "fileno");
	        defineMethod("to_io", Arity.noArguments());
	        defineMethod("ungetc", Arity.singleArgument());
	        defineMethod("write", Arity.singleArgument());
            defineMethod("tty?", Arity.noArguments(), "tty");
            defineAlias("isatty", "tty?");
	        
	        // Constants for seek
	        setConstant("SEEK_SET", getRuntime().newFixnum(IOHandler.SEEK_SET));
	        setConstant("SEEK_CUR", getRuntime().newFixnum(IOHandler.SEEK_CUR));
	        setConstant("SEEK_END", getRuntime().newFixnum(IOHandler.SEEK_END));
	    }
    };
    
    protected Meta getMeta() {
    	return new IOMeta();
    }

    public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
        return new IOMetaClass(name, this, parentCRef);
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
        RubyIO io = (RubyIO) ((FileMetaClass) getRuntime().getClass("File")).open(new IRubyObject[] { filename }, false);

        if (!io.isNil() && io.isOpen()) {
        	try {
	            IRubyObject[] newArgs = new IRubyObject[count - 1];
	            System.arraycopy(args, 1, newArgs, 0, count - 1);
	
	            IRubyObject nextLine = io.internalGets(newArgs);
	            while (!nextLine.isNil()) {
	                getRuntime().getCurrentContext().yield(nextLine);
	                nextLine = io.internalGets(newArgs);
	            }
        	} finally {
        		io.close();
        	}
        }
        
        return getRuntime().getNil();
    }

    private static void registerSelect(Selector selector, IRubyObject obj, int ops) throws IOException {
        RubyIO ioObj;

        if(!(obj instanceof RubyIO)) {
            // invoke to_io
            if(!obj.respondsTo("to_io")) {
                return;
            }
            ioObj = (RubyIO) obj.callMethod("to_io");
        } else {
            ioObj = (RubyIO) obj;
        }

        Channel channel = ioObj.getChannel();
        if(channel == null || !(channel instanceof SelectableChannel)) {
            return;
        }

        ((SelectableChannel) channel).configureBlocking(false);
        int real_ops = ((SelectableChannel) channel).validOps() & ops;
        SelectionKey key = ((SelectableChannel) channel).keyFor(selector);

        if(key == null) {
            ((SelectableChannel) channel).register(selector, real_ops, obj);
        } else {
            key.interestOps(key.interestOps()|real_ops);
        }
    }

    public IRubyObject select(IRubyObject[] args) {
        return select_static(getRuntime(), args);
    }

    public static IRubyObject select_static(IRuby runtime, IRubyObject[] args) {
        try {
        	boolean atLeastOneDescriptor = false;
        	
            Selector selector = Selector.open();
            if (!args[0].isNil()) {
            	atLeastOneDescriptor = true;
            	
                // read
                for (Iterator i = ((RubyArray) args[0]).getList().iterator(); i.hasNext(); ) {
                    IRubyObject obj = (IRubyObject) i.next();
                    registerSelect(selector, obj, SelectionKey.OP_READ|SelectionKey.OP_ACCEPT|SelectionKey.OP_CONNECT);
                }
            }
            if (args.length > 1 && !args[1].isNil()) {
            	atLeastOneDescriptor = true;
                // write
                for (Iterator i = ((RubyArray) args[0]).getList().iterator(); i.hasNext(); ) {
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

            if(args.length > 3) {
                selector.select(timeout);
            } else {
                selector.select();
            }
            
            List r = new ArrayList();
            List w = new ArrayList();
            List e = new ArrayList();
            for (Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                SelectionKey key = (SelectionKey) i.next();
                if ((key.interestOps() & key.readyOps()
                    & (SelectionKey.OP_READ|SelectionKey.OP_ACCEPT|SelectionKey.OP_CONNECT)) != 0) {
                    r.add(key.attachment());
                }
                if ((key.interestOps() & key.readyOps() & (SelectionKey.OP_WRITE)) != 0) {
                    w.add(key.attachment());
                }
            }
            List ret = new ArrayList();
            ret.add(RubyArray.newArray(runtime, r));
            ret.add(RubyArray.newArray(runtime, w));
            ret.add(RubyArray.newArray(runtime, e));
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
            return RubyArray.newArray(runtime, ret);
        } catch(IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
    }

    public IRubyObject read(IRubyObject[] args) {
        checkArgumentCount(args, 1, 3);
        IRubyObject[] fileArguments = new IRubyObject[] {args[0]};
        RubyIO file = (RubyIO) RubyKernel.open(this, fileArguments);
        IRubyObject[] readArguments;
		
        if (args.length >= 2) {
            readArguments = new IRubyObject[] {args[1].convertToType("Fixnum", "to_int", true)};
        } else {
            readArguments = new IRubyObject[] {};
        }
		
        try {

            if (args.length == 3) {
                file.seek(new IRubyObject[] {args[2].convertToType("Fixnum", "to_int", true)});				
            }
			
			return file.read(readArguments);
        } finally {
            file.close();
        }
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
    	IRuby runtime = getRuntime();
    	checkArgumentCount(args, 1, 2);
    	IRubyObject cmdObj = args[0].convertToString();
    	cmdObj.checkSafeString();
    	String command = cmdObj.toString();

    	// only r works so throw error if anything else specified.
        if (args.length >= 2) {
            String mode = args[1].convertToString().toString();
            if (!mode.equals("r")) {
                throw runtime.newNotImplementedError("only 'r' currently supported");
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
	    	
	    	if (runtime.getCurrentContext().isBlockGiven()) {
		        try {
		        	runtime.getCurrentContext().yield(io);
	    	        return runtime.getNil();
		        } finally {
		            io.close();
		            runtime.getGlobalVariables().set("$?", runtime.newFixnum(process.waitFor() * 256));
		        }
		    }
	    	return io;
    	} catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
        	throw runtime.newThreadError("unexpected interrupt");
        }
    }
}
