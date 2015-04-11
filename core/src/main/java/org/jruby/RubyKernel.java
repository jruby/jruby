/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.platform.Platform;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.IdUtil;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.PopenExecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.anno.FrameField.BLOCK;
import static org.jruby.anno.FrameField.FILENAME;
import static org.jruby.anno.FrameField.LASTLINE;
import static org.jruby.anno.FrameField.METHODNAME;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PROTECTED;
import static org.jruby.runtime.Visibility.PUBLIC;
import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.anno.FrameField.*;

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
            return methodMissing(context, self, clazz, name, args, block);
        }

        public abstract IRubyObject methodMissing(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block);

    }
    public static RubyModule createKernelModule(Ruby runtime) {
        RubyModule module = runtime.defineModule("Kernel");
        runtime.setKernel(module);

        module.defineAnnotatedMethods(RubyKernel.class);
        
        module.setFlag(RubyObject.USER7_F, false); //Kernel is the only normal Module that doesn't need an implementor

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

        recacheBuiltinMethods(runtime);

        return module;
    }

    /**
     * Cache built-in versions of several core methods, to improve performance by using identity comparison (==) rather
     * than going ahead with dynamic dispatch.
     *
     * @param runtime
     */
    static void recacheBuiltinMethods(Ruby runtime) {
        RubyModule module = runtime.getKernel();

        runtime.setRespondToMethod(module.searchMethod("respond_to?"));
        runtime.setRespondToMissingMethod(module.searchMethod("respond_to_missing?"));
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject at_exit(ThreadContext context, IRubyObject recv, Block block) {
        return context.runtime.pushExitBlock(context.runtime.newProc(Block.Type.PROC, block));
    }

    @JRubyMethod(name = "autoload?", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject autoload_p(ThreadContext context, final IRubyObject recv, IRubyObject symbol) {
        Ruby runtime = context.runtime;
        final RubyModule module = getModuleForAutoload(runtime, recv);
        String name = symbol.asJavaString();
        
        String file = module.getAutoloadFile(name);
        return (file == null) ? runtime.getNil() : runtime.newString(file);
    }

    @JRubyMethod(required = 2, module = true, visibility = PRIVATE)
    public static IRubyObject autoload(final IRubyObject recv, IRubyObject symbol, IRubyObject file) {
        Ruby runtime = recv.getRuntime(); 
        String nonInternedName = symbol.asJavaString();

        final RubyString fileString = StringSupport.checkEmbeddedNulls(runtime,
                                        RubyFile.get_path(runtime.getCurrentContext(), file));
        
        if (!IdUtil.isValidConstantName(nonInternedName)) {
            throw runtime.newNameError("autoload must be constant name", nonInternedName);
        }

        if (fileString.isEmpty()) throw runtime.newArgumentError("empty file name");
        
        final String baseName = symbol.asJavaString().intern(); // interned, OK for "fast" methods
        final RubyModule module = getModuleForAutoload(runtime, recv);
        
        IRubyObject existingValue = module.fetchConstant(baseName); 
        if (existingValue != null && existingValue != RubyObject.UNDEF) return runtime.getNil();

        module.defineAutoload(baseName, new IAutoloadMethod() {
            @Override
            public String file() {
                return fileString.asJavaString();
            }

            @Override
            public void load(Ruby runtime) {
                if (runtime.getLoadService().autoloadRequire(file())) {
                    // Do not finish autoloading by cyclic autoload 
                    module.finishAutoload(baseName);
                }
            }
        });
        return runtime.getNil();
    }

    private static RubyModule getModuleForAutoload(Ruby runtime, IRubyObject recv) {
        RubyModule module = recv instanceof RubyModule ? (RubyModule) recv : recv.getMetaClass().getRealClass();
        if (module == runtime.getKernel()) {
            // special behavior if calling Kernel.autoload directly
            module = runtime.getObject().getSingletonClass();
        }
        return module;
    }

    public static IRubyObject method_missing(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Visibility lastVis = context.getLastVisibility();
        CallType lastCallType = context.getLastCallType();

        if (args.length == 0 || !(args[0] instanceof RubySymbol)) {
            throw context.runtime.newArgumentError("no id given");
        }

        return methodMissingDirect(context, recv, (RubySymbol)args[0], lastVis, lastCallType, args, block);
    }

    protected static IRubyObject methodMissingDirect(ThreadContext context, IRubyObject recv, RubySymbol symbol, Visibility lastVis, CallType lastCallType, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        
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

    public static IRubyObject methodMissing(ThreadContext context, IRubyObject recv, String name, Visibility lastVis, CallType lastCallType, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
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
    

    private static IRubyObject[] popenArgs(Ruby runtime, String pipedArg, IRubyObject[] args) {
            IRubyObject command = runtime.newString(pipedArg.substring(1));

            if (args.length >= 2) return new IRubyObject[] { command, args[1] };

            return new IRubyObject[] { command };
    }
    
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return open19(context, recv, args, block);
    }

    @JRubyMethod(name = "open", required = 1, optional = 3, module = true, visibility = PRIVATE)
    public static IRubyObject open19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {Ruby runtime = context.runtime;
        //        ID to_open = 0;
        boolean redirect = false;
        int argc = args.length;

        if (argc >= 1) {
            //            CONST_ID(to_open, "to_open");
            if (args[0].respondsTo("to_open")) {
                redirect = true;
            } else {
                IRubyObject tmp = args[0];
                tmp = RubyFile.get_path(context, tmp);
                if (tmp.isNil()) {
                    redirect = true;
                } else {
                    IRubyObject cmd = PopenExecutor.checkPipeCommand(context, tmp);
                    if (!cmd.isNil()) {
                        args[0] = cmd;
                        return PopenExecutor.popen(context, args, runtime.getIO(), block);
                    }
                }
            }
        }
        if (redirect) {
            IRubyObject io = args[0].callMethod(context, "to_open", Arrays.copyOfRange(args, 1, args.length));

            RubyIO.ensureYieldClose(context, io, block);
            return io;
        }
        return RubyIO.open(context, runtime.getFile(), args, block);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject getc(ThreadContext context, IRubyObject recv) {
        context.runtime.getWarnings().warn(ID.DEPRECATED_METHOD, "getc is obsolete; use STDIN.getc instead");
        IRubyObject defin = context.runtime.getGlobalVariables().get("$stdin");
        return defin.callMethod(context, "getc");
    }

    @JRubyMethod(optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArgsFile.gets(context, context.runtime.getArgsFile(), args);
    }

    @JRubyMethod(optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject abort(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        RubyString message = null;
        if(args.length == 1) {
            message = args[0].convertToString();
            runtime.getGlobalVariables().get("$stderr").callMethod(context, "puts", message);
        }
        
        exit(runtime, new IRubyObject[] { runtime.getFalse(), message }, false);
        return runtime.getNil(); // not reached
    }

    // MRI: rb_f_array
    @JRubyMethod(name = "Array", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_array(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return TypeConverter.rb_Array(context, object);
    }

    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv) {
        return Helpers.invoke(context, context.runtime.getComplex(), "convert");
    }
    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return Helpers.invoke(context, context.runtime.getComplex(), "convert", arg);
    }
    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.invoke(context, context.runtime.getComplex(), "convert", arg0, arg1);
    }
    
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv) {
        return Helpers.invoke(context, context.runtime.getRational(), "convert");
    }
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return Helpers.invoke(context, context.runtime.getRational(), "convert", arg);
    }
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return Helpers.invoke(context, context.runtime.getRational(), "convert", arg0, arg1);
    }

    public static RubyFloat new_float(IRubyObject recv, IRubyObject object) {
        return new_float19(recv, object);
    }

    @JRubyMethod(name = "Float", module = true, visibility = PRIVATE)
    public static RubyFloat new_float19(IRubyObject recv, IRubyObject object) {
        Ruby runtime = recv.getRuntime();
        if(object instanceof RubyFixnum){
            return RubyFloat.newFloat(runtime, ((RubyFixnum)object).getDoubleValue());
        } else if (object instanceof RubyFloat) {
            return (RubyFloat)object;
        } else if(object instanceof RubyBignum){
            return RubyFloat.newFloat(runtime, RubyBignum.big2dbl((RubyBignum)object));
        } else if(object instanceof RubyString){
            if(((RubyString) object).getByteList().getRealSize() == 0){ // rb_cstr_to_dbl case
                throw runtime.newArgumentError("invalid value for Float(): " + object.inspect());
            }
            RubyString arg = (RubyString)object;
            if (arg.toString().startsWith("0x")) {
                return ConvertBytes.byteListToInum19(runtime, arg.getByteList(), 16, true).toFloat();
            }
            return RubyNumeric.str2fnum19(runtime, arg,true);
        } else if(object.isNil()){
            throw runtime.newTypeError("can't convert nil into Float");
        } else {
            return (RubyFloat)TypeConverter.convertToType19(object, runtime.getFloat(), "to_f");
        }
    }
    
    @JRubyMethod(name = "Hash", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_hash(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        IRubyObject tmp;
        Ruby runtime = recv.getRuntime();
        if (arg.isNil()) return RubyHash.newHash(runtime);
        tmp = TypeConverter.checkHashType(runtime, arg);
        if (tmp.isNil()) {
            if (arg instanceof RubyArray && ((RubyArray) arg).isEmpty()) {
                return RubyHash.newHash(runtime);
            }
            throw runtime.newTypeError("can't convert " + arg.getMetaClass() + " into Hash");
        }
        return tmp;
    } 

    public static IRubyObject new_integer(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return new_integer19(context, recv, object);
    }

    @JRubyMethod(name = "Integer", module = true, visibility = PRIVATE)
    public static IRubyObject new_integer19(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return newIntegerCommon(context, object, 0);
    }

    @JRubyMethod(name = "Integer", module = true, visibility = PRIVATE)
    public static IRubyObject new_integer19(ThreadContext context, IRubyObject recv, IRubyObject object, IRubyObject base) {
        return newIntegerCommon(context, object, RubyNumeric.num2int(base));
    }

    private static IRubyObject newIntegerCommon(ThreadContext context, IRubyObject object, int bs) {
        return TypeConverter.convertToInteger(context, object, bs);
    }

    public static IRubyObject new_string(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return new_string19(context, recv, object);
    }

    @JRubyMethod(name = "String", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_string19(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return TypeConverter.convertToType19(object, context.runtime.getString(), "to_s");
    }

    // MRI: rb_f_p_internal
    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject p(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int argc = args.length;
        int i;
        IRubyObject ret = context.nil;
        IRubyObject defout = runtime.getGlobalVariables().get("$>");
        IRubyObject defaultRS = context.runtime.getGlobalVariables().getDefaultSeparator();
        boolean defoutWriteBuiltin = defout instanceof RubyIO &&
                defout.getMetaClass().isMethodBuiltin("write");

        for (i=0; i<argc; i++) {
            // pulled out as rb_p in MRI
//            rb_p(argv[i]);
            IRubyObject obj = args[i];
            IRubyObject str = RubyBasicObject.rbInspect(context, obj);
            if (defoutWriteBuiltin) {
                ((RubyIO)defout).write(context, str, true);
                ((RubyIO)defout).write(context, defaultRS, true);
            }
            else {
                RubyIO.write(context, defout, str);
                RubyIO.write(context, defout, defaultRS);
            }
        }
        if (argc == 1) {
            ret = args[0];
        }
        else if (argc > 1) {
            ret = RubyArray.newArray(runtime, args);
        }

        if (defout instanceof RubyIO) {
            ((RubyIO)defout).flush(context);
        }
        return ret;
    }

    @JRubyMethod(required = 1, module = true)
    public static IRubyObject public_method(ThreadContext context, IRubyObject recv, IRubyObject symbol) {
        return recv.getMetaClass().newMethod(recv, symbol.asJavaString(), true, PUBLIC, true, false);
    }

    /** rb_f_putc
     */
    @JRubyMethod(required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject putc(ThreadContext context, IRubyObject recv, IRubyObject ch) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");
        if (recv == defout) {
            return RubyIO.putc(context, recv, ch);
        }
        return defout.callMethod(context, "putc", ch);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts0(context, recv);
        }

        return defout.callMethod(context, "puts");
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts1(context, recv, arg0);
        }

        return defout.callMethod(context, "puts", arg0);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts2(context, recv, arg0, arg1);
        }

        return defout.callMethod(context, "puts", new IRubyObject[]{arg0, arg1});
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts3(context, recv, arg0, arg1, arg2);
        }

        return defout.callMethod(context, "puts", new IRubyObject[]{arg0, arg1, arg2});
    }

    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts(context, recv, args);
        }

        return defout.callMethod(context, "puts", args);
    }

    // rb_f_print
    @JRubyMethod(rest = true, module = true, visibility = PRIVATE, reads = LASTLINE)
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        RubyIO.print(context, context.runtime.getGlobalVariables().get("$>"), args);
        return context.nil;
    }

    // rb_f_printf
    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject printf(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        IRubyObject out;
        int argc = args.length;

        if (argc == 0) return context.nil;
        if (args[0] instanceof RubyString) {
            out = runtime.getGlobalVariables().get("$>");
        }
        else {
            out = args[0];
            args = Arrays.copyOfRange(args, 1, args.length);
            argc--;
        }
        RubyIO.write(context, out, sprintf(context, recv, args));

        return context.nil;
    }

    @JRubyMethod(optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) {
            throw context.runtime.newEOFError();
        }

        return line;
    }

    @JRubyMethod(optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArgsFile.readlines(context, context.runtime.getArgsFile(), args);
    }

    @JRubyMethod(name = "respond_to_missing?", module = true)
    public static IRubyObject respond_to_missing_p(ThreadContext context, IRubyObject recv, IRubyObject symbol) {
        return context.runtime.getFalse();
    }

    @JRubyMethod(name = "respond_to_missing?", module = true)
    public static IRubyObject respond_to_missing_p(ThreadContext context, IRubyObject recv, IRubyObject symbol, IRubyObject isPrivate) {
        return context.runtime.getFalse();
    }

    /** Returns value of $_.
     *
     * @throws TypeError if $_ is not a String or nil.
     * @return value of $_ as String.
     */
    private static RubyString getLastlineString(ThreadContext context, Ruby runtime) {
        IRubyObject line = context.getLastLine();

        if (line.isNil()) {
            throw runtime.newTypeError("$_ value need to be String (nil given).");
        } else if (!(line instanceof RubyString)) {
            throw runtime.newTypeError("$_ value need to be String (" + line.getMetaClass().getName() + " given).");
        } else {
            return (RubyString) line;
        }
    }

    @JRubyMethod(required = 1, optional = 3, module = true, visibility = PRIVATE)
    public static IRubyObject select(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.select(context, recv, args);
    }

    @JRubyMethod(optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject sleep(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        long milliseconds;

        if (args.length == 0) {
            // Zero sleeps forever
            milliseconds = 0;
        } else {
            if (!(args[0] instanceof RubyNumeric)) {
                throw context.runtime.newTypeError("can't convert " + args[0].getMetaClass().getName() + "into time interval");
            }
            milliseconds = (long) (args[0].convertToFloat().getDoubleValue() * 1000);
            if (milliseconds < 0) {
                throw context.runtime.newArgumentError("time interval must be positive");
            } else if (milliseconds == 0) {
                // Explicit zero in MRI returns immediately
                return context.runtime.newFixnum(0);
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

        return context.runtime.newFixnum(Math.round((System.currentTimeMillis() - startTime) / 1000.0));
    }

    // FIXME: Add at_exit and finalizers to exit, then make exit_bang not call those.
    @JRubyMethod(optional = 1, module = true, visibility = PRIVATE)
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
        int status = hard ? 1 : 0;
        String message = null;

        if (args.length > 0) {
            RubyObject argument = (RubyObject) args[0];
            if (argument instanceof RubyBoolean) {
                status = argument.isFalse() ? 1 : 0;
            } else {
                status = RubyNumeric.fix2int(argument);
            }
        }

        if (args.length == 2) {
            if (args[1] instanceof RubyString) {
                message = ((RubyString) args[1]).toString();
            }
        }

        if (hard) {
            if (runtime.getInstanceConfig().isHardExit()) {
                System.exit(status);
            } else {
                throw new MainExitException(status, true);
            }
        } else {
            if (message == null) {
                throw runtime.newSystemExit(status);
            } else {
                throw runtime.newSystemExit(status, message);
            }
        }
    }


    /** Returns an Array with the names of all global variables.
     *
     */
    public static RubyArray global_variables(ThreadContext context, IRubyObject recv) {
        return global_variables19(context, recv);
    }

    // In 1.9, return symbols
    @JRubyMethod(name = "global_variables", module = true, visibility = PRIVATE)
    public static RubyArray global_variables19(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.runtime;
        RubyArray globalVariables = runtime.newArray();

        for (String globalVariableName : runtime.getGlobalVariables().getNames()) {
            globalVariables.append(runtime.newSymbol(globalVariableName));
        }

        return globalVariables;
    }

    /** Returns an Array with the names of all local variables.
     *
     */
    public static RubyArray local_variables(ThreadContext context, IRubyObject recv) {
        return local_variables19(context, recv);
    }

    @JRubyMethod(name = "local_variables", module = true, visibility = PRIVATE)
    public static RubyArray local_variables19(ThreadContext context, IRubyObject recv) {
        final Ruby runtime = context.runtime;
        HashSet<String> encounteredLocalVariables = new HashSet<String>();
        RubyArray allLocalVariables = runtime.newArray();
        DynamicScope currentScope = context.getCurrentScope();

        while (currentScope != null) {
            for (String name : currentScope.getStaticScope().getVariables()) {
                if (IdUtil.isLocal(name) && !encounteredLocalVariables.contains(name)) {
                    allLocalVariables.push(runtime.newSymbol(name));
                    encounteredLocalVariables.add(name);
                }
            }
            currentScope = currentScope.getParentScope();
        }

        return allLocalVariables;
    }
    
    public static RubyBinding binding(ThreadContext context, IRubyObject recv, Block block) {
        return binding19(context, recv, block);
    }
    
    @JRubyMethod(name = "binding", module = true, visibility = PRIVATE,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public static RubyBinding binding19(ThreadContext context, IRubyObject recv, Block block) {
        return RubyBinding.newBinding(context.runtime, context.currentBinding());
    }

    @JRubyMethod(name = {"block_given?", "iterator?"}, module = true, visibility = PRIVATE, reads = BLOCK)
    public static RubyBoolean block_given_p(ThreadContext context, IRubyObject recv) {
        return context.runtime.newBoolean(context.getCurrentFrame().getBlock().isGiven());
    }


    @Deprecated
    public static IRubyObject sprintf(IRubyObject recv, IRubyObject[] args) {
        return sprintf(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = {"sprintf", "format"}, required = 1, rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject sprintf(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw context.runtime.newArgumentError("sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        IRubyObject arg;
        if (args.length == 2 && args[1] instanceof RubyHash) {
            arg = args[1];
        } else {
            RubyArray newArgs = context.runtime.newArrayNoCopy(args);
            newArgs.shift(context);
            arg = newArgs;
        }

        return str.op_format(context, arg);
    }

    @JRubyMethod(name = {"raise", "fail"}, optional = 3, module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject raise(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        int argc = args.length;

        RubyHash opts = extract_raise_opts(context, args);
        if (opts != null) argc--;

        // Check for a Java exception
        ConcreteJavaProxy exception = null;
        switch (argc) {
            case 0:
                if (opts != null) {
                    throw runtime.newArgumentError("only cause is given with no arguments");
                }

                if (runtime.getGlobalVariables().get("$!") instanceof ConcreteJavaProxy) {
                    exception = (ConcreteJavaProxy)runtime.getGlobalVariables().get("$!");
                }
                break;
            case 1:
                if (args.length == 1 && args[0] instanceof ConcreteJavaProxy) {
                    exception = (ConcreteJavaProxy)args[0];
                }
                break;
        }

        if (exception != null) {
            // looks like someone's trying to raise a Java exception. Let them.
            Object maybeThrowable = exception.getObject();

            if (maybeThrowable instanceof Throwable) {
                // yes, we're cheating here.
                Helpers.throwException((Throwable)maybeThrowable);
                return recv; // not reached
            } else {
                throw runtime.newTypeError("can't raise a non-Throwable Java object");
            }
        } else {
            // FIXME: Pass block down?
            RaiseException raise;
            switch (argc) {
                case 0:
                    IRubyObject lastException = runtime.getGlobalVariables().get("$!");
                    if (lastException.isNil()) {
                        raise = new RaiseException(runtime, runtime.getRuntimeError(), "", false);
                    } else {
                        // non RubyException value is allowed to be assigned as $!.
                        raise = new RaiseException((RubyException) lastException);
                    }
                    break;
                case 1:
                    if (args[0] instanceof RubyString) {
                        raise = new RaiseException((RubyException) runtime.getRuntimeError().newInstance(context, args, block));
                    } else {
                        raise = new RaiseException(convertToException(runtime, args[0], null));
                    }
                    break;
                case 2:
                    raise = new RaiseException(convertToException(runtime, args[0], args[1]));
                    break;
                default:
                    raise = new RaiseException(convertToException(runtime, args[0], args[1]), args[2]);
                    break;
            }

            if (runtime.getDebug().isTrue()) {
                printExceptionSummary(context, runtime, raise.getException());
            }

            if (opts != null) {
                IRubyObject cause = opts.op_aref(context, runtime.newSymbol("cause"));
                raise.getException().setCause(cause);
            }

            throw raise;
        }
    }

    private static RubyException convertToException(Ruby runtime, IRubyObject obj, IRubyObject optionalMessage) {
        if (!obj.respondsTo("exception")) {
            throw runtime.newTypeError("exception class/object expected");
        }
        IRubyObject exception;
        if (optionalMessage == null) {
            exception = obj.callMethod(runtime.getCurrentContext(), "exception");
        } else {
            exception = obj.callMethod(runtime.getCurrentContext(), "exception", optionalMessage);
        }
        try {
            return (RubyException) exception;
        } catch (ClassCastException cce) {
            throw runtime.newTypeError("exception object expected");
        }
    }

    private static void printExceptionSummary(ThreadContext context, Ruby runtime, RubyException rEx) {
        RubyStackTraceElement[] elements = rEx.getBacktraceElements();
        RubyStackTraceElement firstElement = elements.length > 0 ? elements[0] : new RubyStackTraceElement("", "", "(empty)", 0, false);
        String msg = String.format("Exception `%s' at %s:%s - %s\n",
                rEx.getMetaClass(),
                firstElement.getFileName(), firstElement.getLineNumber(),
                TypeConverter.convertToType(rEx, runtime.getString(), "to_s"));

        runtime.getErrorStream().print(msg);
    }

    private static RubyHash extract_raise_opts(ThreadContext context, IRubyObject[] args) {
        int i;
        if (args.length > 0) {
            IRubyObject opt = args[args.length-1];
            if (opt instanceof RubyHash) {
                if (!((RubyHash)opt).isEmpty()) {
                    if (!((RubyHash)opt).op_aref(context, context.runtime.newSymbol("cause")).isNil()) {
                        return (RubyHash) opt;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Require.
     * MRI allows to require ever .rb files or ruby extension dll (.so or .dll depending on system).
     * we allow requiring either .rb files or jars.
     * @param recv ruby object used to call require (any object will do and it won't be used anyway).
     * @param name the name of the file to require
     **/
    public static IRubyObject require(IRubyObject recv, IRubyObject name, Block block) {
        return require19(recv.getRuntime().getCurrentContext(), recv, name, block);
    }

    @JRubyMethod(name = "require", module = true, visibility = PRIVATE)
    public static IRubyObject require19(ThreadContext context, IRubyObject recv, IRubyObject name, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject tmp = name.checkStringType();
        
        if (!tmp.isNil()) return requireCommon(runtime, recv, tmp, block);

        return requireCommon(runtime, recv, RubyFile.get_path(context, name), block);
    }

    private static IRubyObject requireCommon(Ruby runtime, IRubyObject recv, IRubyObject name, Block block) {
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, name);
        return runtime.newBoolean(runtime.getLoadService().require(path.toString()));
    }

    public static IRubyObject load(IRubyObject recv, IRubyObject[] args, Block block) {
        return load19(recv.getRuntime().getCurrentContext(), recv, args, block);
    }

    @JRubyMethod(name = "load", required = 1, optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject load19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, args[0]));
        return loadCommon(path, runtime, args, block);
    }

    private static IRubyObject loadCommon(IRubyObject fileName, Ruby runtime, IRubyObject[] args, Block block) {
        RubyString file = fileName.convertToString();

        boolean wrap = args.length == 2 ? args[1].isTrue() : false;

        runtime.getLoadService().load(file.toString(), wrap);

        return runtime.getTrue();
    }

    public static IRubyObject eval(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return eval19(context, recv, args, block);
    }

    @JRubyMethod(name = "eval", required = 1, optional = 3, module = true, visibility = PRIVATE,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public static IRubyObject eval19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return evalCommon(context, recv, args, evalBinding19);
    }

    private static IRubyObject evalCommon(ThreadContext context, IRubyObject recv, IRubyObject[] args, EvalBinding evalBinding) {
        // string to eval
        RubyString src = args[0].convertToString();

        boolean bindingGiven = args.length > 1 && !args[1].isNil();
        Binding binding = bindingGiven ? evalBinding.convertToBinding(args[1]) : context.currentBinding();

        if (args.length > 2) {
            // file given, use it and force it into binding
            binding.setFile(args[2].convertToString().toString());

            if (args.length > 3) {
                // line given, use it and force it into binding
                // -1 because parser uses zero offsets and other code compensates
                binding.setLine(((int) args[3].convertToInteger().getLongValue()) - 1);
            } else {
                // filename given, but no line, start from the beginning.
                binding.setLine(0);
            }
        } else if (!bindingGiven) {  // no binding given, use (eval) and start from first line.
            binding.setFile("(eval)");
            binding.setLine(0);
        }

        // set method to current frame's, which should be caller's
        String frameName = context.getFrameName();
        if (frameName != null) binding.setMethod(frameName);

        if (bindingGiven) recv = binding.getSelf();

        return Interpreter.evalWithBinding(context, recv, src, binding);
    }

    private static abstract class EvalBinding {
        public abstract Binding convertToBinding(IRubyObject scope);
    }

    private static EvalBinding evalBinding19 = new EvalBinding() {
        @Override
        public Binding convertToBinding(IRubyObject scope) {
            if (scope instanceof RubyBinding) {
                return ((RubyBinding)scope).getBinding().cloneForEval();
            } else {
                throw scope.getRuntime().newTypeError("wrong argument type " + scope.getMetaClass() + " (expected binding)");
            }
        }
    };

    public static IRubyObject caller(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return caller20(context, recv, args, block);
    }
    
    public static IRubyObject caller19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return caller20(context, recv, args, block);
    }

    @JRubyMethod(name = "caller", optional = 2, module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller20(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = context.runtime;
        Integer[] ll = levelAndLengthFromArgs(runtime, args, 1);
        Integer level = ll[0], length = ll[1];

        return context.createCallerBacktrace(level, length, Thread.currentThread().getStackTrace());
    }
    
    @JRubyMethod(optional = 2, module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller_locations(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        Integer[] ll = levelAndLengthFromArgs(runtime, args, 1);
        Integer level = ll[0], length = ll[1];
        
        return context.createCallerLocations(level, length, Thread.currentThread().getStackTrace());
    }
    
    static Integer[] levelAndLengthFromArgs(Ruby runtime, IRubyObject[] args, int defaultLevel) {
        int level;
        Integer length = null;
        if (args.length > 1) {
            level = RubyNumeric.fix2int(args[0]);
            length = RubyNumeric.fix2int(args[1]);
        } else if (args.length > 0 && args[0] instanceof RubyRange) {
            RubyRange range = (RubyRange) args[0];
            ThreadContext context = runtime.getCurrentContext();
            level = RubyNumeric.fix2int(range.first(context));
            length = RubyNumeric.fix2int(range.last(context)) - level;
            if (!range.exclude_end_p().isTrue()){
                length++;
            }
            length = length < 0 ? 0 : length;
        } else if (args.length > 0) {
            level = RubyNumeric.fix2int(args[0]);
        } else {
            level = defaultLevel;
        }

        if (level < 0) {
            throw runtime.newArgumentError("negative level (" + level + ')');
        }
        if (length != null && length < 0) {
            throw runtime.newArgumentError("negative size (" + length + ')');
        }
        
        return new Integer[] {level, length};
    }

    public static IRubyObject rbCatch(ThreadContext context, IRubyObject recv, IRubyObject tag, Block block) {
        return rbCatch19(context, recv, tag, block);
    }

    @JRubyMethod(name = "catch", module = true, visibility = PRIVATE)
    public static IRubyObject rbCatch19(ThreadContext context, IRubyObject recv, Block block) {
        IRubyObject tag = new RubyObject(context.runtime.getObject());
        return rbCatch19Common(context, tag, block);
    }

    @JRubyMethod(name = "catch", module = true, visibility = PRIVATE)
    public static IRubyObject rbCatch19(ThreadContext context, IRubyObject recv, IRubyObject tag, Block block) {
        return rbCatch19Common(context, tag, block);
    }

    private static IRubyObject rbCatch19Common(ThreadContext context, IRubyObject tag, Block block) {
        RubyContinuation rbContinuation = new RubyContinuation(context.runtime, tag);
        try {
            context.pushCatch(rbContinuation.getContinuation());
            return rbContinuation.enter(context, tag, block);
        } finally {
            context.popCatch();
        }
    }

    public static IRubyObject rbThrow(ThreadContext context, IRubyObject recv, IRubyObject tag, Block block) {
        return rbThrow19(context, recv, tag, block);
    }

    public static IRubyObject rbThrow(ThreadContext context, IRubyObject recv, IRubyObject tag, IRubyObject arg, Block block) {
        return rbThrow19(context, recv, tag, arg, block);
    }

    @JRubyMethod(name = "throw", module = true, visibility = PRIVATE)
    public static IRubyObject rbThrow19(ThreadContext context, IRubyObject recv, IRubyObject tag, Block block) {
        return rbThrowInternal(context, tag, IRubyObject.NULL_ARRAY, block, uncaught19);
    }

    @JRubyMethod(name = "throw", module = true, visibility = PRIVATE)
    public static IRubyObject rbThrow19(ThreadContext context, IRubyObject recv, IRubyObject tag, IRubyObject arg, Block block) {
        return rbThrowInternal(context, tag, new IRubyObject[] {arg}, block, uncaught19);
    }

    private static IRubyObject rbThrowInternal(ThreadContext context, IRubyObject tag, IRubyObject[] args, Block block, Uncaught uncaught) {
        Ruby runtime = context.runtime;
        runtime.getGlobalVariables().set("$!", runtime.getNil());

        RubyContinuation.Continuation continuation = context.getActiveCatch(tag);

        if (continuation != null) {
            continuation.args = args;
            throw continuation;
        }

        // No catch active for this throw
        String message;
        if (tag instanceof RubyString) {
            message = "uncaught throw `" + tag + "'";
        } else {
            message = "uncaught throw " + tag.inspect();
        }
        RubyThread currentThread = context.getThread();

        if (currentThread == runtime.getThreadService().getMainThread()) {
            throw uncaught.uncaughtThrow(runtime, message, tag);
        } else {
            message += " in thread 0x" + Integer.toHexString(RubyInteger.fix2int(currentThread.id()));
            throw runtime.newArgumentError(message);
        }
    }

    private static abstract class Uncaught {
        public abstract RaiseException uncaughtThrow(Ruby runtime, String message, IRubyObject tag);
    }

    private static final Uncaught uncaught19 = new Uncaught() {
        @Override
        public RaiseException uncaughtThrow(Ruby runtime, String message, IRubyObject tag) {
            return runtime.newArgumentError(message);
        }
    };
    
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject message) {
        Ruby runtime = context.runtime;
        
        if (runtime.warningsEnabled()) {
            IRubyObject out = runtime.getGlobalVariables().get("$stderr");
            Helpers.invoke(context, out, "write", message);
            Helpers.invoke(context, out, "write", runtime.getGlobalVariables().getDefaultSeparator());
        }
        return runtime.getNil();
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject set_trace_func(ThreadContext context, IRubyObject recv, IRubyObject trace_func, Block block) {
        if (trace_func.isNil()) {
            context.runtime.setTraceFunction(null);
        } else if (!(trace_func instanceof RubyProc)) {
            throw context.runtime.newTypeError("trace_func needs to be Proc.");
        } else {
            context.runtime.setTraceFunction((RubyProc) trace_func);
        }
        return trace_func;
    }

    @JRubyMethod(required = 1, optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject trace_var(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyProc proc = null;
        String var = args[0].toString();
        // ignore if it's not a global var
        if (var.charAt(0) != '$') {
            return context.runtime.getNil();
        }
        if (args.length == 1) {
            proc = RubyProc.newProc(context.runtime, block, Block.Type.PROC);
        }
        if (args.length == 2) {
            proc = (RubyProc)TypeConverter.convertToType(args[1], context.runtime.getProc(), "to_proc", true);
        }

        context.runtime.getGlobalVariables().setTraceVar(var, proc);

        return context.runtime.getNil();
    }

    @JRubyMethod(required = 1, optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject untrace_var(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) {
            throw context.runtime.newArgumentError(0, 1);
        }
        String var = args[0].toString();

        // ignore if it's not a global var
        if (var.charAt(0) != '$') {
            return context.runtime.getNil();
        }
        
        if (args.length > 1) {
            ArrayList<IRubyObject> success = new ArrayList<IRubyObject>();
            for (int i = 1; i < args.length; i++) {
                if (context.runtime.getGlobalVariables().untraceVar(var, args[i])) {
                    success.add(args[i]);
                }
            }
            return RubyArray.newArray(context.runtime, success);
        } else {
            context.runtime.getGlobalVariables().untraceVar(var);
        }

        return context.runtime.getNil();
    }

    @JRubyMethod(required = 1, optional = 1)
    public static IRubyObject define_singleton_method(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) {
            throw context.runtime.newArgumentError(0, 1);
        }

        RubyClass singleton_class = recv.getSingletonClass();
        if (args.length > 1) {
            IRubyObject arg1 = args[1];
            if (context.runtime.getUnboundMethod().isInstance(args[1])) {
                RubyUnboundMethod method = (RubyUnboundMethod)arg1;
                RubyModule owner = (RubyModule)method.owner(context);
                if (owner.isSingleton() &&
                    !(recv.getMetaClass().isSingleton() && recv.getMetaClass().isKindOfModule(owner))) {

                    throw context.runtime.newTypeError("can't bind singleton method to a different class");
                }
            }
            return singleton_class.define_method(context, args[0], args[1], block);
        } else {
            return singleton_class.define_method(context, args[0], block);
        }
    }

    public static RubyProc proc(ThreadContext context, IRubyObject recv, Block block) {
        return proc_1_9(context, recv, block);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static RubyProc lambda(ThreadContext context, IRubyObject recv, Block block) {
        return context.runtime.newProc(Block.Type.LAMBDA, block);
    }
    
    @JRubyMethod(name = "proc", module = true, visibility = PRIVATE)
    public static RubyProc proc_1_9(ThreadContext context, IRubyObject recv, Block block) {
        return context.runtime.newProc(Block.Type.PROC, block);
    }

    @JRubyMethod(name = "loop", module = true, visibility = PRIVATE)
    public static IRubyObject loop(ThreadContext context, IRubyObject recv, Block block) {
        Ruby runtime = context.runtime;
        if (!block.isGiven()) {
            return RubyEnumerator.enumeratorizeWithSize(context, recv, "loop", loopSizeFn(context));
        }
        IRubyObject nil = runtime.getNil();
        RubyClass stopIteration = runtime.getStopIteration();
        IRubyObject oldExc = runtime.getGlobalVariables().get("$!"); // Save $!
        try {
            while (true) {
                block.yieldSpecific(context);

                context.pollThreadEvents();
            }
        } catch (RaiseException ex) {
            if (!stopIteration.op_eqq(context, ex.getException()).isTrue()) {
                throw ex;
            } else {
                runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
            }
        }
        return nil;
    }

    private static SizeFn loopSizeFn(final ThreadContext context) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return RubyFloat.newFloat(context.runtime, RubyFloat.INFINITY);
            }
        };
    }

    @JRubyMethod(required = 2, optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject test(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) {
            throw context.runtime.newArgumentError("wrong number of arguments");
        }

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
            throw context.runtime.newArgumentError("unknown command ?" + (char) cmd);
        }

        // MRI behavior: now check arg count

        switch(cmd) {
        case '-': case '=': case '<': case '>':
            if (args.length != 3) {
                throw context.runtime.newArgumentError(args.length, 3);
            }
            break;
        default:
            if (args.length != 2) {
                throw context.runtime.newArgumentError(args.length, 2);
            }
            break;
        }
        
        switch (cmd) {
        case 'A': // ?A  | Time    | Last access time for file1
            return context.runtime.newFileStat(args[1].convertToString().toString(), false).atime();
        case 'b': // ?b  | boolean | True if file1 is a block device
            return RubyFileTest.blockdev_p(recv, args[1]);
        case 'c': // ?c  | boolean | True if file1 is a character device
            return RubyFileTest.chardev_p(recv, args[1]);
        case 'C': // ?C  | Time    | Last change time for file1
            return context.runtime.newFileStat(args[1].convertToString().toString(), false).ctime();
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
            return context.runtime.newFileStat(args[1].convertToString().toString(), false).mtime();
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
            return context.runtime.newFileStat(args[1].convertToString().toString(), false).mtimeEquals(args[2]);
        case '<': // ?<  | boolean | True if the modification time of file1 is prior to that of file2
            return context.runtime.newFileStat(args[1].convertToString().toString(), false).mtimeLessThan(args[2]);
        case '>': // ?>  | boolean | True if the modification time of file1 is after that of file2
            return context.runtime.newFileStat(args[1].convertToString().toString(), false).mtimeGreaterThan(args[2]);
        case '-': // ?-  | boolean | True if file1 and file2 are identical
            return RubyFileTest.identical_p(recv, args[1], args[2]);
        default:
            throw new InternalError("unreachable code reached!");
        }
    }

    @JRubyMethod(name = "`", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject backquote(ThreadContext context, IRubyObject recv, IRubyObject str) {
        Ruby runtime = context.runtime;

        if (runtime.getPosix().isNative() && !Platform.IS_WINDOWS) {
            IRubyObject port;
            IRubyObject result;
            OpenFile fptr;

            str = str.convertToString();
            context.setLastExitStatus(context.nil);
            port = PopenExecutor.pipeOpen(context, str, "r", OpenFile.READABLE|OpenFile.TEXTMODE, null);
            if (port.isNil()) return RubyString.newEmptyString(runtime);

            fptr = ((RubyIO)port).getOpenFileChecked();
            result = fptr.readAll(context, fptr.remainSize(), context.nil);
            ((RubyIO)port).rbIoClose(runtime);

            return result;
        }

        RubyString string = str.convertToString();
        IRubyObject[] args = new IRubyObject[] {string};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long[] tuple;

        try {
            // NOTE: not searching executable path before invoking args
            tuple = ShellLauncher.runAndWaitPid(runtime, args, output, false);
        } catch (Exception e) {
            tuple = new long[] {127, -1};
        }

        // RubyStatus uses real native status now, so we unshift Java's shifted exit status
        context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, tuple[0] << 8, tuple[1]));

        byte[] out = output.toByteArray();
        int length = out.length;

        if (Platform.IS_WINDOWS) {
            // MRI behavior, replace '\r\n' by '\n'
            int newPos = 0;
            byte curr, next;
            for (int pos = 0; pos < length; pos++) {
                curr = out[pos];
                if (pos == length - 1) {
                    out[newPos++] = curr;
                    break;
                }
                next = out[pos + 1];
                if (curr != '\r' || next != '\n') {
                    out[newPos++] = curr;
                }
            }

            // trim the length
            length = newPos;
        }
        ByteList buf = new ByteList(out, 0, length, runtime.getDefaultExternalEncoding(), false);
        RubyString newString = RubyString.newString(runtime, buf);
        
        return newString;
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv) {
        return RubyRandom.srandCommon(context, recv);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return RubyRandom.srandCommon(context, recv, arg);
    }

    public static IRubyObject rand18(ThreadContext context, IRubyObject recv, IRubyObject[] arg) {
        return rand19(context, recv, arg);
    }

    @JRubyMethod(name = "rand", module = true, optional = 1, visibility = PRIVATE)
    public static IRubyObject rand19(ThreadContext context, IRubyObject recv, IRubyObject[] arg) {
        return RubyRandom.randCommon19(context, recv, arg);
    }

    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static RubyFixnum spawn(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyProcess.spawn(context, recv, args);
    }

    @JRubyMethod(required = 1, optional = 9, module = true, visibility = PRIVATE)
    public static IRubyObject syscall(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("Kernel#syscall is not implemented in JRuby");
    }

    public static IRubyObject system(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return system19(context, recv, args);
    }

    @JRubyMethod(name = "system", required = 1, rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject system19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (runtime.getPosix().isNative() && !Platform.IS_WINDOWS) {
            // MRI: rb_f_system
            long pid;
            int[] status = new int[1];

//            #if defined(SIGCLD) && !defined(SIGCHLD)
//            # define SIGCHLD SIGCLD
//            #endif
//
//            #ifdef SIGCHLD
//            RETSIGTYPE (*chfunc)(int);

            context.setLastExitStatus(context.nil);
//            chfunc = signal(SIGCHLD, SIG_DFL);
//            #endif
            PopenExecutor executor = new PopenExecutor();
            pid = executor.spawnInternal(context, args, null);
//            #if defined(HAVE_FORK) || defined(HAVE_SPAWNV)
            if (pid > 0) {
                long ret;
                ret = RubyProcess.waitpid(runtime, pid, 0);
                if (ret == -1)
                    throw runtime.newErrnoFromInt(runtime.getPosix().errno(), "Another thread waited the process started by system().");
            }
//            #endif
//            #ifdef SIGCHLD
//            signal(SIGCHLD, chfunc);
//            #endif
            if (pid < 0) {
                return runtime.getNil();
            }
            status[0] = (int)((RubyProcess.RubyStatus)context.getLastExitStatus()).getStatus();
            if (status[0] == 0) return runtime.getTrue();
            return runtime.getFalse();
        }

        // else old JDK logic
        if (args[0] instanceof RubyHash) {
            RubyHash env = (RubyHash) args[0].convertToHash();
            if (env != null) {
                runtime.getENV().merge_bang(context, env, Block.NULL_BLOCK);
            }
            // drop the first element for calling systemCommon()
            IRubyObject[] rest = new IRubyObject[args.length - 1];
            System.arraycopy(args, 1, rest, 0, args.length - 1);
            args = rest;
        }
        int resultCode = systemCommon(context, recv, args);
        switch (resultCode) {
            case 0: return runtime.getTrue();
            case 127: return runtime.getNil();
            default: return runtime.getFalse();
        }
    }

    private static int systemCommon(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        long[] tuple;

        try {
            IRubyObject lastArg = args[args.length - 1];
            if (lastArg instanceof RubyHash) {
                runtime.getWarnings().warn(ID.UNSUPPORTED_SUBPROCESS_OPTION, "system does not support options in JRuby yet: " + lastArg.inspect());
                args = Arrays.copyOf(args, args.length - 1);
            }
            if (! Platform.IS_WINDOWS && args[args.length -1].asJavaString().matches(".*[^&]&\\s*")) {
                // looks like we need to send process to the background
                ShellLauncher.runWithoutWait(runtime, args);
                return 0;
            }
            tuple = ShellLauncher.runAndWaitPid(runtime, args);
        } catch (Exception e) {
            tuple = new long[] {127, -1};
        }

        // RubyStatus uses real native status now, so we unshift Java's shifted exit status
        context.setLastExitStatus(RubyProcess.RubyStatus.newProcessStatus(runtime, tuple[0] << 8, tuple[1]));
        return (int)tuple[0];
    }
    
    public static IRubyObject exec(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        return execCommon(runtime, null, args[0], null, args);
    }

    /* Actual exec definition which calls this internal version is specified 
     * in /core/src/main/ruby/jruby/kernel/kernel.rb.
     */
    @JRubyMethod(required = 4, visibility = PRIVATE)
    public static IRubyObject _exec_internal(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        IRubyObject env = args[0];
        IRubyObject prog = args[1];
        IRubyObject options = args[2];
        RubyArray cmdArgs = (RubyArray)args[3];

        RubyIO.checkExecOptions(options);

        return execCommon(runtime, env, prog, options, cmdArgs.toJavaArray());
    }
    
    private static IRubyObject execCommon(Ruby runtime, IRubyObject env, IRubyObject prog, IRubyObject options, IRubyObject[] args) {
        // This is a fairly specific hack for empty string, but it does the job
        if (args.length == 1) {
            RubyString command = args[0].convertToString();
            if (command.isEmpty()) {
                throw runtime.newErrnoENOENTError(command.toString());
            } else {
                for(byte b : command.getBytes()) {
                    if (b == 0x00) {
                        throw runtime.newArgumentError("string contains null byte");
                    }
                }
            }
        }

        ThreadContext context = runtime.getCurrentContext();
        if (env != null && !env.isNil()) {
            RubyHash envMap = env.convertToHash();
            if (envMap != null) {
                runtime.getENV().merge_bang(context, envMap, Block.NULL_BLOCK);
            }
        }
        
        boolean nativeFailed = false;
        boolean nativeExec = Options.NATIVE_EXEC.load();
        boolean jmxStopped = false;
        System.setProperty("user.dir", runtime.getCurrentDirectory());

        if (nativeExec) {
            IRubyObject oldExc = runtime.getGlobalVariables().get("$!"); // Save $!
            try {
                ShellLauncher.LaunchConfig cfg = new ShellLauncher.LaunchConfig(runtime, args, true);

                // Duplicated in part from ShellLauncher.runExternalAndWait
                if (cfg.shouldRunInShell()) {
                    // execute command with sh -c
                    // this does shell expansion of wildcards
                    cfg.verifyExecutableForShell();
                } else {
                    cfg.verifyExecutableForDirect();
                }
                String progStr = cfg.getExecArgs()[0];

                String[] argv = cfg.getExecArgs();

                // attempt to shut down the JMX server
                jmxStopped = runtime.getBeanManager().tryShutdownAgent();

                runtime.getPosix().chdir(System.getProperty("user.dir"));
                
                if (Platform.IS_WINDOWS) {
                    // Windows exec logic is much more elaborate; exec() in jnr-posix attempts to duplicate it
                    runtime.getPosix().exec(progStr, argv);
                } else {
                    // TODO: other logic surrounding this call? In jnr-posix?
                    ArrayList envStrings = new ArrayList();
                    for (Map.Entry<String, String> envEntry : ((Map<String, String>)runtime.getENV()).entrySet()) {
                        envStrings.add(envEntry.getKey() + "=" + envEntry.getValue());
                    }
                    envStrings.add(null);

                    runtime.getPosix().execve(progStr, argv, (String[]) envStrings.toArray(new String[0]));
                }

                // Only here because native exec could not exec (always -1)
                nativeFailed = true;
            } catch (RaiseException e) {
                runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
            } catch (Exception e) {
                throw runtime.newErrnoENOENTError("cannot execute: " + e.getLocalizedMessage());
            }
        }

        // if we get here, either native exec failed or we should try an in-process exec
        if (nativeFailed) {
            if (jmxStopped && runtime.getBeanManager().tryRestartAgent()) {
                runtime.registerMBeans();
            }
            throw runtime.newErrnoFromLastPOSIXErrno();
        }
        
        // Fall back onto our existing code if native not available
        // FIXME: Make jnr-posix Pure-Java backend do this as well
        int resultCode = ShellLauncher.execAndWait(runtime, args);

        exit(runtime, new IRubyObject[] {runtime.newFixnum(resultCode)}, true);

        // not reached
        return runtime.getNil();
    }

    public static IRubyObject fork(ThreadContext context, IRubyObject recv, Block block) {
        return fork19(context, recv, block);
    }

    @JRubyMethod(name = "fork", module = true, visibility = PRIVATE, notImplemented = true)
    public static IRubyObject fork19(ThreadContext context, IRubyObject recv, Block block) {
        Ruby runtime = context.runtime;
        throw runtime.newNotImplementedError("fork is not available on this platform");
    }

    @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
    public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.runtime).dup();

        if (!str.gsub_bang(context, arg0, block).isNil()) {
            context.setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
    public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = (RubyString) getLastlineString(context, context.runtime).dup();

        if (!str.gsub_bang(context, arg0, arg1, block).isNil()) {
            context.setLastLine(str);
        }

        return str;
    }

    @JRubyMethod(module = true)
    public static IRubyObject tap(ThreadContext context, IRubyObject recv, Block block) {
        block.yield(context, recv);
        return recv;
    }
    
    @JRubyMethod(name = {"to_enum", "enum_for"}, optional = 1, rest = true)
    public static IRubyObject obj_to_enum(final ThreadContext context, IRubyObject self, IRubyObject[] args, final Block block) {
        String method = "each";
        SizeFn sizeFn = null;

        if (args.length > 0) {
            method = args[0].asJavaString();
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (block.isGiven()) {
            sizeFn = new SizeFn() {
                @Override
                public IRubyObject size(IRubyObject[] args) {
                    return block.call(context, args);
                }
            };
        }

        return enumeratorizeWithSize(context, self, method, args, sizeFn);
    }

    @JRubyMethod(name = { "__method__", "__callee__" }, module = true, visibility = PRIVATE, reads = METHODNAME, omit = true)
    public static IRubyObject __method__(ThreadContext context, IRubyObject recv) {
        String frameName = context.getFrameName();
        if (frameName == null) {
            return context.nil;
        }
        return context.runtime.newSymbol(frameName);
    }
    
    @JRubyMethod(name = "__dir__", module = true, visibility = PRIVATE, reads = FILENAME)
    public static IRubyObject __dir__(ThreadContext context, IRubyObject recv) {
        String dir = RubyFile.dirname(context, new File(context.gatherCallerBacktrace()[1].getFileName()).getAbsolutePath());
        if (dir == null) return context.nil;
        return RubyString.newString(context.runtime, dir);
    }

    @JRubyMethod(module = true)
    public static IRubyObject singleton_class(IRubyObject recv) {
        return recv.getSingletonClass();
    }

    @JRubyMethod(rest = true)
    public static IRubyObject public_send(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) {
            throw context.runtime.newArgumentError("no method name given");
        }

        String name = RubySymbol.objectToSymbolString(args[0]);
        int newArgsLength = args.length - 1;

        IRubyObject[] newArgs;
        if (newArgsLength == 0) {
            newArgs = IRubyObject.NULL_ARRAY;
        } else {
            newArgs = new IRubyObject[newArgsLength];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        }

        DynamicMethod method = recv.getMetaClass().searchMethod(name);

        if (method.isUndefined() || method.getVisibility() != PUBLIC) {
            return Helpers.callMethodMissing(context, recv, method.getVisibility(), name, CallType.NORMAL, newArgs, block);
        }

        return method.call(context, recv, recv.getMetaClass(), name, newArgs, block);
    }

    /*
     * Moved binding of these methods here, since Kernel can be included into
     * BasicObject subclasses, and these methods must still work.
     * See JRUBY-4871 (because of RubyObject instead of RubyBasicObject cast)
     * BEGIN delegated bindings:
     */
    @JRubyMethod(name = "eql?", required = 1)
    public static IRubyObject eql_p(IRubyObject self, IRubyObject obj) {
        return ((RubyBasicObject)self).eql_p(obj);
    }

    @JRubyMethod(name = "===", required = 1)
    public static IRubyObject op_eqq(ThreadContext context, IRubyObject self, IRubyObject other) {
        return ((RubyBasicObject)self).op_eqq(context, other);
    }

    @JRubyMethod(name = "<=>", required = 1)
    public static IRubyObject op_cmp(ThreadContext context, IRubyObject self, IRubyObject other) {
        return ((RubyBasicObject)self).op_cmp(context, other);
    }

    @JRubyMethod(name = "initialize_copy", required = 1, visibility = PRIVATE)
    public static IRubyObject initialize_copy(IRubyObject self, IRubyObject original) {
        return ((RubyBasicObject)self).initialize_copy(original);
    }

    @JRubyMethod(name = "initialize_clone", required = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject initialize_clone(ThreadContext context, IRubyObject self, IRubyObject original) {
        return self.callMethod(context, "initialize_copy", original);
    }

    @JRubyMethod(name = "initialize_dup", required = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject initialize_dup(ThreadContext context, IRubyObject self, IRubyObject original) {
        return self.callMethod(context, "initialize_copy", original);
    }

    @JRubyMethod(name = "respond_to?")
    public static IRubyObject respond_to_p19(IRubyObject self, IRubyObject mname) {
        return ((RubyBasicObject)self).respond_to_p19(mname);
    }

    @JRubyMethod(name = "respond_to?")
    public static IRubyObject respond_to_p19(IRubyObject self, IRubyObject mname, IRubyObject includePrivate) {
        return ((RubyBasicObject)self).respond_to_p19(mname, includePrivate);
    }

    @JRubyMethod
    public static RubyFixnum hash(IRubyObject self) {
        return ((RubyBasicObject)self).hash();
    }

    @JRubyMethod(name = "class")
    public static RubyClass type(IRubyObject self) {
        return ((RubyBasicObject)self).type();
    }

    @JRubyMethod(name = "clone")
    public static IRubyObject rbClone(IRubyObject self) {
        return ((RubyBasicObject)self).rbClone();
    }

    @JRubyMethod
    public static IRubyObject dup(IRubyObject self) {
        return ((RubyBasicObject)self).dup();
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject display(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return ((RubyBasicObject)self).display(context, args);
    }

    @JRubyMethod(name = {"tainted?", "untrusted?"})
    public static RubyBoolean tainted_p(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject)self).tainted_p(context);
    }

    @JRubyMethod(name = {"taint", "untrust"})
    public static IRubyObject taint(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject)self).taint(context);
    }

    @JRubyMethod(name = {"untaint", "trust"})
    public static IRubyObject untaint(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject)self).untaint(context);
    }

    @JRubyMethod
    public static IRubyObject freeze(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject)self).freeze(context);
    }

    @JRubyMethod(name = "frozen?")
    public static RubyBoolean frozen_p(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject)self).frozen_p(context);
    }

    @JRubyMethod(name = "inspect")
    public static IRubyObject inspect(IRubyObject self) {
        return ((RubyBasicObject)self).inspect();
    }

    @JRubyMethod(name = "instance_of?", required = 1)
    public static RubyBoolean instance_of_p(ThreadContext context, IRubyObject self, IRubyObject type) {
        return ((RubyBasicObject)self).instance_of_p(context, type);
    }

    @JRubyMethod(name = "itself")
    public static IRubyObject itself(IRubyObject self) {
        return self;
    }

    @JRubyMethod(name = {"kind_of?", "is_a?"}, required = 1)
    public static RubyBoolean kind_of_p(ThreadContext context, IRubyObject self, IRubyObject type) {
        return ((RubyBasicObject)self).kind_of_p(context, type);
    }

    @JRubyMethod(name = "methods", optional = 1)
    public static IRubyObject methods19(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return ((RubyBasicObject)self).methods19(context, args);
    }

    @JRubyMethod(name = "object_id")
    public static IRubyObject object_id(IRubyObject self) {
        return self.id();
    }

    @JRubyMethod(name = "public_methods", optional = 1)
    public static IRubyObject public_methods19(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return ((RubyBasicObject)self).public_methods19(context, args);
    }

    @JRubyMethod(name = "protected_methods", optional = 1)
    public static IRubyObject protected_methods19(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return ((RubyBasicObject)self).protected_methods19(context, args);
    }

    @JRubyMethod(name = "private_methods", optional = 1)
    public static IRubyObject private_methods19(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return ((RubyBasicObject)self).private_methods19(context, args);
    }

    @JRubyMethod(name = "singleton_methods", optional = 1)
    public static RubyArray singleton_methods19(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return ((RubyBasicObject)self).singleton_methods19(context, args);
    }

    @JRubyMethod(name = "method", required = 1)
    public static IRubyObject method19(IRubyObject self, IRubyObject symbol) {
        return ((RubyBasicObject)self).method19(symbol);
    }

    @JRubyMethod(name = "to_s")
    public static IRubyObject to_s(IRubyObject self) {
        return ((RubyBasicObject)self).to_s();
    }

    @JRubyMethod(name = "extend", required = 1, rest = true)
    public static IRubyObject extend(IRubyObject self, IRubyObject[] args) {
        return ((RubyBasicObject)self).extend(args);
    }

    @JRubyMethod(name = {"send"}, omit = true)
    public static IRubyObject send19(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        return ((RubyBasicObject)self).send19(context, arg0, block);
    }
    @JRubyMethod(name = {"send"}, omit = true)
    public static IRubyObject send19(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        return ((RubyBasicObject)self).send19(context, arg0, arg1, block);
    }
    @JRubyMethod(name = {"send"}, omit = true)
    public static IRubyObject send19(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return ((RubyBasicObject)self).send19(context, arg0, arg1, arg2, block);
    }
    @JRubyMethod(name = {"send"}, required = 1, rest = true, omit = true)
    public static IRubyObject send19(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return ((RubyBasicObject)self).send19(context, args, block);
    }

    @JRubyMethod(name = "nil?")
    public static IRubyObject nil_p(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject)self).nil_p(context);
    }

    public static IRubyObject op_match(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return op_match19(context, self, arg);
    }

    @JRubyMethod(name = "=~", required = 1, writes = FrameField.BACKREF)
    public static IRubyObject op_match19(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return ((RubyBasicObject)self).op_match19(context, arg);
    }

    @JRubyMethod(name = "!~", required = 1, writes = FrameField.BACKREF)
    public static IRubyObject op_not_match(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return ((RubyBasicObject)self).op_not_match(context, arg);
    }

    @JRubyMethod(name = "instance_variable_defined?", required = 1)
    public static IRubyObject instance_variable_defined_p(ThreadContext context, IRubyObject self, IRubyObject name) {
        return ((RubyBasicObject)self).instance_variable_defined_p(context, name);
    }

    @JRubyMethod(name = "instance_variable_get", required = 1)
    public static IRubyObject instance_variable_get(ThreadContext context, IRubyObject self, IRubyObject name) {
        return ((RubyBasicObject)self).instance_variable_get(context, name);
    }

    @JRubyMethod(name = "instance_variable_set", required = 2)
    public static IRubyObject instance_variable_set(IRubyObject self, IRubyObject name, IRubyObject value) {
        return ((RubyBasicObject)self).instance_variable_set(name, value);
    }

    @JRubyMethod(name = "remove_instance_variable", required = 1)
    public static IRubyObject remove_instance_variable(ThreadContext context, IRubyObject self, IRubyObject name, Block block) {
        return ((RubyBasicObject)self).remove_instance_variable(context, name, block);
    }

    @JRubyMethod(name = "instance_variables")
    public static RubyArray instance_variables19(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject)self).instance_variables19(context);
    }
    /* end delegated bindings */
}
