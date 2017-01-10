/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
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
 */
package org.jruby.truffle;

import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import org.jruby.truffle.language.control.JavaException;
import org.jruby.truffle.options.ArgumentProcessor;
import org.jruby.truffle.options.MainExitException;
import org.jruby.truffle.options.OptionsBuilder;
import org.jruby.truffle.options.OptionsCatalog;
import org.jruby.truffle.options.OutputStrings;
import org.jruby.truffle.options.RubyInstanceConfig;
import org.jruby.truffle.platform.graal.Graal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

public class Main {

    private static final boolean METRICS_TIME = OptionsBuilder.readSystemProperty(OptionsCatalog.METRICS_TIME);
    private static final boolean METRICS_MEMORY_USED_ON_EXIT = OptionsBuilder.readSystemProperty(OptionsCatalog.METRICS_MEMORY_USED_ON_EXIT);

    public static void main(String[] args) {
        printTruffleTimeMetric("before-main");

        final RubyInstanceConfig config = new RubyInstanceConfig();

        try {
            processArguments(config, args);
        } catch (MainExitException mee) {
            if (!mee.isAborted()) {
                System.err.println(mee.getMessage());
                if (mee.isUsageError()) {
                   System.out.print(OutputStrings.getBasicUsageHelp());
                }
            }
            System.exit(mee.getStatus());
        }

        if (config.isShowVersion()) {
            System.out.println(OutputStrings.getVersionString());
        }

        if (config.isShowCopyright()) {
            System.out.println(OutputStrings.getCopyrightString());
        }

        final int exitCode;

        if (config.getShouldRunInterpreter()) {
            final String filename = displayedFileName(config);

            final PolyglotEngine engine = PolyglotEngine.newBuilder()
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.LOAD_PATHS.getName(), config.getLoadPaths().toArray(new String[]{}))
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.REQUIRED_LIBRARIES.getName(), config.getRequiredLibraries().toArray(new String[]{}))
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.INLINE_SCRIPT.getName(), config.inlineScript())
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.ARGUMENTS.getName(), config.getArgv())
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.DISPLAYED_FILE_NAME.getName(), filename)
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.DEBUG.getName(), config.isDebug())
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.VERBOSITY.getName(), config.getVerbosity().ordinal())
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.FROZEN_STRING_LITERALS.getName(), config.isFrozenStringLiteral())
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.DISABLE_GEMS.getName(), config.isDisableGems())
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.INTERNAL_ENCODING.getName(), config.getInternalEncoding())
                    .config(RubyLanguage.MIME_TYPE, OptionsCatalog.EXTERNAL_ENCODING.getName(), config.getExternalEncoding())
                    .build();

            Main.printTruffleTimeMetric("before-load-context");
            final RubyContext context = engine.eval(loadSource("Truffle::Boot.context", "context")).as(RubyContext.class);
            Main.printTruffleTimeMetric("after-load-context");

            printTruffleTimeMetric("before-run");
            try {
                if (config.isXFlag()) {
                    // no shebang was found and x option is set
                    System.err.println("jruby: no Ruby script found in input (LoadError)");
                    exitCode = 1;
                } else if (config.getShouldCheckSyntax()) {
                    // check syntax only and exit
                    exitCode = checkSyntax(engine, context, getScriptSource(config), filename);
                } else {
                    if (!Graal.isGraal() && context.getOptions().GRAAL_WARNING_UNLESS) {
                        Log.performanceOnce("This JVM does not have the Graal compiler - performance will be limited - " +
                                "see https://github.com/jruby/jruby/wiki/Truffle-FAQ#how-do-i-get-jrubytruffle");
                    }

                    if (config.shouldUsePathScript()) {
                        context.setOriginalInputFile(config.getScriptFileName());
                        exitCode = engine.eval(loadSource("Truffle::Boot.main_s", "main")).as(Integer.class);
                    } else {
                        context.setOriginalInputFile(filename);
                        exitCode = engine.eval(loadSource("exit Truffle::Boot.main", "main")).as(Integer.class);
                    }
                }
            } finally {
                printTruffleTimeMetric("after-run");
                engine.dispose();
            }
        } else {
            if (config.getShouldPrintUsage()) {
                System.out.print(OutputStrings.getBasicUsageHelp());
            }
            exitCode = 1;
        }

        printTruffleTimeMetric("after-main");
        printTruffleMemoryMetric();
        System.exit(exitCode);
    }

    public static void processArguments(RubyInstanceConfig config, String[] arguments) {
        new ArgumentProcessor(arguments, config).processArguments();

        Object rubyoptObj = System.getenv("RUBYOPT");
        String rubyopt = rubyoptObj == null ? null : rubyoptObj.toString();

        if (rubyopt != null && rubyopt.length() != 0) {
            String[] rubyoptArgs = rubyopt.split("\\s+");
            if (rubyoptArgs.length != 0) {
                new ArgumentProcessor(rubyoptArgs, false, true, true, config).processArguments();
            }
        }

        if (!TruffleOptions.AOT && !config.doesHaveScriptArgv() && !config.shouldUsePathScript() && System.console() != null) {
            config.setUsePathScript("irb");
        }
    }

    public static InputStream getScriptSource(RubyInstanceConfig config) {
        try {
            if (config.isInlineScript()) {
                return new ByteArrayInputStream(config.inlineScript());
            } else if (config.isForceStdin() || config.getScriptFileName() == null) {
                return System.in;
            } else {
                final String script = config.getScriptFileName();
                return new FileInputStream(script);
            }
        } catch (IOException e) {
            throw new JavaException(e);
        }
    }

    public static String displayedFileName(RubyInstanceConfig config) {
        if (config.isInlineScript()) {
            if (config.getScriptFileName() != null) {
                return config.getScriptFileName();
            } else {
                return "-e";
            }
        } else if (config.shouldUsePathScript()) {
            return "-S";
        } else if (config.isForceStdin() || config.getScriptFileName() == null) {
            return "-";
        } else {
            return config.getScriptFileName();
        }
    }

    public static void printTruffleTimeMetric(String id) {
        if (METRICS_TIME) {
            final long millis = System.currentTimeMillis();
            System.err.printf("%s %d.%03d%n", id, millis / 1000, millis % 1000);
        }
    }

    private static void printTruffleMemoryMetric() {
        // Memory stats aren't available on AOT.
        if (!TruffleOptions.AOT && METRICS_MEMORY_USED_ON_EXIT) {
            for (int n = 0; n < 10; n++) {
                System.gc();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.err.printf("allocated %d%n", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        }
    }

    private static Source loadSource(String source, String name) {
        return Source.newBuilder(source).name(name).internal().mimeType(RubyLanguage.MIME_TYPE).build();
    }

    private static int checkSyntax(PolyglotEngine engine, RubyContext context, InputStream in, String filename) {
        // check primary script
        boolean status = runCheckSyntax(engine, context, in, filename);

        // check other scripts specified on argv
        for (String arg : context.getOptions().ARGUMENTS) {
            status = status && checkFileSyntax(engine, context, arg);
        }

        return status ? 0 : 1;
    }


    private static boolean checkFileSyntax(PolyglotEngine engine, RubyContext context, String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                return runCheckSyntax(engine, context, new FileInputStream(file), filename);
            } catch (FileNotFoundException fnfe) {
                System.err.println("File not found: " + filename);
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean runCheckSyntax(PolyglotEngine engine, RubyContext context, InputStream in, String filename) {
        context.setSyntaxCheckInputStream(in);
        context.setOriginalInputFile(filename);

        return engine.eval(loadSource("Truffle::Boot.check_syntax", "check_syntax")).as(Boolean.class);
    }

}
