/*
 * RubyFrame.java - No description
 * Created on 10. September 2001, 17:54
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;
import org.ablaf.common.ISourcePosition;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class Frame {
    private IRubyObject self = null;
    private IRubyObject[] args = null;
    private String lastFunc = null;
    private RubyModule lastClass = null;
    private Namespace namespace = null;
    private Frame tmp = null;
    private ISourcePosition position;
    private Iter iter = Iter.ITER_NOT;

    public Frame(
        IRubyObject self,
        IRubyObject[] args,
        String lastFunc,
        RubyModule lastClass,
        Namespace namespace,
        Frame tmp,
        ISourcePosition position,
        Iter iter) {

        this.self = self;
        this.args = args;
        this.lastFunc = lastFunc;
        this.lastClass = lastClass;
        this.namespace = namespace;
        this.tmp = tmp;
        this.position = position;
        this.iter = iter;
    }

    public Frame(Frame frame) {
        this(frame.self, frame.args, frame.lastFunc, frame.lastClass, frame.namespace, frame.tmp, frame.position, frame.iter);
    }

    /** Getter for property args.
     * @return Value of property args.
     */
    public IRubyObject[] getArgs() {
        return args;
    }

    /** Setter for property args.
     * @param args New value of property args.
     */
    public void setArgs(IRubyObject[] args) {
        this.args = args;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public ISourcePosition getPosition() {
        return position;
    }

    public String getFile() {
        return position.getFile();
    }

    /** Getter for property iter.
     * @return Value of property iter.
     */
    public Iter getIter() {
        return iter;
    }

    /** Setter for property iter.
     * @param iter New value of property iter.
     */
    public void setIter(Iter iter) {
        this.iter = iter;
    }

    public boolean isBlockGiven() {
        return iter.isBlockGiven();
    }

    /** Getter for property lastClass.
     * @return Value of property lastClass.
     */
    public RubyModule getLastClass() {
        return lastClass;
    }

    /** Setter for property lastClass.
     * @param lastClass New value of property lastClass.
     */
    public void setLastClass(RubyModule lastClass) {
        this.lastClass = lastClass;
    }

    /** Getter for property lastFunc.
     * @return Value of property lastFunc.
     */
    public String getLastFunc() {
        return lastFunc;
    }

    /** Setter for property lastFunc.
     * @param lastFunc New value of property lastFunc.
     */
    public void setLastFunc(String lastFunc) {
        this.lastFunc = lastFunc;
    }

    /** Getter for property line.
     * @return Value of property line.
     */
    public int getLine() {
        return position.getLine();
    }

    /** Getter for property self.
     * @return Value of property self.
     */
    public IRubyObject getSelf() {
        return self;
    }

    /** Setter for property self.
     * @param self New value of property self.
     */
    public void setSelf(IRubyObject self) {
        this.self = self;
    }

    /** Getter for property tmp.
     * @return Value of property tmp.
     */
    public Frame getTmp() {
        return tmp;
    }

    /** Setter for property tmp.
     * @param tmp New value of property tmp.
     */
    public void setTmp(Frame tmp) {
        this.tmp = tmp;
    }

    /**
     * pushes a copy of this frame in the tmp stack.
     * 
     **/
    public void tmpPush() {
        Frame tmpFrame = new Frame(self, args, lastFunc, lastClass, namespace, tmp, position, iter);

        tmp = tmpFrame;
    }

    /**
     * pops the top of the tmp stack
     **/
    public void tmpPop() {
        self = tmp.self;
        args = tmp.args;
        lastFunc = tmp.lastFunc;
        lastClass = tmp.lastClass;
        namespace = tmp.namespace;
        position = tmp.position;
        iter = tmp.iter;
        tmp = tmp.tmp;
    }
}
