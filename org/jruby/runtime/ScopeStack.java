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

import java.util.*;

import org.jruby.*;
import org.jruby.util.collections.*;

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

    public void push(List localNames) {
        push();
        setLocalNames(localNames);
    }

    public void push(StackElement newElement) {
        if (current() != null) {
            current().setMethodScope(ruby.getActMethodScope());
        }
        super.push(newElement);
        ruby.setActMethodScope(Constants.NOEX_PUBLIC);
    }

    public void push() {
        this.push(new Scope(ruby));
    }

    public StackElement pop() {
        Scope result = (Scope) super.pop();
        ruby.setActMethodScope(result.getMethodScope());
        return result;
    }
    
    public void setTop(StackElement newElement) {
        top = newElement;
    }

    private Scope current() {
        return (Scope) top;
    }

    // delegates to the top object

    public int getFlags() {
        return current().getFlags();
    }

    public void setFlags(int flags) {
        current().setFlags(flags);
    }

    public RubyObject getSuperObject() {
        return current().getSuperObject();
    }

    public void setSuperObject(RubyObject superObject) {
        current().setSuperObject(superObject);
    }

    public List getLocalNames() {
        return current().getLocalNames();
    }

    public void setLocalNames(List localName) {
        current().setLocalNames(localName);
    }

    public boolean hasLocalValues() {
        return getLocalValues() != null;
    }

    private List getLocalValues() {
        return current().getLocalValues();
    }

    public RubyObject getValue(int count) {
        return current().getValue(count);
    }

    public void setValue(int count, RubyObject value) {
        current().setValue(count, value);
    }
}
