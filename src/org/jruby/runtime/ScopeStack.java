/*
 * ScopeStack.java - No description
 * Created on 20.01.2002, 15:42:25
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

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.AbstractStack;
import org.jruby.util.collections.StackElement;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class ScopeStack extends AbstractStack {
    private Ruby ruby;

    public ScopeStack(Ruby ruby) {
        this.ruby = ruby;
    }

    public void push(String[] localNames) {
        push(Arrays.asList(localNames));
    }

    public void push(List localNames) {
        push();
        resetLocalVariables(localNames);
    }

    public void push(StackElement newElement) {
        if (current() != null) {
            //current().setVisibility(ruby.getCurrentVisibility());
        }
        super.push(newElement);
        //ruby.setCurrentVisibility(Visibility.PUBLIC);
    }

    public void push() {
        this.push(new Scope(ruby));
    }

    public StackElement pop() {
        Scope result = (Scope) super.pop();
        //ruby.setCurrentVisibility(result.getVisibility());
        return result;
    }
    
    public void setTop(StackElement newElement) {
        top = newElement;
    }

    public Scope current() {
        return (Scope) top;
    }

    public IRubyObject getSuperObject() {
        return current().getSuperObject();
    }

    public void setSuperObject(IRubyObject superObject) {
        current().setSuperObject(superObject);
    }

    public List getLocalNames() {
        return current().getLocalNames();
    }

    public void resetLocalVariables(List localNames) {
        current().resetLocalVariables(localNames);
    }

    public void addLocalVariables(List localNames) {
        current().addLocalVariables(localNames);
    }

    public boolean hasLocalVariables() {
        return current().hasLocalVariables();
    }

    public IRubyObject getValue(int count) {
        return current().getValue(count);
    }

    public void setValue(int count, IRubyObject value) {
        current().setValue(count, value);
    }

    public IRubyObject getLastLine() {
        return current().getLastLine();
    }

    public void setLastLine(IRubyObject value) {
        current().setLastLine(value);
    }

    public IRubyObject getBackref() {
        return current().getBackref();
    }

    public void setBackref(IRubyObject match) {
        current().setBackref(match);
    }


}
