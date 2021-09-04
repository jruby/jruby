/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.RubyInstanceConfig;
import org.jruby.anno.FrameField;
import org.jruby.ir.IRScope;
import org.jruby.runtime.callsite.DivCallSite;
import org.jruby.runtime.callsite.LtCallSite;
import org.jruby.runtime.callsite.LeCallSite;
import org.jruby.runtime.callsite.MinusCallSite;
import org.jruby.runtime.callsite.ModCallSite;
import org.jruby.runtime.callsite.MulCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.runtime.callsite.GtCallSite;
import org.jruby.runtime.callsite.PlusCallSite;
import org.jruby.runtime.callsite.GeCallSite;
import org.jruby.runtime.callsite.CmpCallSite;
import org.jruby.runtime.callsite.EqCallSite;
import org.jruby.runtime.callsite.BitAndCallSite;
import org.jruby.runtime.callsite.BitOrCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.ProfilingCachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;
import org.jruby.runtime.callsite.ShiftLeftCallSite;
import org.jruby.runtime.callsite.ShiftRightCallSite;
import org.jruby.runtime.callsite.SuperCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.runtime.callsite.XorCallSite;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.util.StringSupport;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

/**
 *
 * @author headius
 */
public class MethodIndex {
    private static final boolean DEBUG = false;

    private static final Logger LOG = LoggerFactory.getLogger(MethodIndex.class);

    @Deprecated
    public static final int NO_METHOD = MethodNames.DUMMY.ordinal();
    @Deprecated
    public static final int OP_EQUAL = MethodNames.OP_EQUAL.ordinal();
    @Deprecated
    public static final int EQL = MethodNames.EQL.ordinal();
    @Deprecated
    public static final int HASH = MethodNames.HASH.ordinal();
    @Deprecated
    public static final int OP_CMP = MethodNames.OP_CMP.ordinal();
    @Deprecated
    public static final int MAX_METHODS = MethodNames.values().length;

    @Deprecated
    public static final String[] METHOD_NAMES = {
        "",
        "==",
        "eql?",
        "hash",
        "<=>"
    };

    public static final Set<String> FRAME_AWARE_METHODS = Collections.synchronizedSet(new HashSet<String>());
    public static final Set<String> SCOPE_AWARE_METHODS = Collections.synchronizedSet(new HashSet<String>());

    public static final Map<String, Set<FrameField>> METHOD_FRAME_READS = new ConcurrentHashMap<>();
    public static final Map<String, Set<FrameField>> METHOD_FRAME_WRITES = new ConcurrentHashMap<>();

    public static CallSite getCallSite(String name) {
        // fast and safe respond_to? call site logic
        if (name.equals("respond_to?")) return new RespondToCallSite();

        CallSite callSite = null;

        // only use fast ops if we're not tracing
        if (RubyInstanceConfig.FASTOPS_COMPILE_ENABLED && !(RubyInstanceConfig.FULL_TRACE_ENABLED)) {
            callSite = getFastFixnumOpsCallSite(name);
        }

        return callSite != null ? callSite : new MonomorphicCallSite(name);
    }

    public static CallSite getProfilingCallSite(CallType callType, String name, IRScope scope, long callsiteId) {
        return new ProfilingCachingCallSite(callType, name, scope, callsiteId);
    }

    public static boolean hasFastFixnumOps(String name) {
        return getFastFixnumOpsMethod(name) != null;
    }

    public static String getFastFixnumOpsMethod(String name) {
        switch (name) {
            case "+"  : return "op_plus";
            case "-"  : return "op_minus";
            case "*"  : return "op_mul";
            case "%"  : return "op_mod";
            case "/"  : return "op_div";

            case "&"  : return "op_and";
            case "|"  : return "op_or";
            case "^"  : return "op_xor";
            case ">>" : return "op_rshift";
            case "<<" : return "op_lshift";

            case "==" : return "op_equal";
            case "<"  : return "op_lt";
            case "<=" : return "op_le";
            case ">"  : return "op_gt";
            case ">=" : return "op_ge";
            case "<=>": return "op_cmp";
        }
        return null;
    }

