/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jnr.constants.platform.Errno;
import jnr.posix.POSIX;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.common.RubyWarnings;
import org.jruby.exceptions.CatchThrow;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites.KernelSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.ShellLauncher;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.func.ObjectIntIntFunction;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.PopenExecutor;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyFile.fileResource;
import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.RubyIO.checkUnsupportedOptions;
import static org.jruby.RubyIO.checkValidSpawnOptions;
import static org.jruby.RubyIO.UNSUPPORTED_SPAWN_OPTIONS;
import static org.jruby.anno.FrameField.BLOCK;
import static org.jruby.anno.FrameField.CLASS;
import static org.jruby.anno.FrameField.FILENAME;
import static org.jruby.anno.FrameField.LASTLINE;
import static org.jruby.anno.FrameField.LINE;
import static org.jruby.anno.FrameField.METHODNAME;
import static org.jruby.anno.FrameField.SCOPE;
import static org.jruby.anno.FrameField.SELF;
import static org.jruby.anno.FrameField.VISIBILITY;
import static org.jruby.api.Check.checkEmbeddedNulls;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.ir.runtime.IRRuntimeHelpers.dupIfKeywordRestAtCallsite;
import static org.jruby.runtime.ThreadContext.hasKeywords;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PROTECTED;
import static org.jruby.runtime.Visibility.PUBLIC;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * Note: For CVS history, see KernelModule.java.
 */
@JRubyModule(name="Kernel")
public class RubyKernel {

    public static class MethodMissingMethod extends JavaMethodNBlock {
        private final Visibility visibility;
        private final CallType callType;

        MethodMissingMethod(RubyModule implementationClass, Visibility visibility, CallType callType) {
            super(implementationClass, Visibility.PRIVATE, "method_missing");

            this.callType = callType;
            this.visibility = visibility;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return RubyKernel.methodMissing(context, self, name, visibility, callType, args);
        }

    }

    public static RubyModule createKernelModule(Ruby runtime) {
        RubyModule module = runtime.defineModule("Kernel");

        module.defineAnnotatedMethods(RubyKernel.class);

        module.setFlag(RubyModule.NEEDSIMPL_F, false); //Kernel is the only normal Module that doesn't need an implementor

        runtime.setPrivateMethodMissing(new MethodMissingMethod(module, PRIVATE, CallType.NORMAL));
        runtime.setProtectedMethodMissing(new MethodMissingMethod(module, PROTECTED, CallType.NORMAL));
        runtime.setVariableMethodMissing(new MethodMissingMethod(module, PUBLIC, CallType.VARIABLE));
        runtime.setSuperMethodMissing(new MethodMissingMethod(module, PUBLIC, CallType.SUPER));
        runtime.setNormalMethodMissing(new MethodMissingMethod(module, PUBLIC, CallType.NORMAL));

        if (runtime.getInstanceConfig().isAssumeLoop()) {
            module.defineAnnotatedMethods(LoopMethods.class);
        }

        recacheBuiltinMethods(runtime, module);

        return module;
    }

    /**
     * Cache built-in versions of several core methods, to improve performance by using identity comparison (==) rather
     * than going ahead with dynamic dispatch.
     *
     * @param runtime
     */
    static void recacheBuiltinMethods(Ruby runtime, RubyModule kernelModule) {
        runtime.setRespondToMethod(kernelModule.searchMethod("respond_to?"));
        runtime.setRespondToMissingMethod(kernelModule.searchMethod("respond_to_missing?"));
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject at_exit(ThreadContext context, IRubyObject recv, Block block) {
        if (!block.isGiven()) throw argumentError(context, "called without a block");

        return context.runtime.pushExitBlock(context.runtime.newProc(Block.Type.PROC, block));
    }

    @JRubyMethod(name = "autoload?", required = 1, module = true, visibility = PRIVATE, reads = {SCOPE})
    public static IRubyObject autoload_p(ThreadContext context, final IRubyObject recv, IRubyObject symbol) {
        RubyModule module = IRRuntimeHelpers.getCurrentClassBase(context, recv);

        return module == null || module.isNil() ?
                context.nil : module.autoload_p(context, symbol);
    }

    @JRubyMethod(required = 2, module = true, visibility = PRIVATE, reads = {SCOPE})
    public static IRubyObject autoload(ThreadContext context, final IRubyObject recv, IRubyObject symbol, IRubyObject file) {
        RubyModule module = IRRuntimeHelpers.getCurrentClassBase(context, recv).getRealModule();

        if (module == null || module.isNil()) throw typeError(context, "Can not set autoload on singleton class");

        return module.autoload(context, symbol, file);
    }

    public static IRubyObject method_missing(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Visibility lastVis = context.getLastVisibility();
        CallType lastCallType = context.getLastCallType();

        if (args.length == 0 || !(args[0] instanceof RubySymbol sym)) throw argumentError(context, "no id given");

        return methodMissingDirect(context, recv, sym, lastVis, lastCallType, args);
    }

    protected static IRubyObject methodMissingDirect(ThreadContext context, IRubyObject recv, RubySymbol symbol, Visibility lastVis, CallType lastCallType, IRubyObject[] args) {
        return methodMissing(context, recv, symbol.idString(), lastVis, lastCallType, args, true);
    }

    public static IRubyObject methodMissing(ThreadContext context, IRubyObject recv, String name, Visibility lastVis, CallType lastCallType, IRubyObject[] args) {
        return methodMissing(context, recv, name, lastVis, lastCallType, args, false);
    }

    public static IRubyObject methodMissing(ThreadContext context, IRubyObject recv, String name, Visibility lastVis, CallType lastCallType, IRubyObject[] args, boolean dropFirst) {
        Ruby runtime = context.runtime;

        boolean privateCall = false;
        if (lastCallType == CallType.VARIABLE || lastCallType == CallType.FUNCTIONAL) {
            privateCall = true;
        } else if (lastVis == PUBLIC) {
            privateCall = true;
        }

        if (lastCallType == CallType.VARIABLE) {
            throw runtime.newNameError(getMethodMissingFormat(lastVis, lastCallType), recv, name, privateCall);
        }
        throw runtime.newNoMethodError(getMethodMissingFormat(lastVis, lastCallType), recv, name, RubyArray.newArrayMayCopy(runtime, args, dropFirst ? 1 : 0), privateCall);
    }

    private static String getMethodMissingFormat(Visibility visibility, CallType callType) {

        String format = "undefined method '%s' for %s%s%s";

        if (visibility == PRIVATE) {
            format = "private method '%s' called for %s%s%s";
        } else if (visibility == PROTECTED) {
            format = "protected method '%s' called for %s%s%s";
        } else if (callType == CallType.VARIABLE) {
            format = "undefined local variable or method '%s' for %s%s%s";
        } else if (callType == CallType.SUPER) {
            format = "super: no superclass method '%s' for %s%s%s";
        }

        return format;
    }

    @JRubyMethod(name = "open", required = 1, optional = 3, checkArity = false, module = true, visibility = PRIVATE, keywords = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        boolean redirect = false;
        int callInfo = ThreadContext.resetCallInfo(context);
        boolean keywords = hasKeywords(callInfo);

        if (args.length >= 1) {
            //            CONST_ID(to_open, "to_open");
            if (args[0].respondsTo("to_open")) {
                redirect = true;
            } else {
                IRubyObject tmp = args[0];
                tmp = RubyFile.get_path(context, tmp);
                if (tmp == context.nil) {
                    redirect = true;
                } else {
                    IRubyObject cmd = PopenExecutor.checkPipeCommand(context, tmp);
                    if (cmd != context.nil) {
                        if (PopenExecutor.nativePopenAvailable(context.runtime)) {
                            args[0] = cmd;
                            return PopenExecutor.popen(context, args, context.runtime.getIO(), block);
                        }

                        throw argumentError(context, "pipe open is not supported without native subprocess logic");
                    }
                }
            }
        }

        // Mild hack. We want to arity-mismatch if extra arg is not really a kwarg but not if it is one.
        int maxArgs = keywords ? 5 : 4;
        Arity.checkArgumentCount(context, args, 1, maxArgs);

        //        symbol to_open = 0;

        if (redirect) {
            if (keywords) context.callInfo = ThreadContext.CALL_KEYWORD;
            IRubyObject io = args[0].callMethod(context, "to_open", Arrays.copyOfRange(args, 1, args.length));

            RubyIO.ensureYieldClose(context, io, block);
            return io;
        }

        // We had to save callInfo from original call because kwargs needs to still pass through to IO#open
        context.callInfo = callInfo;
        return RubyIO.open(context, context.runtime.getFile(), args, block);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject getc(ThreadContext context, IRubyObject recv) {
        context.runtime.getWarnings().warn(ID.DEPRECATED_METHOD, "getc is obsolete; use STDIN.getc instead");
        IRubyObject defin = context.runtime.getGlobalVariables().get("$stdin");
        return sites(context).getc.call(context, defin, defin);
    }

    // MRI: rb_f_gets
    @JRubyMethod(optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject argsFile = runtime.getArgsFile();

        if (recv == argsFile) {
            return RubyArgsFile.gets(context, argsFile, args);
        }
        return sites(context).gets.call(context, argsFile, argsFile, args);
    }

    @JRubyMethod(optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject abort(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        Ruby runtime = context.runtime;

        RubyString message = null;
        if(argc == 1) {
            message = args[0].convertToString();
            IRubyObject stderr = runtime.getGlobalVariables().get("$stderr");
            sites(context).puts.call(context, stderr, stderr, message);
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
        RubyClass complex = context.runtime.getComplex();
        return sites(context).convert_complex.call(context, complex, complex);
    }
    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        RubyClass complex = context.runtime.getComplex();
        return sites(context).convert_complex.call(context, complex, complex, arg0);
    }
    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        RubyClass complex = context.runtime.getComplex();
        return sites(context).convert_complex.call(context, complex, complex, arg0, arg1);
    }
    @JRubyMethod(name = "Complex", module = true, visibility = PRIVATE)
    public static IRubyObject new_complex(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass complex = context.runtime.getComplex();
        return sites(context).convert_complex.call(context, complex, complex, arg0, arg1, arg2);
    }

    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        RubyClass rational = context.runtime.getRational();
        return sites(context).convert_rational.call(context, rational, rational, arg0);
    }
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        RubyClass rational = context.runtime.getRational();
        return sites(context).convert_rational.call(context, rational, rational, arg0, arg1);
    }
    @JRubyMethod(name = "Rational", module = true, visibility = PRIVATE)
    public static IRubyObject new_rational(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        RubyClass rational = context.runtime.getRational();
        return sites(context).convert_rational.call(context, rational, rational, arg0, arg1, arg2);
    }

