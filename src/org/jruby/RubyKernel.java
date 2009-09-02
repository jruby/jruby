/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008 Joseph LaFata <joe@quibb.org>
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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.anno.FrameField.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.IdUtil;
import org.jruby.util.ShellLauncher;
import org.jruby.util.TypeConverter;

/**
 * Note: For CVS history, see KernelModule.java.
 */
@JRubyModule(name="Kernel")
public class RubyKernel {
    public final static Class<?> IRUBY_OBJECT = IRubyObject.class;

    public static abstract class MethodMissingMethod extends JavaMethodNBlock {
        public MethodMissingMethod(RubyModule implementationClass) {
            super(implementationClass, Visibility.PRIVATE, CallConfiguration.FrameFullScopeNone);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            try {
                preFrameOnly(context, self, name, block);
                return methodMissing(context, self, clazz, name, args, block);
            } finally {
                postFrameOnly(context);
            }
        }

        public abstract IRubyObject methodMissing(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block);

    }
    public static RubyModule createKernelModule(Ruby runtime) {
        RubyModule module = runtime.defineModule("Kernel");
        runtime.setKernel(module);

        module.defineAnnotatedMethods(RubyKernel.class);
        module.defineAnnotatedMethods(RubyObject.class);
        
        runtime.setRespondToMethod(module.searchMethod("respond_to?"));
        
        module.setFlag(RubyObject.USER7_F, false); //Kernel is the only Module that doesn't need an implementor

        runtime.setPrivateMethodMissing(new MethodMissingMethod(module) {
            @Override
            public IRubyObject methodMissing(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return RubyKernel.methodMissing(context, self, name, PRIVATE, CallType.NORMAL, args, block);
            }
        });

        runtime.setProtectedMethodMissing(new MethodMissingMethod(module) {
            @Override
            public IRubyObject methodMissing(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return RubyKernel.methodMissing(context, self, name, PROTECTED, CallType.NORMAL, args, block);
            }
        });

        runtime.setVariableMethodMissing(new MethodMissingMethod(module) {
            @Override
            public IRubyObject methodMissing(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return RubyKernel.methodMissing(context, self, name, PUBLIC, CallType.VARIABLE, args, block);
            }
        });

        runtime.setSuperMethodMissing(new MethodMissingMethod(module) {
            @Override
            public IRubyObject methodMissing(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return RubyKernel.methodMissing(context, self, name, PUBLIC, CallType.SUPER, args, block);
            }
        });

        runtime.setNormalMethodMissing(new MethodMissingMethod(module) {
            @Override
            public IRubyObject methodMissing(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                return RubyKernel.methodMissing(context, self, name, PUBLIC, CallType.NORMAL, args, block);
            }
        });

        runtime.setDefaultMethodMissing(module.searchMethod("method_missing"));

        return module;
    }

