/*
 * RubyBlock.java - description
 * Created on 02.03.2002, 17:46:25
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.runtime;

import org.ablaf.ast.INode;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;
import org.jruby.util.collections.StackElement;
import org.jruby.RubyModule;
import org.jruby.Ruby;
import org.jruby.RubyArray;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Block implements StackElement {
    private INode var;
    private ICallable method;
    private IRubyObject self;
    private Frame frame;
    private Scope scope;
    private RubyModule klass;
    private Iter iter;
    private DynamicVariableSet dynamicVariables;

    private Block next;

    public static Block createBlock(INode var, ICallable method, IRubyObject self) {
        Ruby ruby = self.getRuntime();
        ThreadContext context = ruby.getCurrentContext();
        return new Block(var,
                         method,
                         self,
                         context.getCurrentFrame(),
                         context.currentScope(),
                         context.getRubyClass(),
                         context.getCurrentIter(),
                         context.getCurrentDynamicVars());
    }

    private Block(
        INode var,
        ICallable method,
        IRubyObject self,
        Frame frame,
        Scope scope,
        RubyModule klass,
        Iter iter,
        DynamicVariableSet dynamicVars) {

        this.var = var;
        this.method = method;
        this.self = self;
        this.frame = frame;
        this.scope = scope;
        this.klass = klass;
        this.iter = iter;
        this.dynamicVariables = dynamicVars;
    }

    public IRubyObject call(IRubyObject[] args) {
        return call(args, null);
    }

    public IRubyObject call(IRubyObject[] args, IRubyObject replacementSelf) {
        Ruby ruby = self.getRuntime();
        ThreadContext context = ruby.getCurrentContext();
        Block oldBlock = context.getBlockStack().getCurrent();
        Block newBlock = this.cloneBlock();
        if (replacementSelf != null) {
            newBlock.self = replacementSelf;
        }
        context.getBlockStack().setCurrent(newBlock);
        context.getIterStack().push(Iter.ITER_CUR);
        context.getCurrentFrame().setIter(Iter.ITER_CUR);
        try {
            return ruby.yield(args != null ? RubyArray.newArray(ruby, args) : null, null, null, true);
        } finally {
            context.getIterStack().pop();
            context.getBlockStack().setCurrent(oldBlock);
        }
    }

    public Block cloneBlock() {
        Block newBlock = new Block(var, method, self, frame, scope, klass, iter, new DynamicVariableSet(dynamicVariables));

        if (getNext() != null) {
            newBlock.setNext(((Block)getNext()));
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

    /**
     * @see StackElement#setNext(StackElement)
     */
    public void setNext(StackElement newNext) {
        Asserts.isTrue(this != newNext);
        this.next = (Block) newNext;
    }

    /**
     * Gets the dynamicVariables.
     * @return Returns a RubyVarmap
     */
    public DynamicVariableSet getDynamicVariables() {
        return dynamicVariables;
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
     * @return Returns a INode
     */
    public INode getVar() {
        return var;
    }
}