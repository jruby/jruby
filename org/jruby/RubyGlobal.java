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

        ruby.defineHookedVariable("$.", RubyFixnum.one(ruby), null, new LineNumberSetter());
        ruby.defineVirtualVariable("$_", lastlineAccessor, lastlineAccessor);

        ruby.defineHookedVariable("$!", ruby.getNil(), null, new ErrorInfoSetter());

        ruby.defineVirtualVariable("$SAFE", safeAccessor, safeAccessor);

		RubyObject stdin = RubyIO.stdin(ruby, ruby.getClasses().getIoClass());
		RubyObject stdout = RubyIO.stdout(ruby, ruby.getClasses().getIoClass());

        ruby.defineHookedVariable("$stdin", stdin, null, new StdInSetter());
        ruby.defineHookedVariable("$stdout", stdout, null, new StdOutSetter());

        ruby.defineGlobalConstant("STDIN", stdin);
        ruby.defineGlobalConstant("STDOUT", stdout);

        // ARGF, $< object
        RubyArgsFile argsFile = new RubyArgsFile(ruby);
        argsFile.initArgsFile();

        // Global functions

        ruby.defineGlobalFunction("open", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "open"));
        ruby.defineGlobalFunction("gets", CallbackFactory.getOptSingletonMethod(RubyArgsFile.class, "gets"));
    }

    public static RubyObject open(Ruby ruby, RubyObject recv, RubyObject[] args) {
        if (args.length > 0 && args[0].toString().startsWith("|")) {
            // +++
            return ruby.getNil();
            // ---
        }
        return RubyFile.open(ruby, ruby.getClasses().getFileClass(), args);
    }

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
            }
        }
    }
}