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

import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 *
 */

public class RubyKernel {

    public static RubyModule createKernelModule(Ruby ruby) {
        RubyModule kernelModule = ruby.defineModule("Kernel");

        kernelModule.defineMethod("open", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "open"));
        kernelModule.defineMethod("format", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "sprintf"));
        kernelModule.defineMethod("gets", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "gets"));
        kernelModule.defineMethod("p", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "p"));
        kernelModule.defineMethod("print", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "print"));
        kernelModule.defineMethod("printf", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "printf"));
        kernelModule.defineMethod("puts", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "puts"));
        kernelModule.defineMethod("readline", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "readline"));
        kernelModule.defineMethod("readlines", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "readlines"));
        kernelModule.defineMethod("sprintf", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "sprintf"));
        kernelModule.defineMethod("gsub!", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "gsub_bang"));
        kernelModule.defineMethod("gsub", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "gsub"));
        kernelModule.defineMethod("sub!", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "sub_bang"));
        kernelModule.defineMethod("sub", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "sub"));

        kernelModule.defineMethod("chop!", CallbackFactory.getSingletonMethod(RubyKernel.class, "chop_bang"));
        kernelModule.defineMethod("chop", CallbackFactory.getSingletonMethod(RubyKernel.class, "chop"));
        kernelModule.defineMethod("chomp!", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "chomp_bang"));
        kernelModule.defineMethod("chomp", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "chomp"));
        kernelModule.defineMethod("split", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "split"));
        kernelModule.defineMethod("scan", CallbackFactory.getSingletonMethod(RubyKernel.class, "scan", RubyObject.class));
        kernelModule.defineMethod("load", CallbackFactory.getSingletonMethod(RubyKernel.class, "load", RubyString.class));
        //kernelModule.defineMethod("autoload", CallbackFactory.getSingletonMethod(RubyKernel.class, "autoload", RubyString.class));
        kernelModule.defineMethod("raise", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "raise"));
        kernelModule.defineMethod("require", CallbackFactory.getSingletonMethod(RubyKernel.class, "require", RubyString.class));
        kernelModule.defineMethod("global_variables", CallbackFactory.getSingletonMethod(RubyKernel.class, "global_variables"));
        kernelModule.defineMethod("local_variables", CallbackFactory.getSingletonMethod(RubyKernel.class, "local_variables"));
        kernelModule.defineMethod("block_given?", CallbackFactory.getSingletonMethod(RubyKernel.class, "block_given"));
        kernelModule.defineMethod("iterator?", CallbackFactory.getSingletonMethod(RubyKernel.class, "block_given"));
        kernelModule.defineMethod("proc", CallbackFactory.getSingletonMethod(RubyKernel.class, "proc"));
        kernelModule.defineMethod("loop", CallbackFactory.getSingletonMethod(RubyKernel.class, "loop"));
        kernelModule.defineMethod("eval", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "eval", RubyString.class));
        kernelModule.defineMethod("caller", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "caller"));
        kernelModule.defineMethod("catch", CallbackFactory.getSingletonMethod(RubyKernel.class, "rbCatch", RubyObject.class));
        kernelModule.defineMethod("throw", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "rbThrow", RubyObject.class));
        kernelModule.defineMethod("singleton_method_added", CallbackFactory.getNilMethod());
        kernelModule.defineMethod("set_trace_func", CallbackFactory.getSingletonMethod(RubyKernel.class, "set_trace_func", RubyObject.class));
        kernelModule.defineMethod("`", CallbackFactory.getSingletonMethod(RubyKernel.class, "backquote", RubyString.class));
        kernelModule.defineMethod("exit", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "exit"));
        kernelModule.defineMethod("srand", CallbackFactory.getSingletonMethod(RubyKernel.class, "srand", RubyObject.class));
        kernelModule.defineMethod("rand", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "rand"));

        kernelModule.defineAlias("lambda", "proc");

        return kernelModule;
    }


    public static RubyObject open(Ruby ruby, RubyObject recv, RubyObject[] args) {
        if (args[0].toString().startsWith("|")) {
            // +++
            return ruby.getNil();
            // ---
        }
        return RubyFile.open(ruby, ruby.getClasses().getFileClass(), args);
    }

    public static RubyString gets(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) ruby.getGlobalVar("$<");

        RubyString line = argsFile.internalGets(args);

        ruby.setLastline(line);

        return line;
    }

    public static RubyObject p(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyObject defout = ruby.getGlobalVar("$>");

        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                defout.funcall("write", args[i].funcall("inspect"));
                defout.funcall("write", RubyString.newString(ruby, "\n"));
            }
        }
        return ruby.getNil();
    }


    public static RubyObject puts(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyObject defout = ruby.getGlobalVar("$>");

        RubyIO.puts(ruby, defout, args);

        return ruby.getNil();
    }

    public static RubyObject print(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyObject defout = ruby.getGlobalVar("$>");

        RubyIO.print(ruby, defout, args);

        return ruby.getNil();
    }

    public static RubyObject printf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length != 0) {
            RubyObject defout = ruby.getGlobalVar("$>");

            if (!(args[0] instanceof RubyString)) {
                defout = args[0];

                RubyObject[] newArgs = new RubyObject[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                args = newArgs;
            }

            RubyIO.printf(ruby, defout, args);
        }

        return ruby.getNil();
    }


    public static RubyString readline(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyString line = gets(ruby, recv, args);

        if (line.isNil()) {
            throw new EOFError(ruby);
        }

        return line;
    }

    public static RubyArray readlines(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) ruby.getGlobalVar("$<");

        RubyArray lines = RubyArray.newArray(ruby);

        RubyString line = argsFile.internalGets(args);
        while (!line.isNil()) {
            lines.push(line);

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
        RubyObject line = ruby.getLastline();

        if (line.isNil()) {
            throw new TypeError(ruby, "$_ value need to be String (nil given).");
        } else if (!(line instanceof RubyString)) {
            throw new TypeError(ruby, "$_ value need to be String (" + line.getRubyClass().toName() + " given).");
        } else {
            return (RubyString) line;
        }
    }

    public static RubyObject sub_bang(Ruby ruby, RubyObject recv, RubyObject args[]) {
        return getLastlineString(ruby).sub_bang(args);
    }

    public static RubyObject sub(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyString str = (RubyString) getLastlineString(ruby).dup();

        if (!str.sub_bang(args).isNil()) {
            ruby.setLastline(str);
        }

        return str;
    }

    public static RubyObject gsub_bang(Ruby ruby, RubyObject recv, RubyObject args[]) {
        return getLastlineString(ruby).gsub_bang(args);
    }

    public static RubyObject gsub(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyString str = (RubyString) getLastlineString(ruby).dup();

        if (!str.gsub_bang(args).isNil()) {
            ruby.setLastline(str);
        }

        return str;
    }

    public static RubyObject chop_bang(Ruby ruby, RubyObject recv) {
        return getLastlineString(ruby).chop_bang();
    }

    public static RubyObject chop(Ruby ruby, RubyObject recv) {
        RubyString str = getLastlineString(ruby);

        if (str.getValue().length() > 0) {
            str = (RubyString) str.dup();
            str.chop_bang();
            ruby.setLastline(str);
        }

        return str;
    }

    public static RubyObject chomp_bang(Ruby ruby, RubyObject recv, RubyObject[] args) {
        return getLastlineString(ruby).chomp_bang(args);
    }

    public static RubyObject chomp(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyString str = getLastlineString(ruby);
        RubyString dup = (RubyString) str.dup();

        if (dup.chomp_bang(args).isNil()) {
            return str;
        } else {
            ruby.setLastline(dup);
            return str;
        }
    }

    public static RubyObject split(Ruby ruby, RubyObject recv, RubyObject[] args) {
        return getLastlineString(ruby).split(args);
    }

    public static RubyObject scan(Ruby ruby, RubyObject recv, RubyObject pattern) {
        return getLastlineString(ruby).scan(pattern);
    }


    public static RubyObject exit(Ruby ruby, RubyObject recv, RubyObject args[]) {
        int status = 0;

        if (args.length > 0) {
            status = RubyNumeric.fix2int(args[0]);
        }

        System.exit(status);
        return ruby.getNil();
    }

    /** Returns an Array with the names of all global variables.
     * 
     */
    public static RubyArray global_variables(Ruby ruby, RubyObject recv) {
        RubyArray globalVariables = RubyArray.newArray(ruby);

        Iterator iter = ruby.getGlobalMap().keySet().iterator();
        while (iter.hasNext()) {
            String globalVariableName = (String) iter.next();

            globalVariables.push(RubyString.newString(ruby, globalVariableName));
        }

        return globalVariables;
    }

    /** Returns an Array with the names of all local variables.
     * 
     */
    public static RubyArray local_variables(Ruby ruby, RubyObject recv) {
        RubyArray localVariables = RubyArray.newArray(ruby);

        if (ruby.getScope().getLocalNames() != null) {
            for (int i = 2; i < ruby.getScope().getLocalNames().size(); i++) {
                if (ruby.getScope().getLocalNames().get(i) != null) {
                    localVariables.push(RubyString.newString(ruby, (String) ruby.getScope().getLocalNames().get(i)));
                }
            }
        }

        RubyVarmap dynamicVars = ruby.getDynamicVars();
        while (dynamicVars != null) {
            if (dynamicVars.getName() != null) {
                localVariables.push(RubyString.newString(ruby, dynamicVars.getName()));
            }
            dynamicVars = dynamicVars.getNext();
        }

        return localVariables;
    }

    public static RubyBoolean block_given(Ruby ruby, RubyObject recv) {
        return RubyBoolean.newBoolean(ruby, ruby.isFBlockGiven());
    }

    public static RubyObject sprintf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            throw new ArgumentError(ruby, "sprintf must have at least one argument");
        }

        RubyString str = RubyString.stringValue(args[0]);

        RubyArray newArgs = RubyArray.create(ruby, null, args);
        newArgs.shift();

        return str.format(newArgs);
    }

    public static RubyObject raise(Ruby ruby, RubyObject recv, RubyObject args[]) {
        switch (args.length) {
        case 0 :
        case 1 :
            throw new RaiseException(RubyException.newInstance(ruby, ruby.getExceptions().getRuntimeError(), args));
        case 2 :
            RubyException excptn = (RubyException) args[0].funcall("exception", args[1]);
            throw new RaiseException(excptn);
        default :
            throw new ArgumentError(ruby, "wrong # of arguments");
        }
    }

    /**
     * Require.
     * MRI allows to require ever .rb files or ruby extension dll (.so or .dll depending on system).
     * we allow requiring either .rb files or jars.
     * @param ruby the ruby interpreter to use.
     * @param recv ruby object used to call require (any object will do and it won't be used anyway).
     * @param i2Load the name of the file to require
     **/
    public static RubyObject require(Ruby ruby, RubyObject recv, RubyString i2Load) {
        //this is inefficient but it will do for now
        RubyArray lFeatures = (RubyArray) ruby.getGlobalVar("$\"");
        if (lFeatures.index(i2Load).isNil()) {
            load(ruby, recv, i2Load);
            lFeatures.push(i2Load);
            return ruby.getTrue();
        }
        return ruby.getFalse();
    }

    public static RubyObject load(Ruby ruby, RubyObject recv, RubyString i2Load) {
        if (i2Load.getValue().endsWith(".jar")) {
            File jarFile = ruby.findFile(ruby, new File(i2Load.getValue()));
            if (!jarFile.exists()) {
                ruby.getRuntime().getErrorStream().println("[Error] Jarfile + \"" + jarFile.getAbsolutePath() + "\"not found.");
            } else {
                /*try {
                  ClassLoader javaClassLoader = new URLClassLoader(new URL[] { jarFile.toURL()}, ruby.getJavaClassLoader());
                  ruby.setJavaClassLoader(javaClassLoader);
                  } catch (MalformedURLException murlExcptn) {
                  }*/
            }
        } else {
            if (!i2Load.getValue().endsWith(".rb")) {
                i2Load = RubyString.newString(ruby, i2Load.getValue() + ".rb");
            }
            File rbFile = ruby.findFile(ruby, new File(i2Load.getValue()));
            ruby.getRuntime().loadFile(rbFile, false);
        }
        return ruby.getTrue();
    }

    public static RubyObject eval(Ruby ruby, RubyObject recv, RubyString src, RubyObject[] args) {
        RubyObject scope = args.length > 0 ? args[0] : ruby.getNil();
        String file = "(eval)";
        int line = 1;

        if (args.length > 1) {
            // +++
            file = args[1].toString();
            // ---
        }

        if (args.length > 2) {
            line = RubyFixnum.fix2int(args[2]);
        }

        // +++
        src.checkSafeString();
        // ---

        if (scope.isNil() && ruby.getFrameStack().getPrevious() != null) {
            try {
                // XXX
                ruby.getFrameStack().push(ruby.getFrameStack().getPrevious());

                return recv.eval(src, scope, file, line);
            } finally {
                ruby.getFrameStack().pop();
            }
        }

        return recv.eval(src, scope, file, line);
    }

    public static RubyObject caller(Ruby ruby, RubyObject recv, RubyObject[] args) {
        int level = args.length > 0 ? RubyFixnum.fix2int(args[0]) : 1;

        if (level < 0) {
            throw new ArgumentError(ruby, "negative level(" + level + ')');
        }

        return RaiseException.createBacktrace(ruby, level);
    }

    public static RubyObject rbCatch(Ruby ruby, RubyObject recv, RubyObject tag) {
        try {
            return ruby.yield(tag);
        } catch (ThrowJump throwJump) {
            if (throwJump.getTag().equals(tag.toId())) {
                return throwJump.getValue();
            } else {
                throw throwJump;
            }
        }
    }

    public static RubyObject rbThrow(Ruby ruby, RubyObject recv, RubyObject tag, RubyObject[] args) {
        throw new ThrowJump(tag.toId(), args.length > 0 ? args[0] : ruby.getNil());
    }

    public static RubyObject set_trace_func(Ruby ruby, RubyObject recv, RubyObject trace_func) {
        if (trace_func.isNil()) {
            ruby.getRuntime().setTraceFunction(null);
        } else if (!(trace_func instanceof RubyProc)) {
            throw new TypeError(ruby, "trace_func needs to be Proc.");
        }
        ruby.getRuntime().setTraceFunction((RubyProc) trace_func);
        return trace_func;
    }

    public static RubyProc proc(Ruby ruby, RubyObject recv) {
        return RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
    }

    public static RubyObject loop(Ruby ruby, RubyObject recv) {
        while (true) {
            ruby.yield(ruby.getNil());

            Thread.yield();
        }
    }

    public static RubyObject backquote(Ruby ruby, RubyObject recv, RubyString aString) {
        // XXX use other methods
        try {
            String lShellProp = System.getProperty("jruby.shell");
            Process aProcess;
            String lCommand = aString.toString();
            String lSwitch = "-c";
            if (lShellProp != null)
                {
                    if (!lShellProp.endsWith("sh"))	//case windowslike
                        lSwitch = "/c";
                    //				System.out.println("command: " + lShellProp + " " + lSwitch +  " " + lCommand);
                    aProcess = Runtime.getRuntime().exec(new String[] { lShellProp, lSwitch, lCommand});
                }
            else
                {
                    //				System.out.println("command: " + lCommand);
                    aProcess = Runtime.getRuntime().exec(lCommand);
                }

            final StringBuffer sb = new StringBuffer();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(aProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            aProcess.waitFor();

            return RubyString.newString(ruby, sb.toString());
        } catch (Exception excptn) {
            excptn.printStackTrace();

            return RubyString.newString(ruby, "");
        }
    }

    public static RubyInteger srand(Ruby ruby, RubyObject recv, RubyObject arg) {
        long oldRandomSeed = ruby.randomSeed;
        RubyInteger integerSeed = (RubyInteger) arg.convertToType("Integer", "to_int", true);
        ruby.randomSeed = integerSeed.getLongValue();
        ruby.random.setSeed(ruby.randomSeed);
        return RubyFixnum.newFixnum(ruby, oldRandomSeed);
    }

    public static RubyNumeric rand(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            double result = ruby.random.nextDouble();
            return RubyFloat.newFloat(ruby, result);
        } else if (args.length == 1) {
            RubyInteger integerCeil = (RubyInteger) args[0].convertToType("Integer", "to_int", true);
            long ceil = integerCeil.getLongValue();
            if (ceil > Integer.MAX_VALUE) {
                throw new NotImplementedError("Random values larger than Integer.MAX_VALUE not supported");
            }
            return RubyFixnum.newFixnum(ruby, ruby.random.nextInt((int) ceil));
        } else {
            throw new ArgumentError(ruby, "wrong # of arguments(" + args.length + " for 1)");
        }
    }
}
