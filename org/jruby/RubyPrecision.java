/*
 * RubyPrecision.java - description
 * Created on 20.03.2002, 22:27:35
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.runtime.Callback;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyPrecision {
    
    public static RubyModule createPrecisionModule(Ruby ruby) {
        RubyModule precisionModule = ruby.defineModule("Precision");

        precisionModule.defineSingletonMethod("append_features", CallbackFactory.getSingletonMethod(RubyPrecision.class, "append_features", RubyObject.class));

        precisionModule.defineMethod("prec", CallbackFactory.getSingletonMethod(RubyPrecision.class, "prec", RubyObject.class));
        precisionModule.defineMethod("prec_i", CallbackFactory.getSingletonMethod(RubyPrecision.class, "prec_i"));
        precisionModule.defineMethod("prec_f", CallbackFactory.getSingletonMethod(RubyPrecision.class, "prec_f"));
        
        return precisionModule;     
    }

    public static RubyObject induced_from(Ruby ruby, RubyObject receiver, RubyObject source) {
        throw new TypeError(ruby, "Undefined conversion from " + source.getRubyClass().toName() + " into " + ((RubyClass)receiver).toName());
    }

    public static RubyObject append_features(Ruby ruby, RubyObject receiver, RubyObject include) {
        if (include instanceof RubyModule) {
            ((RubyModule)include).includeModule(receiver);
            ((RubyModule)include).defineSingletonMethod("induced_from", CallbackFactory.getSingletonMethod(RubyPrecision.class, "induced_from", RubyObject.class));
        }
        
        return receiver;
    }
    
    public static RubyObject prec(Ruby ruby, RubyObject receiver, RubyObject type) {
        return type.funcall("induced_from", receiver);
    }

    public static RubyObject prec_i(Ruby ruby, RubyObject receiver) {
        return ruby.getClasses().getIntegerClass().funcall("induced_from", receiver);
    }

    public static RubyObject prec_f(Ruby ruby, RubyObject receiver) {
        return ruby.getClasses().getFloatClass().funcall("induced_from", receiver);
    }
}
