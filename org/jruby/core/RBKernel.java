/*
 * RBKernel.java - No description
 * Created on 10. September 2001, 17:56
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
public class RBKernel {
    public static RubyModule createKernelModule(Ruby ruby) {
        RubyModule kernelModule = ruby.defineModule("Kernel");
        
        kernelModule.defineMethod("puts", getKernelMethod("m_puts"));
        kernelModule.defineMethod("print", getKernelMethod("m_print"));
        kernelModule.defineMethod("sprintf", getKernelMethod("m_sprintf"));
        kernelModule.defineMethod("format", getKernelMethod("m_sprintf"));
        kernelModule.defineMethod("printf", getKernelMethod("m_printf"));
        kernelModule.defineMethod("require", getKernelMethod("m_require", RubyString.class));
        kernelModule.defineMethod("to_s", getObjectMethod("m_to_s"));
        kernelModule.defineMethod("nil?", DefaultCallbackMethods.getMethodFalse());
        kernelModule.defineMethod("=~", DefaultCallbackMethods.getMethodFalse());
        
        kernelModule.defineModuleFunction("singleton_method_added", getDummyMethod());
        
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
        return new ReflectionCallbackMethod(RBKernel.class, methodName, RubyObject[].class, true, true);
    }
    
    public static RubyCallbackMethod getKernelMethod(String methodName, Class arg1) {
        return new ReflectionCallbackMethod(RBKernel.class, methodName, arg1, false, true);
    }
    
    public static RubyCallbackMethod getObjectMethod(String methodName) {
        return new ReflectionCallbackMethod(RubyObject.class, methodName);
    }
    
    public static RubyObject m_puts(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            System.out.println();
        } else {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    System.out.println(((RubyString)args[i].funcall(ruby.intern("to_s"))).getValue());
                }
            }
        }
        return ruby.getNil();
    }
    
    public static RubyObject m_print(Ruby ruby, RubyObject recv, RubyObject args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                System.out.print(((RubyString)args[i].funcall(ruby.intern("to_s"))).getValue());
            }
        }
        return ruby.getNil();
    }
    
    public static RubyObject m_sprintf(Ruby ruby, RubyObject recv, RubyObject args[]) {
        if (args.length == 0) {
            throw new RubyArgumentException("sprintf must have at least one argument");
        }
        RubyString str = null;
        if (!(args[0] instanceof RubyString)) {
            try {
                str = (RubyString)args[0].convertType(RubyString.class, "String", "to_str");
            } catch (Exception ex) {
                throw new RubyArgumentException("first argument to sprintf must be a string");
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
        if (arg1.getValue().endsWith(".rb")) {
            // Not supported yet
            ruby.getInterpreter().load((RubyString)arg1, false);
            // System.err.println("[BUG] Not supported yet.");
        } else if (arg1.getValue().endsWith(".jar")) {
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
        }
        return ruby.getNil();
    }
}
