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
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
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
import org.jruby.exceptions.EOFError;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.NoMethodError;
import org.jruby.exceptions.NotImplementedError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.SystemExit;
import org.jruby.exceptions.ThreadError;
import org.jruby.exceptions.ThrowJump;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.LastCallStatus;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.FileMetaClass;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.ILoadService;
import org.jruby.util.PrintfFormat;

/**
 * Note: For CVS history, see KernelModule.java.
 *
 * @author jpetersen
 * @version $Revision$
 */
public class RubyKernel {
    public static RubyModule createKernelModule(Ruby runtime) {
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
        // TODO: Implement Kernel#binding
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
        module.defineModuleFunction("method_missing",
        		runtime.callbackFactory(RubyObject.class).getOptMethod("method_missing"));
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
        module.defineModuleFunction("sleep", callbackFactory.getSingletonMethod("sleep", RubyNumeric.class));
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
        // TODO: Implement Kernel#trap
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
        final ILoadService loadService = recv.getRuntime().getLoadService();
        loadService.addAutoload(symbol.asSymbol(), new IAutoloadMethod() {
            /**
             * @see org.jruby.runtime.load.IAutoloadMethod#load(Ruby, String)
             */
            public IRubyObject load(Ruby runtime, String name) {
                loadService.require(file.toString());
                return runtime.getClasses().getObjectClass().getConstant(name);
            }
        });
        return recv;
    }

    public IRubyObject method_missing(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw recv.getRuntime().newArgumentError("no id given");
        }

        String name = args[0].asSymbol();

        String description = recv.callMethod("inspect").toString();
        boolean noClass = description.charAt(0) == '#';
        if (recv.isNil()) {
            noClass = true;
            description = "nil";
        } else if (recv == recv.getRuntime().getTrue()) {
            noClass = true;
            description = "true";
        } else if (recv == recv.getRuntime().getFalse()) {
            noClass = true;
            description = "false";
        }

        LastCallStatus lastCallStatus = recv.getRuntime().getLastCallStatus();

        String format = lastCallStatus.errorMessageFormat(name);

        String msg =
            new PrintfFormat(format).sprintf(
                new Object[] { name, description, noClass ? "" : ":", noClass ? "" : recv.getType().getName()});

