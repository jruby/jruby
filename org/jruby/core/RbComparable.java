/*
 * RbComparable.java - No description
 * Created on 11. September 2001, 22:51
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
 * @version 
 */
public class RbComparable {
    private static RubyCallbackMethod methodEqual = null;
    private static RubyCallbackMethod methodGt = null;
    private static RubyCallbackMethod methodGe = null;
    private static RubyCallbackMethod methodLt = null;
    private static RubyCallbackMethod methodLe = null;
    private static RubyCallbackMethod methodBetweenP = null;
    
    public static RubyModule createComparable(Ruby ruby) {
        RubyModule comparableModule = ruby.defineModule("Comparable");
        
        comparableModule.defineMethod("==", getMethodEqual());
        comparableModule.defineMethod(">", getMethodGt());
        comparableModule.defineMethod(">=", getMethodGe());
        comparableModule.defineMethod("<", getMethodLt());
        comparableModule.defineMethod("<=", getMethodLe());
        comparableModule.defineMethod("between?", getMethodBetweenP());
        
        return comparableModule;
    }

    public static RubyCallbackMethod getMethodEqual() {
        if (methodEqual == null) {
            methodEqual = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aObject) required");
                    }
                    
                    if (recv == args[0]) {
                        return ruby.getTrue();
                    } else {
                        try {
                            RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                            return RubyBoolean.m_newBoolean(ruby, fn.getValue() == 0);
                        } catch (RubyNameException rnExcptn) {
                            return ruby.getFalse();
                        }
                    }
                }
            };
        }
        return methodEqual;
    }

    public static RubyCallbackMethod getMethodGt() {
        if (methodGt == null) {
            methodGt = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.m_newBoolean(ruby, fn.getValue() > 0);
                }
            };
        }
        return methodGt;
    }

    public static RubyCallbackMethod getMethodGe() {
        if (methodGe == null) {
            methodGe = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.m_newBoolean(ruby, fn.getValue() >= 0);
                }
            };
        }
        return methodGe;
    }

    public static RubyCallbackMethod getMethodLt() {
        if (methodLt == null) {
            methodLt = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.m_newBoolean(ruby, fn.getValue() < 0);
                }
            };
        }
        return methodLt;
    }

    public static RubyCallbackMethod getMethodLe() {
        if (methodLe == null) {
            methodLe = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException("Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.m_newBoolean(ruby, fn.getValue() <= 0);
                }
            };
        }
        return methodLe;
    }

    public static RubyCallbackMethod getMethodBetweenP() {
        if (methodBetweenP == null) {
            methodBetweenP = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 2) {
                        throw new RubyArgumentException("Parameter: (aObject, aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    if (fn.getValue() < 0) {
                        return ruby.getFalse();
                    }
                    
                    fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[1]);
                    if (fn.getValue() > 0) {
                        return ruby.getFalse();
                    }
                    
                    return ruby.getTrue();
                }
            };
        }
        return methodBetweenP;
    }
}