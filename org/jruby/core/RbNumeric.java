/*
 * RbNumeric.java - No description
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

import org.jruby.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RbNumeric {
    private static RubyCallbackMethod methodCoerce = null;
    private static RubyCallbackMethod methodClone = null;
    
    private static RubyCallbackMethod methodUPlus = null;
    private static RubyCallbackMethod methodUMinus = null;
    private static RubyCallbackMethod methodEqual = null;
    private static RubyCallbackMethod methodEql = null;
    private static RubyCallbackMethod methodDivmod = null;
    private static RubyCallbackMethod methodModulo = null;
    private static RubyCallbackMethod methodRemainder = null;
    private static RubyCallbackMethod methodAbs = null;

    private static RubyCallbackMethod methodIntP = null;
    private static RubyCallbackMethod methodZeroP = null;
    private static RubyCallbackMethod methodNonzeroP = null;

    private static RubyCallbackMethod methodFloor = null;
    private static RubyCallbackMethod methodCeil = null;
    private static RubyCallbackMethod methodRound = null;
    private static RubyCallbackMethod methodTruncate = null;
    
    public static RubyClass createNumericClass(Ruby ruby) {
        RubyClass numericClass = ruby.defineClass("Numeric", ruby.getClasses().getObjectClass());
     
        numericClass.includeModule(ruby.getClasses().getComparableModule());
        
        numericClass.defineMethod("coerce", getMethodCoerce());
        numericClass.defineMethod("clone", getMethodClone());
        
        numericClass.defineMethod("+@", getMethodUPlus());
        numericClass.defineMethod("-@", getMethodUMinus());
        numericClass.defineMethod("===", getMethodEqual());
        numericClass.defineMethod("eql?", getMethodEql());
        numericClass.defineMethod("divmod", getMethodDivmod());
        numericClass.defineMethod("modulo", getMethodModulo());
        numericClass.defineMethod("remainder", getMethodRemainder());
        numericClass.defineMethod("abs", getMethodAbs());
        
        numericClass.defineMethod("integer?", getMethodIntP());
        numericClass.defineMethod("zero?", getMethodZeroP());
        numericClass.defineMethod("nonzero?", getMethodNonzeroP());
        
        numericClass.defineMethod("floor", getMethodFloor());
        numericClass.defineMethod("ceil", getMethodCeil());
        numericClass.defineMethod("round", getMethodRound());
        numericClass.defineMethod("truncate", getMethodTruncate());

        return numericClass;
    }

    public static RubyCallbackMethod getMethodCoerce() {
        if (methodCoerce == null) {
            methodCoerce = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException();
                    }
                    
                    return ((RubyNumeric)recv).m_coerce((RubyNumeric)args[0]);
                }
            };
        }
        
        return methodCoerce;
    }

    public static RubyCallbackMethod getMethodClone() {
        if (methodClone == null) {
            methodClone = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return recv.m_clone();
                }
            };
        }
        return methodClone;
    }

    public static RubyCallbackMethod getMethodUPlus() {
        if (methodUPlus == null) {
            methodUPlus = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).op_uplus();
                }
            };
        }
        
        return methodUPlus;
    }

    public static RubyCallbackMethod getMethodUMinus() {
        if (methodUMinus == null) {
            methodUMinus = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).op_uminus();
                }
            };
        }
        return methodUMinus;
    }

    public static RubyCallbackMethod getMethodEqual() {
        if (methodEqual == null) {
            methodEqual = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException();
                    }
                    
                    return recv.m_equal(args[0]);
                }
            };
        }
        return methodEqual;
    }

    public static RubyCallbackMethod getMethodEql() {
        if (methodEql == null) {
            methodEql = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException();
                    }
                    
                    return ((RubyNumeric)recv).m_eql(args[0]);
                }
            };
        }
        
        return methodEql;
    }

    public static RubyCallbackMethod getMethodDivmod() {
        if (methodDivmod == null) {
            methodDivmod = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException();
                    }
                    
                    return ((RubyNumeric)recv).m_divmod((RubyNumeric)args[0]);
                }
            };
        }
        return methodDivmod;
    }

    public static RubyCallbackMethod getMethodModulo() {
        if (methodModulo == null) {
            methodModulo = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException();
                    }
                    
                    return ((RubyNumeric)recv).m_modulo((RubyNumeric)args[0]);
                }
            };
        }
        return methodModulo;
    }

    public static RubyCallbackMethod getMethodRemainder() {
        if (methodRemainder == null) {
            methodRemainder = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException();
                    }
                    
                    return ((RubyNumeric)recv).m_remainder((RubyNumeric)args[0]);
                }
            };
        }
        return methodRemainder;
    }

    public static RubyCallbackMethod getMethodAbs() {
        if (methodAbs == null) {
            methodAbs = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_abs();
                }
            };
        }
        
        return methodAbs;
    }

    public static RubyCallbackMethod getMethodIntP() {
        if (methodIntP == null) {
            methodIntP = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_int_p();
                }
            };
        }
        
        return methodIntP;
    }

    public static RubyCallbackMethod getMethodZeroP() {
        if (methodZeroP == null) {
            methodZeroP = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_zero_p();
                }
            };
        }
        
        return methodZeroP;
    }

    public static RubyCallbackMethod getMethodNonzeroP() {
        if (methodNonzeroP == null) {
            methodNonzeroP = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_nonzero_p();
                }
            };
        }
        
        return methodNonzeroP;
    }

    public static RubyCallbackMethod getMethodFloor() {
        if (methodFloor == null) {
            methodFloor = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_floor();
                }
            };
        }
        
        return methodFloor;
    }

    public static RubyCallbackMethod getMethodCeil() {
        if (methodCeil == null) {
            methodCeil = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_ceil();
                }
            };
        }
        
        return methodCeil;
    }
    
    public static RubyCallbackMethod getMethodRound() {
        if (methodRound == null) {
            methodRound = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_round();
                }
            };
        }
        
        return methodRound;
    }

    public static RubyCallbackMethod getMethodTruncate() {
        if (methodTruncate == null) {
            methodTruncate = new RubyCallbackMethod() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_truncate();
                }
            };
        }
        
        return methodTruncate;
    }
}