        throw new NoMethodError(recv.getRuntime(), msg);
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args) {
        String arg = ((RubyString) args[0].convertToString()).getValue();

        // Should this logic be pushed into RubyIO Somewhere?
        if (arg.startsWith("|")) {
            String command = arg.substring(1);
        	// exec process, create IO with process
        	try {
                // TODO: may need to part cli parms out ourself?
                Process p = Runtime.getRuntime().exec(command);
                RubyIO io = new RubyIO(recv.getRuntime(), p);
        		
        	    if (recv.getRuntime().isBlockGiven()) {
        	        try {
        	            recv.getRuntime().yield(io);
        	            
            	        return recv.getRuntime().getNil();
        	        } finally {
        	            io.close();
        	        }
        	    }

                return io;
        	} catch (IOException ioe) {
        		throw new IOError(recv.getRuntime(), ioe.getMessage());
        	}
        } 

        return ((FileMetaClass) recv.getRuntime().getClasses().getFileClass()).open(args);
    }

    public static IRubyObject gets(IRubyObject recv, IRubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) recv.getRuntime().getGlobalVariables().get("$<");

        IRubyObject line = argsFile.internalGets(args);

        recv.getRuntime().setLastline(line);

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
            throw new EOFError(recv.getRuntime());
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
    private static RubyString getLastlineString(Ruby runtime) {
        IRubyObject line = runtime.getLastline();

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
            recv.getRuntime().setLastline(str);
        }

        return str;
    }

    public static IRubyObject gsub_bang(IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(recv.getRuntime()).gsub_bang(args);
    }

    public static IRubyObject gsub(IRubyObject recv, IRubyObject[] args) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.gsub_bang(args).isNil()) {
            recv.getRuntime().setLastline(str);
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
            recv.getRuntime().setLastline(str);
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

        recv.getRuntime().setLastline(dup);
        return dup;
    }

    public static IRubyObject split(IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(recv.getRuntime()).split(args);
    }

    public static IRubyObject scan(IRubyObject recv, IRubyObject pattern) {
        return getLastlineString(recv.getRuntime()).scan(pattern);
    }

    public static IRubyObject sleep(IRubyObject recv, RubyNumeric seconds) {
    	long milliseconds = (long) (seconds.getDoubleValue() * 1000);
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

        throw new SystemExit(recv.getRuntime(), status);
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
        final Ruby runtime = recv.getRuntime();
        RubyArray localVariables = runtime.newArray();

        if (runtime.getScope().getLocalNames() != null) {
            for (int i = 2; i < runtime.getScope().getLocalNames().size(); i++) {
                if (runtime.getScope().getLocalNames().get(i) != null) {
                    localVariables.append(runtime.newString((String) runtime.getScope().getLocalNames().get(i)));
                }
            }
        }

        Iterator dynamicNames = runtime.getDynamicNames().iterator();
        while (dynamicNames.hasNext()) {
            String name = (String) dynamicNames.next();
            localVariables.append(runtime.newString(name));
        }

        return localVariables;
    }

    public static RubyBoolean block_given(IRubyObject recv) {
        return recv.getRuntime().newBoolean(recv.getRuntime().isFBlockGiven());
    }

    public static IRubyObject sprintf(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw recv.getRuntime().newArgumentError("sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        RubyArray newArgs = recv.getRuntime().newArray(args);
        newArgs.shift();

        return str.format(newArgs);
    }

    public static IRubyObject raise(IRubyObject recv, IRubyObject[] args) {
        // FIXME  special case in ruby
        // recv.checkArgumentCount(args, 0, 2); 
        Ruby runtime = recv.getRuntime();
        RubyString string = null;
        RubyException excptn = null;
        
        switch (args.length) {
        case 0 :
            IRubyObject defaultException = runtime.getGlobalVariables().get("$!");
            if (defaultException.isNil()) {
                throw new RaiseException(runtime, runtime.getExceptions().getRuntimeError(), "", false);
            }
            throw new RaiseException((RubyException) defaultException);
        case 1 :
            if (args[0] instanceof RubyException) {
                throw new RaiseException((RubyException) args[0]);
            } else if (args[0] instanceof RubyClass) {
            	throw new RaiseException(RubyException.newInstance(args[0], new IRubyObject[0]));
            }
            throw new RaiseException(RubyException.newInstance(runtime.getExceptions().getRuntimeError(), args));
        case 2 :
            if (args[0] == runtime.getClasses().getExceptionClass()) {
                throw new RaiseException((RubyException) args[0].callMethod("exception", args[1]));
            }
            string = (RubyString) args[1];
            excptn = RubyException.newException(runtime, (RubyClass)args[0], string.getValue()); 
            throw new RaiseException(excptn);
        case 3:
            if (args[0] == runtime.getClasses().getExceptionClass()) {
                throw new RaiseException((RubyException) args[0].callMethod("exception", args[1]));
            }
            string = (RubyString) args[1];
            excptn = RubyException.newException(runtime, (RubyClass)args[0], string.getValue()); 
            excptn.set_backtrace(args[2]);
            throw new RaiseException(excptn);
        default :
            throw runtime.newArgumentError("wrong number of arguments");
        }
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
        Ruby runtime = recv.getRuntime();
        RubyString src = (RubyString) args[0];
        IRubyObject scope = args.length > 1 ? args[1] : runtime.getNil();
        String file = args.length > 2 ? args[2].toString() : "(eval)";
        int line = args.length > 3 ? RubyNumeric.fix2int(args[3]) : 1;

        src.checkSafeString();

        if (scope.isNil() && runtime.getFrameStack().getPrevious() != null) {
            try {
                runtime.getFrameStack().push(runtime.getFrameStack().getPrevious());
                return recv.eval(src, scope, file, line);
            } finally {
                runtime.getFrameStack().pop();
            }
        }
        return recv.eval(src, scope, file, line);
    }

    public static IRubyObject caller(IRubyObject recv, IRubyObject[] args) {
        int level = args.length > 0 ? RubyNumeric.fix2int(args[0]) : 1;

        if (level < 0) {
            throw recv.getRuntime().newArgumentError("negative level(" + level + ')');
        }

        return RaiseException.createBacktrace(recv.getRuntime(), level, false);
    }

    public static IRubyObject rbCatch(IRubyObject recv, IRubyObject tag) {
        try {
            return recv.getRuntime().yield(tag);
        } catch (ThrowJump throwJump) {
            if (throwJump.getTag().equals(tag.asSymbol())) {
                return throwJump.getValue();
            }
			throw throwJump;
        }
    }

    public static IRubyObject rbThrow(IRubyObject recv, IRubyObject[] args) {
        throw new ThrowJump(args[0].asSymbol(), args.length > 1 ? args[1] : recv.getRuntime().getNil());
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
            recv.getRuntime().yield(recv.getRuntime().getNil());

            Thread.yield();
        }
    }

    public static IRubyObject backquote(IRubyObject recv, IRubyObject aString) {
        StringBuffer output = new StringBuffer();
        Ruby runtime = recv.getRuntime();
        runtime.getGlobalVariables().set("$?", runtime.newFixnum(
            runInShell(runtime, aString.toString(), output)));
        
        return recv.getRuntime().newString(output.toString());
    }

    private static int runInShell(Ruby runtime, String command, StringBuffer output) {
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
            throw IOError.fromException(runtime, e);
        } catch (InterruptedException e) {
            throw new ThreadError(runtime, "unexpected interrupt");
        }
    }

    public static RubyInteger srand(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        long oldRandomSeed = runtime.randomSeed;

        if (args.length > 0) {
            RubyInteger integerSeed = 
            	(RubyInteger) args[0].convertToType("Integer", "to_i", true);
            runtime.randomSeed = integerSeed.getLongValue();
        } else {
        	// Not sure how well this works, but it works much better than
        	// just currentTimeMillis by itself.
            runtime.randomSeed = System.currentTimeMillis() ^
			  recv.hashCode() ^ runtime.randomSeedSequence++ ^
			  runtime.random.nextInt(Math.abs((int)runtime.randomSeed));
        }
        runtime.random.setSeed(runtime.randomSeed);
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
                throw new NotImplementedError(recv.getRuntime(), "Random values larger than Integer.MAX_VALUE not supported");
            }
        } else {
            throw recv.getRuntime().newArgumentError("wrong # of arguments(" + args.length + " for 1)");
        }

        if (ceil == 0) {
            double result = recv.getRuntime().random.nextDouble();
            return RubyFloat.newFloat(recv.getRuntime(), result);
        }
		return recv.getRuntime().newFixnum(recv.getRuntime().random.nextInt((int) ceil));
    }

    public static RubyBoolean system(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        if (args.length > 1) {
            throw runtime.newArgumentError("more arguments not yet supported");
        }
        StringBuffer output = new StringBuffer();
        int resultCode = runInShell(runtime, args[0].toString(), output);
        recv.getRuntime().getGlobalVariables().set("$?", runtime.newFixnum(resultCode));
        return runtime.newBoolean(resultCode == 0);
    }
}
