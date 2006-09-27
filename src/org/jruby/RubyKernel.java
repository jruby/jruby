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
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Kiel Hodges <jruby-devel@selfsosoft.com>
 * Copyright (C) 2006 Evan Buswell <evan@heron.sytes.net>
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.FileMetaClass;
import org.jruby.runtime.builtin.meta.IOMetaClass;
import org.jruby.runtime.builtin.meta.StringMetaClass;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.PrintfFormat;
import org.jruby.util.UnsynchronizedStack;

/**
 * Note: For CVS history, see KernelModule.java.
 *
 * @author jpetersen
 */
public class RubyKernel {
    public static RubyModule createKernelModule(IRuby runtime) {
        RubyModule module = runtime.defineModule("Kernel");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyKernel.class);
        CallbackFactory objectCallbackFactory = runtime.callbackFactory(RubyObject.class);

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
        module.defineModuleFunction("exit", callbackFactory.getOptSingletonMethod("exit"));
        module.defineModuleFunction("exit!", callbackFactory.getOptSingletonMethod("exit_bang"));
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
        module.defineModuleFunction("select", callbackFactory.getOptSingletonMethod("select"));
        module.defineModuleFunction("set_trace_func", callbackFactory.getSingletonMethod("set_trace_func", IRubyObject.class));
        module.defineModuleFunction("sleep", callbackFactory.getSingletonMethod("sleep", IRubyObject.class));
        module.defineModuleFunction("split", callbackFactory.getOptSingletonMethod("split"));
        module.defineAlias("sprintf", "format");
        module.defineModuleFunction("srand", callbackFactory.getOptSingletonMethod("srand"));
        module.defineModuleFunction("sub", callbackFactory.getOptSingletonMethod("sub"));
        module.defineModuleFunction("sub!", callbackFactory.getOptSingletonMethod("sub_bang"));
        // Skipping: Kernel#syscall (too system dependent)
        module.defineModuleFunction("system", callbackFactory.getOptSingletonMethod("system"));
        // TODO: Implement Kernel#exec differently?
        module.defineAlias("exec", "system");
        // TODO: Implement Kernel#test (partial impl)
        module.defineModuleFunction("throw", callbackFactory.getOptSingletonMethod("rbThrow"));
        // TODO: Implement Kernel#trace_var
        module.defineModuleFunction("trap", callbackFactory.getOptSingletonMethod("trap"));
        // TODO: Implement Kernel#untrace_var
        module.defineModuleFunction("warn", callbackFactory.getSingletonMethod("warn", IRubyObject.class));
        
        // Defined p411 Pickaxe 2nd ed.
        module.defineModuleFunction("singleton_method_added", callbackFactory.getSingletonMethod("singleton_method_added", IRubyObject.class));
        
