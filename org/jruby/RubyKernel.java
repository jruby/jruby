/*
 * RubyKernel.java - No description
 * Created on 10. September 2001, 17:56
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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
import java.net.*;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyKernel {
	public static void createKernelModule(Ruby ruby) {
		ruby.defineGlobalFunction("format", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "sprintf"));
		ruby.defineGlobalFunction("print", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "print"));
		ruby.defineGlobalFunction("printf", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "printf"));
		ruby.defineGlobalFunction("puts", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "puts"));
		ruby.defineGlobalFunction("load", CallbackFactory.getSingletonMethod(RubyKernel.class, "load", RubyString.class));
		//FIXME autoload method needs to be implemented
		//ruby.defineGlobalFunction("autoload", CallbackFactory.getSingletonMethod(RubyKernel.class, "autoload", RubyString.class));
		ruby.defineGlobalFunction("raise", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "raise"));
		ruby.defineGlobalFunction("require", CallbackFactory.getSingletonMethod(RubyKernel.class, "require", RubyString.class));
		ruby.defineGlobalFunction("singleton_method_added", CallbackFactory.getNilMethod());
		ruby.defineGlobalFunction("sprintf", CallbackFactory.getOptSingletonMethod(RubyKernel.class, "sprintf"));
	}

    public static RubyObject raise(Ruby ruby, RubyObject recv, RubyObject args[]) {
        int argsLength = args != null ? args.length : 0;

        switch (argsLength) {
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

    public static RubyObject print(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyObject ofsObj = ruby.getGlobalVar("$,");
        RubyObject orsObj = ruby.getGlobalVar("$\\");
        String ofs = ofsObj.isNil() ? "" : RubyString.stringValue(ofsObj).getValue();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (i > 0) {
                    ruby.getRuntime().getOutputStream().print(ofs);
                }
                ruby.getRuntime().getOutputStream().print(
                    args[i].isNil() ? "nil" : ((RubyString) args[i].funcall("to_s")).getValue());
            }
        }
        ruby.getRuntime().getOutputStream().print(orsObj.isNil() ? "" : RubyString.stringValue(orsObj).getValue());
        return ruby.getNil();
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

    public static RubyObject printf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        ruby.getRuntime().getOutputStream().print(((RubyString) sprintf(ruby, recv, args)).getValue());
        return ruby.getNil();
    }

	/**
	 * Require.
	 * MRI allows to require ever .rb files or ruby extension dll (.so or .dll depending on system).
	 * we allow requiring either .rb files or jars.
	 * @param ruby the ruby interpreter to use.
	 * @param recv ruby object used to call require (any object will do and it won't be used anyway).
	 * @param i2Load the name of the file to require
	 **/
	public static RubyObject require(Ruby ruby, RubyObject recv, RubyString i2Load) 
	{
		//this is inefficient but it will do for now
		RubyArray lFeatures =  (RubyArray) ruby.getGlobalVar("$\"");
		if (lFeatures.index(i2Load).isNil())
		{
			load(ruby, recv, i2Load);
			lFeatures.push( i2Load);
			return ruby.getTrue();
		}
		return ruby.getFalse();
	}

	public static RubyObject load(Ruby ruby, RubyObject recv, RubyString i2Load) 
	{	
		if (i2Load.getValue().endsWith(".jar")) {
			File jarFile = ruby.findFile(new File(i2Load.getValue()));
			if (!jarFile.exists()) {
				ruby.getRuntime().getErrorStream().println("[Error] Jarfile + \"" + jarFile.getAbsolutePath() + "\"not found.");
			}
			else {
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

	public static RubyObject gets(Ruby ruby)
	{
		return ruby.getNil();
	}
}