    @JRubyMethod(name = "at_exit", frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject at_exit(ThreadContext context, IRubyObject recv, Block block) {
        return context.getRuntime().pushExitBlock(context.getRuntime().newProc(Block.Type.PROC, block));
    }

    @JRubyMethod(name = "autoload?", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject autoload_p(ThreadContext context, final IRubyObject recv, IRubyObject symbol) {
        Ruby runtime = context.getRuntime();
        RubyModule module = recv instanceof RubyModule ? (RubyModule) recv : runtime.getObject();
        String name = module.getName() + "::" + symbol.asJavaString();
        
        IAutoloadMethod autoloadMethod = runtime.getLoadService().autoloadFor(name);
        if (autoloadMethod == null) return runtime.getNil();

        return runtime.newString(autoloadMethod.file());
    }

    @JRubyMethod(name = "autoload", required = 2, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject autoload(final IRubyObject recv, IRubyObject symbol, final IRubyObject file) {
        Ruby runtime = recv.getRuntime(); 
        final LoadService loadService = runtime.getLoadService();
        String nonInternedName = symbol.asJavaString();
        
        if (!IdUtil.isValidConstantName(nonInternedName)) {
            throw runtime.newNameError("autoload must be constant name", nonInternedName);
        }

        if (!(file instanceof RubyString)) throw runtime.newTypeError(file, runtime.getString());
        RubyString fileString = file.convertToString();
        
        if (fileString.isEmpty()) {
            throw runtime.newArgumentError("empty file name");
        }
        
        final String baseName = symbol.asJavaString().intern(); // interned, OK for "fast" methods
        final RubyModule module = recv instanceof RubyModule ? (RubyModule) recv : runtime.getObject();
        String nm = module.getName() + "::" + baseName;
        
        IRubyObject existingValue = module.fastFetchConstant(baseName); 
        if (existingValue != null && existingValue != RubyObject.UNDEF) return runtime.getNil();
        
        module.fastStoreConstant(baseName, RubyObject.UNDEF);
        
        loadService.addAutoload(nm, new IAutoloadMethod() {
            public String file() {
                return file.toString();
            }
            /**
             * @see org.jruby.runtime.load.IAutoloadMethod#load(Ruby, String)
             */
            public IRubyObject load(Ruby runtime, String name) {
                boolean required = loadService.require(file());
                
                // File to be loaded by autoload has already been or is being loaded.
                if (!required) return null;
                
                return module.fastGetConstant(baseName);
            }
        });
        return runtime.getNil();
    }

    @JRubyMethod(name = "method_missing", rest = true, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject method_missing(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Visibility lastVis = context.getLastVisibility();
        CallType lastCallType = context.getLastCallType();

        if (args.length == 0 || !(args[0] instanceof RubySymbol)) throw context.getRuntime().newArgumentError("no id given");

        return methodMissingDirect(context, recv, (RubySymbol)args[0], lastVis, lastCallType, args, block);
    }

    private static IRubyObject methodMissingDirect(ThreadContext context, IRubyObject recv, RubySymbol symbol, Visibility lastVis, CallType lastCallType, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
        
        // create a lightweight thunk
        IRubyObject msg = new RubyNameError.RubyNameErrorMessage(runtime,
                                                                 recv,
                                                                 symbol,
                                                                 lastVis,
                                                                 lastCallType);
        final IRubyObject[]exArgs;
        final RubyClass exc;
        if (lastCallType != CallType.VARIABLE) {
            exc = runtime.getNoMethodError();
            exArgs = new IRubyObject[]{msg, symbol, RubyArray.newArrayNoCopy(runtime, args, 1)};
        } else {
            exc = runtime.getNameError();
            exArgs = new IRubyObject[]{msg, symbol};
        }

        throw new RaiseException((RubyException)exc.newInstance(context, exArgs, Block.NULL_BLOCK));
    }

    private static IRubyObject methodMissing(ThreadContext context, IRubyObject recv, String name, Visibility lastVis, CallType lastCallType, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
        // TODO: pass this in?
        RubySymbol symbol = runtime.newSymbol(name);

        // create a lightweight thunk
        IRubyObject msg = new RubyNameError.RubyNameErrorMessage(runtime,
                                                                 recv,
                                                                 symbol,
                                                                 lastVis,
                                                                 lastCallType);
        final IRubyObject[]exArgs;
        final RubyClass exc;
        if (lastCallType != CallType.VARIABLE) {
            exc = runtime.getNoMethodError();
            exArgs = new IRubyObject[]{msg, symbol, RubyArray.newArrayNoCopy(runtime, args)};
        } else {
            exc = runtime.getNameError();
            exArgs = new IRubyObject[]{msg, symbol};
        }

        throw new RaiseException((RubyException)exc.newInstance(context, exArgs, Block.NULL_BLOCK));
    }

    @JRubyMethod(name = "open", required = 1, optional = 2, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        String arg = args[0].convertToString().toString();
        Ruby runtime = context.getRuntime();

        if (arg.startsWith("|")) {
            String command = arg.substring(1);
            // exec process, create IO with process
            return RubyIO.popen(context, runtime.getIO(), new IRubyObject[] {runtime.newString(command)}, block);
        } 

        return RubyFile.open(context, runtime.getFile(), args, block);
    }

    @JRubyMethod(name = "getc", module = true, visibility = PRIVATE)
    public static IRubyObject getc(ThreadContext context, IRubyObject recv) {
        context.getRuntime().getWarnings().warn(ID.DEPRECATED_METHOD, "getc is obsolete; use STDIN.getc instead", "getc", "STDIN.getc");
        IRubyObject defin = context.getRuntime().getGlobalVariables().get("$stdin");
        return defin.callMethod(context, "getc");
    }

    @JRubyMethod(name = "gets", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArgsFile.gets(context, context.getRuntime().getGlobalVariables().get("$<"), args);
    }

    @JRubyMethod(name = "abort", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject abort(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if(args.length == 1) {
            context.getRuntime().getGlobalVariables().get("$stderr").callMethod(context,"puts",args[0]);
        }
        throw new MainExitException(1,true);
    }

    @JRubyMethod(name = "Array", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_array(ThreadContext context, IRubyObject recv, IRubyObject object) {
        IRubyObject value = object.checkArrayType();

        if (value.isNil()) {
            if (object.getMetaClass().searchMethod("to_a").getImplementationClass() != context.getRuntime().getKernel()) {
                value = object.callMethod(context, "to_a");
                if (!(value instanceof RubyArray)) throw context.getRuntime().newTypeError("`to_a' did not return Array");
                return value;
            } else {
                return context.getRuntime().newArray(object);
            }
        }
        return value;
    }

    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv) {
        return RuntimeHelpers.invoke(context, context.getRuntime().getComplex(), "convert");
    }
    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return RuntimeHelpers.invoke(context, context.getRuntime().getComplex(), "convert", arg);
    }
    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return RuntimeHelpers.invoke(context, context.getRuntime().getComplex(), "convert", arg0, arg1);
    }
    
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv) {
        return RuntimeHelpers.invoke(context, context.getRuntime().getRational(), "convert");
    }
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return RuntimeHelpers.invoke(context, context.getRuntime().getRational(), "convert", arg);
    }
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return RuntimeHelpers.invoke(context, context.getRuntime().getRational(), "convert", arg0, arg1);
    }

    @JRubyMethod(name = "Float", module = true, visibility = PRIVATE)
    public static RubyFloat new_float(IRubyObject recv, IRubyObject object) {
        if(object instanceof RubyFixnum){
            return RubyFloat.newFloat(object.getRuntime(), ((RubyFixnum)object).getDoubleValue());
        }else if(object instanceof RubyFloat){
            return (RubyFloat)object;
        }else if(object instanceof RubyBignum){
            return RubyFloat.newFloat(object.getRuntime(), RubyBignum.big2dbl((RubyBignum)object));
        }else if(object instanceof RubyString){
            if(((RubyString)object).getByteList().realSize == 0){ // rb_cstr_to_dbl case
                throw recv.getRuntime().newArgumentError("invalid value for Float(): " + object.inspect());
            }
            return RubyNumeric.str2fnum(recv.getRuntime(),(RubyString)object,true);
        }else if(object.isNil()){
            throw recv.getRuntime().newTypeError("can't convert nil into Float");
        } else {
            RubyFloat rFloat = (RubyFloat)TypeConverter.convertToType(object, recv.getRuntime().getFloat(), "to_f");
            if (Double.isNaN(rFloat.getDoubleValue())) throw recv.getRuntime().newArgumentError("invalid value for Float()");
            return rFloat;
        }
    }

