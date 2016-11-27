/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;

import java.io.InputStream;
import java.lang.management.ManagementFactory;

public class Main {

    public static void main(String[] args) {
        printTruffleTimeMetric("before-main");

        final RubyInstanceConfig config = new RubyInstanceConfig(false);
        config.setHardExit(true);
        config.processArguments(args);
        config.setCompileMode(RubyInstanceConfig.CompileMode.TRUFFLE);

        doShowVersion(config);
        doShowCopyright(config);
        doPrintProperties(config);

        final int exitCode;

        if (config.getShouldRunInterpreter()) {
            final InputStream in = config.getScriptSource();
            final String filename = config.displayedFileName();

            final RubyEngine rubyEngine = new RubyEngine(config);

            printTruffleTimeMetric("before-run");
            try {
                if (in == null) {
                    exitCode = 1;
                } else if (config.isXFlag() && !config.hasShebangLine()) {
                    // no shebang was found and x option is set
                    config.getError().println("jruby: no Ruby script found in input (LoadError)");
                    exitCode = 1;
                } else if (config.getShouldCheckSyntax()) {
                    // check syntax only and exit
                    exitCode = rubyEngine.doCheckSyntax(in, filename);
                } else {
                    exitCode = rubyEngine.execute(filename);
                }
            } finally {
                printTruffleTimeMetric("after-run");
                rubyEngine.dispose();
            }
        } else {
            doPrintUsage(config, false);
            exitCode = 1;
        }

        printTruffleTimeMetric("after-main");
        printTruffleMemoryMetric();
        System.exit(exitCode);
    }

    private static void doPrintProperties(RubyInstanceConfig config) {
        if (config.getShouldPrintProperties()) {
            config.getOutput().print(OutputStrings.getPropertyHelp());
        }
    }

    private static void doPrintUsage(RubyInstanceConfig config, boolean force) {
        if (config.getShouldPrintUsage() || force) {
            config.getOutput().print(OutputStrings.getBasicUsageHelp());
            config.getOutput().print(OutputStrings.getFeaturesHelp());
        }
    }

    private static void doShowCopyright(RubyInstanceConfig config) {
        if (config.isShowCopyright()) {
            config.getOutput().println(OutputStrings.getCopyrightString());
        }
    }

    private static void doShowVersion(RubyInstanceConfig config) {
        if (config.isShowVersion()) {
            config.getOutput().println(OutputStrings.getVersionString());
        }
    }

    public static void printTruffleTimeMetric(String id) {
        if (Options.TRUFFLE_METRICS_TIME.load()) {
            final long millis = System.currentTimeMillis();
            System.err.printf("%s %d.%03d%n", id, millis / 1000, millis % 1000);
        }
    }

    private static void printTruffleMemoryMetric() {
        // Memory stats aren't available on AOT.
        if (!TruffleOptions.AOT && Options.TRUFFLE_METRICS_MEMORY_USED_ON_EXIT.load()) {
            for (int n = 0; n < 10; n++) {
                System.gc();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            System.err.printf("allocated %d%n", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        }
    }

}
