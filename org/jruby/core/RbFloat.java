/*
 * RbFloat.java - No description
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
public class RbFloat {
    private static RubyCallbackMethod methodPlus = null;
    private static RubyCallbackMethod methodMinus = null;
    private static RubyCallbackMethod methodMul = null;
    private static RubyCallbackMethod methodDiv = null;
    private static RubyCallbackMethod methodMod = null;
    private static RubyCallbackMethod methodPow = null;
    
/*    private static RubyCallbackMethod methodToI = null;
 */
    private static RubyCallbackMethod methodToS = null;
    
/*    private static RubyCallbackMethod methodEqual = null;
    private static RubyCallbackMethod methodCmp = null;
    private static RubyCallbackMethod methodGt = null;
    private static RubyCallbackMethod methodGe = null;
    private static RubyCallbackMethod methodLt = null;
    private static RubyCallbackMethod methodLe = null;
*/    
    public static RubyClass createFloat(Ruby ruby) {
        RubyClass floatClass = ruby.defineClass("Float", (RubyClass)ruby.getRubyClass("Numeric"));
        
        // floatClass.defineMethod("to_i", getMethodToI());
        floatClass.defineMethod("to_s", getMethodToS());
        
        floatClass.defineMethod("+", getMethodPlus());
        floatClass.defineMethod("-", getMethodMinus());
        floatClass.defineMethod("*", getMethodMul());
        floatClass.defineMethod("/", getMethodDiv());
        floatClass.defineMethod("%", getMethodMod());
        floatClass.defineMethod("**", getMethodPow());
        
        // floatClass.defineMethod("==", getMethodEqual());
        // floatClass.defineMethod("<=>", getMethodCmp());
        // floatClass.defineMethod(">", getMethodGt());
        // floatClass.defineMethod(">=", getMethodGe());
        // floatClass.defineMethod("<", getMethodLt());
        // floatClass.defineMethod("<=", getMethodLe());
        
        return floatClass;
    }
    
    /*public static RubyCallbackMethod getMethodToI() {
        if (methodToI == null) {
            methodToI = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return recv;
                }
            };
        }
        
        return methodToI;
    }
*/
    public static RubyCallbackMethod getMethodToS() {
        if (methodToS == null) {
            methodToS = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    return ((RubyFloat)recv).m_to_s();
                }
            };
        }
        
        return methodToS;
    }

    public static RubyCallbackMethod getMethodPlus() {
        if (methodPlus == null) {
            methodPlus = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    return ((RubyFloat)recv).op_plus((RubyNumeric)args[0]);
                }
            };
        }
        return methodPlus;
    }

    public static RubyCallbackMethod getMethodMinus() {
        if (methodMinus == null) {
            methodMinus = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFloat)recv).op_minus((RubyNumeric)args[0]);
                }
            };
        }
        return methodMinus;
    }

    public static RubyCallbackMethod getMethodMul() {
        if (methodMul == null) {
            methodMul = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFloat)recv).op_mul((RubyNumeric)args[0]);
                }
            };
        }
        return methodMul;
    }

    public static RubyCallbackMethod getMethodDiv() {
        if (methodDiv == null) {
            methodDiv = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFloat)recv).op_div((RubyNumeric)args[0]);
                }
            };
        }
        return methodDiv;
    }

    public static RubyCallbackMethod getMethodMod() {
        if (methodMod == null) {
            methodMod = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFloat)recv).op_mod((RubyNumeric)args[0]);
                }
            };
        }
        return methodMod;
    }

    public static RubyCallbackMethod getMethodPow() {
        if (methodPow == null) {
            methodPow = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFloat)recv).op_pow((RubyNumeric)args[0]);
                }
            };
        }
        return methodPow;
    }

/*    public static RubyCallbackMethod getMethodEqual() {
        if (methodEqual == null) {
            methodEqual = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFixnum)recv).op_equal((RubyFixnum)args[0]);
                }
            };
        }
        return methodEqual;
    }

    public static RubyCallbackMethod getMethodCmp() {
        if (methodCmp == null) {
            methodCmp = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFixnum)recv).op_cmp((RubyFixnum)args[0]);
                }
            };
        }
        return methodCmp;
    }

    public static RubyCallbackMethod getMethodGt() {
        if (methodGt == null) {
            methodGt = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFixnum)recv).op_gt((RubyFixnum)args[0]);
                }
            };
        }
        return methodGt;
    }

    public static RubyCallbackMethod getMethodGe() {
        if (methodGe == null) {
            methodGe = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFixnum)recv).op_ge((RubyFixnum)args[0]);
                }
            };
        }
        return methodGe;
    }

    public static RubyCallbackMethod getMethodLt() {
        if (methodLt == null) {
            methodLt = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFixnum)recv).op_lt((RubyFixnum)args[0]);
                }
            };
        }
        return methodLt;
    }

    public static RubyCallbackMethod getMethodLe() {
        if (methodLe == null) {
            methodLe = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject args[], Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException("a Numeric excepted");
                    }
                    
                    return ((RubyFixnum)recv).op_le((RubyFixnum)args[0]);
                }
            };
        }
        return methodLe;
    }*/
}