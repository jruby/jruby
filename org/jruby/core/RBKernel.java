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

import org.jruby.*;

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
}
