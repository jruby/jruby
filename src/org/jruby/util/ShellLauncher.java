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
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jruby.Main;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author nicksieger
 */
public class ShellLauncher {

    private static final Pattern PATH_SEPARATORS = Pattern.compile("[/\\\\]");

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
        private PipedInputStream processOutput = new PipedInputStream();
        private PipedInputStream processError = new PipedInputStream();
        private PipedOutputStream processInput = new PipedOutputStream();
        private final String[] env;
        private final File pwd;

        public ScriptThreadProcess(final String[] argArray, final String[] env, final File dir) {
            this.argArray = argArray;
            this.env = env;
            this.pwd = dir;
        }
        
        public void run() {
            this.result = new Main(config).run(argArray);
            this.config.getOutput().close();
            this.config.getError().close();
        }

        private Map environmentMap(String[] env) {
            Map m = new HashMap();
            for (int i = 0; i < env.length; i++) {
                String[] kv = env[i].split("=", 2);
                m.put(kv[0], kv[1]);
            }
            return m;
        }

        public void start() throws IOException {
            this.config = new RubyInstanceConfig() {{
                setInput(new PipedInputStream(processInput));
                setOutput(new PrintStream(new PipedOutputStream(processOutput)));
                setError(new PrintStream(new PipedOutputStream(processError)));
                setEnvironment(environmentMap(env));
                setCurrentDirectory(pwd.toString());
            }};
            processThread = new Thread(this, "ScriptThreadProcess: " + argArray[0]);
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
            closeStreams();
            processThread.join();
            return result;
        }

        public int exitValue() {
            return result;
        }

        public void destroy() {
            closeStreams();
            processThread.interrupt();
        }

        private void closeStreams() {
            try { processInput.close(); } catch (IOException io) {}
            try { processOutput.close(); } catch (IOException io) {}
            try { processError.close(); } catch (IOException io) {}
        }
    }

    private String[] getCurrentEnv() {
        RubyHash hash = (RubyHash)runtime.getObject().getConstant("ENV");
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
        String shell = runtime.evalScript("require 'rbconfig'; Config::CONFIG['SHELL']").toString();
        rawArgs[0] = runtime.newString(repairDirSeps(rawArgs[0].toString()));
        Process aProcess = null;
        File pwd = new File(runtime.getCurrentDirectory());

        if (shouldRunInProcess(rawArgs[0].toString())) {
            List args = parseCommandLine(rawArgs);
            String command = (String)args.get(0);

            // snip off ruby or jruby command from list of arguments
            // leave alone if the command is the name of a script
            int startIndex = command.endsWith(".rb") ? 0 : 1;
            if(command.trim().endsWith("irb")) {
                startIndex = 0;
                args.set(0,runtime.getJRubyHome() + File.separator + "bin" + File.separator + "jirb");
            }
            String[] argArray = (String[])args.subList(startIndex,args.size()).toArray(new String[0]);
            ScriptThreadProcess ipScript = new ScriptThreadProcess(argArray, getCurrentEnv(), pwd);
            ipScript.start();
            aProcess = ipScript;
        } else if (shouldRunInShell(shell, rawArgs)) {
            // execute command with sh -c or cmd.exe /c
            // this does shell expansion of wildcards
            String shellSwitch = shell.endsWith("sh") ? "-c" : "/c";
            String[] argArray = new String[3];
            argArray[0] = shell;
            argArray[1] = shellSwitch;
            argArray[2] = rawArgs[0].toString();
            aProcess = Runtime.getRuntime().exec(argArray, getCurrentEnv(), pwd);
        } else {
            // execute command directly, no wildcard expansion
            if (rawArgs.length > 1) {
                String[] argArray = new String[rawArgs.length];
                for (int i=0;i<rawArgs.length;i++) {
                    argArray[i] = rawArgs[i].toString();
                }
                aProcess = Runtime.getRuntime().exec(argArray,getCurrentEnv(), pwd);
            } else {
                aProcess = Runtime.getRuntime().exec(rawArgs[0].toString(), getCurrentEnv(), pwd);
            }
        }
        return aProcess;
    }

    private static class StreamCopier extends Thread {
        private InputStream in;
        private OutputStream out;
        private boolean onlyIfAvailable;
        private boolean quit;
        StreamCopier(InputStream in, OutputStream out, boolean avail) {
            this.in = in;
            this.out = out;
            this.onlyIfAvailable = avail;
        }
        public void run() {
            byte[] buf = new byte[128];
            int numRead;
            try {
                while (true) {
                    if (quit) {
                        break;
                    }
                    Thread.sleep(10);
                    if (onlyIfAvailable && in.available() == 0) {
                        continue;
                    }
                    if ((numRead = in.read(buf)) == -1) {
                        break;
                    }
                    out.write(buf, 0, numRead);
                }
            } catch (Exception e) {
            }
        }
        public void quit() {
            this.quit = true;
        }
    }

    private void handleStreams(Process p, InputStream in, OutputStream out, OutputStream err) throws IOException {
        InputStream pOut = p.getInputStream();
        InputStream pErr = p.getErrorStream();
        OutputStream pIn = p.getOutputStream();

        StreamCopier t1 = new StreamCopier(pOut, out, false);
        StreamCopier t2 = new StreamCopier(pErr, err, false);
        StreamCopier t3 = new StreamCopier(in, pIn, true);
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

    /**
     * For the first full token on the command, most likely the actual executable to run, replace
     * all dir separators with that which is appropriate for the current platform. Return the new
     * with this executable string at the beginning.
     *
     * @param command The all-forward-slashes command to be "fixed"
     * @return The "fixed" full command line
     */
    private String repairDirSeps(String command) {
        String executable = "", remainder = "";
        command = command.trim();
        if (command.startsWith("'")) {
            String [] tokens = command.split("'", 3);
            executable = "'"+tokens[1]+"'";
            if (tokens.length > 2)
                remainder = tokens[2];
        } else if (command.startsWith("\"")) {
            String [] tokens = command.split("\"", 3);
            executable = "\""+tokens[1]+"\"";
            if (tokens.length > 2)
                remainder = tokens[2];
        } else {
            String [] tokens = command.split(" ", 2);
            executable = tokens[0];
            if (tokens.length > 1)
                remainder = " "+tokens[1];
        }

        // Matcher.replaceAll treats backslashes in the replacement string as escaped characters
        String replacement = File.separator;
        if (File.separatorChar == '\\')
            replacement = "\\\\";

        return PATH_SEPARATORS.matcher(executable).replaceAll(replacement) + remainder;
    }

    private List parseCommandLine(IRubyObject[] rawArgs) {
        String[] args = new String[rawArgs.length];
        for (int i = 0; i < rawArgs.length; i ++) {
            args[i] = rawArgs[i].toString();
        }
        return new RawArgParser(args).getArgs();
    }

    /**
     * Only run an in-process script if the script name has "ruby", ".rb", or "irb" in the name
     */
    private boolean shouldRunInProcess(String command) {
        command = command.trim();
        String [] spaceDelimitedTokens = command.split(" ", 2);
        String [] slashDelimitedTokens = spaceDelimitedTokens[0].split("/");
        String finalToken = slashDelimitedTokens[slashDelimitedTokens.length-1];
        return (finalToken.indexOf("ruby") != -1 || finalToken.endsWith(".rb") || finalToken.endsWith("irb"));
    }

    private boolean shouldRunInShell(String shell, IRubyObject[] rawArgs) {
        return shell != null && rawArgs.length == 1 && rawArgs[0].toString().indexOf(" ") >= 0;
    }
}
