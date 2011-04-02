/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006-2007 Charles Nutter <headius@headius.com>
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
package org.jruby.runtime;

import org.jruby.runtime.callsite.LtCallSite;
import org.jruby.runtime.callsite.LeCallSite;
import org.jruby.runtime.callsite.MinusCallSite;
import org.jruby.runtime.callsite.MulCallSite;
import org.jruby.runtime.callsite.NormalCachingCallSite;
import org.jruby.runtime.callsite.GtCallSite;
import org.jruby.runtime.callsite.PlusCallSite;
import org.jruby.runtime.callsite.GeCallSite;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.callsite.CmpCallSite;
import org.jruby.runtime.callsite.EqCallSite;
import org.jruby.runtime.callsite.BitAndCallSite;
import org.jruby.runtime.callsite.BitOrCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;
import org.jruby.runtime.callsite.ShiftLeftCallSite;
import org.jruby.runtime.callsite.ShiftRightCallSite;
import org.jruby.runtime.callsite.SuperCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.runtime.callsite.XorCallSite;

/**
 *
 * @author headius
 */
public class MethodIndex {
    public static final int NO_METHOD = 0;
    public static final int OP_EQUAL = 1;
    public static final int EQL = 2;
    public static final int HASH = 3;
    public static final int OP_CMP = 4;
    public static final int MAX_METHODS = 5;
    
    public static final String[] METHOD_NAMES = {
        "",
        "==",
        "eql?",
        "hash",
        "<=>"
    };

    public static CallSite getCallSite(String name) {
        // fast and safe respond_to? call site logic
        if (name.equals("respond_to?")) return new RespondToCallSite();

        // only use fast ops if we're not tracing
        if (RubyInstanceConfig.FASTOPS_COMPILE_ENABLED &&
                !(RubyInstanceConfig.FULL_TRACE_ENABLED)) return getFastOpsCallSite(name);

        return new NormalCachingCallSite(name);
    }
    
    public static boolean hasFastOps(String name) {
        return name.equals("+")
                || name.equals("-")
                || name.equals("*")
                || name.equals("<")
                || name.equals("<=")
                || name.equals(">")
                || name.equals(">=")
                || name.equals("==")
                || name.equals("<=>")
                || name.equals("&")
                || name.equals("|")
                || name.equals("^")
                || name.equals(">>")
                || name.equals("<<");
    }

    public static CallSite getFastOpsCallSite(String name) {
        if (name.equals("+")) {
            return new PlusCallSite();
        } else if (name.equals("-")) {
            return new MinusCallSite();
        } else if (name.equals("*")) {
            return new MulCallSite();
        } else if (name.equals("<")) {
            return new LtCallSite();
        } else if (name.equals("<=")) {
            return new LeCallSite();
        } else if (name.equals(">")) {
            return new GtCallSite();
        } else if (name.equals(">=")) {
            return new GeCallSite();
        } else if (name.equals("==")) {
            return new EqCallSite();
        } else if (name.equals("<=>")) {
            return new CmpCallSite();
        } else if (name.equals("&")) {
            return new BitAndCallSite();
        } else if (name.equals("|")) {
            return new BitOrCallSite();
        } else if (name.equals("^")) {
            return new XorCallSite();
        } else if (name.equals(">>")) {
            return new ShiftRightCallSite();
        } else if (name.equals("<<")) {
            return new ShiftLeftCallSite();
        // disabled because Array subclasses often override
//        } else if (name.equals("[]")) {
//            return new ArefCallSite();
//        } else if (name.equals("[]=")) {
//            return new AsetCallSite();
        // disabled because of differing 1.8/1.9 behavior
//        } else if (name.equals("%")) {
//            return new ModCallSite();
        }

        return new NormalCachingCallSite(name);
    }
    
    public static CallSite getFunctionalCallSite(String name) {
        return new FunctionalCachingCallSite(name);
    }
    
    public static CallSite getVariableCallSite(String name) {
        return new VariableCachingCallSite(name);
    }

    public static CallSite getSuperCallSite() {
        return new SuperCallSite();
    }
}
