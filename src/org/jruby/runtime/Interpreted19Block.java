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
import org.jruby.RubyModule;
import org.jruby.ast.ArgsNoArgNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.Node;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.builtin.IRubyObject;
/**
 *
 * @author enebo
 */
public class Interpreted19Block  extends ContextAwareBlockBody {
    private static final boolean ALREADY_ARRAY = true;

    /** The position for the block */
    private final ISourcePosition position;

    /** Filename from position */
    private final String file;

    /** Line from position */
    private final int line;

    /** The argument list, pulled out of iterNode */
    private final ArgsNode args;

    /**
     * Whether the arguments "need splat".
     *
     * @see RuntimeHelpers#needsSplat19(int, boolean)
     */
    private final boolean needsSplat;

    /** The parameter names, for Proc#parameters */
    private final String[] parameterList;

    /** The body of the block, pulled out of bodyNode */
    private final Node body;

    public static Block newInterpretedClosure(ThreadContext context, BlockBody body, IRubyObject self) {
        Binding binding = context.currentBinding(self);
        return new Block(body, binding);
    }

    // ENEBO: Some of this logic should be put back into the Nodes themselves, but the more
    // esoteric features of 1.9 make this difficult to know how to do this yet.
    public static BlockBody newBlockBody(IterNode iter) {
        if (iter instanceof LambdaNode) {
            return new Interpreted19Block((LambdaNode) iter);
        } else {
            return new Interpreted19Block(iter);
        }

    }

    public Interpreted19Block(IterNode iterNode) {
        super(iterNode.getScope(), ((ArgsNode)iterNode.getVarNode()).getArity(), -1); // We override that the logic which uses this

        this.args = (ArgsNode)iterNode.getVarNode();
        this.needsSplat = RuntimeHelpers.needsSplat19(args.getRequiredArgsCount(), args.getRestArg() != -1);
        this.parameterList = RuntimeHelpers.encodeParameterList(args).split(";");
        this.body = iterNode.getBodyNode() == null ? NilImplicitNode.NIL : iterNode.getBodyNode();
        this.position = iterNode.getPosition();

        // precache these
        this.file = position.getFile();
        this.line = position.getLine();
    }

    public Interpreted19Block(LambdaNode lambdaNode) {
        super(lambdaNode.getScope(), lambdaNode.getArgs().getArity(), -1); // We override that the logic which uses this

        this.args = lambdaNode.getArgs();
        this.needsSplat = RuntimeHelpers.needsSplat19(args.getRequiredArgsCount(), args.getRestArg() != -1);
        this.parameterList = RuntimeHelpers.encodeParameterList(args).split(";");
        this.body = lambdaNode.getBody() == null ? NilImplicitNode.NIL : lambdaNode.getBody();
        this.position = lambdaNode.getPosition();

        // precache these
        this.file = position.getFile();
        this.line = position.getLine();
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

            return evalBlockBody(context, binding, self);
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
    @Override
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
            return evalBlockBody(context, binding, self);
        } catch (JumpException.NextJump nj) {
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    private IRubyObject evalBlockBody(ThreadContext context, Binding binding, IRubyObject self) {
        // This while loop is for restarting the block call in case a 'redo' fires.
        while (true) {
            try {
                return ASTInterpreter.INTERPRET_BLOCK(context.getRuntime(), context, file, line, body, binding.getMethod(), self, Block.NULL_BLOCK);
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

    private void setupBlockArg(ThreadContext context, IRubyObject value, IRubyObject self, Block block, Block.Type type) {
        setupBlockArgs(context, value, self, block, type, false);
    }

    /**
     * @see RuntimeHelpers#restructureBlockArgs19(IRubyObject, boolean, boolean)
     */
    private void setupBlockArgs(ThreadContext context, IRubyObject value, IRubyObject self, Block block, Block.Type type, boolean alreadyArray) {
        IRubyObject[] parameters = RuntimeHelpers.restructureBlockArgs19(value, needsSplat, alreadyArray);

        Ruby runtime = context.getRuntime();        
        if (type == Block.Type.LAMBDA) args.checkArgCount(runtime, parameters.length);        
        if (!(args instanceof ArgsNoArgNode)) args.prepare(context, runtime, self, parameters, block);
    }

    public ArgsNode getArgs() {
        return args;
    }
    
    public Node getBody() {
        return body;
    }

    public String getFile() {
        return position.getFile();
    }

    public int getLine() {
        return position.getLine();
    }

    @Override
    public String[] getParameterList() {
        return parameterList;
    }
}
