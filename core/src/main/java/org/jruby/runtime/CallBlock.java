/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

import org.jruby.RubyModule;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;

/**
 * A Block implemented using a Java-based BlockCallback implementation. For
 * lightweight block logic within Java code.
 */
public class CallBlock extends BlockBody {
    private final BlockCallback callback;
    private final StaticScope dummyScope;

    public static Block newCallClosure(IRubyObject self, RubyModule imClass, Signature signature, BlockCallback callback, ThreadContext context) {
        return newCallClosure(context, self, signature, callback);
    }

    public static Block newCallClosure(ThreadContext context, IRubyObject self, Signature signature, BlockCallback callback) {
        Binding binding = context.currentBinding(self, Visibility.PUBLIC);
        BlockBody body = new CallBlock(context, signature, callback);

        return new Block(body, binding);
    }

    // Put back because fishwife 1.10.1 still relies on this.
    @Deprecated(since = "9.3.3.0")
    public static Block newCallClosure(IRubyObject self, RubyModule imClass, Arity arity, BlockCallback callback, ThreadContext context) {
        return newCallClosure(self, imClass, Signature.from(arity), callback, context);
    }

    private CallBlock(ThreadContext context, Signature signature, BlockCallback callback) {
        super(signature);
        this.callback = callback;
        this.dummyScope = context.runtime.getStaticScopeFactory().getDummyScope();
    }

    static IRubyObject[] adjustArgs(Block block, IRubyObject[] args) {
        Signature signature = block.getSignature();
        int required = signature.required();
        if (required > 0 && required < args.length && signature.isFixed()) args = ArraySupport.newCopy(args, required);

        return args;
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args) {
        return callback.call(context, adjustArgs(block, args), Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, Block block, IRubyObject[] args, Block blockArg) {
        return callback.call(context, adjustArgs(block, args), blockArg);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block) {
        return callback.call(context, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Block block, IRubyObject arg0) {
        return callback.call(context, arg0, Block.NULL_BLOCK);
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
        return callback.call(context, value, Block.NULL_BLOCK);
    }

    @Override
    protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
        return callback.call(context, adjustArgs(block, args));
    }

    public StaticScope getStaticScope() {
        return dummyScope;
    }

    public void setStaticScope(StaticScope newScope) {
        // ignore
    }

    public String getFile() {
        return "(internal)";
    }

    public int getLine() {
        return -1;
    }
}
