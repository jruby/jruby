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

import java.util.Map;

import org.ablaf.ast.INode;
import org.jruby.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.StackElement;

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
    private Map dynamicVariables;
    // private IRubyObject origThread;

    private Block next;

    public static Block createBlock(INode var, ICallable method, IRubyObject self) {
        Ruby ruby = self.toRubyObject().getRuntime();
        return new Block(var, method, self, ruby.getCurrentFrame(), ruby.currentScope(), ruby.getRubyClass(), ruby.getCurrentIter(), ruby.getDynamicVars()/*, null*/);
    }

    private Block(
        INode var,
        ICallable method,
        IRubyObject self,
        Frame frame,
        Scope scope,
        RubyModule klass,
        Iter iter,
        Map dynamicVars/*,
        IRubyObject origThread*/) {

        this.var = var;
        this.method = method;
        this.self = self;
        this.frame = frame;
        this.scope = scope;
        this.klass = klass;
        this.iter = iter;
        this.dynamicVariables = dynamicVars;
        // this.origThread = origThread;
    }

    public Block cloneBlock() {
        Block newBlock = new Block(var, method, self, frame, scope, klass, iter, dynamicVariables/*, origThread*/);

        if (getNext() != null) {
            newBlock.setNext(((Block)getNext()));
        }

        return newBlock;
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
        this.next = (Block) newNext;
    }

    /**
     * Gets the dynamicVariables.
     * @return Returns a RubyVarmap
     */
    public Map getDynamicVariables() {
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