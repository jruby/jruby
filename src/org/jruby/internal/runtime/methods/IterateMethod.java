/*
 * EvaluateMethod.java - description
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
import org.jruby.runtime.ICallable;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class IterateMethod extends AbstractMethod {
    private Callback callback;
    private IRubyObject data;

    public IterateMethod(Callback callback, IRubyObject data) {
        super(null);
        this.callback = callback;
        this.data = data;
    }

    /**
     * @see IMethod#execute(Ruby, RubyObject, String, RubyObject[], boolean)
	 * @fixme implement it 
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return callback.execute(args[0], new IRubyObject[] { data, receiver });
    }
    
    public ICallable dup() {
        return new IterateMethod(callback, data);
    }       
}