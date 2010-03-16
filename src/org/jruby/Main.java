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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private boolean hasPrintedUsage = false;
    private final RubyInstanceConfig config;

    public Main(RubyInstanceConfig config) {
        this.config = config;
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

    public static void main(String[] args) {
        // Ensure we're not running on GCJ, since it's not supported and leads to weird errors
        if (Platform.IS_GCJ) {
            System.err.println("Fatal: GCJ (GNU Compiler for Java) is not supported by JRuby.");
            System.exit(1);
        }
        
        Main main = new Main();
        
        try {
            Status status = main.run(args);
            if (status.isExit()) {
                System.exit(status.getStatus());
            }
        } catch (RaiseException rj) {
            RubyException raisedException = rj.getException();
            Ruby runtime = raisedException.getRuntime();
            if (runtime.getSystemExit().isInstance(raisedException)) {
                IRubyObject status = raisedException.callMethod(runtime.getCurrentContext(), "status");

                if (status != null && !status.isNil()) {
                    System.exit(RubyNumeric.fix2int(status));
                }
            } else {
                rj.printStackTrace(System.err);
                System.exit(1);
            }
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
            if (!mee.isAborted()) {
                config.getOutput().println(mee.getMessage());
                if (mee.isUsageError()) {
                    printUsage();
                }
            }
            return new Status(mee.getStatus());
        } catch (OutOfMemoryError oome) {
            // produce a nicer error since Rubyists aren't used to seeing this
            System.gc();
            
            String memoryMax = SafePropertyAccessor.getProperty("jruby.memory.max");
            String message = "";
            if (memoryMax != null) {
                message = " of " + memoryMax;
            }
            config.getError().println("Error: Your application used more memory than the safety cap" + message + ".");
            config.getError().println("Specify -J-Xmx####m to increase it (#### = cap size in MB).");
            
            if (config.getVerbose()) {
                config.getError().println("Exception trace follows:");
                oome.printStackTrace();
            } else {
                config.getError().println("Specify -w for full OutOfMemoryError stack trace");
            }
            return new Status(1);
        } catch (StackOverflowError soe) {
            // produce a nicer error since Rubyists aren't used to seeing this
            System.gc();
            
            String stackMax = SafePropertyAccessor.getProperty("jruby.stack.max");
            String message = "";
            if (stackMax != null) {
                message = " of " + stackMax;
            }
            config.getError().println("Error: Your application used more stack memory than the safety cap" + message + ".");
            config.getError().println("Specify -J-Xss####k to increase it (#### = cap size in KB).");
            
            if (config.getVerbose()) {
                config.getError().println("Exception trace follows:");
                soe.printStackTrace();
            } else {
                config.getError().println("Specify -w for full StackOverflowError stack trace");
            }
            return new Status(1);
        } catch (UnsupportedClassVersionError ucve) {
            config.getError().println("Error: Some library (perhaps JRuby) was built with a later JVM version.");
            config.getError().println("Please use libraries built with the version you intend to use or an earlier one.");
            
            if (config.getVerbose()) {
                config.getError().println("Exception trace follows:");
                ucve.printStackTrace();
            } else {
                config.getError().println("Specify -w for full UnsupportedClassVersionError stack trace");
            }
            return new Status(1);
        } catch (ThreadKill kill) {
            return new Status();
        }
    }

    public Status run() {
        if (config.isShowVersion()) {
            showVersion();
        }
        
        if (config.isShowCopyright()) {
            showCopyright();
        }

        if (!config.shouldRunInterpreter() ) {
            if (config.shouldPrintUsage()) {
                printUsage();
            }
            if (config.shouldPrintProperties()) {
                printProperties();
            }
            return new Status();
        }

        InputStream in   = config.getScriptSource();
        String filename  = config.displayedFileName();

        String[] args = parseShebangOptions(in);
        if (args.length > 0) {
            config.processArguments(args);
        }
        Ruby runtime     = Ruby.newInstance(config);

        // set thread context JRuby classloader here, for the main thread
        try {
            Thread.currentThread().setContextClassLoader(runtime.getJRubyClassLoader());
        } catch (SecurityException se) {
            // can't set TC classloader
            if (runtime.getInstanceConfig().isVerbose()) {
                config.getError().println("WARNING: Security restrictions disallowed setting context classloader for main thread.");
            }
        }

        if (in == null) {
            // no script to run, return success below
        } else if (config.isxFlag() && !config.hasShebangLine()) {
            // no shebang was found and x option is set
            throw new MainExitException(1, "jruby: no Ruby script found in input (LoadError)");
        } else if (config.isShouldCheckSyntax()) {
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
        } else {
            long now = -1;

            try {
                if (config.isBenchmarking()) {
                    now = System.currentTimeMillis();
                }

                if (config.isSamplingEnabled()) {
                    SimpleSampler.startSampleThread();
                }

                try {
                    runtime.runFromMain(in, filename);
                } finally {
                    runtime.tearDown();

                    if (config.isBenchmarking()) {
                        config.getOutput().println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
                    }

                    if (config.isSamplingEnabled()) {
                        org.jruby.util.SimpleSampler.report();
                    }
                }
            } catch (RaiseException rj) {
                RubyException raisedException = rj.getException();
                if (runtime.getSystemExit().isInstance(raisedException)) {
                    IRubyObject status = raisedException.callMethod(runtime.getCurrentContext(), "status");

                    if (status != null && !status.isNil()) {
                        return new Status(RubyNumeric.fix2int(status));
                    } else {
                        return new Status(0);
                    }
                } else {
                    runtime.printError(raisedException);
                    return new Status(1);
                }
            }
        }
        return new Status();
    }

    private void showVersion() {
        config.getOutput().println(config.getVersionString());
    }

    private void showCopyright() {
        config.getOutput().println(config.getCopyrightString());
    }

    public void printUsage() {
        if (!hasPrintedUsage) {
            config.getOutput().print(config.getBasicUsageHelp());
            hasPrintedUsage = true;
        }
    }
    
    public void printProperties() {
        config.getOutput().print(config.getPropertyHelp());
    }
    /**
     * The intent here is to gather up any options that might have
     * been specified in the shebang line and return them so they can
     * be merged into the ones specified on the commandline.  This is
     * kind of a hopeless task because it's impossible to figure out
     * where the command invocation stops and the parameters start.
     * We try to work with the common scenarios where /usr/bin/env is
     * used to invoke the jruby shell script, and skip any parameters
     * it might have.  Then we look for the interpreter invokation and
     * assume that the binary will have the word "ruby" in the name.
     * This is error prone but should cover more cases than the
     * previous code.
     */
    private String[] parseShebangOptions(InputStream in) {
        BufferedReader reader = null;
        String[] result = new String[0];
        if (in == null) return result;
        try {
            in.mark(1024);
            reader = new BufferedReader(new InputStreamReader(in, "iso-8859-1"), 8192);
            String firstLine = reader.readLine();

            // Search for the shebang line in the given stream
            // if it wasn't found on the first line and the -x option
            // was specified
            if (config.isxFlag()) {
                while (firstLine != null && !isShebangLine(firstLine)) {
                    firstLine = reader.readLine();
                }
            }

            boolean usesEnv = false;
            if (firstLine.length() > 2 && firstLine.charAt(0) == '#' && firstLine.charAt(1) == '!') {
                String[] options = firstLine.substring(2).split("\\s+");
                int i;
                for (i = 0; i < options.length; i++) {
                    // Skip /usr/bin/env if it's first
                    if (i == 0 && options[i].endsWith("/env")) {
                        usesEnv = true;
                        continue;
                    }
                    // Skip any assignments if /usr/bin/env is in play
                    if (usesEnv && options[i].indexOf('=') > 0) {
                        continue;
                    }
                    // Skip any commandline args if /usr/bin/env is in play
                    if (usesEnv && options[i].startsWith("-")) {
                        continue;
                    }
                    String basename = (new File(options[i])).getName();
                    if (basename.indexOf("ruby") > 0) {
                        break;
                    }
                }
                config.setHasShebangLine(true);
                System.arraycopy(options, i, result, 0, options.length - i);
            } else {
                // No shebang line found
                config.setHasShebangLine(false);
            }
        } catch (Exception ex) {
            // ignore error
        } finally {
            try {
                in.reset();
            } catch (IOException ex) {}
        }
        return result;
    }

    public static class Status {
        private boolean isExit = false;
        private int status = 0;

        /**
         * Creates a status object with the specified value and with explicit
         * exit flag. An exit flag means that Kernel.exit() has been explicitly
         * invoked during the run.
         *
         * @param status
         *            The status value.
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

    protected boolean isShebangLine(String line) {
        return (line.length() > 2 && line.charAt(0) == '#' && line.charAt(1) == '!');
    }
}

