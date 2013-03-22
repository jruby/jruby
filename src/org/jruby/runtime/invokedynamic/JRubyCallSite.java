/*
 ***** BEGIN LICENSE BLOCK *****
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

package org.jruby.runtime.invokedynamic;

import com.headius.invokebinder.Signature;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.jruby.runtime.Block;

import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

public class JRubyCallSite extends MutableCallSite {
    private final Lookup lookup;
    private final CallType callType;
    public CacheEntry entry = CacheEntry.NULL_CACHE;
    private final Set<Integer> seenTypes = new HashSet<Integer>();
    private final boolean attrAssign;
    private final boolean iterator;
    private final boolean expression;
    private final String name;
    private int clearCount;
    private static final AtomicLong SITE_ID = new AtomicLong(1);
    private final long siteID = SITE_ID.getAndIncrement();
    private final String file;
    private final int line;
    private boolean boundOnce = false;
    private final Signature signature;
    private final Signature fullSignature;
    private final int arity;

    public JRubyCallSite(Lookup lookup, MethodType type, CallType callType, String file, int line, String name, boolean attrAssign, boolean iterator, boolean expression) {
        super(type);
        this.lookup = lookup;
        this.callType = callType;
        this.attrAssign = attrAssign;
        this.iterator = iterator;
        this.expression = expression;
        this.name = name;
        this.file = file;
        this.line = line;
        
        // all signatures have (context, caller, self), so length, block, and arg before block indicates signature
        int arity = -1;
        if (type.parameterType(type.parameterCount() - 1) == Block.class) {
            switch (type.parameterCount()) {
                case 4:
                    arity = 0;
                    break;
                case 5:
                    arity = (type.parameterType(3) == IRubyObject[].class) ? 4 : 1;
                    break;
                case 6:
                    arity = 2;
                    break;
                case 7:
                    arity = 3;
                    break;
                default:
                    throw new RuntimeException("unknown incoming signature: " + type);
            }

            fullSignature = signature = STANDARD_SITE_SIGS_BLOCK[arity];
        } else {
            switch (type.parameterCount()) {
                case 3:
                    arity = 0;
                    break;
                case 4:
                    arity = (type.parameterType(3) == IRubyObject[].class) ? 4 : 1;
                    break;
                case 5:
                    arity = 2;
                    break;
                case 6:
                    arity = 3;
                    break;
                default:
                    throw new RuntimeException("unknown incoming signature: " + type);
            }

            signature = STANDARD_SITE_SIGS[arity];
            fullSignature = STANDARD_SITE_SIGS_BLOCK[arity];
        }
        
        this.arity = getSiteCount(type.parameterArray());
    }
    
    public int arity() {
        return arity;
    }

    private static int getSiteCount(Class[] args) {
        if (args[args.length - 1] == Block.class) {
            if (args[args.length - 2] == IRubyObject[].class) {
                return 4;
            } else {
                return args.length - 4; // TC, caller, self, block
            }
        } else {
            if (args[args.length - 1] == IRubyObject[].class) {
                return 4;
            } else {
                return args.length - 3; // TC, caller, self
            }
        }
    }
    
    public Lookup lookup() {
        return lookup;
    }

    public CallType callType() {
        return callType;
    }

    public boolean isAttrAssign() {
        return attrAssign;
    }
    
    public boolean isIterator() {
        return iterator;
    }
    
    public boolean isExpression() {
        return expression;
    }
    
    public String name() {
        return name;
    }
    
    public synchronized boolean hasSeenType(int typeCode) {
        return seenTypes.contains(typeCode);
    }
    
    public synchronized void addType(int typeCode) {
        seenTypes.add(typeCode);
    }
    
    public synchronized int seenTypesCount() {
        return seenTypes.size();
    }
    
    public synchronized void clearTypes() {
        seenTypes.clear();
        clearCount++;
    }
    
    public int clearCount() {
        return clearCount;
    }

    public long siteID() {
        return siteID;
    }

    public String file() {
        return file;
    }

    public int line() {
        return line;
    }

    public boolean boundOnce() {
        return boundOnce;
    }

    public void boundOnce(boolean boundOnce) {
        this.boundOnce = boundOnce;
    }

    @Override
    public void setTarget(MethodHandle target) {
        super.setTarget(target);
        boundOnce = true;
    }

    public void setInitialTarget(MethodHandle target) {
        super.setTarget(target);
    }
    
    /**
     * Get the actual incoming Signature for this call site.
     * 
     * This represents the actual argument list.
     * 
     * @return the actual Signature at the call site
     */
    public Signature signature() {
        return signature;
    }
    
    /**
     * Get the "full" signature equivalent to this call site.
     * 
     * The "full" signature always guarantees context, caller, and block args
     * are provided. It could also be considered the standard intermediate
     * signature all calls eventually pass through.
     * 
     * @return the "full" intermediate signature
     */
    public Signature fullSignature() {
        return fullSignature;
    }
    
    public static final Signature STANDARD_SITE_SIG = Signature
            .returning(IRubyObject.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("caller", IRubyObject.class)
            .appendArg("self", IRubyObject.class);
    public static final Signature STANDARD_SITE_SIG_1ARG = STANDARD_SITE_SIG.appendArg("arg0", IRubyObject.class);
    public static final Signature STANDARD_SITE_SIG_2ARG = STANDARD_SITE_SIG_1ARG.appendArg("arg1", IRubyObject.class);
    public static final Signature STANDARD_SITE_SIG_3ARG = STANDARD_SITE_SIG_2ARG.appendArg("arg2", IRubyObject.class);
    public static final Signature STANDARD_SITE_SIG_NARG = STANDARD_SITE_SIG.appendArg("args", IRubyObject[].class);
    
    public static final Signature[] STANDARD_SITE_SIGS = {
        STANDARD_SITE_SIG,
        STANDARD_SITE_SIG_1ARG,
        STANDARD_SITE_SIG_2ARG,
        STANDARD_SITE_SIG_3ARG,
        STANDARD_SITE_SIG_NARG,
    };
    
    public static final Signature STANDARD_SITE_SIG_BLOCK = STANDARD_SITE_SIG.appendArg("block", Block.class);
    public static final Signature STANDARD_SITE_SIG_1ARG_BLOCK = STANDARD_SITE_SIG_1ARG.appendArg("block", Block.class);
    public static final Signature STANDARD_SITE_SIG_2ARG_BLOCK = STANDARD_SITE_SIG_2ARG.appendArg("block", Block.class);
    public static final Signature STANDARD_SITE_SIG_3ARG_BLOCK = STANDARD_SITE_SIG_3ARG.appendArg("block", Block.class);
    public static final Signature STANDARD_SITE_SIG_NARG_BLOCK = STANDARD_SITE_SIG_NARG.appendArg("block", Block.class);
    public static final Signature[] STANDARD_SITE_SIGS_BLOCK = {
        STANDARD_SITE_SIG_BLOCK,
        STANDARD_SITE_SIG_1ARG_BLOCK,
        STANDARD_SITE_SIG_2ARG_BLOCK,
        STANDARD_SITE_SIG_3ARG_BLOCK,
        STANDARD_SITE_SIG_NARG_BLOCK,
    };
}