    public static CallSite getFastFixnumOpsCallSite(String name) {
        switch (name) {
            case "+"   : return new PlusCallSite();
            case "-"   : return new MinusCallSite();
            case "*"   : return new MulCallSite();
            case "%"   : return new ModCallSite();
            case "/"   : return new DivCallSite();

            case "&"   : return new BitAndCallSite();
            case "|"   : return new BitOrCallSite();
            case "^"   : return new XorCallSite();
            case ">>"  : return new ShiftRightCallSite();
            case "<<"  : return new ShiftLeftCallSite();

            case "=="  : return new EqCallSite();
            case "<"   : return new LtCallSite();
            case "<="  : return new LeCallSite();
            case ">"   : return new GtCallSite();
            case ">="  : return new GeCallSite();
            case "<=>" : return new CmpCallSite();
        }
        return null;
    }

    public static boolean hasFastFloatOps(String name) {
        return getFastFloatOpsMethod(name) != null;
    }

    public static String getFastFloatOpsMethod(String name) {
        switch (name) {
            case "+"  : return "op_plus";
            case "-"  : return "op_minus";
            case "*"  : return "op_mul";
            case "%"  : return "op_mod";
            case "/"  : return "op_div";

            case "==" : return "op_equal";
            case "<"  : return "op_lt";
            case "<=" : return "op_le";
            case ">"  : return "op_gt";
            case ">=" : return "op_ge";
            case "<=>": return "op_cmp";
        }
        return null;
    }

    public static CallSite getFastFloatOpsCallSite(String name) {
        switch (name) {
            case "+"   : return new PlusCallSite();
            case "-"   : return new MinusCallSite();
            case "*"   : return new MulCallSite();
            case "%"   : return new ModCallSite();
            case "/"   : return new DivCallSite();

            case "=="  : return new EqCallSite();
            case "<"   : return new LtCallSite();
            case "<="  : return new LeCallSite();
            case ">"   : return new GtCallSite();
            case ">="  : return new GeCallSite();
            case "<=>" : return new CmpCallSite();
        }
        return null;
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

    public static void addMethodReadFieldsPacked(int readBits, String methodsPacked) {
        processFrameFields(readBits, methodsPacked, "read", METHOD_FRAME_READS);
    }

    public static void addMethodWriteFieldsPacked(int writeBits, String methodsPacked) {
        processFrameFields(writeBits, methodsPacked, "write", METHOD_FRAME_WRITES);
    }

    private static void processFrameFields(int bits, String methodNames, String usage, Map<String, Set<FrameField>> methodFrameAccesses) {
        Set<FrameField> writes = FrameField.unpack(bits);

        boolean needsFrame = FrameField.needsFrame(bits);
        boolean needsScope = FrameField.needsScope(bits);

        if (DEBUG) LOG.debug("Adding method fields for {}: {} for {}", usage, writes, methodNames);

        if (writes.size() > 0) {
            List<String> names = StringSupport.split(methodNames, ';');

            addAwareness(needsFrame, needsScope, names);

            addFieldAccesses(methodFrameAccesses, names, writes);
        }
    }

    private static void addFieldAccesses(Map<String, Set<FrameField>> methodFrameWrites, List<String> names, Set<FrameField> writes) {
        for (String name : names) {
            methodFrameWrites.compute(
                    name,
                    (key, cur) -> cur == null ? writes : concat(cur.stream(), writes.stream()).collect(toSet()));
        }
    }

    private static void addAwareness(boolean needsFrame, boolean needsScope, List<String> names) {
        if (needsFrame) FRAME_AWARE_METHODS.addAll(names);
        if (needsScope) SCOPE_AWARE_METHODS.addAll(names);
    }

    public static void addMethodReadFields(String name, FrameField... reads) {
        addMethodReadFieldsPacked(FrameField.pack(reads), name);
    }

    public static void addMethodWriteFields(String name, FrameField... write) {
        addMethodWriteFieldsPacked(FrameField.pack(write), name);
    }

    @Deprecated
    public static void addFrameAwareMethods(String... methods) {
        if (DEBUG) LOG.debug("Adding frame-aware method names: {}", Arrays.toString(methods));
        FRAME_AWARE_METHODS.addAll(Arrays.asList(methods));
    }

    @Deprecated
    public static void addScopeAwareMethods(String... methods) {
        if (DEBUG) LOG.debug("Adding scope-aware method names: {}", Arrays.toString(methods));
        SCOPE_AWARE_METHODS.addAll(Arrays.asList(methods));
    }
}
