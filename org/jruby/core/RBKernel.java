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
    private static RubyCallbackMethod methodPuts = null;
    private static RubyCallbackMethod methodToS = null;
    
    public static RubyModule createKernelModule(Ruby ruby) {
        RubyModule kernelModule = ruby.defineModule("Kernel");
        
        kernelModule.defineMethod("puts", getMethodPuts());
        kernelModule.defineMethod("to_s", getMethodToS());
        
        return kernelModule;
    }
    
    public static RubyCallbackMethod getMethodPuts() {
        if (methodPuts == null) {
            methodPuts = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            System.out.println(((RubyString)args[i].funcall(ruby.intern("to_s"))).getString());
                        }
                    }
                    return ruby.getNil();
                }
            };
        }
        
        return methodPuts;
    }
    
    public static RubyCallbackMethod getMethodToS() {
        if (methodToS == null) {
            methodToS = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return recv.m_to_s();
                }
            };
        }
        
        return methodToS;
    }
}