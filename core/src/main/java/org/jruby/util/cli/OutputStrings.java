package org.jruby.util.cli;

import com.headius.options.Option;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.Constants;
import org.jruby.util.SafePropertyAccessor;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * Utility methods to generate the command-line output strings for help,
 * extended options, properties, version, and copyright strings.
 */
public class OutputStrings {
    public static String getBasicUsageHelp() {
        return getBasicUsageHelp(false);
    }

    public static String getBasicUsageHelp(boolean tty) {
        String[][] basicUsageOptions = {
                {"-0[octal]", "specify record separator (\\0, if no argument)"},
                {"-a", "autosplit mode with -n or -p (splits $_ into $F)"},
                {"-c", "check syntax only"},
                {"-Cdirectory", "cd to directory, before executing your script"},
                {"-d", "set debugging flags (set $DEBUG to true)"},
                {"-e 'command'", "one line of script. Several -e's allowed. Omit [programfile]"},
                {"-Eex[:in]", "specify the default external and internal character encodings"},
                {"-Fpattern", "split() pattern for autosplit (-a)"},
                {"-G", "load a Bundler Gemspec before executing any user code"},
                {"-i[extension]", "edit ARGV files in place (make backup if extension supplied)"},
                {"-Idirectory", "specify $LOAD_PATH directory (may be used more than once)"},
                {"-J[java option]", "pass an option on to the JVM (e.g. -J-Xmx512m); use --properties to list JRuby properties; run 'java -help' for a list of other Java options"},
                {"-l", "enable line ending processing"},
                {"-n", "assume 'while gets(); ... end' loop around your script"},
                {"-p", "assume loop like -n but print line also like sed"},
                {"-rlibrary", "require the library, before executing your script"},
                {"-s", "enable some switch parsing for switches after script name"},
                {"-S", "look for the script in bin or using PATH environment variable"},
                {"-U", "use UTF-8 as default internal encoding"},
                {"-v", "print version number, then turn on verbose mode"},
                {"-w", "turn warnings on for your script"},
                {"-W[level]", "set warning level; 0=silence, 1=medium, 2=verbose (default)"},
                {"-x[directory]", "strip off text before #!ruby line and perhaps cd to directory"},
                {"-X[option]", "enable extended option (omit option to list)"},
                {"-y", "enable parsing debug output"},
                {"--backtrace-limit[lines]", "limits the maximum length of a backtrace"},
                {"--copyright", "print the copyright"},
                {"--debug", "sets the execution mode most suitable for debugger functionality"},
                {"--jdb", "runs JRuby process under JDB"},
                {"--properties", "List all JRuby configuration properties"},
                {"--sample", "run with profiling using the JVM's sampling profiler"},
                {"--profile", "run with instrumented (timed) profiling, flat format"},
                {"--profile.api", "activate Ruby profiler API"},
                {"--profile.flat", "synonym for --profile"},
                {"--profile.graph", "run with instrumented (timed) profiling, graph format"},
                {"--profile.html", "run with instrumented (timed) profiling, graph format in HTML"},
                {"--profile.json", "run with instrumented (timed) profiling, graph format in JSON"},
                {"--profile.out", "[file]"},
                {"--profile.service", "<ProfilingService implementation classname> output profile data to [file]"},
                {"--headless", "do not launch a GUI window, no matter what"},
                {"--dev", "prioritize startup time over long term performance"},
                {"--cache", "EXPERIMENTAL: Regenerate the JRuby AppCDS archive to improve startup"},
                {"--manage", "enable remote JMX management and monitoring of JVM and JRuby"},
                {"--bytecode", "show the JVM bytecode produced by compiling specified code"},
                {"--version", "print the version"},
                {"--disable-gems", "do not load RubyGems on startup (only for debugging)"},
                {"--enable=feature[,...], --disable=feature[,...]", "enable or disable features"}
        };

        String header = strBold("Usage:", tty) + " jruby [switches] [--] [programfile] [arguments]";
        return buildOutputOptions(basicUsageOptions, header, tty);
    }

    private static String buildOutputOptions(String[][] options, String header, boolean tty) {
        StringBuilder sb = new StringBuilder();
        sb.append(strBold(header, tty)).append("\n");

        int max = Integer.MIN_VALUE;
        for (String[] strings : options) {
            int s = strings[0].length();
            if (s > max) {
                max = s;
            }
        }

        for (String[] option : options) {
            String key = option[0];
            String value = option[1];

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

        String[][] options = {
                {"gems", "rubygems (default: " + (Options.CLI_RUBYGEMS_ENABLE.defaultValue() ? "enabled" : "disabled") + ")"},
                {"did_you_mean", "did_you_mean (default: " + (Options.CLI_DID_YOU_MEAN_ENABLE.defaultValue() ? "enabled" : "disabled") + ")"},
                {"rubyopt", "RUBYOPT environment variable (default: " + (Options.CLI_RUBYOPT_ENABLE.defaultValue() ? "enabled" : "disabled") + ")"},
                {"frozen-string-literal", "freeze all string literals (default: disabled)"}};

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