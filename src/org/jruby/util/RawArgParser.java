/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 David R. Halliday <hallidave@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Parse the raw arguments passed in by the ShellLauncher.  These arguments
 * have not been processed by a real command line so they need to be
 * massaged so that it looks like they have been.  Mostly we just want to
 * get rid of extraneous quotes.
 */
public class RawArgParser {
    
    private final static int INIT_STATE = 0;
    private final static int SKIPPING_SPACES_STATE = 1;
    private final static int COLLECTING_ARG_STATE = 2;
    private final static int IN_DOUBLE_QUOTE_STATE = 3;
    private final static int IN_SINGLE_QUOTE_STATE = 4;
    
    private Stack args = new Stack();
    
    public RawArgParser(String[] rawArgs) {
        if (rawArgs.length > 0) {
            args.addAll(parse(rawArgs[0]));
            for (int i = 1; i < rawArgs.length; i++) {
                args.push(rawArgs[i]);
            }
        }
    }
    
    public List getArgs() {
        return args;
    }
    
    private List parse(String command) {
        List tokens = new ArrayList();
        int state = INIT_STATE;
        StringBuffer token = new StringBuffer();
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            switch (state) {
                case INIT_STATE:
                    if (c == ' ') {
                        state = SKIPPING_SPACES_STATE;
                    } else if (c == '"') {
                        state = IN_DOUBLE_QUOTE_STATE;
                    } else if (c == '\'') {
                        state = IN_SINGLE_QUOTE_STATE;
                    } else {
                        token.append(c);
                        state = COLLECTING_ARG_STATE;
                    }
                    break;
                case SKIPPING_SPACES_STATE:
                    if (c == '"') {
                        state = IN_DOUBLE_QUOTE_STATE;
                    } else if (c == '\'') {
                        state = IN_SINGLE_QUOTE_STATE;
                    } else if (c != ' ') {
                        token.append(c);
                        state = COLLECTING_ARG_STATE;
                    }
                    break;
                case IN_DOUBLE_QUOTE_STATE:
                    if (c == '"') {
                        state = COLLECTING_ARG_STATE;
                    } else {
                        token.append(c);
                    }
                    break;
                case IN_SINGLE_QUOTE_STATE:
                    if (c == '\'') {
                        state = COLLECTING_ARG_STATE;
                    } else {
                        token.append(c);
                    }
                    break;
                case COLLECTING_ARG_STATE:
                    if (c == ' ') {
                        tokens.add(token.toString());
                        token = new StringBuffer();
                        state = SKIPPING_SPACES_STATE;
                    } else if (c == '"') {
                        state = IN_DOUBLE_QUOTE_STATE;
                    } else if (c == '\'') {
                        state = IN_SINGLE_QUOTE_STATE;
                    } else {
                        token.append(c);
                    } 
                    break;
                default:
                    throw new IllegalStateException("Unknown parsing state: " + state);
            }
        }
        if (state == COLLECTING_ARG_STATE && token.length() > 0) {
            tokens.add(token.toString());
        } else if (state == IN_DOUBLE_QUOTE_STATE || state == IN_SINGLE_QUOTE_STATE) {
            throw new IllegalArgumentException("Unterminated quote: " + command);
        }
        return tokens;
    }
}
