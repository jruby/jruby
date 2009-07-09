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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.ZeroArgNode;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.assigner.Assigner;
import org.jruby.runtime.assigner.Pre0Rest0Post0Assigner;
import org.jruby.runtime.assigner.Pre0Rest1Post0Assigner;
import org.jruby.runtime.assigner.Pre1ExpandedRest0Post0Assigner;
import org.jruby.runtime.assigner.Pre1Rest0Post0Assigner;
import org.jruby.runtime.assigner.Pre1Rest1Post0Assigner;
import org.jruby.runtime.assigner.Pre2Rest0Post0Assigner;
import org.jruby.runtime.assigner.Pre2Rest1Post0Assigner;
import org.jruby.runtime.assigner.Pre3Rest0Post0Assigner;
import org.jruby.runtime.assigner.Pre3Rest1Post0Assigner;
import org.jruby.runtime.assigner.PreManyRest0Post0Assigner;
import org.jruby.runtime.assigner.PreManyRest1Post0Assigner;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This branch of the BlockBody hierarchy represents an interpreted block that
 * passes its AST nodes to the interpreter. It forms the top of the hierarchy
 * of interpreted blocks. In a typical application, it is the most heavily
 * consumed type of block.
 * 
 * @see SharedScopeBlock, CompiledBlock
 */
public class InterpretedBlock extends BlockBody {
    /** This block has no arguments at all (simple secondary optimization @see assignerFor for an
     * explanation).
     */
    private boolean noargblock;

    /** The body of the block, pulled out of bodyNode */
    private final Node bodyNode;
    
    /** The static scope for the block body */
    private StaticScope scope;
    
    /** The arity of the block */
    private final Arity arity;

    /** Logic for assigning the blocks local variables */
    protected Assigner assigner;

    public static Block newInterpretedClosure(ThreadContext context, IterNode iterNode, IRubyObject self) {
        Binding binding = context.currentBinding(self);
        NodeType argsNodeId = getArgumentTypeWackyHack(iterNode);

        BlockBody body = new InterpretedBlock(
                iterNode,
                Arity.procArityOf(iterNode.getVarNode()),
                asArgumentType(argsNodeId));
        return new Block(body, binding);
    }

    public static Block newInterpretedClosure(ThreadContext context, BlockBody body, IRubyObject self) {
        Binding binding = context.currentBinding(self);
        return new Block(body, binding);
    }

    public static BlockBody newBlockBody(IterNode iter, Arity arity, int argumentType) {
        return new InterpretedBlock(iter, arity, argumentType);
    }

    /*
     * Determine what sort of assigner should be used for the provided 'iter' (e.g. block).
     * Assigner provides just the right logic for assigning values to local parameters of the
     * block.
     *
     * This method also has a second optimization which is to set 'noargblock' in the case that
     * the block is a block which accepts no arguments.  The primary reason for this second
     * optimization is that in the case of a yield with a RubyArray we will bypass some logic
     * processing the RubyArray into a proper form (only to then not do anythign with it).  A
     * secondary benefit is that a simple boolean seems to optimize by hotspot much faster
     * than the zero arg assigner.
     */
    private void assignerFor(IterNode iter) {
        Node varNode = iter.getVarNode();

        if (varNode == null || varNode instanceof ZeroArgNode) { // No argument blocks
            assigner = new Pre0Rest0Post0Assigner();
            noargblock = true;
        } else if (varNode instanceof MultipleAsgnNode) {
            MultipleAsgnNode masgn = (MultipleAsgnNode) varNode;
            int preCount = masgn.getPreCount();
            boolean isRest = masgn.getRest() != null;
            Node rest = masgn.getRest();
            ListNode pre = masgn.getPre();
            noargblock = false;

            switch(preCount) {
                case 0:  // Not sure if this is actually possible, but better safe than sorry
                    if (isRest) {
                        assigner = new Pre0Rest1Post0Assigner(rest);
                    } else {
                        noargblock = true;
                        assigner = new Pre0Rest0Post0Assigner();
                    }
                    break;
                case 1:
                    assigner = isRest ? new Pre1Rest1Post0Assigner(pre.get(0), rest) :
                        new Pre1Rest0Post0Assigner(pre.get(0));
                    break;
                case 2:
                    assigner = isRest ? new Pre2Rest1Post0Assigner(pre.get(0), pre.get(1), rest) :
                        new Pre2Rest0Post0Assigner(pre.get(0), pre.get(1));
                    break;
                case 3:
                    assigner = isRest ? new Pre3Rest1Post0Assigner(pre.get(0), pre.get(1), pre.get(2), rest) :
                        new Pre3Rest0Post0Assigner(pre.get(0), pre.get(1), pre.get(2));
                    break;
                default:
                    assigner = isRest ? new PreManyRest1Post0Assigner(pre, preCount, rest) : new PreManyRest0Post0Assigner(pre, preCount);
                    break;
            }
        } else {
            assigner =  new Pre1ExpandedRest0Post0Assigner(varNode);
        }
    }

