/*
 * RbComparable.java - No description
 * Created on 11. September 2001, 22:51
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

import org.jruby.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RbComparable {
    private static Callback methodEqual = null;
    private static Callback methodGt = null;
    private static Callback methodGe = null;
    private static Callback methodLt = null;
    private static Callback methodLe = null;
    private static Callback methodBetweenP = null;
    
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

    public static Callback getMethodEqual() {
        if (methodEqual == null) {
            methodEqual = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException(ruby, "Parameter: (aObject) required");
                    }
                    
                    if (recv == args[0]) {
                        return ruby.getTrue();
                    } else {
                        try {
                            RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                            return RubyBoolean.newBoolean(ruby, fn.getValue() == 0);
                        } catch (RubyNameException rnExcptn) {
                            return ruby.getFalse();
                        }
                    }
                }
            };
        }
        return methodEqual;
    }

    public static Callback getMethodGt() {
        if (methodGt == null) {
            methodGt = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException(ruby, "Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.newBoolean(ruby, fn.getValue() > 0);
                }
            };
        }
        return methodGt;
    }

    public static Callback getMethodGe() {
        if (methodGe == null) {
            methodGe = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException(ruby, "Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.newBoolean(ruby, fn.getValue() >= 0);
                }
            };
        }
        return methodGe;
    }

    public static Callback getMethodLt() {
        if (methodLt == null) {
            methodLt = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException(ruby, "Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.newBoolean(ruby, fn.getValue() < 0);
                }
            };
        }
        return methodLt;
    }

    public static Callback getMethodLe() {
        if (methodLe == null) {
            methodLe = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 1) {
                        throw new RubyArgumentException(ruby, "Parameter: (aObject) required");
                    }
                    
                    RubyFixnum fn = (RubyFixnum)recv.funcall(ruby.intern("<=>"), args[0]);
                    return RubyBoolean.newBoolean(ruby, fn.getValue() <= 0);
                }
            };
        }
        return methodLe;
    }

    public static Callback getMethodBetweenP() {
        if (methodBetweenP == null) {
            methodBetweenP = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length != 2) {
                        throw new RubyArgumentException(ruby, "Parameter: (aObject, aObject) required");
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