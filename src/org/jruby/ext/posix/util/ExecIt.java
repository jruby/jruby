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
 * Copyright (C) 2008 JRuby Community
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

package org.jruby.ext.posix.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jruby.ext.posix.POSIXHandler;

public class ExecIt {
    POSIXHandler handler;

    /** Creates a new instance of ShellLauncher */
    public ExecIt(POSIXHandler handler) {
        this.handler = handler;
    }

    public int runAndWait(String... args) throws IOException, InterruptedException {
        return runAndWait(handler.getOutputStream(), args);
    }

    public int runAndWait(OutputStream output, String... args) throws IOException, InterruptedException {
        Process process = run(args);
        
        handleStreams(process, handler.getInputStream(), output, handler.getErrorStream());
        
        return process.waitFor();
    }

    public Process run(String... args) throws IOException {
        File cwd = handler.getCurrentWorkingDirectory();
          
        return Runtime.getRuntime().exec(args, handler.getEnv(), cwd);        
    }

    private static class StreamPumper extends Thread {
        private InputStream in;
        private OutputStream out;
        private boolean onlyIfAvailable;
        private volatile boolean quit;
        private final Object waitLock = new Object();
        StreamPumper(InputStream in, OutputStream out, boolean avail) {
            this.in = in;
            this.out = out;
            this.onlyIfAvailable = avail;
        }
        
        public void run() {
            byte[] buf = new byte[1024];
            int numRead;
            boolean hasReadSomething = false;
            try {
                while (!quit) {
                    // The problem we trying to solve below: STDIN in Java is blocked and 
                    // non-interruptible, so if we invoke read on it, we might never be able to
                    // interrupt such thread.  So, we use in.available() to see if there is any 
                    // input ready, and only then read it. But this approach can't tell whether 
                    // the end of stream reached or not, so we might end up looping right at the
                    // end of the stream.  Well, at least, we can improve the situation by checking
                    // if some input was ever available, and if so, not checking for available 
                    // anymore, and just go to read.
                    if (onlyIfAvailable && !hasReadSomething) {
                        if (in.available() == 0) {
                            synchronized (waitLock) {
                                waitLock.wait(10);                                
                            }
                            continue;
                        } else {
                            hasReadSomething = true;
                        }
                    }

                    if ((numRead = in.read(buf)) == -1) {
                        break;
                    }
                    out.write(buf, 0, numRead);
                }
            } catch (Exception e) {
            } finally {
                if (onlyIfAvailable) {
                    // We need to close the out, since some
                    // processes would just wait for the stream
                    // to be closed before they process its content,
                    // and produce the output. E.g.: "cat".
                    try { out.close(); } catch (IOException ioe) {}
                }                
            }
        }
        
        public void quit() {
            this.quit = true;
            synchronized (waitLock) {
                waitLock.notify();                
            }
        }
    }

    private void handleStreams(Process p, InputStream in, OutputStream out, OutputStream err) throws IOException {
        InputStream pOut = p.getInputStream();
        InputStream pErr = p.getErrorStream();
        OutputStream pIn = p.getOutputStream();

        StreamPumper t1 = new StreamPumper(pOut, out, false);
        StreamPumper t2 = new StreamPumper(pErr, err, false);
        StreamPumper t3 = new StreamPumper(in, pIn, true);
        t1.start();
        t2.start();
        t3.start();

        try { t1.join(); } catch (InterruptedException ie) {}
        try { t2.join(); } catch (InterruptedException ie) {}
        t3.quit();

        try { err.flush(); } catch (IOException io) {}
        try { out.flush(); } catch (IOException io) {}

        try { pIn.close(); } catch (IOException io) {}
        try { pOut.close(); } catch (IOException io) {}
        try { pErr.close(); } catch (IOException io) {}

    }
}
