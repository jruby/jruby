/*
 * RubyGlobal.java - No description
 * Created on 12.01.2002, 17:33:23
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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

import java.io.*;
import java.util.*;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;

/** This class initializes all global functions, variables and constants.
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class RubyGlobal {
    public static void createGlobals(Ruby ruby) {
        StringSetter stringSetter = new StringSetter();
        LastlineAccessor lastlineAccessor = new LastlineAccessor();
        SafeAccessor safeAccessor = new SafeAccessor();

        ruby.defineHookedVariable("$/", RubyString.newString(ruby, "\n"), null, stringSetter);
        ruby.defineHookedVariable("$\\", ruby.getNil(), null, stringSetter);
        ruby.defineHookedVariable("$,", ruby.getNil(), null, stringSetter);

        ruby.defineHookedVariable("$.", RubyFixnum.one(ruby), null, new LineNumberSetter());
        ruby.defineVirtualVariable("$_", lastlineAccessor, lastlineAccessor);

        ruby.defineHookedVariable("$!", ruby.getNil(), null, new ErrorInfoSetter());

        ruby.defineVirtualVariable("$SAFE", safeAccessor, safeAccessor);

        RubyObject stdin = RubyIO.stdin(ruby, ruby.getClasses().getIoClass());
        RubyObject stdout = RubyIO.stdout(ruby, ruby.getClasses().getIoClass());
        RubyObject stderr = RubyIO.stderr(ruby, ruby.getClasses().getIoClass());

        ruby.defineHookedVariable("$stdin", stdin, null, new StdInSetter());
        ruby.defineHookedVariable("$stdout", stdout, null, new StdOutSetter());
        ruby.defineHookedVariable("$stderr", stderr, null, new StdErrSetter());

        ruby.defineHookedVariable("$>", stdout, null, new DefSetter());
        ruby.defineHookedVariable("$defout", stdout, null, new DefSetter());

        ruby.defineGlobalConstant("STDIN", stdin);
        ruby.defineGlobalConstant("STDOUT", stdout);
        ruby.defineGlobalConstant("STDERR", stderr);

        // ARGF, $< object
        RubyArgsFile argsFile = new RubyArgsFile(ruby);
        argsFile.initArgsFile();

        // Global functions

        // IO, $_ String
        ruby.defineGlobalFunction("open", CallbackFactory.getSingletonMethod(RubyGlobal.class, "open", RubyString.class));

        ruby.defineGlobalFunction("format", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "sprintf"));
        ruby.defineGlobalFunction("gets", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "gets"));
        ruby.defineGlobalFunction("p", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "p"));
        ruby.defineGlobalFunction("print", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "print"));
        ruby.defineGlobalFunction("printf", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "printf"));
        ruby.defineGlobalFunction("puts", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "puts"));
        ruby.defineGlobalFunction("readline", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readline"));
        ruby.defineGlobalFunction("readlines", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readlines"));
        ruby.defineGlobalFunction("sprintf", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "sprintf"));

        ruby.defineGlobalFunction("gsub!", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "gsub_bang"));
        ruby.defineGlobalFunction("gsub", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "gsub"));
        ruby.defineGlobalFunction("sub!", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "sub_bang"));
        ruby.defineGlobalFunction("sub", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "sub"));

        ruby.defineGlobalFunction("chop!", CallbackFactory.getSingletonMethod(RubyGlobal.class, "chop_bang"));
        ruby.defineGlobalFunction("chop", CallbackFactory.getSingletonMethod(RubyGlobal.class, "chop"));
        ruby.defineGlobalFunction("chomp!", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "chomp_bang"));
        ruby.defineGlobalFunction("chomp", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "chomp"));

        ruby.defineGlobalFunction("split", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "split"));
        ruby.defineGlobalFunction("scan", CallbackFactory.getSingletonMethod(RubyGlobal.class, "scan", RubyObject.class));

        ruby.defineGlobalFunction("load", CallbackFactory.getSingletonMethod(RubyGlobal.class, "load", RubyString.class));
        //FIXME autoload method needs to be implemented
        //ruby.defineGlobalFunction("autoload", CallbackFactory.getSingletonMethod(RubyGlobal.class, "autoload", RubyString.class));
        ruby.defineGlobalFunction("raise", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "raise"));
        ruby.defineGlobalFunction("require", CallbackFactory.getSingletonMethod(RubyGlobal.class, "require", RubyString.class));

        ruby.defineGlobalFunction("global_variables", CallbackFactory.getSingletonMethod(RubyGlobal.class, "global_variables"));
        ruby.defineGlobalFunction("local_variables", CallbackFactory.getSingletonMethod(RubyGlobal.class, "local_variables"));
        ruby.defineGlobalFunction("block_given?", CallbackFactory.getSingletonMethod(RubyGlobal.class, "block_given"));

        ruby.defineGlobalFunction("lambda", CallbackFactory.getSingletonMethod(RubyGlobal.class, "lambda"));
        ruby.defineGlobalFunction("proc", CallbackFactory.getSingletonMethod(RubyGlobal.class, "proc"));

        ruby.defineGlobalFunction("loop", CallbackFactory.getSingletonMethod(RubyGlobal.class, "loop"));

        ruby.defineGlobalFunction("eval", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "eval", RubyString.class));

        ruby.defineGlobalFunction("singleton_method_added", CallbackFactory.getNilMethod());
    }

    // Global functions.

    public static RubyObject open(Ruby ruby, RubyObject recv, RubyString filename) {
        if (filename.toString().startsWith("|")) {
            // +++
            return ruby.getNil();
            // ---
        }
        return RubyFile.open(ruby, ruby.getClasses().getFileClass(), new RubyObject[] { filename });
    }

    public static RubyString gets(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyArgsFile argsFile = (RubyArgsFile) ruby.getGlobalVar("$<");

        RubyString line = argsFile.internalGets(args);

        ruby.getParserHelper().setLastline(line);

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
            if (dynamicVars.getId() != null) {
                localVariables.push(RubyString.newString(ruby, dynamicVars.getId()));
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
            throw new RubyArgumentException(ruby, "sprintf must have at least one argument");
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
                throw new RubyArgumentException(ruby, "wrong # of arguments");
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
            File jarFile = ruby.findFile(new File(i2Load.getValue()));
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
            File rbFile = ruby.findFile(new File(i2Load.getValue()));
            ruby.getRuntime().loadFile(rbFile, false);
        }
        return ruby.getTrue();
    }

    /** Returns value of $_.
     * 
     * @throws TypeError if $_ is not a String or nil.
     * @return value of $_ as String.
     */
    private static RubyString getLastlineString(Ruby ruby) {
        RubyObject line = ruby.getParserHelper().getLastline();

        if (!line.isNil()) {
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
            ruby.getParserHelper().setLastline(str);
        }

        return str;
    }

    public static RubyObject gsub_bang(Ruby ruby, RubyObject recv, RubyObject args[]) {
        return getLastlineString(ruby).gsub_bang(args);
    }

    public static RubyObject gsub(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyString str = (RubyString) getLastlineString(ruby).dup();

        if (!str.gsub_bang(args).isNil()) {
            ruby.getParserHelper().setLastline(str);
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
            ruby.getParserHelper().setLastline(str);
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
            ruby.getParserHelper().setLastline(dup);
            return str;
        }
    }

    public static RubyObject split(Ruby ruby, RubyObject recv, RubyObject[] args) {
        return getLastlineString(ruby).split(args);
    }

    public static RubyObject scan(Ruby ruby, RubyObject recv, RubyObject pattern) {
        return getLastlineString(ruby).scan(pattern);
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

        if (scope.isNil() && ruby.getRubyFrame().getPrev() != null) {
            try {
                // +++
                RubyFrame prev = new RubyFrame(ruby.getRubyFrame());
                ruby.getRubyFrame().push();

                ruby.setRubyFrame(prev.getPrev());
                ruby.getRubyFrame().setPrev(prev);
                // ---

                return recv.eval(src, scope, file, line);
            } finally {
                ruby.getRubyFrame().pop();
            }
        }

        return recv.eval(src, scope, file, line);
    }

    public static RubyObject lambda(Ruby ruby, RubyObject recv) {
        return RubyProc.newProc(ruby, ruby.getClasses().getProcClass());
    }

    public static RubyObject loop(Ruby ruby, RubyObject recv) {
        while (true) {
            ruby.yield(ruby.getNil());

            Thread.yield();
        }
    }

    // Accessor methods.

    private static class LineNumberSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            ((RubyArgsFile) entry.getRuby().getGlobalVar("$<")).setCurrentLineNumber(RubyFixnum.fix2int(value));
            entry.setData(value);
        }
    }

    private static class ErrorInfoSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            if (!value.isNil() && value.kind_of(entry.getRuby().getClasses().getExceptionClass()).isTrue()) {
                throw new TypeError(entry.getRuby(), "assigning non-exception to $!");
            }
            entry.setData(value);
        }
    }

    private static class StringSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            if (!(value instanceof RubyString)) {
                throw new TypeError(entry.getRuby(), "value of " + id + " must be a String");
            }
            entry.setData(value);
        }
    }

    private static class SafeAccessor implements RubyGlobalEntry.GetterMethod, RubyGlobalEntry.SetterMethod {
        /*
         * @see GetterMethod#get(String, RubyObject, RubyGlobalEntry)
         */
        public RubyObject get(String id, RubyObject value, RubyGlobalEntry entry) {
            return RubyFixnum.newFixnum(entry.getRuby(), entry.getRuby().getSafeLevel());
        }

        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            Ruby ruby = entry.getRuby();

            int level = RubyFixnum.fix2int(value);

            if (level < ruby.getSafeLevel()) {
                throw new SecurityException("tried to downgrade level from " + ruby.getSafeLevel() + " to " + level);
            } else {
                ruby.setSafeLevel(level);
                // thread.setSafeLevel(level);
            }
        }
    }

    private static class LastlineAccessor implements RubyGlobalEntry.GetterMethod, RubyGlobalEntry.SetterMethod {
        /*
         * @see GetterMethod#get(String, RubyObject, RubyGlobalEntry)
         */
        public RubyObject get(String id, RubyObject value, RubyGlobalEntry entry) {
            return entry.getRuby().getParserHelper().getLastline();
        }

        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            entry.getRuby().getParserHelper().setLastline(value);
        }
    }

    private static class StdInSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            if (value == data) {
                return;
            } else if (!(value instanceof RubyIO)) {
                entry.setData(value);
                return;
            } else {
                ((RubyIO) value).checkReadable();
                // ((RubyIO)value).fileno = 0;

                entry.setData(value);
                ((RubyIO) value).setAsRubyInputStream();
            }
        }
    }

    private static class StdOutSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            if (value == data) {
                return;
            } else if (!(value instanceof RubyIO)) {
                entry.setData(value);
                return;
            } else {
                ((RubyIO) value).checkWriteable();
                // ((RubyIO)value).fileno = 0;

                entry.setData(value);
                //set the ruby outputstream to match
                 ((RubyIO) value).setAsRubyOutputStream();
            }
        }
    }

    private static class StdErrSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            if (value == data) {
                return;
            } else if (!(value instanceof RubyIO)) {
                entry.setData(value);
                return;
            } else {
                ((RubyIO) value).checkWriteable();
                // ((RubyIO)value).f= 0;

                entry.setData(value);
                ((RubyIO) value).setAsRubyErrorStream();
            }
        }
    }

    private static class DefSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            if (value.respond_to(RubySymbol.newSymbol(entry.getRuby(), "write")).isFalse()) {
                throw new TypeError(entry.getRuby(), "$> must have write method, " + value.type().toName() + " given");
            }

            entry.setData(value);
        }
    }
}