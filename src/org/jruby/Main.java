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

import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;
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
    private RubyInstanceConfig config;

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
        Main main = new Main();
        int status = main.run(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    public int run(String[] args) {
        config.processArguments(args);
        
        return run();
    }

    public int run() {
        if (config.isShowVersion()) {
            showVersion();
        }

        if (! config.shouldRunInterpreter() ) {
            if (config.shouldPrintUsage()) {
                printUsage();
            }
            if (config.shouldPrintProperties()) {
                printProperties();
            }
            return 0;
        }

        long now = -1;
        if (config.isBenchmarking()) {
            now = System.currentTimeMillis();
        }

        int status;

        try {
            status = runInterpreter();
        } catch (MainExitException mee) {
            config.getOutput().println(mee.getMessage());
            if (mee.isUsageError()) {
                printUsage();
            }
            status = mee.getStatus();
        }

        if (config.isBenchmarking()) {
            config.getOutput().println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
        }

        return status;
    }

    private void showVersion() {
        config.getOutput().print("ruby ");
        config.getOutput().print(Constants.RUBY_VERSION);
        config.getOutput().print(" (");
        config.getOutput().print(Constants.COMPILE_DATE + " rev " + Constants.REVISION);
        config.getOutput().print(") [");
        config.getOutput().print(System.getProperty("os.arch") + "-jruby" + Constants.VERSION);
        config.getOutput().println("]");
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

    private int runInterpreter() {
        InputStream in   = config.getScriptSource();
        String filename = config.displayedFileName();
        final Ruby runtime = Ruby.newInstance(config);
        runtime.setKCode(config.getKCode());
        
        if (config.isSamplingEnabled()) {
            SimpleSampler.startSampleThread();
        }
        
        if (config.isVerbose()) {
            runtime.setVerbose(runtime.getTrue());
        }

        if (in == null) {
            // no script to run, return success
            return 0;
        }

        try {
            runInterpreter(runtime, in, filename);
            return 0;
        } catch (RaiseException rj) {
            RubyException raisedException = rj.getException();
            if (runtime.fastGetClass("SystemExit").isInstance(raisedException)) {
                IRubyObject status = raisedException.callMethod(runtime.getCurrentContext(), "status");

                if (status != null && !status.isNil()) {
                    return RubyNumeric.fix2int(status);
                } else {
                    return 0;
                }
            } else {
                runtime.printError(raisedException);
                return 1;
            }
        } catch (JumpException.ThrowJump tj) {
            return 1;
        } catch(MainExitException e) {
            if(e.isAborted()) {
                return e.getStatus();
            } else {
                throw e;
            }
        } finally {
            // Dump the contents of the runtimeInformation map.
            // This map can be used at development-time to log profiling information
            // that must be updated as the execution runs.
            if (!Ruby.isSecurityRestricted() && !runtime.getRuntimeInformation().isEmpty()) {
                System.err.println("Runtime information dump:");

                for (Object key: runtime.getRuntimeInformation().keySet()) {
                    System.err.println("[" + key + "]: " + runtime.getRuntimeInformation().get(key));
                }
            }
            if(config.isSamplingEnabled()) {
                org.jruby.util.SimpleSampler.report();
            }
        }
    }

    private void runInterpreter(Ruby runtime, InputStream in, String filename) {
        try {
            initializeRuntime(runtime, config, filename);
            runtime.runFromMain(in, filename);
        } finally {
            runtime.tearDown();
        }
    }

    private void initializeRuntime(final Ruby runtime, RubyInstanceConfig commandline, String filename) {
        runtime.setVerbose(runtime.newBoolean(commandline.isVerbose()));
        runtime.setDebug(runtime.newBoolean(commandline.isDebug()));

        //
        // FIXME: why "constant" and not global variable? this doesn't seem right,
        // $VERBOSE is set as global var elsewhere.
        //
//        runtime.getObject().setConstant("$VERBOSE",
//                commandline.isVerbose() ? runtime.getTrue() : runtime.getNil());

        // storing via internal var for now, as setConstant will now fail
        // validation
        runtime.getObject().setInternalVariable("$VERBOSE",
                commandline.isVerbose() ? runtime.getTrue() : runtime.getNil());
        //
        //

        defineGlobal(runtime, "$-p", commandline.isAssumePrinting());
        defineGlobal(runtime, "$-n", commandline.isAssumeLoop());
        defineGlobal(runtime, "$-a", commandline.isSplit());
        defineGlobal(runtime, "$-l", commandline.isProcessLineEnds());

        IAccessor d = new ValueAccessor(runtime.newString(filename));
        runtime.getGlobalVariables().define("$PROGRAM_NAME", d);
        runtime.getGlobalVariables().define("$0", d);

        runtime.getLoadService().init(commandline.loadPaths());
        
        for (String scriptName : commandline.requiredLibraries()) {
            RubyKernel.require(runtime.getTopSelf(), runtime.newString(scriptName), Block.NULL_BLOCK);
        }
    }

    private void defineGlobal(Ruby runtime, String name, boolean value) {
        runtime.getGlobalVariables().defineReadonly(name, new ValueAccessor(value ? runtime.getTrue() : runtime.getNil()));
    }
}
