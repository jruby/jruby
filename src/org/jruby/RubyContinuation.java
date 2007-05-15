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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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

import org.jruby.exceptions.JumpException;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Placeholder until/if we can support this
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyContinuation {
    public static void createContinuation(Ruby runtime) {
        RubyModule cContinuation = runtime.defineClass("Continuation",runtime.getObject(),runtime.getObject().getAllocator());
        CallbackFactory cf = runtime.callbackFactory(RubyContinuation.class);
        cContinuation.defineMethod("call", cf.getOptSingletonMethod("call"));
        cContinuation.defineAlias("[]","call");
    }

    public static IRubyObject call(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        recv.getRuntime().getWarnings().warn("Continuation.call: Continuations are not implemented in JRuby and will not work");
        ThreadContext context = recv.getRuntime().getCurrentContext();
        if(args.length == 1) {
            throw context.prepareJumpException(JumpException.JumpType.BreakJump, recv.dataGetStruct(), args[0]);
        } else {
            throw context.prepareJumpException(JumpException.JumpType.BreakJump, recv.dataGetStruct(), recv.getRuntime().newArray(args));
        }
    }
}// RubyContinuation
