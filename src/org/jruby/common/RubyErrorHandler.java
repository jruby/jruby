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
import org.jruby.RubyString;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.parser.SyntaxErrorState;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyErrorHandler implements IRubyErrorHandler {
    private Ruby runtime;
    private boolean verbose = false;

    /**
     * Constructor for RubyErrorHandler.
     */
    public RubyErrorHandler(Ruby runtime) {
        this.runtime = runtime;
    }

    /**
     * @see org.ablaf.common.IErrorHandler#isHandled(int)
     */
    public boolean isHandled(int type) {
        if (type == IErrors.WARNING || type == IErrors.VERBOSE) {
            return verbose;
        }
        return true;
    }

    /**
     * @see org.ablaf.common.IErrorHandler#handleError(int, ISourcePosition, String, Object)
     */
    public void handleError(int type, SourcePosition position, String message, Object args) {
        if (isHandled(type)) {
            if (type == IErrors.WARN || type == IErrors.WARNING) {
                message = "warning: " + message;
            }

            if (position != null) {
                message = position.getFile() + ":" + position.getLine() + " " + message;
            }

            writeError(message + "\n");

            if (type == IErrors.SYNTAX_ERROR) {
                writeError("\tExpecting:");
                String[] lExpected = {};
				String lFound = "";
                if (args instanceof String[]) {
					lExpected = (String[])args;
                } else if (args instanceof SyntaxErrorState) {
					lExpected = ((SyntaxErrorState)args).expected();
					lFound = ((SyntaxErrorState)args).found();
				}
				for (int i = 0; i < lExpected.length; i++) {
					String msg = lExpected[i];
					writeError(" " + msg);
				}
				writeError(" but found " + lFound + " instead\n");
            }
        }
    }

    private void writeError(String s) {
        IRubyObject errorStream = runtime.getGlobalVariables().get("$stderr");
        errorStream.callMethod("write", RubyString.newString(runtime, s));
    }

    /**
     * @see org.ablaf.common.IErrorHandler#handleError(int, ISourcePosition, String)
     */
    public void handleError(int type, SourcePosition position, String message) {
        handleError(type, position, message, null);
    }

    /**
     * @see org.ablaf.common.IErrorHandler#handleError(int, String)
     */
    public void handleError(int type, String message) {
        handleError(type, null, message, null);
    }

    /**
     * Gets the verbose.
     * @return Returns a boolean
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Sets the verbose.
     * @param verbose The verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    /**
     * @see org.jruby.common.IRubyErrorHandler#warn(String)
     */
    public void warn(String message) {
        handleError(IErrors.WARN, runtime.getPosition(), message);
    }

    /**
     * @see org.jruby.common.IRubyErrorHandler#warning(String)
     */
    public void warning(String message) {
        handleError(IErrors.WARNING, runtime.getPosition(), message);
    }
}