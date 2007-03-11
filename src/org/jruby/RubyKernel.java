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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.MainExitException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.meta.FileMetaClass;
import org.jruby.runtime.builtin.meta.IOMetaClass;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.Sprintf;
import org.jruby.util.UnsynchronizedStack;

/**
 * Note: For CVS history, see KernelModule.java.
 *
 * @author jpetersen
 */
public class RubyKernel {
    public final static Class IRUBY_OBJECT = IRubyObject.class;

    public static RubyModule createKernelModule(Ruby runtime) {
        RubyModule module = runtime.defineModule("Kernel");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyKernel.class);
        CallbackFactory objectCallbackFactory = runtime.callbackFactory(RubyObject.class);

        module.defineFastModuleFunction("Array", callbackFactory.getFastSingletonMethod("new_array", IRUBY_OBJECT));
        module.defineFastModuleFunction("Float", callbackFactory.getFastSingletonMethod("new_float", IRUBY_OBJECT));
        module.defineFastModuleFunction("Integer", callbackFactory.getFastSingletonMethod("new_integer", IRUBY_OBJECT));
        module.defineFastModuleFunction("String", callbackFactory.getFastSingletonMethod("new_string", IRUBY_OBJECT));
        module.defineFastModuleFunction("`", callbackFactory.getFastSingletonMethod("backquote", IRUBY_OBJECT));
        module.defineFastModuleFunction("abort", callbackFactory.getFastOptSingletonMethod("abort"));
        module.defineModuleFunction("at_exit", callbackFactory.getSingletonMethod("at_exit"));
        module.defineFastModuleFunction("autoload", callbackFactory.getFastSingletonMethod("autoload", IRUBY_OBJECT, IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("autoload?", callbackFactory.getFastSingletonMethod("autoload_p", IRUBY_OBJECT));
        module.defineModuleFunction("binding", callbackFactory.getSingletonMethod("binding"));
        module.defineModuleFunction("block_given?", callbackFactory.getSingletonMethod("block_given"));
        // TODO: Implement Kernel#callcc
        module.defineModuleFunction("caller", callbackFactory.getOptSingletonMethod("caller"));
        module.defineModuleFunction("catch", callbackFactory.getSingletonMethod("rbCatch", IRUBY_OBJECT));
        module.defineFastModuleFunction("chomp", callbackFactory.getFastOptSingletonMethod("chomp"));
        module.defineFastModuleFunction("chomp!", callbackFactory.getFastOptSingletonMethod("chomp_bang"));
        module.defineFastModuleFunction("chop", callbackFactory.getFastSingletonMethod("chop"));
        module.defineFastModuleFunction("chop!", callbackFactory.getFastSingletonMethod("chop_bang"));
        module.defineModuleFunction("eval", callbackFactory.getOptSingletonMethod("eval"));
        module.defineFastModuleFunction("exit", callbackFactory.getFastOptSingletonMethod("exit"));
        module.defineFastModuleFunction("exit!", callbackFactory.getFastOptSingletonMethod("exit_bang"));
        module.defineModuleFunction("fail", callbackFactory.getOptSingletonMethod("raise"));
        // TODO: Implement Kernel#fork
        module.defineFastModuleFunction("format", callbackFactory.getFastOptSingletonMethod("sprintf"));
        module.defineFastModuleFunction("gets", callbackFactory.getFastOptSingletonMethod("gets"));
        module.defineFastModuleFunction("global_variables", callbackFactory.getFastSingletonMethod("global_variables"));
        module.defineModuleFunction("gsub", callbackFactory.getOptSingletonMethod("gsub"));
        module.defineModuleFunction("gsub!", callbackFactory.getOptSingletonMethod("gsub_bang"));
        // TODO: Add deprecation to Kernel#iterator? (maybe formal deprecation mech.)
        module.defineModuleFunction("iterator?", callbackFactory.getSingletonMethod("block_given"));
        module.defineModuleFunction("lambda", callbackFactory.getSingletonMethod("proc"));
        module.defineModuleFunction("load", callbackFactory.getOptSingletonMethod("load"));
        module.defineFastModuleFunction("local_variables", callbackFactory.getFastSingletonMethod("local_variables"));
        module.defineModuleFunction("loop", callbackFactory.getSingletonMethod("loop"));
        // Note: method_missing is documented as being in Object, but ruby appears to stick it in Kernel.
        module.defineModuleFunction("method_missing", callbackFactory.getOptSingletonMethod("method_missing"));
        module.defineModuleFunction("open", callbackFactory.getOptSingletonMethod("open"));
        module.defineFastModuleFunction("p", callbackFactory.getFastOptSingletonMethod("p"));
        module.defineFastModuleFunction("print", callbackFactory.getFastOptSingletonMethod("print"));
        module.defineFastModuleFunction("printf", callbackFactory.getFastOptSingletonMethod("printf"));
        module.defineModuleFunction("proc", callbackFactory.getSingletonMethod("proc"));
        // TODO: implement Kernel#putc
        module.defineFastModuleFunction("puts", callbackFactory.getFastOptSingletonMethod("puts"));
        module.defineModuleFunction("raise", callbackFactory.getOptSingletonMethod("raise"));
        module.defineFastModuleFunction("rand", callbackFactory.getFastOptSingletonMethod("rand"));
        module.defineFastModuleFunction("readline", callbackFactory.getFastOptSingletonMethod("readline"));
        module.defineFastModuleFunction("readlines", callbackFactory.getFastOptSingletonMethod("readlines"));
        module.defineModuleFunction("require", callbackFactory.getSingletonMethod("require", IRUBY_OBJECT));
        module.defineModuleFunction("scan", callbackFactory.getSingletonMethod("scan", IRUBY_OBJECT));
        module.defineFastModuleFunction("select", callbackFactory.getFastOptSingletonMethod("select"));
        module.defineModuleFunction("set_trace_func", callbackFactory.getSingletonMethod("set_trace_func", IRUBY_OBJECT));
        module.defineFastModuleFunction("sleep", callbackFactory.getFastSingletonMethod("sleep", IRUBY_OBJECT));
        module.defineFastModuleFunction("split", callbackFactory.getFastOptSingletonMethod("split"));
        module.defineFastModuleFunction("sprintf", callbackFactory.getFastOptSingletonMethod("sprintf"));
        module.defineFastModuleFunction("srand", callbackFactory.getFastOptSingletonMethod("srand"));
        module.defineModuleFunction("sub", callbackFactory.getOptSingletonMethod("sub"));
        module.defineModuleFunction("sub!", callbackFactory.getOptSingletonMethod("sub_bang"));
        // Skipping: Kernel#syscall (too system dependent)
        module.defineFastModuleFunction("system", callbackFactory.getFastOptSingletonMethod("system"));
        // TODO: Implement Kernel#exec differently?
        module.defineFastModuleFunction("exec", callbackFactory.getFastOptSingletonMethod("system"));
        module.defineFastModuleFunction("test", callbackFactory.getFastOptSingletonMethod("test"));
        module.defineModuleFunction("throw", callbackFactory.getOptSingletonMethod("rbThrow"));
        // TODO: Implement Kernel#trace_var
        module.defineModuleFunction("trap", callbackFactory.getOptSingletonMethod("trap"));
        // TODO: Implement Kernel#untrace_var
        module.defineFastModuleFunction("warn", callbackFactory.getFastSingletonMethod("warn", IRUBY_OBJECT));
        
        // Defined p411 Pickaxe 2nd ed.
        module.defineModuleFunction("singleton_method_added", callbackFactory.getSingletonMethod("singleton_method_added", IRUBY_OBJECT));
        module.defineModuleFunction("singleton_method_removed", callbackFactory.getSingletonMethod("singleton_method_removed", IRUBY_OBJECT));
        module.defineModuleFunction("singleton_method_undefined", callbackFactory.getSingletonMethod("singleton_method_undefined", IRUBY_OBJECT));
        
        // Object methods
        module.defineFastPublicModuleFunction("==", objectCallbackFactory.getFastMethod("obj_equal", IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("===", objectCallbackFactory.getFastMethod("equal", IRUBY_OBJECT));

        module.defineAlias("eql?", "==");
        module.defineFastPublicModuleFunction("to_s", objectCallbackFactory.getFastMethod("to_s"));
        module.defineFastPublicModuleFunction("nil?", objectCallbackFactory.getFastMethod("nil_p"));
        module.defineFastPublicModuleFunction("to_a", callbackFactory.getFastSingletonMethod("to_a"));
        module.defineFastPublicModuleFunction("hash", objectCallbackFactory.getFastMethod("hash"));
        module.defineFastPublicModuleFunction("id", objectCallbackFactory.getFastMethod("id_deprecated"));
        module.defineFastPublicModuleFunction("object_id", objectCallbackFactory.getFastMethod("id"));
        module.defineAlias("__id__", "object_id");
        module.defineFastPublicModuleFunction("is_a?", objectCallbackFactory.getFastMethod("kind_of", IRUBY_OBJECT));
        module.defineAlias("kind_of?", "is_a?");
        module.defineFastPublicModuleFunction("dup", objectCallbackFactory.getFastMethod("dup"));
        module.defineFastPublicModuleFunction("equal?", objectCallbackFactory.getFastMethod("same", IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("type", objectCallbackFactory.getFastMethod("type_deprecated"));
        module.defineFastPublicModuleFunction("class", objectCallbackFactory.getFastMethod("type"));
        module.defineFastPublicModuleFunction("inspect", objectCallbackFactory.getFastMethod("inspect"));
        module.defineFastPublicModuleFunction("=~", objectCallbackFactory.getFastMethod("match", IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("clone", objectCallbackFactory.getFastMethod("rbClone"));
        module.defineFastPublicModuleFunction("display", objectCallbackFactory.getFastOptMethod("display"));
        module.defineFastPublicModuleFunction("extend", objectCallbackFactory.getFastOptMethod("extend"));
        module.defineFastPublicModuleFunction("freeze", objectCallbackFactory.getFastMethod("freeze"));
        module.defineFastPublicModuleFunction("frozen?", objectCallbackFactory.getFastMethod("frozen"));
        module.defineFastModuleFunction("initialize_copy", objectCallbackFactory.getFastMethod("initialize_copy", IRUBY_OBJECT));
        module.definePublicModuleFunction("instance_eval", objectCallbackFactory.getOptMethod("instance_eval"));
        module.defineFastPublicModuleFunction("instance_of?", objectCallbackFactory.getFastMethod("instance_of", IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("instance_variables", objectCallbackFactory.getFastMethod("instance_variables"));
        module.defineFastPublicModuleFunction("instance_variable_get", objectCallbackFactory.getFastMethod("instance_variable_get", IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("instance_variable_set", objectCallbackFactory.getFastMethod("instance_variable_set", IRUBY_OBJECT, IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("method", objectCallbackFactory.getFastMethod("method", IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("methods", objectCallbackFactory.getFastOptMethod("methods"));
        module.defineFastPublicModuleFunction("private_methods", objectCallbackFactory.getFastMethod("private_methods"));
        module.defineFastPublicModuleFunction("protected_methods", objectCallbackFactory.getFastMethod("protected_methods"));
        module.defineFastPublicModuleFunction("public_methods", objectCallbackFactory.getFastOptMethod("public_methods"));
        module.defineFastModuleFunction("remove_instance_variable", objectCallbackFactory.getMethod("remove_instance_variable", IRUBY_OBJECT));
        module.defineFastPublicModuleFunction("respond_to?", objectCallbackFactory.getFastOptMethod("respond_to"));
        module.definePublicModuleFunction("send", objectCallbackFactory.getOptMethod("send"));
        module.defineAlias("__send__", "send");
        module.defineFastPublicModuleFunction("singleton_methods", objectCallbackFactory.getFastOptMethod("singleton_methods"));
        module.defineFastPublicModuleFunction("taint", objectCallbackFactory.getFastMethod("taint"));
        module.defineFastPublicModuleFunction("tainted?", objectCallbackFactory.getFastMethod("tainted"));
        module.defineFastPublicModuleFunction("untaint", objectCallbackFactory.getFastMethod("untaint"));

        return module;
    }

    public static IRubyObject at_exit(IRubyObject recv, Block block) {
        return recv.getRuntime().pushExitBlock(recv.getRuntime().newProc(false, block));
    }

    public static IRubyObject autoload_p(final IRubyObject recv, IRubyObject symbol) {
        String name = symbol.asSymbol();
        if (recv instanceof RubyModule) {
            name = ((RubyModule)recv).getName() + "::" + name;
        }
        
        IAutoloadMethod autoloadMethod = recv.getRuntime().getLoadService().autoloadFor(name);
        if(autoloadMethod == null) return recv.getRuntime().getNil();

        return recv.getRuntime().newString(autoloadMethod.file());
    }

    public static IRubyObject autoload(final IRubyObject recv, IRubyObject symbol, final IRubyObject file) {
        final LoadService loadService = recv.getRuntime().getLoadService();
        final String baseName = symbol.asSymbol();
        String nm = baseName;
        if(recv instanceof RubyModule) {
            nm = ((RubyModule)recv).getName() + "::" + nm;
        }
        loadService.addAutoload(nm, new IAutoloadMethod() {
                public String file() {
                    return file.toString();
                }
            /**
             * @see org.jruby.runtime.load.IAutoloadMethod#load(Ruby, String)
             */
            public IRubyObject load(Ruby runtime, String name) {
                loadService.require(file.toString());
                if(recv instanceof RubyModule) {
                    return ((RubyModule)recv).getConstant(baseName);
                }
                return runtime.getObject().getConstant(baseName);
            }
        });
        return recv;
    }

    public static IRubyObject method_missing(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        if (args.length == 0) {
            throw recv.getRuntime().newArgumentError("no id given");
        }

        String name = args[0].asSymbol();
        String description = null;
        if("inspect".equals(name) || "to_s".equals(name)) {
            description = recv.anyToString().toString();
        } else {
            description = recv.inspect().toString();
        }
        boolean noClass = description.length() > 0 && description.charAt(0) == '#';
        ThreadContext tc = runtime.getCurrentContext();
        Visibility lastVis = tc.getLastVisibility();
        if(null == lastVis) {
            lastVis = Visibility.PUBLIC;
        }
        CallType lastCallType = tc.getLastCallType();
        String format = lastVis.errorMessageFormat(lastCallType, name);
        // FIXME: Modify sprintf to accept Object[] as well...
        String msg = Sprintf.sprintf(runtime.newString(format), 
                runtime.newArray(new IRubyObject[] { 
                        runtime.newString(name), 
                        runtime.newString(description),
                        runtime.newString(noClass ? "" : ":"), 
                        runtime.newString(noClass ? "" : recv.getType().getName())
                })).toString();
        
        throw lastCallType == CallType.VARIABLE ? runtime.newNameError(msg, name) : runtime.newNoMethodError(msg, name);
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        recv.checkArgumentCount(args,1,3);
        String arg = args[0].convertToString().toString();

        // Should this logic be pushed into RubyIO Somewhere?
        if (arg.startsWith("|")) {
            String command = arg.substring(1);
            // exec process, create IO with process
            try {
                // TODO: may need to part cli parms out ourself?
                Process p = Runtime.getRuntime().exec(command,getCurrentEnv(recv.getRuntime()));
                RubyIO io = new RubyIO(recv.getRuntime(), p);
                
                if (block.isGiven()) {
                    try {
                        recv.getRuntime().getCurrentContext().yield(io, block);
                        
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

        return ((FileMetaClass) recv.getRuntime().getClass("File")).open(args, block);
    }

    public static IRubyObject gets(IRubyObject recv, IRubyObject[] args) {
        return ((RubyArgsFile) recv.getRuntime().getGlobalVariables().get("$<")).gets(args);
    }

    public static IRubyObject abort(IRubyObject recv, IRubyObject[] args) {
        if(recv.checkArgumentCount(args,0,1) == 1) {
            recv.getRuntime().getGlobalVariables().get("$stderr").callMethod(recv.getRuntime().getCurrentContext(),"puts",args[0]);
        }
        throw new MainExitException(1,true);
    }

    public static IRubyObject new_array(IRubyObject recv, IRubyObject object) {
        IRubyObject value = object.convertToTypeWithCheck("Array", "to_ary");
        
        if (value.isNil()) {
            DynamicMethod method = object.getMetaClass().searchMethod("to_a");
            
            if (method.getImplementationClass() == recv.getRuntime().getKernel()) {
                return recv.getRuntime().newArray(object);
            }
            
            // Strange that Ruby has custom code here and not convertToTypeWithCheck equivalent.
            value = object.callMethod(recv.getRuntime().getCurrentContext(), "to_a");
            if (value.getMetaClass() != recv.getRuntime().getClass("Array")) {
                throw recv.getRuntime().newTypeError("`to_a' did not return Array");
               
            }
        }
        
        return value;
    }
    
    public static IRubyObject new_float(IRubyObject recv, IRubyObject object) {
        if(object instanceof RubyFixnum){
            return RubyFloat.newFloat(object.getRuntime(), ((RubyFixnum)object).getDoubleValue());
        }else if(object instanceof RubyFloat){
            return object;
        }else if(object instanceof RubyBignum){
            return RubyFloat.newFloat(object.getRuntime(), RubyBignum.big2dbl((RubyBignum)object));
        }else if(object instanceof RubyString){
            if(((RubyString)object).getValue().length() == 0){ // rb_cstr_to_dbl case
                throw recv.getRuntime().newArgumentError("invalid value for Float(): " + object.inspect());
            }
            return RubyNumeric.str2fnum(recv.getRuntime(),(RubyString)object,true);
        }else if(object.isNil()){
            throw recv.getRuntime().newTypeError("can't convert nil into Float");
        } else {
            RubyFloat rFloat = object.convertToFloat();
            if(Double.isNaN(rFloat.getDoubleValue())){
                recv.getRuntime().newArgumentError("invalid value for Float()");
        }
            return rFloat;
    }
    }
    
    public static IRubyObject new_integer(IRubyObject recv, IRubyObject object) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        if(object instanceof RubyString) {
            return RubyNumeric.str2inum(recv.getRuntime(),(RubyString)object,0,true);
                    }
        return object.callMethod(context,"to_i");
    }
    
    public static IRubyObject new_string(IRubyObject recv, IRubyObject object) {
        return object.callMethod(recv.getRuntime().getCurrentContext(), "to_s");
    }
    
    
    public static IRubyObject p(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        ThreadContext context = recv.getRuntime().getCurrentContext();

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                defout.callMethod(context, "write", args[i].callMethod(context, "inspect"));
                defout.callMethod(context, "write", recv.getRuntime().newString("\n"));
            }
        }
        return recv.getRuntime().getNil();
    }

    public static IRubyObject puts(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        defout.callMethod(context, "puts", args);

        return recv.getRuntime().getNil();
    }

    public static IRubyObject print(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        ThreadContext context = recv.getRuntime().getCurrentContext();

        defout.callMethod(context, "print", args);

        return recv.getRuntime().getNil();
    }

    public static IRubyObject printf(IRubyObject recv, IRubyObject[] args) {
        if (args.length != 0) {
            IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");

            if (!(args[0] instanceof RubyString)) {
                defout = args[0];
                args = ArgsUtil.popArray(args);
            }

            ThreadContext context = recv.getRuntime().getCurrentContext();

            defout.callMethod(context, "write", RubyKernel.sprintf(recv, args));
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
        return ((RubyArgsFile) recv.getRuntime().getGlobalVariables().get("$<")).readlines(args);
    }

    /** Returns value of $_.
     *
     * @throws TypeError if $_ is not a String or nil.
     * @return value of $_ as String.
     */
    private static RubyString getLastlineString(Ruby runtime) {
        IRubyObject line = runtime.getCurrentContext().getLastline();

        if (line.isNil()) {
            throw runtime.newTypeError("$_ value need to be String (nil given).");
        } else if (!(line instanceof RubyString)) {
            throw runtime.newTypeError("$_ value need to be String (" + line.getMetaClass().getName() + " given).");
        } else {
            return (RubyString) line;
        }
    }

    public static IRubyObject sub_bang(IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(recv.getRuntime()).sub_bang(args, block);
    }

    public static IRubyObject sub(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.sub_bang(args, block).isNil()) {
            recv.getRuntime().getCurrentContext().setLastline(str);
        }

        return str;
    }

    public static IRubyObject gsub_bang(IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(recv.getRuntime()).gsub_bang(args, block);
    }

    public static IRubyObject gsub(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.gsub_bang(args, block).isNil()) {
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

    public static IRubyObject scan(IRubyObject recv, IRubyObject pattern, Block block) {
        return getLastlineString(recv.getRuntime()).scan(pattern, block);
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
        final Ruby runtime = recv.getRuntime();
        RubyArray localVariables = runtime.newArray();
        
        String[] names = runtime.getCurrentContext().getCurrentScope().getAllNamesInScope();
        for (int i = 0; i < names.length; i++) {
            localVariables.append(runtime.newString(names[i]));
        }

        return localVariables;
    }

    public static RubyBinding binding(IRubyObject recv, Block block) {
        // FIXME: Pass block into binding
        return recv.getRuntime().newBinding();
    }

    public static RubyBoolean block_given(IRubyObject recv, Block block) {
        return recv.getRuntime().newBoolean(recv.getRuntime().getCurrentContext().getPreviousFrame().getBlock().isGiven());
    }

    public static IRubyObject sprintf(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw recv.getRuntime().newArgumentError("sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        RubyArray newArgs = recv.getRuntime().newArrayNoCopy(args);
        newArgs.shift();

        return str.format(newArgs);
    }

    public static IRubyObject raise(IRubyObject recv, IRubyObject[] args, Block block) {
        // FIXME: Pass block down?
        recv.checkArgumentCount(args, 0, 3); 
        Ruby runtime = recv.getRuntime();

        if (args.length == 0) {
            IRubyObject lastException = runtime.getGlobalVariables().get("$!");
            if (lastException.isNil()) {
                throw new RaiseException(runtime, runtime.getClass("RuntimeError"), "", false);
            } 
            throw new RaiseException((RubyException) lastException);
        }

        IRubyObject exception;
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        if (args.length == 1) {
            if (args[0] instanceof RubyString) {
                throw new RaiseException((RubyException)runtime.getClass("RuntimeError").newInstance(args, block));
            }
            
            if (!args[0].respondsTo("exception")) {
                throw runtime.newTypeError("exception class/object expected");
            }
            exception = args[0].callMethod(context, "exception");
        } else {
            if (!args[0].respondsTo("exception")) {
                throw runtime.newTypeError("exception class/object expected");
            }
            
            exception = args[0].callMethod(context, "exception", args[1]);
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
    public static IRubyObject require(IRubyObject recv, IRubyObject name, Block block) {
        if (recv.getRuntime().getLoadService().require(name.toString())) {
            return recv.getRuntime().getTrue();
        }
        return recv.getRuntime().getFalse();
    }

    public static IRubyObject load(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString file = args[0].convertToString();
        recv.getRuntime().getLoadService().load(file.toString());
        return recv.getRuntime().getTrue();
    }

    public static IRubyObject eval(IRubyObject recv, IRubyObject[] args, Block block) {
        if (args == null || args.length == 0) {
            throw recv.getRuntime().newArgumentError(args.length, 1);
        }
            
        RubyString src = args[0].convertToString();
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
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        if (scope == null) {
            scope = recv.getRuntime().newBinding();
        }
        
        return recv.evalWithBinding(context, src, scope, file);
    }

    public static IRubyObject caller(IRubyObject recv, IRubyObject[] args, Block block) {
        int level = args.length > 0 ? RubyNumeric.fix2int(args[0]) : 1;

        if (level < 0) {
            throw recv.getRuntime().newArgumentError("negative level(" + level + ')');
        }
        
        return recv.getRuntime().getCurrentContext().createBacktrace(level, false);
    }

    public static IRubyObject rbCatch(IRubyObject recv, IRubyObject tag, Block block) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        try {
            context.pushCatch(tag.asSymbol());
            return context.yield(tag, block);
        } catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.ThrowJump &&
                je.getTarget().equals(tag.asSymbol())) {
                    return (IRubyObject) je.getValue();
            }
            throw je;
        } finally {
            context.popCatch();
        }
    }

    public static IRubyObject rbThrow(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();

        String tag = args[0].asSymbol();
        String[] catches = runtime.getCurrentContext().getActiveCatches();

        String message = "uncaught throw '" + tag + '\'';

        //Ordering of array traversal not important, just intuitive
        for (int i = catches.length - 1 ; i >= 0 ; i--) {
            if (tag.equals(catches[i])) {
                //Catch active, throw for catch to handle
                JumpException je = new JumpException(JumpException.JumpType.ThrowJump);

                je.setTarget(tag);
                je.setValue(args.length > 1 ? args[1] : runtime.getNil());
                throw je;
            }
        }

        //No catch active for this throw
        throw runtime.newNameError(message, tag);
    }

    public static IRubyObject trap(IRubyObject recv, IRubyObject[] args, Block block) {
        // FIXME: We can probably fake some basic signals, but obviously can't do everything. For now, stub.
        return recv.getRuntime().getNil();
    }
    
    public static IRubyObject warn(IRubyObject recv, IRubyObject message) {
        IRubyObject out = recv.getRuntime().getObject().getConstant("STDERR");
        RubyIO io = (RubyIO) out.convertToType("IO", "to_io", true); 

        io.puts(new IRubyObject[] { message });
        return recv.getRuntime().getNil();
    }

    public static IRubyObject set_trace_func(IRubyObject recv, IRubyObject trace_func, Block block) {
        if (trace_func.isNil()) {
            recv.getRuntime().setTraceFunction(null);
        } else if (!(trace_func instanceof RubyProc)) {
            throw recv.getRuntime().newTypeError("trace_func needs to be Proc.");
        } else {
            recv.getRuntime().setTraceFunction((RubyProc) trace_func);
        }
        return trace_func;
    }

    public static IRubyObject singleton_method_added(IRubyObject recv, IRubyObject symbolId, Block block) {
        return recv.getRuntime().getNil();
    }

    public static IRubyObject singleton_method_removed(IRubyObject recv, IRubyObject symbolId, Block block) {
        return recv.getRuntime().getNil();
    }

    public static IRubyObject singleton_method_undefined(IRubyObject recv, IRubyObject symbolId, Block block) {
        return recv.getRuntime().getNil();
    }
    
    
    public static RubyProc proc(IRubyObject recv, Block block) {
        return recv.getRuntime().newProc(true, block);
    }

    public static IRubyObject loop(IRubyObject recv, Block block) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        while (true) {
            try {
                context.yield(recv.getRuntime().getNil(), block);

                Thread.yield();
            } catch (JumpException je) {
                // JRUBY-530, specifically the Kernel#loop case:
                // Kernel#loop always takes a block.  But what we're looking
                // for here is breaking an iteration where the block is one 
                // used inside loop's block, not loop's block itself.  Set the 
                // appropriate flag on the JumpException if this is the case
                // (the FCALLNODE case in EvaluationState will deal with it)
                if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                    if (je.getTarget() != null && je.getTarget() != block) {
                        je.setBreakInKernelLoop(true);
                    }
                }
                 
                throw je;
            }
        }
    }
    public static IRubyObject test(IRubyObject recv, IRubyObject[] args) {
        int cmd = (int) args[0].convertToInteger().getLongValue();
        Ruby runtime = recv.getRuntime();
        File pwd = new File(recv.getRuntime().getCurrentDirectory());
        File file1 = new File(pwd, args[1].toString());
        Calendar calendar;
        switch (cmd) {
        //        ?A  | Time    | Last access time for file1
        //        ?b  | boolean | True if file1 is a block device
        //        ?c  | boolean | True if file1 is a character device
        //        ?C  | Time    | Last change time for file1
        //        ?d  | boolean | True if file1 exists and is a directory
        //        ?e  | boolean | True if file1 exists
        //        ?f  | boolean | True if file1 exists and is a regular file
        case 'f':
            return RubyBoolean.newBoolean(runtime, file1.isFile());
        //        ?g  | boolean | True if file1 has the \CF{setgid} bit
        //            |         | set (false under NT)
        //        ?G  | boolean | True if file1 exists and has a group
        //            |         | ownership equal to the caller's group
        //        ?k  | boolean | True if file1 exists and has the sticky bit set
        //        ?l  | boolean | True if file1 exists and is a symbolic link
        //        ?M  | Time    | Last modification time for file1
        case 'M':
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(file1.lastModified());
            return RubyTime.newTime(runtime, calendar);
        //        ?o  | boolean | True if file1 exists and is owned by
        //            |         | the caller's effective uid
        //        ?O  | boolean | True if file1 exists and is owned by
        //            |         | the caller's real uid
        //        ?p  | boolean | True if file1 exists and is a fifo
        //        ?r  | boolean | True if file1 is readable by the effective
        //            |         | uid/gid of the caller
        //        ?R  | boolean | True if file is readable by the real
        //            |         | uid/gid of the caller
        //        ?s  | int/nil | If file1 has nonzero size, return the size,
        //            |         | otherwise return nil
        //        ?S  | boolean | True if file1 exists and is a socket
        //        ?u  | boolean | True if file1 has the setuid bit set
        //        ?w  | boolean | True if file1 exists and is writable by
        //            |         | the effective uid/gid
        //        ?W  | boolean | True if file1 exists and is writable by
        //            |         | the real uid/gid
        //        ?x  | boolean | True if file1 exists and is executable by
        //            |         | the effective uid/gid
        //        ?X  | boolean | True if file1 exists and is executable by
        //            |         | the real uid/gid
        //        ?z  | boolean | True if file1 exists and has a zero length
        //
        //        Tests that take two files:
        //
        //        ?-  | boolean | True if file1 and file2 are identical
        //        ?=  | boolean | True if the modification times of file1
        //            |         | and file2 are equal
        //        ?<  | boolean | True if the modification time of file1
        //            |         | is prior to that of file2
        //        ?>  | boolean | True if the modification time of file1
        //            |         | is after that of file2
        }
        throw RaiseException.createNativeRaiseException(runtime, 
            new UnsupportedOperationException("test flag " + ((char) cmd) + " is not implemented"));
    }

    public static IRubyObject backquote(IRubyObject recv, IRubyObject aString) {
        Ruby runtime = recv.getRuntime();
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
        
    /**
     * Only run an in-process script if the script name has "ruby", ".rb", or "irb" in the name
     */
    private static boolean shouldRunInProcess(Ruby runtime, String command) {
        if (runtime.getInstanceConfig().isInProcessScriptDisabled()) {
            return false;
        }

        command = command.trim();
        String [] spaceDelimitedTokens = command.split(" ", 2);
        String [] slashDelimitedTokens = spaceDelimitedTokens[0].split("/");
        String finalToken = slashDelimitedTokens[slashDelimitedTokens.length-1];
        return (finalToken.indexOf("ruby") != -1 || finalToken.endsWith(".rb") || finalToken.endsWith("irb"));
    }
    
    private static class InProcessScript extends Thread {
        private String[] argArray;
        private int result;
        private RubyInstanceConfig config;
        
        public InProcessScript(final String[] argArray, final InputStream in, 
                               final OutputStream out, final OutputStream err, final String[] env, final File dir) {
            this.argArray = argArray;
            this.config   = new RubyInstanceConfig() {{
                setInput(in);
                setOutput(new PrintStream(out));
                setError(new PrintStream(err));
                setEnvironment(environmentMap(env));
                setCurrentDirectory(dir.toString());
            }};
        }

        public int getResult() {
            return result;
        }

        public void setResult(int result) {
            this.result = result;
        }
        
        public void run() {
            result = new Main(config).run(argArray);
        }

        private Map environmentMap(String[] env) {
            Map m = new HashMap();
            for (int i = 0; i < env.length; i++) {
                String[] kv = env[i].split("=", 2);
                m.put(kv[0], kv[1]);
            }
            return m;
        }
    }

    public static int runInShell(Ruby runtime, IRubyObject[] rawArgs) {
        return runInShell(runtime,rawArgs,runtime.getOutputStream());
    }

    private static String[] getCurrentEnv(Ruby runtime) {
        Map h = ((RubyHash)runtime.getObject().getConstant("ENV")).getValueMap();
        String[] ret = new String[h.size()];
        int i=0;
        for(Iterator iter = h.entrySet().iterator();iter.hasNext();i++) {
            Map.Entry e = (Map.Entry)iter.next();
            ret[i] = e.getKey().toString() + "=" + e.getValue().toString();
        }
        return ret;
    }

    public static int runInShell(Ruby runtime, IRubyObject[] rawArgs, OutputStream output) {
        OutputStream error = runtime.getErrorStream();
        InputStream input = runtime.getInputStream();
        try {
            String shell = runtime.evalScript("require 'rbconfig'; Config::CONFIG['SHELL']").toString();
            rawArgs[0] = runtime.newString(repairDirSeps(rawArgs[0].toString()));
            Process aProcess = null;
            InProcessScript ipScript = null;
            File pwd = new File(runtime.getCurrentDirectory());
            
            if (shouldRunInProcess(runtime, rawArgs[0].toString())) {
                List args = parseCommandLine(rawArgs);
                String command = (String)args.get(0);

                // snip off ruby or jruby command from list of arguments
                // leave alone if the command is the name of a script
                int startIndex = command.endsWith(".rb") ? 0 : 1;
                if(command.trim().endsWith("irb")) {
                    startIndex = 0;
                    args.set(0,runtime.getJRubyHome() + File.separator + "bin" + File.separator + "jirb");
                }
                String[] argArray = (String[])args.subList(startIndex,args.size()).toArray(new String[0]);
                ipScript = new InProcessScript(argArray, input, output, error, getCurrentEnv(runtime), pwd);
                
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
                aProcess = Runtime.getRuntime().exec(argArray, getCurrentEnv(runtime), pwd);
            } else {
                // execute command directly, no wildcard expansion
                if (rawArgs.length > 1) {
                    String[] argArray = new String[rawArgs.length];
                    for (int i=0;i<rawArgs.length;i++) {
                        argArray[i] = rawArgs[i].toString();
                    }
                    aProcess = Runtime.getRuntime().exec(argArray,getCurrentEnv(runtime), pwd);
                } else {
                    aProcess = Runtime.getRuntime().exec(rawArgs[0].toString(), getCurrentEnv(runtime), pwd);
                }
            }
            
            if (aProcess != null) {
                handleStreams(aProcess,input,output,error);
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
    
    private static void handleStreams(Process p, InputStream in, OutputStream out, OutputStream err) throws IOException {
        InputStream pOut = p.getInputStream();
        InputStream pErr = p.getErrorStream();
        OutputStream pIn = p.getOutputStream();

        boolean done = false;
        int b;
        boolean proc = false;
        while(!done) {
            if(pOut.available() > 0) {
                byte[] input = new byte[pOut.available()];
                if((b = pOut.read(input)) == -1) {
                    done = true;
                } else {
                    out.write(input);
                }
                proc = true;
            }
            if(pErr.available() > 0) {
                byte[] input = new byte[pErr.available()];
                if((b = pErr.read(input)) != -1) {
                    err.write(input);
                }
                proc = true;
            }
            if(in.available() > 0) {
                byte[] input = new byte[in.available()];
                if((b = in.read(input)) != -1) {
                    pIn.write(input);
                }
                proc = true;
            }
            if(!proc) {
                if((b = pOut.read()) == -1) {
                    if((b = pErr.read()) == -1) {
                        done = true;
                    } else {
                        err.write(b);
                    }
                } else {
                    out.write(b);
                }
            }
            proc = false;
        }
        pOut.close();
        pErr.close();
        pIn.close();
    }

    public static RubyInteger srand(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
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
        Ruby runtime = recv.getRuntime();
        int resultCode = runInShell(runtime, args);
        recv.getRuntime().getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, resultCode));
        return runtime.newBoolean(resultCode == 0);
    }
    
    public static RubyArray to_a(IRubyObject recv) {
        recv.getRuntime().getWarnings().warn("default 'to_a' will be obsolete");
        return recv.getRuntime().newArray(recv);
    }
}
