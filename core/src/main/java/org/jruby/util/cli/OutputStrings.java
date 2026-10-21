package org.jruby.util.cli;

import com.headius.options.Option;
import org.jruby.RubyInstanceConfig;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Constants;
import org.jruby.util.SafePropertyAccessor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods to generate the command-line output strings for help,
 * extended options, properties, version, and copyright strings.
 */
public class OutputStrings {
    public static String getBasicUsageHelp() {
        return getBasicUsageHelp(false);
    }

    public static String getBasicUsageHelp(boolean tty) {
        String header = strBold("Usage:", tty) + " " + BASIC_USAGE_SYNOPSIS;
        return buildOutputOptions(BASIC_USAGE_OPTIONS, header, tty);
    }

    /**
     * The command synopsis shared by {@code --help} and the generated
     * {@code jruby.1} man page (see {@code tool/man/man_page.rb})
     */
    public static final String BASIC_USAGE_SYNOPSIS = "jruby [switches] [--] [programfile] [arguments]";

    /**
     * A single command-line switch and its description.
     *
     * @param flag the switch as shown by {@code --help} (e.g. {@code "--dev"})
     * @param description the human-readable description of the switch
     */
    public record CliOption(String flag, String description) {}

    /**
     * The command-line switches shown by {@code --help}.
     *
     * <p>This is the single source of truth for JRuby's documented switches: both the
     * {@code --help} text and the generated {@code jruby.1} man page render from this list
     * (see {@code tool/man/man_page.rb})
     */
    public static final List<CliOption> BASIC_USAGE_OPTIONS = List.of(
                new CliOption("-0[octal]", "specify record separator (\\0, if no argument)"),
                new CliOption("-a", "autosplit mode with -n or -p (splits $_ into $F)"),
                new CliOption("-c", "check syntax only"),
                new CliOption("-Cdirectory", "cd to directory, before executing your script"),
                new CliOption("-d", "set debugging flags (set $DEBUG to true)"),
                new CliOption("-e 'command'", "one line of script. Several -e's allowed. Omit [programfile]"),
                new CliOption("-Eex[:in]", "specify the default external and internal character encodings"),
                new CliOption("-Fpattern", "split() pattern for autosplit (-a)"),
                new CliOption("-G", "load a Bundler Gemspec before executing any user code"),
                new CliOption("-i[extension]", "edit ARGV files in place (make backup if extension supplied)"),
                new CliOption("-Idirectory", "specify $LOAD_PATH directory (may be used more than once)"),
                new CliOption("-J[java option]", "pass an option on to the JVM (e.g. -J-Xmx512m); use --properties to list JRuby properties; run 'java -help' for a list of other Java options"),
                new CliOption("-l", "enable line ending processing"),
                new CliOption("-n", "assume 'while gets(); ... end' loop around your script"),
                new CliOption("-p", "assume loop like -n but print line also like sed"),
                new CliOption("-rlibrary", "require the library, before executing your script"),
                new CliOption("-s", "enable some switch parsing for switches after script name"),
                new CliOption("-S", "look for the script in bin or using PATH environment variable"),
                new CliOption("-U", "use UTF-8 as default internal encoding"),
                new CliOption("-v", "print version number, then turn on verbose mode"),
                new CliOption("-w", "turn warnings on for your script"),
                new CliOption("-W[level]", "set warning level; 0=silence, 1=medium, 2=verbose (default)"),
                new CliOption("-x[directory]", "strip off text before #!ruby line and perhaps cd to directory"),
                new CliOption("-X[option]", "enable extended option (omit option to list)"),
                new CliOption("-y", "enable parsing debug output"),
                new CliOption("--backtrace-limit[lines]", "limits the maximum length of a backtrace"),
                new CliOption("--copyright", "print the copyright"),
                new CliOption("--debug", "sets the execution mode most suitable for debugger functionality"),
                new CliOption("--jdb", "runs JRuby process under JDB"),
                new CliOption("--properties", "List all JRuby configuration properties"),
                new CliOption("--sample", "run with profiling using the JVM's sampling profiler"),
                new CliOption("--profile", "run with instrumented (timed) profiling, flat format"),
                new CliOption("--profile.api", "activate Ruby profiler API"),
                new CliOption("--profile.flat", "synonym for --profile"),
                new CliOption("--profile.graph", "run with instrumented (timed) profiling, graph format"),
                new CliOption("--profile.html", "run with instrumented (timed) profiling, graph format in HTML"),
                new CliOption("--profile.json", "run with instrumented (timed) profiling, graph format in JSON"),
                new CliOption("--profile.out", "[file]"),
                new CliOption("--profile.service", "<ProfilingService implementation classname> output profile data to [file]"),
                new CliOption("--headless", "do not launch a GUI window, no matter what"),
                new CliOption("--dev", "prioritize startup time over long term performance"),
                new CliOption("--cache", "EXPERIMENTAL: Regenerate the JRuby AppCDS archive to improve startup"),
                new CliOption("--manage", "enable remote JMX management and monitoring of JVM and JRuby"),
                new CliOption("--bytecode", "show the JVM bytecode produced by compiling specified code"),
                new CliOption("--version", "print the version"),
                new CliOption("--disable-gems", "do not load RubyGems on startup (only for debugging)"),
                new CliOption("--enable=feature[,...], --disable=feature[,...]", "enable or disable features")
        );

