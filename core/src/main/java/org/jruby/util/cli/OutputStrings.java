package org.jruby.util.cli;

import com.headius.options.Option;
import java.time.LocalDate;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.runtime.Constants;
import org.jruby.util.SafePropertyAccessor;

/**
 * Utility methods to generate the command-line output strings for help,
 * extended options, properties, version, and copyright strings.
 */
public class OutputStrings {
    public static String getBasicUsageHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("Usage: jruby [switches] [--] [programfile] [arguments]\n")
                .append("  -0[octal]         specify record separator (\\0, if no argument)\n")
                .append("  -a                autosplit mode with -n or -p (splits $_ into $F)\n")
                .append("  -c                check syntax only\n")
                .append("  -Cdirectory       cd to directory, before executing your script\n")
                .append("  -d                set debugging flags (set $DEBUG to true)\n")
                .append("  -e 'command'      one line of script. Several -e's allowed. Omit [programfile]\n")
                .append("  -Eex[:in]         specify the default external and internal character encodings\n")
                .append("  -Fpattern         split() pattern for autosplit (-a)\n")
                .append("  -G                load a Bundler Gemspec before executing any user code\n")
                .append("  -i[extension]     edit ARGV files in place (make backup if extension supplied)\n")
                .append("  -Idirectory       specify $LOAD_PATH directory (may be used more than once)\n")
                .append("  -J[java option]   pass an option on to the JVM (e.g. -J-Xmx512m)\n")
                .append("                      use --properties to list JRuby properties\n")
                .append("                      run 'java -help' for a list of other Java options\n")
                //.append("  -Kkcode           specifies code-set (e.g. -Ku for Unicode, -Ke for EUC and -Ks\n")
                //.append("                      for SJIS)\n")
                .append("  -l                enable line ending processing\n")
                .append("  -n                assume 'while gets(); ... end' loop around your script\n")
                .append("  -p                assume loop like -n but print line also like sed\n")
                .append("  -rlibrary         require the library, before executing your script\n")
                .append("  -s                enable some switch parsing for switches after script name\n")
                .append("  -S                look for the script in bin or using PATH environment variable\n")
                .append("  -T[level]         turn on tainting checks\n")
                .append("  -U                use UTF-8 as default internal encoding\n")
                .append("  -v                print version number, then turn on verbose mode\n")
                .append("  -w                turn warnings on for your script\n")
                .append("  -W[level]         set warning level; 0=silence, 1=medium, 2=verbose (default)\n")
                .append("  -x[directory]     strip off text before #!ruby line and perhaps cd to directory\n")
                .append("  -X[option]        enable extended option (omit option to list)\n")
                .append("  -y                enable parsing debug output\n")
                .append("  --copyright       print the copyright\n")
                .append("  --debug           sets the execution mode most suitable for debugger\n")
                .append("                      functionality\n")
                .append("  --jdb             runs JRuby process under JDB\n")
                .append("  --properties      List all configuration Java properties\n")
                .append("                      (prepend \"jruby.\" when passing directly to Java)\n")
                .append("  --environment     Log environment and command line flags but do not run JRuby\n")
                .append("  --sample          run with profiling using the JVM's sampling profiler\n")
                .append("  --profile         run with instrumented (timed) profiling, flat format\n")
                .append("  --profile.api     activate Ruby profiler API\n")
                .append("  --profile.flat    synonym for --profile\n")
                .append("  --profile.graph   run with instrumented (timed) profiling, graph format\n")
                .append("  --profile.html    run with instrumented (timed) profiling, graph format in HTML\n")
                .append("  --profile.json    run with instrumented (timed) profiling, graph format in JSON\n")
                .append("  --profile.out     [file]\n")
                .append("  --profile.service <ProfilingService implementation classname>\n")
                .append("                    output profile data to [file]\n")
                .append("  --client          use the non-optimizing \"client\" JVM\n")
                .append("                      (improves startup; default)\n")
                .append("  --server          use the optimizing \"server\" JVM (improves perf)\n")
                .append("  --headless        do not launch a GUI window, no matter what\n")
                .append("  --dev             prioritize startup time over long term performance\n")
                .append("  --manage          enable remote JMX management and monitoring of JVM and JRuby\n")
                .append("  --bytecode        show the JVM bytecode produced by compiling specified code\n")
                .append("  --version         print the version\n")
                .append("  --disable-gems    do not load RubyGems on startup\n")
                .append("  --enable=feature[,...], --disable=feature[,...]\n")
                .append("                    enable or disable features\n");

        return sb.toString();
    }

    public static String getFeaturesHelp() { return
        "Features:\n" +
        "  gems                   rubygems (default: " + (Options.CLI_RUBYGEMS_ENABLE.defaultValue() ? "enabled" : "disabled") + ")\n" +
        "  did_you_mean           did_you_mean (default: " + (Options.CLI_DID_YOU_MEAN_ENABLE.defaultValue() ? "enabled" : "disabled") + ")\n" +
        "  rubyopt                RUBYOPT environment variable (default: " + (Options.CLI_RUBYOPT_ENABLE.defaultValue() ? "enabled" : "disabled") + ")\n" +
        "  frozen-string-literal  freeze all string literals (default: disabled)\n" ;
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
                .append("# These properties can be used to alter runtime behavior for perf or compatibility.\n")
                .append("# Specify them by passing -X<property>=<value>\n")
                .append("#   or if passing directly to Java, -Djruby.<property>=<value>\n")
                .append("#   or put <property>=<value> in .jrubyrc\n")
                .append("#\n")
                .append("# This dump is a valid .jrubyrc file of current settings. Uncomment and modify\n")
                .append("# settings to customize.\n");
        
        sb.append(Option.formatOptions(Options.PROPERTIES));

        return sb.toString();
    }

    public static String getVersionString() {
        return String.format(
            "jruby %s (%s) %s %s %s %s on %s%s%s [%s-%s]",
                Constants.VERSION,
                Constants.RUBY_VERSION,
                Constants.COMPILE_DATE,
                Constants.REVISION,
                SafePropertyAccessor.getProperty("java.vm.name", "Unknown JVM"),
                SafePropertyAccessor.getProperty("java.vm.version", "Unknown JVM version"),
                SafePropertyAccessor.getProperty("java.runtime.version", SafePropertyAccessor.getProperty("java.version", "Unknown version")),
                Options.COMPILE_INVOKEDYNAMIC.load() ? " +indy" : "",
                Options.COMPILE_MODE.load().shouldJIT() ? " +jit" : "",
                RbConfigLibrary.getOSName(),
                RbConfigLibrary.getArchitecture()
        );
    }

    public static String getCopyrightString() {
        return String.format("JRuby - Copyright (C) 2001-%s The JRuby Community (and contribs)", LocalDate.now().getYear());
    }
}
