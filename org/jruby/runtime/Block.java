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
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.util.collections.StackElement;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Block implements StackElement {
    private INode var;
    private ICallable method;
    private RubyObject self;
    private Frame frame;
    private Scope scope;
    private RubyModule klass;
    private Iter iter;
    private Map dynamicVars;
    private RubyObject origThread;

    private Block next;

    public Block(
        INode var,
        ICallable method,
        RubyObject self,
        Frame frame,
        Scope scope,
        RubyModule klass,
        Iter iter,
        Map dynamicVars,
        RubyObject origThread) {

        this.var = var;
        this.method = method;
        this.self = self;
        this.frame = frame;
        this.scope = scope;
        this.klass = klass;
        this.iter = iter;
        this.dynamicVars = dynamicVars;
        this.origThread = origThread;
    }

    /*public void push(INode v, INode b, RubyObject newSelf) {
        Block oldBlock = new Block(var, body, self, frame, scope, klass, iter, dynamicVars, origThread, next, ruby);
    
        var = v;
        body = b;
        self = newSelf;
        frame = ruby.getRubyFrame();
        klass = ruby.getRubyClass();
        frame.setFile(ruby.getSourceFile());
        frame.setLine(ruby.getSourceLine());
        scope = (Scope) ruby.getScope().getTop();
        next = oldBlock;
        iter = ruby.getIter().getIter();
        dynamicVars = ruby.getDynamicVars();
    }*/

    /*public void pop() {
        if (next == null) {
            ruby.getRuntime().printBug("Try to pop block from empty stack.");
            return;
        }
    
        this.var = next.var;
        this.body = next.body;
        this.self = next.self;
        this.frame = next.frame;
        this.scope = next.scope;
        this.klass = next.klass;
        this.iter = next.iter;
        this.dynamicVars = next.dynamicVars;
        this.origThread = next.origThread;
        this.next = next.next;
    }*/

    /*public Block getTmp() {
        return new Block(var, body, self, frame, scope, klass, iter, dynamicVars, origThread, next, ruby);
    }
    */
    public Block cloneBlock() {
        Block newBlock = new Block(var, method, self, frame, scope, klass, iter, dynamicVars, origThread);

        // XXX
        if (getNext() != null) {
            // XXX
            // newBlock.setNext(((Block)getNext()).cloneBlock());
            newBlock.setNext(((Block)getNext()));
        }

        return newBlock;
    }
    /*
        public void setTmp(Block block) {
            this.var = block.var;
            this.body = block.body;
            this.self = block.self;
            this.frame = block.frame;
            this.scope = block.scope;
            this.klass = block.klass;
            this.iter = block.iter;
            // this.vmode = block.vmode;
            // this.flags = block.flags;
            this.dynamicVars = block.dynamicVars;
            this.origThread = block.origThread;
    
            this.next = block.next;
        }*/

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
     * Gets the dynamicVars.
     * @return Returns a RubyVarmap
     */
    public Map getDynamicVars() {
        return dynamicVars;
    }

    /**
     * Sets the dynamicVars.
     * @param dynamicVars The dynamicVars to set
     */
    public void setDynamicVars(Map dynamicVars) {
        this.dynamicVars = dynamicVars;
    }

    /**
     * Gets the frame.
     * @return Returns a RubyFrame
     */
    public Frame getFrame() {
        return frame;
    }

    /**
     * Sets the frame.
     * @param frame The frame to set
     */
    public void setFrame(Frame frame) {
        this.frame = frame;
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
     * Sets the klass.
     * @param klass The klass to set
     */
    public void setKlass(RubyModule klass) {
        this.klass = klass;
    }

    /**
     * Gets the method.
     * @return Returns a IMethod
     */
    public ICallable getMethod() {
        return method;
    }

    /**
     * Sets the method.
     * @param method The method to set
     */
    public void setMethod(ICallable method) {
        this.method = method;
    }

    /**
     * Gets the origThread.
     * @return Returns a RubyObject
     */
    public RubyObject getOrigThread() {
        return origThread;
    }

    /**
     * Sets the origThread.
     * @param origThread The origThread to set
     */
    public void setOrigThread(RubyObject origThread) {
        this.origThread = origThread;
    }

    /**
     * Gets the scope.
     * @return Returns a Scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the scope.
     * @param scope The scope to set
     */
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * Gets the self.
     * @return Returns a RubyObject
     */
    public RubyObject getSelf() {
        return self;
    }

    /**
     * Sets the self.
     * @param self The self to set
     */
    public void setSelf(RubyObject self) {
        this.self = self;
    }

    /**
     * Gets the var.
     * @return Returns a INode
     */
    public INode getVar() {
        return var;
    }

    /**
     * Sets the var.
     * @param var The var to set
     */
    public void setVar(INode var) {
        this.var = var;
    }
}