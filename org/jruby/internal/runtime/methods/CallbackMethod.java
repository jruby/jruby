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

import org.jruby.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class CallbackMethod extends AbstractMethod {
    private Callback callback;

    public CallbackMethod(Callback callback) {
        this.callback = callback;
    }

    /**
     * @see IMethod#execute(Ruby, RubyObject, String, RubyObject[], boolean)
     */
    public RubyObject execute(Ruby ruby, RubyObject receiver, String name, RubyObject[] args, boolean noSuper) {
        if (ruby.getRuntime().getTraceFunction() != null) {
            String file = ruby.getFrameStack().getPrevious().getFile();
            int line = ruby.getFrameStack().getPrevious().getLine();
                
            if (file == null) {
                file = ruby.getSourceFile();
                line = ruby.getSourceLine();
            }

            ruby.getRuntime().callTraceFunction("c-call", file, line, receiver, name, getImplementationClass()); // XXX
            try {
                return callback.execute(receiver, args, ruby);
            } finally {
                if (ruby.getRuntime().getTraceFunction() != null) {
                    ruby.getRuntime().callTraceFunction("c-return", file, line, receiver, name, getImplementationClass()); // XXX
                }
            }
        } else {
            return callback.execute(receiver, args, ruby);
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