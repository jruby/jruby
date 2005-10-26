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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
public class RubyPrecision {
    
    public static RubyModule createPrecisionModule(IRuby runtime) {
        RubyModule precisionModule = runtime.defineModule("Precision");
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyPrecision.class);
        precisionModule.defineSingletonMethod("append_features", callbackFactory.getSingletonMethod("append_features", IRubyObject.class));
        precisionModule.defineMethod("prec", callbackFactory.getSingletonMethod("prec", IRubyObject.class));
        precisionModule.defineMethod("prec_i", callbackFactory.getSingletonMethod("prec_i"));
        precisionModule.defineMethod("prec_f", callbackFactory.getSingletonMethod("prec_f"));
        return precisionModule;
    }

    public static IRubyObject induced_from(IRubyObject receiver, IRubyObject source) {
        throw receiver.getRuntime().newTypeError("Undefined conversion from " + source.getMetaClass().getName() + " into " + ((RubyClass)receiver).getName());
    }

    public static IRubyObject append_features(IRubyObject receiver, IRubyObject include) {
        if (include instanceof RubyModule) {
            ((RubyModule) include).includeModule(receiver);
            CallbackFactory f = receiver.getRuntime().callbackFactory(RubyPrecision.class);
            include.defineSingletonMethod("induced_from", f.getSingletonMethod("induced_from", IRubyObject.class));
        }
        return receiver;
    }
    
    public static IRubyObject prec(IRubyObject receiver, IRubyObject type) {
        return type.callMethod("induced_from", receiver);
    }

    public static IRubyObject prec_i(IRubyObject receiver) {
        return receiver.getRuntime().getClass("Integer").callMethod("induced_from", receiver);
    }

    public static IRubyObject prec_f(IRubyObject receiver) {
        return receiver.getRuntime().getClass("Float").callMethod("induced_from", receiver);
    }
}
