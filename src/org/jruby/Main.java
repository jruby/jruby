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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.SimpleSampler;

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
    public Main(RubyInstanceConfig config) {
        this(config, false);
    }

    public Main(final InputStream in, final PrintStream out, final PrintStream err) {
        this(new RubyInstanceConfig(){{
            setInput(in);
            setOutput(out);
            setError(err);
        }});
    }

    public Main() {
        this(new RubyInstanceConfig());
    }

    private Main(RubyInstanceConfig config, boolean hardExit) {
        this.config = config;
        config.setHardExit(hardExit);
    }

    private Main(boolean hardExit) {
        this(new RubyInstanceConfig(), hardExit);
    }

    public static class Status {
        private boolean isExit = false;
        private int status = 0;

        /**
         * Creates a status object with the specified value and with explicit
         * exit flag. An exit flag means that Kernel.exit() has been explicitly
         * invoked during the run.
         *
         * @param staus The status value.
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
        
        Main main = new Main(true);
        
        try {
            Status status = main.run(args);
            if (status.isExit()) {
                System.exit(status.getStatus());
            }
        } catch (RaiseException rj) {
            System.exit(handleRaiseException(rj));
        } catch (Throwable t) {
            // print out as a nice Ruby backtrace
            System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t));
            while ((t = t.getCause()) != null) {
                System.err.println("Caused by:");
                System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t));
            }
            System.exit(1);
        }
    }

    public Status run(String[] args) {
        try {
            config.processArguments(args);
            return run();
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

        if (!config.shouldRunInterpreter() ) {
            doPrintUsage(false);
            doPrintProperties();
            return new Status();
        }

        InputStream in   = config.getScriptSource();
        String filename  = config.displayedFileName();
        
        doProcessArguments(in);
        
        Ruby runtime     = Ruby.newInstance(config);

        try {
            doSetContextClassLoader(runtime);

            if (in == null) {
                // no script to run, return success
                return new Status();
            } else if (config.isxFlag() && !config.hasShebangLine()) {
                // no shebang was found and x option is set
                throw new MainExitException(1, "jruby: no Ruby script found in input (LoadError)");
            } else if (config.isShouldCheckSyntax()) {
                // check syntax only and exit
                return doCheckSyntax(runtime, in, filename);
            } else {
                // proceed to run the script
                return doRunFromMain(runtime, in, filename);
            }
        } finally {
            runtime.tearDown();
        }
    }

    private Status handleUnsupportedClassVersion(UnsupportedClassVersionError ucve) {
        config.getError().println("Error: Some library (perhaps JRuby) was built with a later JVM version.");
        config.getError().println("Please use libraries built with the version you intend to use or an earlier one.");
        if (config.isVerbose()) {
            config.getError().println("Exception trace follows:");
            ucve.printStackTrace();
        } else {
            config.getError().println("Specify -w for full UnsupportedClassVersionError stack trace");
        }
        return new Status(1);
    }

    private Status handleStackOverflow(StackOverflowError soe) {
        // produce a nicer error since Rubyists aren't used to seeing this
        System.gc();
        String stackMax = SafePropertyAccessor.getProperty("jruby.stack.max");
        String message = "";
        if (stackMax != null) {
            message = " of " + stackMax;
        }
        config.getError().println("Error: Your application used more stack memory than the safety cap" + message + ".");
        config.getError().println("Specify -J-Xss####k to increase it (#### = cap size in KB).");
        if (config.isVerbose()) {
            config.getError().println("Exception trace follows:");
            soe.printStackTrace();
        } else {
            config.getError().println("Specify -w for full StackOverflowError stack trace");
        }
        return new Status(1);
    }

    private Status handleOutOfMemory(OutOfMemoryError oome) {
        // produce a nicer error since Rubyists aren't used to seeing this
        System.gc();
        String memoryMax = SafePropertyAccessor.getProperty("jruby.memory.max");
        String message = "";
        if (memoryMax != null) {
            message = " of " + memoryMax;
        }
        config.getError().println("Error: Your application used more memory than the safety cap" + message + ".");
        config.getError().println("Specify -J-Xmx####m to increase it (#### = cap size in MB).");
        if (config.isVerbose()) {
            config.getError().println("Exception trace follows:");
            oome.printStackTrace();
        } else {
            config.getError().println("Specify -w for full OutOfMemoryError stack trace");
        }
        return new Status(1);
    }

    private Status handleMainExit(MainExitException mee) {
        if (!mee.isAborted()) {
            config.getOutput().println(mee.getMessage());
            if (mee.isUsageError()) {
                doPrintUsage(true);
            }
        }
        return new Status(mee.getStatus());
    }

    private Status doRunFromMain(Ruby runtime, InputStream in, String filename) {
        long now = -1;
        try {
            if (config.isBenchmarking()) {
                now = System.currentTimeMillis();
            }
            if (config.isSamplingEnabled()) {
                SimpleSampler.startSampleThread();
            }

            doCheckSecurityManager();

            try {
                runtime.runFromMain(in, filename);
            } finally {
                if (config.isBenchmarking()) {
                    config.getOutput().println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
                }
                if (config.isSamplingEnabled()) {
                    org.jruby.util.SimpleSampler.report();
                }
            }
        } catch (RaiseException rj) {
            return new Status(handleRaiseException(rj));
        }
        return new Status();
    }

    private Status doCheckSyntax(Ruby runtime, InputStream in, String filename) throws RaiseException {
        int status = 0;
        try {
            runtime.parseFromMain(in, filename);
            config.getOutput().println("Syntax OK for " + filename);
        } catch (RaiseException re) {
            status = -1;
            if (re.getException().getMetaClass().getBaseName().equals("SyntaxError")) {
                config.getOutput().println("SyntaxError in " + re.getException().message(runtime.getCurrentContext()));
            } else {
                throw re;
            }
        }
        if (config.getArgv().length > 0) {
            for (String arg : config.getArgv()) {
                File argFile = new File(arg);
                if (argFile.exists()) {
                    try {
                        runtime.parseFromMain(new FileInputStream(argFile), arg);
                        config.getOutput().println("Syntax OK for " + arg);
                    } catch (FileNotFoundException fnfe) {
                        status = -1;
                        config.getOutput().println("File not found: " + arg);
                    } catch (RaiseException re) {
                        status = -1;
                        if (re.getException().getMetaClass().getBaseName().equals("SyntaxError")) {
                            config.getOutput().println("SyntaxError in " + re.getException().message(runtime.getCurrentContext()));
                        } else {
                            throw re;
                        }
                    }
                } else {
                    status = -1;
                    config.getOutput().println("File not found: " + arg);
                }
            }
        }
        return new Status(status);
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

    private void doProcessArguments(InputStream in) {
        String[] args = config.parseShebangOptions(in);
        if (args.length > 0) {
            config.processArguments(args);
        }
    }

    private void doPrintProperties() {
        if (config.shouldPrintProperties()) {
            config.getOutput().print(config.getPropertyHelp());
        }
    }

    private void doPrintUsage(boolean force) {
        if (config.shouldPrintUsage() || force) {
            config.getOutput().print(config.getBasicUsageHelp());
        }
    }

    private void doShowCopyright() {
        if (config.isShowCopyright()) {
            config.getOutput().println(config.getCopyrightString());
        }
    }

    private void doShowVersion() {
        if (config.isShowVersion()) {
            config.getOutput().println(config.getVersionString());
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

    private static int handleRaiseException(RaiseException rj) {
        RubyException raisedException = rj.getException();
        Ruby runtime = raisedException.getRuntime();
        if (runtime.getSystemExit().isInstance(raisedException)) {
            IRubyObject status = raisedException.callMethod(runtime.getCurrentContext(), "status");
            if (status != null && !status.isNil()) {
                return RubyNumeric.fix2int(status);
            } else {
                return 0;
            }
        } else {
            System.err.print(runtime.getInstanceConfig().getTraceType().printBacktrace(raisedException));
            return 1;
        }
    }

    private final RubyInstanceConfig config;
}

