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

import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyPrecision {
    
    public static RubyModule createPrecisionModule(Ruby runtime) {
        RubyModule precisionModule = runtime.defineModule("Precision");
        CallbackFactory callbackFactory = runtime.callbackFactory();
        precisionModule.defineSingletonMethod("append_features", callbackFactory.getSingletonMethod(RubyPrecision.class, "append_features", IRubyObject.class));
        precisionModule.defineMethod("prec", callbackFactory.getSingletonMethod(RubyPrecision.class, "prec", IRubyObject.class));
        precisionModule.defineMethod("prec_i", callbackFactory.getSingletonMethod(RubyPrecision.class, "prec_i"));
        precisionModule.defineMethod("prec_f", callbackFactory.getSingletonMethod(RubyPrecision.class, "prec_f"));
        return precisionModule;
    }

    public static IRubyObject induced_from(IRubyObject receiver, IRubyObject source) {
        throw new TypeError(receiver.getRuntime(), "Undefined conversion from " + source.getMetaClass().getName() + " into " + ((RubyClass)receiver).getName());
    }

    public static IRubyObject append_features(IRubyObject receiver, IRubyObject include) {
        if (include instanceof RubyModule) {
            ((RubyModule) include).includeModule(receiver);
            include.defineSingletonMethod("induced_from", receiver.getRuntime().callbackFactory().getSingletonMethod(RubyPrecision.class, "induced_from", IRubyObject.class));
        }
        return receiver;
    }
    
    public static IRubyObject prec(IRubyObject receiver, IRubyObject type) {
        return type.callMethod("induced_from", receiver);
    }

    public static IRubyObject prec_i(IRubyObject receiver) {
        return receiver.getRuntime().getClasses().getIntegerClass().callMethod("induced_from", receiver);
    }

    public static IRubyObject prec_f(IRubyObject receiver) {
        return receiver.getRuntime().getClasses().getFloatClass().callMethod("induced_from", receiver);
    }
}
