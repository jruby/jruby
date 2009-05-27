
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

import org.jruby.compiler.ArgumentsCallback;
import org.jruby.compiler.CompilerCallback;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.util.CodegenUtils.*;
import org.objectweb.asm.Label;

/**
 *
 * @author headius
 */
public class InvokeDynamicInvocationCompiler extends StandardInvocationCompiler {
    public InvokeDynamicInvocationCompiler(BaseBodyCompiler methodCompiler, SkinnyMethodAdapter method) {
        super(methodCompiler, method);

        // HACK: force clinit to be created
        methodCompiler.getScriptCompiler().getClassInitMethod();
    }

    @Override
    public void invokeDynamic(String name, CompilerCallback receiverCallback, ArgumentsCallback argsCallback, CallType callType, CompilerCallback closureArg, boolean iterator) {
        if (callType == CallType.SUPER) {
            super.invokeDynamic(name, receiverCallback, argsCallback, callType, closureArg, iterator);
            return;
        }
        
        methodCompiler.loadThreadContext(); // [adapter, tc]
        
        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        methodCompiler.loadSelf();

        if (receiverCallback != null) {
            receiverCallback.call(methodCompiler);
        } else {
            methodCompiler.loadSelf();
        }

        methodCompiler.method.ldc(name);

        String invokeName = callType == CallType.NORMAL ? "call" : "fcall";
        String signature;

        // args
        if (argsCallback == null) {
            // block
            if (closureArg == null) {
                // no args, no block
                signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
            } else {
                // no args, with block
                closureArg.call(methodCompiler);
                signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
            }
        } else {
            argsCallback.call(methodCompiler);
            // block
            if (closureArg == null) {
                // with args, no block
                switch (argsCallback.getArity()) {
                case 1:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
                    break;
                case 2:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
                    break;
                case 3:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
                    break;
                default:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
                }
            } else {
                // with args, with block
                closureArg.call(methodCompiler);
                
                switch (argsCallback.getArity()) {
                case 1:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
                    break;
                case 2:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                case 3:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
                    break;
                default:
                    signature = sig(IRubyObject.class, params(ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
                }
            }
        }
        
        // adapter, tc, recv, args{0,1}, block{0,1}]
        method.invokedynamic(p(Object.class), invokeName, signature);
    }
}
