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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Kiel Hodges <jruby-devel@selfsosoft.com>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.FileMetaClass;
import org.jruby.runtime.builtin.meta.StringMetaClass;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.PrintfFormat;

/**
 * Note: For CVS history, see KernelModule.java.
 *
 * @author jpetersen
 */
public class RubyKernel {
    public static RubyModule createKernelModule(IRuby runtime) {
        RubyModule module = runtime.defineModule("Kernel");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyKernel.class);

        module.defineModuleFunction("Array", callbackFactory.getSingletonMethod("new_array", IRubyObject.class));
        module.defineModuleFunction("Float", callbackFactory.getSingletonMethod("new_float", IRubyObject.class));
        module.defineModuleFunction("Integer", callbackFactory.getSingletonMethod("new_integer", IRubyObject.class));
        module.defineModuleFunction("String", callbackFactory.getSingletonMethod("new_string", IRubyObject.class));
        module.defineModuleFunction("`", callbackFactory.getSingletonMethod("backquote", IRubyObject.class));
        // TODO: Implement Kernel#abort
        module.defineModuleFunction("at_exit", callbackFactory.getSingletonMethod("at_exit"));
        module.defineModuleFunction("autoload", callbackFactory.getSingletonMethod("autoload", IRubyObject.class, IRubyObject.class));
        // TODO: Implement Kernel#autoload?
        module.defineModuleFunction("binding", callbackFactory.getSingletonMethod("binding"));
        module.defineModuleFunction("block_given?", callbackFactory.getSingletonMethod("block_given"));
        // TODO: Implement Kernel#callcc
        module.defineModuleFunction("caller", callbackFactory.getOptSingletonMethod("caller"));
        module.defineModuleFunction("catch", callbackFactory.getSingletonMethod("rbCatch", IRubyObject.class));
        module.defineModuleFunction("chomp", callbackFactory.getOptSingletonMethod("chomp"));
        module.defineModuleFunction("chomp!", callbackFactory.getOptSingletonMethod("chomp_bang"));
        module.defineModuleFunction("chop", callbackFactory.getSingletonMethod("chop"));
        module.defineModuleFunction("chop!", callbackFactory.getSingletonMethod("chop_bang"));
        module.defineModuleFunction("eval", callbackFactory.getOptSingletonMethod("eval"));
        // TODO: Implement Kernel#exec
        module.defineModuleFunction("exit", callbackFactory.getOptSingletonMethod("exit"));
        // TODO: Implement Kernel#exit!
        module.defineModuleFunction("fail", callbackFactory.getOptSingletonMethod("raise"));
        // TODO: Implement Kernel#fork
        module.defineModuleFunction("format", callbackFactory.getOptSingletonMethod("sprintf"));
        module.defineModuleFunction("gets", callbackFactory.getOptSingletonMethod("gets"));
        module.defineModuleFunction("global_variables", callbackFactory.getSingletonMethod("global_variables"));
        module.defineModuleFunction("gsub", callbackFactory.getOptSingletonMethod("gsub"));
        module.defineModuleFunction("gsub!", callbackFactory.getOptSingletonMethod("gsub_bang"));
        // TODO: Add deprecation to Kernel#iterator? (maybe formal deprecation mech.)
        module.defineAlias("iterator?", "block_given?");
        module.defineModuleFunction("lambda", callbackFactory.getSingletonMethod("proc"));
        module.defineModuleFunction("load", callbackFactory.getOptSingletonMethod("load"));
        module.defineModuleFunction("local_variables", callbackFactory.getSingletonMethod("local_variables"));
        module.defineModuleFunction("loop", callbackFactory.getSingletonMethod("loop"));
        // Note: method_missing is documented as being in Object, but ruby appears to stick it in Kernel.
        module.defineModuleFunction("method_missing", callbackFactory.getOptSingletonMethod("method_missing"));
        module.defineModuleFunction("open", callbackFactory.getOptSingletonMethod("open"));
        module.defineModuleFunction("p", callbackFactory.getOptSingletonMethod("p"));
        module.defineModuleFunction("print", callbackFactory.getOptSingletonMethod("print"));
        module.defineModuleFunction("printf", callbackFactory.getOptSingletonMethod("printf"));
        module.defineModuleFunction("proc", callbackFactory.getSingletonMethod("proc"));
        // TODO: implement Kernel#putc
        module.defineModuleFunction("puts", callbackFactory.getOptSingletonMethod("puts"));
        module.defineAlias("raise", "fail");
        module.defineModuleFunction("rand", callbackFactory.getOptSingletonMethod("rand"));
        module.defineModuleFunction("readline", callbackFactory.getOptSingletonMethod("readline"));
        module.defineModuleFunction("readlines", callbackFactory.getOptSingletonMethod("readlines"));
        module.defineModuleFunction("require", callbackFactory.getSingletonMethod("require", IRubyObject.class));
        module.defineModuleFunction("scan", callbackFactory.getSingletonMethod("scan", IRubyObject.class));
        // TODO: Implement Kernel#select
        module.defineModuleFunction("set_trace_func", callbackFactory.getSingletonMethod("set_trace_func", IRubyObject.class));
        module.defineModuleFunction("sleep", callbackFactory.getSingletonMethod("sleep", IRubyObject.class));
        module.defineModuleFunction("split", callbackFactory.getOptSingletonMethod("split"));
        module.defineAlias("sprintf", "format");
        module.defineModuleFunction("srand", callbackFactory.getOptSingletonMethod("srand"));
        module.defineModuleFunction("sub", callbackFactory.getOptSingletonMethod("sub"));
        module.defineModuleFunction("sub!", callbackFactory.getOptSingletonMethod("sub_bang"));
        // Skipping: Kernel#syscall (too system dependent)
        module.defineModuleFunction("system", callbackFactory.getOptSingletonMethod("system"));
        // TODO: Implement Kernel#test (partial impl)
        module.defineModuleFunction("throw", callbackFactory.getOptSingletonMethod("rbThrow"));
        // TODO: Implement Kernel#trace_var
        module.defineModuleFunction("trap", callbackFactory.getOptSingletonMethod("trap"));
        // TODO: Implement Kernel#untrace_var
        // TODO: Implement Kernel#warn
        