        // Object methods
        module.definePublicModuleFunction("==", objectCallbackFactory.getMethod("equal", IRubyObject.class));
        module.defineAlias("===", "==");
        module.defineAlias("eql?", "==");
        module.definePublicModuleFunction("to_s", objectCallbackFactory.getMethod("to_s"));
        module.definePublicModuleFunction("nil?", objectCallbackFactory.getMethod("nil_p"));
        module.definePublicModuleFunction("to_a", callbackFactory.getSingletonMethod("to_a"));
        module.definePublicModuleFunction("hash", objectCallbackFactory.getMethod("hash"));
        module.definePublicModuleFunction("id", objectCallbackFactory.getMethod("id"));
        module.defineAlias("__id__", "id");
        module.defineAlias("object_id", "id");
        module.definePublicModuleFunction("is_a?", objectCallbackFactory.getMethod("kind_of", IRubyObject.class));
        module.defineAlias("kind_of?", "is_a?");
        module.definePublicModuleFunction("dup", objectCallbackFactory.getMethod("dup"));
        module.definePublicModuleFunction("equal?", objectCallbackFactory.getMethod("same", IRubyObject.class));
        module.definePublicModuleFunction("type", objectCallbackFactory.getMethod("type_deprecated"));
        module.definePublicModuleFunction("class", objectCallbackFactory.getMethod("type"));
        module.definePublicModuleFunction("inspect", objectCallbackFactory.getMethod("inspect"));
        module.definePublicModuleFunction("=~", objectCallbackFactory.getMethod("match", IRubyObject.class));
        module.definePublicModuleFunction("clone", objectCallbackFactory.getMethod("rbClone"));
        module.definePublicModuleFunction("display", objectCallbackFactory.getOptMethod("display"));
        module.definePublicModuleFunction("extend", objectCallbackFactory.getOptMethod("extend"));
        module.definePublicModuleFunction("freeze", objectCallbackFactory.getMethod("freeze"));
        module.definePublicModuleFunction("frozen?", objectCallbackFactory.getMethod("frozen"));
        module.defineModuleFunction("initialize_copy", objectCallbackFactory.getMethod("initialize_copy", IRubyObject.class));
        module.definePublicModuleFunction("instance_eval", objectCallbackFactory.getOptMethod("instance_eval"));
        module.definePublicModuleFunction("instance_of?", objectCallbackFactory.getMethod("instance_of", IRubyObject.class));
        module.definePublicModuleFunction("instance_variables", objectCallbackFactory.getMethod("instance_variables"));
        module.definePublicModuleFunction("instance_variable_get", objectCallbackFactory.getMethod("instance_variable_get", IRubyObject.class));
        module.definePublicModuleFunction("instance_variable_set", objectCallbackFactory.getMethod("instance_variable_set", IRubyObject.class, IRubyObject.class));
        module.definePublicModuleFunction("method", objectCallbackFactory.getMethod("method", IRubyObject.class));
        module.definePublicModuleFunction("methods", objectCallbackFactory.getOptMethod("methods"));
        module.definePublicModuleFunction("private_methods", objectCallbackFactory.getMethod("private_methods"));
        module.definePublicModuleFunction("protected_methods", objectCallbackFactory.getMethod("protected_methods"));
        module.definePublicModuleFunction("public_methods", objectCallbackFactory.getOptMethod("public_methods"));
        module.defineModuleFunction("remove_instance_variable", objectCallbackFactory.getMethod("remove_instance_variable", IRubyObject.class));
        module.definePublicModuleFunction("respond_to?", objectCallbackFactory.getOptMethod("respond_to"));
        module.definePublicModuleFunction("send", objectCallbackFactory.getOptMethod("send"));
        module.defineAlias("__send__", "send");
        module.definePublicModuleFunction("singleton_methods", objectCallbackFactory.getMethod("singleton_methods"));
        module.definePublicModuleFunction("taint", objectCallbackFactory.getMethod("taint"));
        module.definePublicModuleFunction("tainted?", objectCallbackFactory.getMethod("tainted"));
        module.definePublicModuleFunction("untaint", objectCallbackFactory.getMethod("untaint"));

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
        ThreadContext tc = runtime.getCurrentContext();
        Visibility lastVis = tc.getLastVisibility();
        CallType lastCallType = tc.getLastCallType();
        String format = lastVis.errorMessageFormat(lastCallType, name);
        String msg = new PrintfFormat(format).sprintf(new Object[] { name, description, 
            noClass ? "" : ":", noClass ? "" : recv.getType().getName()});

