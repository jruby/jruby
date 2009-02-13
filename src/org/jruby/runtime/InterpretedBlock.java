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
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.IterNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.exceptions.JumpException;
import org.jruby.parser.StaticScope;
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
    /** The node wrapping the body and parameter list for this block */
    private final IterNode iterNode;
    
    /** Whether this block has an argument list or not */
    private final boolean hasVarNode;
    
    /** The argument list, pulled out of iterNode */
    private final Node varNode;
    
    /** The body of the block, pulled out of bodyNode */
    private final Node bodyNode;
    
    /** The static scope for the block body */
    private final StaticScope scope;
    
    /** The arity of the block */
    private final Arity arity;

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

    public InterpretedBlock(IterNode iterNode, int argumentType) {
        this(iterNode, Arity.procArityOf(iterNode == null ? null : iterNode.getVarNode()), argumentType);
    }
    
    public InterpretedBlock(IterNode iterNode, Arity arity, int argumentType) {
        super(argumentType);
        this.iterNode = iterNode;
        this.arity = arity;
        this.hasVarNode = iterNode.getVarNode() != null;
        this.varNode = iterNode.getVarNode();
        this.bodyNode = iterNode.getBodyNode() == null ? NilImplicitNode.NIL : iterNode.getBodyNode();
        this.scope = iterNode.getScope();
    }
    
    protected Frame pre(ThreadContext context, RubyModule klass, Binding binding) {
        return context.preYieldSpecificBlock(binding, iterNode.getScope(), klass);
    }
    
    protected void post(ThreadContext context, Binding binding, Visibility vis, Frame lastFrame) {
        binding.getFrame().setVisibility(vis);
        context.postYield(binding, lastFrame);
    }

    public IRubyObject yieldSpecific(ThreadContext context, Binding binding, Block.Type type) {
        return yield(context, null, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, Binding binding, Block.Type type) {
        return yield(context, arg0, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Binding binding, Block.Type type) {
        return yield(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1), null, null, true, binding, type);
    }

    public IRubyObject yieldSpecific(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Binding binding, Block.Type type) {
        return yield(context, context.getRuntime().newArrayNoCopyLight(arg0, arg1, arg2), null, null, true, binding, type);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value, Binding binding, Block.Type type) {
        IRubyObject self = prepareSelf(binding);
        
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, null, binding);

        try {
            if (hasVarNode) {
                setupBlockArg(context, varNode, value, self);
            }
            
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
        if (klass == null) {
            self = prepareSelf(binding);
        }
        
        Visibility oldVis = binding.getFrame().getVisibility();
        Frame lastFrame = pre(context, klass, binding);

        try {
            if (hasVarNode) {
                if (aValue) {
                    setupBlockArgs(context, varNode, value, self);
                } else {
                    setupBlockArg(context, varNode, value, self);
                }
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

    private void setupBlockArgs(ThreadContext context, Node varNode, IRubyObject value, IRubyObject self) {
        Ruby runtime = context.getRuntime();
        
        switch (varNode.getNodeType()) {
        case ZEROARGNODE:
            break;
        case MULTIPLEASGNNODE:
            value = AssignmentVisitor.multiAssign(runtime, context, self, (MultipleAsgnNode)varNode, (RubyArray)value, false);
            break;
        default:
            defaultArgsLogic(context, runtime, self, value);
        }
    }

    private void setupBlockArg(ThreadContext context, Node varNode, IRubyObject value, IRubyObject self) {
        Ruby runtime = context.getRuntime();
        
        switch (varNode.getNodeType()) {
        case ZEROARGNODE:
            return;
        case MULTIPLEASGNNODE:
            value = AssignmentVisitor.multiAssign(runtime, context, self, (MultipleAsgnNode)varNode,
                    ArgsUtil.convertToRubyArray(runtime, value, ((MultipleAsgnNode)varNode).getHeadNode() != null), false);
            break;
        default:
            defaultArgLogic(context, runtime, self, value);
        }
    }
    
    private final void defaultArgsLogic(ThreadContext context, Ruby ruby, IRubyObject self, IRubyObject value) {
        int length = ArgsUtil.arrayLength(value);
        switch (length) {
        case 0:
            value = ruby.getNil();
            break;
        case 1:
            value = ((RubyArray)value).eltInternal(0);
            break;
        default:
            ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + length + " for 1)");
        }
        
        varNode.assign(ruby, context, self, value, Block.NULL_BLOCK, false);
    }
    
    private final void defaultArgLogic(ThreadContext context, Ruby ruby, IRubyObject self, IRubyObject value) {
        if (value == null) {
            ruby.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (0 for 1)");
        }
        varNode.assign(ruby, context, self, value, Block.NULL_BLOCK, false);
    }
    
    public StaticScope getStaticScope() {
        return scope;
    }

    public Block cloneBlock(Binding binding) {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        binding = binding.clone();
        return new Block(this, binding);
    }

    public IterNode getIterNode() {
        return iterNode;
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
