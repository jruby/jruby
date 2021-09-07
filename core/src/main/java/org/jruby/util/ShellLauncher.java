/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007-2011 JRuby Team <team@jruby.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import static com.headius.backport9.buffer.Buffers.clearBuffer;
import static com.headius.backport9.buffer.Buffers.flipBuffer;
import static java.lang.System.out;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Main;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import jnr.posix.util.Platform;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Helpers;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.io.ChannelHelper;
import org.jruby.util.io.IOOptions;
import org.jruby.util.io.ModeFlags;

/**
 * This mess of a class is what happens when all Java gives you is
 * Runtime.getRuntime().exec(). Thanks dude, that really helped.
 * @author nicksieger
 */
@SuppressWarnings("deprecation")
public class ShellLauncher {
    private static final boolean DEBUG = false;

    private static final String PATH_ENV = "PATH";

    // from MRI -- note the unixy file separators
    private static final String[] DEFAULT_PATH =
        { "/usr/local/bin", "/usr/ucb", "/usr/bin", "/bin" };

    private static final String[] WINDOWS_EXE_SUFFIXES =
        { ".exe", ".com", ".bat", ".cmd" }; // the order is important

    private static final String[] WINDOWS_INTERNAL_CMDS = {
        "assoc", "break", "call", "cd", "chcp",
        "chdir", "cls", "color", "copy", "ctty", "date", "del", "dir", "echo", "endlocal",
        "erase", "exit", "for", "ftype", "goto", "if", "lfnfor", "lh", "lock", "md", "mkdir",
        "move", "path", "pause", "popd", "prompt", "pushd", "rd", "rem", "ren", "rename",
        "rmdir", "set", "setlocal", "shift", "start", "time", "title", "truename", "type",
        "unlock", "ver", "verify", "vol", };

    // TODO: better check is needed, with quoting/escaping
    private static final Pattern SHELL_METACHARACTER_PATTERN =
        Pattern.compile("[*?{}\\[\\]<>()~&|$;'`\\\\\"\\n]");