        throw lastCallType == CallType.VARIABLE ? runtime.newNameError(msg) : runtime.newNoMethodError(msg);
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args) {
        String arg = args[0].convertToString().toString();

        // Should this logic be pushed into RubyIO Somewhere?
        if (arg.startsWith("|")) {
            String command = arg.substring(1);
        	// exec process, create IO with process
        	try {
                // TODO: may need to part cli parms out ourself?
                Process p = Runtime.getRuntime().exec(command);
                RubyIO io = new RubyIO(recv.getRuntime(), p);
                ThreadContext tc = recv.getRuntime().getCurrentContext();
        		
        	    if (tc.isBlockGiven()) {
        	        try {
        	            tc.yield(io);
        	            
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
        IRubyObject value = object.convertToTypeWithCheck("Array", "to_ary");
        
        if (value.isNil()) {
            ICallable method = object.getMetaClass().searchMethod("to_a");
            
            if (method.getImplementationClass() == recv.getRuntime().getKernel()) {
                return recv.getRuntime().newArray(object);
            }
            
            // Strange that Ruby has custom code here and not convertToTypeWithCheck equivalent.
            value = object.callMethod("to_a");
            if (value.getMetaClass() != recv.getRuntime().getClass("Array")) {
                throw recv.getRuntime().newTypeError("`to_a' did not return Array");
               
            }
        }
        
        return value;
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
        
        defout.callMethod("puts", args);

        return recv.getRuntime().getNil();
    }

    public static IRubyObject print(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");

        defout.callMethod("print", args);

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

    public static IRubyObject select(IRubyObject recv, IRubyObject[] args) {
        return IOMetaClass.select_static(recv.getRuntime(), args);
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

    // FIXME: Add at_exit and finalizers to exit, then make exit_bang not call those.
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

    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        return exit(recv, args);
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
        ThreadContext tc = runtime.getCurrentContext();

        if (tc.getFrameScope().getLocalNames() != null) {
            for (int i = 2; i < tc.getFrameScope().getLocalNames().length; i++) {
				String variableName = (String) tc.getFrameScope().getLocalNames()[i];
                if (variableName != null) {
                    localVariables.append(runtime.newString(variableName));
                }
            }
        }

        Iterator dynamicNames = tc.getCurrentDynamicVars().names().iterator();
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

        return ((StringMetaClass)str.getMetaClass()).format.call(recv.getRuntime(), str, str.getMetaClass(), "%", new IRubyObject[] {newArgs}, false);
    }

    public static IRubyObject raise(IRubyObject recv, IRubyObject[] args) {
        recv.checkArgumentCount(args, 0, 3); 
        IRuby runtime = recv.getRuntime();

        if (args.length == 0) {
            IRubyObject lastException = runtime.getGlobalVariables().get("$!");
            if (lastException.isNil()) {
                throw new RaiseException(runtime, runtime.getClass("RuntimeError"), "", false);
            } 
            throw new RaiseException((RubyException) lastException);
        }

        IRubyObject exception;
        if (args.length == 1) {
            if (args[0] instanceof RubyString) {
                throw new RaiseException(RubyException.newInstance(runtime.getClass("RuntimeError"), args));
            }
            
            if (!args[0].respondsTo("exception")) {
                throw runtime.newTypeError("exception class/object expected");
            }
            exception = args[0].callMethod("exception");
        } else {
            if (!args[0].respondsTo("exception")) {
                throw runtime.newTypeError("exception class/object expected");
            }
            
            exception = args[0].callMethod("exception", args[1]);
        }
        
        if (!exception.isKindOf(runtime.getClass("Exception"))) {
            throw runtime.newTypeError("exception object expected");
        }
        
        if (args.length == 3) {
            ((RubyException) exception).set_backtrace(args[2]);
        }
        
        throw new RaiseException((RubyException) exception);
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
        IRubyObject scope = null;
        String file = "(eval)";
        
        if (args.length > 1) {
            if (!args[1].isNil()) {
                scope = args[1];
            }
            
            if (args.length > 2) {
                file = args[2].toString();
            }
        }
        // FIXME: line number is not supported yet
        //int line = args.length > 3 ? RubyNumeric.fix2int(args[3]) : 1;

        src.checkSafeString();
        ThreadContext tc = runtime.getCurrentContext();

        if (scope == null && tc.getPreviousFrame() != null) {
            try {
                tc.preKernelEval();
                return recv.evalSimple(src, file);
            } finally {
                tc.postKernelEval();
            }
        }
        return recv.evalWithBinding(src, scope, file);
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
	                return (IRubyObject)je.getSecondaryData();
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
    
    public static IRubyObject warn(IRubyObject recv, IRubyObject message) {
    	IRubyObject out = recv.getRuntime().getObject().getConstant("STDERR");
        RubyIO io = (RubyIO) out.convertToType("IO", "to_io", true); 

        io.puts(new IRubyObject[] { message });
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
        IRuby runtime = recv.getRuntime();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        int resultCode = runInShell(runtime, new IRubyObject[] {aString}, output);
        
        recv.getRuntime().getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, resultCode));
        
        return recv.getRuntime().newString(output.toString());
    }
    
    private static final Pattern PATH_SEPARATORS = Pattern.compile("[/\\\\]");
    
    /**
     * For the first full token on the command, most likely the actual executable to run, replace
     * all dir separators with that which is appropriate for the current platform. Return the new
     * with this executable string at the beginning.
     * 
     * @param command The all-forward-slashes command to be "fixed"
     * @return The "fixed" full command line
     */
    private static String repairDirSeps(String command) {
        String executable = "", remainder = "";
        command = command.trim();
        if (command.startsWith("'")) {
            String [] tokens = command.split("'", 3);
            executable = "'"+tokens[1]+"'";
            if (tokens.length > 2)
                remainder = tokens[2];
        } else if (command.startsWith("\"")) {
            String [] tokens = command.split("\"", 3);
            executable = "\""+tokens[1]+"\"";
            if (tokens.length > 2)
                remainder = tokens[2];
        } else {
            String [] tokens = command.split(" ", 2);
            executable = tokens[0];
            if (tokens.length > 1)
                remainder = " "+tokens[1];
        }
        
        // Matcher.replaceAll treats backslashes in the replacement string as escaped characters
        String replacement = File.separator;
        if (File.separatorChar == '\\')
            replacement = "\\\\";
            
        return PATH_SEPARATORS.matcher(executable).replaceAll(replacement) + remainder;
                }

    private static List parseCommandLine(IRubyObject[] rawArgs) {
        // first parse the first element of rawArgs since this may contain
        // the whole command line
        String command = rawArgs[0].toString();
        UnsynchronizedStack args = new UnsynchronizedStack();
        StringTokenizer st = new StringTokenizer(command, " ");
        String quoteChar = null;

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (quoteChar == null) {
                // not currently in the middle of a quoted token
                if (token.startsWith("'") || token.startsWith("\"")) {
                    // note quote char and remove from beginning of token
                    quoteChar = token.substring(0, 1);
                    token = token.substring(1);
                }
                if (quoteChar!=null && token.endsWith(quoteChar)) {
                    // quoted token self contained, remove from end of token
                    token = token.substring(0, token.length()-1);
                    quoteChar = null;
                }
                // add new token to list
                args.push(token);
            } else {
                // in the middle of quoted token
                if (token.endsWith(quoteChar)) {
                    // end of quoted token
                    token = token.substring(0, token.length()-1);
                    quoteChar = null;
                }
                // update token at end of list
                token = args.pop() + " " + token;
                args.push(token);
            }
        }
        
        // now append the remaining raw args to the cooked arg list
        for (int i=1;i<rawArgs.length;i++) {
            args.push(rawArgs[i].toString());
        }
        
        return args;
    }
        
    private static boolean isRubyCommand(String command) {
        command = command.trim();
        String [] spaceDelimitedTokens = command.split(" ", 2);
        String [] slashDelimitedTokens = spaceDelimitedTokens[0].split("/");
        String finalToken = slashDelimitedTokens[slashDelimitedTokens.length-1];
        if (finalToken.indexOf("ruby") != -1 || finalToken.endsWith(".rb"))
            return true;
        else
            return false;
    }
    
    private static class InProcessScript extends Thread {
    	private String[] argArray;
    	private InputStream in;
    	private PrintStream out;
    	private PrintStream err;
    	private int result;
    	
    	public InProcessScript(String[] argArray, InputStream in, PrintStream out, PrintStream err) {
    		this.argArray = argArray;
    		this.in = in;
    		this.out = out;
    		this.err = err;
    	}

		public int getResult() {
			return result;
		}

		public void setResult(int result) {
			this.result = result;
		}
		
        public void run() {
            result = new Main(in, out, err).run(argArray);
        }
    }

    public static int runInShell(IRuby runtime, IRubyObject[] rawArgs, OutputStream output) {
        try {
            // startup scripts set jruby.shell to /bin/sh for Unix, cmd.exe for Windows
            String shell = System.getProperty("jruby.shell");
            rawArgs[0] = runtime.newString(repairDirSeps(rawArgs[0].toString()));
            Process aProcess = null;
            InProcessScript ipScript = null;
            
            if (isRubyCommand(rawArgs[0].toString())) {
                List args = parseCommandLine(rawArgs);
                PrintStream redirect = new PrintStream(output);
                String command = (String)args.get(0);

                String[] argArray = new String[args.size()-1];
                // snip off ruby or jruby command from list of arguments
                // leave alone if the command is the name of a script
                int startIndex = command.endsWith(".rb") ? 0 : 1;
                args.subList(startIndex,args.size()).toArray(argArray);

                // FIXME: Where should we get in and err from?
                ipScript = new InProcessScript(argArray, System.in, redirect, redirect);
                
                // execute ruby command in-process
                ipScript.start();
                ipScript.join();
            } else if (shell != null && rawArgs.length == 1) {
                // execute command with sh -c or cmd.exe /c
                // this does shell expansion of wildcards
                String shellSwitch = shell.endsWith("sh") ? "-c" : "/c";
                String[] argArray = new String[3];
                argArray[0] = shell;
                argArray[1] = shellSwitch;
                argArray[2] = rawArgs[0].toString();
                aProcess = Runtime.getRuntime().exec(argArray);
            } else {
                // execute command directly, no wildcard expansion
                if (rawArgs.length > 1) {
                    String[] argArray = new String[rawArgs.length];
                    for (int i=0;i<rawArgs.length;i++) {
                        argArray[i] = rawArgs[i].toString();
                    }
                    aProcess = Runtime.getRuntime().exec(argArray);
                } else {
                    aProcess = Runtime.getRuntime().exec(rawArgs[0].toString());
                }
            }
            
            if (aProcess != null) {
                InputStream processOutput = aProcess.getInputStream();
                
                // Fairly innefficient impl, but readLine is unable to tell
                // whether the last line in a process ended with a newline or not.
                int b;
                boolean crSeen = false;
                while ((b = processOutput.read()) != -1) {
                    if (b == '\r') {
                        crSeen = true;
                    } else {
                        if (crSeen) {
                            if (b != '\n') {
                                output.write('\r');
                            }
                            crSeen = false;
                        }
                        output.write(b);
                    }
                }
                if (crSeen) {
                    output.write('\r');
                }
                aProcess.getErrorStream().close();
                aProcess.getOutputStream().close();
                processOutput.close();
                
                return aProcess.waitFor();
            } else if (ipScript != null) {
            	return ipScript.getResult();
            } else {
                return 0;
            }
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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int resultCode = runInShell(runtime, args, output);
        recv.getRuntime().getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, resultCode));
        return runtime.newBoolean(resultCode == 0);
    }
    
    public static RubyArray to_a(IRubyObject recv) {
        recv.getRuntime().getWarnings().warn("default 'to_a' will be obsolete");
        return recv.getRuntime().newArray(recv);
    }
}
