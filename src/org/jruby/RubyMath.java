/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyModule(name="Math")
public class RubyMath {
    /** Create the Math module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createMathModule(Ruby runtime) {
        RubyModule result = runtime.defineModule("Math");
        runtime.setMath(result);
        
        result.defineConstant("E", RubyFloat.newFloat(runtime, Math.E));
        result.defineConstant("PI", RubyFloat.newFloat(runtime, Math.PI));
        
        result.defineAnnotatedMethods(RubyMath.class);

        return result;
    }
    
    
    private static void domainCheck(IRubyObject recv, double value, String msg) {  
        if (Double.isNaN(value)) {
            throw recv.getRuntime().newErrnoEDOMError(msg);
        }
    }
    
    private static double chebylevSerie(double x, double coef[]) {
        double  b0, b1, b2, twox;
        int i;
        b1 = 0.0;
        b0 = 0.0;
        b2 = 0.0;
        twox = 2.0 * x;
        for (i = coef.length-1; i >= 0; i--) {
            b2 = b1;
            b1 = b0;
            b0 = twox * b1 - b2 + coef[i];
        }
        return 0.5*(b0 - b2);
    }
    
    private static double sign(double x, double y) {
        double abs = ((x < 0) ? -x : x);
        return (y < 0.0) ? -abs : abs;
    }
    
    @JRubyMethod(name = "atan2", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat atan2(IRubyObject recv, IRubyObject x, IRubyObject y) {
        double valuea = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double valueb = ((RubyFloat)RubyKernel.new_float(recv,y)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(), Math.atan2(valuea, valueb));
    }

    @JRubyMethod(name = "cos", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat cos(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),Math.cos(value));
    }

    @JRubyMethod(name = "sin", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat sin(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),Math.sin(value));
    }

    @JRubyMethod(name = "tan", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat tan(IRubyObject recv,  IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),Math.tan(value));
    }
    
    @JRubyMethod(name = "asin", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat asin(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double result = Math.asin(value);
        domainCheck(recv, result, "asin");        
        return RubyFloat.newFloat(recv.getRuntime(),result);
    }

    @JRubyMethod(name = "acos", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat acos(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double result = Math.acos(value);  
        domainCheck(recv, result, "acos");
        return RubyFloat.newFloat(recv.getRuntime(), result);
    }
    
    @JRubyMethod(name = "atan", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat atan(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),Math.atan(value));
    }

    @JRubyMethod(name = "cosh", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat cosh(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),(Math.exp(value) + Math.exp(-value)) / 2.0);
    }    

    @JRubyMethod(name = "sinh", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat sinh(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),(Math.exp(value) - Math.exp(-value)) / 2.0);
    }
    
    @JRubyMethod(name = "tanh", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat tanh(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(), Math.tanh(value));
    }          
    
    @JRubyMethod(name = "acosh", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat acosh(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double result;
        if (Double.isNaN(value) || value < 1) {
            result = Double.NaN;
        } else if (value < 94906265.62) {
            result = Math.log(value + Math.sqrt(value * value - 1.0));
        } else{
            result = 0.69314718055994530941723212145818 + Math.log(value);
        }
        
        domainCheck(recv, result, "acosh");
        
        return RubyFloat.newFloat(recv.getRuntime(),result);
    }
    
    private static final double ASINH_COEF[] = {
        -.12820039911738186343372127359268e+0,
        -.58811761189951767565211757138362e-1,
        .47274654322124815640725249756029e-2,
        -.49383631626536172101360174790273e-3,
        .58506207058557412287494835259321e-4,
        -.74669983289313681354755069217188e-5,
        .10011693583558199265966192015812e-5,
        -.13903543858708333608616472258886e-6,
        .19823169483172793547317360237148e-7,
        -.28847468417848843612747272800317e-8,
        .42672965467159937953457514995907e-9,
        -.63976084654366357868752632309681e-10,
        .96991686089064704147878293131179e-11,
        -.14844276972043770830246658365696e-11,
        .22903737939027447988040184378983e-12,
        -.35588395132732645159978942651310e-13,
        .55639694080056789953374539088554e-14,
        -.87462509599624678045666593520162e-15,
        .13815248844526692155868802298129e-15,
        -.21916688282900363984955142264149e-16,
        .34904658524827565638313923706880e-17
    };      
    
    @JRubyMethod(name = "asinh", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat asinh(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double  y = Math.abs(value);
        double result;
        
        if (Double.isNaN(value)) {
            result = Double.NaN;
        } else if (y <= 1.05367e-08) {
            result = value;
        } else if (y <= 1.0) {          
            result = value * (1.0 + chebylevSerie(2.0 * value * value - 1.0, ASINH_COEF));
        } else if (y < 94906265.62) {
            result = Math.log(value + Math.sqrt(value * value + 1.0));
        } else {    
            result = 0.69314718055994530941723212145818 + Math.log(y);
            if (value < 0) result *= -1;
        }

        return RubyFloat.newFloat(recv.getRuntime(),result);        
    }
    
    private static final double ATANH_COEF[] = {
        .9439510239319549230842892218633e-1,
        .4919843705578615947200034576668e-1,
        .2102593522455432763479327331752e-2,
        .1073554449776116584640731045276e-3,
        .5978267249293031478642787517872e-5,
        .3505062030889134845966834886200e-6,
        .2126374343765340350896219314431e-7,
        .1321694535715527192129801723055e-8,
        .8365875501178070364623604052959e-10,
        .5370503749311002163881434587772e-11,
        .3486659470157107922971245784290e-12,
        .2284549509603433015524024119722e-13,
        .1508407105944793044874229067558e-14,
        .1002418816804109126136995722837e-15,
        .6698674738165069539715526882986e-17,
        .4497954546494931083083327624533e-18
    };    
    
    @JRubyMethod(name = "atanh", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat atanh(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double  y = Math.abs(value);
        double  result;

        if (Double.isNaN(value)) {
            result = Double.NaN;
        } else if (y < 1.82501e-08) {
            result = value;
        } else if (y <= 0.5) {
            result = value * (1.0 + chebylevSerie(8.0 * value * value - 1.0, ATANH_COEF));
        } else if (y < 1.0) {
            result = 0.5 * Math.log((1.0 + value) / (1.0 - value));
        } else if (y == 1.0) {
            result = value * Double.POSITIVE_INFINITY;
        } else {
            result = Double.NaN;
        }

        domainCheck(recv, result, "atanh");
        return RubyFloat.newFloat(recv.getRuntime(),result);        
    }
    
    @JRubyMethod(name = "exp", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat exp(IRubyObject recv, IRubyObject exponent) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,exponent)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),Math.exp(value));
    }

    /** Returns the natural logarithm of x.
     * 
     */
    @JRubyMethod(name = "log", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat log(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double result = Math.log(value);
        domainCheck(recv, result, "log");
        return RubyFloat.newFloat(recv.getRuntime(),result);
    }

    /** Returns the base 10 logarithm of x.
     * 
     */
    @JRubyMethod(name = "log10", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat log10(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double result =  Math.log(value) / Math.log(10);
        domainCheck(recv, result, "log10");
        return RubyFloat.newFloat(recv.getRuntime(),result);
    }

    @JRubyMethod(name = "sqrt", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat sqrt(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double result;

        if (value < 0) {
            result = Double.NaN;
        } else{
            result = Math.sqrt(value);
        }
        
        domainCheck(recv, result, "sqrt");
        return RubyFloat.newFloat(recv.getRuntime(), result);
    }
    
    @JRubyMethod(name = "hypot", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat hypot(IRubyObject recv, IRubyObject x, IRubyObject y) {
        double valuea = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue(); 
        double valueb = ((RubyFloat)RubyKernel.new_float(recv,y)).getDoubleValue();
        double result;
        
        if (Math.abs(valuea) > Math.abs(valueb)) {
            result = valueb / valuea;
            result = Math.abs(valuea) * Math.sqrt(1 + result * result);
        } else if (valueb != 0) {
            result = valuea / valueb;
            result = Math.abs(valueb) * Math.sqrt(1 + result * result);
        } else {
            result = 0;
        }
        return RubyFloat.newFloat(recv.getRuntime(),result);
    }    
    
    
    /*
     * x = mantissa * 2 ** exponent
     *
     * Where mantissa is in the range of [.5, 1)
     *
     */
    @JRubyMethod(name = "frexp", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyArray frexp(IRubyObject recv, IRubyObject other) {
        double mantissa = ((RubyFloat)RubyKernel.new_float(recv,other)).getDoubleValue();
        short sign = 1;
        long exponent = 0;

        if (!Double.isInfinite(mantissa) && mantissa != 0.0) {
            // Make mantissa same sign so we only have one code path.
            if (mantissa < 0) {
                mantissa = -mantissa;
                sign = -1;
            }

            // Increase value to hit lower range.
            for (; mantissa < 0.5; mantissa *= 2.0, exponent -=1) { }

            // Decrease value to hit upper range.  
            for (; mantissa >= 1.0; mantissa *= 0.5, exponent +=1) { }
        }
	 
        return RubyArray.newArray(recv.getRuntime(), 
                                 RubyFloat.newFloat(recv.getRuntime(), sign * mantissa),
                                 RubyNumeric.int2fix(recv.getRuntime(), exponent));
    }

    /*
     * r = x * 2 ** y
     */
    @JRubyMethod(name = "ldexp", required = 2, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat ldexp(IRubyObject recv, IRubyObject mantissa, IRubyObject exponent) {
        double mantissaValue = ((RubyFloat)RubyKernel.new_float(recv, mantissa)).getDoubleValue();
        return RubyFloat.newFloat(recv.getRuntime(),mantissaValue * Math.pow(2.0, RubyNumeric.num2int(exponent)));
    }

    private static final double ERFC_COEF[] = {
         -.490461212346918080399845440334e-1,
         -.142261205103713642378247418996e0,
         .100355821875997955757546767129e-1,
         -.576876469976748476508270255092e-3,
         .274199312521960610344221607915e-4,
         -.110431755073445076041353812959e-5,
         .384887554203450369499613114982e-7,
         -.118085825338754669696317518016e-8,
         .323342158260509096464029309534e-10,
         -.799101594700454875816073747086e-12,
         .179907251139614556119672454866e-13,
         -.371863548781869263823168282095e-15,
         .710359900371425297116899083947e-17,
         -.126124551191552258324954248533e-18
    };
    
    @JRubyMethod(name = "erf", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat erf(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();

        double  result;
        double  y = Math.abs(value);

        if (y <= 1.49012e-08) {
            result = 2 * value / 1.77245385090551602729816748334;
        } else if (y <= 1) {
            result = value * (1 + chebylevSerie(2 * value * value - 1, ERFC_COEF));
        } else if (y < 6.013687357) {
            result = sign(1 - erfc(recv, RubyFloat.newFloat(recv.getRuntime(),y)).getDoubleValue(), value);
        } else {
            result = sign(1, value);
        }
        return RubyFloat.newFloat(recv.getRuntime(),result);
    }

    private static final double ERFC2_COEF[] = {
         -.69601346602309501127391508262e-1,
         -.411013393626208934898221208467e-1,
         .391449586668962688156114370524e-2,
         -.490639565054897916128093545077e-3,
         .715747900137703638076089414183e-4,
         -.115307163413123283380823284791e-4,
         .199467059020199763505231486771e-5,
         -.364266647159922287393611843071e-6,
         .694437261000501258993127721463e-7,
         -.137122090210436601953460514121e-7,
         .278838966100713713196386034809e-8,
         -.581416472433116155186479105032e-9,
         .123892049175275318118016881795e-9,
         -.269063914530674343239042493789e-10,
         .594261435084791098244470968384e-11,
         -.133238673575811957928775442057e-11,
         .30280468061771320171736972433e-12,
         -.696664881494103258879586758895e-13,
         .162085454105392296981289322763e-13,
         -.380993446525049199987691305773e-14,
         .904048781597883114936897101298e-15,
         -.2164006195089607347809812047e-15,
         .522210223399585498460798024417e-16,
         -.126972960236455533637241552778e-16,
         .310914550427619758383622741295e-17,
         -.766376292032038552400956671481e-18,
         .190081925136274520253692973329e-18
    };

    private static final double ERFCC_COEF[] = {
         .715179310202924774503697709496e-1,
         -.265324343376067157558893386681e-1,
         .171115397792085588332699194606e-2,
         -.163751663458517884163746404749e-3,
         .198712935005520364995974806758e-4,
         -.284371241276655508750175183152e-5,
         .460616130896313036969379968464e-6,
         -.822775302587920842057766536366e-7,
         .159214187277090112989358340826e-7,
         -.329507136225284321486631665072e-8,
         .72234397604005554658126115389e-9,
         -.166485581339872959344695966886e-9,
         .401039258823766482077671768814e-10,
         -.100481621442573113272170176283e-10,
         .260827591330033380859341009439e-11,
         -.699111056040402486557697812476e-12,
         .192949233326170708624205749803e-12,
         -.547013118875433106490125085271e-13,
         .158966330976269744839084032762e-13,
         -.47268939801975548392036958429e-14,
         .14358733767849847867287399784e-14,
         -.444951056181735839417250062829e-15,
         .140481088476823343737305537466e-15,
         -.451381838776421089625963281623e-16,
         .147452154104513307787018713262e-16,
         -.489262140694577615436841552532e-17,
         .164761214141064673895301522827e-17,
         -.562681717632940809299928521323e-18,
         .194744338223207851429197867821e-18
    };
        
    @JRubyMethod(name = "erfc", required = 1, module = true, visibility = Visibility.PRIVATE)
    public static RubyFloat erfc(IRubyObject recv, IRubyObject x) {
        double value = ((RubyFloat)RubyKernel.new_float(recv,x)).getDoubleValue();
        double  result;
        double  y = Math.abs(value);

        if (value <= -6.013687357) {
            result = 2;
        } else if (y < 1.49012e-08) {
            result = 1 - 2 * value / 1.77245385090551602729816748334;
        } else {
            double ysq = y*y;
            if (y < 1) {
                result = 1 - value * (1 + chebylevSerie(2 * ysq - 1, ERFC_COEF));
            } else if (y <= 4.0) {
                result = Math.exp(-ysq)/y*(0.5+chebylevSerie((8.0 / ysq - 5.0) / 3.0, ERFC2_COEF));
                if (value < 0) result = 2.0 - result;
                if (value < 0) result = 2.0 - result;
                if (value < 0) result = 2.0 - result;
            } else {
                result = Math.exp(-ysq) / y * (0.5 + chebylevSerie(8.0 / ysq - 1, ERFCC_COEF));
                if (value < 0) result = 2.0 - result;
            }
        }
        return RubyFloat.newFloat(recv.getRuntime(),result);        
    }

}
