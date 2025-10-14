/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Kiel Hodges <jruby-devel@selfsosoft.com>
 * Copyright (C) 2005 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
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

package org.jruby;

import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.main.PrebootMain;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.cli.OutputStrings;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.jruby.api.Convert.toInt;

/**
 * Class used to launch the interpreter.
 * This is the main class as defined in the jruby.mf manifest.
 * It is very basic and does not support yet the same array of switches
 * as the C interpreter.
 *       Usage: java -jar jruby.jar [switches] [rubyfile.rb] [arguments]
 *           -e 'command'    one line of script. Several -e's allowed. Omit [programfile]
 * @author  jpetersen
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public Main(RubyInstanceConfig config) {
        this(config, false);
    }

    public Main(final InputStream in, final PrintStream out, final PrintStream err) {
        this(new RubyInstanceConfig(in, out, err));
    }

    public Main() {
        this(new RubyInstanceConfig());
    }

    private Main(RubyInstanceConfig config, boolean hardExit) {
        this.config = config;
        config.setHardExit(hardExit);
    }

    private Main(boolean hardExit) {
        // used only from main(String[]), so we process dotfile here
        processDotfile();
        this.config = new RubyInstanceConfig();
        config.setHardExit(hardExit);
    }

    private static List<String> getDotfileDirectories() {
        final ArrayList<String> searchList = new ArrayList<>(4);
        for (String homeProp : new String[] {"user.dir", "user.home"}) {
            String home = SafePropertyAccessor.getProperty(homeProp);
            if (home != null) searchList.add(home);
        }

        // JVM sometimes picks odd location for user.home based on a registry entry
        // (see http://bugs.sun.com/view_bug.do?bug_id=4787931).  Add extra check in
        // case that entry is wrong. Add before user.home in search list.
        if (Platform.IS_WINDOWS) {
            String homeDrive = System.getenv("HOMEDRIVE");
            String homePath = System.getenv("HOMEPATH");
            if (homeDrive != null && homePath != null) {
                searchList.add(1, (homeDrive + homePath).replace('\\', '/'));
            }
        }

        return searchList;
    }

    public static void processDotfile() {
        final StringBuilder path = new StringBuilder();
        for (String home : getDotfileDirectories()) {
            path.setLength(0);
            path.append(home).append("/.jrubyrc");
            final File dotfile = new File(path.toString());
            if (dotfile.exists()) loadJRubyProperties(dotfile);
        }
    }

    private static void loadJRubyProperties(File dotfile) {
        FileInputStream fis = null;

        try {
            // update system properties with long form jruby properties from .jrubyrc
            Properties sysProps = System.getProperties();
            Properties newProps = new Properties();
            // load properties and re-set as jruby.*
            fis = new FileInputStream(dotfile);
            newProps.load(fis);
            for (Map.Entry entry : newProps.entrySet()) {
                sysProps.put("jruby." + entry.getKey(), entry.getValue());
            }
        }
        catch (IOException|SecurityException ex) {
            if (LOG.isDebugEnabled()) LOG.debug("exception loading properties from: " + dotfile, ex);
        }
        finally {
            if (fis != null) try { fis.close(); } catch (Exception e) {}
        }
    }

    public static class Status {
        private boolean isExit = false;
        private int status = 0;

        /**
         * Creates a status object with the specified value and with explicit
         * exit flag. An exit flag means that Kernel.exit() has been explicitly
         * invoked during the run.
         *
         * @param status The status value.
         */
        Status(int status) {
            this.isExit = true;
            this.status = status;
        }

        /**
         * Creates a status object with 0 value and no explicit exit flag.
         */
        Status() {}

        public boolean isExit() { return isExit; }
        public int getStatus() { return status; }
    }

    /**
     * This is the command-line entry point for JRuby, and should ONLY be used by
     * Java when starting up JRuby from a command-line. Use other mechanisms when
     * embedding JRuby into another application.
     *
     * @param args command-line args, provided by the JVM.
     */
    public static void main(String[] args) {
        doGCJCheck();

        Main main;

        if (PrebootMain.getPrebootMain() != null) {
            main = new Main(PrebootMain.getPrebootMain().getPrebootConfig(), true);
        } else {
            main = new Main(true);
        }

        try {
            Status status = main.run(args);

            if (status.isExit()) {
                System.exit(status.getStatus());
            }
        }
        catch (RaiseException ex) {
            System.exit( handleRaiseException(ex) );
        }
        catch (JumpException ex) {
            System.exit( handleUnexpectedJump(ex) );
        }
        catch (Throwable t) {
            // print out as a nice Ruby backtrace
            System.err.println("Unhandled Java exception: " + t);
            System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t, false));
            while ((t = t.getCause()) != null) {
                System.err.println("Caused by:");
                System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t, false));
            }

            System.exit(1);
        }
    }

    public Status run(String[] args) {
        try {
            config.processArguments(args);
            return internalRun();
        } catch (MainExitException mee) {
            return handleMainExit(mee);
        } catch (OutOfMemoryError oome) {
            return handleOutOfMemory(oome);
        } catch (StackOverflowError soe) {
            return handleStackOverflow(soe);
        } catch (UnsupportedClassVersionError ucve) {
            return handleUnsupportedClassVersion(ucve);
        } catch (ThreadKill kill) {
            return new Status();
        }
    }

    @Deprecated(since = "1.6.0")
    public Status run() {
        return internalRun();
    }

    private Status internalRun() {
        doShowVersion();
        doShowCopyright();
        doPrintProperties();

        if (!config.getShouldRunInterpreter()) {
            doPrintUsage(false);
            return new Status();
        }

        InputStream in   = config.getScriptSource();
        String filename  = config.displayedFileName();

        final Ruby runtime;

        if (PrebootMain.getPrebootMain() != null) {
            // use prebooted runtime, reinitializing config
            runtime = PrebootMain.getPrebootMain().getPrebootRuntime();
            runtime.reinitialize(true);
        } else {
            runtime = Ruby.newMain(config);
        }

        Status status = null;
        try {
            try {
                doSetContextClassLoader(runtime);

                if (in == null) {
                    // no script to run, return success
                    return new Status();
                } else if (config.getShouldCheckSyntax()) {
                    // check syntax only and exit
                    return doCheckSyntax(runtime, in, filename);
                } else {
                    // proceed to run the script
                    runtime.runFromMain(in, filename);
                }
                status = new Status();
            } finally {
                try {
                    runtime.tearDown();
                } catch (RaiseException rj) {
                    status = new Status(handleRaiseException(rj));
                }
            }
        } catch (RaiseException rj) {
            int ret = handleRaiseException(rj);
            if (status == null) status = new Status(ret);
        }

        return status;
    }

    private Status handleUnsupportedClassVersion(UnsupportedClassVersionError ex) {
        config.getError().println("Error: Some library (perhaps JRuby) was built with a later JVM version.");
        config.getError().println("Please use libraries built with the version you intend to use or an earlier one.");
        if (config.isVerbose()) {
            ex.printStackTrace(config.getError());
        } else {
            config.getError().println("Specify -w for full " + ex + " stack trace");
        }
        return new Status(1);
    }

    /**
     * Print a nicer stack size error since Rubyists aren't used to seeing this.
     */
    private Status handleStackOverflow(StackOverflowError ex) {
        String memoryMax = getRuntimeFlagValue("-Xss");

        if (memoryMax != null) {
            config.getError().println("Error: Your application used more stack memory than the safety cap of " + memoryMax + '.');
        } else {
            config.getError().println("Error: Your application used more stack memory than the default safety cap.");
        }
        config.getError().println("Specify -J-Xss####k to increase it (#### = cap size in KB).");

        if (config.isVerbose()) {
            ex.printStackTrace(config.getError());
        } else {
            config.getError().println("Specify -w for full " + ex + " stack trace");
        }

        return new Status(1);
    }

    /**
     * Print a nicer memory error since Rubyists aren't used to seeing this.
     */
    private Status handleOutOfMemory(OutOfMemoryError ex) {
        System.gc(); // try to clean up a bit of space, hopefully, so we can report this error

        String oomeMessage = ex.getMessage();
        boolean heapError = false;

        if (oomeMessage != null) {
            if (oomeMessage.contains("unable to create new native thread")) {
                // report thread exhaustion error
                config.getError().println("Error: Your application demanded too many live threads, perhaps for Fiber or Enumerator.");
                config.getError().println("Ensure your old Fibers and Enumerators are being cleaned up.");
            } else {
                heapError = true;
            }
        }

        if (heapError) { // report heap memory error

            String memoryMax = getRuntimeFlagValue("-Xmx");

            if (memoryMax != null) {
                config.getError().println("Error: Your application used more memory than the safety cap of " + memoryMax + ".");
            } else {
                config.getError().println("Error: Your application used more memory than the automatic cap of " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB.");
            }
            config.getError().println("Specify -J-Xmx####M to increase it (#### = cap size in MB).");
        }

        if (config.isVerbose()) {
            ex.printStackTrace(config.getError());
        } else {
            config.getError().println("Specify -w for full " + ex + " stack trace");
        }

        return new Status(1);
    }

    private String getRuntimeFlagValue(String prefix) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        for (String param : runtime.getInputArguments()) {
            if (param.startsWith(prefix)) {
                return param.substring(prefix.length()).toUpperCase();
            }
        }

        return null;
    }

    private Status handleMainExit(MainExitException mee) {
        if (!mee.isAborted()) {
            config.getError().println(mee.getMessage());
            if (mee.isUsageError()) {
                doPrintUsage(true);
            }
        }
        return new Status(mee.getStatus());
    }

    private Status doCheckSyntax(Ruby runtime, InputStream in, String filename) throws RaiseException {
        // check primary script
        boolean status = checkStreamSyntax(runtime, in, filename);

        // check other scripts specified on argv
        for (String arg : config.getArgv()) {
            status = status && checkFileSyntax(runtime, arg);
        }

        return new Status(status ? 0 : -1);
    }

    private boolean checkFileSyntax(Ruby runtime, String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                return checkStreamSyntax(runtime, new FileInputStream(file), filename);
            } catch (FileNotFoundException fnfe) {
                config.getError().println("File not found: " + filename);
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean checkStreamSyntax(Ruby runtime, InputStream in, String filename) {
        final ThreadContext context = runtime.getCurrentContext();
        final IRubyObject $ex = context.getErrorInfo();
        try {
            runtime.parseFromMain(filename, in);
            config.getOutput().println("Syntax OK");
            return true;
        } catch (RaiseException re) {
            if (re.getException().getMetaClass().getBaseName().equals("SyntaxError")) {
                context.setErrorInfo($ex);
                config.getError().println("SyntaxError in " + re.getException().message(context));
                return false;
            }
            throw re;
        }
    }

    private void doSetContextClassLoader(Ruby runtime) {
        // set thread context JRuby classloader here, for the main thread
        try {
            Thread.currentThread().setContextClassLoader(runtime.getJRubyClassLoader());
        } catch (SecurityException se) {
            // can't set TC classloader
            if (runtime.getInstanceConfig().isVerbose()) {
                config.getError().println("WARNING: Security restrictions disallowed setting context classloader for main thread.");
            }
        }
    }

    private void doPrintProperties() {
        if (config.getShouldPrintProperties()) {
            config.getOutput().print(OutputStrings.getPropertyHelp());
        }
    }

    private void doPrintUsage(boolean force) {
        if (config.getShouldPrintUsage() || force) {
            String rubyPager = getRubyPagerEnv();

            // Do not want to boot native subsystem here, so we do best guess based on System.console. It will be
            // non-null only if both STDIN and STDOUT are tty.
            boolean tty = System.console() != null;

            if (rubyPager == null) {
                config.getOutput().print(OutputStrings.getBasicUsageHelp(tty));
                config.getOutput().print(OutputStrings.getFeaturesHelp(tty));
            } else {
                try {
                    ProcessBuilder builder = new ProcessBuilder(rubyPager);
                    builder.environment().put("LESS", "-R");

                    builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    builder.redirectError(ProcessBuilder.Redirect.INHERIT);

                    Process process = builder.start();
                    OutputStream in = process.getOutputStream();

                    String fullHelp = OutputStrings.getBasicUsageHelp(tty) + OutputStrings.getFeaturesHelp(tty);
                    in.write(fullHelp.getBytes(StandardCharsets.UTF_8));

                    in.flush();
                    in.close();
                    process.waitFor();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getRubyPagerEnv() {
        String rubyPager = System.getenv("RUBY_PAGER");
        if (rubyPager == null)
            rubyPager = System.getenv("PAGER");

        return rubyPager;
    }

    private void doShowCopyright() {
        if (config.isShowCopyright()) {
            config.getOutput().println(OutputStrings.getCopyrightString());
        }
    }

    private void doShowVersion() {
        if (config.isShowVersion()) {
            config.getOutput().println(OutputStrings.getVersionString());
        }
    }

    private static void doGCJCheck() {
        // Ensure we're not running on GCJ, since it's not supported and leads to weird errors
        if (Platform.IS_GCJ) {
            System.err.println("Fatal: GCJ (GNU Compiler for Java) is not supported by JRuby.");
            System.exit(1);
        }
    }

    /**
     * This is only used from the main(String[]) path, in which case the err for this
     * run should be System.err. In order to avoid the Ruby err being closed and unable
     * to write, we use System.err unconditionally.
     *
     * @param ex
     * @return
     */
    protected static int handleRaiseException(final RaiseException ex) {
        RubyException raisedException = ex.getException();
        final Ruby runtime = raisedException.getRuntime();
        var context = runtime.getCurrentContext();
        if ( runtime.getSystemExit().isInstance(raisedException) ) {
            IRubyObject status = raisedException.callMethod(context, "status");
            return status != null && ! status.isNil() ? toInt(context, status) : 0;
        } else if ( runtime.getSignalException().isInstance(raisedException) ) {
            IRubyObject status = raisedException.callMethod(context, "signo");
            return status != null && ! status.isNil() ? toInt(context, status) + 128 : 0;
        }

        TraceType traceType = runtime.getInstanceConfig().getTraceType();
        boolean isatty = runtime.getPosix().isatty(FileDescriptor.err);

        System.err.print(traceType.printBacktrace(raisedException, isatty));
        return 1;
    }

    private static int handleUnexpectedJump(final JumpException ex) {
        if ( ex instanceof JumpException.SpecialJump ) { // ex == JumpException.SPECIAL_JUMP
            System.err.println("Unexpected break: " + ex);
        }
        else if ( ex instanceof JumpException.FlowControlException ) {
            // NOTE: assuming a single global runtime main(args) should have :
            if ( Ruby.isGlobalRuntimeReady() ) {
                final Ruby runtime = Ruby.getGlobalRuntime();
                RaiseException raise = ((JumpException.FlowControlException) ex).buildException(runtime);
                if ( raise != null ) handleRaiseException(raise);
            }
            else {
                System.err.println("Unexpected jump: " + ex);
            }
        }
        else {
            System.err.println("Unexpected: " + ex);
        }
        
        final StackTraceElement[] trace = ex.getStackTrace();
        if ( trace != null && trace.length > 0 ) {
            System.err.println( ThreadContext.createRawBacktraceStringFromThrowable(ex, false) );
        }
        else {
            System.err.println("HINT: to get backtrace for jump exceptions run with -Xjump.backtrace=true");
        }
        
        // TODO: should match MRI (>= 2.2.3) exit status - @see ruby/test_enum.rb#test_first
        return 2;
    }

    private final RubyInstanceConfig config;
}

