/*
 * RbNilClass.java - No description
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
public class RbNilClass {
    private static RubyCallbackMethod methodInspect = null;
    private static RubyCallbackMethod methodToA = null;
    private static RubyCallbackMethod methodToI = null;
    private static RubyCallbackMethod methodToS = null;
    private static RubyCallbackMethod methodType = null;
    private static RubyCallbackMethod methodAnd = null;
    private static RubyCallbackMethod methodOr = null;
    private static RubyCallbackMethod methodXor = null;
    
    public static RubyClass createNilClass(Ruby ruby) {
        RubyClass nilClass = ruby.defineClass("NilClass", ruby.getObjectClass());
        
        nilClass.defineMethod("type", getMethodType());
        nilClass.defineMethod("to_i", getMethodToI());
        nilClass.defineMethod("to_s", getMethodToS());
        nilClass.defineMethod("to_a", getMethodToA());
        nilClass.defineMethod("inspect", getMethodInspect());
        
        nilClass.defineMethod("&", getMethodAnd());
        nilClass.defineMethod("|", getMethodOr());
        nilClass.defineMethod("^", getMethodXor());
        nilClass.defineMethod("nil?", DefaultCallbackMethods.getMethodTrue());
        
        nilClass.getRubyClass().undefMethod("new");
        
        ruby.defineGlobalConstant("NIL", ruby.getNil());
        
        return nilClass;
    }
    
    public static RubyCallbackMethod getMethodInspect() {
        if (methodInspect == null) {
            methodInspect = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyNil)recv).m_inspect();
                }
            };
        }
        
        return methodInspect;
    }
    
    public static RubyCallbackMethod getMethodToA() {
        if (methodToA == null) {
            methodToA = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyNil)recv).m_to_a();
                }
            };
        }
        
        return methodToA;
    }
    
    
    public static RubyCallbackMethod getMethodToI() {
        if (methodToI == null) {
            methodToI = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyNil)recv).m_to_i();
                }
            };
        }
        
        return methodToI;
    }

    public static RubyCallbackMethod getMethodToS() {
        if (methodToS == null) {
            methodToS = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyNil)recv).m_to_s();
                }
            };
        }
        
        return methodToS;
    }

    public static RubyCallbackMethod getMethodType() {
        if (methodType == null) {
            methodType = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyNil)recv).m_type();
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
                    
                    return ruby.getFalse().op_and((RubyBoolean)args[0]);
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
                    
                    return ruby.getFalse().op_or((RubyBoolean)args[0]);
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
                    
                    return ruby.getFalse().op_xor((RubyBoolean)args[0]);
                }
            };
        }
        
        return methodXor;
    }
}