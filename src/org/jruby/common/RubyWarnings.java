/*
 * RubyErrorHandler.java - description
 * Created on 04.03.2002, 12:47:19
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
package org.jruby.common;

import org.jruby.Ruby;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyWarnings {
    private Ruby runtime;

    public RubyWarnings(Ruby runtime) {
        this.runtime = runtime;
    }

    public void warn(SourcePosition position, String message) {
        StringBuffer buffer = new StringBuffer(100);
        if (position != null) {
            buffer.append(position.getFile()).append(':');
            buffer.append(position.getLine()).append(' ');
        }
        buffer.append("warning: ").append(message).append('\n');
        IRubyObject errorStream = runtime.getGlobalVariables().get("$stderr");
        errorStream.callMethod("write", runtime.newString(buffer.toString()));
    }

    public boolean isVerbose() {
        return runtime.getVerbose().isTrue();
    }

    public void warn(String message) {
        warn(runtime.getPosition(), message);
    }

    public void warning(String message) {
        warning(runtime.getPosition(), message);
    }
    
    public void warning(SourcePosition position, String message) {
        if (isVerbose()) {
            warn(position, message);
        }
    }
}