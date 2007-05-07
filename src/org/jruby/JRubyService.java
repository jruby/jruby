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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jruby;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class JRubyService {
    protected static class Configuration {
        private final static int DEFAULT_PORT = 19222;

        private String key;
        private int port = DEFAULT_PORT;
        private boolean terminate;
        private boolean noMore;
        private boolean debug;
        private String command;

        public Configuration(String args) {
            int i=0;
            int stop;
            loop: for(int j=args.length();i<j;i++) {
                if(args.charAt(i) == '-' && i+1 < j) {
                    switch(args.charAt(++i)) {
                    case 'k':
                        stop = args.indexOf(" ", (++i) + 1);
                        if(stop == -1) {
                            stop = args.length();
                        }
                        key = args.substring(i, stop).trim();
                        i = stop;
                        break;
                    case 'p':
                        stop = args.indexOf(" ", (++i) + 1);
                        if(stop == -1) {
                            stop = args.length();
                        }
                        port = Integer.parseInt(args.substring(i, stop).trim());
                        i = stop;
                        break;
                    case 't':
                        terminate = true;
                        i++;
                        break;
                    case 'n':
                        noMore = true;
                        i++;
                        break;
                    case 'd':
                        debug = true;
                        i++;
                        break;
                    case '-': // handle everything after -- as arguments to the jruby process
                        i++;
                        break loop;
                    default:
                        i--;
                        break loop;
                    }                    
                } else if(args.charAt(i) != ' ') {
                    break loop;
                }
            }
            if(i<args.length()) {
                command = args.substring(i).trim();
            }
        }
        
        public String getKey() {
            return key;
        }

        public int getPort() {
            return port;
        }

        public boolean terminate() {
            return terminate;
        }

        public boolean noMore() {
            return noMore;
        }

        public boolean isDebug() {
            return debug;
        }

        public String getCommand() {
            return command;
        }
    }

    public static final String CMD_START = "START";
    public static final String CMD_NO_MORE = "NO_MORE";
    public static final String CMD_TERM = "TERM";
}// JRubyService
