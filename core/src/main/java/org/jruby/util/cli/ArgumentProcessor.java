/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007-2011 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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
package org.jruby.util.cli;

import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.MainExitException;
import org.jruby.runtime.profile.builtin.ProfileOutput;
import org.jruby.util.JRubyFile;
import org.jruby.util.FileResource;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.StringSupport;
import org.jruby.util.func.Function2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;

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

    public static final String SEPARATOR = "(?<!jar:file|jar|file|classpath|uri:classloader|uri|http|https):";

    private static final class Argument {
        final String originalValue;
        private String dashedValue;
        Argument(String value, boolean dashed) {
            this.originalValue = value;
            this.dashedValue = dashed ? null : value;
        }

        final String getDashedValue() {
            String dashedValue = this.dashedValue;
            if ( dashedValue == null ) {
                final String value = originalValue;
                dashedValue = ! value.startsWith("-") ? ('-' + value) : value;
                this.dashedValue = dashedValue;
            }
            return dashedValue;
        }

        public String toString() {
            return getDashedValue();
        }
    }

    private final List<Argument> arguments;
    private int argumentIndex = 0;
    private boolean processArgv;
    private final boolean rubyOpts;
    final RubyInstanceConfig config;
    private boolean endOfArguments = false;
    private int characterIndex = 0;

    private static final Pattern VERSION_FLAG = Pattern.compile("^--[12]\\.[89012]$");

    public ArgumentProcessor(String[] arguments, RubyInstanceConfig config) {
        this(arguments, true, false, false, config);
    }

    public ArgumentProcessor(String[] arguments, boolean processArgv, boolean dashed, boolean rubyOpts, RubyInstanceConfig config) {
        this.config = config;
        if (arguments != null && arguments.length > 0) {
            this.arguments = new ArrayList<>(arguments.length);
            for (String argument : arguments) {
                this.arguments.add(new Argument(argument, dashed));
            }
        }
        else {
            this.arguments = new ArrayList<>(0);
        }
        this.processArgv = processArgv;
        this.rubyOpts = rubyOpts;
    }

    public void processArguments() {
        processArguments(true);
    }

    public void processArguments(boolean inline) {
        checkProperties();

        while (argumentIndex < arguments.size() && isInterpreterArgument(arguments.get(argumentIndex).originalValue)) {
            processArgument();
            argumentIndex++;
        }
        if (inline && !config.isInlineScript() && config.getScriptFileName() == null && !config.isForceStdin()) {
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
        ArrayList<String> arglist = new ArrayList<String>();
        for (; argumentIndex < arguments.size(); argumentIndex++) {
            String arg = arguments.get(argumentIndex).originalValue;
            if (config.isArgvGlobalsOn() && arg.startsWith("-")) {
                arg = arg.substring(1);
                int split = arg.indexOf('=');
                if (split > 0) {
                    final String key = arg.substring(0, split);
                    final String val = arg.substring(split + 1);
                    // argv globals getService their dashes replaced with underscores
                    String globalName = key.replace('-', '_');
                    config.getOptionGlobals().put(globalName, val);
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
        String argument = arguments.get(argumentIndex).getDashedValue();

        if (argument.length() == 1) {
            // sole "-" means read from stdin and pass remaining args as ARGV
            endOfArguments = true;
            config.setForceStdin(true);
            return;
        }

        FOR:
        for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
            switch (argument.charAt(characterIndex)) {
                case '0':
                    {
                        disallowedInRubyOpts(argument);
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
                                config.setRecordSeparator(String.valueOf((char) val));
                            } catch (Exception e) {
                                MainExitException mee = new MainExitException(1, getArgumentError(" -0 must be followed by either 0, 777, or a valid octal value"));
                                mee.setUsageError(true);
                                throw mee;
                            }
                        }
                        break FOR;
                    }
                case 'a':
                    disallowedInRubyOpts(argument);
                    config.setSplit(true);
                    break;
                case 'c':
                    disallowedInRubyOpts(argument);
                    config.setShouldCheckSyntax(true);
                    break;
                case 'C':
                    disallowedInRubyOpts(argument);
                    try {
                        String saved = grabValue(getArgumentError(" -C must be followed by a directory expression"));
                        File base = new File(config.getCurrentDirectory());
                        File newDir = new File(saved);
                        if (saved.startsWith("uri:classloader:")) {
                            config.setCurrentDirectory(saved);
                        } else if (newDir.isAbsolute()) {
                            config.setCurrentDirectory(newDir.getCanonicalPath());
                        } else {
                            config.setCurrentDirectory(new File(base, newDir.getPath()).getCanonicalPath());
                        }
                        if (!(new File(config.getCurrentDirectory()).isDirectory()) && !config.getCurrentDirectory().startsWith("uri:classloader:")) {
                            throw new MainExitException(1, "jruby: Can't chdir to " + saved + " (fatal)");
                        }
                    } catch (IOException e) {
                        throw new MainExitException(1, getArgumentError(" -C must be followed by a valid directory"));
                    }
                    break FOR;
                case 'd':
                    config.setDebug(true);
                    config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                    break;
                case 'e':
                    disallowedInRubyOpts(argument);
                    config.getInlineScript().append(grabValue(getArgumentError(" -e must be followed by an expression to report")));
                    config.getInlineScript().append('\n');
                    config.setHasInlineScript(true);
                    break FOR;
                case 'E':
                    processEncodingOption(grabValue(getArgumentError("unknown encoding name")));
                    break FOR;
                case 'F':
                    disallowedInRubyOpts(argument);
                    config.setInputFieldSeparator(grabValue(getArgumentError(" -F must be followed by a pattern for input field separation")));
                    break FOR;
                case 'h':
                    disallowedInRubyOpts(argument);
                    config.setShouldPrintUsage(true);
                    config.setShouldRunInterpreter(false);
                    break;
                case 'i':
                    disallowedInRubyOpts(argument);
                    config.setInPlaceBackupExtension(grabOptionalValue());
                    if (config.getInPlaceBackupExtension() == null) {
                        config.setInPlaceBackupExtension("");
                    }
                    break FOR;
                case 'I':
                    String s = grabValue(getArgumentError("-I must be followed by a directory name to add to lib path"));
                    String separator = java.io.File.pathSeparator;
                    if (":".equals(separator)) {
                        separator = SEPARATOR;
                    }
                    String[] ls = s.split(separator);
                    config.getLoadPaths().addAll(Arrays.asList(ls));
                    break FOR;
                case 'J':
                    String js = grabOptionalValue();
                    config.getError().println("warning: " + argument + " argument ignored (launched in same VM?)");
                    if (js.equals("-cp") || js.equals("-classpath")) {
                        for(;grabOptionalValue() != null;) {}
                        grabValue(getArgumentError(" -J-cp must be followed by a path expression"));
                    }
                    break FOR;
                case 'K': // @Deprecated TODO no longer relevant in Ruby 2.x
                    String eArg = grabValue(getArgumentError("provide a value for -K"));

                    config.setKCode(KCode.create(eArg));

                    // source encoding
                    config.setSourceEncoding(config.getKCode().getEncoding().toString());

                    // set external encoding if not already specified
                    if (config.getExternalEncoding() == null) {
                        config.setExternalEncoding(config.getKCode().getEncoding().toString());
                    }

                    break;
                case 'l':
                    disallowedInRubyOpts(argument);
                    config.setProcessLineEnds(true);
                    break;
                case 'n':
                    disallowedInRubyOpts(argument);
                    config.setAssumeLoop(true);
                    config.setKernelGsubDefined(true);
                    break;
                case 'p':
                    disallowedInRubyOpts(argument);
                    config.setAssumePrinting(true);
                    config.setAssumeLoop(true);
                    config.setKernelGsubDefined(true);
                    break;
                case 'r':
                    config.getRequiredLibraries().add(grabValue(getArgumentError("-r must be followed by a package to require")));
                    break FOR;
                case 's':
                    disallowedInRubyOpts(argument);
                    config.setArgvGlobalsOn(true);
                    break;
                case 'G':
                    config.setLoadGemfile(true);
                    break;
                case 'S':
                    disallowedInRubyOpts(argument);
                    runBinScript();
                    break FOR;
                case 'T':
                    {
                        grabOptionalValue();
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
                        if (temp == null) {
                            config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                        } else {
                            if (temp.equals("0")) {
                                config.setVerbosity(RubyInstanceConfig.Verbosity.NIL);
                            } else if (temp.equals("1")) {
                                config.setVerbosity(RubyInstanceConfig.Verbosity.FALSE);
                            } else if (temp.equals("2")) {
                                config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                            } else {
                                MainExitException mee = new MainExitException(1, getArgumentError(" -W must be followed by either 0, 1, 2 or nothing"));
                                mee.setUsageError(true);
                                throw mee;
                            }
                        }
                        break FOR;
                    }
                case 'x':
                    disallowedInRubyOpts(argument);
                    try {
                        String saved = grabOptionalValue();
                        if (saved != null) {
                            File base = new File(config.getCurrentDirectory());
                            File newDir = new File(saved);
                            if (saved.startsWith("uri:classloader:")) {
                                config.setCurrentDirectory(saved);
                            } else if (newDir.isAbsolute()) {
                                config.setCurrentDirectory(newDir.getCanonicalPath());
                            } else {
                                config.setCurrentDirectory(new File(base, newDir.getPath()).getCanonicalPath());
                            }
                            if (!(new File(config.getCurrentDirectory()).isDirectory()) && !config.getCurrentDirectory().startsWith("uri:classloader:")) {
                                throw new MainExitException(1, "jruby: Can't chdir to " + saved + " (fatal)");
                            }
                        }
                        config.setXFlag(true);
                    } catch (IOException e) {
                        throw new MainExitException(1, getArgumentError(" -x must be followed by a valid directory"));
                    }
                    break FOR;
                case 'X':
                    disallowedInRubyOpts(argument);
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
                    } else if (extendedOption.equals("-C") || extendedOption.equals("-CIR")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
                    } else if (extendedOption.equals("+C") || extendedOption.equals("+CIR")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.FORCE);
                    } else if (extendedOption.equals("classic")) {
                        throw new MainExitException(0, "jruby: the -Xclassic option should have been handled in the launcher");
                    } else if (extendedOption.equals("+T") || extendedOption.equals("+TM")) {
                        Options.PARSER_WARN_GROUPED_EXPRESSIONS.force(Boolean.FALSE.toString());
                        config.setCompileMode(RubyInstanceConfig.CompileMode.TRUFFLE);
                        // Make the static option consistent with the compile mode.
                        Options.COMPILE_MODE.force("TRUFFLE");
                    } else if (extendedOption.endsWith("...")) {
                        Options.listPrefix(extendedOption.substring(0, extendedOption.length() - "...".length()));
                        config.setShouldRunInterpreter(false);
                    } else if (extendedOption.endsWith("?")) {
                        Options.listContains(extendedOption.substring(0, extendedOption.length() - 1));
                        config.setShouldRunInterpreter(false);
                    } else {
                        MainExitException mee = new MainExitException(1, "jruby: invalid extended option " + extendedOption + " (-X will list valid options)\n");
                        mee.setUsageError(true);
                        throw mee;
                    }
                    break FOR;
                case 'y':
                    disallowedInRubyOpts(argument);
                    config.setParserDebug(true);
                    break FOR;
                case '-':
                    if (argument.equals("--command") || argument.equals("--bin")) {
                        characterIndex = argument.length();
                        runBinScript();
                        break;
                    } else if (argument.equals("--compat")) {
                        characterIndex = argument.length();
                        grabValue(getArgumentError("--compat takes an argument, but will be ignored"));
                        config.getError().println("warning: " + argument + " ignored");
                        break FOR;
                    } else if (argument.equals("--copyright")) {
                        disallowedInRubyOpts(argument);
                        config.setShowCopyright(true);
                        config.setShouldRunInterpreter(false);
                        break FOR;
                    } else if (argument.equals("--debug")) {
                        disallowedInRubyOpts(argument);
                        Options.DEBUG_FULLTRACE.force("true");
                        RubyInstanceConfig.FULL_TRACE_ENABLED = true;
                        config.setDebuggingFrozenStringLiteral(true);
                        break FOR;
                    } else if (argument.startsWith("--debug=")) {
                        for (String debug : valueListFor(argument, "debug")) {
                            boolean all = debug.equals("all");
                            if (all || debug.equals("frozen-string-literal")) {
                                config.setDebuggingFrozenStringLiteral(true);
                                continue;
                            }

                            config.getError().println("warning: unknown argument for --debug: `" + debug + "'");
                        }
                        break FOR;
                    } else if (argument.equals("--jdb")) {
                        config.setDebug(true);
                        config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
                        break;
                    } else if (argument.equals("--help")) {
                        disallowedInRubyOpts(argument);
                        config.setShouldPrintUsage(true);
                        config.setShouldRunInterpreter(false);
                        break;
                    } else if (argument.equals("--properties")) {
                        config.setShouldPrintProperties(true);
                        config.setShouldRunInterpreter(false);
                        break;
                    } else if (argument.equals("--version")) {
                        disallowedInRubyOpts(argument);
                        config.setShowVersion(true);
                        config.setShouldRunInterpreter(false);
                        break FOR;
                    } else if (argument.equals("--bytecode")) {
                        config.setShowBytecode(true);
                        break FOR;
                    } else if (argument.equals("--fast")) {
                        config.setCompileMode(RubyInstanceConfig.CompileMode.FORCE);
                        break FOR;
                    } else if (argument.startsWith("--profile")) {
                        characterIndex = argument.length();
                        int dotIndex = argument.indexOf('.');

                        if (dotIndex == -1) {
                            config.setProfilingMode(RubyInstanceConfig.ProfilingMode.FLAT);

                        } else {
                            String profilingMode = argument.substring(dotIndex + 1, argument.length());

                            if (profilingMode.equals("out")) {
                                // output file for profiling results
                                String outputFile = grabValue(getArgumentError("--profile.out requires an output file argument"));

                                try {
                                    config.setProfileOutput(new ProfileOutput(new File(outputFile)));
                                } catch (FileNotFoundException e) {
                                    throw new MainExitException(1, String.format("jruby: %s", e.getMessage()));
                                }

                            } else if (profilingMode.equals("service")) {
                                // service class name
                                String service = grabValue(getArgumentError("--profile.service requires an class name argument"));

                                config.setProfilingMode( RubyInstanceConfig.ProfilingMode.SERVICE);
                                config.setProfilingService(service);

                            } else {
                                try {
                                    config.setProfilingMode(RubyInstanceConfig.ProfilingMode.valueOf(profilingMode.toUpperCase()));
                                } catch (IllegalArgumentException e) {
                                    throw new MainExitException(1, String.format("jruby: unknown profiler mode \"%s\"", profilingMode));
                                }
                            }
                        }

                        break FOR;
                    } else if (VERSION_FLAG.matcher(argument).matches()) {
                        config.getError().println("warning: " + argument + " ignored");
                        break FOR;
                    } else if (argument.equals("--debug-frozen-string-literal")) {
                        config.setDebuggingFrozenStringLiteral(true);
                        break FOR;
                    } else if (argument.startsWith("--disable")) {
                        final int len = argument.length();
                        if (len == "--disable".length()) {
                            characterIndex = len;
                            String feature = grabValue(getArgumentError("missing argument for --disable"), false);
                            argument = "--disable=" + feature;
                        }
                        for (String disable : valueListFor(argument, "disable")) {
                            enableDisableFeature(disable, false);
                        }
                        break FOR;
                    } else if (argument.startsWith("--enable")) {
                        final int len = argument.length();
                        if (len == "--enable".length()) {
                            characterIndex = len;
                            String feature = grabValue(getArgumentError("missing argument for --enable"), false);
                            argument = "--enable=" + feature;
                        }
                        for (String enable : valueListFor(argument, "enable")) {
                            enableDisableFeature(enable, true);
                        }
                        break FOR;
                    } else if (argument.equals("--gemfile")) {
                        config.setLoadGemfile(true);
                        break FOR;
                    } else if (argument.equals("--dump")) {
                        characterIndex = argument.length();
                        String error = "--dump only supports [version, copyright, usage, yydebug, syntax, insns] on JRuby";
                        String dumpArg = grabValue(getArgumentError(error));
                        if (dumpArg.equals("version")) {
                            config.setShowVersion(true);
                            config.setShouldRunInterpreter(false);
                            break FOR;
                        } else if (dumpArg.equals("copyright")) {
                            config.setShowCopyright(true);
                            config.setShouldRunInterpreter(false);
                            break FOR;
                        } else if (dumpArg.equals("usage")) {
                            config.setShouldPrintUsage(true);
                            config.setShouldRunInterpreter(false);
                            break FOR;
                        } else if (dumpArg.equals("yydebug")) {
                            config.setParserDebug(true);
                            break FOR;
                        } else if (dumpArg.equals("syntax")) {
                            config.setShouldCheckSyntax(true);
                        } else if (dumpArg.equals("insns")) {
                            config.setShowBytecode(true);
                        } else {
                            MainExitException mee = new MainExitException(1, error);
                            mee.setUsageError(true);
                            throw mee;
                        }
                        break;
                    } else if (argument.equals("--dev")) {
                        // most we can do after JVM boot
                        Options.COMPILE_INVOKEDYNAMIC.force("false");
                        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
                        break FOR;
                    } else if (argument.equals("--server")) {
                        // ignore this...can't do anything with it after boot
                        break FOR;
                    } else if (argument.equals("--client")) {
                        // ignore this...can't do anything with it after boot
                        break FOR;
                    } else if (argument.equals("--yydebug")) {
                        disallowedInRubyOpts(argument);
                        config.setParserDebug(true);
                    } else if (argument.equals("--verbose")) {
                        config.setVerbosity(RubyInstanceConfig.Verbosity.TRUE);
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

    private void enableDisableFeature(String name, boolean enable) {
        Function2<Boolean, ArgumentProcessor, Boolean> feature = FEATURES.get(name);

        if (feature == null) {
            config.getError().println("warning: unknown argument for --" + (enable ? "enable" : "disable") + ": `" + name + "'");
        } else {
            feature.apply(this, enable);
        }
    }

    private static String[] valueListFor(String argument, String key) {
        int length = key.length() + 3; // 3 is from -- and = (e.g. --disable=)
        String[] values = argument.substring(length).split(",");

        if (values.length == 0) errorMissingEquals(key);

        return values;
    }

    private void disallowedInRubyOpts(CharSequence option) {
        if (rubyOpts) {
            throw new MainExitException(1, "jruby: invalid switch in RUBYOPT: " + option + " (RuntimeError)");
        }
    }

    private static void errorMissingEquals(String label) {
        MainExitException mee;
        mee = new MainExitException(1, "missing argument for --" + label + "\n");
        mee.setUsageError(true);
        throw mee;
    }

    private void processEncodingOption(String value) {
        List<String> encodings = StringSupport.split(value, ':', 3);
        switch (encodings.size()) {
            case 3:
                throw new MainExitException(1, "extra argument for -E: " + encodings.get(2));
            case 2:
                config.setInternalEncoding(encodings.get(1));
            case 1:
                config.setExternalEncoding(encodings.get(0));
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

    private String resolve(String path, String scriptName) {
        if (RubyInstanceConfig.DEBUG_SCRIPT_RESOLUTION) {
            config.getError().println("Trying path: " + path);
        }
        try {
            FileResource fullName = JRubyFile.createRestrictedResource(path, scriptName);
            if (fullName.exists() && fullName.isFile()) {
                if (RubyInstanceConfig.DEBUG_SCRIPT_RESOLUTION) {
                    config.getError().println("Found: " + fullName.absolutePath());
                }
                return fullName.absolutePath();
            }
        } catch (Exception e) {
            // keep going
        }
        return null;
    }

    private String resolveScript(String scriptName) {
        // These try/catches are to allow failing over to the "commands" logic
        // when running from within a jruby-complete jar file, which has
        // jruby.home = a jar file URL that does not resolve correctly with
        // JRubyFile.create.
        String result = resolve(config.getCurrentDirectory(), scriptName);
        if (result != null) return scriptName;// use relative filename
        result = resolve(config.getJRubyHome() + "/bin", scriptName);
        if (result != null) return result;
        // since the current directory is also on the classpath we
        // want to find it on filesystem first
        result = resolve(config.getCurrentDirectory() + "/bin", scriptName);
        if (result != null) return result;
        result = resolve("uri:classloader:/bin", scriptName);
        if (result != null) return result;

        Object maybePath = config.getEnvironment().get("PATH");
        if (maybePath != null) {
            String path = maybePath.toString();
            String[] paths = path.split(System.getProperty("path.separator"));
            for (int i = 0; i < paths.length; i++) {
                result = resolve(new File(paths[i]).getAbsolutePath(), scriptName);
                if (result != null) return result;
            }
        }
        if (config.isDebug()) {
            config.getError().println("warning: could not resolve -S script: " + scriptName);
        }
        // fall back to JRuby::Commands
        return null;
    }

    @Deprecated
    public String resolveScriptUsingClassLoader(String scriptName) {
        if (RubyInstanceConfig.defaultClassLoader().getResourceAsStream("bin/" + scriptName) != null){
            return "classpath:/bin/" + scriptName;
        }
        return null;
    }

    private String grabValue(String errorMessage) {
        return grabValue(errorMessage, true);
    }

    private String grabValue(String errorMessage, boolean usageError) {
        String optValue = grabOptionalValue();
        if (optValue != null) {
            return optValue;
        }
        argumentIndex++;
        if (argumentIndex < arguments.size()) {
            return arguments.get(argumentIndex).originalValue;
        }
        MainExitException mee = new MainExitException(1, errorMessage);
        if (usageError) mee.setUsageError(true);
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

    private static final Set<String> KNOWN_PROPERTIES = new HashSet<>(Options.PROPERTIES.size() + 16, 1);

    static {
        Options.addPropertyNames(KNOWN_PROPERTIES);
        KNOWN_PROPERTIES.add("jruby.home");
        KNOWN_PROPERTIES.add("jruby.script");
        KNOWN_PROPERTIES.add("jruby.shell");
        KNOWN_PROPERTIES.add("jruby.lib");
        KNOWN_PROPERTIES.add("jruby.bindir");
        KNOWN_PROPERTIES.add("jruby.jar");
        KNOWN_PROPERTIES.add("jruby.compat.version");
        KNOWN_PROPERTIES.add("jruby.reflection");
        KNOWN_PROPERTIES.add("jruby.thread.pool.enabled");
        KNOWN_PROPERTIES.add("jruby.memory.max");
        KNOWN_PROPERTIES.add("jruby.stack.max");
    }

    private static final List<String> KNOWN_PROPERTY_PREFIXES = new ArrayList<>(4);

    static {
        KNOWN_PROPERTY_PREFIXES.add("jruby.openssl.");
    }

    private static void checkProperties() {
        for (String propertyName : System.getProperties().stringPropertyNames()) {
            if (propertyName.startsWith("jruby.")) {
                if (!isPropertySupported(propertyName)) {
                    System.err.println("jruby: warning: unknown property " + propertyName);
                }
            }
        }
    }

    private static boolean isPropertySupported(String propertyName) {
        if (KNOWN_PROPERTIES.contains(propertyName)) {
            return true;
        }

        for (String prefix : KNOWN_PROPERTY_PREFIXES) {
            if (propertyName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static final Map<String, Function2<Boolean, ArgumentProcessor, Boolean>> FEATURES;

    static {
        Map<String, Function2<Boolean, ArgumentProcessor, Boolean>> features = new HashMap<>(12, 1);
        Function2<Boolean, ArgumentProcessor, Boolean> function2;

        features.put("all", new Function2<Boolean, ArgumentProcessor, Boolean>() {
            public Boolean apply(ArgumentProcessor processor, Boolean enable) {
                // disable all features
                for (Map.Entry<String, Function2<Boolean, ArgumentProcessor, Boolean>> entry : FEATURES.entrySet()) {
                    if (entry.getKey().equals("all")) continue; // skip self
                    entry.getValue().apply(processor, enable);
                }
                return true;
            }
        });
        features.put("gem", new Function2<Boolean, ArgumentProcessor, Boolean>() {
            public Boolean apply(ArgumentProcessor processor, Boolean enable) {
                processor.config.setDisableGems(!enable);
                return true;
            }
        });
        features.put("gems", new Function2<Boolean, ArgumentProcessor, Boolean>() {
            public Boolean apply(ArgumentProcessor processor, Boolean enable) {
                processor.config.setDisableGems(!enable);
                return true;
            }
        });
        features.put("did-you-mean", function2 = new Function2<Boolean, ArgumentProcessor, Boolean>() {
            public Boolean apply(ArgumentProcessor processor, Boolean enable) {
                processor.config.setDisableDidYouMean(!enable);
                return true;
            }
        });
        features.put("did_you_mean", function2); // alias
        features.put("rubyopt", new Function2<Boolean, ArgumentProcessor, Boolean>() {
            public Boolean apply(ArgumentProcessor processor, Boolean enable) {
                processor.config.setDisableRUBYOPT(!enable);
                return true;
            }
        });
        features.put("frozen-string-literal", function2 = new Function2<Boolean, ArgumentProcessor, Boolean>() {
            public Boolean apply(ArgumentProcessor processor, Boolean enable) {
                processor.config.setFrozenStringLiteral(enable);
                return true;
            }
        });
        features.put("frozen_string_literal", function2); // alias

        FEATURES = features;
    }
}
