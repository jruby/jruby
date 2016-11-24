/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import org.jruby.main.DripMain;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

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
        printTruffleTimeMetric("before-main");

        doGCJCheck();

        Main main;

        if (DripMain.DRIP_RUNTIME != null) {
            main = new Main(DripMain.DRIP_CONFIG, true);
        } else {
            main = new Main(true);
        }

        try {
            Status status = main.run(args);

            printTruffleTimeMetric("after-main");
            printTruffleMemoryMetric();

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
            // If a Truffle exception gets this far it's a hard failure - don't try and dress it up as a Ruby exception

            if (main.isTruffle()) {
                System.err.println("Truffle internal error: " + t);
                t.printStackTrace(System.err);
            } else {
                // print out as a nice Ruby backtrace
                System.err.println("Unhandled Java exception: " + t);
                System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t, false));
                while ((t = t.getCause()) != null) {
                    System.err.println("Caused by:");
                    System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t, false));
                }
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

    @Deprecated
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

        Ruby _runtime;

        if (isTruffle()) {
            _runtime = null;
        } else {
            if (DripMain.DRIP_RUNTIME != null) {
                // use drip's runtime, reinitializing config
                _runtime = DripMain.DRIP_RUNTIME;
                _runtime.reinitialize(true);
            } else {
                _runtime = Ruby.newInstance(config);
            }
        }

        final Ruby runtime = _runtime;
        final AtomicBoolean didTeardown = new AtomicBoolean();

        if (runtime != null && config.isHardExit()) {
            // we're the command-line JRuby, and should set a shutdown hook for
            // teardown.
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    if (didTeardown.compareAndSet(false, true)) {
                        runtime.tearDown();
                    }
                }
            });
        }

        try {
            if (runtime != null) {
                doSetContextClassLoader(runtime);
            }

            if (in == null) {
                // no script to run, return success
                return new Status();
            } else if (config.isXFlag() && !config.hasShebangLine()) {
                // no shebang was found and x option is set
                throw new MainExitException(1, "jruby: no Ruby script found in input (LoadError)");
            } else if (config.getShouldCheckSyntax()) {
                // check syntax only and exit
                if (isTruffle()) {
                    final TruffleRubyEngineInterface truffle = loadTruffle();

                    try {
                        final int exitCode = truffle.doCheckSyntax(in, filename);
                        return new Status(exitCode);
                    } finally {
                        truffle.dispose();
                    }
                } else {
                    return doCheckSyntax(runtime, in, filename);
                }
            } else {
                // proceed to run the script
                if (isTruffle()) {
                    final TruffleRubyEngineInterface truffle = loadTruffle();

                    printTruffleTimeMetric("before-run");

                    try {
                        final int exitCode = truffle.execute(filename);
                        return new Status(exitCode);
                    } finally {
                        printTruffleTimeMetric("after-run");
                        truffle.dispose();
                    }
                } else {
                    return doRunFromMain(runtime, in, filename);
                }
            }
        } finally {
            if (runtime != null && didTeardown.compareAndSet(false, true)) {
                runtime.tearDown();
            }
        }
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
            if (oomeMessage.contains("PermGen")) {
                // report permgen memory error
                config.getError().println("Error: Your application exhausted PermGen area of the heap.");
                config.getError().println("Specify -J-XX:MaxPermSize=###M to increase it (### = PermGen size in MB).");
            } else if (oomeMessage.contains("unable to create new native thread")) {
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

    private Status doRunFromMain(Ruby runtime, InputStream in, String filename) {
        try {
            doCheckSecurityManager();

            runtime.runFromMain(in, filename);
        } catch (RaiseException rj) {
            return new Status(handleRaiseException(rj));
        }
        return new Status();
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
            runtime.parseFromMain(in, filename);
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
            config.getOutput().print(OutputStrings.getBasicUsageHelp());
            config.getOutput().print(OutputStrings.getFeaturesHelp());
        }
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

    private void doCheckSecurityManager() {
        if (Main.class.getClassLoader() == null && System.getSecurityManager() != null) {
            System.err.println("Warning: security manager and JRuby running from boot classpath.\n" +
                    "Run from jruby.jar or set env VERIFY_JRUBY=true to enable security.");
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
        if ( runtime.getSystemExit().isInstance(raisedException) ) {
            IRubyObject status = raisedException.callMethod(runtime.getCurrentContext(), "status");
            if (status != null && ! status.isNil()) {
                return RubyNumeric.fix2int(status);
            }
            return 0;
        }
        System.err.print(runtime.getInstanceConfig().getTraceType().printBacktrace(raisedException, runtime.getPosix().isatty(FileDescriptor.err)));
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

    private boolean isTruffle() {
        return config.getCompileMode().isTruffle();
    }

    private TruffleRubyEngineInterface loadTruffle() {
        Main.printTruffleTimeMetric("before-load-context");

        String javaVersion = System.getProperty("java.version");
        String[] parts = javaVersion.split("\\D+");
        int firstPart = Integer.valueOf(parts[0]);
        if (!(firstPart >= 9 || Integer.valueOf(parts[1]) >= 8)) {
            System.err.println("JRuby+Truffle needs Java 8 to run (found " + javaVersion + ").");
            System.exit(1);
        }

        final Class<?> truffleRubyEngineClass;

        try {
            truffleRubyEngineClass = Class.forName("org.jruby.truffle.RubyEngine");
        } catch (Exception e) {
            throw new RuntimeException("JRuby's Truffle backend not available - either it was not compiled because JRuby was built with Java 7, or it has been removed", e);
        }

        final TruffleRubyEngineInterface truffleEngine;

        try {
            final Object truffleEngineInstance = truffleRubyEngineClass.getConstructor(RubyInstanceConfig.class).newInstance(config);

            truffleEngine = (TruffleRubyEngineInterface) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{TruffleRubyEngineInterface.class}, new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    return truffleRubyEngineClass.getMethod(method.getName(), method.getParameterTypes()).invoke(truffleEngineInstance, args);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException("Error while calling the constructor of Truffle's RubyContext", e);
        }

        Main.printTruffleTimeMetric("after-load-context");

        return truffleEngine;
    }
    
    public static void printTruffleTimeMetric(String id) {
        if (Options.TRUFFLE_METRICS_TIME.load()) {
            final long millis = System.currentTimeMillis();
            System.err.printf("%s %d.%03d%n", id, millis / 1000, millis % 1000);
        }
    }

    private static void printTruffleMemoryMetric() {
        if (Options.TRUFFLE_METRICS_MEMORY_USED_ON_EXIT.load()) {
            for (int n = 0; n < 10; n++) {
                System.gc();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            System.err.printf("allocated %d%n", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        }
    }

    private final RubyInstanceConfig config;
}

