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
package org.jruby.compiler.impl;

import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyRegexp;
import org.jruby.RubySymbol;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.util.ByteList;
import static org.jruby.util.CodegenUtils.*;

public class InvokeDynamicCacheCompiler extends InheritedCacheCompiler {
    public InvokeDynamicCacheCompiler(StandardASMCompiler scriptCompiler) {
        super(scriptCompiler);
    }

    /**
     * Cache a constant reference using invokedynamic.
     * 
     * This cache uses a java.lang.invoke.SwitchPoint as the invalidation
     * mechanism in order to avoid the cost of constantly pinging a constant
     * generation in org.jruby.Ruby. This allows a nearly free constant cache.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param constantName the name of the constant to look up
     */
    @Override
    public void cacheConstant(BaseBodyCompiler method, String constantName) {
        if (!RubyInstanceConfig.INVOKEDYNAMIC_CONSTANTS) {
            super.cacheConstant(method, constantName);
            return;
        }
        
        method.loadThreadContext();
        method.method.invokedynamic(
                constantName,
                sig(IRubyObject.class, ThreadContext.class),
                InvokeDynamicSupport.getConstantHandle());
    }

    /**
     * This doesn't get used, since it's only used from cacheRegexp in superclass,
     * and that is completely bound via invokedynamic now. Implemented here and in
     * InvokeDynamicSupport for consistency.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param contents the contents of the bytelist to cache
     */
    @Override
    public void cacheByteList(BaseBodyCompiler method, ByteList contents) {
        String asString = RuntimeHelpers.rawBytesToString(contents.bytes());
        String encodingName = new String(contents.getEncoding().getName());
        
        method.method.invokedynamic(
                "getByteList",
                sig(ByteList.class),
                InvokeDynamicSupport.getByteListHandle(),
                asString,
                encodingName);
    }

    /**
     * Cache a Regexp literal using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param pattern the contents of the bytelist for the regexp pattern
     * @param options the regexp options
     */
    @Override
    public void cacheRegexp(BaseBodyCompiler method, ByteList pattern, int options) {
        String asString = RuntimeHelpers.rawBytesToString(pattern.bytes());
        String encodingName = new String(pattern.getEncoding().getName());
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getRegexp",
                sig(RubyRegexp.class, ThreadContext.class),
                InvokeDynamicSupport.getRegexpHandle(),
                asString,
                encodingName,
                options);
    }

    /**
     * Cache a Fixnum literal using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param value the value of the Fixnum
     */
    public void cacheFixnum(BaseBodyCompiler method, long value) {
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getFixnum",
                sig(RubyFixnum.class, ThreadContext.class),
                InvokeDynamicSupport.getFixnumHandle(),
                value);
    }

    /**
     * Cache a Float literal using invokedynamic.
     * 
     * @param method the method compiler with which bytecode is emitted
     * @param value the value of the Float
     */
    public void cacheFloat(BaseBodyCompiler method, double value) {
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getFloat",
                sig(RubyFloat.class, ThreadContext.class),
                InvokeDynamicSupport.getFloatHandle(),
                value);
    }

    public void cacheStaticScope(BaseBodyCompiler method, StaticScope scope) {
        String scopeString = RuntimeHelpers.encodeScope(scope);
        
        method.loadThreadContext();
        
        method.method.invokedynamic(
                "getStaticScope",
                sig(StaticScope.class, ThreadContext.class),
                InvokeDynamicSupport.getStaticScopeHandle(),
                scopeString);
    }
    
    public void cacheCallSite(BaseBodyCompiler method, String name, CallType callType) {
        char callTypeChar = 0;
        
        switch (callType) {
            case NORMAL:
                callTypeChar = 'N';
                break;
            case FUNCTIONAL:
                callTypeChar = 'F';
                break;
            case VARIABLE:
                callTypeChar = 'V';
                break;
            case SUPER:
                callTypeChar = 'S';
                break;
        }
        
        method.method.invokedynamic(
                "getCallSite",
                sig(CallSite.class),
                InvokeDynamicSupport.getCallSiteHandle(),
                name,
                callTypeChar);
    }
}
