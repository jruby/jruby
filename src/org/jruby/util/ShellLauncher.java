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
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.Main;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.ext.posix.util.Platform;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This mess of a class is what happens when all Java gives you is
 * Runtime.getRuntime().exec(). Thanks dude, that really helped.
 * @author nicksieger
 */
public class ShellLauncher {
    private Ruby runtime;

    /** Creates a new instance of ShellLauncher */
    public ShellLauncher(Ruby runtime) {
        this.runtime = runtime;
    }
    
    private static class ScriptThreadProcess extends Process implements Runnable {
        private String[] argArray;
        private int result;
        private RubyInstanceConfig config;
        private Thread processThread;
        private PipedInputStream processOutput;
        private PipedInputStream processError;
        private PipedOutputStream processInput;
        private final String[] env;
        private final File pwd;
        private final boolean pipedStreams;

        public ScriptThreadProcess(final String[] argArray, final String[] env, final File dir) {
            this(argArray, env, dir, true);
        }

        public ScriptThreadProcess(final String[] argArray, final String[] env, final File dir, final boolean pipedStreams) {
            this.argArray = argArray;
            this.env = env;
            this.pwd = dir;
            this.pipedStreams = pipedStreams;
            if (pipedStreams) {
                processOutput = new PipedInputStream();
                processError = new PipedInputStream();
                processInput = new PipedOutputStream();
            }
        }
        public void run() {
            try {
                this.result = new Main(config).run(argArray);
            } catch (Throwable throwable) {
                throwable.printStackTrace(this.config.getError());
                this.result = -1;
            } finally {
                this.config.getOutput().close();
                this.config.getError().close();
            }
        }

        private Map<String, String> environmentMap(String[] env) {
            Map<String, String> m = new HashMap<String, String>();
            for (int i = 0; i < env.length; i++) {
                String[] kv = env[i].split("=", 2);
                m.put(kv[0], kv[1]);
            }
            return m;
        }

        public void start() throws IOException {
            this.config = new RubyInstanceConfig() {{
                setEnvironment(environmentMap(env));
                setCurrentDirectory(pwd.toString());
            }};
            if (pipedStreams) {
                this.config.setInput(new PipedInputStream(processInput));
                this.config.setOutput(new PrintStream(new PipedOutputStream(processOutput)));
                this.config.setError(new PrintStream(new PipedOutputStream(processError)));
            }
            String procName = "piped";
            if (argArray.length > 0) {
                procName = argArray[0];
            }
            processThread = new Thread(this, "ScriptThreadProcess: " + procName);
            processThread.setDaemon(true);
            processThread.start();
        }

        public OutputStream getOutputStream() {
            return processInput;
        }

        public InputStream getInputStream() {
            return processOutput;
        }

        public InputStream getErrorStream() {
            return processError;
        }

        public int waitFor() throws InterruptedException {
            processThread.join();
            return result;
        }

        public int exitValue() {
            return result;
        }

        public void destroy() {
            if (pipedStreams) {
                closeStreams();
            }
            processThread.interrupt();
        }

        private void closeStreams() {
            try { processInput.close(); } catch (IOException io) {}
            try { processOutput.close(); } catch (IOException io) {}
            try { processError.close(); } catch (IOException io) {}
        }
    }
    

    private String[] getCurrentEnv() {
        RubyHash hash = (RubyHash)runtime.getObject().fastGetConstant("ENV");
        String[] ret = new String[hash.size()];
        int i=0;

        for(Iterator iter = hash.directEntrySet().iterator();iter.hasNext();i++) {
            Map.Entry e = (Map.Entry)iter.next();
            ret[i] = e.getKey().toString() + "=" + e.getValue().toString();
        }

        return ret;
    }

    public int runAndWait(IRubyObject[] rawArgs) {
        return runAndWait(rawArgs, runtime.getOutputStream());
    }

    public int execAndWait(IRubyObject[] rawArgs) {
        String[] args = parseCommandLine(runtime, rawArgs);
        if (shouldRunInProcess(runtime, args)) {
            // exec needs to behave differently in-process, because it's technically
            // supposed to replace the calling process. So if we're supposed to run
            // in-process, we allow it to use the default streams and not use
            // pumpers at all. See JRUBY-2156 and JRUBY-2154.
            try {
                File pwd = new File(runtime.getCurrentDirectory());
                String command = args[0];
                // snip off ruby or jruby command from list of arguments
                // leave alone if the command is the name of a script
                int startIndex = command.endsWith(".rb") ? 0 : 1;
                if (command.trim().endsWith("irb")) {
                    startIndex = 0;
                    args[0] = runtime.getJRubyHome() + File.separator + "bin" + File.separator + "jirb";
                }
                String[] newargs = new String[args.length - startIndex];
                System.arraycopy(args, startIndex, newargs, 0, newargs.length);
                ScriptThreadProcess ipScript = new ScriptThreadProcess(newargs, getCurrentEnv(), pwd, false);
                ipScript.start();
                
                return ipScript.waitFor();
            } catch (IOException e) {
                throw runtime.newIOErrorFromException(e);
            } catch (InterruptedException e) {
                throw runtime.newThreadError("unexpected interrupt");
            }
        } else {
            return runAndWait(rawArgs);
        }
    }