    public InterpretedBlock(IterNode iterNode, int argumentType) {
        this(iterNode, Arity.procArityOf(iterNode == null ? null : iterNode.getVarNode()), argumentType);
    }
    
    public InterpretedBlock(IterNode iterNode, Arity arity, int argumentType) {
        super(argumentType);
        this.arity = arity;
        this.bodyNode = iterNode.getBodyNode() == null ? NilImplicitNode.NIL : iterNode.getBodyNode();
        this.scope = iterNode.getScope();
        assignerFor(iterNode);
    }
    
    protected Frame pre(ThreadContext context, RubyModule klass, Binding binding) {
        return context.preYieldSpecificBlock(binding, scope, klass);
    }
    
    protected void post(ThreadContext context, Binding binding, Visibility vis, Frame lastFrame) {
        binding.getFrame().setVisibility(vis);
        context.postYield(binding, lastFrame);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yield(context, binding, type);
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);
        IRubyObject self = prepareSelf(binding);

        try {
            if (!noargblock) assigner.assign(context.getRuntime(), context, self, arg0, Block.NULL_BLOCK);

            // This while loop is for restarting the block call in case a 'redo' fires.
            return evalBlockBody(context, self);
        } catch (JumpException.NextJump nj) {
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);
        IRubyObject self = prepareSelf(binding);

        try {
            if (!noargblock) assigner.assign(context.getRuntime(), context, self, arg0, arg1, Block.NULL_BLOCK);

            // This while loop is for restarting the block call in case a 'redo' fires.
            return evalBlockBody(context, self);
        } catch (JumpException.NextJump nj) {
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    @Override
    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);
        IRubyObject self = prepareSelf(binding);

        try {
            if (!noargblock) assigner.assign(context.getRuntime(), context, self, arg0, arg1, arg2, Block.NULL_BLOCK);

            // This while loop is for restarting the block call in case a 'redo' fires.
            return evalBlockBody(context, self);
        } catch (JumpException.NextJump nj) {
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    public IRubyObject yield(ThreadContext context, Binding binding, Block.Type type) {
        IRubyObject self = prepareSelf(binding);

        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);

        try {
            if (!noargblock) assigner.assign(context.getRuntime(), context, self, Block.NULL_BLOCK);

            return evalBlockBody(context, self);
        } catch (JumpException.NextJump nj) {
            return handleNextJump(context, nj, type);
        } finally {
            post(context, binding, oldVis, lastFrame);
        }
    }

    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        IRubyObject self = prepareSelf(binding);
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);

        try {
            if (!noargblock) assigner.assignArray(context.getRuntime(), context, self,
                    assigner.convertToArray(context.getRuntime(), value), Block.NULL_BLOCK);
            
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
     * @param alreadyArray do we need an array or should we assume it already is one?
     * @return result of block invocation
     */
    public IRubyObject yield(ThreadContext context, IRubyObject value, IRubyObject self, 
            RubyModule klass, boolean alreadyArray, Binding binding, Block.Type type) {
        if (klass == null) {
            self = prepareSelf(binding);
        }
        
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, klass, binding);
        Ruby runtime = context.getRuntime();

        try {
            if (!noargblock) {
                value = alreadyArray ? assigner.convertIfAlreadyArray(runtime, value) :
                    assigner.convertToArray(runtime, value);
            
                assigner.assignArray(runtime, context, self, value, Block.NULL_BLOCK);
            }
            
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
                return bodyNode.interpret(context.getRuntime(), context, self, Block.NULL_BLOCK);
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
    
    public StaticScope getStaticScope() {
        return scope;
    }

    public void setStaticScope(StaticScope newScope) {
        this.scope = newScope;
    }

    public Block cloneBlock(Binding binding) {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        binding = binding.clone();
        return new Block(this, binding);
    }

    public Node getBodyNode() {
        return bodyNode;
    }

    /**
     * What is the arity of this block?
     * 
     * @return the arity
     */
    public Arity arity() {
        return arity;
    }
}
