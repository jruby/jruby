/*
 * RBKernel.java - No description
 * Created on 10. September 2001, 17:56
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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

package org.jruby.core;

import java.io.*;
import java.net.*;

import org.jruby.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RbKernel {
    public static RubyModule createKernelModule(Ruby ruby) {
        RubyModule kernelModule = ruby.defineModule("Kernel");
        
        kernelModule.defineMethod("puts", getKernelMethod("m_puts"));
        kernelModule.defineMethod("print", getKernelMethod("m_print"));
        kernelModule.defineMethod("p", getKernelMethod("m_p"));
        kernelModule.defineMethod("sprintf", getKernelMethod("m_sprintf"));
        kernelModule.defineMethod("format", getKernelMethod("m_sprintf"));
        kernelModule.defineMethod("printf", getKernelMethod("m_printf"));
        kernelModule.defineMethod("require", getKernelMethod("m_require", RubyString.class));
        kernelModule.defineMethod("to_s", getObjectMethod("m_to_s"));
        kernelModule.defineMethod("inspect", getObjectMethod("m_inspect"));
        kernelModule.defineMethod("nil?", DefaultCallbackMethods.getMethodFalse());
        kernelModule.defineMethod("=~", DefaultCallbackMethods.getMethodFalse());
        kernelModule.defineMethod("inspect", getObjectMethod("m_inspect"));        
        kernelModule.defineModuleFunction("singleton_method_added", getDummyMethod());
        kernelModule.defineMethod("raise", getKernelMethod("m_raise"));
        
        kernelModule.defineMethod("method", getObjectMethod("m_method", RubyObject.class));
        
        return kernelModule;
    }
    
    public static RubyCallbackMethod getDummyMethod() {
        return new RubyCallbackMethod() {
            public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                return ruby.getNil();
            }
        };
    }
    
    public static RubyCallbackMethod getKernelMethod(String methodName) {
        return new ReflectionCallbackMethod(RbKernel.class, methodName, RubyObject[].class, true, true);
    }
    
    public static RubyCallbackMethod getKernelMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RbKernel.class, methodName, arg1, false, true);
    }
    
    public static RubyCallbackMethod getObjectMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyObject.class, methodName);
    }
    
    public static RubyCallbackMethod getObjectMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RubyObject.class, methodName, arg1);
    }
    
    public static RubyObject m_puts(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            System.out.println();
            return ruby.getNil();
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (args[i] instanceof RubyArray) {
                    m_puts(ruby, recv, ((RubyArray)args[i]).toJavaArray());
                } else {
                    System.out.println(args[i].isNil() ? "nil" :
                        ((RubyString)args[i].funcall(ruby.intern("to_s"))).getValue());
                }
            }
        }
        return ruby.getNil();
    }
    
    public static RubyObject m_raise(Ruby ruby, RubyObject recv, RubyObject args[]) {
        int argsLength = args != null? args.length : 0;
    
        switch (argsLength) {
            case 0:
            case 1:
                throw new RaiseException(RubyException.s_new(ruby, ruby.getExceptions().getRuntimeError(), args));
            case 2:
                RubyException excptn = (RubyException)args[0].funcall(ruby.intern("exception"), args[1]);
                throw new RaiseException(excptn);
            default:
                throw new RubyArgumentException(ruby, "wrong # of arguments");
        }
    }
    
    public static RubyObject m_print(Ruby ruby, RubyObject recv, RubyObject args[]) {
        RubyObject ofsObj = ruby.getGlobalVar("$,");
        RubyObject orsObj = ruby.getGlobalVar("$\\");
        String ofs = ofsObj.isNil() ? "" : RubyString.stringValue(ofsObj).getValue();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                if (i > 0) {
                    System.out.print(ofs);
                }
                System.out.print(args[i].isNil() ? "nil" :
                        ((RubyString)args[i].funcall(ruby.intern("to_s"))).getValue());
            }
        }
        System.out.print(orsObj.isNil() ? "" : RubyString.stringValue(orsObj).getValue());
        return ruby.getNil();
    }
    
    public static RubyObject m_p(Ruby ruby, RubyObject recv, RubyObject args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                System.out.println(((RubyString)args[i].funcall(ruby.intern("inspect"))).getValue());
            }
        }
        return ruby.getNil();
    }
    
    public static RubyObject m_sprintf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            throw new RubyArgumentException(ruby, "sprintf must have at least one argument");
        }
        RubyString str = null;
        if (!(args[0] instanceof RubyString)) {
            try {
                str = (RubyString)args[0].convertType(RubyString.class, "String", "to_str");
            } catch (Exception ex) {
                throw new RubyArgumentException(ruby, "first argument to sprintf must be a string");
            }
        } else {
            str = (RubyString)args[0];
        }
        RubyArray newArgs = RubyArray.m_create(ruby, args);
        newArgs.m_shift();
        return str.m_format(newArgs);
    }
    
    public static RubyObject m_printf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        System.out.print(((RubyString)m_sprintf(ruby, recv, args)).getValue());
        return ruby.getNil();
    }
    
    public static RubyObject m_require(Ruby ruby, RubyObject recv, RubyString arg1) {
        if (arg1.getValue().endsWith(".jar")) {
            File jarFile = new File(arg1.getValue());
            if (!jarFile.exists()) {
                jarFile = new File(new File(ruby.getSourceFile()).getParentFile(), arg1.getValue());
                if (!jarFile.exists()) {
                    System.err.println("[Error] Jarfile + \"" + jarFile.getAbsolutePath() + "\"not found.");
                }
            }
            if (jarFile.exists()) {
                try {
                    ClassLoader javaClassLoader = new URLClassLoader(new URL[] { jarFile.toURL() }, ruby.getJavaClassLoader());
                    ruby.setJavaClassLoader(javaClassLoader);
                } catch (MalformedURLException murlExcptn) {
                }
            }
        } else {
            if (!arg1.getValue().endsWith(".rb")) {
                arg1 = RubyString.m_newString(ruby, arg1.getValue() + ".rb");
            }
            ruby.getRuntime().loadFile(arg1, false);
        }
        return ruby.getNil();
    }
}