    private static String buildOutputOptions(List<CliOption> options, String header, boolean tty) {
        StringBuilder sb = new StringBuilder();
        sb.append(strBold(header, tty)).append("\n");

        int max = Integer.MIN_VALUE;
        for (CliOption option : options) {
            int s = option.flag().length();
            if (s > max) {
                max = s;
            }
        }

        for (CliOption option : options) {
            String key = option.flag();
            String value = option.description();

            String text = breakLine(value, 60, max + 8);
            sb.append("   ").append(strBold(key, tty)).append(generateSpaces(max + 5 - key.length())).append(text).append("\n");
        }
        return sb.toString();
    }

    public static String getFeaturesHelp() {
        return getFeaturesHelp(false);
    }

    public static String getFeaturesHelp(boolean tty) {
        String header = "Features:";

        List<CliOption> options = List.of(
                new CliOption("gems", "rubygems (default: " + (Options.CLI_RUBYGEMS_ENABLE.defaultValue() ? "enabled" : "disabled") + ")"),
                new CliOption("did_you_mean", "did_you_mean (default: " + (Options.CLI_DID_YOU_MEAN_ENABLE.defaultValue() ? "enabled" : "disabled") + ")"),
                new CliOption("rubyopt", "RUBYOPT environment variable (default: " + (Options.CLI_RUBYOPT_ENABLE.defaultValue() ? "enabled" : "disabled") + ")"),
                new CliOption("frozen-string-literal", "freeze all string literals (default: disabled)"));

        return buildOutputOptions(options, header, tty);
    }

    public static String getExtendedHelp() { return
        "Extended options:\n" +
        "  -X-O          run with ObjectSpace disabled (default; improves performance)\n" +
        "  -X+O          run with ObjectSpace enabled (reduces performance)\n" +
        "  -X-C          disable all compilation\n" +
        "  -X-CIR        disable all compilation and use IR runtime\n" +
        "  -X+C          force compilation of all scripts before they are run (except eval)\n" +
        "  -X+CIR        force compilation and use IR runtime\n" +
        "  -X+JIR        JIT compilation and use IR runtime\n" +
        "  -Xsubstring?  list options that contain substring in their name\n" +
        "  -Xprefix...   list options that are prefixed with prefix\n" ;
    }