    public int runAndWait(IRubyObject[] rawArgs, OutputStream output) {
        OutputStream error = runtime.getErrorStream();
        InputStream input = runtime.getInputStream();
        try {
            Process aProcess = run(rawArgs);
            handleStreams(aProcess,input,output,error);
            return aProcess.waitFor();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    public Process run(IRubyObject string) throws IOException {
        return run(new IRubyObject[] {string});
    }
    
    public Process run(IRubyObject[] rawArgs) throws IOException {
        String shell = getShell(runtime);
        Process aProcess = null;
        File pwd = new File(runtime.getCurrentDirectory());
        String[] args = parseCommandLine(runtime, rawArgs);

        if (shouldRunInProcess(runtime, args)) {
            String command = args[0];
            // snip off ruby or jruby command from list of arguments
            // leave alone if the command is the name of a script
            int startIndex = command.endsWith(".rb") ? 0 : 1;
            if (command.trim().endsWith("irb")) {
                startIndex = 0;
                args[0] = runtime.getJRubyHome() + File.separator + "bin" + File.separator + "jirb";
            }
            String[] newargs = new String[args.length - startIndex];
            System.arraycopy(args, startIndex, newargs, 0, newargs.length);
            ScriptThreadProcess ipScript = new ScriptThreadProcess(newargs, getCurrentEnv(), pwd);
            ipScript.start();
            aProcess = ipScript;
        } else if (rawArgs.length == 1 && shouldRunInShell(shell, args)) {
            // execute command with sh -c
            // this does shell expansion of wildcards
            String[] argArray = new String[3];
            String cmdline = rawArgs[0].toString();
            argArray[0] = shell;
            argArray[1] = shell.endsWith("sh") ? "-c" : "/c";
            argArray[2] = cmdline;
            aProcess = Runtime.getRuntime().exec(argArray, getCurrentEnv(), pwd);
        } else {
            aProcess = Runtime.getRuntime().exec(args, getCurrentEnv(), pwd);        
        }
        return aProcess;
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
        @Override
        public void run() {
            byte[] buf = new byte[1024];
            int numRead;
            boolean hasReadSomething = false;
            try {
                while (!quit) {
                    // The problem we trying to solve below: STDIN in Java
                    // is blocked and non-interruptible, so if we invoke read
                    // on it, we might never be able to interrupt such thread.
                    // So, we use in.available() to see if there is any input
                    // ready, and only then read it. But this approach can't
                    // tell whether the end of stream reached or not, so we
                    // might end up looping right at the end of the stream.
                    // Well, at least, we can improve the situation by checking
                    // if some input was ever available, and if so, not
                    // checking for available anymore, and just go to read.
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

        // The assumption here is that the 'in' stream provides
        // proper available() support. If available() always
        // returns 0, we'll hang!
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

        // Force t3 to quit, just in case if it's stuck.
        // Note: On some platforms, even interrupt might not
        // have an effect if the thread is IO blocked.
        try { t3.interrupt(); } catch (SecurityException se) {}
    }

    private String[] parseCommandLine(Ruby runtime, IRubyObject[] rawArgs) {
        String[] args;
        if (rawArgs.length == 1) {
            RubyArray parts = (RubyArray) runtime.evalScriptlet(
                "require 'jruby/path_helper'; JRuby::PathHelper"
                ).callMethod(runtime.getCurrentContext(),
                "smart_split_command", rawArgs);
            args = new String[parts.getLength()];
            for (int i = 0; i < parts.getLength(); i++) {
                args[i] = parts.entry(i).toString();
            }
        } else {
            args = new String[rawArgs.length];
            for (int i = 0; i < rawArgs.length; i++) {
                args[i] = rawArgs[i].toString();
            }
        }
        return args;
    }

    /**
     * Only run an in-process script if the script name has "ruby", ".rb", or "irb" in the name
     */
    private boolean shouldRunInProcess(Ruby runtime, String[] commands) {
        if (!runtime.getInstanceConfig().isRunRubyInProcess()) {
            return false;
        }

        // Check for special shell characters [<>|] at the beginning
        // and end of each command word and don't run in process if we find them.
        for (int i = 0; i < commands.length; i++) {
            String c = commands[i];
            if (c.trim().length() == 0) {
                continue;
            }
            char[] firstLast = new char[] {c.charAt(0), c.charAt(c.length()-1)};
            for (int j = 0; j < firstLast.length; j++) {
                switch (firstLast[j]) {
                case '<': case '>': case '|':
                    return false;
                }
            }
        }

        String command = commands[0];
        String[] slashDelimitedTokens = command.split("/");
        String finalToken = slashDelimitedTokens[slashDelimitedTokens.length - 1];
        int indexOfRuby = finalToken.indexOf("ruby");
        return ((indexOfRuby != -1 && indexOfRuby == (finalToken.length() - 4))
                || finalToken.endsWith(".rb")
                || finalToken.endsWith("irb"));
    }

    /**
     * This hack is to work around a problem with cmd.exe on windows where it can't
     * interpret a filename with spaces in the first argument position as a command.
     * In that case it's better to try passing the bare arguments to runtime.exec.
     * On all other platforms we'll always run the command in the shell.
     */
    private boolean shouldRunInShell(String shell, String[] args) {
        return !Platform.IS_WINDOWS ||
                (shell != null && args.length > 1 && !new File(args[0]).exists());
    }

    private String getShell(Ruby runtime) {
        return runtime.evalScriptlet("require 'rbconfig'; Config::CONFIG['SHELL']").toString();
    }
}
