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
import java.io.Reader;
import java.util.Iterator;

import org.jruby.ast.Node;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.ParserSupport;
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CommandlineParser;
import org.jruby.ast.executable.YARVCompiledRunner;

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
    private CommandlineParser commandline;
    private boolean hasPrintedUsage = false;
    private RubyInstanceConfig config;
    // ENEBO: We used to have in, but we do not use it in this class anywhere
    private PrintStream out;
    private PrintStream err;

    public Main(RubyInstanceConfig config) {
        this.config = config;
        this.out    = config.getOutput();
        this.err    = config.getError();
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
        commandline = new CommandlineParser(this, args);

        if (commandline.isShowVersion()) {
            showVersion();
        }

        if (! commandline.shouldRunInterpreter()) {
            return 0;
        }

        long now = -1;
        if (commandline.isBenchmarking()) {
            now = System.currentTimeMillis();
        }

        int status;

        try {
            status = runInterpreter(commandline);
        } catch (MainExitException mee) {
            err.println(mee.getMessage());
            if (mee.isUsageError()) {
                printUsage();
            }
            status = mee.getStatus();
        }

        if (commandline.isBenchmarking()) {
            out.println("Runtime: " + (System.currentTimeMillis() - now) + " ms");
        }

        return status;
    }

    private void showVersion() {
        out.print("ruby ");
        out.print(Constants.RUBY_VERSION);
        out.print(" (");
        out.print(Constants.COMPILE_DATE + " rev " + Constants.REVISION);
        out.print(") [");
        out.print(System.getProperty("os.arch") + "-jruby" + Constants.VERSION);
        out.println("]");
    }

    public void printUsage() {
        if (!hasPrintedUsage) {
            out.println("Usage: jruby [switches] [--] [rubyfile.rb] [arguments]");
            out.println("    -e 'command'    one line of script. Several -e's allowed. Omit [programfile]");
            out.println("    -b              benchmark mode, times the script execution");
            out.println("    -Jjava option   pass an option on to the JVM (e.g. -J-Xmx512m)");
            out.println("    -Idirectory     specify $LOAD_PATH directory (may be used more than once)");
            out.println("    --              optional -- before rubyfile.rb for compatibility with ruby");
            out.println("    -d              set debugging flags (set $DEBUG to true)");
            out.println("    -v              print version number, then turn on verbose mode");
            out.println("    -O              run with ObjectSpace disabled (improves performance)");
            out.println("    -S cmd          run the specified command in JRuby's bin dir");
            out.println("    -C              pre-compile scripts before running (EXPERIMENTAL)");
            out.println("    -y              read a YARV-compiled Ruby script and run that (EXPERIMENTAL)");
            out.println("    -Y              compile a Ruby script into YARV bytecodes and run this (EXPERIMENTAL)");
            out.println("    --command word  Execute ruby-related shell command (i.e., irb, gem)");
            hasPrintedUsage = true;
        }
    }

    private int runInterpreter(CommandlineParser commandline) {
        Reader reader   = commandline.getScriptSource();
        String filename = commandline.displayedFileName();
        config.updateWithCommandline(commandline);
        final Ruby runtime = Ruby.newInstance(config);
        runtime.setKCode(commandline.getKCode());

        try {
            runInterpreter(runtime, reader, filename);
            return 0;
        } catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.RaiseJump) {
                RubyException raisedException = ((RaiseException)je).getException();
                if (raisedException.isKindOf(runtime.getClass("SystemExit"))) {
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
            } else if (je.getJumpType() == JumpException.JumpType.ThrowJump) {
                return 1;
            } else {
                throw je;
            }
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

                for (Iterator iter = runtime.getRuntimeInformation().keySet().iterator(); iter.hasNext();) {
                    Object key = iter.next();
                    System.err.println("[" + key + "]: " + runtime.getRuntimeInformation().get(key));
                }
            }
        }
    }

    private void runInterpreter(Ruby runtime, Reader reader, String filename) {
        try {
            initializeRuntime(runtime, filename);
            if(commandline.isYARVEnabled()) {
                new YARVCompiledRunner(runtime,reader,filename).run();
            } else {
                Node parsedScript = getParsedScript(runtime, reader, filename);
                if (commandline.isCompilerEnabled()) {
                    runtime.compileAndRun(parsedScript);
                } else if(commandline.isYARVCompileEnabled()) {
                    runtime.ycompileAndRun(parsedScript);
                } else {
                    runtime.compileOrFallbackAndRun(parsedScript);
                }
            }
        } finally {
            runtime.tearDown();
        }
    }

    private Node getParsedScript(Ruby runtime, Reader reader, String filename) {
        // current scope is top-level scope (what we set TOPLEVEL_BINDING to).
        Node result = runtime.parse(reader, filename, runtime.getCurrentContext().getCurrentScope(), 0);
        if (commandline.isAssumePrinting()) {
            result = new ParserSupport().appendPrintToBlock(result);
        }
        if (commandline.isAssumeLoop()) {
            result = new ParserSupport().appendWhileLoopToBlock(result, commandline.isProcessLineEnds(), commandline.isSplit());
        }
        return result;
    }

    private void initializeRuntime(final Ruby runtime, String filename) {
        IRubyObject argumentArray = runtime.newArrayNoCopy(JavaUtil.convertJavaArrayToRuby(runtime, commandline.getScriptArguments()));
        runtime.setVerbose(runtime.newBoolean(commandline.isVerbose()));
        runtime.setDebug(runtime.newBoolean(commandline.isDebug()));

        defineGlobalVERBOSE(runtime);
        defineGlobalDEBUG(runtime);

        runtime.getObject().setConstant("$VERBOSE",
                commandline.isVerbose() ? runtime.getTrue() : runtime.getNil());
        runtime.defineGlobalConstant("ARGV", argumentArray);

        defineGlobal(runtime, "$-p", commandline.isAssumePrinting());
        defineGlobal(runtime, "$-n", commandline.isAssumeLoop());
        defineGlobal(runtime, "$-a", commandline.isSplit());
        defineGlobal(runtime, "$-l", commandline.isProcessLineEnds());
        runtime.getGlobalVariables().defineReadonly("$*", new ValueAccessor(argumentArray));
        // TODO this is a fake cause we have no real process number in Java
        runtime.getGlobalVariables().defineReadonly("$$", new ValueAccessor(runtime.newFixnum(runtime.hashCode())));

        IAccessor d = new ValueAccessor(runtime.newString(filename));
        runtime.getGlobalVariables().define("$PROGRAM_NAME", d);
        runtime.getGlobalVariables().define("$0", d);

        runtime.getLoadService().init(commandline.loadPaths());
        Iterator iter = commandline.requiredLibraries().iterator();
        while (iter.hasNext()) {
            String scriptName = (String) iter.next();
            RubyKernel.require(runtime.getTopSelf(), runtime.newString(scriptName), Block.NULL_BLOCK);
        }
    }

    private void defineGlobalVERBOSE(final Ruby runtime) {
        // $VERBOSE can be true, false, or nil.  Any non-false-nil value will get stored as true
        runtime.getGlobalVariables().define("$VERBOSE", new IAccessor() {
            public IRubyObject getValue() {
                return runtime.getVerbose();
            }

            public IRubyObject setValue(IRubyObject newValue) {
                if (newValue.isNil()) {
                    runtime.setVerbose(newValue);
                } else {
                    runtime.setVerbose(runtime.newBoolean(newValue != runtime.getFalse()));
                }

                return newValue;
            }
        });
    }

    private void defineGlobalDEBUG(final Ruby runtime) {
        IAccessor d = new IAccessor() {
            public IRubyObject getValue() {
                return runtime.getDebug();
            }

            public IRubyObject setValue(IRubyObject newValue) {
                if (newValue.isNil()) {
                    runtime.setDebug(newValue);
                } else {
                    runtime.setDebug(runtime.newBoolean(newValue != runtime.getFalse()));
                }

                return newValue;
            }
            };
        runtime.getGlobalVariables().define("$DEBUG", d);
        runtime.getGlobalVariables().define("$-d", d);
    }

    private void defineGlobal(Ruby runtime, String name, boolean value) {
        runtime.getGlobalVariables().defineReadonly(name, new ValueAccessor(value ? runtime.getTrue() : runtime.getNil()));
    }
}
