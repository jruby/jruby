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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import org.jruby.anno.JRubyMethod;

import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.IdUtil;
import org.jruby.util.ShellLauncher;
import org.jruby.util.Sprintf;

/**
 * Note: For CVS history, see KernelModule.java.
 *
 * @author jpetersen
 */
public class RubyKernel {
    public final static Class IRUBY_OBJECT = IRubyObject.class;

    public static RubyModule createKernelModule(Ruby runtime) {
        RubyModule module = runtime.defineModule("Kernel");
        runtime.setKernel(module);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyKernel.class);
        CallbackFactory objectCallbackFactory = runtime.callbackFactory(RubyObject.class);

        module.defineAnnotatedMethods(RubyKernel.class);
        module.defineAnnotatedMethods(RubyObject.class);
        
        runtime.setRespondToMethod(module.searchMethod("respond_to?"));
        
        runtime.getObject().dispatcher = objectCallbackFactory.createDispatcher(runtime.getObject());
        module.setFlag(RubyObject.USER7_F, false); //Kernel is the only Module that doesn't need an implementor

        return module;
    }

    @JRubyMethod(name = "at_exit", frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject at_exit(IRubyObject recv, Block block) {
        return recv.getRuntime().pushExitBlock(recv.getRuntime().newProc(Block.Type.PROC, block));
    }

    @JRubyMethod(name = "autoload?", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject autoload_p(final IRubyObject recv, IRubyObject symbol) {
        RubyModule module = recv instanceof RubyModule ? (RubyModule) recv : recv.getRuntime().getObject();
        String name = module.getName() + "::" + symbol.asSymbol();
        
        IAutoloadMethod autoloadMethod = recv.getRuntime().getLoadService().autoloadFor(name);
        if (autoloadMethod == null) return recv.getRuntime().getNil();

        return recv.getRuntime().newString(autoloadMethod.file());
    }

    @JRubyMethod(name = "autoload", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject autoload(final IRubyObject recv, IRubyObject symbol, final IRubyObject file) {
        Ruby runtime = recv.getRuntime(); 
        final LoadService loadService = runtime.getLoadService();
        final String baseName = symbol.asSymbol(); // interned, OK for "fast" methods
        final RubyModule module = recv instanceof RubyModule ? (RubyModule) recv : runtime.getObject();
        String nm = module.getName() + "::" + baseName;
        
        IRubyObject undef = runtime.getUndef();
        IRubyObject existingValue = module.fastFetchConstant(baseName); 
        if (existingValue != null && existingValue != undef) return runtime.getNil();
        
        module.fastStoreConstant(baseName, undef);
        
        loadService.addAutoload(nm, new IAutoloadMethod() {
            public String file() {
                return file.toString();
            }
            /**
             * @see org.jruby.runtime.load.IAutoloadMethod#load(Ruby, String)
             */
            public IRubyObject load(Ruby runtime, String name) {
                boolean required = loadService.require(file.toString());
                
                // File to be loaded by autoload has already been or is being loaded.
                if (!required) return null;
                
                return module.fastGetConstant(baseName);
            }
        });
        return runtime.getNil();
    }

    @JRubyMethod(name = "method_missing", rest = true, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject method_missing(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();

        if (args.length == 0 || !(args[0] instanceof RubySymbol)) throw runtime.newArgumentError("no id given");
        
        String name = args[0].asSymbol();
        ThreadContext context = runtime.getCurrentContext();
        Visibility lastVis = context.getLastVisibility();
        CallType lastCallType = context.getLastCallType();

        String format = null;

        boolean noMethod = true; // NoMethodError

        if (lastVis == Visibility.PRIVATE) {
            format = "private method `%s' called for %s";
        } else if (lastVis == Visibility.PROTECTED) {
            format = "protected method `%s' called for %s";
        } else if (lastCallType == CallType.VARIABLE) {
            format = "undefined local variable or method `%s' for %s";
            noMethod = false; // NameError
        } else if (lastCallType == CallType.SUPER) {
            format = "super: no superclass method `%s'";
        }

        if (format == null) format = "undefined method `%s' for %s";

        String description = null;
        
        if (recv.isNil()) {
            description = "nil";
        } else if (recv instanceof RubyBoolean && recv.isTrue()) {
            description = "true";
        } else if (recv instanceof RubyBoolean && !recv.isTrue()) {
            description = "false";
        } else {
            if (name.equals("inspect") || name.equals("to_s")) {
                description = recv.anyToString().toString();
            } else {
                IRubyObject d;
                try {
                    d = recv.callMethod(context, "inspect");
                    if (d.getMetaClass() == recv.getMetaClass() || (d instanceof RubyString && ((RubyString)d).length().getLongValue() > 65)) {
                        d = recv.anyToString();
                    }
                } catch (JumpException je) {
                    d = recv.anyToString();
                }
                description = d.toString();
            }
        }
        if (description.length() == 0 || (description.length() > 0 && description.charAt(0) != '#')) {
            description = description + ":" + recv.getMetaClass().getRealClass().getName();            
        }
        
        IRubyObject[]exArgs = new IRubyObject[noMethod ? 3 : 2];

        RubyArray arr = runtime.newArray(args[0], runtime.newString(description));
        RubyString msg = runtime.newString(Sprintf.sprintf(runtime.newString(format), arr).toString());
        
        if (recv.isTaint()) msg.setTaint(true);

        exArgs[0] = msg;
        exArgs[1] = args[0];

        RubyClass exc;
        if (noMethod) {
            IRubyObject[]NMEArgs = new IRubyObject[args.length - 1];
            System.arraycopy(args, 1, NMEArgs, 0, NMEArgs.length);
            exArgs[2] = runtime.newArrayNoCopy(NMEArgs);
            exc = runtime.fastGetClass("NoMethodError");
        } else {
            exc = runtime.fastGetClass("NameError");
        }
        
        throw new RaiseException((RubyException)exc.newInstance(exArgs, Block.NULL_BLOCK));
    }

    @JRubyMethod(name = "open", required = 1, optional = 2, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(recv.getRuntime(), args,1,3);
        String arg = args[0].convertToString().toString();
        Ruby runtime = recv.getRuntime();

        if (arg.startsWith("|")) {
            String command = arg.substring(1);
            // exec process, create IO with process
            try {
                Process p = new ShellLauncher(runtime).run(RubyString.newString(runtime,command));
                RubyIO io = new RubyIO(runtime, p);
                
                if (block.isGiven()) {
                    try {
                        block.yield(recv.getRuntime().getCurrentContext(), io);
                        return runtime.getNil();
                    } finally {
                        io.close();
                    }
                }

                return io;
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        } 

        return RubyFile.open(runtime.getFile(), args, block);
    }

    @JRubyMethod(name = "gets", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject gets(IRubyObject recv, IRubyObject[] args) {
        return ((RubyArgsFile) recv.getRuntime().getGlobalVariables().get("$<")).gets(args);
    }

    @JRubyMethod(name = "abort", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject abort(IRubyObject recv, IRubyObject[] args) {
        if(Arity.checkArgumentCount(recv.getRuntime(), args,0,1) == 1) {
            recv.getRuntime().getGlobalVariables().get("$stderr").callMethod(recv.getRuntime().getCurrentContext(),"puts",args[0]);
        }
        throw new MainExitException(1,true);
    }

    @JRubyMethod(name = "Array", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject new_array(IRubyObject recv, IRubyObject object) {
        IRubyObject value = object.checkArrayType();

        if (value.isNil()) {
            if (object.getMetaClass().searchMethod("to_a").getImplementationClass() != recv.getRuntime().getKernel()) {
                value = object.callMethod(recv.getRuntime().getCurrentContext(), MethodIndex.TO_A, "to_a");
                if (!(value instanceof RubyArray)) throw recv.getRuntime().newTypeError("`to_a' did not return Array");
                return value;
            } else {
                return recv.getRuntime().newArray(object);
            }
        }
        return value;
    }

    @JRubyMethod(name = "Float", required = 1, module = true, visibility = Visibility.PRIVATE)
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
            RubyFloat rFloat = (RubyFloat)object.convertToType(recv.getRuntime().getFloat(), MethodIndex.TO_F, "to_f");
            if (Double.isNaN(rFloat.getDoubleValue())) throw recv.getRuntime().newArgumentError("invalid value for Float()");
            return rFloat;
        }
    }

    @JRubyMethod(name = "Integer", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject new_integer(IRubyObject recv, IRubyObject object) {
        if (object instanceof RubyFloat) {
            double val = ((RubyFloat)object).getDoubleValue(); 
            if (val > (double) RubyFixnum.MAX && val < (double) RubyFixnum.MIN) {
                return RubyNumeric.dbl2num(recv.getRuntime(),((RubyFloat)object).getDoubleValue());
            }
        } else if (object instanceof RubyFixnum || object instanceof RubyBignum) {
            return object;
        } else if (object instanceof RubyString) {
            return RubyNumeric.str2inum(recv.getRuntime(),(RubyString)object,0,true);
        }
        
        IRubyObject tmp = object.convertToType(recv.getRuntime().getInteger(), MethodIndex.TO_INT, "to_int", false);
        if (tmp.isNil()) return object.convertToInteger(MethodIndex.TO_I, "to_i");
        return tmp;
    }

    @JRubyMethod(name = "String", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject new_string(IRubyObject recv, IRubyObject object) {
        return object.convertToType(recv.getRuntime().getString(), MethodIndex.TO_S, "to_s");
    }

    @JRubyMethod(name = "p", rest = true, module = true, visibility = Visibility.PRIVATE)
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

    /** rb_f_putc
     */
    @JRubyMethod(name = "putc", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject putc(IRubyObject recv, IRubyObject ch) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        return defout.callMethod(recv.getRuntime().getCurrentContext(), "putc", ch);
    }

    @JRubyMethod(name = "puts", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject puts(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        defout.callMethod(context, "puts", args);

        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "print", rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject print(IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = recv.getRuntime().getGlobalVariables().get("$>");
        ThreadContext context = recv.getRuntime().getCurrentContext();

        defout.callMethod(context, "print", args);

        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "printf", rest = true, module = true, visibility = Visibility.PRIVATE)
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

    @JRubyMethod(name = "readline", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject readline(IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(recv, args);

        if (line.isNil()) {
            throw recv.getRuntime().newEOFError();
        }

        return line;
    }

    @JRubyMethod(name = "readlines", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyArray readlines(IRubyObject recv, IRubyObject[] args) {
        return ((RubyArgsFile) recv.getRuntime().getGlobalVariables().get("$<")).readlines(args);
    }

    /** Returns value of $_.
     *
     * @throws TypeError if $_ is not a String or nil.
     * @return value of $_ as String.
     */
    private static RubyString getLastlineString(Ruby runtime) {
        IRubyObject line = runtime.getCurrentContext().getPreviousFrame().getLastLine();

        if (line.isNil()) {
            throw runtime.newTypeError("$_ value need to be String (nil given).");
        } else if (!(line instanceof RubyString)) {
            throw runtime.newTypeError("$_ value need to be String (" + line.getMetaClass().getName() + " given).");
        } else {
            return (RubyString) line;
        }
    }

    @JRubyMethod(name = "sub!", required = 1, optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject sub_bang(IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(recv.getRuntime()).sub_bang(args, block);
    }

    @JRubyMethod(name = "sub", required = 1, optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject sub(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.sub_bang(args, block).isNil()) {
            recv.getRuntime().getCurrentContext().getPreviousFrame().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "gsub!", required = 1, optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject gsub_bang(IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(recv.getRuntime()).gsub_bang(args, block);
    }

    @JRubyMethod(name = "gsub", required = 1, optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject gsub(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.gsub_bang(args, block).isNil()) {
            recv.getRuntime().getCurrentContext().getPreviousFrame().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "chop!", frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject chop_bang(IRubyObject recv, Block block) {
        return getLastlineString(recv.getRuntime()).chop_bang();
    }

    @JRubyMethod(name = "chop", frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject chop(IRubyObject recv, Block block) {
        RubyString str = getLastlineString(recv.getRuntime());

        if (str.getValue().length() > 0) {
            str = (RubyString) str.dup();
            str.chop_bang();
            recv.getRuntime().getCurrentContext().getPreviousFrame().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "chomp!", optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject chomp_bang(IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(recv.getRuntime()).chomp_bang(args);
    }

    @JRubyMethod(name = "chomp", optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject chomp(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = getLastlineString(recv.getRuntime());
        RubyString dup = (RubyString) str.dup();

        if (dup.chomp_bang(args).isNil()) {
            return str;
        } 

        recv.getRuntime().getCurrentContext().getPreviousFrame().setLastLine(dup);
        return dup;
    }

    @JRubyMethod(name = "split", optional = 2, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject split(IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(recv.getRuntime()).split(args);
    }

    @JRubyMethod(name = "scan", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject scan(IRubyObject recv, IRubyObject pattern, Block block) {
        return getLastlineString(recv.getRuntime()).scan(pattern, block);
    }

    @JRubyMethod(name = "select", required = 1, optional = 3, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject select(IRubyObject recv, IRubyObject[] args) {
        return RubyIO.select_static(recv.getRuntime(), args);
    }

    @JRubyMethod(name = "sleep", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject sleep(IRubyObject recv, IRubyObject[] args) {
        long milliseconds;

        if (args.length == 0) {
            // Zero sleeps forever
            milliseconds = 0;
        } else {
            milliseconds = (long) (args[0].convertToFloat().getDoubleValue() * 1000);
            if (milliseconds < 0) {
                throw recv.getRuntime().newArgumentError("time interval must be positive");
            } else if (milliseconds == 0) {
                // Explicit zero in MRI returns immediately
                return recv.getRuntime().newFixnum(0);
            }
        }
        long startTime = System.currentTimeMillis();
        
        RubyThread rubyThread = recv.getRuntime().getThreadService().getCurrentContext().getThread();
        
        while(milliseconds > 0) {
            long loopStartTime = System.currentTimeMillis();
            try {
                rubyThread.sleep(milliseconds);
            } catch (InterruptedException iExcptn) {
            }
            milliseconds -= (System.currentTimeMillis() - loopStartTime);
        }

        return recv.getRuntime().newFixnum(
                Math.round((System.currentTimeMillis() - startTime) / 1000.0));
    }

    // FIXME: Add at_exit and finalizers to exit, then make exit_bang not call those.
    @JRubyMethod(name = "exit", optional = 1, module = true, visibility = Visibility.PRIVATE)
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

    @JRubyMethod(name = "exit!", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        return exit(recv, args);
    }


    /** Returns an Array with the names of all global variables.
     *
     */
    @JRubyMethod(name = "global_variables", module = true, visibility = Visibility.PRIVATE)
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
    @JRubyMethod(name = "local_variables", module = true, visibility = Visibility.PRIVATE)
    public static RubyArray local_variables(IRubyObject recv) {
        final Ruby runtime = recv.getRuntime();
        RubyArray localVariables = runtime.newArray();
        
        for (String name: runtime.getCurrentContext().getCurrentScope().getAllNamesInScope()) {
            if (IdUtil.isLocal(name)) localVariables.append(runtime.newString(name));
        }

        return localVariables;
    }

    @JRubyMethod(name = "binding", frame = true, module = true, visibility = Visibility.PRIVATE)
    public static RubyBinding binding(IRubyObject recv, Block block) {
        // FIXME: Pass block into binding
        return recv.getRuntime().newBinding();
    }

    @JRubyMethod(name = {"block_given?", "iterator?"}, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static RubyBoolean block_given_p(IRubyObject recv, Block block) {
        return recv.getRuntime().newBoolean(recv.getRuntime().getCurrentContext().getPreviousFrame().getBlock().isGiven());
    }

    @JRubyMethod(name = {"sprintf", "format"}, required = 1, rest = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject sprintf(IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw recv.getRuntime().newArgumentError("sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        RubyArray newArgs = recv.getRuntime().newArrayNoCopy(args);
        newArgs.shift();

        return str.op_format(newArgs);
    }

    @JRubyMethod(name = {"raise", "fail"}, optional = 3, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject raise(IRubyObject recv, IRubyObject[] args, Block block) {
        // FIXME: Pass block down?
        Ruby runtime = recv.getRuntime();

        if (args.length == 0) {
            IRubyObject lastException = runtime.getGlobalVariables().get("$!");
            if (lastException.isNil()) {
                throw new RaiseException(runtime, runtime.fastGetClass("RuntimeError"), "", false);
            } 
            throw new RaiseException((RubyException) lastException);
        }

        IRubyObject exception;
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        if (args.length == 1) {
            if (args[0] instanceof RubyString) {
                throw new RaiseException((RubyException)runtime.fastGetClass("RuntimeError").newInstance(args, block));
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
        
        if (!exception.isKindOf(runtime.fastGetClass("Exception"))) {
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
    @JRubyMethod(name = "require", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject require(IRubyObject recv, IRubyObject name, Block block) {
        if (recv.getRuntime().getLoadService().require(name.toString())) {
            return recv.getRuntime().getTrue();
        }
        return recv.getRuntime().getFalse();
    }

    @JRubyMethod(name = "load", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject load(IRubyObject recv, IRubyObject arg1, Block block) {
        RubyString file = arg1.convertToString();
        recv.getRuntime().getLoadService().load(file.toString());
        return recv.getRuntime().getTrue();
    }

    @JRubyMethod(name = "eval", required = 1, optional = 3, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject eval(IRubyObject recv, IRubyObject[] args, Block block) {
        if (args == null || args.length == 0) {
            throw recv.getRuntime().newArgumentError(args.length, 1);
        }
            
        RubyString src = args[0].convertToString();
        IRubyObject scope = null;
        String file = "(eval)";
        int line = 1;

        if (args.length > 1) {
            if (!args[1].isNil()) {
                scope = args[1];

                org.jruby.lexer.yacc.ISourcePosition pos = ((scope instanceof RubyBinding) ? (RubyBinding)scope : (RubyBinding)((RubyProc)scope).binding()).getBinding().getFrame().getPosition();
                file = pos.getFile();
                line = pos.getEndLine();
            }
            
            if (args.length > 2) {
                file = args[2].toString();
            }

            if(args.length > 3) {
                line = RubyNumeric.fix2int(args[3]) - 1;
            }
        }

        recv.getRuntime().checkSafeString(src);
        ThreadContext context = recv.getRuntime().getCurrentContext();
        
        if (scope == null) {
            scope = RubyBinding.newBindingForEval(recv.getRuntime());
        }
        
        return recv.evalWithBinding(context, src, scope, file, line);
    }

    @JRubyMethod(name = "callcc", frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject callcc(IRubyObject recv, Block block) {
        Ruby runtime = recv.getRuntime();
        runtime.getWarnings().warn("Kernel#callcc: Continuations are not implemented in JRuby and will not work");
        IRubyObject cc = runtime.getContinuation().callMethod(runtime.getCurrentContext(),"new");
        cc.dataWrapStruct(block);
        return block.yield(runtime.getCurrentContext(),cc);
    }

    @JRubyMethod(name = "caller", optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject caller(IRubyObject recv, IRubyObject[] args, Block block) {
        int level = args.length > 0 ? RubyNumeric.fix2int(args[0]) : 1;

        if (level < 0) {
            throw recv.getRuntime().newArgumentError("negative level(" + level + ')');
        }
        
        return ThreadContext.createBacktraceFromFrames(recv.getRuntime(), recv.getRuntime().getCurrentContext().createBacktrace(level, false));
    }

    @JRubyMethod(name = "catch", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject rbCatch(IRubyObject recv, IRubyObject tag, Block block) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        CatchTarget target = new CatchTarget(tag.asSymbol());
        try {
            context.pushCatch(target);
            return block.yield(context, tag);
        } catch (JumpException.ThrowJump tj) {
            if (tj.getTarget() == target) return (IRubyObject) tj.getValue();
            
            throw tj;
        } finally {
            context.popCatch();
        }
    }
    
    public static class CatchTarget implements JumpTarget {
        private final String tag;
        public CatchTarget(String tag) { this.tag = tag; }
        public String getTag() { return tag; }
    }

    @JRubyMethod(name = "throw", required = 1, frame = true, optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject rbThrow(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();

        String tag = args[0].asSymbol();
        ThreadContext context = runtime.getCurrentContext();
        CatchTarget[] catches = context.getActiveCatches();

        String message = "uncaught throw `" + tag + "'";

        // Ordering of array traversal not important, just intuitive
        for (int i = catches.length - 1 ; i >= 0 ; i--) {
            if (tag.equals(catches[i].getTag())) {
                //Catch active, throw for catch to handle
                throw new JumpException.ThrowJump(catches[i], args.length > 1 ? args[1] : runtime.getNil());
            }
        }

        // No catch active for this throw
        throw runtime.newNameError(message, tag);
    }

    @JRubyMethod(name = "trap", required = 1, frame = true, optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject trap(IRubyObject recv, IRubyObject[] args, Block block) {
        recv.getRuntime().getLoadService().require("jsignal");
        return recv.callMethod(recv.getRuntime().getCurrentContext(), "__jtrap", args, CallType.FUNCTIONAL, block);
    }
    
    @JRubyMethod(name = "warn", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject warn(IRubyObject recv, IRubyObject message) {
        Ruby runtime = recv.getRuntime();
        if (!runtime.getVerbose().isNil()) {
            IRubyObject out = runtime.getGlobalVariables().get("$stderr");
            out.callMethod(runtime.getCurrentContext(), "puts", new IRubyObject[] { message });
        }
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "set_trace_func", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
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

    @JRubyMethod(name = "trace_var", required = 1, optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject trace_var(IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) throw recv.getRuntime().newArgumentError(0, 1);
        RubyProc proc = null;
        String var = null;
        
        if (args.length > 1) {
            var = args[0].toString();
        }
        
        if (var.charAt(0) != '$') {
            // ignore if it's not a global var
            return recv.getRuntime().getNil();
        }
        
        if (args.length == 1) {
            proc = RubyProc.newProc(recv.getRuntime(), block, Block.Type.PROC);
        }
        if (args.length == 2) {
            proc = (RubyProc)args[1].convertToType(recv.getRuntime().getProc(), 0, "to_proc", true);
        }
        
        recv.getRuntime().getGlobalVariables().setTraceVar(var, proc);
        
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "untrace_var", required = 1, optional = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject untrace_var(IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) throw recv.getRuntime().newArgumentError(0, 1);
        String var = null;
        
        if (args.length >= 1) {
            var = args[0].toString();
        }
        
        if (var.charAt(0) != '$') {
            // ignore if it's not a global var
            return recv.getRuntime().getNil();
        }
        
        if (args.length > 1) {
            ArrayList success = new ArrayList();
            for (int i = 1; i < args.length; i++) {
                if (recv.getRuntime().getGlobalVariables().untraceVar(var, args[i])) {
                    success.add(args[i]);
                }
            }
            return RubyArray.newArray(recv.getRuntime(), success);
        } else {
            recv.getRuntime().getGlobalVariables().untraceVar(var);
        }
        
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "singleton_method_added", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject singleton_method_added(IRubyObject recv, IRubyObject symbolId, Block block) {
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "singleton_method_removed", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject singleton_method_removed(IRubyObject recv, IRubyObject symbolId, Block block) {
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "singleton_method_undefined", required = 1, frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject singleton_method_undefined(IRubyObject recv, IRubyObject symbolId, Block block) {
        return recv.getRuntime().getNil();
    }
    
    @JRubyMethod(name = {"proc", "lambda"}, frame = true, module = true, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_8)
    public static RubyProc proc(IRubyObject recv, Block block) {
        return recv.getRuntime().newProc(Block.Type.LAMBDA, block);
    }
    
    @JRubyMethod(name = {"lambda"}, frame = true, module = true, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_9)
    public static RubyProc lambda(IRubyObject recv, Block block) {
        return recv.getRuntime().newProc(Block.Type.LAMBDA, block);
    }
    
    @JRubyMethod(name = {"proc"}, frame = true, module = true, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_9)
    public static RubyProc proc_1_9(IRubyObject recv, Block block) {
        return recv.getRuntime().newProc(Block.Type.PROC, block);
    }

    @JRubyMethod(name = "loop", frame = true, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject loop(IRubyObject recv, Block block) {
        ThreadContext context = recv.getRuntime().getCurrentContext();
        while (true) {
            try {
                block.yield(context, recv.getRuntime().getNil());
                
                context.pollThreadEvents();
            } catch (JumpException.BreakJump bj) {
                // JRUBY-530, specifically the Kernel#loop case:
                // Kernel#loop always takes a block.  But what we're looking
                // for here is breaking an iteration where the block is one 
                // used inside loop's block, not loop's block itself.  Set the 
                // appropriate flag on the JumpException if this is the case
                // (the FCALLNODE case in EvaluationState will deal with it)
                if (bj.getTarget() != null && bj.getTarget() != block.getBody()) {
                    bj.setBreakInKernelLoop(true);
                }
                 
                throw bj;
            }
        }
    }
    
    @JRubyMethod(name = "test", required = 2, optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject test(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        if (args.length == 0) {
            // MRI message if no args given
            throw runtime.newArgumentError("wrong number of arguments");
        }
        IRubyObject cmdArg = args[0];
        int cmd;
        if (cmdArg instanceof RubyFixnum) {
            cmd = (int)((RubyFixnum)cmdArg).getLongValue();
        } else if (cmdArg instanceof RubyString &&
                ((RubyString)cmdArg).getByteList().length() > 0) {
            // MRI behavior: use first byte of string value if len > 0
            cmd = ((RubyString)cmdArg).getByteList().charAt(0);
        } else {
            cmd = (int)cmdArg.convertToInteger().getLongValue();
        }
        
        // MRI behavior: raise ArgumentError for 'unknown command' before
        // checking number of args.
        switch(cmd) {
        
        // implemented commands
        case 'C': // ?C  | Time    | Last change time for file1
        case 'd': // ?d  | boolean | True if file1 exists and is a directory
        case 'e': // ?e  | boolean | True if file1 exists
        case 'f':
        case 'M':
        case 's': // ?s  | int/nil | If file1 has nonzero size, return the size,
                  //     |         | otherwise return nil
        case 'z': // ?z  | boolean | True if file1 exists and has a zero length
        case '=': // ?=  | boolean | True if the modification times of file1
                  //     |         | and file2 are equal
        case '<': // ?<  | boolean | True if the modification time of file1
                  //     |         | is prior to that of file2
        case '>': // ?>  | boolean | True if the modification time of file1
                  //     |         | is after that of file2
        case '-': // ?-  | boolean | True if file1 and file2 are identical
            break;

        // unimplemented commands

        // FIXME: obviously, these are mostly unimplemented.  Raising an
        // ArgumentError 'unimplemented command' for them.
        
        case 'A': // ?A  | Time    | Last access time for file1
        case 'b': // ?b  | boolean | True if file1 is a block device
        case 'c': // ?c  | boolean | True if file1 is a character device
        case 'g': // ?g  | boolean | True if file1 has the \CF{setgid} bit
        case 'G': // ?G  | boolean | True if file1 exists and has a group
                  //     |         | ownership equal to the caller's group
        case 'k': // ?k  | boolean | True if file1 exists and has the sticky bit set
        case 'l': // ?l  | boolean | True if file1 exists and is a symbolic link
        case 'o': // ?o  | boolean | True if file1 exists and is owned by
                  //     |         | the caller's effective uid  
        case 'O': // ?O  | boolean | True if file1 exists and is owned by 
                  //     |         | the caller's real uid
        case 'p': // ?p  | boolean | True if file1 exists and is a fifo
        case 'r': // ?r  | boolean | True if file1 is readable by the effective
                  //     |         | uid/gid of the caller
        case 'R': // ?R  | boolean | True if file is readable by the real
                  //     |         | uid/gid of the caller
        case 'S': // ?S  | boolean | True if file1 exists and is a socket
        case 'u': // ?u  | boolean | True if file1 has the setuid bit set
        case 'w': // ?w  | boolean | True if file1 exists and is writable by
        case 'W': // ?W  | boolean | True if file1 exists and is writable by
                  //     |         | the real uid/gid
        case 'x': // ?x  | boolean | True if file1 exists and is executable by
                  //     |         | the effective uid/gid
        case 'X': // ?X  | boolean | True if file1 exists and is executable by
                  //     |         | the real uid/gid
            throw runtime.newArgumentError("unimplemented command ?"+(char)cmd);
        default:
            // matches MRI message
            throw runtime.newArgumentError("unknown command ?"+(char)cmd);
            
        }

        // MRI behavior: now check arg count

        switch(cmd) {
        case '-':
        case '=':
        case '<':
        case '>':
            if (args.length != 3) {
                throw runtime.newArgumentError(args.length,3);
            }
            break;
        default:
            if (args.length != 2) {
                throw runtime.newArgumentError(args.length,2);
            }
            break;
        }
        
        String cwd = runtime.getCurrentDirectory();
        File file1 = org.jruby.util.JRubyFile.create(cwd, args[1].convertToString().toString());
        File file2 = null;
        Calendar calendar = null;
                
        switch (cmd) {
        case 'C': // ?C  | Time    | Last change time for file1
            return runtime.newFixnum(file1.lastModified());
        case 'd': // ?d  | boolean | True if file1 exists and is a directory
            return runtime.newBoolean(file1.isDirectory());
        case 'e': // ?e  | boolean | True if file1 exists
            return runtime.newBoolean(file1.exists());
        case 'f': // ?f  | boolean | True if file1 exists and is a regular file
            return runtime.newBoolean(file1.isFile());
        
        case 'M': // ?M  | Time    | Last modification time for file1
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(file1.lastModified());
            return RubyTime.newTime(runtime, calendar);
        case 's': // ?s  | int/nil | If file1 has nonzero size, return the size,
                  //     |         | otherwise return nil
            long length = file1.length();

            return length == 0 ? runtime.getNil() : runtime.newFixnum(length);
        case 'z': // ?z  | boolean | True if file1 exists and has a zero length
            return runtime.newBoolean(file1.exists() && file1.length() == 0);
        case '=': // ?=  | boolean | True if the modification times of file1
                  //     |         | and file2 are equal
            file2 = org.jruby.util.JRubyFile.create(cwd, args[2].convertToString().toString());
            
            return runtime.newBoolean(file1.lastModified() == file2.lastModified());
        case '<': // ?<  | boolean | True if the modification time of file1
                  //     |         | is prior to that of file2
            file2 = org.jruby.util.JRubyFile.create(cwd, args[2].convertToString().toString());
            
            return runtime.newBoolean(file1.lastModified() < file2.lastModified());
        case '>': // ?>  | boolean | True if the modification time of file1
                  //     |         | is after that of file2
            file2 = org.jruby.util.JRubyFile.create(cwd, args[2].convertToString().toString());
            
            return runtime.newBoolean(file1.lastModified() > file2.lastModified());
        case '-': // ?-  | boolean | True if file1 and file2 are identical
            file2 = org.jruby.util.JRubyFile.create(cwd, args[2].convertToString().toString());

            return runtime.newBoolean(file1.equals(file2));
        default:
            throw new InternalError("unreachable code reached!");
        }
    }

    @JRubyMethod(name = "`", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static IRubyObject backquote(IRubyObject recv, IRubyObject aString) {
        Ruby runtime = recv.getRuntime();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        int resultCode = new ShellLauncher(runtime).runAndWait(new IRubyObject[] {aString}, output);
        
        recv.getRuntime().getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, resultCode));
        
        return RubyString.newString(recv.getRuntime(), output.toByteArray());
    }
    
    @JRubyMethod(name = "srand", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyInteger srand(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        long oldRandomSeed = runtime.getRandomSeed();

        if (args.length > 0) {
            RubyInteger integerSeed = args[0].convertToInteger(MethodIndex.TO_INT, "to_int");
            runtime.setRandomSeed(integerSeed.getLongValue());
        } else {
            // Not sure how well this works, but it works much better than
            // just currentTimeMillis by itself.
            runtime.setRandomSeed(System.currentTimeMillis() ^
              recv.hashCode() ^ runtime.incrementRandomSeedSequence() ^
              runtime.getRandom().nextInt(Math.max(1, Math.abs((int)runtime.getRandomSeed()))));
        }
        runtime.getRandom().setSeed(runtime.getRandomSeed());
        return runtime.newFixnum(oldRandomSeed);
    }

    @JRubyMethod(name = "rand", optional = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyNumeric rand(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        long ceil;
        if (args.length == 0) {
            ceil = 0;
        } else if (args.length == 1) {
            if (args[0] instanceof RubyBignum) {
                byte[] bigCeilBytes = ((RubyBignum) args[0]).getValue().toByteArray();
                BigInteger bigCeil = new BigInteger(bigCeilBytes).abs();
                
                byte[] randBytes = new byte[bigCeilBytes.length];
                runtime.getRandom().nextBytes(randBytes);
                
                BigInteger result = new BigInteger(randBytes).abs().mod(bigCeil);
                
                return new RubyBignum(runtime, result); 
            }
             
            RubyInteger integerCeil = (RubyInteger)RubyKernel.new_integer(recv, args[0]); 
            ceil = Math.abs(integerCeil.getLongValue());
        } else {
            throw runtime.newArgumentError("wrong # of arguments(" + args.length + " for 1)");
        }

        if (ceil == 0) {
            return RubyFloat.newFloat(runtime, runtime.getRandom().nextDouble()); 
        }
        if (ceil > Integer.MAX_VALUE) {
            return runtime.newFixnum(Math.abs(runtime.getRandom().nextLong()) % ceil);
        }
            
        return runtime.newFixnum(runtime.getRandom().nextInt((int) ceil));
    }

    @JRubyMethod(name = {"system","exec"}, required = 1, rest = true, module = true, visibility = Visibility.PRIVATE)
    public static RubyBoolean system(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        int resultCode;
        try {
            resultCode = new ShellLauncher(runtime).runAndWait(args);
        } catch (Exception e) {
            resultCode = 127;
        }
        recv.getRuntime().getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, resultCode));
        return runtime.newBoolean(resultCode == 0);
    }
}