    @JRubyMethod(name = "Integer", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_integer(ThreadContext context, IRubyObject recv, IRubyObject object) {
        if (object instanceof RubyFloat) {
            double val = ((RubyFloat)object).getDoubleValue(); 
            if (val > (double) RubyFixnum.MAX && val < (double) RubyFixnum.MIN) {
                return RubyNumeric.dbl2num(context.getRuntime(),((RubyFloat)object).getDoubleValue());
            }
        } else if (object instanceof RubyFixnum || object instanceof RubyBignum) {
            return object;
        } else if (object instanceof RubyString) {
            return RubyNumeric.str2inum(context.getRuntime(),(RubyString)object,0,true);
        }
        
        IRubyObject tmp = TypeConverter.convertToType(object, context.getRuntime().getInteger(), "to_int", false);
        if (tmp.isNil()) return object.convertToInteger("to_i");
        return tmp;
    }

    @JRubyMethod(name = "String", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_string(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return TypeConverter.convertToType(object, context.getRuntime().getString(), "to_s");
    }

    @JRubyMethod(name = "p", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject p(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        IRubyObject defout = runtime.getGlobalVariables().get("$>");

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                defout.callMethod(context, "write", RubyObject.inspect(context, args[i]));
                defout.callMethod(context, "write", runtime.newString("\n"));
            }
        }

        IRubyObject result = runtime.getNil();
        if (runtime.is1_9()) {
            if (args.length == 1) {
                result = args[0];
            } else if (args.length > 1) {
                result = runtime.newArray(args);
            }
        }

        if (defout instanceof RubyFile) {
            ((RubyFile)defout).flush();
        }

        return result;
    }

    /** rb_f_putc
     */
    @JRubyMethod(name = "putc", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject putc(ThreadContext context, IRubyObject recv, IRubyObject ch) {
        IRubyObject defout = context.getRuntime().getGlobalVariables().get("$>");
        
        return RubyIO.putc(context, defout, ch);
    }

    @JRubyMethod(name = "puts", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = context.getRuntime().getGlobalVariables().get("$>");

        return RubyIO.puts(context, defout, args);
    }

    @JRubyMethod(name = "print", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = context.getRuntime().getGlobalVariables().get("$>");

        return RubyIO.print(context, defout, args);
    }

    @JRubyMethod(name = "printf", rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject printf(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length != 0) {
            IRubyObject defout = context.getRuntime().getGlobalVariables().get("$>");

            if (!(args[0] instanceof RubyString)) {
                defout = args[0];
                args = ArgsUtil.popArray(args);
            }

            defout.callMethod(context, "write", RubyKernel.sprintf(recv, args));
        }

        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "readline", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) throw context.getRuntime().newEOFError();

        return line;
    }

    @JRubyMethod(name = "readlines", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArgsFile.readlines(context, context.getRuntime().getGlobalVariables().get("$<"), args);
    }

    /** Returns value of $_.
     *
     * @throws TypeError if $_ is not a String or nil.
     * @return value of $_ as String.
     */
    private static RubyString getLastlineString(ThreadContext context, Ruby runtime) {
        IRubyObject line = context.getCurrentScope().getLastLine(runtime);

        if (line.isNil()) {
            throw runtime.newTypeError("$_ value need to be String (nil given).");
        } else if (!(line instanceof RubyString)) {
            throw runtime.newTypeError("$_ value need to be String (" + line.getMetaClass().getName() + " given).");
        } else {
            return (RubyString) line;
        }
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the one or two-arg versions.
     */
    public static IRubyObject sub_bang(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(context, context.getRuntime()).sub_bang(context, args, block);
    }

    @JRubyMethod(name = "sub!", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject sub_bang(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
        return getLastlineString(context, context.getRuntime()).sub_bang(context, arg0, block);
    }

    @JRubyMethod(name = "sub!", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject sub_bang(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        return getLastlineString(context, context.getRuntime()).sub_bang(context, arg0, arg1, block);
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the one or two-arg versions.
     */
    public static IRubyObject sub(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.getRuntime()).dup();

        if (!str.sub_bang(context, args, block).isNil()) {
            context.getCurrentScope().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "sub", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject sub(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.getRuntime()).dup();

        if (!str.sub_bang(context, arg0, block).isNil()) {
            context.getCurrentScope().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "sub", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject sub(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.getRuntime()).dup();

        if (!str.sub_bang(context, arg0, arg1, block).isNil()) {
            context.getCurrentScope().setLastLine(str);
        }

        return str;
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the one or two-arg versions.
     */
    public static IRubyObject gsub_bang(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(context, context.getRuntime()).gsub_bang(context, args, block);
    }

    @JRubyMethod(name = "gsub!", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject gsub_bang(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
        return getLastlineString(context, context.getRuntime()).gsub_bang(context, arg0, block);
    }

    @JRubyMethod(name = "gsub!", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject gsub_bang(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        return getLastlineString(context, context.getRuntime()).gsub_bang(context, arg0, arg1, block);
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the one or two-arg versions.
     */
    public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.getRuntime()).dup();

        if (!str.gsub_bang(context, args, block).isNil()) {
            context.getCurrentScope().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "gsub", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.getRuntime()).dup();

        if (!str.gsub_bang(context, arg0, block).isNil()) {
            context.getCurrentScope().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "gsub", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.getRuntime()).dup();

        if (!str.gsub_bang(context, arg0, arg1, block).isNil()) {
            context.getCurrentScope().setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(name = "chop!", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject chop_bang(ThreadContext context, IRubyObject recv, Block block) {
        return getLastlineString(context, context.getRuntime()).chop_bang(context);
    }

    @JRubyMethod(name = "chop", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject chop(ThreadContext context, IRubyObject recv, Block block) {
        RubyString str = getLastlineString(context, context.getRuntime());

        if (str.getByteList().realSize > 0) {
            str = (RubyString) str.dup();
            str.chop_bang(context);
            context.getCurrentScope().setLastLine(str);
        }

        return str;
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the zero or one-arg versions.
     */
    public static IRubyObject chomp_bang(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return getLastlineString(context, context.getRuntime()).chomp_bang(args);
    }

    @JRubyMethod(name = "chomp!", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject chomp_bang(ThreadContext context, IRubyObject recv) {
        return getLastlineString(context, context.getRuntime()).chomp_bang(context);
    }

    @JRubyMethod(name = "chomp!", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject chomp_bang(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return getLastlineString(context, context.getRuntime()).chomp_bang(context, arg0);
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the zero or one-arg versions.
     */
    public static IRubyObject chomp(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = getLastlineString(context, context.getRuntime());
        RubyString dup = (RubyString) str.dup();

        if (dup.chomp_bang(args).isNil()) {
            return str;
        } 

        context.getCurrentScope().setLastLine(dup);
        return dup;
    }

    @JRubyMethod(name = "chomp", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject chomp(ThreadContext context, IRubyObject recv) {
        RubyString str = getLastlineString(context, context.getRuntime());
        RubyString dup = (RubyString) str.dup();

        if (dup.chomp_bang(context).isNil()) {
            return str;
        } 

        context.getCurrentScope().setLastLine(dup);
        return dup;
    }

    @JRubyMethod(name = "chomp", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject chomp(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        RubyString str = getLastlineString(context, context.getRuntime());
        RubyString dup = (RubyString) str.dup();

        if (dup.chomp_bang(context, arg0).isNil()) {
            return str;
        } 

        context.getCurrentScope().setLastLine(dup);
        return dup;
    }

    /**
     * Variable arity version for compatibility. Not bound to a Ruby method.
     * 
     * @param context The thread context for the current thread
     * @param recv The receiver of the method (usually a class that has included Kernel)
     * @return
     * @deprecated Use the versions with zero, one, or two args.
     */
    public static IRubyObject split(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(context, context.getRuntime()).split(context, args);
    }

    @JRubyMethod(name = "split", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = {LASTLINE, BACKREF}, compat = CompatVersion.RUBY1_8)
    public static IRubyObject split(ThreadContext context, IRubyObject recv) {
        return getLastlineString(context, context.getRuntime()).split(context);
    }

    @JRubyMethod(name = "split", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = {LASTLINE, BACKREF}, compat = CompatVersion.RUBY1_8)
    public static IRubyObject split(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return getLastlineString(context, context.getRuntime()).split(context, arg0);
    }

    @JRubyMethod(name = "split", frame = true, module = true, visibility = PRIVATE, reads = LASTLINE, writes = {LASTLINE, BACKREF}, compat = CompatVersion.RUBY1_8)
    public static IRubyObject split(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return getLastlineString(context, context.getRuntime()).split(context, arg0, arg1);
    }

    @JRubyMethod(name = "scan", required = 1, frame = true, module = true, visibility = PRIVATE, reads = {LASTLINE, BACKREF}, writes = {LASTLINE, BACKREF}, compat = CompatVersion.RUBY1_8)
    public static IRubyObject scan(ThreadContext context, IRubyObject recv, IRubyObject pattern, Block block) {
        return getLastlineString(context, context.getRuntime()).scan(context, pattern, block);
    }

    @JRubyMethod(name = "select", required = 1, optional = 3, module = true, visibility = PRIVATE)
    public static IRubyObject select(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.select_static(context, context.getRuntime(), args);
    }

    @JRubyMethod(name = "sleep", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject sleep(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        long milliseconds;

        if (args.length == 0) {
            // Zero sleeps forever
            milliseconds = 0;
        } else {
            if (!(args[0] instanceof RubyNumeric)) {
                throw context.getRuntime().newTypeError("can't convert " + args[0].getMetaClass().getName() + "into time interval");
            }
            milliseconds = (long) (args[0].convertToFloat().getDoubleValue() * 1000);
            if (milliseconds < 0) {
                throw context.getRuntime().newArgumentError("time interval must be positive");
            } else if (milliseconds == 0) {
                // Explicit zero in MRI returns immediately
                return context.getRuntime().newFixnum(0);
            }
        }
        long startTime = System.currentTimeMillis();
        
        RubyThread rubyThread = context.getThread();

        // Spurious wakeup-loop
        do {
            long loopStartTime = System.currentTimeMillis();
            try {
                // We break if we know this sleep was explicitly woken up/interrupted
                if (!rubyThread.sleep(milliseconds)) break;
            } catch (InterruptedException iExcptn) {
            }
            milliseconds -= (System.currentTimeMillis() - loopStartTime);
        } while (milliseconds > 0);

        return context.getRuntime().newFixnum(Math.round((System.currentTimeMillis() - startTime) / 1000.0));
    }

    // FIXME: Add at_exit and finalizers to exit, then make exit_bang not call those.
    @JRubyMethod(name = "exit", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject exit(IRubyObject recv, IRubyObject[] args) {
        exit(recv.getRuntime(), args, false);
        return recv.getRuntime().getNil(); // not reached
    }

    @JRubyMethod(name = "exit!", optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        exit(recv.getRuntime(), args, true);
        return recv.getRuntime().getNil(); // not reached
    }
    
    private static void exit(Ruby runtime, IRubyObject[] args, boolean hard) {
        runtime.secure(4);

        int status = 1;
        if (args.length > 0) {
            RubyObject argument = (RubyObject)args[0];
            if (argument instanceof RubyFixnum) {
                status = RubyNumeric.fix2int(argument);
            } else {
                status = argument.isFalse() ? 1 : 0;
            }
        }

        if (hard) {
            throw new MainExitException(status, true);
        } else {
            throw runtime.newSystemExit(status);
        }
    }


    /** Returns an Array with the names of all global variables.
     *
     */
    @JRubyMethod(name = "global_variables", module = true, visibility = PRIVATE)
    public static RubyArray global_variables(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        RubyArray globalVariables = runtime.newArray();

        for (String globalVariableName : runtime.getGlobalVariables().getNames()) {
            globalVariables.append(runtime.newString(globalVariableName));
        }

        return globalVariables;
    }

    /** Returns an Array with the names of all local variables.
     *
     */
    @JRubyMethod(name = "local_variables", module = true, visibility = PRIVATE)
    public static RubyArray local_variables(ThreadContext context, IRubyObject recv) {
        final Ruby runtime = context.getRuntime();
        RubyArray localVariables = runtime.newArray();
        
        for (String name: context.getCurrentScope().getAllNamesInScope()) {
            if (IdUtil.isLocal(name)) localVariables.append(runtime.newString(name));
        }

        return localVariables;
    }

    @JRubyMethod(name = "binding", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_8)
    public static RubyBinding binding(ThreadContext context, IRubyObject recv, Block block) {
        return RubyBinding.newBinding(context.getRuntime(), context.currentBinding(recv));
    }

    @JRubyMethod(name = "binding", module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static RubyBinding binding_1_9(ThreadContext context, IRubyObject recv, Block block) {
        return RubyBinding.newBinding(context.getRuntime(), context.currentBinding());
    }

    @JRubyMethod(name = {"block_given?", "iterator?"}, frame = true, module = true, visibility = PRIVATE)
    public static RubyBoolean block_given_p(ThreadContext context, IRubyObject recv, Block block) {
        return context.getRuntime().newBoolean(context.getPreviousFrame().getBlock().isGiven());
    }


    @Deprecated
    public static IRubyObject sprintf(IRubyObject recv, IRubyObject[] args) {
        return sprintf(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = {"sprintf", "format"}, required = 1, rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject sprintf(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw context.getRuntime().newArgumentError("sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        RubyArray newArgs = context.getRuntime().newArrayNoCopy(args);
        newArgs.shift(context);

        return str.op_format(context, newArgs);
    }

    @JRubyMethod(name = {"raise", "fail"}, optional = 3, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject raise(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // FIXME: Pass block down?
        Ruby runtime = context.getRuntime();

        if (args.length == 0) {
            IRubyObject lastException = runtime.getGlobalVariables().get("$!");
            if (lastException.isNil()) {
                throw new RaiseException(runtime, runtime.getRuntimeError(), "", false);
            } 
            throw new RaiseException((RubyException) lastException);
        }

        IRubyObject exception;
        
        if (args.length == 1) {
            if (args[0] instanceof RubyString) {
                throw new RaiseException((RubyException)runtime.getRuntimeError().newInstance(context, args, block));
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
        
        if (!runtime.fastGetClass("Exception").isInstance(exception)) {
            throw runtime.newTypeError("exception object expected");
        }
        
        if (args.length == 3) {
            ((RubyException) exception).set_backtrace(args[2]);
        }

        if (runtime.getDebug().isTrue()) {
            printExceptionSummary(context, runtime, (RubyException) exception);
        }

        throw new RaiseException((RubyException) exception);
    }

    private static void printExceptionSummary(ThreadContext context, Ruby runtime, RubyException rEx) {
        Frame currentFrame = context.getCurrentFrame();

        String msg = String.format("Exception `%s':%s at %s:%s - %s\n",
                rEx.getMetaClass(), rEx.convertToString().toString(),
                currentFrame.getFile(), currentFrame.getLine() + 1,
                rEx.to_s());

        runtime.getErr().print(msg);
    }

    /**
     * Require.
     * MRI allows to require ever .rb files or ruby extension dll (.so or .dll depending on system).
     * we allow requiring either .rb files or jars.
     * @param recv ruby object used to call require (any object will do and it won't be used anyway).
     * @param name the name of the file to require
     **/
    @JRubyMethod(name = "require", required = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject require(IRubyObject recv, IRubyObject name, Block block) {
        Ruby runtime = recv.getRuntime();
        
        if (runtime.getLoadService().lockAndRequire(name.convertToString().toString())) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    @JRubyMethod(name = "load", required = 1, optional = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject load(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyString file = args[0].convertToString();
        boolean wrap = args.length == 2 ? args[1].isTrue() : false;

        runtime.getLoadService().load(file.getByteList().toString(), wrap);
        
        return runtime.getTrue();
    }

    @JRubyMethod(name = "eval", required = 1, optional = 3, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject eval(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();
        // string to eval
        RubyString src = args[0].convertToString();
        runtime.checkSafeString(src);

        boolean bindingGiven = args.length > 1 && !args[1].isNil();
        Binding binding = bindingGiven ? convertToBinding(args[1]) : context.previousBinding();
        if (args.length > 2) {
            // file given, use it and force it into binding
            binding.setFile(args[2].convertToString().toString());
        } else {
            // file not given
            if (bindingGiven) {
                // binding given, use binding's file
            } else {
                // no binding given, use (eval)
                binding.setFile("(eval)");
            }
        }
        if (args.length > 3) {
            // file given, use it and force it into binding
            binding.setLine((int) args[3].convertToInteger().getLongValue());
        } else {
            // no binding given, use 0 for both
            binding.setLine(0);
        }
        
        return ASTInterpreter.evalWithBinding(context, src, binding);
    }

    private static Binding convertToBinding(IRubyObject scope) {
        if (scope instanceof RubyBinding) {
            return ((RubyBinding)scope).getBinding().clone();
        } else {
            if (scope instanceof RubyProc) {
                return ((RubyProc) scope).getBlock().getBinding().clone();
            } else {
                // bomb out, it's not a binding or a proc
                throw scope.getRuntime().newTypeError("wrong argument type " + scope.getMetaClass() + " (expected Proc/Binding)");
            }
        }
    }

    @JRubyMethod(name = "callcc", frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject callcc(ThreadContext context, IRubyObject recv, Block block) {
        RubyContinuation continuation = new RubyContinuation(context.getRuntime());
        return continuation.enter(context, block);
    }

    public static IRubyObject caller(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int level = args.length > 0 ? RubyNumeric.fix2int(args[0]) : 1;

        if (level < 0) {
            throw context.getRuntime().newArgumentError("negative level(" + level + ')');
        }

        return context.createCallerBacktrace(context.getRuntime(), level);
    }

    @JRubyMethod(name = "caller", optional = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject caller1_9(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int level = args.length > 0 ? RubyNumeric.fix2int(args[0]) : 1;

        if (level < 0) {
            throw context.getRuntime().newArgumentError("negative level(" + level + ')');
        }

        return context.createCallerBacktrace(context.getRuntime(), level);
    }

    @JRubyMethod(name = "catch", required = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject rbCatch(ThreadContext context, IRubyObject recv, IRubyObject tag, Block block) {
        CatchTarget target = new CatchTarget(tag.asJavaString());
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

    @JRubyMethod(name = "throw", required = 1, frame = true, optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject rbThrow(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.getRuntime();

        String tag = args[0].asJavaString();
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
        RubyThread currentThread = context.getThread();
        if (currentThread == runtime.getThreadService().getMainThread()) {
            throw runtime.newNameError(message, tag);
        } else {
            throw runtime.newThreadError(message + " in thread 0x" + Integer.toHexString(RubyInteger.fix2int(currentThread.id())));
        }
    }

    @JRubyMethod(name = "trap", required = 1, frame = true, optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject trap(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        context.getRuntime().getLoadService().require("jsignal");
        return RuntimeHelpers.invoke(context, recv, "__jtrap", args, block);
    }
    
    @JRubyMethod(name = "warn", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject message) {
        Ruby runtime = context.getRuntime();
        
        if (runtime.warningsEnabled()) {
            IRubyObject out = runtime.getGlobalVariables().get("$stderr");
            RuntimeHelpers.invoke(context, out, "puts", message);
        }
        return runtime.getNil();
    }

    @JRubyMethod(name = "set_trace_func", required = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject set_trace_func(ThreadContext context, IRubyObject recv, IRubyObject trace_func, Block block) {
        if (!RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // without full tracing, many events will not fire
            context.getRuntime().getWarnings().warn("set_trace_func will not capture all events without --debug flag");
        }
        
        if (trace_func.isNil()) {
            context.getRuntime().setTraceFunction(null);
        } else if (!(trace_func instanceof RubyProc)) {
            throw context.getRuntime().newTypeError("trace_func needs to be Proc.");
        } else {
            context.getRuntime().setTraceFunction((RubyProc) trace_func);
        }
        return trace_func;
    }

    @JRubyMethod(name = "trace_var", required = 1, optional = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject trace_var(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) throw context.getRuntime().newArgumentError(0, 1);
        RubyProc proc = null;
        String var = args.length > 1 ? args[0].toString() : null;
        // ignore if it's not a global var
        if (var.charAt(0) != '$') return context.getRuntime().getNil();
        if (args.length == 1) proc = RubyProc.newProc(context.getRuntime(), block, Block.Type.PROC);
        if (args.length == 2) {
            proc = (RubyProc)TypeConverter.convertToType(args[1], context.getRuntime().getProc(), "to_proc", true);
        }
        
        context.getRuntime().getGlobalVariables().setTraceVar(var, proc);
        
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "untrace_var", required = 1, optional = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject untrace_var(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) throw context.getRuntime().newArgumentError(0, 1);
        String var = args.length >= 1 ? args[0].toString() : null;

        // ignore if it's not a global var
        if (var.charAt(0) != '$') return context.getRuntime().getNil();
        
        if (args.length > 1) {
            ArrayList<IRubyObject> success = new ArrayList<IRubyObject>();
            for (int i = 1; i < args.length; i++) {
                if (context.getRuntime().getGlobalVariables().untraceVar(var, args[i])) {
                    success.add(args[i]);
                }
            }
            return RubyArray.newArray(context.getRuntime(), success);
        } else {
            context.getRuntime().getGlobalVariables().untraceVar(var);
        }
        
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "singleton_method_added", required = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_added(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "singleton_method_removed", required = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_removed(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "singleton_method_undefined", required = 1, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject singleton_method_undefined(ThreadContext context, IRubyObject recv, IRubyObject symbolId, Block block) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "define_singleton_method", required = 1, optional = 1, module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static IRubyObject define_singleton_method(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) throw context.getRuntime().newArgumentError(0, 1);

        RubyClass singleton_class = recv.getSingletonClass();
        IRubyObject obj = args.length > 1 ?
            singleton_class.define_method(context, args[0], args[1], block) :
            singleton_class.define_method(context, args[0], block);
        return obj;
    }

    @JRubyMethod(name = {"proc", "lambda"}, frame = true, module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_8)
    public static RubyProc proc(ThreadContext context, IRubyObject recv, Block block) {
        return context.getRuntime().newProc(Block.Type.LAMBDA, block);
    }
    
    @Deprecated
    public static RubyProc proc(IRubyObject recv, Block block) {
        return recv.getRuntime().newProc(Block.Type.LAMBDA, block);
    }
    
    @JRubyMethod(name = {"lambda"}, frame = true, module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static RubyProc lambda(ThreadContext context, IRubyObject recv, Block block) {
        return context.getRuntime().newProc(Block.Type.LAMBDA, block);
    }
    
    @JRubyMethod(name = {"proc"}, frame = true, module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static RubyProc proc_1_9(ThreadContext context, IRubyObject recv, Block block) {
        return context.getRuntime().newProc(Block.Type.PROC, block);
    }

    @JRubyMethod(name = {"loop"}, frame = true, module = true, visibility = PRIVATE)
    public static IRubyObject loop_1_9(ThreadContext context, IRubyObject recv, Block block) {
        IRubyObject nil = context.getRuntime().getNil();
        RubyClass stopIteration = context.getRuntime().getStopIteration();
        try {
            while (true) {
                block.yield(context, nil);

                context.pollThreadEvents();
            }
        } catch (RaiseException ex) {
            if (!stopIteration.op_eqq(context, ex.getException()).isTrue()) {
                throw ex;
            }
        }
        return nil;
    }

    @JRubyMethod(name = "test", required = 2, optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject test(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) throw context.getRuntime().newArgumentError("wrong number of arguments");

        int cmd;
        if (args[0] instanceof RubyFixnum) {
            cmd = (int)((RubyFixnum) args[0]).getLongValue();
        } else if (args[0] instanceof RubyString &&
                ((RubyString) args[0]).getByteList().length() > 0) {
            // MRI behavior: use first byte of string value if len > 0
            cmd = ((RubyString) args[0]).getByteList().charAt(0);
        } else {
            cmd = (int) args[0].convertToInteger().getLongValue();
        }
        
        // MRI behavior: raise ArgumentError for 'unknown command' before
        // checking number of args.
        switch(cmd) {
        case 'A': case 'b': case 'c': case 'C': case 'd': case 'e': case 'f': case 'g': case 'G': 
        case 'k': case 'M': case 'l': case 'o': case 'O': case 'p': case 'r': case 'R': case 's':
        case 'S': case 'u': case 'w': case 'W': case 'x': case 'X': case 'z': case '=': case '<':
        case '>': case '-':
            break;
        default:
            throw context.getRuntime().newArgumentError("unknown command ?" + (char) cmd);
        }

        // MRI behavior: now check arg count

        switch(cmd) {
        case '-': case '=': case '<': case '>':
            if (args.length != 3) throw context.getRuntime().newArgumentError(args.length, 3);
            break;
        default:
            if (args.length != 2) throw context.getRuntime().newArgumentError(args.length, 2);
            break;
        }
        
        switch (cmd) {
        case 'A': // ?A  | Time    | Last access time for file1
            return context.getRuntime().newFileStat(args[1].convertToString().toString(), false).atime();
        case 'b': // ?b  | boolean | True if file1 is a block device
            return RubyFileTest.blockdev_p(recv, args[1]);
        case 'c': // ?c  | boolean | True if file1 is a character device
            return RubyFileTest.chardev_p(recv, args[1]);
        case 'C': // ?C  | Time    | Last change time for file1
            return context.getRuntime().newFileStat(args[1].convertToString().toString(), false).ctime();
        case 'd': // ?d  | boolean | True if file1 exists and is a directory
            return RubyFileTest.directory_p(recv, args[1]);
        case 'e': // ?e  | boolean | True if file1 exists
            return RubyFileTest.exist_p(recv, args[1]);
        case 'f': // ?f  | boolean | True if file1 exists and is a regular file
            return RubyFileTest.file_p(recv, args[1]);
        case 'g': // ?g  | boolean | True if file1 has the \CF{setgid} bit
            return RubyFileTest.setgid_p(recv, args[1]);
        case 'G': // ?G  | boolean | True if file1 exists and has a group ownership equal to the caller's group
            return RubyFileTest.grpowned_p(recv, args[1]);
        case 'k': // ?k  | boolean | True if file1 exists and has the sticky bit set
            return RubyFileTest.sticky_p(recv, args[1]);
        case 'M': // ?M  | Time    | Last modification time for file1
            return context.getRuntime().newFileStat(args[1].convertToString().toString(), false).mtime();
        case 'l': // ?l  | boolean | True if file1 exists and is a symbolic link
            return RubyFileTest.symlink_p(recv, args[1]);
        case 'o': // ?o  | boolean | True if file1 exists and is owned by the caller's effective uid
            return RubyFileTest.owned_p(recv, args[1]);
        case 'O': // ?O  | boolean | True if file1 exists and is owned by the caller's real uid 
            return RubyFileTest.rowned_p(recv, args[1]);
        case 'p': // ?p  | boolean | True if file1 exists and is a fifo
            return RubyFileTest.pipe_p(recv, args[1]);
        case 'r': // ?r  | boolean | True if file1 is readable by the effective uid/gid of the caller
            return RubyFileTest.readable_p(recv, args[1]);
        case 'R': // ?R  | boolean | True if file is readable by the real uid/gid of the caller
            // FIXME: Need to implement an readable_real_p in FileTest
            return RubyFileTest.readable_p(recv, args[1]);
        case 's': // ?s  | int/nil | If file1 has nonzero size, return the size, otherwise nil
            return RubyFileTest.size_p(recv, args[1]);
        case 'S': // ?S  | boolean | True if file1 exists and is a socket
            return RubyFileTest.socket_p(recv, args[1]);
        case 'u': // ?u  | boolean | True if file1 has the setuid bit set
            return RubyFileTest.setuid_p(recv, args[1]);
        case 'w': // ?w  | boolean | True if file1 exists and is writable by effective uid/gid
            return RubyFileTest.writable_p(recv, args[1]);
        case 'W': // ?W  | boolean | True if file1 exists and is writable by the real uid/gid
            // FIXME: Need to implement an writable_real_p in FileTest
            return RubyFileTest.writable_p(recv, args[1]);
        case 'x': // ?x  | boolean | True if file1 exists and is executable by the effective uid/gid
            return RubyFileTest.executable_p(recv, args[1]);
        case 'X': // ?X  | boolean | True if file1 exists and is executable by the real uid/gid
            return RubyFileTest.executable_real_p(recv, args[1]);
        case 'z': // ?z  | boolean | True if file1 exists and has a zero length
            return RubyFileTest.zero_p(recv, args[1]);
        case '=': // ?=  | boolean | True if the modification times of file1 and file2 are equal
            return context.getRuntime().newFileStat(args[1].convertToString().toString(), false).mtimeEquals(args[2]);
        case '<': // ?<  | boolean | True if the modification time of file1 is prior to that of file2
            return context.getRuntime().newFileStat(args[1].convertToString().toString(), false).mtimeLessThan(args[2]);
        case '>': // ?>  | boolean | True if the modification time of file1 is after that of file2
            return context.getRuntime().newFileStat(args[1].convertToString().toString(), false).mtimeGreaterThan(args[2]);
        case '-': // ?-  | boolean | True if file1 and file2 are identical
            return RubyFileTest.identical_p(recv, args[1], args[2]);
        default:
            throw new InternalError("unreachable code reached!");
        }
    }

    @JRubyMethod(name = "`", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject backquote(ThreadContext context, IRubyObject recv, IRubyObject aString) {
        Ruby runtime = context.getRuntime();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        RubyString string = aString.convertToString();
        int resultCode = ShellLauncher.runAndWait(runtime, new IRubyObject[] {string}, output);
        
        runtime.getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, resultCode));
        
        return RubyString.newStringNoCopy(runtime, output.toByteArray());
    }
    
    @JRubyMethod(name = "srand", module = true, visibility = PRIVATE)
    public static RubyInteger srand(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();

        // Not sure how well this works, but it works much better than
        // just currentTimeMillis by itself.
        long oldRandomSeed = runtime.getRandomSeed();
        runtime.setRandomSeed(System.currentTimeMillis() ^
               recv.hashCode() ^ runtime.incrementRandomSeedSequence() ^
               runtime.getRandom().nextInt(Math.max(1, Math.abs((int)runtime.getRandomSeed()))));

        runtime.getRandom().setSeed(runtime.getRandomSeed());
        return runtime.newFixnum(oldRandomSeed);
    }
    
    @JRubyMethod(name = "srand", module = true, visibility = PRIVATE)
    public static RubyInteger srand(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RubyInteger integerSeed = arg.convertToInteger("to_int");
        Ruby runtime = context.getRuntime();

        long oldRandomSeed = runtime.getRandomSeed();
        runtime.setRandomSeed(integerSeed.getLongValue());

        runtime.getRandom().setSeed(runtime.getRandomSeed());
        return runtime.newFixnum(oldRandomSeed);
    }

    @JRubyMethod(name = "rand", module = true, visibility = PRIVATE)
    public static RubyNumeric rand(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        return RubyFloat.newFloat(runtime, runtime.getRandom().nextDouble());
    }

    @JRubyMethod(name = "rand", module = true, visibility = PRIVATE)
    public static RubyNumeric rand(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        Random random = runtime.getRandom();

        if (arg instanceof RubyBignum) {
            byte[] bigCeilBytes = ((RubyBignum) arg).getValue().toByteArray();
            BigInteger bigCeil = new BigInteger(bigCeilBytes).abs();
            byte[] randBytes = new byte[bigCeilBytes.length];
            random.nextBytes(randBytes);
            BigInteger result = new BigInteger(randBytes).abs().mod(bigCeil);
            return new RubyBignum(runtime, result); 
        }

        RubyInteger integerCeil = (RubyInteger)RubyKernel.new_integer(context, recv, arg); 
        long ceil = Math.abs(integerCeil.getLongValue());
        if (ceil == 0) return RubyFloat.newFloat(runtime, random.nextDouble());
        if (ceil > Integer.MAX_VALUE) return runtime.newFixnum(Math.abs(random.nextLong()) % ceil);

        return runtime.newFixnum(random.nextInt((int) ceil));
    }

    @JRubyMethod(name = "spawn", required = 1, rest = true, module = true, visibility = PRIVATE, compat = CompatVersion.RUBY1_9)
    public static RubyFixnum spawn(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        long pid = ShellLauncher.runWithoutWait(runtime, args);
        return RubyFixnum.newFixnum(runtime, pid);
    }

    @JRubyMethod(name = "syscall", required = 1, optional = 9, module = true, visibility = PRIVATE)
    public static IRubyObject syscall(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        throw context.getRuntime().newNotImplementedError("Kernel#syscall is not implemented in JRuby");
    }

    @JRubyMethod(name = "system", required = 1, rest = true, module = true, visibility = PRIVATE)
    public static RubyBoolean system(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        int resultCode;
        try {
            resultCode = ShellLauncher.runAndWait(runtime, args);
        } catch (Exception e) {
            resultCode = 127;
        }
        runtime.getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, resultCode));
        return runtime.newBoolean(resultCode == 0);
    }
    
    @JRubyMethod(name = {"exec"}, required = 1, rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject exec(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();

        // This is a fairly specific hack for empty string, but it does the job
        if (args.length == 1 && args[0].convertToString().isEmpty()) {
            throw runtime.newErrnoENOENTError(args[0].convertToString().toString());
        }
        
        int resultCode;
        try {
            // TODO: exec should replace the current process.
            // This could be possible with JNA. 
            resultCode = ShellLauncher.execAndWait(runtime, args);
        } catch (Exception e) {
            throw runtime.newErrnoENOENTError("cannot execute");
        }
        
        exit(runtime, new IRubyObject[] {runtime.newFixnum(resultCode)}, true);

        // not reached
        return runtime.getNil();
    }

    @JRubyMethod(name = "fork", module = true, visibility = PRIVATE)
    public static IRubyObject fork(ThreadContext context, IRubyObject recv, Block block) {
        Ruby runtime = context.getRuntime();
        
        if (!RubyInstanceConfig.FORK_ENABLED) {
            throw runtime.newNotImplementedError("fork is unsafe and disabled by default on JRuby");
        }
        
        if (block.isGiven()) {
            int pid = runtime.getPosix().fork();
            
            if (pid == 0) {
                try {
                    block.yield(context, runtime.getNil());
                } catch (RaiseException re) {
                    if (re.getException() instanceof RubySystemExit) {
                        throw re;
                    }
                    return exit_bang(recv, new IRubyObject[] {RubyFixnum.minus_one(runtime)});
                } catch (Throwable t) {
                    return exit_bang(recv, new IRubyObject[] {RubyFixnum.minus_one(runtime)});
                }
                return exit_bang(recv, new IRubyObject[] {RubyFixnum.zero(runtime)});
            } else {
                return runtime.newFixnum(pid);
            }
        } else {
            int result = runtime.getPosix().fork();
        
            if (result == -1) {
                return runtime.getNil();
            }

            return runtime.newFixnum(result);
        }
    }

    @JRubyMethod(frame = true, module = true)
    public static IRubyObject tap(ThreadContext context, IRubyObject recv, Block block) {
        block.yield(context, recv);
        return recv;
    }

    @JRubyMethod(name = {"to_enum", "enum_for"}, rest = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject to_enum(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        switch (args.length) {
        case 0: return enumeratorize(runtime, recv, "each");
        case 1: return enumeratorize(runtime, recv, args[0].asJavaString());
        case 2: return enumeratorize(runtime, recv, args[0].asJavaString(), args[1]);
        default:
            IRubyObject enumArgs[] = new IRubyObject[args.length - 1];
            System.arraycopy(args, 1, enumArgs, 0, enumArgs.length);
            return enumeratorize(runtime, recv, args[0].asJavaString(), enumArgs);
        }
    }

    @JRubyMethod(name = { "__method__", "__callee__" }, module = true, visibility = PRIVATE)
    public static IRubyObject __method__(ThreadContext context, IRubyObject recv) {
        Frame f = context.getCurrentFrame();
        String name = f != null ? f.getName() : null;
        return name != null ? context.getRuntime().newSymbol(name) : context.getRuntime().getNil();
    }
}
