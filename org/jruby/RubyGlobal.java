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
 * @fixme autoload method needs to be implemented
 */
public class RubyGlobal {
    public static void createGlobals(Ruby ruby) {
        StringSetter stringSetter = new StringSetter();
        LastlineAccessor lastlineAccessor = new LastlineAccessor();
        SafeAccessor safeAccessor = new SafeAccessor();

        // Version information:
        RubyObject version = RubyString.newString(ruby, Ruby.RUBY_VERSION).freeze();
        RubyObject release = RubyString.newString(ruby, "$Date$").freeze(); // XXX
        RubyObject platform = RubyString.newString(ruby, "java").freeze();

        ruby.defineGlobalConstant("RUBY_VERSION", version);
        ruby.defineGlobalConstant("RUBY_RELEASE_DATE", release);
        ruby.defineGlobalConstant("RUBY_PLATFORM", platform);

        ruby.defineGlobalConstant("VERSION", version);
        ruby.defineGlobalConstant("RELEASE_DATE", release);
        ruby.defineGlobalConstant("PLATFORM", platform);

        ruby.defineHookedVariable("$/", RubyString.newString(ruby, "\n"), null, stringSetter);
        ruby.defineHookedVariable("$\\", ruby.getNil(), null, stringSetter);
        ruby.defineHookedVariable("$,", ruby.getNil(), null, stringSetter);

        ruby.defineHookedVariable("$.", RubyFixnum.one(ruby), null, new LineNumberSetter());
        ruby.defineVirtualVariable("$_", lastlineAccessor, lastlineAccessor);

        ruby.defineHookedVariable("$!", ruby.getNil(), null, new ErrorInfoSetter());

        ruby.defineVirtualVariable("$SAFE", safeAccessor, safeAccessor);

        BacktraceAccessor btAccessor = new BacktraceAccessor();
        ruby.defineVirtualVariable("$@", btAccessor, btAccessor);

        RubyObject stdin = RubyIO.stdin(ruby, ruby.getClasses().getIoClass(), System.in);
        RubyObject stdout = RubyIO.stdout(ruby, ruby.getClasses().getIoClass(), System.out);
        RubyObject stderr = RubyIO.stderr(ruby, ruby.getClasses().getIoClass(), System.err);

        ruby.defineHookedVariable("$stdin", stdin, null, new StdInSetter());
        ruby.defineHookedVariable("$stdout", stdout, null, new StdOutSetter());
        ruby.defineHookedVariable("$stderr", stderr, null, new StdErrSetter());

        ruby.defineHookedVariable("$>", stdout, null, new DefSetter());
        ruby.defineHookedVariable("$defout", stdout, null, new DefSetter());

        ruby.defineGlobalConstant("STDIN", stdin);
        ruby.defineGlobalConstant("STDOUT", stdout);
        ruby.defineGlobalConstant("STDERR", stderr);

        ruby.defineReadonlyVariable("$\"", RubyArray.newArray(ruby));
        ruby.defineReadonlyVariable("$*", RubyArray.newArray(ruby));

        RubyArray loadPath = RubyArray.newArray(ruby);
        ruby.defineReadonlyVariable("$:", loadPath);
        ruby.defineReadonlyVariable("$-I", loadPath);
        ruby.defineReadonlyVariable("$LOAD_PATH", loadPath);

        // ARGF, $< object
        RubyArgsFile argsFile = new RubyArgsFile(ruby);
        argsFile.initArgsFile();
    }

    // Accessor methods.

