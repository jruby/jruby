/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */

package org.jruby.evaluator;

import org.jruby.Ruby;
import org.jruby.util.collections.IStack;
import org.jruby.util.collections.CollectionFactory;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.ablaf.ast.INode;

public class Evaluator {
    private final Ruby ruby;

    private ScopeStack scope;
    private FrameStack frameStack;
    private IStack iterStack = CollectionFactory.getInstance().newStack();

    public Evaluator(Ruby ruby) {
        this.ruby = ruby;
        scope = new ScopeStack(ruby);
        frameStack = new FrameStack(ruby);
    }

    public ScopeStack getScope() {
        return scope;
    }

    public FrameStack getFrameStack() {
        return frameStack;
    }

    public Frame getCurrentFrame() {
        return (Frame) getFrameStack().peek();
    }

    public IStack getIterStack() {
        return iterStack;
    }

    public Iter getCurrentIter() {
        return (Iter) getIterStack().peek();
    }

    public IRubyObject eval(INode node) {
        return EvaluateVisitor.createVisitor(ruby.getRubyTopSelf()).eval(node);
    }
}
