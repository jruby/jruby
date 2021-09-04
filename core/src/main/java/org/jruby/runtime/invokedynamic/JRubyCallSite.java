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
import java.util.concurrent.atomic.AtomicLong;

import org.jruby.RubyClass;
import org.jruby.runtime.Block;

import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

public class JRubyCallSite extends MutableCallSite {

    public static final AtomicLong SITE_ID = new AtomicLong(1);

    final long siteID = SITE_ID.getAndIncrement();
    final CallType callType;
    final String name;
    private final String file;
    private final int line;
    private final Signature signature;
    private final Signature fullSignature;
    private final int arity;

    CacheEntry entry = CacheEntry.NULL_CACHE;

    JRubyCallSite(Lookup lookup, MethodType type, CallType callType, String file, int line, String name) {
        super(type);

        this.name = name;
        this.callType = callType;

        Signature startSig;
        int argOffset;

        if (callType == CallType.SUPER) {
            // super calls receive current class argument, so offsets and signature are different
            startSig = JRubyCallSite.STANDARD_SUPER_SIG;
            argOffset = 4;
        } else {
            startSig = JRubyCallSite.STANDARD_SITE_SIG;
            argOffset = 3;
        }

        int arity;
        if (type.parameterType(type.parameterCount() - 1) == Block.class) {
            arity = type.parameterCount() - (argOffset + 1);

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            startSig = startSig.appendArg("block", Block.class);
            fullSignature = signature = startSig;
        } else {
            arity = type.parameterCount() - argOffset;

            if (arity == 1 && type.parameterType(argOffset) == IRubyObject[].class) {
                arity = -1;
                startSig = startSig.appendArg("args", IRubyObject[].class);
            } else {
                for (int i = 0; i < arity; i++) {
                    startSig = startSig.appendArg("arg" + i, IRubyObject.class);
                }
            }
            signature = startSig;
            fullSignature = startSig.appendArg("block", Block.class);
        }

        this.arity = arity;

        this.file = file;
        this.line = line;
    }

    public int arity() {
        return arity;
    }

    public CallType callType() {
        return callType;
    }

    public String file() {
        return file;
    }

    public int line() {
        return line;
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

    public static final Signature STANDARD_SUPER_SIG = Signature
            .returning(IRubyObject.class)
            .appendArg("context", ThreadContext.class)
            .appendArg("caller", IRubyObject.class)
            .appendArg("self", IRubyObject.class)
            .appendArg("class", RubyClass.class);
}
