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

        // top = new Scope();
    }

    public void push(List localNames) {
        push();

        setLocalNames(localNames);
        if (localNames != null) {
            setLocalValues(new ArrayList(Collections.nCopies(localNames.size(), ruby.getNil())));
        }
    }

    public void push(StackElement newElement) {
        if (top != null) {
            ((Scope) top).setMethodScope(ruby.getActMethodScope());
        }

        super.push(newElement);

        ruby.setActMethodScope(Constants.NOEX_PUBLIC);
    }

    public void push() {
        this.push(new Scope());
    }

    public StackElement pop() {
        Scope result = (Scope) super.pop();

        ruby.setActMethodScope(result.getMethodScope());

        return result;
    }
    
    public void setTop(StackElement newElement) {
        top = newElement;
    }

    // delegates to the top object

    public int getFlags() {
        return ((Scope) top).getFlags();
    }

    public void setFlags(int flags) {
        ((Scope) top).setFlags(flags);
    }

    public RubyObject getSuperObject() {
        return ((Scope) top).getSuperObject();
    }

    public void setSuperObject(RubyObject superObject) {
        ((Scope) top).setSuperObject(superObject);
    }

    public List getLocalNames() {
        return ((Scope) top).getLocalNames();
    }

    public void setLocalNames(List localName) {
        ((Scope) top).setLocalNames(localName);
    }

    public List getLocalValues() {
        return ((Scope) top).getLocalValues();
    }

    public void setLocalValues(List localValue) {
        ((Scope) top).setLocalValues(localValue);
    }

    public RubyObject getValue(int count) {
        return ((Scope) top).getValue(count);
    }

    public void setValue(int count, RubyObject value) {
        ((Scope) top).setValue(count, value);
    }
}