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

import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.OutputStrings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Main {

    public static void main(String[] args) {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        config.setHardExit(true);
        config.processArguments(args);

        doShowVersion(config);
        doShowCopyright(config);
        doPrintProperties(config);

        final int exitCode;

        if (config.getShouldRunInterpreter()) {
            final InputStream in = config.getScriptSource();
            final String filename = config.displayedFileName();

            final RubyEngine rubyEngine = new RubyEngine(config);

            try {
                if (in == null) {
                    exitCode = 1;
                } else if (config.isXFlag() && !config.hasShebangLine()) {
                    // no shebang was found and x option is set
                    config.getError().println("jruby: no Ruby script found in input (LoadError)");
                    exitCode = 1;
                } else if (config.getShouldCheckSyntax()) {
                    // check syntax only and exit
                    exitCode = doCheckSyntax(rubyEngine, in, filename);
                } else {
                    exitCode = rubyEngine.execute(filename);
                }
            } finally {
                rubyEngine.dispose();
            }
        } else {
            doPrintUsage(config, false);
            exitCode = 1;
        }

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

    private static int doCheckSyntax(RubyEngine engine, InputStream in, String filename) {
        // check primary script
        boolean status = checkStreamSyntax(engine, in, filename);

        // check other scripts specified on argv
        for (String arg : engine.getContext().getInstanceConfig().getArgv()) {
            status = status && checkFileSyntax(engine, arg);
        }

        return status ? 0 : -1;
    }

    private static boolean checkFileSyntax(RubyEngine engine, String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                return checkStreamSyntax(engine, new FileInputStream(file), filename);
            } catch (FileNotFoundException fnfe) {
                engine.getContext().getInstanceConfig().getError().println("File not found: " + filename);
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean checkStreamSyntax(RubyEngine engine, InputStream in, String filename) {
        return engine.checkSyntax(in, filename);
    }

}
