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
import org.jruby.RubyProc;
import org.jruby.ast.IterNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public class InterpretedBlock extends Block {
    /**
     * AST Node representing the parameter (VARiable) list to the block.
     */
    private IterNode iterNode;
    
    /**
     * The Proc that this block is associated with.  When we reference blocks via variable
     * reference they are converted to Proc objects.  We store a reference of the associated
     * Proc object for easy conversion.  
     */
    private RubyProc proc = null;
    
    protected Arity arity;

    public static InterpretedBlock createBlock(ThreadContext context, IterNode iterNode, DynamicScope dynamicScope, IRubyObject self) {
        Frame f = context.getCurrentFrame();
        f.setPosition(context.getPosition());
        return new InterpretedBlock(iterNode,
                         self,
                         f,
                         f.getVisibility(),
                         context.getRubyClass(),
                         dynamicScope);
    }
    
    protected InterpretedBlock() {
        this(null, null, null, null, null, null);
    }

    public InterpretedBlock(IterNode iterNode, IRubyObject self, Frame frame,
        Visibility visibility, RubyModule klass, DynamicScope dynamicScope) {
        this(iterNode, self,iterNode == null ? null : Arity.procArityOf(iterNode.getVarNode()),
                frame, visibility, klass, dynamicScope);
    }
    
    public InterpretedBlock(IterNode iterNode, IRubyObject self, Arity arity, Frame frame,
            Visibility visibility, RubyModule klass, DynamicScope dynamicScope) {
        super(self, frame, visibility, klass, dynamicScope);
        this.iterNode = iterNode;
        this.arity = arity;
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        switch (type) {
        case NORMAL: {
            assert false : "can this happen?";
            if (args.length == 1 && args[0] instanceof RubyArray && iterNode != null) {
                Node vNode = iterNode.getVarNode();

                if (vNode.nodeId == NodeType.MULTIPLEASGNNODE) {
                    args = ((RubyArray) args[0]).toJavaArray();
                }
            }
            break;
        }
        case PROC: {
            if (args.length == 1 && args[0] instanceof RubyArray && iterNode != null) {
                Node vNode = iterNode.getVarNode();

                if (vNode.nodeId == NodeType.MULTIPLEASGNNODE) {
                    // if we only have *arg, we leave it wrapped in the array
                    if (((MultipleAsgnNode)vNode).getArgsNode() == null) {
                        args = ((RubyArray) args[0]).toJavaArray();
                    }
                }
            }
            break;
        }
        case LAMBDA:
            arity().checkArity(context.getRuntime(), args);
            break;
        }

        return yield(context, context.getRuntime().newArrayNoCopy(args), null, null, true);
    }
    
    protected void pre(ThreadContext context, RubyModule klass) {
        context.preYieldSpecificBlock(this, klass);
    }
    
    protected void post(ThreadContext context) {
        context.postYield(this);
    }
    
    public IRubyObject yield(ThreadContext context, IRubyObject value) {
        return yield(context, value, null, null, false);
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
            RubyModule klass, boolean aValue) {
        if (klass == null) {
            self = binding.getSelf();
            binding.getFrame().setSelf(self);
        }
        
        Visibility oldVis = binding.getFrame().getVisibility();
        pre(context, klass);

        try {
            if (iterNode.getVarNode() != null) {
                if (aValue) {
                    setupBlockArgs(context, iterNode.getVarNode(), value, self);
                } else {
                    setupBlockArg(context, iterNode.getVarNode(), value, self);
                }
            }
            
            // This while loop is for restarting the block call in case a 'redo' fires.
            while (true) {
                try {
                    return ASTInterpreter.eval(context.getRuntime(), context, iterNode.getBodyNode(), self, NULL_BLOCK);
                } catch (JumpException.RedoJump rj) {
                    context.pollThreadEvents();
                    // do nothing, allow loop to redo
                } catch (JumpException.BreakJump bj) {
                    if (bj.getTarget() == null) {
                        bj.setTarget(this);                            
                    }                        
                    throw bj;
                }
            }
        } catch (JumpException.NextJump nj) {
            // A 'next' is like a local return from the block, ending this call or yield.
            return type == Type.LAMBDA ? context.getRuntime().getNil() : (IRubyObject)nj.getValue();
        } finally {
            binding.getFrame().setVisibility(oldVis);
            post(context);
        }
    }

    private void setupBlockArgs(ThreadContext context, Node varNode, IRubyObject value, IRubyObject self) {
        Ruby runtime = self.getRuntime();
        
        switch (varNode.nodeId) {
        case ZEROARGNODE:
            break;
        case MULTIPLEASGNNODE:
            value = AssignmentVisitor.multiAssign(runtime, context, self, (MultipleAsgnNode)varNode, (RubyArray)value, false);
            break;
        default:
            int length = arrayLength(value);
            switch (length) {
            case 0:
                value = runtime.getNil();
                break;
            case 1:
                value = ((RubyArray)value).eltInternal(0);
                break;
            default:
                runtime.getWarnings().warn("multiple values for a block parameter (" + length + " for 1)");
            }
            AssignmentVisitor.assign(runtime, context, self, varNode, value, InterpretedBlock.NULL_BLOCK, false);
        }
    }

    private void setupBlockArg(ThreadContext context, Node varNode, IRubyObject value, IRubyObject self) {
        Ruby runtime = self.getRuntime();
        
        switch (varNode.nodeId) {
        case ZEROARGNODE:
            return;
        case MULTIPLEASGNNODE:
            value = AssignmentVisitor.multiAssign(runtime, context, self, (MultipleAsgnNode)varNode,
                    ArgsUtil.convertToRubyArray(runtime, value, ((MultipleAsgnNode)varNode).getHeadNode() != null), false);
            break;
        default:
            if (value == null) {
                runtime.getWarnings().warn("multiple values for a block parameter (0 for 1)");
            }
            AssignmentVisitor.assign(runtime, context, self, varNode, value, InterpretedBlock.NULL_BLOCK, false);
        }
    }
    
    protected int arrayLength(IRubyObject node) {
        return node instanceof RubyArray ? ((RubyArray)node).getLength() : 0;
    }

    public Block cloneBlock() {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        // ENEBO: Once we make self, lastClass, and lastMethod immutable we can remove duplicate
        InterpretedBlock newBlock = new InterpretedBlock(iterNode, binding.getSelf(), binding.getFrame().duplicate(), binding.getVisibility(), binding.getKlass(), 
                binding.getDynamicScope().cloneScope());
        
        newBlock.type = type;

        return newBlock;
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

    /**
     * Retrieve the proc object associated with this block
     * 
     * @return the proc or null if this has no proc associated with it
     */
    public RubyProc getProcObject() {
    	return proc;
    }
    
    /**
     * Set the proc object associated with this block
     * 
     * @param procObject
     */
    public void setProcObject(RubyProc procObject) {
    	this.proc = procObject;
    }
    
    /**
     * Is the current block a real yield'able block instead a null one
     * 
     * @return true if this is a valid block or false otherwise
     */
    public boolean isGiven() {
        return true;
    }
    
    /**
     * Compiled codes way of examining arguments
     * 
     * @param nodeId to be considered
     * @return something not linked to AST and a constant to make compiler happy
     */
    public static int asArgumentType(NodeType nodeId) {
        if (nodeId == null) return ZERO_ARGS;
        
        switch (nodeId) {
        case ZEROARGNODE: return ZERO_ARGS;
        case MULTIPLEASGNNODE: return MULTIPLE_ASSIGNMENT;
        case SVALUENODE: return SINGLE_RESTARG;
        }
        return ARRAY;
    }
}
