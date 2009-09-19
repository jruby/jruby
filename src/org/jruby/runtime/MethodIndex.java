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

import org.jruby.runtime.callsite.DivCallSite;
import org.jruby.runtime.callsite.LtCallSite;
import org.jruby.runtime.callsite.LeCallSite;
import org.jruby.runtime.callsite.MinusCallSite;
import org.jruby.runtime.callsite.MulCallSite;
import org.jruby.runtime.callsite.NormalCachingCallSite;
import org.jruby.runtime.callsite.GtCallSite;
import org.jruby.runtime.callsite.PlusCallSite;
import org.jruby.runtime.callsite.GeCallSite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.callsite.ArefCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;
import org.jruby.runtime.callsite.SuperCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;

/**
 *
 * @author headius
 */
public class MethodIndex {
    @Deprecated
    public static final List<String> NAMES = new ArrayList<String>();
    @Deprecated
    private static final Map<String, Integer> NUMBERS = new HashMap<String, Integer>();
    
    // ensure zero is devoted to no method name
    @Deprecated
    public static final int NO_INDEX = getIndex("");
    
    // predefine a few other methods we invoke directly elsewhere
    @Deprecated
    public static final int OP_PLUS = getIndex("+");
    @Deprecated
    public static final int OP_MINUS = getIndex("-");
    @Deprecated
    public static final int OP_LT = getIndex("<");
    @Deprecated
    public static final int AREF = getIndex("[]");
    @Deprecated
    public static final int ASET = getIndex("[]=");
    @Deprecated
    public static final int EQUALEQUAL = getIndex("==");
    @Deprecated
    public static final int OP_LSHIFT = getIndex("<<");
    @Deprecated
    public static final int EMPTY_P = getIndex("empty?");
    @Deprecated
    public static final int TO_S = getIndex("to_s");
    @Deprecated
    public static final int TO_I = getIndex("to_i");
    @Deprecated
    public static final int TO_STR = getIndex("to_str");
    @Deprecated
    public static final int TO_ARY = getIndex("to_ary");
    @Deprecated
    public static final int TO_INT = getIndex("to_int");
    @Deprecated
    public static final int TO_F = getIndex("to_f");
    @Deprecated
    public static final int TO_A = getIndex("to_a");
    @Deprecated
    public static final int TO_IO = getIndex("to_io");
    @Deprecated
    public static final int HASH = getIndex("hash");
    @Deprecated
    public static final int OP_GT = getIndex(">");
    @Deprecated
    public static final int OP_TIMES = getIndex("*");
    @Deprecated
    public static final int OP_LE = getIndex("<=");
    @Deprecated
    public static final int OP_SPACESHIP = getIndex("<=>");
    @Deprecated
    public static final int OP_EQQ = getIndex("===");
    @Deprecated
    public static final int EQL_P = getIndex("eql?");
    @Deprecated
    public static final int TO_HASH = getIndex("to_hash");
    @Deprecated
    public static final int METHOD_MISSING = getIndex("method_missing");
    @Deprecated
    public static final int DEFAULT = getIndex("default");

    @Deprecated
    public synchronized static int getIndex(String methodName) {
        Integer index = NUMBERS.get(methodName);
        
        if (index == null) {
            index = new Integer(NAMES.size());
            NUMBERS.put(methodName, index);
            NAMES.add(methodName);
        }
        
        return index;
    }
    
    public synchronized static CallSite getCallSite(String name) {
        // fast and safe respond_to? call site logic
        if (name.equals("respond_to?")) return new RespondToCallSite();
        
        if (RubyInstanceConfig.FASTOPS_COMPILE_ENABLED) return getFastOpsCallSite(name);

        return new NormalCachingCallSite(name);
    }

    public synchronized static CallSite getFastOpsCallSite(String name) {
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
        } else if (name.equals("[]")) {
            return new ArefCallSite();
        }

        return new NormalCachingCallSite(name);
    }
    
    public synchronized static CallSite getFunctionalCallSite(String name) {
        return new FunctionalCachingCallSite(name);
    }
    
    public synchronized static CallSite getVariableCallSite(String name) {
        return new VariableCachingCallSite(name);
    }

    public synchronized static CallSite getSuperCallSite() {
        return new SuperCallSite();
    }
}