    @JRubyMethod(name = "Float", module = true, visibility = PRIVATE)
    public static IRubyObject new_float(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return new_float(context, object, true);
    }

    @JRubyMethod(name = "Float", module = true, visibility = PRIVATE)
    public static IRubyObject new_float(ThreadContext context, IRubyObject recv, IRubyObject object, IRubyObject opts) {
        boolean exception = checkExceptionOpt(context, context.runtime.getFloat(), opts);

        return new_float(context, object, exception);
    }

    private static boolean checkExceptionOpt(ThreadContext context, RubyClass rubyClass, IRubyObject opts) {
        IRubyObject maybeOpts = ArgsUtil.getOptionsArg(context.runtime, opts, false);
        if (maybeOpts.isNil()) return true;

        IRubyObject exObj = ArgsUtil.extractKeywordArg(context,  opts, "exception");

        if (exObj != context.tru && exObj != context.fals) {
            throw argumentError(context, "'" + rubyClass.getName() + "': expected true or false as exception: " + exObj);
        }

        return  exObj.isTrue();
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static RubyFloat new_float(IRubyObject recv, IRubyObject object) {
        return (RubyFloat) new_float(recv.getRuntime().getCurrentContext(), object, true);
    }

    private static final ByteList ZEROx = new ByteList(new byte[] { '0','x' }, false);

    @Deprecated(since = "10.0", forRemoval = true)
    public static RubyFloat new_float(final Ruby runtime, IRubyObject object) {
        return (RubyFloat) new_float(runtime.getCurrentContext(), object, true);
    }

    public static RubyFloat new_float(ThreadContext context, IRubyObject object) {
        return (RubyFloat) new_float(context, object, true);
    }

    private static BigInteger SIXTEEN = BigInteger.valueOf(16L);
    private static BigInteger MINUS_ONE = BigInteger.valueOf(-1L);

    private static RaiseException floatError(ThreadContext context, ByteList string) {
        throw argumentError(context, str(context.runtime, "invalid value for Float(): ", newString(context, string)));
    }

    @Deprecated(since = "10.0", forRemoval = true)
    public static double parseHexidecimalExponentString2(Ruby runtime, ByteList str) {
        return parseHexidecimalExponentString2(runtime.getCurrentContext(), str);
    }
    /**
     * Parse hexidecimal exponential notation:
     * <a href="https://en.wikipedia.org/wiki/Hexadecimal#Hexadecimal_exponential_notation">...</a>
     * <p/>
     * This method assumes the string is a valid-formatted string.
     *
     * @param str the bytelist to be parsed
     * @return the result.
     */

    public static double parseHexidecimalExponentString2(ThreadContext context, ByteList str) {
        byte[] bytes = str.unsafeBytes();
        int length = str.length();
        if (length <= 2) throw floatError(context, str);
        int sign = 1;
        int letter;
        int i = str.begin();

        letter = bytes[i];
        if (letter == '+') {
            i++;
        } else if (letter == '-') {
            sign = -1;
            i++;
        }

        // Skip '0x'
        letter = bytes[i++];
        if (letter != '0') throw floatError(context, str);
        letter = bytes[i++];
        if (letter != 'x') throw floatError(context, str);

        int exponent = 0;
        int explicitExponent = 0;
        int explicitExponentSign = 1;
        boolean periodFound = false;
        boolean explicitExponentFound = false;
        BigInteger value = BigInteger.valueOf(0L);

        if (i == length || bytes[i] == '_' || bytes[i] == 'p' || bytes[i] == 'P') throw floatError(context, str);

        for(; i < length && !explicitExponentFound; i++) {
            letter = bytes[i];
            switch (letter) {
                case '.':                              // Fractional part of floating point value "1(.)23"
                    periodFound = true;
                    continue;
                case 'p':                              // Explicit exponent "1.23(p)1a"
                case 'P':
                    if (bytes[i-1] == '_' || i == length - 1) throw floatError(context, str);
                    explicitExponentFound = true;
                    continue;
                case '_':
                    continue;
            }

            // base 16 value representing main pieces of number
            int digit = Character.digit(letter, 16);
            if (Character.forDigit(digit, 16) == 0) throw floatError(context, str);
            value = value.multiply(SIXTEEN).add(BigInteger.valueOf(digit));

            if (periodFound) exponent++;
        }

        if (explicitExponentFound) {
            if (bytes[i] == '-') {
                explicitExponentSign = -1;
                i++;
            } else if (bytes[i] == '+') {
                i++;
            } else if (bytes[i] == '_') {
                throw floatError(context, str);
            }

            for (; i < length; i++) { // base 10 value representing base 2 exponent
                letter = bytes[i];
                if (letter == '_') {
                    if (i == length - 1) throw floatError(context, str);
                    continue;
                }
                int digit = Character.digit(letter, 10);
                if (Character.forDigit(digit, 10) == 0) throw floatError(context, str);
                explicitExponent = explicitExponent * 10 + digit;
            }
        }

        // each exponent in main number is 4 bits and the value after 'p' represents a power of 2.  Wacky.
        int scaleFactor = 4 * exponent - explicitExponent * explicitExponentSign;
        return sign * Math.scalb(value.doubleValue(), -scaleFactor);
    }

    public static IRubyObject new_float(ThreadContext context, IRubyObject object, boolean exception) {
        Ruby runtime = context.runtime;

        if (object instanceof RubyInteger)return new_float(context, (RubyInteger) object);
        if (object instanceof RubyFloat) return object;
        if (object instanceof RubyString str) {
            ByteList bytes = str.getByteList();
            if (bytes.isEmpty()) { // rb_cstr_to_dbl case
                if (!exception) return context.nil;
                throw argumentError(context, "invalid value for Float(): " + object.inspect());
            }

            if (bytes.startsWith(ZEROx)) { // startsWith("0x")
                if (bytes.indexOf('p') != -1 || bytes.indexOf('P') != -1) {
                    return asFloat(context, parseHexidecimalExponentString2(context, bytes));
                }
                IRubyObject inum = ConvertBytes.byteListToInum(runtime, bytes, 16, true, exception);
                if (!exception && inum.isNil()) return inum;
                return ((RubyInteger) inum).toFloat();
            }
            return RubyNumeric.str2fnum(runtime, str, true, exception);
        }
        if (object.isNil()) {
            if (exception) throw typeError(context, "can't convert nil into Float");
            return object;
        }

        try {
            IRubyObject flote = TypeConverter.convertToType(context, object, runtime.getFloat(), sites(context).to_f_checked, false);
            if (flote instanceof RubyFloat) return flote;
        } catch (RaiseException re) {
            if (exception) throw re;
        }

        if (!exception) return context.nil;

        return TypeConverter.handleUncoercibleObject(runtime, object, runtime.getFloat(), true);
    }

    static RubyFloat new_float(ThreadContext context, RubyInteger num) {
        return num instanceof RubyBignum ?
                asFloat(context, RubyBignum.big2dbl((RubyBignum) num)) :
                asFloat(context, num.getDoubleValue());
    }

    @JRubyMethod(name = "Hash", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_hash(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        if (arg == context.nil) return newHash(context);

        IRubyObject tmp = TypeConverter.checkHashType(context, sites(context).to_hash_checked, arg);
        if (tmp == context.nil) {
            if (arg instanceof RubyArray && ((RubyArray) arg).isEmpty()) return newHash(context);
            throw typeError(context, "can't convert ", arg, " into Hash");
        }
        return tmp;
    }

    @JRubyMethod(name = "Integer", module = true, visibility = PRIVATE)
    public static IRubyObject new_integer(ThreadContext context, IRubyObject recv, IRubyObject object) {
        return TypeConverter.convertToInteger(context, object, 0, true);
    }

    @JRubyMethod(name = "Integer", module = true, visibility = PRIVATE)
    public static IRubyObject new_integer(ThreadContext context, IRubyObject recv, IRubyObject object, IRubyObject baseOrOpts) {
        IRubyObject maybeOpts = ArgsUtil.getOptionsArg(context.runtime, baseOrOpts, false);

        if (maybeOpts.isNil()) {
            return TypeConverter.convertToInteger(context, object, baseOrOpts.convertToInteger().getIntValue(), true);
        }

        boolean exception = checkExceptionOpt(context, context.runtime.getInteger(), maybeOpts);

        return TypeConverter.convertToInteger(
                context,
                object,
                0,
                exception);
    }

    @JRubyMethod(name = "Integer", module = true, visibility = PRIVATE)
    public static IRubyObject new_integer(ThreadContext context, IRubyObject recv, IRubyObject object, IRubyObject base, IRubyObject opts) {
        boolean exception = checkExceptionOpt(context, context.runtime.getInteger(), opts);

        IRubyObject baseInteger = TypeConverter.convertToInteger(context, base, 0, exception);

        if (baseInteger.isNil()) return baseInteger;

        return TypeConverter.convertToInteger(
                context,
                object,
                ((RubyInteger) baseInteger).getIntValue(),
                exception);
    }

    @JRubyMethod(name = "String", required = 1, module = true, visibility = PRIVATE)
    public static IRubyObject new_string(ThreadContext context, IRubyObject recv, IRubyObject object) {
        Ruby runtime = context.runtime;
        KernelSites sites = sites(context);

        IRubyObject tmp = TypeConverter.checkStringType(context, sites.to_str_checked, object, runtime.getString());
        if (tmp == context.nil) {
            tmp = TypeConverter.convertToType(context, object, runtime.getString(), sites(context).to_s_checked);
        }
        return tmp;
    }

    // MRI: rb_f_p
    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject p(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyThread.uninterruptible(context, args, RubyKernel::pBody);
    }

    private static IRubyObject pBody(ThreadContext context, IRubyObject[] args) {
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

    @JRubyMethod(module = true)
    public static IRubyObject public_method(ThreadContext context, IRubyObject recv, IRubyObject symbol) {
        return recv.getMetaClass().newMethod(recv, symbol.asJavaString(), true, PUBLIC, true, false);
    }

    /** rb_f_putc
     */
    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject putc(ThreadContext context, IRubyObject recv, IRubyObject ch) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");
        if (recv == defout) {
            return RubyIO.putc(context, recv, ch);
        }
        return sites(context).putc.call(context, defout, defout, ch);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts0(context, recv);
        }

        return sites(context).puts.call(context, defout, defout);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts1(context, recv, arg0);
        }

        return sites(context).puts.call(context, defout, defout, arg0);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts2(context, recv, arg0, arg1);
        }

