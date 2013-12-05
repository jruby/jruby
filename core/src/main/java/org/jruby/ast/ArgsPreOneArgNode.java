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
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class ArgsPreOneArgNode extends ArgsNode {
    public ArgsPreOneArgNode(ISourcePosition position, ListNode pre) {
        super(position, pre, null, null, null, null);
    }

    @Override
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject[] args, Block block) {
        super.prepare(context, runtime, self, args, block);
    }

    @Override
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, Block block) {
        context.getCurrentScope().setArgValues(arg0);
    }

    @Override
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0,
            IRubyObject arg1, Block block) {
        throw runtime.newArgumentError(2, 1);
    }

    @Override
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0,
            IRubyObject arg1, IRubyObject arg2, Block block) {
        throw runtime.newArgumentError(3, 1);
    }

    @Override
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0,
            IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        throw runtime.newArgumentError(4, 1);
    }
}
