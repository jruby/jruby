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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import static java.lang.System.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jruby.Main;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ext.posix.util.FieldAccess;
import org.jruby.ext.posix.util.Platform;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ModeFlags;

/**
 * This mess of a class is what happens when all Java gives you is
 * Runtime.getRuntime().exec(). Thanks dude, that really helped.
 * @author nicksieger
 */
public class ShellLauncher {
    private static final boolean DEBUG = false;
    private static class ScriptThreadProcess extends Process implements Runnable {
        private final String[] argArray;
        private final String[] env;
        private final File pwd;
        private final boolean pipedStreams;
        private final PipedInputStream processOutput;
        private final PipedInputStream processError;
        private final PipedOutputStream processInput;
        
        private RubyInstanceConfig config;
        private Thread processThread;
        private int result;
        private Ruby parentRuntime;
        
        public ScriptThreadProcess(Ruby parentRuntime, final String[] argArray, final String[] env, final File dir) {
            this(parentRuntime, argArray, env, dir, true);
        }

        public ScriptThreadProcess(Ruby parentRuntime, final String[] argArray, final String[] env, final File dir, final boolean pipedStreams) {
            this.parentRuntime = parentRuntime;
            this.argArray = argArray;
            this.env = env;
            this.pwd = dir;
            this.pipedStreams = pipedStreams;
            if (pipedStreams) {
                processOutput = new PipedInputStream();
                processError = new PipedInputStream();
                processInput = new PipedOutputStream();
            } else {
                processOutput = processError = null;
                processInput = null;
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
                try {this.config.getInput().close();} catch (IOException ioe) {}
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
            config = new RubyInstanceConfig(parentRuntime.getInstanceConfig()) {{
                setEnvironment(environmentMap(env));
                setCurrentDirectory(pwd.toString());
            }};
            if (pipedStreams) {
                config.setInput(new PipedInputStream(processInput));
                config.setOutput(new PrintStream(new PipedOutputStream(processOutput)));
                config.setError(new PrintStream(new PipedOutputStream(processError)));
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
    

    private static String[] getCurrentEnv(Ruby runtime) {
        RubyHash hash = (RubyHash)runtime.getObject().fastGetConstant("ENV");
        String[] ret = new String[hash.size()];
        int i=0;

        for(Iterator iter = hash.directEntrySet().iterator();iter.hasNext();i++) {
            Map.Entry e = (Map.Entry)iter.next();
            ret[i] = e.getKey().toString() + "=" + e.getValue().toString();
        }

        return ret;
    }

    public static int runAndWait(Ruby runtime, IRubyObject[] rawArgs) {
        return runAndWait(runtime, rawArgs, runtime.getOutputStream());
    }

    public static long runWithoutWait(Ruby runtime, IRubyObject[] rawArgs) {
        return runWithoutWait(runtime, rawArgs, runtime.getOutputStream());
    }

    public static int execAndWait(Ruby runtime, IRubyObject[] rawArgs) {
        String[] args = parseCommandLine(runtime.getCurrentContext(), runtime, rawArgs);
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
                ScriptThreadProcess ipScript = new ScriptThreadProcess(runtime, newargs, getCurrentEnv(runtime), pwd, false);
                ipScript.start();
                
                return ipScript.waitFor();
            } catch (IOException e) {
                throw runtime.newIOErrorFromException(e);
            } catch (InterruptedException e) {
                throw runtime.newThreadError("unexpected interrupt");
            }
        } else {
            return runAndWait(runtime, rawArgs);
        }
    }

    public static int runAndWait(Ruby runtime, IRubyObject[] rawArgs, OutputStream output) {
        OutputStream error = runtime.getErrorStream();
        InputStream input = runtime.getInputStream();
        try {
            Process aProcess = run(runtime, rawArgs);
            handleStreams(aProcess,input,output,error);
            return aProcess.waitFor();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    public static long runWithoutWait(Ruby runtime, IRubyObject[] rawArgs, OutputStream output) {
        try {
            POpenProcess aProcess = new POpenProcess(popenShared(runtime, rawArgs), runtime);
            return getPidFromProcess(aProcess);
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    public static long getPidFromProcess(Process process) {
        if (process instanceof ScriptThreadProcess) {
            return process.hashCode();
        } else if (process instanceof POpenProcess) {
            return reflectPidFromProcess(((POpenProcess)process).getChild());
        } else {
            return reflectPidFromProcess(process);
        }
    }
    
    private static final Class UNIXProcess;
    private static final Field UNIXProcess_pid;
    private static final Class ProcessImpl;
    private static final Field ProcessImpl_handle;
    private interface PidGetter { public long getPid(Process process); }
    private static final PidGetter PID_GETTER;
    
    static {
        // default PidGetter
        PidGetter pg = new PidGetter() {
            public long getPid(Process process) {
                return process.hashCode();
            }
        };
        
        Class up = null;
        Field pid = null;
        try {
            up = Class.forName("java.lang.UNIXProcess");
            pid = up.getDeclaredField("pid");
            pid.setAccessible(true);
        } catch (Exception e) {
            // ignore and try windows version
        }
        UNIXProcess = up;
        UNIXProcess_pid = pid;

        Class pi = null;
        Field handle = null;
        try {
            pi = Class.forName("java.lang.ProcessImpl");
            handle = pi.getDeclaredField("handle");
            handle.setAccessible(true);
        } catch (Exception e) {
            // ignore and use hashcode
        }
        ProcessImpl = pi;
        ProcessImpl_handle = handle;

        if (UNIXProcess_pid != null) {
            if (ProcessImpl_handle != null) {
                // try both
                pg = new PidGetter() {
                    public long getPid(Process process) {
                        try {
                            if (UNIXProcess.isInstance(process)) {
                                return (Integer)UNIXProcess_pid.get(process);
                            } else if (ProcessImpl.isInstance(process)) {
                                return (Long)ProcessImpl_handle.get(process);
                            }
                        } catch (Exception e) {
                            // ignore and use hashcode
                        }
                        return process.hashCode();
                    }
                };
            } else {
                // just unix
                pg = new PidGetter() {
                    public long getPid(Process process) {
                        try {
                            if (UNIXProcess.isInstance(process)) {
                                return (Integer)UNIXProcess_pid.get(process);
                            }
                        } catch (Exception e) {
                            // ignore and use hashcode
                        }
                        return process.hashCode();
                    }
                };
            }
        } else if (ProcessImpl_handle != null) {
            // just windows
            pg = new PidGetter() {
                public long getPid(Process process) {
                    try {
                        if (ProcessImpl.isInstance(process)) {
                            return (Long)ProcessImpl_handle.get(process);
                        }
                    } catch (Exception e) {
                        // ignore and use hashcode
                    }
                    return process.hashCode();
                }
            };
        } else {
            // neither
            pg = new PidGetter() {
                public long getPid(Process process) {
                    return process.hashCode();
                }
            };
        }
        PID_GETTER = pg;
    }

    public static long reflectPidFromProcess(Process process) {
        return PID_GETTER.getPid(process);
    }

    public static Process run(Ruby runtime, IRubyObject string) throws IOException {
        return run(runtime, new IRubyObject[] {string});
    }

    public static POpenProcess popen(Ruby runtime, IRubyObject string, ModeFlags modes) throws IOException {
        return new POpenProcess(popenShared(runtime, new IRubyObject[] {string}), runtime, modes);
    }

    public static POpenProcess popen3(Ruby runtime, IRubyObject[] strings) throws IOException {
        return new POpenProcess(popenShared(runtime, strings), runtime);
    }
    
    private static Process popenShared(Ruby runtime, IRubyObject[] strings) throws IOException {
        String shell = getShell(runtime);
        Process childProcess = null;
        File pwd = new File(runtime.getCurrentDirectory());

        // CON: popen is a case where I think we should just always shell out.
        if (strings.length == 1) {
            // single string command, pass to sh to expand wildcards
            String[] argArray = new String[3];
            argArray[0] = shell;
            argArray[1] = shell.endsWith("sh") ? "-c" : "/c";
            argArray[2] = strings[0].asJavaString();
            childProcess = Runtime.getRuntime().exec(argArray, getCurrentEnv(runtime), pwd);
        } else {
            // direct invocation of the command
            String[] args = parseCommandLine(runtime.getCurrentContext(), runtime, strings);
            childProcess = Runtime.getRuntime().exec(args, getCurrentEnv(runtime), pwd);
        }
        
        return childProcess;
    }
    
    /**
     * Unwrap all filtering streams between the given stream and its actual
     * unfiltered stream. This is primarily to unwrap streams that have
     * buffers that would interfere with interactivity.
     * 
     * @param filteredStream The stream to unwrap
     * @return An unwrapped stream, presumably unbuffered
     */
    public static OutputStream unwrapBufferedStream(OutputStream filteredStream) {
        while (filteredStream instanceof FilterOutputStream) {
            try {
                filteredStream = (OutputStream)
                    FieldAccess.getProtectedFieldValue(FilterOutputStream.class,
                        "out", filteredStream);
            } catch (Exception e) {
                break; // break out if we've dug as deep as we can
            }
        }
        return filteredStream;
    }
    
    /**
     * Unwrap all filtering streams between the given stream and its actual
     * unfiltered stream. This is primarily to unwrap streams that have
     * buffers that would interfere with interactivity.
     * 
     * @param filteredStream The stream to unwrap
     * @return An unwrapped stream, presumably unbuffered
     */
    public static InputStream unwrapBufferedStream(InputStream filteredStream) {
        while (filteredStream instanceof FilterInputStream) {
            try {
                filteredStream = (InputStream)
                    FieldAccess.getProtectedFieldValue(FilterInputStream.class,
                        "in", filteredStream);
            } catch (Exception e) {
                break; // break out if we've dug as deep as we can
            }
        }
        return filteredStream;
    }
    
    public static class POpenProcess extends Process {
        private final Process child;
        private final Ruby runtime;
        private final ModeFlags modes;
        
        private InputStream input;
        private OutputStream output;
        private InputStream inerr;
        private FileChannel inputChannel;
        private FileChannel outputChannel;
        private FileChannel inerrChannel;
        private Pumper inputPumper;
        private Pumper inerrPumper;
        private Pumper outputPumper;
        
        public POpenProcess(Process child, Ruby runtime, ModeFlags modes) {
            this.child = child;
            this.runtime = runtime;
            this.modes = modes;
            
            if (modes.isWritable()) {
                prepareOutput(child);
            } else {
                // close process output
                // See JRUBY-3405; hooking up to parent process stdin caused
                // problems for IRB etc using stdin.
                try {child.getOutputStream().close();} catch (IOException ioe) {}
            }
            
            if (modes.isReadable()) {
                prepareInput(child);
            } else {
                pumpInput(child, runtime);
            }
            
            pumpInerr(child, runtime);
        }
        
        public POpenProcess(Process child, Ruby runtime) {
            this.child = child;
            this.runtime = runtime;
            this.modes = null;
            
            prepareOutput(child);
            prepareInput(child);
            prepareInerr(child);
        }

        @Override
        public OutputStream getOutputStream() {
            return output;
        }

        @Override
        public InputStream getInputStream() {
            return input;
        }

        @Override
        public InputStream getErrorStream() {
            return inerr;
        }
        
        public FileChannel getInput() {
            return inputChannel;
        }
        
        public FileChannel getOutput() {
            return outputChannel;
        }
        
        public FileChannel getError() {
            return inerrChannel;
        }

        public boolean hasOutput() {
            return output != null || outputChannel != null;
        }

        public Process getChild() {
            return child;
        }

        @Override
        public int waitFor() throws InterruptedException {
            if (outputPumper == null) {
                try {
                    if (output != null) output.close();
                } catch (IOException ioe) {
                    // ignore, we're on the way out
                }
            } else {
                outputPumper.quit();
            }
            
            int result = child.waitFor();
            
            return result;
        }

        @Override
        public int exitValue() {
            return child.exitValue();
        }

        @Override
        public void destroy() {
            try {
                if (input != null) input.close();
                if (inerr != null) inerr.close();
                if (output != null) output.close();
                if (inputChannel != null) inputChannel.close();
                if (inerrChannel != null) inerrChannel.close();
                if (outputChannel != null) outputChannel.close();
                
                // processes seem to have some peculiar locking sequences, so we
                // need to ensure nobody is trying to close/destroy while we are
                synchronized (this) {
                    if (inputPumper != null) synchronized(inputPumper) {inputPumper.quit();}
                    if (inerrPumper != null) synchronized(inerrPumper) {inerrPumper.quit();}
                    if (outputPumper != null) synchronized(outputPumper) {outputPumper.quit();}
                    child.destroy();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        private void prepareInput(Process child) {
            // popen callers wants to be able to read, provide subprocess in directly
            input = unwrapBufferedStream(child.getInputStream());
            if (input instanceof FileInputStream) {
                inputChannel = ((FileInputStream) input).getChannel();
            } else {
                inputChannel = null;
            }
            inputPumper = null;
        }

        private void prepareInerr(Process child) {
            // popen callers wants to be able to read, provide subprocess in directly
            inerr = unwrapBufferedStream(child.getErrorStream());
            if (inerr instanceof FileInputStream) {
                inerrChannel = ((FileInputStream) inerr).getChannel();
            } else {
                inerrChannel = null;
            }
            inerrPumper = null;
        }

        private void prepareOutput(Process child) {
            // popen caller wants to be able to write, provide subprocess out directly
            output = unwrapBufferedStream(child.getOutputStream());
            if (output instanceof FileOutputStream) {
                outputChannel = ((FileOutputStream) output).getChannel();
            } else {
                outputChannel = null;
            }
            outputPumper = null;
        }

        private void pumpInput(Process child, Ruby runtime) {
            // no read requested, hook up read to parents output
            InputStream childIn = unwrapBufferedStream(child.getInputStream());
            FileChannel childInChannel = null;
            if (childIn instanceof FileInputStream) {
                childInChannel = ((FileInputStream) childIn).getChannel();
            }
            OutputStream parentOut = unwrapBufferedStream(runtime.getOut());
            FileChannel parentOutChannel = null;
            if (parentOut instanceof FileOutputStream) {
                parentOutChannel = ((FileOutputStream) parentOut).getChannel();
            }
            if (childInChannel != null && parentOutChannel != null) {
                inputPumper = new ChannelPumper(childInChannel, parentOutChannel, Pumper.Slave.IN, this);
            } else {
                inputPumper = new StreamPumper(childIn, parentOut, false, Pumper.Slave.IN, this);
            }
            inputPumper.start();
            input = null;
            inputChannel = null;
        }

        private void pumpInerr(Process child, Ruby runtime) {
            // no read requested, hook up read to parents output
            InputStream childIn = unwrapBufferedStream(child.getErrorStream());
            FileChannel childInChannel = null;
            if (childIn instanceof FileInputStream) {
                childInChannel = ((FileInputStream) childIn).getChannel();
            }
            OutputStream parentOut = unwrapBufferedStream(runtime.getOut());
            FileChannel parentOutChannel = null;
            if (parentOut instanceof FileOutputStream) {
                parentOutChannel = ((FileOutputStream) parentOut).getChannel();
            }
            if (childInChannel != null && parentOutChannel != null) {
                inerrPumper = new ChannelPumper(childInChannel, parentOutChannel, Pumper.Slave.IN, this);
            } else {
                inerrPumper = new StreamPumper(childIn, parentOut, false, Pumper.Slave.IN, this);
            }
            inerrPumper.start();
            inerr = null;
            inerrChannel = null;
        }
    }
    
    public static Process run(Ruby runtime, IRubyObject[] rawArgs) throws IOException {
        String shell = getShell(runtime);
        Process aProcess = null;
        File pwd = new File(runtime.getCurrentDirectory());
        String[] args = parseCommandLine(runtime.getCurrentContext(), runtime, rawArgs);

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
            ScriptThreadProcess ipScript = new ScriptThreadProcess(runtime, newargs, getCurrentEnv(runtime), pwd);
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
            aProcess = Runtime.getRuntime().exec(argArray, getCurrentEnv(runtime), pwd);
        } else {
            aProcess = Runtime.getRuntime().exec(args, getCurrentEnv(runtime), pwd);        
        }
        return aProcess;
    }

    private interface Pumper extends Runnable {
        public enum Slave { IN, OUT };
        public void start();
        public void quit();
    }

    private static class StreamPumper extends Thread implements Pumper {
        private final InputStream in;
        private final OutputStream out;
        private final boolean onlyIfAvailable;
        private final Object waitLock = new Object();
        private final Object sync;
        private final Slave slave;
        private volatile boolean quit;
        
        StreamPumper(InputStream in, OutputStream out, boolean avail, Slave slave, Object sync) {
            this.in = in;
            this.out = out;
            this.onlyIfAvailable = avail;
            this.slave = slave;
            this.sync = sync;
            setDaemon(true);
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
                    synchronized (sync) {
                        // We need to close the out, since some
                        // processes would just wait for the stream
                        // to be closed before they process its content,
                        // and produce the output. E.g.: "cat".
                        if (slave == Slave.OUT) {
                            // we only close out if it's the slave stream, to avoid
                            // closing a directly-mapped stream from parent process
                            try { out.close(); } catch (IOException ioe) {}
                        }
                    }
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

    private static class ChannelPumper extends Thread implements Pumper {
        private final FileChannel inChannel;
        private final FileChannel outChannel;
        private final Slave slave;
        private final Object sync;
        private volatile boolean quit;
        
        ChannelPumper(FileChannel inChannel, FileChannel outChannel, Slave slave, Object sync) {
            if (DEBUG) out.println("using channel pumper");
            this.inChannel = inChannel;
            this.outChannel = outChannel;
            this.slave = slave;
            this.sync = sync;
            setDaemon(true);
        }
        @Override
        public void run() {
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            buf.clear();
            try {
                while (!quit && inChannel.isOpen() && outChannel.isOpen()) {
                    int read = inChannel.read(buf);
                    if (read == -1) break;
                    buf.flip();
                    outChannel.write(buf);
                    buf.clear();
                }
            } catch (Exception e) {
            } finally {
                // processes seem to have some peculiar locking sequences, so we
                // need to ensure nobody is trying to close/destroy while we are
                synchronized (sync) {
                    switch (slave) {
                    case OUT:
                        try { outChannel.close(); } catch (IOException ioe) {}
                        break;
                    case IN:
                        try { inChannel.close(); } catch (IOException ioe) {}
                    }
                }
            }
        }
        public void quit() {
            interrupt();
            this.quit = true;
        }
    }

    private static void handleStreams(Process p, InputStream in, OutputStream out, OutputStream err) throws IOException {
        InputStream pOut = p.getInputStream();
        InputStream pErr = p.getErrorStream();
        OutputStream pIn = p.getOutputStream();

        StreamPumper t1 = new StreamPumper(pOut, out, false, Pumper.Slave.IN, p);
        StreamPumper t2 = new StreamPumper(pErr, err, false, Pumper.Slave.IN, p);

        // The assumption here is that the 'in' stream provides
        // proper available() support. If available() always
        // returns 0, we'll hang!
        StreamPumper t3 = new StreamPumper(in, pIn, true, Pumper.Slave.OUT, p);

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

    private static String[] parseCommandLine(ThreadContext context, Ruby runtime, IRubyObject[] rawArgs) {
        String[] args;
        if (rawArgs.length == 1) {
            synchronized (runtime.getLoadService()) {
                runtime.getLoadService().require("jruby/path_helper");
            }
            RubyModule pathHelper = runtime.getClassFromPath("JRuby::PathHelper");
            RubyArray parts = (RubyArray) RuntimeHelpers.invoke(context, pathHelper, "smart_split_command", rawArgs);
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
    private static boolean shouldRunInProcess(Ruby runtime, String[] commands) {
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
                case '<': case '>': case '|': case ';':
                case '*': case '?': case '{': case '}':
                case '[': case ']': case '(': case ')':
                case '~': case '&': case '$': case '"':
                case '`': case '\n': case '\\': case '\'':
                    return false;
                case '2':
                    if(c.length() > 1 && c.charAt(1) == '>') {
                        return false;
                    }
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
    private static boolean shouldRunInShell(String shell, String[] args) {
        return !Platform.IS_WINDOWS ||
                (shell != null && args.length > 1 && !new File(args[0]).exists());
    }

    private static String getShell(Ruby runtime) {
        return runtime.evalScriptlet("require 'rbconfig'; Config::CONFIG['SHELL']").toString();
    }
}
