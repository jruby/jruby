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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Block implemented using a Java-based BlockCallback implementation
 * rather than with an ICallable. For lightweight block logic within
 * Java code.
 */
public class CompiledBlock19 extends ContextAwareBlockBody {
    protected final CompiledBlockCallback19 callback;
    protected final boolean hasMultipleArgsHead;
    protected final String[] parameterList;
    
    public static Block newCompiledClosure(ThreadContext context, IRubyObject self, Arity arity,
            StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType) {
        Binding binding = context.currentBinding(self, Visibility.PUBLIC);
        BlockBody body = new CompiledBlock19(arity, scope, callback, hasMultipleArgsHead, argumentType, EMPTY_PARAMETER_LIST);

        return new Block(body, binding);
    }
    
    public static Block newCompiledClosure(ThreadContext context, IRubyObject self, BlockBody body) {
        Binding binding = context.currentBinding(self, Visibility.PUBLIC);
        return new Block(body, binding);
    }
    
    public static BlockBody newCompiledBlock(Arity arity,
            StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType, String[] parameterList) {
        return new CompiledBlock19(arity, scope, callback, hasMultipleArgsHead, argumentType, parameterList);
    }

    protected CompiledBlock19(Arity arity, StaticScope scope, CompiledBlockCallback19 callback, boolean hasMultipleArgsHead, int argumentType, String[] parameterList) {
        super(scope, arity, argumentType);
        
        this.callback = callback;
        this.hasMultipleArgsHead = hasMultipleArgsHead;
        this.parameterList = parameterList;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        IRubyObject value = args.length == 1 ? args[0] : context.getRuntime().newArrayNoCopy(args);

        return yield(context, value, null, null, true, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type, Block block) {
        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, true, binding, type, block);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, IRubyObject.NULL_ARRAY, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return yield(context, arg0, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, new IRubyObject[] {arg0, arg1}, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yieldSpecificInternal(context, new IRubyObject[] {arg0, arg1, arg2}, binding, type);
    }

    private IRubyObject yieldSpecificInternal(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        IRubyObject self = prepareSelf(binding);

        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);

        try {
            return callback.call(context, self, args, Block.NULL_BLOCK);
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        IRubyObject self = prepareSelf(binding);

        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);
        
        try {
            IRubyObject[] realArgs = setupBlockArg(context.getRuntime(), value, self);
            return callback.call(context, self, realArgs, Block.NULL_BLOCK);
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    public IRubyObject yield(ThreadContext context, IRubyObject args, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Block.Type type) {
        return yield(context, args, self, klass, aValue, binding, type, Block.NULL_BLOCK);
    }
    
    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject args, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Block.Type type, Block block) {
        if (klass == null) {
            self = prepareSelf(binding);
        }

        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, klass, binding);
        
        try {
            IRubyObject[] realArgs = setupBlockArgs(args, aValue);
            return callback.call(context, self, realArgs, block);
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }
    
    private IRubyObject prepareSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);
        
        return self;
    }

    private IRubyObject handleNextJump(ThreadContext context, JumpException.NextJump nj, Block.Type type) {
        return nj.getValue() == null ? context.getRuntime().getNil() : (IRubyObject)nj.getValue();
    }

    private IRubyObject[] setupBlockArgs(IRubyObject value, boolean alreadyArray) {
        Arity arity = arity();
        int requiredCount = arity.required();
        boolean isRest = !arity.isFixed();
        
        IRubyObject[] parameters;
        if (value == null) {
            parameters = IRubyObject.NULL_ARRAY;
        } else if (value instanceof RubyArray && (alreadyArray || (isRest && requiredCount > 0))) {
            parameters = ((RubyArray) value).toJavaArray();
        } else {
            parameters = new IRubyObject[] { value };
        }

        return parameters;
    }

    protected IRubyObject[] setupBlockArg(Ruby ruby, IRubyObject value, IRubyObject self) {
        Arity arity = arity();
        int requiredCount = arity.required();
        boolean isRest = !arity.isFixed();

        IRubyObject[] parameters;
        if (value == null) {
            parameters = IRubyObject.NULL_ARRAY;
        } else if (value instanceof RubyArray && ((isRest && requiredCount > 0) || (!isRest && requiredCount > 1))) {
            parameters = ((RubyArray) value).toJavaArray();
        } else {
            parameters = new IRubyObject[] { value };
        }
        return parameters;
    }

    public String getFile() {
        return callback.getFile();
    }

    public int getLine() {
        return callback.getLine();
    }

    public String[] getParameterList() {
        return parameterList;
    }
}
