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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.parser.BlockStaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;
import org.jruby.util.collections.StackElement;

/**
 *
 * @author  jpetersen
 */
public class Block implements StackElement {
    private Node var;
    private ICallable method;
    private IRubyObject self;
    private Frame frame;
    private SinglyLinkedList cref;
    private Scope scope;
    private RubyModule klass;
    private Iter iter;
    // Fixme: null dynamic scope screams for subclass (after rest of block clean up do this)
    
    // For loops are done via blocks and they have no dynamic scope of their own so we use
    // whatever is there.  What is important is that this may be null.
    private DynamicScope dynamicScope;
    private IRubyObject blockObject = null;
    public boolean isLambda = false;

    private Block next;

    public static Block createBlock(ThreadContext context, Node var, DynamicScope dynamicScope, ICallable method, 
            IRubyObject self) {
        return new Block(var,
                         method,
                         self,
                         context.getCurrentFrame(),
                         context.peekCRef(),
                         context.getFrameScope(),
                         context.getRubyClass(),
                         context.getCurrentIter(),
                         dynamicScope);
    }

    public Block(
        Node var,
        ICallable method,
        IRubyObject self,
        Frame frame,
        SinglyLinkedList cref,
        Scope scope,
        RubyModule klass,
        Iter iter,
        DynamicScope dynamicScope) {
    	
        //assert method != null;

        this.var = var;
        this.method = method;
        this.self = self;
        this.frame = frame;
        this.scope = scope;
        this.klass = klass;
        this.iter = iter;
        this.cref = cref;
        this.dynamicScope = dynamicScope;
    }
    
    public static Block createBinding(RubyModule wrapper, Iter iter, Frame frame, DynamicScope dynamicScope) {
        ThreadContext context = frame.getSelf().getRuntime().getCurrentContext();
        
        // We create one extra dynamicScope on a binding so that when we 'eval "b=1", binding' the
        // 'b' will get put into this new dynamic scope.  The original scope does not see the new
        // 'b' and successive evals with this binding will.  I take it having the ability to have 
        // succesive binding evals be able to share same scope makes sense from a programmers 
        // perspective.   One crappy outcome of this design is it requires Dynamic and Static 
        // scopes to be mutable for this one case.
        
        // Note: In Ruby 1.9 all of this logic can go away since they will require explicit
        // bindings for evals.
        
        // We only define one special dynamic scope per 'logical' binding.  So all bindings for
        // the same scope should share the same dynamic scope.  This allows multiple evals with
        // different different bindings in the same scope to see the same stuff.
        DynamicScope extraScope = dynamicScope.getBindingScope();
        
        // No binding scope so we should create one
        if (extraScope == null) {
            // If the next scope out has the same binding scope as this scope it means
            // we are evaling within an eval and in that case we should be sharing the same
            // binding scope.
            DynamicScope parent = dynamicScope.getNextCapturedScope(); 
            if (parent != null && parent.getBindingScope() == dynamicScope) {
                extraScope = dynamicScope;
            } else {
                extraScope = new DynamicScope(new BlockStaticScope(dynamicScope.getStaticScope()), dynamicScope);
                dynamicScope.setBindingScope(extraScope);
            }
        } 
        
        // FIXME: Ruby also saves wrapper, which we do not
        return new Block(null, null, frame.getSelf(), frame, context.peekCRef(), frame.getScope(), 
                context.getRubyClass(), iter, extraScope);
    }

    public IRubyObject call(IRubyObject[] args, IRubyObject replacementSelf) {
        IRuby runtime = self.getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        Block newBlock;
        
        // If we have no dynamic scope we are a block representing a for loop eval.
        // We can just reuse
        if (dynamicScope == null) {
            newBlock = this;
        } else {
            newBlock = this.cloneBlock();
            if (replacementSelf != null) {
                newBlock.self = replacementSelf;
            }
        }

        return context.yieldSpecificBlock(newBlock, runtime.newArray(args), null, null, true);
    }

    public Block cloneBlock() {
        // We clone dynamic scope because this will be a new instance of a block.  Any previously
        // captured instances of this block may still be around and we do not want to start
        // overwriting those values when we create a new one.
        Block newBlock = new Block(var, method, self, frame, cref, scope, klass, iter, ((dynamicScope == null)?null:dynamicScope.cloneScope()));
        
        newBlock.isLambda = isLambda;

        if (getNext() != null) {
            newBlock.setNext(getNext());
        }

        return newBlock;
    }

    public Arity arity() {
        return method.getArity();
    }

    public Visibility getVisibility() {
        return scope.getVisibility();
    }

    public void setVisibility(Visibility visibility) {
        scope.setVisibility(visibility);
    }

    /**
     * @see StackElement#getNext()
     */
    public StackElement getNext() {
        return next;
    }
    
    public SinglyLinkedList getCRef() {
        return cref;
    }

    /**
     * @see StackElement#setNext(StackElement)
     */
    public void setNext(StackElement newNext) {
        assert this != newNext;
        this.next = (Block) newNext;
    }
    
    public IRubyObject getBlockObject() {
    	return blockObject;
    }
    
    public void setBlockObject(IRubyObject blockObject) {
    	this.blockObject = blockObject;
    }

    /**
     * Gets the dynamicVariables.
     * @return Returns a RubyVarmap
     */
    public DynamicScope getDynamicScope() {
        return dynamicScope;
    }

    /**
     * Gets the frame.
     * @return Returns a RubyFrame
     */
    public Frame getFrame() {
        return frame;
    }

    /**
     * Gets the iter.
     * @return Returns a int
     */
    public Iter getIter() {
        return iter;
    }

    /**
     * Sets the iter.
     * @param iter The iter to set
     */
    public void setIter(Iter iter) {
        this.iter = iter;
    }

    /**
     * Gets the klass.
     * @return Returns a RubyModule
     */
    public RubyModule getKlass() {
        return klass;
    }

    /**
     * Gets the method.
     * @return Returns a IMethod
     */
    public ICallable getMethod() {
        return method;
    }

    /**
     * Gets the scope.
     * @return Returns a Scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets the self.
     * @return Returns a RubyObject
     */
    public IRubyObject getSelf() {
        return self;
    }

    /**
     * Gets the var.
     * @return Returns a Node
     */
    public Node getVar() {
        return var;
    }
}
