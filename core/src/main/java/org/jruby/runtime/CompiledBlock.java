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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A Block implemented using a Java-based BlockCallback implementation
 * rather than with an ICallable. For lightweight block logic within
 * Java code.
 */
public class CompiledBlock extends ContextAwareBlockBody {
    protected final CompiledBlockCallback callback;
    protected final boolean hasMultipleArgsHead;
    
    public static Block newCompiledClosure(ThreadContext context, IRubyObject self, Arity arity,
            StaticScope scope, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        Binding binding = context.currentBinding(self, Visibility.PUBLIC);
        BlockBody body = new CompiledBlock(arity, scope, callback, hasMultipleArgsHead, argumentType);

        return new Block(body, binding);
    }
    
    public static Block newCompiledClosure(ThreadContext context, IRubyObject self, BlockBody body) {
        Binding binding = context.currentBinding(self, Visibility.PUBLIC);
        return new Block(body, binding);
    }
    
    public static BlockBody newCompiledBlock(Arity arity,
            StaticScope scope, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        return new CompiledBlock(arity, scope, callback, hasMultipleArgsHead, argumentType);
    }

    public CompiledBlock(Arity arity, StaticScope scope, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argumentType) {
        super(scope, arity, argumentType);
        
        this.callback = callback;
        this.hasMultipleArgsHead = hasMultipleArgsHead;
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yield(context, null, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return yield(context, arg0, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yield(context, context.runtime.newArrayNoCopyLight(arg0, arg1), null, null, true, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yield(context, context.runtime.newArrayNoCopyLight(arg0, arg1, arg2), null, null, true, binding, type);
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        return yield(context, value, binding, type, Block.NULL_BLOCK);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject args, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Block.Type type) {
        return yield(context, args, self, klass, aValue, binding, type, Block.NULL_BLOCK);
    }

    // FIXME: These two duplicate overrides should go away
    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type, Block block) {
        IRubyObject self = prepareSelf(binding);

        IRubyObject realArg = setupBlockArg(context.runtime, value, self);
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);

        try {
            return callback.call(context, self, realArg, block);
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    @Override
    public IRubyObject yield(ThreadContext context, IRubyObject args, IRubyObject self, RubyModule klass, boolean aValue, Binding binding, Block.Type type, Block block) {
        if (klass == null) {
            self = prepareSelf(binding);
        }

        IRubyObject realArg = aValue ?
                setupBlockArgs(context, args, self) : setupBlockArg(context.runtime, args, self);
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, klass, binding);

        try {
            return callback.call(context, self, realArg, block);
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
        return nj.getValue() == null ? context.runtime.getNil() : (IRubyObject)nj.getValue();
    }

    protected IRubyObject setupBlockArgs(ThreadContext context, IRubyObject value, IRubyObject self) {
        switch (argumentType) {
        case ZERO_ARGS:
            return null;
        case MULTIPLE_ASSIGNMENT:
        case SINGLE_RESTARG:
            return value;
        default:
            return defaultArgsLogic(context.runtime, value);
        }
    }
    
    private IRubyObject defaultArgsLogic(Ruby ruby, IRubyObject value) {
        int length = ArgsUtil.arrayLength(value);
        switch (length) {
        case 0:
            return ruby.getNil();
        case 1:
            return ((RubyArray)value).eltInternal(0);
        default:
            blockArgWarning(ruby, length);
        }
        return value;
    }
    
    private void blockArgWarning(Ruby ruby, int length) {
        ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" +
                    length + " for 1)");
    }

    protected IRubyObject setupBlockArg(Ruby ruby, IRubyObject value, IRubyObject self) {
        switch (argumentType) {
        case ZERO_ARGS:
            return null;
        case MULTIPLE_ASSIGNMENT:
        case SINGLE_RESTARG:
            return ArgsUtil.convertToRubyArray(ruby, value, hasMultipleArgsHead);
        default:
            return defaultArgLogic(ruby, value);
        }
    }
    
    private IRubyObject defaultArgLogic(Ruby ruby, IRubyObject value) {
        if (value == null) {
            return warnMultiReturnNil(ruby);
        }
        return value;
    }

    public String getFile() {
        return callback.getFile();
    }

    public int getLine() {
        return callback.getLine();
    }

    private IRubyObject warnMultiReturnNil(Ruby ruby) {
        ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (0 for 1)");
        return ruby.getNil();
    }
}
