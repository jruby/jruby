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
    private static Callback methodCoerce = null;
    private static Callback methodClone = null;
    
    private static Callback methodUPlus = null;
    private static Callback methodUMinus = null;
    private static Callback methodEqual = null;
    private static Callback methodEql = null;
    private static Callback methodDivmod = null;
    private static Callback methodModulo = null;
    private static Callback methodRemainder = null;
    private static Callback methodAbs = null;

    private static Callback methodIntP = null;
    private static Callback methodZeroP = null;
    private static Callback methodNonzeroP = null;

    private static Callback methodFloor = null;
    private static Callback methodCeil = null;
    private static Callback methodRound = null;
    private static Callback methodTruncate = null;
    
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

    public static Callback getMethodCoerce() {
        if (methodCoerce == null) {
            methodCoerce = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException(ruby, "wrong argument count");
                    }
                    
                    return ((RubyNumeric)recv).m_coerce((RubyNumeric)args[0]);
                }
            };
        }
        
        return methodCoerce;
    }

    public static Callback getMethodClone() {
        if (methodClone == null) {
            methodClone = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return recv.rbClone();
                }
            };
        }
        return methodClone;
    }

    public static Callback getMethodUPlus() {
        if (methodUPlus == null) {
            methodUPlus = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).op_uplus();
                }
            };
        }
        
        return methodUPlus;
    }

    public static Callback getMethodUMinus() {
        if (methodUMinus == null) {
            methodUMinus = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).op_uminus();
                }
            };
        }
        return methodUMinus;
    }

    public static Callback getMethodEqual() {
        if (methodEqual == null) {
            methodEqual = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException(ruby, "wrong argument count");
                    }
                    
                    return RubyBoolean.newBoolean(ruby, recv.equals(args[0]));
                }
            };
        }
        return methodEqual;
    }

    public static Callback getMethodEql() {
        if (methodEql == null) {
            methodEql = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException(ruby, "wrong argument count");
                    }
                    
                    return ((RubyNumeric)recv).m_eql(args[0]);
                }
            };
        }
        
        return methodEql;
    }

    public static Callback getMethodDivmod() {
        if (methodDivmod == null) {
            methodDivmod = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException(ruby, "wrong argument count");
                    }
                    
                    return ((RubyNumeric)recv).m_divmod((RubyNumeric)args[0]);
                }
            };
        }
        return methodDivmod;
    }

    public static Callback getMethodModulo() {
        if (methodModulo == null) {
            methodModulo = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException(ruby, "wrong argument count");
                    }
                    
                    return ((RubyNumeric)recv).m_modulo((RubyNumeric)args[0]);
                }
            };
        }
        return methodModulo;
    }

    public static Callback getMethodRemainder() {
        if (methodRemainder == null) {
            methodRemainder = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    if (args.length < 1) {
                        throw new RubyArgumentException(ruby, "wrong argument count");
                    }
                    
                    return ((RubyNumeric)recv).m_remainder((RubyNumeric)args[0]);
                }
            };
        }
        return methodRemainder;
    }

    public static Callback getMethodAbs() {
        if (methodAbs == null) {
            methodAbs = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_abs();
                }
            };
        }
        
        return methodAbs;
    }

    public static Callback getMethodIntP() {
        if (methodIntP == null) {
            methodIntP = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_int_p();
                }
            };
        }
        
        return methodIntP;
    }

    public static Callback getMethodZeroP() {
        if (methodZeroP == null) {
            methodZeroP = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_zero_p();
                }
            };
        }
        
        return methodZeroP;
    }

    public static Callback getMethodNonzeroP() {
        if (methodNonzeroP == null) {
            methodNonzeroP = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_nonzero_p();
                }
            };
        }
        
        return methodNonzeroP;
    }

    public static Callback getMethodFloor() {
        if (methodFloor == null) {
            methodFloor = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_floor();
                }
            };
        }
        
        return methodFloor;
    }

    public static Callback getMethodCeil() {
        if (methodCeil == null) {
            methodCeil = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_ceil();
                }
            };
        }
        
        return methodCeil;
    }
    
    public static Callback getMethodRound() {
        if (methodRound == null) {
            methodRound = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_round();
                }
            };
        }
        
        return methodRound;
    }

    public static Callback getMethodTruncate() {
        if (methodTruncate == null) {
            methodTruncate = new Callback() {
                public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                    return ((RubyNumeric)recv).m_truncate();
                }
            };
        }
        
        return methodTruncate;
    }
}