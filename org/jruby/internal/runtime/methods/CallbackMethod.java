/*
 * CallbackMethod.java - description
 * Created on 11.03.2002, 21:42:20
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
package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.runtime.Callback;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.ablaf.common.ISourcePosition;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class CallbackMethod extends AbstractMethod {
    private Callback callback;

    public CallbackMethod(Callback callback, Visibility visibility) {
        super(visibility);
        this.callback = callback;
    }

    /**
     * @see IMethod#execute(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        if (ruby.getRuntime().getTraceFunction() != null) {
            ISourcePosition position = ruby.getFrameStack().getPrevious().getPosition();
            if (position == null) {
                position = ruby.getPosition();
            }

            ruby.getRuntime().callTraceFunction("c-call", position, receiver, name, getImplementationClass()); // XXX
            try {
                return callback.execute(receiver, args);
            } finally {
                ruby.getRuntime().callTraceFunction("c-return", position, receiver, name, getImplementationClass()); // XXX
            }
        } else {
            return callback.execute(receiver, args);
        }
    }

    /**
     * Gets the callback.
     * @return Returns a Callback
     */
    public Callback getCallback() {
        return callback;
    }

}