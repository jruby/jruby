/*
 * ProcMethod.java - description
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
import org.jruby.RubyProc;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ProcMethod extends AbstractMethod {
    private RubyProc proc;

    /**
     * Constructor for ProcMethod.
     * @param visibility
     */
    public ProcMethod(RubyProc proc, Visibility visibility) {
        super(visibility);
        this.proc = proc;
    }

    /**
     * @see org.jruby.runtime.ICallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        IRubyObject self = receiver;
        return proc.call(args, self);
    }
    
    public ICallable dup() {
        return new ProcMethod(proc, getVisibility());
    }
}