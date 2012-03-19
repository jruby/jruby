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
 * Copyright (C) 2007-2011 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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
package org.jruby.util.cli;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.MainExitException;
import org.jruby.util.JRubyFile;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulated logic for processing JRuby's command-line arguments.
 * 
 * This class holds the processing logic for JRuby's non-JVM command-line arguments.
 * All standard Ruby options are processed here, as well as nonstandard JRuby-
 * specific options.
 * 
 * Options passed directly to the JVM are processed separately, by either a launch
 * script or by a native executable.
 */
public class ArgumentProcessor {
    private final class Argument {
        public final String originalValue;
        public final String dashedValue;
        public Argument(String value, boolean dashed) {
            this.originalValue = value;
            this.dashedValue = dashed && !value.startsWith("-") ? "-" + value : value;
        }
    }

    private List<Argument> arguments;
    private int argumentIndex = 0;
    private boolean processArgv;
    RubyInstanceConfig config;
    private boolean endOfArguments = false;
    private int characterIndex = 0;

    public ArgumentProcessor(String[] arguments, RubyInstanceConfig config) {
        this(arguments, true, false, config);
    }

    public ArgumentProcessor(String[] arguments, boolean processArgv, boolean dashed, RubyInstanceConfig config) {
        this.config = config;
        this.arguments = new ArrayList<Argument>();
        if (arguments != null && arguments.length > 0) {
            for (String argument : arguments) {
                this.arguments.add(new Argument(argument, dashed));
            }
        }
        this.processArgv = processArgv;
    }

    public void processArguments() {
        processArguments(true);
    }

    public void processArguments(boolean inline) {
        while (argumentIndex < arguments.size() && isInterpreterArgument(arguments.get(argumentIndex).originalValue)) {
            processArgument();
            argumentIndex++;
        }
        if (inline && !config.isInlineScript() && config.getScriptFileName() == null) {
            if (argumentIndex < arguments.size()) {
                config.setScriptFileName(arguments.get(argumentIndex).originalValue); //consume the file name
                argumentIndex++;
            }
        }
        if (processArgv) {
            processArgv();
        }
    }

    private void processArgv() {
        List<String> arglist = new ArrayList<String>();
        for (; argumentIndex < arguments.size(); argumentIndex++) {
            String arg = arguments.get(argumentIndex).originalValue;
            if (config.isArgvGlobalsOn() && arg.startsWith("-")) {
                arg = arg.substring(1);
                if (arg.indexOf('=') > 0) {
                    String[] keyvalue = arg.split("=", 2);
                    config.getOptionGlobals().put(keyvalue[0], keyvalue[1]);
                } else {
                    config.getOptionGlobals().put(arg, null);
                }
            } else {
                config.setArgvGlobalsOn(false);
                arglist.add(arg);
            }
        }
        // Remaining arguments are for the script itself
        arglist.addAll(Arrays.asList(config.getArgv()));
        config.setArgv(arglist.toArray(new String[arglist.size()]));
    }

    private boolean isInterpreterArgument(String argument) {
        return argument.length() > 0 && (argument.charAt(0) == '-' || argument.charAt(0) == '+') && !endOfArguments;
    }

    private String getArgumentError(String additionalError) {
        return "jruby: invalid argument\n" + additionalError + "\n";
    }

