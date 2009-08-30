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
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.Node;
import org.jruby.ast.ArgsNoArgNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
/**
 *
 * @author enebo
 */
public class Interpreted19Block  extends BlockBody {
    private static final boolean ALREADY_ARRAY = true;

    /** The argument list, pulled out of iterNode */
    private final ArgsNode args;

    /** The body of the block, pulled out of bodyNode */
    private final Node body;

    /** The static scope for the block body */
    private StaticScope scope;

    /** The arity of the block */
    private final Arity arity;

    public static Block newInterpretedClosure(ThreadContext context, BlockBody body, IRubyObject self) {
        Binding binding = context.currentBinding(self);
        return new Block(body, binding);
    }

    public Interpreted19Block(IterNode iter) {
        super(-1); // We override that the logic which uses this

        if (iter instanceof LambdaNode) {
            LambdaNode lambda = (LambdaNode) iter;
            
            this.arity = lambda.getArgs().getArity();
            this.args = lambda.getArgs();
            this.body = lambda.getBody() == null ? NilImplicitNode.NIL : lambda.getBody();
            this.scope = lambda.getScope();
        } else {
            ArgsNode argsNode = (ArgsNode) iter.getVarNode();

            this.arity = argsNode.getArity();
            this.args = argsNode;
            this.body = iter.getBodyNode() == null ? NilImplicitNode.NIL : iter.getBodyNode();
            this.scope = iter.getScope();
        }
    }

    protected Frame pre(ThreadContext context, RubyModule klass, Binding binding) {
        return context.preYieldSpecificBlock(binding, scope, klass);
    }

    protected void post(ThreadContext context, Binding binding, Visibility vis, Frame lastFrame) {
        binding.getFrame().setVisibility(vis);
        context.postYield(binding, lastFrame);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type) {
        IRubyObject value = args.length == 1 ? args[0] : context.getRuntime().newArrayNoCopy(args);

        return yield(context, value, null, null, ALREADY_ARRAY, binding, type, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Binding binding, Block.Type type, Block block) {
        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, ALREADY_ARRAY, binding, type, block);
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
        return yield(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1), null, null, ALREADY_ARRAY, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yield(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1, arg2), null, null, ALREADY_ARRAY, binding, type);
    }

    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        IRubyObject self = prepareSelf(binding);

        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);

        try {
            setupBlockArg(context, value, self, Block.NULL_BLOCK, type);

            return evalBlockBody(context, self);
        } catch (JumpException.NextJump nj) {
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    /**
     * Yield to this block, usually passed to the current call.
     *
     * @param context represents the current thread-specific data
     * @param value The value to yield, either a single value or an array of values
     * @param self The current self
     * @param klass
     * @param aValue Should value be arrayified or not?
     * @return
     */
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self,
            RubyModule klass, boolean aValue, Binding binding, Block.Type type) {
        return yield(context, value, self, klass, aValue, binding, type, Block.NULL_BLOCK);

    }
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self,
            RubyModule klass, boolean aValue, Binding binding, Block.Type type, Block block) {
        if (klass == null) {
            self = prepareSelf(binding);
        }

        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, klass, binding);

        try {
            setupBlockArgs(context, value, self, block, type, aValue);

            // This while loop is for restarting the block call in case a 'redo' fires.
            return evalBlockBody(context, self);
        } catch (JumpException.NextJump nj) {
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    private IRubyObject evalBlockBody(ThreadContext context, IRubyObject self) {
        // This while loop is for restarting the block call in case a 'redo' fires.
        while (true) {
            try {
                return body.interpret(context.getRuntime(), context, self, Block.NULL_BLOCK);
            } catch (JumpException.RedoJump rj) {
                context.pollThreadEvents();
                // do nothing, allow loop to redo
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep", soe);
            }
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

    private IRubyObject convertIfAlreadyArray(ThreadContext context, IRubyObject value) {
        int length = ArgsUtil.arrayLength(value);
        switch (length) {
        case 0:
            value = context.getRuntime().getNil();
            break;
        case 1:
            value = ((RubyArray)value).eltInternal(0);
            break;
        default:
            context.getRuntime().getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + length + " for 1)");
        }

        return value;
    }

    private void setupBlockArg(ThreadContext context, IRubyObject value, IRubyObject self, Block block, Block.Type type) {
//        System.out.println("AA: (" + value + ")");
        
        int requiredCount = args.getRequiredArgsCount();
        boolean isRest = args.getRestArg() != -1;

        IRubyObject[] parameters;
        if (value == null) {
            parameters = IRubyObject.NULL_ARRAY;
        } else if (value instanceof RubyArray && ((isRest && requiredCount > 0) || (!isRest && requiredCount > 1))) {
            parameters = ((RubyArray) value).toJavaArray();
        } else {
            parameters = new IRubyObject[] { value };
        }

        if (!(args instanceof ArgsNoArgNode)) {
            Ruby runtime = context.getRuntime();

            // FIXME: This needs to happen for lambdas
//            args.checkArgCount(runtime, parameters.length);
            args.prepare(context, runtime, self, parameters, block);
        }
    }
    // . Array given to rest should pass itself
    // . Array with rest + other args should extract array
    // . Array with multiple values and NO rest should extract args if there are more than one argument

    private void setupBlockArgs(ThreadContext context, IRubyObject value, IRubyObject self, Block block, Block.Type type, boolean alreadyArray) {
//        System.out.println("AS: " + alreadyArray + "(" + value + ")");

        int requiredCount = args.getRequiredArgsCount();
        boolean isRest = args.getRestArg() != -1;

        IRubyObject[] parameters;
        if (value == null) {
            parameters = IRubyObject.NULL_ARRAY;
        } else if (value instanceof RubyArray && (alreadyArray || (isRest && requiredCount > 0))) {
            parameters = ((RubyArray) value).toJavaArray();
        } else {
            parameters = new IRubyObject[] { value };
        }

        if (!(args instanceof ArgsNoArgNode)) {
            Ruby runtime = context.getRuntime();

            // FIXME: This needs to happen for lambdas
//            args.checkArgCount(runtime, parameters.length);
            args.prepare(context, runtime, self, parameters, block);
        }
    }

    public Block cloneBlock(Binding binding) {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        binding = binding.clone();

        return new Block(this, binding);
    }

    public ArgsNode getArgs() {
        return args;
    }
    
    public Node getBody() {
        return body;
    }

    public StaticScope getStaticScope() {
        return scope;
    }

    public void setStaticScope(StaticScope newScope) {
        this.scope = newScope;
    }

    public Arity arity() {
        return arity;
    }
}