    private static class LineNumberSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            ((RubyArgsFile) ruby.getGlobalVar("$<")).setCurrentLineNumber(RubyFixnum.fix2int(value));
            entry.setInternalData(value);
        }
    }

    private static class ErrorInfoSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            if (!value.isNil() && value.kind_of(entry.getRuby().getClasses().getExceptionClass()).isFalse()) {
                throw new TypeError(ruby, "assigning non-exception to $!");
            }
            entry.setInternalData(value);
        }
    }

    public static class StringSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            if (!(value instanceof RubyString)) {
                throw new TypeError(ruby, "value of " + entry.getName() + " must be a String");
            }
            entry.setInternalData(value);
        }
    }

    private static class SafeAccessor implements RubyGlobalEntry.GetterMethod, RubyGlobalEntry.SetterMethod {
        /*
         * @see GetterMethod#get(String, RubyObject, RubyGlobalEntry)
         */
        public RubyObject get(Ruby ruby, RubyGlobalEntry entry) {
            return RubyFixnum.newFixnum(ruby, ruby.getSafeLevel());
        }

        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            int level = RubyFixnum.fix2int(value);

            if (level < ruby.getSafeLevel()) {
                throw new SecurityException("tried to downgrade level from " + ruby.getSafeLevel() + " to " + level);
            } else {
                ruby.setSafeLevel(level);
                // thread.setSafeLevel(level);
            }
        }
    }

    private static class BacktraceAccessor implements RubyGlobalEntry.GetterMethod, RubyGlobalEntry.SetterMethod {
        /*
         * @see GetterMethod#get(String, RubyObject, RubyGlobalEntry)
         */
        public RubyObject get(Ruby ruby, RubyGlobalEntry entry) {
            RubyObject errorInfo = ruby.getGlobalVar("$!");

            return errorInfo.isNil() ? ruby.getNil() : errorInfo.funcall("backtrace");
        }

        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            if (ruby.getGlobalVar("$!").isNil()) {
                throw new ArgumentError(ruby, "$! not set.");
            }

            ruby.getGlobalVar("$!").funcall("set_backtrace", value);
        }
    }

    private static class LastlineAccessor implements RubyGlobalEntry.GetterMethod, RubyGlobalEntry.SetterMethod {
        /*
         * @see GetterMethod#get(String, RubyObject, RubyGlobalEntry)
         */
        public RubyObject get(Ruby ruby, RubyGlobalEntry entry) {
            return ruby.getLastline();
        }

        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            ruby.setLastline(value);
        }
    }

    private static class BackrefAccessor implements RubyGlobalEntry.GetterMethod, RubyGlobalEntry.SetterMethod {
        /*
         * @see GetterMethod#get(String, RubyObject, RubyGlobalEntry)
         */
        public RubyObject get(Ruby ruby, RubyGlobalEntry entry) {
            return ruby.getBackref();
        }

        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            ruby.setBackref(value);
        }
    }

    private static class StdInSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            if (value == entry.getInternalData()) {
                return;
            } else if (!(value instanceof RubyIO)) {
                entry.setInternalData(value);
                return;
            } else {
                ((RubyIO) value).checkReadable();
                // ((RubyIO)value).fileno = 0;

                entry.setInternalData(value);
            }
        }
    }

    private static class StdOutSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            if (value == entry.getInternalData()) {
                return;
            } else if (!(value instanceof RubyIO)) {
                entry.setInternalData(value);
                return;
            } else {
                ((RubyIO) value).checkWriteable();
                //((RubyIO)value).fileno = 0;

                entry.setInternalData(value);
            }
        }
    }

    private static class StdErrSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            if (value == entry.getInternalData()) {
                return;
            } else if (!(value instanceof RubyIO)) {
                entry.setInternalData(value);
                return;
            } else {
                ((RubyIO) value).checkWriteable();
                // ((RubyIO)value).f= 0;

                entry.setInternalData(value);
            }
        }
    }

    private static class DefSetter implements RubyGlobalEntry.SetterMethod {
        /*
         * @see SetterMethod#set(RubyObject, String, Object, RubyGlobalEntry)
         */
        public void set(Ruby ruby, RubyGlobalEntry entry, RubyObject value) {
            if (value.respond_to(RubySymbol.newSymbol(entry.getRuby(), "write")).isFalse()) {
                throw new TypeError(entry.getRuby(), "$> must have write method, " + value.type().toName() + " given");
            }

            entry.setInternalData(value);
        }
    }
}