    private void processArgument() {
        String argument = arguments.get(argumentIndex).dashedValue;
        FOR:
        for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
            switch (argument.charAt(characterIndex)) {
                case '0':
                    {
                        String temp = grabOptionalValue();
                        if (null == temp) {
                            config.setRecordSeparator("\u0000");
                        } else if (temp.equals("0")) {
                            config.setRecordSeparator("\n\n");
                        } else if (temp.equals("777")) {
                            config.setRecordSeparator("\uffff"); // Specify something that can't separate
                        } else {
                            try {
                                int val = Integer.parseInt(temp, 8);
                                config.setRecordSeparator("" + (char) val);
                            } catch (Exception e) {
                                MainExitException mee = new MainExitException(1, getArgumentError(" -0 must be followed by either 0, 777, or a valid octal value"));
                                mee.setUsageError(true);
                                throw mee;
                            }
                        }
                        break FOR;
                    }
                case 'a':
                    config.setSplit(true);
                    break;
                case 'b':
                    config.setBenchmarking(true);
                    break;
                case 'c':
                    config.setShouldCheckSyntax(true);
                    break;
                case 'C':
                    try {
                        String saved = grabValue(getArgumentError(" -C must be followed by a directory expression"));
                        File base = new File(config.getCurrentDirectory());
                        File newDir = new File(saved);
                        if (newDir.isAbsolute()) {
                            config.setCurrentDirectory(newDir.getCanonicalPath());
                        } else {
                            config.setCurrentDirectory(new File(base, newDir.getPath()).getCanonicalPath());
                        }
                        if (!(new File(config.getCurrentDirectory()).isDirectory())) {
                            MainExitException mee = new MainExitException(1, "jruby: Can't chdir to " + saved + " (fatal)");
                            throw mee;
                        }
                    } catch (IOException e) {
                        MainExitException mee = new MainExitException(1, getArgumentError(" -C must be followed by a valid directory"));
                        throw mee;
                    }
                    break FOR;
                case 'd':
                    config.setDebug(true);
                    config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                    break;
                case 'e':
                    config.getInlineScript().append(grabValue(getArgumentError(" -e must be followed by an expression to evaluate")));
                    config.getInlineScript().append('\n');
                    config.setHasInlineScript(true);
                    break FOR;
                case 'E':
                    processEncodingOption(grabValue(getArgumentError("unknown encoding name")));
                    break FOR;
                case 'F':
                    config.setInputFieldSeparator(grabValue(getArgumentError(" -F must be followed by a pattern for input field separation")));
                    break FOR;
                case 'h':
                    config.setShouldPrintUsage(true);
                    config.setShouldRunInterpreter(false);
                    break;
                case 'i':
                    config.setInPlaceBackupExtension(grabOptionalValue());
                    if (config.getInPlaceBackupExtension() == null) {
                        config.setInPlaceBackupExtension("");
                    }
                    break FOR;
                case 'I':
                    String s = grabValue(getArgumentError("-I must be followed by a directory name to add to lib path"));
                    String[] ls = s.split(java.io.File.pathSeparator);
                    config.getLoadPaths().addAll(Arrays.asList(ls));
                    break FOR;
                case 'J':
                    grabOptionalValue();
                    config.getError().println("warning: " + argument + " argument ignored (launched in same VM?)");
                    break FOR;
                case 'K':
                    // FIXME: No argument seems to work for -K in MRI plus this should not
                    // siphon off additional args 'jruby -K ~/scripts/foo'.  Also better error
                    // processing.
                    String eArg = grabValue(getArgumentError("provide a value for -K"));
                    config.setKCode(KCode.create(null, eArg));
                    break;
                case 'l':
                    config.setProcessLineEnds(true);
                    break;
                case 'n':
                    config.setAssumeLoop(true);
                    break;
                case 'p':
                    config.setAssumePrinting(true);
                    config.setAssumeLoop(true);
                    break;
                case 'r':
                    config.getRequiredLibraries().add(grabValue(getArgumentError("-r must be followed by a package to require")));
                    break FOR;
                case 's':
                    config.setArgvGlobalsOn(true);
                    break;
                case 'G':
                    config.setLoadGemfile(true);
                    break;
                case 'S':
                    runBinScript();
                    break FOR;
                case 'T':
                    {
                        String temp = grabOptionalValue();
                        int value = 1;
                        if (temp != null) {
                            try {
                                value = Integer.parseInt(temp, 8);
                            } catch (Exception e) {
                                value = 1;
                            }
                        }
                        config.setSafeLevel(value);
                        break FOR;
                    }
                case 'U':
                    config.setInternalEncoding("UTF-8");
                    break;
                case 'v':
                    config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                    config.setShowVersion(true);
                    break;
                case 'w':
                    config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                    break;
                case 'W':
                    {
                        String temp = grabOptionalValue();
                        int value = 2;
                        if (null != temp) {
                            if (temp.equals("2")) {
                                value = 2;
                            } else if (temp.equals("1")) {
                                value = 1;
                            } else if (temp.equals("0")) {
                                value = 0;
                            } else {
                                MainExitException mee = new MainExitException(1, getArgumentError(" -W must be followed by either 0, 1, 2 or nothing"));
                                mee.setUsageError(true);
                                throw mee;
                            }
                        }
                        switch (value) {
                            case 0:
                                config.setVerbosity(RubyInstanceConfig.Verbosity.NIL);
                                break;
                            case 1:
                                config.setVerbosity(RubyInstanceConfig.Verbosity.FALSE);
                                break;
                            case 2:
                                config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                                break;
                        }
                        break FOR;
                    }
                case 'x':
                    try {
                        String saved = grabOptionalValue();
                        if (saved != null) {
                            File base = new File(config.getCurrentDirectory());
                            File newDir = new File(saved);
                            if (newDir.isAbsolute()) {
                                config.setCurrentDirectory(newDir.getCanonicalPath());
                            } else {
                                config.setCurrentDirectory(new File(base, newDir.getPath()).getCanonicalPath());
                            }
                            if (!(new File(config.getCurrentDirectory()).isDirectory())) {
                                MainExitException mee = new MainExitException(1, "jruby: Can't chdir to " + saved + " (fatal)");
                                throw mee;
                            }
                        }
                        config.setXFlag(true);
                    } catch (IOException e) {
                        MainExitException mee = new MainExitException(1, getArgumentError(" -x must be followed by a valid directory"));
                        throw mee;
                    }
                    break FOR;
                case 'X':
                    String extendedOption = grabOptionalValue();
                    if (extendedOption == null) {
                        if (SafePropertyAccessor.getBoolean("jruby.launcher.nopreamble", false)) {
                            throw new MainExitException(0, OutputStrings.getExtendedHelp());
                        } else {
                            throw new MainExitException(0, "jruby: missing argument\n" + OutputStrings.getExtendedHelp());
                        }
                    } else if (extendedOption.equals("-O")) {
                        config.setObjectSpaceEnabled(false);
                    } else if (extendedOption.equals("+O")) {
                        config.setObjectSpaceEnabled(true);
                    } else if (extendedOption.equals("-C")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
                    } else if (extendedOption.equals("-CIR")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.OFFIR);
                    } else if (extendedOption.equals("+C")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.FORCE);
                    } else if (extendedOption.equals("+CIR")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.FORCEIR);
                    } else {
                        MainExitException mee = new MainExitException(1, "jruby: invalid extended option " + extendedOption + " (-X will list valid options)\n");
                        mee.setUsageError(true);
                        throw mee;
                    }
                    break FOR;
                case 'y':
                    config.setParserDebug(true);
                    break FOR;
                case '-':
                    if (argument.equals("--command") || argument.equals("--bin")) {
                        characterIndex = argument.length();
                        runBinScript();
                        break;
                    } else if (argument.equals("--compat")) {
                        characterIndex = argument.length();
                        config.setCompatVersion(CompatVersion.getVersionFromString(grabValue(getArgumentError("--compat must be RUBY1_8 or RUBY1_9"))));
                        break FOR;
                    } else if (argument.equals("--copyright")) {
                        config.setShowCopyright(true);
                        config.setShouldRunInterpreter(false);
                        break FOR;
                    } else if (argument.equals("--debug")) {
                        RubyInstanceConfig.FULL_TRACE_ENABLED = true;
                        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
                        break FOR;
                    } else if (argument.equals("--jdb")) {
                        config.setDebug(true);
                        config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                        break;
                    } else if (argument.equals("--help")) {
                        config.setShouldPrintUsage(true);
                        config.setShouldRunInterpreter(false);
                        break;
                    } else if (argument.equals("--properties")) {
                        config.setShouldPrintProperties(true);
                        config.setShouldRunInterpreter(false);
                        break;
                    } else if (argument.equals("--version")) {
                        config.setShowVersion(true);
                        config.setShouldRunInterpreter(false);
                        break FOR;
                    } else if (argument.equals("--bytecode")) {
                        config.setShowBytecode(true);
                        break FOR;
                    } else if (argument.equals("--fast")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.FORCE);
                        RubyInstanceConfig.FASTOPS_COMPILE_ENABLED = true;
                        RubyInstanceConfig.FASTSEND_COMPILE_ENABLED = true;
                        RubyInstanceConfig.INLINE_DYNCALL_ENABLED = true;
                        break FOR;
                    } else if (argument.equals("--profile.api")) {
                        config.setProfilingMode(RubyInstanceConfig.ProfilingMode.API);
                        break FOR;
                    } else if (argument.equals("--profile") || argument.equals("--profile.flat")) {
                        config.setProfilingMode(RubyInstanceConfig.ProfilingMode.FLAT);
                        break FOR;
                    } else if (argument.equals("--profile.graph")) {
                        config.setProfilingMode(RubyInstanceConfig.ProfilingMode.GRAPH);
                        break FOR;
                    } else if (argument.equals("--profile.html")) {
                        config.setProfilingMode(RubyInstanceConfig.ProfilingMode.HTML);
                        break FOR;
                    } else if (argument.equals("--1.9")) {
                        config.setCompatVersion(CompatVersion.RUBY1_9);
                        break FOR;
                    } else if (argument.equals("--2.0")) {
                        config.setCompatVersion(CompatVersion.RUBY2_0);
                        break FOR;
                    } else if (argument.equals("--1.8")) {
                        config.setCompatVersion(CompatVersion.RUBY1_8);
                        break FOR;
                    } else if (argument.equals("--disable-gems")) {
                        config.setDisableGems(true);
                        break FOR;
                    } else if (argument.equals("--gemfile")) {
                        config.setLoadGemfile(true);
                        break FOR;
                    } else {
                        if (argument.equals("--")) {
                            // ruby interpreter compatibilty
                            // Usage: ruby [switches] [--] [programfile] [arguments])
                            endOfArguments = true;
                            break;
                        }
                    }
                default:
                    throw new MainExitException(1, "jruby: unknown option " + argument);
            }
        }
    }

    private void processEncodingOption(String value) {
        String[] encodings = value.split(":", 3);
        switch (encodings.length) {
            case 3:
                throw new MainExitException(1, "extra argument for -E: " + encodings[2]);
            case 2:
                config.setInternalEncoding(encodings[1]);
            case 1:
                config.setExternalEncoding(encodings[0]);
                // Zero is impossible
        }
    }

    private void runBinScript() {
        String scriptName = grabValue("jruby: provide a bin script to execute");
        if (scriptName.equals("irb")) {
            scriptName = "jirb";
        }
        config.setScriptFileName(resolveScript(scriptName));
        // run as a command if we couldn't find a script
        if (config.getScriptFileName() == null) {
            config.setScriptFileName(scriptName);
            config.getRequiredLibraries().add("jruby/commands");
            config.getInlineScript().append("JRuby::Commands.").append(scriptName);
            config.getInlineScript().append("\n");
            config.setHasInlineScript(true);
        }
        endOfArguments = true;
    }

    private String resolveScript(String scriptName) {
        // These try/catches are to allow failing over to the "commands" logic
        // when running from within a jruby-complete jar file, which has
        // jruby.home = a jar file URL that does not resolve correctly with
        // JRubyFile.create.
        File fullName = null;
        try {
            // try cwd first
            fullName = JRubyFile.create(config.getCurrentDirectory(), scriptName);
            if (fullName.exists() && fullName.isFile()) {
                if (RubyInstanceConfig.DEBUG_SCRIPT_RESOLUTION) {
                    config.getError().println("Found: " + fullName.getAbsolutePath());
                }
                return scriptName;
            }
        } catch (Exception e) {
            // keep going, try bin/#{scriptName}
        }
        try {
            fullName = JRubyFile.create(config.getJRubyHome(), "bin/" + scriptName);
            if (fullName.exists() && fullName.isFile()) {
                if (RubyInstanceConfig.DEBUG_SCRIPT_RESOLUTION) {
                    config.getError().println("Found: " + fullName.getAbsolutePath());
                }
                return fullName.getAbsolutePath();
            }
        } catch (Exception e) {
            // keep going, try PATH
        }
        if(Ruby.getClassLoader().getResourceAsStream("bin/" + scriptName) != null){
            return "classpath:bin/" + scriptName;
        }
        try {
            Object pathObj = config.getEnvironment().get("PATH");
            String path = pathObj.toString();
            if (path != null) {
                String[] paths = path.split(System.getProperty("path.separator"));
                for (int i = 0; i < paths.length; i++) {
                    fullName = JRubyFile.create(new File(paths[i]).getAbsolutePath(), scriptName);
                    if (fullName.exists() && fullName.isFile()) {
                        if (RubyInstanceConfig.DEBUG_SCRIPT_RESOLUTION) {
                            config.getError().println("Found: " + fullName.getAbsolutePath());
                        }
                        return fullName.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            // will fall back to JRuby::Commands
        }
        if (config.isDebug()) {
            config.getError().println("warning: could not resolve -S script on filesystem: " + scriptName);
        }
        return null;
    }

    private String grabValue(String errorMessage) {
        String optValue = grabOptionalValue();
        if (optValue != null) {
            return optValue;
        }
        argumentIndex++;
        if (argumentIndex < arguments.size()) {
            return arguments.get(argumentIndex).originalValue;
        }
        MainExitException mee = new MainExitException(1, errorMessage);
        mee.setUsageError(true);
        throw mee;
    }

    private String grabOptionalValue() {
        characterIndex++;
        String argValue = arguments.get(argumentIndex).originalValue;
        if (characterIndex < argValue.length()) {
            return argValue.substring(characterIndex);
        }
        return null;
    }
    
}
