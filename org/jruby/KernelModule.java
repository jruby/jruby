/*
 * RubyKernel.java
 * Created on May 2, 2002
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina,
 * Chad Fowler, Anders Bengtsson
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby;

import java.util.*;
import java.io.*;

import org.jruby.internal.runtime.builtin.definitions.Kernel;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.EOFError;
import org.jruby.exceptions.SystemExit;
import org.jruby.exceptions.TypeError;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThrowJump;
import org.jruby.exceptions.NotImplementedError;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class KernelModule {

    public static RubyModule createKernelModule(Ruby runtime) {
        return new Kernel(runtime).getModule();
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args) {
        if (args[0].toString().startsWith("|")) {
            // +++
            return recv.getRuntime().getNil();
            // ---
        }
        return RubyFile.open(recv.getRuntime().getClasses().getFileClass(), args);
    }

    public static RubyString gets(IRubyObject recv, IRubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) recv.getRuntime().getGlobalVar("$<");

        RubyString line = argsFile.internalGets(args);

        recv.getRuntime().setLastline(line);

        return line;
    }

    public static IRubyObject p(IRubyObject recv, IRubyObject args[]) {
        IRubyObject defout = recv.getRuntime().getGlobalVar("$>");

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                defout.callMethod("write", args[i].callMethod("inspect"));
                defout.callMethod("write", RubyString.newString(recv.getRuntime(), "\n"));
            }
        }
        return recv.getRuntime().getNil();
    }

    public static IRubyObject puts(IRubyObject recv, IRubyObject args[]) {
        IRubyObject defout = recv.getRuntime().getGlobalVar("$>");

        RubyIO.puts(defout, args);

        return recv.getRuntime().getNil();
    }

    public static IRubyObject print(IRubyObject recv, IRubyObject args[]) {
        IRubyObject defout = recv.getRuntime().getGlobalVar("$>");

        RubyIO.print(defout, args);

        return recv.getRuntime().getNil();
    }

    public static IRubyObject printf(IRubyObject recv, IRubyObject args[]) {
        if (args.length != 0) {
            IRubyObject defout = recv.getRuntime().getGlobalVar("$>");

            if (!(args[0] instanceof RubyString)) {
                defout = args[0];

                IRubyObject[] newArgs = new IRubyObject[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                args = newArgs;
            }

            RubyIO.printf(defout, args);
        }

        return recv.getRuntime().getNil();
    }

    public static RubyString readline(IRubyObject recv, IRubyObject[] args) {
        RubyString line = gets(recv, args);

        if (line.isNil()) {
            throw new EOFError(recv.getRuntime());
        }

        return line;
    }

    public static RubyArray readlines(IRubyObject recv, IRubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) recv.getRuntime().getGlobalVar("$<");

        RubyArray lines = RubyArray.newArray(recv.getRuntime());

        RubyString line = argsFile.internalGets(args);
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
    private static RubyString getLastlineString(Ruby ruby) {
        IRubyObject line = ruby.getLastline();

        if (line.isNil()) {
            throw new TypeError(ruby, "$_ value need to be String (nil given).");
        } else if (!(line instanceof RubyString)) {
            throw new TypeError(ruby, "$_ value need to be String (" + line.getInternalClass().toName() + " given).");
        } else {
            return (RubyString) line;
        }
    }

    public static IRubyObject sub_bang(IRubyObject recv, IRubyObject args[]) {
        return getLastlineString(recv.getRuntime()).sub_bang(args);
    }

    public static IRubyObject sub(IRubyObject recv, IRubyObject args[]) {
        RubyString str = (RubyString) getLastlineString(recv.getRuntime()).dup();

        if (!str.sub_bang(args).isNil()) {
            recv.getRuntime().setLastline(str);
        }

        return str;
    }

    public static IRubyObject gsub_bang(IRubyObject recv, IRubyObject args[]) {
        return getLastlineString(recv.getRuntime()).gsub_bang(args);
    }

    public static IRubyObject gsub(IRubyObject recv, IRubyObject args[]) {
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
        } else {
            recv.getRuntime().setLastline(dup);
            return str;
        }
    }

    public static IRubyObject split(IRubyObject recv, IRubyObject[] args) {
        return getLastlineString(recv.getRuntime()).split(args);
    }

    public static IRubyObject scan(IRubyObject recv, IRubyObject pattern) {
        return getLastlineString(recv.getRuntime()).scan(pattern);
    }

    public static IRubyObject sleep(IRubyObject recv, IRubyObject seconds) {
        try {
            Thread.sleep((long) (RubyNumeric.numericValue(seconds).getDoubleValue() * 1000.0));
        } catch (InterruptedException iExcptn) {
        }

        return recv;
    }

    public static IRubyObject exit(IRubyObject recv, IRubyObject args[]) {
        recv.getRuntime().secure(4);

        int status = 0;
        if (args.length > 0) {
            status = RubyNumeric.fix2int(args[0]);
        }

        throw new SystemExit(recv.getRuntime(), status);
    }

    /** Returns an Array with the names of all global variables.
     *
     */
    public static RubyArray global_variables(IRubyObject recv) {
        RubyArray globalVariables = RubyArray.newArray(recv.getRuntime());

        Iterator iter = recv.getRuntime().globalVariableNames();
        while (iter.hasNext()) {
            String globalVariableName = (String) iter.next();

            globalVariables.append(RubyString.newString(recv.getRuntime(), globalVariableName));
        }

        return globalVariables;
    }

    /** Returns an Array with the names of all local variables.
     *
     */
    public static RubyArray local_variables(IRubyObject recv) {
        RubyArray localVariables = RubyArray.newArray(recv.getRuntime());

        if (recv.getRuntime().getScope().getLocalNames() != null) {
            for (int i = 2; i < recv.getRuntime().getScope().getLocalNames().size(); i++) {
                if (recv.getRuntime().getScope().getLocalNames().get(i) != null) {
                    localVariables.append(RubyString.newString(recv.getRuntime(), (String) recv.getRuntime().getScope().getLocalNames().get(i)));
                }
            }
        }

        Iterator dynamicNames = recv.getRuntime().getDynamicNames().iterator();
        while (dynamicNames.hasNext()) {
            String name = (String) dynamicNames.next();
            localVariables.append(RubyString.newString(recv.getRuntime(), name));
        }

        return localVariables;
    }

    public static RubyBoolean block_given(IRubyObject recv) {
        return RubyBoolean.newBoolean(recv.getRuntime(), recv.getRuntime().isFBlockGiven());
    }

    public static IRubyObject sprintf(IRubyObject recv, IRubyObject args[]) {
        if (args.length == 0) {
            throw new ArgumentError(recv.getRuntime(), "sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        RubyArray newArgs = RubyArray.newArray(recv.getRuntime(), args);
        newArgs.shift();

        return str.format(newArgs);
    }

    public static IRubyObject raise(IRubyObject recv, IRubyObject args[]) {
        Ruby runtime = recv.getRuntime();
        switch (args.length) {
        case 0 :
            IRubyObject defaultException = runtime.getGlobalVar("$!");
            if (defaultException.isNil()) {
                throw new RaiseException(runtime, runtime.getExceptions().getRuntimeError(), "");
            }
            throw new RaiseException((RubyException) defaultException);
        case 1 :
            if (args[0] instanceof RubyException) {
                throw new RaiseException((RubyException) args[0]);
            } else {
                throw new RaiseException(RubyException.newInstance(runtime.getExceptions().getRuntimeError(), args));
            }
        case 2 :
            RubyException excptn = (RubyException) args[0].callMethod("exception", args[1]);
            throw new RaiseException(excptn);
        default :
            throw new ArgumentError(runtime, "wrong # of arguments");
        }
    }

    /**
     * Require.
     * MRI allows to require ever .rb files or ruby extension dll (.so or .dll depending on system).
     * we allow requiring either .rb files or jars.
     * @param recv ruby object used to call require (any object will do and it won't be used anyway).
     * @param file the name of the file to require
     **/
    public static IRubyObject require(IRubyObject recv, IRubyObject file) {
        if (recv.getRuntime().getLoadService().require(file.toString())) {
            return recv.getRuntime().getTrue();
        }
        return recv.getRuntime().getFalse();
    }

    public static IRubyObject load(IRubyObject recv, IRubyObject[] args) {
        RubyString file = (RubyString)args[0];
        if (recv.getRuntime().getLoadService().load(file.toString())) {
            return recv.getRuntime().getTrue();
        }
        return recv.getRuntime().getFalse();
    }

    public static IRubyObject eval(IRubyObject recv, IRubyObject[] args) {
        RubyString src = (RubyString)args[0];
        IRubyObject scope = args.length > 1 ? args[1] : recv.getRuntime().getNil();
        String file = "(eval)";
        int line = 1;

        if (args.length > 2) {
            // +++
            file = args[2].toString();
            // ---
        }

        if (args.length > 3) {
            line = RubyFixnum.fix2int(args[3]);
        }

        // +++
        src.checkSafeString();
        // ---

        if (scope.isNil() && recv.getRuntime().getFrameStack().getPrevious() != null) {
            try {
                // XXX
                recv.getRuntime().getFrameStack().push(recv.getRuntime().getFrameStack().getPrevious());

                return recv.eval(src, scope, file, line);
            } finally {
                recv.getRuntime().getFrameStack().pop();
            }
        }

        return recv.eval(src, scope, file, line);
    }

    public static IRubyObject caller(IRubyObject recv, IRubyObject[] args) {
        int level = args.length > 0 ? RubyFixnum.fix2int(args[0]) : 1;

        if (level < 0) {
            throw new ArgumentError(recv.getRuntime(), "negative level(" + level + ')');
        }

        return RaiseException.createBacktrace(recv.getRuntime(), level);
    }

    public static IRubyObject rbCatch(IRubyObject recv, IRubyObject tag) {
        try {
            return recv.getRuntime().yield(tag);
        } catch (ThrowJump throwJump) {
            if (throwJump.getTag().equals(tag.toId())) {
                return throwJump.getValue();
            } else {
                throw throwJump;
            }
        }
    }

    public static IRubyObject rbThrow(IRubyObject recv, IRubyObject[] args) {
        throw new ThrowJump(args[0].toId(), args.length > 1 ? args[1] : recv.getRuntime().getNil());
    }

    public static IRubyObject set_trace_func(IRubyObject recv, IRubyObject trace_func) {
        if (trace_func.isNil()) {
            recv.getRuntime().getRuntime().setTraceFunction(null);
        } else if (!(trace_func instanceof RubyProc)) {
            throw new TypeError(recv.getRuntime(), "trace_func needs to be Proc.");
        } else {
            recv.getRuntime().getRuntime().setTraceFunction((RubyProc) trace_func);
        }
        return trace_func;
    }

    public static RubyProc proc(IRubyObject recv) {
        return RubyProc.newProc(recv.getRuntime());
    }

    public static IRubyObject loop(IRubyObject recv) {
        while (true) {
            recv.getRuntime().yield(recv.getRuntime().getNil());

            Thread.yield();
        }
    }

    public static IRubyObject backquote(IRubyObject recv, IRubyObject aString) {
        // FIXME clean this up.
        try {
            String lShellProp = System.getProperty("jruby.shell");
            Process aProcess;
            String lCommand = aString.toString();
            String lSwitch = "-c";
            if (lShellProp != null) {
                if (!lShellProp.endsWith("sh")) { //case windowslike
                    lSwitch = "/c";
                }
                aProcess = Runtime.getRuntime().exec(new String[] { lShellProp, lSwitch, lCommand });
            } else {
                aProcess = Runtime.getRuntime().exec(lCommand);
            }

            final StringBuffer sb = new StringBuffer();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            aProcess.waitFor();

            return RubyString.newString(recv.getRuntime(), sb.toString());
        } catch (Exception excptn) {
            excptn.printStackTrace();

            return RubyString.newString(recv.getRuntime(), "");
        }
    }

    public static RubyInteger srand(IRubyObject recv, IRubyObject[] args) {

        long oldRandomSeed = recv.getRuntime().randomSeed;
        if (args.length > 0) {
            RubyInteger integerSeed = (RubyInteger) args[0].convertToType("Integer", "to_int", true);
            recv.getRuntime().randomSeed = integerSeed.getLongValue();
        } else {
            recv.getRuntime().randomSeed = System.currentTimeMillis(); // FIXME
        }
        recv.getRuntime().random.setSeed(recv.getRuntime().randomSeed);
        return RubyFixnum.newFixnum(recv.getRuntime(), oldRandomSeed);
    }

    public static RubyNumeric rand(IRubyObject recv, IRubyObject args[]) {
        long ceil;
        if (args.length == 0) {
            ceil = 0;
        } else if (args.length == 1) {
            RubyInteger integerCeil = (RubyInteger) args[0].convertToType("Integer", "to_int", true);
            ceil = integerCeil.getLongValue();
            ceil = Math.abs(ceil);
            if (ceil > Integer.MAX_VALUE) {
                throw new NotImplementedError(recv.getRuntime(), "Random values larger than Integer.MAX_VALUE not supported");
            }
        } else {
            throw new ArgumentError(recv.getRuntime(), "wrong # of arguments(" + args.length + " for 1)");
        }

        if (ceil == 0) {
            double result = recv.getRuntime().random.nextDouble();
            return RubyFloat.newFloat(recv.getRuntime(), result);
        } else {
            return RubyFixnum.newFixnum(recv.getRuntime(), recv.getRuntime().random.nextInt((int) ceil));
        }
    }
}