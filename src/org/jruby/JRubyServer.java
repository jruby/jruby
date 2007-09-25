/*
 ***** BEGIN LICENSE BLOCK *****
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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.List;
import java.util.ArrayList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class JRubyServer extends JRubyService {
    private Configuration conf;

    private boolean stillStarting = true;

    private JRubyServer(String[] args) throws Exception {
        conf = new Configuration(args[0]);
        if(conf.isDebug()) {
            System.err.println("Starting server with port " + conf.getPort() + " and key " + conf.getKey());
        }
        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(InetAddress.getLocalHost(),conf.getPort()));
        while(true) {
            Thread t1 = new Thread(new Handler(server.accept()));
            t1.setDaemon(true);
            t1.start();
        }
    }

    private class Handler implements Runnable {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader rr = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String command = rr.readLine();
                rr.close();
                this.socket.close();
                this.socket = null;
                if(conf.isDebug()) {
                    System.err.println("Got command: " + command);
                }
                String[] cmds = command.split(" ", 3);
                if(cmds[1].equals(conf.getKey())) {
                    if(cmds[0].equals(CMD_TERM)) {
                        if(conf.isDebug()) {
                            System.err.println("Terminating hard");
                        }
                        System.exit(0);
                    } else if(cmds[0].equals(CMD_NO_MORE)) {
                        if(conf.isDebug()) {
                            System.err.println("Accepting no more START");
                        }
                        stillStarting = false;
                    } else if(cmds[0].equals(CMD_START)) {
                        if(stillStarting) {
                            if(conf.isDebug()) {
                                System.err.println("Doing START on command " + cmds[2]);
                            }
                            new Main().run(intoCommandArguments(cmds[2].trim()));
                        } else {
                            if(conf.isDebug()) {
                                System.err.println("Not doing START anymore, invalid command");
                            }
                        }
                    } else {
                        if(conf.isDebug()) {
                            System.err.println("Unrecognized command");
                        }
                    }
                } else {
                    if(conf.isDebug()) {
                        System.err.println("Invalid key");
                    }
                }
            } catch(Exception e) {}
        }
    }

    protected static String[] intoCommandArguments(String str) {
        List<String> args = new ArrayList<String>();
        boolean inSingle = false;
        int contentStart = -1;

        for(int i=0,j=str.length();i<j;i++) {
            if(str.charAt(i) == ' ' && !inSingle && contentStart != -1) {
                args.add(str.substring(contentStart,i));
                contentStart = -1;
                continue;
            }
            if(str.charAt(i) == ' ') {
                continue;
            }
            if(str.charAt(i) == '\'' && !inSingle) {
                inSingle = true;
                contentStart = i+1;
                continue;
            }
            if(str.charAt(i) == '\'') {
                inSingle = false;
                args.add(str.substring(contentStart,i));
                contentStart = -1;
                continue;
            }
            if(contentStart == -1) {
                contentStart = i;
            }
        }
        if(contentStart != -1) {
            args.add(str.substring(contentStart));
        }
        return (String[])args.toArray(new String[0]);
    }

    public static void main(String[] args) throws Exception {
        new JRubyServer(args);        
    }
}// JRubyServer