        return sites(context).puts.call(context, defout, defout, arg0, arg1);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts3(context, recv, arg0, arg1, arg2);
        }

        return sites(context).puts.call(context, defout, defout, arg0, arg1, arg2);
    }

    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject defout = context.runtime.getGlobalVariables().get("$>");

        if (recv == defout) {
            return RubyIO.puts(context, recv, args);
        }

        return sites(context).puts.call(context, defout, defout, args);
    }

    // rb_f_print
    @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE)
    public static IRubyObject print(ThreadContext context, IRubyObject recv) {
        return RubyIO.print0(context, context.runtime.getGlobalVariables().get("$>"));
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return RubyIO.print1(context, context.runtime.getGlobalVariables().get("$>"), arg0);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return RubyIO.print2(context, context.runtime.getGlobalVariables().get("$>"), arg0, arg1);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return RubyIO.print3(context, context.runtime.getGlobalVariables().get("$>"), arg0, arg1, arg2);
    }

    @JRubyMethod(rest = true, module = true, visibility = PRIVATE, reads = LASTLINE)
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.print(context, context.runtime.getGlobalVariables().get("$>"), args);
    }

    // rb_f_printf
    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static IRubyObject printf(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) return context.nil;

        final IRubyObject out;
        if (args[0] instanceof RubyString) {
            out = context.runtime.getGlobalVariables().get("$>");
        }
        else {
            out = args[0];
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        RubyIO.write(context, out, sprintf(context, recv, args));

        return context.nil;
    }

    @JRubyMethod(optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) {
            throw context.runtime.newEOFError();
        }

        return line;
    }

    @JRubyMethod(optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyArgsFile.readlines(context, context.runtime.getArgsFile(), args);
    }

    @JRubyMethod(name = "respond_to_missing?", visibility = PRIVATE)
    public static IRubyObject respond_to_missing_p(ThreadContext context, IRubyObject recv, IRubyObject symbol) {
        return context.fals;
    }

    @JRubyMethod(name = "respond_to_missing?", visibility = PRIVATE)
    public static IRubyObject respond_to_missing_p(ThreadContext context, IRubyObject recv, IRubyObject symbol, IRubyObject isPrivate) {
        return context.fals;
    }

    /** Returns value of $_.
     *
     * @throws RaiseException TypeError if $_ is not a String or nil.
     * @return value of $_ as String.
     */
    private static RubyString getLastlineString(ThreadContext context) {
        IRubyObject line = context.getLastLine();

        if (line.isNil()) throw typeError(context, "$_ value need to be String (nil given).");
        if (!(line instanceof RubyString)) throw typeError(context, "$_ value need to be String (", line, " given).");

        return (RubyString) line;
    }

    @JRubyMethod(required = 1, optional = 3, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject select(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.select(context, recv, args);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject sleep(ThreadContext context, IRubyObject recv) {
        // Zero sleeps forever
        return sleepCommon(context, 0);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject sleep(ThreadContext context, IRubyObject recv, IRubyObject timeout) {
        if (timeout.isNil()) return sleep(context, recv);

        long nanoseconds = (long) (RubyTime.convertTimeInterval(context, timeout) * 1_000_000_000);

        // Explicit zero in MRI returns immediately
        if (nanoseconds == 0) return RubyFixnum.zero(context.runtime);

        return sleepCommon(context, nanoseconds);
    }

    private static RubyFixnum sleepCommon(ThreadContext context, long nanoseconds) {
        final long startTime = System.nanoTime();
        final RubyThread rubyThread = context.getThread();

        boolean interrupted = false;
        try {
            // Spurious wakeup-loop
            do {
                long loopStartTime = System.nanoTime();
                long milliseconds = TimeUnit.NANOSECONDS.toMillis(nanoseconds);
                long remainingNanos = nanoseconds - TimeUnit.MILLISECONDS.toNanos(milliseconds);

                if (!rubyThread.sleep(milliseconds, remainingNanos)) break;

                nanoseconds -= (System.nanoTime() - loopStartTime);
            } while (nanoseconds > 0);
        } catch (InterruptedException ie) {
            // ignore; sleep gets interrupted
            interrupted = true;
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }

        return asFixnum(context, Math.round((System.nanoTime() - startTime) / 1_000_000_000.0));
    }

    @Deprecated
    public static IRubyObject exit(IRubyObject recv, IRubyObject[] args) {
        return exit(recv.getRuntime().getCurrentContext(), recv, args);
    }

    // FIXME: Add at_exit and finalizers to exit, then make exit_bang not call those.
    @JRubyMethod(optional = 1, module = true, visibility = PRIVATE)
    public static IRubyObject exit(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        Arity.checkArgumentCount(context, args, 0, 1);

        exit(runtime, args, false);
        return runtime.getNil(); // not reached
    }

    @Deprecated
    public static IRubyObject exit_bang(IRubyObject recv, IRubyObject[] args) {
        return exit_bang(recv.getRuntime().getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "exit!", optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject exit_bang(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        Arity.checkArgumentCount(context, args, 0, 1);

        exit(runtime, args, true);
        return runtime.getNil(); // not reached
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


    /**
     * @param context
     * @param recv
     * @return an Array with the names of all global variables.
     */
    @JRubyMethod(name = "global_variables", module = true, visibility = PRIVATE)
    public static RubyArray global_variables(ThreadContext context, IRubyObject recv) {
        GlobalVariables globals = context.runtime.getGlobalVariables();
        var globalVariables = newRawArray(context, globals.size());

        globals.eachName(context, globalVariables, (c, g, s) -> g.append(c, asSymbol(c, s)));

        return globalVariables.finishRawArray(context);
    }

    /**
     * @param context
     * @param recv
     * @return an Array with the names of all local variables.
     */
    @JRubyMethod(name = "local_variables", module = true, visibility = PRIVATE, reads = SCOPE)
    public static RubyArray local_variables(ThreadContext context, IRubyObject recv) {
        return context.getCurrentStaticScope().getLocalVariables(context);
    }

    @JRubyMethod(name = "binding", module = true, visibility = PRIVATE,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public static RubyBinding binding(ThreadContext context, IRubyObject recv, Block block) {
        return RubyBinding.newBinding(context.runtime, context.currentBinding());
    }

    @JRubyMethod(name = {"block_given?", "iterator?"}, module = true, visibility = PRIVATE, reads = BLOCK)
    public static RubyBoolean block_given_p(ThreadContext context, IRubyObject recv) {
        return asBoolean(context, context.getCurrentFrame().getBlock().isGiven());
    }

    public static RubyBoolean blockGiven(ThreadContext context, IRubyObject recv, Block frameBlock) {
        return asBoolean(context, frameBlock.isGiven());
    }

    @JRubyMethod(name = {"sprintf", "format"}, required = 1, rest = true, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject sprintf(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        if (args.length == 0) throw argumentError(context, "sprintf must have at least one argument");

        RubyString str = RubyString.stringValue(args[0]);

        IRubyObject arg;
        if (args.length == 2 && args[1] instanceof RubyHash) {
            arg = args[1];
        } else {
            RubyArray newArgs = RubyArray.newArrayMayCopy(context.runtime, args);
            newArgs.shift(context);
            arg = newArgs;
        }

        return str.op_format(context, arg);
    }

    @Deprecated
    public static IRubyObject sprintf(IRubyObject recv, IRubyObject[] args) {
        return sprintf(recv.getRuntime().getCurrentContext(), recv, args);
    }
    public static IRubyObject raise(ThreadContext context, IRubyObject self, IRubyObject arg0) {
        // semi extract_raise_opts :
        if (arg0 instanceof RubyHash opt && !opt.isEmpty() &&
                opt.has_key_p(context, asSymbol(context, "cause")) == context.tru) {
                throw argumentError(context, "only cause is given with no arguments");
        }

        IRubyObject cause = context.getErrorInfo(); // returns nil for no error-info

        maybeRaiseJavaException(context, arg0);

        RaiseException raise = arg0 instanceof RubyString ?
                ((RubyException) context.runtime.getRuntimeError().newInstance(context, arg0)).toThrowable() :
                convertToException(context, arg0, null).toThrowable();

        var exception = raise.getException();

        if (context.runtime.isDebug()) printExceptionSummary(context, exception);
        if (exception.getCause() == null && cause != exception) exception.setCause(cause);

        throw raise;
    }

    @JRubyMethod(name = {"raise", "fail"}, optional = 3, checkArity = false, module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject raise(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 0, 3);

        final Ruby runtime = context.runtime;
        boolean forceCause = false;

        // semi extract_raise_opts :
        IRubyObject cause = null;
        if (argc > 0) {
            IRubyObject last = args[argc - 1];
            if (last instanceof RubyHash opt) {
                RubySymbol key;
                if (!opt.isEmpty() && (opt.has_key_p(context, key = asSymbol(context, "cause")) == context.tru)) {
                    cause = opt.delete(context, key, Block.NULL_BLOCK);
                    forceCause = true;
                    if (opt.isEmpty() && --argc == 0) { // more opts will be passed along
                        throw argumentError(context, "only cause is given with no arguments");
                    }
                }
            }
        }

        if ( argc > 0 ) { // for argc == 0 we will be raising $!
            // NOTE: getErrorInfo needs to happen before new RaiseException(...)
            if ( cause == null ) cause = context.getErrorInfo(); // returns nil for no error-info
        }

        maybeRaiseJavaException(context, args, argc);

        RaiseException raise;
        switch (argc) {
            case 0:
                IRubyObject lastException = runtime.getGlobalVariables().get("$!");
                if (lastException.isNil()) {
                    raise = RaiseException.from(runtime, runtime.getRuntimeError(), "");
                } else {
                    // non RubyException value is allowed to be assigned as $!.
                    raise = ((RubyException) lastException).toThrowable();
                }
                break;
            case 1:
                if (args[0] instanceof RubyString) {
                    raise = ((RubyException) runtime.getRuntimeError().newInstance(context, args, block)).toThrowable();
                } else {
                    raise = convertToException(context, args[0], null).toThrowable();
                }
                break;
            case 2:
                raise = convertToException(context, args[0], args[1]).toThrowable();
                break;
            default:
                RubyException exception = convertToException(context, args[0], args[1]);
                exception.setBacktrace(args[2]);
                raise = exception.toThrowable();
                break;
        }

        var exception = raise.getException();
        if (runtime.isDebug()) printExceptionSummary(context, exception);
        if (forceCause || argc > 0 && exception.getCause() == null && cause != exception) exception.setCause(cause);

        throw raise;
    }

    private static void maybeRaiseJavaException(ThreadContext context, final IRubyObject[] args, final int argc) {
        // Check for a Java exception
        IRubyObject maybeException = null;
        switch (argc) {
            case 0:
                maybeException = context.runtime.getGlobalVariables().get("$!");
                break;
            case 1:
                if (args.length == 1) maybeException = args[0];
                break;
        }

        maybeRaiseJavaException(context, maybeException);
    }

    private static void maybeRaiseJavaException(ThreadContext context, final IRubyObject arg) {
        // Check for a Java exception
        if (arg instanceof ConcreteJavaProxy) {
            // looks like someone's trying to raise a Java exception. Let them.
            Object maybeThrowable = ((ConcreteJavaProxy) arg).getObject();

            if (!(maybeThrowable instanceof Throwable)) throw typeError(context, "can't raise a non-Throwable Java object");

            final Throwable ex = (Throwable) maybeThrowable;
            Helpers.throwException(ex);
            return; // not reached
        }
    }

    private static RubyException convertToException(ThreadContext context, IRubyObject obj, IRubyObject optionalMessage) {
        if (!obj.respondsTo("exception")) throw typeError(context, "exception class/object expected");

        IRubyObject exception = optionalMessage == null ?
                obj.callMethod(context, "exception") :
                obj.callMethod(context, "exception", optionalMessage);

        if (!RubyException.class.isInstance(exception)) throw typeError(context, "exception object expected");

        return (RubyException) exception;
    }

    private static void printExceptionSummary(ThreadContext context, RubyException rEx) {
        RubyStackTraceElement[] elements = rEx.getBacktraceElements();
        RubyStackTraceElement firstElement = elements.length > 0 ? elements[0] :
                new RubyStackTraceElement("", "", "(empty)", 0, false);
        String msg = String.format("Exception '%s' at %s:%s - %s\n",
                rEx.getMetaClass(),
                firstElement.getFileName(), firstElement.getLineNumber(),
                TypeConverter.convertToType(rEx, context.runtime.getString(), "to_s"));

        context.runtime.getErrorStream().print(msg);
    }

    /**
     * Require.
     * MRI allows to require ever .rb files or ruby extension dll (.so or .dll depending on system).
     * we allow requiring either .rb files or jars.
     * @param recv ruby object used to call require (any object will do and it won't be used anyway).
     * @param name the name of the file to require
     **/
    @JRubyMethod(name = "require", module = true, visibility = PRIVATE)
    public static IRubyObject require(ThreadContext context, IRubyObject recv, IRubyObject name, Block block) {
        IRubyObject tmp = name.checkStringType();
        RubyString requireName = tmp != context.nil ? (RubyString) tmp : RubyFile.get_path(context, name);

        return requireCommon(context, requireName, block);
    }

    private static IRubyObject requireCommon(ThreadContext context, RubyString name, Block block) {
        RubyString path = checkEmbeddedNulls(context, name);
        return asBoolean(context, context.runtime.getLoadService().require(path.toString()));
    }

    @JRubyMethod(name = "require_relative", module = true, visibility = PRIVATE, reads = SCOPE)
    public static IRubyObject require_relative(ThreadContext context, IRubyObject recv, IRubyObject name){
        Ruby runtime = context.runtime;

        RubyString relativePath = RubyFile.get_path(context, name);

        String file = context.getCurrentStaticScope().getFile();

        if (file == null || file.equals("-") || file.equals("-e") || file.matches("\\A\\((.*)\\)")) {
            throw runtime.newLoadError("cannot infer basepath");
        }

        file = runtime.getLoadService().getPathForLocation(file);

        RubyClass fileClass = runtime.getFile();
        IRubyObject realpath = RubyFile.realpath(context, fileClass, newString(context, file));
        IRubyObject dirname = RubyFile.dirname(context, fileClass, realpath);
        IRubyObject absoluteFeature = RubyFile.expand_path(context, fileClass, relativePath, dirname);

        return RubyKernel.require(context, runtime.getKernel(), absoluteFeature, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "load", module = true, visibility = PRIVATE)
    public static IRubyObject load(ThreadContext context, IRubyObject recv, IRubyObject path, Block block) {
        return loadCommon(context, RubyFile.get_path(context, path), false);
    }

    @JRubyMethod(name = "load", module = true, visibility = PRIVATE)
    public static IRubyObject load(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject wrap, Block block) {
        return loadCommon(context, RubyFile.get_path(context, path), wrap);
    }

    public static IRubyObject load(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 1: return load(context, recv, args[0], block);
            case 2: return load(context, recv, args[0], args[1], block);
        }
        Arity.raiseArgumentError(context, args.length, 1, 2);
        return null; // not reached
    }

    private static IRubyObject loadCommon(ThreadContext context, RubyString path, boolean wrap) {
        context.runtime.getLoadService().load(path.toString(), wrap);

        return context.tru;
    }

    private static IRubyObject loadCommon(ThreadContext context, RubyString path, IRubyObject wrap) {
        String file = path.toString();
        LoadService loadService = context.runtime.getLoadService();

        if (wrap.isNil() || wrap instanceof RubyBoolean) {
            loadService.load(file, wrap.isTrue());
        } else {
            loadService.load(file, wrap);
        }

        return context.tru;
    }

    @JRubyMethod(name = "eval", required = 1, optional = 3, checkArity = false, module = true, visibility = PRIVATE,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public static IRubyObject eval(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, 4);

        return evalCommon(context, recv, args);
    }

    private static IRubyObject evalCommon(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        // string to eval
        RubyString src = args[0].convertToString();

        boolean bindingGiven = args.length > 1 && args[1] != context.nil;
        Binding binding = bindingGiven ? getBindingForEval(context, args[1]) : context.currentBinding();

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
        } else {  // no explicit file/line argument given
            binding.setFile("(eval at " + context.getFileAndLine() + ")");
            binding.setLine(0);
        }

        // set method to current frame's, which should be caller's
        String frameName = context.getCompositeName();
        if (frameName != null) binding.setMethod(frameName);

        if (bindingGiven) recv = binding.getSelf();

        return Interpreter.evalWithBinding(context, recv, src, binding, bindingGiven);
    }

    private static Binding getBindingForEval(ThreadContext context, IRubyObject scope) {
        if (scope instanceof RubyBinding binding) return binding.getBinding().cloneForEval();

        throw typeError(context, scope, "Binding");
    }

    @JRubyMethod(name = "caller", module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller(ThreadContext context, IRubyObject recv) {
        return callerInternal(context, recv, null, null);
    }

    @JRubyMethod(name = "caller", module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller(ThreadContext context, IRubyObject recv, IRubyObject level) {
        return callerInternal(context, recv, level, null);
    }

    @JRubyMethod(name = "caller", module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller(ThreadContext context, IRubyObject recv, IRubyObject level, IRubyObject length) {
        return callerInternal(context, recv, level, length);
    }

    private static IRubyObject callerInternal(ThreadContext context, IRubyObject recv, IRubyObject level, IRubyObject length) {
        if (length != null && length.isNil()) length = null;  // caller(0, nil) should act like caller(0)

        if (length == null) {
            // use Java 8 version of walker to reduce overhead (GH-5857)
            return withLevelAndLength(context, level, length, 1,
                    (ctx, lev, len) -> ThreadContext.WALKER8.walk(stream -> ctx.createCallerBacktrace(lev, len, stream)));
        }

        return withLevelAndLength(context, level, length, 1,
                (ctx, lev, len) -> ThreadContext.WALKER.walk(stream -> ctx.createCallerBacktrace(lev, len, stream)));
    }

    @JRubyMethod(module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller_locations(ThreadContext context, IRubyObject recv) {
        return callerLocationsInternal(context, null, null);
    }

    @JRubyMethod(module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller_locations(ThreadContext context, IRubyObject recv, IRubyObject level) {
        return callerLocationsInternal(context, level, null);
    }

    @JRubyMethod(module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject caller_locations(ThreadContext context, IRubyObject recv, IRubyObject level, IRubyObject length) {
        return callerLocationsInternal(context, level, length);
    }

    private static IRubyObject callerLocationsInternal(ThreadContext context, IRubyObject level, IRubyObject length) {
        if (length == null) {
            // use Java 8 version of walker to reduce overhead (GH-5857)
            return withLevelAndLength(
                    context, level, length, 1,
                    (ctx, lev, len) -> ThreadContext.WALKER8.walk(stream -> ctx.createCallerLocations(lev, len, stream)));
        }

        return withLevelAndLength(
                context, level, length, 1,
                (ctx, lev, len) -> ThreadContext.WALKER.walk(stream -> ctx.createCallerLocations(lev, len, stream)));
    }

    /**
     * Retrieve the level and length from given args, if non-null.
     */
    static <R> R withLevelAndLength(ThreadContext context, IRubyObject level, IRubyObject length, int defaultLevel, ObjectIntIntFunction<ThreadContext, R> func) {
        int lev;
        // Suitably large but no chance to overflow int when combined with level
        int len = 1 << 24;
        if (length != null) {
            lev = RubyNumeric.fix2int(level);
            len = RubyNumeric.fix2int(length);
        } else if (level instanceof RubyRange) {
            RubyRange range = (RubyRange) level;
            IRubyObject first = range.begin(context);
            lev = first.isNil() ? 0 : RubyNumeric.fix2int(first);
            IRubyObject last = range.end(context);
            if (last.isNil()) {
                len = 1 << 24;
            } else {
                len = RubyNumeric.fix2int(last) - lev;
            }
            if (!range.isExcludeEnd()) len++;
            len = len < 0 ? 0 : len;
        } else if (level != null) {
            lev = RubyNumeric.fix2int(level);
        } else {
            lev = defaultLevel;
        }

        if (lev < 0) throw argumentError(context, "negative level (" + lev + ')');
        if (len < 0) throw argumentError(context, "negative size (" + len + ')');

        return func.apply(context, lev, len);
    }

    @JRubyMethod(name = "catch", module = true, visibility = PRIVATE)
    public static IRubyObject rbCatch(ThreadContext context, IRubyObject recv, Block block) {
        return rbCatch(context, recv, new RubyObject(context.runtime.getObject()), block);
    }

    @JRubyMethod(name = "catch", module = true, visibility = PRIVATE)
    public static IRubyObject rbCatch(ThreadContext context, IRubyObject recv, IRubyObject tag, Block block) {
        return new CatchThrow(tag).enter(context, tag, block);
    }

    @JRubyMethod(name = "throw", module = true, visibility = PRIVATE)
    public static IRubyObject rbThrow(ThreadContext context, IRubyObject recv, IRubyObject tag, Block block) {
        return rbThrowInternal(context, tag, null);
    }

    @JRubyMethod(name = "throw", module = true, visibility = PRIVATE)
    public static IRubyObject rbThrow(ThreadContext context, IRubyObject recv, IRubyObject tag, IRubyObject value, Block block) {
        return rbThrowInternal(context, tag, value);
    }

    private static final byte[] uncaught_throw_p = { 'u','n','c','a','u','g','h','t',' ','t','h','r','o','w',' ','%','p' };

    private static IRubyObject rbThrowInternal(ThreadContext context, IRubyObject tag, IRubyObject arg) {
        final Ruby runtime = context.runtime;
        runtime.getGlobalVariables().set("$!", context.nil);

        CatchThrow continuation = context.getActiveCatch(tag);

        if (continuation != null) {
            continuation.args = arg == null ? IRubyObject.NULL_ARRAY : new IRubyObject[] { arg };
            throw continuation;
        }

        // No catch active for this throw
        IRubyObject value = arg == null ? context.nil : arg;
        throw uncaughtThrow(runtime, tag, value, RubyString.newStringShared(runtime, uncaught_throw_p));
    }

    private static RaiseException uncaughtThrow(Ruby runtime, IRubyObject tag, IRubyObject value, RubyString message) {
        return RubyUncaughtThrowError.newUncaughtThrowError(runtime, tag, value, message).toThrowable();
    }

    @JRubyMethod(module = true, visibility = PRIVATE, omit = true)
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        if (arg instanceof RubyHash) { // warn({}) - treat as kwarg
            return warn(context, recv, new IRubyObject[] { arg });
        }

        if (!context.runtime.getVerbose().isNil()) {
            warnObj(context, recv, arg, context.nil);
        }

        return context.nil;
    }

    private static void warnObj(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject category) {
        if (arg instanceof RubyArray) {
            final RubyArray argAry = arg.convertToArray();
            for (int i = 0; i < argAry.size(); i++) warnObj(context, recv, argAry.eltOk(i), category);
            return;
        }

        warnStr(context, recv, arg.asString(), category);
    }

    static void warnStr(ThreadContext context, IRubyObject recv, RubyString message, IRubyObject category) {
        if (!message.endsWithAsciiChar('\n')) message = dupString(context, message).cat('\n', USASCIIEncoding.INSTANCE);

        var warning = context.runtime.getWarning();
        if (recv == warning) {
            RubyWarnings.warn(context, message);
            return;
        }

        // FIXME: This seems "fragile".  Not sure if other transitional Ruby methods need this sort of thing or not (e.g. perhaps we need a helper on callsite for this).
        DynamicMethod method = ((CachingCallSite) sites(context).warn).retrieveCache(warning).method;
        if (method.getSignature().isOneArgument()) {
            sites(context).warn.call(context, recv, warning, message);
        } else {
            RubyHash keywords = RubyHash.newHash(context.runtime, asSymbol(context, "category"), category);
            context.callInfo = ThreadContext.CALL_KEYWORD;
            sites(context).warn.call(context, recv, warning, message, keywords);
        }
    }

    @JRubyMethod(module = true, rest = true, visibility = PRIVATE, omit = true)
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        boolean explicitUplevel = false;
        int uplevel = 0;
        IRubyObject category = context.nil;

        int argMessagesLen = args.length;
        if (argMessagesLen > 0) {
            IRubyObject opts = TypeConverter.checkHashType(context.runtime, args[argMessagesLen - 1]);
            if (opts != context.nil) {
                argMessagesLen--;

                IRubyObject[] ret = ArgsUtil.extractKeywordArgs(context, (RubyHash) opts, "uplevel", "category");
                if (ret[0] != null) {
                    explicitUplevel = true;
                    uplevel = RubyNumeric.num2int(ret[0]);
                    if (uplevel < 0) throw argumentError(context, "negative level (" + uplevel + ")");
                }

                if (ret[1] != null) {
                    category = ret[1].isNil() ?
                            context.nil : TypeConverter.convertToType(ret[1], context.runtime.getSymbol(), "to_sym");
                }
            }
        }

        int i = 0;

        if (!context.runtime.getVerbose().isNil() && argMessagesLen > 0) {
            if (explicitUplevel && argMessagesLen > 0) { // warn(uplevel: X) does nothing
                warnStr(context, recv, buildWarnMessage(context, uplevel, args[0]), category);
                i = 1;
            }

            for (; i < argMessagesLen; i++) {
                warnObj(context, recv, args[i], category);
            }
        }

        return context.nil;
    }

    private static RubyString buildWarnMessage(ThreadContext context, final int uplevel, final IRubyObject arg) {
        RubyStackTraceElement element = context.getSingleBacktraceExact(uplevel);

        RubyString message = RubyString.newStringLight(context.runtime, 32);
        if (element != null) {
            message.catString(element.getFileName()).cat(':').catString(Integer.toString(element.getLineNumber()))
                   .catString(": warning: ");
        } else {
            message.catString("warning: ");
        }
        IRubyObject arg1 = arg.asString();
        return (RubyString) message.op_plus(context, arg1);
    }

    @JRubyMethod(module = true, alias = "then")
    public static IRubyObject yield_self(ThreadContext context, IRubyObject recv, Block block) {
        if (block.isGiven()) {
            return block.yield(context, recv);
        } else {
            return RubyEnumerator.enumeratorizeWithSize(context, recv, "yield_self", RubyKernel::objectSize);
        }
    }

    /**
     * An exactly-one size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject objectSize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // always 1
        return RubyFixnum.one(context.runtime);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject set_trace_func(ThreadContext context, IRubyObject recv, IRubyObject trace_func, Block block) {
        if (trace_func.isNil()) {
            context.traceEvents.setTraceFunction(null);
        } else if (!(trace_func instanceof RubyProc)) {
            throw typeError(context, "trace_func needs to be Proc.");
        } else {
            context.traceEvents.setTraceFunction((RubyProc) trace_func);
        }
        return trace_func;
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject trace_var(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);

        RubyProc proc = null;
        String var = args[0].toString();
        // ignore if it's not a global var
        if (var.charAt(0) != '$') {
            return context.nil;
        }
        if (argc == 1) {
            proc = RubyProc.newProc(context.runtime, block, Block.Type.PROC);
        } else if (argc == 2) {
            if (args[1] instanceof RubyString) {
                RubyString rubyString = newString(context, "proc {");
                RubyString s = rubyString.catWithCodeRange(((RubyString) args[1])).cat('}');
                proc = (RubyProc) evalCommon(context, recv, new IRubyObject[] { s });
            } else {
                proc = (RubyProc) TypeConverter.convertToType(args[1], context.runtime.getProc(), "to_proc", true);
            }
        }

        context.runtime.getGlobalVariables().setTraceVar(var, proc);

        return context.nil;
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject untrace_var(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);

        String var = args[0].toString();

        // ignore if it's not a global var
        if (var.charAt(0) != '$') {
            return context.nil;
        }

        if (argc > 1) {
            ArrayList<IRubyObject> success = new ArrayList<>(argc);
            for (int i = 1; i < argc; i++) {
                if (context.runtime.getGlobalVariables().untraceVar(var, args[i])) {
                    success.add(args[i]);
                }
            }
            return RubyArray.newArray(context.runtime, success);
        } else {
            context.runtime.getGlobalVariables().untraceVar(var);
        }

        return context.nil;
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false)
    public static IRubyObject define_singleton_method(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);

        RubyClass singleton_class = recv.getSingletonClass();
        if (argc > 1) {
            IRubyObject arg1 = args[1];
            if (context.runtime.getUnboundMethod().isInstance(arg1)) {
                RubyUnboundMethod method = (RubyUnboundMethod) arg1;
                RubyModule owner = (RubyModule) method.owner(context);
                if (owner.isSingleton() &&
                    !(recv.getMetaClass().isSingleton() && recv.getMetaClass().isKindOfModule(owner))) {

                    throw typeError(context, "can't bind singleton method to a different class");
                }
            }
            return singleton_class.defineMethodFromCallable(context, args[0], args[1], PUBLIC);
        } else {
            return singleton_class.defineMethodFromBlock(context, args[0], block, PUBLIC);
        }
    }

    @JRubyMethod(name = "proc", module = true, visibility = PRIVATE)
    public static RubyProc proc(ThreadContext context, IRubyObject recv, Block block) {
        return context.runtime.newProc(Block.Type.PROC, block);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static RubyProc lambda(ThreadContext context, IRubyObject recv, Block block) {
        // existing procs remain procs vs becoming lambdas.
        if (block.type == Block.Type.PROC) throw argumentError(context, "the lambda method requires a literal block");

        return context.runtime.newProc(Block.Type.LAMBDA, block);
    }

    @Deprecated
    public static RubyProc proc_1_9(ThreadContext context, IRubyObject recv, Block block) {
        return proc(context, recv, block);
    }

    @JRubyMethod(name = "loop", module = true, visibility = PRIVATE)
    public static IRubyObject loop(ThreadContext context, IRubyObject recv, Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, recv, "loop", RubyKernel::loopSize);
        }
        final Ruby runtime = context.runtime;
        IRubyObject oldExc = runtime.getGlobalVariables().get("$!"); // Save $!
        try {
            while (true) {
                block.yieldSpecific(context);

                context.pollThreadEvents();
            }
        }
        catch (RaiseException ex) {
            final RubyClass StopIteration = runtime.getStopIteration();
            if ( StopIteration.isInstance(ex.getException()) ) {
                runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
                return ex.getException().callMethod("result");
            }
            else {
                throw ex;
            }
        }
    }

    /**
     * A loop size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject loopSize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return RubyFloat.newFloat(context.runtime, RubyFloat.INFINITY);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject test(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return testCommon(context, recv, getTestCommand(context, arg0), arg1, null);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject test(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return testCommon(context, recv, getTestCommand(context, arg0), arg1, arg2);
    }

    private static IRubyObject testCommon(ThreadContext context, IRubyObject recv, int cmd, IRubyObject arg1, IRubyObject arg2) {
        // MRI behavior: now check arg count
        switch (cmd) {
            case '-':
            case '=':
            case '<':
            case '>':
                if (arg2 == null) throw argumentError(context, 2, 3);
                break;
            default:
                if (arg1 == null) throw argumentError(context, 1, 2);
                break;
        }

        switch (cmd) {
        case 'A': // ?A  | Time    | Last access time for file1
            return context.runtime.newFileStat(fileResource(context, arg1).path(), false).atime(context);
        case 'b': // ?b  | boolean | True if file1 is a block device
            return RubyFileTest.blockdev_p(context, recv, arg1);
        case 'c': // ?c  | boolean | True if file1 is a character device
            return RubyFileTest.chardev_p(context, recv, arg1);
        case 'C': // ?C  | Time    | Last change time for file1
            return context.runtime.newFileStat(fileResource(context, arg1).path(), false).ctime(context);
        case 'd': // ?d  | boolean | True if file1 exists and is a directory
            return RubyFileTest.directory_p(context, recv, arg1);
        case 'e': // ?e  | boolean | True if file1 exists
            return RubyFileTest.exist_p(context, recv, arg1);
        case 'f': // ?f  | boolean | True if file1 exists and is a regular file
            return RubyFileTest.file_p(context, recv, arg1);
        case 'g': // ?g  | boolean | True if file1 has the \CF{setgid} bit
            return RubyFileTest.setgid_p(context, recv, arg1);
        case 'G': // ?G  | boolean | True if file1 exists and has a group ownership equal to the caller's group
            return RubyFileTest.grpowned_p(context, recv, arg1);
        case 'k': // ?k  | boolean | True if file1 exists and has the sticky bit set
            return RubyFileTest.sticky_p(context, recv, arg1);
        case 'M': // ?M  | Time    | Last modification time for file1
            return context.runtime.newFileStat(fileResource(context, arg1).path(), false).mtime(context);
        case 'l': // ?l  | boolean | True if file1 exists and is a symbolic link
            return RubyFileTest.symlink_p(context, recv, arg1);
        case 'o': // ?o  | boolean | True if file1 exists and is owned by the caller's effective uid
            return RubyFileTest.owned_p(context, recv, arg1);
        case 'O': // ?O  | boolean | True if file1 exists and is owned by the caller's real uid
            return RubyFileTest.rowned_p(context, recv, arg1);
        case 'p': // ?p  | boolean | True if file1 exists and is a fifo
            return RubyFileTest.pipe_p(context, recv, arg1);
        case 'r': // ?r  | boolean | True if file1 is readable by the effective uid/gid of the caller
            return RubyFileTest.readable_p(context, recv, arg1);
        case 'R': // ?R  | boolean | True if file is readable by the real uid/gid of the caller
            return RubyFileTest.readable_p(context, recv, arg1);
        case 's': // ?s  | int/nil | If file1 has nonzero size, return the size, otherwise nil
            return RubyFileTest.size_p(context, recv, arg1);
        case 'S': // ?S  | boolean | True if file1 exists and is a socket
            return RubyFileTest.socket_p(context, recv, arg1);
        case 'u': // ?u  | boolean | True if file1 has the setuid bit set
            return RubyFileTest.setuid_p(context, recv, arg1);
        case 'w': // ?w  | boolean | True if file1 exists and is writable by effective uid/gid
            return RubyFileTest.writable_p(context, recv, arg1);
        case 'W': // ?W  | boolean | True if file1 exists and is writable by the real uid/gid
            // FIXME: Need to implement an writable_real_p in FileTest
            return RubyFileTest.writable_p(context, recv, arg1);
        case 'x': // ?x  | boolean | True if file1 exists and is executable by the effective uid/gid
            return RubyFileTest.executable_p(context, recv, arg1);
        case 'X': // ?X  | boolean | True if file1 exists and is executable by the real uid/gid
            return RubyFileTest.executable_real_p(context, recv, arg1);
        case 'z': // ?z  | boolean | True if file1 exists and has a zero length
            return RubyFileTest.zero_p(context, recv, arg1);
        case '=': // ?=  | boolean | True if the modification times of file1 and file2 are equal
            return context.runtime.newFileStat(arg1.convertToString().toString(), false).mtimeEquals(context, arg2);
        case '<': // ?<  | boolean | True if the modification time of file1 is prior to that of file2
            return context.runtime.newFileStat(arg1.convertToString().toString(), false).mtimeLessThan(context, arg2);
        case '>': // ?>  | boolean | True if the modification time of file1 is after that of file2
            return context.runtime.newFileStat(arg1.convertToString().toString(), false).mtimeGreaterThan(context, arg2);
        case '-': // ?-  | boolean | True if file1 and file2 are identical
            return RubyFileTest.identical_p(context, recv, arg1, arg2);
        default:
            throw new InternalError("unreachable code reached!");
        }
    }

    private static int getTestCommand(ThreadContext context, IRubyObject arg0) {
        int cmd;
        if (arg0 instanceof RubyFixnum) {
            cmd = (int)((RubyFixnum) arg0).getLongValue();
        } else if (arg0 instanceof RubyString && ((RubyString) arg0).getByteList().length() > 0) {
            // MRI behavior: use first byte of string value if len > 0
            cmd = ((RubyString) arg0).getByteList().charAt(0);
        } else {
            cmd = (int) arg0.convertToInteger().getLongValue();
        }

        // MRI behavior: raise ArgumentError for 'unknown command' before checking number of args
        switch(cmd) {
        case 'A': case 'b': case 'c': case 'C': case 'd': case 'e': case 'f': case 'g': case 'G':
        case 'k': case 'M': case 'l': case 'o': case 'O': case 'p': case 'r': case 'R': case 's':
        case 'S': case 'u': case 'w': case 'W': case 'x': case 'X': case 'z': case '=': case '<':
        case '>': case '-':
            break;
        default:
            throw argumentError(context, "unknown command ?" + (char) cmd);
        }
        return cmd;
    }

    @JRubyMethod(name = "`", module = true, visibility = PRIVATE)
    public static IRubyObject backquote(ThreadContext context, IRubyObject recv, IRubyObject str) {
        Ruby runtime = context.runtime;

        if (PopenExecutor.nativePopenAvailable(runtime)) {
            str = str.convertToString();
            context.setLastExitStatus(context.nil);
            IRubyObject port = PopenExecutor.pipeOpen(context, str, "r", OpenFile.READABLE|OpenFile.TEXTMODE, null);
            if (port.isNil()) return newEmptyString(context);

            OpenFile fptr = ((RubyIO)port).getOpenFileChecked();
            IRubyObject result = fptr.readAll(context, fptr.remainSize(), context.nil);
            ((RubyIO)port).rbIoClose(context);

            return result;
        }

        IRubyObject[] args = new IRubyObject[] { str.convertToString() };
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
        return newString(context, buf);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv) {
        return RubyRandom.srandCommon(context, recv);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return RubyRandom.srandCommon(context, recv, arg);
    }

    @JRubyMethod(name = "rand", module = true, visibility = PRIVATE)
    public static IRubyObject rand(ThreadContext context, IRubyObject recv) {
        return RubyRandom.randFloat(context);
    }

    @JRubyMethod(name = "rand", module = true, visibility = PRIVATE)
    public static IRubyObject rand(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return RubyRandom.randKernel(context, recv, arg);
    }

    @JRubyMethod(rest = true, module = true, visibility = PRIVATE)
    public static RubyFixnum spawn(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyProcess.spawn(context, recv, args);
    }

    @JRubyMethod(required = 1, optional = 9, checkArity = false, module = true, notImplemented = true, visibility = PRIVATE)
    public static IRubyObject syscall(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("Kernel#syscall is not implemented in JRuby");
    }

    @JRubyMethod(name = "system", required = 1, rest = true, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject    system(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, -1);

        final Ruby runtime = context.runtime;
        boolean needChdir = !runtime.getCurrentDirectory().equals(runtime.getPosix().getcwd());

        if (PopenExecutor.nativePopenAvailable(runtime)) {
            // MRI: rb_f_system
            long pid;
            int status;

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
            return executor.systemInternal(context, args, null);
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
            args = dropLastArgIfOptions(runtime, args);
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
        return (int) tuple[0];
    }

    private static IRubyObject[] dropLastArgIfOptions(final Ruby runtime, final IRubyObject[] args) {
        IRubyObject lastArg = args[args.length - 1];
        if (lastArg instanceof RubyHash) {
            if (!((RubyHash) lastArg).isEmpty()) {
                runtime.getWarnings().warn(ID.UNSUPPORTED_SUBPROCESS_OPTION, "system does not support options in JRuby yet: " + lastArg);
            }
            return Arrays.copyOf(args, args.length - 1);
        }
        return args;
    }

    public static IRubyObject exec(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return execCommon(context, null, args[0], null, args);
    }

    /* Actual exec definition which calls this internal version is specified
     * in /core/src/main/ruby/jruby/kernel/kernel.rb.
     */
    @JRubyMethod(required = 4, visibility = PRIVATE)
    public static IRubyObject _exec_internal(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject env = args[0];
        IRubyObject prog = args[1];
        IRubyObject options = args[2];
        var cmdArgs = (RubyArray<?>) args[3];

        if (options instanceof RubyHash) checkExecOptions(context, (RubyHash) options);

        return execCommon(context, env, prog, options, cmdArgs.toJavaArray(context));
    }

    static void checkExecOptions(ThreadContext context, RubyHash opts) {
        checkValidSpawnOptions(context, opts);
        checkUnsupportedOptions(context, opts, UNSUPPORTED_SPAWN_OPTIONS, "unsupported exec option");
    }

    private static IRubyObject execCommon(ThreadContext context, IRubyObject env, IRubyObject prog, IRubyObject options, IRubyObject[] args) {
        // This is a fairly specific hack for empty string, but it does the job
        if (args.length == 1) {
            RubyString command = args[0].convertToString();
            if (command.isEmpty()) throw context.runtime.newErrnoENOENTError(command.toString());

            for (byte b : command.getBytes()) {
                if (b == 0x00) throw argumentError(context, "string contains null byte");
            }
        }

        final Ruby runtime = context.runtime;

        if (env != null && env != context.nil) {
            RubyHash envMap = env.convertToHash();
            if (envMap != null) {
                runtime.getENV().merge_bang(context, new IRubyObject[]{envMap}, Block.NULL_BLOCK);
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

                final POSIX posix = runtime.getPosix();

                posix.chdir(System.getProperty("user.dir"));

                if (Platform.IS_WINDOWS) {
                    // Windows exec logic is much more elaborate; exec() in jnr-posix attempts to duplicate it
                    posix.exec(progStr, argv);
                } else {
                    // TODO: other logic surrounding this call? In jnr-posix?
                    @SuppressWarnings("unchecked")
                    final Map<String, String> ENV = (Map<String, String>) runtime.getENV();
                    ArrayList<String> envStrings = new ArrayList<>(ENV.size() + 1);
                    for ( Map.Entry<String, String> envEntry : ENV.entrySet() ) {
                        envStrings.add( envEntry.getKey() + '=' + envEntry.getValue() );
                    }
                    envStrings.add(null);

                    int status = posix.execve(progStr, argv, envStrings.toArray(new String[envStrings.size()]));
                    if (Platform.IS_WSL && status == -1) { // work-around a bug in Windows Subsystem for Linux
                        if (posix.errno() == Errno.ENOMEM.intValue()) {
                            posix.exec(progStr, argv);
                        }
                    }
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
            if (jmxStopped && runtime.getBeanManager().tryRestartAgent()) runtime.registerMBeans();

            throw runtime.newErrnoFromLastPOSIXErrno();
        }

        // Fall back onto our existing code if native not available
        // FIXME: Make jnr-posix Pure-Java backend do this as well
        int resultCode = ShellLauncher.execAndWait(runtime, args);

        exit(runtime, new IRubyObject[] {asFixnum(context, resultCode)}, true);
        return context.nil; // not reached
    }

    @JRubyMethod(name = "fork", module = true, visibility = PRIVATE, notImplemented = true)
    public static IRubyObject fork(ThreadContext context, IRubyObject recv, Block block) {
        Ruby runtime = context.runtime;
        throw runtime.newNotImplementedError("fork is not available on this platform");
    }

    @JRubyMethod(name = {"to_enum", "enum_for"}, rest = true, keywords = true)
    public static IRubyObject obj_to_enum(final ThreadContext context, IRubyObject self, IRubyObject[] args, final Block block) {
        // to_enum is a bit strange in that it will propagate the arguments it passes to each element it calls.  We are determining
        // whether we have received keywords so we can propagate this info.
        int callInfo = context.callInfo;
        String method = "each";
        SizeFn sizeFn = null;

        if (args.length > 0) {
            method = RubySymbol.retrieveIDSymbol(args[0]).asJavaString();
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (block.isGiven()) {
            sizeFn = (ctx, recv, args1) -> {
                ctx.callInfo = callInfo;
                return block.yieldValues(ctx, args1);
            };
        }

        boolean keywords = (callInfo & ThreadContext.CALL_KEYWORD) != 0 && (callInfo & ThreadContext.CALL_KEYWORD_EMPTY) == 0;

        ThreadContext.resetCallInfo(context);
        return enumeratorizeWithSize(context, self, method, args, sizeFn, keywords);
    }

    @JRubyMethod(name = { "__method__" }, module = true, visibility = PRIVATE, reads = METHODNAME, omit = true)
    public static IRubyObject __method__(ThreadContext context, IRubyObject recv) {
        String frameName = context.getFrameName();
        return frameName == null || frameName == Ruby.ROOT_FRAME_NAME ? context.nil : asSymbol(context, frameName);
    }

    @JRubyMethod(name = { "__callee__" }, module = true, visibility = PRIVATE, reads = METHODNAME, omit = true)
    public static IRubyObject __callee__(ThreadContext context, IRubyObject recv) {
        String frameName = context.getCalleeName();
        return frameName == null || frameName == Ruby.ROOT_FRAME_NAME ? context.nil : asSymbol(context, frameName);
    }

    @JRubyMethod(name = "__dir__", module = true, visibility = PRIVATE, reads = FILENAME)
    public static IRubyObject __dir__(ThreadContext context, IRubyObject recv) {
        // NOTE: not using __FILE__ = context.getFile() since it won't work with JIT
        String __FILE__ = context.getSingleBacktrace().getFileName();

        __FILE__ = context.runtime.getLoadService().getPathForLocation(__FILE__);

        RubyString path = RubyFile.expandPathInternal(context, newString(context, __FILE__), null, false, true);
        return newString(context, RubyFile.dirname(context, path.asJavaString()));
    }

    @JRubyMethod(module = true)
    public static IRubyObject singleton_class(IRubyObject recv) {
        return recv.getSingletonClass();
    }

    @JRubyMethod(rest = true, keywords = true, reads = SCOPE)
    public static IRubyObject public_send(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (args.length == 0) throw argumentError(context, "no method name given");

        String name = RubySymbol.idStringFromObject(context, args[0]);

        if (args.length > 1) {
            args[args.length - 1] = dupIfKeywordRestAtCallsite(context, args[args.length - 1]);
        }

        final int length = args.length - 1;
        args = ( length == 0 ) ? IRubyObject.NULL_ARRAY : ArraySupport.newCopy(args, 1, length);

        final RubyClass klass = RubyBasicObject.getMetaClass(recv);
        CacheEntry entry = klass.searchWithRefinements(name, context.getCurrentStaticScope());
        DynamicMethod method = entry.method;

        if (method.isUndefined() || method.getVisibility() != PUBLIC) {
            return Helpers.callMethodMissing(context, recv, klass, method.getVisibility(), name, CallType.NORMAL, args, block);
        }

        return method.call(context, recv, entry.sourceModule, name, args, block);
    }

    /*
     * Moved binding of these methods here, since Kernel can be included into
     * BasicObject subclasses, and these methods must still work.
     * See JRUBY-4871 (because of RubyObject instead of RubyBasicObject cast)
     * BEGIN delegated bindings:
     */
    @JRubyMethod(name = "eql?")
    public static IRubyObject eql_p(IRubyObject self, IRubyObject obj) {
        return ((RubyBasicObject)self).eql_p(obj);
    }

    @JRubyMethod(name = "===")
    public static IRubyObject op_eqq(ThreadContext context, IRubyObject self, IRubyObject other) {
        return ((RubyBasicObject) self).op_eqq(context, other);
    }

    @JRubyMethod(name = "<=>")
    public static IRubyObject op_cmp(ThreadContext context, IRubyObject self, IRubyObject other) {
        return ((RubyBasicObject) self).op_cmp(context, other);
    }

    @JRubyMethod(name = "initialize_copy", required = 1, visibility = PRIVATE)
    public static IRubyObject initialize_copy(IRubyObject self, IRubyObject original) {
        return ((RubyBasicObject) self).initialize_copy(original);
    }

    // Replaced in jruby/kernel/kernel.rb with Ruby for better caching
    @JRubyMethod(name = "initialize_clone", required = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject initialize_clone(ThreadContext context, IRubyObject self, IRubyObject original) {
        return sites(context).initialize_copy.call(context, self, self, original);
    }

    // Replaced in jruby/kernel/kernel.rb with Ruby for better caching
    @JRubyMethod(name = "initialize_dup", required = 1, visibility = Visibility.PRIVATE)
    public static IRubyObject initialize_dup(ThreadContext context, IRubyObject self, IRubyObject original) {
        return sites(context).initialize_copy.call(context, self, self, original);
    }

    @Deprecated
    public static RubyBoolean respond_to_p(IRubyObject self, IRubyObject mname) {
        return ((RubyBasicObject) self).respond_to_p(mname);
    }

    @Deprecated
    public static RubyBoolean respond_to_p(IRubyObject self, IRubyObject mname, IRubyObject includePrivate) {
        return ((RubyBasicObject) self).respond_to_p(mname, includePrivate);
    }

    @JRubyMethod(name = "respond_to?")
    public static IRubyObject respond_to_p(ThreadContext context, IRubyObject self, IRubyObject name) {
        return ((RubyBasicObject) self).respond_to_p(context, name, false);
    }

    @JRubyMethod(name = "respond_to?")
    public static IRubyObject respond_to_p(ThreadContext context, IRubyObject self, IRubyObject name, IRubyObject includePrivate) {
        return ((RubyBasicObject) self).respond_to_p(context, name, includePrivate.isTrue());
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
    public static IRubyObject rbClone(ThreadContext context, IRubyObject self) {
        return self.rbClone();
    }

    @JRubyMethod(name = "clone")
    public static IRubyObject rbClone(ThreadContext context, IRubyObject self, IRubyObject opts) {
        return ((RubyBasicObject) self).rbClone(context, opts);
    }

    @JRubyMethod
    public static IRubyObject dup(IRubyObject self) {
        return ((RubyBasicObject)self).dup();
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public static IRubyObject display(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return ((RubyBasicObject)self).display(context, args);
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

    @JRubyMethod(name = "instance_of?")
    public static RubyBoolean instance_of_p(ThreadContext context, IRubyObject self, IRubyObject type) {
        return ((RubyBasicObject)self).instance_of_p(context, type);
    }

    @JRubyMethod(name = "itself")
    public static IRubyObject itself(IRubyObject self) {
        return self;
    }

    @JRubyMethod(name = {"kind_of?", "is_a?"})
    public static RubyBoolean kind_of_p(ThreadContext context, IRubyObject self, IRubyObject type) {
        return ((RubyBasicObject)self).kind_of_p(context, type);
    }

    @JRubyMethod(name = "methods", optional = 1, checkArity = false)
    public static IRubyObject methods(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return ((RubyBasicObject)self).methods(context, args);
    }

    @JRubyMethod(name = "object_id")
    public static IRubyObject object_id(IRubyObject self) {
        return self.id();
    }

    @JRubyMethod(name = "public_methods", optional = 1, checkArity = false)
    public static IRubyObject public_methods(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return ((RubyBasicObject)self).public_methods(context, args);
    }

    @JRubyMethod(name = "protected_methods", optional = 1, checkArity = false)
    public static IRubyObject protected_methods(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return ((RubyBasicObject)self).protected_methods(context, args);
    }

    @JRubyMethod(name = "private_methods", optional = 1, checkArity = false)
    public static IRubyObject private_methods(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return ((RubyBasicObject)self).private_methods(context, args);
    }

    @JRubyMethod(name = "singleton_methods", optional = 1, checkArity = false)
    public static RubyArray singleton_methods(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return ((RubyBasicObject)self).singleton_methods(context, args);
    }

    @JRubyMethod(name = "singleton_method")
    public static IRubyObject singleton_method(IRubyObject self, IRubyObject symbol) {
        return ((RubyBasicObject)self).singleton_method(symbol);
    }

    @JRubyMethod(name = "method", required = 1, reads = SCOPE)
    public static IRubyObject method(ThreadContext context, IRubyObject self, IRubyObject symbol) {
        return ((RubyBasicObject)self).method(symbol, context.getCurrentStaticScope());
    }

    /**
     * @param self
     * @return ""
     * @deprecated Use {@link RubyKernel#to_s(ThreadContext, IRubyObject)}
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static IRubyObject to_s(IRubyObject self) {
        return to_s(self.getRuntime().getCurrentContext(), self);
    }

    @JRubyMethod(name = "to_s")
    public static IRubyObject to_s(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject) self).to_s(context);
    }

    @Deprecated
    public static IRubyObject extend(IRubyObject self, IRubyObject[] args) {
        return extend(self.getRuntime().getCurrentContext(), self, args);
    }

    @JRubyMethod(name = "extend", required = 1, rest = true, checkArity = false)
    public static IRubyObject extend(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, -1);

        return ((RubyBasicObject)self).extend(args);
    }

    @JRubyMethod(name = "send", omit = true, keywords = true)
    public static IRubyObject send(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        return ((RubyBasicObject)self).send(context, arg0, block);
    }
    @JRubyMethod(name = "send", omit = true, keywords = true)
    public static IRubyObject send(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        return ((RubyBasicObject)self).send(context, arg0, arg1, block);
    }
    @JRubyMethod(name = "send", omit = true, keywords = true)
    public static IRubyObject send(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return ((RubyBasicObject)self).send(context, arg0, arg1, arg2, block);
    }
    @JRubyMethod(name = "send", required = 1, rest = true, checkArity = false, omit = true, keywords = true)
    public static IRubyObject send(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 1, -1);

        return ((RubyBasicObject)self).send(context, args, block);
    }

    @JRubyMethod(name = "nil?")
    public static IRubyObject nil_p(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject) self).nil_p(context);
    }

    // Writes backref due to decendant calls ending up in Regexp#=~
    @JRubyMethod(name = "!~", writes = FrameField.BACKREF)
    public static IRubyObject op_not_match(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return ((RubyBasicObject) self).op_not_match(context, arg);
    }

    @JRubyMethod(name = "instance_variable_defined?")
    public static IRubyObject instance_variable_defined_p(ThreadContext context, IRubyObject self, IRubyObject name) {
        return ((RubyBasicObject)self).instance_variable_defined_p(context, name);
    }

    @JRubyMethod(name = "instance_variable_get")
    public static IRubyObject instance_variable_get(ThreadContext context, IRubyObject self, IRubyObject name) {
        return ((RubyBasicObject)self).instance_variable_get(context, name);
    }

    @JRubyMethod(name = "instance_variable_set")
    public static IRubyObject instance_variable_set(IRubyObject self, IRubyObject name, IRubyObject value) {
        return ((RubyBasicObject)self).instance_variable_set(name, value);
    }

    @JRubyMethod(name = "remove_instance_variable")
    public static IRubyObject remove_instance_variable(ThreadContext context, IRubyObject self, IRubyObject name, Block block) {
        return ((RubyBasicObject) self).remove_instance_variable(context, name, block);
    }

    @JRubyMethod(name = "instance_variables")
    public static RubyArray instance_variables(ThreadContext context, IRubyObject self) {
        return ((RubyBasicObject) self).instance_variables(context);
    }

    /* end delegated bindings */

    public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
        RubyString str = (RubyString) getLastlineString(context).dup();

        if (!str.gsub_bang(context, arg0, block).isNil()) {
            context.setLastLine(str);
        }

        return str;
    }

    public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
        RubyString str = (RubyString) getLastlineString(context).dup();

        if (!str.gsub_bang(context, arg0, arg1, block).isNil()) {
            context.setLastLine(str);
        }

        return str;
    }

    public static IRubyObject rbClone(IRubyObject self) {
        return rbClone(self.getRuntime().getCurrentContext(), self);
    }

    public static class LoopMethods {
        @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
        public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
            return context.setLastLine(getLastlineString(context).gsub(context, arg0, block));
        }

        @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
        public static IRubyObject gsub(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
            return context.setLastLine(getLastlineString(context).gsub(context, arg0, arg1, block));
        }

        @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
        public static IRubyObject sub(ThreadContext context, IRubyObject recv, IRubyObject arg0, Block block) {
            return context.setLastLine(getLastlineString(context).sub(context, arg0, block));
        }

        @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
        public static IRubyObject sub(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, Block block) {
            return context.setLastLine(getLastlineString(context).sub(context, arg0, arg1, block));
        }

        @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
        public static IRubyObject chop(ThreadContext context, IRubyObject recv) {
            return context.setLastLine(getLastlineString(context).chop(context));
        }

        @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
        public static IRubyObject chomp(ThreadContext context, IRubyObject recv) {
            return context.setLastLine(getLastlineString(context).chomp(context));
        }

        @JRubyMethod(module = true, visibility = PRIVATE, reads = LASTLINE, writes = LASTLINE)
        public static IRubyObject chomp(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
            return context.setLastLine(getLastlineString(context).chomp(context, arg0));
        }
    }

    private static KernelSites sites(ThreadContext context) {
        return context.sites.Kernel;
    }

    @Deprecated
    public static IRubyObject methodMissing(ThreadContext context, IRubyObject recv, String name, Visibility lastVis, CallType lastCallType, IRubyObject[] args, Block block) {
        return methodMissing(context, recv, name, lastVis, lastCallType, args);
    }

    @Deprecated
    private static IRubyObject caller(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0:
                return caller(context, recv);
            case 1:
                return caller(context, recv, args[0]);
            case 2:
                return caller(context, recv, args[0], args[1]);
            default:
                Arity.checkArgumentCount(context, args, 0, 2);
                return null; // not reached
        }
    }

    @Deprecated
    public static IRubyObject caller_locations(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return caller_locations(context, recv);
            case 1:
                return caller_locations(context, recv, args[0]);
            case 2:
                return caller_locations(context, recv, args[0], args[1]);
            default:
                Arity.checkArgumentCount(context, args, 0, 2);
                return null; // not reached
        }
    }

    @Deprecated
    public static IRubyObject require(IRubyObject recv, IRubyObject name, Block block) {
        return require(recv.getRuntime().getCurrentContext(), recv, name, block);
    }

    @Deprecated
    public static IRubyObject op_match(ThreadContext context, IRubyObject self, IRubyObject arg) {
        context.runtime.getWarnings().warn(ID.DEPRECATED_METHOD,
                "deprecated Object#=~ is called on " + ((RubyBasicObject) self).type() +
                        "; it always returns nil");
        return ((RubyBasicObject) self).op_match(context, arg);
    }

    @Deprecated
    public static IRubyObject autoload(final IRubyObject recv, IRubyObject symbol, IRubyObject file) {
        return autoload(recv.getRuntime().getCurrentContext(), recv, symbol, file);
    }

    @Deprecated
    public static IRubyObject rand(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return RubyRandom.randFloat(context);
            case 1:
                return RubyRandom.randKernel(context, recv, args[0]);
            default:
                throw argumentError(context, args.length, 0, 1);
        }
    }

    @Deprecated
    public static IRubyObject method(IRubyObject self, IRubyObject symbol) {
        return ((RubyBasicObject)self).method(symbol);
    }

    // defined in Ruby now but left here for backward compat
    @Deprecated
    public static IRubyObject tap(ThreadContext context, IRubyObject recv, Block block) {
        if (block.getProcObject() != null) {
            block.getProcObject().call(context, recv);
        } else {
            block.yield(context, recv);
        }
        return recv;
    }

    @Deprecated
    public static IRubyObject sleep(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return sleep(context, recv);
            case 1:
                return sleep(context, recv, args[0]);
            default:
                throw argumentError(context, args.length, 0, 1);
        }
    }

    @Deprecated
    public static IRubyObject test(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 2:
                return test(context, recv, args[0], args[1]);
            case 3:
                return test(context, recv, args[0], args[1], args[2]);
            default:
                throw argumentError(context, args.length, 2, 3);
        }
    }
}
