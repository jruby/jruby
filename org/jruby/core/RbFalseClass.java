/*
 * RbFalseClass.java - No description
 * Created on 04. Juli 2001, 22:53
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

import org.jruby.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 */
public class RbFalseClass {
    private static RubyCallbackMethod methodType = null;
    private static RubyCallbackMethod methodToS = null;
    private static RubyCallbackMethod methodAnd = null;
    private static RubyCallbackMethod methodOr = null;
    private static RubyCallbackMethod methodXor = null;
    
    public static RubyClass createFalseClass(Ruby ruby) {
        RubyClass falseClass = ruby.defineClass("FalseClass", ruby.getObjectClass());
        
        falseClass.defineMethod("to_s", getMethodToS());
        falseClass.defineMethod("type", getMethodType());
        
        falseClass.defineMethod("&", getMethodAnd());
        falseClass.defineMethod("|", getMethodOr());
        falseClass.defineMethod("^", getMethodXor());
        
        falseClass.getRubyClass().undefMethod("new");
        
        ruby.defineGlobalConstant("FALSE", ruby.getFalse());
        
        return falseClass;
    }

    public static RubyCallbackMethod getMethodToS() {
        if (methodToS == null) {
            methodToS = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyBoolean)recv).m_to_s();
                }
            };
        }
        
        return methodToS;
    }

    public static RubyCallbackMethod getMethodType() {
        if (methodType == null) {
            methodType = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyBoolean)recv).m_type();
                }
            };
        }
        
        return methodType;
    }

    public static RubyCallbackMethod getMethodAnd() {
        if (methodAnd == null) {
            methodAnd = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aBoolean) required");
                    }
                    
                    return ((RubyBoolean)recv).op_and((RubyBoolean)args[0]);
                }
            };
        }
        
        return methodAnd;
    }

    public static RubyCallbackMethod getMethodOr() {
        if (methodOr == null) {
            methodOr = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aBoolean) required");
                    }
                    
                    return ((RubyBoolean)recv).op_or((RubyBoolean)args[0]);
                }
            };
        }
        
        return methodOr;
    }

    public static RubyCallbackMethod getMethodXor() {
        if (methodXor == null) {
            methodXor = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aBoolean) required");
                    }
                    
                    return ((RubyBoolean)recv).op_xor((RubyBoolean)args[0]);
                }
            };
        }
        
        return methodXor;
    }
}