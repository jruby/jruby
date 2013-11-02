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
import org.jruby.RubyProc;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Block implemented using a Java-based BlockCallback implementation. For
 * lightweight block logic within Java code.
 */
public class CallBlock extends BlockBody {
    private final Arity arity;
    private final BlockCallback callback;
    private final StaticScope dummyScope;
    
    public static Block newCallClosure(IRubyObject self, RubyModule imClass, Arity arity, BlockCallback callback, ThreadContext context) {
        Binding binding = context.currentBinding(self, Visibility.PUBLIC);
        BlockBody body = new CallBlock(arity, callback, context);
        
        return new Block(body, binding);
    }

    private CallBlock(Arity arity, BlockCallback callback, ThreadContext context) {
        super(BlockBody.SINGLE_RESTARG);
        this.arity = arity;
        this.callback = callback;
        this.dummyScope = context.runtime.getStaticScopeFactory().getDummyScope();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        return callback.call(context, args, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding,
            Block.Type type, Block block) {
        return callback.call(context, args, block);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return callback.call(context, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return callback.call(context, new IRubyObject[]{arg0}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        return callback.call(context, new IRubyObject[]{value}, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject[] args, IRubyObject self,
            RubyModule klass, boolean aValue, Binding binding, Block.Type type) {
        IRubyObject[] preppedArgs = RubyProc.prepareProcArgs(context, arity(), args);
        return callback.call(context, preppedArgs, Block.NULL_BLOCK);
    }
    
    public StaticScope getStaticScope() {
        return dummyScope;
    }

    public void setStaticScope(StaticScope newScope) {
        // ignore
    }

    public Block cloneBlock(Binding binding) {
        return new Block(this, binding.clone());
    }

    public Arity arity() {
        return arity;
    }

    public String getFile() {
        return "(internal)";
    }

    public int getLine() {
        return -1;
    }
}