        // Defined p411 Pickaxe 2nd ed.
        module.defineModuleFunction("singleton_method_added", callbackFactory.getSingletonMethod("singleton_method_added", IRubyObject.class));

        return module;
    }

    public static IRubyObject at_exit(IRubyObject recv) {
        return recv.getRuntime().pushExitBlock(recv.getRuntime().newProc());
    }

    public static IRubyObject autoload(IRubyObject recv, IRubyObject symbol, final IRubyObject file) {
        final LoadService loadService = recv.getRuntime().getLoadService();
        loadService.addAutoload(symbol.asSymbol(), new IAutoloadMethod() {
            /**
             * @see org.jruby.runtime.load.IAutoloadMethod#load(IRuby, String)
             */
            public IRubyObject load(IRuby runtime, String name) {
                loadService.require(file.toString());
                return runtime.getObject().getConstant(name);
            }
        });
        return recv;
    }

    public static IRubyObject method_missing(IRubyObject recv, IRubyObject[] args) {
        IRuby runtime = recv.getRuntime();
        if (args.length == 0) {
            throw recv.getRuntime().newArgumentError("no id given");
        }

        String name = args[0].asSymbol();
        String description = recv.callMethod("inspect").toString();
        boolean noClass = description.length() > 0 && description.charAt(0) == '#';
        Visibility lastVis = runtime.getCurrentContext().getLastVisibility();
        CallType lastCallType = runtime.getCurrentContext().getLastCallType();
        String format = lastVis.errorMessageFormat(lastCallType, name);
        String msg = new PrintfFormat(format).sprintf(new Object[] { name, description, 
            noClass ? "" : ":", noClass ? "" : recv.getType().getName()});

        throw lastCallType == CallType.VARIABLE ? runtime.newNameError(msg) : runtime.newNoMethodError(msg);
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args) {
        String arg = args[0].convertToString().getValue();

        // Should this logic be pushed into RubyIO Somewhere?
        if (arg.startsWith("|")) {
            String command = arg.substring(1);
        	// exec process, create IO with process
        	try {
                // TODO: may need to part cli parms out ourself?
                Process p = Runtime.getRuntime().exec(command);
                RubyIO io = new RubyIO(recv.getRuntime(), p);
        		
        	    if (recv.getRuntime().getCurrentContext().isBlockGiven()) {
        	        try {
        	            recv.getRuntime().getCurrentContext().yield(io);
        	            
            	        return recv.getRuntime().getNil();
        	        } finally {
        	            io.close();
        	        }
        	    }

                return io;
        	} catch (IOException ioe) {
        		throw recv.getRuntime().newIOErrorFromException(ioe);
        	}
        } 

        return ((FileMetaClass) recv.getRuntime().getClass("File")).open(args);
    }

    public static IRubyObject gets(IRubyObject recv, IRubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) recv.getRuntime().getGlobalVariables().get("$<");

        IRubyObject line = argsFile.internalGets(args);

        recv.getRuntime().getCurrentContext().setLastline(line);

        return line;
    }

    public static IRubyObject new_array(IRubyObject recv, IRubyObject object) {
        return object.callMethod("to_a");
    }
    
    public static IRubyObject new_float(IRubyObject recv, IRubyObject object) {
        return object.callMethod("to_f");
    }
    
    public static IRubyObject new_integer(IRubyObject recv, IRubyObject object) {
        return object.callMethod("to_i");
    }
    
    public static IRubyObject new_string(IRubyObject recv, IRubyObject object) {
        return object.callMethod("to_s");
    }
    
    
    public static IRubyObject p(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                defout.callMethod("write", args[i].callMethod("inspect"));
                defout.callMethod("write", recv.getRuntime().newString("\n"));
            }
        }
        return recv.getRuntime().getNil();
    }

    public static IRubyObject puts(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        // TODO: $> requires 'write' to be defined, but we implement via 'print' method. Resolve.
        RubyIO io = (RubyIO) defout.convertToType("IO", "to_io", true); 

        io.puts(args);

        return recv.getRuntime().getNil();
    }

    public static IRubyObject print(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        // TODO: $> requires 'write' to be defined, but we implement via 'print' method. Resolve.
        RubyIO io = (RubyIO) defout.convertToType("IO", "to_io", true); 

        io.print(args);

        return recv.getRuntime().getNil();
    }

    public static IRubyObject printf(IRubyObject recv, IRubyObject[] args) {
        if (args.length != 0) {
            IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");

            if (!(args[0] instanceof RubyString)) {
            	defout = args[0];
            	args = ArgsUtil.popArray(args);
            }

            defout.callMethod("write", RubyKernel.sprintf(recv, args));
        }

        return recv.getRuntime().getNil();
    }

    public static IRubyObject readline(IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(recv, args);

        if (line.isNil()) {
            throw recv.getRuntime().newEOFError();
        }

        return line;
    }

    public static RubyArray readlines(IRubyObject recv, IRubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) recv.getRuntime().getGlobalVariables().get("$<");

        RubyArray lines = recv.getRuntime().newArray();

        IRubyObject line = argsFile.internalGets(args);
        while (!line.isNil()) {
            lines.append(line);

            line = argsFile.internalGets(args);
        }

        return lines;
    }

    /** Returns value of $_.
     *
     * @throws TypeError if $_ is not a String or nil.
     * @return value of $_ as String.
     */
    private static RubyString getLastlineString(IRuby runtime) {
        IRubyObject line = runtime.getCurrentContext().getLastline();

        if (line.isNil()) {
            throw runtime.newTypeError("$_ value need to be String (nil given).");
        } else if (!(line instanceof RubyString)) {
            throw runtime.newTypeError("$_ value need to be String (" + line.getMetaClass().getName() + " given).");
        } else {
            return (RubyString) line;
        }
    }

    public static IRubyObject sub_bang(IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(recv.getRuntime()).sub_bang(args);
    }

    public static IRubyObject sub(IRubyObject recv, IRubyObject[] args) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.sub_bang(args).isNil()) {
            recv.getRuntime().getCurrentContext().setLastline(str);
        }

        return str;
    }

    public static IRubyObject gsub_bang(IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(recv.getRuntime()).gsub_bang(args);
    }

    public static IRubyObject gsub(IRubyObject recv, IRubyObject[] args) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.gsub_bang(args).isNil()) {
            recv.getRuntime().getCurrentContext().setLastline(str);
        }

        return str;
    }

    public static IRubyObject chop_bang(IRubyObject recv) {
        return getLastlineString(recv.getRuntime()).chop_bang();
    }

    public static IRubyObject chop(IRubyObject recv) {
        RubyString str = getLastlineString(recv.getRuntime());

        if (str.getValue().length() > 0) {
            str = (RubyString) str.dup();
            str.chop_bang();
            recv.getRuntime().getCurrentContext().setLastline(str);
        }

        return str;
    }

    public static IRubyObject chomp_bang(IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(recv.getRuntime()).chomp_bang(args);
    }

    public static IRubyObject chomp(IRubyObject recv, IRubyObject[] args) {
        RubyString str = getLastlineString(recv.getRuntime());
        RubyString dup = (RubyString) str.dup();

        if (dup.chomp_bang(args).isNil()) {
            return str;
        } 

        recv.getRuntime().getCurrentContext().setLastline(dup);
        return dup;
    }

    public static IRubyObject split(IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(recv.getRuntime()).split(args);
    }

    public static IRubyObject scan(IRubyObject recv, IRubyObject pattern) {
        return getLastlineString(recv.getRuntime()).scan(pattern);
    }

    public static IRubyObject sleep(IRubyObject recv, IRubyObject seconds) {
    	long milliseconds = (long) (seconds.convertToFloat().getDoubleValue() * 1000);
    	long startTime = System.currentTimeMillis();
    	
    	RubyThread rubyThread = recv.getRuntime().getThreadService().getCurrentContext().getThread();
        try {
        	rubyThread.sleep(milliseconds);
        } catch (InterruptedException iExcptn) {
        }

        return recv.getRuntime().newFixnum(
        		Math.round((System.currentTimeMillis() - startTime) / 1000.0));
    }

    public static IRubyObject exit(IRubyObject recv, IRubyObject[] args) {
        recv.getRuntime().secure(4);

        int status = 1;
        if (args.length > 0) {
            RubyObject argument = (RubyObject)args[0];
            if (argument instanceof RubyFixnum) {
                status = RubyNumeric.fix2int(argument);
            } else {
                status = argument.isFalse() ? 1 : 0;
            }
        }

        throw recv.getRuntime().newSystemExit(status);
    }


    /** Returns an Array with the names of all global variables.
     *
     */
    public static RubyArray global_variables(IRubyObject recv) {
        RubyArray globalVariables = recv.getRuntime().newArray();

        Iterator iter = recv.getRuntime().getGlobalVariables().getNames();
        while (iter.hasNext()) {
            String globalVariableName = (String) iter.next();

            globalVariables.append(recv.getRuntime().newString(globalVariableName));
        }

        return globalVariables;
    }

    /** Returns an Array with the names of all local variables.
     *
     */
    public static RubyArray local_variables(IRubyObject recv) {
        final IRuby runtime = recv.getRuntime();
        RubyArray localVariables = runtime.newArray();

        if (runtime.getCurrentContext().getCurrentScope().getLocalNames() != null) {
            for (int i = 2; i < runtime.getCurrentContext().getCurrentScope().getLocalNames().size(); i++) {
				String variableName = (String) runtime.getCurrentContext().getCurrentScope().getLocalNames().get(i);
                if (variableName != null) {
                    localVariables.append(runtime.newString(variableName));
                }
            }
        }

        Iterator dynamicNames = runtime.getCurrentContext().getCurrentDynamicVars().names().iterator();
        while (dynamicNames.hasNext()) {
            String name = (String) dynamicNames.next();
            localVariables.append(runtime.newString(name));
        }

        return localVariables;
    }

    public static RubyBinding binding(IRubyObject recv) {
        return recv.getRuntime().newBinding();
    }

    public static RubyBoolean block_given(IRubyObject recv) {
        return recv.getRuntime().newBoolean(recv.getRuntime().getCurrentContext().isFBlockGiven());
    }

    public static IRubyObject sprintf(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw recv.getRuntime().newArgumentError("sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        RubyArray newArgs = recv.getRuntime().newArray(args);
        newArgs.shift();

        return ((StringMetaClass)str.getMetaClass()).format.call(recv.getRuntime(), str, "%", new IRubyObject[] {newArgs}, false);
    }

    public static IRubyObject raise(IRubyObject recv, IRubyObject[] args) {
        // FIXME  special case in ruby
        // recv.checkArgumentCount(args, 0, 2); 
        IRuby runtime = recv.getRuntime();
        RubyString string = null;
        RubyException excptn = null;
        RaiseException re = null;
        
        switch (args.length) {
        case 0 :
            IRubyObject defaultException = runtime.getGlobalVariables().get("$!");
            if (defaultException.isNil()) {
                re = new RaiseException(runtime, runtime.getClass("RuntimeError"), "", false);
            } else {
            	re = new RaiseException((RubyException) defaultException);
            }
            break;
        case 1 :
            if (args[0] instanceof RubyException) {
                re = new RaiseException((RubyException) args[0]);
            } else if (args[0] instanceof RubyClass) {
            	re = new RaiseException(RubyException.newInstance(args[0], new IRubyObject[0]));
            } else {
            	re = new RaiseException(RubyException.newInstance(runtime.getClass("RuntimeError"), args));
            }
            break;
        case 2 :
            if (args[0] == runtime.getClass("Exception")) {
                re = new RaiseException((RubyException) args[0].callMethod("exception", args[1]));
            } else {
	            string = (RubyString) args[1]; 
	            re = new RaiseException(new RubyException(runtime, ((RubyClass)args[0]), string.getValue()));
            }
            break;
        case 3:
            if (args[0] == runtime.getClass("Exception")) {
                re = new RaiseException((RubyException) args[0].callMethod("exception", args[1]));
            } else {
	            string = (RubyString) args[1];
	            excptn = new RubyException(runtime, ((RubyClass)args[0]), string.getValue()); 
	            excptn.set_backtrace(args[2]);
	            re = new RaiseException(excptn);
            }
            break;
        default :
            re = runtime.newArgumentError("wrong number of arguments");
        }
        
        // Insert exception to be raised into global var and current thread
        runtime.getGlobalVariables().set("$!", re.getException());
        
        throw re;
    }
    
    /**
     * Require.
     * MRI allows to require ever .rb files or ruby extension dll (.so or .dll depending on system).
     * we allow requiring either .rb files or jars.
     * @param recv ruby object used to call require (any object will do and it won't be used anyway).
     * @param name the name of the file to require
     **/
    public static IRubyObject require(IRubyObject recv, IRubyObject name) {
        if (recv.getRuntime().getLoadService().require(name.toString())) {
            return recv.getRuntime().getTrue();
        }
        return recv.getRuntime().getFalse();
    }

    public static IRubyObject load(IRubyObject recv, IRubyObject[] args) {
        RubyString file = (RubyString)args[0];
        recv.getRuntime().getLoadService().load(file.toString());
        return recv.getRuntime().getTrue();
    }

    public static IRubyObject eval(IRubyObject recv, IRubyObject[] args) {
        IRuby runtime = recv.getRuntime();
        RubyString src = (RubyString) args[0];
        IRubyObject scope = args.length > 1 ? args[1] : runtime.getNil();
        String file = args.length > 2 ? args[2].toString() : "(eval)";
        int line = args.length > 3 ? RubyNumeric.fix2int(args[3]) : 1;

        src.checkSafeString();

        if (scope.isNil() && runtime.getCurrentContext().getPreviousFrame() != null) {
            try {
                runtime.getCurrentContext().preKernelEval();
                return recv.eval(src, scope, file, line);
            } finally {
                runtime.getCurrentContext().postKernelEval();
            }
        }
        return recv.eval(src, scope, file, line);
    }

    public static IRubyObject caller(IRubyObject recv, IRubyObject[] args) {
        int level = args.length > 0 ? RubyNumeric.fix2int(args[0]) : 1;

        if (level < 0) {
            throw recv.getRuntime().newArgumentError("negative level(" + level + ')');
        }
        
        return recv.getRuntime().getCurrentContext().createBacktrace(level, false);
    }

    public static IRubyObject rbCatch(IRubyObject recv, IRubyObject tag) {
        try {
            return recv.getRuntime().getCurrentContext().yield(tag);
        } catch (JumpException je) {
        	if (je.getJumpType() == JumpException.JumpType.ThrowJump) {
	            if (je.getPrimaryData().equals(tag.asSymbol())) {
	                return (IRubyObject)je.getTertiaryData();
	            }
        	}
       		throw je;
        }
    }

    public static IRubyObject rbThrow(IRubyObject recv, IRubyObject[] args) {
        IRuby runtime = recv.getRuntime();
        JumpException je = new JumpException(JumpException.JumpType.ThrowJump);
        String tag = args[0].asSymbol();
        IRubyObject value = args.length > 1 ? args[1] : recv.getRuntime().getNil();
        RubyException nameException = new RubyException(runtime, runtime.getClass("NameError"), "uncaught throw '" + tag + '\'');
        
        je.setPrimaryData(tag);
        je.setSecondaryData(value);
        je.setTertiaryData(nameException);
        
        throw je;
    }

    public static IRubyObject trap(IRubyObject recv, IRubyObject[] args) {
        // FIXME: We can probably fake some basic signals, but obviously can't do everything. For now, stub.
        return recv.getRuntime().getNil();
    }

    public static IRubyObject set_trace_func(IRubyObject recv, IRubyObject trace_func) {
        if (trace_func.isNil()) {
            recv.getRuntime().setTraceFunction(null);
        } else if (!(trace_func instanceof RubyProc)) {
            throw recv.getRuntime().newTypeError("trace_func needs to be Proc.");
        } else {
            recv.getRuntime().setTraceFunction((RubyProc) trace_func);
        }
        return trace_func;
    }

    public static IRubyObject singleton_method_added(IRubyObject recv, IRubyObject symbolId) {
        return recv.getRuntime().getNil();
    }

    public static RubyProc proc(IRubyObject recv) {
        return RubyProc.newProc(recv.getRuntime(), true);
    }

    public static IRubyObject loop(IRubyObject recv) {
        while (true) {
            recv.getRuntime().getCurrentContext().yield(recv.getRuntime().getNil());

            Thread.yield();
        }
    }

    public static IRubyObject backquote(IRubyObject recv, IRubyObject aString) {
        StringBuffer output = new StringBuffer();
        IRuby runtime = recv.getRuntime();
        runtime.getGlobalVariables().set("$?", runtime.newFixnum(
            runInShell(runtime, aString.toString(), output)));
        
        return recv.getRuntime().newString(output.toString());
    }

    private static int runInShell(IRuby runtime, String command, StringBuffer output) {
        try {
            String shell = System.getProperty("jruby.shell");
            Process aProcess;
            String shellSwitch = "-c";
            if (shell != null) {
                if (!shell.endsWith("sh")) {
                    shellSwitch = "/c";
                }
                aProcess = Runtime.getRuntime().exec(new String[] { shell, shellSwitch, command });
            } else {
                aProcess = Runtime.getRuntime().exec(command);
            }

            final BufferedReader reader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));

            // Fairly innefficient impl, but readLine is unable to tell 
            // whether the last line in a process ended with a newline or not.
            int c;
            boolean crSeen = false;
            while ((c = reader.read()) != -1) {
            	if (c == '\r') {
            		crSeen = true;
            	} else {
            		if (crSeen) {
            			if (c != '\n') {
            				output.append('\r');
            			}
            			crSeen = false;
            		}
            		output.append((char)c);
            	}
            }
            if (crSeen) {
            	output.append('\r');
            }
            aProcess.getErrorStream().close();
            aProcess.getOutputStream().close();
            reader.close();
            
            return aProcess.waitFor();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    public static RubyInteger srand(IRubyObject recv, IRubyObject[] args) {
        IRuby runtime = recv.getRuntime();
        long oldRandomSeed = runtime.getRandomSeed();

        if (args.length > 0) {
            RubyInteger integerSeed = 
            	(RubyInteger) args[0].convertToType("Integer", "to_i", true);
            runtime.setRandomSeed(integerSeed.getLongValue());
        } else {
        	// Not sure how well this works, but it works much better than
        	// just currentTimeMillis by itself.
            runtime.setRandomSeed(System.currentTimeMillis() ^
			  recv.hashCode() ^ runtime.incrementRandomSeedSequence() ^
			  runtime.getRandom().nextInt(Math.abs((int)runtime.getRandomSeed())));
        }
        runtime.getRandom().setSeed(runtime.getRandomSeed());
        return runtime.newFixnum(oldRandomSeed);
    }

    public static RubyNumeric rand(IRubyObject recv, IRubyObject[] args) {
        long ceil;
        if (args.length == 0) {
            ceil = 0;
        } else if (args.length == 1) {
            RubyInteger integerCeil = (RubyInteger) args[0].convertToType("Integer", "to_i", true);
            ceil = integerCeil.getLongValue();
            ceil = Math.abs(ceil);
            if (ceil > Integer.MAX_VALUE) {
                throw recv.getRuntime().newNotImplementedError("Random values larger than Integer.MAX_VALUE not supported");
            }
        } else {
            throw recv.getRuntime().newArgumentError("wrong # of arguments(" + args.length + " for 1)");
        }

        if (ceil == 0) {
            double result = recv.getRuntime().getRandom().nextDouble();
            return RubyFloat.newFloat(recv.getRuntime(), result);
        }
		return recv.getRuntime().newFixnum(recv.getRuntime().getRandom().nextInt((int) ceil));
    }

    public static RubyBoolean system(IRubyObject recv, IRubyObject[] args) {
        IRuby runtime = recv.getRuntime();
        if (args.length > 1) {
            throw runtime.newArgumentError("more arguments not yet supported");
        }
        StringBuffer output = new StringBuffer();
        int resultCode = runInShell(runtime, args[0].toString(), output);
        recv.getRuntime().getGlobalVariables().set("$?", runtime.newFixnum(resultCode));
        return runtime.newBoolean(resultCode == 0);
    }
}