    public static String getPropertyHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("# These properties can be used to alter runtime behavior for performance\n")
                .append("# or compatibility.\n")
                .append("#\n")
                .append("# Specify them by passing '-X<property>=<value>' to the jruby command,\n")
                .append("# or put '<property>=<value>' in .jrubyrc. If passing to the java command,\n")
                .append("# use the flag '-Djruby.<property>=<value>'\n")
                .append("#\n")
                .append("# This output is the current settings as a valid .jrubyrc file.\n");

        return sb.append(Option.formatOptions(Options.PROPERTIES)).toString();
    }

    /**
     * Produce a version string based on the given configuration.
     *
     * @param config the configuraton
     * @return a version string representing the given configuraton
     */
    public static String getVersionString(RubyInstanceConfig config) {
        return String.format(
                "jruby %s (%s) %s %s %s %s on %s%s%s [%s-%s]",
                Constants.VERSION,
                Constants.RUBY_VERSION,
                Constants.COMPILE_DATE,
                Constants.REVISION.substring(0, 10),
                SafePropertyAccessor.getProperty("java.vm.name", "Unknown JVM"),
                SafePropertyAccessor.getProperty("java.vm.version", "Unknown JVM version"),
                SafePropertyAccessor.getProperty("java.runtime.version", SafePropertyAccessor.getProperty("java.version", "Unknown version")),
                Options.COMPILE_INVOKEDYNAMIC.load() ? " +indy" : " -indy",
                config.getCompileMode().shouldJIT() ? " +jit" : " -jit",
                RbConfigLibrary.getArchitecture(),
                RbConfigLibrary.getOSName()
        );
    }

    /**
     * Produce a version string containing only static values for JRuby and system properties.
     *
     * Use {@link #getVersionString(RubyInstanceConfig)} to reflect current runtime's parameters.
     *
     * @return a version string containing static information about the available JRuby version
     */
    public static String getVersionString() {
        return String.format(
                "jruby %s (%s) %s %s %s %s on %s%s%s [%s-%s]",
                Constants.VERSION,
                Constants.RUBY_VERSION,
                Constants.COMPILE_DATE,
                Constants.REVISION.substring(0, 10),
                SafePropertyAccessor.getProperty("java.vm.name", "Unknown JVM"),
                SafePropertyAccessor.getProperty("java.vm.version", "Unknown JVM version"),
                SafePropertyAccessor.getProperty("java.runtime.version", SafePropertyAccessor.getProperty("java.version", "Unknown version")),
                Options.COMPILE_INVOKEDYNAMIC.load() ? " +indy" : "",
                Options.COMPILE_MODE.load().shouldJIT() ? " +jit" : "",
                RbConfigLibrary.getArchitecture(),
                RbConfigLibrary.getOSName()
        );
    }

    public static String getCopyrightString() {
        return String.format("JRuby - Copyright (C) 2001-%s The JRuby Community (and contribs)", LocalDate.now().getYear());
    }

    private static String strBold(String str, boolean tty) {
        if (!tty || Platform.IS_WINDOWS)
            return str;

        return "\033[1m" + str + "\033[0m";
    }

    private static final int SPACES_MAX = 256;
    private static final char[] SPACES = new char[SPACES_MAX];
    static {
        Arrays.fill(SPACES, ' ');
    }

    private static String generateSpaces(int total) {
        char[] spaces;

        if (total > SPACES_MAX) {
            spaces = new char[total];
            Arrays.fill(spaces, ' ');
        } else {
            spaces = SPACES;
        }

        return new String(spaces, 0, total);
    }

    private static String breakLine(String str, int index, int spaces) {
        StringBuilder sb = new StringBuilder();

        String[] words = str.split("\\s");
        int counter = 0;

        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            if (counter + word.length() <= index) {
                counter += word.length();
                sb.append(word).append(" ");
            } else {
                counter = 0;
                if (i == words.length - 1) sb.append("\n" + generateSpaces(spaces) + word);
                else sb.append("\n").append(generateSpaces(spaces));
            }
        }
        return sb.toString();
    }
}