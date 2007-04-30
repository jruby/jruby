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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

import org.jruby.RubyModule;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

/**
 * A Block implemented using a Java-based BlockCallback implementation
 * rather than with an ICallable. For lightweight block logic within
 * Java code.
 */
public class CompiledBlock extends Block {
    private CompiledBlockCallback callback;

    public CompiledBlock(ThreadContext context, IRubyObject self, Arity arity, DynamicScope dynamicScope, CompiledBlockCallback callback) {
        this(self,
                context.getCurrentFrame(),
                context.peekCRef(),
                Visibility.PUBLIC,
                context.getRubyClass(),
                dynamicScope, arity, callback);
    }

    private CompiledBlock(IRubyObject self, Frame frame,
        SinglyLinkedList cref, Visibility visibility, RubyModule klass,
        DynamicScope dynamicScope, Arity arity, CompiledBlockCallback callback) {
        super(null, self, frame, cref, visibility, klass, dynamicScope);
        this.arity = arity;
        this.callback = callback;
    }
    
    protected void pre(ThreadContext context, RubyModule klass) {
        context.preYieldSpecificBlock(this, klass);
    }
    
    protected void post(ThreadContext context) {
        context.postYield();
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, true);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject args, IRubyObject self, RubyModule klass, boolean aValue) {
        if (klass == null) {
            self = this.self;
            frame.setSelf(self);
        }
        IRubyObject[] realArgs = null;
        if (!aValue) {
            // handle as though it's just an array coming in...i.e. it should be multiassigned or just assigned as is to var 0.
            // FIXME for now, since masgn isn't supported, this just wraps args in an IRubyObject[], since single vars will want that anyway
            realArgs = new IRubyObject[] {args};
        } else {
            realArgs = ArgsUtil.convertToJavaArray(args);
        }
        try {
            pre(context, klass);
            return callback.call(context, self, realArgs);
        } finally {
            post(context);
        }
    }

    public Block cloneBlock() {
        return new CompiledBlock(self, frame.duplicate(), cref, visibility, klass, 
                dynamicScope.cloneScope(), arity, callback);
    }

    public Arity arity() {
        return arity;
    }
}