    private static final Pattern WIN_ENVVAR_PATTERN = Pattern.compile("%\\w+%");

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
        private final Ruby parentRuntime;

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
                this.result = (new Main(config).run(argArray)).getStatus();
            } catch (Throwable throwable) {
                throwable.printStackTrace(this.config.getError());
                this.result = -1;
            } finally {
                this.config.getOutput().close();
                this.config.getError().close();
                try {this.config.getInput().close();} catch (IOException ioe) {}
            }
        }

        private static Map<String, String> environmentMap(String[] env) {
            Map<String, String> map = new HashMap<>(env.length + 2, 1);
            for (int i = 0; i < env.length; i++) {
                List<String> kv = StringSupport.split(env[i], '=', 2);
                map.put(kv.get(0), kv.get(1));
            }
            return map;
        }

        public void start() throws IOException {
            config = new RubyInstanceConfig(parentRuntime.getInstanceConfig());

            config.setCurrentDirectory(pwd.toString());
            config.setEnvironment(environmentMap(env));

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

    public static String[] getCurrentEnv(Ruby runtime) {
        return getModifiedEnv(runtime, Collections.EMPTY_LIST, false);
    }

    private static String[] getCurrentEnv(Ruby runtime, Map mergeEnv) {
        // TODO: ensure nobody passes null
        return getModifiedEnv(runtime, mergeEnv == null ? Collections.EMPTY_LIST : mergeEnv.entrySet(), false);
    }

    public static String[] getModifiedEnv(Ruby runtime, Collection mergeEnv, boolean clearEnv) {
        ThreadContext context = runtime.getCurrentContext();

        // disable tracing for the dup call below
        boolean traceEnabled = context.isEventHooksEnabled();
        context.setEventHooksEnabled(false);

        try {
            // dup for JRUBY-6603 (avoid concurrent modification while we walk it)
            RubyHash hash = null;
            if (clearEnv) {
                hash = RubyHash.newHash(runtime);
            } else {
                hash = (RubyHash) runtime.getObject().getConstant("ENV").dup();
            }

            if (mergeEnv != null) {
                if (mergeEnv instanceof Set) {
                    for (Map.Entry e : (Set<Map.Entry>)mergeEnv) {
                        // if the key is nil, raise TypeError
                        Object key = e.getKey();
                        if (key == null) {
                            throw runtime.newTypeError(runtime.getNil(), runtime.getStructClass());
                        }
                        // ignore if the value is nil
                        Object value = e.getValue();
                        if (value == null) {
                            hash.remove(key.toString());
                            continue;
                        }
                        hash.put(key.toString(), value.toString());
                    }
                } else if (mergeEnv instanceof RubyArray) {
                    for (int j = 0; j < mergeEnv.size(); j++) {
                        RubyArray e = ((RubyArray)mergeEnv).eltOk(j).convertToArray();
                        // if there are not two elements, raise ArgumentError
                        if (e.size() != 2) {
                            throw runtime.newArgumentError("env assignments must come in pairs");
                        }

                        // if the key is nil, raise TypeError
                        IRubyObject key = e.eltOk(0);
                        if (key == null || key.isNil()) {
                            throw runtime.newTypeError(runtime.getNil(), runtime.getStructClass());
                        }

                        // ignore if the value is nil
                        IRubyObject value = e.eltOk(1);
                        if (value == null || value.isNil()) {
                            hash.remove(key.toString());
                            continue;
                        }

                        hash.put(key.toString(), value.toString());
                    }
                }
            }

            String[] ret = new String[hash.size()];

            int i = 0;
            for (Map.Entry<String, String> e : (Set<Map.Entry<String, String>>)hash.entrySet()) {
                // if the key is nil, raise TypeError
                if (e.getKey() == null) {
                    throw runtime.newTypeError(runtime.getNil(), runtime.getStructClass());
                }
                // ignore if the value is nil
                if (e.getValue() == null) {
                    continue;
                }
                ret[i] = e.getKey() + '=' + e.getValue();
                i++;
            }

            return arrayOfLength(ret, i);

        } finally {
            context.setEventHooksEnabled(traceEnabled);
        }
    }

    private static String[] arrayOfLength(final String[] ary, final int len) {
        return len == ary.length ? ary : Arrays.copyOf(ary, len);
    }

    private static boolean filenameIsPathSearchable(String fname, boolean forExec) {
        if (fname.startsWith("/")   ||
            fname.startsWith("./")  ||
            fname.startsWith("../") ||
            (forExec && (fname.indexOf('/') != -1))) {
            return false;
        }
        if (Platform.IS_WINDOWS) {
            if (fname.startsWith("\\")  ||
                fname.startsWith(".\\") ||
                fname.startsWith("..\\") ||
                ((fname.length() > 2) && fname.charAt(1) == ':') ||
                (forExec && (fname.indexOf('\\') != -1))) {
                return false;
            }
        }
        return true;
    }

    private static File tryFile(Ruby runtime, String fdir, String fname) {
        File pathFile;
        if (fdir == null) {
            pathFile = new File(fname);
        } else {
            pathFile = new File(fdir, fname);
        }

        if (!pathFile.isAbsolute()) {
            pathFile = new File(runtime.getCurrentDirectory(), pathFile.getPath());
        }

        log(runtime, "Trying file " + pathFile);
        if (pathFile.exists()) return pathFile;

        return null;
    }

    private static boolean withExeSuffix(String fname) {
        String lowerCaseFname = fname.toLowerCase();
        for (String suffix : WINDOWS_EXE_SUFFIXES) {
            if (lowerCaseFname.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static File isValidFile(Ruby runtime, String fdir, String fname, boolean isExec) {
        File validFile = null;
        if (isExec && Platform.IS_WINDOWS) {
            if (withExeSuffix(fname)) {
                validFile = tryFile(runtime, fdir, fname);
            } else {
                for (String suffix: WINDOWS_EXE_SUFFIXES) {
                    validFile = tryFile(runtime, fdir, fname + suffix);
                    if (validFile != null) {
                        // found a valid file, no need to search further
                        break;
                    }
                }
            }
        } else {
            validFile = tryFile(runtime, fdir, fname);
            if (validFile != null) {
                if (validFile.isDirectory()) {
                    return null;
                }
                if (isExec && !runtime.getPosix().stat(validFile.getAbsolutePath()).isExecutable()) {
                    throw runtime.newErrnoEACCESError(validFile.getAbsolutePath());
                }
            }
        }
        return validFile;
    }

    private static File isValidFile(Ruby runtime, String fname, boolean isExec) {
        String fdir = null;
        return isValidFile(runtime, fdir, fname, isExec);
    }

    private static File findPathFile(Ruby runtime, String fname, String[] path, boolean isExec) {
        File pathFile = null;
        if (Platform.IS_WINDOWS && fname.startsWith("\"") && fname.endsWith("\"")) {
            fname = fname.substring(1, fname.length() - 1); // remove double quotes if present
        }
        boolean doPathSearch = filenameIsPathSearchable(fname, isExec);
        if (doPathSearch) {
            for (String fdir: path) {
                // NOTE: Jruby's handling of tildes is more complete than
                //       MRI's, which can't handle user names after the tilde
                //       when searching the executable path
                try {
                    pathFile = isValidFile(runtime, fdir, fname, isExec);
                    if (pathFile != null) {
                        break;
                    }
                } catch (SecurityException se) {
                    // Security prevented accessing this PATH entry, proceed to next
                    continue;
                }
            }
        } else {
            pathFile = isValidFile(runtime, fname, isExec);
        }
        return pathFile;
    }

    public static File findPathExecutable(Ruby runtime, String fname) {
        RubyHash env = (RubyHash) runtime.getObject().getConstant("ENV");
        IRubyObject pathObject = env.op_aref(runtime.getCurrentContext(), RubyString.newString(runtime, PATH_ENV));
        return findPathExecutable(runtime, fname, pathObject);
    }

    // MRI: Hopefully close to dln_find_exe_r used by popen logic
    public static File findPathExecutable(Ruby runtime, String fname, IRubyObject pathObject) {
        String[] pathNodes;

        if (pathObject == null || pathObject.isNil()) {
            RubyHash env = (RubyHash) runtime.getObject().getConstant("ENV");
            pathObject = env.op_aref(runtime.getCurrentContext(), RubyString.newString(runtime, PATH_ENV));
        }

        if (pathObject == null) {
            pathNodes = DEFAULT_PATH; // ASSUME: not modified by callee
        }

        else {
            String pathSeparator = System.getProperty("path.separator");
            String path = pathObject.toString();
            if (Platform.IS_WINDOWS) {
                // Windows-specific behavior
                path = "." + pathSeparator + path;
            }
            pathNodes = path.split(pathSeparator);
        }
        return findPathFile(runtime, fname, pathNodes, true);
    }

    public static int runAndWait(Ruby runtime, IRubyObject[] rawArgs) {
        return runAndWait(runtime, rawArgs, runtime.getOutputStream());
    }

    public static long[] runAndWaitPid(Ruby runtime, IRubyObject[] rawArgs) {
        return runAndWaitPid(runtime, rawArgs, runtime.getOutputStream(), true);
    }

    public static long runWithoutWait(Ruby runtime, IRubyObject[] rawArgs) {
        return runWithoutWait(runtime, rawArgs, runtime.getOutputStream());
    }

    public static int runExternalAndWait(Ruby runtime, IRubyObject[] rawArgs, Map mergeEnv) {
        OutputStream output = runtime.getOutputStream();
        OutputStream error = runtime.getErrorStream();
        InputStream input = runtime.getInputStream();

        File pwd = new File(runtime.getCurrentDirectory());
        LaunchConfig cfg = new LaunchConfig(runtime, rawArgs, true);

        try {
            Process process;
            try {
                if (cfg.shouldRunInShell()) {
                    log(runtime, "Launching with shell");
                    // execute command with sh -c ... this does shell expansion of wildcards
                    cfg.verifyExecutableForShell();
                    process = buildProcess(runtime, cfg.getExecArgs(), getCurrentEnv(runtime, mergeEnv), pwd);
                } else {
                    log(runtime, "Launching directly (no shell)");
                    cfg.verifyExecutableForDirect();
                }

                final String[] execArgs = cfg.getExecArgs();
                if (changeDirInsideJar(runtime, execArgs)) {
                    pwd = new File(System.getProperty("user.dir"));
                }
                process = buildProcess(runtime, execArgs, getCurrentEnv(runtime, mergeEnv), pwd);
            } catch (SecurityException se) {
                throw runtime.newSecurityError(se.getLocalizedMessage());
            }
            handleStreams(runtime, process, input, output, error);
            return process.waitFor();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    public static long runExternalWithoutWait(Ruby runtime, IRubyObject env, IRubyObject prog, IRubyObject options, IRubyObject args) {
        return runExternal(runtime, env, prog, options, args, false);
    }

    public static long runExternal(Ruby runtime, IRubyObject env, IRubyObject prog, IRubyObject options, IRubyObject args, boolean wait) {
        if (env.isNil() || !(env instanceof Map)) {
            env = null;
        }

        IRubyObject[] rawArgs = args.convertToArray().toJavaArray();

        OutputStream output = runtime.getOutputStream();
        OutputStream error = runtime.getErrorStream();
        InputStream input = runtime.getInputStream();

        File pwd = new File(runtime.getCurrentDirectory());
        LaunchConfig cfg = new LaunchConfig(runtime, rawArgs, true);

        try {
            Process process;
            try {
                if (cfg.shouldRunInShell()) {
                    log(runtime, "Launching with shell");
                    // execute command with sh -c
                    // this does shell expansion of wildcards
                    cfg.verifyExecutableForShell();
                } else {
                    log(runtime, "Launching directly (no shell)");
                    cfg.verifyExecutableForDirect();
                }

                final String[] execArgs = cfg.getExecArgs();
                if (changeDirInsideJar(runtime, execArgs)) {
                    pwd = new File(".");
                }
                process = buildProcess(runtime, execArgs, getCurrentEnv(runtime, (Map) env), pwd);
            } catch (SecurityException se) {
                throw runtime.newSecurityError(se.getLocalizedMessage());
            }

            if (wait) {
                handleStreams(runtime, process, input, output, error);
                try {
                    return process.waitFor();
                } catch (InterruptedException e) {
                    throw runtime.newThreadError("unexpected interrupt");
                }
            } else {
                handleStreamsNonblocking(runtime, process, runtime.getOutputStream(), error);
                return getPidFromProcess(process);
            }
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    private static boolean changeDirInsideJar(final Ruby runtime, final String[] args) {
        final String arg;
        if ((arg = changeDirInsideJar(runtime, args[args.length - 1])) != null) {
            args[args.length - 1] = arg;
            return true;
        }
        return false;
    }

    public static String changeDirInsideJar(final Ruby runtime, final String arg) {
        // only if inside a jar and spawning org.jruby.Main we change to the current directory inside the jar
        if (runtime.getCurrentDirectory().startsWith("uri:classloader:") && arg.contains("org.jruby.Main")) {
            return StringSupport.replaceFirst(arg, "org.jruby.Main", "org.jruby.Main -C " + runtime.getCurrentDirectory()).toString();
        }
        return null;
    }

    public static Process buildProcess(Ruby runtime, String[] args, String[] env, File pwd) throws IOException {
        return runtime.getPosix().newProcessMaker(args)
                .environment(env)
                .directory(pwd)
                .start();
    }

    public static long runExternalWithoutWait(Ruby runtime, IRubyObject[] rawArgs) {
        return runWithoutWait(runtime, rawArgs, runtime.getOutputStream());
    }

    public static int execAndWait(Ruby runtime, IRubyObject[] rawArgs) {
        return execAndWait(runtime, rawArgs, Collections.EMPTY_MAP);
    }

    public static int execAndWait(Ruby runtime, IRubyObject[] rawArgs, Map mergeEnv) {
        File pwd = new File(runtime.getCurrentDirectory());
        LaunchConfig cfg = new LaunchConfig(runtime, rawArgs, true);

        if (cfg.shouldRunInProcess()) {
            log(runtime, "ExecAndWait in-process");
            try {
                // exec needs to behave differently in-process, because it's technically
                // supposed to replace the calling process. So if we're supposed to run
                // in-process, we allow it to use the default streams and not use
                // pumpers at all. See JRUBY-2156 and JRUBY-2154.
                ScriptThreadProcess ipScript = new ScriptThreadProcess(
                        runtime, cfg.getExecArgs(), getCurrentEnv(runtime, mergeEnv), pwd, false);
                ipScript.start();
                return ipScript.waitFor();
            } catch (IOException e) {
                throw runtime.newIOErrorFromException(e);
            } catch (InterruptedException e) {
                throw runtime.newThreadError("unexpected interrupt");
            }
        } else {
            return runExternalAndWait(runtime, rawArgs, mergeEnv);
        }
    }

    public static int runAndWait(Ruby runtime, IRubyObject[] rawArgs, OutputStream output) {
        return runAndWait(runtime, rawArgs, output, true);
    }

    public static int runAndWait(Ruby runtime, IRubyObject[] rawArgs, OutputStream output, boolean doExecutableSearch) {
        return (int)runAndWaitPid(runtime, rawArgs, output, doExecutableSearch)[0];
    }

    public static long[] runAndWaitPid(Ruby runtime, IRubyObject[] rawArgs, OutputStream output, boolean doExecutableSearch) {
        OutputStream error = runtime.getErrorStream();
        InputStream input = runtime.getInputStream();
        try {
            Process aProcess = run(runtime, rawArgs, doExecutableSearch);
            handleStreams(runtime, aProcess, input, output, error);
            return new long[] {aProcess.waitFor(), getPidFromProcess(aProcess)};
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    private static long runWithoutWait(Ruby runtime, IRubyObject[] rawArgs, OutputStream output) {
        OutputStream error = runtime.getErrorStream();
        try {
            Process aProcess = run(runtime, rawArgs, true);
            handleStreamsNonblocking(runtime, aProcess, output, error);
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
    private interface PidGetter { long getPid(Process process); }
    private static final PidGetter PID_GETTER;

    static {
        PidGetter pidGetter;

        Class up = null;
        Field pid = null;
        try {
            up = Class.forName("java.lang.UNIXProcess");
        } catch (ClassNotFoundException e) {
            try {
                // Renamed in 11 (or earlier)
                up = Class.forName("java.lang.ProcessImpl");
            } catch (ClassNotFoundException e2) {
                // ignore and try windows version
            }
        }

        if (up != null) {
            try {
                pid = up.getDeclaredField("pid");
                Java.trySetAccessible(pid);
            } catch (NoSuchFieldException | SecurityException e) {
                // ignore and try windows version
            }
        }
        UNIXProcess = up;
        UNIXProcess_pid = pid;

        Class pi = null;
        Field handle = null;
        try {
            pi = Class.forName("java.lang.ProcessImpl");
            handle = pi.getDeclaredField("handle");
            Java.trySetAccessible(handle);
        } catch (Exception e) {
            // ignore and use hashcode
        }
        ProcessImpl = pi;
        ProcessImpl_handle = handle;

        if (UNIXProcess_pid != null) {
            if (ProcessImpl_handle != null) {
                // try both
                pidGetter = new PidGetter() {
                    public long getPid(Process process) {
                        try {
                            if (UNIXProcess.isInstance(process)) {
                                return (Integer)UNIXProcess_pid.get(process);
                            } else if (ProcessImpl.isInstance(process)) {
                                Long hproc = (Long) ProcessImpl_handle.get(process);
                                return WindowsFFI.getKernel32().GetProcessId(hproc);
                            }
                        } catch (Exception e) {
                            // ignore and use hashcode
                        }
                        return process.hashCode();
                    }
                };
            } else {
                // just unix
                pidGetter = new PidGetter() {
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
            pidGetter = new PidGetter() {
                public long getPid(Process process) {
                    try {
                        if (ProcessImpl.isInstance(process)) {
                            Long hproc = (Long) ProcessImpl_handle.get(process);
                            return WindowsFFI.getKernel32().GetProcessId(hproc);
                        }

                    } catch (Exception e) {
                        // ignore and use hashcode
                    }
                    return process.hashCode();
                }
            };
        } else {
            // neither - default PidGetter
            pidGetter = new PidGetter() {
                public long getPid(Process process) {
                    return process.hashCode();
                }
            };
        }
        PID_GETTER = pidGetter;
    }

    public static long reflectPidFromProcess(Process process) {
        return PID_GETTER.getPid(process);
    }

    public static Process run(Ruby runtime, IRubyObject string) throws IOException {
        return run(runtime, new IRubyObject[] {string}, false);
    }

    public static POpenProcess popen(Ruby runtime, IRubyObject string, ModeFlags modes) throws IOException {
        return new POpenProcess(popenShared(runtime, new IRubyObject[] {string}, null, true), runtime, modes);
    }

    public static POpenProcess popen(Ruby runtime, IRubyObject[] strings, Map env, ModeFlags modes) throws IOException {
        return new POpenProcess(popenShared(runtime, strings, env), runtime, modes);
    }

    public static POpenProcess popen(Ruby runtime, IRubyObject string, Map env, ModeFlags modes) throws IOException {
        return new POpenProcess(popenShared(runtime, new IRubyObject[] {string}, env, true), runtime, modes);
    }

    @Deprecated
    public static POpenProcess popen(Ruby runtime, IRubyObject string, IOOptions modes) throws IOException {
        return new POpenProcess(popenShared(runtime, new IRubyObject[] {string}, null, true), runtime, modes);
    }

    @Deprecated
    public static POpenProcess popen(Ruby runtime, IRubyObject[] strings, Map env, IOOptions modes) throws IOException {
        return new POpenProcess(popenShared(runtime, strings, env), runtime, modes);
    }

    @Deprecated
    public static POpenProcess popen3(Ruby runtime, IRubyObject[] strings) throws IOException {
        return new POpenProcess(popenShared(runtime, strings));
    }

    @Deprecated
    public static POpenProcess popen3(Ruby runtime, IRubyObject[] strings, boolean addShell) throws IOException {
        return new POpenProcess(popenShared(runtime, strings, null, addShell));
    }

    private static Process popenShared(Ruby runtime, IRubyObject[] strings) throws IOException {
        return popenShared(runtime, strings, null);
    }

    private static Process popenShared(Ruby runtime, IRubyObject[] strings, Map env) throws IOException {
        return popenShared(runtime, strings, env, false);
    }

    private static Process popenShared(Ruby runtime, IRubyObject[] strings, Map env, boolean addShell) throws IOException {
        String shell = getShell(runtime);
        Process childProcess;
        File pwd = new File(runtime.getCurrentDirectory());

        try {
            // Peel off env hash, if given
            IRubyObject envHash;
            if (env == null && strings.length > 0 && !(envHash = TypeConverter.checkHashType(runtime, strings[0])).isNil()) {
                strings = Arrays.copyOfRange(strings, 1, strings.length);
                env = (Map) envHash;
            }

            // Peel off options hash and warn that we don't support them
            if (strings.length > 1 && !(envHash = TypeConverter.checkHashType(runtime, strings[strings.length - 1])).isNil()) {
                if (!((RubyHash)envHash).isEmpty()) {
                    runtime.getWarnings().warn("popen3 does not support spawn options in JRuby 1.7");
                }
                strings = Arrays.copyOfRange(strings, 0, strings.length - 1);
            }

            String[] args = parseCommandLine(runtime.getCurrentContext(), runtime, strings);
            LaunchConfig cfg = new LaunchConfig(runtime, strings, true);
            boolean useShell = Platform.IS_WINDOWS ? cfg.shouldRunInShell() : false;
            if (addShell) for (String arg : args) useShell |= shouldUseShell(arg);

            if (useShell) {
                cfg.verifyExecutableForShell();
            } else {
                cfg.verifyExecutableForDirect();
            }

            childProcess = buildProcess(runtime, cfg.execArgs, getCurrentEnv(runtime, env), pwd);
        } catch (SecurityException se) {
            throw runtime.newSecurityError(se.getLocalizedMessage());
        }

        return childProcess;
    }

    public static class POpenProcess extends Process {
        private final Process child;
        private final boolean waitForChild;

        // real stream references, to keep them from being GCed prematurely
        private InputStream realInput;
        private OutputStream realOutput;
        private InputStream realInerr;

        private InputStream input;
        private OutputStream output;
        private InputStream inerr;
        private FileChannel inputChannel;
        private FileChannel outputChannel;
        private FileChannel inerrChannel;
        private Pumper inputPumper;
        private Pumper inerrPumper;

        @Deprecated
        public POpenProcess(Process child, Ruby runtime, IOOptions modes) {
            this(child, runtime, modes.getModeFlags());
        }

        public POpenProcess(Process child, Ruby runtime, ModeFlags modes) {
            this.child = child;

            if (modes.isWritable()) {
                this.waitForChild = true;
                prepareOutput(child);
            } else {
                this.waitForChild = false;
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

        public POpenProcess(Process child) {
            this.child = child;
            this.waitForChild = false;

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
            return child.waitFor();
        }

        @Override
        public int exitValue() {
            return child.exitValue();
        }

        @Override
        public void destroy() {
            try {
                // We try to safely close all streams and channels to the greatest
                // extent possible.
                try {if (input != null) input.close();} catch (Exception e) {}
                try {if (inerr != null) inerr.close();} catch (Exception e) {}
                try {if (output != null) output.close();} catch (Exception e) {}
                try {if (inputChannel != null) inputChannel.close();} catch (Exception e) {}
                try {if (inerrChannel != null) inerrChannel.close();} catch (Exception e) {}
                try {if (outputChannel != null) outputChannel.close();} catch (Exception e) {}

                // processes seem to have some peculiar locking sequences, so we
                // need to ensure nobody is trying to close/destroy while we are
                synchronized (this) {
                    if (inputPumper != null) synchronized(inputPumper) {inputPumper.quit();}
                    if (inerrPumper != null) synchronized(inerrPumper) {inerrPumper.quit();}
                    if (waitForChild) {
                        waitFor();
                    } else {
                        RubyIO.obliterateProcess(child);
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        private void prepareInput(Process child) {
            // popen callers wants to be able to read, provide subprocess in directly
            realInput = child.getInputStream();
            // We no longer unwrap, because Java 7+ empties the underlying stream and new native IO popen[3,4] works
            // properly. The pure-Java version has always been somewhat crippled and hacky.
            input = realInput;
            inputChannel = null;
            inputPumper = null;
        }

        private void prepareInerr(Process child) {
            // popen callers wants to be able to read, provide subprocess in directly
            realInerr = child.getErrorStream();
            // We no longer unwrap, because Java 7+ empties the underlying stream and new native IO popen[3,4] works
            // properly. The pure-Java version has always been somewhat crippled and hacky.
            inerr = realInerr;
            inerrChannel = null;
            inerrPumper = null;
        }

        private void prepareOutput(Process child) {
            // popen caller wants to be able to write, provide subprocess out directly
            realOutput = child.getOutputStream();
            output = ChannelHelper.unwrapBufferedStream(realOutput);
            if (output instanceof FileOutputStream) {
                outputChannel = ((FileOutputStream) output).getChannel();
            } else {
                outputChannel = null;
            }
        }

        private void pumpInput(Process child, Ruby runtime) {
            // no read requested, hook up read to parents output
            InputStream childIn = ChannelHelper.unwrapBufferedStream(child.getInputStream());
            FileChannel childInChannel = null;
            if (childIn instanceof FileInputStream) {
                childInChannel = ((FileInputStream) childIn).getChannel();
            }
            OutputStream parentOut = ChannelHelper.unwrapBufferedStream(runtime.getOut());
            FileChannel parentOutChannel = null;
            if (parentOut instanceof FileOutputStream) {
                parentOutChannel = ((FileOutputStream) parentOut).getChannel();
            }
            if (childInChannel != null && parentOutChannel != null) {
                inputPumper = new ChannelPumper(runtime, childInChannel, parentOutChannel, Pumper.Slave.IN, this);
            } else {
                inputPumper = new StreamPumper(runtime, childIn, parentOut, false, Pumper.Slave.IN, this);
            }
            inputPumper.start();
            input = null;
            inputChannel = null;
        }

        private void pumpInerr(Process child, Ruby runtime) {
            // no read requested, hook up read to parents output
            InputStream childIn = ChannelHelper.unwrapBufferedStream(child.getErrorStream());
            FileChannel childInChannel = null;
            if (childIn instanceof FileInputStream) {
                childInChannel = ((FileInputStream) childIn).getChannel();
            }
            OutputStream parentOut = ChannelHelper.unwrapBufferedStream(runtime.getOut());
            FileChannel parentOutChannel = null;
            if (parentOut instanceof FileOutputStream) {
                parentOutChannel = ((FileOutputStream) parentOut).getChannel();
            }
            if (childInChannel != null && parentOutChannel != null) {
                inerrPumper = new ChannelPumper(runtime, childInChannel, parentOutChannel, Pumper.Slave.IN, this);
            } else {
                inerrPumper = new StreamPumper(runtime, childIn, parentOut, false, Pumper.Slave.IN, this);
            }
            inerrPumper.start();
            inerr = null;
            inerrChannel = null;
        }
    }

    public static class LaunchConfig {
        public LaunchConfig(Ruby runtime, IRubyObject[] rawArgs, boolean doExecutableSearch) {
            this.runtime = runtime;
            this.rawArgs = rawArgs;
            this.doExecutableSearch = doExecutableSearch;
            shell = getShell(runtime);
            args = parseCommandLine(runtime.getCurrentContext(), runtime, rawArgs);
        }

        /**
         * Only run an in-process script if the script name has "ruby", ".rb",
         * or "irb" in the name.
         */
        public boolean shouldRunInProcess() {
            if (!runtime.getInstanceConfig().isRunRubyInProcess()
                    || RubyInstanceConfig.hasLoadedNativeExtensions()) {
                return false;
            }

            // Check for special shell characters [<>|] at the beginning
            // and end of each command word and don't run in process if we find them.
            for (int i = 0; i < args.length; i++) {
                String c = args[i];
                if (c.trim().length() == 0) continue;

                char[] firstLast = new char[] {c.charAt(0), c.charAt(c.length()-1)};
                for (int j = 0; j < firstLast.length; j++) {
                    switch (firstLast[j]) {
                    case '<': case '>': case '|': case ';': case '(': case ')':
                    case '~': case '&': case '$': case '"': case '`': case '\n':
                    case '\\': case '\'':
                        return false;
                    case '2':
                        if(c.length() > 1 && c.charAt(1) == '>') return false;
                    }
                }
            }

            String command = args[0];

            if (Platform.IS_WINDOWS) command = command.toLowerCase();

            // handle both slash types, \ and /.
            String[] slashDelimitedTokens = command.split("[/\\\\]");
            String finalToken = slashDelimitedTokens[slashDelimitedTokens.length - 1];
            boolean inProc = (finalToken.endsWith("ruby")
                    || (Platform.IS_WINDOWS && finalToken.endsWith("ruby.exe"))
                    || finalToken.endsWith(".rb")
                    || finalToken.endsWith("irb"));

            if (!inProc) return false;

            // snip off ruby or jruby command from list of arguments
            // leave alone if the command is the name of a script
            int startIndex = command.endsWith(".rb") ? 0 : 1;
            if (command.trim().endsWith("irb")) {
                startIndex = 0;
                args[0] = runtime.getJRubyHome() + File.separator + "bin" + File.separator + "jirb";
            }

            execArgs = new String[args.length - startIndex];
            System.arraycopy(args, startIndex, execArgs, 0, execArgs.length);

            return true;
        }

        /**
         * This hack is to work around a problem with cmd.exe on windows where it can't
         * interpret a filename with spaces in the first argument position as a command.
         * In that case it's better to try passing the bare arguments to runtime.exec.
         * On all other platforms we'll always run the command in the shell.
         */
        public boolean shouldRunInShell() {
            if (rawArgs.length != 1) {
                // this is the case when exact executable and its parameters passed,
                // in such cases MRI just executes it, without any shell.
                return false;
            }

            // in one-arg form, we always use shell, except for Windows
            if (!Platform.IS_WINDOWS) return true;

            // now, deal with Windows
            if (shell == null) return false;

            // TODO: Better name for the method
            // Essentially, we just check for shell meta characters.
            // TODO: we use args here and rawArgs in upper method.
            for (String arg : args) {
                if (!shouldVerifyPathExecutable(arg.trim())) {
                    return true;
                }
            }

            // OK, so no shell meta-chars, now check that the command does exist
            executable = args[0].trim();
            executableFile = findPathExecutable(runtime, executable);

            // if the executable exists, start it directly with no shell
            if (executableFile != null) {
                log(runtime, "Got it: " + executableFile);
                if (isBatch(executableFile)) {
                    log(runtime, "This is a BAT/CMD file, will start in shell");
                    return true;
                }
                return false;
            } else {
                log(runtime, "Didn't find executable: " + executable);
            }

            if (isCmdBuiltin(executable)) {
                cmdBuiltin = true;
                return true;
            }

            // TODO: maybe true here?
            return false;
        }

        private boolean isBatch(File executableFile) {
            return executableFile.getName().toLowerCase().endsWith(".bat");
        }

        public void verifyExecutableForShell() {
            String cmdline = rawArgs[0].toString().trim();
            if (doExecutableSearch && args.length == rawArgs.length && shouldVerifyPathExecutable(cmdline) && !cmdBuiltin) {
                verifyExecutable();
                // replace cmdline with found executable
                cmdline = executableFile.getAbsolutePath();
            }

            // now, prepare the exec args

            execArgs = new String[3];
            execArgs[0] = shell;
            execArgs[1] = shell.endsWith("sh") ? "-c" : "/c";
            execArgs[2] = cmdline;
        }

        public void verifyExecutableForDirect() {
            if (isCmdBuiltin(args[0].trim())) {
                execArgs = new String[args.length + 2];
                execArgs[0] = shell;
                execArgs[1] = "/c";
                execArgs[2] = args[0].trim();
                System.arraycopy(args, 1, execArgs, 3, args.length - 1);
            } else {
                verifyExecutable();
                execArgs = args;
                execArgs[0] = executableFile.getAbsolutePath();
            }
        }

        private void verifyExecutable() {
            if (executableFile == null) {
                if (executable == null) {
                    executable = args[0].trim();
                }
                executableFile = findPathExecutable(runtime, executable);
            }
            if (executableFile == null) {
                throw runtime.newErrnoENOENTError(executable);
            }
        }

        public String[] getExecArgs() {
            return execArgs;
        }

        private boolean isCmdBuiltin(String cmd) {
            if (!shell.endsWith("sh")) { // assume cmd.exe
                int idx = Arrays.binarySearch(WINDOWS_INTERNAL_CMDS, cmd.toLowerCase());
                if (idx >= 0) {
                    log(runtime, "Found Windows shell's built-in command: " + cmd);
                    // Windows shell internal command, launch in shell then
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks a command string to determine if it has I/O redirection
         * characters that require it to be executed by a command interpreter.
         */
        private static boolean hasRedirection(String cmdline) {
            if (Platform.IS_WINDOWS) {
                 // Scan the string, looking for redirection characters (< or >), pipe
                 // character (|) or newline (\n) that are not in a quoted string
                 char quote = '\0';
                 for (int idx = 0; idx < cmdline.length();) {
                     char ptr = cmdline.charAt(idx);
                     switch (ptr) {
                     case '\'':
                     case '\"':
                         if (quote == '\0') {
                             quote = ptr;
                         } else if (quote == ptr) {
                             quote = '\0';
                         }
                         idx++;
                         break;
                     case '>':
                     case '<':
                     case '|':
                     case '\n':
                         if (quote == '\0') {
                             return true;
                         }
                         idx++;
                         break;
                     case '%':
                         // detect Windows environment variables: %ABC%
                         Matcher envVarMatcher = WIN_ENVVAR_PATTERN.matcher(cmdline.substring(idx));
                         if (envVarMatcher.find()) {
                             return true;
                         } else {
                             idx++;
                         }
                         break;
                     case '\\':
                         // slash serves as escape character
                         idx++;
                     default:
                         idx++;
                         break;
                     }
                 }
                 return false;
            } else {
                // TODO: better check here needed, with quoting/escaping
                Matcher metaMatcher = SHELL_METACHARACTER_PATTERN.matcher(cmdline);
                return metaMatcher.find();
            }
        }

        // Should we try to verify the path executable, or just punt to the shell?
        private static boolean shouldVerifyPathExecutable(String cmdline) {
            boolean verifyPathExecutable = true;
            if (hasRedirection(cmdline)) {
                return false;
            }
            return verifyPathExecutable;
        }

        private final Ruby runtime;
        private final boolean doExecutableSearch;
        private final IRubyObject[] rawArgs;
        private final String shell;
        private final String[] args;
        private String[] execArgs;
        private boolean cmdBuiltin = false;

        private String executable;
        private File executableFile;
    }

    public static Process run(Ruby runtime, IRubyObject[] rawArgs, boolean doExecutableSearch) throws IOException {
        RubyHash env = null;
        RubyHash opts = null;
        String dir = runtime.getCurrentDirectory();

        if (rawArgs.length > 0 && rawArgs[0] instanceof RubyHash) {
            // peel off env hash
            env = (RubyHash) rawArgs[0];
            rawArgs = Arrays.copyOfRange(rawArgs, 1, rawArgs.length);
        }

        if (rawArgs.length > 0 && rawArgs[rawArgs.length - 1] instanceof RubyHash) {
            // use opts hash for chdir
            opts = (RubyHash) rawArgs[rawArgs.length - 1];
            rawArgs = Arrays.copyOfRange(rawArgs, 0, rawArgs.length - 1);

            IRubyObject chdir = ArgsUtil.extractKeywordArg(runtime.getCurrentContext(), "chdir", opts);
            if (!chdir.isNil()) dir = chdir.asJavaString();
        }

        return run(runtime, env, dir, rawArgs, doExecutableSearch, false);
    }

    private static boolean hasGlobCharacter(String word) {
        return word.contains("*") || word.contains("?") || word.contains("[") || word.contains("{");
    }

    private static String[] expandGlobs(Ruby runtime, String[] originalArgs) {
        List<String> expandedList = new ArrayList<String>(originalArgs.length);
        for (int i = 0; i < originalArgs.length; i++) {
            if (hasGlobCharacter(originalArgs[i])) {
                // FIXME: Encoding lost here
                List<ByteList> globs = Dir.push_glob(runtime, runtime.getCurrentDirectory(),
                        new ByteList(originalArgs[i].getBytes()), 0);

                for (ByteList glob: globs) {
                    expandedList.add(glob.toString());
                }
            } else {
                expandedList.add(originalArgs[i]);
            }
        }

        String[] args = new String[expandedList.size()];
        expandedList.toArray(args);

        return args;
    }

    public static Process run(Ruby runtime, IRubyObject[] rawArgs, boolean doExecutableSearch, boolean forceExternalProcess) throws IOException {
        return run(runtime, Collections.EMPTY_MAP, runtime.getCurrentDirectory(), rawArgs, doExecutableSearch, forceExternalProcess);
    }

    public static Process run(Ruby runtime, Map env, String dir, IRubyObject[] rawArgs, boolean doExecutableSearch, boolean forceExternalProcess) throws IOException {
        Process aProcess;
        File pwd = new File(dir);
        LaunchConfig cfg = new LaunchConfig(runtime, rawArgs, doExecutableSearch);

        try {
            if (!forceExternalProcess && cfg.shouldRunInProcess()) {
                log(runtime, "Launching in-process");
                ScriptThreadProcess ipScript = new ScriptThreadProcess(runtime,
                        expandGlobs(runtime, cfg.getExecArgs()), getCurrentEnv(runtime, env), pwd);
                ipScript.start();
                return ipScript;
            } else {
                if (cfg.shouldRunInShell()) {
                    log(runtime, "Launching with shell");
                    // execute command with sh -c
                    // this does shell expansion of wildcards
                    cfg.verifyExecutableForShell();
                } else {
                    log(runtime, "Launching directly (no shell)");
                    cfg.verifyExecutableForDirect();
                }
                String[] args = cfg.getExecArgs();
                if (dir.startsWith("uri:classloader:")) {
                    // system commands can't run with a URI for the current dir, so the best we can use is user.dir
                    pwd = new File(System.getProperty("user.dir"));

                    // only if we inside a jar and spawning org.jruby.Main we
                    // change to the current directory inside the jar
                    if (args[args.length - 1].contains("org.jruby.Main")) {
                        args[args.length - 1] = args[args.length - 1].replace("org.jruby.Main",
                                "org.jruby.Main -C " + dir);
                    }
                }
                aProcess = buildProcess(runtime, args, getCurrentEnv(runtime, env), pwd);
            }
        } catch (SecurityException se) {
            throw runtime.newSecurityError(se.getLocalizedMessage());
        }

        return aProcess;
    }

    private interface Pumper extends Runnable {
        enum Slave { IN, OUT }
        void start();
        void quit();
    }

    private static class StreamPumper extends Thread implements Pumper {
        private final InputStream in;
        private final OutputStream out;
        private final boolean onlyIfAvailable;
        private final Object waitLock = new Object();
        private final Object sync;
        private final Slave slave;
        private volatile boolean quit;
        private final Ruby runtime;

        StreamPumper(Ruby runtime, InputStream in, OutputStream out, boolean avail, Slave slave, Object sync) {
            this.in = ChannelHelper.unwrapBufferedStream(in);
            this.out = ChannelHelper.unwrapBufferedStream(out);
            this.onlyIfAvailable = avail;
            this.slave = slave;
            this.sync = sync;
            this.runtime = runtime;
            setDaemon(true);
        }
        @Override
        public void run() {
            runtime.getCurrentContext().setEventHooksEnabled(false);
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
            interrupt();
        }
    }

    private static class ChannelPumper extends Thread implements Pumper {
        private final FileChannel inChannel;
        private final FileChannel outChannel;
        private final Slave slave;
        private final Object sync;
        private volatile boolean quit;
        private final Ruby runtime;

        ChannelPumper(Ruby runtime, FileChannel inChannel, FileChannel outChannel, Slave slave, Object sync) {
            if (DEBUG) out.println("using channel pumper");
            this.inChannel = inChannel;
            this.outChannel = outChannel;
            this.slave = slave;
            this.sync = sync;
            this.runtime = runtime;
            setDaemon(true);
        }
        @Override
        public void run() {
            runtime.getCurrentContext().setEventHooksEnabled(false);
            ByteBuffer buf = ByteBuffer.allocateDirect(1024);
            clearBuffer(buf);
            try {
                while (!quit && inChannel.isOpen() && outChannel.isOpen()) {
                    int read = inChannel.read(buf);
                    if (read == -1) break;
                    flipBuffer(buf);
                    outChannel.write(buf);
                    clearBuffer(buf);
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
            this.quit = true;
            interrupt();
        }
    }

    private static void handleStreams(Ruby runtime, Process p, InputStream in, OutputStream out, OutputStream err) throws IOException {
        InputStream pOut = p.getInputStream();
        InputStream pErr = p.getErrorStream();
        OutputStream pIn = p.getOutputStream();

        StreamPumper t1 = new StreamPumper(runtime, pOut, out, false, Pumper.Slave.IN, p);
        StreamPumper t2 = new StreamPumper(runtime, pErr, err, false, Pumper.Slave.IN, p);

        // The assumption here is that the 'in' stream provides
        // proper available() support. If available() always
        // returns 0, we'll hang!
        StreamPumper t3 = new StreamPumper(runtime, in, pIn, true, Pumper.Slave.OUT, p);

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

        // Interrupt all three, just in case they're stuck.
        // Note: On some platforms, even interrupt might not
        // have an effect if the thread is IO blocked. But we
        // need to move away from Thread.stop so this is the
        // best we can do.
        try {
            t1.quit();
            t2.quit();
            t3.quit();
            t1.interrupt();
            t2.interrupt();
            t3.interrupt();
        } catch (SecurityException se) {}

        try { t1.join(); } catch (InterruptedException ie) {}
        try { t2.join(); } catch (InterruptedException ie) {}
        try { t3.join(); } catch (InterruptedException ie) {}
    }

    private static void handleStreamsNonblocking(Ruby runtime, Process p, OutputStream out, OutputStream err) throws IOException {
        InputStream pOut = p.getInputStream();
        InputStream pErr = p.getErrorStream();

        StreamPumper t1 = new StreamPumper(runtime, pOut, out, false, Pumper.Slave.IN, p);
        StreamPumper t2 = new StreamPumper(runtime, pErr, err, false, Pumper.Slave.IN, p);

        t1.start();
        t2.start();
    }

    // TODO: move inside the LaunchConfig
    private static String[] parseCommandLine(ThreadContext context, Ruby runtime, IRubyObject[] rawArgs) {
        String[] args;
        if (rawArgs.length == 1) {
            if (hasLeadingArgvArray(rawArgs)) {
                // can't make use of it, discard the argv[0] entry
                args = new String[] { getPathEntry((RubyArray) rawArgs[0]) };
            } else {
                synchronized (runtime.getLoadService()) {
                    runtime.getLoadService().require("jruby/path_helper");
                }
                RubyModule pathHelper = runtime.getClassFromPath("JRuby::PathHelper");
                RubyArray parts = (RubyArray) Helpers.invoke(
                        context, pathHelper, "smart_split_command", rawArgs);
                args = new String[parts.getLength()];
                for (int i = 0; i < parts.getLength(); i++) {
                    args[i] = parts.entry(i).toString();
                }
            }
        } else {
            args = new String[rawArgs.length];
            int start = 0;
            if (hasLeadingArgvArray(rawArgs)) {
                start = 1;
                args[0] = getPathEntry((RubyArray) rawArgs[0]);
            }
            for (int i = start; i < rawArgs.length; i++) {
                args[i] = rawArgs[i].toString();
            }
        }
        return args;
    }

    /** Takes an argument array suitable for Kernel#exec or similar,
     * and indicates whether it has a leading two-element array giving
     * the path and argv[0] entries separately.
     *
     * We can't use the argv[0] entry through ProcessBuilder, so
     * we discard it.
     */
    private static boolean hasLeadingArgvArray(IRubyObject[] rawArgs) {
        return (rawArgs.length >= 1
                && (rawArgs[0] instanceof RubyArray)
                && (((RubyArray) rawArgs[0]).getLength() == 2));
    }

    private static String getPathEntry(RubyArray initArray) {
        return initArray.entry(0).toString();
    }

    private static String getShell(Ruby runtime) {
        return findPathExecutable(runtime, RbConfigLibrary.jrubyShell()).getAbsolutePath();
    }

    public static boolean shouldUseShell(String command) {
        boolean useShell = false;
        for (char c : command.toCharArray()) {
            if (c != ' ' && !Character.isLetter(c) && "*?{}[]<>()~&|\\$;'`\"\n".indexOf(c) != -1) {
                useShell = true;
            }
        }
        if (Platform.IS_WINDOWS && command.length() >= 1 && command.charAt(0) == '@') {
            // JRUBY-5522
            useShell = true;
        }
        return useShell;
    }

    static void log(Ruby runtime, String msg) {
        if (RubyInstanceConfig.DEBUG_LAUNCHING) {
            runtime.getErr().println("ShellLauncher: " + msg);
        }
    }

    @Deprecated
    public static OutputStream unwrapBufferedStream(OutputStream filteredStream) {
        return ChannelHelper.unwrapBufferedStream(filteredStream);
    }

    @Deprecated
    public static InputStream unwrapBufferedStream(InputStream filteredStream) {
        return ChannelHelper.unwrapBufferedStream(filteredStream);
    }

    @Deprecated
    public static OutputStream unwrapFilterOutputStream(OutputStream filteredStream) {
        return ChannelHelper.unwrapFilterOutputStream(filteredStream);
    }

    @Deprecated
    public static InputStream unwrapFilterInputStream(InputStream filteredStream) {
        return ChannelHelper.unwrapFilterInputStream(filteredStream);
    }
}
