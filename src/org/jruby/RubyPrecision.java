/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
@JRubyModule(name="Precision")
public class RubyPrecision {
    
    public static RubyModule createPrecisionModule(Ruby runtime) {
        RubyModule precisionModule = runtime.defineModule("Precision");
        runtime.setPrecision(precisionModule);
        
        precisionModule.defineAnnotatedMethods(RubyPrecision.class);
        
        return precisionModule;
    }

    public static IRubyObject induced_from(IRubyObject receiver, IRubyObject source, Block block) {
        throw receiver.getRuntime().newTypeError("Undefined conversion from " + source.getMetaClass().getName() + " into " + ((RubyClass)receiver).getName());
    }

    @JRubyMethod(module = true)
    public static IRubyObject append_features(IRubyObject receiver, IRubyObject include, Block block) {
        if (include instanceof RubyModule) {
            ((RubyModule) include).includeModule(receiver);
            include.getSingletonClass().addMethod("induced_from", new JavaMethod.JavaMethodOne(include.getSingletonClass(), Visibility.PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
                    return RubyPrecision.induced_from(self, arg0, Block.NULL_BLOCK);
                }
            });
        }
        return receiver;
    }
    
    
    @JRubyMethod
    public static IRubyObject prec(ThreadContext context, IRubyObject receiver, IRubyObject type, Block block) {
        return type.callMethod(context, "induced_from", receiver);
    }

    @JRubyMethod
    public static IRubyObject prec_i(ThreadContext context, IRubyObject receiver, Block block) {
        return receiver.getRuntime().getInteger().callMethod(context, "induced_from", receiver);
    }

    @JRubyMethod
    public static IRubyObject prec_f(ThreadContext context, IRubyObject receiver, Block block) {
        return receiver.getRuntime().getFloat().callMethod(context, "induced_from", receiver);
    }
}
