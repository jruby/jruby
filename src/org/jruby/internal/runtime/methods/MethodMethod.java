/*
 * IterateMethod.java - description
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
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
package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.UnboundMethod;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class MethodMethod extends AbstractMethod {
    private UnboundMethod method;

    /**
     * Constructor for MethodMethod.
     * @param visibility
     */
    public MethodMethod(UnboundMethod method, Visibility visibility) {
        super(visibility);
        this.method = method;
    }

    /**
     * @see org.jruby.runtime.ICallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby runtime, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return method.bind(receiver).call(args);
    }
    
    public ICallable dup() {
        return new MethodMethod(method, getVisibility());
    }
}